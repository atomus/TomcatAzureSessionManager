/*
 * (C) Copyright Atomus Ltd 2011 - All rights reserved.
 *
 * This software is provided "as is" without warranty of any kind,
 * express or implied, including but not limited to warranties as to
 * quality and fitness for a particular purpose. Atomus Ltd
 * does not support the Software, nor does it warrant that the Software
 * will meet your requirements or that the operation of the Software will
 * be uninterrupted or error free or that any defects will be
 * corrected. Nothing in this statement is intended to limit or exclude
 * any liability for personal injury or death caused by the negligence of
 * Atomus Ltd, its employees, contractors or agents.
 */

package uk.co.atomus.session.service.dao;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.soyatec.windowsazure.blob.IRetryPolicy;
import org.soyatec.windowsazure.blob.internal.RetryPolicies;
import org.soyatec.windowsazure.error.StorageException;
import org.soyatec.windowsazure.internal.util.TimeSpan;
import org.soyatec.windowsazure.table.AbstractTableServiceEntity;
import org.soyatec.windowsazure.table.ITableServiceEntity;
import org.soyatec.windowsazure.table.TableServiceContext;
import org.soyatec.windowsazure.table.TableStorageClient;
import org.soyatec.windowsazure.table.internal.CloudTable;

import uk.co.atomus.session.TomcatSessionStorageEntity;

/**
 * @author Simon Dingle and Chris Derham
 *
 */
public class SessionDaoImpl implements SessionDao {
	private final static Log log = LogFactory.getLog(SessionDaoImpl.class);
	private static final String TABLE_NAMESPACE = "http://table.core.windows.net/";
	private static final int DEFAULT_RETRY_POLICY_RETRIES = 10;
	private static final int DEFAULT_RETRY_POLICY_INTERVAL_SECONDS = 1;
	private String accountName;
	private String accountKey;
	private CloudTable azureTable;
	private TableStorageClient tableStorage;
	private String tableName;
	private int retryPolicyRetries = DEFAULT_RETRY_POLICY_RETRIES;
	private int retryPolicyIntervalSeconds = DEFAULT_RETRY_POLICY_INTERVAL_SECONDS;

	@Override
	public String getAccountName() {
		return accountName;
	}

	@Override
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	@Override
	public String getAccountKey() {
		return accountKey;
	}

	@Override
	public void setAccountKey(String accountKey) {
		this.accountKey = accountKey;
	}

	@Override
	public void setRetryPolicyRetries(int retryPolicyRetries) {
		this.retryPolicyRetries = retryPolicyRetries;
	}

	@Override
	public void setRetryPolicyInterval(int retryPolicyInterval) {
		this.retryPolicyIntervalSeconds = retryPolicyInterval;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	private CloudTable getAzureTable() {
		if (azureTable == null) {
			azureTable = (CloudTable) getTableStorage().getTableReference(tableName);
			if (null == azureTable) {
				throw new NullPointerException(String.format("TableStorage returned null AzureTable '%s'.", tableName));
			}

			azureTable.setRetryPolicy(getRetryPolicy());

			if (!azureTable.isTableExist()) {
				azureTable.createTable();

				if (!azureTable.isTableExist()) {
					throw new RuntimeException(String.format("Table '%s' was not created.", tableName));
				}
			}
			azureTable.setModelClass(TomcatSessionStorageEntity.class);
		}
		return azureTable;
	}

	private TableStorageClient getTableStorage() {
		if (tableStorage == null) {
			tableStorage = TableStorageClient.create(URI.create(TABLE_NAMESPACE), false, accountName, accountKey);
		}
		return tableStorage;
	}

	private IRetryPolicy getRetryPolicy() {
		return RetryPolicies.retryN(retryPolicyRetries, TimeSpan.fromSeconds(retryPolicyIntervalSeconds));
	}

	@Override
	public void removeAll(String partitionKey) {
		List<ITableServiceEntity> entities = queryEntitiesByKeys(partitionKey, null);
		deleteBatch(entities);
	}

	@Override
	public int countEntities(String partitionKey, String rowKey) {
		List<ITableServiceEntity> entities = queryEntitiesByKeys(partitionKey, rowKey);
		return entities.size();
	}

	@Override
	public void updateStorageEntity(TomcatSessionStorageEntity storageEntity) {
		CloudTable table = getAzureTable();
		table.updateEntity(storageEntity);
	}

	@Override
	public void insertStorageEntity(TomcatSessionStorageEntity storageEntity) {
		CloudTable table = getAzureTable();
		table.insertEntity(storageEntity);
	}

	@Override
	public List<ITableServiceEntity> queryEntitiesByKeys(String partitionKey, String rowKey) {
		List<ITableServiceEntity> storageEntities = null;
		CloudTable table = getAzureTable();
		if (null == partitionKey || partitionKey.isEmpty()) {
			/* Return list of all Entities in Table */
			storageEntities = table.retrieveEntities(TomcatSessionStorageEntity.class);
		} else if (null != partitionKey && !partitionKey.isEmpty() && (null == rowKey || rowKey.isEmpty())) {
			storageEntities = table.retrieveEntitiesByKey(partitionKey, null, TomcatSessionStorageEntity.class);
		} else if (null != partitionKey && !partitionKey.isEmpty() && null != rowKey && !rowKey.isEmpty()) {
			storageEntities = table.retrieveEntitiesByKey(partitionKey, rowKey, TomcatSessionStorageEntity.class);
		} else {
			throw new StorageException(String.format("Unexpected condition: partitionKey '%s', rowKey '%s'",
					partitionKey, rowKey));
		}

		if (null == storageEntities) {
			return new ArrayList<ITableServiceEntity>();
		}

		return storageEntities;
	}

	@Override
	public ITableServiceEntity retrieveEntity(String partitionKey, String rowKey) {
		List<ITableServiceEntity> entities = retrieveEntitiesByKey(partitionKey, rowKey);
		return entities.isEmpty() ? null : entities.get(0);
	}

	@Override
	public List<ITableServiceEntity> retrieveEntitiesByKey(String partitionKey, String rowKey) {
		CloudTable table = getAzureTable();
		return table.retrieveEntitiesByKey(partitionKey, rowKey, TomcatSessionStorageEntity.class);
	}

	@Override
	public void remove(String partitionKey, String rowKey) throws StorageException {
		List<ITableServiceEntity> entities = queryEntitiesByKeys(partitionKey, rowKey);
		if (entities.isEmpty()) {
			log.debug("record not deleted as not found with rowKey " + rowKey);
			return;
		}
		deleteBatch(entities);
	}

	private void deleteBatch(List<ITableServiceEntity> entities) {
		if (entities.isEmpty()) {
			return;
		}
		CloudTable table = getAzureTable();
		TableServiceContext tableServiceContext = new TableServiceContext(table);
		tableServiceContext.startBatch();
		for (ITableServiceEntity entity : entities) {
			deleteTableEntity(entity);
		}
		tableServiceContext.executeBatch();
	}

	private void deleteTableEntity(ITableServiceEntity entity) {
		log.debug("deleting record with rowKey" + entity.getRowKey());
		getAzureTable().deleteEntity(entity);
	}

	@Override
	public void removeExpired(String partitionKey) throws StorageException {
		List<ITableServiceEntity> entities = queryEntitiesByKeys(partitionKey, null);
		log.debug("found " + entities.size() + " records");
		for (ITableServiceEntity entity : entities) {
			TomcatSessionStorageEntity sessionStorageEntity = (TomcatSessionStorageEntity) entity;
			if (sessionStorageEntity.hasExpired()) {
				log.debug("found expired record with rowKey" + entity.getRowKey());
				deleteTableEntity(sessionStorageEntity);
			}
		}
	}

}

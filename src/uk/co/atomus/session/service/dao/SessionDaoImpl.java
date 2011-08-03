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
import org.soyatec.windows.azure.blob.RetryPolicies;
import org.soyatec.windows.azure.blob.RetryPolicy;
import org.soyatec.windows.azure.error.StorageException;
import org.soyatec.windows.azure.table.AzureTable;
import org.soyatec.windows.azure.table.TableStorage;
import org.soyatec.windows.azure.table.TableStorageEntity;
import org.soyatec.windows.azure.util.TimeSpan;

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
	private AzureTable azureTable;
	private TableStorage tableStorage;
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

	private AzureTable getAzureTable() {
		if (azureTable == null) {
			azureTable = getTableStorage().getAzureTable(tableName);
			if (null == azureTable) {
				throw new NullPointerException(String.format("TableStorage returned null AzureTable '%s'.", tableName));
			}

			azureTable.setRetryPolicy(getRetryPolicy());

			if (!azureTable.doesTableExist()) {
				azureTable.createTable();

				if (!azureTable.doesTableExist()) {
					throw new RuntimeException(String.format("Table '%s' was not created.", tableName));
				}
			}
			azureTable.setModelClass(TomcatSessionStorageEntity.class);
		}
		return azureTable;
	}

	private TableStorage getTableStorage() {
		if (tableStorage == null) {
			tableStorage = TableStorage.create(URI.create(TABLE_NAMESPACE), false, accountName, accountKey);
		}
		return tableStorage;
	}

	private RetryPolicy getRetryPolicy() {
		return RetryPolicies.retryN(retryPolicyRetries, TimeSpan.fromSeconds(retryPolicyIntervalSeconds));
	}

	@Override
	public void removeAll(String partitionKey) {
		List<TableStorageEntity> entities = queryEntitiesByKeys(partitionKey, null);
		deleteBatch(entities);
	}

	@Override
	public int countEntities(String partitionKey, String rowKey) {
		List<TableStorageEntity> entities = queryEntitiesByKeys(partitionKey, rowKey);
		return entities.size();
	}

	@Override
	public void updateStorageEntity(TomcatSessionStorageEntity storageEntity) {
		AzureTable table = getAzureTable();
		table.updateEntity(storageEntity);
	}

	@Override
	public void insertStorageEntity(TomcatSessionStorageEntity storageEntity) {
		AzureTable table = getAzureTable();
		table.insertEntity(storageEntity);
	}

	@Override
	public List<TableStorageEntity> queryEntitiesByKeys(String partitionKey, String rowKey) {
		List<TableStorageEntity> storageEntities = null;
		AzureTable table = getAzureTable();
		if (null == partitionKey || partitionKey.isEmpty()) {
			/* Return list of all Entities in Table */
			storageEntities = table.retrieveEntities();
		} else if (null != partitionKey && !partitionKey.isEmpty() && (null == rowKey || rowKey.isEmpty())) {
			storageEntities = table.retrieveEntitiesByKey(partitionKey, null);
		} else if (null != partitionKey && !partitionKey.isEmpty() && null != rowKey && !rowKey.isEmpty()) {
			storageEntities = table.retrieveEntitiesByKey(partitionKey, rowKey);
		} else {
			throw new StorageException(String.format("Unexpected condition: partitionKey '%s', rowKey '%s'",
					partitionKey, rowKey));
		}

		if (null == storageEntities) {
			return new ArrayList<TableStorageEntity>();
		}

		return storageEntities;
	}

	@Override
	public TableStorageEntity retrieveEntity(String partitionKey, String rowKey) {
		List<TableStorageEntity> entities = retrieveEntitiesByKey(partitionKey, rowKey);
		return entities.isEmpty() ? null : entities.get(0);
	}

	@Override
	public List<TableStorageEntity> retrieveEntitiesByKey(String partitionKey, String rowKey) {
		AzureTable table = getAzureTable();
		return table.retrieveEntitiesByKey(partitionKey, rowKey);
	}

	@Override
	public void remove(String partitionKey, String rowKey) throws StorageException {
		List<TableStorageEntity> entities = queryEntitiesByKeys(partitionKey, rowKey);
		if (entities.isEmpty()) {
			log.debug("record not deleted as not found with rowKey " + rowKey);
			return;
		}
		deleteBatch(entities);
	}

	private void deleteBatch(List<TableStorageEntity> entities) {
		if (entities.isEmpty()) {
			return;
		}
		AzureTable table = getAzureTable();
		table.startBatch();
		for (TableStorageEntity entity : entities) {
			deleteTableEntity(entity);
		}
		table.executeBatch();
	}

	private void deleteTableEntity(TableStorageEntity entity) {
		log.debug("deleting record with rowKey" + entity.getRowKey());
		getAzureTable().deleteEntity(entity);
	}

	@Override
	public void removeExpired(String partitionKey) throws StorageException {
		List<TableStorageEntity> entities = queryEntitiesByKeys(partitionKey, null);
		log.debug("found " + entities.size() + " records");
		for (TableStorageEntity entity : entities) {
			TomcatSessionStorageEntity sessionStorageEntity = (TomcatSessionStorageEntity) entity;
			if (sessionStorageEntity.hasExpired()) {
				log.debug("found expired record with rowKey" + entity.getRowKey());
				deleteTableEntity(sessionStorageEntity);
			}
		}
	}

}

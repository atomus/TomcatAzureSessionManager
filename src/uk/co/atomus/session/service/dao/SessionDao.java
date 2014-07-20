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

import java.util.List;

import org.soyatec.windowsazure.error.StorageException;
import org.soyatec.windowsazure.table.AbstractTableServiceEntity;
import org.soyatec.windowsazure.table.ITableServiceEntity;

import uk.co.atomus.session.TomcatSessionStorageEntity;

public interface SessionDao {

	void removeAll(String partitionKey);

	int countEntities(String partitionKey, String rowKey);

	ITableServiceEntity retrieveEntity(String partitionKey, String rowKey);

	List<ITableServiceEntity> retrieveEntitiesByKey(String partitionKey, String rowKey);

	List<ITableServiceEntity> queryEntitiesByKeys(String partitionKey, String rowKey);

	void insertStorageEntity(TomcatSessionStorageEntity storageEntity);

	void updateStorageEntity(TomcatSessionStorageEntity storageEntity);

	String getAccountName();

	void setAccountName(String accountName);

	String getAccountKey();

	String getTableName();

	void setTableName(String tableName);

	void setAccountKey(String accountKey);

	void remove(String partitionKey, String rowKey) throws StorageException;

	void removeExpired(String partitionKey) throws StorageException;

	void setRetryPolicyRetries(int retryPolicyRetries);

	void setRetryPolicyInterval(int retryPolicyInterval);

}

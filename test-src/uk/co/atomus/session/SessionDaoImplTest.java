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

package uk.co.atomus.session;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.soyatec.windowsazure.table.ITableServiceEntity;

import uk.co.atomus.session.service.dao.SessionDaoImpl;

/**
 * @author Simon Dingle and Chris Derham
 *
 */
public class SessionDaoImplTest {
	private SessionDaoImpl sessionDao;
	private String partitionKey;
	private TomcatSessionStorageEntity sessionStorageEntity;
	private Properties storageProperties;

	@Before
	public void setup() throws IOException {
		loadStorageProperties();

		sessionDao = new SessionDaoImpl();
		sessionDao.setAccountKey(storageProperties.getProperty("accountKey", "MY_ACCOUNT_KEY"));
		sessionDao.setAccountName(storageProperties.getProperty("accountName", "MY_ACCOUNT_NAME"));
		sessionDao.setTableName(storageProperties.getProperty("sessionTableName", "MY_TABLE_NAME"));

		partitionKey = storageProperties.getProperty("partitionKey", "MY_PARTITION_KEY");
		sessionStorageEntity = new TomcatSessionStorageEntity(partitionKey, "123");
		sessionStorageEntity.setValidSession(true);
		byte[] data = {1, 0, 0};
		sessionStorageEntity.setData(data);
	}

	private void loadStorageProperties() {
		storageProperties = new Properties();
		FileInputStream in;
		try {
			in = new FileInputStream("test-src/resources/test.properties");
			storageProperties.load(in);
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testInsert() throws Exception {
		sessionDao.insertStorageEntity(sessionStorageEntity);
	}

	@Test
	public void testUpdate() throws Exception {
		sessionDao.updateStorageEntity(sessionStorageEntity);
	}

	@Test
	public void testRetrieveEntitiesByKey() throws Exception {
		List<ITableServiceEntity> entities = sessionDao.retrieveEntitiesByKey(partitionKey,
				sessionStorageEntity.getRowKey());
		Assert.assertNotNull(entities);
		assertEquals(entities.size(), 1);
		Assert.assertTrue(entities.get(0) instanceof TomcatSessionStorageEntity);
		sessionStorageEntity = (TomcatSessionStorageEntity) entities.get(0);
	}

	@Test
	public void testUpdateAfterRetrieval() throws Exception {
		sessionStorageEntity.setValidSession(false);
		sessionDao.updateStorageEntity(sessionStorageEntity);
	}

	@Test
	public void testGetSize() throws Exception {
		int result = sessionDao.countEntities(partitionKey, null);
		assertEquals(result, 1);
	}

	@Test
	public void testRemove() throws Exception {
		sessionDao.remove(partitionKey, sessionStorageEntity.getRowKey());
	}

	@Test
	public void testRemoveAll() throws Exception {
		sessionDao.removeAll(partitionKey);
	}

	@Test
	public void testRemoveExpired() throws Exception {
		sessionDao.removeExpired(partitionKey);
	}
}

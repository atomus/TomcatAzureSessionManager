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

package uk.co.atomus.session.service;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.catalina.Session;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.soyatec.windowsazure.error.StorageException;
import org.soyatec.windowsazure.table.AbstractTableServiceEntity;
import org.soyatec.windowsazure.table.ITableServiceEntity;

import uk.co.atomus.session.TomcatSessionStorageEntity;
import uk.co.atomus.session.service.dao.SessionDao;
import uk.co.atomus.session.service.dao.SessionDaoImpl;
import uk.co.atomus.session.util.SessionMapper;
import uk.co.atomus.session.util.SessionMapperImpl;

/**
 * @author Simon Dingle and Chris Derham
 *
 */
public class SessionServiceImpl implements SessionService {
	private final static Log log = LogFactory.getLog(SessionServiceImpl.class);
	private SessionDao sessionDao;
	private SessionMapper sessionMapper;
	private String partitionKey = "partitionKey";

	public SessionServiceImpl() {
		this(new SessionDaoImpl(), new SessionMapperImpl());
	}

	public SessionServiceImpl(SessionDao sessionDao, SessionMapper sessionMapper) {
		this.sessionDao = sessionDao;
		this.sessionMapper = sessionMapper;
	}

	@Override
	public void setAccountKey(String accountKey) {
		sessionDao.setAccountKey(accountKey);
	}

	@Override
	public void setAccountName(String accountName) {
		sessionDao.setAccountName(accountName);
	}

	@Override
	public void setRetryPolicyRetries(int retryPolicyRetries) {
		sessionDao.setRetryPolicyRetries(retryPolicyRetries);
	}

	@Override
	public void setRetryPolicyInterval(int retryPolicyInterval) {
		sessionDao.setRetryPolicyInterval(retryPolicyInterval);
	}

	@Override
	public void setTableName(String tableName) {
		sessionDao.setTableName(tableName);
	}

	@Override
	public void setPartitionKey(String partitionKey) {
		this.partitionKey = partitionKey;
	}

	protected String getPartitionKey() {
		return partitionKey;
	}

	private TomcatSessionStorageEntity findInStorage(String id) {
		try {
			ITableServiceEntity storageEntity = sessionDao.retrieveEntity(partitionKey, id);
			return storageEntity == null ? null : (TomcatSessionStorageEntity) storageEntity;
		} catch (StorageException e) {
			log.error("Error loading from storage", e);
			return null;
		}
	}

	@Override
	public void saveSessionWithData(Session session) {
		boolean withData = true;
		saveSession(session, withData);
	}

	@Override
	public void saveSessionHeaders(Session session) {
		boolean withData = false;
		saveSession(session, withData);
	}

	private void saveSession(Session session, boolean withData) {
		log.debug("synch block on sessionId:" + session.getIdInternal() + " withData:" + withData);
		synchronized (session.getIdInternal()) {
			String id = session.getIdInternal();
			try {
				log.debug("synch block inside saveSession sessionId:" + id + " withData:" + withData);
				TomcatSessionStorageEntity storageEntity = findInStorage(id);
				if (storageEntity == null) {
					try {
						createNewStorageEntity(session);
					} catch (StorageException e) {
						log.info("Found session created concurrently - updating " + id);
						storageEntity = findInStorage(id);
					}
				}
				if (storageEntity != null) {
					if (withData) {
						sessionMapper.mapToStorageEntityWithData(session, storageEntity);
					} else {
						sessionMapper.mapToStorageEntityHeadersOnly(session, storageEntity);
					}
					sessionDao.updateStorageEntity(storageEntity);
				}
			} catch (Exception e) {
				log.error("Error ocurred saving session. withData: " + withData, e);
				throw new StorageException(e);
			}
		}
		log.debug("synch block off sessionId:" + session.getIdInternal() + " withData:" + withData);
	}

	private void createNewStorageEntity(Session session) throws IOException {
		// do nothing, no point creating a new storage entity for a session that is invalid
		if (session.isValid()) {
			TomcatSessionStorageEntity storageEntity = new TomcatSessionStorageEntity(partitionKey, session.getIdInternal());
			storageEntity = sessionMapper.mapToStorageEntityWithData(session, storageEntity);
			sessionDao.insertStorageEntity(storageEntity);
		}
	}

	@Override
	public void deleteSession(Session session) {
		String id = session.getIdInternal();
		log.debug("deleteSession " + id);
		sessionDao.remove(partitionKey, id);
	}

	@Override
	public ObjectInputStream getSessionAsStream(String id, ClassLoader classLoader) throws IOException {
		TomcatSessionStorageEntity sessionStorageEntity = findInStorage(id);
		if (sessionStorageEntity == null) {
			return null;
		}
		ObjectInputStream ois = sessionMapper.toObjectInputStream(sessionStorageEntity, classLoader);
		return ois;
	}

	@Override
	public ObjectInputStream getSessionAsStream(long thisAccessedTime, String id, ClassLoader classLoader)
			throws IOException {
		log.debug("getSessionAsStream thisAccessedTime " + thisAccessedTime);
		TomcatSessionStorageEntity sessionStorageEntity = findInStorage(id);
		if (sessionStorageEntity == null || thisAccessedTime >= sessionStorageEntity.getLastAccessedTime()) {
			log.debug(sessionStorageEntity == null ? "session in storage not found" : "found not newer session "
					+ sessionStorageEntity.getLastAccessedTime());
			return null;
		}
		return sessionMapper.toObjectInputStream(sessionStorageEntity, classLoader);
	}

	@Override
	public void deleteExpiredSessions() {
		sessionDao.removeExpired(partitionKey);
	}

	@Override
	public Long getLastAccessed(String id) {
		TomcatSessionStorageEntity tomcatSession = findInStorage(id);
		return tomcatSession == null ? null : tomcatSession.getLastAccessedTime();
	}
}

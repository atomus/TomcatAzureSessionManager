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
package uk.co.atomus.session.manager;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.catalina.Container;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.session.StandardSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.soyatec.windowsazure.error.StorageServerException;

import uk.co.atomus.session.AtomusSession;
import uk.co.atomus.session.SessionStorageFacade;
import uk.co.atomus.session.service.SessionService;
import uk.co.atomus.session.service.SessionServiceImpl;
import uk.co.atomus.session.util.SessionDeserializer;
import uk.co.atomus.session.util.SessionDeserializerImpl;

/**
 * Extends StandardManager and does the right thing with respect to reading/writing sessions to storage
 *
 * @author Simon Dingle and Chris Derham
 *
 */
public class AtomusManager extends StandardManager implements SessionStorageFacade {
	private final static Log log = LogFactory.getLog(AtomusManager.class);
	private SessionService sessionService;
	private SessionDeserializer deserializer;

	public AtomusManager() {
		this(new SessionServiceImpl());
	}

	protected AtomusManager(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	protected AtomusManager(SessionService sessionService, SessionDeserializer deserializer) {
		this(sessionService);
		this.deserializer = deserializer;
	}

	public void setAccountName(String accountName) {
		sessionService.setAccountName(accountName);
	}

	public void setAccountKey(String accountKey) {
		sessionService.setAccountKey(accountKey);
	}

	public void setPartitionKey(String partitionKey) {
		sessionService.setPartitionKey(partitionKey);
	}

	public void setTableName(String tableName) {
		sessionService.setTableName(tableName);
	}

	@Override
	protected StandardSession getNewSession() {
		log.info("getNewSession");
		AtomusSession session = new AtomusSession(this, this);
		return session;
	}

	@Override
	public void remove(Session session) {
		log.info("remove session " + session.getIdInternal());
		super.remove(session);
	}

	@Override
	public Session findSession(String id) throws IOException {
		log.debug("findSession " + id);
		Session session = super.findSession(id);
		if (session == null || !session.isValid()) {
			log.info("no valid active session found " + id);
			session = loadPersistedSession(id);
		} else {
			log.info("active session found " + id);
			session = getMostRecentValidSession(session);
		}
		return session;
	}

	private Session loadPersistedSession(String id) {
		try {
			ObjectInputStream ois = sessionService.getSessionAsStream(id, getClassLoader(super.getContainer()));
			if (ois == null) {
				log.debug("session not found in storage: " + id);
				return null;
			}
			log.debug("returning persisted session from storage " + id);
			return toStandardSession(ois);
		} catch (IOException e) {
			log.error("IOException loading from storage", e);
			return null;
		}
	}

	private Session getMostRecentValidSession(Session activeSession) {
		try {
			ObjectInputStream ois = sessionService.getSessionAsStream(activeSession.getLastAccessedTime(),
					activeSession.getIdInternal(), getClassLoader(super.getContainer()));
			if (ois == null) {
				// there is no more recent session in storage so return the
				// active session
				log.debug("returning the active session " + activeSession.getIdInternal());
				return activeSession;
			}
			log.debug("returning persisted session from storage as is newer than active "
					+ activeSession.getIdInternal());
			Session session = toStandardSession(ois);
			return session;
		} catch (IOException e) {
			log.error("IOException loading from storage", e);
			return null;
		}
	}

	private StandardSession toStandardSession(ObjectInputStream ois) {
		StandardSession session = new AtomusSession(this, this);
		session = getSessionDeserializer().deserialize(session, ois);
		if (session == null || !session.isValid()) {
			return null;
		} else {
			super.add(session);
			return session;
		}
	}

	private ClassLoader getClassLoader(Container container) {
		Loader loader = container == null ? null : container.getLoader();
		if (loader == null) {
			return null;
		}
		return loader.getClassLoader();
	}

	private SessionDeserializer getSessionDeserializer() {
		if (deserializer == null) {
			deserializer = new SessionDeserializerImpl();
		}
		return deserializer;
	}

	@Override
	public void saveWithData(Session session) {
		log.debug("saveWithData");
		sessionService.saveSessionWithData(session);
	}

	@Override
	public void saveHeaders(Session session) {
		log.debug("saveHeaders");
		sessionService.saveSessionHeaders(session);
	}

	@Override
	public void deleteSession(Session session) {
		log.info("deleteSession");
		sessionService.deleteSession(session);
	}

	@Override
	public void processExpires() {
		log.debug("processExpires");
		super.processExpires();
		deleteExpiredSessions();
	}

	private void deleteExpiredSessions() {
		try {
			sessionService.deleteExpiredSessions();
		} catch (StorageServerException e) {
			log.warn("Error deleting expired sessions", e);
		} 
	}

	@Override
	public boolean isStorageStale(Session session) {
		log.debug("isStorageStale " + session.getIdInternal());
		Long lastAccessed = sessionService.getLastAccessed(session.getIdInternal());
		boolean stale = true;
		if (lastAccessed != null) {
			stale = session.getLastAccessedTime() > lastAccessed;
		}
		return stale;
	}

}

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

import java.io.ObjectInputStream;
import java.sql.Timestamp;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import uk.co.atomus.session.service.SessionServiceImpl;
import uk.co.atomus.session.service.dao.SessionDao;
import uk.co.atomus.session.util.SessionMapper;

/**
 * @author Simon Dingle and Chris Derham
 *
 */
public class SessionServiceImplTest {
	private static final long ACTIVE_SESSION_THIS_ACCESSED_TIME = 0;
	private static final String PARTITION_KEY = "PK";
	private static final String ROW_KEY = "RK";
	protected static final String SESSION_ID = ROW_KEY;
	private IMocksControl mocksControl;
	private AtomusSession session;
	private SessionDao sessionDao;
	private SessionServiceImpl sessionService;
	private SessionMapper sessionMapper;

	@SuppressWarnings("serial")
	@Before
	public void setup() {
		mocksControl = EasyMock.createStrictControl();
		sessionDao = mocksControl.createMock(SessionDao.class);
		sessionMapper = mocksControl.createMock(SessionMapper.class);
		sessionService = new SessionServiceImpl(sessionDao, sessionMapper);
		sessionService.setPartitionKey(PARTITION_KEY);
		session = new AtomusSession(null, null) {

			@Override
			public String getIdInternal() {
				return SESSION_ID;
			}

		};
		session.setValid(true);
	}

	private void expectDaoRetrieveEntity(TomcatSessionStorageEntity tableStorageEntity) {
		EasyMock.expect(sessionDao.retrieveEntity(PARTITION_KEY, session.getIdInternal()))
				.andReturn(tableStorageEntity);
	}

	@Test
	public void testDeleteSession() throws Exception {
		sessionDao.remove(EasyMock.eq(PARTITION_KEY), EasyMock.eq(session.getIdInternal()));
		mocksControl.replay();
		sessionService.deleteSession(session);
		mocksControl.verify();
	}

	@Test
	public void testDeleteExpiredSessions() throws Exception {
		sessionDao.removeExpired(PARTITION_KEY);
		mocksControl.replay();
		sessionService.deleteExpiredSessions();
		mocksControl.verify();
	}

	@Test
	public void testGetSessionAsStreamIfNewerNotFound() throws Exception {
		expectDaoRetrieveEntity(null);
		mocksControl.replay();
		ObjectInputStream ois = sessionService.getSessionAsStream(ACTIVE_SESSION_THIS_ACCESSED_TIME,
				session.getIdInternal(), null);
		Assert.assertNull(ois);
		mocksControl.verify();
	}

	@Test
	public void testGetSessionAsStreamIfNewerNotNewer() throws Exception {
		TomcatSessionStorageEntity tableStorageEntity = new TomcatSessionStorageEntity(PARTITION_KEY, ROW_KEY);
		tableStorageEntity.setTimestamp(new Timestamp(ACTIVE_SESSION_THIS_ACCESSED_TIME));
		expectDaoRetrieveEntity(tableStorageEntity);
		mocksControl.replay();
		ObjectInputStream ois = sessionService.getSessionAsStream(ACTIVE_SESSION_THIS_ACCESSED_TIME,
				session.getIdInternal(), null);
		Assert.assertNull(ois);
		mocksControl.verify();
	}

	@Test
	public void testGetSessionAsStreamNotFound() throws Exception {
		expectDaoRetrieveEntity(null);
		mocksControl.replay();
		ObjectInputStream ois = sessionService.getSessionAsStream(session.getIdInternal(), null);
		Assert.assertNull(ois);
		mocksControl.verify();
	}

	@Test
	public void testSaveNewSession() throws Exception {
		EasyMock.expect(sessionDao.retrieveEntity(PARTITION_KEY, ROW_KEY)).andReturn(null);
		EasyMock.expect(
				sessionMapper.mapToStorageEntityWithData(EasyMock.eq(session),
						EasyMock.isA(TomcatSessionStorageEntity.class))).andAnswer(
				new CurrentArgsAnswer<TomcatSessionStorageEntity>());
		sessionDao.insertStorageEntity(EasyMock.isA(TomcatSessionStorageEntity.class));
		mocksControl.replay();
		sessionService.saveSessionWithData(session);
		mocksControl.verify();
	}

	@Test
	public void testUpdateSession() throws Exception {
		final TomcatSessionStorageEntity storageSession = new TomcatSessionStorageEntity(PARTITION_KEY, ROW_KEY);
		EasyMock.expect(sessionDao.retrieveEntity(PARTITION_KEY, ROW_KEY)).andReturn(storageSession);
		EasyMock.expect(sessionMapper.mapToStorageEntityWithData(EasyMock.eq(session), EasyMock.eq(storageSession)))
				.andReturn(storageSession);
		sessionDao.updateStorageEntity(EasyMock.isA(TomcatSessionStorageEntity.class));
		mocksControl.replay();
		sessionService.saveSessionWithData(session);
		mocksControl.verify();
	}

	private class CurrentArgsAnswer<T> implements IAnswer<T> {
		@SuppressWarnings("unchecked")
		@Override
		public T answer() throws Throwable {
			return (T) EasyMock.getCurrentArguments()[1];
		}
	}

}

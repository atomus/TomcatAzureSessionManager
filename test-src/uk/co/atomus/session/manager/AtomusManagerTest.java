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

import junit.framework.Assert;

import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import uk.co.atomus.session.service.SessionService;
import uk.co.atomus.session.util.SessionDeserializer;

/**
 * @author Simon Dingle and Chris Derham
 *
 */
public class AtomusManagerTest {
	private static class MockObjectInputStream extends ObjectInputStream {
		protected MockObjectInputStream() throws IOException, SecurityException {
			super();
		}
	}

	@SuppressWarnings("serial")
	private class MockSession extends StandardSession{
		private String sessionId = SESSION_ID;
		private boolean valid;

		MockSession() {
			this(true);
		}

		MockSession(boolean valid) {
			super(atomusManager);
			this.valid = valid;
		}

		@Override
		public String getIdInternal() {
			return sessionId;
		}

		@Override
		public boolean isValid() {
			return valid;
		}

		@Override
		public void setId(String sessionId) {
			this.sessionId = sessionId;
		}

		@Override
		public long getLastAccessedTime() {
			return LAST_ACCESSED_TIME;
		}
	}

	private final static String SESSION_ID = "E28D7A795B0D7B8713DF2C2B54367AD9";
	private static final long LAST_ACCESSED_TIME = 1302793791890l;
	private AtomusManager atomusManager;
	private IMocksControl mocksControl;
	private MockSession session;

	private SessionDeserializer sessionDeserializer;

	private SessionService sessionService;

	@Before
	public void setup() {
		mocksControl = EasyMock.createStrictControl();
		sessionService = mocksControl.createMock(SessionService.class);
		sessionDeserializer = mocksControl.createMock(SessionDeserializer.class);
		atomusManager = new AtomusManager(sessionService, sessionDeserializer);
		session = new MockSession();
	}

	@Test
	public void testFindSessionActiveSessionIsNewer() throws Exception {
		atomusManager.add(session);
		EasyMock.expect(sessionService.getSessionAsStream(LAST_ACCESSED_TIME, SESSION_ID, null)).andReturn(null);
		mocksControl.replay();
		Session result = atomusManager.findSession(SESSION_ID);
		mocksControl.verify();
		Assert.assertEquals(session, result);
	}

	@Test
	public void testFindSessionActiveSessionInvalidLoadsFromStorage() throws Exception {
		Session invalidSession = new MockSession(false);
		atomusManager.add(invalidSession);
		expectLoadFromStorage();
		mocksControl.replay();
		Session result = atomusManager.findSession(SESSION_ID);
		mocksControl.verify();
		Assert.assertEquals(session, result);
	}

	@Test
	public void testFindSessionNoActiveSessionLoadedFromStorage() throws Exception {
		expectLoadFromStorage();
		mocksControl.replay();
		Session result = atomusManager.findSession(SESSION_ID);
		mocksControl.verify();
		Assert.assertNotNull(result);
	}

	private void expectLoadFromStorage() throws IOException {
		ObjectInputStream ois = new MockObjectInputStream();
		EasyMock.expect(sessionService.getSessionAsStream(SESSION_ID, null)).andReturn(ois);
		EasyMock.expect(sessionDeserializer.deserialize(EasyMock.isA(StandardSession.class), EasyMock.eq(ois)))
				.andReturn(session);
	}

	 @Test
	 public void testFindSessionNoActiveSessionErrorLoadingFromStorage() throws Exception {
		 EasyMock.expect(sessionService.getSessionAsStream(SESSION_ID, null)).andThrow(new IOException());
		 mocksControl.replay();
		 Session result = atomusManager.findSession(SESSION_ID);
		 mocksControl.verify();
		 Assert.assertNull(result);
	 }

	@Test
	public void testFindSessionNotFound() throws Exception {
		EasyMock.expect(sessionService.getSessionAsStream(SESSION_ID, null)).andReturn(null);
		mocksControl.replay();
		Session result = atomusManager.findSession(SESSION_ID);
		mocksControl.verify();
		Assert.assertNull(result);
	}

	@Test
	public void testFindSessionPersistedIsNewer() throws Exception {
		atomusManager.add(session);
		StandardSession persistedSession = new MockSession();
		ObjectInputStream ois = new MockObjectInputStream();
		EasyMock.expect(sessionService.getSessionAsStream(LAST_ACCESSED_TIME, SESSION_ID, null)).andReturn(ois);
		EasyMock.expect(sessionDeserializer.deserialize(EasyMock.isA(StandardSession.class), EasyMock.eq(ois)))
				.andReturn(persistedSession);
		mocksControl.replay();
		Session result = atomusManager.findSession(SESSION_ID);
		mocksControl.verify();
		Assert.assertNotNull(result);
		Assert.assertEquals(result, persistedSession);
	}

	@Test
	public void testGetNewSession() throws Exception {
		StandardSession result = atomusManager.getNewSession();
		Assert.assertNotNull(result);
		Assert.assertEquals(result.getManager(), atomusManager);
	}

	@Test
	public void testIsMoreRecentThanStorage_False() throws Exception {
		EasyMock.expect(sessionService.getLastAccessed(SESSION_ID)).andReturn(LAST_ACCESSED_TIME + 1);
		mocksControl.replay();
		boolean result = atomusManager.isStorageStale(session);
		mocksControl.verify();
		Assert.assertFalse(result);
	}

	@Test
	public void testIsMoreRecentThanStorage_NotInStorage() throws Exception {
		EasyMock.expect(sessionService.getLastAccessed(SESSION_ID)).andReturn(null);
		mocksControl.replay();
		boolean result = atomusManager.isStorageStale(session);
		mocksControl.verify();
		Assert.assertTrue(result);
	}

	@Test
	public void testIsMoreRecentThanStorage_True() throws Exception {
		EasyMock.expect(sessionService.getLastAccessed(SESSION_ID)).andReturn(LAST_ACCESSED_TIME - 1);
		mocksControl.replay();
		boolean result = atomusManager.isStorageStale(session);
		mocksControl.verify();
		Assert.assertTrue(result);
	}

	@Test
	public void testOnDeleteRequest() throws Exception {
		sessionService.deleteSession(session);
		mocksControl.replay();
		atomusManager.deleteSession(session);
		mocksControl.verify();
	}

	@Test
	public void testOnSaveRequest() throws Exception {
		sessionService.saveSessionWithData(session);
		mocksControl.replay();
		atomusManager.saveWithData(session);
		mocksControl.verify();
	}

	@Test
	public void testPartialSaveRequest() throws Exception {
		sessionService.saveSessionHeaders(session);
		mocksControl.replay();
		atomusManager.saveHeaders(session);
		mocksControl.verify();
	}

	@Test
	public void testProcessExpires() throws Exception {
		sessionService.deleteExpiredSessions();
		mocksControl.replay();
		atomusManager.processExpires();
		mocksControl.verify();
	}

	@Test
	public void testRemove() throws Exception {
		sessionService.deleteSession(session);
		mocksControl.replay();
		atomusManager.remove(session);
		mocksControl.verify();
	}

	@Test
	public void testSetAccountKey() throws Exception {
		final String key = "key";
		sessionService.setAccountKey(key);
		mocksControl.replay();
		atomusManager.setAccountKey(key);
		mocksControl.verify();
	}

	@Test
	public void testSetAccountName() throws Exception {
		final String name = "name";
		sessionService.setAccountName(name);
		mocksControl.replay();
		atomusManager.setAccountName(name);
		mocksControl.verify();
	}

	@Test
	public void testSetTableName() throws Exception {
		final String name = "name";
		sessionService.setTableName(name);
		mocksControl.replay();
		atomusManager.setTableName(name);
		mocksControl.verify();
	}
}

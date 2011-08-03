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

import junit.framework.Assert;

import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.junit.Before;
import org.junit.Test;

import uk.co.atomus.session.TomcatSessionStorageEntity;
import uk.co.atomus.session.util.SessionMapperImpl;

/**
 * @author Simon Dingle and Chris Derham
 *
 */
public class SessionMapperImplTest {
	private SessionMapperImpl sessionMapper;
	private MockSession session;
	private final static String PARTITION_KEY = "partKey";
	private static final int MAX_INACTIVE_INTERVAL = 10;
	private static final boolean VALID_TRUE = Boolean.TRUE;
	private final static String SESSION_ID = "E28D7A795B0D7B8713DF2C2B54367AD9";
	private final static long THIS_ACCESSED_TIME = 1302793791891l;
	private static final String ROW_KEY = "rowKey";
	private final static byte[] SESSION_DATA = new byte[0];

	@Before
	public void setup() {
		sessionMapper = new SessionMapperImpl() {
			@Override
			protected byte[] toByteArray(Session session) throws IOException {
				Assert.assertNotNull(session);
				return SESSION_DATA;
			}
		};
		session = new MockSession();
		session.setMaxInactiveInterval(MAX_INACTIVE_INTERVAL);
		session.setValid(VALID_TRUE);
	}

	@Test
	public void testMapToStorageEntityHeadersOnly() throws Exception {
		TomcatSessionStorageEntity storageEntity = new TomcatSessionStorageEntity(PARTITION_KEY, ROW_KEY);
		TomcatSessionStorageEntity result = sessionMapper.mapToStorageEntityHeadersOnly(session, storageEntity);
		Assert.assertEquals(result, storageEntity);
		assertFieldsEqual(result, storageEntity);
	}

	@Test
	public void testMapToStorageEntityHeadersOnlyWithData() throws Exception {
		TomcatSessionStorageEntity storageEntity = new TomcatSessionStorageEntity(PARTITION_KEY, ROW_KEY);
		TomcatSessionStorageEntity result = sessionMapper.mapToStorageEntityWithData(session, storageEntity);
		Assert.assertEquals(result, storageEntity);
		assertFieldsEqual(result, storageEntity);
		Assert.assertEquals(result.getData(), SESSION_DATA);
	}

	private void assertFieldsEqual(TomcatSessionStorageEntity actual, TomcatSessionStorageEntity expected) {
		Assert.assertEquals(actual.getLastAccessedTime(), expected.getLastAccessedTime());
		Assert.assertEquals(actual.getLastAccessedTime(), expected.getLastAccessedTime());
		Assert.assertEquals(actual.getMaxInactiveInterval(), expected.getMaxInactiveInterval());
		Assert.assertEquals(actual.getValidSession(), actual.getValidSession());
	}

	@SuppressWarnings("serial")
	private static class MockSession extends StandardSession {
		private String sessionId = SESSION_ID;

		MockSession() {
			super(new AtomusManager(null, null));
		}

		@Override
		public String getIdInternal() {
			return sessionId;
		}

		//@Override commenting out Override so wil work with diff tomcat versions
		@Override
		public long getThisAccessedTime() {
			return THIS_ACCESSED_TIME;
		}

		@Override
		public long getLastAccessedTime() {
			return THIS_ACCESSED_TIME;
		}


		@Override
		public boolean isValid() {
			return VALID_TRUE;
		}

		@Override
		public void setId(String sessionId) {
			this.sessionId = sessionId;
		}
	}
}
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

import org.apache.catalina.Manager;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Dingle and Chris Derham
 *
 */
public class AtomusSessionTest {
	private AtomusSession atomusSession;
	private Manager manager;
	private SessionStorageFacade storageFacade;
	private IMocksControl mocksControl;
	private final static String ATTRIBUTE_NAME = "foo";
	private final static String ATTRIBUTE_VALUE = "value";
	private final static boolean NOTIFY_FALSE = false;
	private int superExpireCalls;
	private int superSetIdCalls;

	@SuppressWarnings("serial")
	@Before
	public void setup() {
		mocksControl = EasyMock.createStrictControl();
		manager = mocksControl.createMock(Manager.class);
		storageFacade = mocksControl.createMock(SessionStorageFacade.class);
		atomusSession = new AtomusSession(manager, storageFacade) {


			@Override
			protected void doSuperExpire() {
				superExpireCalls++;
			}

			@Override
			protected void doSuperSetId(String id) {
				superSetIdCalls++;
			}

		};
		atomusSession.setValid(true);
		atomusSession.setMaxInactiveInterval(-1);
		superExpireCalls = 0;
		superSetIdCalls = 0;
	}

	@Test
	public void testAccess() {
		mocksControl.replay();
		atomusSession.access();
		mocksControl.verify();
	}

	@Test
	public void testEndAccess() {
		storageFacade.saveHeaders(atomusSession);
		mocksControl.replay();
		atomusSession.endAccess();
		mocksControl.verify();
	}

	@Test
	public void testExpire() {
		EasyMock.expect(storageFacade.isStorageStale(atomusSession)).andReturn(false);
		mocksControl.replay();
		atomusSession.expire();
		mocksControl.verify();
		Assert.assertEquals(1, superExpireCalls);
	}

	@Test
	public void testPassivate() {
		storageFacade.saveWithData(atomusSession);
		mocksControl.replay();
		atomusSession.passivate();
		mocksControl.verify();
	}

	@Test
	public void testRemoveAttributeInternal() throws Exception {
		storageFacade.saveWithData(atomusSession);
		mocksControl.replay();
		atomusSession.removeAttributeInternal(ATTRIBUTE_NAME, NOTIFY_FALSE);
		mocksControl.verify();
	}
//
//	@Test
//	public void testSetAttribute2Param() throws Exception {
//		//EasyMock.expect(manager.getContainer()).andReturn(container);
//		EasyMock.expect(manager.getDistributable()).andReturn(Boolean.FALSE);
//		storageFacade.saveWithData(atomusSession);
//		mocksControl.replay();
//		atomusSession.setAttribute(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
//		mocksControl.verify();
//	}

//	@Test
//	public void testSetAttribute3Param() throws Exception {
//		EasyMock.expect(manager.getDistributable()).andReturn(Boolean.FALSE);
//		storageFacade.saveWithData(atomusSession);
//		mocksControl.replay();
//		atomusSession.setAttribute(ATTRIBUTE_NAME, ATTRIBUTE_VALUE, NOTIFY_FALSE);
//		mocksControl.verify();
//	}

	@Test
	public void testSetId() throws Exception {
		storageFacade.saveWithData(atomusSession);
		mocksControl.replay();
		String sessionId = "session_id_";
		atomusSession.setId(sessionId);
		mocksControl.verify();
	}
}
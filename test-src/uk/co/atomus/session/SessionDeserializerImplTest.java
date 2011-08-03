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

import java.io.IOException;
import java.io.ObjectInputStream;

import junit.framework.Assert;

import org.apache.catalina.session.StandardSession;
import org.junit.Before;
import org.junit.Test;

import uk.co.atomus.session.util.SessionDeserializerImpl;

/**
 * @author Simon Dingle and Chris Derham
 *
 */
public class SessionDeserializerImplTest {
	private SessionDeserializerImpl deserializer;
	private StandardSession session;
	private ObjectInputStream ois;
	private int readObjectCount = 0;
	private boolean readIoException;
	private boolean readClassNotFoundException;

	@SuppressWarnings("serial")
	@Before
	public void setup() {
		deserializer = new SessionDeserializerImpl();
		session = new AtomusSession(null, null) {
			@Override
			public void readObjectData(ObjectInputStream stream) throws ClassNotFoundException, IOException {
				readObjectCount++;
				if (readClassNotFoundException) {
					throw new ClassNotFoundException();
				} else if (readIoException) {
					throw new IOException();
				}
			}
		};
		readObjectCount = 0;
		readClassNotFoundException = false;
		readIoException = false;
	}

	@Test
	public void testDeserializeHappyPath() {
		deserializer.deserialize(session, ois);
		Assert.assertEquals(1, readObjectCount);
	}

	@Test
	public void testDeserializeClassNotFoundError() {
		readClassNotFoundException = true;
		deserializer.deserialize(session, ois);
		Assert.assertEquals(1, readObjectCount);
	}

	@Test
	public void testDeserializeIoException() {
		readIoException = true;
		deserializer.deserialize(session, ois);
		Assert.assertEquals(1, readObjectCount);
	}
}

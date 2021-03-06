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

package uk.co.atomus.session.util;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.catalina.session.StandardSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Simon Dingle and Chris Derham
 *
 */
public class SessionDeserializerImpl implements SessionDeserializer {
	private final static Log log = LogFactory.getLog(SessionDeserializerImpl.class);

	@Override
	public StandardSession deserialize(StandardSession session, ObjectInputStream ois) {
		try {
			session.readObjectData(ois);
			return session;
		} catch (ClassNotFoundException e) {
			log.error("ClassNotFoundException reading session ObjectInputStream", e);
			return null;
		} catch (IOException e) {
			log.error("IOException reading session ObjectInputStream", e);
			return null;
		}
	}
}

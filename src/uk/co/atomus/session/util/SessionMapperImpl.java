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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.util.CustomObjectInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.co.atomus.session.TomcatSessionStorageEntity;

/**
 * Implements methods for mapping between Session and TomcatSessionStorageEntity
 *
 * @author Simon Dingle and Chris Derham
 *
 */
public class SessionMapperImpl implements SessionMapper {
	private final static Log log = LogFactory.getLog(SessionMapperImpl.class);

	@Override
	public TomcatSessionStorageEntity mapToStorageEntityHeadersOnly(Session session,
			TomcatSessionStorageEntity storageEntity) {
		storageEntity.setSessionId(session.getIdInternal());
		storageEntity.setValidSession(session.isValid());
		storageEntity.setMaxInactiveInterval(session.getMaxInactiveInterval());
		storageEntity.setLastAccessedTime(session.isValid() ? session.getLastAccessedTime() : new Date().getTime());
		return storageEntity;
	}

	@Override
	public TomcatSessionStorageEntity mapToStorageEntityWithData(Session session,
			TomcatSessionStorageEntity storageEntity) throws IOException {
		storageEntity = mapToStorageEntityHeadersOnly(session, storageEntity);
		byte[] sessionData = toByteArray(session);
		storageEntity.setData(sessionData);
		return storageEntity;
	}

	protected byte[] toByteArray(Session session) throws IOException {
		ByteArrayOutputStream bos = null;
		ObjectOutputStream oos = null;

		bos = new ByteArrayOutputStream();
		oos = new ObjectOutputStream(new BufferedOutputStream(bos));
		try {
			((StandardSession) session).writeObjectData(oos);
			oos.flush();
			byte[] byteArray = bos.toByteArray();
			log.debug("Session " + session.getIdInternal() + " converted to "
					+ (byteArray == null ? "0" : byteArray.length));
			return byteArray;
		} finally {
			close(bos);
			close(oos);
		}
	}

	private void close(OutputStream stream) {
		if (stream == null) {
			return;
		}
		try {
			stream.close();
		} catch (Exception e) {
			log.error("Error closing stream", e);
		}
	}

	@Override
	public ObjectInputStream toObjectInputStream(TomcatSessionStorageEntity storageEntity, ClassLoader classLoader)
			throws IOException {
		byte[] data = storageEntity.getData();
		BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));

		if (classLoader != null) {
			return new CustomObjectInputStream(bis, classLoader);
		} else {
			return new ObjectInputStream(bis);
		}
	}
}

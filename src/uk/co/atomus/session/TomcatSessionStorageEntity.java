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

import org.soyatec.windowsazure.table.AbstractTableServiceEntity;


/**
 * Represents the tomcat session entity to be persisted to Azure Table Storage
 *
 * @author Simon Dingle and Chris Derham
 */
public class TomcatSessionStorageEntity extends AbstractTableServiceEntity {
	private byte[] data;
	private String sessionId;
	private boolean validSession;
	private int maxInactiveInterval;
	private long lastAccessedTime;

	public TomcatSessionStorageEntity(String partitionKey, String rowKey) {
		super(partitionKey, rowKey);
	}

	public boolean getValidSession() {
		return validSession;
	}

	public void setValidSession(boolean validSession) {
		this.validSession = validSession;
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public long getLastAccessedTime() {
		return lastAccessedTime;
	}

	public void setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public boolean hasExpired() {
		if (maxInactiveInterval <=0) {
			return false;
		}
		return System.currentTimeMillis() - (maxInactiveInterval * 1000) > lastAccessedTime;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TomcatSessionStorageEntity [sessionId=").append(sessionId).append(", validSession=")
				.append(validSession).append(", maxInactiveInterval=").append(maxInactiveInterval)
				.append(", lastAccessedTime=").append(lastAccessedTime).append("]");
		return builder.toString();
	}
}

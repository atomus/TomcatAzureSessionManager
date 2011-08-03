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
import org.apache.catalina.session.StandardSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extends a StandardSession and persists the session when necessary via SessionStorageFacade
 *
 * @author Simon Dingle and Chris Derham
 *
 */
public class AtomusSession extends StandardSession {
	private final static Log log = LogFactory.getLog(AtomusSession.class);
	private static final long serialVersionUID = 1L;
	private transient SessionStorageFacade sessionStorage;

	public AtomusSession(Manager manager, SessionStorageFacade sessionStorage) {
		super(manager);
		this.sessionStorage = sessionStorage;
	}

	@Override
	protected void removeAttributeInternal(String name, boolean notify) {
		log.debug("removeAttributeInternal name: " + name + " notify: " + notify);
		super.removeAttributeInternal(name, notify);
		requestSave();
	}

	@Override
	public void setAttribute(String name, Object value) {
		log.debug("setAttribute name: " + name);
		super.setAttribute(name, value);
		requestSave();
	}

//	//@Override Tomcat v5.0.33 onwards
//	public void setAttribute(String name, Object value, boolean notify) {
//		log.debug("setAttribute name: " + name + " notify: " + notify);
//		super.setAttribute(name, value, notify);
//		requestSave();
//	}

	private void requestSave() {
		if (isValid() && !expiring) {
			sessionStorage.saveWithData(this);
		}
	}

	@Override
	public void invalidate() {
		log.debug("invalidate " + this.getIdInternal());
		super.invalidate();
		sessionStorage.saveWithData(this);
	}

	@Override
	public void passivate() {
		log.debug("passivate " + this.getIdInternal());
		requestSave();
		super.passivate();
	}

	@Override
	public void setId(String id) {
		doSuperSetId(id);
		if (id != null) {
			log.debug("setId :" + id);
			requestSave();
		}
	}

	@Override
	public boolean isValid() {
		boolean isValid = super.isValid();
		if (!isValid) {
			log.debug("isValid returned false for session id: + " + getIdInternal());
		}
		return isValid;
	}

	@Override
	public void activate() {
		log.debug("activate" + this.getIdInternal());
		super.activate();
	}

	@Override
	public void expire() {
		log.info("expire " + this.getIdInternal());
		boolean shouldPersist = sessionStorage.isStorageStale(this);
		doSuperExpire();
		if (shouldPersist) {
			log.debug("persisting expired session " + this.getIdInternal());
			sessionStorage.saveWithData(this);
		}
	}

	// allows testing without calling superclass
	protected void doSuperExpire() {
		super.expire();
	}

	protected void doSuperSetId(String id) {
		super.setId(id);
	}

	@Override
	public void access() {
		log.debug("access " + this.getIdInternal());
		super.access();
	}

	@Override
	public void endAccess() {
		log.debug("endAccess " + this.getIdInternal());
		super.endAccess();
		sessionStorage.saveHeaders(this);
	}

}

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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Dingle and Chris Derham
 *
 */
public class TomcatStorageEntityTest {

	private static final String PARTITION_KEY = "myPartitionKey";
	private static final String ROW_KEY = "myRowKey";
	private TomcatSessionStorageEntity subject;

	@Before
	public void setup (){
		subject = new TomcatSessionStorageEntity(PARTITION_KEY, ROW_KEY);
	}

	@Test
	public void testHasExpiredNeverExpires() {
		subject.setMaxInactiveInterval(0);
		subject.setLastAccessedTime(1000);
		Assert.assertFalse(subject.hasExpired());
	}

	@Test
	public void testHasExpiredTrue() {
		subject.setMaxInactiveInterval(2);
		subject.setLastAccessedTime(System.currentTimeMillis() - 2001);
		Assert.assertTrue(subject.hasExpired());
	}

	@Test
	public void testHasExpiredFalse(){
		subject.setMaxInactiveInterval(2);
		subject.setLastAccessedTime(System.currentTimeMillis() - 1900);
		Assert.assertFalse(subject.hasExpired());
	}



}

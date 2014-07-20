/*
 * Copyright (c) Atomus Ltd 2011.
 *
 * This software is copyrighted.  Under the copyright laws, this software
 * may not be copied, in whole or in part, without prior written consent
 * of Atomus Ltd. This software is provided under the terms of a
 * license between Atomus and the recipient, and its use is subject
 * to the terms of that license.
 */
package uk.co.atomus.session.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.session.StandardSession;
import org.soyatec.windowsazure.table.ITableServiceEntity;

import uk.co.atomus.session.AtomusSession;
import uk.co.atomus.session.TomcatSessionStorageEntity;
import uk.co.atomus.session.manager.AtomusManager;
import uk.co.atomus.session.service.dao.SessionDaoImpl;

/**
 *
 * @author "ChrisDerham"
 */
public class SessionDumper {
	public static void main(String[] args) throws Exception {
		if (args == null || args.length != 4) {
			System.out
					.println("You must supply three command line arguments - account name, account key, table name and lib folder. " +
							"You can supply multiple lib folders by separating with semi-colon");
			return;
		}
		String accountName = args[0];
		String accountKey = args[1];
		String tableName = args[2];
		String libFolder = args[3];

		URLClassLoader urlClassLoader = getClassLoader(libFolder);

		String partitionKey = null;
		String rowKey = null;

		SessionDaoImpl sessionDao = new SessionDaoImpl();
		sessionDao.setAccountName(accountName);
		sessionDao.setAccountKey(accountKey);
		sessionDao.setTableName(tableName);

		List<ITableServiceEntity> entities = sessionDao.queryEntitiesByKeys(partitionKey, rowKey);
		System.out.println("Found " + entities.size() + " entities");

		SessionMapper sessionMapper = new SessionMapperImpl();
		SessionDeserializer sessionDeserializer = new SessionDeserializerImpl();
		AtomusManager manager = new AtomusManager();
		manager.setContainer(new StandardWrapper());

		for (ITableServiceEntity tableStorageEntity : entities) {
			dumpTableStorageEntity(tableStorageEntity, sessionMapper, manager, sessionDeserializer, urlClassLoader);
		}
	}

	private static URLClassLoader getClassLoader(String libFoldersString) throws Exception {
		String[] libFolders = libFoldersString.split(";");
		List<URL> urls = new ArrayList<URL>();
		for (String thisLibFolder : libFolders) {
			File libFolderFile = new File(thisLibFolder);
			String[] jars = libFolderFile.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".jar");
				}
			});
			for (String jar : jars) {
				File thisJar = new File(thisLibFolder, jar);
				System.out.println("Adding " + thisJar + " to classpath");
				urls.add(thisJar.toURI().toURL());
			}
		}
		URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[0]),
				SessionDumper.class.getClassLoader());

		return urlClassLoader;
	}

	private static void dumpTableStorageEntity(ITableServiceEntity tableStorageEntity, SessionMapper sessionMapper,
			AtomusManager manager, SessionDeserializer sessionDeserializer, ClassLoader urlClassLoader)
			throws Exception {
		try {
			ObjectInputStream ois = sessionMapper.toObjectInputStream((TomcatSessionStorageEntity) tableStorageEntity,
					urlClassLoader);
			StandardSession session = new AtomusSession(manager, manager);
			session = sessionDeserializer.deserialize(session, ois);

			if (session == null) {
				System.out.println("Session could not be deserialized " + tableStorageEntity.getRowKey());
			} else {
				if (session.isValid()) {
					System.out.println("Session " + session.getId());
					Enumeration<String> enumeration = session.getAttributeNames();
					if (enumeration.hasMoreElements()) {
						while (enumeration.hasMoreElements()) {
							String name = enumeration.nextElement();
							System.out.println(name + "=" + session.getAttribute(name));
						}
					} else {
						System.out.println("No session attributes");
					}
				} else {
					System.out.println("Session invalid " + session.getId());
				}
			}
		} catch (Exception e) {
			System.out.println("Unable to deserialize " + tableStorageEntity.getRowKey());
			e.printStackTrace();
		}
	}
}

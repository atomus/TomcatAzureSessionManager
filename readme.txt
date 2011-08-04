Instructions for how to setup TomcatAzureSessionManager

Deployment

Tomcat 6, 7 
1 Copy the tomcat-azure-session-manager[version].jar from the dist folder to [CATALINA_HOME]/lib
2 Copy all of the jar files from lib/deploy folder to [CATALINA_HOME]/lib 
3 Configure the following attributes in the tomcat instance context.xml file, <Manager> element

Attribute           Description
className          	must be set to uk.co.atomus.session.manager.AtomusManager
accountKey        	The azure storage account key
accountName			The azure storage account name
tableName         	The name of the table storage table to use for sessions (will be created if does not exist)
partitionKey       	Corresponds to partitionKey in table storage

Your Manager tag should end up looking something like this

    <Manager className="uk.co.atomus.session.manager.AtomusManager" 
            accountKey="<accountKey>"
            accountName="<accountName>"
            tableName="tomcatSessions"
            partitionKey="<application name>"     
...

Sessions are shared across all instances which use these settings.

Tomcat 5
1 Copy the tomcat-azure-session-manager[version]tomcat-5.5.15.jar from the dist folder to [CATALINA_HOME]/server/lib
2 Copy commons-logging and log4j jar files from lib/deploy folder to [CATALINA_HOME]/common/lib 
3 Copy all of the other jar files from the deploy folder to [CATALINA_HOME]/server/lib
Configuration is as per step 3 in the section above


Building

The project can be packaged using the ant file build.xml
 
Tomcat 5.5.33, 6, 7
NB ensure that the 3 argument setAttribute method on line 52 of uk.co.atomus.session.AtomusSession is uncommented
Ensure that the versions of catalina.jar, servlet-api.jar and tomct-coyote.jar referenced by the project are the ones in lib/compile

Tomcat 5.5.15
NB ensure that the 3 argument setAttribute method on line 52 of uk.co.atomus.session.AtomusSession is commented out
Ensure that the versions of catalina.jar, servlet-api.jar and tomct-coyote.jar referenced by the project are the ones in lib/5.5.15





/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.integrationstudio.artifact.messagestore.ui.wizard;

import static org.wso2.integrationstudio.artifact.messagestore.Constants.FIELD_IMPORT_STORE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.apache.synapse.config.xml.MessageStoreSerializer;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.message.store.impl.memory.InMemoryStore;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.jaxen.JaxenException;
import org.wso2.integrationstudio.artifact.messagestore.Activator;
import org.wso2.integrationstudio.artifact.messagestore.model.MessageStoreModel;
import org.wso2.integrationstudio.artifact.messagestore.provider.JDBCConnectionInformationList.JDBCConnectionInformationType;
import org.wso2.integrationstudio.artifact.messagestore.provider.MessageStoreTypeList.MessageStoreType;
import org.wso2.integrationstudio.artifact.messagestore.util.MessageStoreImageUtils;
import org.wso2.integrationstudio.esb.core.ESBMavenConstants;
import org.wso2.integrationstudio.esb.core.exceptions.BuildArtifactCreationException;
import org.wso2.integrationstudio.esb.core.utils.SynapseConstants;
import org.wso2.integrationstudio.esb.core.utils.SynapseUtils;
import org.wso2.integrationstudio.esb.project.artifact.ESBArtifact;
import org.wso2.integrationstudio.esb.project.artifact.ESBProjectArtifact;
import org.wso2.integrationstudio.gmf.esb.ArtifactType;
import org.wso2.integrationstudio.logging.core.IIntegrationStudioLog;
import org.wso2.integrationstudio.logging.core.Logger;
import org.wso2.integrationstudio.maven.util.MavenUtils;
import org.wso2.integrationstudio.platform.ui.editor.Openable;
import org.wso2.integrationstudio.platform.ui.startup.ESBGraphicalEditor;
import org.wso2.integrationstudio.platform.ui.wizard.AbstractWSO2ProjectCreationWizard;
import org.wso2.integrationstudio.utils.file.FileUtils;

/**
 * WSO2 message-store creation wizard class
 */
public class MessageStoreCreationWizard extends AbstractWSO2ProjectCreationWizard {
	
	private static IIntegrationStudioLog log=Logger.getLog(Activator.PLUGIN_ID);
	
	private static final String PROJECT_WIZARD_WINDOW_TITLE = "New Message Store Artifact";
	private final MessageStoreModel messageStoreModel;
	private ESBProjectArtifact esbProjectArtifact;
	private IProject esbProject;
	private IFile artifactFile;
	private List<File> fileLst = new ArrayList<File>();

	private String version = "1.0.0";
	private static final String RABBITMQ_MS_FQN = "org.apache.synapse.message.store.impl.rabbitmq.RabbitMQStore";
	private static final String JDBC_MS_FQN = "org.apache.synapse.message.store.impl.jdbc.JDBCMessageStore";
	private static final String RESEQUENCE_MS_FQN = "org.apache.synapse.message.store.impl.resequencer.ResequenceMessageStore";

	private static final String STORE_RABBITMQ_VIRTUAL_HOST = "store.rabbitmq.virtual.host";
	private static final String STORE_RABBITMQ_PASSWORD = "store.rabbitmq.password";
	private static final String STORE_RABBITMQ_USERNAME = "store.rabbitmq.username";
	private static final String STORE_RABBITMQ_ROUTE_KEY = "store.rabbitmq.route.key";
	private static final String STORE_RABBITMQ_EXCHANGE_NAME = "store.rabbitmq.exchange.name";
	private static final String STORE_RABBITMQ_QUEUE_NAME = "store.rabbitmq.queue.name";
	private static final String STORE_RABBITMQ_HOST_PORT = "store.rabbitmq.host.port";
	private static final String STORE_RABBITMQ_HOST_NAME = "store.rabbitmq.host.name";
	private static final String STORE_RABBITMQ_PRODUCER_GUARANTEED_DELIVERY_ENABLE = "store.producer.guaranteed.delivery.enable";
	private static final String STORE_RABBITMQ_FAILOVER_MESSAGE_STORE_NAME = "store.failover.message.store.name";
	
	private static final String STORE_RABBITMQ_SSL_ENABLED = "rabbitmq.connection.ssl.enabled";
	private static final String STORE_RABBITMQ_SSL_KEYSTORE_LOCATION = "rabbitmq.connection.ssl.keystore.location";
	private static final String STORE_RABBITMQ_SSL_KEYSTORE_TYPE = "rabbitmq.connection.ssl.keystore.type";
	private static final String STORE_RABBITMQ_SSL_KEYSTORE_PASSWORD = "rabbitmq.connection.ssl.keystore.password";
	private static final String STORE_RABBITMQ_SSL_TRUSTSTORE_LOCATION = "rabbitmq.connection.ssl.truststore.location";
	private static final String STORE_RABBITMQ_SSL_TRUSTSTORE_TYPE = "rabbitmq.connection.ssl.truststore.type";
	private static final String STORE_RABBITMQ_SSL_TRUSTSTORE_PASSWORD = "rabbitmq.connection.ssl.truststore.password";
	private static final String STORE_RABBITMQ_SSL_VERSION = "rabbitmq.connection.ssl.version";
	
	private static final String STORE_JDBC_DS_NAME = "store.jdbc.dsName";
	private static final String STORE_JDBC_PASSWORD = "store.jdbc.password";
	private static final String STORE_JDBC_USERNAME = "store.jdbc.username";
	private static final String STORE_JDBC_CONNECTION_URL = "store.jdbc.connection.url";
	private static final String STORE_JDBC_DRIVER = "store.jdbc.driver";
	private static final String STORE_JDBC_TABLE = "store.jdbc.table";	
	private static final String STORE_JDBC_PRODUCER_GUARANTEED_DELIVERY_ENABLE = "store.producer.guaranteed.delivery.enable";
	private static final String STORE_JDBC_FAILOVER_MESSAGE_STORE_NAME = "store.failover.message.store.name";
	
	private static final String STORE_RESEQUENCE_DS_NAME = "store.jdbc.dsName";
	private static final String STORE_RESEQUENCE_PASSWORD = "store.jdbc.password";
	private static final String STORE_RESEQUENCE_USERNAME = "store.jdbc.username";
	private static final String STORE_RESEQUENCE_CONNECTION_URL = "store.jdbc.connection.url";
	private static final String STORE_RESEQUENCE_DRIVER = "store.jdbc.driver";
	private static final String STORE_RESEQUENCE_TABLE = "store.jdbc.table";
	private static final String STORE_RESEQUENCE_POLLING_COUNT = "store.resequence.timeout";
	private static final String STORE_RESEQUENCE_XPATH = "store.resequence.id.path";
	private static final String STORE_RESEQUENCE_PRODUCER_GUARANTEED_DELIVERY_ENABLE = "store.producer.guaranteed.delivery.enable";
	private static final String STORE_RESEQUENCE_FAILOVER_MESSAGE_STORE_NAME = "store.failover.message.store.name";

	public MessageStoreCreationWizard() {
		messageStoreModel = new MessageStoreModel();
		setModel(messageStoreModel);
		setWindowTitle(PROJECT_WIZARD_WINDOW_TITLE);
		setDefaultPageImageDescriptor(MessageStoreImageUtils.getInstance().getImageDescriptor("message-store.png"));
	}

	@Override
	public IResource getCreatedResource() {
		return null;
	}

	@Override
	public boolean performFinish() {
		try {
			boolean isNewArtifact =true;
			esbProject = messageStoreModel.getSaveLocation().getProject();
			IContainer location = esbProject.getFolder("src/main/synapse-config/message-stores");
			File pomfile = esbProject.getFile("pom.xml").getLocation().toFile();
			if (!pomfile.exists()) {
				createPOM(pomfile);
			}
            MavenProject mavenProject = MavenUtils.getMavenProject(pomfile);
            version = mavenProject.getVersion().replace("-SNAPSHOT", "");
            esbProjectArtifact = new ESBProjectArtifact();
			esbProjectArtifact.fromFile(esbProject.getFile("artifact.xml").getLocation().toFile());
            if (!esbProject.getFolder(SynapseConstants.BUILD_ARTIFACTS_FOLDER).exists()) {
                updatePom();
            }
			esbProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			String groupId = getMavenGroupId(pomfile) + ".message-store";
            if(getModel().getSelectedOption().equals(FIELD_IMPORT_STORE)){
            	IFile task = location.getFile(new Path(getModel().getImportFile().getName()));
				if(task.exists()){
					if(!MessageDialog.openQuestion(getShell(), "WARNING", "Do you like to override the existing project in workspace")){
						return false;	
					}
					isNewArtifact = false;
				} 	
				copyImportFile(location,isNewArtifact,groupId);
            } else {
                artifactFile = location.getFile(new Path(messageStoreModel.getStoreName() + ".xml"));
                File destFile = artifactFile.getLocation().toFile();
                FileUtils.createFile(destFile, getTemplateContent());
                fileLst.add(destFile);

                addESBArtifactDetails(location, messageStoreModel.getStoreName(), groupId, version,
                        messageStoreModel.getStoreName(), esbProjectArtifact);
            }
			esbProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

			for (File file : fileLst) {
				if (file.exists()) {
					openEditor(file);
				}
			}
		} catch (CoreException e) {
			log.error("CoreException has occurred", e);
		} catch (Exception e) {
			log.error("An unexpected error has occurred", e);
		}
		return true;
	}
	
	protected boolean isRequireProjectLocationSection() {
		return false;
	}

	protected boolean isRequiredWorkingSet() {
		return false;
	}
	
	private ESBArtifact createArtifact(String name,String groupId,String version,String path){
		ESBArtifact artifact=new ESBArtifact();
		artifact.setName(name);
		artifact.setVersion(version);
		artifact.setType(SynapseConstants.MESSAGE_STORE_CONFIG_TYPE);
		artifact.setServerRole("EnterpriseServiceBus");
		artifact.setGroupId(groupId);
		artifact.setFile(path);
		return artifact;
	}

    private void addESBArtifactDetails(IContainer location, String messageStoreName, String groupId, String version,
            String messageStoreFileName, ESBProjectArtifact esbProjectArtifact) throws Exception {

        String relativeLocation = FileUtils
                .getRelativePath(esbProject.getLocation().toFile(),
                        new File(location.getLocation().toFile(), messageStoreName + ".xml"))
                .replaceAll(Pattern.quote(File.separator), "/");
        esbProjectArtifact.addESBArtifact(createArtifact(messageStoreName, groupId, version, relativeLocation));
        esbProjectArtifact.toFile();
        createMessageStoreBuildArtifactPom(groupId, messageStoreName, version, messageStoreFileName, relativeLocation);
    }

    private void createMessageStoreBuildArtifactPom(String groupId, String artifactId, String version,
            String messageStoreFileName, String relativePathToRealArtifact) throws BuildArtifactCreationException {

        IContainer buildArtifactsLocation = esbProject.getFolder(SynapseConstants.BUILD_ARTIFACTS_FOLDER);
        try {
            SynapseUtils.createSynapseConfigBuildArtifactPom(groupId, artifactId, version,
                    SynapseConstants.MESSAGE_STORE_CONFIG_TYPE, messageStoreFileName,
                    SynapseConstants.MESSAGE_STORE_FOLDER, buildArtifactsLocation,
                    "../../../" + relativePathToRealArtifact);
        } catch (IOException | XmlPullParserException e) {
            throw new BuildArtifactCreationException("Error while creating the build artifacts for Message Store: "
                    + messageStoreFileName + " at " + buildArtifactsLocation.getFullPath());
        }
    }
	
	private String getTemplateContent(){
		Map<String,Object> parameters = new HashMap<String,Object>();
		// Fixing TOOLS-2026.
		//String className = "org.apache.synapse.message.store.InMemoryMessageStore";
		//String className = "org.apache.synapse.message.store.impl.memory.InMemoryStore";
		String className = null;
		MessageStore store = new InMemoryStore();
		store.setName(messageStoreModel.getStoreName());
		String lineSeparator = System.getProperty("line.separator","\n");
		
		if(messageStoreModel.getMessageStoreType()==MessageStoreType.CUSTOM){
			className = messageStoreModel.getCustomProviderClass();
			for (Entry<String, String> param : messageStoreModel
					.getCustomParameters().entrySet()) {
				if(!StringUtils.isBlank(param.getKey()) && !StringUtils.isBlank(param.getValue())){
					parameters.put(param.getKey(), param.getValue().toString());
				}
			}
		} else if(messageStoreModel.getMessageStoreType()==MessageStoreType.JMS){
			// Fixing TOOLS-2026.
			//className = "org.wso2.carbon.message.store.persistence.jms.JMSMessageStore";
			className = "org.apache.synapse.message.store.impl.jms.JmsStore";
			if(!StringUtils.isBlank(messageStoreModel.getJmsContextFactory())){
				parameters.put("java.naming.factory.initial", messageStoreModel.getJmsContextFactory());
			}
			if(!StringUtils.isBlank(messageStoreModel.getJmsProviderUrl())){
				parameters.put("java.naming.provider.url", messageStoreModel.getJmsProviderUrl());
			}
			if(!StringUtils.isBlank(messageStoreModel.getJmsQueueName())){
				parameters.put("store.jms.destination", messageStoreModel.getJmsQueueName());
			}
			if(!StringUtils.isBlank(messageStoreModel.getJmsConnectionFactory())){
				parameters.put("store.jms.connection.factory", messageStoreModel.getJmsConnectionFactory());
			}
			if(!StringUtils.isBlank(messageStoreModel.getJmsUsername())){
				parameters.put("store.jms.username", messageStoreModel.getJmsUsername());
			}
			if(!StringUtils.isBlank(messageStoreModel.getJmsPassword())){
				parameters.put("store.jms.password", messageStoreModel.getJmsPassword());
			}
			if(!StringUtils.isBlank(messageStoreModel.getJmsCacheConnection())){
				parameters.put("store.jms.cache.connection", messageStoreModel.getJmsCacheConnection());
			}
			if(!StringUtils.isBlank(messageStoreModel.getJmsApiVersion())){
				parameters.put("store.jms.JMSSpecVersion", messageStoreModel.getJmsApiVersion());
			}			
			if(!StringUtils.isBlank(messageStoreModel.getJmsEnableProducerGuaranteedDelivery())){
				parameters.put("store.producer.guaranteed.delivery.enable", messageStoreModel.getJmsEnableProducerGuaranteedDelivery());
			}			
			if(!StringUtils.isBlank(messageStoreModel.getJmsFailoverMessageStore())){
				parameters.put("store.failover.message.store.name", messageStoreModel.getJmsFailoverMessageStore());
			}
		
			parameters.put("store.jms.ConsumerReceiveTimeOut", ((Integer)messageStoreModel.getJmsTimeout()).toString());
		} else if(messageStoreModel.getMessageStoreType()==MessageStoreType.WSO2_MB){
					className = "org.apache.synapse.message.store.impl.jms.JmsStore";
					if(!StringUtils.isBlank(messageStoreModel.getMbContextFactory())){
						parameters.put("java.naming.factory.initial", messageStoreModel.getMbContextFactory());
					}
					if(!StringUtils.isBlank(messageStoreModel.getMbConnectionFactory())){
						parameters.put("connectionfactory.QueueConnectionFactory", messageStoreModel.getMbConnectionFactory());
					}
					if(!StringUtils.isBlank(messageStoreModel.getMbApiVersion())){
						parameters.put("store.jms.JMSSpecVersion", messageStoreModel.getMbApiVersion());
					}			
					if(!StringUtils.isBlank(messageStoreModel.getMbEnableProducerGuaranteedDelivery())){
						parameters.put("store.producer.guaranteed.delivery.enable", messageStoreModel.getMbEnableProducerGuaranteedDelivery());
					}	
					if(!StringUtils.isBlank(messageStoreModel.getMbQueueName())){
						parameters.put("store.jms.destination", messageStoreModel.getMbQueueName());
					}
					if(!StringUtils.isBlank(messageStoreModel.getMbFailoverMessageStore())){
						parameters.put("store.failover.message.store.name", messageStoreModel.getMbFailoverMessageStore());
					}
					if(!StringUtils.isBlank(messageStoreModel.getMbCacheConnection())){
						parameters.put("store.jms.cache.connection", messageStoreModel.getMbCacheConnection());
					}
				} else if (messageStoreModel.getMessageStoreType() == MessageStoreType.RABBITMQ) {
			className = RABBITMQ_MS_FQN;
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQServerHostName())) {
				parameters.put(STORE_RABBITMQ_HOST_NAME, messageStoreModel.getRabbitMQServerHostName());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQServerHostPort())) {
				parameters.put(STORE_RABBITMQ_HOST_PORT, messageStoreModel.getRabbitMQServerHostPort());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQQueueName())) {
				parameters.put(STORE_RABBITMQ_QUEUE_NAME, messageStoreModel.getRabbitMQQueueName());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQExchangeName())) {
				parameters.put(STORE_RABBITMQ_EXCHANGE_NAME, messageStoreModel.getRabbitMQExchangeName());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQRoutingKey())) {
				parameters.put(STORE_RABBITMQ_ROUTE_KEY, messageStoreModel.getRabbitMQRoutingKey());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQUserName())) {
				parameters.put(STORE_RABBITMQ_USERNAME, messageStoreModel.getRabbitMQUserName());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQPassword())) {
				parameters.put(STORE_RABBITMQ_PASSWORD, messageStoreModel.getRabbitMQPassword());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQVirtualHost())) {
				parameters.put(STORE_RABBITMQ_VIRTUAL_HOST, messageStoreModel.getRabbitMQVirtualHost());
			}			
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQEnableProducerGuaranteedDelivery())) {
				parameters.put(STORE_RABBITMQ_PRODUCER_GUARANTEED_DELIVERY_ENABLE, messageStoreModel.getRabbitMQEnableProducerGuaranteedDelivery());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQFailoverMessageStore())) {
				parameters.put(STORE_RABBITMQ_FAILOVER_MESSAGE_STORE_NAME, messageStoreModel.getRabbitMQFailoverMessageStore());
			}
			if(messageStoreModel.getRabbitMQSSLEnabled()){
				parameters.put(STORE_RABBITMQ_SSL_ENABLED, "true");
			}else{
				parameters.put(STORE_RABBITMQ_SSL_ENABLED, "false");
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQSSLKeyStoreLocation())) {
				parameters.put(STORE_RABBITMQ_SSL_KEYSTORE_LOCATION, messageStoreModel.getRabbitMQSSLKeyStoreLocation());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQSSLKeyStoreType())) {
				parameters.put(STORE_RABBITMQ_SSL_KEYSTORE_TYPE, messageStoreModel.getRabbitMQSSLKeyStoreType());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQSSLKeyStorePassword())) {
				parameters.put(STORE_RABBITMQ_SSL_KEYSTORE_PASSWORD, messageStoreModel.getRabbitMQSSLKeyStorePassword());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQSSLTrustStoreLocation())) {
				parameters.put(STORE_RABBITMQ_SSL_TRUSTSTORE_LOCATION, messageStoreModel.getRabbitMQSSLTrustStoreLocation());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQSSLTrustStoreType())) {
				parameters.put(STORE_RABBITMQ_SSL_TRUSTSTORE_TYPE, messageStoreModel.getRabbitMQSSLTrustStoreType());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQSSLTrustStorePassword())) {
				parameters.put(STORE_RABBITMQ_SSL_TRUSTSTORE_PASSWORD, messageStoreModel.getRabbitMQSSLTrustStorePassword());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getRabbitMQSSLVersion())) {
				parameters.put(STORE_RABBITMQ_SSL_VERSION, messageStoreModel.getRabbitMQSSLVersion());
			}
			
			
		} else if (messageStoreModel.getMessageStoreType() == MessageStoreType.JDBC) {
			className = JDBC_MS_FQN;
			if (StringUtils.isNotBlank(messageStoreModel.getJdbcDatabaseTable())) {
				parameters.put(STORE_JDBC_TABLE, messageStoreModel.getJdbcDatabaseTable());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getJdbcEnableProducerGuaranteedDelivery())) {
				parameters.put(STORE_JDBC_PRODUCER_GUARANTEED_DELIVERY_ENABLE, messageStoreModel.getJdbcEnableProducerGuaranteedDelivery());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getJdbcFailoverMessageStore())) {
				parameters.put(STORE_JDBC_FAILOVER_MESSAGE_STORE_NAME, messageStoreModel.getJdbcFailoverMessageStore());
			}
			// Switch between connection pool and datasource
			String jdbcConnectionInformation = messageStoreModel.getJdbcConnectionInformation();
			if (StringUtils.isNotBlank(jdbcConnectionInformation)) {
				if (JDBCConnectionInformationType.POOL.toString().equals(jdbcConnectionInformation)) {
					if (StringUtils.isNotBlank(messageStoreModel.getJdbcDriver())) {
						parameters.put(STORE_JDBC_DRIVER, messageStoreModel.getJdbcDriver());
					}
					if (StringUtils.isNotBlank(messageStoreModel.getJdbcURL())) {
						parameters.put(STORE_JDBC_CONNECTION_URL, messageStoreModel.getJdbcURL());
					}
					if (StringUtils.isNotBlank(messageStoreModel.getJdbcUser())) {
						parameters.put(STORE_JDBC_USERNAME, messageStoreModel.getJdbcUser());
					}
					if (StringUtils.isNotBlank(messageStoreModel.getJdbcPassword())) {
						parameters.put(STORE_JDBC_PASSWORD, messageStoreModel.getJdbcPassword());
					}
				} else if (JDBCConnectionInformationType.CARBON_DATASOURCE.toString().equals(jdbcConnectionInformation)) {
					if (StringUtils.isNotBlank(messageStoreModel.getJdbcDatasourceName())) {
						parameters.put(STORE_JDBC_DS_NAME, messageStoreModel.getJdbcDatasourceName());
					}
				}
			}
		} else if (messageStoreModel.getMessageStoreType() == MessageStoreType.RESEQUENCE) {
			className = RESEQUENCE_MS_FQN;
			if (StringUtils.isNotBlank(messageStoreModel.getResequenceDatabaseTable())) {
				parameters.put(STORE_RESEQUENCE_TABLE, messageStoreModel.getResequenceDatabaseTable());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getResequenceEnableProducerGuaranteedDelivery())) {
				parameters.put(STORE_RESEQUENCE_PRODUCER_GUARANTEED_DELIVERY_ENABLE, messageStoreModel.getResequenceEnableProducerGuaranteedDelivery());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getResequenceFailoverMessageStore())) {
				parameters.put(STORE_RESEQUENCE_FAILOVER_MESSAGE_STORE_NAME, messageStoreModel.getResequenceFailoverMessageStore());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getResequencePollingCount())
					&& StringUtils.isNumeric(messageStoreModel.getResequencePollingCount())) {
				parameters.put(STORE_RESEQUENCE_POLLING_COUNT, messageStoreModel.getResequencePollingCount());
			}
			if (StringUtils.isNotBlank(messageStoreModel.getResequenceXpath())) {
				SynapseXPath xpath;
				try {
					xpath = new SynapseXPath(messageStoreModel.getResequenceXpath());
					Map<String,String> maps = messageStoreModel.getResequenceXpathNamespaces(); //Add namespaces
					for(String key:maps.keySet()) {
						xpath.addNamespace(key,maps.get(key));
					}
					parameters.put(STORE_RESEQUENCE_XPATH, xpath);
				} catch (JaxenException e) {
					log.error("Cannot convert xpath", e);
				}
			}
			// Switch between connection pool and datasource
			String resequenceConnectionInformation = messageStoreModel.getResequenceConnectionInformation();
			if (StringUtils.isNotBlank(resequenceConnectionInformation)) {
				if (JDBCConnectionInformationType.POOL.toString().equals(resequenceConnectionInformation)) {
					if (StringUtils.isNotBlank(messageStoreModel.getResequenceDriver())) {
						parameters.put(STORE_RESEQUENCE_DRIVER, messageStoreModel.getResequenceDriver());
					}
					if (StringUtils.isNotBlank(messageStoreModel.getResequenceURL())) {
						parameters.put(STORE_RESEQUENCE_CONNECTION_URL, messageStoreModel.getResequenceURL());
					}
					if (StringUtils.isNotBlank(messageStoreModel.getResequenceUser())) {
						parameters.put(STORE_RESEQUENCE_USERNAME, messageStoreModel.getResequenceUser());
					}
					if (StringUtils.isNotBlank(messageStoreModel.getResequencePassword())) {
						parameters.put(STORE_RESEQUENCE_PASSWORD, messageStoreModel.getResequencePassword());
					}
				} else if (JDBCConnectionInformationType.CARBON_DATASOURCE.toString().equals(resequenceConnectionInformation) 
						&& StringUtils.isNotBlank(messageStoreModel.getResequenceDatasourceName())) {
					parameters.put(STORE_RESEQUENCE_DS_NAME, messageStoreModel.getResequenceDatasourceName());
				}
			}
		}
		
		store.setParameters(parameters);
		
		OMElement messageStoreElement = MessageStoreSerializer.serializeMessageStore(null, store);
		OMAttribute classAttr = messageStoreElement.getAttribute(new QName("class"));
		if(classAttr!=null){
			classAttr.setAttributeValue(className);
		} else if (!StringUtils.isBlank(className)) {
			messageStoreElement.addAttribute("class", className, null);
		} else{
			/**
			 * Class attribute is optional for In-Memory Store.
			 * If class name is not defined it will be considered as an 
			 * In-Memory store by default.
			 */
			//messageStoreElement.addAttribute("class", className, null);
		}
		return messageStoreElement.toString().replace("><", ">" + lineSeparator + "<");
	}

    public void updatePom() throws IOException, XmlPullParserException {
        File mavenProjectPomLocation = esbProject.getFile("pom.xml").getLocation().toFile();
        MavenProject mavenProject = MavenUtils.getMavenProject(mavenProjectPomLocation);

        // Skip changing the pom file if group ID and artifact ID are matched
        if (MavenUtils.checkOldPluginEntry(mavenProject, "org.wso2.maven", "wso2-esb-messagestore-plugin",
                ESBMavenConstants.WSO2_ESB_MESSAGE_STORE_PLUGIN_VERSION)) {
            return;
        }

        Plugin plugin = MavenUtils.createPluginEntry(mavenProject, "org.wso2.maven", "wso2-esb-messagestore-plugin",
                ESBMavenConstants.WSO2_ESB_MESSAGE_STORE_PLUGIN_VERSION, true);
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.addGoal("pom-gen");
        pluginExecution.setPhase("process-resources");
        pluginExecution.setId("task");

        Xpp3Dom configurationNode = MavenUtils.createMainConfigurationNode();
        Xpp3Dom artifactLocationNode = MavenUtils.createXpp3Node(configurationNode, "artifactLocation");
        artifactLocationNode.setValue(".");
        Xpp3Dom typeListNode = MavenUtils.createXpp3Node(configurationNode, "typeList");
        typeListNode.setValue("${artifact.types}");
        pluginExecution.setConfiguration(configurationNode);
        plugin.addExecution(pluginExecution);
        MavenUtils.saveMavenProject(mavenProject, mavenProjectPomLocation);
    }

	@Override
		public void openEditor(File file) {
		try{
		refreshDistProjects();
		IFile resource  = ResourcesPlugin
		.getWorkspace()
		.getRoot()
		.getFileForLocation(
				Path.fromOSString(file.getAbsolutePath()));
			String path = resource.getParent().getFullPath() + "/";
			String source = FileUtils.getContentAsString(file);
			Openable openable = ESBGraphicalEditor.getOpenable();
			openable.editorOpen(file.getName(), ArtifactType.MESSAGE_STORE.getLiteral(), path, source);
		}catch(Exception e){
			log.error("Cannot open the editor", e);
		}
	}
	
	public void copyImportFile(IContainer importLocation,boolean isNewAritfact, String groupId) throws Exception {
		File importFile = getModel().getImportFile();
		File destFile = null;
		List<OMElement> selectedList = ((MessageStoreModel)getModel()).getSelectedStoresList();
		if(selectedList != null && selectedList.size() >0 ){
			for (OMElement element : selectedList) {
				String name = element.getAttributeValue(new QName("name"));
				destFile = new File(importLocation.getLocation().toFile(), name + ".xml");
				FileUtils.createFile(destFile, element.toString());
				fileLst.add(destFile);
                if (isNewAritfact) {

                    addESBArtifactDetails(importLocation, name, groupId, version, name, esbProjectArtifact);
                }
			} 
			
		}else{
			destFile = new File(importLocation.getLocation().toFile(), importFile.getName());
			FileUtils.copy(importFile, destFile);
			fileLst.add(destFile);
			String name = importFile.getName().replaceAll(".xml$","");
            if (isNewAritfact) {

                addESBArtifactDetails(importLocation, name, groupId, version, name, esbProjectArtifact);
            }
		}
	}

}

/*
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.integrationstudio.artifact.localentry.ui.wizard;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
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
import org.eclipse.swt.widgets.Display;
import org.wso2.integrationstudio.artifact.localentry.Activator;
import org.wso2.integrationstudio.artifact.localentry.model.LocalEntryModel;
import org.wso2.integrationstudio.artifact.localentry.utils.LocalEntryArtifactConstants;
import org.wso2.integrationstudio.artifact.localentry.utils.LocalEntryImageUtils;
import org.wso2.integrationstudio.artifact.localentry.utils.LocalEntryTemplateUtils;
import org.wso2.integrationstudio.esb.core.ESBMavenConstants;
import org.wso2.integrationstudio.esb.core.exceptions.BuildArtifactCreationException;
import org.wso2.integrationstudio.esb.core.utils.SynapseConstants;
import org.wso2.integrationstudio.esb.core.utils.SynapseUtils;
import org.wso2.integrationstudio.esb.project.artifact.ESBArtifact;
import org.wso2.integrationstudio.esb.project.artifact.ESBProjectArtifact;
import org.wso2.integrationstudio.logging.core.IIntegrationStudioLog;
import org.wso2.integrationstudio.logging.core.Logger;
import org.wso2.integrationstudio.maven.util.MavenUtils;
import org.wso2.integrationstudio.platform.ui.editor.Openable;
import org.wso2.integrationstudio.platform.ui.startup.ESBGraphicalEditor;
import org.wso2.integrationstudio.platform.ui.validator.CommonFieldValidator;
import org.wso2.integrationstudio.platform.ui.wizard.AbstractWSO2ProjectCreationWizard;
import org.wso2.integrationstudio.utils.file.FileUtils;

public class LocalEntryProjectCreationWizard extends AbstractWSO2ProjectCreationWizard {
	
	private static IIntegrationStudioLog log=Logger.getLog(Activator.PLUGIN_ID);

	private LocalEntryModel localEntryModel;
	private IFile localEntryFile;
	private ESBProjectArtifact esbProjectArtifact;
	private List<File> fileLst = new ArrayList<File>();
	private IProject esbProject;
	private String version = "1.0.0";
	
	
	public LocalEntryProjectCreationWizard() {
		this.localEntryModel = new LocalEntryModel();
		setModel(this.localEntryModel);
		setWindowTitle(LocalEntryArtifactConstants.LE_WIZARD_WINDOW_TITLE);
		setDefaultPageImageDescriptor(LocalEntryImageUtils.getInstance().getImageDescriptor("local-entries-wizard-artifact.png"));
	}
	
	
	public IResource getCreatedResource() {
		return localEntryFile;
	}
	
	protected boolean isRequireProjectLocationSection() {
		return false;
	}
	
	public boolean createArtifact(String content, IContainer artifactLocation, String configName) throws Exception{	
		LocalEntryModel localEntryModel = new LocalEntryModel();
		localEntryModel.setLocalEntrySaveLocation(artifactLocation);
		localEntryModel.setLocalENtryName(configName);
		localEntryModel.setSelectedOption("");
		localEntryModel.setSelectedLocalEntryType("");
		
		this.setProject(artifactLocation.getProject());
		this.setModel(localEntryModel);
        return createLocalEntryArtifact(localEntryModel);
	}
	
	
	public boolean createLocalEntryArtifact(LocalEntryModel localEntryModel) throws Exception {
		this.localEntryModel=localEntryModel;
		boolean isNewArtifact = true;
		IContainer location = esbProject.getFolder("src" + File.separator + "main" + File.separator
				+ "synapse-config" + File.separator + "local-entries");
        
		File pomLocation = esbProject.getFile("pom.xml").getLocation().toFile();
		
        MavenProject mavenProject = MavenUtils.getMavenProject(pomLocation);
        version = mavenProject.getVersion().replace("-SNAPSHOT", "");
        
        if (!esbProject.getFolder(SynapseConstants.BUILD_ARTIFACTS_FOLDER).exists()) {
            updatePom();
        }
        
        esbProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		String groupId = getMavenGroupId(pomLocation);
		groupId += ".local-entry";

		// Adding the metadata about the localentry to the metadata store.
		esbProjectArtifact = new ESBProjectArtifact();
		esbProjectArtifact.fromFile(esbProject.getFile("artifact.xml").getLocation().toFile());

		if (getModel().getSelectedOption().equals(
				LocalEntryArtifactConstants.WIZARD_OPTION_IMPORT_OPTION)) {
			localEntryFile = location.getFile(new Path(getModel().getImportFile().getName()));
			if (localEntryFile.exists()) {
				if (!MessageDialog.openQuestion(getShell(), "WARNING",
						"Do you like to override exsiting project in the workspace")) {
					return false;
				}
				isNewArtifact = false;
			}
			copyImportFile(location, isNewArtifact, groupId);

		} else {
			File localEntryFile = new File(location.getLocation().toFile(),
					localEntryModel.getLocalENtryName() + ".xml");
			writeTemplete(localEntryFile);

			addESBArtifactDetails(location, localEntryModel.getLocalENtryName(), groupId, version,
					localEntryModel.getLocalENtryName(), esbProjectArtifact);
		}
		File pomfile = esbProject.getFile("pom.xml").getLocation().toFile();
		getModel().getMavenInfo().setPackageName(SynapseConstants.LOCAL_ENTRY_CONFIG_TYPE);
		if (!pomfile.exists()) {
			createPOM(pomfile);
		}

		esbProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		return true;
	}

    private ESBArtifact createArtifact(String name, String groupId, String version, String path) {
        ESBArtifact artifact = new ESBArtifact();
        artifact.setName(name);
        artifact.setVersion(version);
        artifact.setType(SynapseConstants.LOCAL_ENTRY_CONFIG_TYPE);
        artifact.setServerRole("EnterpriseServiceBus");
        artifact.setGroupId(groupId);
        artifact.setFile(path);
        return artifact;
    }

    private void addESBArtifactDetails(IContainer location, String localEntryName, String groupId, String version,
            String localEntryFileName, ESBProjectArtifact esbProjectArtifact) throws Exception {

        String relativeLocation = FileUtils
                .getRelativePath(esbProject.getLocation().toFile(),
                        new File(location.getLocation().toFile(), localEntryName + ".xml"))
                .replaceAll(Pattern.quote(File.separator), "/");
        esbProjectArtifact.addESBArtifact(createArtifact(localEntryName, groupId, version, relativeLocation));
        esbProjectArtifact.toFile();
        createLocalEntryBuildArtifactPom(groupId, localEntryName, version, localEntryFileName, relativeLocation);
    }

    private void createLocalEntryBuildArtifactPom(String groupId, String artifactId, String version,
            String localEntryFileName, String relativePathToRealArtifact) throws BuildArtifactCreationException {

        IContainer buildArtifactsLocation = esbProject.getFolder(SynapseConstants.BUILD_ARTIFACTS_FOLDER);
        try {
            SynapseUtils.createSynapseConfigBuildArtifactPom(groupId, artifactId, version,
                    SynapseConstants.LOCAL_ENTRY_CONFIG_TYPE, localEntryFileName, SynapseConstants.LOCAL_ENTRY_FOLDER,
                    buildArtifactsLocation, "../../../" + relativePathToRealArtifact);
        } catch (IOException | XmlPullParserException e) {
            throw new BuildArtifactCreationException("Error while creating the build artifacts for Local Entry config "
                    + localEntryFileName + " at " + buildArtifactsLocation.getFullPath());
        }
    }

	public boolean performFinish() {
		if (LocalEntryArtifactConstants.TYPE_IN_LINE_XML_LE.equals(localEntryModel.getSelectedLocalEntryType()) &&
				!CommonFieldValidator.isValidXML(localEntryModel.getInLineXMLValue())) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Invalid In-Line XML",
					"Invalid XML Contents Provided\n" +
							"XML Parsing Error: not well-formed");
			return false;
		}
		try {
			boolean isNewArtifact = true;
			localEntryModel = (LocalEntryModel)getModel();
			if (localEntryModel != null && localEntryModel.getSourceURL() != null 
			        && !localEntryModel.getSourceURL().startsWith("http")) {
			    localEntryModel.setSourceURL(createFileURL(localEntryModel.getSourceURL()));
			}
			esbProject =  localEntryModel.getLocalEntrySaveLocation().getProject();
			createLocalEntryArtifact(localEntryModel);
			
			if(fileLst.size()>0){
				openEditor(fileLst.get(0));
			}
			
		} catch (CoreException e) {
			log.error("CoreException has occurred", e);
		} catch (Exception e) {
			log.error("An unexpected error has occurred", e);
		}
		return true;
	}

    public void updatePom() throws IOException, XmlPullParserException {
        File mavenProjectPomLocation = esbProject.getFile("pom.xml").getLocation().toFile();
        MavenProject mavenProject = MavenUtils.getMavenProject(mavenProjectPomLocation);
        // Skip changing the pom file if group ID and artifact ID are matched
        if (MavenUtils.checkOldPluginEntry(mavenProject, "org.wso2.maven", "wso2-esb-localentry-plugin")) {
            return;
        }

        Plugin plugin = MavenUtils.createPluginEntry(mavenProject, "org.wso2.maven", "wso2-esb-localentry-plugin",
                ESBMavenConstants.WSO2_ESB_LOCAL_ENTRY_VERSION, true);
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.addGoal("pom-gen");
        pluginExecution.setPhase("process-resources");
        pluginExecution.setId("localentry");

        Xpp3Dom configurationNode = MavenUtils.createMainConfigurationNode();
        Xpp3Dom artifactLocationNode = MavenUtils.createXpp3Node(configurationNode, "artifactLocation");
        artifactLocationNode.setValue(".");
        Xpp3Dom typeListNode = MavenUtils.createXpp3Node(configurationNode, "typeList");
        typeListNode.setValue("${artifact.types}");
        pluginExecution.setConfiguration(configurationNode);
        plugin.addExecution(pluginExecution);
        MavenUtils.saveMavenProject(mavenProject, mavenProjectPomLocation);
    }

	private String createFileURL(String fullFilePath){
        //check how it happens in windows
        //linux - file:/home/chathuri/Desktop/input.txt
        //windows - file:\C:\WSO2\tooling\endpoint21.xml
        String fileURL = "";
        if(fullFilePath != null && !fullFilePath.equals("")){
            if(fullFilePath.startsWith("/")){
                fileURL = "file:" + fullFilePath;
            }else if(!fullFilePath.startsWith("file")){
                fileURL = "file:/" + fullFilePath;
            }else{
                return fullFilePath;
            }
        }
        return fileURL;
    }
	
	protected boolean isRequiredWorkingSet() {
		return false;
	}
	
	private void writeTemplete(File localEntryFile) {
		try {
			String content = "";
			String templateToUse = "InLineTextLE.xml";
			if(localEntryModel.getSelectedLocalEntryType().equals(LocalEntryArtifactConstants.TYPE_IN_LINE_TEXT_LE)){
				templateToUse = "InLineTextLE.xml";
				
				content =
					      MessageFormat.format(LocalEntryTemplateUtils.getInstance()
					                                                  .getTemplateString(templateToUse),
					                                                  localEntryModel.getLocalENtryName(), localEntryModel.getInLineTextValue());
				
			}else if(localEntryModel.getSelectedLocalEntryType().equals(LocalEntryArtifactConstants.TYPE_IN_LINE_XML_LE)){
				templateToUse = "InLineXmlLE.xml";
				
				content =
					      MessageFormat.format(LocalEntryTemplateUtils.getInstance()
					                                                  .getTemplateString(templateToUse),
					                                                  localEntryModel.getLocalENtryName(), localEntryModel.getInLineXMLValue());
				
			}else if(localEntryModel.getSelectedLocalEntryType().equals(LocalEntryArtifactConstants.TYPE_SOURCE_URL_LE)){
				templateToUse = "SourceURLLE.xml";
				
				content =
					      MessageFormat.format(LocalEntryTemplateUtils.getInstance()
					                                                  .getTemplateString(templateToUse),
					                                                  localEntryModel.getLocalENtryName(), localEntryModel.getSourceURL());
			}else{
				
			}
			FileUtils.writeContent(localEntryFile, content);
			fileLst.add(localEntryFile);
		} catch (IOException e) {
			log.error("I/O Error has occurred", e);
		}
	}
	
	public void copyImportFile(IContainer importLocation,boolean isNewArtifact,String groupId) throws Exception {
		File importFile = getModel().getImportFile();
		List<OMElement> selectedLEList = localEntryModel.getSelectedLEList();
		File destFile = null;
		if(selectedLEList != null && selectedLEList.size() >0 ){
			for (OMElement element : selectedLEList) {
				String key = element.getAttributeValue(new QName("key"));
				destFile  = new File(importLocation.getLocation().toFile(), key + ".xml");
				FileUtils.createFile(destFile, element.toString());
				fileLst.add(destFile);
				if(isNewArtifact){
					addESBArtifactDetails(importLocation, key, groupId, version, key, esbProjectArtifact);
				}
			}
		}else{
			destFile = new File(importLocation.getLocation().toFile(), importFile.getName());
			FileUtils.copy(importFile, destFile);
			fileLst.add(destFile);
			String key = importFile.getName().replaceAll(".xml$", "");
			if(isNewArtifact){
				addESBArtifactDetails(importLocation, key, groupId, version, key, esbProjectArtifact);
			}
		}
	}
	
	public void openEditor(File file) {
		try{
		refreshDistProjects();
		IFile dbsFile  = ResourcesPlugin
		.getWorkspace()
		.getRoot()
		.getFileForLocation(
				Path.fromOSString(file.getAbsolutePath()));
		/*IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),dbsFile);*/
		String path = dbsFile.getParent().getFullPath()+"/";
		String source = FileUtils.getContentAsString(file);
		Openable openable = ESBGraphicalEditor.getOpenable();
		//openable.editorOpen(file.getName(), ArtifactType.LOCAL_ENTRY.getLiteral(),path, source);
		openable.editorOpen(file.getName(), "LOCAL_ENTRY", path, source);
		}catch(Exception e){
			log.error("Cannot open the editor", e);
		}
	}
	
	public void setProject(IProject project) {
		this.esbProject = project;
	}
}

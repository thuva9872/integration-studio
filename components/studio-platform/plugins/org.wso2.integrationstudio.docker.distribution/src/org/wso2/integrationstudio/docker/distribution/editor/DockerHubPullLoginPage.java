/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.integrationstudio.docker.distribution.editor;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;

public class DockerHubPullLoginPage extends WizardPage {

	private static final String DIALOG_TITLE = "Base Image Docker Registry Credentials";
	private static final String SUB_DIALOG_TITLE = "Enter Docker Registry Credentials";
	private static final String BASE_IMAGE_AUTHENTICATION_TEXT = "Base Image Docker Authentication";
	private static final String EMPTY_STRING = "";
	private static final String REGISTRY_TYPE = "Registry Type:";
	public final String DOCKERHUB_REGISTRY = "Docker Hub";
	public final String OTHER_REGISTRY = "Other";
	private static final String USERNAME = "Username:";
	private static final String PASSWORD = "Password:";
	private static final String ARIAL = "Arial";
	private final String PUBLIC = "Public";
	private final String PRIVATE = "Private";

	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
	        Pattern.CASE_INSENSITIVE);

	private String wso2DockerHubUsername = EMPTY_STRING;
	private String wso2DockerHubPassword = EMPTY_STRING;
	private String selectedWso2RegistryType = EMPTY_STRING;

	private Combo wso2RegistryTypeCombo;
	private Text txtWso2DockerUsername;
	private Text txtWso2DockerPassword;

	private Composite containerPrivateWso2DockerRegistry;

	protected DockerHubPullLoginPage(IWorkbench wb, IStructuredSelection selection, IFile pomFile) {
		super(DIALOG_TITLE);
		setTitle(SUB_DIALOG_TITLE);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new FormLayout());
		FormData data;

		Label titleLabel = new Label(container, SWT.NONE);
		titleLabel.setText(BASE_IMAGE_AUTHENTICATION_TEXT);
		titleLabel.setFont(new Font(Display.getCurrent(), ARIAL, 12, SWT.BOLD));
		
		Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);

		FormData titleData = new FormData();
		titleData.top = new FormAttachment(0);
		titleData.left = new FormAttachment(0, 10);
		titleLabel.setLayoutData(titleData);

		FormData separatorData = new FormData();
		separatorData.top = new FormAttachment(titleLabel, 5);
		separatorData.left = new FormAttachment(0, 10);
		separatorData.right = new FormAttachment(100, -10);
		separator.setLayoutData(separatorData);

		FormData contentData = new FormData();
		contentData.top = new FormAttachment(separator, 5);
		contentData.left = new FormAttachment(0, 10);
		contentData.right = new FormAttachment(100, -10);
		container.setLayoutData(contentData);

		Label lblWso2RegistryType = new Label(container, SWT.NONE);
		data = new FormData();
		data.top = new FormAttachment(separator, 20);
		data.left = new FormAttachment(3);
		data.width = 160;
		lblWso2RegistryType.setLayoutData(data);
		lblWso2RegistryType.setText(REGISTRY_TYPE);

		String[] wso2RegistryContainers = { PUBLIC, PRIVATE };
		wso2RegistryTypeCombo = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
		wso2RegistryTypeCombo.setItems(wso2RegistryContainers);
		data = new FormData();
		data.top = new FormAttachment(separator, 20);
		data.left = new FormAttachment(lblWso2RegistryType, 0);
		data.right = new FormAttachment(97);
		data.width = 400;
		wso2RegistryTypeCombo.setLayoutData(data);
		
		if (getSelectedWso2RegistryType() != null && !getSelectedWso2RegistryType().isEmpty()) {
			wso2RegistryTypeCombo.setText(getSelectedWso2RegistryType());
		} else {
			wso2RegistryTypeCombo.setText(wso2RegistryContainers[0]);
			setSelectedWso2RegistryType(wso2RegistryTypeCombo.getText());
		}

		wso2RegistryTypeCombo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setSelectedWso2RegistryType(wso2RegistryTypeCombo.getText());
				changeWso2LoginView();
				container.layout();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		containerPrivateWso2DockerRegistry = new Composite(container, SWT.NULL);
		containerPrivateWso2DockerRegistry.setLayout(new FormLayout());

		data = new FormData();
		data.top = new FormAttachment(wso2RegistryTypeCombo, 15);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		containerPrivateWso2DockerRegistry.setLayoutData(data);
		containerPrivateWso2DockerRegistry.setVisible(false);
		previewAuthFieldsForWso2Registry();

		changeWso2LoginView();
		container.layout();
		setPageComplete(false);
		validate();
	}

	private void changeWso2LoginView() {
		if (getSelectedWso2RegistryType().equals(PRIVATE)) {
			containerPrivateWso2DockerRegistry.setVisible(true);
			if (!getWso2DockerUsernameValue().isEmpty()) {
				txtWso2DockerUsername.setText(getWso2DockerUsernameValue());
			}
			if (!getWso2DockerPasswordValue().isEmpty()) {
				txtWso2DockerPassword.setText(getWso2DockerPasswordValue());
			}
		} else {
			containerPrivateWso2DockerRegistry.setVisible(false);
		}
		validate();
	}

	private void previewAuthFieldsForWso2Registry() {
		FormData data;

		Label lblUsername = new Label(containerPrivateWso2DockerRegistry, SWT.NONE);
		data = new FormData();
		data.top = new FormAttachment(containerPrivateWso2DockerRegistry);
		data.left = new FormAttachment(3);
		data.width = 160;
		lblUsername.setLayoutData(data);
		lblUsername.setText(USERNAME);

		txtWso2DockerUsername = new Text(containerPrivateWso2DockerRegistry, SWT.BORDER);
		data = new FormData();
		data.top = new FormAttachment(containerPrivateWso2DockerRegistry);
		data.left = new FormAttachment(lblUsername, 0);
		data.right = new FormAttachment(97);
		data.width = 400;
		txtWso2DockerUsername.setLayoutData(data);
		if (!getWso2DockerUsernameValue().isEmpty()) {
			txtWso2DockerUsername.setText(getWso2DockerUsernameValue());
		}

		txtWso2DockerUsername.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				setWso2DockerUsernameValue(txtWso2DockerUsername.getText());
				validate();
			}
		});

		Label lblPassword = new Label(containerPrivateWso2DockerRegistry, SWT.NONE);
		data = new FormData();
		data.top = new FormAttachment(lblUsername, 25);
		data.left = new FormAttachment(3);
		data.width = 160;
		lblPassword.setLayoutData(data);
		lblPassword.setText(PASSWORD);

		txtWso2DockerPassword = new Text(containerPrivateWso2DockerRegistry, SWT.PASSWORD | SWT.BORDER);
		data = new FormData();
		data.top = new FormAttachment(lblUsername, 25);
		data.left = new FormAttachment(lblPassword, 0);
		data.right = new FormAttachment(97);
		data.width = 400;
		txtWso2DockerPassword.setLayoutData(data);
		txtWso2DockerPassword.setFocus();
		if (!getWso2DockerPasswordValue().isEmpty()) {
			txtWso2DockerPassword.setText(getWso2DockerPasswordValue());
		}

		txtWso2DockerPassword.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				setWso2DockerPasswordValue(txtWso2DockerPassword.getText());
				validate();
			}
		});
	}

	/**
	 * Method for validations of wizard page. If validation fails set page as dirty
	 * and not complete
	 */
	private void validate() {
		if (getSelectedWso2RegistryType().equals(PRIVATE)) {
			if (getWso2DockerUsernameValue() == null || getWso2DockerUsernameValue().equals(EMPTY_STRING)) {
				setErrorMessage("Please enter the Base Image Docker registry username");
				setPageComplete(false);
				return;
			} else if (getWso2DockerPasswordValue() == null || getWso2DockerPasswordValue().equals(EMPTY_STRING)) {
				setErrorMessage("Please enter the Base Image Docker registry password");
				setPageComplete(false);
				return;
			}
		}
		setErrorMessage(null);
		setPageComplete(true);
	}

	public String getWso2DockerUsernameValue() {
		return wso2DockerHubUsername;
	}

	public void setWso2DockerUsernameValue(String username) {
		this.wso2DockerHubUsername = username;
	}

	public String getWso2DockerPasswordValue() {
		return wso2DockerHubPassword;
	}

	public void setWso2DockerPasswordValue(String password) {
		this.wso2DockerHubPassword = password;
	}

	public String getSelectedWso2RegistryType() {
		return selectedWso2RegistryType;
	}

	public void setSelectedWso2RegistryType(String selectedWso2RegistryType) {
		this.selectedWso2RegistryType = selectedWso2RegistryType;
	}

}

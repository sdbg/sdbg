/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.sdbg.debug.ui.internal.chromeapp;

import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.ui.internal.SDBGDebugUIPlugin;
import com.github.sdbg.debug.ui.internal.chrome.ChromeLaunchMessages;
import com.github.sdbg.debug.ui.internal.util.AppSelectionDialog;
import com.github.sdbg.debug.ui.internal.util.IResourceFilter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

/**
 * The main UI tab for Chrome app launches.
 */
public class ChromeAppMainTab extends AbstractLaunchConfigurationTab {
  public static class ManifestResourceFilter implements IResourceFilter {
    @Override
    public boolean matches(IResource resource) {
      return resource.getName().equals("manifest.json");
    }
  }

  private Text fileText;

  protected Text argumentText;

  protected Text envText;

  protected ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  public ChromeAppMainTab() {
    setMessage("Create a configuration to launch a Chrome packaged app");
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 3).applyTo(composite);

    // main group
    Group group = new Group(composite, SWT.NONE);
    group.setText(ChromeLaunchMessages.ChromeMainTab_LaunchTarget);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);

    Label label = new Label(group, SWT.NONE);
    label.setText("Select a manifest.json file describing a Chrome packaged app");
    GridDataFactory.swtDefaults().span(2, 1).applyTo(label);

    fileText = new Text(group, SWT.BORDER | SWT.SINGLE);
    fileText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(fileText);

    Button browseButton = new Button(group, SWT.PUSH);
    browseButton.setText(ChromeLaunchMessages.ChromeMainTab_Browse);
    PixelConverter converter = new PixelConverter(browseButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(
        browseButton);
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowseButton();
      }
    });

    // additional browser arguments
    group = new Group(composite, SWT.NONE);
    group.setText("Chrome settings");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);

    Label argsLabel = new Label(group, SWT.NONE);
    argsLabel.setText("Browser arguments:");

    argumentText = new Text(group, SWT.BORDER | SWT.SINGLE);
    argumentText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        argumentText);

    // additional browser arguments
    group = new Group(composite, SWT.NONE);
    group.setText("Environment variables");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().applyTo(group);

    envText = new Text(group, SWT.BORDER | SWT.MULTI);
    envText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).hint(-1, 75).applyTo(
        envText);

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {
    if (performSdkCheck() != null) {
      return performSdkCheck();
    }

    if (fileText.getText().length() == 0) {
      return "manifest.json file not specified";
    }

    return null;
  }

  @Override
  public Image getImage() {
    return SDBGDebugUIPlugin.getImage("chrome_app.png");
  }

  @Override
  public String getName() {
    return "Main";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    SDBGLaunchConfigWrapper dartLauncher = new SDBGLaunchConfigWrapper(configuration);

    fileText.setText(dartLauncher.getApplicationName());
    argumentText.setText(dartLauncher.getArguments());
    envText.setText(dartLauncher.getEnvironmentString());
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return getErrorMessage() == null;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    SDBGLaunchConfigWrapper dartLauncher = new SDBGLaunchConfigWrapper(configuration);
    dartLauncher.setApplicationName(fileText.getText());
    dartLauncher.setArguments(argumentText.getText().trim());
    dartLauncher.setEnvironmentString(envText.getText().trim());
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    SDBGLaunchConfigWrapper dartLauncher = new SDBGLaunchConfigWrapper(configuration);
    dartLauncher.setApplicationName("");
    dartLauncher.setEnvironmentString("");
  }

  protected void handleBrowseButton() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();

    AppSelectionDialog dialog = new AppSelectionDialog(
        getShell(),
        workspace.getRoot(),
        new ManifestResourceFilter());
    dialog.setTitle("Select a manifest.json file");
    dialog.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION);
    IPath path = new Path(fileText.getText());

    if (workspace.validatePath(path.toString(), IResource.FILE).isOK()) {
      IFile file = workspace.getRoot().getFile(path);
      if (file != null && file.exists()) {
        dialog.setInitialSelections(new Object[] {path});
      }
    }

    dialog.open();

    Object[] results = dialog.getResult();

    if ((results != null) && (results.length > 0) && (results[0] instanceof IFile)) {
      IFile resource = (IFile) results[0];
      fileText.setText(resource.getFullPath().toPortableString());
    }
  }

  protected void notifyPanelChanged() {
    setDirty(true);

    updateLaunchConfigurationDialog();
  }

  protected String performSdkCheck() {
//&&&    
//    if (!DartSdkManager.getManager().hasSdk()) {
//      return "Dartium is not installed ("
//          + DartSdkManager.getManager().getSdk().getDartiumWorkingDirectory() + ")";
//    } else {
    return null;
//    }
  }

}

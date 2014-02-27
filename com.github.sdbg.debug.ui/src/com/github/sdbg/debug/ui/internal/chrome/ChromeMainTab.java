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
package com.github.sdbg.debug.ui.internal.chrome;

import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.ui.internal.SDBGDebugUIPlugin;
import com.github.sdbg.debug.ui.internal.util.LaunchTargetComposite;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * The main launch configuration UI for running applications in Chrome.
 */
public class ChromeMainTab extends AbstractLaunchConfigurationTab {

  protected ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  private Button showOutputButton;

  private Button useWebComponentsButton;

  private Text argumentText;

  private LaunchTargetComposite launchTargetGroup;

  /**
   * Create a new instance of DartServerMainTab.
   */
  public ChromeMainTab() {

  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 3).applyTo(composite);

    launchTargetGroup = new LaunchTargetComposite(composite, SWT.NONE);
    launchTargetGroup.addListener(SWT.Modify, new Listener() {

      @Override
      public void handleEvent(Event event) {
        notifyPanelChanged();
      }
    });

    // Chrome settings group
    Group group = new Group(composite, SWT.NONE);
    group.setText("Chrome settings");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
    ((GridLayout) group.getLayout()).marginBottom = 5;

    useWebComponentsButton = new Button(group, SWT.CHECK);
    useWebComponentsButton.setText("Enable experimental browser features (Web Components)");
    useWebComponentsButton.setToolTipText("--enable-experimental-webkit-features"
        + " and --enable-devtools-experiments");
    GridDataFactory.swtDefaults().span(3, 1).applyTo(useWebComponentsButton);
    useWebComponentsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });

    showOutputButton = new Button(group, SWT.CHECK);
    showOutputButton.setText("Show browser stdout and stderr output");
    GridDataFactory.swtDefaults().span(3, 1).applyTo(showOutputButton);
    showOutputButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });

    // additional browser arguments
    Label argsLabel = new Label(group, SWT.NONE);
    argsLabel.setText("Browser arguments:");

    argumentText = new Text(group, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(
        argumentText);

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {
    if (performSdkCheck() != null) {
      return performSdkCheck();
    }

    return launchTargetGroup.getErrorMessage();
  }

  @Override
  public Image getImage() {
    return SDBGDebugUIPlugin.getImage("chromium_16.png"); //$NON-NLS-1$
  }

  @Override
  public String getMessage() {
    return ChromeLaunchMessages.ChromeMainTab_Message;
  }

  @Override
  public String getName() {
    return ChromeLaunchMessages.ChromeMainTab_Name;
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    SDBGLaunchConfigWrapper dartLauncher = new SDBGLaunchConfigWrapper(configuration);

    launchTargetGroup.setHtmlTextValue(dartLauncher.appendQueryParams(dartLauncher.getApplicationName()));
    launchTargetGroup.setUrlTextValue(dartLauncher.getUrl());

    launchTargetGroup.setSourceDirectoryTextValue(dartLauncher.getSourceDirectoryName());

    if (dartLauncher.getShouldLaunchFile()) {
      launchTargetGroup.setHtmlButtonSelection(true);
    } else {
      launchTargetGroup.setHtmlButtonSelection(false);
    }

    if (showOutputButton != null) {
      showOutputButton.setSelection(dartLauncher.getShowLaunchOutput());
    }

    if (useWebComponentsButton != null) {
      useWebComponentsButton.setSelection(dartLauncher.isEnableExperimentalWebkitFeatures());
    }

    if (argumentText != null) {
      argumentText.setText(dartLauncher.getArguments());
    }
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return getErrorMessage() == null;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    SDBGLaunchConfigWrapper dartLauncher = new SDBGLaunchConfigWrapper(configuration);
    dartLauncher.setShouldLaunchFile(launchTargetGroup.getHtmlButtonSelection());

    String fileUrl = launchTargetGroup.getHtmlFileName();

    if (fileUrl.indexOf('?') == -1) {
      dartLauncher.setApplicationName(fileUrl);
      dartLauncher.setUrlQueryParams("");
    } else {
      int index = fileUrl.indexOf('?');

      dartLauncher.setApplicationName(fileUrl.substring(0, index));
      dartLauncher.setUrlQueryParams(fileUrl.substring(index + 1));
    }

    dartLauncher.setUrl(launchTargetGroup.getUrlString());
    dartLauncher.setSourceDirectoryName(launchTargetGroup.getSourceDirectory());

    if (showOutputButton != null) {
      dartLauncher.setShowLaunchOutput(showOutputButton.getSelection());
    }

    if (useWebComponentsButton != null) {
      dartLauncher.setUseWebComponents(useWebComponentsButton.getSelection());
    }
    if (argumentText != null) {
      dartLauncher.setArguments(argumentText.getText().trim());
    }
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    SDBGLaunchConfigWrapper dartLauncher = new SDBGLaunchConfigWrapper(configuration);
    dartLauncher.setShouldLaunchFile(true);
    dartLauncher.setApplicationName(""); //$NON-NLS-1$
  }

  private void notifyPanelChanged() {
    setDirty(true);

    updateLaunchConfigurationDialog();
  }

  private String performSdkCheck() {
//&&&    
//    if (!DartSdkManager.getManager().hasSdk()) {
//      return "Dartium is not installed ("
//          + DartSdkManager.getManager().getSdk().getDartiumWorkingDirectory() + ")";
//    } else {
    return null;
//    }
  }

}

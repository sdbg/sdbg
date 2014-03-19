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

package com.github.sdbg.debug.ui.internal.chromemobileconn;

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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * The main UI tab for connections to running Chrome instances.
 */
public class ChromeMobileConnMainTab extends AbstractLaunchConfigurationTab {
  private Text deviceText;
  private LaunchTargetComposite launchTargetGroup;

  protected ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  public ChromeMobileConnMainTab() {
    setMessage("Create a configuration to connect to a running Mobile Chrome");
  }

  @Override
  public void createControl(Composite parent) {
    final Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 3).applyTo(composite);

    Group group = new Group(composite, SWT.NONE);
    group.setText("Connection parameters");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.fillDefaults().numColumns(2).margins(12, 6).applyTo(group);

    Label label = new Label(group, SWT.NONE);
    label.setText("Device:");

    deviceText = new Text(group, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(deviceText);

    launchTargetGroup = new LaunchTargetComposite(composite, SWT.NONE, false/*allowHtmlFile*/);
    launchTargetGroup.addListener(SWT.Modify, new Listener() {
      @Override
      public void handleEvent(Event event) {
        notifyPanelChanged();
      }
    });

    // spacer
    label = new Label(composite, SWT.NONE);

    final Label instructionsLabel = new Label(composite, SWT.WRAP);
    instructionsLabel.setText("Make sure that \"USB debugging\" is enabled on your Android device. Then connect it to your PC via USB.");
    GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).applyTo(
        instructionsLabel);

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {
    return launchTargetGroup.getErrorMessage();
  }

  @Override
  public Image getImage() {
    return SDBGDebugUIPlugin.getImage("chrome_conn.png");
  }

  @Override
  public String getName() {
    return "Main";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    SDBGLaunchConfigWrapper chromeLauncher = new SDBGLaunchConfigWrapper(configuration);

    deviceText.setText(chromeLauncher.getDevice());
    launchTargetGroup.setUrlTextValue(chromeLauncher.getUrl());
    launchTargetGroup.setProjectTextValue(chromeLauncher.getProjectName());
    launchTargetGroup.setHtmlButtonSelection(false);
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return getErrorMessage() == null;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    SDBGLaunchConfigWrapper chromeLauncher = new SDBGLaunchConfigWrapper(configuration);

    chromeLauncher.setDevice(deviceText.getText());
    chromeLauncher.setShouldLaunchFile(false);
    chromeLauncher.setUrl(launchTargetGroup.getUrlString());
    chromeLauncher.setProjectName(launchTargetGroup.getProject());
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    SDBGLaunchConfigWrapper chromeLauncher = new SDBGLaunchConfigWrapper(configuration);
    chromeLauncher.setApplicationName("");
  }

  protected void notifyPanelChanged() {
    setDirty(true);

    updateLaunchConfigurationDialog();
  }
}

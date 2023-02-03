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

package com.github.sdbg.debug.ui.internal.chromeconn;

import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.ui.internal.Fonts;
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
public class ChromeConnMainTab extends AbstractLaunchConfigurationTab {
  private Text hostText;
  private Text portText;
  private LaunchTargetComposite launchTargetGroup;

  protected ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  public ChromeConnMainTab() {
    setMessage("Create a configuration to connect to a running Chrome");
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
    label.setText("Host:");

    hostText = new Text(group, SWT.SINGLE | SWT.BORDER);
    hostText.addModifyListener(textModifyListener);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(hostText);

    label = new Label(group, SWT.NONE);
    label.setText("Port:");

    portText = new Text(group, SWT.SINGLE | SWT.BORDER);
    portText.addModifyListener(textModifyListener);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(portText);

    launchTargetGroup = new LaunchTargetComposite(
        composite,
        SWT.NONE,
        false/*allowHtmlFile*/,
        true/*urlIsFilter*/,
        false/*launchTabInUrl*/);
    launchTargetGroup.addListener(SWT.Modify, new Listener() {
      @Override
      public void handleEvent(Event event) {
        notifyPanelChanged();
      }
    });

    final Label noteLabel = new Label(composite, SWT.WRAP);
    noteLabel.setText("Note for beginners: it is much easier to get started with the \"Launch Browser\" Debug Configuration.\nConsider switching to it.");
    noteLabel.setFont(Fonts.getFontRegistry().getBold(""));
    GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).applyTo(noteLabel);

    // spacer
    label = new Label(composite, SWT.NONE);

    final Label instructionsLabel = new Label(composite, SWT.WRAP);
    instructionsLabel.setText("To start Chromium, Chrome, Edge with remote connections enabled, use the following flag(s):\n--remote-debugging-port=<port> [--user-data-dir=<remote-profile>]");
    GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).applyTo(
        instructionsLabel);
    final Label instructionsLabel2 = new Label(composite, SWT.WRAP);
    instructionsLabel2.setText("To start Firefox with remote connections enabled, use the following flag(s):\n--start-debugger-server=<port> [--profile=<remote-profile>]"
        + ". Debugging must be enabled in about:config");
    GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).applyTo(
        instructionsLabel2);

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {
    if (hostText.getText().length() == 0) {
      return "Host is not specified";
    }

    if (portText.getText().length() == 0) {
      return "Port is not specified";
    }

    try {
      Integer.parseInt(portText.getText());
    } catch (Exception e) {
      return e.getMessage();
    }

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

    hostText.setText(chromeLauncher.getConnectionHost());
    portText.setText(Integer.toString(chromeLauncher.getConnectionPort()));
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

    chromeLauncher.setConnectionHost(hostText.getText());
    chromeLauncher.setConnectionPort(Integer.parseInt(portText.getText()));
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

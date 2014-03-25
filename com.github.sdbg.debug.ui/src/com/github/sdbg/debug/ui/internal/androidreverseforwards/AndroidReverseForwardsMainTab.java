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

package com.github.sdbg.debug.ui.internal.androidreverseforwards;

import com.github.sdbg.debug.core.SDBGReverseForwardsLaunchConfigWrapper;
import com.github.sdbg.debug.ui.internal.SDBGDebugUIPlugin;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The main UI tab for performing reverse port forwards on Android.
 */
public class AndroidReverseForwardsMainTab extends AbstractLaunchConfigurationTab {
  private static final int MAX_FORWARDS = 5; // for now; results in a simpler user interface (no list views, add/remove etc.

  private Text deviceText;
  private Text[] hostTexts = new Text[MAX_FORWARDS], portTexts = new Text[MAX_FORWARDS],
      devicePortTexts = new Text[MAX_FORWARDS];

  protected ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  public AndroidReverseForwardsMainTab() {
    setMessage("Create a configuration to run port forwards on Android devices");
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
    deviceText.addModifyListener(textModifyListener);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(deviceText);

    Group pfGroup = new Group(composite, SWT.NONE);
    pfGroup.setText("Forwarding rules");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(pfGroup);
    GridLayoutFactory.fillDefaults().numColumns(8).margins(12, 6).applyTo(pfGroup);

    label = new Label(pfGroup, SWT.NONE);

    Composite columnLabel = new Composite(pfGroup, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(1).applyTo(columnLabel);
    GridDataFactory.fillDefaults().span(3, 1).grab(true, false).applyTo(columnLabel);

    label = new Label(columnLabel, SWT.NONE);
    label.setText("Android Device");

    label = new Label(columnLabel, SWT.HORIZONTAL | SWT.SEPARATOR);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

    label = new Label(pfGroup, SWT.NONE);

    columnLabel = new Composite(pfGroup, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(1).applyTo(columnLabel);
    GridDataFactory.fillDefaults().span(3, 1).grab(true, false).applyTo(columnLabel);

    label = new Label(columnLabel, SWT.NONE);
    label.setText("Website");

    label = new Label(columnLabel, SWT.HORIZONTAL | SWT.SEPARATOR);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

    for (int i = 0; i < MAX_FORWARDS; i++) {
      label = new Label(pfGroup, SWT.NONE);
      label.setText((i + 1) + ": ");

      Text deviceHostText = new Text(pfGroup, SWT.SINGLE | SWT.BORDER);
      deviceHostText.setEnabled(false);
      deviceHostText.setText("localhost");
      GridDataFactory.fillDefaults().grab(true, false).applyTo(deviceHostText);

      label = new Label(pfGroup, SWT.NONE);
      label.setText(":");

      devicePortTexts[i] = new Text(pfGroup, SWT.SINGLE | SWT.BORDER);
      devicePortTexts[i].addModifyListener(textModifyListener);
      GridDataFactory.swtDefaults().hint(40, -1).applyTo(devicePortTexts[i]);

      label = new Label(pfGroup, SWT.NONE);
      label.setText("->");
      GridDataFactory.swtDefaults().hint(20, -1).applyTo(label);

      hostTexts[i] = new Text(pfGroup, SWT.SINGLE | SWT.BORDER);
      hostTexts[i].addModifyListener(textModifyListener);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(hostTexts[i]);

      label = new Label(pfGroup, SWT.NONE);
      label.setText(":");

      portTexts[i] = new Text(pfGroup, SWT.SINGLE | SWT.BORDER);
      portTexts[i].addModifyListener(textModifyListener);
      GridDataFactory.swtDefaults().hint(40, -1).applyTo(portTexts[i]);
    }

    final Label instructionsLabel = new Label(composite, SWT.WRAP);
    instructionsLabel.setText("Make sure that \"USB debugging\" is enabled on your Android device. Then connect it to your PC via USB.");
    GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).applyTo(
        instructionsLabel);

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {
    String message = null;

    for (int i = 0; i < MAX_FORWARDS; i++) {
      String host = hostTexts[i].getText().trim();
      String portStr = portTexts[i].getText().trim();
      String devicePortStr = devicePortTexts[i].getText().trim();
      if (host.length() > 0 || portStr.length() > 0 || devicePortStr.length() > 0) {
        if (host.length() == 0) {
          message = "Enter host";
        } else if (portStr.length() == 0) {
          message = "Enter port";
        } else if (devicePortStr.length() == 0) {
          message = "Enter device port";
        } else {
          try {
            Integer.parseInt(portStr);
          } catch (Exception e) {
            message = "Invalid port: " + e.getMessage();
          }

          if (message == null) {
            try {
              Integer.parseInt(devicePortStr);
            } catch (Exception e) {
              message = "Invalid device port: " + e.getMessage();
            }
          }
        }

        if (message != null) {
          message = "Rule " + (i + 1) + ": " + message;
          break;
        }
      }
    }

    return message;
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
    SDBGReverseForwardsLaunchConfigWrapper launchConfig = new SDBGReverseForwardsLaunchConfigWrapper(
        configuration);

    deviceText.setText(launchConfig.getDevice());
    List<String> rules = launchConfig.getReverseForwards();
    for (int i = 0; i < MAX_FORWARDS; i++) {
      if (i < rules.size()) {
        String rule = rules.get(i);
        hostTexts[i].setText(SDBGReverseForwardsLaunchConfigWrapper.getReverseForwardHost(rule));
        portTexts[i].setText(Integer.toString(SDBGReverseForwardsLaunchConfigWrapper.getReverseForwardPort(rule)));
        devicePortTexts[i].setText(Integer.toString(SDBGReverseForwardsLaunchConfigWrapper.getReverseForwardDevicePort(rule)));
      } else {
        hostTexts[i].setText("");
        portTexts[i].setText("");
        devicePortTexts[i].setText("");
      }
    }
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return getErrorMessage() == null;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    SDBGReverseForwardsLaunchConfigWrapper launchConfig = new SDBGReverseForwardsLaunchConfigWrapper(
        configuration);

    launchConfig.setDevice(deviceText.getText());
    List<String> rules = new ArrayList<String>();
    for (int i = 0; i < MAX_FORWARDS; i++) {
      String host = hostTexts[i].getText().trim();
      String portStr = portTexts[i].getText().trim();
      String devicePortStr = devicePortTexts[i].getText().trim();
      if (host.length() > 0 || portStr.length() > 0 || devicePortStr.length() > 0) {
        int port;
        try {
          port = Integer.parseInt(portStr);
        } catch (Exception e) {
          port = -1;
        }

        int devicePort;
        try {
          devicePort = Integer.parseInt(devicePortStr);
        } catch (Exception e) {
          devicePort = -1;
        }

        rules.add(SDBGReverseForwardsLaunchConfigWrapper.getReverseForward(host, port, devicePort));
      }
    }

    launchConfig.setReverseForwards(rules);
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    SDBGReverseForwardsLaunchConfigWrapper launchConfig = new SDBGReverseForwardsLaunchConfigWrapper(
        configuration);
    launchConfig.setDevice("");
    launchConfig.setReverseForwardsStr("");
  }

  protected void notifyPanelChanged() {
    setDirty(true);

    updateLaunchConfigurationDialog();
  }
}

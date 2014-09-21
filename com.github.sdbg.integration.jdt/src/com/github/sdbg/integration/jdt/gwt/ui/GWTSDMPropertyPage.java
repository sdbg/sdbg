package com.github.sdbg.integration.jdt.gwt.ui;

import com.github.sdbg.integration.jdt.gwt.GWTSDMProperties;
import com.github.sdbg.integration.jdt.gwt.GWTSDMProperties.HotCodeReplacePolicy;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class GWTSDMPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
  private Button recompileEnabledCheck;
  private Text codeServerHostText;
  private Text codeServerPortText;
  private Text moduleNamesText;
  private Button[] hotCodeReplacePolicyRadio;

  public GWTSDMPropertyPage() {
  }

  @Override
  public String getErrorMessage() {
    if (recompileEnabledCheck.getSelection()) {
      if (codeServerHostText.getText().trim().length() == 0) {
        return "Code Server Host cannot be empty";
      } else if (codeServerPortText.getText().trim().length() == 0) {
        return "Code Server Port cannot be empty";
      } else if (moduleNamesText.getText().trim().length() == 0) {
        return "At least one GWT Module should be specified";
      } else {
        try {
          int port = Integer.parseInt(codeServerPortText.getText().trim());
          if (port < 0) {
            return "Code Server Port cannot be less than 0";
          } else if (port > 65535) {
            return "Code Server Port cannot be greater than less than 65535";
          }
        } catch (Exception e) {
          return "Invalid Code Server Port number: " + e.getMessage();
        }
      }
    }

    return null;
  }

  @Override
  public boolean isValid() {
    return getErrorMessage() == null;
  }

  @Override
  public boolean performOk() {
    applyChanges();
    return super.performOk();
  }

  @Override
  protected Control createContents(Composite parent) {
    try {
      Composite contents = new Composite(parent, SWT.NONE);
      contents.setLayout(GridLayoutFactory.swtDefaults().numColumns(1).create());

      GWTSDMProperties properties = getProperties();

      recompileEnabledCheck = new Button(contents, SWT.CHECK);
      recompileEnabledCheck.setText("Enable automatic GWT SDM recompilation during project build");
      recompileEnabledCheck.setLayoutData(GridDataFactory.swtDefaults().span(2, 1).create());
      recompileEnabledCheck.setSelection(properties.isRecompileEnabled());
      recompileEnabledCheck.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          updateControls();
          validate();
        }
      });

      Group codeServerGroup = new Group(contents, SWT.NONE);
      codeServerGroup.setText("Code Server");
      codeServerGroup.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
      codeServerGroup.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(
          true,
          false).create());

      Label label = new Label(codeServerGroup, SWT.NONE);
      label.setLayoutData(new GridData());
      label.setText("Host: ");

      codeServerHostText = new Text(codeServerGroup, SWT.BORDER);
      codeServerHostText.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(
          true,
          false).create());
      codeServerHostText.setText(properties.getCodeServerHost());
      codeServerHostText.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent event) {
          validate();
        }
      });

      label = new Label(codeServerGroup, SWT.NONE);
      label.setLayoutData(new GridData());
      label.setText("Port: ");

      codeServerPortText = new Text(codeServerGroup, SWT.BORDER);
      codeServerPortText.setLayoutData(GridDataFactory.swtDefaults().hint(60, SWT.DEFAULT).create());
      codeServerPortText.setText(Integer.toString(properties.getCodeServerPort()));
      codeServerPortText.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent event) {
          validate();
        }
      });

      Group gwtModulesGroup = new Group(contents, SWT.NONE);
      gwtModulesGroup.setText("GWT Modules");
      gwtModulesGroup.setLayout(GridLayoutFactory.swtDefaults().numColumns(1).create());
      gwtModulesGroup.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(
          true,
          false).create());

      moduleNamesText = new Text(gwtModulesGroup, SWT.BORDER);
      moduleNamesText.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(
          true,
          false).create());
      moduleNamesText.setText(properties.getModuleNames());
      moduleNamesText.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent event) {
          validate();
        }
      });

      label = new Label(gwtModulesGroup, SWT.WRAP);
      label.setLayoutData(GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.TOP).create());
      label.setText("(a comma-separated list of module names)");

      Group hotCodeReplacePolicyGroup = new Group(contents, SWT.NONE);
      hotCodeReplacePolicyGroup.setText("Hot Code Replace Policy");
      hotCodeReplacePolicyGroup.setLayout(GridLayoutFactory.swtDefaults().numColumns(1).create());
      hotCodeReplacePolicyGroup.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(
          true,
          false).create());

      hotCodeReplacePolicyRadio = new Button[HotCodeReplacePolicy.values().length];
      int count = 0;
      for (HotCodeReplacePolicy policy : HotCodeReplacePolicy.values()) {
        Button radio = new Button(hotCodeReplacePolicyGroup, SWT.RADIO);
        radio.setText(policy.getDescription());
        radio.setData(HotCodeReplacePolicy.class.getName(), policy);
        radio.setLayoutData(GridDataFactory.swtDefaults().span(2, 1).create());
        radio.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            validate();
          }
        });

        hotCodeReplacePolicyRadio[count++] = radio;
      }

      for (Button radio : hotCodeReplacePolicyRadio) {
        if (radio.getData(HotCodeReplacePolicy.class.getName()) == properties.getHotCodeReplacePolicy()) {
          radio.setSelection(true);
        }
      }

      label = new Label(hotCodeReplacePolicyGroup, SWT.WRAP);
      label.setLayoutData(GridDataFactory.swtDefaults().hint(400, SWT.DEFAULT).create());
      label.setText("\nNote:"
          + "\n- For Hot Code Replace to work, you need an active SDBG debug session"
          + "\n- Chrome Live Edit will likely crash your browser with anything but the smallest GWT project");

      updateControls();
      validate();

      return contents;
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void performApply() {
    applyChanges();
    super.performApply();
  }

  @Override
  protected void performDefaults() {
    recompileEnabledCheck.setSelection(GWTSDMProperties.DEFVALUE_RECOMPILE_ENABLED);
    codeServerHostText.setText(GWTSDMProperties.DEFVALUE_CODE_SERVER_HOST);
    codeServerPortText.setText(Integer.toString(GWTSDMProperties.DEFVALUE_CODE_SERVER_PORT));
    moduleNamesText.setText(GWTSDMProperties.DEFVALUE_MODULE_NAMES);
    for (Button radio : hotCodeReplacePolicyRadio) {
      if (radio.getData(HotCodeReplacePolicy.class.getName()) == GWTSDMProperties.DEFVALUE_HOT_CODE_REPLACE_POLICY) {
        radio.setSelection(true);
      }
    }

    updateControls();

    super.performDefaults();
  }

  private void applyChanges() {
    try {
      GWTSDMProperties properties = getProperties();
      properties.setRecompileEnabled(recompileEnabledCheck.getSelection());
      properties.setCodeServerHost(codeServerHostText.getText().trim());
      properties.setCodeServerPort(Integer.parseInt(codeServerPortText.getText().trim()));
      properties.setModuleNames(moduleNamesText.getText().trim());
      for (Button radio : hotCodeReplacePolicyRadio) {
        if (radio.getSelection()) {
          properties.setHotCodeReplacePolicy((HotCodeReplacePolicy) radio.getData(HotCodeReplacePolicy.class.getName()));
        }
      }
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
  }

  private GWTSDMProperties getProperties() {
    return new GWTSDMProperties((IProject) getElement());
  }

  private void updateControls() {
    boolean enabled = recompileEnabledCheck.getSelection();
    codeServerHostText.setEnabled(enabled);
    codeServerPortText.setEnabled(enabled);
    moduleNamesText.setEnabled(enabled);
    for (Button radio : hotCodeReplacePolicyRadio) {
      radio.setEnabled(enabled);
    }
  }

  private void validate() {
    setErrorMessage(getErrorMessage());
    setValid(isValid());
    //updateApplyButton();
  }
}

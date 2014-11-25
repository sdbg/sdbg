package com.github.sdbg.integration.jdt.gwt;

import com.github.sdbg.integration.jdt.SDBGJDTIntegrationPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

public class GWTSDMProperties {
  public static enum HotCodeReplacePolicy {
    DISABLED("Disabled"),
    RELOAD_PAGE("Reload the page hosting the module"),
    CHROME_LIVE_EDIT("(EXPERIMENTAL) Upload the recompiled module script via Chrome LiveEdit");

    private String description;

    private HotCodeReplacePolicy(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  private static final QualifiedName PROPERTY_RECOMPILE_ENABLED = new QualifiedName(
      SDBGJDTIntegrationPlugin.PLUGIN_ID,
      "recompileEnabled"), PROPERTY_CODE_SERVER_HOST = new QualifiedName(
      SDBGJDTIntegrationPlugin.PLUGIN_ID,
      "codeServerHost"), PROPERTY_CODE_SERVER_PORT = new QualifiedName(
      SDBGJDTIntegrationPlugin.PLUGIN_ID,
      "codeServerPort"), PROPERTY_MODULE_NAMES = new QualifiedName(
      SDBGJDTIntegrationPlugin.PLUGIN_ID,
      "moduleName"), PROPERTY_HOT_CODE_REPLACE_POLICY = new QualifiedName(
      SDBGJDTIntegrationPlugin.PLUGIN_ID,
      "hotCodeReplacePolicy");

  public static final String DEFVALUE_CODE_SERVER_HOST = "localhost", DEFVALUE_MODULE_NAMES = "";
  public static final boolean DEFVALUE_RECOMPILE_ENABLED = false;
  public static final int DEFVALUE_CODE_SERVER_PORT = 9876;
  public static final HotCodeReplacePolicy DEFVALUE_HOT_CODE_REPLACE_POLICY = HotCodeReplacePolicy.DISABLED;

  private IProject project;

  public GWTSDMProperties(IProject project) {
    this.project = project;
  }

  public String getCodeServerHost() throws CoreException {
    return getProperty(PROPERTY_CODE_SERVER_HOST, DEFVALUE_CODE_SERVER_HOST);
  }

  public int getCodeServerPort() throws CoreException {
    return Integer.parseInt(getProperty(
        PROPERTY_CODE_SERVER_PORT,
        Integer.toString(DEFVALUE_CODE_SERVER_PORT)));
  }

  public HotCodeReplacePolicy getHotCodeReplacePolicy() throws CoreException {
    try {
      return HotCodeReplacePolicy.valueOf(getProperty(
          PROPERTY_HOT_CODE_REPLACE_POLICY,
          DEFVALUE_HOT_CODE_REPLACE_POLICY.toString()));
    } catch (Exception e) {
      return DEFVALUE_HOT_CODE_REPLACE_POLICY;
    }
  }

  public String getModuleNames() throws CoreException {
    return getProperty(PROPERTY_MODULE_NAMES, DEFVALUE_MODULE_NAMES);
  }

  public boolean isRecompileEnabled() throws CoreException {
    return Boolean.parseBoolean(getProperty(
        PROPERTY_RECOMPILE_ENABLED,
        Boolean.toString(DEFVALUE_RECOMPILE_ENABLED)));
  }

  public void setCodeServerHost(String value) throws CoreException {
    setProperty(PROPERTY_CODE_SERVER_HOST, value);
  }

  public void setCodeServerPort(Integer value) throws CoreException {
    setProperty(PROPERTY_CODE_SERVER_PORT, value != null ? value.toString() : null);
  }

  public void setHotCodeReplacePolicy(HotCodeReplacePolicy value) throws CoreException {
    setProperty(PROPERTY_HOT_CODE_REPLACE_POLICY, value != null ? value.toString() : null);
  }

  public void setModuleNames(String value) throws CoreException {
    setProperty(PROPERTY_MODULE_NAMES, value);
  }

  public void setRecompileEnabled(Boolean value) throws CoreException {
    setProperty(PROPERTY_RECOMPILE_ENABLED, value != null ? value.toString() : null);
  }

  private String getProperty(QualifiedName name, String defaultValue) throws CoreException {
    String value = project.getPersistentProperty(name);
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  private void setProperty(QualifiedName name, String value) throws CoreException {
    project.setPersistentProperty(name, value);
  }
}

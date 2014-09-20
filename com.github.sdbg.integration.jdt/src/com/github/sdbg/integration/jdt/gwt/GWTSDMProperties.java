package com.github.sdbg.integration.jdt.gwt;

import com.github.sdbg.integration.jdt.SDBGJDTIntegrationPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

public class GWTSDMProperties {
  private static final QualifiedName PROPERTY_RECOMPILE_ENABLED = new QualifiedName(
      SDBGJDTIntegrationPlugin.PLUGIN_ID,
      "recompileEnabled"), PROPERTY_CODE_SERVER_HOST = new QualifiedName(
      SDBGJDTIntegrationPlugin.PLUGIN_ID,
      "codeServerHost"), PROPERTY_CODE_SERVER_PORT = new QualifiedName(
      SDBGJDTIntegrationPlugin.PLUGIN_ID,
      "codeServerPort"), PROPERTY_MODULE_NAME = new QualifiedName(
      SDBGJDTIntegrationPlugin.PLUGIN_ID,
      "moduleName"), PROPERTY_CHROME_LIVE_EDIT_ENABLED = new QualifiedName(
      SDBGJDTIntegrationPlugin.PLUGIN_ID,
      "chromeLiveEditEnabled");

  public static final String DEFVALUE_CODE_SERVER_HOST = "localhost", DEFVALUE_MODULE_NAME = "";

  public static final boolean DEFVALUE_RECOMPILE_ENABLED = false,
      DEFVALUE_CHROME_LIVE_EDIT_ENABLED = false;

  public static final int DEFVALUE_CODE_SERVER_PORT = 9996;

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

  public String getModuleName() throws CoreException {
    return getProperty(PROPERTY_MODULE_NAME, DEFVALUE_MODULE_NAME);
  }

  public boolean isChromeLiveEditEnabled() throws CoreException {
    return Boolean.parseBoolean(getProperty(
        PROPERTY_CHROME_LIVE_EDIT_ENABLED,
        Boolean.toString(DEFVALUE_CHROME_LIVE_EDIT_ENABLED)));
  }

  public boolean isRecompileEnabled() throws CoreException {
    return Boolean.parseBoolean(getProperty(
        PROPERTY_RECOMPILE_ENABLED,
        Boolean.toString(DEFVALUE_RECOMPILE_ENABLED)));
  }

  public void setChromeLiveEditEnabled(boolean value) throws CoreException {
    setProperty(PROPERTY_CHROME_LIVE_EDIT_ENABLED, Boolean.toString(value));
  }

  public void setCodeServerHost(String value) throws CoreException {
    setProperty(PROPERTY_CODE_SERVER_HOST, value);
  }

  public void setCodeServerPort(int value) throws CoreException {
    setProperty(PROPERTY_CODE_SERVER_PORT, Integer.toString(value));
  }

  public void setModuleName(String value) throws CoreException {
    setProperty(PROPERTY_MODULE_NAME, value);
  }

  public void setRecompileEnabled(boolean value) throws CoreException {
    setProperty(PROPERTY_RECOMPILE_ENABLED, Boolean.toString(value));
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

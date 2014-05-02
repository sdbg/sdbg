package com.github.sdbg.integration.jdt;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardSourcePathProvider;

public class JDTSourcePathProvider extends StandardSourcePathProvider {
  public JDTSourcePathProvider() {
  }

  @Override
  public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration)
      throws CoreException {
    IJavaProject proj = getJavaProject(configuration);
    if (proj != null) {
      IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedRuntimeClasspath(proj);
      // TODO: Rather than replacing the JRE, *remove* the JRE from the classpath, as it does not make sense in a GWT setup
      //    // replace project JRE with config's JRE
      //    IRuntimeClasspathEntry projEntry = JavaRuntime.computeJREEntry(proj);
      //    if (jreEntry != null && projEntry != null) {
      //      if (!jreEntry.equals(projEntry)) {
      //        for (int i = 0; i < entries.length; i++) {
      //          IRuntimeClasspathEntry entry = entries[i];
      //          if (entry.equals(projEntry)) {
      //            entries[i] = jreEntry;
      //            return entries;
      //          }
      //        }
      //      }
      //    }
      return entries;
    } else {
      return new IRuntimeClasspathEntry[0];
    }
  }

  private IJavaProject getJavaProject(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    SDBGLaunchConfigWrapper conf = new SDBGLaunchConfigWrapper(launchConfiguration);

    IProject project = conf.getProject();
    if (project == null) {
      return null;
    }

    IJavaProject javaProject = JavaCore.create(project);
    assert javaProject != null;
    if (javaProject.getProject().exists() && !javaProject.getProject().isOpen()) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          0,
          "Project is closed",
          null));
    }

    if (!javaProject.exists()) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          0,
          "Project does not exist",
          null));
    }

    return javaProject;
  }
}

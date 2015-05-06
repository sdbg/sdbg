package com.github.sdbg.debug;

import com.github.sdbg.debug.ui.internal.chrome.ChromeLaunchShortcut;
import com.google.gdt.eclipse.core.sdbg.IDebugLaunch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class DebugLaunch implements IDebugLaunch {

  @Override
  public boolean hasChromeLauncher() {
    return true;
  }

  @Override
  public void launchChrome(IProject project, String mode, String url) {
    IJavaProject javaProject = JavaCore.create(project);
    IResource resource = javaProject.getResource();

    new ChromeLaunchShortcut().launch(resource, mode, url);
  }

}

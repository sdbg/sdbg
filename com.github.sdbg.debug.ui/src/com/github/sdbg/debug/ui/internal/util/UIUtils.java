package com.github.sdbg.debug.ui.internal.util;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class UIUtils {
  public static IWorkbenchPage getActivePage() {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      return null;
    }
    return window.getActivePage();
  }

  public static Shell getActiveWorkbenchShell() {
    IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
    if (window != null) {
      return window.getShell();
    }
    return null;
  }

  public static IWorkbenchWindow getActiveWorkbenchWindow() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
  }

  private UIUtils() {
  }
}

package com.github.sdbg.debug.ui.internal.util;

import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

public class UIBrowserTabChooser implements IBrowserTabChooser {
  private static class TabLabelProvider extends LabelProvider {
    @Override
    public Image getImage(Object element) {
      return null;
    }

    @Override
    public String getText(Object element) {
      if (element instanceof IBrowserTabInfo) {
        IBrowserTabInfo tab = (IBrowserTabInfo) element;

        String text = tab.getTitle();
        String url = tab.getUrl();

        if (tab.getUrl() != null && tab.getUrl().length() > 0 && !text.equalsIgnoreCase(url)) {
          text += " (" + url + ")";
        }

        return text;
      } else {
        return null;
      }
    }
  }

  public UIBrowserTabChooser() {
  }

  @Override
  public IBrowserTabInfo chooseTab(final List<? extends IBrowserTabInfo> tabs) {
    final IBrowserTabInfo[] result = new IBrowserTabInfo[1];
    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        ListDialog dlg = new ListDialog(
            PlatformUI.getWorkbench().getWorkbenchWindows()[0].getShell());
        dlg.setInput(tabs);
        dlg.setTitle("Connect to a running Chrome");
        dlg.setMessage("Select a tab for remote connection");
        dlg.setContentProvider(new ArrayContentProvider());
        dlg.setLabelProvider(new TabLabelProvider());
        if (dlg.open() == Window.OK) {
          result[0] = (IBrowserTabInfo) dlg.getResult()[0];
        }
      }
    });

    if (result[0] != null) {
      return result[0];
    } else {
      return null;
    }
  }
}

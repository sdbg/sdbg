package com.github.sdbg.debug.ui.internal.util;

import com.github.sdbg.debug.core.util.IDeviceChooser;
import com.github.sdbg.debug.core.util.IDeviceInfo;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

public class UIDeviceChooser implements IDeviceChooser {
  private static class DeviceLabelProvider extends LabelProvider {
    @Override
    public Image getImage(Object element) {
      return null;
    }

    @Override
    public String getText(Object element) {
      if (element instanceof IDeviceInfo) {
        IDeviceInfo device = (IDeviceInfo) element;
        return device.getName() + " (" + device.getId().toUpperCase() + ")";
      } else {
        return null;
      }
    }
  }

  public UIDeviceChooser() {
  }

  @Override
  public IDeviceInfo chooseDevice(final List<? extends IDeviceInfo> devices) {
    final IDeviceInfo[] result = new IDeviceInfo[1];
    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        ListDialog dlg = new ListDialog(
            PlatformUI.getWorkbench().getWorkbenchWindows()[0].getShell());
        dlg.setInput(devices);
        dlg.setTitle("Connect to a device");
        dlg.setMessage("Select a device for remote connection");
        dlg.setContentProvider(new ArrayContentProvider());
        dlg.setLabelProvider(new DeviceLabelProvider());
        if (dlg.open() == Window.OK) {
          result[0] = (IDeviceInfo) dlg.getResult()[0];
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

package com.github.sdbg.debug.core.internal.util;

import com.github.sdbg.debug.core.util.IDeviceChooser;
import com.github.sdbg.debug.core.util.IDeviceInfo;

import java.util.ArrayList;
import java.util.List;

public class IDFilterDeviceChooser implements IDeviceChooser {
  private String idFilter;
  private IDeviceChooser delegate;

  public IDFilterDeviceChooser(String idFilter, IDeviceChooser delegate) {
    this.delegate = delegate;
    this.idFilter = idFilter;
  }

  @Override
  public IDeviceInfo chooseDevice(List<? extends IDeviceInfo> devices) {
    if (idFilter != null && idFilter.length() > 0) {
      List<IDeviceInfo> newDevices = new ArrayList<IDeviceInfo>();
      for (IDeviceInfo device : devices) {
        if (device.getId() != null && device.getId().toLowerCase().contains(idFilter.toLowerCase())) {
          newDevices.add(device);
        }
      }

      devices = newDevices;
    }

    if (devices.isEmpty()) {
      return null;
    } else if (devices.size() == 1) {
      return devices.get(0);
    } else if (delegate != null) {
      return delegate.chooseDevice(devices);
    } else {
      // Just choose the first one matching the criteria 
      return devices.get(0);
    }
  }
}

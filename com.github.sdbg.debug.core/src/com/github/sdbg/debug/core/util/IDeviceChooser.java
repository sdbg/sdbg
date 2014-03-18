package com.github.sdbg.debug.core.util;

import java.util.List;

public interface IDeviceChooser {
  IDeviceInfo chooseDevice(List<? extends IDeviceInfo> devices);
}

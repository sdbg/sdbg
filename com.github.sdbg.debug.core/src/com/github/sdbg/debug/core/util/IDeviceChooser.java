package com.github.sdbg.debug.core.util;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

public interface IDeviceChooser {
  IDeviceInfo chooseDevice(List<? extends IDeviceInfo> devices) throws CoreException;
}

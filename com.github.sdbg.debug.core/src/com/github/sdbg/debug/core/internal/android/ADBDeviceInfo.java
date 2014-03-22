package com.github.sdbg.debug.core.internal.android;

import com.github.sdbg.debug.core.util.IDeviceInfo;

public class ADBDeviceInfo implements IDeviceInfo {
  private String id;
  private String name;

  public ADBDeviceInfo(String id, String name) {
    this.id = id;
    this.name = name;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }
}

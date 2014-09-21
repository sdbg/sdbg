package com.github.sdbg.debug.core.model;

public interface IDOMResourceReference {
  public static enum Type {
    SCRIPT,
    CSS,
    ROOT
  };

  String getId();

  Type getType();

  String getUrl();
}

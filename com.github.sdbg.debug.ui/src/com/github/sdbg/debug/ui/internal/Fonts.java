package com.github.sdbg.debug.ui.internal;

import org.eclipse.jface.resource.FontRegistry;

public class Fonts {
  private static FontRegistry fontRegistry;

  public static FontRegistry getFontRegistry() {
    if (fontRegistry == null) {
      fontRegistry = new FontRegistry();
    }

    return fontRegistry;
  }

  private Fonts() {
  }
}

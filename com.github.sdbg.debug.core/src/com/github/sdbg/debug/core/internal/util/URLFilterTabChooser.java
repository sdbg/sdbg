package com.github.sdbg.debug.core.internal.util;

import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;

import java.util.ArrayList;
import java.util.List;

public class URLFilterTabChooser implements IBrowserTabChooser {
  private String urlFilter;
  private IBrowserTabChooser delegate;

  public URLFilterTabChooser(String urlFilter, IBrowserTabChooser delegate) {
    this.delegate = delegate;
    this.urlFilter = urlFilter;
  }

  @Override
  public IBrowserTabInfo chooseTab(List<? extends IBrowserTabInfo> tabs) {
    if (urlFilter != null && urlFilter.length() > 0) {
      List<IBrowserTabInfo> newTabs = new ArrayList<IBrowserTabInfo>();
      for (IBrowserTabInfo tab : tabs) {
        if (tab.getUrl() != null && tab.getUrl().toLowerCase().contains(urlFilter.toLowerCase())) {
          newTabs.add(tab);
        }
      }

      tabs = newTabs;
    }

    if (tabs.isEmpty()) {
      return null;
    } else if (tabs.size() == 1) {
      return tabs.get(0);
    } else if (delegate != null) {
      return delegate.chooseTab(tabs);
    } else {
      // Just choose the first one matching the criteria 
      return tabs.get(0);
    }
  }
}

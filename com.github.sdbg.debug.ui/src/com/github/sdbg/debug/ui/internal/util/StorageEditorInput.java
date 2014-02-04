package com.github.sdbg.debug.ui.internal.util;

import com.github.sdbg.debug.ui.internal.SDBGDebugUIPlugin;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public abstract class StorageEditorInput extends PlatformObject implements IStorageEditorInput {

  /**
   * Storage associated with this editor input
   */
  private IStorage fStorage;

  /**
   * Constructs an editor input on the given storage
   */
  public StorageEditorInput(IStorage storage) {
    fStorage = storage;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object object) {
    return object instanceof StorageEditorInput
        && getStorage().equals(((StorageEditorInput) object).getStorage());
  }

  /**
   * @see IEditorInput#getImageDescriptor()
   */
  @Override
  public ImageDescriptor getImageDescriptor() {
    return SDBGDebugUIPlugin.getImageDescriptor("obj16/remoteDartFile.png");
    //return JavaUI.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_CUNIT);
  }

  /**
   * @see IEditorInput#getName()
   */
  @Override
  public String getName() {
    return getStorage().getName();
  }

  /**
   * @see IEditorInput#getPersistable()
   */
  @Override
  public IPersistableElement getPersistable() {
    return null;
  }

  /**
   * @see IStorageEditorInput#getStorage()
   */
  @Override
  public IStorage getStorage() {
    return fStorage;
  }

  /**
   * @see IEditorInput#getToolTipText()
   */
  @Override
  public String getToolTipText() {
    return getStorage().getFullPath().toOSString();
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getStorage().hashCode();
  }

}

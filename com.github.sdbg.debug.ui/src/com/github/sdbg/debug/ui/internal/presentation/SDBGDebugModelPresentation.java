/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.sdbg.debug.ui.internal.presentation;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.breakpoints.SDBGBreakpoint;
import com.github.sdbg.debug.core.model.IExceptionStackFrame;
import com.github.sdbg.debug.core.model.ISDBGLogicalStructureTypeExtensions;
import com.github.sdbg.debug.core.model.ISDBGStackFrame;
import com.github.sdbg.debug.core.model.ISDBGValue;
import com.github.sdbg.debug.core.model.ISDBGVariable;
import com.github.sdbg.debug.core.util.SDBGNoSourceFoundElement;
import com.github.sdbg.debug.ui.internal.DartUtil;
import com.github.sdbg.debug.ui.internal.SDBGDebugUIPlugin;
import com.github.sdbg.debug.ui.internal.util.DebuggerEditorInput;
import com.github.sdbg.debug.ui.internal.util.StorageEditorInput;
import com.github.sdbg.utilities.Streams;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.debug.core.sourcelookup.containers.ZipEntryStorage;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IInstructionPointerPresentation;
import org.eclipse.debug.ui.ISourcePresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

/**
 * A debug model presentation is responsible for providing labels, images, and editors associated
 * with debug elements in a specific debug model.
 */
@SuppressWarnings("restriction")
public class SDBGDebugModelPresentation implements IDebugModelPresentation,
    IInstructionPointerPresentation {
  private static final String BREAK_ON_EXCEPTION_ANNOTAION = "org.eclipse.debug.ui.currentIPEx";
  private static final LogicalValueProvider LOGICAL_VALUE_PROVIDER = new LogicalValueProvider();

  private List<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

  private Collection<ISourcePresentation> sourcePresentations;

  public SDBGDebugModelPresentation() {

  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    listeners.add(listener);
  }

  /**
   * Computes a detailed description of the given value, reporting the result to the specified
   * listener. This allows a presentation to provide extra details about a selected value in the
   * variable detail portion of the variables view. Since this can be a long-running operation, the
   * details are reported back to the specified listener asynchronously. If <code>null</code> is
   * reported, the value's value string is displayed (<code>IValue.getValueString()</code>).
   * 
   * @param value the value for which a detailed description is required
   * @param listener the listener to report the details to asynchronously
   */
  @Override
  public void computeDetail(final IValue value, final IValueDetailListener listener) {
    if (value instanceof ISDBGValue) {
      ISDBGValue debugValue = (ISDBGValue) value;

      debugValue.computeDetail(new ISDBGValue.IValueCallback() {
        @Override
        public void detailComputed(String stringValue) {
          listener.detailComputed(value, stringValue);
        }
      });
    } else {
      listener.detailComputed(value, null);
    }
  }

  @Override
  public void dispose() {

  }

  @Override
  public String getEditorId(IEditorInput input, Object element) {
    if (input instanceof SDBGSourceNotFoundEditorInput) {
      return SDBGSourceNotFoundEditor.EDITOR_ID;
    }

    for (ISourcePresentation sourcePresentation : getSourcePresentations()) {
      String editorId = sourcePresentation.getEditorId(input, element);
      if (editorId != null) {
        return editorId;
      }
    }

    try {
      return IDE.getEditorDescriptor(input.getName()).getId();
    } catch (PartInitException e) {
      return null;
    }
  }

  @Override
  public IEditorInput getEditorInput(Object element) {
    for (ISourcePresentation sourcePresentation : getSourcePresentations()) {
      IEditorInput result = sourcePresentation.getEditorInput(element);
      if (result != null) {
        return result;
      }
    }

    if (element instanceof IMarker) {
      element = DebugPlugin.getDefault().getBreakpointManager().getBreakpoint((IMarker) element);
    }

    if (element instanceof SDBGBreakpoint) {
      IFile file = ((SDBGBreakpoint) element).getFile();
      if (file != null) {
        return new FileEditorInput(file);
      }

      return new FileStoreEditorInput(EFS.getLocalFileSystem().getStore(
          new Path(((SDBGBreakpoint) element).getFilePath())));
    }

    if (element instanceof ILineBreakpoint) {
      return new FileEditorInput((IFile) ((ILineBreakpoint) element).getMarker().getResource());
    }

    if (element instanceof IFile) {
      return new FileEditorInput((IFile) element);
    }

    if (element instanceof LocalFileStorage) {
      try {
        URI fileUri = ((LocalFileStorage) element).getFile().toURI();

        return new DebuggerEditorInput(EFS.getStore(fileUri));
      } catch (CoreException e) {
        DartUtil.logError(e);
      }
    }

    if (element instanceof ZipEntryStorage) {
      return new StorageEditorInput((IStorage) element) {
        @Override
        public boolean exists() {
          return true;
        }
      };
    }

    if (element instanceof SDBGNoSourceFoundElement) {
      return new SDBGSourceNotFoundEditorInput((SDBGNoSourceFoundElement) element);
    }

    return null;
  }

  /**
   * This method allows us to customize images for Dart objects that are displayed in the debugger.
   */
  @Override
  public Image getImage(Object element) {
    return getImage(element, null);
  }

  public Image getImage(Object element, IPresentationContext context) {
    try {
      if (element instanceof ISDBGVariable) {
        ISDBGVariable variable = (ISDBGVariable) element;

        if (variable.isThrownException()) {
          return SDBGDebugUIPlugin.getImage("obj16/object_exception.png");
        } else if (variable.isThisObject()) {
          return SDBGDebugUIPlugin.getImage("obj16/object_this.png");
        } else if (variable.isLibraryObject()) {
          return SDBGDebugUIPlugin.getImage("obj16/object_library.png");
        } else if (variable.isStatic() || variable.isScope() && "global".equals(variable.getName())) {
          return SDBGDebugUIPlugin.getImage("obj16/object_static.png");
        } else if (variable.isLocal() || variable.isScope() && "local".equals(variable.getName())) {
          return SDBGDebugUIPlugin.getImage("obj16/object_local.gif");
        } else {
          return SDBGDebugUIPlugin.getImage("obj16/object_obj.png");
        }
      } else if (element instanceof ISDBGStackFrame) {
        ISDBGStackFrame frame = (ISDBGStackFrame) element;

        //&&&!!!
        Image image = SDBGDebugUIPlugin.getImage("obj16/field_public.png"); // TODO: Copy over the images for methods
//&&&        
//        Image image = DartDebugUIPlugin.getImage(DartElementImageProvider.getMethodImageDescriptor(
//            false,
//            frame.isPrivate()));

        if (frame.isUsingSourceMaps()) {
          DecorationOverlayIcon overlayDescriptor = new DecorationOverlayIcon(
              image,
              SDBGDebugUIPlugin.getImageDescriptor("ovr16/mapped.png"),
              IDecoration.BOTTOM_RIGHT);

          image = SDBGDebugUIPlugin.getImage(overlayDescriptor);
        }

        return image;
      } else {
        return null;
      }
    } catch (Throwable t) {
      SDBGDebugUIPlugin.logError(t);
      return null;
    }
  }

  @Override
  public Annotation getInstructionPointerAnnotation(IEditorPart editorPart, IStackFrame frame) {
    return null;
  }

  @Override
  public String getInstructionPointerAnnotationType(IEditorPart editorPart, IStackFrame frame) {
    if (frame instanceof IExceptionStackFrame) {
      IExceptionStackFrame f = (IExceptionStackFrame) frame;

      if (f.hasException()) {
        return BREAK_ON_EXCEPTION_ANNOTAION;
      }
    }

    return null;
  }

  @Override
  public Image getInstructionPointerImage(IEditorPart editorPart, IStackFrame frame) {
    if (frame instanceof IExceptionStackFrame) {
      IExceptionStackFrame f = (IExceptionStackFrame) frame;

      if (f.hasException()) {
        return SDBGDebugUIPlugin.getImage("obj16/inst_ptr_exception.png");
      }
    }

    try {
      IStackFrame topOfStack = frame.getThread().getTopStackFrame();

      if (frame.equals(topOfStack)) {
        return SDBGDebugUIPlugin.getImage("obj16/inst_ptr_current.png");
      }
    } catch (DebugException de) {

    }

    return SDBGDebugUIPlugin.getImage("obj16/inst_ptr_normal.png");
  }

  @Override
  public String getInstructionPointerText(IEditorPart editorPart, IStackFrame frame) {
    if (frame instanceof IExceptionStackFrame) {
      IExceptionStackFrame f = (IExceptionStackFrame) frame;

      if (f.hasException()) {
        try {
          return f.getExceptionDisplayText();
        } catch (DebugException e) {
          DartUtil.logError(e);
        }
      }
    }

    return null;
  }

  @Override
  public String getText(Object element) {
    return getText(element, null);
  }

  public String getText(Object element, IPresentationContext context) {
    if (element instanceof IBreakpoint) {
      return getBreakpointText((IBreakpoint) element, context);
    } else if (element instanceof IVariable) {
      return getVariableDetailText((IVariable) element, context);
    } else if (element instanceof IValue) {
      return getValueDetailText((IValue) element, context);
    }

    return null;
  }

  public String getVariableDetailText(IVariable var, IPresentationContext context) {
    try {
      StringBuilder buff = new StringBuilder(getVariableName(var, context));

      String valueString = getValueDetailText(var.getValue(), context);
      if (valueString != null && valueString.length() != 0) {
        buff.append(" = ");
        buff.append(valueString);
      }

      return buff.toString();
    } catch (DebugException e) {
      return null;
    }
  }

  public String getVariableName(IVariable var, IPresentationContext context) {
    try {
      if (var instanceof ISDBGVariable) {
        ISDBGVariable svar = (ISDBGVariable) var;
        if (svar.isScope()) {
          return "(" + svar.getName() + ")";
        }
      }

      ISDBGLogicalStructureTypeExtensions lstExtensions = getLogicalStructureTypeExtensions(
          var.getValue(),
          context);
      if (lstExtensions != null) {
        return lstExtensions.getVariableName(var);
      } else {
        return var.getName();
      }
    } catch (CoreException e) {
      return null;
    }
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void setAttribute(String attribute, Object value) {

  }

  /**
   * Return a textual description of the breakpoint. It looks something like:
   * <p>
   * <code>project-name, path/to/file.dart, line 123, 'text.of.line();'</code>
   * 
   * @param bp
   * @return
   */
  protected String getBreakpointText(IBreakpoint bp, IPresentationContext context) {
    try {
      String text;
      if (bp instanceof SDBGBreakpoint) {
        SDBGBreakpoint sdbgBreakpoint = (SDBGBreakpoint) bp;
        IFile file = sdbgBreakpoint.getFile();
        if (file != null) {
          text = file.getProject().getName() + ", "
              + file.getProjectRelativePath().toPortableString() + ", line "
              + NumberFormat.getNumberInstance().format(sdbgBreakpoint.getLine());
        } else {
          text = sdbgBreakpoint.getName() + ", line "
              + NumberFormat.getNumberInstance().format(sdbgBreakpoint.getLine());
        }
      } else {
        text = bp.getMarker().getResource().getProject().getName() + ", "
            + bp.getMarker().getResource().getProjectRelativePath().toPortableString();

        if (bp instanceof ILineBreakpoint) {
          text += ", line "
              + NumberFormat.getNumberInstance().format(((ILineBreakpoint) bp).getLineNumber());

          String lineInfo = getLineExtract((ILineBreakpoint) bp);
          if (lineInfo != null) {
            text = text + ", '" + lineInfo + "'";
          }
        }
      }

      return text;
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Build the text for an {@link IValue}. This can be a long running call since we wait for the
   * toString call to get back with the value.
   */
  protected String getValueDetailText(IValue value, IPresentationContext context) {
    try {
      if (value == null) {
        return "<unknown value>";
      }

      ISDBGLogicalStructureTypeExtensions lstExtensions = getLogicalStructureTypeExtensions(
          value,
          context);
      if (lstExtensions != null
          && lstExtensions.isValueDetailStringComputedByLogicalStructure(value)) {
        value = getLogicalValue(value, context);
      }

      if (!(value instanceof ISDBGValue) || !((ISDBGValue) value).isPrimitive()) {
        final CountDownLatch latch = new CountDownLatch(1);

        final String valueString[] = new String[1];
        computeDetail(value, new IValueDetailListener() {
          @Override
          public void detailComputed(IValue value, String result) {
            valueString[0] = result;
            latch.countDown();
          }
        });

        try {
          latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          return null;
        }

        if (value instanceof ISDBGValue && ((ISDBGValue) value).isListValue()) {
          valueString[0] = "[" + valueString[0] + "]";
        }

        return valueString[0];
      } else {
        return value.getValueString();
      }
    } catch (CoreException e) {
      return "<unknown value>";
    }
  }

  protected String getValueText(IValue value, IPresentationContext context) throws CoreException {
    ISDBGLogicalStructureTypeExtensions lstExtensions = getLogicalStructureTypeExtensions(
        value,
        context);
    if (lstExtensions != null && lstExtensions.isValueStringComputedByLogicalStructure(value)) {
      value = getLogicalValue(value, context);
    }

    return value.getValueString();
  }

  private String getLineExtract(ILineBreakpoint bp) {
    try {
      Reader r;
      if (bp instanceof SDBGBreakpoint) {
        r = new InputStreamReader(
            ((SDBGBreakpoint) bp).getContents(),
            ((SDBGBreakpoint) bp).getCharset());
      } else {
        r = new InputStreamReader(
            ((IFile) bp.getMarker().getResource()).getContents(),
            ((IFile) bp.getMarker().getResource()).getCharset());
      }

      List<String> lines = Streams.loadLinesAndClose(r);

      int line = bp.getLineNumber() - 1;

      if (line > 0 && line < lines.size()) {
        String lineStr = lines.get(line).trim();

        return lineStr.length() == 0 ? null : lineStr;
      }
    } catch (IOException ioe) {
      return null;
    } catch (CoreException ce) {
      return null;
    }

    return null;
  }

  private ISDBGLogicalStructureTypeExtensions getLogicalStructureTypeExtensions(IValue value,
      IPresentationContext context) {
    if (context != null && isShowLogicalStructure(context)) {
      ILogicalStructureType[] types = DebugPlugin.getLogicalStructureTypes(value);
      if (types.length > 0) {
        ILogicalStructureType type = DebugPlugin.getDefaultStructureType(types);
        if (type instanceof ISDBGLogicalStructureTypeExtensions) {
          return (ISDBGLogicalStructureTypeExtensions) type;
        } else if (type instanceof IAdaptable) {
          return (ISDBGLogicalStructureTypeExtensions) ((IAdaptable) type).getAdapter(ISDBGLogicalStructureTypeExtensions.class);
        } else if (type.getClass().getName().equals(
            "org.eclipse.debug.internal.core.LogicalStructureType")) {
          // This is really nasty now, but LogicalStructureType is unfortunately not IAdaptable...
          try {
            Method method = type.getClass().getDeclaredMethod("getDelegate");
            method.setAccessible(true);
            Object delegateType = method.invoke(type);
            if (delegateType instanceof ISDBGLogicalStructureTypeExtensions) {
              return (ISDBGLogicalStructureTypeExtensions) delegateType;
            } else if (delegateType instanceof IAdaptable) {
              return (ISDBGLogicalStructureTypeExtensions) ((IAdaptable) delegateType).getAdapter(ISDBGLogicalStructureTypeExtensions.class);
            }
          } catch (Exception e) {
            SDBGDebugUIPlugin.logError(e);
            return null;
          }
        }
      }
    }

    return null;
  }

  private IValue getLogicalValue(IValue value, IPresentationContext context) throws CoreException {
    return LOGICAL_VALUE_PROVIDER.getLogicalValue(value, context);
  }

  private Collection<ISourcePresentation> getSourcePresentations() {
    if (sourcePresentations == null) {
      sourcePresentations = new ArrayList<ISourcePresentation>();

      IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
          "com.github.sdbg.debug.ui.sourcePresentation");
      for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
        try {
          sourcePresentations.add((ISourcePresentation) element.createExecutableExtension("class"));
        } catch (CoreException e) {
          SDBGDebugCorePlugin.logError(e);
        }
      }
    }

    return sourcePresentations;
  }

  /**
   * Return whether to show compute a logical structure or a raw structure in the specified context
   * 
   * @return whether to show compute a logical structure or a raw structure in the specified context
   */
  private boolean isShowLogicalStructure(IPresentationContext context) {
    Boolean show = (Boolean) context.getProperty(VariablesView.PRESENTATION_SHOW_LOGICAL_STRUCTURES);
    return show != null && show.booleanValue();
  }
}

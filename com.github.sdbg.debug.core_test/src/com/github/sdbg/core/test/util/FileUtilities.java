/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.github.sdbg.core.test.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * The class <code>FileUtilities</code> implements utility methods used to create and manipulate
 * files.
 */
public class FileUtilities {
  /**
   * Copy over all the files and directories contained in the source directory to the target
   * directory.
   * 
   * @param sourceDirectory the directory whose contents are to be copied
   * @param targetDirectory the directory to which the contents are to be copied
   */
  public static void copyDirectoryContents(File sourceDirectory, File targetDirectory)
      throws IOException {
    File[] children;
    File sourceFile, targetFile;

    if (!sourceDirectory.exists()) {
      throw new IllegalArgumentException(
          "sourceDirectory does not exist: " + sourceDirectory.getAbsolutePath()); //$NON-NLS-1$
    } else if (!sourceDirectory.isDirectory()) {
      throw new IllegalArgumentException(
          "sourceDirectory is not a directory: " + sourceDirectory.getAbsolutePath()); //$NON-NLS-1$
    }
    if (!targetDirectory.exists()) {
      targetDirectory.mkdirs();
    } else if (!targetDirectory.isDirectory()) {
      throw new IllegalArgumentException(
          "targetDirectory is not a directory: " + targetDirectory.getAbsolutePath()); //$NON-NLS-1$
    }
    children = sourceDirectory.listFiles();
    for (int i = 0; i < children.length; i++) {
      sourceFile = children[i];
      targetFile = new File(targetDirectory, sourceFile.getName());
      if (sourceFile.isDirectory()) {
        copyDirectoryContents(sourceFile, targetFile);
      } else {
        copyFile(sourceFile, targetFile);
      }
    }
  }

  /**
   * Copy the contents of the given input file to the given output file.
   * 
   * @param input the input file from which the contents are to be read
   * @param output the output file to which the contents are to be written
   * @throws IOException if the files cannot be either read or written
   */
  public static void copyFile(File input, File output) throws IOException {
    copyFile(new FileInputStream(input), new FileOutputStream(output));
  }

  /**
   * Copy all of the bytes from the given input stream to the given output stream. Both streams will
   * be closed after the operation.
   * 
   * @param input the input stream from which bytes are to be read
   * @param output the output stream to which bytes are to be written
   * @throws IOException if the bytes cannot be either read or written
   */
  public static void copyFile(InputStream input, OutputStream output) throws IOException {
    int bufferSize, readSize;
    byte[] buffer;

    bufferSize = 8192;
    buffer = new byte[bufferSize];
    try {
      readSize = input.read(buffer, 0, bufferSize);
      while (readSize >= 0) {
        output.write(buffer, 0, readSize);
        readSize = input.read(buffer, 0, bufferSize);
      }
    } finally {
      try {
        input.close();
      } catch (IOException exception) {
        // ignore failures to close the input
      }
      try {
        output.close();
      } catch (IOException exception) {
        // ignore failures to close the output
      }
    }
  }

  /**
   * Copy the contents of the given input file to the given output file.
   * 
   * @param input the input file from which the contents are to be read
   * @param output the output file to which the contents are to be written
   * @throws IOException if the files cannot be either read or written
   */
  public static void copyFile(URL input, File output) throws IOException {
    copyFile(input.openStream(), new FileOutputStream(output));
  }

  /**
   * Create the given file, including any parent directories that do not already exist.
   * 
   * @param file the file to be created
   * @throws IOException if the file could not be created
   */
  public static void create(File file) throws IOException {
    File parent;

    parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
      if (!parent.mkdirs()) {
        throw new IOException("Could not create directory " + parent.getAbsolutePath()); //$NON-NLS-1$
      }
    }
    if (file.isDirectory()) {
      if (!file.mkdir()) {
        throw new IOException("Could not create directory " + file.getAbsolutePath()); //$NON-NLS-1$
      }
    } else {
      file.createNewFile();
    }
  }

  /**
   * Delete the given file or directory. If the argument is a directory, then the contents of the
   * directory will be deleted before the directory itself is deleted.
   * 
   * @param file the file or directory to be deleted
   */
  public static void delete(File file) {
    if (file.isDirectory()) {
      safelyDeleteContents(file);
    }
    file.delete();
  }

  /**
   * Delete the contents of the given directory without deleting the directory itself.
   * 
   * @param directory the directory whose contents are to be deleted
   * @throws IllegalArgumentException if the argument is not a directory
   */
  public static void deleteContents(File directory) {
    if (!directory.isDirectory()) {
      throw new IllegalArgumentException(
          "Cannot delete file contents: \"" + directory.getAbsolutePath() + "\""); //$NON-NLS-1$
    }
    safelyDeleteContents(directory);
  }

  /**
   * Return the base name of the given file. The base name is the portion of the name that occurs
   * before the period or extension when a file name is assumed to be of the form
   * <code>baseName '.' extension</code>.
   * 
   * @return the base name of the given file
   */
  public static String getBaseName(File file) {
    String name;
    int index;

    name = file.getName();
    index = name.lastIndexOf('.');
    if (index >= 0) {
      return name.substring(0, index);
    }
    return name;
  }

  /**
   * Return the contents of the given file, interpreted as a string.
   * 
   * @param file the file whose contents are to be returned
   * @return the contents of the given file, interpreted as a string
   * @throws IOException if the file contents could not be read
   */
  public static String getContents(File file) throws IOException {
    FileReader fileReader = null;
    BufferedReader reader;

    try {
      fileReader = new FileReader(file);
      reader = new BufferedReader(fileReader);
      StringBuilder builder = new StringBuilder((int) file.length());
      int nextChar = reader.read();
      while (nextChar >= 0) {
        builder.append((char) nextChar);
        nextChar = reader.read();
      }
      return builder.toString();
    } finally {
      if (fileReader != null) {
        fileReader.close();
      }
    }
  }

  /**
   * Return the extension of the given file. The extension is the portion of the name that occurs
   * after the final period when a file name is assumed to be of the form
   * <code>baseName '.' extension</code>.
   * 
   * @return the extension of the given file
   */
  public static String getExtension(File file) {
    String name;
    int index;

    name = file.getName();
    index = name.lastIndexOf('.');
    if (index >= 0) {
      return name.substring(index + 1);
    }
    return "";
  }

  /**
   * Return a directory with the given name in the given base directory. If the directory did not
   * already exist it will be created. If there is a file of the same name in the base directory,
   * then the directory name will be made unique by appending an integer to the base name.
   * 
   * @return a directory with the given name in the given base directory
   * @throws SecurityException if the directory cannot be accessed or created
   */
  public static File getOrCreateDirectory(File baseDirectory, String baseName) {
    File directory;
    int index;

    directory = new File(baseDirectory, baseName);
    index = 1;
    while (directory.exists() && !directory.isDirectory()) {
      directory = new File(baseDirectory, baseName + index);
      index++;
    }
    if (!directory.exists()) {
      directory.mkdir();
    }
    return directory;
  }

  /**
   * Return a file in the given directory whose name is composed from the given base name and
   * extension (which should include the period), but which does not currently exist.
   * 
   * @param directory the directory that should contain the file
   * @param baseName the base name of the file
   * @param extension the extension used for the file
   * @return a unique file that can be created without overwriting any other file
   */
  public static File getUniqueFile(File directory, String baseName, String extension) {
    File file;
    int index;

    file = new File(directory, baseName + extension);
    index = 1;
    while (file.exists()) {
      file = new File(directory, baseName + index + extension);
      index++;
    }
    return file;
  }

  /**
   * Overwrite the contents of the given file to the given contents.
   * 
   * @param file the file whose contents are to be written
   * @param contents the new contents for the file
   * @throws IOException if the file contents could not be written
   */
  public static void setContents(File file, String contents) throws IOException {
    FileWriter fileWriter = null;
    BufferedWriter writer;

    try {
      fileWriter = new FileWriter(file);
      writer = new BufferedWriter(fileWriter);
      writer.write(contents);
      writer.flush();
    } finally {
      if (fileWriter != null) {
        fileWriter.close();
      }
    }
  }

  /**
   * Delete the contents of the given directory, given that we know it is a directory.
   * 
   * @param directory the directory whose contents are to be deleted
   */
  private static void safelyDeleteContents(File directory) {
    File[] children;

    children = directory.listFiles();
    for (int i = 0; i < children.length; i++) {
      delete(children[i]);
    }
  }

  /**
   * Disallow the creation of instances of this class.
   */
  private FileUtilities() {
  }
}

/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2018 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 */
package workbench.util;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;


/**
 *
 * @author Thomas Kellerer
 */
public class ClasspathUtil
{

  public List<File> checkLibsToMove()
  {
    final File jarFile = getJarFile();
    final File jarDir = jarFile.getParentFile();
    if (jarDir == null)
    {
      return Collections.emptyList();
    }
    LogMgr.logDebug(new CallerInfo(){}, "Checking directory: " + jarDir + " for additional libraries");

    long start = System.currentTimeMillis();
    final List<File> cp = buildClassPath();
    FileFilter ff = (File pathname) -> !pathname.equals(jarFile) && isExtJar(pathname, cp);
    File[] files = jarDir.listFiles(ff);
    long duration = System.currentTimeMillis() - start;
    LogMgr.logInfo(new CallerInfo(){}, "Checking for ext libs took: " + duration + "ms");

    if (files == null) return Collections.emptyList();

    return Arrays.asList(files);
  }

  private List<File> buildClassPath()
  {
    String path = System.getProperty("java.class.path");
    String[] files = path.split(System.getProperty("path.separator"));
    if (files == null) return Collections.emptyList();
    return Arrays.stream(files).map(name -> new File(name)).collect(Collectors.toList());
  }

  private boolean isExtJar(File jarFile, List<File> classpath)
  {
    if (jarFile == null) return false;
    if (jarFile.isDirectory()) return false;
    if (classpath.contains(jarFile)) return false;
    if (jarFile.getName().toLowerCase().endsWith(".jar"))
    {
      return !mightBeJDBCDriver(jarFile);
    }
    return false;
  }

  private boolean mightBeJDBCDriver(File file)
  {
    try (JarFile jar = new JarFile(file);)
    {
      ZipEntry drv = jar.getEntry("META-INF/services/java.sql.Driver");
      return drv != null;
    }
    catch (Throwable th)
    {
      return false;
    }
  }
  /**
   * Returns the location of the application's jar file.
   *
   * @return the file object denoting the running jar file.
   * @see #getJarPath()
   */
  public File getJarFile()
  {
    URL url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
    File f;
    try
    {
      // Sending the path through the URLDecoder is important
      // because otherwise a path with %20 will be created
      // if the directory contains spaces!
      String p = URLDecoder.decode(url.getFile(), "UTF-8");
      f = new File(p);
    }
    catch (Exception e)
    {
      // Fallback, should not happen
      String p = url.getFile().replace("%20", " ");
      f = new File(p);
    }
    return f;
  }

  /**
   * Returns the directory in which the application is installed.
   *
   * @return the full path to the jarfile
   * @see #getJarFile()
   */
  public String getJarPath()
  {
    WbFile parent = new WbFile(getJarFile().getParentFile());
    return parent.getFullPath();
  }
}

/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2016 Thomas Kellerer.
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
package workbench.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.gui.profiles.ProfileKey;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;


/**
 *
 * @author Thomas Kellerer
 */
public class ProfileManager
{
  private boolean loaded = false;
  private boolean profilesDeleted = false;
  private final List<ConnectionProfile> profiles = new ArrayList<>();
  private final List<WbFile> profileFiles = new ArrayList<>(1);
  private final Map<Integer, WbFile> profileSources = new HashMap<>();

  public ProfileManager(File file)
  {
    addProfileSource(file);
    sortFiles();
  }
  
  public ProfileManager(List<WbFile> files)
  {
    if (CollectionUtil.isEmpty(files))
    {
      // this should only happend during unit testing.
      setProfileSource(Collections.singletonList(Settings.getInstance().getDefaultProfileStorage()));
    }
    else
    {
      setProfileSource(files);
    }
  }

  /**
   * Find a connection profile identified by the given key.
   *
   * @param key the key of the profile
   * @return a connection profile with that name or null if none was found.
   */
  public ConnectionProfile getProfile(ProfileKey key)
  {
    if (key == null) return null;
    return findProfile(profiles, key);
  }

  /**
   * Return a list with profile keys that can be displayed to the user.
   *
   * @return all profiles keys (sorted).
   */
  public List<String> getProfileKeys()
  {
    List<String> result = new ArrayList(profiles.size());
    for (ConnectionProfile profile : profiles)
    {
      result.add(profile.getKey().toString());
    }
    result.sort(CaseInsensitiveComparator.INSTANCE);
    return result;
  }


  public List<ConnectionProfile> getProfiles()
  {
    return Collections.unmodifiableList(this.profiles);
  }

  public void ensureLoaded()
  {
    if (loaded == false)
    {
      readProfiles();
    }
  }

  public void load()
  {
    readProfiles();
  }

  /**
   * Save the connection profiles to the files defined during initialization.
   *
   * This will also resetChangedFlags the changed flag for any modified or new profiles.
   * The name of the file defaults to <tt>WbProfiles.xml</tt>, but
   * can be defined in the configuration properties.
   *
   * @see workbench.resource.Settings#getProfileStorage()
   * @see #getFile()
   * @see ProfileStorage#saveProfiles(java.util.List, workbench.util.WbFile)
   */
  public void save()
  {
    for (WbFile file : profileFiles)
    {
      if (Settings.getInstance().getCreateProfileBackup())
      {
        FileUtil.createBackup(file);
      }
      ProfileStorage handler = ProfileStorage.Factory.getStorageHandler(file);
      List<ConnectionProfile> toSave = getProfilesForFile(file);
      handler.saveProfiles(toSave, file);
    }
    resetChangedFlags();
  }

  private List<ConnectionProfile> getProfilesForFile(WbFile file)
  {
    List<ConnectionProfile> result = new ArrayList<>();
    for (ConnectionProfile profile : profiles)
    {
      WbFile source = profileSources.get(profile.internalId());
      if (source != null && source.equals(file))
      {
        result.add(profile);
      }
    }
    return result;
  }

  private void sortFiles()
  {
    Comparator<WbFile> fnameSorter = (WbFile f1, WbFile f2) -> f1.getName().compareToIgnoreCase(f2.getName());
    profileFiles.sort(fnameSorter);
  }

  private void setProfileSource(List<WbFile> files)
  {
    profileFiles.clear();
    profileSources.clear();
    profilesDeleted = false;

    for (File file : files)
    {
      addProfileSource(file);
    }
    sortFiles();
}

  private void addProfileSource(File file)
  {
    if (file.isDirectory())
    {
      profileFiles.addAll(listFiles(file));
    }
    else
    {
      profileFiles.add(new WbFile(file));
    }
  }

  private List<WbFile> listFiles(File profileDir)
  {
    List<WbFile> result = new ArrayList<>(5);
    try
    {
      DirectoryStream<Path> files = Files.newDirectoryStream(profileDir.toPath(), "*.{xml,properties}");
      for (Path file : files)
      {
        result.add(new WbFile(file.toFile()));
      }
      LogMgr.logDebug(new CallerInfo(){}, "Found " + result.size() + " potential profile files");
    }
    catch (IOException ex)
    {
      LogMgr.logError(new CallerInfo(){}, "Error listing files in " + profileDir, ex);
    }
    return result;
  }


  private void readProfiles()
  {
    final CallerInfo ci = new CallerInfo(){};
    long start = System.currentTimeMillis();
    LogMgr.logTrace(ci, "readProfiles() called at " + start + " from " + Thread.currentThread().getName());

    profiles.clear();
    profileSources.clear();
    profilesDeleted = false;

    for (WbFile f : this.profileFiles)
    {
      if (!f.exists())
      {
        LogMgr.logWarning(ci, "Profile storage file " + f.getFullPath() + " not found!");
        continue;
      }

      List<ConnectionProfile> pf = readFile(f);
      if (pf == null)
      {
        LogMgr.logWarning(ci, "Ignoring profile file \"" + f + "\" because it does not seem to be a valid profile storage");
      }
      else if (pf.isEmpty())
      {
        LogMgr.logWarning(ci, "No profiles found in \"" + f + "\" file will be ignored");
      }
      else
      {
        profiles.addAll(pf);
        for (ConnectionProfile profile : pf)
        {
          profileSources.put(profile.internalId(), f);
        }
      }
    }

    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug(ci, profiles.size() + " profiles loaded in " + duration + "ms from " + profileFiles.size() + " files");
    resetChangedFlags();
    loaded = true;
  }

  private List<ConnectionProfile> readFile(WbFile f)
  {
    ProfileStorage reader = ProfileStorage.Factory.getStorageHandler(f);
    List<ConnectionProfile> result = null;
    if (f.exists())
    {
      long start = System.currentTimeMillis();

      result = reader.readProfiles(f);

      long duration = System.currentTimeMillis() - start;
      if (result != null)
      {
        LogMgr.logDebug(new CallerInfo(){}, result.size() + " profiles loaded from " + f.getFullPath() + " in " + duration + "ms");
      }
    }
    return result;
  }


  /**
   * Reset the changed status on the profiles.
   *
   * Called after saving the profiles.
   */
  private void resetChangedFlags()
  {
    for (ConnectionProfile profile : this.profiles)
    {
      profile.resetChangedFlags();
    }
    profilesDeleted = false;
  }

  public void reset()
  {
    loaded = false;
    profilesDeleted = false;
    profiles.clear();
    profileSources.clear();
  }

  public String getProfilesPath()
  {
    return getFile().getFullPath();
  }

  public WbFile getFile()
  {
    return getDefaultStorage();
  }

  public List<WbFile> getSourceFiles()
  {
    return Collections.unmodifiableList(profileFiles);
  }

  public boolean isLoaded()
  {
    return loaded;
  }

  /**
   * Returns true if any of the profile definitions has changed.
   * (Or if a profile has been deleted or added)
   *
   * @return true if at least one profile has been changed, deleted or added
   */
  public boolean profilesAreModified()
  {
    if (profiles == null) return false;
    if (profilesDeleted) return true;

    for (ConnectionProfile profile : this.profiles)
    {
      if (profile.isChanged())
      {
        return true;
      }
    }
    return false;
  }

  public void setSourceFile(ConnectionProfile profile, WbFile source)
  {
    if (profile != null && source != null)
    {
      this.profileSources.put(profile.internalId(), source);
    }
  }

  public void applyProfiles(List<ConnectionProfile> newProfiles)
  {
    if (newProfiles == null) return;

    for (ConnectionProfile profile : profiles)
    {
      if (!newProfiles.contains(profile))
      {
        profilesDeleted = true;
        break;
      }
    }

    this.profiles.clear();

    WbFile defaultStorage = getDefaultStorage();

    for (ConnectionProfile profile : newProfiles)
    {
      this.profiles.add(profile.createStatefulCopy());
      if (profileSources.get(profile.internalId()) == null)
      {
        profileSources.put(profile.internalId(), defaultStorage);
      }
    }
  }

  public void addProfile(ConnectionProfile profile)
  {
    this.addProfile(profile, null);
  }

  public void addProfile(ConnectionProfile profile, WbFile storage)
  {
    this.profiles.remove(profile);
    this.profiles.add(profile);
    if (storage == null)
    {
      storage = getDefaultStorage();
    }
    profileSources.put(profile.internalId(), storage);
  }

  private WbFile getDefaultStorage()
  {
    for (WbFile file : this.profileFiles)
    {
      String name = file.getName();
      if (name.equalsIgnoreCase("wb-profiles.properties")) return file;
      if (name.equalsIgnoreCase("WbProfiles.xml")) return file;
    }
    return this.profileFiles.get(0);
  }

  public void removeProfile(ConnectionProfile profile)
  {
    this.profiles.remove(profile);
    this.profileSources.remove(profile.internalId());

    // deleting a new profile should not change the status to "modified"
    if (!profile.isNew())
    {
      this.profilesDeleted = true;
    }
  }

  public static ConnectionProfile findProfile(List<ConnectionProfile> list, ProfileKey key)
  {
    if (key == null) return null;
    if (list == null) return null;

    String name = key.getName();
    String group = key.getGroup();

    ConnectionProfile firstMatch = null;
    for (ConnectionProfile prof : list)
    {
      if (name.equalsIgnoreCase(prof.getName().trim()))
      {
        if (firstMatch == null) firstMatch = prof;
        if (group == null)
        {
          return prof;
        }
        else if (group.equalsIgnoreCase(prof.getGroup().trim()))
        {
          return prof;
        }
      }
    }
    return firstMatch;
  }

}

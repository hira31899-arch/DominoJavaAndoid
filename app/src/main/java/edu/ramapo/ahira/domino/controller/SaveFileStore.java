/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.controller;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 Reads and writes save files on the device. Saves live in the app's own
 folder on shared storage (Android/data/edu.ramapo.ahira.domino/files),
 where they can also be copied to or from a computer over USB. The model
 never touches files; it only turns a game into text and back.
 */
public class SaveFileStore {

   // Folder inside the app's assets holding the sample save files from the course
   private static final String SAMPLE_FOLDER = "saves";
   // A file name: letters, digits, space, dot, dash, underscore; 1 to 40 characters
   private static final String NAME_PATTERN = "[A-Za-z0-9 ._-]{1,40}";
   // Size of the buffer used when copying file contents
   private static final int BUFFER_SIZE = 4096;

   // Where save files are kept
   private final File m_folder;

   /**
    Finds the folder used for save files.
    @param context Context, the app, used to find its storage folders
    */
   public SaveFileStore(Context context) {
      File external = context.getExternalFilesDir(null);
      m_folder = external != null ? external : context.getFilesDir();
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports where save files are kept, so the user can be told.
    @return The full path of the save folder
    */
   public String getFolderPath() {
      return m_folder.getAbsolutePath();
   }

   /**
    Lists the save files in the folder, in alphabetical order.
    @return A new list of file names (possibly empty)
    */
   public List<String> listSaveNames() {
      List<String> names = new ArrayList<>();
      File[] files = m_folder.listFiles();
      if (files == null) {
         return names;
      }
      for (File file : files) {
         if (file.isFile()) {
            names.add(file.getName());
         }
      }
      Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
      return names;
   }

   /**
    Reads a save file from the folder.
    @param name String, the file name
    @return The whole file as text
    @throws IOException if the name is not allowed or the file cannot be read
    */
   public String read(String name) throws IOException {
      if (!isValidName(name)) {
         throw new IOException("Not a valid file name: " + name);
      }
      try (InputStream in = new FileInputStream(new File(m_folder, name))) {
         return readAll(in);
      }
   }

   /**
    Reads a file the user picked anywhere on the device with the system file picker.
    Reference: Storage Access Framework, from https://developer.android.com/training/data-storage/shared/documents-files
    @param resolver ContentResolver, used to open the picked document
    @param uri Uri, the picked document
    @return The whole file as text
    @throws IOException if the document cannot be read
    */
   public String read(ContentResolver resolver, Uri uri) throws IOException {
      try (InputStream in = resolver.openInputStream(uri)) {
         if (in == null) {
            throw new IOException("Could not open the chosen file.");
         }
         return readAll(in);
      }
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Writes a save file to the folder, replacing any file of the same name.
    @param name String, the file name
    @param text String, the save-file text
    @return The full path the game was written to
    @throws IOException if the name is not allowed or the file cannot be written
    */
   public String write(String name, String text) throws IOException {
      if (!isValidName(name)) {
         throw new IOException("Not a valid file name: " + name);
      }
      File file = new File(m_folder, name);
      try (OutputStream out = new FileOutputStream(file)) {
         out.write(text.getBytes(StandardCharsets.UTF_8));
      }
      return file.getAbsolutePath();
   }

   /**
    Copies the sample save files packaged with the app into the save folder, skipping any already there.
    Algorithm:
       1) List the sample files in the app's assets
       2) For each one not yet in the save folder, copy it there
    Reference: AssetManager, from https://developer.android.com/reference/android/content/res/AssetManager
    @param assets AssetManager, gives access to the files packaged with the app
    @throws IOException if a sample cannot be copied
    */
   public void installSamples(AssetManager assets) throws IOException {
      String[] samples = assets.list(SAMPLE_FOLDER);
      if (samples == null) {
         return;
      }
      List<String> existing = listSaveNames();
      for (String sample : Arrays.asList(samples)) {
         if (existing.contains(sample)) {
            continue;
         }
         try (InputStream in = assets.open(SAMPLE_FOLDER + "/" + sample)) {
            write(sample, readAll(in));
         }
      }
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Tells whether a file name is safe to use: no folders, nothing hidden, reasonable length.
    @param name String, the name to check
    @return true if the name may be used, false otherwise
    */
   public static boolean isValidName(String name) {
      return name != null && name.matches(NAME_PATTERN) && !name.startsWith(".")
             && !name.trim().isEmpty();
   }

   /**
    Reads everything from a stream as UTF-8 text.
    @param in InputStream, the stream to read; read to the end but not closed
    @return The text read
    @throws IOException if reading fails
    */
   private static String readAll(InputStream in) throws IOException {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buffer = new byte[BUFFER_SIZE];
      int count = in.read(buffer);
      while (count != -1) {
         bytes.write(buffer, 0, count);
         count = in.read(buffer);
      }
      return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
   }
}

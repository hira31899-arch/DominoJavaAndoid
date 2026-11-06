/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.controller;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;

import edu.ramapo.ahira.domino.R;
import edu.ramapo.ahira.domino.model.GameState;
import edu.ramapo.ahira.domino.model.Serializer;
import edu.ramapo.ahira.domino.model.Tournament;

/**
 The start screen: begin a new tournament (after asking for the tournament
 score) or resume a saved game (after asking which file). Both lead to the
 game screen. Messages appear on the screen itself; toasts are not used.
 */
public class MainActivity extends AppCompatActivity {

   // Opens the system file picker so a save file can be chosen from anywhere on the device
   private ActivityResultLauncher<String[]> m_filePicker;
   // Reads and writes save files
   private SaveFileStore m_saveFiles;

   // Where the user types the tournament score
   private EditText m_tournamentScoreInput;
   // Shows problems (bad score, unreadable file) and other messages
   private TextView m_message;
   // Shows where save files are kept
   private TextView m_folderText;

   /* *********************************************************************
   Event handlers
   ********************************************************************* */

   /**
    Builds the start screen and connects its buttons.
    Algorithm:
       1) Show the layout; find the widgets
       2) Copy the sample save files into the save folder (first run only) and show the folder
       3) Connect the Start, Resume, and Browse handlers
    Reference: Activity result API, from https://developer.android.com/training/basics/intents/result
    @param savedInstanceState Bundle, state from a previous run of this screen (not used)
    */
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      m_tournamentScoreInput = findViewById(R.id.tournamentScoreInput);
      m_message = findViewById(R.id.mainMessage);
      m_folderText = findViewById(R.id.saveFolderText);
      m_saveFiles = new SaveFileStore(this);
      installSampleSaves();
      m_filePicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                                               this::onDocumentPicked);
      Button start = findViewById(R.id.startButton);
      start.setOnClickListener(view -> onStartClicked());
      Button resume = findViewById(R.id.resumeButton);
      resume.setOnClickListener(view -> onResumeClicked());
   }

   /**
    Starts a new tournament if the tournament score typed is valid; otherwise says what is wrong.
    */
   private void onStartClicked() {
      int score = readTournamentScore();
      if (score < Tournament.MIN_TOURNAMENT_SCORE) {
         showMessage(getString(R.string.bad_tournament_score, Tournament.MIN_TOURNAMENT_SCORE,
                               Tournament.MAX_TOURNAMENT_SCORE));
         return;
      }
      Intent intent = new Intent(this, GameActivity.class);
      intent.putExtra(GameActivity.EXTRA_TOURNAMENT_SCORE, score);
      showMessage("");
      startActivity(intent);
   }

   /**
    Asks which saved game to resume: a list of the files in the save folder, or the file picker.
    */
   private void onResumeClicked() {
      List<String> names = m_saveFiles.listSaveNames();
      String[] choices = names.toArray(new String[0]);
      AlertDialog.Builder builder = new AlertDialog.Builder(this)
         .setTitle(R.string.choose_save_title)
         .setNeutralButton(R.string.browse_device, (dialog, which) -> openFilePicker())
         .setNegativeButton(R.string.cancel, null);
      if (choices.length == 0) {
         builder.setMessage(getString(R.string.no_saves, m_saveFiles.getFolderPath()));
      }
      else {
         builder.setItems(choices, (dialog, which) -> onSaveNameChosen(choices[which]));
      }
      builder.show();
   }

   /**
    Loads the save file the user chose from the list.
    @param name String, the chosen file name
    */
   private void onSaveNameChosen(String name) {
      try {
         resumeFromText(name, m_saveFiles.read(name));
      }
      catch (IOException problem) {
         showMessage(getString(R.string.cannot_read_file, name, problem.getMessage()));
      }
   }

   /**
    Loads the save file the user picked with the system file picker.
    @param uri Uri, the picked document, or null if the user backed out
    */
   private void onDocumentPicked(Uri uri) {
      if (uri == null) {
         return;
      }
      try {
         String name = uri.getLastPathSegment() == null ? "chosen file" : uri.getLastPathSegment();
         resumeFromText(name, m_saveFiles.read(getContentResolver(), uri));
      }
      catch (IOException problem) {
         showMessage(getString(R.string.cannot_read_file, "chosen file", problem.getMessage()));
      }
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Opens the game screen on a saved game, after checking that the text describes a valid game.
    Algorithm:
       1) Turn the text into a game record; check that a tournament can be resumed from it
       2) If not, say so and stay on this screen
       3) Otherwise open the game screen, handing it the save text
    Reference: Ported from my C++ version with Claude
    @param name String, the file name, for messages
    @param text String, the file's contents
    */
   private void resumeFromText(String name, String text) {
      GameState state = Serializer.read(text);
      if (state == null || !new Tournament().resume(state)) {
         showMessage(getString(R.string.not_a_save_file, name));
         return;
      }
      Intent intent = new Intent(this, GameActivity.class);
      intent.putExtra(GameActivity.EXTRA_SAVE_TEXT, text);
      intent.putExtra(GameActivity.EXTRA_SAVE_NAME, name);
      showMessage("");
      startActivity(intent);
   }

   /**
    Opens the system file picker for any kind of file (save files often have no extension).
    */
   private void openFilePicker() {
      m_filePicker.launch(new String[] { "*/*" });
   }

   /**
    Reads the tournament score typed on the screen.
    @return The score, or 0 if the text is not a whole number from 1 to 10000
    */
   private int readTournamentScore() {
      String text = m_tournamentScoreInput.getText().toString().trim();
      try {
         int score = Integer.parseInt(text);
         boolean inRange = score >= Tournament.MIN_TOURNAMENT_SCORE
                           && score <= Tournament.MAX_TOURNAMENT_SCORE;
         return inRange ? score : 0;
      }
      catch (NumberFormatException notANumber) {
         return 0;
      }
   }

   /**
    Copies the sample save files into the save folder and shows where the folder is.
    */
   private void installSampleSaves() {
      try {
         m_saveFiles.installSamples(getAssets());
      }
      catch (IOException problem) {
         showMessage(getString(R.string.samples_not_copied, problem.getMessage()));
      }
      m_folderText.setText(getString(R.string.save_folder, m_saveFiles.getFolderPath()));
   }

   /**
    Shows a message on the start screen.
    @param text String, the message; empty to clear it
    */
   private void showMessage(String text) {
      m_message.setText(text);
   }
}

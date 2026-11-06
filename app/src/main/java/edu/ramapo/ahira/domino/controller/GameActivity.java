/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;

import edu.ramapo.ahira.domino.R;
import edu.ramapo.ahira.domino.model.GameState;
import edu.ramapo.ahira.domino.model.Layout;
import edu.ramapo.ahira.domino.model.Move;
import edu.ramapo.ahira.domino.model.Player;
import edu.ramapo.ahira.domino.model.Recommendation;
import edu.ramapo.ahira.domino.model.Round;
import edu.ramapo.ahira.domino.model.Serializer;
import edu.ramapo.ahira.domino.model.Tile;
import edu.ramapo.ahira.domino.model.Tournament;
import edu.ramapo.ahira.domino.view.BoardView;
import edu.ramapo.ahira.domino.view.HandView;

/**
 The game screen and its controller.
 Model-View-Controller:
    model      - Tournament and the classes it owns (package model); knows nothing about the screen
    view       - BoardView, HandView, and the text widgets; they redraw themselves from the model
    controller - this class: each event handler asks the Tournament to change the game, then
                 calls refresh() so every view redraws itself from the updated model
 */
public class GameActivity extends AppCompatActivity {

   // Intent extras: the tournament score for a new game, or the text and name of a saved game
   public static final String EXTRA_TOURNAMENT_SCORE = "edu.ramapo.ahira.domino.TOURNAMENT_SCORE";
   public static final String EXTRA_SAVE_TEXT = "edu.ramapo.ahira.domino.SAVE_TEXT";
   public static final String EXTRA_SAVE_NAME = "edu.ramapo.ahira.domino.SAVE_NAME";

   // Key under which an unfinished round is kept if Android has to rebuild the screen
   private static final String STATE_SAVE_TEXT = "savedRound";
   // File name offered when saving
   private static final String DEFAULT_SAVE_NAME = "domino_save.txt";
   // Tile sizes in dp: the human's hand, the computer's hand, the boneyard
   private static final float HUMAN_TILE_DP = 30f;
   private static final float COMPUTER_TILE_DP = 20f;
   private static final float BONEYARD_TILE_DP = 14f;
   // The four ends, in the same order as m_endButtons
   private static final char[] ENDS = { Layout.LEFT, Layout.RIGHT, Layout.UP, Layout.DOWN };

   // The game being played
   private Tournament m_tournament;
   // Reads and writes save files
   private SaveFileStore m_saveFiles;
   // Position in the human's hand of the tile the person tapped, or HandView.NO_SELECTION
   private int m_selectedTile;

   // The table
   private BoardView m_board;
   // The two hands and the boneyard
   private HandView m_humanHand;
   private HandView m_computerHand;
   private HandView m_boneyardView;
   // Captions for the hands and the boneyard
   private TextView m_humanLabel;
   private TextView m_computerLabel;
   private TextView m_boneyardLabel;
   // What the person should do next
   private TextView m_prompt;
   // Round number, target, and tournament goal; whose turn it is
   private TextView m_roundInfo;
   private TextView m_nextPlayer;
   // Score table: round and tournament score of each player
   private TextView m_humanRoundScore;
   private TextView m_humanTournamentScore;
   private TextView m_computerRoundScore;
   private TextView m_computerTournamentScore;
   // The game log: every message, in one scrollable window
   private TextView m_logText;
   private ScrollView m_logScroll;
   // Buttons: place at an end (same order as ENDS), draw, pass, computer's turn, help, save
   private Button[] m_endButtons;
   private Button m_drawButton;
   private Button m_passButton;
   private Button m_computerButton;
   private Button m_helpButton;
   private Button m_saveButton;

   /* *********************************************************************
   Event handlers
   ********************************************************************* */

   /**
    Builds the game screen and starts or resumes the tournament.
    Algorithm:
       1) Show the layout, find the widgets, connect the buttons
       2) If Android rebuilt the screen mid-round, continue that round
       3) Otherwise resume the saved game handed over, or start a new tournament and ask for a target
    Reference: Activity lifecycle, from https://developer.android.com/guide/components/activities/activity-lifecycle
    @param savedInstanceState Bundle, the round in progress if Android rebuilt the screen, else null
    */
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_game);
      findWidgets();
      connectHandlers();
      m_saveFiles = new SaveFileStore(this);
      m_tournament = new Tournament();
      m_selectedTile = HandView.NO_SELECTION;
      String rebuilt = savedInstanceState == null ? null
                       : savedInstanceState.getString(STATE_SAVE_TEXT);
      Intent intent = getIntent();
      if (rebuilt != null) {
         resumeGame(rebuilt, getString(R.string.unfinished_round));
      }
      else if (intent.hasExtra(EXTRA_SAVE_TEXT)) {
         resumeGame(intent.getStringExtra(EXTRA_SAVE_TEXT), intent.getStringExtra(EXTRA_SAVE_NAME));
      }
      else {
         startNewTournament(intent.getIntExtra(EXTRA_TOURNAMENT_SCORE, 0));
      }
   }

   /**
    Keeps the round in progress if Android has to rebuild the screen (e.g. after low memory).
    @param outState Bundle, passed by reference; receives the round in save-file form
    */
   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      GameState state = m_tournament.toState();
      if (state != null) {
         outState.putString(STATE_SAVE_TEXT, Serializer.write(state));
      }
   }

   /**
    Selects (or unselects) a tile in the human's hand.
    @param index int, position of the tapped tile
    */
   private void onHumanTileTapped(int index) {
      m_selectedTile = index == m_selectedTile ? HandView.NO_SELECTION : index;
      refresh();
   }

   /**
    Places the selected tile at the end whose button was tapped.
    Algorithm:
       1) Build the move from the selected tile and the end
       2) Ask the tournament to play it (the model checks it again)
       3) Clear the selection, refresh the screen, and check whether the round ended
    Reference: Ported from my C++ version with Claude
    @param end char, the end letter of the button tapped
    */
   private void onEndChosen(char end) {
      Tile tile = selectedTile();
      if (tile == null) {
         return;
      }
      m_tournament.playHumanMove(new Move(tile, end));
      m_selectedTile = HandView.NO_SELECTION;
      afterTurnStep();
   }

   /**
    Draws a tile for the human, who has nothing that fits.
    */
   private void onDrawClicked() {
      m_tournament.drawForHuman();
      m_selectedTile = HandView.NO_SELECTION;
      refresh();
   }

   /**
    Passes the human's turn: nothing fits and the boneyard is empty.
    */
   private void onPassClicked() {
      m_tournament.passHumanTurn();
      m_selectedTile = HandView.NO_SELECTION;
      afterTurnStep();
   }

   /**
    Lets the computer play its turn; its move and reasoning appear in the log.
    */
   private void onComputerTurnClicked() {
      m_tournament.playComputerTurn();
      afterTurnStep();
   }

   /**
    Asks the computer for a recommendation, selects the recommended tile, and shows the rationale.
    Algorithm:
       1) Ask the tournament for help (the computer's own strategy, applied to the human's hand)
       2) If it recommends a tile, select that tile so its end buttons appear
       3) Show the recommendation and its reason
    Reference: Ported from my C++ version with Claude
    */
   private void onHelpClicked() {
      Recommendation advice = m_tournament.getHelp();
      if (advice == null) {
         return;
      }
      if (advice.getAction() == Recommendation.Action.PLACE) {
         List<Tile> hand = m_tournament.getHuman().getHand().getTiles();
         m_selectedTile = hand.indexOf(advice.getMove().getTile());
      }
      refresh();
      new AlertDialog.Builder(this)
         .setTitle(R.string.help_title)
         .setMessage(advice.describe())
         .setPositiveButton(R.string.ok, null)
         .show();
   }

   /**
    Asks for a file name, then saves the round and closes the game.
    */
   private void onSaveClicked() {
      EditText nameInput = new EditText(this);
      nameInput.setSingleLine(true);
      nameInput.setText(DEFAULT_SAVE_NAME);
      nameInput.setSelectAllOnFocus(true);
      new AlertDialog.Builder(this)
         .setTitle(R.string.save_title)
         .setMessage(getString(R.string.save_prompt, m_saveFiles.getFolderPath()))
         .setView(nameInput)
         .setPositiveButton(R.string.save_and_quit,
                            (dialog, which) -> saveAndQuit(nameInput.getText().toString().trim()))
         .setNegativeButton(R.string.cancel, null)
         .show();
   }

   /**
    Confirms before leaving a game that has not been saved.
    */
   private void onBackRequested() {
      if (!m_tournament.isRoundInProgress()) {
         finish();
         return;
      }
      new AlertDialog.Builder(this)
         .setTitle(R.string.leave_title)
         .setMessage(R.string.leave_message)
         .setPositiveButton(R.string.leave, (dialog, which) -> finish())
         .setNegativeButton(R.string.keep_playing, null)
         .show();
   }

   /* *********************************************************************
   Source code to control the flow of the tournament
   ********************************************************************* */

   /**
    Starts a new tournament with the score typed on the start screen and asks for the first target.
    @param tournamentScore int, the score that ends the tournament
    */
   private void startNewTournament(int tournamentScore) {
      if (!m_tournament.setTournamentScore(tournamentScore)) {
         showFatalProblem(getString(R.string.bad_tournament_score, Tournament.MIN_TOURNAMENT_SCORE,
                                    Tournament.MAX_TOURNAMENT_SCORE));
         return;
      }
      refresh();
      askTargetScore();
   }

   /**
    Resumes a saved game.
    @param saveText String, the contents of the save file
    @param name String, the file name, for messages
    */
   private void resumeGame(String saveText, String name) {
      GameState state = Serializer.read(saveText);
      if (state == null || !m_tournament.resume(state)) {
         showFatalProblem(getString(R.string.not_a_save_file, name));
         return;
      }
      refresh();
   }

   /**
    Asks for the next round's target score, 3 or 5, then deals the round.
    Only the two legal targets are offered, so no invalid target can be chosen.
    */
   private void askTargetScore() {
      String[] targets = { String.valueOf(Round.MIN_TARGET), String.valueOf(Round.MAX_TARGET) };
      new AlertDialog.Builder(this)
         .setTitle(getString(R.string.target_title, m_tournament.getRoundNumber() + 1))
         .setItems(targets, (dialog, which) -> startRound(Integer.parseInt(targets[which])))
         .setCancelable(false)
         .show();
   }

   /**
    Deals a round with the chosen target and shows how the first player was decided.
    @param targetScore int, 3 or 5
    */
   private void startRound(int targetScore) {
      m_tournament.startNewRound(targetScore);
      m_selectedTile = HandView.NO_SELECTION;
      refresh();
      Round round = m_tournament.getRound();
      if (round != null) {
         new AlertDialog.Builder(this)
            .setTitle(R.string.first_player_title)
            .setMessage(round.getOpeningText())
            .setPositiveButton(R.string.ok, null)
            .show();
      }
   }

   /**
    After any action that may end the round: refresh the screen and, if the round ended, report it.
    */
   private void afterTurnStep() {
      refresh();
      Round round = m_tournament.getRound();
      if (round != null && round.isOver()) {
         showRoundOver(round);
      }
   }

   /**
    Shows the round's result; then either the next round begins or the tournament winner is shown.
    @param round Round, the round that just ended (read only)
    */
   private void showRoundOver(Round round) {
      boolean tournamentOver = m_tournament.isOver();
      String message = round.getSummary();
      if (tournamentOver) {
         message += "\n\n" + m_tournament.getWinnerAnnouncement();
      }
      AlertDialog.Builder builder = new AlertDialog.Builder(this)
         .setTitle(tournamentOver ? R.string.tournament_over_title : R.string.round_over_title)
         .setMessage(message)
         .setCancelable(false);
      if (tournamentOver) {
         builder.setPositiveButton(R.string.main_menu, (dialog, which) -> finish());
      }
      else {
         builder.setPositiveButton(R.string.next_round, (dialog, which) -> askTargetScore());
      }
      builder.show();
   }

   /**
    Saves the round to a file and closes the game.
    Algorithm:
       1) Refuse a bad file name and ask again
       2) Turn the round into save-file text and write it; on failure say why
       3) On success, say where it was saved and close the app
    Reference: Ported from my C++ version with Claude
    @param name String, the file name typed
    */
   private void saveAndQuit(String name) {
      GameState state = m_tournament.toState();
      if (state == null) {
         return;
      }
      if (!SaveFileStore.isValidName(name)) {
         showProblem(getString(R.string.bad_file_name), this::onSaveClicked);
         return;
      }
      try {
         String path = m_saveFiles.write(name, Serializer.write(state));
         m_tournament.noteSaved(name);
         refresh();
         new AlertDialog.Builder(this)
            .setTitle(R.string.saved_title)
            .setMessage(getString(R.string.saved_message, path))
            .setPositiveButton(R.string.ok, (dialog, which) -> finishAffinity())
            .setCancelable(false)
            .show();
      }
      catch (IOException problem) {
         showProblem(getString(R.string.cannot_write_file, name, problem.getMessage()), null);
      }
   }

   /* *********************************************************************
   Source code to refresh the screen from the model
   ********************************************************************* */

   /**
    Redraws every part of the screen from the model: board, hands, boneyard, scores, buttons, log.
    */
   private void refresh() {
      Round round = m_tournament.getRound();
      m_board.update(round == null ? new Layout() : round.getLayout());
      refreshHands();
      refreshBoneyard(round);
      refreshScores(round);
      refreshButtons(round);
      refreshPrompt(round);
      refreshLog();
   }

   /**
    Shows both hands with their tile counts and pip totals; the selected tile is highlighted.
    */
   private void refreshHands() {
      Player human = m_tournament.getHuman();
      Player computer = m_tournament.getComputer();
      List<Tile> humanTiles = human.getHand().getTiles();
      if (m_selectedTile >= humanTiles.size()) {
         m_selectedTile = HandView.NO_SELECTION;
      }
      m_humanHand.update(humanTiles, m_selectedTile);
      m_computerHand.update(computer.getHand().getTiles(), HandView.NO_SELECTION);
      m_humanLabel.setText(getString(R.string.hand_caption, human.getName(), humanTiles.size(),
                                     human.getHandSum()));
      m_computerLabel.setText(getString(R.string.hand_caption, computer.getName(),
                                        computer.getHand().size(), computer.getHandSum()));
   }

   /**
    Shows the boneyard, top tile first, and how many tiles it holds.
    @param round Round, the current round (read only), or null before the first round
    */
   private void refreshBoneyard(Round round) {
      List<Tile> stock = round == null ? null : round.getBoneyard().getTiles();
      int count = stock == null ? 0 : stock.size();
      m_boneyardView.update(stock, HandView.NO_SELECTION);
      m_boneyardLabel.setText(getString(R.string.boneyard_caption, count));
   }

   /**
    Shows the round number, target, tournament goal, whose turn it is, and both players' scores.
    @param round Round, the current round (read only), or null before the first round
    */
   private void refreshScores(Round round) {
      int target = round == null ? 0 : round.getTargetScore();
      m_roundInfo.setText(getString(R.string.round_info, m_tournament.getRoundNumber(), target,
                                    m_tournament.getTournamentScore()));
      if (m_tournament.isRoundInProgress()) {
         m_nextPlayer.setText(getString(R.string.next_player, round.getNextPlayerName()));
      }
      else {
         m_nextPlayer.setText(m_tournament.isOver() ? R.string.tournament_over_title
                                                    : R.string.waiting_for_round);
      }
      Player human = m_tournament.getHuman();
      Player computer = m_tournament.getComputer();
      m_humanRoundScore.setText(String.valueOf(human.getRoundScore()));
      m_humanTournamentScore.setText(String.valueOf(human.getTournamentScore()));
      m_computerRoundScore.setText(String.valueOf(computer.getRoundScore()));
      m_computerTournamentScore.setText(String.valueOf(computer.getTournamentScore()));
   }

   /**
    Shows only the buttons that make sense right now.
    Algorithm:
       1) Help and the end buttons only on the human's turn; Save whenever a round is in progress
       2) An end button only for an end where the selected tile fits
       3) Draw only when nothing fits and the boneyard has tiles; Pass only when it is empty
       4) "Play computer's turn" only on the computer's turn
    Reference: Ported from my C++ version with Claude
    @param round Round, the current round (read only), or null before the first round
    */
   private void refreshButtons(Round round) {
      boolean active = m_tournament.isRoundInProgress();
      boolean humanTurn = active && round.isHumanTurn();
      Tile selected = selectedTile();
      Layout layout = round == null ? new Layout() : round.getLayout();
      for (int i = 0; i < ENDS.length; i++) {
         boolean fits = humanTurn && selected != null && layout.canPlace(selected, ENDS[i]);
         boolean show = fits && (!layout.isEmpty() || ENDS[i] == Layout.LEFT);
         m_endButtons[i].setVisibility(show ? View.VISIBLE : View.GONE);
         m_endButtons[i].setText(layout.isEmpty() ? getString(R.string.start_layout)
            : getString(R.string.play_at_end, Layout.endName(ENDS[i]), layout.getEndPips(ENDS[i])));
      }
      setShown(m_drawButton, humanTurn && round.canCurrentDraw());
      setShown(m_passButton, humanTurn && round.canCurrentPass());
      setShown(m_computerButton, active && !humanTurn);
      setShown(m_helpButton, humanTurn);
      setShown(m_saveButton, active);
   }

   /**
    Tells the person what they can do next.
    @param round Round, the current round (read only), or null before the first round
    */
   private void refreshPrompt(Round round) {
      Tile selected = selectedTile();
      if (!m_tournament.isRoundInProgress()) {
         m_prompt.setText(R.string.prompt_no_round);
      }
      else if (!round.isHumanTurn()) {
         m_prompt.setText(R.string.prompt_computer_turn);
      }
      else if (round.canCurrentDraw()) {
         m_prompt.setText(R.string.prompt_draw);
      }
      else if (round.canCurrentPass()) {
         m_prompt.setText(R.string.prompt_pass);
      }
      else if (selected == null) {
         m_prompt.setText(R.string.prompt_pick_tile);
      }
      else if (fitsAnywhere(round.getLayout(), selected)) {
         m_prompt.setText(getString(R.string.prompt_pick_end, selected.toString()));
      }
      else {
         m_prompt.setText(getString(R.string.prompt_no_fit, selected.toString()));
      }
   }

   /**
    Shows every log message and scrolls to the newest one.
    */
   private void refreshLog() {
      m_logText.setText(String.join("\n", m_tournament.getLogLines()));
      m_logScroll.post(() -> m_logScroll.fullScroll(View.FOCUS_DOWN));
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Finds every widget on the screen and sets the tile sizes and styles of the hand views.
    */
   private void findWidgets() {
      m_board = findViewById(R.id.boardView);
      m_humanHand = findViewById(R.id.humanHand);
      m_computerHand = findViewById(R.id.computerHand);
      m_boneyardView = findViewById(R.id.boneyardView);
      m_humanLabel = findViewById(R.id.humanLabel);
      m_computerLabel = findViewById(R.id.computerLabel);
      m_boneyardLabel = findViewById(R.id.boneyardLabel);
      m_prompt = findViewById(R.id.promptText);
      m_roundInfo = findViewById(R.id.roundInfo);
      m_nextPlayer = findViewById(R.id.nextPlayer);
      m_humanRoundScore = findViewById(R.id.humanRoundScore);
      m_humanTournamentScore = findViewById(R.id.humanTournamentScore);
      m_computerRoundScore = findViewById(R.id.computerRoundScore);
      m_computerTournamentScore = findViewById(R.id.computerTournamentScore);
      m_logText = findViewById(R.id.logText);
      m_logScroll = findViewById(R.id.logScroll);
      m_endButtons = new Button[] { findViewById(R.id.playLeftButton),
         findViewById(R.id.playRightButton), findViewById(R.id.playUpButton),
         findViewById(R.id.playDownButton) };
      m_drawButton = findViewById(R.id.drawButton);
      m_passButton = findViewById(R.id.passButton);
      m_computerButton = findViewById(R.id.computerTurnButton);
      m_helpButton = findViewById(R.id.helpButton);
      m_saveButton = findViewById(R.id.saveButton);
      m_humanHand.setTileUnit(HUMAN_TILE_DP);
      m_computerHand.setTileUnit(COMPUTER_TILE_DP);
      m_boneyardView.setTileUnit(BONEYARD_TILE_DP);
      m_boneyardView.setMarkTopOnly(true);
   }

   /**
    Connects every button and the human's hand to its event handler.
    */
   private void connectHandlers() {
      m_humanHand.setOnTileTappedListener(this::onHumanTileTapped);
      for (int i = 0; i < ENDS.length; i++) {
         char end = ENDS[i];
         m_endButtons[i].setOnClickListener(view -> onEndChosen(end));
      }
      m_drawButton.setOnClickListener(view -> onDrawClicked());
      m_passButton.setOnClickListener(view -> onPassClicked());
      m_computerButton.setOnClickListener(view -> onComputerTurnClicked());
      m_helpButton.setOnClickListener(view -> onHelpClicked());
      m_saveButton.setOnClickListener(view -> onSaveClicked());
      getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
         @Override
         public void handleOnBackPressed() {
            onBackRequested();
         }
      });
   }

   /**
    Reports the tile the person selected in their hand.
    @return The selected tile, or null if none is selected
    */
   private Tile selectedTile() {
      return m_tournament.getHuman().getHand().getTile(m_selectedTile);
   }

   /**
    Tells whether a tile fits any open end of the layout.
    @param layout Layout, the table (read only)
    @param tile Tile, the tile to try
    @return true if the tile can be placed somewhere, false otherwise
    */
   private static boolean fitsAnywhere(Layout layout, Tile tile) {
      for (char end : ENDS) {
         if (layout.canPlace(tile, end)) {
            return true;
         }
      }
      return false;
   }

   /**
    Shows or hides a widget.
    @param widget View, the widget
    @param shown boolean, true to show it
    */
   private static void setShown(View widget, boolean shown) {
      widget.setVisibility(shown ? View.VISIBLE : View.GONE);
   }

   /**
    Explains a problem in a dialog; when it is closed, an optional follow-up runs.
    @param message String, what went wrong
    @param afterwards Runnable, what to do when the dialog is closed, or null
    */
   private void showProblem(String message, Runnable afterwards) {
      new AlertDialog.Builder(this)
         .setTitle(R.string.problem_title)
         .setMessage(message)
         .setPositiveButton(R.string.ok, (dialog, which) -> {
            if (afterwards != null) {
               afterwards.run();
            }
         })
         .show();
   }

   /**
    Explains a problem that means the game cannot go on, then returns to the start screen.
    @param message String, what went wrong
    */
   private void showFatalProblem(String message) {
      new AlertDialog.Builder(this)
         .setTitle(R.string.problem_title)
         .setMessage(message)
         .setCancelable(false)
         .setPositiveButton(R.string.ok, (dialog, which) -> finish())
         .show();
   }
}

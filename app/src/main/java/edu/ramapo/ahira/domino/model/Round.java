/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

import java.util.ArrayList;
import java.util.List;

/**
 One round: deal, decide who starts, take turns until a hand is empty or
 neither player can move, then award the bonus. The two players are held
 as Player references so the round never knows (or cares) which is which.

 The C++ version ran the whole round in one loop that waited on the
 keyboard. A GUI cannot wait, so here each step of a turn (draw, pass,
 place) is its own function, called when the person taps a button.
 The mutators are package-private: outside the model, the round can only
 be changed through the Tournament.
 */
public class Round {

   // Number of players in a round
   public static final int PLAYER_COUNT = 2;
   // Tiles dealt to each player at the start of a round
   public static final int HAND_SIZE = 7;
   // The two legal targets: the open-end total must be a multiple of 3 or 5
   public static final int MIN_TARGET = 3;
   public static final int MAX_TARGET = 5;

   // The two players; index 0 draws first when deciding who starts
   private final Player[] m_players;
   // Index of the player about to move
   private int m_next;
   // 3 or 5
   private int m_targetScore;
   // The table
   private final Layout m_layout;
   // The undealt tiles
   private final Boneyard m_boneyard;
   // Passes in a row; two means nobody can move and the round is over
   private int m_consecutivePasses;
   // true once the round has ended and the bonus has been given
   private boolean m_over;
   // Where every message about the round is recorded
   private final GameLog m_log;
   // How the first player was decided, shown when a round starts
   private String m_openingText;
   // Bonus and final scores, shown when the round ends
   private String m_summary;

   /**
    Sets up a round between two players with a given target, before any tiles are dealt.
    @param firstDrawer Player, draws first when deciding who starts (the human, per the rules)
    @param secondDrawer Player, the other player
    @param targetScore int, 3 or 5 (any other value is replaced by 5)
    @param log GameLog, passed by reference; every message about the round is added to it
    */
   Round(Player firstDrawer, Player secondDrawer, int targetScore, GameLog log) {
      m_players = new Player[] { firstDrawer, secondDrawer };
      m_next = 0;
      m_targetScore = isValidTarget(targetScore) ? targetScore : MAX_TARGET;
      m_layout = new Layout();
      m_boneyard = new Boneyard();
      m_consecutivePasses = 0;
      m_over = false;
      m_log = log;
      m_openingText = "";
      m_summary = "";
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports the round's target.
    @return 3 or 5
    */
   public int getTargetScore() {
      return m_targetScore;
   }

   /**
    Hands out a copy of the table for display.
    @return A copy of the Layout
    */
   public Layout getLayout() {
      return new Layout(m_layout);
   }

   /**
    Hands out a copy of the boneyard for display.
    @return A copy of the Boneyard
    */
   public Boneyard getBoneyard() {
      return new Boneyard(m_boneyard);
   }

   /**
    Reports whose turn it is, for the screen and the save file.
    @return The name of the player about to move
    */
   public String getNextPlayerName() {
      return current().getName();
   }

   /**
    Tells whether the round is waiting for the person using the device.
    @return true if the round is in progress and it is the human's turn
    */
   public boolean isHumanTurn() {
      return !m_over && current().isHuman();
   }

   /**
    Tells whether the round has ended.
    @return true once a hand has emptied or nobody could move
    */
   public boolean isOver() {
      return m_over;
   }

   /**
    Reports how the first player was decided.
    @return The draws and who plays first; empty for a resumed round
    */
   public String getOpeningText() {
      return m_openingText;
   }

   /**
    Reports how the round ended: the bonus and both players' scores.
    @return The summary; empty while the round is in progress
    */
   public String getSummary() {
      return m_summary;
   }

   /**
    Tells whether the player to move has a tile that fits an open end.
    @return true if a placement is possible, false otherwise
    */
   public boolean canCurrentPlay() {
      return !m_over && current().canPlay(m_layout);
   }

   /**
    Tells whether the player to move must draw: nothing fits and the boneyard has tiles.
    @return true if drawing is the only option, false otherwise
    */
   public boolean canCurrentDraw() {
      return !m_over && !current().canPlay(m_layout) && !m_boneyard.isEmpty();
   }

   /**
    Tells whether the player to move must pass: nothing fits and the boneyard is empty.
    @return true if passing is the only option, false otherwise
    */
   public boolean canCurrentPass() {
      return !m_over && !current().canPlay(m_layout) && m_boneyard.isEmpty();
   }

   /**
    Suggests the best action for the player to move, using the computer's strategy.
    Algorithm:
       1) If the player can place a tile, ask the strategy for the best placement and its reason
       2) Otherwise, if the boneyard has tiles, suggest drawing, naming the open ends
       3) Otherwise suggest passing
    Reference: Ported from my C++ version with Claude
    @return The suggestion and its rationale
    */
   public Recommendation recommendForCurrent() {
      if (canCurrentPlay()) {
         return Computer.recommend(current().getHand(), m_layout, m_targetScore);
      }
      if (!m_boneyard.isEmpty()) {
         return new Recommendation(Recommendation.Action.DRAW, null,
            "because no tile in the hand matches an open end (" + describeOpenEnds() + ")");
      }
      return new Recommendation(Recommendation.Action.PASS, null,
         "because no tile matches an open end (" + describeOpenEnds()
         + ") and the boneyard is empty");
   }

   /**
    Captures everything about the round that a save file needs.
    Algorithm:
       1) Copy the tournament goal, round target, table and boneyard text, and next player's name
       2) For each player, store its hand, round score, and tournament score under its name
    Reference: Ported from my C++ version with Claude
    @param tournamentScore int, the score that ends the tournament
    @return A new game record describing this round
    */
   public GameState toState(int tournamentScore) {
      GameState state = new GameState();
      state.setTournamentScore(tournamentScore);
      state.setTargetScore(m_targetScore);
      state.setLayoutText(m_layout.toString());
      state.setBoneyardText(m_boneyard.toString());
      state.setNextPlayer(getNextPlayerName());
      for (Player player : m_players) {
         PlayerState saved = state.addPlayer(player.getName());
         saved.setHand(player.getHand());
         saved.setRoundScore(player.getRoundScore());
         saved.setTournamentScore(player.getTournamentScore());
      }
      return state;
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Begins a fresh round: full shuffled boneyard, empty table, seven tiles each, first player chosen.
    Algorithm:
       1) Refill and shuffle the boneyard; clear the table and both hands
       2) Deal seven tiles to each player
       3) Decide who plays first
    Reference: Ported from my C++ version with Claude
    */
   void startNew() {
      m_boneyard.reset();
      m_boneyard.shuffle();
      m_layout.clear();
      m_consecutivePasses = 0;
      m_over = false;
      m_summary = "";
      for (Player player : m_players) {
         player.clearHand();
      }
      dealHands();
      chooseFirstPlayer();
   }

   /**
    Continues a round from a saved game record.
    Algorithm:
       1) The target must be 3 or 5
       2) Rebuild the table from its text; fail if it is not a legal layout
       3) Rebuild the boneyard from its text; fail on a bad or repeated tile
       4) Every player must appear in the record, and the next player must be one of them
       5) Give each player its saved hand and scores; install table, boneyard, target, next player
    Reference: Ported from my C++ version with Claude
    @param state GameState, the saved game (passed by reference, not modified)
    @return true if the round was restored; false (no change to the round) if the record is not valid
    */
   boolean restore(GameState state) {
      Layout layout = new Layout();
      Boneyard boneyard = new Boneyard();
      List<Tile> stock = parseTiles(state.getBoneyardText());
      int nextIndex = findPlayer(state.getNextPlayer());
      if (!isValidTarget(state.getTargetScore()) || !layout.fromString(state.getLayoutText())
          || stock == null || !boneyard.setTiles(stock) || nextIndex < 0 || !hasEveryPlayer(state)) {
         return false;
      }
      for (Player player : m_players) {
         PlayerState saved = state.getPlayer(player.getName());
         player.setHand(saved.getHand());
         player.setScores(saved.getRoundScore(), saved.getTournamentScore());
      }
      m_layout.fromString(state.getLayoutText());
      m_boneyard.setTiles(stock);
      m_targetScore = state.getTargetScore();
      m_next = nextIndex;
      m_consecutivePasses = 0;
      m_over = false;
      m_openingText = "";
      m_summary = "";
      return true;
   }

   /**
    The player to move draws one tile because nothing in their hand fits.
    @return The tile drawn, or null if drawing is not allowed (a tile fits, or the boneyard is empty)
    */
   Tile drawForCurrent() {
      if (!canCurrentDraw()) {
         return null;
      }
      Tile drawn = m_boneyard.drawTile();
      Player player = current();
      player.addTile(drawn);
      m_log.add(player.getName() + " cannot play and draws " + drawn + " from the boneyard.");
      return drawn;
   }

   /**
    The player to move passes because nothing fits and the boneyard is empty.
    Algorithm:
       1) Refuse if passing is not allowed
       2) Count the pass; two in a row means nobody can move, so the round ends
       3) Otherwise it is the other player's turn
    Reference: Rules of 35 Domino from the course project page
    @return true if the player passed, false if passing was not allowed
    */
   boolean passCurrent() {
      if (!canCurrentPass()) {
         return false;
      }
      String name = current().getName();
      m_log.add(name + " cannot play and the boneyard is empty. " + name + " passes.");
      m_consecutivePasses++;
      if (m_consecutivePasses >= PLAYER_COUNT) {
         m_log.add("Neither player can place a tile.");
         finish();
      }
      else {
         advanceTurn();
      }
      return true;
   }

   /**
    The player to move places the tile they choose and scores it. This is one step of the game loop.
    Algorithm:
       1) Ask the player for a move (the Human gives the one picked on screen; the Computer uses
          its strategy); refuse an illegal one
       2) Place it on the table, remove it from the hand, and score it
       3) If that player's hand is now empty, the round ends; otherwise the other player moves
    Reference: Ported from my C++ version with Claude
    @return true if a tile was placed, false if the move was not legal (nothing changes)
    */
   boolean playCurrentMove() {
      if (m_over) {
         return false;
      }
      Player player = current();
      Move move = player.chooseMove(m_layout, m_targetScore);
      if (!m_layout.place(move)) {
         return false;
      }
      player.removeTile(move.getTile());
      scorePlacement(player, move);
      m_consecutivePasses = 0;
      if (!player.hasTiles()) {
         m_log.add(player.getName() + " has played every tile.");
         finish();
      }
      else {
         advanceTurn();
      }
      return true;
   }

   /**
    Plays a whole turn for a player who decides by itself: draw until able to play, then place, or pass.
    Algorithm:
       1) While nothing fits and the boneyard has tiles, draw one
       2) If still nothing fits, pass
       3) Otherwise place the player's chosen tile
    Reference: Rules of 35 Domino from the course project page
    */
   void playAutomaticTurn() {
      if (m_over) {
         return;
      }
      Tile drawn = drawForCurrent();
      while (drawn != null) {
         drawn = drawForCurrent();
      }
      if (!canCurrentPlay()) {
         passCurrent();
         return;
      }
      playCurrentMove();
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Tells whether a number is one of the two legal round targets.
    @param target int, the value to check
    @return true for 3 or 5, false otherwise
    */
   public static boolean isValidTarget(int target) {
      return target == MIN_TARGET || target == MAX_TARGET;
   }

   /**
    Reports the player whose turn it is.
    @return The player about to move
    */
   private Player current() {
      return m_players[m_next];
   }

   /**
    Passes the turn to the other player.
    */
   private void advanceTurn() {
      m_next = (m_next + 1) % PLAYER_COUNT;
   }

   /**
    Deals the opening hand of seven tiles to each player.
    */
   private void dealHands() {
      for (Player player : m_players) {
         for (int i = 0; i < HAND_SIZE; i++) {
            player.addTile(m_boneyard.drawTile());
         }
      }
      m_log.add("Each player is dealt " + HAND_SIZE + " tiles; " + m_boneyard.size()
                + " tiles form the boneyard.");
   }

   /**
    Decides who plays first by drawing: the higher pip total starts.
    Algorithm:
       1) Repeat: player 0 draws a tile, then player 1 draws the next
       2) Announce both draws; put both tiles back on the bottom of the boneyard
       3) If the pip totals differ, the higher total plays first; otherwise draw again
    Reference: Rules of 35 Domino from the course project page
    */
   private void chooseFirstPlayer() {
      StringBuilder opening = new StringBuilder();
      m_next = 0;
      while (m_boneyard.size() >= PLAYER_COUNT) {
         Tile first = m_boneyard.drawTile();
         Tile second = m_boneyard.drawTile();
         opening.append(m_players[0].getName()).append(" draws ").append(first).append(", ")
                .append(m_players[1].getName()).append(" draws ").append(second).append(".\n");
         m_boneyard.addToBottom(first);
         m_boneyard.addToBottom(second);
         if (first.getSum() != second.getSum()) {
            m_next = first.getSum() > second.getSum() ? 0 : 1;
            break;
         }
         opening.append("Same number of pips; drawing again.\n");
      }
      opening.append(current().getName()).append(" plays first.");
      m_openingText = opening.toString();
      for (String line : m_openingText.split("\n")) {
         m_log.add(line);
      }
   }

   /**
    Scores a placement and records it in the log.
    Algorithm:
       1) Find the open-end total; if it is a multiple of the target the player earns that total
       2) Log the move, the player's explanation (the computer explains its strategy), and the score
    Reference: Rules of 35 Domino from the course project page
    @param player Player, passed by reference; its round score is increased by the points earned
    @param move Move, the move just made (passed by reference, not modified)
    */
   private void scorePlacement(Player player, Move move) {
      int sum = m_layout.getOpenEndSum();
      int points = sum % m_targetScore == 0 ? sum : 0;
      player.addRoundPoints(points);
      m_log.add(player.getName() + " placed " + move.getTile() + " at the "
                + Layout.endName(move.getEnd()) + " end.");
      m_log.add(player.explainLastMove());
      if (points > 0) {
         m_log.add("Open-end total is " + sum + ", a multiple of " + m_targetScore + ": "
                   + player.getName() + " earns " + points + " points!");
      }
      else {
         m_log.add("Open-end total is " + sum + ", not a multiple of " + m_targetScore
                   + ": no points.");
      }
   }

   /**
    Ends the round: the lighter hand earns its bonus, scores are reported and folded into the tournament.
    Algorithm:
       1) Record both remaining hands
       2) If the hand sums differ, the lighter hand earns (heavier - lighter) div target
       3) For each player, record this round's points and the new tournament total, then move
          the round points into the tournament total
    Reference: Rules of 35 Domino from the course project page
    */
   private void finish() {
      List<String> lines = new ArrayList<>();
      lines.add("=== Round over ===");
      for (Player player : m_players) {
         lines.add(player.getName() + "'s hand: " + player.getHand() + " (sum " + player.getHandSum()
                   + ")");
      }
      lines.add(awardBonus());
      for (Player player : m_players) {
         lines.add(player.getName() + ": " + player.getRoundScore() + " points this round, "
                   + (player.getTournamentScore() + player.getRoundScore()) + " in the tournament.");
         player.finishRound();
      }
      m_over = true;
      m_summary = String.join("\n", lines);
      for (String line : lines) {
         m_log.add(line);
      }
   }

   /**
    Gives the end-of-round bonus to the player with the lighter hand.
    @return A sentence describing the bonus (or that there was none)
    */
   private String awardBonus() {
      Player first = m_players[0];
      Player second = m_players[1];
      if (first.getHandSum() == second.getHandSum()) {
         return "Both hands weigh the same; no bonus.";
      }
      Player lighter = first.getHandSum() < second.getHandSum() ? first : second;
      Player heavier = lighter == first ? second : first;
      int bonus = (heavier.getHandSum() - lighter.getHandSum()) / m_targetScore;
      lighter.addRoundPoints(bonus);
      return lighter.getName() + " has the lighter hand and earns a bonus of " + bonus + " = ("
             + heavier.getHandSum() + " - " + lighter.getHandSum() + ") div " + m_targetScore + ".";
   }

   /**
    Lists the pips at every open end, for help messages, e.g. "L=4, R=6".
    @return The open ends and their pips, separated by commas
    */
   private String describeOpenEnds() {
      List<String> ends = new ArrayList<>();
      for (char end : Layout.allEnds()) {
         if (m_layout.isOpenEnd(end)) {
            ends.add(end + "=" + m_layout.getEndPips(end));
         }
      }
      return String.join(", ", ends);
   }

   /**
    Reads a space-separated list of tiles, e.g. the boneyard line of a save file.
    @param text String, the tiles as text (may be empty)
    @return A new list of the tiles, or null if any word is not a legal tile
    */
   private static List<Tile> parseTiles(String text) {
      List<Tile> tiles = new ArrayList<>();
      for (String word : text.trim().split("\\s+")) {
         if (word.isEmpty()) {
            continue;
         }
         Tile tile = Tile.fromString(word);
         if (tile == null) {
            return null;
         }
         tiles.add(tile);
      }
      return tiles;
   }

   /**
    Finds a player of this round by name.
    @param name String, the name to look for
    @return The player's index, or -1 if no player has that name
    */
   private int findPlayer(String name) {
      for (int i = 0; i < PLAYER_COUNT; i++) {
         if (m_players[i].getName().equals(name)) {
            return i;
         }
      }
      return -1;
   }

   /**
    Tells whether a saved game has a section for every player of this round.
    @param state GameState, the saved game (passed by reference, not modified)
    @return true if every player appears, false otherwise
    */
   private boolean hasEveryPlayer(GameState state) {
      for (Player player : m_players) {
         if (!state.hasPlayer(player.getName())) {
            return false;
         }
      }
      return true;
   }
}

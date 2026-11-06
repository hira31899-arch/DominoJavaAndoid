/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

import java.util.List;

/**
 The whole session: rounds are played until one player reaches the
 tournament score. This is the one class the controller talks to in order
 to change the game; everything it changes goes through the functions here.
 */
public class Tournament {

   // Smallest and largest tournament score the user may ask to play to
   public static final int MIN_TOURNAMENT_SCORE = 1;
   public static final int MAX_TOURNAMENT_SCORE = 10000;

   // The two players; they live for the whole tournament so scores carry over
   private final Human m_human;
   private final Computer m_computer;
   // The score that ends the tournament; 0 until it has been set
   private int m_tournamentScore;
   // The round being played (or just finished); null before the first round
   private Round m_round;
   // Rounds started in this session, for display
   private int m_roundNumber;
   // true once the end of the current round has been recorded
   private boolean m_roundRecorded;
   // Every message about the tournament and its rounds
   private final GameLog m_log;

   /**
    Creates the two players and a tournament with no goal and no round yet.
    */
   public Tournament() {
      m_human = new Human();
      m_computer = new Computer();
      m_tournamentScore = 0;
      m_round = null;
      m_roundNumber = 0;
      m_roundRecorded = false;
      m_log = new GameLog();
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports the score that ends the tournament.
    @return The goal, an integer (0 if not set yet)
    */
   public int getTournamentScore() {
      return m_tournamentScore;
   }

   /**
    Reports how many rounds have been started in this session.
    @return The round number, an integer
    */
   public int getRoundNumber() {
      return m_roundNumber;
   }

   /**
    Hands out the current round so the view can read it. Its mutators are not visible outside the model.
    @return The round being played or just finished, or null before the first round
    */
   public Round getRound() {
      return m_round;
   }

   /**
    Hands out the human player so the view can read its hand and scores.
    @return The human player
    */
   public Player getHuman() {
      return m_human;
   }

   /**
    Hands out the computer player so the view can read its hand and scores.
    @return The computer player
    */
   public Player getComputer() {
      return m_computer;
   }

   /**
    Hands out every message so far, oldest first, for the log window.
    @return A new list holding every log line
    */
   public List<String> getLogLines() {
      return m_log.getLines();
   }

   /**
    Tells whether a round is being played.
    @return true if there is a round and it has not ended
    */
   public boolean isRoundInProgress() {
      return m_round != null && !m_round.isOver();
   }

   /**
    Tells whether the tournament has ended: no round in progress and someone has reached the goal.
    @return true if the tournament is over, false otherwise
    */
   public boolean isOver() {
      if (isRoundInProgress() || m_tournamentScore <= 0) {
         return false;
      }
      return m_human.getTournamentScore() >= m_tournamentScore
             || m_computer.getTournamentScore() >= m_tournamentScore;
   }

   /**
    Describes the end of the tournament: both totals and the winner, or a draw.
    Algorithm:
       1) List both tournament totals
       2) The higher total wins; equal totals are a draw
    Reference: Ported from my C++ version with Claude
    @return The announcement, several lines long
    */
   public String getWinnerAnnouncement() {
      int human = m_human.getTournamentScore();
      int computer = m_computer.getTournamentScore();
      String result;
      if (human > computer) {
         result = "The Human wins the tournament!";
      }
      else if (computer > human) {
         result = "The Computer wins the tournament!";
      }
      else {
         result = "The tournament is a draw.";
      }
      return "Tournament points: Human " + human + ", Computer " + computer + ".\n" + result;
   }

   /**
    Captures the round in progress for a save file.
    @return The game record, or null if no round is in progress
    */
   public GameState toState() {
      if (!isRoundInProgress()) {
         return null;
      }
      return m_round.toState(m_tournamentScore);
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Sets the score that ends the tournament.
    @param tournamentScore int, the goal
    @return true if set; false (no change) if it is outside 1..10000
    */
   public boolean setTournamentScore(int tournamentScore) {
      if (tournamentScore < MIN_TOURNAMENT_SCORE || tournamentScore > MAX_TOURNAMENT_SCORE) {
         return false;
      }
      m_tournamentScore = tournamentScore;
      return true;
   }

   /**
    Starts a new round with the target chosen by the user.
    Algorithm:
       1) Refuse if the goal is not set, a round is in progress, the tournament is over, or the
          target is not 3 or 5
       2) Create the round with the human drawing first, deal it, and decide the first player
    Reference: Ported from my C++ version with Claude
    @param targetScore int, 3 or 5
    @return true if the round started, false otherwise
    */
   public boolean startNewRound(int targetScore) {
      if (m_tournamentScore <= 0 || isRoundInProgress() || isOver()
          || !Round.isValidTarget(targetScore)) {
         return false;
      }
      m_roundNumber++;
      m_log.add("=== Round " + m_roundNumber + " (target score " + targetScore + ", playing to "
                + m_tournamentScore + ") ===");
      m_round = new Round(m_human, m_computer, targetScore, m_log);
      m_round.startNew();
      m_roundRecorded = false;
      return true;
   }

   /**
    Continues a tournament from a saved game.
    Algorithm:
       1) The saved tournament score must be a legal goal
       2) Restore the round; if the record is not a valid game, change nothing
    Reference: Ported from my C++ version with Claude
    @param state GameState, the saved game (passed by reference, not modified)
    @return true if the game was resumed, false if the record is not a valid game
    */
   public boolean resume(GameState state) {
      int goal = state.getTournamentScore();
      if (goal < MIN_TOURNAMENT_SCORE || goal > MAX_TOURNAMENT_SCORE) {
         return false;
      }
      Round round = new Round(m_human, m_computer, state.getTargetScore(), m_log);
      if (!round.restore(state)) {
         return false;
      }
      m_tournamentScore = goal;
      m_round = round;
      m_roundNumber = 1;
      m_roundRecorded = false;
      m_log.add("=== Resumed saved game (target score " + round.getTargetScore() + ", playing to "
                + goal + ") ===");
      m_log.add(round.getNextPlayerName() + " plays next.");
      return true;
   }

   /**
    The human places the tile and end they picked on the screen.
    @param move Move, the picked tile and end (passed by reference, not modified)
    @return true if the tile was placed; false (nothing changes) if it is not the human's turn
            or the move is not legal
    */
   public boolean playHumanMove(Move move) {
      if (!isRoundInProgress() || !m_round.isHumanTurn() || !m_human.setPendingMove(move)) {
         return false;
      }
      boolean placed = m_round.playCurrentMove();
      recordRoundEnd();
      return placed;
   }

   /**
    The human draws a tile because nothing in their hand fits.
    @return The tile drawn, or null if it is not the human's turn or drawing is not allowed
    */
   public Tile drawForHuman() {
      if (!isRoundInProgress() || !m_round.isHumanTurn()) {
         return null;
      }
      return m_round.drawForCurrent();
   }

   /**
    The human passes because nothing fits and the boneyard is empty.
    @return true if the human passed, false if passing was not allowed
    */
   public boolean passHumanTurn() {
      if (!isRoundInProgress() || !m_round.isHumanTurn()) {
         return false;
      }
      boolean passed = m_round.passCurrent();
      recordRoundEnd();
      return passed;
   }

   /**
    The computer plays its whole turn: draws as needed, then places a tile or passes.
    @return true if the computer moved; false if it is not the computer's turn
    */
   public boolean playComputerTurn() {
      if (!isRoundInProgress() || m_round.isHumanTurn()) {
         return false;
      }
      m_round.playAutomaticTurn();
      recordRoundEnd();
      return true;
   }

   /**
    Asks the computer's strategy what the human should do, and records the advice in the log.
    @return The suggestion and its rationale, or null if it is not the human's turn
    */
   public Recommendation getHelp() {
      if (!isRoundInProgress() || !m_round.isHumanTurn()) {
         return null;
      }
      Recommendation advice = m_round.recommendForCurrent();
      m_log.add("Help: " + advice.describe());
      return advice;
   }

   /**
    Records that the game was saved, just before the program closes.
    @param fileName String, the name the game was saved under
    */
   public void noteSaved(String fileName) {
      m_log.add("Game saved to '" + fileName + "'.");
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Once a round has ended, records the tournament totals and, if someone has reached the goal,
    the winner. Does nothing while the round is in progress or once already recorded.
    */
   private void recordRoundEnd() {
      if (m_round == null || !m_round.isOver() || m_roundRecorded) {
         return;
      }
      m_roundRecorded = true;
      m_log.add("Tournament totals (playing to " + m_tournamentScore + "): Human "
                + m_human.getTournamentScore() + ", Computer " + m_computer.getTournamentScore());
      if (isOver()) {
         m_log.add("=== Tournament over ===");
         for (String line : getWinnerAnnouncement().split("\n")) {
            m_log.add(line);
         }
      }
   }
}

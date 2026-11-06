/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 Everything needed to save a game and pick it up later, in the same shape
 as the course's save file. The Serializer reads and writes it; the Round
 fills and consumes it.
 */
public class GameState {

   // Score that ends the tournament
   private int m_tournamentScore;
   // 3 or 5: the multiple that scores in this round
   private int m_targetScore;
   // Each player's record, keyed by player name ("Human", "Computer")
   private final Map<String, PlayerState> m_players;
   // The table in save-file form, e.g. "L 5-3 [D 3-3 U] 3-2 R"
   private String m_layoutText;
   // The boneyard in save-file form, e.g. "0-6 1-5 ..."
   private String m_boneyardText;
   // Name of the player whose turn it is
   private String m_nextPlayer;

   /**
    Starts an empty game record with zero scores and no players.
    */
   public GameState() {
      m_tournamentScore = 0;
      m_targetScore = 0;
      m_players = new LinkedHashMap<>();
      m_layoutText = "";
      m_boneyardText = "";
      m_nextPlayer = "";
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports the score that ends the tournament.
    @return The tournament goal, an integer
    */
   public int getTournamentScore() {
      return m_tournamentScore;
   }

   /**
    Reports the round's target.
    @return The target, an integer (3 or 5 in a valid game)
    */
   public int getTargetScore() {
      return m_targetScore;
   }

   /**
    Tells whether the record has a section for a player.
    @param name String, the player's name
    @return true if the player appears in the record, false otherwise
    */
   public boolean hasPlayer(String name) {
      return m_players.containsKey(name);
   }

   /**
    Looks up one player's record. The record itself is returned so the Serializer can fill it in.
    @param name String, the player's name
    @return The PlayerState, or null if the player is not in the record
    */
   public PlayerState getPlayer(String name) {
      return m_players.get(name);
   }

   /**
    Reports the layout in save-file form.
    @return The layout text
    */
   public String getLayoutText() {
      return m_layoutText;
   }

   /**
    Reports the boneyard in save-file form.
    @return The boneyard text
    */
   public String getBoneyardText() {
      return m_boneyardText;
   }

   /**
    Reports whose turn it is.
    @return The next player's name, or an empty string if none was given
    */
   public String getNextPlayer() {
      return m_nextPlayer;
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Stores the score that ends the tournament.
    @param tournamentScore int, the goal
    @return true if stored; false (no change) for a negative value
    */
   public boolean setTournamentScore(int tournamentScore) {
      if (tournamentScore < 0) {
         return false;
      }
      m_tournamentScore = tournamentScore;
      return true;
   }

   /**
    Stores the round's target.
    @param targetScore int, the target
    @return true if stored; false (no change) unless the value is positive
    */
   public boolean setTargetScore(int targetScore) {
      if (targetScore <= 0) {
         return false;
      }
      m_targetScore = targetScore;
      return true;
   }

   /**
    Adds an empty section for a player, or returns the existing one.
    @param name String, the player's name
    @return The player's record, ready to be filled in; null for a blank name
    */
   public PlayerState addPlayer(String name) {
      if (name == null || name.trim().isEmpty()) {
         return null;
      }
      if (!m_players.containsKey(name)) {
         m_players.put(name, new PlayerState());
      }
      return m_players.get(name);
   }

   /**
    Stores the layout in save-file form.
    @param layoutText String, the layout text (a missing value is stored as empty)
    */
   public void setLayoutText(String layoutText) {
      m_layoutText = layoutText == null ? "" : layoutText.trim();
   }

   /**
    Stores the boneyard in save-file form.
    @param boneyardText String, the boneyard text (a missing value is stored as empty)
    */
   public void setBoneyardText(String boneyardText) {
      m_boneyardText = boneyardText == null ? "" : boneyardText.trim();
   }

   /**
    Stores whose turn it is.
    @param nextPlayer String, the next player's name (a missing value is stored as empty)
    */
   public void setNextPlayer(String nextPlayer) {
      m_nextPlayer = nextPlayer == null ? "" : nextPlayer.trim();
   }
}

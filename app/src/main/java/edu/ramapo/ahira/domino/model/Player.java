/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

import java.util.List;

/**
 Base class for the two players. Human and Computer inherit from it and
 override chooseMove(), isHuman(), and explainLastMove(); everything else
 is shared. The round never needs to know which kind of player it is using.
 */
public abstract class Player {

   // "Human" or "Computer": shown on screen and written to the save file
   private final String m_name;
   // The tiles this player is holding
   private final Hand m_hand;
   // Points earned in the round in progress
   private int m_roundScore;
   // Points from rounds already finished
   private int m_tournamentScore;

   /**
    Creates a player with a name, an empty hand, and zero scores.
    @param name String, the player's name
    */
   protected Player(String name) {
      m_name = name;
      m_hand = new Hand();
      m_roundScore = 0;
      m_tournamentScore = 0;
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports the player's name for messages and the save file.
    @return The name, a string
    */
   public String getName() {
      return m_name;
   }

   /**
    Hands out a copy of the player's tiles for display, saving, or strategy.
    @return A copy of the player's Hand
    */
   public Hand getHand() {
      return new Hand(m_hand);
   }

   /**
    Weighs the player's hand for the end-of-round bonus.
    @return The total pips in the hand, an integer
    */
   public int getHandSum() {
      return m_hand.getSum();
   }

   /**
    Tells whether the player still has a tile (the round ends when a player runs out).
    @return true if the hand is not empty, false otherwise
    */
   public boolean hasTiles() {
      return !m_hand.isEmpty();
   }

   /**
    Tells whether a particular tile is in the player's hand.
    @param tile Tile, the tile to look for
    @return true if the player holds the tile, false otherwise
    */
   public boolean holds(Tile tile) {
      return m_hand.contains(tile);
   }

   /**
    Reports the points earned so far in the current round.
    @return The round score, an integer
    */
   public int getRoundScore() {
      return m_roundScore;
   }

   /**
    Reports the points carried over from completed rounds.
    @return The tournament score, an integer
    */
   public int getTournamentScore() {
      return m_tournamentScore;
   }

   /**
    Tells whether the player has any legal move on the current table; if not, they must draw or pass.
    Algorithm:
       1) For each tile in the hand, for each of the four ends: if the layout accepts it, return true
       2) Return false if nothing fit
    Reference: Ported from my C++ version with Claude
    @param layout Layout, the table (passed by reference, not modified)
    @return true if some tile in the hand fits some open end, false otherwise
    */
   public boolean canPlay(Layout layout) {
      List<Tile> tiles = m_hand.getTiles();
      for (Tile tile : tiles) {
         for (char end : Layout.allEnds()) {
            if (layout.canPlace(tile, end)) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    Tells whether this player is controlled by the person using the device.
    @return true for the human player, false for the computer
    */
   public abstract boolean isHuman();

   /**
    Explains, in a sentence for the game log, why the player made its last move.
    @return The explanation, or an empty string if the player gives none
    */
   public abstract String explainLastMove();

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Gives the player a tile that was dealt or drawn.
    @param tile Tile, the tile to add
    @return true if added; false if the player already had that tile
    */
   boolean addTile(Tile tile) {
      return m_hand.addTile(tile);
   }

   /**
    Takes a tile out of the player's hand after it is placed.
    @param tile Tile, the tile to remove
    @return true if removed, false if the player did not hold that tile
    */
   boolean removeTile(Tile tile) {
      return m_hand.removeTile(tile);
   }

   /**
    Gives the player a whole hand at once when resuming a saved game.
    @param hand Hand, the tiles to hold (passed by reference, copied, not modified)
    @return true if the hand was accepted; false (no change) if it was missing
    */
   boolean setHand(Hand hand) {
      if (hand == null) {
         return false;
      }
      m_hand.clear();
      for (Tile tile : hand.getTiles()) {
         m_hand.addTile(tile);
      }
      return true;
   }

   /**
    Empties the player's hand at the start of a new round.
    */
   void clearHand() {
      m_hand.clear();
   }

   /**
    Credits points earned by a placement or the end-of-round bonus.
    @param points int, the points to add
    @return true if added; false (no change) for a negative amount
    */
   boolean addRoundPoints(int points) {
      if (points < 0) {
         return false;
      }
      m_roundScore += points;
      return true;
   }

   /**
    Restores both scores when resuming a saved game.
    @param roundScore int, points so far in the interrupted round
    @param tournamentScore int, points from finished rounds
    @return true if set; false (no change) if either value is negative
    */
   boolean setScores(int roundScore, int tournamentScore) {
      if (roundScore < 0 || tournamentScore < 0) {
         return false;
      }
      m_roundScore = roundScore;
      m_tournamentScore = tournamentScore;
      return true;
   }

   /**
    Closes out a round: the round's points join the tournament total and the round score restarts at zero.
    */
   void finishRound() {
      m_tournamentScore += m_roundScore;
      m_roundScore = 0;
   }

   /**
    Decides which tile to place and where. The layout is guaranteed to accept at least one
    tile from this player's hand when this is called.
    @param layout Layout, the current table (passed by reference, not modified)
    @param targetScore int, the round's target (3 or 5)
    @return The chosen Move, or an invalid Move if the player has none
    */
   abstract Move chooseMove(Layout layout, int targetScore);
}

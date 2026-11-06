/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

/**
 One player's part of a saved game: hand, round score, and tournament score.
 */
public class PlayerState {

   // The tiles the player was holding
   private Hand m_hand;
   // Points earned so far in the interrupted round
   private int m_roundScore;
   // Points carried from earlier rounds of the tournament
   private int m_tournamentScore;

   /**
    Starts a player record with an empty hand and zero scores.
    */
   public PlayerState() {
      m_hand = new Hand();
      m_roundScore = 0;
      m_tournamentScore = 0;
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports the saved hand.
    @return A copy of the saved Hand
    */
   public Hand getHand() {
      return new Hand(m_hand);
   }

   /**
    Reports the saved round score.
    @return The round score, an integer
    */
   public int getRoundScore() {
      return m_roundScore;
   }

   /**
    Reports the saved tournament score.
    @return The tournament score, an integer
    */
   public int getTournamentScore() {
      return m_tournamentScore;
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Stores the hand.
    @param hand Hand, the tiles (copied, not modified)
    @return true if stored; false (no change) if the hand was missing
    */
   public boolean setHand(Hand hand) {
      if (hand == null) {
         return false;
      }
      m_hand = new Hand(hand);
      return true;
   }

   /**
    Stores the round score.
    @param roundScore int, points so far in the round
    @return true if stored; false (no change) for a negative value
    */
   public boolean setRoundScore(int roundScore) {
      if (roundScore < 0) {
         return false;
      }
      m_roundScore = roundScore;
      return true;
   }

   /**
    Stores the tournament score.
    @param tournamentScore int, points from finished rounds
    @return true if stored; false (no change) for a negative value
    */
   public boolean setTournamentScore(int tournamentScore) {
      if (tournamentScore < 0) {
         return false;
      }
      m_tournamentScore = tournamentScore;
      return true;
   }
}

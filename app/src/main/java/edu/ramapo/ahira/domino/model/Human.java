/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

/**
 The human player. The move is chosen on the screen: the controller hands
 the selected tile and end to setPendingMove(), and chooseMove() then
 returns it to the round like any other player's move.
 */
public class Human extends Player {

   // Name used on screen and in the save file
   public static final String NAME = "Human";

   // The move the person picked on the screen; invalid until one is picked
   private Move m_pendingMove;

   /**
    Creates the human player, named "Human", with no move picked yet.
    */
   public Human() {
      super(NAME);
      m_pendingMove = new Move();
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Tells the round that this player is controlled by the person using the device.
    @return true always
    */
   @Override
   public boolean isHuman() {
      return true;
   }

   /**
    The human explains their own moves, so there is nothing to add to the log.
    @return An empty string
    */
   @Override
   public String explainLastMove() {
      return "";
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Records the tile and end the person picked on the screen.
    @param move Move, the picked move (passed by reference, not modified)
    @return true if accepted; false (no change) if it is not a real move or the tile is not in the hand
    */
   boolean setPendingMove(Move move) {
      if (move == null || !move.isValid() || !holds(move.getTile())) {
         return false;
      }
      m_pendingMove = move;
      return true;
   }

   /**
    Hands over the move the person picked, if it is legal on the current table, and forgets it.
    Algorithm:
       1) Take the pending move and reset it, so it cannot be played twice
       2) Return it only if the layout accepts it; otherwise return "no move"
    Reference: Ported from my C++ version with Claude
    @param layout Layout, the current table (passed by reference, not modified)
    @param targetScore int, the round's target (not needed: the person has already decided)
    @return The picked Move, or an invalid Move if none was picked or it is not legal
    */
   @Override
   Move chooseMove(Layout layout, int targetScore) {
      Move move = m_pendingMove;
      m_pendingMove = new Move();
      return layout.canPlace(move) ? move : new Move();
   }
}

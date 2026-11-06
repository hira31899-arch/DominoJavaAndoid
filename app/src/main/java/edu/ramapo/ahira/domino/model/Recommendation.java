/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

/**
 What the strategy suggests for one turn, and why.
 The action is to place a tile, to draw from the boneyard, or to pass.
 (In C++ the reason came back through a reference parameter; Java returns both together.)
 */
public final class Recommendation {

   // The three things a player can do on a turn
   public enum Action { PLACE, DRAW, PASS }

   // What to do
   private final Action m_action;
   // The tile and end to play when the action is PLACE; an invalid Move otherwise
   private final Move m_move;
   // Sentence fragment explaining the choice, e.g. "to earn a score of 15"
   private final String m_reason;

   /**
    Creates a suggestion.
    @param action Action, what to do
    @param move Move, the tile and end for PLACE (null is stored as "no move")
    @param reason String, why this is the best thing to do
    */
   public Recommendation(Action action, Move move, String reason) {
      m_action = action;
      m_move = move == null ? new Move() : move;
      m_reason = reason == null ? "" : reason;
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports what the strategy suggests doing.
    @return PLACE, DRAW, or PASS
    */
   public Action getAction() {
      return m_action;
   }

   /**
    Reports the suggested placement.
    @return The Move to make; invalid unless the action is PLACE
    */
   public Move getMove() {
      return m_move;
   }

   /**
    Reports why the strategy made this suggestion.
    @return A sentence fragment, e.g. "to earn a score of 15"
    */
   public String getReason() {
      return m_reason;
   }

   /**
    Expresses the suggestion as one sentence for the help window and the log.
    Algorithm:
       1) PLACE: "Play 2-4 at the Right end, " followed by the reason
       2) DRAW or PASS: the action followed by the reason
    Reference: Ported from my C++ version with Claude
    @return The suggestion and its rationale as a sentence
    */
   public String describe() {
      switch (m_action) {
         case PLACE:
            return "Play " + m_move.getTile() + " at the " + Layout.endName(m_move.getEnd())
                   + " end, " + m_reason + ".";
         case DRAW:
            return "Draw a tile from the boneyard, " + m_reason + ".";
         default:
            return "Pass your turn, " + m_reason + ".";
      }
   }
}

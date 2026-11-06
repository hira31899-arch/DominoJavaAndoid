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
 The computer player. Its strategy lives in recommend() so the human
 player's help can reuse exactly the same reasoning.
 */
public class Computer extends Player {

   // Name used on screen and in the save file
   public static final String NAME = "Computer";

   // Why the computer made its last move, for the game log
   private String m_lastReason;

   /**
    Creates the computer player, named "Computer".
    */
   public Computer() {
      super(NAME);
      m_lastReason = "";
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Tells the round that this player plays by itself.
    @return false always
    */
   @Override
   public boolean isHuman() {
      return false;
   }

   /**
    Explains the computer's last move for the game log, e.g. "It did so to earn a score of 10."
    @return The explanation sentence, or an empty string before its first move
    */
   @Override
   public String explainLastMove() {
      return m_lastReason.isEmpty() ? "" : "It did so " + m_lastReason + ".";
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Picks the computer's move with its strategy and remembers the reason for the log.
    @param layout Layout, the current table (passed by reference, not modified)
    @param targetScore int, the round's target (3 or 5)
    @return The chosen Move (invalid if nothing can be placed)
    */
   @Override
   Move chooseMove(Layout layout, int targetScore) {
      Recommendation choice = recommend(getHand(), layout, targetScore);
      m_lastReason = choice.getReason();
      return choice.getMove();
   }

   /* *********************************************************************
   Source code to help the computer win the game
   ********************************************************************* */

   /**
    Finds the best move for a hand on a table and explains it. This is the computer's
    strategy and also the help given to the human.
    Algorithm:
       1) List every legal (tile, end) pair; on an empty table one placement per tile is enough
       2) If any pair scores, take the highest score (ties: prefer a double, then the heavier
          tile). Reason: the points
       3) Otherwise, if any double can be placed, play the heaviest double at the end that leaves
          the smallest open-end total. Reason: doubles are hard to place later and count both pips
       4) Otherwise play the heaviest tile at the end that leaves the smallest open-end total.
          Reason: lighten the hand for the bonus and deny the opponent a big total
    Reference: Ported from my C++ version with Claude
    @param hand Hand, the tiles to choose from (passed by reference, not modified)
    @param layout Layout, the current table (passed by reference, not modified)
    @param targetScore int, the round's target (3 or 5)
    @return A PLACE recommendation with its reason, or a DRAW recommendation if nothing fits
    */
   public static Recommendation recommend(Hand hand, Layout layout, int targetScore) {
      List<Move> legal = listLegalMoves(hand, layout);
      if (legal.isEmpty()) {
         return new Recommendation(Recommendation.Action.DRAW, null,
                                   "because no tile in the hand matches an open end");
      }
      Move best = bestScoringMove(legal, layout, targetScore);
      int bestPoints = pointsFor(layout, best, targetScore);
      if (bestPoints > 0) {
         return new Recommendation(Recommendation.Action.PLACE, best,
                                   "to earn a score of " + bestPoints);
      }
      return bestNonScoringMove(legal, layout);
   }

   /**
    Lists every placement the rules allow right now.
    @param hand Hand, the tiles to choose from (passed by reference, not modified)
    @param layout Layout, the current table (passed by reference, not modified)
    @return A new list of legal moves; on an empty table, one move per tile
    */
   private static List<Move> listLegalMoves(Hand hand, Layout layout) {
      List<Move> legal = new ArrayList<>();
      for (Tile tile : hand.getTiles()) {
         for (char end : Layout.allEnds()) {
            if (layout.canPlace(tile, end)) {
               legal.add(new Move(tile, end));
               // On an empty table every end is the same, so one entry per tile is enough
               if (layout.isEmpty()) {
                  break;
               }
            }
         }
      }
      return legal;
   }

   /**
    Picks the legal move that scores the most points.
    Algorithm:
       1) Start with the first move
       2) A move replaces the best so far if it scores more; on an equal, non-zero score,
          a double beats a non-double, and otherwise the heavier tile wins
    Reference: Ported from my C++ version with Claude
    @param legal List of Move, the legal moves, at least one (passed by reference, not modified)
    @param layout Layout, the current table (passed by reference, not modified)
    @param targetScore int, the round's target
    @return The best-scoring move (which may score 0 if none scores)
    */
   private static Move bestScoringMove(List<Move> legal, Layout layout, int targetScore) {
      Move best = legal.get(0);
      int bestPoints = pointsFor(layout, best, targetScore);
      for (Move move : legal) {
         int points = pointsFor(layout, move, targetScore);
         boolean better = points > bestPoints;
         if (points == bestPoints && points > 0) {
            Tile tile = move.getTile();
            Tile bestTile = best.getTile();
            boolean doubleBeatsSingle = tile.isDouble() && !bestTile.isDouble();
            boolean heavierOfSameKind = tile.isDouble() == bestTile.isDouble()
                                        && tile.getSum() > bestTile.getSum();
            better = doubleBeatsSingle || heavierOfSameKind;
         }
         if (better) {
            best = move;
            bestPoints = points;
         }
      }
      return best;
   }

   /**
    Chooses a move when nothing scores: dump the heaviest double if any, else the heaviest tile.
    Algorithm:
       1) If any legal move places a double, consider only doubles
       2) Keep the heaviest tile; for the same tile, keep the end leaving the smallest open-end total
       3) Explain the choice
    Reference: Ported from my C++ version with Claude
    @param legal List of Move, the legal moves, at least one (passed by reference, not modified)
    @param layout Layout, the current table (passed by reference, not modified)
    @return A PLACE recommendation with its reason
    */
   private static Recommendation bestNonScoringMove(List<Move> legal, Layout layout) {
      boolean anyDouble = false;
      for (Move move : legal) {
         anyDouble = anyDouble || move.getTile().isDouble();
      }
      Move pick = new Move();
      int pickSum = -1;
      for (Move move : legal) {
         if (anyDouble && !move.getTile().isDouble()) {
            continue;
         }
         int leftover = sumAfter(layout, move);
         boolean heavier = !pick.isValid() || move.getTile().getSum() > pick.getTile().getSum();
         boolean sameTileBetterEnd = pick.isValid() && move.getTile().equals(pick.getTile())
                                     && leftover < pickSum;
         if (heavier || sameTileBetterEnd) {
            pick = move;
            pickSum = leftover;
         }
      }
      String what = anyDouble
         ? "because no move scores; doubles are hard to place later and count twice at the end of "
           + "the round, so it is best to play the heaviest double"
         : "because no move scores; it is best to play the heaviest tile to lighten the hand for "
           + "the end-of-round bonus";
      String where = ", choosing the " + Layout.endName(pick.getEnd()) + " end because that leaves "
                     + "the smallest open-end total (" + pickSum + ") for the opponent";
      return new Recommendation(Recommendation.Action.PLACE, pick, what + where);
   }

   /**
    Computes how many points a move would score.
    @param layout Layout, the current table (passed by reference, not modified)
    @param move Move, the move to evaluate (passed by reference, not modified)
    @param targetScore int, the round's target (3 or 5)
    @return The open-end total after the move if it is a multiple of the target, otherwise 0
    */
   private static int pointsFor(Layout layout, Move move, int targetScore) {
      int sum = sumAfter(layout, move);
      if (targetScore > 0 && sum % targetScore == 0) {
         return sum;
      }
      return 0;
   }

   /**
    Finds the open-end total the opponent would face after a move, without touching the real table.
    Algorithm:
       1) Copy the table
       2) Place the tile on the copy
       3) Return the copy's open-end total
    Reference: Ported from my C++ version with Claude
    @param layout Layout, the current table (passed by reference, not modified)
    @param move Move, the move to try (passed by reference, not modified)
    @return The open-end total after the move, an integer
    */
   private static int sumAfter(Layout layout, Move move) {
      Layout trial = new Layout(layout);
      trial.place(move);
      return trial.getOpenEndSum();
   }
}

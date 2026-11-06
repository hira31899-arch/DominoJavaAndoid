/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

/**
 One placement: which tile goes on which open end of the layout.
 A small immutable record shared by the players, the layout, and the round.
 */
public final class Move {

   // The tile to place; null for "no move"
   private final Tile m_tile;
   // The end to place it on: 'L', 'R', 'U', or 'D'
   private final char m_end;
   // false means "no move" (e.g. nothing in the hand can be placed)
   private final boolean m_valid;

   /**
    Creates the "no move" record, used when a player has nothing to place.
    */
   public Move() {
      m_tile = null;
      m_end = Layout.LEFT;
      m_valid = false;
   }

   /**
    Creates a real placement of a tile at an end. A missing tile gives the "no move" record.
    @param tile Tile, the tile to place
    @param end char, the end letter: 'L', 'R', 'U', or 'D'
    */
   public Move(Tile tile, char end) {
      m_tile = tile;
      m_end = end;
      m_valid = tile != null && Layout.isEndLetter(end);
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports the tile being placed.
    @return The tile, or null for the "no move" record
    */
   public Tile getTile() {
      return m_tile;
   }

   /**
    Reports the end the tile is placed on.
    @return One of 'L', 'R', 'U', 'D'
    */
   public char getEnd() {
      return m_end;
   }

   /**
    Tells whether this is a real placement rather than "no move".
    @return true for a real placement, false otherwise
    */
   public boolean isValid() {
      return m_valid;
   }
}

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
 The tiles one player is currently holding.
 Model class only: no input/output happens here.
 */
public class Hand {

   // Value returned by indexOf when the tile is absent
   private static final int NOT_FOUND = -1;

   // The tiles being held, in the order they were received
   private final List<Tile> m_tiles;

   /**
    Creates a hand with no tiles in it.
    */
   public Hand() {
      m_tiles = new ArrayList<>();
   }

   /**
    Creates an independent copy of another hand.
    @param other Hand, the hand to copy (passed by reference, not modified)
    */
   public Hand(Hand other) {
      m_tiles = new ArrayList<>(other.m_tiles);
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports how many tiles the player is holding.
    @return The number of tiles, an integer
    */
   public int size() {
      return m_tiles.size();
   }

   /**
    Tells whether the player has played every tile.
    @return true if the hand holds no tiles, false otherwise
    */
   public boolean isEmpty() {
      return m_tiles.isEmpty();
   }

   /**
    Looks at one tile in the hand by position.
    @param index int, 0-based position of the wanted tile
    @return The tile at that position, or null if the position is out of range
    */
   public Tile getTile(int index) {
      if (index < 0 || index >= m_tiles.size()) {
         return null;
      }
      return m_tiles.get(index);
   }

   /**
    Hands out every tile so a caller can scan them without being able to change the hand.
    @return A new list holding every tile, in hand order
    */
   public List<Tile> getTiles() {
      return new ArrayList<>(m_tiles);
   }

   /**
    Weighs the hand: the total pips of all tiles, which decides the end-of-round bonus.
    Algorithm:
       1) Start a total at zero
       2) For each tile, add its pip sum to the total
    Reference: Ported from my C++ version with Claude
    @return The total pip count, an integer
    */
   public int getSum() {
      int total = 0;
      for (Tile tile : m_tiles) {
         total += tile.getSum();
      }
      return total;
   }

   /**
    Tells whether a particular tile is in the hand (either orientation counts).
    @param tile Tile, the tile to look for
    @return true if the tile is in the hand, false otherwise
    */
   public boolean contains(Tile tile) {
      return indexOf(tile) != NOT_FOUND;
   }

   /**
    Expresses the hand in the text form used on screen and in the save file ("0-1 2-5 4-4").
    @return The tiles as one string, separated by single spaces
    */
   @Override
   public String toString() {
      return joinTiles(m_tiles);
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Gives the player a tile (dealt or drawn) while preventing the same domino from being held twice.
    @param tile Tile, the tile to add
    @return true if the tile was added; false if it was missing or already in the hand
    */
   public boolean addTile(Tile tile) {
      if (tile == null || contains(tile)) {
         return false;
      }
      m_tiles.add(tile);
      return true;
   }

   /**
    Takes a tile out of the hand after it has been placed on the table.
    Algorithm:
       1) Find the tile's position; if it is not there, return false
       2) Otherwise remove it and return true
    Reference: Ported from my C++ version with Claude
    @param tile Tile, the tile to remove
    @return true if the tile was removed; false if it was not in the hand
    */
   public boolean removeTile(Tile tile) {
      int position = indexOf(tile);
      if (position == NOT_FOUND) {
         return false;
      }
      m_tiles.remove(position);
      return true;
   }

   /**
    Empties the hand at the start of a new round.
    */
   public void clear() {
      m_tiles.clear();
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Joins tiles into the space-separated save-file form. Shared with the Boneyard.
    @param tiles List of Tile, the tiles to join (passed by reference, not modified)
    @return The tiles as one string, separated by single spaces
    */
   static String joinTiles(List<Tile> tiles) {
      StringBuilder text = new StringBuilder();
      for (int i = 0; i < tiles.size(); i++) {
         if (i > 0) {
            text.append(' ');
         }
         text.append(tiles.get(i).toString());
      }
      return text.toString();
   }

   /**
    Locates a tile inside the hand.
    @param tile Tile, the tile to find
    @return The 0-based position of the matching tile, or NOT_FOUND (-1) if it is absent
    */
   private int indexOf(Tile tile) {
      if (tile == null) {
         return NOT_FOUND;
      }
      return m_tiles.indexOf(tile);
   }
}

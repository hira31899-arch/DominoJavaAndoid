/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 The stock of tiles not yet dealt. Tiles are drawn from the front (top)
 and returned to the bottom. Model class only.
 */
public class Boneyard {

   // Number of tiles in a full double-six set
   public static final int FULL_SET_SIZE = 28;

   // The undealt tiles, top (next to be drawn) first
   private final List<Tile> m_tiles;

   /**
    Creates an empty stock; call reset() for a full set.
    */
   public Boneyard() {
      m_tiles = new ArrayList<>();
   }

   /**
    Creates an independent copy of another boneyard.
    @param other Boneyard, the stock to copy (passed by reference, not modified)
    */
   public Boneyard(Boneyard other) {
      m_tiles = new ArrayList<>(other.m_tiles);
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports how many tiles are still available to draw.
    @return The number of tiles, an integer
    */
   public int size() {
      return m_tiles.size();
   }

   /**
    Tells whether the tiles have run out (a player who cannot play must then pass).
    @return true if no tiles are left, false otherwise
    */
   public boolean isEmpty() {
      return m_tiles.isEmpty();
   }

   /**
    Looks at the tile that the next draw would take, without taking it.
    @return The top tile, or null if the stock is empty
    */
   public Tile peekFront() {
      return m_tiles.isEmpty() ? null : m_tiles.get(0);
   }

   /**
    Hands out every undealt tile, top to bottom, for display or saving.
    @return A new list holding every tile, top first
    */
   public List<Tile> getTiles() {
      return new ArrayList<>(m_tiles);
   }

   /**
    Tells whether a tile is still undealt.
    @param tile Tile, the tile to look for
    @return true if the tile is in the boneyard, false otherwise
    */
   public boolean contains(Tile tile) {
      return tile != null && m_tiles.contains(tile);
   }

   /**
    Expresses the boneyard in the save-file form ("0-6 1-5 5-6"), top tile first.
    @return The tiles as one string, separated by single spaces
    */
   @Override
   public String toString() {
      return Hand.joinTiles(m_tiles);
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Fills the boneyard with a complete double-six set (28 tiles), each domino exactly once.
    Algorithm:
       1) Empty the list
       2) For every left value 0..6, for every right value from left up to 6, add left-right
          (starting right at left means 2-5 is added once and 5-2 never is)
    Reference: Ported from my C++ version with Claude
    */
   public void reset() {
      m_tiles.clear();
      for (int left = Tile.MIN_PIPS; left <= Tile.MAX_PIPS; left++) {
         for (int right = left; right <= Tile.MAX_PIPS; right++) {
            m_tiles.add(new Tile(left, right));
         }
      }
   }

   /**
    Randomly reorders the tiles before dealing a round.
    Reference: Collections.shuffle, from the Java API documentation
    (https://docs.oracle.com/javase/8/docs/api/java/util/Collections.html)
    */
   public void shuffle() {
      Collections.shuffle(m_tiles);
   }

   /**
    Takes the top tile off the stock (when dealing, or when a player cannot play).
    @return The tile drawn, or null if the boneyard was empty
    */
   public Tile drawTile() {
      if (m_tiles.isEmpty()) {
         return null;
      }
      return m_tiles.remove(0);
   }

   /**
    Returns a tile to the bottom of the stock (used after the draw that decides who plays first).
    @param tile Tile, the tile to put back
    @return true if the tile was added, false if it was missing or already in the boneyard
    */
   public boolean addToBottom(Tile tile) {
      if (tile == null || contains(tile)) {
         return false;
      }
      m_tiles.add(tile);
      return true;
   }

   /**
    Restores the boneyard from a saved game, refusing a list that could not come from a real set.
    @param tiles List of Tile, the tiles to load, top first (passed by reference, not modified)
    @return true if the boneyard was replaced; false (no change) if the list repeats a tile
    */
   public boolean setTiles(List<Tile> tiles) {
      if (tiles == null || hasDuplicates(tiles)) {
         return false;
      }
      m_tiles.clear();
      m_tiles.addAll(tiles);
      return true;
   }

   /**
    Removes every tile from the pile.
    */
   public void clear() {
      m_tiles.clear();
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Detects the same domino appearing twice in a list of tiles.
    Algorithm:
       1) Put each tile into a set; tiles that are equal either way round collide
       2) A failed insertion means a repeat
    Reference: Ported from my C++ version with Claude
    @param tiles List of Tile, the list to check (passed by reference, not modified)
    @return true if any two tiles in the list are the same domino, false otherwise
    */
   private static boolean hasDuplicates(List<Tile> tiles) {
      Set<Tile> seen = new HashSet<>();
      for (Tile tile : tiles) {
         if (tile == null || !seen.add(tile)) {
            return true;
         }
      }
      return false;
   }
}

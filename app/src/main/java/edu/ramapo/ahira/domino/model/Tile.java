/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

/**
 A single domino tile: two ends, each showing 0-6 pips.
 Tiles are immutable: "turning a tile around" produces a new tile.
 Model class only: no input/output happens here.
 */
public final class Tile {

   // Smallest and largest number of pips one end of a tile can show
   public static final int MIN_PIPS = 0;
   public static final int MAX_PIPS = 6;

   // A tile is written as one digit, a dash, and one digit, e.g. "2-5"
   private static final int TEXT_LENGTH = 3;
   // Multiplier used to fold the two pip counts into one hash code
   private static final int HASH_BASE = 10;

   // Pips showing on the left end of the tile
   private final int m_leftPips;
   // Pips showing on the right end of the tile
   private final int m_rightPips;

   /**
    Creates a tile showing the requested pips, or the blank tile 0-0 when either value is not legal.
    Algorithm:
       1) If both values are legal pip counts, keep them
       2) Otherwise the tile is the blank tile 0-0
    Reference: Ported from my C++ version with Claude
    @param leftPips int, pips wanted on the left end
    @param rightPips int, pips wanted on the right end
    */
   public Tile(int leftPips, int rightPips) {
      boolean legal = isValidPips(leftPips) && isValidPips(rightPips);
      m_leftPips = legal ? leftPips : MIN_PIPS;
      m_rightPips = legal ? rightPips : MIN_PIPS;
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports the number of pips on the left end of the tile.
    @return The left pip count, an integer 0-6
    */
   public int getLeftPips() {
      return m_leftPips;
   }

   /**
    Reports the number of pips on the right end of the tile.
    @return The right pip count, an integer 0-6
    */
   public int getRightPips() {
      return m_rightPips;
   }

   /**
    Reports the total pips on the tile, used to pick the first player and to weigh a hand.
    @return The sum of both ends, an integer 0-12
    */
   public int getSum() {
      return m_leftPips + m_rightPips;
   }

   /**
    Tells whether both ends of the tile match, e.g. 3-3.
    @return true if the tile is a double, false otherwise
    */
   public boolean isDouble() {
      return m_leftPips == m_rightPips;
   }

   /**
    Produces the same tile turned around, so it can be laid with the matching pips touching.
    @return A new Tile with the left and right pips swapped
    */
   public Tile flipped() {
      return new Tile(m_rightPips, m_leftPips);
   }

   /**
    Expresses the tile in the text form used on screen and in the save file, e.g. "2-5".
    @return The tile as a string: left pips, a dash, right pips
    */
   @Override
   public String toString() {
      return m_leftPips + "-" + m_rightPips;
   }

   /**
    Decides whether two tiles are the same physical domino, either way round (2-5 equals 5-2).
    Algorithm:
       1) A non-tile is never equal
       2) Same tile if both ends match as-is, or both ends match after flipping one tile
    Reference: Ported from my C++ version with Claude
    @param other Object, the tile to compare with (passed by reference, not modified)
    @return true if both tiles show the same pair of pip counts, false otherwise
    */
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof Tile)) {
         return false;
      }
      Tile tile = (Tile) other;
      boolean sameWay = m_leftPips == tile.m_leftPips && m_rightPips == tile.m_rightPips;
      boolean flippedWay = m_leftPips == tile.m_rightPips && m_rightPips == tile.m_leftPips;
      return sameWay || flippedWay;
   }

   /**
    Produces a hash code that is the same for a tile and its flipped form, to agree with equals().
    @return The smaller pip count times 10 plus the larger pip count
    */
   @Override
   public int hashCode() {
      return Math.min(m_leftPips, m_rightPips) * HASH_BASE + Math.max(m_leftPips, m_rightPips);
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Tells whether a number is a legal pip count for one end of a double-six domino.
    @param pips int, the value to check
    @return true when 0 &lt;= pips &lt;= 6, false otherwise
    */
   public static boolean isValidPips(int pips) {
      return pips >= MIN_PIPS && pips <= MAX_PIPS;
   }

   /**
    Reads a tile typed by the user or stored in a save file, e.g. "2-5", rejecting anything else.
    Algorithm:
       1) The text must be exactly three characters: digit, dash, digit
       2) Both digits must be legal pip counts
       3) Build the tile from the two digits
    Reference: Ported from my C++ version with Claude
    @param text String, the text to parse
    @return The parsed Tile, or null if the text is not a legal tile
    */
   public static Tile fromString(String text) {
      if (text == null || text.length() != TEXT_LENGTH || text.charAt(1) != '-') {
         return null;
      }
      if (!Character.isDigit(text.charAt(0)) || !Character.isDigit(text.charAt(2))) {
         return null;
      }
      int left = text.charAt(0) - '0';
      int right = text.charAt(2) - '0';
      if (!isValidPips(left) || !isValidPips(right)) {
         return null;
      }
      return new Tile(left, right);
   }
}

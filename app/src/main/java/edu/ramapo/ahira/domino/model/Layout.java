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
 The tiles on the table. A main line runs left (L) to right (R). The first
 double played is the spinner; it also opens an up (U) and a down (D) arm.
 Tiles are stored oriented so that touching pips match:
    main line:  line[i].right == line[i+1].left
    arms:       arm[0].left == spinner pip, arm[i].right is the open end
 Model class only: no input/output happens here.
 */
public class Layout {

   // Letters naming the four ends of the layout
   public static final char LEFT = 'L';
   public static final char RIGHT = 'R';
   public static final char UP = 'U';
   public static final char DOWN = 'D';
   // Spinner index meaning "no spinner yet"; also "no pips" for a closed end
   public static final int NO_SPINNER = -1;

   // Words that open and close the spinner group in the save-file layout text
   private static final String GROUP_START = "[D";
   private static final String GROUP_END = "U]";

   // The main line of tiles, left to right
   private final List<Tile> m_line;
   // The up arm, from the spinner outward
   private final List<Tile> m_up;
   // The down arm, from the spinner outward
   private final List<Tile> m_down;
   // Position of the spinner in m_line, or NO_SPINNER
   private int m_spinnerIndex;

   /**
    Creates an empty table with no tiles and no spinner.
    */
   public Layout() {
      m_line = new ArrayList<>();
      m_up = new ArrayList<>();
      m_down = new ArrayList<>();
      m_spinnerIndex = NO_SPINNER;
   }

   /**
    Creates an independent copy of a table, so a move can be tried without disturbing the game.
    @param other Layout, the table to copy (passed by reference, not modified)
    */
   public Layout(Layout other) {
      m_line = new ArrayList<>(other.m_line);
      m_up = new ArrayList<>(other.m_up);
      m_down = new ArrayList<>(other.m_down);
      m_spinnerIndex = other.m_spinnerIndex;
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Tells whether any tile has been placed on the table yet.
    @return true if the table is empty, false otherwise
    */
   public boolean isEmpty() {
      return m_line.isEmpty();
   }

   /**
    Tells whether the first double has been played, which opens the up and down arms.
    @return true if there is a spinner, false otherwise
    */
   public boolean hasSpinner() {
      return m_spinnerIndex != NO_SPINNER;
   }

   /**
    Reports where the spinner sits in the main line.
    @return The 0-based position of the spinner, or NO_SPINNER (-1) if there is none
    */
   public int getSpinnerIndex() {
      return m_spinnerIndex;
   }

   /**
    Hands out the main line of tiles, left to right, for display.
    @return A new list holding every tile on the main line
    */
   public List<Tile> getLine() {
      return new ArrayList<>(m_line);
   }

   /**
    Hands out the up arm, from the spinner outward, for display.
    @return A new list holding every tile on the up arm
    */
   public List<Tile> getUpArm() {
      return new ArrayList<>(m_up);
   }

   /**
    Hands out the down arm, from the spinner outward, for display.
    @return A new list holding every tile on the down arm
    */
   public List<Tile> getDownArm() {
      return new ArrayList<>(m_down);
   }

   /**
    Tells whether tiles may currently be placed at a given end of the table.
    Algorithm:
       1) Left and right are open as soon as any tile is on the table
       2) Up and down are open only once a spinner exists
       3) Any other letter is never open
    Reference: Ported from my C++ version with Claude
    @param end char, one of 'L', 'R', 'U', 'D'
    @return true if the end is open for play, false otherwise
    */
   public boolean isOpenEnd(char end) {
      if (end == LEFT || end == RIGHT) {
         return !isEmpty();
      }
      if (end == UP || end == DOWN) {
         return hasSpinner();
      }
      return false;
   }

   /**
    Reports the pip count a tile must match to be placed at a given end.
    Algorithm:
       1) A closed end has no pips
       2) L shows the left pip of the first line tile; R the right pip of the last
       3) U and D show the outer pip of the arm's last tile, or the spinner's pip when the arm is empty
    Reference: Ported from my C++ version with Claude
    @param end char, one of 'L', 'R', 'U', 'D'
    @return The pips showing at that end, or NO_SPINNER (-1) if the end is closed
    */
   public int getEndPips(char end) {
      if (!isOpenEnd(end)) {
         return NO_SPINNER;
      }
      switch (end) {
         case LEFT:
            return m_line.get(0).getLeftPips();
         case RIGHT:
            return m_line.get(m_line.size() - 1).getRightPips();
         case UP:
            return armEndPips(m_up);
         case DOWN:
            return armEndPips(m_down);
         default:
            return NO_SPINNER;
      }
   }

   /**
    Computes the total pips showing at all open ends, which scores when it is a multiple of the target.
    Algorithm:
       1) An empty table totals 0
       2) A single tile on the line counts both of its pips
       3) Otherwise add the value at the L tile and at the R tile, a double at an end counting both pips
       4) If there is a spinner, for each arm: if the arm has tiles add its outer tile's value;
          otherwise add the spinner's pip, but only when the spinner is not itself the L or R end
    Reference: Rules of 35 Domino from the course project page
    @return The open-end total, an integer (0 for an empty table)
    */
   public int getOpenEndSum() {
      if (isEmpty()) {
         return 0;
      }
      int sum;
      if (m_line.size() == 1) {
         sum = m_line.get(0).getSum();
      }
      else {
         Tile first = m_line.get(0);
         Tile last = m_line.get(m_line.size() - 1);
         sum = endValue(first, first.getLeftPips()) + endValue(last, last.getRightPips());
      }
      if (hasSpinner()) {
         int lastIndex = m_line.size() - 1;
         boolean spinnerAtEdge = m_spinnerIndex == 0 || m_spinnerIndex == lastIndex;
         sum += armValue(m_up, spinnerAtEdge) + armValue(m_down, spinnerAtEdge);
      }
      return sum;
   }

   /**
    Decides whether a tile may legally be placed at a given end right now.
    Algorithm:
       1) A missing tile or unknown end letter is never legal
       2) On an empty table any tile may start the layout
       3) A closed end is not legal
       4) Otherwise one pip of the tile must equal the pips showing at that end
    Reference: Ported from my C++ version with Claude
    @param tile Tile, the tile to place
    @param end char, one of 'L', 'R', 'U', 'D'
    @return true if the placement is legal, false otherwise
    */
   public boolean canPlace(Tile tile, char end) {
      if (tile == null || !isEndLetter(end)) {
         return false;
      }
      if (isEmpty()) {
         return true;
      }
      if (!isOpenEnd(end)) {
         return false;
      }
      int pips = getEndPips(end);
      return tile.getLeftPips() == pips || tile.getRightPips() == pips;
   }

   /**
    Tells whether a whole move (tile plus end) is legal right now.
    @param move Move, the move to check (passed by reference, not modified)
    @return true if the move is a real placement and its tile fits its end
    */
   public boolean canPlace(Move move) {
      return move != null && move.isValid() && canPlace(move.getTile(), move.getEnd());
   }

   /**
    Expresses the table in the save-file form from the course page ("L 5-3 [D 3-3 U] 3-2 2-2 R").
    Algorithm:
       1) Start with "L"
       2) Write each line tile left to right; at the spinner write "[D", the down arm from its outer
          tile inward (flipped so the text reads toward the spinner), the spinner, the up arm, "U]"
       3) End with "R"
    Reference: Save-file format from the course project page
    @return The layout as one string
    */
   @Override
   public String toString() {
      StringBuilder text = new StringBuilder();
      text.append(LEFT);
      for (int i = 0; i < m_line.size(); i++) {
         if (i == m_spinnerIndex) {
            text.append(' ').append(GROUP_START);
            for (int k = m_down.size() - 1; k >= 0; k--) {
               text.append(' ').append(m_down.get(k).flipped());
            }
            text.append(' ').append(m_line.get(i));
            for (Tile tile : m_up) {
               text.append(' ').append(tile);
            }
            text.append(' ').append(GROUP_END);
         }
         else {
            text.append(' ').append(m_line.get(i));
         }
      }
      text.append(' ').append(RIGHT);
      return text.toString();
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Puts a tile on the table at an end, oriented so touching pips match; the first double is the spinner.
    Algorithm:
       1) Refuse an illegal move
       2) Empty table: the tile becomes the line; if it is a double it is the spinner
       3) L: flip so the tile's right pip touches the line; insert at the front (spinner index shifts)
       4) R: flip so the tile's left pip touches the line; append
       5) U/D: flip so the tile's left pip touches the arm; append to that arm
       6) A double placed on the line when there is no spinner yet becomes the spinner
    Reference: Ported from my C++ version with Claude
    @param tile Tile, the tile to place
    @param end char, one of 'L', 'R', 'U', 'D'
    @return true if the tile was placed; false (and no change) if the move is illegal
    */
   public boolean place(Tile tile, char end) {
      if (!canPlace(tile, end)) {
         return false;
      }
      if (isEmpty()) {
         m_line.add(tile);
         if (tile.isDouble()) {
            m_spinnerIndex = 0;
         }
         return true;
      }
      int pips = getEndPips(end);
      if (end == LEFT) {
         placeLeft(tile.getRightPips() == pips ? tile : tile.flipped());
      }
      else if (end == RIGHT) {
         placeRight(tile.getLeftPips() == pips ? tile : tile.flipped());
      }
      else {
         Tile oriented = tile.getLeftPips() == pips ? tile : tile.flipped();
         (end == UP ? m_up : m_down).add(oriented);
      }
      return true;
   }

   /**
    Places a whole move (tile plus end) on the table.
    @param move Move, the move to make (passed by reference, not modified)
    @return true if the tile was placed; false (no change) if the move is invalid or illegal
    */
   public boolean place(Move move) {
      return move != null && move.isValid() && place(move.getTile(), move.getEnd());
   }

   /**
    Rebuilds the table from the layout line of a save file, so a saved round can be resumed.
    Algorithm:
       1) Split the text into words; drop a leading "L" and a trailing "R"
       2) Tiles before "[D" are the left part of the line, tiles between "[D" and "U]" are the
          spinner group, tiles after "U]" are the right part
       3) No group: the line is the left part alone and there is no spinner
       4) With a group: orient it as a chain; the spinner is the double in the group that fits the
          neighbouring line tiles; tiles before it are the down arm (reversed, flipped), after it the up arm
       5) Orient the whole line as a chain; if anything fails, change nothing
    Reference: Save-file format from the course project page
    @param text String, the layout text, e.g. "L 5-3 [D 3-3 U] 3-2 2-2 R"
    @return true if the table was rebuilt; false (and no change) if the text is not a legal layout
    */
   public boolean fromString(String text) {
      List<String> words = splitLayoutWords(text);
      // Tiles left of the spinner group, inside it, and right of it
      List<Tile> leftPart = new ArrayList<>();
      List<Tile> group = new ArrayList<>();
      List<Tile> rightPart = new ArrayList<>();
      if (words == null || !sortLayoutWords(words, leftPart, group, rightPart)) {
         return false;
      }
      boolean groupSeen = !group.isEmpty();
      if (!groupSeen && words.contains(GROUP_START)) {
         // "[D U]" with no spinner inside it
         return false;
      }
      List<Tile> newLine = new ArrayList<>();
      List<Tile> newUp = new ArrayList<>();
      List<Tile> newDown = new ArrayList<>();
      int newSpinner = NO_SPINNER;
      if (!groupSeen) {
         newLine.addAll(leftPart);
      }
      else {
         newSpinner = splitSpinnerGroup(group, leftPart, rightPart, newDown, newUp);
         if (newSpinner == NO_SPINNER) {
            return false;
         }
         newLine.addAll(leftPart);
         newLine.add(group.get(newSpinner));
         newLine.addAll(rightPart);
         newSpinner = leftPart.size();
      }
      if (!orientChain(newLine)) {
         return false;
      }
      installTiles(newLine, newUp, newDown, newSpinner);
      return true;
   }

   /**
    Empties the table at the start of a new round.
    */
   public void clear() {
      installTiles(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), NO_SPINNER);
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Turns an end letter into a word for messages to the user.
    @param end char, one of 'L', 'R', 'U', 'D'
    @return "Left", "Right", "Up", or "Down"; "?" for any other letter
    */
   public static String endName(char end) {
      switch (end) {
         case LEFT:
            return "Left";
         case RIGHT:
            return "Right";
         case UP:
            return "Up";
         case DOWN:
            return "Down";
         default:
            return "?";
      }
   }

   /**
    Tells whether a character names one of the four ends.
    @param end char, the letter to check
    @return true for 'L', 'R', 'U', or 'D'; false otherwise
    */
   public static boolean isEndLetter(char end) {
      return end == LEFT || end == RIGHT || end == UP || end == DOWN;
   }

   /**
    Lists the four end letters in the order they are offered to players.
    @return A new array holding 'L', 'R', 'U', 'D'
    */
   public static char[] allEnds() {
      return new char[] { LEFT, RIGHT, UP, DOWN };
   }

   /**
    Reports the pip count of the spinner, which the first tile on each arm must match.
    @return The spinner's pips, or NO_SPINNER (-1) if there is no spinner
    */
   private int spinnerPips() {
      if (!hasSpinner()) {
         return NO_SPINNER;
      }
      return m_line.get(m_spinnerIndex).getLeftPips();
   }

   /**
    Reports the pips showing at the open end of one arm of the spinner.
    @param arm List of Tile, the arm (passed by reference, not modified)
    @return The outer pip of the arm's last tile, or the spinner's pip if the arm is empty
    */
   private int armEndPips(List<Tile> arm) {
      return arm.isEmpty() ? spinnerPips() : arm.get(arm.size() - 1).getRightPips();
   }

   /**
    Computes what one arm adds to the open-end total.
    @param arm List of Tile, the arm (passed by reference, not modified)
    @param spinnerAtEdge boolean, true when the spinner is itself the L or R end of the line
    @return The outer tile's value; for an empty arm the spinner's pip, or 0 if the spinner is at an edge
    */
   private int armValue(List<Tile> arm, boolean spinnerAtEdge) {
      if (!arm.isEmpty()) {
         Tile outer = arm.get(arm.size() - 1);
         return endValue(outer, outer.getRightPips());
      }
      return spinnerAtEdge ? 0 : spinnerPips();
   }

   /**
    Computes what a tile at an open end contributes: a double counts both pips, any other tile its exposed pip.
    Reference: Rules of 35 Domino from the course project page
    @param tile Tile, the tile at the end
    @param exposedPips int, the pip count facing outward
    @return The tile's contribution to the open-end total
    */
   private static int endValue(Tile tile, int exposedPips) {
      return tile.isDouble() ? tile.getSum() : exposedPips;
   }

   /**
    Adds an already-oriented tile to the left of the main line, shifting the spinner and making a first double the spinner.
    @param tile Tile, the tile to add, with its right pip touching the line
    */
   private void placeLeft(Tile tile) {
      m_line.add(0, tile);
      if (hasSpinner()) {
         m_spinnerIndex++;
      }
      else if (tile.isDouble()) {
         m_spinnerIndex = 0;
      }
   }

   /**
    Adds an already-oriented tile to the right of the main line, making a first double the spinner.
    @param tile Tile, the tile to add, with its left pip touching the line
    */
   private void placeRight(Tile tile) {
      m_line.add(tile);
      if (!hasSpinner() && tile.isDouble()) {
         m_spinnerIndex = m_line.size() - 1;
      }
   }

   /**
    Replaces every tile on the table at once.
    @param line List of Tile, the new main line (its contents are copied)
    @param up List of Tile, the new up arm (its contents are copied)
    @param down List of Tile, the new down arm (its contents are copied)
    @param spinnerIndex int, position of the spinner in the new line, or NO_SPINNER
    */
   private void installTiles(List<Tile> line, List<Tile> up, List<Tile> down, int spinnerIndex) {
      m_line.clear();
      m_line.addAll(line);
      m_up.clear();
      m_up.addAll(up);
      m_down.clear();
      m_down.addAll(down);
      m_spinnerIndex = spinnerIndex;
   }

   /**
    Splits the layout text into words, dropping the leading "L" and trailing "R".
    @param text String, the layout text
    @return The words between L and R, or null if there is no text
    */
   private static List<String> splitLayoutWords(String text) {
      if (text == null) {
         return null;
      }
      List<String> words = new ArrayList<>();
      for (String word : text.trim().split("\\s+")) {
         if (!word.isEmpty()) {
            words.add(word);
         }
      }
      if (!words.isEmpty() && words.get(0).equals(String.valueOf(LEFT))) {
         words.remove(0);
      }
      if (!words.isEmpty() && words.get(words.size() - 1).equals(String.valueOf(RIGHT))) {
         words.remove(words.size() - 1);
      }
      return words;
   }

   /**
    Sorts the layout words into the tiles left of, inside, and right of the spinner group.
    Algorithm:
       1) "[D" starts the group (only once); "U]" ends it (only after it started)
       2) Every other word must be a tile; it goes to whichever part is current
       3) The group must have been closed by the end
    Reference: Save-file format from the course project page
    @param words List of String, the words between L and R (passed by reference, not modified)
    @param leftPart List of Tile, passed by reference; receives tiles before the group
    @param group List of Tile, passed by reference; receives tiles inside the group
    @param rightPart List of Tile, passed by reference; receives tiles after the group
    @return true if every word was understood, false otherwise
    */
   private static boolean sortLayoutWords(List<String> words, List<Tile> leftPart,
                                          List<Tile> group, List<Tile> rightPart) {
      boolean inGroup = false;
      boolean groupSeen = false;
      for (String word : words) {
         if (word.equals(GROUP_START)) {
            if (groupSeen) {
               return false;
            }
            inGroup = true;
            groupSeen = true;
            continue;
         }
         if (word.equals(GROUP_END)) {
            if (!inGroup) {
               return false;
            }
            inGroup = false;
            continue;
         }
         Tile tile = Tile.fromString(word);
         if (tile == null) {
            return false;
         }
         (inGroup ? group : (groupSeen ? rightPart : leftPart)).add(tile);
      }
      return !inGroup;
   }

   /**
    Finds the spinner inside the saved spinner group and splits the rest of the group into the two arms.
    Algorithm:
       1) Orient the group as a chain; fail if it does not connect
       2) The spinner is the first double whose pips fit both neighbouring line tiles
       3) Tiles before the spinner become the down arm, reversed and flipped so they read outward
       4) Tiles after the spinner become the up arm
    Reference: Save-file format from the course project page
    @param group List of Tile, passed by reference; the group's tiles, flipped in place to form a chain
    @param leftPart List of Tile, the line tiles left of the group (not modified)
    @param rightPart List of Tile, the line tiles right of the group (not modified)
    @param down List of Tile, passed by reference; receives the down arm
    @param up List of Tile, passed by reference; receives the up arm
    @return The spinner's position inside the group, or NO_SPINNER if none fits
    */
   private static int splitSpinnerGroup(List<Tile> group, List<Tile> leftPart, List<Tile> rightPart,
                                        List<Tile> down, List<Tile> up) {
      if (!orientChain(group)) {
         return NO_SPINNER;
      }
      Tile leftNeighbour = leftPart.isEmpty() ? null : leftPart.get(leftPart.size() - 1);
      Tile rightNeighbour = rightPart.isEmpty() ? null : rightPart.get(0);
      for (int k = 0; k < group.size(); k++) {
         Tile candidate = group.get(k);
         if (candidate.isDouble() && touches(leftNeighbour, candidate.getLeftPips())
             && touches(rightNeighbour, candidate.getLeftPips())) {
            for (int i = k - 1; i >= 0; i--) {
               down.add(group.get(i).flipped());
            }
            up.addAll(group.subList(k + 1, group.size()));
            return k;
         }
      }
      return NO_SPINNER;
   }

   /**
    Tells whether a neighbouring tile shows a pip count on either end (no neighbour always fits).
    @param neighbour Tile, the neighbouring tile, or null for none
    @param pips int, the pip count to look for
    @return true if there is no neighbour or one of its ends shows the pips
    */
   private static boolean touches(Tile neighbour, int pips) {
      return neighbour == null || neighbour.getLeftPips() == pips || neighbour.getRightPips() == pips;
   }

   /**
    Turns tiles read from a save file so each tile's right pip touches the next tile's left pip.
    Algorithm:
       1) A chain of fewer than two tiles is already fine
       2) For the first tile: if its right pip does not touch tile 2 but its left pip does, flip it
       3) For every later tile: flip it if needed so its left pip equals the previous tile's right
          pip; fail if neither orientation touches
    Reference: Ported from my C++ version with Claude
    @param tiles List of Tile, passed by reference; tiles in table order, individual tiles flipped in place
    @return true if every tile could be made to touch its neighbour, false otherwise
    */
   private static boolean orientChain(List<Tile> tiles) {
      if (tiles.size() < 2) {
         return true;
      }
      Tile first = tiles.get(0);
      Tile second = tiles.get(1);
      boolean rightTouches = touches(second, first.getRightPips());
      boolean leftTouches = touches(second, first.getLeftPips());
      if (!rightTouches && leftTouches) {
         tiles.set(0, first.flipped());
      }
      for (int i = 1; i < tiles.size(); i++) {
         int need = tiles.get(i - 1).getRightPips();
         if (tiles.get(i).getLeftPips() != need) {
            tiles.set(i, tiles.get(i).flipped());
         }
         if (tiles.get(i).getLeftPips() != need) {
            return false;
         }
      }
      return true;
   }
}

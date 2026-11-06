/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.model;

/**
 Converts a game record to and from the course's text save-file format.
 It works on text only; reading and writing the file itself is done by the
 controller, so the model stays free of Android file handling.
 */
public final class Serializer {

   // Labels used in the save file
   private static final String TOURNAMENT_LABEL = "Tournament Score:";
   private static final String TARGET_LABEL = "Target Score:";
   private static final String HAND_LABEL = "Hand:";
   private static final String CURRENT_LABEL = "Current Score:";
   private static final String LAYOUT_LABEL = "Layout:";
   private static final String BONEYARD_LABEL = "Boneyard:";
   private static final String NEXT_LABEL = "Next Player:";
   // The course example lists the Computer first, then the Human
   private static final String[] PLAYER_ORDER = { Computer.NAME, Human.NAME };
   // Indentation of the lines inside a player's section
   private static final String INDENT = "   ";
   // Returned by parseCount for a value that is not a non-negative whole number
   private static final int BAD_NUMBER = -1;

   /**
    No objects are made: every function is static.
    */
   private Serializer() {
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Writes a game record as save-file text in the course's format.
    Algorithm:
       1) Write the tournament score and the round target
       2) For the Computer and then the Human: the name, hand, current score, tournament score
       3) Write the layout, the boneyard, and the next player
    Reference: Save-file format from the course project page
    @param state GameState, the game to write (passed by reference, not modified)
    @return The complete save-file text
    */
   public static String write(GameState state) {
      StringBuilder out = new StringBuilder();
      out.append(TOURNAMENT_LABEL).append(' ').append(state.getTournamentScore()).append("\n\n");
      out.append(TARGET_LABEL).append(' ').append(state.getTargetScore()).append("\n\n");
      for (String name : PLAYER_ORDER) {
         PlayerState player = state.getPlayer(name);
         if (player == null) {
            continue;
         }
         out.append(name).append(":\n");
         out.append(INDENT).append(HAND_LABEL).append(' ').append(player.getHand()).append('\n');
         out.append(INDENT).append(CURRENT_LABEL).append(' ').append(player.getRoundScore()).append('\n');
         out.append(INDENT).append(TOURNAMENT_LABEL).append(' ').append(player.getTournamentScore())
            .append("\n\n");
      }
      out.append(LAYOUT_LABEL).append("   ").append(state.getLayoutText()).append("\n\n");
      out.append(BONEYARD_LABEL).append("  ").append(state.getBoneyardText()).append("\n\n");
      out.append(NEXT_LABEL).append(' ').append(state.getNextPlayer()).append('\n');
      return out.toString();
   }

   /**
    Reads a game record from save-file text, rejecting anything malformed.
    Algorithm:
       1) Read line by line, ignoring blank lines and remembering which player section we are in
       2) A line that is just "Name:" starts that player's section
       3) Every other line must be one of the labelled lines; hand it to readLabelledLine
       4) Succeed only if a next player was named and that player has a section
    Reference: Save-file format from the course project page
    @param text String, the whole save file
    @return The game record, or null if the text is not a valid save file
    */
   public static GameState read(String text) {
      if (text == null) {
         return null;
      }
      GameState state = new GameState();
      // Name of the player whose section we are reading; empty outside a section
      String section = "";
      for (String rawLine : text.split("\\r?\\n")) {
         String line = rawLine.trim();
         if (line.isEmpty()) {
            continue;
         }
         if (isSectionHeader(line)) {
            section = line.substring(0, line.length() - 1);
            state.addPlayer(section);
            continue;
         }
         section = readLabelledLine(line, section, state);
         if (section == null) {
            return null;
         }
      }
      String next = state.getNextPlayer();
      return !next.isEmpty() && state.hasPlayer(next) ? state : null;
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Tells whether a line starts a player's section, e.g. "Computer:".
    An empty labelled line such as "Hand:" (empty hand) or "Boneyard:" (empty stock) is not a section.
    @param line String, the trimmed line
    @return true if the line is a single word ending in a colon that is not one of the labels
    */
   private static boolean isSectionHeader(String line) {
      if (!line.endsWith(":") || line.indexOf(' ') >= 0) {
         return false;
      }
      return !line.equals(HAND_LABEL) && !line.equals(LAYOUT_LABEL) && !line.equals(BONEYARD_LABEL);
   }

   /**
    Stores the value of one labelled line of the save file in the game record.
    Algorithm:
       1) "Tournament Score:" outside a section is the goal; inside a section, that player's total
       2) "Target Score:", "Hand:", and "Current Score:" fill the matching field
       3) "Layout:", "Boneyard:", and "Next Player:" fill their field and end any player section
       4) Anything else, or a value that cannot be read, is an error
    Reference: Save-file format from the course project page
    @param line String, the trimmed line
    @param section String, the player section being read, or empty
    @param state GameState, passed by reference; the matching field is filled in
    @return The player section in effect after this line, or null if the line is not valid
    */
   private static String readLabelledLine(String line, String section, GameState state) {
      PlayerState player = section.isEmpty() ? null : state.getPlayer(section);
      if (line.startsWith(TOURNAMENT_LABEL)) {
         int value = parseCount(valueAfter(line, TOURNAMENT_LABEL));
         boolean stored = player == null ? state.setTournamentScore(value)
                                         : player.setTournamentScore(value);
         return stored ? section : null;
      }
      if (line.startsWith(TARGET_LABEL)) {
         return state.setTargetScore(parseCount(valueAfter(line, TARGET_LABEL))) ? section : null;
      }
      if (line.startsWith(HAND_LABEL)) {
         Hand hand = parseHand(valueAfter(line, HAND_LABEL));
         return player != null && player.setHand(hand) ? section : null;
      }
      if (line.startsWith(CURRENT_LABEL)) {
         int value = parseCount(valueAfter(line, CURRENT_LABEL));
         return player != null && player.setRoundScore(value) ? section : null;
      }
      if (line.startsWith(LAYOUT_LABEL)) {
         state.setLayoutText(valueAfter(line, LAYOUT_LABEL));
         return "";
      }
      if (line.startsWith(BONEYARD_LABEL)) {
         state.setBoneyardText(valueAfter(line, BONEYARD_LABEL));
         return "";
      }
      if (line.startsWith(NEXT_LABEL)) {
         state.setNextPlayer(valueAfter(line, NEXT_LABEL));
         return "";
      }
      // A line we do not understand means the text is not a save file
      return null;
   }

   /**
    Separates the value of a labelled line from its label.
    @param line String, the line, which starts with the label
    @param label String, the label, e.g. "Hand:"
    @return The trimmed text after the label
    */
   private static String valueAfter(String line, String label) {
      return line.substring(label.length()).trim();
   }

   /**
    Reads a non-negative whole number from the start of a value, e.g. "35" or "35 points".
    @param value String, the text after a label
    @return The number, or BAD_NUMBER (-1) if the value does not start with a whole number
    */
   private static int parseCount(String value) {
      String first = value.trim().split("\\s+")[0];
      try {
         int number = Integer.parseInt(first);
         return number < 0 ? BAD_NUMBER : number;
      }
      catch (NumberFormatException notANumber) {
         return BAD_NUMBER;
      }
   }

   /**
    Reads a player's tiles from the text after "Hand:".
    Algorithm:
       1) Start an empty hand
       2) For each word: parse it as a tile and add it; fail on a bad tile or a repeat
    Reference: Ported from my C++ version with Claude
    @param text String, e.g. "0-1 2-5 4-4" (may be empty for an empty hand)
    @return The hand, or null if any word is not a legal, unrepeated tile
    */
   private static Hand parseHand(String text) {
      Hand hand = new Hand();
      for (String word : text.trim().split("\\s+")) {
         if (word.isEmpty()) {
            continue;
         }
         if (!hand.addTile(Tile.fromString(word))) {
            return null;
         }
      }
      return hand;
   }
}

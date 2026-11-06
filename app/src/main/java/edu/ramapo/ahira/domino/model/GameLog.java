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
 The running record of everything that happened in the game, in order.
 The model only adds lines; the view shows them in one scrollable window.
 */
public class GameLog {

   // Every message so far, oldest first
   private final List<String> m_lines;

   /**
    Creates an empty log.
    */
   public GameLog() {
      m_lines = new ArrayList<>();
   }

   /* *********************************************************************
   Selectors
   ********************************************************************* */

   /**
    Reports how many messages have been logged.
    @return The number of lines, an integer
    */
   public int size() {
      return m_lines.size();
   }

   /**
    Hands out every message, oldest first, for display.
    @return A new list holding every line
    */
   public List<String> getLines() {
      return new ArrayList<>(m_lines);
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Records one message. Missing or blank text is ignored.
    @param line String, the message to record
    */
   public void add(String line) {
      if (line != null && !line.trim().isEmpty()) {
         m_lines.add(line);
      }
   }
}

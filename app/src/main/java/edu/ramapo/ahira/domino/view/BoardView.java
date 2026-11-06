/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.List;

import edu.ramapo.ahira.domino.model.Layout;
import edu.ramapo.ahira.domino.model.Tile;

/**
 Draws the layout (the tiles on the table). The main line runs left to
 right; doubles are drawn cross-wise; the spinner is outlined in gold and
 its up and down arms grow above and below it. Every open end is labelled
 with its letter and the pips showing there, and the open-end total is
 shown in the corner. The whole picture is scaled to fit the view.
 The view reads the model through update(); it never changes it.
 */
public class BoardView extends View {

   // Gap between neighbouring tiles, as a fraction of a unit
   private static final float GAP = 0.08f;
   // Room left for an end label beside the tiles, in units
   private static final float LABEL_ROOM = 1.7f;
   // Height of the header line ("Open-end total"), in sp
   private static final float HEADER_SP = 16f;
   // Largest unit (tile square) in dp, so a near-empty board does not look huge
   private static final float MAX_UNIT_DP = 44f;
   // Padding around the picture, in dp
   private static final float PADDING_DP = 8f;

   // The table to draw; a private copy, read only
   private Layout m_layout;
   // Draws each tile
   private final TilePainter m_painter;
   // Paints for the header and the open-end badges
   private final Paint m_textPaint;
   private final Paint m_badgePaint;
   private final Paint m_badgeTextPaint;
   // Reused rectangle for the badges
   private final RectF m_badgeRect;

   /**
    Creates the board when it is inflated from the screen's XML layout.
    @param context Context, the screen the view belongs to
    @param attributes AttributeSet, the XML attributes of the view
    */
   public BoardView(Context context, AttributeSet attributes) {
      super(context, attributes);
      m_layout = new Layout();
      m_painter = new TilePainter();
      m_textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      m_textPaint.setColor(Color.WHITE);
      m_textPaint.setTextSize(spToPixels(HEADER_SP));
      m_textPaint.setTypeface(Typeface.DEFAULT_BOLD);
      m_badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      m_badgePaint.setColor(Color.rgb(13, 71, 41));
      m_badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      m_badgeTextPaint.setColor(Color.rgb(255, 235, 59));
      m_badgeTextPaint.setTextAlign(Paint.Align.CENTER);
      m_badgeTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
      m_badgeRect = new RectF();
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Shows a new state of the table.
    @param layout Layout, the table to draw (a copy handed out by the model)
    */
   public void update(Layout layout) {
      m_layout = layout == null ? new Layout() : layout;
      invalidate();
   }

   /* *********************************************************************
   Source code to draw the game board on the screen
   ********************************************************************* */

   /**
    Draws the whole board.
    Algorithm:
       1) Paint the felt; write the open-end total in the corner
       2) An empty table: say so and stop
       3) Choose a unit size so the line, both arms, and the labels fit
       4) Draw the main line, then the up and down arms from the spinner, then the end labels
    Reference: Custom views, from https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing
    @param canvas Canvas, passed by reference; the board is drawn on it
    */
   @Override
   protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      canvas.drawColor(Color.rgb(27, 94, 32));
      float padding = dpToPixels(PADDING_DP);
      float headerHeight = m_textPaint.getTextSize() * 1.6f;
      if (m_layout.isEmpty()) {
         canvas.drawText("The layout is empty: the first tile played starts it.", padding,
                         padding + m_textPaint.getTextSize(), m_textPaint);
         return;
      }
      canvas.drawText("Open-end total: " + m_layout.getOpenEndSum(), padding,
                      padding + m_textPaint.getTextSize(), m_textPaint);
      float availableWidth = getWidth() - 2 * padding;
      float availableHeight = getHeight() - 2 * padding - headerHeight;
      float unit = chooseUnit(availableWidth, availableHeight);
      float lineLeft = (getWidth() - widthInUnits() * unit) / 2 + LABEL_ROOM * unit;
      float extraHeight = availableHeight - heightInUnits() * unit;
      float lineCentre = padding + headerHeight + extraHeight / 2 + unitsAboveLine() * unit;
      float spinnerCentre = drawLine(canvas, lineLeft, lineCentre, unit);
      if (m_layout.hasSpinner()) {
         drawUpArm(canvas, spinnerCentre, lineCentre - unit, unit);
         drawDownArm(canvas, spinnerCentre, lineCentre + unit, unit);
      }
   }

   /**
    Draws the main line from left to right, with doubles cross-wise and the L and R labels.
    @param canvas Canvas, passed by reference; the line is drawn on it
    @param left float, x where the first tile starts
    @param centre float, y of the line's middle
    @param unit float, side of a tile square in pixels
    @return The x of the spinner's middle, or 0 if there is no spinner
    */
   private float drawLine(Canvas canvas, float left, float centre, float unit) {
      List<Tile> line = m_layout.getLine();
      float x = left;
      float spinnerCentre = 0;
      for (int i = 0; i < line.size(); i++) {
         Tile tile = line.get(i);
         boolean spinner = i == m_layout.getSpinnerIndex();
         int highlight = spinner ? TilePainter.HIGHLIGHT_SPINNER : TilePainter.NO_HIGHLIGHT;
         if (tile.isDouble()) {
            m_painter.drawTile(canvas, x, centre - unit, unit, tile.getLeftPips(),
                               tile.getRightPips(), true, highlight);
            spinnerCentre = spinner ? x + unit / 2 : spinnerCentre;
            x += unit;
         }
         else {
            m_painter.drawTile(canvas, x, centre - unit / 2, unit, tile.getLeftPips(),
                               tile.getRightPips(), false, highlight);
            x += 2 * unit;
         }
         x += GAP * unit;
      }
      drawBadge(canvas, left - LABEL_ROOM * unit / 2, centre, unit, Layout.LEFT);
      drawBadge(canvas, x - GAP * unit + LABEL_ROOM * unit / 2, centre, unit, Layout.RIGHT);
      return spinnerCentre;
   }

   /**
    Draws the up arm, growing upward from the top of the spinner, and its U label.
    A tile's inner pip (touching the arm) is drawn at the bottom; doubles lie cross-wise.
    @param canvas Canvas, passed by reference; the arm is drawn on it
    @param centreX float, x of the spinner's middle
    @param bottom float, y of the spinner's top edge
    @param unit float, side of a tile square in pixels
    */
   private void drawUpArm(Canvas canvas, float centreX, float bottom, float unit) {
      float y = bottom;
      for (Tile tile : m_layout.getUpArm()) {
         y -= GAP * unit;
         if (tile.isDouble()) {
            y -= unit;
            m_painter.drawTile(canvas, centreX - unit, y, unit, tile.getLeftPips(),
                               tile.getRightPips(), false, TilePainter.NO_HIGHLIGHT);
         }
         else {
            y -= 2 * unit;
            m_painter.drawTile(canvas, centreX - unit / 2, y, unit, tile.getRightPips(),
                               tile.getLeftPips(), true, TilePainter.NO_HIGHLIGHT);
         }
      }
      drawBadge(canvas, centreX, y - LABEL_ROOM * unit / 2, unit, Layout.UP);
   }

   /**
    Draws the down arm, growing downward from the bottom of the spinner, and its D label.
    A tile's inner pip (touching the arm) is drawn at the top; doubles lie cross-wise.
    @param canvas Canvas, passed by reference; the arm is drawn on it
    @param centreX float, x of the spinner's middle
    @param top float, y of the spinner's bottom edge
    @param unit float, side of a tile square in pixels
    */
   private void drawDownArm(Canvas canvas, float centreX, float top, float unit) {
      float y = top;
      for (Tile tile : m_layout.getDownArm()) {
         y += GAP * unit;
         if (tile.isDouble()) {
            m_painter.drawTile(canvas, centreX - unit, y, unit, tile.getLeftPips(),
                               tile.getRightPips(), false, TilePainter.NO_HIGHLIGHT);
            y += unit;
         }
         else {
            m_painter.drawTile(canvas, centreX - unit / 2, y, unit, tile.getLeftPips(),
                               tile.getRightPips(), true, TilePainter.NO_HIGHLIGHT);
            y += 2 * unit;
         }
      }
      drawBadge(canvas, centreX, y + LABEL_ROOM * unit / 2, unit, Layout.DOWN);
   }

   /**
    Draws the label for one open end: its letter on top and the pips showing there below.
    @param canvas Canvas, passed by reference; the label is drawn on it
    @param centreX float, x of the label's middle
    @param centreY float, y of the label's middle
    @param unit float, side of a tile square in pixels
    @param end char, the end letter: 'L', 'R', 'U', or 'D'
    */
   private void drawBadge(Canvas canvas, float centreX, float centreY, float unit, char end) {
      float half = unit * 0.7f;
      m_badgeRect.set(centreX - half, centreY - half, centreX + half, centreY + half);
      canvas.drawRoundRect(m_badgeRect, unit * 0.2f, unit * 0.2f, m_badgePaint);
      m_badgeTextPaint.setTextSize(unit * 0.55f);
      canvas.drawText(String.valueOf(end), centreX, centreY - unit * 0.08f, m_badgeTextPaint);
      m_badgeTextPaint.setColor(Color.WHITE);
      canvas.drawText(String.valueOf(m_layout.getEndPips(end)), centreX, centreY + unit * 0.5f,
                      m_badgeTextPaint);
      m_badgeTextPaint.setColor(Color.rgb(255, 235, 59));
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Chooses the size of one tile square so the whole layout fits the space available.
    @param availableWidth float, pixels across
    @param availableHeight float, pixels down
    @return The unit in pixels: the largest that fits, but no bigger than MAX_UNIT_DP
    */
   private float chooseUnit(float availableWidth, float availableHeight) {
      float byWidth = availableWidth / widthInUnits();
      float byHeight = availableHeight / heightInUnits();
      return Math.max(1f, Math.min(dpToPixels(MAX_UNIT_DP), Math.min(byWidth, byHeight)));
   }

   /**
    Measures the picture across: the main line plus room for the L and R labels.
    @return The width in units
    */
   private float widthInUnits() {
      return lengthInUnits(m_layout.getLine(), true) + 2 * LABEL_ROOM;
   }

   /**
    Measures the picture top to bottom: the line, both arms, and the U and D labels.
    @return The height in units
    */
   private float heightInUnits() {
      return unitsAboveLine() + unitsBelowLine();
   }

   /**
    Measures how far the picture reaches above the middle of the main line.
    @return The height above the line's middle, in units
    */
   private float unitsAboveLine() {
      if (!m_layout.hasSpinner()) {
         return 1;
      }
      return 1 + lengthInUnits(m_layout.getUpArm(), false) + LABEL_ROOM;
   }

   /**
    Measures how far the picture reaches below the middle of the main line.
    @return The depth below the line's middle, in units
    */
   private float unitsBelowLine() {
      if (!m_layout.hasSpinner()) {
         return 1;
      }
      return 1 + lengthInUnits(m_layout.getDownArm(), false) + LABEL_ROOM;
   }

   /**
    Measures a row of tiles along its direction: a double takes 1 unit (cross-wise), any other 2.
    @param tiles List of Tile, the row (passed by reference, not modified)
    @param withinLine boolean, true for the main line (gaps between tiles), false for an arm
                      (a gap before every tile, including the first)
    @return The row's length in units
    */
   private static float lengthInUnits(List<Tile> tiles, boolean withinLine) {
      float length = 0;
      for (Tile tile : tiles) {
         length += (tile.isDouble() ? 1 : 2) + GAP;
      }
      return withinLine && !tiles.isEmpty() ? length - GAP : length;
   }

   /**
    Converts density-independent pixels to screen pixels.
    @param dp float, the size in dp
    @return The size in pixels
    */
   private float dpToPixels(float dp) {
      return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                                       getResources().getDisplayMetrics());
   }

   /**
    Converts scale-independent pixels (text sizes) to screen pixels.
    @param sp float, the size in sp
    @return The size in pixels
    */
   private float spToPixels(float sp) {
      return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                                       getResources().getDisplayMetrics());
   }
}

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
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import edu.ramapo.ahira.domino.model.Tile;

/**
 Draws a row of tiles that wraps onto more rows when needed: a player's
 hand (each tile numbered 1, 2, 3, ...) or the boneyard (top tile marked).
 When selectable, tapping a tile tells the listener which one was tapped.
 The view reads the model through update(); it never changes it.
 */
public class HandView extends View {

   // Means "no tile is selected"
   public static final int NO_SELECTION = -1;

   // Gap between tiles, as a fraction of a unit
   private static final float GAP = 0.25f;
   // Height of the number above each tile, as a fraction of a unit
   private static final float LABEL_FRACTION = 0.55f;
   // Default tile square size in dp
   private static final float DEFAULT_UNIT_DP = 24f;

   /**
    Receives taps on tiles.
    */
   public interface OnTileTappedListener {
      /**
       Called when a tile is tapped.
       @param index int, 0-based position of the tapped tile
       */
      void onTileTapped(int index);
   }

   // The tiles to draw, in order
   private final List<Tile> m_tiles;
   // Position of the selected tile, or NO_SELECTION
   private int m_selected;
   // Side of one tile square, in pixels
   private float m_unit;
   // true for the boneyard: the first tile is labelled "top" instead of numbering every tile
   private boolean m_markTopOnly;
   // Who to tell about taps; null means the view does not respond to taps
   private OnTileTappedListener m_listener;
   // Draws each tile
   private final TilePainter m_painter;
   // Paint for the numbers above the tiles and the "no tiles" message
   private final Paint m_labelPaint;

   /**
    Creates the view when it is inflated from the screen's XML layout.
    @param context Context, the screen the view belongs to
    @param attributes AttributeSet, the XML attributes of the view
    */
   public HandView(Context context, AttributeSet attributes) {
      super(context, attributes);
      m_tiles = new ArrayList<>();
      m_selected = NO_SELECTION;
      m_unit = dpToPixels(DEFAULT_UNIT_DP);
      m_markTopOnly = false;
      m_listener = null;
      m_painter = new TilePainter();
      m_labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      m_labelPaint.setColor(Color.rgb(236, 239, 241));
      m_labelPaint.setTextAlign(Paint.Align.CENTER);
   }

   /* *********************************************************************
   Event handlers
   ********************************************************************* */

   /**
    Reports a tap on a tile to the listener.
    Algorithm:
       1) Ignore touches when no listener is set
       2) On finger up, find the tile under the finger and tell the listener
    Reference: Handling touch, from https://developer.android.com/develop/ui/views/touch-and-input/gestures
    @param event MotionEvent, the touch
    @return true if the touch was handled
    */
   @Override
   public boolean onTouchEvent(MotionEvent event) {
      if (m_listener == null) {
         return super.onTouchEvent(event);
      }
      if (event.getAction() == MotionEvent.ACTION_UP) {
         int index = tileAt(event.getX(), event.getY());
         performClick();
         if (index != NO_SELECTION) {
            m_listener.onTileTapped(index);
         }
      }
      return true;
   }

   /**
    Lets accessibility services click the view; the tile is chosen in onTouchEvent.
    @return The result of the standard click handling
    */
   @Override
   public boolean performClick() {
      return super.performClick();
   }

   /* *********************************************************************
   Mutators
   ********************************************************************* */

   /**
    Shows a new set of tiles and which one is selected.
    @param tiles List of Tile, the tiles to show (copied, not modified)
    @param selected int, position of the selected tile, or NO_SELECTION
    */
   public void update(List<Tile> tiles, int selected) {
      m_tiles.clear();
      if (tiles != null) {
         m_tiles.addAll(tiles);
      }
      m_selected = selected >= 0 && selected < m_tiles.size() ? selected : NO_SELECTION;
      requestLayout();
      invalidate();
   }

   /**
    Sets how big the tiles are drawn.
    @param unitDp float, side of one tile square in dp; values below 8 are ignored
    */
   public void setTileUnit(float unitDp) {
      if (unitDp >= 8) {
         m_unit = dpToPixels(unitDp);
         requestLayout();
      }
   }

   /**
    Chooses the boneyard style (first tile labelled "top") or the hand style (tiles numbered).
    @param markTopOnly boolean, true for the boneyard style
    */
   public void setMarkTopOnly(boolean markTopOnly) {
      m_markTopOnly = markTopOnly;
      invalidate();
   }

   /**
    Sets who is told about taps; null makes the tiles not respond to taps.
    @param listener OnTileTappedListener, the receiver of taps
    */
   public void setOnTileTappedListener(OnTileTappedListener listener) {
      m_listener = listener;
   }

   /* *********************************************************************
   Source code to draw the hand on the screen
   ********************************************************************* */

   /**
    Works out the height needed to show every tile at the width given.
    @param widthSpec int, the width constraint from the parent
    @param heightSpec int, the height constraint from the parent
    */
   @Override
   protected void onMeasure(int widthSpec, int heightSpec) {
      int width = MeasureSpec.getSize(widthSpec);
      int rows = Math.max(1, (m_tiles.size() + perRow(width) - 1) / perRow(width));
      int wanted = (int) Math.ceil(rows * cellHeight() + getPaddingTop() + getPaddingBottom());
      setMeasuredDimension(width, resolveSize(wanted, heightSpec));
   }

   /**
    Draws every tile with its label, the selected one outlined and raised.
    Algorithm:
       1) No tiles: write "(no tiles)"
       2) For each tile: find its row and column; write its label above it; draw it
    Reference: Custom views, from https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing
    @param canvas Canvas, passed by reference; the tiles are drawn on it
    */
   @Override
   protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      m_labelPaint.setTextSize(m_unit * LABEL_FRACTION * 0.9f);
      if (m_tiles.isEmpty()) {
         canvas.drawText("(no tiles)", getWidth() / 2f, getPaddingTop() + m_unit, m_labelPaint);
         return;
      }
      int columns = perRow(getWidth());
      for (int i = 0; i < m_tiles.size(); i++) {
         float left = getPaddingLeft() + (i % columns) * cellWidth();
         float top = getPaddingTop() + (i / columns) * cellHeight();
         canvas.drawText(labelFor(i), left + m_unit, top + m_unit * LABEL_FRACTION * 0.85f,
                         m_labelPaint);
         boolean selected = i == m_selected;
         float tileTop = top + m_unit * LABEL_FRACTION + (selected ? 0 : m_unit * GAP / 2);
         Tile tile = m_tiles.get(i);
         m_painter.drawTile(canvas, left, tileTop, m_unit, tile.getLeftPips(), tile.getRightPips(),
                            false, selected ? TilePainter.HIGHLIGHT_SELECTED : TilePainter.NO_HIGHLIGHT);
      }
   }

   /* *********************************************************************
   Utility
   ********************************************************************* */

   /**
    Chooses the text shown above a tile.
    @param index int, 0-based position of the tile
    @return "1", "2", ... for a hand; "top" over the boneyard's first tile and nothing over the rest
    */
   private String labelFor(int index) {
      if (m_markTopOnly) {
         return index == 0 ? "top" : "";
      }
      return String.valueOf(index + 1);
   }

   /**
    Finds the tile under a point on the view.
    @param x float, x of the point in pixels
    @param y float, y of the point in pixels
    @return The 0-based position of the tile there, or NO_SELECTION if there is none
    */
   private int tileAt(float x, float y) {
      int columns = perRow(getWidth());
      int column = (int) ((x - getPaddingLeft()) / cellWidth());
      int row = (int) ((y - getPaddingTop()) / cellHeight());
      if (x < getPaddingLeft() || y < getPaddingTop() || column >= columns) {
         return NO_SELECTION;
      }
      int index = row * columns + column;
      return index < m_tiles.size() ? index : NO_SELECTION;
   }

   /**
    Works out how many tiles fit across a row.
    @param width int, the view's width in pixels
    @return The number of tiles per row, at least 1
    */
   private int perRow(int width) {
      float usable = width - getPaddingLeft() - getPaddingRight();
      return Math.max(1, (int) (usable / cellWidth()));
   }

   /**
    Reports the width taken by one tile and the gap after it.
    @return The width in pixels
    */
   private float cellWidth() {
      return m_unit * (2 + GAP);
   }

   /**
    Reports the height taken by one row: label, tile, and gap.
    @return The height in pixels
    */
   private float cellHeight() {
      return m_unit * (1 + LABEL_FRACTION + GAP);
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
}

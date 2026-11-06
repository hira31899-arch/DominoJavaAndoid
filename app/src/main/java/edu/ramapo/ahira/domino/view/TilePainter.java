/************************************************************
 * Name:    Ahmed Hira                                      *
 * Project: 35 Domino (Java/Android)                        *
 * Class:   CMPS 366 - Organization of Programming Languages*
 * Date:    October 31, 2026                                *
 ************************************************************/

package edu.ramapo.ahira.domino.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 Draws one domino tile with real pip dots. Shared by the board, the hands,
 and the boneyard so every tile on screen looks the same.
 A tile is two squares of side "unit": 2 x 1 units lying down, 1 x 2 standing up.
 */
public class TilePainter {

   // Colour of a highlighted tile's outline: the selected tile, or the spinner
   public static final int HIGHLIGHT_SELECTED = Color.rgb(255, 152, 0);
   public static final int HIGHLIGHT_SPINNER = Color.rgb(255, 213, 79);
   // Means "draw the normal outline"
   public static final int NO_HIGHLIGHT = 0;

   // Rounded-corner radius, as a fraction of the unit
   private static final float CORNER_FRACTION = 0.16f;
   // Pip dot radius, as a fraction of the unit
   private static final float PIP_FRACTION = 0.095f;
   // Outline thickness, normal and highlighted, as a fraction of the unit
   private static final float EDGE_FRACTION = 0.04f;
   private static final float HIGHLIGHT_FRACTION = 0.12f;
   // Where the pips sit inside one square (fractions of the square), for 0 to 6 pips
   private static final float[][][] PIP_SPOTS = {
      {},
      { { 0.5f, 0.5f } },
      { { 0.27f, 0.27f }, { 0.73f, 0.73f } },
      { { 0.27f, 0.27f }, { 0.5f, 0.5f }, { 0.73f, 0.73f } },
      { { 0.27f, 0.27f }, { 0.73f, 0.27f }, { 0.27f, 0.73f }, { 0.73f, 0.73f } },
      { { 0.27f, 0.27f }, { 0.73f, 0.27f }, { 0.5f, 0.5f }, { 0.27f, 0.73f }, { 0.73f, 0.73f } },
      { { 0.27f, 0.25f }, { 0.73f, 0.25f }, { 0.27f, 0.5f }, { 0.73f, 0.5f }, { 0.27f, 0.75f },
        { 0.73f, 0.75f } }
   };

   // Paint for the ivory face of the tile
   private final Paint m_facePaint;
   // Paint for the outline and the dividing line
   private final Paint m_edgePaint;
   // Paint for the pip dots
   private final Paint m_pipPaint;
   // Reused rectangle, so drawing does not create objects
   private final RectF m_rect;

   /**
    Creates the paints used for every tile.
    */
   public TilePainter() {
      m_facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      m_facePaint.setColor(Color.rgb(250, 247, 238));
      m_edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      m_edgePaint.setStyle(Paint.Style.STROKE);
      m_edgePaint.setColor(Color.rgb(55, 55, 55));
      m_pipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      m_pipPaint.setColor(Color.rgb(25, 25, 25));
      m_rect = new RectF();
   }

   /**
    Draws one tile.
    Algorithm:
       1) Work out the tile's rectangle: 2 x 1 units lying down, 1 x 2 units standing up
       2) Fill the face, draw the outline (thicker and coloured when highlighted)
       3) Draw the dividing line between the two squares
       4) Draw the first pip count in the left (or top) square, the second in the right (or bottom)
    Reference: Canvas drawing, from https://developer.android.com/reference/android/graphics/Canvas
    @param canvas Canvas, passed by reference; the tile is drawn on it
    @param left float, x of the tile's left edge in pixels
    @param top float, y of the tile's top edge in pixels
    @param unit float, side of one square in pixels
    @param firstPips int, pips in the left (lying down) or top (standing up) square
    @param secondPips int, pips in the right or bottom square
    @param standing boolean, true to draw the tile standing up (1 x 2 units)
    @param highlight int, outline colour for a highlighted tile, or NO_HIGHLIGHT
    */
   public void drawTile(Canvas canvas, float left, float top, float unit, int firstPips,
                        int secondPips, boolean standing, int highlight) {
      float width = standing ? unit : 2 * unit;
      float height = standing ? 2 * unit : unit;
      float corner = unit * CORNER_FRACTION;
      m_rect.set(left, top, left + width, top + height);
      canvas.drawRoundRect(m_rect, corner, corner, m_facePaint);
      boolean highlighted = highlight != NO_HIGHLIGHT;
      m_edgePaint.setColor(highlighted ? highlight : Color.rgb(55, 55, 55));
      m_edgePaint.setStrokeWidth(unit * (highlighted ? HIGHLIGHT_FRACTION : EDGE_FRACTION));
      canvas.drawRoundRect(m_rect, corner, corner, m_edgePaint);
      m_edgePaint.setColor(Color.rgb(55, 55, 55));
      m_edgePaint.setStrokeWidth(unit * EDGE_FRACTION);
      float inset = unit * 0.15f;
      if (standing) {
         canvas.drawLine(left + inset, top + unit, left + unit - inset, top + unit, m_edgePaint);
         drawPips(canvas, left, top, unit, firstPips);
         drawPips(canvas, left, top + unit, unit, secondPips);
      }
      else {
         canvas.drawLine(left + unit, top + inset, left + unit, top + unit - inset, m_edgePaint);
         drawPips(canvas, left, top, unit, firstPips);
         drawPips(canvas, left + unit, top, unit, secondPips);
      }
   }

   /**
    Draws the pip dots for one square of a tile.
    @param canvas Canvas, passed by reference; the dots are drawn on it
    @param left float, x of the square's left edge in pixels
    @param top float, y of the square's top edge in pixels
    @param unit float, side of the square in pixels
    @param pips int, number of dots, 0 to 6 (other values draw nothing)
    */
   private void drawPips(Canvas canvas, float left, float top, float unit, int pips) {
      if (pips < 0 || pips >= PIP_SPOTS.length) {
         return;
      }
      float radius = unit * PIP_FRACTION;
      for (float[] spot : PIP_SPOTS[pips]) {
         canvas.drawCircle(left + spot[0] * unit, top + spot[1] * unit, radius, m_pipPaint);
      }
   }
}

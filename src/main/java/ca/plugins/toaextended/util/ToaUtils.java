/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2023, rdutta <https://github.com/rdutta>
 * Copyright (c) 2022, LlemonDuck
 * Copyright (c) 2022, TheStonedTurtle
 * Copyright (c) 2019, Ron Young <https://github.com/raiyni>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.plugins.toaextended.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import net.runelite.api.Constants;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class ToaUtils
{
	public static final Color COLOR_SPEC_ATK = new Color(255, 221, 56);
	private static final int CYCLES_PER_GAME_TICK = Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH;

	private static final Stroke BASIC_STROKE = new BasicStroke(2);

	public static void renderSpecialAttackOutline(final Graphics2D graphics2D, final NPC npc, final ModelOutlineRenderer modelOutlineRenderer)
	{
		renderSpecialAttackOutline(graphics2D, npc, COLOR_SPEC_ATK, modelOutlineRenderer);
	}

	public static void renderSpecialAttackOutline(final Graphics2D graphics2D, final NPC npc, final Color color, final ModelOutlineRenderer modelOutlineRenderer)
	{
		modelOutlineRenderer.drawOutline(npc, 4, color, 4);
		final Color originalColor = graphics2D.getColor();
		graphics2D.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
		graphics2D.fill(npc.getConvexHull());
		graphics2D.setColor(originalColor);
	}

	public static Polygon rectangleToPolygon(final Rectangle rectangle)
	{
		final int[] xpoints = {rectangle.x, rectangle.x + rectangle.width, rectangle.x + rectangle.width, rectangle.x};
		final int[] ypoints = {rectangle.y, rectangle.y, rectangle.y + rectangle.height, rectangle.y + rectangle.height};

		return new Polygon(xpoints, ypoints, 4);
	}

	public static void renderTextLocation(final Graphics2D graphics2D, final Point point, final String text, final Color color, final int size, final int style, final boolean shadow)
	{
		if (point == null)
		{
			return;
		}

		final Color originalColor = graphics2D.getColor();
		final Font originalFont = graphics2D.getFont();

		graphics2D.setFont(new Font("Arial", style, size));

		if (shadow)
		{
			graphics2D.setColor(Color.BLACK);
			graphics2D.drawString(text, point.getX() + 1, point.getY() + 1);
		}

		graphics2D.setColor(color);
		graphics2D.drawString(text, point.getX(), point.getY());

		graphics2D.setFont(originalFont);
		graphics2D.setColor(originalColor);
	}

	public static void renderFilledPolygon(final Graphics2D graphics2D, final Shape shape, final Color color)
	{
		graphics2D.setColor(color);
		final Stroke originalStroke = graphics2D.getStroke();
		graphics2D.setStroke(BASIC_STROKE);
		graphics2D.draw(shape);
		graphics2D.fill(shape);
		graphics2D.setStroke(originalStroke);
	}

	public static void renderOutlinePolygon(final Graphics2D graphics2D, final Shape shape, final Color color)
	{
		graphics2D.setColor(color);
		final Stroke originalStroke = graphics2D.getStroke();
		graphics2D.setStroke(BASIC_STROKE);
		graphics2D.draw(shape);
		graphics2D.setStroke(originalStroke);
	}

	public static void drawOutlineAndFill(
		final Graphics2D graphics2D,
		final Color outlineColor,
		final Color fillColor,
		final float strokeWidth,
		final Shape shape)
	{
		final Color originalColor = graphics2D.getColor();
		final Stroke originalStroke = graphics2D.getStroke();

		graphics2D.setStroke(new BasicStroke(strokeWidth));
		graphics2D.setColor(outlineColor);
		graphics2D.draw(shape);

		graphics2D.setColor(fillColor);
		graphics2D.fill(shape);

		graphics2D.setColor(originalColor);
		graphics2D.setStroke(originalStroke);
	}

	public static int cyclesToTicks(final int cycles)
	{
		return (cycles / CYCLES_PER_GAME_TICK) + 1;
	}

	public static int colorToRs2hsb(final Color color)
	{
		final float[] hsbVals = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

		// "Correct" the brightness level to avoid going to white at full saturation, or having a low brightness at
		// low saturation
		hsbVals[2] -= Math.min(hsbVals[1], hsbVals[2] / 2);

		final int encode_hue = (int) (hsbVals[0] * 63);
		final int encode_saturation = (int) (hsbVals[1] * 7);
		final int encode_brightness = (int) (hsbVals[2] * 127);
		return (encode_hue << 10) + (encode_saturation << 7) + (encode_brightness);
	}

	public static int getHpUntilNextBreakPoint(final int[] hpBreakpoints, final int currentHp)
	{
		for (final int hpBreakpoint : hpBreakpoints)
		{
			if (currentHp > hpBreakpoint)
			{
				return currentHp - hpBreakpoint;
			}
		}

		return currentHp;
	}
}

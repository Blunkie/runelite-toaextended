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
package ca.plugins.toaextended.challenge.scabaras;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Collection;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ColorUtil;

@Singleton
public class ScabarasOverlay extends Overlay implements PluginLifecycleComponent
{
	private static final Stroke BASIC_STROKE = new BasicStroke(1);
	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	private final Client client;
	private final ToaExtendedConfig config;
	private final OverlayManager overlayManager;

	private final ModelOutlineRenderer modelOutlineRenderer;
	private final ScabarasAdditionPuzzle additionPuzzleSolver;
	private final ScabarasLightPuzzle scabarasLightPuzzle;
	private final ScabarasMatchingPuzzle scabarasMatchingPuzzle;
	private final ScabarasObeliskPuzzle scabarasObeliskPuzzle;
	private final ScabarasSequencePuzzle scabarasSequencePuzzle;

	@Inject
	public ScabarasOverlay(
		final Client client,
		final ToaExtendedConfig config,
		final OverlayManager overlayManager,
		final ModelOutlineRenderer modelOutlineRenderer,
		final ScabarasAdditionPuzzle scabarasAdditionPuzzle,
		final ScabarasLightPuzzle scabarasLightPuzzle,
		final ScabarasMatchingPuzzle scabarasMatchingPuzzle,
		final ScabarasObeliskPuzzle scabarasObeliskPuzzle,
		final ScabarasSequencePuzzle scabarasSequencePuzzle
	)
	{
		this.client = client;
		this.config = config;
		this.overlayManager = overlayManager;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.additionPuzzleSolver = scabarasAdditionPuzzle;
		this.scabarasLightPuzzle = scabarasLightPuzzle;
		this.scabarasMatchingPuzzle = scabarasMatchingPuzzle;
		this.scabarasObeliskPuzzle = scabarasObeliskPuzzle;
		this.scabarasSequencePuzzle = scabarasSequencePuzzle;

		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.SCABARAS;
	}

	@Override
	public void startUp()
	{
		overlayManager.add(this);
	}

	@Override
	public void shutDown()
	{
		overlayManager.remove(this);
	}

	@Override
	public Dimension render(final Graphics2D graphics)
	{
		renderLocalPoints(graphics, additionPuzzleSolver.getFlips());
		renderLocalPoints(graphics, scabarasLightPuzzle.getFlips());

		renderLocalSequence(
			graphics,
			scabarasObeliskPuzzle.getObeliskOrder(),
			scabarasObeliskPuzzle.getActiveObelisks()
		);
		renderLocalSequence(
			graphics,
			scabarasSequencePuzzle.getPoints(),
			scabarasSequencePuzzle.getCompletedTiles()
		);

		renderMatchingTiles(graphics);

		renderFallingRocks(graphics, scabarasObeliskPuzzle.getFallingRocks());
		renderEntranceTile(scabarasObeliskPuzzle.getEntranceTile());

		return null;
	}

	private void renderEntranceTile(final TileObject tileObject)
	{
		if (tileObject == null || !config.scabarasHighlightEntrance())
		{
			return;
		}

		modelOutlineRenderer.drawOutline(tileObject, 1, Color.GREEN, 1);
	}

	private void renderLocalPoints(final Graphics2D graphics, final Iterable<LocalPoint> points)
	{
		for (final LocalPoint tile : points)
		{
			final Polygon canvasTilePoly = Perspective.getCanvasTilePoly(client, tile);
			if (canvasTilePoly == null)
			{
				continue;
			}
			OverlayUtil.renderPolygon(graphics, canvasTilePoly, config.dangerOutlineColor());
		}
	}

	private void renderLocalSequence(final Graphics2D graphics, final Iterable<LocalPoint> points, final int progress)
	{
		int ix = 0;
		for (final LocalPoint tile : points)
		{
			final Color c = ix < progress ? Color.gray : ColorUtil.colorLerp(Color.CYAN, Color.BLUE, ix / 5.0);

			final Polygon canvasTilePoly = Perspective.getCanvasTilePoly(client, tile);
			if (canvasTilePoly == null)
			{
				continue;
			}

			OverlayUtil.renderPolygon(graphics, canvasTilePoly, c);

			final Rectangle bounds = canvasTilePoly.getBounds();

			OverlayUtil.renderTextLocation(
				graphics,
				new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2),
				String.valueOf(++ix),
				ix <= progress ? Color.GRAY : Color.WHITE
			);
		}
	}

	private void renderMatchingTiles(final Graphics2D graphics)
	{
		for (final Integer upId : scabarasMatchingPuzzle.getUpTiles())
		{
			final Integer downId = ScabarasMatchingPuzzle.upToDown.get(upId);

			if (downId == null)
			{
				continue;
			}

			for (final Map.Entry<TileObject, Integer> entry : scabarasMatchingPuzzle.getDownTiles().entrySet())
			{
				if (entry.getValue().equals(downId))
				{
					final Polygon polygon = entry.getKey().getCanvasTilePoly();

					if (polygon != null)
					{
						OverlayUtil.renderPolygon(graphics, polygon, Color.RED, TRANSPARENT, BASIC_STROKE);
					}
					break;
				}
			}
		}
	}

	private void renderFallingRocks(final Graphics2D graphics2D, final Collection<GraphicsObject> fallingRocks)
	{
		if (fallingRocks.isEmpty())
		{
			return;
		}

		for (final GraphicsObject rock : fallingRocks)
		{
			final Polygon polygon = Perspective.getCanvasTilePoly(client, rock.getLocation());

			if (polygon == null)
			{
				continue;
			}

			ToaUtils.drawOutlineAndFill(graphics2D, config.dangerOutlineColor(), config.dangerFillColor(),
				1, polygon);
		}
	}
}

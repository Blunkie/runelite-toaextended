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
package ca.plugins.toaextended.boss.akkha;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.ToaExtendedConfig.Tile;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
public class AkkhaSceneOverlay extends Overlay implements PluginLifecycleComponent
{
	private static final Stroke BORDER_STROKE = new BasicStroke(1);
	private static final Color COLOR_FILL = new Color(0, 0, 0, 0);

	private static final int ANIM_ID_MEMORY_BLAST = 9777;
	private static final int ANIM_ID_TRAILING_ORBS = 9778;

	private final Client client;
	private final ToaExtendedConfig config;
	private final OverlayManager overlayManager;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private final Akkha akkha;
	private final AkkhaMemoryBlast akkhaMemoryBlast;
	private final AkkhaFinalStand akkhaFinalStand;

	@Inject
	protected AkkhaSceneOverlay(final Client client, final ToaExtendedConfig config, final OverlayManager overlayManager,
								final ModelOutlineRenderer modelOutlineRenderer,
								final Akkha akkha, final AkkhaMemoryBlast akkhaMemoryBlast, final AkkhaFinalStand akkhaFinalStand)
	{
		this.client = client;
		this.config = config;
		this.overlayManager = overlayManager;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.akkha = akkha;
		this.akkhaMemoryBlast = akkhaMemoryBlast;
		this.akkhaFinalStand = akkhaFinalStand;

		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return akkha.isEnabled(config, raidState);
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
	public Dimension render(final Graphics2D graphics2D)
	{
		final NPC npc = akkha.getNpc();

		if (npc != null)
		{
			if (config.akkhaSpecialAttackOutline())
			{
				renderSpecOutline(graphics2D, npc);
			}

			if (config.akkhaHealthCounter())
			{
				renderHealthCounter(graphics2D, npc);
			}

			if (config.akkhaAttackTickCounter())
			{
				renderAttackTickCounter(graphics2D, npc);
			}

			renderTileOutline(graphics2D, npc);
		}

		if (config.akkhaMemoryBlastTracker())
		{
			renderMemoryBlast(graphics2D);
		}

		if (config.akkhaUnstableOrbRadius())
		{
			renderUnstableOrbRadius(graphics2D);
		}

		return null;
	}

	private void renderHealthCounter(final Graphics2D graphics2D, final Actor actor)
	{
		final int hp = akkha.getHpUntilNextBreakPoint();

		if (hp <= 0)
		{
			return;
		}

		final String text = Integer.toString(hp);

		final Point point = actor.getCanvasTextLocation(graphics2D, text, 0);

		if (point == null)
		{
			return;
		}

		ToaUtils.renderTextLocation(graphics2D, point, text, Color.WHITE, config.fontSize(),
			config.fontStyle().getFont(), true);
	}

	private void renderTileOutline(final Graphics2D graphics2D, final NPC npc)
	{
		final Tile tile = config.akkhaTile();

		if (tile == Tile.OFF)
		{
			return;
		}

		final Color color = npc.getId() == NpcID.AKKHA_11795 ?
			Color.WHITE : akkha.getAttackStyle().getColor();
		final Polygon polygon;

		switch (tile)
		{
			case TILE:
				polygon = npc.getCanvasTilePoly();
				if (polygon != null)
				{
					OverlayUtil.renderPolygon(graphics2D, polygon, color, COLOR_FILL, BORDER_STROKE);
				}
				break;
			case TRUE_TILE:
				LocalPoint localPoint = LocalPoint.fromWorld(client, npc.getWorldLocation());

				if (localPoint != null)
				{
					final int size = npc.getComposition().getSize();
					localPoint = new LocalPoint(
						localPoint.getX() + Perspective.LOCAL_TILE_SIZE * (size - 1) / 2,
						localPoint.getY() + Perspective.LOCAL_TILE_SIZE * (size - 1) / 2
					);
					polygon = Perspective.getCanvasTileAreaPoly(client, localPoint, size);
					if (polygon != null)
					{
						OverlayUtil.renderPolygon(graphics2D, polygon, color, COLOR_FILL, BORDER_STROKE);
					}
				}
				break;
			default:
				break;
		}
	}

	private void renderSpecOutline(final Graphics2D graphics2D, final NPC npc)
	{

		switch (npc.getAnimation())
		{
			case ANIM_ID_TRAILING_ORBS:
				ToaUtils.renderSpecialAttackOutline(graphics2D, npc, Color.BLACK, modelOutlineRenderer);
				break;
			case ANIM_ID_MEMORY_BLAST:
				ToaUtils.renderSpecialAttackOutline(graphics2D, npc, modelOutlineRenderer);
				return;
			default:
				break;
		}
	}

	private void renderAttackTickCounter(final Graphics2D graphics2D, final Actor actor)
	{
		final int ticks = akkha.getTicksUntilNextAttack();

		if (ticks <= 0)
		{
			return;
		}

		final Actor target = actor.getInteracting();

		if (target == null)
		{
			return;
		}

		final String text = Integer.toString(ticks);
		final Point point = target.getCanvasTextLocation(graphics2D, text, 0);
		final Color color = ticks == 1 ? Color.RED : Color.WHITE;
		ToaUtils.renderTextLocation(graphics2D, point, text, color, config.fontSize(), config.fontStyle().getFont(),
			true);
	}

	private void renderMemoryBlast(final Graphics2D graphics2D)
	{
		final LinkedList<WorldPoint> worldPoints = akkhaMemoryBlast.getWorldPoints();

		if (worldPoints.isEmpty())
		{
			return;
		}

		drawTileFromWorldPoint(graphics2D, worldPoints.peekFirst(), Color.GREEN);

		if (worldPoints.size() > 1)
		{
			drawTileFromWorldPoint(graphics2D, worldPoints.get(1), config.dangerOutlineColor());
		}
	}

	private void drawTileFromWorldPoint(final Graphics2D graphics2D, final WorldPoint worldPoint, final Color color)
	{
		final LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);

		if (localPoint == null)
		{
			return;
		}

		final Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);

		if (polygon == null)
		{
			return;
		}

		ToaUtils.drawOutlineAndFill(graphics2D, color,
			new Color(color.getRed(), color.getGreen(), color.getBlue(), 20),
			1, polygon);
	}

	private void renderUnstableOrbRadius(final Graphics2D graphics2D)
	{
		final List<Actor> unstableOrbs = akkhaFinalStand.getUnstableOrbs();

		if (unstableOrbs.isEmpty())
		{
			return;
		}

		final Collection<LocalPoint> localPoints = getUnstableOrbLocalPoints(unstableOrbs);

		if (localPoints.isEmpty())
		{
			return;
		}

		for (final LocalPoint lp : localPoints)
		{
			final Polygon polygon = Perspective.getCanvasTilePoly(client, lp);

			if (polygon == null)
			{
				continue;
			}

			ToaUtils.drawOutlineAndFill(graphics2D, config.dangerOutlineColor(),
				config.dangerFillColor(), 1, polygon);
		}
	}

	private Set<LocalPoint> getUnstableOrbLocalPoints(final Iterable<Actor> orbs)
	{
		final Set<LocalPoint> localPoints = new HashSet<>();

		for (final Actor actor : orbs)
		{
			final int orientation = actor.getOrientation() / 256;

			int dx = 0, dy = 0;

			switch (orientation)
			{
				case 0: // South
					dy = -1;
					break;
				case 1: // Southwest
					dx = -1;
					dy = -1;
					break;
				case 2: // West
					dx = -1;
					break;
				case 3: // Northwest
					dx = -1;
					dy = 1;
					break;
				case 4: // North
					dy = 1;
					break;
				case 5: // Northeast
					dx = 1;
					dy = 1;
					break;
				case 6: // East
					dx = 1;
					break;
				case 7: // Southeast
					dx = 1;
					dy = -1;
					break;
				default:
					continue;
			}

			final WorldArea worldArea = actor.getWorldArea();
			final LocalPoint localPoint = actor.getLocalLocation();

			final int distance = config.akkhaRadiusDistance();

			for (int d = 1; d <= distance; d++)
			{
				final int xD = d * dx;
				final int yD = d * dy;

				localPoints.add(new LocalPoint(
					localPoint.getX() +
						(xD * Perspective.LOCAL_TILE_SIZE) +
						(xD * Perspective.LOCAL_TILE_SIZE * (worldArea.getWidth() - 1) / 2),
					localPoint.getY() +
						(yD * Perspective.LOCAL_TILE_SIZE) +
						(yD * Perspective.LOCAL_TILE_SIZE * (worldArea.getHeight() - 1) / 2)
				));
			}
		}

		return localPoints;
	}

}

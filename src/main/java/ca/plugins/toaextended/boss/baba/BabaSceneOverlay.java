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
package ca.plugins.toaextended.boss.baba;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
public class BabaSceneOverlay extends Overlay implements PluginLifecycleComponent
{

	private static final int ANIMATION_ID_ROCK_THROW = 9744;

	private final Client client;
	private final ToaExtendedConfig config;
	private final OverlayManager overlayManager;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private final Baba baba;

	@Inject
	protected BabaSceneOverlay(final Client client, final ToaExtendedConfig config, final OverlayManager overlayManager, final ModelOutlineRenderer modelOutlineRenderer, final Baba baba)
	{
		this.client = client;
		this.config = config;
		this.overlayManager = overlayManager;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.baba = baba;

		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return baba.isEnabled(config, raidState);
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
		if (config.babaSpecialAttackOutline())
		{
			renderBabaRockThrow(graphics2D);
		}

		if (config.babaHealthCounter())
		{
			renderHealthCounter(graphics2D);
		}

		if (config.babaShockwaveTiles())
		{
			renderShockwaveTiles(graphics2D);
		}

		if (config.babaFallingBoulderTiles())
		{
			renderFallingBoulderTiles(graphics2D);
		}

		if (config.babaSarcophagusTiles())
		{
			renderSarcophagusTiles(graphics2D);
		}

		if (config.babaBananaPeelTiles())
		{
			renderBananaPeelTiles(graphics2D);
		}

		if (config.babaBaboonOutline())
		{
			renderBaboonOutline();
		}

		return null;
	}

	private void renderHealthCounter(final Graphics2D graphics2D)
	{
		final int hp = baba.getHpUntilNextBreakPoint();
		if (hp <= 0)
		{
			return;
		}

		final NPC npc = baba.getNpc();
		if (npc == null)
		{
			return;
		}

		final String text = Integer.toString(hp);

		final Point point = npc.getCanvasTextLocation(graphics2D, text, 0);
		if (point == null)
		{
			return;
		}

		ToaUtils.renderTextLocation(graphics2D, point, text, Color.WHITE, config.fontSize(),
			config.fontStyle().getFont(), true);
	}

	private void renderBaboonOutline()
	{
		final List<NPC> baboons = baba.getBaboonNpcs();

		if (baboons.isEmpty())
		{
			return;
		}

		for (final NPC npc : baboons)
		{
			modelOutlineRenderer.drawOutline(npc, 1, Color.GREEN, 0);
		}
	}

	private void renderShockwaveTiles(final Graphics2D graphics2D)
	{
		final GraphicsObject shockwave = baba.getShockwave();

		if (shockwave == null)
		{
			return;
		}

		final Area area = new Area();

		final WorldArea worldArea = client.getLocalPlayer().getWorldArea();

		final LocalPoint localPoint = shockwave.getLocation();

		LocalPoint lp;

		final int size = shockwave.getId() == Baba.GRAPHICS_OBJECT_ID_SHOCKWAVE_SMALL ? 1 : 2;

		for (int dx = -size; dx <= size; dx++)
		{
			for (int dy = -size; dy <= size; dy++)
			{
				lp = getNewLocalPoint(worldArea, localPoint, dx, dy);
				final Polygon polygon = Perspective.getCanvasTilePoly(client, lp);
				if (polygon == null)
				{
					continue;
				}
				area.add(new Area(polygon));
			}
		}

		Polygon polygon;
		final int distance = size == 1 ? 2 : 3;

		lp = getNewLocalPoint(worldArea, localPoint, distance, 0);
		polygon = Perspective.getCanvasTilePoly(client, lp);
		if (polygon != null)
		{
			area.add(new Area(polygon));
		}

		lp = getNewLocalPoint(worldArea, localPoint, -distance, 0);
		polygon = Perspective.getCanvasTilePoly(client, lp);
		if (polygon != null)
		{
			area.add(new Area(polygon));
		}

		lp = getNewLocalPoint(worldArea, localPoint, 0, distance);
		polygon = Perspective.getCanvasTilePoly(client, lp);
		if (polygon != null)
		{
			area.add(new Area(polygon));
		}

		lp = getNewLocalPoint(worldArea, localPoint, 0, -distance);
		polygon = Perspective.getCanvasTilePoly(client, lp);
		if (polygon != null)
		{
			area.add(new Area(polygon));
		}

		ToaUtils.drawOutlineAndFill(graphics2D, config.dangerOutlineColor(), config.dangerFillColor(), 1,
			area);
	}

	private LocalPoint getNewLocalPoint(final WorldArea worldArea, final LocalPoint localPoint,
										final int dx, final int dy)
	{
		return new LocalPoint(
			localPoint.getX() + dx * Perspective.LOCAL_TILE_SIZE + dx * Perspective.LOCAL_TILE_SIZE *
				(worldArea.getWidth() - 1) / 2,
			localPoint.getY() + dy * Perspective.LOCAL_TILE_SIZE + dy * Perspective.LOCAL_TILE_SIZE *
				(worldArea.getHeight() - 1) / 2
		);
	}

	private void renderFallingBoulderTiles(final Graphics2D graphics2D)
	{
		final Map<GraphicsObject, Integer> fallingBoulders = baba.getFallingBouldersToTicks();

		if (fallingBoulders.isEmpty())
		{
			return;
		}

		for (final Map.Entry<GraphicsObject, Integer> entry : fallingBoulders.entrySet())
		{
			final int ticks = entry.getValue();

			final Color color = ticks <= 2 ? Color.RED : ticks == 3 ? Color.ORANGE : Color.YELLOW;

			final LocalPoint localPoint = entry.getKey().getLocation();

			if (localPoint == null)
			{
				continue;
			}

			final Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);

			if (polygon == null)
			{
				continue;
			}

			ToaUtils.drawOutlineAndFill(
				graphics2D,
				color,
				new Color(color.getRed(), color.getGreen(), color.getBlue(), 20),
				1,
				polygon
			);
		}
	}

	private void renderSarcophagusTiles(final Graphics2D graphics2D)
	{
		final List<Projectile> sarcophagusProjectiles = baba.getSarcophagusProjectiles();

		if (sarcophagusProjectiles.isEmpty())
		{
			return;
		}

		for (final Projectile projectile : sarcophagusProjectiles)
		{
			final LocalPoint localPoint = projectile.getTarget();

			final Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);

			if (polygon == null)
			{
				continue;
			}

			ToaUtils.drawOutlineAndFill(graphics2D, config.dangerOutlineColor(), config.dangerFillColor(),
				1, polygon);
		}
	}

	private void renderBabaRockThrow(final Graphics2D graphics2D)
	{
		final NPC npc = baba.getNpc();

		if (npc == null || npc.getAnimation() != ANIMATION_ID_ROCK_THROW)
		{
			return;
		}

		ToaUtils.renderSpecialAttackOutline(graphics2D, npc, modelOutlineRenderer);
	}

	private void renderBananaPeelTiles(final Graphics2D graphics2D)
	{
		final List<GameObject> bananaPeels = baba.getBananaPeelGameObjects();

		if (bananaPeels.isEmpty())
		{
			return;
		}

		for (final GameObject bananaPeel : bananaPeels)
		{
			final Polygon polygon = bananaPeel.getCanvasTilePoly();

			if (polygon == null)
			{
				continue;
			}

			ToaUtils.drawOutlineAndFill(graphics2D, config.dangerOutlineColor(), config.dangerFillColor(),
				1, polygon);
		}
	}

}

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
package ca.plugins.toaextended.boss.warden.phase3;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class WardenP3SceneOverlay extends Overlay implements PluginLifecycleComponent
{

	private final Client client;
	private final ToaExtendedConfig config;
	private final OverlayManager overlayManager;
	private final WardenP3 wardenP3;

	@Inject
	public WardenP3SceneOverlay(final Client client, final ToaExtendedConfig config, final OverlayManager overlayManager,
								final WardenP3 wardenP3)
	{
		this.client = client;
		this.config = config;
		this.overlayManager = overlayManager;
		this.wardenP3 = wardenP3;

		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return wardenP3.isEnabled(config, raidState);
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
		if (config.wardenP3HealthCounter())
		{
			renderHealthCounter(graphics2D);
		}

		if (config.wardenSlamIndicator())
		{
			renderSlamIndicator(graphics2D);
		}

		if (config.wardenEnergySiphonTickTimer())
		{
			renderEnergySiphonTickTimer(graphics2D);
		}

		if (config.wardenEnergySiphonProjectileTile())
		{
			renderEnergySiphonProjectileTile(graphics2D);
		}

		if (config.wardenKephriFireballTile() != ToaExtendedConfig.FireballRadius.OFF)
		{
			renderKephriFireballTile(graphics2D);
		}

		if (config.wardenBabaFallingBoulderTile())
		{
			renderBabaFallingBoulderTile(graphics2D);
		}

		if (config.wardenRedLightningTiles())
		{
			renderRedLightningTiles(graphics2D);
		}

		return null;
	}

	private void renderHealthCounter(final Graphics2D graphics2D)
	{
		final NPC warden = wardenP3.getWarden();

		if (warden == null || wardenP3.isWardenCharging())
		{
			return;
		}

		final int hp = wardenP3.getHpUntilNextBreakPoint();
		if (hp <= 0)
		{
			return;
		}

		final String text = Integer.toString(hp);

		final Point point = warden.getCanvasTextLocation(graphics2D, text, 0);
		if (point == null)
		{
			return;
		}

		ToaUtils.renderTextLocation(graphics2D, point, text, Color.WHITE, config.fontSize(),
			config.fontStyle().getFont(), true);
	}

	private void renderSlamIndicator(final Graphics2D graphics2D)
	{
		if (!wardenP3.isDrawSafeTile())
		{
			return;
		}

		final LocalPoint localPoint = wardenP3.getSafeTileLocalPoint();

		if (localPoint == null)
		{
			return;
		}

		final Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
		if (polygon == null)
		{
			return;
		}

		ToaUtils.drawOutlineAndFill(graphics2D, Color.GREEN,
			new Color(0, 255, 0, 20), 1,
			polygon);
	}

	private void renderEnergySiphonTickTimer(final Graphics2D graphics2D)
	{
		final Map<NPC, Integer> energySiphonToTicks = wardenP3.getEnergySiphonToTicks();

		if (energySiphonToTicks.isEmpty())
		{
			return;
		}

		for (final Map.Entry<NPC, Integer> entry : energySiphonToTicks.entrySet())
		{
			final int ticks = entry.getValue();

			if (ticks < 0)
			{
				continue;
			}

			final NPC npc = entry.getKey();

			if (npc.getId() != NpcID.ENERGY_SIPHON)
			{
				continue;
			}

			final String text = Integer.toString(ticks);

			final Point point = npc.getCanvasTextLocation(graphics2D, text, npc.getLogicalHeight() + 40);

			if (point == null)
			{
				continue;
			}

			OverlayUtil.renderTextLocation(graphics2D, point, text, ticks == 1 ? Color.RED : Color.WHITE);
		}
	}

	private void renderEnergySiphonProjectileTile(final Graphics2D graphics2D)
	{
		final List<Projectile> energySiphonProjectiles = wardenP3.getEnergySiphonProjectiles();

		if (energySiphonProjectiles.isEmpty())
		{
			return;
		}

		for (final Projectile projectile : energySiphonProjectiles)
		{
			final Polygon polygon = Perspective.getCanvasTilePoly(client, projectile.getTarget());

			if (polygon == null)
			{
				continue;
			}

			ToaUtils.drawOutlineAndFill(graphics2D, Color.MAGENTA, new Color(255, 0, 255, 20),
				1, polygon);
		}
	}

	private void renderKephriFireballTile(final Graphics2D graphics2D)
	{
		final List<Projectile> fireballProjectiles = wardenP3.getFireballProjectiles();

		if (fireballProjectiles.isEmpty())
		{
			return;
		}

		for (final Projectile projectile : fireballProjectiles)
		{
			final int ticks = ToaUtils.cyclesToTicks(projectile.getRemainingCycles());
			final Color color = ticks <= 1 ? Color.RED : Color.ORANGE;

			final LocalPoint localPoint = projectile.getTarget();

			final Polygon polygon = config.wardenKephriFireballTile() == ToaExtendedConfig.FireballRadius.AERIAL ?
				Perspective.getCanvasTileAreaPoly(client, localPoint, 3) :
				Perspective.getCanvasTilePoly(client, localPoint);

			if (polygon == null)
			{
				return;
			}

			ToaUtils.drawOutlineAndFill(graphics2D, color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 20),
				1, polygon);
		}
	}

	private void renderBabaFallingBoulderTile(final Graphics2D graphics2D)
	{
		final Map<GraphicsObject, Integer> fallingBoulders = wardenP3.getFallingBouldersToTicks();

		if (fallingBoulders.isEmpty())
		{
			return;
		}

		for (final Map.Entry<GraphicsObject, Integer> entry : fallingBoulders.entrySet())
		{
			final int ticks = entry.getValue();

			final Color color = ticks <= 2 ? Color.RED : Color.ORANGE;

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

	private void renderRedLightningTiles(final Graphics2D graphics2D)
	{
		final Map<GraphicsObject, Integer> redLightning = wardenP3.getRedLightningToTicks();

		if (redLightning.isEmpty())
		{
			return;
		}

		for (final Map.Entry<GraphicsObject, Integer> entry : redLightning.entrySet())
		{
			final int ticks = entry.getValue();

			final Color color = ticks <= 1 ? Color.RED : ticks == 2 ? new Color(255, 77, 0) : Color.YELLOW;

			final Polygon polygon = Perspective.getCanvasTilePoly(client, entry.getKey().getLocation());

			if (polygon == null)
			{
				continue;
			}

			ToaUtils.drawOutlineAndFill(graphics2D, color,
				new Color(color.getRed(), color.getGreen(), color.getBlue(), 20),
				1, polygon);
		}
	}

}

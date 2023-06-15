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
package ca.plugins.toaextended.boss.warden.phase2;

import ca.plugins.toaextended.ToaExtendedConfig;
import static ca.plugins.toaextended.boss.warden.phase2.WardenP2.ANIMATION_ID_WARDEN_STANDING_UP;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.util.AbstractMap;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
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
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class WardenP2SceneOverlay extends Overlay implements PluginLifecycleComponent
{

	private final Client client;
	private final ToaExtendedConfig config;
	private final OverlayManager overlayManager;
	private final WardenP2 wardenP2;

	@Inject
	public WardenP2SceneOverlay(final Client client, final ToaExtendedConfig config, final OverlayManager overlayManager,
								final WardenP2 wardenP2)
	{
		this.client = client;
		this.config = config;
		this.overlayManager = overlayManager;
		this.wardenP2 = wardenP2;

		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return wardenP2.isEnabled(config, raidState);
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
		if (config.wardenP2HealthCounter())
		{
			renderHealthCounter(graphics2D);
		}

		if (config.wardenCoreTickTimer())
		{
			renderCoreTickTimer(graphics2D);
		}

		if (config.wardenCoreTile())
		{
			renderCoreTile(graphics2D);
		}

		if (config.wardenBlackSkullProjectileTile())
		{
			renderBlackSkullProjectileTile(graphics2D);
		}

		if (config.wardenLightningProjectileTile())
		{
			renderLightningProjectileTiles(graphics2D);
		}

		return null;
	}

	private void renderHealthCounter(final Graphics2D graphics2D)
	{
		final int hp = wardenP2.getHpRemaining();
		if (hp <= 0)
		{
			return;
		}

		final NPC npc = wardenP2.getNpc();

		if (npc == null ||
			!WardenP2.NPC_IDS_WARDEN.contains(npc.getId()) ||
			npc.getAnimation() == ANIMATION_ID_WARDEN_STANDING_UP)
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

	private void renderCoreTickTimer(final Graphics2D graphics2D)
	{
		final AbstractMap.SimpleEntry<NPC, Integer> coreToTicks = wardenP2.getCoreToTicks();

		if (coreToTicks == null)
		{
			return;
		}

		final NPC npc = coreToTicks.getKey();
		final String text = Integer.toString(coreToTicks.getValue());

		final Point point = npc.getCanvasTextLocation(graphics2D, text, npc.getLogicalHeight() + 40);

		if (point == null)
		{
			return;
		}

		OverlayUtil.renderTextLocation(graphics2D, point, text, Color.WHITE);
	}

	private void renderCoreTile(final Graphics2D graphics2D)
	{
		final Projectile projectile = wardenP2.getCoreProjectile();

		if (projectile == null)
		{
			return;
		}

		final LocalPoint localPoint = projectile.getTarget();

		final Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);

		if (polygon == null)
		{
			return;
		}

		ToaUtils.drawOutlineAndFill(graphics2D, Color.MAGENTA, new Color(255, 0, 255, 10),
			1, polygon);
	}

	private void renderBlackSkullProjectileTile(final Graphics2D graphics2D)
	{
		final Set<Projectile> projectiles = wardenP2.getBlackSkullProjectiles();

		if (projectiles.isEmpty())
		{
			return;
		}

		for (final Projectile projectile : projectiles)
		{
			final LocalPoint localPoint = projectile.getTarget();

			final Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);

			if (polygon == null)
			{
				return;
			}

			ToaUtils.drawOutlineAndFill(graphics2D, config.dangerOutlineColor(), config.dangerFillColor(),
				1, polygon);
		}
	}

	private void renderLightningProjectileTiles(final Graphics2D graphics2D)
	{
		final Set<Projectile> lightningProjectiles = wardenP2.getLightningProjectiles();

		if (lightningProjectiles.isEmpty())
		{
			return;
		}

		final Area area = new Area();
		final WorldArea worldArea = client.getLocalPlayer().getWorldArea();

		final int size = 3;

		for (final Projectile projectile : lightningProjectiles)
		{
			area.reset();

			final int ticks = ToaUtils.cyclesToTicks(projectile.getRemainingCycles());
			final Color color = ticks <= 2 ? Color.RED : ticks == 3 ? Color.ORANGE : Color.YELLOW;

			for (int dx = -size; dx <= size; dx++)
			{
				for (int dy = -size; dy <= size; dy++)
				{
					if (dx == 0 && (dy < size && dy > -size && dy != 0))
					{
						continue;
					}
					if (dy == 0 && (dx < size && dx > -size && dx != 0))
					{
						continue;
					}

					final LocalPoint lp = getNewLocalPoint(worldArea, projectile.getTarget(), dx, dy);
					final Polygon polygon = Perspective.getCanvasTilePoly(client, lp);
					if (polygon == null)
					{
						continue;
					}
					area.add(new Area(polygon));
				}
			}

			ToaUtils.drawOutlineAndFill(graphics2D, color,
				new Color(color.getRed(), color.getGreen(), color.getBlue(), 20),
				1, area);
		}
	}

	private LocalPoint getNewLocalPoint(final WorldArea worldArea, final LocalPoint localPoint, final int dx, final int dy)
	{
		return new LocalPoint(
			localPoint.getX() + dx * Perspective.LOCAL_TILE_SIZE + dx * Perspective.LOCAL_TILE_SIZE * (worldArea.getWidth() - 1) / 2,
			localPoint.getY() + dy * Perspective.LOCAL_TILE_SIZE + dy * Perspective.LOCAL_TILE_SIZE * (worldArea.getHeight() - 1) / 2
		);
	}

}

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
package ca.plugins.toaextended.boss.zebak;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
public class ZebakSceneOverlay extends Overlay implements PluginLifecycleComponent
{

	private final Client client;
	private final ToaExtendedConfig config;
	private final OverlayManager overlayManager;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private final Zebak zebak;

	@Inject
	public ZebakSceneOverlay(
		final Client client, final ToaExtendedConfig config, final OverlayManager overlayManager,
		final ModelOutlineRenderer modelOutlineRenderer,
		final Zebak zebak)
	{
		this.client = client;
		this.config = config;
		this.overlayManager = overlayManager;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.zebak = zebak;

		setPriority(OverlayPriority.HIGHEST);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return zebak.isEnabled(config, raidState);
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
		if (config.zebakHealthCounter())
		{
			renderHealthCounter(graphics2D);
		}

		if (config.zebakBloodMagicOutline())
		{
			renderBloodMagic();
		}

		if (config.zebakProjectileTiles())
		{
			renderProjectileTiles(graphics2D);
		}

		return null;
	}

	private void renderHealthCounter(final Graphics2D graphics2D)
	{
		final int hp = zebak.getHpUntilNextBreakPoint();

		if (hp <= 0)
		{
			return;
		}

		final NPC npc = zebak.getNpc();

		if (npc == null || npc.getId() == NpcID.ZEBAK_11733)
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

	private void renderBloodMagic()
	{
		final List<GraphicsObject> bloodMagicGfxObjects = zebak.getBloodMagicGfxObjects();

		if (bloodMagicGfxObjects.isEmpty())
		{
			return;
		}

		for (final GraphicsObject bloodMagic : bloodMagicGfxObjects)
		{
			modelOutlineRenderer.drawOutline(bloodMagic, 2, config.dangerOutlineColor(), 4);
		}
	}

	private void renderProjectileTiles(final Graphics2D graphics2D)
	{
		final List<Projectile> arenaProjectiles = zebak.getArenaProjectiles();

		if (arenaProjectiles.isEmpty())
		{
			return;
		}

		for (final Projectile projectile : arenaProjectiles)
		{
			final Polygon polygon = Perspective.getCanvasTilePoly(client, projectile.getTarget());

			if (polygon == null)
			{
				continue;
			}

			ToaUtils.drawOutlineAndFill(graphics2D, config.dangerOutlineColor(), config.dangerFillColor(),
				1, polygon);
		}
	}

}

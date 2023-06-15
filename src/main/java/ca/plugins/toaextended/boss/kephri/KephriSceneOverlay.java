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
package ca.plugins.toaextended.boss.kephri;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.ToaExtendedConfig.AttackCounter;
import ca.plugins.toaextended.ToaExtendedConfig.FireballRadius;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.nexus.PathLevelTracker;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import static ca.plugins.toaextended.util.ToaUtils.COLOR_SPEC_ATK;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
public class KephriSceneOverlay extends Overlay implements PluginLifecycleComponent
{
	private static final int SPOT_ANIM_ID_DUNG = 2146;

	private final Client client;
	private final OverlayManager overlayManager;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private final ToaExtendedConfig config;
	private final Kephri kephri;
	private final PathLevelTracker pathLevelTracker;

	private int eggRadius;

	@Inject
	protected KephriSceneOverlay(final Client client, final OverlayManager overlayManager, final ModelOutlineRenderer modelOutlineRenderer, final ToaExtendedConfig config, final Kephri kephri, final PathLevelTracker pathLevelTracker)
	{
		this.client = client;
		this.overlayManager = overlayManager;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.config = config;
		this.kephri = kephri;
		this.pathLevelTracker = pathLevelTracker;

		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return kephri.isEnabled(config, raidState);
	}

	@Override
	public void startUp()
	{
		overlayManager.add(this);
		eggRadius = pathLevelTracker.getKephriPathLevel() >= 4 ? 5 : 3;
	}

	@Override
	public void shutDown()
	{
		overlayManager.remove(this);
		eggRadius = 0;
	}

	@Override
	public Dimension render(final Graphics2D graphics2D)
	{
		if (config.kephriDungOutline())
		{
			renderDungOutline(graphics2D);
		}

		if (config.kephriAttackCounter() != AttackCounter.OFF)
		{
			renderAttackCounter(graphics2D);
		}

		if (config.kephriFireballTiles() != FireballRadius.OFF)
		{
			renderFireballs(graphics2D);
		}

		if (config.kephriEggTiles())
		{
			renderEggs(graphics2D);
		}

		if (config.kephriScarabSwarmOutline())
		{
			renderScarabSwarms();
		}

		return null;
	}

	private void renderAttackCounter(final Graphics2D graphics2D)
	{
		final NPC npc = kephri.getNpc();

		if (npc == null)
		{
			return;
		}

		final int count = kephri.getAtkCount();

		final String text;
		final Color color;

		if (count > 0)
		{
			text = Integer.toString(count);
			color = Color.WHITE;
		}
		else
		{
			text = "Special";
			color = Color.RED;
		}

		final Point point = npc.getCanvasTextLocation(graphics2D, text, 0);

		if (point == null)
		{
			return;
		}

		ToaUtils.renderTextLocation(graphics2D, point, text, color, config.fontSize(),
			config.fontStyle().getFont(), true);
	}

	private void renderFireballs(final Graphics2D graphics2D)
	{
		final List<Projectile> fireballs = kephri.getFireballProjectiles();

		if (fireballs.isEmpty())
		{
			return;
		}

		final boolean aerialAssault = config.kephriFireballTiles() == FireballRadius.AERIAL;

		for (final Projectile fireball : fireballs)
		{
			final int ticks = ToaUtils.cyclesToTicks(fireball.getRemainingCycles());

			final Color color = ticks <= 2 ? Color.RED :
				ticks == 3 ? Color.ORANGE :
					Color.YELLOW;

			final LocalPoint localPoint = fireball.getTarget();

			final Polygon polygon = aerialAssault ?
				Perspective.getCanvasTileAreaPoly(client, localPoint, 3) :
				Perspective.getCanvasTilePoly(client, localPoint);

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

	private void renderEggs(final Graphics2D graphics2D)
	{
		final Map<NPC, Integer> eggs = kephri.getEggsToTicks();

		if (eggs.isEmpty())
		{
			return;
		}

		for (final Map.Entry<NPC, Integer> entry : eggs.entrySet())
		{
			final int ticks = entry.getValue();

			final Color color = ticks <= 2 ? Color.RED : ticks <= 7 ? Color.ORANGE : Color.YELLOW;

			final LocalPoint localPoint = entry.getKey().getLocalLocation();

			final Polygon polygon = Perspective.getCanvasTileAreaPoly(client, localPoint, eggRadius);

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

	private void renderScarabSwarms()
	{
		final List<NPC> scarabSwarms = kephri.getScarabSwarmNpcs();

		if (scarabSwarms.isEmpty())
		{
			return;
		}

		for (final NPC swarm : scarabSwarms)
		{
			modelOutlineRenderer.drawOutline(swarm, 1, Color.WHITE, 0);
		}
	}

	private void renderDungOutline(final Graphics2D graphics2D)
	{
		final Player player = client.getLocalPlayer();

		if (player == null || !player.hasSpotAnim(SPOT_ANIM_ID_DUNG))
		{
			return;
		}

		modelOutlineRenderer.drawOutline(player, 2, COLOR_SPEC_ATK, 4);

		final NPC npc = kephri.getNpc();

		if (npc == null)
		{
			return;
		}

		ToaUtils.renderSpecialAttackOutline(graphics2D, npc, modelOutlineRenderer);
	}

}

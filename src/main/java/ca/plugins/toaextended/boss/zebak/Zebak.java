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
import ca.plugins.toaextended.boss.AttackProjectile;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.nexus.PathLevelTracker;
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.Renderable;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class Zebak implements PluginLifecycleComponent
{

	private static final double[] HP_PERCENT_BREAKPOINTS = new double[]{0.85, 0.70, 0.55, 0.40};

	private static final String NPC_NAME = "Zebak";
	private static final int PROJECTILE_ID_VENOM = 2194;
	private static final int PROJECTILE_ID_VENOM_SPLASH = 1555;
	private static final int PROJECTILE_ID_ROCK = 2172;
	private static final int PROJECTILE_ID_JUG = 2173;
	private static final int PROJECTILE_ID_MAGIC_ROCK = 2176;
	private static final int PROJECTILE_ID_MAGIC_ROCK_ENRAGED = 2177;
	private static final int PROJECTILE_ID_RANGE_ROCK = 2178;
	private static final int PROJECTILE_ID_RANGE_ROCK_ENRAGED = 2179;

	private static final int GRAPHICS_OBJECT_ID_BLOOD_MAGIC = 377;
	private static final int BLOOD_MAGIC_TICK_COUNT = 4;

	private final EventBus eventBus;
	private final Client client;
	private final ClientThread clientThread;
	private final Hooks hooks;
	private final ToaExtendedConfig config;
	private final NpcOverlayService npcOverlayService;
	private final PathLevelTracker pathLevelTracker;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	private final Function<NPC, HighlightedNpc> npcHighlighter = this::npcHighlight;

	@Getter(AccessLevel.PACKAGE)
	private final Queue<AttackProjectile> attackProjectiles = new PriorityQueue<>();

	@Getter(AccessLevel.PACKAGE)
	private final List<Projectile> arenaProjectiles = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private final List<GraphicsObject> bloodMagicGfxObjects = new ArrayList<>();

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private NPC npc;

	@Getter(AccessLevel.PACKAGE)
	private long lastTickTime;

	@Nullable
	private int[] hpBreakpoints;
	@Getter(AccessLevel.PACKAGE)
	private int hpUntilNextBreakPoint = -1;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.ZEBAK;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
		hooks.registerRenderableDrawListener(drawListener);
		npcOverlayService.registerHighlighter(npcHighlighter);
		clientThread.invoke(() ->
		{
			for (final NPC npc : client.getNpcs())
			{
				onNpcSpawned(new NpcSpawned(npc));
			}
		});
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
		hooks.unregisterRenderableDrawListener(drawListener);
		npcOverlayService.unregisterHighlighter(npcHighlighter);
		reset();
	}

	private void reset()
	{
		attackProjectiles.clear();
		arenaProjectiles.clear();
		bloodMagicGfxObjects.clear();

		npc = null;
		lastTickTime = 0L;
		hpBreakpoints = null;
		hpUntilNextBreakPoint = -1;
	}

	@Subscribe
	private void onConfigChanged(final ConfigChanged event)
	{
		if (!event.getGroup().equals(ToaExtendedConfig.CONFIG_GROUP))
		{
			return;
		}

		final String key = event.getKey();

		if (key.equals("miscDangerOutlineColor") || key.equals("miscDangerFillColor") ||
			key.equals("zebakBloodCloudTile") || key.equals("zebakWaveTile"))
		{
			npcOverlayService.rebuild();
		}
	}

	@Subscribe
	private void onGameTick(final GameTick event)
	{
		lastTickTime = System.currentTimeMillis();

		if (!attackProjectiles.isEmpty())
		{
			attackProjectiles.forEach(AttackProjectile::decrementTicks);
			attackProjectiles.removeIf(AttackProjectile::isExpired);
		}

		if (!bloodMagicGfxObjects.isEmpty())
		{
			bloodMagicGfxObjects.removeIf(GraphicsObject::finished);
		}

		if (!arenaProjectiles.isEmpty())
		{
			arenaProjectiles.removeIf(p -> p.getRemainingCycles() <= 0);
		}
	}

	@Subscribe
	private void onNpcSpawned(final NpcSpawned event)
	{
		final NPC npc = event.getNpc();

		if (NPC_NAME.equals(npc.getName()))
		{
			this.npc = npc;
		}
	}

	@Subscribe
	private void onNpcDespawned(final NpcDespawned event)
	{
		if (event.getNpc() == this.npc)
		{
			reset();
		}
	}

	@Subscribe
	private void onProjectileMoved(final ProjectileMoved event)
	{
		final Projectile projectile = event.getProjectile();

		if (projectile.getRemainingCycles() <= 0)
		{
			return;
		}

		switch (projectile.getId())
		{
			case PROJECTILE_ID_MAGIC_ROCK:
			case PROJECTILE_ID_MAGIC_ROCK_ENRAGED:
				attackProjectiles.add(new AttackProjectile(Prayer.PROTECT_FROM_MAGIC,
					ToaUtils.cyclesToTicks(projectile.getRemainingCycles()) + getProjectileTickCount()));
				break;
			case PROJECTILE_ID_RANGE_ROCK:
			case PROJECTILE_ID_RANGE_ROCK_ENRAGED:
				attackProjectiles.add(new AttackProjectile(Prayer.PROTECT_FROM_MISSILES,
					ToaUtils.cyclesToTicks(projectile.getRemainingCycles()) + getProjectileTickCount()));
				break;
			case PROJECTILE_ID_JUG:
			case PROJECTILE_ID_ROCK:
			case PROJECTILE_ID_VENOM:
			case PROJECTILE_ID_VENOM_SPLASH:
				arenaProjectiles.add(projectile);
				break;
			default:
				break;
		}
	}

	@Subscribe
	private void onGraphicsObjectCreated(final GraphicsObjectCreated event)
	{
		final GraphicsObject graphicsObject = event.getGraphicsObject();

		if (graphicsObject.getId() != GRAPHICS_OBJECT_ID_BLOOD_MAGIC)
		{
			return;
		}

		if (bloodMagicGfxObjects.isEmpty())
		{
			attackProjectiles.add(new AttackProjectile(Prayer.PROTECT_FROM_MAGIC, BLOOD_MAGIC_TICK_COUNT, 1));
		}

		bloodMagicGfxObjects.add(graphicsObject);
	}

	@Subscribe
	private void onScriptPostFired(final ScriptPostFired event)
	{
		if (event.getScriptId() != ScriptID.HP_HUD_UPDATE)
		{
			return;
		}

		if (hpBreakpoints == null)
		{
			final int maxHp = client.getVarbitValue(Varbits.BOSS_HEALTH_MAXIMUM);

			if (maxHp <= 1)
			{
				return;
			}

			hpBreakpoints = new int[HP_PERCENT_BREAKPOINTS.length];

			for (int i = 0; i < HP_PERCENT_BREAKPOINTS.length; i++)
			{
				hpBreakpoints[i] = (int) (HP_PERCENT_BREAKPOINTS[i] * maxHp);
			}
		}

		final int currentHp = client.getVarbitValue(Varbits.BOSS_HEALTH_CURRENT);
		if (currentHp <= 0)
		{
			return;
		}

		hpUntilNextBreakPoint = ToaUtils.getHpUntilNextBreakPoint(hpBreakpoints, currentHp);
	}

	private boolean shouldDraw(final Renderable renderable, final boolean drawingUI)
	{
		if (renderable instanceof NPC)
		{
			final int id = ((NPC) renderable).getId();

			if (id == NpcID.WAVE)
			{
				return !config.zebakHideWaves();
			}
		}

		return true;
	}

	private HighlightedNpc npcHighlight(final NPC npc)
	{
		final int id = npc.getId();

		ToaExtendedConfig.Tile tile;

		switch (id)
		{
			case NpcID.JUG:
				return HighlightedNpc.builder()
					.npc(npc)
					.outline(true)
					.borderWidth(1.0F)
					.highlightColor(Color.CYAN)
					.render(n -> config.zebakJugOutline())
					.build();
			case NpcID.BOULDER_11737:
				return HighlightedNpc.builder()
					.npc(npc)
					.outline(true)
					.borderWidth(1.0F)
					.highlightColor(Color.GREEN)
					.render(n -> config.zebakBoulderOutline())
					.build();
			case NpcID.BLOOD_CLOUD:
			case NpcID.BLOOD_CLOUD_11743:
				tile = config.zebakBloodCloudTile();
				switch (tile)
				{
					case TILE:
						return HighlightedNpc.builder()
							.npc(npc)
							.tile(true)
							.borderWidth(1)
							.highlightColor(config.dangerOutlineColor())
							.fillColor(config.dangerFillColor())
							.build();
					case TRUE_TILE:
						return HighlightedNpc.builder()
							.npc(npc)
							.trueTile(true)
							.borderWidth(1)
							.highlightColor(config.dangerOutlineColor())
							.fillColor(config.dangerFillColor())
							.build();
					case OFF:
						return null;
				}
			case NpcID.WAVE:
				tile = config.zebakWaveTile();
				switch (tile)
				{
					case TILE:
						return HighlightedNpc.builder()
							.npc(npc)
							.tile(true)
							.borderWidth(1)
							.highlightColor(config.dangerOutlineColor())
							.fillColor(config.dangerFillColor())
							.build();
					case TRUE_TILE:
						return HighlightedNpc.builder()
							.npc(npc)
							.trueTile(true)
							.borderWidth(1)
							.highlightColor(config.dangerOutlineColor())
							.fillColor(config.dangerFillColor())
							.build();
					case OFF:
						return null;
				}
			default:
				return null;
		}
	}

	private int getProjectileTickCount()
	{
		return pathLevelTracker.getZebakPathLevel() >= 4 ? 2 : 3;
	}

}

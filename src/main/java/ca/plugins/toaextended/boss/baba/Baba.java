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
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.Projectile;
import net.runelite.api.Renderable;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
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
public class Baba implements PluginLifecycleComponent
{

	private static final double[] HP_PERCENT_BREAKPOINTS = new double[]{0.66, 0.33};

	private static final Set<Integer> GRAPHICS_OBJECT_IDS_FALLING_BOULDER = ImmutableSet.of(2250, 2251);

	static final int GRAPHICS_OBJECT_ID_SHOCKWAVE_SMALL = 1447;

	private static final String NPC_NAME = "Ba-Ba";
	private static final int GRAPHICS_OBJECT_ID_SHOCKWAVE_LARGE = 1448;

	private static final int PROJECTILE_ID_BOULDER = 2245;
	private static final int PROJECTILE_ID_BABOON_ATTACK = 2243;
	private static final int PROJECTILE_ID_SARCOPHAGUS_ATTACK = 2246;

	private static final int FALLING_BOULDER_TICK_COUNT = 7;

	private final EventBus eventBus;
	private final Client client;
	private final ClientThread clientThread;
	private final Hooks hooks;
	private final NpcOverlayService npcOverlayService;
	private final ToaExtendedConfig config;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	private final Function<NPC, HighlightedNpc> npcHighlighter = this::npcHighlight;

	@Getter(AccessLevel.PACKAGE)
	private final Map<GraphicsObject, Integer> fallingBouldersToTicks = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private final List<Projectile> sarcophagusProjectiles = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private final List<GameObject> bananaPeelGameObjects = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private final List<NPC> baboonNpcs = new ArrayList<>();

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private NPC npc;
	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private GraphicsObject shockwave;

	@Nullable
	private int[] hpBreakpoints;
	@Getter(AccessLevel.PACKAGE)
	private int hpUntilNextBreakPoint = -1;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.BABA;
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
		sarcophagusProjectiles.clear();
		fallingBouldersToTicks.clear();
		bananaPeelGameObjects.clear();
		baboonNpcs.clear();

		npc = null;
		shockwave = null;
		hpUntilNextBreakPoint = -1;
		hpBreakpoints = null;
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
			key.equals("babaTile"))
		{
			npcOverlayService.rebuild();
		}
	}

	@Subscribe
	private void onGameTick(final GameTick event)
	{
		if (!sarcophagusProjectiles.isEmpty())
		{
			sarcophagusProjectiles.removeIf(p -> p.getRemainingCycles() <= 0);
		}

		if (!fallingBouldersToTicks.isEmpty())
		{
			fallingBouldersToTicks.replaceAll((k, v) -> v - 1);
			fallingBouldersToTicks.values().removeIf(v -> v <= 0);
		}

		if (shockwave != null && shockwave.finished())
		{
			shockwave = null;
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
		else if (npc.getId() == NpcID.BABOON)
		{
			baboonNpcs.add(npc);
		}
	}

	@Subscribe
	private void onNpcDespawned(final NpcDespawned event)
	{
		final NPC npc = event.getNpc();

		final int id = npc.getId();

		if (npc == this.npc)
		{
			this.npc = null;
		}
		else if (id == NpcID.BABOON)
		{
			baboonNpcs.remove(npc);
		}
	}

	@Subscribe
	private void onActorDeath(final ActorDeath event)
	{
		final Actor actor = event.getActor();

		if (!(actor instanceof NPC))
		{
			return;
		}

		final NPC npc = (NPC) actor;

		if (npc.getId() == NpcID.BABOON)
		{
			baboonNpcs.remove(npc);
		}
	}

	@Subscribe
	private void onProjectileMoved(final ProjectileMoved event)
	{
		final Projectile projectile = event.getProjectile();

		final int id = projectile.getId();

		if (id == PROJECTILE_ID_SARCOPHAGUS_ATTACK)
		{
			sarcophagusProjectiles.add(projectile);
		}
	}

	@Subscribe
	private void onGraphicsObjectCreated(final GraphicsObjectCreated event)
	{
		final GraphicsObject graphicsObject = event.getGraphicsObject();

		final int id = graphicsObject.getId();

		if (GRAPHICS_OBJECT_IDS_FALLING_BOULDER.contains(id))
		{
			fallingBouldersToTicks.put(graphicsObject, FALLING_BOULDER_TICK_COUNT);
		}
		else if (id == GRAPHICS_OBJECT_ID_SHOCKWAVE_SMALL || id == GRAPHICS_OBJECT_ID_SHOCKWAVE_LARGE)
		{
			if (shockwave == null || id == GRAPHICS_OBJECT_ID_SHOCKWAVE_LARGE)
			{
				shockwave = graphicsObject;
			}
		}
	}

	@Subscribe
	private void onGameObjectSpawned(final GameObjectSpawned event)
	{
		final GameObject gameObject = event.getGameObject();
		final int id = gameObject.getId();
		if (id == ObjectID.BANANA_PEEL)
		{
			bananaPeelGameObjects.add(gameObject);
		}
	}

	@Subscribe
	private void onGameObjectDespawned(final GameObjectDespawned event)
	{
		final GameObject gameObject = event.getGameObject();
		final int id = gameObject.getId();
		if (id == ObjectID.BANANA_PEEL)
		{
			bananaPeelGameObjects.remove(gameObject);
		}
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

			if (id == NpcID.BOULDER_11782)
			{
				return !config.babaHideNonWeakenedRollingBoulders();
			}
		}
		else if (renderable instanceof Projectile)
		{
			final int id = ((Projectile) renderable).getId();

			if (id == PROJECTILE_ID_BOULDER)
			{
				return !config.babaHideRollingBoulderProjectiles();
			}

			if (id == PROJECTILE_ID_BABOON_ATTACK)
			{
				return !config.babaHideBaboonProjectiles();
			}

		}
		else if (renderable instanceof GraphicsObject)
		{
			final GraphicsObject graphicsObject = (GraphicsObject) renderable;

			final int id = graphicsObject.getId();

			if (GRAPHICS_OBJECT_IDS_FALLING_BOULDER.contains(id))
			{
				return !config.babaHideFallingBoulders();
			}
		}

		return true;
	}

	private HighlightedNpc npcHighlight(final NPC npc)
	{
		if (NPC_NAME.equals(npc.getName()))
		{
			switch (config.babaTile())
			{
				case TILE:
					return HighlightedNpc.builder()
						.npc(npc)
						.tile(true)
						.borderWidth(1)
						.highlightColor(config.tileOutlineColor())
						.fillColor(config.tileFillColor())
						.build();
				case TRUE_TILE:
					return HighlightedNpc.builder()
						.npc(npc)
						.trueTile(true)
						.borderWidth(1)
						.highlightColor(config.tileOutlineColor())
						.fillColor(config.tileFillColor())
						.build();
				case OFF:
				default:
					return null;
			}
		}

		final int id = npc.getId();

		switch (id)
		{
			case NpcID.BOULDER_11782:
				return HighlightedNpc.builder()
					.npc(npc)
					.tile(true)
					.borderWidth(1)
					.highlightColor(config.dangerOutlineColor())
					.fillColor(config.dangerFillColor())
					.render(n -> config.babaNonWeakenedRollingBoulderTiles())
					.build();
			case NpcID.RUBBLE_11784:
				return HighlightedNpc.builder()
					.npc(npc)
					.tile(true)
					.borderWidth(1)
					.highlightColor(Color.GREEN)
					.fillColor(new Color(0, 0, 0, 0))
					.render(n -> config.babaRubbleTiles())
					.build();
			default:
				return null;
		}
	}

}

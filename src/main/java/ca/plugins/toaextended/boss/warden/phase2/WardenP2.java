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
import ca.plugins.toaextended.boss.AttackProjectile;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import com.google.common.collect.ImmutableSet;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
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
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.Renderable;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.client.callback.Hooks;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WardenP2 implements PluginLifecycleComponent
{

	static final Set<Integer> NPC_IDS_WARDEN = ImmutableSet.of(
		NpcID.ELIDINIS_WARDEN_11753,
		NpcID.ELIDINIS_WARDEN_11754,
		NpcID.TUMEKENS_WARDEN_11756,
		NpcID.TUMEKENS_WARDEN_11757
	);
	static final int ANIMATION_ID_WARDEN_STANDING_UP = 9672;
	private static final Set<Integer> NPC_IDS_WARDEN_CORE = ImmutableSet.of(NpcID.CORE, NpcID.CORE_11771);
	private static final Set<Integer> GRAPHICS_OBJECT_IDS_LIGHTNING_TILE = ImmutableSet.of(1447, 2197, 2198);

	private static final int PROJECTILE_ID_ARCANE_SCIMITAR = 2204;
	private static final int PROJECTILE_ID_WHITE_ARROW = 2206;
	private static final int PROJECTILE_ID_BLUE_SPELL = 2208;
	private static final int PROJECTILE_ID_RED_SKULL = 2224;
	private static final int PROJECTILE_ID_WHITE_SKULL = 2241;
	private static final int PROJECTILE_ID_BLACK_SKULL = 2210;
	private static final int PROJECTILE_ID_LIGHTNING = 2225;
	private static final int PROJECTILE_ID_CORE = 2240;

	private static final int WARDEN_SPECIAL_PROJECTILE_TICKS = 5;

	private final EventBus eventBus;
	private final Client client;
	private final Hooks hooks;
	private final NpcOverlayService npcOverlayService;
	private final ToaExtendedConfig config;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	private final Function<NPC, HighlightedNpc> npcHighlighter = this::npcHighlight;

	@Getter(AccessLevel.PACKAGE)
	private final Queue<AttackProjectile> attackProjectiles = new PriorityQueue<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<Projectile> lightningProjectiles = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<Projectile> blackSkullProjectiles = new HashSet<>();

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private Projectile coreProjectile;

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private AbstractMap.SimpleEntry<NPC, Integer> coreToTicks;

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private NPC npc;

	@Getter(AccessLevel.PACKAGE)
	private long lastTickTime;
	private int lastStartCycle;

	@Getter(AccessLevel.PACKAGE)
	private int hpRemaining;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.WARDEN_P2;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
		npcOverlayService.registerHighlighter(npcHighlighter);
		hooks.registerRenderableDrawListener(drawListener);
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
		npcOverlayService.unregisterHighlighter(npcHighlighter);
		hooks.unregisterRenderableDrawListener(drawListener);
		reset();
	}

	private void reset()
	{
		attackProjectiles.clear();
		lightningProjectiles.clear();
		blackSkullProjectiles.clear();

		coreProjectile = null;

		coreToTicks = null;
		npc = null;

		lastTickTime = 0L;
		lastStartCycle = 0;
		hpRemaining = 0;
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

		if (!lightningProjectiles.isEmpty())
		{
			lightningProjectiles.removeIf(p -> p.getRemainingCycles() <= 0);
		}

		if (!blackSkullProjectiles.isEmpty())
		{
			blackSkullProjectiles.removeIf(p -> p.getRemainingCycles() <= 0);
		}

		if (coreProjectile != null && coreProjectile.getRemainingCycles() <= 0)
		{
			coreProjectile = null;
		}

		if (coreToTicks != null)
		{
			coreToTicks.setValue(coreToTicks.getValue() + 1);
		}
	}

	@Subscribe
	private void onProjectileMoved(final ProjectileMoved event)
	{
		final Projectile projectile = event.getProjectile();

		final int remainingCycles = projectile.getRemainingCycles();

		if (remainingCycles <= 0)
		{
			return;
		}

		final Actor actor = projectile.getInteracting();

		if (actor != null && actor != client.getLocalPlayer())
		{
			return;
		}

		final int startCycle = projectile.getStartCycle();

		final Prayer prayer;
		final int ticks;
		final int priority = 0;

		switch (projectile.getId())
		{
			case PROJECTILE_ID_WHITE_SKULL:
				if (startCycle == lastStartCycle)
				{
					return;
				}
				lastStartCycle = startCycle;
				ticks = ToaUtils.cyclesToTicks(getProjectileCycles(projectile));
				prayer = Prayer.PROTECT_FROM_MISSILES;
				break;
			case PROJECTILE_ID_RED_SKULL:
				if (startCycle == lastStartCycle)
				{
					return;
				}
				lastStartCycle = startCycle;
				ticks = ToaUtils.cyclesToTicks(getProjectileCycles(projectile));
				prayer = Prayer.PROTECT_FROM_MAGIC;
				break;
			case PROJECTILE_ID_ARCANE_SCIMITAR:
				if (startCycle == lastStartCycle)
				{
					return;
				}
				lastStartCycle = startCycle;
				ticks = WARDEN_SPECIAL_PROJECTILE_TICKS;
				prayer = Prayer.PROTECT_FROM_MELEE;
				break;
			case PROJECTILE_ID_WHITE_ARROW:
				if (startCycle == lastStartCycle)
				{
					return;
				}
				lastStartCycle = startCycle;
				ticks = WARDEN_SPECIAL_PROJECTILE_TICKS;
				prayer = Prayer.PROTECT_FROM_MISSILES;
				break;
			case PROJECTILE_ID_BLUE_SPELL:
				if (startCycle == lastStartCycle)
				{
					return;
				}
				lastStartCycle = startCycle;
				ticks = WARDEN_SPECIAL_PROJECTILE_TICKS;
				prayer = Prayer.PROTECT_FROM_MAGIC;
				break;
			case PROJECTILE_ID_LIGHTNING:
				lightningProjectiles.add(projectile);
				return;
			case PROJECTILE_ID_BLACK_SKULL:
				blackSkullProjectiles.add(projectile);
				return;
			case PROJECTILE_ID_CORE:
				coreProjectile = projectile;
				return;
			default:
				return;
		}

		attackProjectiles.add(new AttackProjectile(prayer, ticks, priority));
	}

	@Subscribe
	private void onNpcSpawned(final NpcSpawned event)
	{
		final NPC npc = event.getNpc();

		final int id = npc.getId();

		if (NPC_IDS_WARDEN_CORE.contains(id))
		{
			coreToTicks = new AbstractMap.SimpleEntry<>(npc, 0);
		}
	}

	@Subscribe
	private void onNpcDespawned(final NpcDespawned event)
	{
		final NPC npc = event.getNpc();

		if (coreToTicks != null && coreToTicks.getKey() == npc)
		{
			coreToTicks = null;
		}
	}

	@Subscribe
	private void onNpcChanged(final NpcChanged event)
	{
		final NPC npc = event.getNpc();

		if (NPC_IDS_WARDEN.contains(npc.getId()))
		{
			this.npc = npc;
		}
	}

	@Subscribe
	private void onScriptPostFired(final ScriptPostFired event)
	{
		if (event.getScriptId() != ScriptID.HP_HUD_UPDATE)
		{
			return;
		}

		final int maxHp = client.getVarbitValue(Varbits.BOSS_HEALTH_MAXIMUM);
		if (maxHp <= 1)
		{
			return;
		}

		final int currentHp = client.getVarbitValue(Varbits.BOSS_HEALTH_CURRENT);
		if (currentHp <= 0)
		{
			return;
		}

		hpRemaining = maxHp - currentHp;
	}

	private HighlightedNpc npcHighlight(final NPC npc)
	{
		final int id = npc.getId();

		if (NPC_IDS_WARDEN_CORE.contains(id))
		{
			return HighlightedNpc.builder()
				.npc(npc)
				.tile(true)
				.borderWidth(1)
				.highlightColor(config.tileOutlineColor())
				.fillColor(config.tileFillColor())
				.render(n -> config.wardenCoreTile())
				.build();
		}

		if (NPC_IDS_WARDEN.contains(id))
		{
			return HighlightedNpc.builder()
				.npc(npc)
				.trueTile(true)
				.borderWidth(1)
				.highlightColor(config.tileOutlineColor())
				.fillColor(config.tileFillColor())
				.render(n -> config.wardenTile())
				.build();
		}

		return null;
	}

	private boolean shouldDraw(final Renderable renderable, final boolean drawingUI)
	{
		if (renderable instanceof Projectile)
		{
			final Projectile projectile = (Projectile) renderable;

			final int id = projectile.getId();

			if (id == PROJECTILE_ID_LIGHTNING)
			{
				return !config.wardenHideLightningProjectiles();
			}
		}
		else if (renderable instanceof GraphicsObject)
		{
			final GraphicsObject graphicsObject = (GraphicsObject) renderable;

			final int id = graphicsObject.getId();

			if (GRAPHICS_OBJECT_IDS_LIGHTNING_TILE.contains(id))
			{
				return !config.wardenHideLightningTiles();
			}
		}

		return true;
	}

	private int getProjectileCycles(final Projectile projectile)
	{
		final WorldPoint projPoint = WorldPoint.fromLocal(client, projectile.getX1(), projectile.getY1(), projectile.getFloor());
		final WorldPoint playerPoint = client.getLocalPlayer().getWorldLocation();
		final int distance = projPoint.distanceTo2D(playerPoint);
		return projectile.getRemainingCycles() + (distance * 3);
	}

}

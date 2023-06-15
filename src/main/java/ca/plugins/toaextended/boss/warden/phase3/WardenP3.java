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
import ca.plugins.toaextended.boss.AttackProjectile;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.nexus.PathLevelTracker;
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
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
import net.runelite.api.NullNpcID;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.Renderable;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WardenP3 implements PluginLifecycleComponent
{

	private static final Set<Integer> NPC_IDS_WARDEN = ImmutableSet.of(NpcID.ELIDINIS_WARDEN_11761,
		NpcID.TUMEKENS_WARDEN_11762);
	private static final Set<Integer> NPC_IDS_WARDEN_CHARGING = ImmutableSet.of(NpcID.ELIDINIS_WARDEN_11763,
		NpcID.TUMEKENS_WARDEN_11764);
	private static final Set<Integer> GRAPHICS_OBJECT_IDS_FALLING_BOULDER = ImmutableSet.of(2250, 2251);

	private static final double[] HP_PERCENT_BREAKPOINTS = new double[]{0.80, 0.60, 0.40, 0.20, 0.05};

	private static final Point POINT_RIGHT_COORD = new Point(33, 36);
	private static final Point POINT_MIDDLE_COORD = new Point(32, 36);
	private static final Point POINT_LEFT_COORD = new Point(31, 36);

	private static final int GRAPHICS_OBJECT_ID_RED_LIGHTNING = 1446;

	private static final int PROJECTILE_ID_ENERGY_SIPHON = 2226;
	private static final int PROJECTILE_ID_ENERGY_SIPHON_CHARGE = 2227;
	private static final int PROJECTILE_ID_TILE_DEBRIS = 2228;
	private static final int PROJECTILE_ID_PRE_FIREBALL = 1481;
	private static final int PROJECTILE_ID_FIREBALL = 2266;

	private static final int PROJECTILE_ID_ZEBAK_MAGIC_ROCK = 2176;
	private static final int PROJECTILE_ID_ZEBAK_MAGIC_ROCK_ENRAGED = 2177;
	private static final int PROJECTILE_ID_ZEBAK_MAGIC_ATTACK = 2181;
	private static final int PROJECTILE_ID_ZEBAK_RANGE_ROCK = 2178;
	private static final int PROJECTILE_ID_ZEBAK_RANGE_ROCK_ENRAGED = 2179;
	private static final int PROJECTILE_ID_ZEBAK_RANGE_ATTACK = 2187;

	private static final Set<Integer> PROJECTILE_IDS_ZEBAK = ImmutableSet.of(
		PROJECTILE_ID_ZEBAK_MAGIC_ROCK,
		PROJECTILE_ID_ZEBAK_MAGIC_ROCK_ENRAGED,
		PROJECTILE_ID_ZEBAK_MAGIC_ATTACK,
		PROJECTILE_ID_ZEBAK_RANGE_ROCK,
		PROJECTILE_ID_ZEBAK_RANGE_ROCK_ENRAGED,
		PROJECTILE_ID_ZEBAK_RANGE_ATTACK
	);

	private static final int ANIMATION_ID_AKKHA_RANGE = 9772;
	private static final int ANIMATION_ID_AKKHA_MAGIC = 9774;
	private static final int ANIMATION_ID_AKKHA_STYLE_SWITCH = 9777;

	private static final int BABA_BOULDER_TICKS = 7;
	private static final int RED_LIGHTNING_TICKS = 4;
	private static final int ENERGY_SIPHON_BASE_TICKS = 7;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

	private final EventBus eventBus;
	private final Client client;
	private final ClientThread clientThread;
	private final Hooks hooks;
	private final ToaExtendedConfig config;
	private final PathLevelTracker pathLevelTracker;

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private NPC warden;

	@Nullable
	private LocalPoint lpRight;
	@Nullable
	private LocalPoint lpLeft;
	@Nullable
	private LocalPoint lpMiddle;

	@Getter(AccessLevel.PACKAGE)
	private SlamDirection nextSlamDirection = SlamDirection.RIGHT;

	@Getter(AccessLevel.PACKAGE)
	private final Queue<AttackProjectile> attackProjectiles = new PriorityQueue<>();
	@Getter(AccessLevel.PACKAGE)
	private final Map<GraphicsObject, Integer> redLightningToTicks = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final Map<NPC, Integer> energySiphonToTicks = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final Map<GraphicsObject, Integer> fallingBouldersToTicks = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final List<Projectile> energySiphonProjectiles = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private final List<Projectile> fireballProjectiles = new ArrayList<>();

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private Prayer akkhaPrayer;

	@Nullable
	private int[] hpBreakpoints;
	@Getter(AccessLevel.PACKAGE)
	private int hpUntilNextBreakPoint = -1;

	@Getter(AccessLevel.PACKAGE)
	private boolean drawSafeTile;

	@Getter(AccessLevel.PACKAGE)
	private long lastTickTime;

	private int energySiphonSet;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.WARDEN_P3;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
		hooks.registerRenderableDrawListener(drawListener);
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
		reset();
	}

	private void reset()
	{
		warden = null;

		lpLeft = null;
		lpRight = null;
		lpMiddle = null;

		nextSlamDirection = SlamDirection.RIGHT;

		attackProjectiles.clear();
		redLightningToTicks.clear();
		energySiphonToTicks.clear();
		fallingBouldersToTicks.clear();
		energySiphonProjectiles.clear();
		fireballProjectiles.clear();

		akkhaPrayer = null;

		hpBreakpoints = null;
		hpUntilNextBreakPoint = -1;

		drawSafeTile = false;
		lastTickTime = 0L;
		energySiphonSet = 0;
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

		if (!fallingBouldersToTicks.isEmpty())
		{
			fallingBouldersToTicks.replaceAll((k, v) -> v - 1);
			fallingBouldersToTicks.values().removeIf(v -> v <= 0);
		}

		if (!redLightningToTicks.isEmpty())
		{
			redLightningToTicks.replaceAll((k, v) -> v - 1);
			redLightningToTicks.values().removeIf(i -> i <= 0);
		}

		if (!energySiphonToTicks.isEmpty())
		{
			energySiphonToTicks.replaceAll((k, v) -> v - 1);
		}

		if (!energySiphonProjectiles.isEmpty())
		{
			energySiphonProjectiles.removeIf(p -> p.getRemainingCycles() <= 0);
		}

		if (!fireballProjectiles.isEmpty())
		{
			fireballProjectiles.removeIf(p -> p.getRemainingCycles() <= 0);
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

	@Subscribe
	private void onProjectileMoved(final ProjectileMoved event)
	{
		final Projectile projectile = event.getProjectile();

		final int remainingCycles = projectile.getRemainingCycles();

		if (remainingCycles <= 0)
		{
			return;
		}

		final Prayer prayer;
		final int ticks;
		final int priority = 0;

		switch (projectile.getId())
		{
			case PROJECTILE_ID_ZEBAK_MAGIC_ROCK:
			case PROJECTILE_ID_ZEBAK_MAGIC_ROCK_ENRAGED:
				prayer = Prayer.PROTECT_FROM_MAGIC;
				ticks = ToaUtils.cyclesToTicks(projectile.getRemainingCycles()) + getZebakProjectileTickCount();
				break;
			case PROJECTILE_ID_ZEBAK_RANGE_ROCK:
			case PROJECTILE_ID_ZEBAK_RANGE_ROCK_ENRAGED:
				prayer = Prayer.PROTECT_FROM_MISSILES;
				ticks = ToaUtils.cyclesToTicks(projectile.getRemainingCycles()) + getZebakProjectileTickCount();
				break;
			case PROJECTILE_ID_ENERGY_SIPHON:
				energySiphonProjectiles.add(projectile);
				return;
			case PROJECTILE_ID_FIREBALL:
				fireballProjectiles.add(projectile);
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

		if (NPC_IDS_WARDEN.contains(id))
		{
			warden = npc;
			drawSafeTile = true;
		}
		else if (id == NpcID.ENERGY_SIPHON)
		{
			energySiphonToTicks.put(npc, ENERGY_SIPHON_BASE_TICKS + energySiphonSet);
		}
	}

	@Subscribe
	private void onNpcDespawned(final NpcDespawned event)
	{
		final NPC npc = event.getNpc();

		if (npc == warden)
		{
			warden = null;
		}
		else if (npc.getId() == NullNpcID.NULL_11773)
		{
			energySiphonToTicks.remove(npc);
		}
	}

	@Subscribe
	private void onNpcChanged(final NpcChanged event)
	{
		if (event.getNpc() == warden &&
			!NPC_IDS_WARDEN_CHARGING.contains(event.getOld().getId()) &&
			isWardenCharging())
		{
			drawSafeTile = true;
			energySiphonSet++;
		}
	}

	@Subscribe
	private void onAnimationChanged(final AnimationChanged event)
	{
		final Actor actor = event.getActor();

		if (!(actor instanceof NPC))
		{
			return;
		}

		final NPC npc = (NPC) actor;

		if (npc == warden)
		{
			final SlamDirection slamDirection = SlamDirection.byAnimId(npc.getAnimation());
			if (slamDirection == null)
			{
				return;
			}
			if (drawSafeTile)
			{
				drawSafeTile = false;
			}
			this.nextSlamDirection = getNextSlamDirection(slamDirection);
		}
		else if (npc.getId() == NpcID.AKKHAS_PHANTOM)
		{
			switch (npc.getAnimation())
			{
				case ANIMATION_ID_AKKHA_STYLE_SWITCH:
					akkhaPrayer = akkhaPrayer == Prayer.PROTECT_FROM_MISSILES ?
						Prayer.PROTECT_FROM_MAGIC : Prayer.PROTECT_FROM_MISSILES;
					break;
				case ANIMATION_ID_AKKHA_MAGIC:
					akkhaPrayer = Prayer.PROTECT_FROM_MAGIC;
					break;
				case ANIMATION_ID_AKKHA_RANGE:
					akkhaPrayer = Prayer.PROTECT_FROM_MISSILES;
					break;
			}
		}
	}

	@Subscribe
	private void onGraphicsObjectCreated(final GraphicsObjectCreated event)
	{
		final GraphicsObject graphicsObject = event.getGraphicsObject();

		final int id = graphicsObject.getId();

		if (GRAPHICS_OBJECT_IDS_FALLING_BOULDER.contains(id))
		{
			fallingBouldersToTicks.put(graphicsObject, BABA_BOULDER_TICKS);
		}
		else if (id == GRAPHICS_OBJECT_ID_RED_LIGHTNING)
		{
			redLightningToTicks.put(graphicsObject, RED_LIGHTNING_TICKS);
		}
	}

	private int getZebakProjectileTickCount()
	{
		return pathLevelTracker.getZebakPathLevel() >= 4 ? 2 : 3;
	}

	boolean isWardenCharging()
	{
		return warden != null && NPC_IDS_WARDEN_CHARGING.contains(warden.getId());
	}

	@Nullable
	LocalPoint getSafeTileLocalPoint()
	{
		switch (nextSlamDirection)
		{
			case RIGHT:
				if (lpLeft == null)
				{
					lpLeft = getInstanceLocalPoint(POINT_LEFT_COORD);
				}
				return lpLeft;
			case LEFT:
				if (lpRight == null)
				{
					lpRight = getInstanceLocalPoint(POINT_RIGHT_COORD);
				}
				return lpRight;
			case MIDDLE:
				if (lpMiddle == null)
				{
					lpMiddle = getInstanceLocalPoint(POINT_MIDDLE_COORD);
				}
				return lpMiddle;
			default:
				throw new IllegalStateException("Unexpected slam direction: " + nextSlamDirection);
		}
	}

	private @Nullable LocalPoint getInstanceLocalPoint(final Point point)
	{
		final Collection<WorldPoint> worldPoints = WorldPoint.toLocalInstance(client,
			WorldPoint.fromRegion(RaidRoom.WARDEN_P3.getRegionId(), point.getX(), point.getY(), 1));

		final WorldPoint worldPoint = worldPoints.stream().findFirst().orElse(null);

		if (worldPoint == null)
		{
			return null;
		}

		return LocalPoint.fromWorld(client, worldPoint);
	}

	private boolean shouldDraw(final Renderable renderable, final boolean drawingUI)
	{
		if (renderable instanceof NPC)
		{
			final NPC npc = (NPC) renderable;
			final int id = npc.getId();

			if (id == NullNpcID.NULL_11773)
			{
				return !config.wardenHideDeadEnergySiphon();
			}
		}
		else if (renderable instanceof Projectile)
		{
			final Projectile projectile = (Projectile) renderable;

			final int id = projectile.getId();

			if (id == PROJECTILE_ID_ENERGY_SIPHON_CHARGE)
			{
				return !config.wardenHideEnergySiphonChargeProjectiles();
			}

			if (id == PROJECTILE_ID_ENERGY_SIPHON)
			{
				return !config.wardenHideEnergySiphonProjectile();
			}

			if (id == PROJECTILE_ID_TILE_DEBRIS)
			{
				return !config.wardenHideTileDebrisProjectile();
			}

			if (id == PROJECTILE_ID_PRE_FIREBALL || id == PROJECTILE_ID_FIREBALL)
			{
				return !config.wardenHideKephriFireballProjectile();
			}

			if (PROJECTILE_IDS_ZEBAK.contains(id))
			{
				return !config.wardenHideZebakProjectile();
			}
		}
		else if (renderable instanceof GraphicsObject)
		{
			final GraphicsObject graphicsObject = (GraphicsObject) renderable;

			final int id = graphicsObject.getId();

			if (GRAPHICS_OBJECT_IDS_FALLING_BOULDER.contains(id))
			{
				return !config.wardenHideBabaFallingBoulders();
			}

			if (id == GRAPHICS_OBJECT_ID_RED_LIGHTNING)
			{
				return !config.wardenHideRedLightning();
			}
		}

		return true;
	}

	private static SlamDirection getNextSlamDirection(final SlamDirection slamDirection)
	{
		switch (slamDirection)
		{
			case RIGHT:
				return SlamDirection.LEFT;
			case LEFT:
				return SlamDirection.MIDDLE;
			case MIDDLE:
				return SlamDirection.RIGHT;
			default:
				throw new IllegalArgumentException("Unexpected slam direction: " + slamDirection);
		}
	}

	@RequiredArgsConstructor
	enum SlamDirection
	{
		RIGHT(9674, 9675),
		LEFT(9676, 9677),
		MIDDLE(9678, 9679);

		private static final Map<Integer, SlamDirection> DIR_BY_ANIM_ID = new HashMap<>();

		static
		{
			DIR_BY_ANIM_ID.put(RIGHT.elidinisAnimId, RIGHT);
			DIR_BY_ANIM_ID.put(RIGHT.tumekenAnimId, RIGHT);
			DIR_BY_ANIM_ID.put(LEFT.elidinisAnimId, LEFT);
			DIR_BY_ANIM_ID.put(LEFT.tumekenAnimId, LEFT);
			DIR_BY_ANIM_ID.put(MIDDLE.elidinisAnimId, MIDDLE);
			DIR_BY_ANIM_ID.put(MIDDLE.tumekenAnimId, MIDDLE);
		}

		private final int elidinisAnimId;
		private final int tumekenAnimId;

		@Nullable
		static SlamDirection byAnimId(final int id)
		{
			return DIR_BY_ANIM_ID.get(id);
		}
	}

}

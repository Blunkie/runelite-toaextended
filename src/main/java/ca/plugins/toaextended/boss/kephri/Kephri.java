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
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Projectile;
import net.runelite.api.Renderable;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class Kephri implements PluginLifecycleComponent
{
	private static final String NPC_NAME = "Kephri";
	private static final int ANIM_ID_FIREBALL_ATTACK = 9577;
	private static final int ANIM_ID_SPECIAL_ATTACK = 9578;
	private static final int ANIM_ID_INACTIVE = 9579;
	private static final int ANIM_ID_SCARAB_SWARM_DEATH = 9607;

	private static final int PROJECTILE_ID_AGILE_SCARAB = 2152;
	private static final int PROJECTILE_ID_PRE_FIREBALL = 1481;
	private static final int PROJECTILE_ID_FIREBALL = 2266;
	private static final int PROJECTILE_ID_BOMBER_SCARAB = 2147;

	private static final int ATK_COUNT_INIT = 3;
	private static final int ATK_COUNT_MAX = 5;
	private static final int TICK_COUNT_EGG = 15;

	private final EventBus eventBus;
	private final Client client;
	private final ClientThread clientThread;
	private final Hooks hooks;
	private final NpcOverlayService npcOverlayService;
	private final ToaExtendedConfig config;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	private final Function<NPC, HighlightedNpc> npcHighlighter = this::npcHighlight;

	@Getter(AccessLevel.PACKAGE)
	private final List<Projectile> fireballProjectiles = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private final Map<NPC, Integer> eggsToTicks = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final List<NPC> scarabSwarmNpcs = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private int atkCount;
	private int prevAtkCount;
	private int seenFours;
	private int numOfFours;

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private NPC npc;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.KEPHRI;
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

		int initAtkCount = ATK_COUNT_INIT;

		if (config.kephriAttackCounter() == AttackCounter.MEDIC)
		{
			--initAtkCount;
		}

		atkCount = prevAtkCount = initAtkCount;
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
		npc = null;
		fireballProjectiles.clear();
		eggsToTicks.clear();
		scarabSwarmNpcs.clear();
		atkCount = 0;
		prevAtkCount = 0;
		seenFours = 0;
		numOfFours = 0;
	}

	@Subscribe
	private void onGameTick(final GameTick event)
	{
		if (!fireballProjectiles.isEmpty())
		{
			fireballProjectiles.removeIf(p -> p.getRemainingCycles() <= 0);
		}

		if (!scarabSwarmNpcs.isEmpty() && npc != null)
		{
			final WorldArea worldArea = npc.getWorldArea();
			scarabSwarmNpcs.removeIf(s -> worldArea.isInMeleeDistance(s.getWorldLocation()));
		}

		if (!eggsToTicks.isEmpty())
		{
			eggsToTicks.replaceAll((k, v) -> v - 1);
			eggsToTicks.values().removeIf(v -> v <= 0);
		}
	}

	@Subscribe
	private void onProjectileMoved(final ProjectileMoved event)
	{
		final Projectile projectile = event.getProjectile();

		final int id = projectile.getId();

		if (id == PROJECTILE_ID_FIREBALL || id == PROJECTILE_ID_BOMBER_SCARAB)
		{
			fireballProjectiles.add(projectile);
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
		else
		{
			final int id = npc.getId();

			if (id == NpcID.EGG_11728)
			{
				eggsToTicks.put(npc, TICK_COUNT_EGG);
			}
			else if (id == NpcID.SCARAB_SWARM_11723)
			{
				scarabSwarmNpcs.add(npc);
			}
		}
	}

	@Subscribe
	private void onNpcDespawned(final NpcDespawned event)
	{
		final NPC npc = event.getNpc();

		final int id = npc.getId();

		if (npc == this.npc)
		{
			reset();
		}
		else if (id == NpcID.EGG_11728)
		{
			eggsToTicks.remove(npc);
		}
		else if (id == NpcID.SCARAB_SWARM_11723)
		{
			scarabSwarmNpcs.remove(npc);
		}
	}

	@Subscribe
	private void onNpcChanged(final NpcChanged event)
	{
		if (event.getNpc().getId() == NpcID.KEPHRI_11722)
		{
			npc = null;
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

		if (npc.getId() == NpcID.SCARAB_SWARM_11723)
		{
			scarabSwarmNpcs.remove(npc);
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

		final int animId = npc.getAnimation();

		if (npc == this.npc)
		{
			updateAttackCount(animId);
		}
		else if (npc.getId() == NpcID.SCARAB_SWARM_11723 && animId == ANIM_ID_SCARAB_SWARM_DEATH)
		{
			scarabSwarmNpcs.remove(npc);
		}
	}

	// TODO incomplete/rewrite. Kephri's attack cycle differs if medic invocation is on.
	private void updateAttackCount(final int animationId)
	{
		if (animationId == ANIM_ID_FIREBALL_ATTACK)
		{
			if (atkCount > 0)
			{
				--atkCount;
			}

			return;
		}

		final boolean medic = config.kephriAttackCounter() == AttackCounter.MEDIC;

		switch (animationId)
		{
			case ANIM_ID_SPECIAL_ATTACK:
				if (!medic)
				{
					atkCount = ATK_COUNT_MAX;
					return;
				}

				if (prevAtkCount <= ATK_COUNT_INIT)
				{
					atkCount = prevAtkCount = ATK_COUNT_MAX - 1;
					numOfFours = 1;
					seenFours = 0;
				}
				else if (prevAtkCount == (ATK_COUNT_MAX - 1))
				{
					if (++seenFours == numOfFours)
					{
						prevAtkCount = ATK_COUNT_MAX;
					}
					atkCount = prevAtkCount;
				}
				else if (prevAtkCount == ATK_COUNT_MAX)
				{
					atkCount = prevAtkCount = ATK_COUNT_MAX - 1;
					seenFours = 0;
					if (numOfFours < 3)
					{
						numOfFours++;
					}
				}
				break;
			case ANIM_ID_INACTIVE:
				atkCount = medic ? ATK_COUNT_MAX : Math.max(atkCount + 1, 2);
				break;
			default:
				break;
		}
	}

	private boolean shouldDraw(final Renderable renderable, final boolean drawingUI)
	{
		if (renderable instanceof NPC)
		{
			final NPC npc = (NPC) renderable;
			final int id = npc.getId();

			if (id == NpcID.SCARAB_SWARM_11723)
			{
				if (!config.kephriHideUnattackableScarabSwarm())
				{
					return true;
				}
				if (npc.getAnimation() == ANIM_ID_SCARAB_SWARM_DEATH)
				{
					return false;
				}
				if (this.npc == null)
				{
					return true;
				}
				return !this.npc.getWorldArea().isInMeleeDistance(npc.getWorldLocation());
			}

			if (id == NpcID.AGILE_SCARAB)
			{
				return !config.kephriHideAgileScarabNpc();
			}

			return true;
		}
		else if (renderable instanceof Projectile)
		{
			final int id = ((Projectile) renderable).getId();

			if (id == PROJECTILE_ID_FIREBALL || id == PROJECTILE_ID_PRE_FIREBALL)
			{
				return !config.kephriHideFireballProjectile();
			}
			if (id == PROJECTILE_ID_AGILE_SCARAB)
			{
				return !config.kephriHideAgileScarabProjectile();
			}
			if (id == PROJECTILE_ID_BOMBER_SCARAB)
			{
				return !config.kephriHideBomberScarabProjectile();
			}

			return true;
		}

		return true;
	}

	private HighlightedNpc npcHighlight(final NPC npc)
	{
		final Color color;

		switch (npc.getId())
		{
			case NpcID.SOLDIER_SCARAB:
				color = Color.RED;
				break;
			case NpcID.SPITTING_SCARAB:
				color = Color.GREEN;
				break;
			case NpcID.ARCANE_SCARAB:
				color = Color.BLUE;
				break;
			default:
				return null;
		}

		return HighlightedNpc.builder()
			.npc(npc)
			.outline(true)
			.borderWidth(1.0F)
			.highlightColor(color)
			.render(n -> config.kephriOverlordOutline() && !npc.isDead())
			.build();
	}
}

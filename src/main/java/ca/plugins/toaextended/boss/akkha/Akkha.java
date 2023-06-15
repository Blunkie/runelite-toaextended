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
package ca.plugins.toaextended.boss.akkha;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.Color;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.NpcID;
import net.runelite.api.Prayer;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class Akkha implements PluginLifecycleComponent
{

	private static final double[] HP_PERCENT_BREAKPOINTS = new double[]{0.8, 0.6, 0.4, 0.2};

	private static final int ANIM_ID_MELEE = 9770;
	private static final int ANIM_ID_MAGIC = 9774;
	private static final int ANIM_ID_MISSILES = 9772;
	private static final int ATTACK_TICK_COUNT = 6;

	private static final String NPC_NAME = "Akkha";
	private static final String MESSAGE_START = "challenge started: akkha.";

	private final EventBus eventBus;
	private final Client client;
	private final ClientThread clientThread;

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private NPC npc;

	@Getter(AccessLevel.PACKAGE)
	private AttackStyle attackStyle = AttackStyle.MELEE;

	@Nullable
	private int[] hpBreakpoints;
	@Getter(AccessLevel.PACKAGE)
	private int hpUntilNextBreakPoint = -1;

	@Getter(AccessLevel.PACKAGE)
	private int ticksUntilNextAttack;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.AKKHA;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
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
		reset();
	}

	private void reset()
	{
		npc = null;
		hpBreakpoints = null;
		hpUntilNextBreakPoint = -1;
		ticksUntilNextAttack = 0;
		attackStyle = AttackStyle.MELEE;
	}

	@Subscribe
	private void onGameTick(final GameTick event)
	{
		if (ticksUntilNextAttack > 0)
		{
			--ticksUntilNextAttack;
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
		if (event.getNpc() == npc)
		{
			npc = null;
		}
	}

	@Subscribe
	private void onNpcChanged(final NpcChanged event)
	{
		final NPC npc = event.getNpc();

		if (npc != this.npc)
		{
			return;
		}

		final NPCComposition composition = npc.getComposition();

		final AttackStyle attackStyle;

		switch (composition.getId())
		{
			case NpcID.AKKHA_11791:
				attackStyle = AttackStyle.RANGE;
				break;
			case NpcID.AKKHA_11790:
				attackStyle = AttackStyle.MELEE;
				break;
			case NpcID.AKKHA_11792:
				attackStyle = AttackStyle.MAGE;
				break;
			default:
				return;
		}

		this.attackStyle = attackStyle;
	}

	@Subscribe
	private void onAnimationChanged(final AnimationChanged event)
	{
		final Actor actor = event.getActor();

		if (actor != this.npc)
		{
			return;
		}

		final int animationId = actor.getAnimation();

		final AttackStyle attackStyle;

		switch (animationId)
		{
			case ANIM_ID_MELEE:
				attackStyle = AttackStyle.MELEE;
				break;
			case ANIM_ID_MISSILES:
				attackStyle = AttackStyle.RANGE;
				break;
			case ANIM_ID_MAGIC:
				attackStyle = AttackStyle.MAGE;
				break;
			default:
				return;
		}

		this.attackStyle = attackStyle;
		ticksUntilNextAttack = ATTACK_TICK_COUNT;
	}

	@Subscribe
	private void onChatMessage(final ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		final String message = Text.standardize(event.getMessage());

		if (message.equals(MESSAGE_START))
		{
			attackStyle = AttackStyle.MELEE;
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

	Prayer getPrayer()
	{
		if (npc != null)
		{
			final int animId = npc.getAnimation();

			switch (animId)
			{
				case Akkha.ANIM_ID_MAGIC:
					return Prayer.PROTECT_FROM_MAGIC;
				case Akkha.ANIM_ID_MISSILES:
					return Prayer.PROTECT_FROM_MISSILES;
				default:
					break;
			}
		}

		return attackStyle.getPrayer();
	}

	@Getter(AccessLevel.PACKAGE)
	@RequiredArgsConstructor
	enum AttackStyle
	{
		MELEE(Prayer.PROTECT_FROM_MELEE, Color.RED),
		RANGE(Prayer.PROTECT_FROM_MISSILES, Color.GREEN),
		MAGE(Prayer.PROTECT_FROM_MAGIC, Color.BLUE);

		private final Prayer prayer;
		private final Color color;
	}
}


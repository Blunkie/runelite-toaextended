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
package ca.plugins.toaextended.challenge;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.ToaExtendedConfig.QuickProceed;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.RaidStateTracker;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.MenuEntry;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class QuickProceedSwaps implements PluginLifecycleComponent
{
	private static final Set<Integer> NPC_IDS = ImmutableSet.of(
		NpcID.OSMUMTEN, // post-demi-boss
		NpcID.OSMUMTEN_11690, // pre-warden
		NpcID.OSMUMTEN_11693 // loot room
	);

	private static final Set<Integer> OBJECT_IDS = ImmutableSet.of(
		ObjectID.PATH_OF_CRONDIS,
		ObjectID.PATH_OF_SCABARAS,
		ObjectID.PATH_OF_HET,
		ObjectID.PATH_OF_APMEKEN,
		ObjectID.BARRIER_45135,
		ObjectID.TELEPORT_CRYSTAL_45505, // kephri
		ObjectID.TELEPORT_CRYSTAL_45506, // zebak
		ObjectID.TELEPORT_CRYSTAL_45866, // akkha
		ObjectID.TELEPORT_CRYSTAL_45754, // ba-ba // Quick-Use
		ObjectID.ENTRY_45131, // het
		ObjectID.ENTRY_45337, // scabaras
		ObjectID.ENTRY_45397, // crondis
		ObjectID.ENTRY_45500, // apmeken
		ObjectID.ENTRY_46168 // wardens
	);

	private final EventBus eventBus;
	private final ToaExtendedConfig config;
	private final RaidStateTracker raidStateTracker;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.isInRaid() && config.quickProceedSwaps() != QuickProceed.OFF;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
	}

	@Subscribe
	public void onMenuEntryAdded(final MenuEntryAdded event)
	{
		final MenuEntry menuEntry = event.getMenuEntry();

		if (shouldDeprioritize(menuEntry))
		{
			menuEntry.setDeprioritized(true);
		}
	}

	private boolean shouldDeprioritize(final MenuEntry menuEntry)
	{
		final String option = menuEntry.getOption();

		switch (menuEntry.getType())
		{
			case NPC_FIRST_OPTION:
				return menuEntry.getNpc() != null &&
					NPC_IDS.contains(menuEntry.getNpc().getId()) && option.equals("Talk-to");

			case GAME_OBJECT_FIRST_OPTION:
				final int id = menuEntry.getIdentifier();

				if (id == ObjectID.BARRIER_45135 &&
					raidStateTracker.getCurrentRoom() == RaidRoom.CRONDIS &&
					option.equals("Pass"))
				{
					return config.quickProceedSwaps() != QuickProceed.SPEEDRUN;
				}

				return OBJECT_IDS.contains(id) &&
					(option.equals("Enter") || option.equals("Use") || option.equals("Pass"));
			default:
				return false;
		}
	}
}

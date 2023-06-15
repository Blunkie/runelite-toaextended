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
package ca.plugins.toaextended.util;

import ca.plugins.toaextended.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RaidStateTracker implements PluginLifecycleComponent
{

	private static final int REGION_LOBBY = 13454;
	private static final int WIDGET_PARENT_ID = 481;
	private static final int WIDGET_CHILD_ID = 40;

	private final Client client;
	private final EventBus eventBus;

	private RaidState currentState = new RaidState(false, false, null);

	// delay inRaid = false by 3 ticks to alleviate any unexpected delays between rooms
	private int raidLeaveTicks = 3;

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

	@Subscribe(priority = 5)
	public void onGameTick(final GameTick event)
	{
		final LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		final int region = lp == null ? -1 : WorldPoint.fromLocalInstance(client, lp).getRegionID();

		final Widget w = client.getWidget(WIDGET_PARENT_ID, WIDGET_CHILD_ID);

		final boolean inLobby = region == REGION_LOBBY;
		final RaidRoom currentRoom = RaidRoom.fromRegionId(region);
		final boolean inRaidRaw = currentRoom != null || (w != null && !w.isHidden());

		raidLeaveTicks = inRaidRaw ? 3 : raidLeaveTicks - 1;
		final boolean inRaid = raidLeaveTicks > 0;

		final RaidState previousState = this.currentState;
		final RaidState newState = new RaidState(inLobby, inRaid, currentRoom);
		if (!previousState.equals(newState))
		{
			this.currentState = newState;
			eventBus.post(new RaidStateChanged(previousState, newState));
		}
	}

	public RaidRoom getCurrentRoom()
	{
		return this.currentState.getCurrentRoom();
	}

	public RaidState getCurrentState()
	{
		return this.currentState;
	}

}

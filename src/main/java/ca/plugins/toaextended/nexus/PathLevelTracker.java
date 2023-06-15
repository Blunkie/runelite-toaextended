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
package ca.plugins.toaextended.nexus;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PathLevelTracker implements PluginLifecycleComponent
{

	private static final int CHILD_ID_KEPHRI = 49;
	private static final int CHILD_ID_AKKHA = 51;
	private static final int CHILD_ID_BABA = 53;
	private static final int CHILD_ID_ZEBAK = 55;

	private final EventBus eventBus;
	private final Client client;

	@Getter
	private int kephriPathLevel;
	@Getter
	private int akkhaPathLevel;
	@Getter
	private int babaPathLevel;
	@Getter
	private int zebakPathLevel;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.NEXUS;
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
	public void onWidgetLoaded(final WidgetLoaded event)
	{
		final int groupId = event.getGroupId();

		if (groupId != WidgetID.TOA_RAID_GROUP_ID)
		{
			return;
		}

		Widget widget = client.getWidget(WidgetID.TOA_RAID_GROUP_ID, CHILD_ID_KEPHRI);

		if (widget != null)
		{
			kephriPathLevel = Integer.parseInt(widget.getText());
		}

		widget = client.getWidget(WidgetID.TOA_RAID_GROUP_ID, CHILD_ID_AKKHA);

		if (widget != null)
		{
			akkhaPathLevel = Integer.parseInt(widget.getText());
		}

		widget = client.getWidget(WidgetID.TOA_RAID_GROUP_ID, CHILD_ID_BABA);

		if (widget != null)
		{
			babaPathLevel = Integer.parseInt(widget.getText());
		}

		widget = client.getWidget(WidgetID.TOA_RAID_GROUP_ID, CHILD_ID_ZEBAK);

		if (widget != null)
		{
			zebakPathLevel = Integer.parseInt(widget.getText());
		}
	}

}

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
package ca.plugins.toaextended.pointstracker;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.event.Sarcophagus;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.RaidStateChanged;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Varbits;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PointsTracker implements PluginLifecycleComponent
{

	private static final String START_MESSAGE = "You enter the Tombs of Amascut";
	private static final String DEATH_MESSAGE = "You have died";
	private static final String ROOM_FAIL_MESSAGE = "Your party failed to complete";
	private static final String ROOM_FINISH_MESSAGE = "Challenge complete";

	private static final NumberFormat POINTS_FORMAT = NumberFormat.getInstance();
	private static final NumberFormat PERCENT_FORMAT = new DecimalFormat("#.##%");

	private static final int BASE_POINTS = 5000;
	private static final int MAX_ROOM_POINTS = 20_000;

	// i'm not sure whether BASE_POINTS should be added
	// i.e. is it 64k available to earn pre- or post- 5000 pt subtraction
	private static final int MAX_TOTAL_POINTS = 64_000 + BASE_POINTS;

	private static final int ANIMATION_ID_WARDEN_DOWN = 9670;

	private static final Map<Integer, Double> DAMAGE_POINTS_FACTORS = ImmutableMap.<Integer, Double>builder()
		.put(NpcID.CORE, 0.0)
		.put(NpcID.CORE_11771, 0.0)
		.put(NpcID.ENERGY_SIPHON, 0.0)
		.put(NpcID.BOULDER_11782, 0.0)
		.put(NpcID.BOULDER_11783, 0.0)
		.put(NpcID.BABOON_BRAWLER, 1.2)
		.put(NpcID.BABOON_BRAWLER_11712, 1.2)
		.put(NpcID.BABOON_THROWER, 1.2)
		.put(NpcID.BABOON_THROWER_11713, 1.2)
		.put(NpcID.BABOON_MAGE, 1.2)
		.put(NpcID.BABOON_MAGE_11714, 1.2)
		.put(NpcID.BABOON_SHAMAN, 1.2)
		.put(NpcID.VOLATILE_BABOON, 1.2)
		.put(NpcID.CURSED_BABOON, 1.2)
		.put(NpcID.BABOON_THRALL, 1.2)
		.put(NpcID.BABA, 2.0)
		.put(NpcID.BABA_11779, 2.0)
		.put(NpcID.BABA_11780, 2.0)
		.put(NpcID.ZEBAK, 1.5)
		.put(NpcID.ZEBAK_11730, 1.5)
		.put(NpcID.ZEBAK_11732, 1.5)
		.put(NpcID.ZEBAK_11733, 1.5)
		.put(NpcID.SPITTING_SCARAB, 0.5)
		.put(NpcID.SOLDIER_SCARAB, 0.5)
		.put(NpcID.ARCANE_SCARAB, 0.5)
		.put(NpcID.HETS_SEAL_WEAKENED, 2.5)
		.put(NpcID.OBELISK_11750, 1.5)
		.put(NpcID.OBELISK_11751, 1.5)
		.put(NpcID.OBELISK_11752, 1.5)
		.put(NpcID.ELIDINIS_WARDEN, 0.0) // non-combat wardens (prevents extra points during p1->p2 transition)
		.put(NpcID.ELIDINIS_WARDEN_11748, 0.0)
		.put(NpcID.TUMEKENS_WARDEN, 0.0)
		.put(NpcID.TUMEKENS_WARDEN_11749, 0.0)
		.put(NpcID.ELIDINIS_WARDEN_11759, 0.0)
		.put(NpcID.TUMEKENS_WARDEN_11760, 0.0)
		.put(NpcID.ELIDINIS_WARDEN_11753, 2.0) // p2 wardens
		.put(NpcID.ELIDINIS_WARDEN_11754, 2.0)
		.put(NpcID.ELIDINIS_WARDEN_11755, 0.0) // downed
		.put(NpcID.TUMEKENS_WARDEN_11756, 2.0)
		.put(NpcID.TUMEKENS_WARDEN_11757, 2.0)
		.put(NpcID.TUMEKENS_WARDEN_11758, 0.0) // downed
		.put(NpcID.ELIDINIS_WARDEN_11761, 2.5) // p3 wardens
		.put(NpcID.ELIDINIS_WARDEN_11763, 2.5)
		.put(NpcID.TUMEKENS_WARDEN_11762, 2.5)
		.put(NpcID.TUMEKENS_WARDEN_11764, 2.5)
		.build();

	// these have a cap at 3 "downs"
	private static final ImmutableSet<Integer> P2_WARDENS = ImmutableSet.of(
		NpcID.ELIDINIS_WARDEN_11753,
		NpcID.ELIDINIS_WARDEN_11754,
		NpcID.ELIDINIS_WARDEN_11755,
		NpcID.TUMEKENS_WARDEN_11756,
		NpcID.TUMEKENS_WARDEN_11757,
		NpcID.TUMEKENS_WARDEN_11758
	);

	private static final ImmutableSet<Integer> MVP_ITEMS = ImmutableSet.of(
		ItemID.FANG_27219,
		ItemID.SCARAB_DUNG,
		ItemID.BIG_BANANA,
		ItemID.ELDRITCH_ASHES
	);

	private static final ImmutableSet<Integer> WARDEN_HITSPLAT_TYPES = ImmutableSet.of(
		53, // hit
		55 // max hit
	);

	private final EventBus eventBus;
	private final Client client;
	private final ConfigManager configManager;

	private final Collection<Integer> seenMvpItems = new HashSet<>(4);

	@Getter
	private int personalRoomPoints;
	private int personalTotalPoints = BASE_POINTS;
	private int nonPartyPoints; // points that are earned once by the entire party

	private int raidLevel;
	private int wardenDowns;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.isInRaid();
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
		reset();
	}

	@Subscribe
	public void onGameTick(final GameTick e)
	{
		raidLevel = client.getVarbitValue(Varbits.TOA_RAID_LEVEL);
	}

	@Subscribe
	public void onRaidStateChanged(final RaidStateChanged e)
	{
		if (e.getPreviousState() == null || e.getPreviousState().getCurrentRoom() == null)
		{
			return;
		}

		switch (e.getPreviousState().getCurrentRoom())
		{
			case NEXUS:
				break;

			// puzzle estimates
			case SCABARAS:
				personalTotalPoints += 300;
				nonPartyPoints += 300;
				break;

			case APMEKEN:
				personalTotalPoints += 450;
				nonPartyPoints += 300;
				break;

			case CRONDIS:
				personalTotalPoints += 400;
				nonPartyPoints += 300;
				break;

			case HET:
			case WARDEN_P2:
			case WARDEN_P3:
				nonPartyPoints += 300;
				break;
		}
	}

	@Subscribe
	public void onChatMessage(final ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (e.getMessage().startsWith(START_MESSAGE))
		{
			reset();
		}
		else if (e.getMessage().startsWith(DEATH_MESSAGE))
		{
			personalTotalPoints -= Math.max(0.2 * personalTotalPoints, 1000);
			if (personalTotalPoints < 0)
			{
				personalTotalPoints = 0;
			}
		}
		else if (e.getMessage().startsWith(ROOM_FAIL_MESSAGE))
		{
			wardenDowns = 0;
			personalRoomPoints = 0;
		}
		else if (e.getMessage().startsWith(ROOM_FINISH_MESSAGE))
		{
			personalTotalPoints = Math.min(MAX_TOTAL_POINTS, personalTotalPoints + personalRoomPoints);
			personalRoomPoints = 0;
		}
	}

	@Subscribe
	public void onHitsplatApplied(final HitsplatApplied e)
	{
		if (e.getHitsplat().getAmount() < 1 || !(e.getActor() instanceof NPC))
		{
			return;
		}

		final NPC target = (NPC) e.getActor();

		if (P2_WARDENS.contains(target.getId()) && wardenDowns > 3)
		{
			return;
		}

		final double factor = DAMAGE_POINTS_FACTORS.getOrDefault(target.getId(), 1.0);

		if (e.getHitsplat().isMine() || WARDEN_HITSPLAT_TYPES.contains(e.getHitsplat().getHitsplatType()))
		{
			this.personalRoomPoints = (int) Math.min(MAX_ROOM_POINTS, personalRoomPoints + e.getHitsplat().getAmount() * factor);
		}
	}

	@Subscribe
	public void onItemSpawned(final ItemSpawned e)
	{
		final int id = e.getItem().getId();

		if (MVP_ITEMS.contains(id) && !seenMvpItems.contains(id))
		{
			personalTotalPoints += 300;
			seenMvpItems.add(id);
		}
	}

	@Subscribe
	public void onAnimationChanged(final AnimationChanged e)
	{
		if (!(e.getActor() instanceof NPC) || !P2_WARDENS.contains(((NPC) e.getActor()).getId()))
		{
			return;
		}

		if (e.getActor().getAnimation() == ANIMATION_ID_WARDEN_DOWN)
		{
			wardenDowns++;
		}
	}

	@Subscribe
	public void onSarcophagus(final Sarcophagus e)
	{
		final int dryStreak = getPurpleDryStreak() + 1;

		final String msg = String.format(
			"Total Points: <col=ff0000>%s</col> Unique Chance: <col=ff0000>%s</col> Dry Streak%s: <col=ff0000>%d</col> (<col=ff009a>%s</col>)",
			POINTS_FORMAT.format(getTotalPoints()),
			PERCENT_FORMAT.format(getUniqueChance()),
			e.isPurple() ? " Ended" : "",
			dryStreak,
			PERCENT_FORMAT.format(UniqueChanceCalculator.calcUniqueOdds(getUniqueChance(), dryStreak))
		);

		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);

		savePurpleDryStreak(e.isPurple() ? 0 : dryStreak);
	}

	private int getPersonalTotalPoints()
	{
		return this.personalTotalPoints - BASE_POINTS;
	}

	private int getTotalPoints()
	{
		return getPersonalTotalPoints() + personalRoomPoints + nonPartyPoints;
	}

	private double getUniqueChance()
	{
		return UniqueChanceCalculator.getUniqueChance(raidLevel, getTotalPoints()) / 100;
	}

	private void reset()
	{
		personalRoomPoints = 0;
		personalTotalPoints = BASE_POINTS;
		nonPartyPoints = 0;
		raidLevel = -1;
		wardenDowns = 0;
		seenMvpItems.clear();
	}

	private int getPurpleDryStreak()
	{
		final String value = configManager.getConfiguration(ToaExtendedConfig.CONFIG_GROUP,
			ToaExtendedConfig.CONFIG_KEY_DRYSTREAK);

		if (value == null || value.isEmpty())
		{
			return 1;
		}
		else
		{
			try
			{
				return Integer.parseInt(value);
			}
			catch (final NumberFormatException e)
			{
				return 0;
			}
		}
	}

	private void savePurpleDryStreak(final int dryStreak)
	{
		configManager.setConfiguration(ToaExtendedConfig.CONFIG_GROUP, ToaExtendedConfig.CONFIG_KEY_DRYSTREAK,
			dryStreak);
	}
}

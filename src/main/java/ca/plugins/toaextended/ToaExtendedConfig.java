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
package ca.plugins.toaextended;

import java.awt.Color;
import java.awt.Font;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(ToaExtendedConfig.CONFIG_GROUP)
public interface ToaExtendedConfig extends Config
{

	String CONFIG_GROUP = "toaextended";
	String CONFIG_KEY_DRYSTREAK = "drystreak";

	// Sections

	@ConfigSection(
		name = "Zebak",
		description = "Zebak configuration.",
		position = 0,
		closedByDefault = true
	)
	String SECTION_ZEBAK = "zebakSection";

	@ConfigSection(
		name = "Kephri",
		description = "Kephri configuration.",
		position = 1,
		closedByDefault = true
	)
	String SECTION_KEPHRI = "kephriSection";

	@ConfigSection(
		name = "Baba",
		description = "Baba configuration.",
		position = 2,
		closedByDefault = true
	)
	String SECTION_BABA = "babaSection";

	@ConfigSection(
		name = "Akkha",
		description = "Akkha configuration.",
		position = 3,
		closedByDefault = true
	)
	String SECTION_AKKHA = "akkhaSection";

	@ConfigSection(
		name = "Warden Phase 2",
		description = "Warden phase 2 configuration.",
		position = 4,
		closedByDefault = true
	)
	String SECTION_WARDEN_P2 = "wardenPhase2Section";

	@ConfigSection(
		name = "Warden Phase 3",
		description = "Warden phase 3 configuration.",
		position = 5,
		closedByDefault = true
	)
	String SECTION_WARDEN_P3 = "wardenPhase3Section";

	@ConfigSection(
		name = "Scabaras Challenge",
		description = "Scabaras configuration.",
		position = 6,
		closedByDefault = true
	)
	String SECTION_SCABARAS = "scabarasSection";

	@ConfigSection(
		name = "Apmeken Challenge",
		description = "Apmeken configuration.",
		position = 7,
		closedByDefault = true
	)
	String SECTION_APMEKEN = "apmekenSection";

	@ConfigSection(
		name = "Het Challenge",
		description = "Het configuration.",
		position = 8,
		closedByDefault = true
	)
	String SECTION_HET = "hetSection";

	@ConfigSection(
		name = "Prayer",
		description = "Prayer configuration.",
		position = 9,
		closedByDefault = true
	)
	String SECTION_PRAYER = "prayerSection";

	@ConfigSection(
		name = "Recolor Sarcophagus",
		description = "Sarcophagus recolor configuration.",
		position = 10,
		closedByDefault = true
	)
	String SECTION_RECOLOR_SARCOPHAGUS = "recolorSarcophagusSection";

	@ConfigSection(
		name = "Miscellaneous",
		description = "Miscellaneous configurations.",
		position = 11,
		closedByDefault = true
	)
	String SECTION_MISC = "miscSection";

	// Zebak

	@ConfigItem(
		name = "Prayer Overlay",
		description = "Enable prayer overlays for Zebak." +
			"<br>Requires prayer mode config to be set.",
		position = 0,
		keyName = "zebakPrayerIndicator",
		section = SECTION_ZEBAK
	)
	default boolean zebakPrayerIndicator()
	{
		return false;
	}

	@ConfigItem(
		name = "Health Counter",
		description = "Overlay hp until next phase." +
			"<br>Requires boss hp bar in-game setting turned on.",
		position = 1,
		keyName = "zebakHealthCounter",
		section = SECTION_ZEBAK
	)
	default boolean zebakHealthCounter()
	{
		return false;
	}

	@ConfigItem(
		name = "Blood Cloud Tile",
		description = "Highlight blood cloud tiles.",
		position = 2,
		keyName = "zebakBloodCloudTile",
		section = SECTION_ZEBAK
	)
	default Tile zebakBloodCloudTile()
	{
		return Tile.OFF;
	}

	@ConfigItem(
		name = "Wave Tile",
		description = "Highlight wave tiles.",
		position = 3,
		keyName = "zebakWaveTile",
		section = SECTION_ZEBAK
	)
	default Tile zebakWaveTile()
	{
		return Tile.OFF;
	}

	@ConfigItem(
		name = "Projectile Tile",
		description = "Highlight where rocks, jugs, and poison will land.",
		position = 4,
		keyName = "zebakProjectileTiles",
		section = SECTION_ZEBAK
	)
	default boolean zebakProjectileTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Blood Magic Outline",
		description = "Outline blood magic for visibility.",
		position = 5,
		keyName = "zebakBloodMagicOutline",
		section = SECTION_ZEBAK
	)
	default boolean zebakBloodMagicOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Jug Outline",
		description = "Outline jugs.",
		position = 6,
		keyName = "zebakJugOutline",
		section = SECTION_ZEBAK
	)
	default boolean zebakJugOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Boulder Outline",
		description = "Outline boulders.",
		position = 7,
		keyName = "zebakBoulderOutline",
		section = SECTION_ZEBAK
	)
	default boolean zebakBoulderOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Wave",
		description = "Prevent rendering waves." +
			"<br>Use with wave tile highlighting.",
		position = 8,
		keyName = "zebakHideWaves",
		section = SECTION_ZEBAK
	)
	default boolean zebakHideWaves()
	{
		return false;
	}

	// Kephri

	@ConfigItem(
		name = "Attack Counter",
		description = "Overlay Kephri with an attack counter.",
		position = 0,
		keyName = "kephriAttackCounter",
		section = SECTION_KEPHRI
	)
	default AttackCounter kephriAttackCounter()
	{
		return AttackCounter.OFF;
	}

	@ConfigItem(
		name = "Fireball Tile",
		description = "Highlight where fireball projectiles will land." +
			"<br>Includes bomber scarabs.",
		position = 1,
		keyName = "kephriFireballTiles",
		section = SECTION_KEPHRI
	)
	default FireballRadius kephriFireballTiles()
	{
		return FireballRadius.OFF;
	}

	@ConfigItem(
		name = "Egg Tile",
		description = "Highlight egg explosion radius tiles.",
		position = 2,
		keyName = "kephriEggTiles",
		section = SECTION_KEPHRI
	)
	default boolean kephriEggTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Dung Outline",
		description = "Outline Kephri and player when targetted by dung attack.",
		position = 3,
		keyName = "kephriDungOutline",
		section = SECTION_KEPHRI
	)
	default boolean kephriDungOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Overlord Outline",
		description = "Outline overlord scarabs.",
		position = 4,
		keyName = "kephriOverlordOutline",
		section = SECTION_KEPHRI
	)
	default boolean kephriOverlordOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Swarm Outline",
		description = "Outline scarab swarms.",
		position = 5,
		keyName = "kephriScarabSwarmOutline",
		section = SECTION_KEPHRI
	)
	default boolean kephriScarabSwarmOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Fireball",
		description = "Prevent rendering fireballs." +
			"<br>Use with fireball tile highlighting.",
		position = 6,
		keyName = "kephriHideFireballProjectile",
		section = SECTION_KEPHRI
	)
	default boolean kephriHideFireballProjectile()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Bomber Scarab",
		description = "Prevent rendering bomber scarabs." +
			"<br>Use with fireball tile highlighting.",
		position = 7,
		keyName = "kephriHideBomberScarabProjectile",
		section = SECTION_KEPHRI
	)
	default boolean kephriHideBomberScarabProjectile()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Unattackable Swarm",
		description = "Prevent rendering unattackable swarms.",
		position = 8,
		keyName = "kephriHideUnattackableScarabSwarm",
		section = SECTION_KEPHRI
	)
	default boolean kephriHideUnattackableScarabSwarm()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Agile Scarab",
		description = "Prevent rendering agile scarabs.",
		position = 9,
		keyName = "kephriHideAgileScarabNpc",
		section = SECTION_KEPHRI
	)
	default boolean kephriHideAgileScarabNpc()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Agile Scarab Projectile",
		description = "Prevent rendering agile scarab projectiles.",
		position = 10,
		keyName = "kephriHideAgileScarabProjectile",
		section = SECTION_KEPHRI
	)
	default boolean kephriHideAgileScarabProjectile()
	{
		return false;
	}

	// Baba

	@ConfigItem(
		name = "Health Counter",
		description = "Overlay hp until next phase." +
			"<br>Requires boss hp bar in-game setting turned on.",
		position = 0,
		keyName = "babaHealthCounter",
		section = SECTION_BABA
	)
	default boolean babaHealthCounter()
	{
		return false;
	}

	@ConfigItem(
		name = "Baba Tile",
		description = "Highlight Baba's tile.",
		position = 1,
		keyName = "babaTile",
		section = SECTION_BABA
	)
	default Tile babaTile()
	{
		return Tile.OFF;
	}

	@ConfigItem(
		name = "Shockwave Tile",
		description = "Highlight where shockwaves will hit.",
		position = 2,
		keyName = "babaShockwaveTiles",
		section = SECTION_BABA
	)
	default boolean babaShockwaveTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Rolling Boulder Tile",
		description = "Highlight non-weakened rolling boulders.",
		position = 3,
		keyName = "babaNonWeakenedRollingBoulderTiles",
		section = SECTION_BABA
	)
	default boolean babaNonWeakenedRollingBoulderTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Falling Boulder Tile",
		description = "Highlight where falling boulders will land.",
		position = 4,
		keyName = "babaFallingBoulderTiles",
		section = SECTION_BABA
	)
	default boolean babaFallingBoulderTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Sarcophagus Projectile Tile",
		description = "Highlight where projectiles will land.",
		position = 5,
		keyName = "babaSarcophagusTiles",
		section = SECTION_BABA
	)
	default boolean babaSarcophagusTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Rubble Tile",
		description = "Highlight perimiter of rubble.",
		position = 6,
		keyName = "babaRubbleTiles",
		section = SECTION_BABA
	)
	default boolean babaRubbleTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Banana Peel Tile",
		description = "Highlight banana peels.",
		position = 7,
		keyName = "babaBananaPeelTiles",
		section = SECTION_BABA
	)
	default boolean babaBananaPeelTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Boulder Throw Outline",
		description = "Outline Baba when she throws a boulder.",
		position = 8,
		keyName = "babaBabaSpecOutline",
		section = SECTION_BABA
	)
	default boolean babaSpecialAttackOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Baboon Outline",
		description = "Outline baboons.",
		position = 9,
		keyName = "babaBaboonOutline",
		section = SECTION_BABA
	)
	default boolean babaBaboonOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Rolling Boulder",
		description = "Prevent rendering non-weakened boulders." +
			"<br>Use with rolling boulder tile highlighting.",
		position = 10,
		keyName = "babaHideNonWeakenedRollingBoulders",
		section = SECTION_BABA
	)
	default boolean babaHideNonWeakenedRollingBoulders()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Rolling Boulder Projectile",
		description = "Prevent rendering rolling boulder projectiles.",
		position = 11,
		keyName = "babaHideRollingBoulderProjectiles",
		section = SECTION_BABA
	)
	default boolean babaHideRollingBoulderProjectiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Falling Boulder",
		description = "Prevent rendering falling boulders." +
			"<br>Use with falling boulder tile highlighting.",
		position = 12,
		keyName = "babaHideFallingBoulders",
		section = SECTION_BABA
	)
	default boolean babaHideFallingBoulders()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Baboon Projectile",
		description = "Prevent rendering baboon projectiles.",
		position = 13,
		keyName = "babaHideBaboonProjectiles",
		section = SECTION_BABA
	)
	default boolean babaHideBaboonProjectiles()
	{
		return false;
	}

	// Akkha

	@ConfigItem(
		name = "Prayer Overlay",
		description = "Enable prayer overlays for Akkha." +
			"<br>Requires prayer mode config to be set.",
		position = 0,
		keyName = "akkhaPrayerIndicator",
		section = SECTION_AKKHA
	)
	default boolean akkhaPrayerIndicator()
	{
		return false;
	}

	@ConfigItem(
		name = "Health Counter",
		description = "Overlay hp until next phase." +
			"<br>Requires boss hp bar in-game setting turned on.",
		position = 1,
		keyName = "akkhaHealthCounter",
		section = SECTION_AKKHA
	)
	default boolean akkhaHealthCounter()
	{
		return false;
	}

	@ConfigItem(
		name = "Akkha Tile",
		description = "Highlight Akkha's tile with color indicating attack style.",
		position = 2,
		keyName = "akkhaTile",
		section = SECTION_AKKHA
	)
	default Tile akkhaTile()
	{
		return Tile.OFF;
	}

	@ConfigItem(
		name = "Special Attack Outline",
		description = "Outline Akkha when a special attack is imminent." +
			"<br>Trailing orbs or memory blast.",
		position = 3,
		keyName = "akkhaSpecialAttackOutline",
		section = SECTION_AKKHA
	)
	default boolean akkhaSpecialAttackOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Attack Tick Counter",
		description = "Overlay ticks until Akkha's next attack on player." +
			"<br>Step back invocation.",
		position = 4,
		keyName = "akkhaAttackTickCounter",
		section = SECTION_AKKHA
	)
	default boolean akkhaAttackTickCounter()
	{
		return false;
	}

	@ConfigItem(
		name = "Memory Blast Tracker",
		description = "Highlight the memorized tiles.",
		position = 5,
		keyName = "akkhaMemoryBlastTracker",
		section = SECTION_AKKHA
	)
	default boolean akkhaMemoryBlastTracker()
	{
		return false;
	}

	@ConfigItem(
		name = "Unstable Orb Radius",
		description = "Highlight radius of unstable orbs.",
		position = 6,
		keyName = "akkhaUnstableOrbRadius",
		section = SECTION_AKKHA
	)
	default boolean akkhaUnstableOrbRadius()
	{
		return false;
	}

	@Range(
		max = 7
	)
	@ConfigItem(
		name = "Radius Distance",
		description = "How many tiles to highlight.",
		position = 7,
		keyName = "akkhaRadiusDistance",
		section = SECTION_AKKHA
	)
	default int akkhaRadiusDistance()
	{
		return 3;
	}

	@ConfigItem(
		name = "Unstable Orb Tile",
		description = "Highlight unstable orbs.",
		position = 8,
		keyName = "akkhaUnstableOrbTiles",
		section = SECTION_AKKHA
	)
	default Tile akkhaUnstableOrbTiles()
	{
		return Tile.OFF;
	}

	@ConfigItem(
		name = "Hide Unstable Orbs",
		description = "Prevent rendering unstable orbs." +
			"<br>Use with unstable orb highlighting.",
		position = 9,
		keyName = "akkhaHideUnstableOrbs",
		section = SECTION_AKKHA
	)
	default boolean akkhaHideUnstableOrbs()
	{
		return false;
	}

	// Warden Phase 2

	@ConfigItem(
		name = "Prayer Overlay",
		description = "Enable prayer overlays for Wardens." +
			"<br>Requires prayer mode config to be set.",
		position = 0,
		keyName = "wardenP2PrayerIndicator",
		section = SECTION_WARDEN_P2
	)
	default boolean wardenP2PrayerIndicator()
	{
		return false;
	}

	@ConfigItem(
		name = "Health Counter",
		description = "Overlay hp until next phase." +
			"<br>Requires boss hp bar in-game setting turned on.",
		position = 1,
		keyName = "wardenP2HealthCounter",
		section = SECTION_WARDEN_P2
	)
	default boolean wardenP2HealthCounter()
	{
		return false;
	}

	@ConfigItem(
		name = "Warden Core Tick Timer",
		description = "Overlay warden core with a tick timer.",
		position = 2,
		keyName = "wardenCoreTickTimer",
		section = SECTION_WARDEN_P2
	)
	default boolean wardenCoreTickTimer()
	{
		return false;
	}

	@ConfigItem(
		name = "Warden Tile",
		description = "Highlight warden's tile.",
		position = 3,
		keyName = "wardenTile",
		section = SECTION_WARDEN_P2
	)
	default boolean wardenTile()
	{
		return false;
	}

	@ConfigItem(
		name = "Warden Core Tile",
		description = "Highlight warden core tile.",
		position = 4,
		keyName = "wardenCoreTile",
		section = SECTION_WARDEN_P2
	)
	default boolean wardenCoreTile()
	{
		return false;
	}

	@ConfigItem(
		name = "Black Skull Projectile Tile",
		description = "Highlight black skull projectile.",
		position = 5,
		keyName = "wardenBlackSkullProjectileTile",
		section = SECTION_WARDEN_P2
	)
	default boolean wardenBlackSkullProjectileTile()
	{
		return false;
	}

	@ConfigItem(
		name = "Lightning Projectile Tile",
		description = "Highlight obelisk's lightning projectile.",
		position = 6,
		keyName = "wardenLightningProjectileTile",
		section = SECTION_WARDEN_P2
	)
	default boolean wardenLightningProjectileTile()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Lightning Tile",
		description = "Prevent rendering lightning gfx objects." +
			"<br>Use with lightning tile highlighting.",
		position = 7,
		keyName = "wardenHideLightningTiles",
		section = SECTION_WARDEN_P2
	)
	default boolean wardenHideLightningTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Lightning Projectile",
		description = "Prevent rendering lightning projectiles." +
			"<br>Use with lightning tile highlighting.",
		position = 8,
		keyName = "wardenHideLightningProjectiles",
		section = SECTION_WARDEN_P2
	)
	default boolean wardenHideLightningProjectiles()
	{
		return false;
	}

	// Warden Phase 3

	@ConfigItem(
		name = "Prayer Overlay",
		description = "Enable prayer overlays for Wardens." +
			"<br>Requires prayer mode config to be set.",
		position = 0,
		keyName = "wardenP3PrayerIndicator",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenP3PrayerIndicator()
	{
		return false;
	}

	@ConfigItem(
		name = "Health Counter",
		description = "Overlay hp until next phase." +
			"<br>Requires boss hp bar in-game setting turned on.",
		position = 1,
		keyName = "wardenP3HealthCounter",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenP3HealthCounter()
	{
		return false;
	}

	@ConfigItem(
		name = "Warden Slam Indicator",
		description = "Indicate where to stand when slamming starts/resumes." +
			"<br>Insanity invocation.",
		position = 2,
		keyName = "wardenSlamIndicator",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenSlamIndicator()
	{
		return false;
	}

	@ConfigItem(
		name = "Siphon Tick Timer",
		description = "Overlay energy siphons with a tick timer." +
			"<br>Solo and Insanity invocation.",
		position = 3,
		keyName = "wardenEnergySiphonTickTimer",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenEnergySiphonTickTimer()
	{
		return false;
	}

	@ConfigItem(
		name = "Siphon Projectile Tile",
		description = "Highlight energy siphon projectiles.",
		position = 4,
		keyName = "wardenEnergySiphonProjectileTile",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenEnergySiphonProjectileTile()
	{
		return false;
	}

	@ConfigItem(
		name = "Boulder Tile",
		description = "Highlight Phantom Baba's falling boulder.",
		position = 5,
		keyName = "wardenBabaFallingBoulderTile",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenBabaFallingBoulderTile()
	{
		return false;
	}

	@ConfigItem(
		name = "Fireball Tile",
		description = "Highlight Phantom Kephri's fireball.",
		position = 6,
		keyName = "wardenKephriFireballTile",
		section = SECTION_WARDEN_P3
	)
	default FireballRadius wardenKephriFireballTile()
	{
		return FireballRadius.OFF;
	}

	@ConfigItem(
		name = "Red Lightning Tile",
		description = "Highlight red lightning gfx objects." +
			"<br>Insanity phase.",
		position = 7,
		keyName = "wardenRedLightningTiles",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenRedLightningTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Tile Debris Projectile",
		description = "Prevent rendering tile debris projectiles." +
			"<br>Insanity phase.",
		position = 8,
		keyName = "wardenHideTileDebrisProjectile",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenHideTileDebrisProjectile()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Siphon Projectile",
		description = "Prevent rendering energy siphon projectiles.",
		position = 9,
		keyName = "wardenHideEnergySiphonProjectile",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenHideEnergySiphonProjectile()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Charge Projectile",
		description = "Prevent rendering energy siphon charge projectiles.",
		position = 10,
		keyName = "wardenHideEnergySiphonChargeProjectiles",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenHideEnergySiphonChargeProjectiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Dead Siphon",
		description = "Prevent rendering dead energy siphons.",
		position = 11,
		keyName = "wardenHideDeadEnergySiphon",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenHideDeadEnergySiphon()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Baba Boulder",
		description = "Prevent rendering Phantom Baba's falling boulders." +
			"<br>Use with boulder tile highlighting.",
		position = 12,
		keyName = "wardenHideBabaFallingBoulders",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenHideBabaFallingBoulders()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Kephri Fireball",
		description = "Prevent rendering Phantom Kephri's fireballs." +
			"<br>Use with fireball tile highlighting.",
		position = 13,
		keyName = "wardenHideKephriFireballProjectile",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenHideKephriFireballProjectile()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Zebak Projectile",
		description = "Prevent renndering Zebak's range and mage projectiles." +
			"<br>Use with prayer overlay.",
		position = 14,
		keyName = "wardenHideZebakProjectile",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenHideZebakProjectile()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Red Lightning",
		description = "Prevent rendering red lightning gfx objects." +
			"<br>Insanity phase. Use with red lighting tile highlighting.",
		position = 15,
		keyName = "wardenHideRedLightning",
		section = SECTION_WARDEN_P3
	)
	default boolean wardenHideRedLightning()
	{
		return false;
	}

	// Crondis

	// Scabaras

	@ConfigItem(
		name = "Addition Puzzle Solver",
		description = "Toggle the addition puzzle solver.",
		position = 0,
		keyName = "scabarasAdditionPuzzle",
		section = SECTION_SCABARAS
	)
	default boolean scabarasAdditionPuzzle()
	{
		return false;
	}

	@ConfigItem(
		name = "Light Puzzle Solver",
		description = "Toggle the light puzzle solver.",
		position = 1,
		keyName = "scabarasLightPuzzle",
		section = SECTION_SCABARAS
	)
	default boolean scabarasLightPuzzle()
	{
		return false;
	}

	@ConfigItem(
		name = "Sequence Puzzle Solver",
		description = "Toggle the sequence puzzle solver.",
		position = 2,
		keyName = "scabarasSequencePuzzle",
		section = SECTION_SCABARAS
	)
	default boolean scabarasSequencePuzzle()
	{
		return false;
	}

	@ConfigItem(
		name = "Obelisk Puzzle Solver",
		description = "Toggle the obelisk puzzle solver.",
		position = 3,
		keyName = "scabarasObeliskPuzzle",
		section = SECTION_SCABARAS
	)
	default boolean scabarasObeliskPuzzle()
	{
		return false;
	}

	@ConfigItem(
		name = "Obelisk Falling Rocks Tile",
		description = "Outline tile of falling rocks.",
		position = 4,
		keyName = "scabarasObeliskFallingRocksHighlight",
		section = SECTION_SCABARAS
	)
	default boolean scabarasObeliskFallingRocksHighlight()
	{
		return false;
	}

	@ConfigItem(
		name = "Matching Puzzle Solver",
		description = "Toggle the matching puzzle solver.",
		position = 5,
		keyName = "scabarasMatchingPuzzle",
		section = SECTION_SCABARAS
	)
	default boolean scabarasMatchingPuzzle()
	{
		return false;
	}

	@ConfigItem(
		name = "Highlight Entrance",
		description = "Highlight the entrance to skip obelisks.",
		position = 6,
		keyName = "scabarasHighlightEntrance",
		section = SECTION_SCABARAS
	)
	default boolean scabarasHighlightEntrance()
	{
		return false;
	}

	// Apmeken

	@ConfigItem(
		name = "Baboon Outline",
		description = "Highlight baboons.",
		position = 0,
		keyName = "apmekenBaboonOutline",
		section = SECTION_APMEKEN
	)
	default boolean apmekenBaboonOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Volatile Baboon Tile",
		description = "Highlight the tiles of the explode radius.",
		position = 1,
		keyName = "apmekenVolatileBaboonTiles",
		section = SECTION_APMEKEN
	)
	default boolean apmekenVolatileBaboonTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Roof/Vent Outline",
		description = "Outline vents and roof support.",
		position = 2,
		keyName = "apmekenRoofVentOutline",
		section = SECTION_APMEKEN
	)
	default boolean apmekenRoofVentOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Repair Roof Menu Entry",
		description = "Deprioritize menu entry when inactive.",
		position = 3,
		keyName = "apmekenRepairMenuEntry",
		section = SECTION_APMEKEN
	)
	default boolean apmekenRepairMenuEntry()
	{
		return false;
	}

	// Het

	@ConfigItem(
		name = "Deposit-Pickaxe",
		description = "Automatically swap to Deposit-pickaxe when a pickaxe is in your inventory.",
		position = 0,
		keyName = "hetDepositPickaxe",
		section = SECTION_HET
	)
	default boolean hetDepositPickaxe()
	{
		return false;
	}

	@ConfigItem(
		name = "Caster Statue Beam Timer",
		description = "Overlay caster statue with a count down timer.",
		position = 1,
		keyName = "hetCasterStatueBeamTimer",
		section = SECTION_HET
	)
	default boolean hetCasterStatueBeamTimer()
	{
		return false;
	}

	@ConfigItem(
		name = "Mirror Outline",
		description = "Highlight mirrors you can pick up.",
		position = 2,
		keyName = "hetMirrorOutline",
		section = SECTION_HET
	)
	default boolean hetMirrorOutline()
	{
		return false;
	}

	@ConfigItem(
		name = "Orb of Darkness Tile",
		description = "Highlight orb of darkness tiles.",
		position = 3,
		keyName = "hetOrbOfDarknessTiles",
		section = SECTION_HET
	)
	default boolean hetOrbOfDarknessTiles()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Orbs of Darkness",
		description = "Prevent rendering orbs of darkness.",
		position = 4,
		keyName = "hetHideOrbsOfDarkness",
		section = SECTION_HET
	)
	default boolean hetHideOrbsOfDarkness()
	{
		return false;
	}

	// Prayer

	@ConfigItem(
		name = "Prayer Mode",
		description = "Types of overlays to use to indicate prayers.",
		position = 0,
		keyName = "prayerMode",
		section = SECTION_PRAYER
	)
	default PrayerMode prayerMode()
	{
		return PrayerMode.OFF;
	}

	@ConfigItem(
		name = "Descending Boxes (Guitar Hero)",
		description = "Overlay descending boxes on prayer widgets.",
		position = 2,
		keyName = "prayerDescendingBoxes",
		section = SECTION_PRAYER
	)
	default boolean prayerDescendingBoxes()
	{
		return false;
	}

	@ConfigItem(
		name = "Non-Priority Boxes (Guitar Hero)",
		description = "Show boxes for all upcoming attacks." +
			"<br>Requires descending boxes to be enabled.",
		position = 3,
		keyName = "prayerNonPriority",
		section = SECTION_PRAYER
	)
	default boolean prayerNonPriorityBoxes()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		name = "Box Color",
		description = "Color of boxes that have > 1 ticks remaining.",
		position = 4,
		keyName = "prayerBoxColor",
		section = SECTION_PRAYER
	)
	default Color prayerBoxColor()
	{
		return Color.ORANGE;
	}

	@Alpha
	@ConfigItem(
		name = "Box Warning Color",
		description = "Color of boxes that have 1 tick remaining.",
		position = 5,
		keyName = "prayerBoxWarnColor",
		section = SECTION_PRAYER
	)
	default Color prayerBoxWarnColor()
	{
		return Color.RED;
	}

	// Recolor Sarcophagus

	String SARCOPHAGUS_PREFIX = "sarcophagus";

	@ConfigItem(
		name = "Recolor White",
		description = "Recolor white sarcophagus.",
		position = 0,
		keyName = SARCOPHAGUS_PREFIX + "RecolorWhite",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default boolean sarcophagusRecolorWhite()
	{
		return false;
	}

	@ConfigItem(
		name = "Color",
		description = "Color to replace white sarcophagus with.",
		position = 1,
		keyName = SARCOPHAGUS_PREFIX + "WhiteRecolor",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default Color sarcophagusWhiteRecolor()
	{
		return new Color(237, 177, 23);
	}

	@ConfigItem(
		name = "Recolor Purple (Mine)",
		description = "Recolor purple sarcophagus." +
			"<br>When the sarcophagus is my loot.",
		position = 2,
		keyName = SARCOPHAGUS_PREFIX + "RecolorMyPurple",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default boolean sarcophagusRecolorMyPurple()
	{
		return false;
	}

	@ConfigItem(
		name = "Color",
		description = "Color to replace purple sarcophagus with." +
			"<br>When the sarcophagus is my loot.",
		position = 3,
		keyName = SARCOPHAGUS_PREFIX + "MyPurpleRecolor",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default Color sarcophagusMyPurpleRecolor()
	{
		return new Color(192, 20, 124);
	}

	@ConfigItem(
		name = "Recolor Purple (Other)",
		description = "Recolor purple sarcophagus." +
			"<br>When the sarcophagus is not my loot.",
		position = 4,
		keyName = SARCOPHAGUS_PREFIX + "RecolorOtherPurple",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default boolean sarcophagusRecolorOtherPurple()
	{
		return false;
	}

	@ConfigItem(
		name = "Color",
		description = "Color to replace purple sarcophagus with." +
			"<br>When the sarcophagus is not my loot.",
		position = 5,
		keyName = SARCOPHAGUS_PREFIX + "OtherPurpleRecolor",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default Color sarcophagusOtherPurpleRecolor()
	{
		return new Color(17, 88, 152);
	}

	@ConfigItem(
		name = "Recolor Loot",
		description = "Recolor purple sarcophagus based on loot.",
		position = 6,
		keyName = SARCOPHAGUS_PREFIX + "RecolorLoot",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default boolean sarcophagusRecolorLoot()
	{
		return false;
	}

	@ConfigItem(
		name = "Lightbearer",
		description = "Color to use when loot is a lightbearer.",
		position = 7,
		keyName = SARCOPHAGUS_PREFIX + "LightbearerColor",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default Color sarcophagusLightbearerColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
		name = "Elidinis' Ward",
		description = "Color to use when loot is Elidinis' Ward.",
		position = 8,
		keyName = SARCOPHAGUS_PREFIX + "ElidinisWardColor",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default Color sarcophagusElidinisWardColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
		name = "Osmumten's Fang",
		description = "Color to use when loot is Osmumten's Fang.",
		position = 9,
		keyName = SARCOPHAGUS_PREFIX + "OsmumtensFangColor",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default Color sarcophagusOsmumtensFangColor()
	{
		return new Color(255, 135, 0);
	}

	@ConfigItem(
		name = "Masori Mask",
		description = "Color to use when loot is a Masori Mask.",
		position = 10,
		keyName = SARCOPHAGUS_PREFIX + "MasoriMaskColor",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default Color sarcophagusMasoriMaskColor()
	{
		return new Color(255, 135, 0);
	}

	@ConfigItem(
		name = "Masori Chaps",
		description = "Color to use when loot is a Masori Chaps.",
		position = 11,
		keyName = SARCOPHAGUS_PREFIX + "MasoriChapsColor",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default Color sarcophagusMasoriChapsColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		name = "Masori Body",
		description = "Color to use when loot is a Masori Body.",
		position = 12,
		keyName = SARCOPHAGUS_PREFIX + "MasoriBodyColor",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default Color sarcophagusMasoriBodyColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		name = "Tumeken's Shadow",
		description = "Color to use when loot is Tumeken's Shadow.",
		position = 13,
		keyName = SARCOPHAGUS_PREFIX + "TumekensShadowColor",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default Color sarcophagusTumekensShadowColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		name = "Hide Loot",
		description = "Prevent rendering loot animation and chatbox text." +
			"<br>Only active when purple is mine.",
		position = 14,
		keyName = SARCOPHAGUS_PREFIX + "HideLoot",
		section = SECTION_RECOLOR_SARCOPHAGUS
	)
	default boolean sarcophagusHideLoot()
	{
		return false;
	}

	// Misc

	@ConfigItem(
		name = "Hide Fade Transition",
		description = "Hide the room fade transitions.",
		position = 0,
		keyName = "hideFadeTransition",
		section = SECTION_MISC
	)
	default boolean hideFadeTransition()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide HP Orbs",
		description = "Hide the HP orbs in the HUD.",
		position = 1,
		keyName = "hideHpOrbs",
		section = SECTION_MISC
	)
	default boolean hideHPOrbs()
	{
		return false;
	}

	@ConfigItem(
		name = "Menu Swaps",
		description = "Menu entry swaps for entering/exiting etc.",
		position = 2,
		keyName = "quickProceedSwaps",
		section = SECTION_MISC
	)
	default QuickProceed quickProceedSwaps()
	{
		return QuickProceed.OFF;
	}

	@ConfigItem(
		name = "Font Style",
		description = "Font style of most text overlays.",
		position = 3,
		keyName = "fontStyle",
		section = SECTION_MISC
	)
	default FontStyle fontStyle()
	{
		return FontStyle.BOLD;
	}

	@ConfigItem(
		name = "Font Size",
		description = "Font size of most text overlays.",
		position = 4,
		keyName = "fontSize",
		section = SECTION_MISC
	)
	@Units(Units.PIXELS)
	@Range(min = 12)
	default int fontSize()
	{
		return 12;
	}

	@Alpha
	@ConfigItem(
		name = "Danger Outline",
		description = "Color used to mark \"dangerous\" tiles.",
		position = 5,
		keyName = "dangerOutlineColor",
		section = SECTION_MISC
	)
	default Color dangerOutlineColor()
	{
		return Color.RED;
	}

	@Alpha
	@ConfigItem(
		name = "Danger Fill",
		description = "Color used to mark \"dangerous\" tiles.",
		position = 6,
		keyName = "dangerFillColor",
		section = SECTION_MISC
	)
	default Color dangerFillColor()
	{
		return new Color(255, 0, 0, 20);
	}

	@Alpha
	@ConfigItem(
		name = "Tile Outline",
		description = "Color used to outline npc tiles.",
		position = 7,
		keyName = "tileOutlineColor",
		section = SECTION_MISC
	)
	default Color tileOutlineColor()
	{
		return Color.BLACK;
	}

	@Alpha
	@ConfigItem(
		name = "Tile Fill",
		description = "Color used to fill npc tiles.",
		position = 8,
		keyName = "tileFillColor",
		section = SECTION_MISC
	)
	default Color tileFillColor()
	{
		return new Color(0, 0, 0, 10);
	}

	// Enums

	@AllArgsConstructor
	enum PrayerMode
	{
		WIDGET("Widget"),
		INFO_BOX("Infobox"),
		ALL("All"),
		OFF("Off");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@Getter
	@AllArgsConstructor
	enum FontStyle
	{
		BOLD("Bold", Font.BOLD),
		ITALIC("Italic", Font.ITALIC),
		PLAIN("Plain", Font.PLAIN);

		private final String name;
		private final int font;

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum FireballRadius
	{
		AERIAL,
		DEFAULT,
		OFF
	}

	enum Tile
	{
		TILE,
		TRUE_TILE,
		OFF
	}

	enum QuickProceed
	{
		ON,
		SPEEDRUN,
		OFF
	}

	enum AttackCounter
	{
		MEDIC,
		DEFAULT,
		OFF
	}
}

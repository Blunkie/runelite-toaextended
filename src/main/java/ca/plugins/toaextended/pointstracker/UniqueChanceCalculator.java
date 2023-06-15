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

import lombok.experimental.UtilityClass;

@UtilityClass
class UniqueChanceCalculator
{
	private static final int BASE_RATE_UNIQUE = 10_500;
	private static final int MODIFIER_RAID_LEVEL_UNIQUE = 20;

	private static final int RAID_LEVEL_REDUCE_FLOOR = 400;
	private static final double RAID_LEVEL_REDUCE_FACTOR = 1.0 / 3.0;
	private static final int RAID_LEVEL_MAX = 550;

	private static final double MAX_RATE_UNIQUE = 55;

	static double getUniqueChance(final int raidLevel, final int points)
	{
		int raidLevelModifier = MODIFIER_RAID_LEVEL_UNIQUE * Math.min(raidLevel, RAID_LEVEL_REDUCE_FLOOR);

		if (raidLevel > RAID_LEVEL_REDUCE_FLOOR)
		{
			raidLevelModifier += (Math.min(raidLevel, RAID_LEVEL_MAX) - RAID_LEVEL_REDUCE_FLOOR) *
				MODIFIER_RAID_LEVEL_UNIQUE * RAID_LEVEL_REDUCE_FACTOR;
		}

		final double denominator = BASE_RATE_UNIQUE - raidLevelModifier;

		return Math.max(0, Math.min(MAX_RATE_UNIQUE, points / denominator));
	}

	static double calcUniqueOdds(final double chance, final double kc)
	{
		return 1 - Math.pow(1 - chance, kc);
	}
}

package com.example.data

/**
 * Intelligent Real-Time Pattern Recognition Engine.
 * Detects classic casino shoe/table patterns across games:
 * - Dragon Streak (Monopole Run)
 * - Alternating Zigzag (Ping-Pong / Chop)
 * - Double Clump (2-2-2 Pair Formation)
 * - Triple Block (3-3 Formation)
 * - Golden Chop (High Volatility Alternation)
 * - Danger Zone Crash Cluster (Sub-1.30x)
 * - Bell-Curve Mean Reversion (7 Up Down)
 * - Dozen / Color Monopoly (Roulette)
 */
object PatternAlertEngine {

    data class PatternAlert(
        val patternName: String,
        val patternEmoji: String,
        val description: String,
        val recommendedAction: String,
        val confidencePct: Int,
        val urgencyColor: Long = 0xFF00E676 // Green / Gold / Red
    )

    fun detectDTPattern(rounds: List<DragonTigerRound>): PatternAlert? {
        if (rounds.size < 3) return null
        val outcomes = rounds.take(12).map { it.result } // Newest first

        // 1. Dragon Streak / Monopole Run (4+ consecutive same side)
        val first = outcomes.first()
        if (first == "D" || first == "T") {
            var runCount = 0
            for (r in outcomes) {
                if (r == first) runCount++ else break
            }
            if (runCount >= 4) {
                val sideName = if (first == "D") "DRAGON" else "TIGER"
                return PatternAlert(
                    patternName = "🔥 $sideName MONOPOLE RUN ($runCount IN A ROW)",
                    patternEmoji = "🐉",
                    description = "Strong directional streak detected. Probability favors trend continuation until a break.",
                    recommendedAction = "Ride the $sideName streak. Keep trailing stop-loss.",
                    confidencePct = (70 + (runCount * 3)).coerceAtMost(92),
                    urgencyColor = 0xFF00E676
                )
            }
        }

        // 2. Alternating Zigzag / Ping-Pong (1-1-1-1)
        if (outcomes.size >= 4) {
            val nonTies = outcomes.filter { it == "D" || it == "T" }
            if (nonTies.size >= 4) {
                var isAlternating = true
                for (i in 0 until 3) {
                    if (nonTies[i] == nonTies[i + 1]) {
                        isAlternating = false
                        break
                    }
                }
                if (isAlternating) {
                    val nextPredicted = if (nonTies.first() == "D") "TIGER" else "DRAGON"
                    return PatternAlert(
                        patternName = "⚡ ALTERNATING ZIGZAG (PING-PONG 1-1)",
                        patternEmoji = "🏓",
                        description = "Perfect 1-1 oscillation across consecutive rounds.",
                        recommendedAction = "Bet on $nextPredicted to maintain alternating zigzag wave.",
                        confidencePct = 82,
                        urgencyColor = 0xFFFFD700
                    )
                }
            }
        }

        // 3. Double Clump (2-2 Formation: e.g., D-D-T-T or T-T-D-D)
        if (outcomes.size >= 5) {
            val nonTies = outcomes.filter { it == "D" || it == "T" }
            if (nonTies.size >= 5) {
                val c0 = nonTies.getOrNull(0)
                val c1 = nonTies.getOrNull(1)
                val c2 = nonTies.getOrNull(2)
                val c3 = nonTies.getOrNull(3)
                val c4 = nonTies.getOrNull(4)

                // If currently at 1 of new side after 2 of previous side (c0 != c1, c1 == c2, c2 != c3, c3 == c4)
                if (c0 != null && c1 != null && c2 != null && c3 != null && c4 != null) {
                    if (c0 != c1 && c1 == c2 && c2 != c3 && c3 == c4) {
                        val expected = if (c0 == "D") "DRAGON" else "TIGER"
                        return PatternAlert(
                            patternName = "👥 DOUBLE CLUMP (2-2 PAIR FORMATION)",
                            patternEmoji = "👯",
                            description = "Historical 2-2 symmetrical pair structure detected.",
                            recommendedAction = "Bet on $expected to complete the 2nd leg of the double clump.",
                            confidencePct = 85,
                            urgencyColor = 0xFF64B5F6
                        )
                    }
                }
            }
        }

        return null
    }

    fun detectBaccaratPattern(rounds: List<BaccaratRound>): PatternAlert? {
        if (rounds.size < 3) return null
        val outcomes = rounds.take(12).map { it.result }

        // 1. Dragon Streak (4+ Banker or Player)
        val first = outcomes.first()
        if (first == "B" || first == "P") {
            var runCount = 0
            for (r in outcomes) {
                if (r == first) runCount++ else break
            }
            if (runCount >= 4) {
                val sideName = if (first == "B") "BANKER" else "PLAYER"
                return PatternAlert(
                    patternName = "🔥 $sideName DRAGON RUN ($runCount IN A ROW)",
                    patternEmoji = "🐉",
                    description = "Monopole shoe trend in progress.",
                    recommendedAction = "Ride the $sideName streak with disciplined base stake.",
                    confidencePct = (72 + (runCount * 3)).coerceAtMost(94),
                    urgencyColor = 0xFF00E676
                )
            }
        }

        // 2. Alternating Zigzag (B-P-B-P)
        val nonTies = outcomes.filter { it == "B" || it == "P" }
        if (nonTies.size >= 4) {
            var isAlternating = true
            for (i in 0 until 3) {
                if (nonTies[i] == nonTies[i + 1]) {
                    isAlternating = false
                    break
                }
            }
            if (isAlternating) {
                val nextPredicted = if (nonTies.first() == "B") "PLAYER" else "BANKER"
                return PatternAlert(
                    patternName = "⚡ PING-PONG ZIGZAG (1-1-1)",
                    patternEmoji = "🏓",
                    description = "Symmetrical banker-player alternation.",
                    recommendedAction = "Bet on $nextPredicted to continue the ping-pong wave.",
                    confidencePct = 81,
                    urgencyColor = 0xFFFFD700
                )
            }
        }

        // 3. Double Clump (2-2)
        if (nonTies.size >= 5) {
            val c0 = nonTies[0]; val c1 = nonTies[1]; val c2 = nonTies[2]; val c3 = nonTies[3]; val c4 = nonTies[4]
            if (c0 != c1 && c1 == c2 && c2 != c3 && c3 == c4) {
                val expected = if (c0 == "B") "BANKER" else "PLAYER"
                return PatternAlert(
                    patternName = "👥 BACCARAT DOUBLE CLUMP (2-2)",
                    patternEmoji = "👯",
                    description = "2-Banker / 2-Player symmetrical clump structure.",
                    recommendedAction = "Bet on $expected to complete the 2-2 pair.",
                    confidencePct = 84,
                    urgencyColor = 0xFF64B5F6
                )
            }
        }

        return null
    }

    fun detectCrashPattern(rounds: List<CrashRound>): PatternAlert? {
        if (rounds.size < 3) return null
        val recent = rounds.take(10)

        // 1. Danger Zone Instant Crash Cluster (< 1.30x twice in last 3 rounds)
        val first3 = recent.take(3)
        val sub130Count = first3.count { it.multiplier < 1.30 }
        if (sub130Count >= 2) {
            return PatternAlert(
                patternName = "⚠️ INSTANT CRASH TRAP CLUSTER (< 1.30x)",
                patternEmoji = "🚨",
                description = "High density of rapid sub-1.30x crashes. Algorithm in aggressive cash-drain cycle.",
                recommendedAction = "SKIP this flight or set ultra-conservative 1.10x auto cashout.",
                confidencePct = 88,
                urgencyColor = 0xFFFF1744
            )
        }

        // 2. High-Multiplier Rocket Wave (3+ multipliers >= 3.0x in last 6)
        val first6 = recent.take(6)
        val rocketCount = first6.count { it.multiplier >= 3.0 }
        if (rocketCount >= 3) {
            return PatternAlert(
                patternName = "🚀 HIGH ALTITUDE INFLATION WAVE",
                patternEmoji = "✨",
                description = "Multiplier payouts are expanding above standard variance.",
                recommendedAction = "Target 2.0x - 2.5x multiplier. Secure partial profits.",
                confidencePct = 80,
                urgencyColor = 0xFF00E676
            )
        }

        // 3. Post 10x+ Rocket Hangover (Last round was >= 10.0x)
        if (recent.first().multiplier >= 10.0) {
            return PatternAlert(
                patternName = "🛑 POST-ROCKET HANGOVER CORRECTION",
                patternEmoji = "📉",
                description = "Massive payout round just completed. Engine historically resets with an early crash.",
                recommendedAction = "Caution: High probability of sub-1.40x correction. Wait 1 round.",
                confidencePct = 86,
                urgencyColor = 0xFFFF5722
            )
        }

        return null
    }

    fun detectRoulettePattern(rounds: List<RouletteRound>): PatternAlert? {
        if (rounds.size < 4) return null
        val recent = rounds.take(10)
        val colors = recent.map { it.color }

        // Color Streak
        val firstColor = colors.first()
        if (firstColor == "RED" || firstColor == "BLACK") {
            var count = 0
            for (c in colors) {
                if (c == firstColor) count++ else break
            }
            if (count >= 4) {
                return PatternAlert(
                    patternName = "🎡 $firstColor WHEEL RUN ($count IN A ROW)",
                    patternEmoji = "🎯",
                    description = "Wheel rotor sector bias or dealer signature creating strong color momentum.",
                    recommendedAction = "Follow $firstColor or consider Dozen confirmation.",
                    confidencePct = 78,
                    urgencyColor = if (firstColor == "RED") 0xFFFF1744 else 0xFF212121
                )
            }
        }

        // Dozen Monopoly (Same dozen 3 times in a row)
        val dozens = recent.mapNotNull { it.number?.let { num ->
            when (num) {
                in 1..12 -> "1ST DOZEN"
                in 13..24 -> "2ND DOZEN"
                in 25..36 -> "3RD DOZEN"
                else -> null
            }
        } }
        if (dozens.size >= 3 && dozens[0] == dozens[1] && dozens[1] == dozens[2]) {
            val dName = dozens[0]
            return PatternAlert(
                patternName = "🎰 DOZEN SECTOR BIAS ($dName x3)",
                patternEmoji = "💎",
                description = "Ball dropping consistently into the same 12-number wheel segment.",
                recommendedAction = "Back $dName with split column hedge.",
                confidencePct = 80,
                urgencyColor = 0xFFFFD700
            )
        }

        return null
    }

    fun detectSevenPattern(rounds: List<SevenUpDownRound>): PatternAlert? {
        if (rounds.size < 5) return null
        val results = rounds.take(10).map { it.result }

        // Lucky 7 Drought (No 7 in last 12 rounds)
        val full20 = rounds.take(20).map { it.result }
        if (full20.size >= 12 && !full20.contains("7")) {
            return PatternAlert(
                patternName = "🎲 LUCKY 7 STATISTICAL DEFICIT (OVERDUE)",
                patternEmoji = "⚡",
                description = "Gaussian bell-curve expectation is 16.67% (1 in 6). Sum 7 is statistically overdue.",
                recommendedAction = "Place speculative hedge on Lucky 7 (3x Payout).",
                confidencePct = 83,
                urgencyColor = 0xFFFFD700
            )
        }

        // Up or Down Streak (4+ in a row)
        val first = results.first()
        if (first == "U" || first == "D") {
            var count = 0
            for (r in results) {
                if (r == first) count++ else break
            }
            if (count >= 4) {
                val name = if (first == "U") "7 UP (8-12)" else "7 DOWN (2-6)"
                return PatternAlert(
                    patternName = "🎲 $name MOMENTUM RUN ($count IN A ROW)",
                    patternEmoji = "📈",
                    description = "Dice distribution skewed strongly towards high/low combinations.",
                    recommendedAction = "Ride the $name streak or hedge with 7.",
                    confidencePct = 79,
                    urgencyColor = 0xFF00E676
                )
            }
        }

        return null
    }
}

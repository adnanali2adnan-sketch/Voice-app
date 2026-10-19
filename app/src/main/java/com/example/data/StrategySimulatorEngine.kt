package com.example.data

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Offline Monte-Carlo Statistical Strategy Simulator Engine.
 * Simulates 100 to 1000 rounds across games and staking strategies to test
 * mathematical expectancy, bankroll drawdown, risk-of-ruin, and equity curves.
 */
object StrategySimulatorEngine {

    enum class SimulationGame(val label: String, val defaultMultiplier: Double, val winProbability: Double) {
        DRAGON_TIGER("Dragon Tiger", 2.0, 0.463), // ~46.3% win, 46.3% loss, ~7.4% tie (half loss / push)
        BACCARAT_BANKER("Baccarat (Banker)", 1.95, 0.458), // 0.95:1 payout (5% comm)
        BACCARAT_PLAYER("Baccarat (Player)", 2.0, 0.446), // 1:1 payout
        ROULETTE_COLOR("Roulette (Red/Black)", 2.0, 0.486), // 18/37 = 48.65%
        ROULETTE_DOZEN("Roulette (1st/2nd/3rd Dozen)", 3.0, 0.324), // 12/37 = 32.4%
        SEVEN_UP_DOWN("7 Up Down (Up/Down)", 2.0, 0.416), // 15/36 = 41.67%
        CRASH_SAFE("Crash / Aviator (1.30x Safe)", 1.30, 0.746), // 97% / 1.30 = 74.6%
        CRASH_MEDIUM("Crash / Aviator (2.00x Target)", 2.00, 0.485) // 97% / 2.00 = 48.5%
    }

    enum class StakingStrategy(val label: String, val description: String) {
        MARTINGALE("Martingale (2x Multiplier)", "Double bet after every loss. Reset to base stake on any win."),
        FIBONACCI("Fibonacci Sequence (1,1,2,3,5,8...)", "Move 1 step forward in sequence on loss; move 2 steps backward on win."),
        FLAT_BETTING("Flat Fixed Betting", "Place exactly the same base stake on every single round."),
        REVERSE_MARTINGALE("Reverse Martingale (Paroli)", "Double stake after each win up to 3 consecutive wins, reset on loss."),
        DALEMBERT("D'Alembert (+1 / -1 Unit)", "Increase bet by 1 base unit after loss, decrease by 1 unit after win.")
    }

    data class SimulationResult(
        val game: SimulationGame,
        val strategy: StakingStrategy,
        val totalRounds: Int,
        val startingBankroll: Double,
        val finalBankroll: Double,
        val netProfitLoss: Double,
        val roiPct: Double,
        val totalWins: Int,
        val totalLosses: Int,
        val winRatePct: Double,
        val maxDrawdownAmount: Double,
        val maxDrawdownPct: Double,
        val maxBetAmount: Double,
        val longestWinStreak: Int,
        val longestLossStreak: Int,
        val isBusted: Boolean,
        val bustRoundNumber: Int?,
        val balanceTrajectory: List<Pair<Int, Double>>, // (Round, Balance)
        val roundLogs: List<String>
    )

    private val FIBONACCI_SERIES = listOf(1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597)

    fun runSimulation(
        game: SimulationGame,
        strategy: StakingStrategy,
        roundsCount: Int = 250,
        startingBankroll: Double = 10000.0,
        baseStake: Double = 100.0
    ): SimulationResult {
        var currentBalance = startingBankroll
        var peakBalance = startingBankroll
        var maxDrawdown = 0.0

        var currentStake = baseStake
        var fiboIndex = 0
        var paroliWinStreak = 0

        var totalWins = 0
        var totalLosses = 0
        var currentWinStreak = 0
        var maxWinStreak = 0
        var currentLossStreak = 0
        var maxLossStreak = 0
        var maxBetEncountered = baseStake

        var isBusted = false
        var bustRound: Int? = null

        val trajectory = mutableListOf<Pair<Int, Double>>()
        trajectory.add(0 to startingBankroll)

        val logs = mutableListOf<String>()

        val random = Random(System.currentTimeMillis())

        for (r in 1..roundsCount) {
            if (currentBalance < currentStake || currentBalance <= 0) {
                isBusted = true
                bustRound = r
                logs.add("Round $r: 💀 BANKROLL BUSTED! Balance insufficient for next stake (PKR ${String.format("%.2f", currentStake)})")
                break
            }

            maxBetEncountered = max(maxBetEncountered, currentStake)
            val betAmount = currentStake

            // Deduct stake
            currentBalance -= betAmount

            // Simulate round outcome
            val roll = random.nextDouble() // 0.0 to 1.0
            val isWin = roll < game.winProbability

            if (isWin) {
                val payout = betAmount * game.defaultMultiplier
                val netWin = payout - betAmount
                currentBalance += payout

                totalWins++
                currentWinStreak++
                currentLossStreak = 0
                maxWinStreak = max(maxWinStreak, currentWinStreak)

                if (logs.size < 15) {
                    logs.add("Round $r: 🎯 WIN (+PKR ${String.format("%.0f", netWin)}) | Stake: PKR ${String.format("%.0f", betAmount)} | Bal: PKR ${String.format("%.0f", currentBalance)}")
                }

                // Strategy updates on WIN
                when (strategy) {
                    StakingStrategy.MARTINGALE -> {
                        currentStake = baseStake
                    }
                    StakingStrategy.FIBONACCI -> {
                        fiboIndex = max(0, fiboIndex - 2)
                        val multiplier = FIBONACCI_SERIES.getOrElse(fiboIndex) { 1 }
                        currentStake = baseStake * multiplier
                    }
                    StakingStrategy.FLAT_BETTING -> {
                        currentStake = baseStake
                    }
                    StakingStrategy.REVERSE_MARTINGALE -> {
                        paroliWinStreak++
                        if (paroliWinStreak >= 3) {
                            currentStake = baseStake // Lock in 3-win cycle
                            paroliWinStreak = 0
                        } else {
                            currentStake = betAmount * 2
                        }
                    }
                    StakingStrategy.DALEMBERT -> {
                        currentStake = max(baseStake, currentStake - baseStake)
                    }
                }
            } else {
                totalLosses++
                currentLossStreak++
                currentWinStreak = 0
                maxLossStreak = max(maxLossStreak, currentLossStreak)

                if (logs.size < 15) {
                    logs.add("Round $r: ❌ LOSS (-PKR ${String.format("%.0f", betAmount)}) | Next Stake Adj | Bal: PKR ${String.format("%.0f", currentBalance)}")
                }

                // Strategy updates on LOSS
                when (strategy) {
                    StakingStrategy.MARTINGALE -> {
                        currentStake = betAmount * 2
                    }
                    StakingStrategy.FIBONACCI -> {
                        fiboIndex = min(FIBONACCI_SERIES.size - 1, fiboIndex + 1)
                        val multiplier = FIBONACCI_SERIES.getOrElse(fiboIndex) { 1 }
                        currentStake = baseStake * multiplier
                    }
                    StakingStrategy.FLAT_BETTING -> {
                        currentStake = baseStake
                    }
                    StakingStrategy.REVERSE_MARTINGALE -> {
                        currentStake = baseStake
                        paroliWinStreak = 0
                    }
                    StakingStrategy.DALEMBERT -> {
                        currentStake = currentStake + baseStake
                    }
                }
            }

            peakBalance = max(peakBalance, currentBalance)
            val currentDrawdown = peakBalance - currentBalance
            maxDrawdown = max(maxDrawdown, currentDrawdown)

            trajectory.add(r to currentBalance)
        }

        val netProfitLoss = currentBalance - startingBankroll
        val roi = if (startingBankroll > 0) (netProfitLoss / startingBankroll) * 100.0 else 0.0
        val totalFinishedRounds = totalWins + totalLosses
        val winRate = if (totalFinishedRounds > 0) (totalWins.toDouble() / totalFinishedRounds) * 100.0 else 0.0
        val maxDrawdownPct = if (peakBalance > 0) (maxDrawdown / peakBalance) * 100.0 else 0.0

        return SimulationResult(
            game = game,
            strategy = strategy,
            totalRounds = totalFinishedRounds,
            startingBankroll = startingBankroll,
            finalBankroll = currentBalance,
            netProfitLoss = netProfitLoss,
            roiPct = roi,
            totalWins = totalWins,
            totalLosses = totalLosses,
            winRatePct = winRate,
            maxDrawdownAmount = maxDrawdown,
            maxDrawdownPct = maxDrawdownPct,
            maxBetAmount = maxBetEncountered,
            longestWinStreak = maxWinStreak,
            longestLossStreak = maxLossStreak,
            isBusted = isBusted,
            bustRoundNumber = bustRound,
            balanceTrajectory = trajectory,
            roundLogs = logs
        )
    }
}

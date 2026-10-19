package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat

enum class StakingStrategy(val displayName: String, val description: String) {
    MARTINGALE_2X("Martingale (2x)", "Double bet after every loss. Resets to base on win."),
    CONSERVATIVE_1_5X("Conservative (1.5x)", "1.5x multiplier on loss for lower risk recovery."),
    AGGRESSIVE_3X("Aggressive (3x)", "3x multiplier on loss for quick high-yield recovery."),
    DALEMBERT("D'Alembert (+1 / -1)", "Increase 1 unit on loss, decrease 1 unit on win."),
    FIBONACCI("Fibonacci Sequence", "Progress along 1, 1, 2, 3, 5, 8, 13 sequence on loss."),
    FLAT("Flat Staking (1x)", "Always wager constant base bet. Safe for long sessions.")
}

data class BankrollState(
    val baseBet: Double = 50.0,
    val startingBankroll: Double = 1000.0,
    val currentBankroll: Double = 1000.0,
    val targetProfit: Double = 500.0,
    val stopLossLimit: Double = 300.0,
    val strategy: StakingStrategy = StakingStrategy.MARTINGALE_2X,
    val currentLossStreak: Int = 0,
    val currentStep: Int = 1,
    val recommendedStake: Double = 50.0,
    val netProfitLoss: Double = 0.0,
    val isTargetProfitTriggered: Boolean = false,
    val isStopLossTriggered: Boolean = false
) {
    val recommendedBetStake: Double get() = recommendedStake
}

object BankrollEngine {
    private const val PREFS_NAME = "BankrollEnginePrefs"
    private val fibonacciSeq = listOf(1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89)

    private val _state = MutableStateFlow(BankrollState())
    val state: StateFlow<BankrollState> = _state.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baseBet = prefs.getFloat("baseBet", 50f).toDouble()
        val starting = prefs.getFloat("startingBankroll", 1000f).toDouble()
        val target = prefs.getFloat("targetProfit", 500f).toDouble()
        val stopLoss = prefs.getFloat("stopLossLimit", 300f).toDouble()
        val stratName = prefs.getString("strategy", StakingStrategy.MARTINGALE_2X.name) ?: StakingStrategy.MARTINGALE_2X.name
        val strat = try { StakingStrategy.valueOf(stratName) } catch (e: Exception) { StakingStrategy.MARTINGALE_2X }

        _state.value = _state.value.copy(
            baseBet = baseBet,
            startingBankroll = starting,
            currentBankroll = starting,
            targetProfit = target,
            stopLossLimit = stopLoss,
            strategy = strat,
            recommendedStake = baseBet
        )
        recalculateStake()
    }

    fun setBaseBet(context: Context, amount: Double) {
        val safeAmt = amount.coerceAtLeast(1.0)
        _state.value = _state.value.copy(baseBet = safeAmt)
        save(context)
        recalculateStake()
    }

    fun setTargetProfit(context: Context, amount: Double) {
        val safeAmt = amount.coerceAtLeast(10.0)
        _state.value = _state.value.copy(targetProfit = safeAmt, isTargetProfitTriggered = false)
        save(context)
        checkSafetyLimits()
    }

    fun setStopLossLimit(context: Context, amount: Double) {
        val safeAmt = amount.coerceAtLeast(10.0)
        _state.value = _state.value.copy(stopLossLimit = safeAmt, isStopLossTriggered = false)
        save(context)
        checkSafetyLimits()
    }

    fun setStrategy(context: Context, strategy: StakingStrategy) {
        _state.value = _state.value.copy(strategy = strategy)
        save(context)
        recalculateStake()
    }

    fun setStartingBankroll(context: Context, amount: Double) {
        val safeAmt = amount.coerceAtLeast(10.0)
        _state.value = _state.value.copy(
            startingBankroll = safeAmt,
            currentBankroll = safeAmt,
            netProfitLoss = 0.0,
            currentLossStreak = 0,
            currentStep = 1,
            isTargetProfitTriggered = false,
            isStopLossTriggered = false
        )
        save(context)
        recalculateStake()
    }

    /**
     * Record round outcome to auto-step the bankroll strategy
     */
    fun recordOutcome(isWin: Boolean?, profitLossDelta: Double? = null) {
        val current = _state.value
        val stake = current.recommendedStake

        val newStreak = when (isWin) {
            true -> 0
            false -> current.currentLossStreak + 1
            null -> current.currentLossStreak
        }

        val step = when (current.strategy) {
            StakingStrategy.MARTINGALE_2X,
            StakingStrategy.CONSERVATIVE_1_5X,
            StakingStrategy.AGGRESSIVE_3X -> {
                if (isWin == true) 1 else current.currentStep + 1
            }
            StakingStrategy.DALEMBERT -> {
                if (isWin == true) (current.currentStep - 1).coerceAtLeast(1)
                else current.currentStep + 1
            }
            StakingStrategy.FIBONACCI -> {
                if (isWin == true) (current.currentStep - 2).coerceAtLeast(1)
                else current.currentStep + 1
            }
            StakingStrategy.FLAT -> 1
        }

        val delta = profitLossDelta ?: if (isWin == true) stake else if (isWin == false) -stake else 0.0
        val newNetPL = current.netProfitLoss + delta
        val newCurrentBankroll = current.startingBankroll + newNetPL

        _state.value = current.copy(
            currentLossStreak = newStreak,
            currentStep = step,
            netProfitLoss = newNetPL,
            currentBankroll = newCurrentBankroll
        )
        recalculateStake()
        checkSafetyLimits()
    }

    fun resetSession() {
        _state.value = _state.value.copy(
            currentLossStreak = 0,
            currentStep = 1,
            netProfitLoss = 0.0,
            currentBankroll = _state.value.startingBankroll,
            isTargetProfitTriggered = false,
            isStopLossTriggered = false
        )
        recalculateStake()
    }

    fun dismissAlarm() {
        _state.value = _state.value.copy(
            isTargetProfitTriggered = false,
            isStopLossTriggered = false
        )
    }

    private fun checkSafetyLimits() {
        val current = _state.value
        val isTargetReached = current.netProfitLoss >= current.targetProfit
        val isStopLossHit = current.netProfitLoss <= -current.stopLossLimit

        if (isTargetReached && !current.isTargetProfitTriggered) {
            _state.value = _state.value.copy(isTargetProfitTriggered = true)
        }
        if (isStopLossHit && !current.isStopLossTriggered) {
            _state.value = _state.value.copy(isStopLossTriggered = true)
        }
    }

    private fun recalculateStake() {
        val current = _state.value
        val base = current.baseBet
        val streak = current.currentLossStreak

        val newStake = when (current.strategy) {
            StakingStrategy.MARTINGALE_2X -> {
                var s = base
                for (i in 0 until streak) {
                    s *= 2.0
                }
                s
            }
            StakingStrategy.CONSERVATIVE_1_5X -> {
                var s = base
                for (i in 0 until streak) {
                    s *= 1.5
                }
                s
            }
            StakingStrategy.AGGRESSIVE_3X -> {
                var s = base
                for (i in 0 until streak) {
                    s *= 3.0
                }
                s
            }
            StakingStrategy.DALEMBERT -> {
                base * current.currentStep.coerceAtLeast(1)
            }
            StakingStrategy.FIBONACCI -> {
                val idx = (current.currentStep - 1).coerceIn(0, fibonacciSeq.lastIndex)
                base * fibonacciSeq[idx]
            }
            StakingStrategy.FLAT -> base
        }

        val roundedStake = Math.round(newStake * 10.0) / 10.0
        _state.value = _state.value.copy(recommendedStake = roundedStake)
    }

    private fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val s = _state.value
        prefs.edit()
            .putFloat("baseBet", s.baseBet.toFloat())
            .putFloat("startingBankroll", s.startingBankroll.toFloat())
            .putFloat("targetProfit", s.targetProfit.toFloat())
            .putFloat("stopLossLimit", s.stopLossLimit.toFloat())
            .putString("strategy", s.strategy.name)
            .apply()
    }
}

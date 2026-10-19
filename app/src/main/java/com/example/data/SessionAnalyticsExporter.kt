package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Session Analytics & Multi-Format Exporter Utility.
 * Exports comprehensive session audits, PnL breakdowns, and CSV data
 * to WhatsApp, Telegram, Email, Notes, or Clipboard.
 */
object SessionAnalyticsExporter {

    data class SessionOverview(
        val totalRounds: Int,
        val totalWins: Int,
        val totalLosses: Int,
        val winRatePct: Double,
        val totalProfitLoss: Double,
        val dtRoundsCount: Int,
        val baccaratRoundsCount: Int,
        val crashRoundsCount: Int,
        val rouletteRoundsCount: Int,
        val sevenRoundsCount: Int,
        val abRoundsCount: Int,
        val bestPerformingGame: String,
        val formattedTimestamp: String
    )

    fun calculateOverview(
        dtRounds: List<DragonTigerRound>,
        baccaratRounds: List<BaccaratRound>,
        crashRounds: List<CrashRound>,
        rouletteRounds: List<RouletteRound>,
        sevenRounds: List<SevenUpDownRound>,
        abRounds: List<AndarBaharRound>,
        bankrollState: BankrollState
    ): SessionOverview {
        val totalRounds = dtRounds.size + baccaratRounds.size + crashRounds.size + rouletteRounds.size + sevenRounds.size + abRounds.size
        val totalWins = (dtRounds.count { it.isWin == true } +
                baccaratRounds.count { it.isWin == true } +
                crashRounds.count { it.isWin == true } +
                rouletteRounds.count { it.isWin == true } +
                sevenRounds.count { it.isWin == true } +
                abRounds.count { it.isWin == true })

        val totalLosses = (dtRounds.count { it.isWin == false } +
                baccaratRounds.count { it.isWin == false } +
                crashRounds.count { it.isWin == false } +
                rouletteRounds.count { it.isWin == false } +
                sevenRounds.count { it.isWin == false } +
                abRounds.count { it.isWin == false })

        val finishedRounds = totalWins + totalLosses
        val winRate = if (finishedRounds > 0) (totalWins.toDouble() / finishedRounds) * 100.0 else 0.0

        val gameCounts = mapOf(
            "Dragon Tiger" to dtRounds.size,
            "Baccarat" to baccaratRounds.size,
            "Aviator / Crash" to crashRounds.size,
            "Roulette" to rouletteRounds.size,
            "7 Up Down" to sevenRounds.size,
            "Andar Bahar" to abRounds.size
        )
        val bestGame = gameCounts.maxByOrNull { it.value }?.key ?: "None"

        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        return SessionOverview(
            totalRounds = totalRounds,
            totalWins = totalWins,
            totalLosses = totalLosses,
            winRatePct = winRate,
            totalProfitLoss = bankrollState.netProfitLoss,
            dtRoundsCount = dtRounds.size,
            baccaratRoundsCount = baccaratRounds.size,
            crashRoundsCount = crashRounds.size,
            rouletteRoundsCount = rouletteRounds.size,
            sevenRoundsCount = sevenRounds.size,
            abRoundsCount = abRounds.size,
            bestPerformingGame = bestGame,
            formattedTimestamp = sdf.format(Date())
        )
    }

    fun generateTextSummary(overview: SessionOverview, bankrollState: BankrollState): String {
        return buildString {
            appendLine("══════════════════════════════════════")
            appendLine("  🎰 CASINO COMPANION SESSION REPORT")
            appendLine("══════════════════════════════════════")
            appendLine("📅 Date: ${overview.formattedTimestamp}")
            appendLine("🎯 Total Rounds Logged: ${overview.totalRounds}")
            appendLine("🏆 Win / Loss Record: ${overview.totalWins}W - ${overview.totalLosses}L")
            appendLine("📊 Session Win Rate: ${String.format("%.1f", overview.winRatePct)}%")
            appendLine("💰 Total Net PnL: ${if (overview.totalProfitLoss >= 0) "+" else ""}PKR ${String.format("%.2f", overview.totalProfitLoss)}")
            appendLine("🏦 Staking Strategy: ${bankrollState.strategy.displayName}")
            appendLine("💵 Base Bet Unit: PKR ${String.format("%.2f", bankrollState.baseBet)}")
            appendLine("--------------------------------------")
            appendLine("🎮 Game Breakdown:")
            if (overview.dtRoundsCount > 0) appendLine("  • Dragon Tiger: ${overview.dtRoundsCount} rounds")
            if (overview.baccaratRoundsCount > 0) appendLine("  • Baccarat: ${overview.baccaratRoundsCount} rounds")
            if (overview.crashRoundsCount > 0) appendLine("  • Aviator / Crash: ${overview.crashRoundsCount} rounds")
            if (overview.rouletteRoundsCount > 0) appendLine("  • Roulette: ${overview.rouletteRoundsCount} rounds")
            if (overview.sevenRoundsCount > 0) appendLine("  • 7 Up Down: ${overview.sevenRoundsCount} rounds")
            if (overview.abRoundsCount > 0) appendLine("  • Andar Bahar: ${overview.abRoundsCount} rounds")
            appendLine("--------------------------------------")
            appendLine("🛡️ Bankroll Discipline:")
            appendLine("  • Consecutive Losses: ${bankrollState.currentLossStreak}")
            appendLine("  • Target Profit Target: PKR ${String.format("%.2f", bankrollState.targetProfit)}")
            appendLine("  • Stop Loss Limit: PKR ${String.format("%.2f", bankrollState.stopLossLimit)}")
            appendLine("══════════════════════════════════════")
            appendLine("Generated by Casino AI Companion Pro")
        }
    }

    fun generateCSV(
        dtRounds: List<DragonTigerRound>,
        baccaratRounds: List<BaccaratRound>,
        crashRounds: List<CrashRound>,
        rouletteRounds: List<RouletteRound>,
        sevenRounds: List<SevenUpDownRound>,
        abRounds: List<AndarBaharRound>
    ): String {
        return buildString {
            appendLine("Timestamp,GameType,RoundResult,WinLossOutcome,Notes")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val nowStr = sdf.format(Date())

            dtRounds.forEach { r ->
                appendLine("$nowStr,DragonTiger,${r.result},${if (r.isWin == true) "WIN" else if (r.isWin == false) "LOSS" else "PENDING"},DTRound")
            }
            baccaratRounds.forEach { r ->
                appendLine("$nowStr,Baccarat,${r.result},${if (r.isWin == true) "WIN" else if (r.isWin == false) "LOSS" else "PENDING"},BaccaratRound")
            }
            crashRounds.forEach { r ->
                appendLine("$nowStr,AviatorCrash,${String.format("%.2f", r.multiplier)}x,${if (r.isWin == true) "WIN" else if (r.isWin == false) "LOSS" else "PENDING"},Cashout:${r.cashOutMultiplier}x")
            }
            rouletteRounds.forEach { r ->
                appendLine("$nowStr,Roulette,${r.number ?: r.color},${if (r.isWin == true) "WIN" else if (r.isWin == false) "LOSS" else "PENDING"},Color:${r.color}")
            }
            sevenRounds.forEach { r ->
                appendLine("$nowStr,SevenUpDown,${r.result},${if (r.isWin == true) "WIN" else if (r.isWin == false) "LOSS" else "PENDING"},SevenRound")
            }
            abRounds.forEach { r ->
                appendLine("$nowStr,AndarBahar,${r.result},${if (r.isWin == true) "WIN" else if (r.isWin == false) "LOSS" else "PENDING"},ABRound")
            }
        }
    }

    fun copyToClipboard(context: Context, csvData: String) {
        copyCsvToClipboard(context, csvData)
    }

    fun shareReport(context: Context, textReport: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Casino Companion Session Audit Report")
                putExtra(Intent.EXTRA_TEXT, textReport)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, "Share Session Report via")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch share sheet: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyCsvToClipboard(context: Context, csvData: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Casino Companion CSV", csvData)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "✅ CSV Audit Data Copied to Clipboard!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error copying to clipboard: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

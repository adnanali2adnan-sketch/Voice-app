package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 6-Deck and 8-Deck Shoe Card Counter & Bias Tracker for Dragon Tiger & Baccarat.
 * Standard Casino Shoes:
 * 6 Decks = 312 cards
 * 8 Decks = 416 cards
 *
 * Card Classification (Card Counting for Baccarat & Dragon Tiger):
 * - Low Cards (A, 2, 3, 4, 5, 6): Valued as Low. Count weight: -1.
 * - Neutral Cards (7, 8): Valued as Neutral. Count weight: 0.
 * - High Cards (9, 10, J, Q, K): Valued as High. Count weight: +1.
 */
object ShoeDeckTracker {

    data class ShoeState(
        val totalDecks: Int = 8,                 // 6 or 8
        val totalCardsInShoe: Int = 416,         // 6*52 = 312 or 8*52 = 416
        val cardsDealt: Int = 0,
        val lowCardsCount: Int = 0,             // A to 6
        val neutralCardsCount: Int = 0,         // 7 & 8
        val highCardsCount: Int = 0,            // 9, 10, J, Q, K
        val runningCount: Int = 0,
        val trueCount: Double = 0.0,
        val lowCardPct: Int = 0,
        val highCardPct: Int = 0,
        val neutralCardPct: Int = 0,
        val deckBias: String = "BALANCED SHOE", // "HIGH CARD RICH", "LOW CARD RICH", "BALANCED SHOE"
        val deckAdvantage: String = "NEUTRAL",
        val tieProbabilityBoost: Int = 0,        // % extra tie probability when 0/tens/high cards saturate
        val estimatedRemainingDecks: Double = 8.0,
        val recommendation: String = "Shoe is well-balanced. Standard pattern road strategy recommended."
    )

    private val _shoeState = MutableStateFlow(ShoeState())
    val shoeState: StateFlow<ShoeState> = _shoeState.asStateFlow()

    fun setShoeSize(decks: Int) {
        val total = if (decks == 6) 312 else 416
        _shoeState.value = _shoeState.value.copy(
            totalDecks = decks,
            totalCardsInShoe = total,
            cardsDealt = 0,
            lowCardsCount = 0,
            neutralCardsCount = 0,
            highCardsCount = 0,
            runningCount = 0,
            trueCount = 0.0,
            lowCardPct = 0,
            highCardPct = 0,
            neutralCardPct = 0,
            deckBias = "BALANCED SHOE",
            deckAdvantage = "NEUTRAL",
            tieProbabilityBoost = 0,
            estimatedRemainingDecks = decks.toDouble(),
            recommendation = "New ${decks}-Deck shoe initialized. Ready to count card distribution."
        )
    }

    fun resetShoe() {
        setShoeSize(_shoeState.value.totalDecks)
    }

    /**
     * Record dealt cards:
     * cardType: "LOW" (A-6), "NEUTRAL" (7-8), "HIGH" (9-K)
     */
    fun recordCard(cardType: String, count: Int = 1) {
        val current = _shoeState.value
        val newLow = current.lowCardsCount + if (cardType == "LOW") count else 0
        val newNeutral = current.neutralCardsCount + if (cardType == "NEUTRAL") count else 0
        val newHigh = current.highCardsCount + if (cardType == "HIGH") count else 0
        val newDealt = current.cardsDealt + count

        recalculate(newLow, newNeutral, newHigh, newDealt, current.totalDecks, current.totalCardsInShoe)
    }

    /**
     * Auto-infer card consumption from round results if manual card logging is skipped
     */
    fun recordRoundDealt(gameType: String, result: String) {
        when (gameType) {
            "DRAGON_TIGER" -> {
                // Dragon Tiger burns 2 cards per round (1 Dragon, 1 Tiger)
                if (result == "TIE" || result == "X" || result == "P") {
                    // Ties often occur on equal ranks
                    recordCard("NEUTRAL", 1)
                    recordCard("HIGH", 1)
                } else if (result == "D") {
                    recordCard("HIGH", 1)
                    recordCard("LOW", 1)
                } else {
                    recordCard("LOW", 1)
                    recordCard("HIGH", 1)
                }
            }
            "BACCARAT" -> {
                // Baccarat consumes 4 to 6 cards per round (average 4.5 cards)
                if (result == "T" || result == "TIE") {
                    recordCard("HIGH", 2)
                    recordCard("NEUTRAL", 1)
                    recordCard("LOW", 1)
                } else if (result == "P") {
                    recordCard("LOW", 2)
                    recordCard("HIGH", 2)
                } else {
                    recordCard("HIGH", 2)
                    recordCard("LOW", 2)
                }
            }
        }
    }

    private fun recalculate(low: Int, neutral: Int, high: Int, dealt: Int, decks: Int, totalCards: Int) {
        val running = high - low
        val remainingCards = (totalCards - dealt).coerceAtLeast(1)
        val remainingDecks = (remainingCards.toDouble() / 52.0).coerceAtLeast(0.5)
        val trueCount = running / remainingDecks

        val totalLogged = (low + neutral + high).coerceAtLeast(1)
        val lowPct = (low * 100) / totalLogged
        val highPct = (high * 100) / totalLogged
        val neutralPct = (neutral * 100) / totalLogged

        val bias = when {
            trueCount >= 2.0 -> "HIGH CARD RICH (True Count +${String.format("%.1f", trueCount)})"
            trueCount <= -2.0 -> "LOW CARD RICH (True Count ${String.format("%.1f", trueCount)})"
            else -> "BALANCED SHOE (True Count ${String.format("%.1f", trueCount)})"
        }

        val advantage = when {
            trueCount >= 2.5 -> "HIGH CARD ADVANTAGE"
            trueCount <= -2.5 -> "LOW CARD ADVANTAGE"
            else -> "NEUTRAL"
        }

        val tieBoost = when {
            trueCount >= 3.0 -> 18 // Abundant tens and high cards increase matching ranks / natural 9s
            trueCount >= 1.5 -> 10
            trueCount <= -2.0 -> -6
            else -> 0
        }

        val rec = when {
            trueCount >= 2.5 -> "Deck is saturated with High Cards (9-K). Natural 8/9 & Tie probabilities are +${tieBoost}% above average."
            trueCount <= -2.5 -> "Deck is saturated with Low Cards (A-6). Longer card drawing rounds expected with reduced tie frequency."
            dealt > (totalCards * 0.75) -> "Shoe penetration > 75%. Caution: Shuffle window approaching soon."
            else -> "Shoe distribution is normal and balanced. Multi-road trend patterns are highly reliable."
        }

        _shoeState.value = ShoeState(
            totalDecks = decks,
            totalCardsInShoe = totalCards,
            cardsDealt = dealt,
            lowCardsCount = low,
            neutralCardsCount = neutral,
            highCardsCount = high,
            runningCount = running,
            trueCount = trueCount,
            lowCardPct = lowPct,
            highCardPct = highPct,
            neutralCardPct = neutralPct,
            deckBias = bias,
            deckAdvantage = advantage,
            tieProbabilityBoost = tieBoost,
            estimatedRemainingDecks = remainingDecks,
            recommendation = rec
        )
    }
}

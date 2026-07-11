package com.recalldeck.app.data.db

/** Last counted rating given to a card (derived from review_logs). */
data class CardLastRating(
    val cardId: Long,
    val rating: Int,
)

/**
 * User-facing category of a card, derived from the button last clicked for it
 * (Easy / Medium / Hard / Very hard) rather than the internal scheduling state.
 */
enum class CardBucket(val displayLabel: String) {
    NOT_STUDIED("Not studied"),
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    VERY_HARD("Very hard"),
    NEVER_ASK("Never ask"),
    ;

    companion object {
        fun of(state: CardState, lastRating: Int?): CardBucket = when {
            state == CardState.SUSPENDED -> NEVER_ASK
            lastRating == null -> NOT_STUDIED
            lastRating >= 4 -> EASY
            lastRating == 3 -> MEDIUM
            lastRating == 2 -> HARD
            else -> VERY_HARD
        }
    }
}

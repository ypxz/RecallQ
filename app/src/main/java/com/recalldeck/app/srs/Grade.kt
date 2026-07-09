package com.recalldeck.app.srs

/** Review grade shown to the user, mapped to its FSRS rating value. */
enum class Grade(val fsrsRating: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4),
    ;

    companion object {
        fun fromRating(rating: Int): Grade =
            entries.firstOrNull { it.fsrsRating == rating }
                ?: throw IllegalArgumentException("Unknown FSRS rating: $rating")
    }
}

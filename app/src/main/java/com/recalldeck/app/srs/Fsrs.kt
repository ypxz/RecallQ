package com.recalldeck.app.srs

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * FSRS-6 memory model.
 *
 * Ported from the MIT-licensed reference implementation at
 * https://github.com/open-spaced-repetition/FSRS-Kotlin
 * (Copyright (c) Open Spaced Repetition, MIT License).
 *
 * The port keeps the reference's two-decimal rounding of stability/difficulty so results
 * stay comparable, but is pure Kotlin (no Android dependencies) and deterministic
 * (interval fuzzing is opt-in via an injectable fuzz factor). Deviations from literal
 * reference code are documented in docs/DECISIONS.md.
 */
class Fsrs(
    private val requestRetention: Double = DEFAULT_RETENTION,
    private val params: List<Double> = DEFAULT_PARAMS,
) {
    init {
        require(params.size == PARAM_COUNT) { "FSRS-6 requires $PARAM_COUNT parameters" }
        require(requestRetention > 0.0 && requestRetention < 1.0) {
            "requestRetention must be in (0, 1)"
        }
    }

    /** The two FSRS memory variables. */
    data class MemoryState(val stability: Double, val difficulty: Double)

    private val decay = -params[20]
    private val factor = 0.9.pow(1.0 / decay) - 1

    /** Initial memory state for the first review of a card. */
    fun initialState(grade: Grade): MemoryState =
        MemoryState(stability = initStability(grade), difficulty = initDifficulty(grade))

    /**
     * Next memory state after a long-term (REVIEW) rating, given the days elapsed since
     * the last review.
     */
    fun nextState(state: MemoryState, elapsedDays: Double, grade: Grade): MemoryState {
        val retrievability = forgettingCurve(elapsedDays, state.stability)
        val stability = if (grade == Grade.AGAIN) {
            nextForgetStability(state.difficulty, state.stability, retrievability)
        } else {
            nextRecallStability(state.difficulty, state.stability, retrievability, grade)
        }
        return MemoryState(stability = stability, difficulty = nextDifficulty(state.difficulty, grade))
    }

    /** Next memory state after a same-day / short-term (LEARNING) rating. */
    fun shortTermState(state: MemoryState, grade: Grade): MemoryState =
        MemoryState(
            stability = nextShortTermStability(state.stability, grade),
            difficulty = nextDifficulty(state.difficulty, grade),
        )

    /** Probability of recall after [elapsedDays] with the given [stability]. */
    fun forgettingCurve(elapsedDays: Double, stability: Double): Double {
        if (stability <= 0.0) return 0.0
        return (1.0 + factor * elapsedDays / stability).pow(decay)
    }

    /**
     * Next interval in whole days for the given stability, targeting [requestRetention].
     * Deterministic unless a [fuzzFactor] in [0, 1) is supplied.
     */
    fun nextIntervalDays(stability: Double, fuzzFactor: Double? = null, maxInterval: Int = MAX_INTERVAL_DAYS): Int {
        val rawInterval = stability / factor * (requestRetention.pow(1.0 / decay) - 1.0)
        val interval = if (fuzzFactor != null) applyFuzz(rawInterval, fuzzFactor) else rawInterval
        return interval.roundToInt().coerceIn(1, maxInterval)
    }

    private fun applyFuzz(interval: Double, fuzzFactor: Double): Double {
        if (interval < 2.5) return interval
        val ivl = interval.roundToInt()
        val minIvl = max(2, (ivl * 0.95 - 1).roundToInt())
        val maxIvl = (ivl * 1.05 + 1).roundToInt()
        return kotlin.math.floor(fuzzFactor * (maxIvl - minIvl + 1) + minIvl)
    }

    private fun initDifficulty(grade: Grade): Double {
        val raw = params[4] - exp(params[5] * (grade.fsrsRating - 1)) + 1.0
        return round2(raw.coerceIn(1.0, 10.0))
    }

    private fun initStability(grade: Grade): Double =
        round2(params[grade.fsrsRating - 1].coerceAtLeast(MIN_STABILITY))

    private fun linearDamping(delta: Double, oldD: Double): Double = delta * (10.0 - oldD) / 9.0

    private fun meanReversion(initD: Double, nextD: Double): Double =
        params[7] * initD + (1.0 - params[7]) * nextD

    private fun nextDifficulty(currentD: Double, grade: Grade): Double {
        val deltaD = -params[6] * (grade.fsrsRating - 3)
        val nextD = currentD + linearDamping(deltaD, currentD)
        val reverted = meanReversion(initDifficulty(Grade.EASY), nextD)
        return round2(reverted.coerceIn(1.0, 10.0))
    }

    private fun nextShortTermStability(currentS: Double, grade: Grade): Double {
        var sinc = exp(params[17] * (grade.fsrsRating - 3 + params[18])) * currentS.pow(-params[19])
        if (grade.fsrsRating >= 3) sinc = max(sinc, 1.0)
        return round2((currentS * sinc).coerceAtLeast(MIN_STABILITY))
    }

    private fun nextForgetStability(difficulty: Double, stability: Double, retrievability: Double): Double {
        val sMin = stability / exp(params[17] * params[18])
        val result = params[11] *
            difficulty.pow(-params[12]) *
            ((stability + 1.0).pow(params[13]) - 1.0) *
            exp((1.0 - retrievability) * params[14])
        return round2(min(result, sMin).coerceAtLeast(MIN_STABILITY))
    }

    private fun nextRecallStability(d: Double, s: Double, r: Double, grade: Grade): Double {
        val hardPenalty = if (grade == Grade.HARD) params[15] else 1.0
        val easyBonus = if (grade == Grade.EASY) params[16] else 1.0
        val growth = exp(params[8]) *
            (11.0 - d) *
            s.pow(-params[9]) *
            (exp((1.0 - r) * params[10]) - 1.0) *
            hardPenalty *
            easyBonus
        return round2((s * (1.0 + growth)).coerceAtLeast(MIN_STABILITY))
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0

    companion object {
        const val DEFAULT_RETENTION = 0.9
        const val PARAM_COUNT = 21
        const val MAX_INTERVAL_DAYS = 36500
        const val MIN_STABILITY = 0.01

        /** FSRS-6 default parameters (open-spaced-repetition defaults). */
        val DEFAULT_PARAMS: List<Double> = listOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722,
            0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425,
            0.0912, 0.0658, 0.1542,
        )
    }
}

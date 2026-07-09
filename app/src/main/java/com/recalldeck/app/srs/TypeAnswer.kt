package com.recalldeck.app.srs

/**
 * Type-answer comparison: normalized equality with a Levenshtein-based "almost" hint.
 * The verdict is advisory only — the user's self-grade is always final.
 */
object TypeAnswer {

    enum class Verdict { CORRECT, ALMOST, WRONG }

    /** Lowercases, strips punctuation, and collapses whitespace. */
    fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("""[^\p{L}\p{Nd}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    /**
     * Compares typed [input] against [expected]. CORRECT on normalized equality; ALMOST when
     * the edit distance is small relative to the answer length (<= 1 for short answers,
     * <= 20% of length otherwise); WRONG else.
     */
    fun judge(input: String, expected: String): Verdict {
        val a = normalize(input)
        val b = normalize(expected)
        if (a == b) return Verdict.CORRECT
        if (b.isEmpty() || a.isEmpty()) return Verdict.WRONG
        val distance = levenshtein(a, b)
        val allowed = maxOf(1, b.length / 5)
        return if (distance <= allowed) Verdict.ALMOST else Verdict.WRONG
    }

    /** Classic dynamic-programming Levenshtein edit distance. */
    fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }
}

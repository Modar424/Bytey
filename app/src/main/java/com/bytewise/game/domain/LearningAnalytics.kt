package com.bytewise.game.domain

import com.bytewise.game.data.model.AnswerRecord
import com.bytewise.game.data.model.ConceptLevelStat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Phase 6 learning analytics. Everything here is pure Kotlin (no Android), so it is JVM-testable.
 * Texts are NOT produced here: results carry ids / kinds and the UI localizes them, so switching the
 * language needs no re-analysis.
 */
enum class Trend { NEW, IMPROVING, STABLE, REGRESSING }

object LearningMath {
    /** Added to (1 - accuracy) so a concept with a wrong answer never has zero priority. */
    const val PRIORITY_BIAS = 0.1f

    /** A concept/level pair below this accuracy counts as weak. */
    const val WEAK_ACCURACY = 0.6f

    /** Minimum attempts before a trend is reported (otherwise NEW). */
    const val MIN_HISTORY_FOR_TREND = 2

    /** Accuracy change that counts as a real improvement / regression. */
    const val TREND_DELTA = 0.15f

    /** priority = incorrect x (1 - accuracy + 0.1); higher means "study this first". */
    fun priority(incorrect: Int, accuracy: Float): Float = incorrect * (1f - accuracy + PRIORITY_BIAS)

    fun trend(histAttempts: Int, histCorrect: Int, recentAttempts: Int, recentCorrect: Int): Trend {
        if (histAttempts < MIN_HISTORY_FOR_TREND || recentAttempts == 0) return Trend.NEW
        val delta = recentCorrect.toFloat() / recentAttempts - histCorrect.toFloat() / histAttempts
        return when {
            delta >= TREND_DELTA -> Trend.IMPROVING
            delta <= -TREND_DELTA -> Trend.REGRESSING
            else -> Trend.STABLE
        }
    }
}

// ---------------------------------------------------------------- one finished level

@Serializable
data class ConceptPerformance(
    val conceptId: String,
    val attempts: Int,
    val correct: Int,
    val timeouts: Int,
    val avgSeconds: Float,
    val trend: Trend,
) {
    val incorrect: Int get() = attempts - correct
    val accuracy: Float get() = if (attempts == 0) 0f else correct.toFloat() / attempts
    val priority: Float get() = LearningMath.priority(incorrect, accuracy)
}

/** Analysis of one completed session; this is also the JSON payload stored in `learning_reports`. */
@Serializable
data class SessionAnalysis(
    val concepts: List<ConceptPerformance>,
    val timeouts: Int,
    /** Average response time of wrong (non-timeout) answers; 0 when there were none. */
    val wrongAvgSeconds: Float,
    /** Average response time of correct answers; 0 when there were none. */
    val correctAvgSeconds: Float,
) {
    /** Concepts with at least one mistake, most urgent first. */
    val weak: List<ConceptPerformance>
        get() = concepts.filter { it.incorrect > 0 }
            .sortedWith(compareByDescending<ConceptPerformance> { it.priority }.thenByDescending { it.incorrect }.thenBy { it.conceptId })

    /** Concepts answered perfectly, most practised first. */
    val strong: List<ConceptPerformance>
        get() = concepts.filter { it.attempts > 0 && it.incorrect == 0 }
            .sortedWith(compareByDescending<ConceptPerformance> { it.attempts }.thenBy { it.conceptId })

    val totalAttempts: Int get() = concepts.sumOf { it.attempts }
    val totalIncorrect: Int get() = concepts.sumOf { it.incorrect }
}

object LearningAnalyzer {
    /**
     * @param sessionAnswers answers of the session being analysed.
     * @param history answers of EARLIER completed sessions (any level); used only for trends.
     */
    fun analyzeSession(sessionAnswers: List<AnswerRecord>, history: List<AnswerRecord>): SessionAnalysis {
        val histByConcept = history.groupBy { it.conceptId }
        val concepts = sessionAnswers.groupBy { it.conceptId }.map { (id, list) ->
            val h = histByConcept[id].orEmpty()
            val correct = list.count { it.isCorrect }
            ConceptPerformance(
                conceptId = id,
                attempts = list.size,
                correct = correct,
                timeouts = list.count { it.wasTimeout },
                avgSeconds = list.map { it.responseTimeSeconds }.average().toFloat(),
                trend = LearningMath.trend(h.size, h.count { it.isCorrect }, list.size, correct),
            )
        }.sortedBy { it.conceptId }
        val wrong = sessionAnswers.filter { !it.isCorrect && !it.wasTimeout }
        val right = sessionAnswers.filter { it.isCorrect }
        return SessionAnalysis(
            concepts = concepts,
            timeouts = sessionAnswers.count { it.wasTimeout },
            wrongAvgSeconds = if (wrong.isEmpty()) 0f else wrong.map { it.responseTimeSeconds }.average().toFloat(),
            correctAvgSeconds = if (right.isEmpty()) 0f else right.map { it.responseTimeSeconds }.average().toFloat(),
        )
    }
}

object ReportCodec {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun encode(analysis: SessionAnalysis): String = json.encodeToString(SessionAnalysis.serializer(), analysis)

    /** Null when the stored payload is unreadable (the caller then re-analyses). */
    fun decode(payload: String): SessionAnalysis? =
        try {
            json.decodeFromString(SessionAnalysis.serializer(), payload)
        } catch (e: kotlinx.serialization.SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}

// ---------------------------------------------------------------- all levels together

/** A concept over all completed sessions. Accuracy = correct / attempts (raw counts, never an average of percentages). */
data class ConceptOverview(
    val conceptId: String,
    val attempts: Int,
    val correct: Int,
    /** Levels (ascending) in which the concept was below [LearningMath.WEAK_ACCURACY]. */
    val weakLevels: List<Int>,
    /** Weak in two or more levels, or many attempts overall and still weak. */
    val persistent: Boolean,
) {
    val incorrect: Int get() = attempts - correct
    val accuracy: Float get() = if (attempts == 0) 0f else correct.toFloat() / attempts
    val priority: Float get() = LearningMath.priority(incorrect, accuracy)
}

object MultiLevelAnalyzer {
    private const val PERSISTENT_MIN_ATTEMPTS = 4

    /** Concepts with at least one mistake, most urgent first. */
    fun analyze(stats: List<ConceptLevelStat>): List<ConceptOverview> =
        stats.groupBy { it.conceptId }.map { (id, rows) ->
            val attempts = rows.sumOf { it.attempts }
            val correct = rows.sumOf { it.correct }
            val weakLevels = rows
                .filter { it.attempts > 0 && it.correct.toFloat() / it.attempts < LearningMath.WEAK_ACCURACY }
                .map { it.level }.distinct().sorted()
            val accuracy = if (attempts == 0) 0f else correct.toFloat() / attempts
            ConceptOverview(
                conceptId = id,
                attempts = attempts,
                correct = correct,
                weakLevels = weakLevels,
                persistent = weakLevels.size >= 2 ||
                    (attempts >= PERSISTENT_MIN_ATTEMPTS && accuracy < LearningMath.WEAK_ACCURACY),
            )
        }.filter { it.incorrect > 0 }
            .sortedWith(compareByDescending<ConceptOverview> { it.priority }.thenByDescending { it.incorrect }.thenBy { it.conceptId })

    /**
     * Long-term trend per concept: the latest two attempts against everything before them.
     * [answersChronological] must be sorted oldest first.
     */
    fun trends(answersChronological: List<AnswerRecord>): Map<String, Trend> =
        answersChronological.groupBy { it.conceptId }.mapValues { (_, list) ->
            if (list.size < LearningMath.MIN_HISTORY_FOR_TREND + 2) {
                Trend.NEW
            } else {
                val recent = list.takeLast(2)
                val older = list.dropLast(2)
                LearningMath.trend(older.size, older.count { it.isCorrect }, recent.size, recent.count { it.isCorrect })
            }
        }
}

// ---------------------------------------------------------------- study plan

data class StudyCandidate(val conceptId: String, val priority: Float, val persistent: Boolean)

data class StudyItem(val conceptId: String, val minutes: Int, val persistent: Boolean)

data class StudyPlan(val items: List<StudyItem>) {
    val totalMinutes: Int get() = items.sumOf { it.minutes }
}

object StudyPlanner {
    const val MAX_ITEMS = 5

    /** Most urgent first; persistent weaknesses win ties; [minutesOf] supplies each concept's estimated minutes. */
    fun build(candidates: List<StudyCandidate>, minutesOf: (String) -> Int, maxItems: Int = MAX_ITEMS): StudyPlan =
        StudyPlan(
            candidates
                .sortedWith(compareByDescending<StudyCandidate> { it.priority }.thenByDescending { it.persistent }.thenBy { it.conceptId })
                .take(maxItems)
                .map { StudyItem(it.conceptId, minutesOf(it.conceptId).coerceAtLeast(0), it.persistent) },
        )
}

// ---------------------------------------------------------------- advice

enum class AdviceKind {
    /** No mistakes at all. */
    PERFECT,

    /** Study [Advice.conceptId] first. */
    FOCUS_CONCEPT,

    /** [Advice.conceptId] was weak in several levels. */
    PERSISTENT_CONCEPT,
    IMPROVED_CONCEPT,
    REGRESSED_CONCEPT,

    /** [Advice.count] timeouts. */
    TIMEOUTS,

    /** Wrong answers came quickly: read the whole question and all options. */
    SLOW_DOWN,

    /** Correct but slow: practise recall to answer faster. */
    SPEED_UP,
    RETRY_FAILED,
    NEXT_LEVEL,
}

data class Advice(val kind: AdviceKind, val conceptId: String? = null, val count: Int = 0)

object AdviceGenerator {
    const val MAX_ADVICE = 6
    const val RUSHED_SECONDS = 5f
    const val SLOW_SECONDS = 12f
    private const val MIN_TIMEOUTS = 3
    private const val MIN_RUSHED_WRONG = 3
    private const val MIN_SLOW_CORRECT = 5

    fun forSession(
        analysis: SessionAnalysis,
        passed: Boolean,
        persistentIds: Set<String> = emptySet(),
    ): List<Advice> {
        val out = mutableListOf<Advice>()
        val weak = analysis.weak
        if (weak.isEmpty()) {
            out += Advice(AdviceKind.PERFECT)
        } else {
            out += Advice(AdviceKind.FOCUS_CONCEPT, weak.first().conceptId)
            weak.firstOrNull { it.conceptId in persistentIds }?.let {
                out += Advice(AdviceKind.PERSISTENT_CONCEPT, it.conceptId)
            }
        }
        val wrongCount = analysis.concepts.sumOf { it.incorrect } - analysis.timeouts
        val correctCount = analysis.concepts.sumOf { it.correct }
        when {
            analysis.timeouts >= MIN_TIMEOUTS -> out += Advice(AdviceKind.TIMEOUTS, count = analysis.timeouts)
            wrongCount >= MIN_RUSHED_WRONG && analysis.wrongAvgSeconds in 0.01f..RUSHED_SECONDS -> out += Advice(AdviceKind.SLOW_DOWN)
            correctCount >= MIN_SLOW_CORRECT && analysis.correctAvgSeconds > SLOW_SECONDS -> out += Advice(AdviceKind.SPEED_UP)
        }
        analysis.concepts.firstOrNull { it.trend == Trend.IMPROVING }?.let { out += Advice(AdviceKind.IMPROVED_CONCEPT, it.conceptId) }
        analysis.concepts.firstOrNull { it.trend == Trend.REGRESSING }?.let { out += Advice(AdviceKind.REGRESSED_CONCEPT, it.conceptId) }
        out += if (passed) Advice(AdviceKind.NEXT_LEVEL) else Advice(AdviceKind.RETRY_FAILED)
        return out.take(MAX_ADVICE)
    }
}

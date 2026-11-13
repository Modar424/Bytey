package com.bytewise.game.domain

import com.bytewise.game.data.model.AnswerRecord
import com.bytewise.game.data.model.ConceptLevelStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningAnalyticsTest {
    private fun a(session: Long, concept: String, ok: Boolean, secs: Float = 8f, timeout: Boolean = false, at: Long = 0) =
        AnswerRecord(
            sessionId = session, questionId = "q$session$concept$at${secs}", conceptId = concept, level = 1,
            selectedIndex = 0, correctIndex = 0, isCorrect = ok, responseTimeSeconds = secs,
            earnedPoints = 0, wasTimeout = timeout, answeredAt = at,
        )

    private val current = listOf(
        a(2, "x", false, 3f), a(2, "x", false, 3f), a(2, "y", true),
        a(2, "z", false, 20f, true), a(2, "z", false, 20f, true), a(2, "z", false, 20f, true),
    )
    private val history = listOf(a(1, "y", false), a(1, "y", false), a(1, "x", true), a(1, "x", true))

    @Test fun priorityIsIncorrectTimesInverseAccuracyPlusBias() {
        assertEquals(2.2f, LearningMath.priority(2, 0f), 0.0001f) // 2 x (1 - 0 + 0.1)
        assertEquals(0.1f, LearningMath.priority(1, 1f), 0.0001f)
    }

    @Test fun weakConceptsAreOrderedByPriority() {
        val an = LearningAnalyzer.analyzeSession(current, history)
        assertEquals(listOf("z", "x"), an.weak.map { it.conceptId }) // z: 3 x 1.1, x: 2 x 1.1
        assertEquals(listOf("y"), an.strong.map { it.conceptId })
        assertEquals(3, an.timeouts)
    }

    @Test fun trendsComeFromEarlierSessions() {
        val an = LearningAnalyzer.analyzeSession(current, history)
        assertEquals(Trend.IMPROVING, an.concepts.first { it.conceptId == "y" }.trend)
        assertEquals(Trend.REGRESSING, an.concepts.first { it.conceptId == "x" }.trend)
        assertEquals(Trend.NEW, an.concepts.first { it.conceptId == "z" }.trend)
    }

    @Test fun overviewFlagsPersistentWeaknesses() {
        val ov = MultiLevelAnalyzer.analyze(
            listOf(
                ConceptLevelStat("p", 1, 2, 0), ConceptLevelStat("p", 3, 1, 0),
                ConceptLevelStat("q", 1, 5, 5), ConceptLevelStat("r", 2, 4, 3),
            ),
        )
        assertEquals(listOf("p", "r"), ov.map { it.conceptId }) // q has no mistakes
        assertTrue(ov[0].persistent)
        assertEquals(listOf(1, 3), ov[0].weakLevels)
        assertTrue(!ov[1].persistent)
    }

    @Test fun accuracyIsWeightedByAttemptsNotAveragedPercentages() {
        val ov = MultiLevelAnalyzer.analyze(listOf(ConceptLevelStat("c", 1, 1, 0), ConceptLevelStat("c", 2, 9, 9)))
        assertEquals(0.9f, ov.single().accuracy, 0.0001f)
    }

    @Test fun adviceIsPrioritisedAndEndsWithTheNextStep() {
        val an = LearningAnalyzer.analyzeSession(current, history)
        val adv = AdviceGenerator.forSession(an, passed = false, persistentIds = setOf("x"))
        assertEquals(AdviceKind.FOCUS_CONCEPT, adv.first().kind)
        assertEquals("z", adv.first().conceptId)
        assertTrue(adv.any { it.kind == AdviceKind.TIMEOUTS && it.count == 3 })
        assertTrue(adv.any { it.kind == AdviceKind.PERSISTENT_CONCEPT && it.conceptId == "x" })
        assertEquals(AdviceKind.RETRY_FAILED, adv.last().kind)
    }

    @Test fun perfectRunGetsPerfectAdvice() {
        val an = LearningAnalyzer.analyzeSession(listOf(a(5, "x", true), a(5, "y", true)), emptyList())
        val adv = AdviceGenerator.forSession(an, passed = true)
        assertEquals(AdviceKind.PERFECT, adv.first().kind)
        assertEquals(AdviceKind.NEXT_LEVEL, adv.last().kind)
    }

    @Test fun studyPlanTakesTheMostUrgentAndSumsMinutes() {
        val plan = StudyPlanner.build(
            listOf(StudyCandidate("a", 1f, false), StudyCandidate("b", 3f, false), StudyCandidate("c", 2f, true)),
            minutesOf = { 30 }, maxItems = 2,
        )
        assertEquals(listOf("b", "c"), plan.items.map { it.conceptId })
        assertEquals(60, plan.totalMinutes)
    }

    @Test fun reportPayloadSurvivesJsonRoundTrip() {
        val an = LearningAnalyzer.analyzeSession(current, history)
        assertEquals(an, ReportCodec.decode(ReportCodec.encode(an)))
        assertNull(ReportCodec.decode("not json"))
        assertNotNull(ReportCodec.decode(ReportCodec.encode(an)))
    }
}

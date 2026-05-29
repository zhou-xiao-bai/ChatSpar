package com.chatspar.app.data.review

import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewJsonParserTest {
    private val parser = ReviewJsonParser(
        now = { FIXED_TIME },
        reviewIdProvider = { "review_001" },
    )

    @Test
    fun parse_standardJson_returnsReview() {
        val review = parser.parse(
            rawResponse = STANDARD_JSON,
            sessionId = "session_001",
            scenarioId = "S001",
        )

        assertEquals("review_001", review.id)
        assertEquals("session_001", review.sessionId)
        assertEquals("S001", review.scenarioId)
        assertEquals(FIXED_TIME, review.createdAt)
        assertEquals("你能正常回应，但话题推进偏少。", review.overallSummary)
        assertEquals(4, review.scores.courage)
        assertEquals("topic_progress", review.problems.first().type)
        assertEquals("我是小陈大学同学。", review.keyMoments.first().userText)
        assertEquals(listOf("开场", "接话"), review.suggestedExpressions.first().tags)
        assertEquals(STANDARD_JSON, review.rawResponse)
    }

    @Test
    fun parse_fencedJson_returnsReview() {
        val review = parser.parse(
            rawResponse = "```json\n$STANDARD_JSON\n```",
            sessionId = "session_001",
            scenarioId = "S001",
        )

        assertEquals("你能正常回应，但话题推进偏少。", review.overallSummary)
    }

    @Test
    fun parse_missingRequiredField_throwsClearError() {
        val result = runCatching {
            parser.parse(
                rawResponse = """{"overallSummary":"缺少评分"}""",
                sessionId = "session_001",
                scenarioId = "S001",
            )
        }

        assertTrue(result.exceptionOrNull() is ReviewParseException)
        assertTrue(result.exceptionOrNull()?.message?.contains("缺少字段") == true)
    }

    @Test
    fun parse_invalidScore_throwsClearError() {
        val result = runCatching {
            parser.parse(
                rawResponse = STANDARD_JSON.replace(""""courage": 4""", """"courage": 6"""),
                sessionId = "session_001",
                scenarioId = "S001",
            )
        }

        assertTrue(result.exceptionOrNull() is ReviewParseException)
        assertTrue(result.exceptionOrNull()?.message?.contains("1 到 5") == true)
    }

    private companion object {
        val FIXED_TIME: OffsetDateTime = OffsetDateTime.parse("2026-05-26T11:40:00+08:00")

        const val STANDARD_JSON = """
            {
              "overallSummary": "你能正常回应，但话题推进偏少。",
              "scores": {
                "courage": 4,
                "response": 3,
                "boundary": 4,
                "topicProgress": 2,
                "naturalness": 3
              },
              "problems": [
                {
                  "type": "topic_progress",
                  "title": "话题延展不够",
                  "description": "回答能接住问题，但没有给对方继续聊的入口。"
                }
              ],
              "keyMoments": [
                {
                  "userText": "我是小陈大学同学。",
                  "issue": "信息比较少，对方不容易继续接。",
                  "betterExpression": "我是小陈大学同学，之前一起参加过活动。你们平时也经常一起吃饭吗？"
                }
              ],
              "suggestedExpressions": [
                {
                  "content": "我跟他是之前一起参加活动认识的。",
                  "tags": ["开场", "接话"]
                }
              ],
              "nextSuggestion": "下次回答后补一个细节，再抛出一个轻问题。"
            }
        """
    }
}

package com.embabel.template.agent

import com.embabel.agent.api.common.autonomy.AgentInvocation
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.testing.integration.EmbabelMockitoIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.contains

/**
 * Use framework superclass to test the complete workflow of writing and reviewing a story.
 * This will run under Spring Boot against an AgentPlatform instance
 * that has loaded all our agents.
 */
class WriteAndReviewAgentIntegrationTest : EmbabelMockitoIntegrationTest() {

    @Test
    fun `should execute complete workflow`() {
        val input = UserInput("Write about artificial intelligence")

        val story = Story("AI will transform our world...")
        val reviewedStory = ReviewedStory(story, "Excellent exploration of AI themes.", Reviewer)

        whenCreateObject(contains("Craft a short story"), Story::class.java)
            .thenReturn(story)

        // The second call uses generateText
        whenGenerateText(contains("You will be given a short story to review"))
            .thenReturn(reviewedStory.review)

        val invocation = AgentInvocation.create(agentPlatform, ReviewedStory::class.java)
        val reviewedStoryResult = invocation.invoke(input)

        assertNotNull(reviewedStoryResult)
        assertTrue(
            reviewedStoryResult.content.contains(story.text),
            "Expected story content to be present: ${reviewedStoryResult.content}"
        )
        assertEquals(
            reviewedStory,
            reviewedStoryResult,
            "Expected review to match: $reviewedStoryResult"
        )

        verifyCreateObjectMatching(
            { prompt -> prompt.contains("Craft a short story") },
            Story::class.java,
            ArgumentMatcher { llmInteraction -> llmInteraction?.llm?.temperature?.let { it > 0.6 } ?: false }
        )
        verifyGenerateTextMatching { prompt ->
            prompt.contains("You will be given a short story to review")
        }

        verifyNoMoreInteractions()
    }
}
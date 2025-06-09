/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.template.agent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.create
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.HasContent
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.Auto
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile

data class Greeting(
    val name: String?,
    val greeting: String,
) : HasContent {

    override val content: String
        get() = greeting + (name?.let { ", $it" } ?: "")
}

@Agent(
    description = "Say hello",
)
@Profile("!test")
class HelloAgent(
    @Value("\${wordCount:20}") private val wordCount: Int,
) {

    @AchievesGoal("The user has been greeted")
    @Action
    fun greet(userInput: UserInput): Greeting {
        return using(
            LlmOptions(criteria = Auto).withTemperature(.9)
        ).create(
            """
            Greet the user. If you know their name, include it in the greeting.
            Otherwise, improvise a funny greeting in $wordCount words or less.
        """.trimIndent()
        )
    }

}

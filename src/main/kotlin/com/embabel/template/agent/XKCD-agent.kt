package com.embabel.template.agent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.Export
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.createObject
import com.embabel.agent.domain.io.UserInput
import com.embabel.common.ai.model.LlmOptions
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import kotlin.random.Random

enum class Classification{
    HIGHSCHOOL_OK, HIGHSCHOOL_NOT_OK
}

data class ComicClassification(
    val filePath: ComicFilePath,
    val classification: Classification,
    val reasoning: String?
)

data class ComicFilePath(
    val filePath: String?
)

data class MultipleComicAnalysis(
    val analyses: List<ComicClassification> = emptyList(),
    val targetCount: Int = 5
)

data class FiveClassifications(
    val list: List<ComicClassification> = emptyList()
)


private val restTemplate = RestTemplate()

data class XkcdResponse(
    val month: String,
    val num: Int,
    val link: String,
    val year: String,
    val news: String,
    @JsonProperty("safe_title")
    val safeTitle: String,
    val transcript: String,
    val alt: String,
    val img: String,
    val title: String,
    val day: String
)

fun fetchAndDownloadImage(apiUrl: String, downloadDirectory: String): String? {
    return try {
        val response = restTemplate.getForObject(apiUrl, XkcdResponse::class.java)

        response?.let {
            val imageUrl = it.img
            println("Image URL: $imageUrl")

            val filePath = downloadImage(imageUrl, downloadDirectory, it.num.toString())
            return filePath
        }
    } catch (e: Exception) {
        println("Error fetching or downloading image: ${e.message}")
        null
    }
}

private fun downloadImage(imageUrl: String, directory: String, comicNumber: String): String {
    val dir = File(directory)
    if (!dir.exists()) {
        dir.mkdirs()
    }

    val extension = imageUrl.substringAfterLast('.').substringBefore('?')
    val fullFileName = "${comicNumber}.${extension}"
    val filePath = Paths.get(directory, fullFileName).toString()

    val imageBytes = restTemplate.getForObject(imageUrl, ByteArray::class.java)

    imageBytes?.let { bytes ->
        FileOutputStream(filePath).use { output ->
            output.write(bytes)
        }
        println("Image downloaded successfully to: $filePath")
    }

    return filePath
}

@Agent(description = "Fetch a XKCD comic image")
class XkcdAgent {

    @Action
    fun fetchRandomXkcd(userInput: UserInput, context: OperationContext): ComicFilePath? {
        var comicNumber = Random.nextInt(1, 3001)
        val url = "https://xkcd.com/$comicNumber/info.0.json"

        val filePath = fetchAndDownloadImage(url, downloadDirectory = "/Users/lilimartin/Downloads")

        return ComicFilePath(filePath)
    }

    @Action
    fun classifyComic(
        comicFilePath: ComicFilePath,
        context: OperationContext): ComicClassification? {
        println("$comicFilePath Is the second Action")

        val imageFile = File(comicFilePath.filePath!!)
        println("$imageFile This is the image file")

        val classification = context.ai()
            .withLlm(LlmOptions.withModel("qwen2.5vl:7b"))
            .createObject<ComicClassification>(
                text = """
                Assess whether the comic image provided is suitable for a high-schooler aged audience 
                (i.e., would they understand the context behind the comic with a high-school level background knowledge).
                Return a) a reason b) either HIGHSCHOOL_OK or HIGHSCHOOL_NOT_OK c) $imageFile 
            """.trimIndent(),
                imageFile = imageFile
            )

        println("$classification This is the classification printed")
        return classification
    }

    @AchievesGoal(
        description = "Download and classify 5 random XKCD comics for high school suitability",
        export = Export(remote = true, name = "ClassifyFiveComics")
    )
   @Action
    fun collectFiveComics(userInput: UserInput, context: OperationContext): MultipleComicAnalysis {
        val results = mutableListOf<ComicClassification>()

        repeat(5) { index ->
            println("Processing comic ${index + 1} of 5...")
            // Fetch comic
            val comicPath = fetchRandomXkcd(userInput, context)
            comicPath?.let { path ->
                // Classify it
                val classification = classifyComic(path, context)
                classification?.let { results.add(it) }
            }
        }

        return MultipleComicAnalysis(results)
    }

    @Action
    fun displayFiveClassifications(
        fiveClassifications: FiveClassifications
    ) {

    }
}


package com.example.hw2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.hw2.network.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val apiKey = "AIzaSyBBfmwzpBrfkyiiTAJciDn1iM_8gOiBQEg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SentimentAnalyzer(apiKey)
        }
    }
}

@Composable
fun SentimentAnalyzer(apiKey: String) {
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    var resultText by remember { mutableStateOf("Result") }
    var sentiment by remember { mutableStateOf("neutral") }
    val scope = rememberCoroutineScope()

    val backgroundColor = when (sentiment) {
        "positive" -> Color(0xFF4CAF50) // Green
        "negative" -> Color(0xFFB71C1C) // Red
        else -> Color(0xFFEEEEEE) // Gray
    }

    val emoji = when (sentiment) {
        "positive" -> R.drawable.smile
        "negative" -> R.drawable.sad
        else -> R.drawable.neutral
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Enter the sentence to analyze") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (userInput.text.isNotBlank()) {
                    scope.launch {
                        val analysisResult = analyzeSentiment(userInput.text, apiKey)
                        resultText = analysisResult
                        sentiment = when {
                            "positive" in analysisResult.lowercase() -> "positive"
                            "negative" in analysisResult.lowercase() -> "negative"
                            else -> "neutral"
                        }
                    }
                } else {
                    resultText = "Enter the text!"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = emoji),
            contentDescription = "Sentiment Emoji",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = resultText, style = MaterialTheme.typography.bodyLarge)
    }
}

suspend fun analyzeSentiment(text: String, apiKey: String): String {
    return try {
        val systemInstruction = "Tell me whether the following sentence's sentiment is positive, negative, or something in between."
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "$systemInstruction\n$text")
                    )
                )
            )
        )

        println("🔵 Sending request: $request")

        val response = RetrofitInstance.api.getSentiment(apiKey, request)
        val responseBody = response.body()

        println("🟢 Response code: ${response.code()}")
        println("🟢 Response body: $responseBody")

        if (!response.isSuccessful) {
            return "API Error: ${response.code()} - ${response.message()}"
        }

        if (responseBody?.candidates?.isNotEmpty() == true) {
            return "Result: ${responseBody.candidates[0].content.parts[0].text}"
        } else {
            return "Cannot analyze emotions."
        }

    } catch (e: Exception) {
        println("🔴 API Call Failed: ${e.message}")
        return "Error: ${e.message}"
    }
}

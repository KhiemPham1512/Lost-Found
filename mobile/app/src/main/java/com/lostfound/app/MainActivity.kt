
package com.lostfound.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.lostfound.app.ui.theme.LostFoundTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LostFoundTheme {
                var currentScreen by remember {
                    mutableStateOf("authTest")
                }

                when (currentScreen) {
                    "authTest" -> {
                        AuthTestScreen(
                            onLoginSuccess = {
                                currentScreen = "detail"
                            }
                        )
                    }
                    "detail" -> {
                        DetailScreen(
                            isFound = true,
                            onVerify = {
                                currentScreen = "verify"
                            }
                        )
                    }

                    "verify" -> {
                        VerifyApiScreen(
                            postId = "9f000000-0000-4000-8000-000000000001",
                            onBack = {
                                currentScreen = "detail"
                            },
                            onSubmitSuccess = { claimId, status ->
                                currentScreen = "detail"
                            }
                        )
                    }

                    "claimReview" -> {
                        ClaimReviewScreen(
                            onDecision = { username, decision ->
                                if (decision == "PASS") {
                                    currentScreen = "chat"
                                }
                                // FAIL: giu nguyen man hinh duyet.
                            }
                        )
                    }

                    "chat" -> {
                        ChatScreen(
                            onStartHandover = {
                                currentScreen = "handover"
                            }
                        )
                    }

                    "handover" -> {
                        HandoverScreen(
                            onConfirmReceived = {
                                currentScreen = "notifications"
                            },
                            onReportProblem = {
                                // Tam thoi chua xu ly.
                            }
                        )
                    }

                    "notifications" -> {
                        NotificationsScreen()
                    }
                }
            }
        }
    }
}

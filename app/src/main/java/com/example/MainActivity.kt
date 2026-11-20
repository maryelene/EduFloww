package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ai.configureAppCheck
import com.example.ui.EduFlowApp
import com.example.ui.theme.EduFlowTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseApp = FirebaseApp.initializeApp(this) ?: FirebaseApp.getInstance()
        getSharedPreferences(
            "com.google.firebase.appcheck.debug.store.${firebaseApp.persistenceKey}",
            Context.MODE_PRIVATE
        ).edit().putString(
            "com.google.firebase.appcheck.debug.DEBUG_SECRET",
            "4B5E0858-88D1-45A1-9945-5F95E373572C"
        ).commit()

        // Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            EduFlowTheme {
                EduFlowApp()
            }
        }
    }
}

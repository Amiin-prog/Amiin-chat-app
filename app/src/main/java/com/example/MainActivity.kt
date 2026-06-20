package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.database.AppDatabase
import com.example.p2p.P2PManager
import com.example.repository.OfflineChatRepository
import com.example.ui.screens.AppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Database and DAOs
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = OfflineChatRepository(
            userDao = database.userDao(),
            messageDao = database.messageDao(),
            chatDao = database.chatDao(),
            myProfileDao = database.myProfileDao()
        )

        // Initialize P2P & simulation service
        val p2pManager = P2PManager(applicationContext, repository)

        // Initialize ViewModel via Constructor Factory
        val factory = MainViewModelFactory(applicationContext, repository, p2pManager)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            val appThemeState by viewModel.appTheme.collectAsState()

            // Resolve light / dark appearance preferences dynamically
            val isDark = when (appThemeState) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(viewModel = viewModel)
                }
            }
        }
    }
}

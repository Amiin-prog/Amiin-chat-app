package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.p2p.P2PManager
import com.example.repository.OfflineChatRepository

class MainViewModelFactory(
    private val context: Context,
    private val repository: OfflineChatRepository,
    private val p2pManager: P2PManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context, repository, p2pManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val nickname: String,
    val bio: String,
    val avatarIndex: Int = 0,
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val isOnline: Boolean = false,
    val connectionStatus: String = "Disconnected", // "Disconnected", "Connecting", "Connected"
    val signalStrength: Int = 3, // 1 to 5 scale
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: String, // Matches partner's userId for 1-1, or "group_chat" for group chat
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Sent", // "Pending", "Sent", "Delivered", "Read"
    val isEncrypted: Boolean = true,
    val messageType: String = "Text", // "Text", "Image", "Document", "Voice", "Video"
    val fileUri: String? = null, // URI of stored attachment path
    val fileName: String? = null,
    val fileSize: String? = null,
    val isIncoming: Boolean = true,
    val reaction: String? = null // Message reaction, e.g. "👍", "❤️", "😂"
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val chatName: String,
    val isGroup: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val avatarIndex: Int = 0,
    val isPinned: Boolean = false // Conversation pinning support
)

@Entity(tableName = "my_profile")
data class MyProfileEntity(
    @PrimaryKey val id: Int = 1, // Single row ID
    val name: String = "Amiin Cabdi",
    val nickname: String = "Amiin",
    val bio: String = "Amiin Offline Chat - Secure nearby messaging developed by Amiin Cabdi.",
    val avatarIndex: Int = 0,
    val preferredLanguage: String = "so", // "en" or "so" (Default is Somali!)
    val preferredTheme: String = "system", // "light", "dark", "system"
    val chatWallpaperIndex: Int = 0, // 0 for default, 1 for starry sky, 2 for neon bubble, 3 for soft mint
    val batterySavingMode: Boolean = false // Toggle for offline BLE energy optimization
)

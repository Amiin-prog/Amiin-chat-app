package com.example.repository

import com.example.database.*
import com.example.security.EncryptionHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineChatRepository(
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val myProfileDao: MyProfileDao
) {
    // Expose flows to the UI safely
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val favoriteUsers: Flow<List<UserEntity>> = userDao.getFavoriteUsers()
    val blockedUsers: Flow<List<UserEntity>> = userDao.getBlockedUsers()
    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()
    val myProfile: Flow<MyProfileEntity?> = myProfileDao.getMyProfileFlow()

    // Decrypt messages on-the-fly for reactive UI updates
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForChat(chatId).map { messageList ->
            messageList.map { msg ->
                if (msg.isEncrypted) {
                    msg.copy(content = EncryptionHelper.decrypt(msg.content))
                } else {
                    msg
                }
            }
        }
    }

    // Live search through and decrypt messages
    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        return messageDao.searchMessages(query).map { messageList ->
            messageList.map { msg ->
                if (msg.isEncrypted) {
                    msg.copy(content = EncryptionHelper.decrypt(msg.content))
                } else {
                    msg
                }
            }
        }
    }

    suspend fun getMyProfileDirect(): MyProfileEntity? {
        return myProfileDao.getMyProfileDirect()
    }

    suspend fun saveMyProfile(profile: MyProfileEntity) {
        myProfileDao.insertOrUpdateProfile(profile)
    }

    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun updateUserBlockStatus(userId: String, isBlocked: Boolean) {
        userDao.updateUserBlockStatus(userId, isBlocked)
    }

    suspend fun updateUserFavoriteStatus(userId: String, isFavorite: Boolean) {
        userDao.updateUserFavoriteStatus(userId, isFavorite)
    }

    suspend fun updateConnectionStatus(userId: String, status: String, isOnline: Boolean) {
        userDao.updateConnectionStatus(userId, status, isOnline)
    }

    suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
    }

    suspend fun resetChatUnread(chatId: String) {
        chatDao.resetUnread(chatId)
    }

    /**
     * Send or receive message: encrypts the payload, persists in DB, and sets up/updates chat metrics.
     */
    suspend fun insertNewMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        content: String,
        messageType: String = "Text",
        fileUri: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        isIncoming: Boolean = false,
        status: String = "Sent"
    ): Long {
        val encryptedContent = EncryptionHelper.encrypt(content)
        val message = MessageEntity(
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            content = encryptedContent,
            isEncrypted = true,
            messageType = messageType,
            fileUri = fileUri,
            fileName = fileName,
            fileSize = fileSize,
            isIncoming = isIncoming,
            status = if (isIncoming) "Read" else status
        )
        val id = messageDao.insertMessage(message)

        // Make sure a representing active chat item exists as well
        val finalChatName = if (chatId == "group_chat") {
            if (senderName.contains("Amiin", ignoreCase = true)) "Guurti Offline Group" else "Offline Group Chat"
        } else {
            senderName
        }
        
        val recipientPeer = userDao.getUserById(chatId)
        val avatar = recipientPeer?.avatarIndex ?: (if (chatId == "group_chat") 99 else 0)

        val chat = ChatEntity(
            chatId = chatId,
            chatName = finalChatName,
            isGroup = chatId == "group_chat",
            lastMessage = content,
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = if (isIncoming) 1 else 0,
            avatarIndex = avatar
        )
        chatDao.insertChat(chat)
        return id
    }

    suspend fun clearHistory() {
        messageDao.clearAllMessages()
        chatDao.clearAllChats()
    }

    suspend fun deleteChat(chatId: String) {
        chatDao.deleteChatById(chatId)
    }

    suspend fun updateMessageReaction(messageId: Long, reaction: String?) {
        messageDao.updateMessageReaction(messageId, reaction)
    }

    suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean) {
        chatDao.updateChatPinStatus(chatId, isPinned)
    }
}

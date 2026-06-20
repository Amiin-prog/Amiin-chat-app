package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY isFavorite DESC, name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isBlocked = 1 ORDER BY name ASC")
    fun getBlockedUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET isBlocked = :isBlocked WHERE userId = :userId")
    suspend fun updateUserBlockStatus(userId: String, isBlocked: Boolean)

    @Query("UPDATE users SET isFavorite = :isFavorite WHERE userId = :userId")
    suspend fun updateUserFavoriteStatus(userId: String, isFavorite: Boolean)

    @Query("UPDATE users SET connectionStatus = :status, isOnline = :isOnline WHERE userId = :userId")
    suspend fun updateConnectionStatus(userId: String, status: String, isOnline: Boolean)

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: String)

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :id")
    suspend fun updateMessageReaction(id: Long, reaction: String?)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTime = :lastMessageTime, unreadCount = unreadCount + :unreadIncrement WHERE chatId = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, lastMessageTime: Long, unreadIncrement: Int)

    @Query("UPDATE chats SET unreadCount = 0 WHERE chatId = :chatId")
    suspend fun resetUnread(chatId: String)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE chatId = :chatId")
    suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChatById(chatId: String)

    @Query("DELETE FROM chats")
    suspend fun clearAllChats()
}

@Dao
interface MyProfileDao {
    @Query("SELECT * FROM my_profile WHERE id = 1 LIMIT 1")
    fun getMyProfileFlow(): Flow<MyProfileEntity?>

    @Query("SELECT * FROM my_profile WHERE id = 1 LIMIT 1")
    suspend fun getMyProfileDirect(): MyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: MyProfileEntity)
}

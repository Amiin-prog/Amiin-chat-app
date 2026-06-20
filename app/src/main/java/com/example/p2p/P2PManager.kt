package com.example.p2p

import android.content.Context
import android.util.Log
import com.example.database.UserEntity
import com.example.repository.OfflineChatRepository
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.UUID

class P2PManager(
    private val context: Context,
    private val repository: OfflineChatRepository
) {
    private val TAG = "P2PManager"
    private val STRATEGY = Strategy.P2P_CLUSTER // or Strategy.P2P_STAR

    private val connectionsClient = Nearby.getConnectionsClient(context)

    private val ioScope = CoroutineScope(Dispatchers.IO + Job())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _connectionNotifications = MutableStateFlow<String?>(null)
    val connectionNotifications: StateFlow<String?> = _connectionNotifications

    private var localUserId = UUID.randomUUID().toString()
    private var localUserName = "Amiin Cabdi"

    init {
        ioScope.launch {
            val myProfile = repository.getMyProfileDirect()
            if (myProfile == null) {
                com.example.database.MyProfileEntity().let { repository.saveMyProfile(it) }
                localUserName = "Amiin Cabdi"
            } else {
                localUserName = myProfile.name
                localUserId = "my_uuid" // or fetch from profile
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val message = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                    ioScope.launch {
                        val users = repository.allUsers.first()
                        val sender = users.find { it.userId == endpointId }
                        val senderName = sender?.name ?: "Unknown Peer"
                        repository.insertNewMessage(
                            chatId = endpointId,
                            senderId = endpointId,
                            senderName = senderName,
                            content = message,
                            messageType = "Text",
                            isIncoming = true
                        )
                    }
                }
                // Later: Handle FILE, STREAM, etc.
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Automatically accept connection for offline chat
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            ioScope.launch {
                val existingUser = repository.allUsers.first().find { it.userId == endpointId }
                if (existingUser == null) {
                    repository.insertUser(UserEntity(
                        userId = endpointId,
                        name = info.endpointName,
                        nickname = info.endpointName,
                        bio = "Discovered via Nearby Connections",
                        avatarIndex = 1,
                        isFavorite = false,
                        isOnline = true,
                        connectionStatus = "Connecting"
                    ))
                }
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    ioScope.launch {
                        repository.updateConnectionStatus(endpointId, "Connected", true)
                        val updatedUser = repository.allUsers.first().find { it.userId == endpointId }
                        if (updatedUser != null) {
                            _connectionNotifications.value = "${updatedUser.name}:Connected"
                        }
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    ioScope.launch { repository.updateConnectionStatus(endpointId, "Disconnected", true) }
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    ioScope.launch { repository.updateConnectionStatus(endpointId, "Disconnected", true) }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            ioScope.launch {
                repository.updateConnectionStatus(endpointId, "Disconnected", false)
                val updatedUser = repository.allUsers.first().find { it.userId == endpointId }
                if (updatedUser != null) {
                    _connectionNotifications.value = "${updatedUser.name}:Disconnected"
                }
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            ioScope.launch {
                val existing = repository.allUsers.first().find { it.userId == endpointId }
                if (existing == null) {
                    repository.insertUser(UserEntity(
                        userId = endpointId,
                        name = info.endpointName,
                        nickname = info.endpointName,
                        bio = "Discovered via Nearby Connections",
                        avatarIndex = 1,
                        isFavorite = false,
                        isOnline = true,
                        connectionStatus = "Disconnected"
                    ))
                } else {
                    repository.updateConnectionStatus(endpointId, "Disconnected", true)
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            ioScope.launch {
                repository.updateConnectionStatus(endpointId, "Disconnected", false)
            }
        }
    }

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            context.packageName,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started successfully")
        }.addOnFailureListener {
            Log.e(TAG, "Discovery failed to start")
            _isScanning.value = false
        }
        
        startAdvertising()
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            localUserName,
            context.packageName,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully")
        }.addOnFailureListener {
            Log.e(TAG, "Advertising failed to start")
        }
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        connectionsClient.stopAdvertising()
        _isScanning.value = false
    }

    fun connectToPeer(userId: String, onConnected: () -> Unit = {}) {
        ioScope.launch {
            repository.updateConnectionStatus(userId, "Connecting", true)
        }
        connectionsClient.requestConnection(localUserName, userId, connectionLifecycleCallback)
            .addOnSuccessListener {
                onConnected()
            }
            .addOnFailureListener {
                ioScope.launch {
                    repository.updateConnectionStatus(userId, "Disconnected", true)
                }
            }
    }

    fun disconnectFromPeer(userId: String) {
        connectionsClient.disconnectFromEndpoint(userId)
        ioScope.launch {
            repository.updateConnectionStatus(userId, "Disconnected", false)
        }
    }

    fun sendPayload(endpointId: String, payloadBytes: ByteArray) {
        val payload = Payload.fromBytes(payloadBytes)
        connectionsClient.sendPayload(endpointId, payload)
    }

    fun clearNotification() {
        _connectionNotifications.value = null
    }
}

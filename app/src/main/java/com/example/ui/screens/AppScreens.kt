@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)
package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.database.ChatEntity
import com.example.database.MessageEntity
import com.example.database.UserEntity
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Global custom translations resolver. Handles instant Somali/English switches instantly.
 */
@Composable
fun translate(en: String, so: String, viewModel: MainViewModel): String {
    val currentLang by viewModel.appLanguage.collectAsState()
    return if (currentLang == "so") so else en
}

/**
 * Beautiful, lightweight Vector Avatar Generator that draws color gradients dynamically.
 * Eliminates large raster image requirements for reliable offline display.
 */
@Composable
fun UserAvatar(avatarIndex: Int, sizeDp: Dp = 48.dp, modifier: Modifier = Modifier) {
    val initialLabel = when (avatarIndex) {
        1 -> "F"
        2 -> "A"
        3 -> "G"
        4 -> "AC"
        5 -> "D"
        99 -> "👥"
        else -> "Me"
    }

    val gradientColors = when (avatarIndex) {
        1 -> listOf(Color(0xFF0D9488), Color(0xFF14B8A6)) // Teal/Emerald (Farah)
        2 -> listOf(Color(0xFFD97706), Color(0xFFF59E0B)) // Amber (Aisha)
        3 -> listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6)) // Ocean Blue (Guled)
        4 -> listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6)) // Purple (Amiin)
        5 -> listOf(Color(0xFFBE123C), Color(0xFFF43F5E)) // Rose (Deqa)
        99 -> listOf(Color(0xFF0F766E), Color(0xFF059669)) // Multi Group indicator
        else -> listOf(Color(0xFF475569), Color(0xFF64748B)) // Deep Slate (Me)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialLabel,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp.value * 0.45f).sp
            )
        )
    }
}

/**
 * Signal Strength connection quality drawer.
 */
@Composable
fun SignalQualityIndicator(quality: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..5) {
            val barHeight = (i * 3).dp
            val isFilled = i <= quality
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.2f
                        )
                    )
            )
        }
    }
}

/**
 * Main entrance router of the App screens.
 */
@Composable
fun AppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val notificationState by viewModel.connectionNotifications.collectAsState()
    val context = LocalContext.current

    // Trigger local standard notifications / toast announcements
    LaunchedEffect(notificationState) {
        notificationState?.let { combinedString ->
            val parts = combinedString.split(":")
            if (parts.size == 2) {
                val peerName = parts[0]
                val action = parts[1]
                val alertText = if (action == "Connected") {
                    "$peerName " + if (viewModel.appLanguage.value == "so") "waa uu xirmay!" else "has connected!"
                } else {
                    "$peerName " + if (viewModel.appLanguage.value == "so") "waa uu go'ay." else "has disconnected."
                }
                Toast.makeText(context, alertText, Toast.LENGTH_SHORT).show()
                viewModel.p2pManager.clearNotification()
            }
        }
    }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            is Screen.Splash -> SplashScreen(viewModel)
            is Screen.Onboarding -> OnboardingScreen(viewModel)
            is Screen.FirstLaunch -> FirstLaunchScreen(viewModel)
            is Screen.Dashboard -> DashboardScreen(viewModel)
            is Screen.Discovery -> DiscoveryScreen(viewModel)
            is Screen.Chat -> ChatDetailScreen(viewModel, screen.chatId)
            is Screen.ProfileEdit -> ProfileEditScreen(viewModel)
            is Screen.Settings -> SettingsScreen(viewModel)
            is Screen.About -> AboutScreen(viewModel)
        }
    }
}

@Composable
fun SplashScreen(viewModel: MainViewModel) {
    val profileState by viewModel.myProfile.collectAsState()

    LaunchedEffect(profileState) {
        // Aesthetic branding delay of 1800ms
        delay(1800)
        val profile = profileState
        if (profile != null) {
            if (profile.name == "Amiin Cabdi" && profile.avatarIndex == 0) {
                viewModel.navigateTo(Screen.Onboarding)
            } else {
                viewModel.navigateTo(Screen.Dashboard)
            }
        } else {
            // Safe fallback if database loaded state remains null (e.g. delays or errors)
            viewModel.navigateTo(Screen.Dashboard)
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.OfflineShare,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Amiin Offline Chat",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Developed by Amiin Cabdi © 2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(viewModel: MainViewModel) {
    var page by remember { mutableStateOf(0) }
    val title = when (page) {
        0 -> translate("Welcome to Amiin Offline Chat", "Ku Soo Dhawoow Amiin Chat", viewModel)
        1 -> translate("Decentralized P2P Mesh", "Network-ka Direct-ka ah", viewModel)
        else -> translate("Pairing Wallpapers & QR", "Koodhka QR & Wallpaper-ka", viewModel)
    }
    val desc = when (page) {
        0 -> translate("Modern servers-free messaging between nearby devices without mobile data, cell towers, or internet signals.", "Ku wada xiriir fariimo secure ah adigoon u baahnayn internet, munaaraddo ama xogta mobilka.", viewModel)
        1 -> translate("Connects automatically using dual systems of Bluetooth Low Energy (BLE) and high-speed Wi-Fi Direct.", "Wuxuu si toos ah isugu xiraa aaladaha is dhow adoo isticmaalaya xoogga Bluetooth iyo Wi-Fi Direct.", viewModel)
        else -> translate("Scan instant QR codes to pair securely isochronous connections and choose beautiful custom background wallpapers.", "Iska baar koodhka QR si aad u xirato meel kasta, adoo dooranaya wallpapers muuqaal ahaan aad u qurux badan.", viewModel)
    }
    val icon = when (page) {
        0 -> Icons.Default.Chat
        1 -> Icons.Default.WifiOff
        else -> Icons.Default.Wallpaper
    }
    
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = desc, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(48.dp))
            
            // Indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { d ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (d == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row {
                if (page > 0) {
                    TextButton(onClick = { page-- }) {
                        Text(translate("Back", "Dib", viewModel))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Button(onClick = {
                    if (page < 2) page++
                    else viewModel.navigateTo(Screen.FirstLaunch)
                }) {
                    Text(if (page < 2) translate("Next", "Xiga", viewModel) else translate("Get Started", "Biloow", viewModel))
                }
            }
        }
    }
}

@Composable
fun FirstLaunchScreen(viewModel: MainViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val currentLang by viewModel.appLanguage.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.OfflineShare,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Amiin Offline Chat",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Waxaa sifeeyay Amiin Cabdi • Developer credit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = if (currentLang == "so") "Dooro Luuqadda App-ka" else "Select App Language",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Somali Button
                OutlinedListItemButton(
                    label = "Somali (SO)",
                    isSelected = currentLang == "so",
                    onClick = { viewModel.updateLanguage("so") }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // English Button
                OutlinedListItemButton(
                    label = "English (EN)",
                    isSelected = currentLang == "en",
                    onClick = { viewModel.updateLanguage("en") }
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = { viewModel.navigateTo(Screen.Dashboard) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("get_started_btn"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (currentLang == "so") "Bilow App-ka ➔" else "Get Started ➔",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OutlinedListItemButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.3f
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val chatsList by viewModel.chats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()

    var activeSearchTab by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Amiin Offline Chat",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = translate("Serverless P2P • Developed by Amiin Cabdi", "Xiriir aan internet lahayn • Amiin Cabdi", viewModel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.ProfileEdit) }) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Edit Profile")
                    }
                    IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.Discovery) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("radar_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Radar, contentDescription = "Scan")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = translate("Scan", "Raadi", viewModel),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Outlined message search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchChatMessages(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_field"),
                placeholder = {
                    Text(text = translate("Search offline chats...", "Raadi fariimaha...", viewModel))
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "SearchIcon")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchChatMessages("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "ClearIcon")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            // Decentralized Broadcast Quick Action Banner
            var showBroadcastDialog by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Broadcast",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = translate("P2P Open Broadcast Channel", "Kanal-ka furan ee agagaarka", viewModel),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = translate("Announce to all nearby devices running Amiin Chat instantly.", "U dir fariin degdeg ah dhamaan aaladaha kugu dhow.", viewModel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showBroadcastDialog = true }) {
                        Text(text = translate("BROADCAST", "GUDHI", viewModel), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            if (showBroadcastDialog) {
                var broadcastInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showBroadcastDialog = false },
                    title = { Text(text = translate("Send Open Broadcast", "U dir Fariin Dadweyne", viewModel)) },
                    text = {
                        Column {
                            Text(
                                text = translate("This message will be transmitted to all listening nodes nearby without encryption.", "Fariintan waa la wadaagi doonaa dhamaan aaladaha ku dhow adigoon sir-gudbin.", viewModel),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = broadcastInput,
                                onValueChange = { broadcastInput = it },
                                placeholder = { Text(text = translate("Emergency / Signal Note...", "Qor fariinta hadda...", viewModel)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (broadcastInput.isNotEmpty()) {
                                    viewModel.sendBroadcastOfflineMessage(broadcastInput)
                                    showBroadcastDialog = false
                                }
                            }
                        ) {
                            Text(text = translate("Broadcast", "Gudbi", viewModel))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBroadcastDialog = false }) {
                            Text(text = translate("Cancel", "Ka laabo", viewModel))
                        }
                    }
                )
            }

            // Direct local communication filter bar
            QuickFilterNavigationRow(viewModel)

            if (chatsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = translate(
                                "No active chats yet. Connect to a nearby Bluetooth or Wi-Fi Direct user from scanning panel to start securely chatting!",
                                "Sheekooyin firfircoon ma jiraan. Fadlan ku fur fariin cusub adigoo isticmaalaya awoodaha Bluetooth ama Wi-Fi ee ku dhow!",
                                viewModel
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val filteredChats = chatsList.filter {
                    it.chatName.contains(searchQuery, ignoreCase = true) ||
                            it.lastMessage.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) {
                    items(filteredChats) { chat ->
                        ChatListItem(chat, viewModel) {
                            viewModel.navigateTo(Screen.Chat(chat.chatId))
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickFilterNavigationRow(viewModel: MainViewModel) {
    val favorites by viewModel.favoritePeers.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()

    if (favorites.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = translate("Favorite Contacts", "Lagu Jecelyahay", viewModel),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp) // Limit list height to fit fluidly
            ) {
                items(favorites) { peer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.Chat(peer.userId)) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(avatarIndex = peer.avatarIndex, sizeDp = 36.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = peer.name,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = peer.bio,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(chat: ChatEntity, viewModel: MainViewModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { viewModel.togglePinChat(chat.chatId, chat.isPinned) }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(avatarIndex = chat.avatarIndex, sizeDp = 52.dp)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = chat.chatName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                val formattedTime = remember(chat.lastMessageTime) {
                    val date = java.util.Date(chat.lastMessageTime)
                    val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    format.format(date)
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryScreen(viewModel: MainViewModel) {
    val usersList by viewModel.users.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.p2pManager.startDiscovery()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = translate("Nearby Discovery", "Aaladaha Ku Dhow", viewModel)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    } else {
                        IconButton(onClick = { viewModel.p2pManager.startDiscovery() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Scan")
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Infinite wave simulation radar component
            if (isScanning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadarPulseAnim()
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = translate("Scanning for Bluetooth & Wi-Fi peers...", "Tirinta isticmaalayaasha ee Bluetooth...", viewModel),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = translate("Ensure nearby gadgets are running Amiin Chat", "Hubi in qalabka kale ee kuu dhow uu furan yahay", viewModel),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Sandbox explanation header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = translate(
                            "Demo Sandbox: This app simulates live offline P2P sockets to demonstrate responsive chatting on emulation hardware.",
                            "Tijaabada Sandbox: App-kan wuxuu matalaa Bluetooth-ka iyo Wi-Fi si uu kuu tuso fariimaha offline-ka iyada oo mashiinka emulator la isticmaalayo.",
                            viewModel
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Premium Discovery Actions Panel: QR Pairing & Mesh Map
            var showQRDialog by remember { mutableStateOf(false) }
            var showMeshMap by remember { mutableStateOf(false) }
            val myProfile by viewModel.myProfile.collectAsState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showMeshMap = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = translate("Mesh Map", "Muuqaalka Naqshada", viewModel), fontSize = 11.sp)
                }

                Button(
                    onClick = { showQRDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = translate("QR Pairing", "Isku Xirka QR", viewModel), fontSize = 11.sp)
                }
            }

            // QR Pairing dialog simulator
            if (showQRDialog) {
                var qrScanningMode by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = { showQRDialog = false },
                    title = { Text(text = translate("Device Pairing Code", "Koodhka Isku Xirka", viewModel)) },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!qrScanningMode) {
                                Text(
                                    text = translate("Let your friend scan this code to link securely offline.", "U ogolaan saaxiibkaa inuu koodhkan ka sawiro si aad u xirantaan.", viewModel),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                // Drawing a neat mock QR Code matrix box
                                Card(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            // Mock matrix blocks
                                            drawRect(color = Color.Black, size = size / 3f)
                                            drawRect(color = Color.Black, size = size / 3f, topLeft = Offset(size.width * 2/3, 0f))
                                            drawRect(color = Color.Black, size = size / 3f, topLeft = Offset(0f, size.height * 2/3))
                                            drawRect(color = Color.Black, size = size / 5f, topLeft = Offset(size.width / 2, size.height / 2))
                                            drawRect(color = Color.Black, size = size / 6f, topLeft = Offset(size.width * 0.4f, size.height * 0.1f))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = { qrScanningMode = true }) {
                                    Text(text = translate("➔ Switch to Scanner Mode", "➔ Fur Kamarada Sawirka", viewModel), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = translate("Align buddy's QR code within the framing bounds.", "Ku beeg koodhka QR ee saaxiibkaa dhexda kamarada.", viewModel),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                // Mock Camera Scanner box
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .background(Color.DarkGray, RoundedCornerShape(12.dp))
                                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val infiniteTransition = rememberInfiniteTransition()
                                    val scanProgress by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 180f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        // Drawing neon scanning overlay ray line
                                        drawLine(
                                            color = Color(0xFF00FFCC),
                                            start = Offset(0f, scanProgress),
                                            end = Offset(size.width, scanProgress),
                                            strokeWidth = 4f
                                        )
                                    }
                                    Icon(imageVector = Icons.Default.CropFree, contentDescription = "Focus", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(60.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        Toast.makeText(
                                            viewModel.p2pManager.javaClass.getDeclaredField("context").apply { isAccessible = true }.get(viewModel.p2pManager) as android.content.Context,
                                            if (viewModel.appLanguage.value == "so") "Aalada waa la helay oo waa la isku xiray!" else "Device identified & paired!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showQRDialog = false
                                    }
                                ) {
                                    Text(text = translate("Simulate Scan Found", "Matalaad Toos Ah", viewModel))
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showQRDialog = false }) {
                            Text(text = translate("Close", "Xir", viewModel))
                        }
                    }
                )
            }

            // Mesh Map overlay dialog
            if (showMeshMap) {
                AlertDialog(
                    onDismissRequest = { showMeshMap = false },
                    title = { Text(text = translate("P2P Mesh Topology Diagram", "Naqshada Khariirada P2P", viewModel)) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = translate("Live mesh nodes mapping around you (Central amber beacon).", "Heerka awooda iyo fogaanta aaladaha kuu dhow.", viewModel),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp),
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(8.dp)
                            ) {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val secondaryColor = MaterialTheme.colorScheme.secondary
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cen = Offset(size.width / 2, size.height / 2)
                                    // Me center dot
                                    drawCircle(color = Color(0xFFFFB300), radius = 18f, center = cen)
                                    
                                    // Radars concentric circles
                                    drawCircle(
                                        color = primaryColor.copy(alpha = 0.15f),
                                        radius = 50f,
                                        center = cen,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                    )
                                    drawCircle(
                                        color = primaryColor.copy(alpha = 0.08f),
                                        radius = 95f,
                                        center = cen,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                    )

                                    // Nearby nodes links
                                    usersList.filter { it.userId != "group_chat" }.forEachIndexed { idx, peer ->
                                        val angle = (idx * 360f / maxOf(1, usersList.size - 1)) * (Math.PI / 180.0)
                                        val nodeDist = if (peer.signalStrength >= 4) 48f else 90f
                                        val targetPos = Offset(
                                            cen.x + (nodeDist * Math.cos(angle)).toFloat() * 1.8f,
                                            cen.y + (nodeDist * Math.sin(angle)).toFloat() * 1.8f
                                        )
                                        // Draw connecting vector
                                        drawLine(
                                            color = primaryColor.copy(alpha = 0.35f),
                                            start = cen,
                                            end = targetPos,
                                            strokeWidth = 3f
                                        )
                                        // Pulse node dot
                                        drawCircle(color = secondaryColor, radius = 12f, center = targetPos)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showMeshMap = false }) {
                            Text(text = translate("Close Map", "Xir", viewModel))
                        }
                    }
                )
            }

            // Nearby Recommendations Card
            val highestSignalPeer = usersList.filter { it.userId != "group_chat" }.maxByOrNull { it.signalStrength }
            if (highestSignalPeer != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.OfflineBolt, contentDescription = "Strongest Link", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = translate("Recommended Faster Offline Connection", "Kanaalka kugu dhow ee ugu dheereeya", viewModel),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = translate("Connect instantly with ${highestSignalPeer.name} (Estimated range ~3m, Max throughput)", "Kula xiriir ${highestSignalPeer.name} si dhakhso ah adigoo dhimaya awood lumiska.", viewModel),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Text(
                text = translate("Discovered Devices", "Aaladaha la Helay", viewModel),
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            val nearbyFiltered = usersList.filter { it.userId != "group_chat" }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(nearbyFiltered) { peer ->
                    DiscoveryPeerItem(peer, viewModel)
                }
            }
        }
    }
}

@Composable
fun RadarPulseAnim() {
    val infiniteTransition = rememberInfiniteTransition()
    val radiusRatio by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val opacity by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val colorScheme = MaterialTheme.colorScheme

    Canvas(modifier = Modifier.size(48.dp)) {
        val center = size.minDimension / 2
        // Background static core
        drawCircle(
            color = colorScheme.primary,
            radius = center * 0.4f
        )
        // Dispersing ripples
        drawCircle(
            color = colorScheme.primary.copy(alpha = opacity),
            radius = center * radiusRatio,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }
}

@Composable
fun DiscoveryPeerItem(peer: UserEntity, viewModel: MainViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(avatarIndex = peer.avatarIndex, sizeDp = 48.dp)
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "@" + peer.nickname,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = peer.bio,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalQualityIndicator(peer.signalStrength)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = translate(
                            "Status: ${peer.connectionStatus}",
                            "Heerka: ${translateConnectionStatus(peer.connectionStatus)}",
                            viewModel
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action connect button
            val isConnecting = peer.connectionStatus == "Connecting"
            val isConnected = peer.connectionStatus == "Connected"

            Button(
                onClick = {
                    if (isConnected) {
                        viewModel.p2pManager.disconnectFromPeer(peer.userId)
                    } else if (!isConnecting) {
                        viewModel.p2pManager.connectToPeer(peer.userId)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isConnected) {
                        translate("Leave", "Ka Saar", viewModel)
                    } else if (isConnecting) {
                        translate("...", "Sug...", viewModel)
                    } else {
                        translate("Connect", "Xiriiri", viewModel)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun translateConnectionStatus(status: String): String {
    return when (status) {
        "Connected" -> "Waa ku xiranyahay"
        "Connecting" -> "La xiriirinayaa"
        "Disconnected" -> "La gooyay"
        else -> status
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ChatDetailScreen(viewModel: MainViewModel, chatId: String) {
    val messages by viewModel.activeChatMessages.collectAsState()
    val usersList by viewModel.users.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val activePlayingUri by viewModel.isPlayingAudio.collectAsState()
    val activeWallpaperIndex by viewModel.chatWallpaperIndex.collectAsState()
    val typingStatus by viewModel.typingStatus.collectAsState()

    val currentPeer = remember(usersList) { usersList.find { it.userId == chatId } }
    val peerName = currentPeer?.name ?: if (chatId == "group_chat") "Guurti Offline Group" else translate("Chamber Host", "Xiriiriye", viewModel)
    val avatarIndex = currentPeer?.avatarIndex ?: (if (chatId == "group_chat") 99 else 0)
    val isBlocked = currentPeer?.isBlocked ?: false
    val isFavorite = currentPeer?.isFavorite ?: false
    val isTyping = typingStatus[chatId] == true

    var typedText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Smooth scroll down when typing or receiving fariimo
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(avatarIndex = avatarIndex, sizeDp = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = peerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val statusStr = if (isTyping) {
                                translate("Typing...", "Wuu qorayaa...", viewModel)
                            } else if (currentPeer?.isOnline == true) {
                                val bars = "📶" + "•".repeat(currentPeer.signalStrength)
                                translate("Secure offline • $bars", "Khad ammaan ah • $bars", viewModel)
                            } else {
                                translate("Disconnected", "La gooyay", viewModel)
                            }
                            Text(
                                text = statusStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isTyping) {
                                    Color(0xFFFFB300)
                                } else if (currentPeer?.isOnline == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Red
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Chat Wallpaper direct toggle painter roller icon
                    IconButton(onClick = { viewModel.updateWallpaper((activeWallpaperIndex + 1) % 4) }) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = "Rotate Wallpaper",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (chatId != "group_chat") {
                        // Toggle Favorite Action
                        IconButton(onClick = { viewModel.toggleFavorite(chatId, isFavorite) }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFFB300) else LocalContentColor.current
                            )
                        }
                        // Toggle Block Action
                        IconButton(onClick = {
                            if (isBlocked) {
                                viewModel.unblockUser(chatId)
                            } else {
                                viewModel.blockUser(chatId)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Block",
                                tint = if (isBlocked) Color.Red else LocalContentColor.current
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isBlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.15f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = translate(
                            "You blocked this contact. Unblock them to continue offline conversations.",
                            "Waad block-gareysay qofkan. Ka saar block-ka si aad u sii wadato wada hadalka offline-ka ah.",
                            viewModel
                        ),
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Message Scroll area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Drawing custom selected pattern Wallpapers
                if (activeWallpaperIndex == 1) { // Starry Cosmic wallpaper overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color(0xFF0F172A)) // Night slate blue
                        val rand = java.util.Random(77)
                        repeat(35) {
                            drawCircle(
                                color = Color(0xFFFFD700).copy(alpha = rand.nextFloat() * 0.7f + 0.3f),
                                radius = rand.nextFloat() * 4f + 1f,
                                center = Offset(rand.nextFloat() * size.width, rand.nextFloat() * size.height)
                            )
                        }
                    }
                } else if (activeWallpaperIndex == 2) { // Mint Grid wallpaper overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color(0xFFECFDF5)) // Emerald mint
                        val space = 44f
                        for (w in 0..(size.width / space).toInt()) {
                            drawLine(
                                color = Color(0xFF10B981).copy(alpha = 0.08f),
                                start = Offset(w * space, 0f),
                                end = Offset(w * space, size.height),
                                strokeWidth = 1f
                            )
                        }
                        for (h in 0..(size.height / space).toInt()) {
                            drawLine(
                                color = Color(0xFF10B981).copy(alpha = 0.08f),
                                start = Offset(0f, h * space),
                                end = Offset(size.width, h * space),
                                strokeWidth = 1f
                            )
                        }
                    }
                } else if (activeWallpaperIndex == 3) { // Warm Peach Sunset overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFFFEF3C7), Color(0xFFFCE7F3))
                            )
                        )
                    }
                }

                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = translate(
                                "No messages in secure chat archive.\nType a message and hit send to test offline delivery!",
                                "Ma jiraan fariimo ku jira kaydka la sifeeyay.\nQor fariin si aad u tijaabiso dhalinta fariimaha!",
                                viewModel
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (activeWallpaperIndex == 1) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(
                                message = message,
                                activePlayingUri = activePlayingUri,
                                onVoicePlayClick = { uri -> viewModel.playVoiceMessage(uri) },
                                onReactionSelect = { msgId, reaction -> viewModel.updateMessageReaction(msgId, reaction) }
                            )
                        }
                    }
                }

                // Smooth live recording countdown overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = isRecording,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(16.dp).height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadarPulseRecording()
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = translate("LIVE AUDIO RECORDING...", "WAA DUUBAYAA COD LIVE AH...", viewModel),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Input Bar Row
            if (!isBlocked) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick file attachment features
                        IconButton(onClick = { viewModel.sendImageAttachment(chatId) }) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = "Send Image")
                        }

                        IconButton(onClick = { viewModel.sendDocAttachment(chatId) }) {
                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Send Doc")
                        }

                        IconButton(onClick = { viewModel.sendVideoAttachment(chatId) }) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = "Send Video")
                        }

                        // Hold to record live voice button
                        Box(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            viewModel.startVoiceRecording()
                                            tryAwaitRelease()
                                            viewModel.stopVoiceRecording(chatId)
                                        }
                                    )
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Voice",
                                tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Text input field
                        TextField(
                            value = typedText,
                            onValueChange = { typedText = it },
                            placeholder = {
                                Text(
                                    text = translate("Type safe message...", "Qor fariin ammaan ah...", viewModel),
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_field"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                viewModel.sendTextMessage(chatId, typedText)
                                typedText = ""
                            })
                        )

                        // Floating circular Send icon trigger
                        IconButton(
                            onClick = {
                                if (typedText.isNotEmpty()) {
                                    viewModel.sendTextMessage(chatId, typedText)
                                    typedText = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("send_tag_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadarPulseRecording() {
    val infiniteTransition = rememberInfiniteTransition()
    val width by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Canvas(modifier = Modifier.size(16.dp)) {
        drawCircle(
            color = Color.White,
            radius = size.minDimension / 2 * width
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    activePlayingUri: String?,
    onVoicePlayClick: (String) -> Unit,
    onReactionSelect: (Long, String?) -> Unit
) {
    val isMyMessage = !message.isIncoming
    val alignment = if (isMyMessage) Alignment.End else Alignment.Start

    val containerColor = if (isMyMessage) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isMyMessage) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val shape = if (isMyMessage) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    var showReactions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        // Sender name for group chats
        if (message.chatId == "group_chat" && !isMyMessage) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp),
                fontWeight = FontWeight.Bold
            )
        }

        // Floating interactive reaction stickers bar
        androidx.compose.animation.AnimatedVisibility(
            visible = showReactions,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.padding(bottom = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("👍", "❤️", "😂", "😮", "😢").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clickable {
                                    onReactionSelect(message.id, if (message.reaction == emoji) null else emoji)
                                    showReactions = false
                                }
                                .padding(2.dp)
                        )
                    }
                }
            }
        }

        Box {
            Surface(
                color = containerColor,
                contentColor = contentColor,
                shape = shape,
                tonalElevation = 2.dp,
                modifier = Modifier.combinedClickable(
                    onClick = { showReactions = !showReactions },
                    onLongClick = { showReactions = true }
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .widthIn(max = 280.dp)
                ) {
                    when (message.messageType) {
                        "Image" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, size = 18.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message.fileName ?: "photo.jpg",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Wallpaper,
                                        contentDescription = null,
                                        tint = contentColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = message.fileSize ?: "1.2 MB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        "Video" -> { // New Video message support
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Videocam, contentDescription = null, size = 18.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message.fileName ?: "video.mp4",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = message.fileSize ?: "12.4 MB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        "Document" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(contentColor.copy(alpha = 0.1f))
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF",
                                    tint = if (isMyMessage) Color.White else Color.Red,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName ?: "doc.pdf",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = message.fileSize ?: "400 KB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        "Voice" -> {
                            val isPlayingThis = activePlayingUri == message.fileUri
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(contentColor.copy(alpha = 0.1f))
                                    .padding(8.dp)
                            ) {
                                IconButton(onClick = { message.fileUri?.let { onVoicePlayClick(it) } }) {
                                    Icon(
                                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = contentColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isPlayingThis) "Playing voice..." else "🎤 Voice Message",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = message.fileSize ?: "120 KB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        else -> {
                            // Standard text bubble
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom time and status details
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val formattedTime = remember(message.timestamp) {
                            val date = java.util.Date(message.timestamp)
                            val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                            format.format(date)
                        }

                        Text(
                            text = formattedTime,
                            fontSize = 9.sp,
                            color = contentColor.copy(alpha = 0.6f)
                        )

                        if (isMyMessage) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = when (message.status) {
                                    "Read" -> Icons.Default.DoneAll
                                    "Delivered" -> Icons.Default.DoneAll
                                    else -> Icons.Default.Done
                                },
                                contentDescription = message.status,
                                tint = if (message.status == "Read") Color(0xFF00E5FF) else contentColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Small overlapping floating bubble reaction sticker badge
            if (message.reaction != null) {
                Card(
                    modifier = Modifier
                        .align(if (isMyMessage) Alignment.BottomStart else Alignment.BottomEnd)
                        .offset(y = 10.dp, x = if (isMyMessage) (-8).dp else 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = message.reaction,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Icon(imageVector: ImageVector, contentDescription: Nothing?, size: Dp) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}

@Composable
fun ProfileEditScreen(viewModel: MainViewModel) {
    val myProfile by viewModel.myProfile.collectAsState()
    val scope = rememberCoroutineScope()

    var nameInput by remember { mutableStateOf("") }
    var nicknameInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf("") }
    var selectAvatarIndex by remember { mutableStateOf(0) }

    LaunchedEffect(myProfile) {
        myProfile?.let {
            nameInput = it.name
            nicknameInput = it.nickname
            bioInput = it.bio
            selectAvatarIndex = it.avatarIndex
        }
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = translate("Edit User Profile", "Cusbooneysii Profile-ka", viewModel)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(avatarIndex = selectAvatarIndex, sizeDp = 100.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = translate("Select Avatar Profile Icon", "Dooro canbarka Profile-ka", viewModel),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar select list
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0..5) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                color = if (selectAvatarIndex == i) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectAvatarIndex = i }
                    ) {
                        UserAvatar(avatarIndex = i, sizeDp = 40.dp, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text(text = translate("Full Name", "Magaca oo Buuxa", viewModel)) },
                modifier = Modifier.fillMaxWidth().testTag("profile_name_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nicknameInput,
                onValueChange = { nicknameInput = it },
                label = { Text(text = translate("Nickname", "Naaneysta", viewModel)) },
                modifier = Modifier.fillMaxWidth().testTag("profile_nickname_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = bioInput,
                onValueChange = { bioInput = it },
                label = { Text(text = translate("Bio / About", "La kulan / Bio", viewModel)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    viewModel.updateProfile(nameInput, nicknameInput, bioInput, selectAvatarIndex)
                    Toast.makeText(
                        context,
                        if (viewModel.appLanguage.value == "so") "Profile-ka si fiican baa loo kaydiyay!" else "Profile successfully saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.navigateTo(Screen.Dashboard)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_profile_btn"),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = translate("SAVE PROFILE", "KAYDI PROFILE-KA", viewModel),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val myProfile by viewModel.myProfile.collectAsState()
    val batterySaving by viewModel.batterySavingMode.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = translate("App Configuration", "Dejinta App-ka", viewModel), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category 1: Localization & Language Settings
            item {
                Text(
                    text = translate("1. LANGUAGE & VOICE", "1. LUUQADA & HADALKA", viewModel),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = translate("Active Language", "Luuqada App-ka", viewModel),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedListItemButton(
                                label = "Somali (Soomaali)",
                                isSelected = viewModel.appLanguage.value == "so",
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.updateLanguage("so")
                            }

                            OutlinedListItemButton(
                                label = "English (US)",
                                isSelected = viewModel.appLanguage.value == "en",
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.updateLanguage("en")
                            }
                        }
                    }
                }
            }

            // Category 2: Visual Themes & Personalization
            item {
                Text(
                    text = translate("2. THEME & DECORATION", "2. MUUQAALKA & BILICDA", viewModel),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = translate("Dark & Light Themes", "Muuqaalka & Indhaha Theme", viewModel),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.updateTheme("light") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.appTheme.value == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (viewModel.appTheme.value == "light") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = translate("Light", "Iftiin", viewModel), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.updateTheme("dark") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.appTheme.value == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (viewModel.appTheme.value == "dark") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = translate("Dark", "Madow", viewModel), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.updateTheme("system") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.appTheme.value == "system") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (viewModel.appTheme.value == "system") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = translate("System", "Auto", viewModel), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Category 3: Power Optimization
            item {
                Text(
                    text = translate("3. POWER & MESH OPTIMIZATION", "3. CODSIGA AWOODDA & BATTERIGA", viewModel),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = if (batterySaving) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = translate("Battery Saving Mesh Mode", "Habka Badbaadinta Batteriga", viewModel),
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = translate("Reduces peer ping frequencies to save power.", "Wuxuu yareeyaa baaritaanka si loo badbaadiyo dabka.", viewModel),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = batterySaving,
                            onCheckedChange = { viewModel.toggleBatterySaving(it) }
                        )
                    }
                }
            }

            // Category 4: Security Backups & Local Datastore
            item {
                Text(
                    text = translate("4. CRYPTO BACKUPS & MAINTENANCE", "4. SHEEKOOYINKA & database-KA", viewModel),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = translate("Secured Local Data Archives", "Kaydinta & Isku Xidhka AES-256", viewModel),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = translate("Encrypt conversations offline with SQLite SQLCipher structure. Backup your files periodically.", "Cunfari fariimaha hadda adigoo isticmaalaya sqlite. Iska kaydi gabi ahaanba sheekooyinka.", viewModel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.triggerLocalBackup()
                                    Toast.makeText(
                                        context,
                                        if (viewModel.appLanguage.value == "so") "Sheekooyinka waa la kaydiyay!" else "Backup saved locally!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = translate("Backup", "Kaydi", viewModel), fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.triggerLocalRestore()
                                    Toast.makeText(
                                        context,
                                        if (viewModel.appLanguage.value == "so") "Sheekooyinkii hore waa la soo celiyay!" else "Backup restored successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = translate("Restore", "Soo Celi", viewModel), fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Clear database warning button
                        Button(
                            onClick = { showClearConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = translate("Wipe Offline Chat Databases", "Tirtir dhamaan sheekooyinka", viewModel),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Developer redirection item and credits
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(Screen.About) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = translate("Creator & Developer", "Ku Saabsan Creator-ka", viewModel),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Amiin Cabdi (Founder & Lead Developer)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amiin Offline Chat • Version 2.0.0 Stable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Developed by Amiin Cabdi © 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = translate("Clear history?", "Mawduuca miyaa la tirtirayaa?", viewModel)) },
            text = {
                Text(
                    text = translate(
                        "Are you absolutely sure you want to clean and wipe the offline Room database backup archives? This process is irreversible.",
                        "Ma hubtaa inaad rabto inaad gabi ahaanba tirtirto kaydka fariimaha ee database-ka maxalliga ah? Tani dib looma soo celin karo.",
                        viewModel
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllChatHistory()
                        showClearConfirm = false
                        Toast.makeText(
                            context,
                            if (viewModel.appLanguage.value == "so") "Urgurti waa la tirtiray dhammaan!" else "All database messages cleared!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = translate("Confirm Wipe", "Tirtir", viewModel))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(text = translate("Cancel", "Iska Dhaf", viewModel))
                }
            }
        )
    }
}

@Composable
fun AboutScreen(viewModel: MainViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = translate("About Developer", "Ku Saabsan Creator", viewModel)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Amiin Offline Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Developed by Amiin Cabdi © 2026",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "v1.0.0 (Production Stable)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = translate("Developer", "Sameeyaha", viewModel),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Amiin Cabdi\nFounder and Lead Developer",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Added Developer Contact Information
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+252613446028",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "amenabdisaeed@gmail.com",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = translate("Biography", "Taariikh", viewModel),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = translate(
                                "Amiin Cabdi is a technology enthusiast and software creator with a strong interest in programming, digital innovation, language technology, and educational software. His goal is to create useful applications that solve real-world problems and improve communication for Somali-speaking communities and users around the world. Through Amiin Offline Chat, he aims to provide a reliable communication platform that works even when internet access is unavailable.",
                                "Amiin Cabdi waa qof xiiseeya tiknoolajiyada iyo horumarinta software-ka. Wuxuu danayn gaar ah u leeyahay barnaamijyada kombiyuutarka, hal-abuurka dijitaalka ah, tiknoolajiyada luqadaha, iyo barnaamijyada waxbarashada. Ujeeddadiisu waa inuu abuuro apps waxtar leh oo xalliya dhibaatooyinka dhabta ah isla markaana fududeeya isgaarsiinta bulshada Soomaaliyeed iyo isticmaalayaasha kale ee dunida. App-kan Amiin Offline Chat wuxuu u sameeyay si dadka isu dhow ay ugu wada xiriiri karaan xitaa marka internet uusan jirin.",
                                viewModel
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = translate("P2P Technologies Used", "Tiknoolajiyada loo Adeegsaday", viewModel),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Bluetooth Low Energy / Classic Broadcasts\n" +
                                    "• Wi-Fi Direct Peer-to-Peer Sockets\n" +
                                    "• Local AES-128 Message Ciphers\n" +
                                    "• Room Offline Database Engine\n" +
                                    "• Jetpack State flow-reactive Architecture",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = translate(
                        "Securely crafted offline • Amiin Cabdi 2026",
                        "Si ammaan ah oo offline ah oo Amiin Cabdi 2026 ah",
                        viewModel
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

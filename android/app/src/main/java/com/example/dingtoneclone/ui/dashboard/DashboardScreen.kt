package com.example.dingtoneclone.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dingtoneclone.data.ApiClient
import com.example.dingtoneclone.data.SmsMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var credits by remember { mutableStateOf(0) }

    fun loadInbox() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val profile = ApiClient.service.getProfile()
                credits = profile.user.credits
                val inbox = ApiClient.service.getInbox()
                messages = inbox.messages
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadInbox() }

    Scaffold(
        containerColor = Color(0xFF0F0C29),
        topBar = {
            TopAppBar(
                title = { Text("SMS Inbox", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1B3A)),
                actions = {
                    // Credits chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF302B63),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "💰 $credits credits",
                            color = Color(0xFFFFD700),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    IconButton(onClick = { loadInbox() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF9D7CE0))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    color = Color(0xFF7C5CBF),
                    modifier = Modifier.align(Alignment.Center)
                )
                error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚠️ ${error}", color = Color(0xFFFF6B6B), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(24.dp))
                    Button(onClick = { loadInbox() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C5CBF))) {
                        Text("Retry")
                    }
                }
                messages.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Email, null, tint = Color(0xFF3A3A5C), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No messages yet", color = Color(0xFF808090), fontSize = 16.sp)
                    Text("Get a virtual number to start receiving SMS", color = Color(0xFF606070), fontSize = 13.sp)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg -> SmsCard(msg) }
                }
            }
        }
    }
}

@Composable
private fun SmsCard(msg: SmsMessage) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (!msg.read) Color(0xFF1E1B3A) else Color(0xFF16142E),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar circle with initial
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF302B63)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    msg.from.takeLast(2),
                    color = Color(0xFF9D7CE0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(msg.from, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (!msg.read) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C5CBF))
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "To: ${msg.to}",
                    color = Color(0xFF707090),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    msg.body,
                    color = Color(0xFFCCCCDD),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    msg.receivedAt.take(19).replace("T", " "),
                    color = Color(0xFF606070),
                    fontSize = 11.sp
                )
            }
        }
    }
}

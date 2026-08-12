package com.example.dingtoneclone.ui.numbers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dingtoneclone.data.ApiClient
import com.example.dingtoneclone.data.AvailableNumber
import com.example.dingtoneclone.data.VirtualNumber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumbersScreen() {
    val scope = rememberCoroutineScope()
    var myNumbers by remember { mutableStateOf<List<VirtualNumber>>(emptyList()) }
    var availableNumbers by remember { mutableStateOf<List<AvailableNumber>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isBuying by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    fun loadNumbers() {
        scope.launch {
            isLoading = true; error = null
            try {
                myNumbers = ApiClient.service.getMyNumbers().numbers
                availableNumbers = ApiClient.service.getAvailableNumbers().numbers
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun buyNumber(phoneNumber: String) {
        scope.launch {
            isBuying = phoneNumber
            error = null; successMsg = null
            try {
                val res = ApiClient.service.buyNumber(com.example.dingtoneclone.data.BuyNumberRequest(phoneNumber))
                successMsg = "✅ ${res.number.phoneNumber} purchased!"
                loadNumbers()
            } catch (e: Exception) {
                error = e.message
            } finally {
                isBuying = null
            }
        }
    }

    fun releaseNumber(sid: String) {
        scope.launch {
            error = null; successMsg = null
            try {
                ApiClient.service.releaseNumber(sid)
                successMsg = "Number released"
                loadNumbers()
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    LaunchedEffect(Unit) { loadNumbers() }

    Scaffold(
        containerColor = Color(0xFF0F0C29),
        topBar = {
            TopAppBar(
                title = { Text("Virtual Numbers", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1B3A))
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1B3A),
                contentColor = Color(0xFF7C5CBF)
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("My Numbers (${myNumbers.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Buy Number") })
            }

            // Status messages
            successMsg?.let {
                Surface(color = Color(0xFF1A3A1A), modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = Color(0xFF66FF66), modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
            }
            error?.let {
                Surface(color = Color(0xFF3A1A1A), modifier = Modifier.fillMaxWidth()) {
                    Text("⚠️ $it", color = Color(0xFFFF6B6B), modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF7C5CBF))
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        if (myNumbers.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Phone, null, tint = Color(0xFF3A3A5C), modifier = Modifier.size(64.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("No numbers yet", color = Color(0xFF808090))
                                    Text("Buy a number to get started", color = Color(0xFF606070), fontSize = 13.sp)
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = { selectedTab = 1 },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C5CBF))
                                    ) { Text("Browse Numbers") }
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(myNumbers) { num -> MyNumberCard(num, onRelease = { releaseNumber(num.sid) }) }
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableNumbers) { num ->
                                AvailableNumberCard(
                                    num,
                                    isBuying = isBuying == num.phoneNumber,
                                    onBuy = { buyNumber(num.phoneNumber) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyNumberCard(num: VirtualNumber, onRelease: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1B3A),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Phone, null, tint = Color(0xFF7C5CBF), modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(num.phoneNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("SMS received: ${num.smsCount}", color = Color(0xFF808090), fontSize = 12.sp)
                Text("Since: ${num.assignedAt.take(10)}", color = Color(0xFF606070), fontSize = 11.sp)
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B6B))
            }
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Release Number?", color = Color.White) },
            text = { Text("${num.phoneNumber} will be released and can't be recovered.", color = Color(0xFFB0B0C0)) },
            confirmButton = {
                TextButton(onClick = { onRelease(); showConfirm = false }) { Text("Release", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = Color(0xFF9D7CE0)) }
            },
            containerColor = Color(0xFF1E1B3A)
        )
    }
}

@Composable
private fun AvailableNumberCard(num: AvailableNumber, isBuying: Boolean, onBuy: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1B3A),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(num.phoneNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (num.region.isNotEmpty()) Text("Region: ${num.region}", color = Color(0xFF808090), fontSize = 12.sp)
                Text("Cost: 5 credits", color = Color(0xFFFFD700), fontSize = 12.sp)
            }
            Button(
                onClick = onBuy,
                enabled = !isBuying,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C5CBF))
            ) {
                if (isBuying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Buy")
                }
            }
        }
    }
}

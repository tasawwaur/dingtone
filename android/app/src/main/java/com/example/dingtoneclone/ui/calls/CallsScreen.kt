package com.example.dingtoneclone.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dingtoneclone.data.ApiClient
import com.example.dingtoneclone.data.MakeCallRequest
import com.example.dingtoneclone.data.VirtualNumber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen() {
    val scope = rememberCoroutineScope()
    var myNumbers by remember { mutableStateOf<List<VirtualNumber>>(emptyList()) }
    var selectedFromNumber by remember { mutableStateOf<String?>(null) }
    var dialNumber by remember { mutableStateOf("") }
    var isCalling by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            val res = ApiClient.service.getMyNumbers()
            myNumbers = res.numbers
            if (res.numbers.isNotEmpty()) {
                selectedFromNumber = res.numbers.first().phoneNumber
            }
        }
    }

    fun initiateOutboundCall() {
        val fromNum = selectedFromNumber
        if (fromNum.isNullOrEmpty()) {
            errorMsg = "Please buy a virtual number first to make calls."
            return
        }
        if (dialNumber.isBlank()) {
            errorMsg = "Please enter a phone number to dial."
            return
        }

        scope.launch {
            isCalling = true
            errorMsg = null
            statusMsg = null
            try {
                val formattedTo = if (!dialNumber.startsWith("+")) "+$dialNumber" else dialNumber
                val res = ApiClient.service.makeCall(MakeCallRequest(to = formattedTo, from = fromNum))
                statusMsg = "📞 Call Initiated! SID: ${res.sid.take(12)}..."
            } catch (e: Exception) {
                errorMsg = e.message ?: "Failed to initiate call"
            } finally {
                isCalling = false
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F0C29),
        topBar = {
            TopAppBar(
                title = { Text("Make Outbound Call", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1B3A))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // From number selector
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1B3A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF7C5CBF))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Caller ID (From)", color = Color(0xFF808090), fontSize = 11.sp)
                        Text(
                            selectedFromNumber ?: "No virtual number assigned",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Display dialed number
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (dialNumber.isEmpty()) "Enter Number" else dialNumber,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dialNumber.isEmpty()) Color(0xFF505070) else Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (dialNumber.isNotEmpty()) {
                        IconButton(onClick = { dialNumber = dialNumber.dropLast(1) }) {
                            Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = Color(0xFF9D7CE0))
                        }
                    }
                }
            }

            // Status / Error alerts
            statusMsg?.let {
                Surface(color = Color(0xFF1A3A1A), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(it, color = Color(0xFF66FF66), fontSize = 13.sp, modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center)
                }
            }

            errorMsg?.let {
                Surface(color = Color(0xFF3A1A1A), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("⚠️ $it", color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Dialpad grid (1-9, *, 0, +)
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("*", "0", "+")
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                keypad.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1B3A))
                                    .clickable { dialNumber += digit },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(digit, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Call action button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
                        )
                    )
                    .clickable(enabled = !isCalling) { initiateOutboundCall() },
                contentAlignment = Alignment.Center
            ) {
                if (isCalling) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                } else {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

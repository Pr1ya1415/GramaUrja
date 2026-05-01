package com.gramaurja.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gramaurja.app.ui.theme.GramaUrjaTheme
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class PowerStatusActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val zoneId = intent.getStringExtra("zoneId") ?: "zone_001"
        val zoneName = intent.getStringExtra("zoneName") ?: "Your Zone"
        val database = FirebaseDatabase.getInstance().getReference("zones/$zoneId/power_status")

        setContent {
            var status by remember { mutableStateOf<PowerStatus?>(null) }
            var isConnected by remember { mutableStateOf(true) }

            DisposableEffect(Unit) {
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        status = snapshot.getValue(PowerStatus::class.java)
                        isConnected = true
                    }
                    override fun onCancelled(error: DatabaseError) {
                        isConnected = false
                    }
                }
                database.addValueEventListener(listener)
                onDispose { database.removeEventListener(listener) }
            }

            GramaUrjaTheme {
                PowerStatusScreen(
                    zoneName = zoneName,
                    status = status,
                    isConnected = isConnected,
                    onToggle = {
                        val newStatus = PowerStatus(
                            isOn = !(status?.isOn ?: false),
                            timestamp = System.currentTimeMillis(),
                            zoneId = zoneId
                        )
                        database.setValue(newStatus)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerStatusScreen(
    zoneName: String,
    status: PowerStatus?,
    isConnected: Boolean,
    onToggle: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(zoneName, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // FR-07: Offline Banner
            if (!isConnected) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    color = Color(0xFFB91C1C),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("No connection — showing last known status", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // FR-02: Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.current_power_status),
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val isOn = status?.isOn ?: false
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isOn) Color(0xFF4CAF50) else Color(0xFFF44336),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isOn) stringResource(R.string.power_on) else stringResource(R.string.power_off),
                            modifier = Modifier.padding(24.dp),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // FR-05: Dynamic Timestamp with Color Coding
                    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while(true) {
                            delay(30000)
                            currentTime = System.currentTimeMillis()
                        }
                    }

                    status?.let {
                        val diffMillis = currentTime - it.timestamp
                        val mins = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
                        val textColor = when {
                            mins < 10 -> Color(0xFF4CAF50)
                            mins < 30 -> Color(0xFFF59E0B)
                            else -> Color(0xFF94A3B8)
                        }
                        Text(
                            text = "${stringResource(R.string.updated_prefix)} $mins ${stringResource(R.string.min_ago)}",
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // FR-03: Status Toggle
            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (status?.isOn == true) "Switch Power OFF" else stringResource(R.string.notify_restored),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // FR-06: Pump Timer Tool
            PumpTimerCard()
        }
    }
}

@Composable
fun PumpTimerCard() {
    val crops = listOf(
        "Paddy" to 150, // 2h 30m
        "Sugarcane" to 180, // 3h
        "Ragi" to 90, // 1h 30m
        "Wheat" to 120
    )
    var selectedCrop by remember { mutableStateOf(crops[0]) }
    var timeLeft by remember { mutableStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning, timeLeft) {
        if (isTimerRunning && timeLeft > 0) {
            delay(1000)
            timeLeft -= 1
        } else if (timeLeft == 0L) {
            isTimerRunning = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(stringResource(R.string.pump_timer_title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            crops.forEach { crop ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedCrop == crop),
                        onClick = { if (!isTimerRunning) selectedCrop = crop },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4CAF50))
                    )
                    Text(crop.first, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isTimerRunning) {
                val h = timeLeft / 3600
                val m = (timeLeft % 3600) / 60
                val s = timeLeft % 60
                Text(
                    text = String.format("%02d:%02d:%02d", h, m, s),
                    color = Color(0xFFF59E0B),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Button(
                onClick = {
                    if (isTimerRunning) {
                        isTimerRunning = false
                        timeLeft = 0
                    } else {
                        timeLeft = selectedCrop.second.toLong() * 60
                        isTimerRunning = true
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) Color.Gray else Color(0xFF3B82F6))
            ) {
                Text(if (isTimerRunning) "Stop Timer" else "Start Watering Timer")
            }
        }
    }
}
package com.gramaurja.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.gramaurja.app.ui.theme.GramaUrjaTheme
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit

class PowerStatusActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val zoneId = intent.getStringExtra("zoneId") ?: "zone_001"
        val zoneName = intent.getStringExtra("zoneName") ?: "Your Village"
        
        val database = FirebaseDatabase.getInstance().getReference("zones/$zoneId")
        val statusRef = database.child("power_status")
        
        statusRef.keepSynced(true)
        FirebaseMessaging.getInstance().subscribeToTopic("zone_$zoneId")
        
        val sharedPrefs = getSharedPreferences("grama_urja", MODE_PRIVATE)
        var userId = sharedPrefs.getString("user_id", null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            sharedPrefs.edit().putString("user_id", userId).apply()
        }

        setContent {
            var status by remember { mutableStateOf<PowerStatus?>(value = null) }
            var isConnected by remember { mutableStateOf(value = true) }
            var activeReportersCount by remember { mutableIntStateOf(value = 0) }

            // 🚀 ULTRA FAST LOCAL STATE
            var localIsOn by remember { mutableStateOf<Boolean?>(value = null) }

            DisposableEffect(Unit) {
                val statusListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val newStatus = snapshot.getValue(PowerStatus::class.java)
                        if (newStatus != null) {
                            status = newStatus
                            // Only update local state from server if we are not in the middle of a toggle
                            if (localIsOn == null) {
                                // Initial sync
                            }
                        }
                        isConnected = true
                    }
                    override fun onCancelled(error: DatabaseError) { isConnected = false }
                }
                statusRef.addValueEventListener(statusListener)
                onDispose { statusRef.removeEventListener(statusListener) }
            }

            // Sync local state with server state when server state changes
            LaunchedEffect(status) {
                if (status != null) {
                    localIsOn = status!!.isOn
                }
            }

            GramaUrjaTheme {
                PowerStatusScreen(
                    zoneName = zoneName,
                    status = status,
                    displayIsOn = localIsOn ?: (status?.isOn ?: false),
                    isConnected = isConnected,
                    activeReporters = activeReportersCount,
                    onBack = { 
                        startActivity(Intent(this@PowerStatusActivity, ZoneSelectionActivity::class.java))
                        finish()
                    },
                    onUpdateStatus = { newIsOn ->
                        // 1. INSTANT UI CHANGE
                        localIsOn = newIsOn
                        val currentTime = System.currentTimeMillis()
                        
                        // 2. BACKGROUND SYNC
                        val updateMap = mapOf(
                            "isOn" to newIsOn,
                            "timestamp" to currentTime,
                            "updatedBy" to "Village Admin"
                        )
                        
                        statusRef.updateChildren(updateMap).addOnFailureListener {
                            // Rollback only on hard failure
                            localIsOn = !newIsOn
                            Toast.makeText(this@PowerStatusActivity, "Sync Error", Toast.LENGTH_SHORT).show()
                        }
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
    displayIsOn: Boolean,
    isConnected: Boolean,
    activeReporters: Int,
    onBack: () -> Unit,
    onUpdateStatus: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(zoneName, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isConnected) Color(0xFF16A34A) else Color(0xFFDC2626)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isConnected) stringResource(R.string.live) else stringResource(R.string.offline), fontSize = 11.sp, color = if (isConnected) Color(0xFF16A34A) else Color(0xFFDC2626))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    Row(modifier = Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.SignalCellular4Bar else Icons.Default.SignalCellularConnectedNoInternet0Bar,
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF16A34A) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Bolt, null) },
                    label = { Text("Status") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFF57C00), selectedTextColor = Color(0xFFF57C00))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Analytics, null) },
                    label = { Text("Insights") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFF57C00), selectedTextColor = Color(0xFFF57C00))
                )
            }
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedTab == 0) {
                if (!isConnected) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudOff, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.offline_msg), color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                PowerStatusCard(status, displayIsOn, activeReporters, onUpdateStatus)
                Spacer(modifier = Modifier.height(16.dp))
                if (!displayIsOn) SmartPredictionCard()
                Spacer(modifier = Modifier.height(16.dp))
                PumpTimerCard()
            } else {
                InsightsTab(activeReporters)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PowerStatusCard(
    status: PowerStatus?,
    displayIsOn: Boolean,
    activeReporters: Int,
    onUpdateStatus: (Boolean) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (!displayIsOn) 0.3f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ConfidenceIndicator(activeReporters)
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.current_power_status), color = Color(0xFF94A3B8), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Surface(modifier = Modifier.size(110.dp).alpha(if (!displayIsOn) alpha else 1f), shape = CircleShape, color = if (displayIsOn) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)) {
                Icon(imageVector = if (displayIsOn) Icons.Default.FlashOn else Icons.Default.FlashOff, null, modifier = Modifier.padding(28.dp), tint = if (displayIsOn) Color(0xFF16A34A) else Color(0xFFDC2626))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = if (displayIsOn) stringResource(R.string.power_on) else stringResource(R.string.power_off), color = if (displayIsOn) Color(0xFF16A34A) else Color(0xFFDC2626), fontSize = 36.sp, fontWeight = FontWeight.Black)
            
            var currentTime by remember { mutableStateOf(value = System.currentTimeMillis()) }
            LaunchedEffect(Unit) { while(true) { delay(30000); currentTime = System.currentTimeMillis() } }
            status?.let {
                val mins = TimeUnit.MILLISECONDS.toMinutes(currentTime - it.timestamp)
                Text(text = if (mins < 1) stringResource(R.string.updated_just_now) else "${stringResource(R.string.updated_prefix)} $mins ${stringResource(R.string.min_ago)}", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (displayIsOn) {
                    Button(
                        onClick = { onUpdateStatus(false) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PowerOff, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Switch Power OFF", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                } else {
                    Button(
                        onClick = { onUpdateStatus(true) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Switch Power ON", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun InsightsTab(activeReporters: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timeline, null, tint = Color(0xFFF57C00))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Village Statistics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.power_available_today, 7), color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.weekly_avg, 6), color = Color(0xFF64748B), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.best_irrigation_time, "4:00 AM - 9:00 AM"), color = Color(0xFFF57C00), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, null, tint = Color(0xFF64748B))
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.people_nearby, activeReporters + 3), color = Color(0xFF475569), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        HistoryTimelineCard()
        NotificationToggleCard()
    }
}

@Composable
fun HistoryTimelineCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = Color(0xFF64748B))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.history_timeline), fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
            Spacer(modifier = Modifier.height(12.dp))
            listOf(true to "10:30 AM", false to "08:15 AM", true to "04:00 AM").forEach { (isOn, time) ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isOn) Color(0xFF16A34A) else Color(0xFFDC2626)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = if (isOn) stringResource(R.string.power_on_at, time) else stringResource(R.string.power_off_at, time), color = Color(0xFF475569), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ConfidenceIndicator(count: Int) {
    val (label, color) = when {
        count >= 5 -> "High Confidence" to Color(0xFF16A34A)
        count >= 2 -> "Medium Confidence" to Color(0xFFF59E0B)
        else -> "Low Confidence" to Color(0xFFDC2626)
    }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NotificationToggleCard() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(value = false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (enabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone, null, tint = if (enabled) Color(0xFFF57C00) else Color(0xFF64748B))
            Spacer(modifier = Modifier.width(16.dp))
            Text(stringResource(R.string.notifications), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Switch(checked = enabled, onCheckedChange = { enabled = it; Toast.makeText(context, if (it) "Notifications Enabled" else "Disabled", Toast.LENGTH_SHORT).show() })
        }
    }
}

@Composable
fun SmartPredictionCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEDD5))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color(0xFFFFB300)) { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.padding(10.dp), tint = Color.White) }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.smart_prediction), fontWeight = FontWeight.Bold, color = Color(0xFF9A3412), fontSize = 14.sp)
                Text(stringResource(R.string.expected_restore, 45), color = Color(0xFFC2410C), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun PumpTimerCard() {
    val crops = listOf(stringResource(R.string.crop_paddy) to 150, stringResource(R.string.crop_sugarcane) to 180, stringResource(R.string.crop_ragi) to 90, stringResource(R.string.crop_maize) to 120, stringResource(R.string.crop_arecanut) to 240)
    var selectedCrop by remember { mutableStateOf(value = crops[0]) }
    var timeLeft by remember { mutableStateOf(value = 0L) }
    var isTimerRunning by remember { mutableStateOf(value = false) }
    LaunchedEffect(isTimerRunning, timeLeft) { if (isTimerRunning && timeLeft > 0) { delay(1000); timeLeft -= 1 } else if (timeLeft == 0L) { isTimerRunning = false } }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = Color(0xFFF57C00))
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.pump_timer_title), color = Color(0xFF0F172A), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            crops.forEach { crop ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { if (!isTimerRunning) selectedCrop = crop }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = (selectedCrop == crop), onClick = { if (!isTimerRunning) selectedCrop = crop }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF57C00)))
                    Text(crop.first, color = if (selectedCrop == crop) Color(0xFF0F172A) else Color(0xFF64748B), fontWeight = if (selectedCrop == crop) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (isTimerRunning) {
                val h = timeLeft / 3600; val m = (timeLeft % 3600) / 60; val s = timeLeft % 60
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF7ED), RoundedCornerShape(16.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(text = String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", h, m, s), color = Color(0xFFF57C00), fontSize = 40.sp, fontWeight = FontWeight.Black)
                }
            }
            Button(onClick = { if (isTimerRunning) { isTimerRunning = false; timeLeft = 0 } else { timeLeft = selectedCrop.second.toLong() * 60; isTimerRunning = true } }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) Color(0xFFF1F5F9) else Color(0xFF0F172A), contentColor = if (isTimerRunning) Color(0xFF64748B) else Color.White), shape = RoundedCornerShape(16.dp)) {
                Text(if (isTimerRunning) "Stop Timer" else "Start Watering Timer", fontWeight = FontWeight.Bold)
            }
        }
    }
}

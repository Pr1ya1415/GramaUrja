package com.gramaurja.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.edit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gramaurja.app.ui.theme.GramaUrjaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ZoneSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val zones = listOf(
            "zone_001" to getString(R.string.village_hunsur),
            "zone_002" to getString(R.string.village_mandya),
            "zone_003" to getString(R.string.village_kolar),
            "zone_004" to getString(R.string.village_davangere),
            "zone_005" to getString(R.string.village_belagavi),
        )

        setContent {
            GramaUrjaTheme {
                ZoneSelectionScreen(
                    zones = zones,
                    onBack = { finish() },
                ) { zoneId, zoneName ->
                    getSharedPreferences("grama_urja", MODE_PRIVATE).edit {
                        putString("selected_zone", zoneId)
                    }

                    val intent = Intent(this, PowerStatusActivity::class.java).apply {
                        putExtra("zoneId", zoneId)
                        putExtra("zoneName", zoneName)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneSelectionScreen(
    zones: List<Pair<String, String>>,
    onBack: () -> Unit,
    onZoneSelected: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredZones = remember(searchQuery) {
        if (searchQuery.isEmpty()) zones
        else zones.filter { it.second.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.transformer_zones), 
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // FR: Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_zones)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFF57C00)
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 🎯 Feature: Auto-detect Zone
            var isDetecting by remember { mutableStateOf(value = false) }
            val coroutineScope = rememberCoroutineScope()

            Button(
                onClick = {
                    isDetecting = true
                    coroutineScope.launch {
                        delay(600) // 🚀 Reduced simulated delay
                        isDetecting = false
                        // Pick the first one for simulation
                        val detected = zones[0]
                        onZoneSelected(detected.first, detected.second)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isDetecting
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.detecting))
                } else {
                    Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.auto_detect))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(filteredZones) { (id, name) ->
                    ZoneCard(id, name) { onZoneSelected(id, name) }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun ZoneCard(id: String, name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF7ED)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = Color(0xFFF57C00)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.zone_id_label, id),
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFCBD5E1)
            )
        }
    }
}
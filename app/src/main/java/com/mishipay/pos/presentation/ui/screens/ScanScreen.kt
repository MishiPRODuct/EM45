package com.mishipay.pos.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mishipay.pos.domain.ReaderState
import com.mishipay.pos.presentation.ui.theme.LightGray
import com.mishipay.pos.presentation.ui.theme.MediumGray
import com.mishipay.pos.presentation.ui.theme.PriceGreen
import com.mishipay.pos.presentation.viewmodel.ScannedTag

@Composable
fun ScanScreen(
    readerState: ReaderState,
    scannedTags: List<ScannedTag>,
    onDoneClicked: () -> Unit,
    onConnectReader: () -> Unit,
    onStartScanning: () -> Unit
) {
    // Auto-connect or start scanning when screen opens
    LaunchedEffect(Unit) {
        when (readerState) {
            is ReaderState.Disconnected -> onConnectReader()
            is ReaderState.Connected -> onStartScanning()
            else -> { /* Already scanning or connecting */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        ScanHeader(readerState = readerState)

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (readerState) {
                is ReaderState.Connecting -> {
                    ConnectingContent()
                }
                is ReaderState.Error -> {
                    ErrorContent(message = readerState.message, onRetry = onConnectReader)
                }
                is ReaderState.Disconnected -> {
                    DisconnectedContent(onConnect = onConnectReader)
                }
                is ReaderState.Connected, is ReaderState.Scanning -> {
                    ScanningContent(
                        isScanning = readerState is ReaderState.Scanning,
                        scannedTags = scannedTags
                    )
                }
            }
        }

        // Bottom section
        BottomSection(
            readerState = readerState,
            scannedCount = scannedTags.size,
            onDoneClicked = onDoneClicked
        )
    }
}

@Composable
private fun ScanHeader(readerState: ReaderState) {
    val statusText = when (readerState) {
        is ReaderState.Connecting -> "Connecting..."
        is ReaderState.Connected -> "Ready to scan"
        is ReaderState.Scanning -> "Scanning..."
        is ReaderState.Error -> "Error"
        is ReaderState.Disconnected -> "Disconnected"
    }

    val statusColor = when (readerState) {
        is ReaderState.Scanning -> PriceGreen
        is ReaderState.Connected -> PriceGreen
        is ReaderState.Connecting -> Color(0xFFFF9800)
        is ReaderState.Error -> Color(0xFFE53935)
        is ReaderState.Disconnected -> MediumGray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGray)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Scan Items",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    color = MediumGray
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            modifier = Modifier.align(Alignment.CenterEnd),
            tint = Color.Black
        )
    }
}

@Composable
private fun ConnectingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color.Black,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connecting to RFID reader...",
                fontSize = 16.sp,
                color = MediumGray
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun DisconnectedContent(onConnect: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reader disconnected",
                fontSize = 16.sp,
                color = MediumGray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Text("Connect")
            }
        }
    }
}

@Composable
private fun ScanningContent(
    isScanning: Boolean,
    scannedTags: List<ScannedTag>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scanning indicator
        if (isScanning) {
            PulsingIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (scannedTags.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isScanning) "Hold items near the scanner..." else "Tap Start Scanning to begin",
                    fontSize = 16.sp,
                    color = MediumGray
                )
            }
        } else {
            Text(
                text = "Scanned items (${scannedTags.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scannedTags, key = { it.epc }) { tag ->
                    ScannedTagItem(tag = tag)
                }
            }
        }
    }
}

@Composable
private fun PulsingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .scale(scale)
                .background(PriceGreen, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Scanning active",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = PriceGreen
        )
    }
}

@Composable
private fun ScannedTagItem(tag: ScannedTag) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LightGray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Scanned",
                tint = PriceGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Show SKU (or EPC if no SKU)
                Text(
                    text = tag.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Show EPC below if SKU was decoded
                if (tag.sku != null) {
                    Text(
                        text = "EPC: ${tag.epc}",
                        fontSize = 11.sp,
                        color = MediumGray
                    )
                }
                Text(
                    text = "100 SAR",
                    fontSize = 14.sp,
                    color = PriceGreen
                )
            }
        }
    }
}

@Composable
private fun BottomSection(
    readerState: ReaderState,
    scannedCount: Int,
    onDoneClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGray)
            .padding(16.dp)
    ) {
        // Scanned count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Items scanned",
                fontSize = 16.sp,
                color = MediumGray
            )
            Text(
                text = "$scannedCount",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Done button
        Button(
            onClick = onDoneClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            enabled = readerState is ReaderState.Connected || readerState is ReaderState.Scanning
        ) {
            Text(
                text = "Done",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

package com.mishipay.pos.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mishipay.pos.presentation.ui.theme.LightGray
import com.mishipay.pos.presentation.ui.theme.MediumGray

@Composable
fun WelcomeScreen(
    onStartClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header bar with hamburger menu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightGray)
                .padding(16.dp)
        ) {
            // Hamburger menu icon (visual only)
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                modifier = Modifier.align(Alignment.CenterEnd),
                tint = Color.Black
            )
        }

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "MishiPay RFID Scan",
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MediumGray
            )
        }

        // Bottom section with button and footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Touch here to start button
            Button(
                onClick = onStartClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Touch here to start",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer text
            Text(
                text = "Powered by MishiPay",
                fontSize = 14.sp,
                color = MediumGray
            )
        }
    }
}

package com.mishipay.pos.presentation.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mishipay.pos.domain.Basket
import com.mishipay.pos.domain.BasketItem
import com.mishipay.pos.presentation.ui.theme.LightGray
import com.mishipay.pos.presentation.ui.theme.MediumGray
import com.mishipay.pos.presentation.ui.theme.PriceGreen

@Composable
fun BasketScreen(
    basket: Basket,
    onScanItemsClicked: () -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        BasketHeader(itemCount = basket.totalItems)

        if (basket.isEmpty) {
            // Empty basket state
            EmptyBasketContent(
                modifier = Modifier.weight(1f)
            )
        } else {
            // Items list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(basket.items, key = { it.epc }) { item ->
                    BasketItemCard(
                        item = item,
                        onDelete = { onDeleteItem(item.epc) }
                    )
                }
            }
        }

        // Bottom section
        BottomSection(
            basket = basket,
            isEmpty = basket.isEmpty,
            onScanClicked = onScanItemsClicked,
            onPayClicked = {
                Toast.makeText(context, "Payment coming soon!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun BasketHeader(itemCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGray)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Basket",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            if (itemCount > 0) {
                Text(
                    text = "$itemCount items",
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
private fun EmptyBasketContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Please scan your first item",
            fontSize = 20.sp,
            color = MediumGray
        )
    }
}

@Composable
private fun BasketItemCard(
    item: BasketItem,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // SKU (or EPC if no SKU decoded)
            Text(
                text = item.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Show Serial Number (AI 21) if decoded
            if (item.serialNumber != null) {
                Text(
                    text = "Serial: ${item.serialNumber}",
                    fontSize = 12.sp,
                    color = MediumGray
                )
            }

            // Show EPC
            Text(
                text = "EPC: ${item.epc}",
                fontSize = 11.sp,
                color = MediumGray
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.price} SAR",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = PriceGreen
            )
        }

        // Delete button
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    Divider(
        color = LightGray,
        thickness = 1.dp
    )
}

@Composable
private fun BottomSection(
    basket: Basket,
    isEmpty: Boolean,
    onScanClicked: () -> Unit,
    onPayClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGray)
            .padding(16.dp)
    ) {
        // Total row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "${basket.totalPrice} SAR",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isEmpty) {
            // Single scan button for empty state
            OutlinedButton(
                onClick = onScanClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
            ) {
                Text(
                    text = "Scan items",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Two buttons for items state
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Scan more button
                OutlinedButton(
                    onClick = onScanClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Black
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
                ) {
                    Text(
                        text = "Scan more",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Finish and pay button
                Button(
                    onClick = onPayClicked,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Finish and pay",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

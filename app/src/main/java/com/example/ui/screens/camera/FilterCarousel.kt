package com.example.ui.screens.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.filters.FilterCategory
import com.example.data.filters.FilterRegistry
import com.example.data.filters.SnaporaFilter
import com.example.ui.theme.*

@Composable
fun FilterCarousel(
    selectedFilter: SnaporaFilter,
    onFilterSelected: (SnaporaFilter) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(FilterCategory.ALL) }
    val filteredList = remember(selectedCategory) {
        FilterRegistry.getFiltersByCategory(selectedCategory)
    }
    val listState = rememberLazyListState()

    // Auto scroll when filter changes
    LaunchedEffect(selectedFilter.id, selectedCategory) {
        val index = filteredList.indexOfFirst { it.id == selectedFilter.id }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Active Filter Info & Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SnaporaPink,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedFilter.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (selectedFilter.description.isNotBlank()) {
                    Text(
                        text = " • ${selectedFilter.description}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Filters", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        // Category Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FilterCategory.values()) { category ->
                val isSelected = category == selectedCategory
                Surface(
                    onClick = { selectedCategory = category },
                    color = if (isSelected) SnaporaPurple else DarkSurfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = category.label,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal Carousel of Filter Badges
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            items(filteredList) { filter ->
                val isSelected = filter.id == selectedFilter.id

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "filterBadgeScale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(scale)
                        .clickable { onFilterSelected(filter) }
                        .testTag("filter_item_${filter.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        3.dp,
                                        Brush.sweepGradient(listOf(SnaporaPurple, SnaporaPink, SnaporaCyan, SnaporaPurple)),
                                        CircleShape
                                    )
                                } else {
                                    Modifier.border(1.5.dp, DarkCardBorder, CircleShape)
                                }
                            )
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) DarkSurfaceVariant else DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.iconEmoji,
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = filter.name,
                        color = if (isSelected) SnaporaPink else Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

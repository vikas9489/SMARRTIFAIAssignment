package com.vikas.tryon.presentation.garment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.model.GarmentCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: GarmentViewModel = hiltViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedGarmentId by viewModel.selectedGarmentId.collectAsState(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wardrobe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category filter chips
            ScrollableTabRow(
                selectedTabIndex = GarmentCategory.entries.indexOfFirst { it == selectedCategory }
                    .coerceAtLeast(-1) + 1,
                edgePadding = 16.dp,
                divider = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                GarmentCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.displayName) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.garments) { garment ->
                    GarmentGridItem(
                        garment = garment,
                        isSelected = garment.id == selectedGarmentId,
                        onClick = {
                            viewModel.selectGarment(
                                if (garment.id == selectedGarmentId) null else garment.id
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GarmentGridItem(
    garment: Garment,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
    } else Modifier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Box {
            Column {
                // Garment visual
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(garment.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Stylized garment shape
                    GarmentShape(garment = garment)
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        garment.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        garment.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        garment.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GarmentShape(garment: Garment) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(
                when (garment.category) {
                    GarmentCategory.TOP, GarmentCategory.OUTERWEAR ->
                        RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                    GarmentCategory.BOTTOM ->
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                    GarmentCategory.DRESS ->
                        RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp, bottomStart = 15.dp, bottomEnd = 15.dp)
                }
            )
            .background(garment.color)
    )
}

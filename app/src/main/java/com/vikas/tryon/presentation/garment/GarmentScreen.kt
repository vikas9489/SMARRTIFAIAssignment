package com.vikas.tryon.presentation.garment

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vikas.tryon.data.model.Garment
import com.vikas.tryon.data.model.GarmentCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarmentScreen(
    onNavigateBack: () -> Unit,
    onScanGarment: () -> Unit,
    viewModel: GarmentViewModel = hiltViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedGarmentId by viewModel.selectedGarmentId.collectAsState(null)
    val favouriteIds by viewModel.favouriteIds.collectAsState(emptyList())

    // "Favourites" is a virtual filter, not a GarmentCategory
    var showFavouritesOnly by remember { mutableStateOf(false) }

    val displayedGarments = remember(viewModel.garments, showFavouritesOnly, favouriteIds) {
        if (showFavouritesOnly) viewModel.garments.filter { it.id in favouriteIds }
        else viewModel.garments
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wardrobe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onScanGarment) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Scan garment")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanGarment,
                icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
                text = { Text("Scan Garment") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category filter row + Favourites chip
            ScrollableTabRow(
                selectedTabIndex = when {
                    showFavouritesOnly -> GarmentCategory.entries.size + 1
                    selectedCategory == null -> 0
                    else -> GarmentCategory.entries.indexOfFirst { it == selectedCategory } + 1
                },
                edgePadding = 16.dp,
                divider = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = !showFavouritesOnly && selectedCategory == null,
                    onClick = { viewModel.selectCategory(null); showFavouritesOnly = false },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                GarmentCategory.entries.forEach { category ->
                    FilterChip(
                        selected = !showFavouritesOnly && selectedCategory == category,
                        onClick = { viewModel.selectCategory(category); showFavouritesOnly = false },
                        label = { Text(category.displayName) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                FilterChip(
                    selected = showFavouritesOnly,
                    onClick = { showFavouritesOnly = !showFavouritesOnly; viewModel.selectCategory(null) },
                    label = { Text("Favourites") },
                    leadingIcon = {
                        Icon(
                            if (showFavouritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            if (showFavouritesOnly && displayedGarments.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No favourites yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            "Tap ♥ on any garment to save it here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedGarments) { garment ->
                        GarmentGridItem(
                            garment = garment,
                            isSelected = garment.id == selectedGarmentId,
                            isFavourite = garment.id in favouriteIds,
                            onClick = {
                                viewModel.selectGarment(
                                    if (garment.id == selectedGarmentId) null else garment.id
                                )
                            },
                            onFavouriteClick = { viewModel.toggleFavourite(garment.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GarmentGridItem(
    garment: Garment,
    isSelected: Boolean,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onFavouriteClick: () -> Unit
) {
    val borderModifier = if (isSelected)
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
    else Modifier

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(garment.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        garment.scannedBitmap != null -> Image(
                            bitmap = garment.scannedBitmap.asImageBitmap(),
                            contentDescription = garment.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        garment.imageRes != 0 -> Image(
                            painter = painterResource(garment.imageRes),
                            contentDescription = garment.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(garment.color, BlendMode.Multiply)
                        )
                        else -> GarmentShape(garment = garment)
                    }
                }

                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        garment.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            garment.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (garment.isScanned) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Scanned",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        garment.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            // Selection check badge (top-end)
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

            // Favourite heart button (bottom-end)
            IconButton(
                onClick = onFavouriteClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = if (isFavourite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
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
                    GarmentCategory.DRESS, GarmentCategory.SCANNED ->
                        RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp, bottomStart = 15.dp, bottomEnd = 15.dp)
                }
            )
            .background(garment.color)
    )
}

package com.vikas.tryon.presentation.measurement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vikas.tryon.data.local.MeasurementHistoryEntity
import com.vikas.tryon.data.model.BodyMeasurement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    onNavigateBack: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val measurement by viewModel.bodyMeasurement.collectAsState(initial = BodyMeasurement())
    val avatar by viewModel.avatar.collectAsState(initial = null)
    val history by viewModel.measurementHistory.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body Measurements") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Suggested size card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Suggested Size",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        measurement.suggestedSize(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!measurement.isValid) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Open camera and stand in frame to measure",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (measurement.confidence > 0f) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { measurement.confidence },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        )
                        Text(
                            "Confidence: ${(measurement.confidence * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Measurements grid
            Text(
                "Measurements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MeasurementCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Straighten,
                    label = "Chest",
                    valueCm = measurement.chestCm
                )
                MeasurementCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Straighten,
                    label = "Waist",
                    valueCm = measurement.waistCm
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MeasurementCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Straighten,
                    label = "Hips",
                    valueCm = measurement.hipsCm
                )
                MeasurementCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Straighten,
                    label = "Shoulder",
                    valueCm = measurement.shoulderWidthCm
                )
            }
            MeasurementCard(
                modifier = Modifier.fillMaxWidth(0.5f),
                icon = Icons.Default.Straighten,
                label = "Inseam",
                valueCm = measurement.inseamCm
            )

            // Avatar reference info
            avatar?.let { av ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "Reference",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoItem(label = "Height", value = "${av.heightCm} cm")
                        InfoItem(label = "Weight", value = "${av.weightKg} kg")
                        InfoItem(label = "Body Type", value = av.bodyType.displayName)
                    }
                }
            }

            // Measurement history from Room
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Scan History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                history.forEach { entry ->
                    MeasurementHistoryRow(entry)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "* Measurements are estimated from pose landmarks and may vary ±5cm. " +
                    "For precise sizing, use a tape measure.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun MeasurementCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    valueCm: Float
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(
                if (valueCm > 0f) "${valueCm.roundToInt()} cm" else "--",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MeasurementHistoryRow(entry: MeasurementHistoryEntity) {
    val dateStr = remember(entry.timestampMs) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(entry.timestampMs))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(dateStr, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(2.dp))
                Text(
                    "Chest ${entry.chestCm.roundToInt()} · Waist ${entry.waistCm.roundToInt()} · Hip ${entry.hipsCm.roundToInt()} cm",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    entry.suggestedSize,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

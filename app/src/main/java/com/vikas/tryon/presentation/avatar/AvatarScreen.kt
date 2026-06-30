package com.vikas.tryon.presentation.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vikas.tryon.data.model.BodyType
import com.vikas.tryon.data.model.SkinTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarScreen(
    onNavigateBack: () -> Unit,
    viewModel: AvatarViewModel = hiltViewModel()
) {
    val avatar by viewModel.avatar.collectAsState()
    var showSavedSnackbar by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSavedSnackbar) {
        if (showSavedSnackbar) {
            snackbarHostState.showSnackbar("Avatar saved!")
            showSavedSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Avatar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveAvatar()
                        showSavedSnackbar = true
                    }) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Avatar preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(skinToneColor(avatar.skinTone)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(avatar.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${avatar.heightCm} cm · ${avatar.weightKg} kg · ${avatar.bodyType.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Name
            OutlinedTextField(
                value = avatar.name,
                onValueChange = viewModel::updateName,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(20.dp))

            // Height slider
            SectionLabel("Height: ${avatar.heightCm} cm")
            Slider(
                value = avatar.heightCm.toFloat(),
                onValueChange = { viewModel.updateHeight(it.toInt()) },
                valueRange = 140f..220f,
                steps = 79
            )

            Spacer(Modifier.height(8.dp))

            // Weight slider
            SectionLabel("Weight: ${avatar.weightKg} kg")
            Slider(
                value = avatar.weightKg.toFloat(),
                onValueChange = { viewModel.updateWeight(it.toInt()) },
                valueRange = 40f..150f,
                steps = 109
            )

            Spacer(Modifier.height(20.dp))

            // Body type
            SectionLabel("Body Type")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BodyType.entries.forEach { type ->
                    FilterChip(
                        selected = avatar.bodyType == type,
                        onClick = { viewModel.updateBodyType(type) },
                        label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Skin tone
            SectionLabel("Skin Tone")
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkinTone.entries.forEach { tone ->
                    val isSelected = avatar.skinTone == tone
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(skinToneColor(tone))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.updateSkinTone(tone) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.saveAvatar()
                    showSavedSnackbar = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Avatar")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
}

private fun skinToneColor(tone: SkinTone): Color = when (tone) {
    SkinTone.LIGHT -> Color(0xFFFFDBAC)
    SkinTone.MEDIUM_LIGHT -> Color(0xFFE8B89A)
    SkinTone.MEDIUM -> Color(0xFFD4956A)
    SkinTone.MEDIUM_DARK -> Color(0xFFAB7348)
    SkinTone.DARK -> Color(0xFF6E4230)
}

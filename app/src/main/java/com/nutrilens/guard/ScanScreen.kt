package com.nutrilens.guard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NutriLens Guard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Health Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Select conditions to customize the analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    @Composable
                    fun ProfileCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    ProfileCheckbox("Diabetic", state.isDiabetic) { viewModel.processIntent(ScanIntent.ToggleDiabetic(it)) }
                    ProfileCheckbox("Hypertension", state.isHypertension) { viewModel.processIntent(ScanIntent.ToggleHypertension(it)) }
                    ProfileCheckbox("Peanut Allergy", state.isPeanutAllergy) { viewModel.processIntent(ScanIntent.TogglePeanutAllergy(it)) }
                    ProfileCheckbox("Dairy Allergy", state.isDairyAllergy) { viewModel.processIntent(ScanIntent.ToggleDairyAllergy(it)) }
                    ProfileCheckbox("Gluten Intolerance", state.isGlutenIntolerance) { viewModel.processIntent(ScanIntent.ToggleGlutenIntolerance(it)) }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.processIntent(ScanIntent.OnProductScanned("Hide & Seek")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !state.isLoading
            ) {
                Text("Simulate Scan: Hide & Seek", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (state.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Agents Analyzing Food Safety...", style = MaterialTheme.typography.bodyMedium)
            }

            AnimatedVisibility(visible = state.analysisResult != null) {
                state.analysisResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Analysis Result",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MarkdownText(
                                markdown = result,
                                style = MaterialTheme.typography.bodyMedium,
                                isTextSelectable = false,
                                disableLinkMovementMethod = true
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.errorMessage != null) {
                state.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
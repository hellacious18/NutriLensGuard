package com.nutrilens.guard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("NutriLens Guard Scanner", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.isDiabetic,
                onCheckedChange = { viewModel.processIntent(ScanIntent.ToggleDiabetic(it)) }
            )
            Text("Diabetic Profile")
            Spacer(modifier = Modifier.width(16.dp))
            Checkbox(
                checked = state.isHypertension,
                onCheckedChange = { viewModel.processIntent(ScanIntent.ToggleHypertension(it)) }
            )
            Text("Hypertension Profile")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.processIntent(ScanIntent.OnProductScanned("Hide & Seek")) }
        ) {
            Text("Simulate Scan: Hide & Seek")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
            Text("Agents Analyzing Food Safety...", modifier = Modifier.padding(top = 8.dp))
        }

        state.analysisResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(text = result, modifier = Modifier.padding(16.dp))
            }
        }

        state.errorMessage?.let { error ->
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        }
    }
}
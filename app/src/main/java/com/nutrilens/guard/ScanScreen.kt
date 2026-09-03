package com.nutrilens.guard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Scanning photo with on-device AI...", Toast.LENGTH_SHORT).show()
            FoodOcrAnalyzer.analyzeImageUri(
                context = context,
                uri = uri,
                onSuccess = { fullText, title ->
                    viewModel.processIntent(
                        ScanIntent.AnalyzeExtractedText(
                            text = fullText,
                            title = title,
                            imageUri = uri.toString()
                        )
                    )
                },
                onError = { e ->
                    Toast.makeText(context, "Failed to analyze photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val activeProfileCount = listOf(
        state.isDiabetic,
        state.isHypertension,
        state.isPeanutAllergy,
        state.isDairyAllergy,
        state.isGlutenIntolerance
    ).count { it }

    LaunchedEffect(state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    if (state.showCameraScanner) {
        CameraScannerView(
            onDismiss = { viewModel.processIntent(ScanIntent.ToggleCameraScanner(false)) },
            onScanned = { candidateTitle, fullText, imageUri ->
                viewModel.processIntent(
                    ScanIntent.AnalyzeExtractedText(
                        text = fullText,
                        title = candidateTitle,
                        imageUri = imageUri
                    )
                )
            }
        )
    } else {
        if (state.showLinkDialog) {
            ProductLinkDialog(
                onDismiss = { viewModel.processIntent(ScanIntent.ToggleLinkDialog(false)) },
                onSubmitLink = { url ->
                    viewModel.processIntent(ScanIntent.AnalyzeLink(url))
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "NutriLens Guard",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Food Safety & Health AI",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        BadgedBox(
                            badge = {
                                if (activeProfileCount > 0) {
                                    Badge { Text("$activeProfileCount") }
                                }
                            }
                        ) {
                            IconButton(onClick = { viewModel.processIntent(ScanIntent.ToggleProfileExpanded()) }) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Dietary Profile Settings",
                                    tint = if (state.isProfileExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.processIntent(ScanIntent.ClearChat) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Chat History"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
            ) {
                // Collapsible Dietary Profile Section
                AnimatedVisibility(
                    visible = state.isProfileExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    DietaryProfilesCard(
                        state = state,
                        onToggleDiabetic = { viewModel.processIntent(ScanIntent.ToggleDiabetic(it)) },
                        onToggleHypertension = { viewModel.processIntent(ScanIntent.ToggleHypertension(it)) },
                        onTogglePeanut = { viewModel.processIntent(ScanIntent.TogglePeanutAllergy(it)) },
                        onToggleDairy = { viewModel.processIntent(ScanIntent.ToggleDairyAllergy(it)) },
                        onToggleGluten = { viewModel.processIntent(ScanIntent.ToggleGlutenIntolerance(it)) }
                    )
                }

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        ChatMessageBubble(message = message)
                    }

                    if (state.isLoading) {
                        item(key = "loading_indicator") {
                            LoadingMessageBubble()
                        }
                    }
                }

                // Quick Suggestions Chips Row
                QuickSuggestionsRow(
                    onSuggestionClick = { product ->
                        viewModel.processIntent(ScanIntent.SendMessage(product))
                    },
                    enabled = !state.isLoading
                )

                // Chat Input Bar with Camera, Photo, and Link Actions
                ChatInputBar(
                    text = state.inputText,
                    onTextChange = { viewModel.processIntent(ScanIntent.UpdateInputText(it)) },
                    onSendMessage = {
                        if (state.inputText.isNotBlank()) {
                            viewModel.processIntent(ScanIntent.SendMessage(state.inputText))
                            keyboardController?.hide()
                        }
                    },
                    onCameraClick = {
                        viewModel.processIntent(ScanIntent.ToggleCameraScanner(true))
                    },
                    onPhotoClick = {
                        galleryLauncher.launch("image/*")
                    },
                    onLinkClick = {
                        viewModel.processIntent(ScanIntent.ToggleLinkDialog(true))
                    },
                    isLoading = state.isLoading
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DietaryProfilesCard(
    state: ScanUiState,
    onToggleDiabetic: (Boolean) -> Unit,
    onToggleHypertension: (Boolean) -> Unit,
    onTogglePeanut: (Boolean) -> Unit,
    onToggleDairy: (Boolean) -> Unit,
    onToggleGluten: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Active Health Constraints",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.isDiabetic,
                    onClick = { onToggleDiabetic(!state.isDiabetic) },
                    label = { Text("Diabetic") }
                )
                FilterChip(
                    selected = state.isHypertension,
                    onClick = { onToggleHypertension(!state.isHypertension) },
                    label = { Text("Hypertension") }
                )
                FilterChip(
                    selected = state.isPeanutAllergy,
                    onClick = { onTogglePeanut(!state.isPeanutAllergy) },
                    label = { Text("Peanut Allergy") }
                )
                FilterChip(
                    selected = state.isDairyAllergy,
                    onClick = { onToggleDairy(!state.isDairyAllergy) },
                    label = { Text("Dairy Allergy") }
                )
                FilterChip(
                    selected = state.isGlutenIntolerance,
                    onClick = { onToggleGluten(!state.isGlutenIntolerance) },
                    label = { Text("Gluten Intolerance") }
                )
            }
        }
    }
}

private fun extractDomain(url: String): String {
    return try {
        val uri = java.net.URI(if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url")
        val host = uri.host ?: url
        host.removePrefix("www.")
    } catch (e: Exception) {
        "Website"
    }
}

private fun extractLinkTitle(url: String): String {
    return try {
        val path = java.net.URI(if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url").path
        val lastSegment = path.split("/").filter { it.isNotBlank() }.lastOrNull()
        if (!lastSegment.isNullOrBlank() && lastSegment.length > 2) {
            lastSegment.replace("-", " ").replace("_", " ").replace("+", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        } else {
            url
        }
    } catch (e: Exception) {
        url
    }
}

private fun isDirectImageUrl(url: String): Boolean {
    val lower = url.lowercase().substringBefore("?")
    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif")
}

@Composable
private fun LinkPreviewBubble(
    url: String,
    onOpenLink: () -> Unit
) {
    val context = LocalContext.current
    val domain = remember(url) { extractDomain(url) }
    val title = remember(url) { extractLinkTitle(url) }
    val isImage = remember(url) { isDirectImageUrl(url) }

    Surface(
        shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .widthIn(max = 240.dp)
            .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
            .clickable { onOpenLink() }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (isImage) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Link Image Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = domain,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Link",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (!message.isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "NutriLens AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (message.isUser) {
            val detectedLink = message.linkUrl ?: if (message.text.trim().startsWith("http://") || message.text.trim().startsWith("https://")) message.text.trim() else null

            when {
                // If images are sent, just show image for user's messages 200x200 ratio of image, center cropped
                message.imageUri != null -> {
                    Surface(
                        shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier.widthIn(max = 220.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(message.imageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Scanned Food Packaging",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }

                // If link is sent, show preview of the link / image
                detectedLink != null -> {
                    LinkPreviewBubble(
                        url = detectedLink,
                        onOpenLink = {
                            try {
                                val fullUrl = if (detectedLink.startsWith("http://") || detectedLink.startsWith("https://")) detectedLink else "https://$detectedLink"
                                uriHandler.openUri(fullUrl)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Standard Text Message
                else -> {
                    Surface(
                        shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 0.dp,
                        modifier = Modifier.widthIn(max = 340.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = message.text,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        } else {
            // AI Response Bubble
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                color = if (message.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    SelectionContainer {
                        MarkdownText(
                            markdown = message.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (message.isError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            isTextSelectable = true,
                            disableLinkMovementMethod = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingMessageBubble() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "NutriLens AI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Analyzing food ingredients & safety...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickSuggestionsRow(
    onSuggestionClick: (String) -> Unit,
    enabled: Boolean
) {
    val suggestions = listOf(
        "🍪 Hide & Seek",
        "🍫 Nutella",
        "🥔 Lay's Classic",
        "🥤 Coca-Cola Zero",
        "🥣 Kellogg's Corn Flakes"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = {
                    // Strip leading emoji if present for clean search query
                    val cleanText = suggestion.substringAfter(" ")
                    onSuggestionClick(cleanText)
                },
                label = { Text(suggestion, fontSize = 12.sp) },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onCameraClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onLinkClick: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCameraClick,
                enabled = !isLoading,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Scan Packaging with Camera",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onPhotoClick,
                enabled = !isLoading,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Upload Food Photo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onLinkClick,
                enabled = !isLoading,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Paste Product Link",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = {
                    Text(
                        "Product or ingredients...",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSendMessage() }
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            FilledIconButton(
                onClick = onSendMessage,
                enabled = text.isNotBlank() && !isLoading,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message"
                )
            }
        }
    }
}
package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiUiState
import com.example.model.CampusConstants
import com.example.model.ListingDraftAssist
import com.example.model.PrefillListingDraft
import com.example.model.UserProfile
import com.example.ui.components.CameraIcon
import com.example.ui.components.PillInputField
import com.example.ui.components.SparkleIcon
import com.example.ui.components.SpringActionButton
import com.example.ui.components.categoryAccent
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CanvasOffWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.OrangeAccentGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletLight
import com.example.ui.theme.VioletPop

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SellUploadScreen(
    user: UserProfile,
    isPosting: Boolean,
    errorMessage: String?,
    prefillDraft: PrefillListingDraft?,
    draftAiState: AiUiState<ListingDraftAssist>,
    onConsumePrefill: () -> Unit,
    onRequestAiAssist: (
        title: String,
        description: String,
        category: String,
        attachmentName: String?,
        resourceLink: String?,
        firstPhotoUri: Uri?
    ) -> Unit,
    onClearAiAssist: () -> Unit,
    onPublish: (
        title: String,
        description: String,
        category: String,
        pricingMode: String,
        priceAmount: Long,
        borrowDurationDays: Long?,
        borrowWindowLabel: String?,
        condition: String,
        meetupSpot: String,
        digitalItem: Boolean,
        attachmentName: String?,
        resourceLink: String?,
        photoUris: List<Uri>,
        studyTags: List<String>,
        aiSummary: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Notes") }
    var pricingMode by remember { mutableStateOf("Paid") }
    var price by remember { mutableStateOf("") }
    var selectedBorrowWindow by remember { mutableStateOf(CampusConstants.BORROW_WINDOWS[1]) }
    var condition by remember { mutableStateOf("Good") }
    var meetupSpot by remember { mutableStateOf("Central Library") }
    var resourceLink by remember { mutableStateOf("") }
    val selectedPhotoUris = remember { mutableStateListOf<Uri>() }
    val activeTags = remember { mutableStateListOf<String>() }
    var aiSummaryText by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(prefillDraft) {
        if (prefillDraft != null) {
            if (prefillDraft.title.isNotBlank()) title = prefillDraft.title
            if (prefillDraft.description.isNotBlank()) description = prefillDraft.description
            if (prefillDraft.category in CampusConstants.POST_CATEGORIES) category = prefillDraft.category
            if (prefillDraft.meetupSpot in CampusConstants.MEETUP_SPOTS) meetupSpot = prefillDraft.meetupSpot
            onConsumePrefill()
        }
    }

    LaunchedEffect(draftAiState) {
        if (draftAiState is AiUiState.Success) {
            val data = draftAiState.data
            activeTags.clear()
            activeTags.addAll(data.tags)
            aiSummaryText = data.summary
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        val remaining = (3 - selectedPhotoUris.size).coerceAtLeast(0)
        selectedPhotoUris.addAll(uris.take(remaining))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasOffWhite)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 112.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Text("List on CampusVault", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Posting as ${user.fullName.ifBlank { "Member" }} • ${user.batchTag}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Photos (${selectedPhotoUris.size}/3)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(selectedPhotoUris, key = { it.toString() }) { uri ->
                        EditablePhotoThumb(
                            model = uri,
                            onRemove = { selectedPhotoUris.remove(uri) }
                        )
                    }
                    if (selectedPhotoUris.size < 3) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(78.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceSubtle)
                                    .testTag("add_photo_button")
                                    .clickable {
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CameraIcon(color = SunsetOrange, size = 22.dp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Add Photo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SunsetOrange)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Category", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CampusConstants.POST_CATEGORIES) { cat ->
                        val active = cat == category
                        val accent = categoryAccent(cat)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) accent else SurfaceSubtle)
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text(
                                cat,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) PureWhite else TextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                PillInputField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Title (e.g., Operating Systems Notes)",
                    modifier = Modifier.testTag("post_title_input")
                )

                Spacer(Modifier.height(12.dp))

                PillInputField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Short details, edition, or pickup notes",
                    singleLine = false,
                    modifier = Modifier.testTag("post_description_input")
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = VioletLight.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, VioletPop.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SparkleIcon(color = VioletPop, size = 18.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "AI Study Tag & Summary",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = VioletPop
                                )
                            }
                            if (draftAiState is AiUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = VioletPop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(VioletPop)
                                        .testTag("ai_assist_button")
                                        .clickable {
                                            if (title.isBlank()) {
                                                localError = "Enter a title first so AI can summarize and tag it."
                                            } else {
                                                localError = null
                                                onRequestAiAssist(
                                                    title.trim(),
                                                    description.trim(),
                                                    category,
                                                    null,
                                                    resourceLink.trim().ifEmpty { null },
                                                    selectedPhotoUris.firstOrNull()
                                                )
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        if (draftAiState is AiUiState.Success) "Refresh" else "Auto-Tag",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                }
                            }
                        }

                        when (draftAiState) {
                            is AiUiState.Idle -> {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Generate semester/topic tags and a polished overview from your title or first photo.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            is AiUiState.Loading -> {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Analyzing listing details with Gemini...",
                                    fontSize = 12.sp,
                                    color = VioletPop
                                )
                            }
                            is AiUiState.Error -> {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    draftAiState.message,
                                    fontSize = 12.sp,
                                    color = AlertCrimson,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            is AiUiState.Success -> {
                                val data = draftAiState.data
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    data.summary,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 17.sp
                                )
                                if (activeTags.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        activeTags.forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(PureWhite)
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    "#$tag",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VioletPop
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(EmeraldLight)
                                            .clickable {
                                                description = data.polishedDescription
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "Use AI Description",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(PureWhite)
                                            .clickable {
                                                activeTags.clear()
                                                aiSummaryText = null
                                                onClearAiAssist()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "Clear",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Listing Type", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampusConstants.PRICING_MODES.forEach { mode ->
                        val active = mode == pricingMode
                        val modeColor = when (mode) {
                            "Free" -> EmeraldGreen
                            "Borrow" -> SkyBlue
                            else -> SunsetOrange
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (active) modeColor else SurfaceSubtle)
                                .clickable {
                                    pricingMode = mode
                                    if (mode == "Free" || mode == "Borrow") price = ""
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                mode,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) PureWhite else TextPrimary
                            )
                        }
                    }
                }

                if (pricingMode == "Paid") {
                    Spacer(Modifier.height(12.dp))
                    PillInputField(
                        value = price,
                        onValueChange = { price = it.filter { ch -> ch.isDigit() }.take(6) },
                        placeholder = "Price in ₹ (e.g., 150)",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.testTag("post_price_input")
                    )
                } else if (pricingMode == "Borrow") {
                    Spacer(Modifier.height(14.dp))
                    Text("Borrow Return Window", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CampusConstants.BORROW_WINDOWS) { option ->
                            val (label, _) = option
                            val active = label == selectedBorrowWindow.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (active) SkyBlue else SurfaceSubtle)
                                    .clickable { selectedBorrowWindow = option }
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) PureWhite else TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Condition", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampusConstants.CONDITIONS.forEach { cond ->
                        val active = cond == condition
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (active) VioletPop else SurfaceSubtle)
                                .clickable { condition = cond }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                cond,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) PureWhite else TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Campus Meetup Spot", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CampusConstants.MEETUP_SPOTS) { spot ->
                        val active = spot == meetupSpot
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) EmeraldGreen else SurfaceSubtle)
                                .clickable { meetupSpot = spot }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text(
                                spot,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) PureWhite else TextPrimary
                            )
                        }
                    }
                }

                if (category == "Notes" || category == "Books") {
                    Spacer(Modifier.height(16.dp))
                    PillInputField(
                        value = resourceLink,
                        onValueChange = { resourceLink = it },
                        placeholder = "Google Drive / PDF HTTPS link (optional)",
                        keyboardType = KeyboardType.Uri
                    )
                }

                val feedback = localError ?: errorMessage
                if (!feedback.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        feedback,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlertCrimson
                    )
                }

                Spacer(Modifier.height(20.dp))

                SpringActionButton(
                    text = if (isPosting) "Publishing..." else "Publish Listing",
                    gradient = OrangeAccentGradient,
                    enabled = !isPosting,
                    onClick = {
                        val parsedAmount = price.toLongOrNull() ?: 0L
                        val trimmedLink = resourceLink.trim()
                        when {
                            title.isBlank() -> localError = "Please enter a title"
                            pricingMode == "Paid" && parsedAmount <= 0L -> localError = "Enter a valid price or choose Free / Borrow"
                            trimmedLink.isNotEmpty() && !trimmedLink.startsWith("https://") && !trimmedLink.startsWith("http://") ->
                                localError = "Notes link must start with https:// or http://"
                            else -> {
                                localError = null
                                onPublish(
                                    title.trim(),
                                    description.trim(),
                                    category,
                                    pricingMode,
                                    parsedAmount,
                                    if (pricingMode == "Borrow") selectedBorrowWindow.second else null,
                                    if (pricingMode == "Borrow") selectedBorrowWindow.first else null,
                                    condition,
                                    meetupSpot,
                                    category == "Notes" || trimmedLink.isNotEmpty(),
                                    null,
                                    trimmedLink.ifEmpty { null },
                                    selectedPhotoUris.toList(),
                                    activeTags.toList(),
                                    aiSummaryText
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_listing_button")
                )
            }
        }
    }
}

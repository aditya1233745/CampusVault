package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.CampusImageProcessor
import com.example.model.AiUiState
import com.example.model.SellerTrustStats
import com.example.model.StudyGuideResult
import com.example.model.VaultListing
import com.example.ui.components.BackIcon
import com.example.ui.components.BookmarkIcon
import com.example.ui.components.CategoryArtIcon
import com.example.ui.components.EditIcon
import com.example.ui.components.PhoneIcon
import com.example.ui.components.PillInputField
import com.example.ui.components.SparkleIcon
import com.example.ui.components.SpringActionButton
import com.example.ui.components.StarIcon
import com.example.ui.components.UploadIcon
import com.example.ui.components.categoryAccent
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CanvasOffWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GreenActionGradient
import com.example.ui.theme.OrangeAccentGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SkyLight
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletLight
import com.example.ui.theme.VioletPop

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    item: VaultListing,
    currentUserId: String,
    currentUserPhone: String,
    sellerTrust: SellerTrustStats?,
    isSaved: Boolean,
    isClaimed: Boolean,
    isBusy: Boolean,
    studyGuideState: AiUiState<StudyGuideResult>,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onClaim: () -> Unit,
    onSendRequest: (message: String, buyerPhone: String, offeredPriceAmount: Long?) -> Unit,
    onEditClick: () -> Unit,
    onMarkSold: () -> Unit,
    onDelete: () -> Unit,
    onGenerateStudyGuide: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val accent = categoryAccent(item.category)
    val isOwner = item.sellerId == currentUserId
    var showRequestDialog by remember { mutableStateOf(false) }
    var pickupMessage by remember(item.id) { mutableStateOf("Hi ${item.sellerName}, can we meet at ${item.meetupSpot}?") }
    var buyerPhone by remember(item.id, currentUserPhone) { mutableStateOf(currentUserPhone) }
    var offerPriceText by remember(item.id) {
        mutableStateOf(if (item.pricingMode == "Paid" && item.priceAmount > 0L) item.priceAmount.toString() else "")
    }
    var requestDialogError by remember { mutableStateOf<String?>(null) }
    val selectedQuizAnswers = remember(item.id) { mutableStateMapOf<Int, Int>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasOffWhite)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceSubtle)
                            .testTag("detail_back_button")
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        BackIcon(color = TextPrimary, size = 18.dp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.14f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            item.category.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isSaved) SunsetOrange.copy(alpha = 0.14f) else SurfaceSubtle)
                            .testTag("detail_bookmark_button")
                            .clickable(onClick = onToggleSave),
                        contentAlignment = Alignment.Center
                    ) {
                        BookmarkIcon(
                            filled = isSaved,
                            color = if (isSaved) SunsetOrange else TextSecondary,
                            size = 18.dp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (item.photoUrls.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { item.photoUrls.size })
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceSubtle)
                ) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        val rawSource = item.photoUrls[page]
                        val photoModel = remember(rawSource) { CampusImageProcessor.toCoilModel(rawSource) }
                        AsyncImage(
                            model = photoModel,
                            contentDescription = "${item.title} photo ${page + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (item.photoUrls.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(TextPrimary.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            repeat(item.photoUrls.size) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(if (pagerState.currentPage == idx) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (pagerState.currentPage == idx) PureWhite else PureWhite.copy(alpha = 0.45f))
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = PureWhite,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryArtIcon(category = item.category, tint = accent, size = 24.dp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (item.isFree) "FREE" else item.price,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (item.isFree) EmeraldGreen else TextPrimary
                                )
                                if (item.hasPriceDrop && item.originalPriceAmount != null) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "₹${item.originalPriceAmount}",
                                        fontSize = 15.sp,
                                        color = TextSecondary,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(EmeraldLight)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("Price Drop", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                                    }
                                }
                            }
                            Text(
                                "${item.pricingMode} • ${item.condition}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(item.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(item.description, style = MaterialTheme.typography.bodyMedium)

                    if (item.studyTags.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item.studyTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(VioletLight)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("#$tag", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VioletPop)
                                }
                            }
                        }
                    }

                    if (!item.aiSummary.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(VioletLight.copy(alpha = 0.55f))
                                .padding(12.dp)
                        ) {
                            Text(
                                item.aiSummary,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    val sellerLabel = buildString {
                        append("${item.sellerName} (${item.batchTag})")
                        if (sellerTrust != null && sellerTrust.reviewCount > 0) {
                            append(" • ★ ${String.format("%.1f", sellerTrust.averageScore)} (${sellerTrust.reviewCount})")
                        }
                    }
                    DetailRowChip("Seller", sellerLabel, VioletPop)
                    Spacer(Modifier.height(8.dp))
                    DetailRowChip("Meetup Spot", item.meetupSpot, EmeraldGreen)

                    if (item.pricingMode == "Borrow") {
                        Spacer(Modifier.height(8.dp))
                        DetailRowChip("Return Window", item.borrowWindowLabel ?: "7 Days", SkyBlue)
                    }

                    if (!item.attachmentName.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        DetailRowChip("Attachment", item.attachmentName, SkyBlue)
                    }

                    if (!item.resourceLink.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SkyLight)
                                .clickable {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.resourceLink)))
                                    }
                                    onClaim()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UploadIcon(color = SkyBlue, size = 18.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Open Shared Study Link",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyBlue
                                )
                            }
                            Text("Open", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = SkyBlue)
                        }
                    }

                    if (isOwner) {
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!item.isSold) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50))
                                        .background(SkyLight)
                                        .testTag("detail_edit_button")
                                        .clickable(onClick = onEditClick)
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        EditIcon(color = SkyBlue, size = 14.dp)
                                        Spacer(Modifier.width(5.dp))
                                        Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyBlue)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50))
                                        .background(EmeraldLight)
                                        .clickable(onClick = onMarkSold)
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Mark Sold", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(AlertCrimson.copy(alpha = 0.1f))
                                    .clickable(onClick = onDelete)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AlertCrimson)
                            }
                        }
                    }
                }
            }

            if (item.category in listOf("Notes", "Books")) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SparkleIcon(color = VioletPop, size = 20.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("AI Study Guide & Quiz", style = MaterialTheme.typography.titleMedium)
                            }
                            if (studyGuideState !is AiUiState.Loading) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(VioletPop)
                                        .testTag("generate_study_guide_button")
                                        .clickable(onClick = onGenerateStudyGuide)
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        if (studyGuideState is AiUiState.Success) "Regenerate" else "Generate",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                }
                            }
                        }

                        when (studyGuideState) {
                            is AiUiState.Idle -> {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Tap Generate to get an AI topic breakdown and 3 quick practice questions for \"${item.title}\".",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            is AiUiState.Loading -> {
                                Spacer(Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = VioletPop)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Building summary & practice quiz with Gemini...", fontSize = 12.sp, color = VioletPop)
                                }
                            }
                            is AiUiState.Error -> {
                                Spacer(Modifier.height(10.dp))
                                Text(studyGuideState.message, fontSize = 12.sp, color = AlertCrimson, fontWeight = FontWeight.SemiBold)
                            }
                            is AiUiState.Success -> {
                                val guide = studyGuideState.data
                                Spacer(Modifier.height(12.dp))
                                Text(guide.summary, fontSize = 13.sp, color = TextPrimary, lineHeight = 19.sp)

                                if (guide.keyTopics.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        guide.keyTopics.forEach { topic ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(VioletLight)
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(topic, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VioletPop)
                                            }
                                        }
                                    }
                                }

                                guide.quiz.forEachIndexed { qIndex, q ->
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        "Q${qIndex + 1}. ${q.question}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    val chosen = selectedQuizAnswers[qIndex]
                                    q.options.forEachIndexed { oIndex, opt ->
                                        val isPicked = chosen == oIndex
                                        val isCorrect = oIndex == q.correctIndex
                                        val bg = when {
                                            chosen == null -> SurfaceSubtle
                                            isCorrect -> EmeraldLight
                                            isPicked -> AlertCrimson.copy(alpha = 0.12f)
                                            else -> SurfaceSubtle
                                        }
                                        val fg = when {
                                            chosen == null -> TextPrimary
                                            isCorrect -> EmeraldGreen
                                            isPicked -> AlertCrimson
                                            else -> TextSecondary
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(enabled = chosen == null) {
                                                    selectedQuizAnswers[qIndex] = oIndex
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = bg,
                                            border = if (isPicked) BorderStroke(1.dp, fg.copy(alpha = 0.4f)) else null
                                        ) {
                                            Text(
                                                opt,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = fg,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                            )
                                        }
                                    }
                                    if (chosen != null && q.explanation.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            q.explanation,
                                            fontSize = 11.sp,
                                            color = EmeraldGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PureWhite,
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                if (isOwner) {
                    SpringActionButton(
                        text = if (item.isSold) "Already Sold" else "Edit Listing or Price",
                        gradient = OrangeAccentGradient,
                        enabled = !item.isSold,
                        onClick = onEditClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val ctaLabel = when {
                        item.isSold -> "No Longer Available"
                        item.isFree || item.pricingMode == "Borrow" ->
                            if (isClaimed) "Claimed • Send Pickup Note" else "Claim & Request Pickup"
                        else -> "Request Item on Campus"
                    }
                    SpringActionButton(
                        text = ctaLabel,
                        gradient = GreenActionGradient,
                        enabled = !item.isSold && !isBusy,
                        onClick = {
                            if ((item.isFree || item.pricingMode == "Borrow") && !isClaimed) {
                                onClaim()
                            }
                            showRequestDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_primary_action")
                    )
                }
            }
        }
    }

    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = {
                requestDialogError = null
                showRequestDialog = false
            },
            containerColor = PureWhite,
            title = {
                Text(
                    if (item.pricingMode == "Paid") "Request or Make Offer" else "Request Pickup",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text(
                        "Share your phone number so ${item.sellerName} can call you for ${item.title}.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    PillInputField(
                        value = buyerPhone,
                        onValueChange = {
                            buyerPhone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' }.take(20)
                            requestDialogError = null
                        },
                        placeholder = "Your Phone Number (for seller call)",
                        keyboardType = KeyboardType.Phone,
                        leadingIcon = { PhoneIcon(color = EmeraldGreen, size = 18.dp) },
                        modifier = Modifier.testTag("request_phone_input")
                    )
                    if (item.pricingMode == "Paid") {
                        Spacer(Modifier.height(10.dp))
                        PillInputField(
                            value = offerPriceText,
                            onValueChange = {
                                offerPriceText = it.filter { ch -> ch.isDigit() }.take(6)
                                requestDialogError = null
                            },
                            placeholder = "Offer Price in ₹ (asking ₹${item.priceAmount})",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.testTag("request_offer_input")
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    PillInputField(
                        value = pickupMessage,
                        onValueChange = { pickupMessage = it },
                        placeholder = "Pickup message",
                        singleLine = false
                    )
                    if (!requestDialogError.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            requestDialogError!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AlertCrimson
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val digits = buyerPhone.filter { it.isDigit() }
                        val parsedOffer = offerPriceText.toLongOrNull()
                        when {
                            digits.length < 7 -> {
                                requestDialogError = "Enter a valid phone number so the seller can call you."
                            }
                            item.pricingMode == "Paid" && (parsedOffer == null || parsedOffer <= 0L) -> {
                                requestDialogError = "Enter a valid offer price in ₹."
                            }
                            else -> {
                                requestDialogError = null
                                showRequestDialog = false
                                onSendRequest(
                                    pickupMessage,
                                    buyerPhone.trim(),
                                    if (item.pricingMode == "Paid") parsedOffer else null
                                )
                            }
                        }
                    }
                ) {
                    Text("Send Request", color = EmeraldGreen, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    requestDialogError = null
                    showRequestDialog = false
                }) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun DetailRowChip(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceSubtle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.CampusImageProcessor
import com.example.model.CampusConstants
import com.example.model.VaultListing
import com.example.ui.components.BackIcon
import com.example.ui.components.CameraIcon
import com.example.ui.components.CloseIcon
import com.example.ui.components.PillInputField
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
import com.example.ui.theme.VioletPop

@Composable
fun EditListingScreen(
    listing: VaultListing,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: String,
        pricingMode: String,
        priceAmount: Long,
        borrowDurationDays: Long?,
        borrowWindowLabel: String?,
        condition: String,
        meetupSpot: String,
        resourceLink: String?,
        keptPhotoUrls: List<String>,
        newPhotoUris: List<Uri>
    ) -> Unit
) {
    BackHandler(onBack = onBack)

    var title by remember(listing.id) { mutableStateOf(listing.title) }
    var description by remember(listing.id) { mutableStateOf(listing.description) }
    var category by remember(listing.id) { mutableStateOf(listing.category) }
    var pricingMode by remember(listing.id) { mutableStateOf(listing.pricingMode) }
    var price by remember(listing.id) {
        mutableStateOf(if (listing.priceAmount > 0L) listing.priceAmount.toString() else "")
    }
    var selectedBorrowWindow by remember(listing.id) {
        mutableStateOf(
            CampusConstants.BORROW_WINDOWS.firstOrNull {
                it.first == listing.borrowWindowLabel || it.second == listing.borrowDurationDays
            } ?: CampusConstants.BORROW_WINDOWS[1]
        )
    }
    var condition by remember(listing.id) { mutableStateOf(listing.condition) }
    var meetupSpot by remember(listing.id) { mutableStateOf(listing.meetupSpot) }
    var resourceLink by remember(listing.id) { mutableStateOf(listing.resourceLink.orEmpty()) }
    var localError by remember { mutableStateOf<String?>(null) }

    val keptUrls = remember(listing.id) { mutableStateListOf<String>().apply { addAll(listing.photoUrls) } }
    val newUris = remember(listing.id) { mutableStateListOf<Uri>() }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        val slots = (3 - keptUrls.size - newUris.size).coerceAtLeast(0)
        newUris.addAll(uris.take(slots))
    }

    val currentAmount = price.toLongOrNull() ?: 0L
    val isPriceDrop = pricingMode == "Paid" &&
        listing.pricingMode == "Paid" &&
        currentAmount in 1L until (listing.originalPriceAmount ?: listing.priceAmount)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasOffWhite)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PureWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceSubtle)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    BackIcon(color = TextPrimary, size = 18.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Edit Listing", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Update details or drop the price",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = PureWhite,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Photos (${keptUrls.size + newUris.size}/3)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(keptUrls, key = { it }) { url ->
                            EditablePhotoThumb(
                                model = url,
                                onRemove = { keptUrls.remove(url) }
                            )
                        }
                        items(newUris, key = { it.toString() }) { uri ->
                            EditablePhotoThumb(
                                model = uri,
                                onRemove = { newUris.remove(uri) }
                            )
                        }
                        if (keptUrls.size + newUris.size < 3) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(78.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SurfaceSubtle)
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
                                        Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SunsetOrange)
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

                    Spacer(Modifier.height(14.dp))
                    PillInputField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "Title",
                        modifier = Modifier.testTag("edit_title_input")
                    )

                    Spacer(Modifier.height(12.dp))
                    PillInputField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "Description",
                        singleLine = false,
                        modifier = Modifier.testTag("edit_description_input")
                    )

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
                            placeholder = "Price in ₹",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.testTag("edit_price_input")
                        )
                        if (isPriceDrop) {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(EmeraldLight)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Price Drop badge will highlight ₹${listing.originalPriceAmount ?: listing.priceAmount} → ₹$currentAmount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }
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

                    if (category == "Notes" || resourceLink.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        PillInputField(
                            value = resourceLink,
                            onValueChange = { resourceLink = it },
                            placeholder = "Shared notes HTTPS link (optional)",
                            keyboardType = KeyboardType.Uri
                        )
                    }

                    if (!localError.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(localError!!, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AlertCrimson)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PureWhite,
            shadowElevation = 10.dp
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                SpringActionButton(
                    text = if (isBusy) "Saving..." else "Save Changes",
                    gradient = OrangeAccentGradient,
                    enabled = !isBusy,
                    onClick = {
                        val parsedAmount = price.toLongOrNull() ?: 0L
                        val trimmedLink = resourceLink.trim()
                        when {
                            title.isBlank() -> localError = "Please enter a title"
                            pricingMode == "Paid" && parsedAmount <= 0L -> localError = "Enter a valid price or select Free / Borrow"
                            trimmedLink.isNotEmpty() && !trimmedLink.startsWith("https://") && !trimmedLink.startsWith("http://") ->
                                localError = "Notes link must start with https:// or http://"
                            else -> {
                                localError = null
                                onSave(
                                    title.trim(),
                                    description.trim(),
                                    category,
                                    pricingMode,
                                    parsedAmount,
                                    if (pricingMode == "Borrow") selectedBorrowWindow.second else null,
                                    if (pricingMode == "Borrow") selectedBorrowWindow.first else null,
                                    condition,
                                    meetupSpot,
                                    trimmedLink.ifEmpty { null },
                                    keptUrls.toList(),
                                    newUris.toList()
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_edit_listing_button")
                )
            }
        }
    }
}

@Composable
fun EditablePhotoThumb(
    model: Any,
    onRemove: () -> Unit
) {
    val coilModel = remember(model) {
        if (model is String) CampusImageProcessor.toCoilModel(model) else model
    }
    Box(modifier = Modifier.size(78.dp)) {
        AsyncImage(
            model = coilModel,
            contentDescription = "Listing photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceSubtle)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(TextPrimary.copy(alpha = 0.78f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            CloseIcon(color = PureWhite, size = 11.dp)
        }
    }
}

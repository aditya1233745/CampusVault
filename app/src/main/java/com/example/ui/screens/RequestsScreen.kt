package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CampusRequest
import com.example.model.UiState
import com.example.ui.components.ChatIcon
import com.example.ui.components.PhoneIcon
import com.example.ui.components.PillInputField
import com.example.ui.components.StarIcon
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CanvasOffWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SkyLight
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletLight
import com.example.ui.theme.VioletPop
import kotlin.math.ceil

@Composable
fun RequestsScreen(
    currentUserId: String,
    requestsState: UiState<List<CampusRequest>>,
    ratedRequestIds: Set<String>,
    onUpdateStatus: (requestId: String, newStatus: String) -> Unit,
    onAcceptWithPin: (CampusRequest) -> Unit,
    onCounterOffer: (requestId: String, counterAmount: Long) -> Unit,
    onAcceptCounterOffer: (CampusRequest) -> Unit,
    onCompleteWithPin: (CampusRequest, enteredPin: String) -> Unit,
    onMarkReturned: (requestId: String) -> Unit,
    onOpenChat: (CampusRequest) -> Unit,
    onSubmitRating: (request: CampusRequest, score: Int, comment: String) -> Unit,
    onRetry: () -> Unit = {}
) {
    var selectedSegment by remember { mutableIntStateOf(0) } // 0 = Incoming, 1 = Sent
    var ratingTarget by remember { mutableStateOf<CampusRequest?>(null) }
    var counterTarget by remember { mutableStateOf<CampusRequest?>(null) }
    var pinVerifyTarget by remember { mutableStateOf<CampusRequest?>(null) }

    val requests = remember(requestsState) {
        when (requestsState) {
            is UiState.Success -> requestsState.data
            else -> emptyList()
        }
    }

    val displayed = remember(requests, selectedSegment, currentUserId) {
        if (selectedSegment == 0) requests.filter { it.sellerId == currentUserId }
        else requests.filter { it.buyerId == currentUserId }
    }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text("Campus Requests", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Offers, PIN handovers, returns & meetup chat",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SegmentTabButton(
                        label = "Incoming (${requests.count { it.sellerId == currentUserId }})",
                        selected = selectedSegment == 0,
                        onClick = { selectedSegment = 0 },
                        modifier = Modifier.weight(1f).testTag("requests_incoming_tab")
                    )
                    SegmentTabButton(
                        label = "Sent (${requests.count { it.buyerId == currentUserId }})",
                        selected = selectedSegment == 1,
                        onClick = { selectedSegment = 1 },
                        modifier = Modifier.weight(1f).testTag("requests_sent_tab")
                    )
                }
            }
        }

        when (requestsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SkyBlue)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(requestsState.message, color = AlertCrimson, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = onRetry) {
                            Text("Retry", color = SkyBlue, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            is UiState.Success -> {
                if (displayed.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp, vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SkyLight),
                                contentAlignment = Alignment.Center
                            ) {
                                ChatIcon(color = SkyBlue, size = 28.dp)
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                if (selectedSegment == 0) "No Incoming Requests" else "No Sent Requests",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (selectedSegment == 0) "Requests from students interested in your listings will appear here."
                                else "Tap Request Item on any listing to coordinate a campus meetup.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayed, key = { it.id }) { req ->
                            RequestCard(
                                request = req,
                                isIncoming = selectedSegment == 0,
                                isRated = req.id in ratedRequestIds,
                                onAcceptWithPin = { onAcceptWithPin(req) },
                                onCounterClick = { counterTarget = req },
                                onAcceptCounter = { onAcceptCounterOffer(req) },
                                onVerifyPinClick = {
                                    if (req.handoverPin.isNullOrBlank()) onCompleteWithPin(req, "")
                                    else pinVerifyTarget = req
                                },
                                onMarkReturned = { onMarkReturned(req.id) },
                                onUpdateStatus = { newStatus -> onUpdateStatus(req.id, newStatus) },
                                onOpenChat = { onOpenChat(req) },
                                onRateClick = { ratingTarget = req }
                            )
                        }
                    }
                }
            }
        }
    }

    ratingTarget?.let { target ->
        RateHandoverDialog(
            request = target,
            onDismiss = { ratingTarget = null },
            onSubmit = { score, comment ->
                onSubmitRating(target, score, comment)
                ratingTarget = null
            }
        )
    }

    counterTarget?.let { target ->
        CounterOfferDialog(
            request = target,
            onDismiss = { counterTarget = null },
            onSubmit = { amount ->
                onCounterOffer(target.id, amount)
                counterTarget = null
            }
        )
    }

    pinVerifyTarget?.let { target ->
        VerifyPinDialog(
            request = target,
            onDismiss = { pinVerifyTarget = null },
            onSubmit = { enteredPin ->
                onCompleteWithPin(target, enteredPin)
                pinVerifyTarget = null
            }
        )
    }
}

@Composable
private fun SegmentTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) SkyBlue else SurfaceSubtle
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) PureWhite else TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun RequestCard(
    request: CampusRequest,
    isIncoming: Boolean,
    isRated: Boolean,
    onAcceptWithPin: () -> Unit,
    onCounterClick: () -> Unit,
    onAcceptCounter: () -> Unit,
    onVerifyPinClick: () -> Unit,
    onMarkReturned: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onOpenChat: () -> Unit,
    onRateClick: () -> Unit
) {
    val context = LocalContext.current
    val (statusBg, statusColor) = when (request.status) {
        "accepted" -> EmeraldLight to EmeraldGreen
        "completed" -> SkyLight to SkyBlue
        "declined", "cancelled" -> AlertCrimson.copy(alpha = 0.1f) to AlertCrimson
        else -> OrangeLight to SunsetOrange
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PureWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.listingTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isIncoming) "From ${request.buyerName} (${request.buyerBatch})"
                        else "Seller: ${request.sellerName}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        request.status.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
            }

            if (request.negotiationStatus in listOf("offered", "countered", "accepted") &&
                (request.offeredPriceAmount != null || request.counterPriceAmount != null || request.agreedPriceAmount != null)
            ) {
                Spacer(Modifier.height(8.dp))
                val (badgeText, badgeBg, badgeFg) = when (request.negotiationStatus) {
                    "offered" -> Triple(
                        "Buyer Offer: ₹${request.offeredPriceAmount ?: 0} (Asking ${request.listingPrice})",
                        OrangeLight,
                        SunsetOrange
                    )
                    "countered" -> Triple(
                        "Seller Counter-Offer: ₹${request.counterPriceAmount ?: 0}",
                        VioletLight,
                        VioletPop
                    )
                    else -> Triple(
                        "Agreed Price: ₹${request.agreedPriceAmount ?: 0}",
                        EmeraldLight,
                        EmeraldGreen
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(badgeText, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = badgeFg)
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                request.message,
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Meetup: ${request.meetupSpot}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldGreen
                )
                Text(
                    request.listingPrice,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            if (!isIncoming && request.status == "accepted" && !request.handoverPin.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(VioletLight)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Pickup PIN: ${request.handoverPin}  •  Share with ${request.sellerName} at meetup",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VioletPop
                    )
                }
            }

            if (request.status == "completed" && request.pricingMode == "Borrow") {
                Spacer(Modifier.height(10.dp))
                val (borrowText, borrowBg, borrowFg) = formatBorrowStatusBanner(request)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(borrowBg)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(borrowText, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = borrowFg)
                }
            }

            if (isIncoming && !request.buyerPhone.isNullOrBlank() && request.status in listOf("pending", "accepted", "completed")) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .testTag("call_buyer_${request.id}")
                        .clickable {
                            val cleanPhone = request.buyerPhone.filter { it.isDigit() || it == '+' }
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")))
                            }
                        },
                    shape = RoundedCornerShape(50),
                    color = EmeraldLight,
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PhoneIcon(color = EmeraldGreen, size = 15.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Call ${request.buyerName} (${request.buyerPhone})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                    }
                }
            }

            if (request.status in listOf("pending", "accepted", "completed")) {
                Spacer(Modifier.height(10.dp))
                SmallActionPill(
                    label = if (request.status == "completed") "View Meetup Chat" else "Open Meetup Chat",
                    bg = SkyLight,
                    fg = SkyBlue,
                    onClick = onOpenChat,
                    modifier = Modifier.fillMaxWidth().testTag("open_chat_${request.id}")
                )
            }

            if (isIncoming && request.status == "pending") {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallActionPill(
                        label = if (request.negotiationStatus == "offered") "Accept Offer" else "Accept",
                        bg = EmeraldLight,
                        fg = EmeraldGreen,
                        onClick = onAcceptWithPin,
                        modifier = Modifier.weight(1f).testTag("accept_request_${request.id}")
                    )
                    if (request.pricingMode == "Paid" && request.negotiationStatus != "countered") {
                        SmallActionPill(
                            label = "Counter",
                            bg = VioletLight,
                            fg = VioletPop,
                            onClick = onCounterClick,
                            modifier = Modifier.weight(1f).testTag("counter_request_${request.id}")
                        )
                    }
                    SmallActionPill(
                        label = "Decline",
                        bg = AlertCrimson.copy(alpha = 0.1f),
                        fg = AlertCrimson,
                        onClick = { onUpdateStatus("declined") },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (!isIncoming && request.status == "pending" && request.negotiationStatus == "countered") {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallActionPill(
                        label = "Accept Counter ₹${request.counterPriceAmount ?: 0}",
                        bg = EmeraldLight,
                        fg = EmeraldGreen,
                        onClick = onAcceptCounter,
                        modifier = Modifier.weight(1f).testTag("accept_counter_${request.id}")
                    )
                    SmallActionPill(
                        label = "Cancel",
                        bg = SurfaceSubtle,
                        fg = TextSecondary,
                        borderColor = BorderLight,
                        onClick = { onUpdateStatus("cancelled") },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (isIncoming && request.status == "accepted") {
                Spacer(Modifier.height(8.dp))
                SmallActionPill(
                    label = if (request.handoverPin.isNullOrBlank()) "Mark Handover Complete" else "Verify PIN & Complete",
                    bg = EmeraldLight,
                    fg = EmeraldGreen,
                    onClick = onVerifyPinClick,
                    modifier = Modifier.fillMaxWidth().testTag("verify_pin_complete_${request.id}")
                )
            } else if (isIncoming && request.status == "completed" &&
                request.pricingMode == "Borrow" && request.borrowReturnStatus == "borrowed"
            ) {
                Spacer(Modifier.height(8.dp))
                SmallActionPill(
                    label = "Mark Borrowed Item Returned",
                    bg = EmeraldLight,
                    fg = EmeraldGreen,
                    onClick = onMarkReturned,
                    modifier = Modifier.fillMaxWidth().testTag("mark_returned_${request.id}")
                )
            } else if (!isIncoming && request.status in listOf("pending", "accepted")) {
                Spacer(Modifier.height(8.dp))
                SmallActionPill(
                    label = "Cancel Request",
                    bg = SurfaceSubtle,
                    fg = TextSecondary,
                    borderColor = BorderLight,
                    onClick = { onUpdateStatus("cancelled") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!isIncoming && request.status == "completed") {
                Spacer(Modifier.height(8.dp))
                if (isRated) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(EmeraldLight)
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "★ Handover Rated",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                } else {
                    SmallActionPill(
                        label = "Rate Handover ★",
                        bg = OrangeLight,
                        fg = SunsetOrange,
                        onClick = onRateClick,
                        modifier = Modifier.fillMaxWidth().testTag("rate_handover_${request.id}")
                    )
                }
            }
        }
    }
}

private fun formatBorrowStatusBanner(request: CampusRequest): Triple<String, Color, Color> {
    if (request.borrowReturnStatus == "returned") {
        return Triple("Borrow Return: Returned ✓", EmeraldLight, EmeraldGreen)
    }
    val dueMillis = request.borrowDueAt?.toDate()?.time
    if (dueMillis == null) {
        val days = request.borrowDurationDays ?: 7L
        return Triple("Borrow Return Window: $days days", SkyLight, SkyBlue)
    }
    val diffMillis = dueMillis - System.currentTimeMillis()
    val daysRemaining = ceil(diffMillis.toDouble() / 86_400_000.0).toLong()
    return when {
        daysRemaining < 0L -> Triple(
            "Overdue by ${-daysRemaining} day(s) — Follow up for return",
            AlertCrimson.copy(alpha = 0.12f),
            AlertCrimson
        )
        daysRemaining == 0L -> Triple("Borrow Return Due Today", OrangeLight, SunsetOrange)
        else -> Triple("Borrow Return Due in $daysRemaining day(s)", SkyLight, SkyBlue)
    }
}

@Composable
private fun VerifyPinDialog(
    request: CampusRequest,
    onDismiss: () -> Unit,
    onSubmit: (enteredPin: String) -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        title = { Text("Verify Handover PIN", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    "Ask ${request.buyerName} for the 4-digit Pickup PIN shown on their Requests screen before marking ${request.listingTitle} complete.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                PillInputField(
                    value = enteredPin,
                    onValueChange = {
                        enteredPin = it.filter { ch -> ch.isDigit() }.take(4)
                        localError = null
                    },
                    placeholder = "4-digit PIN (e.g., 4829)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.testTag("verify_pin_input")
                )
                if (!localError.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(localError!!, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AlertCrimson)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (enteredPin.length != 4) {
                        localError = "Enter the 4-digit PIN."
                    } else {
                        onSubmit(enteredPin)
                    }
                }
            ) {
                Text("Verify & Complete", color = EmeraldGreen, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun CounterOfferDialog(
    request: CampusRequest,
    onDismiss: () -> Unit,
    onSubmit: (counterAmount: Long) -> Unit
) {
    var counterText by remember {
        mutableStateOf(
            (request.offeredPriceAmount ?: request.askingPriceAmount ?: 100L).toString()
        )
    }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        title = { Text("Send Counter-Offer", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    "Propose your counter-offer price to ${request.buyerName} for ${request.listingTitle}.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                PillInputField(
                    value = counterText,
                    onValueChange = {
                        counterText = it.filter { ch -> ch.isDigit() }.take(6)
                        localError = null
                    },
                    placeholder = "Counter price in ₹",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.testTag("counter_offer_input")
                )
                if (!localError.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(localError!!, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AlertCrimson)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = counterText.toLongOrNull()
                    if (amount == null || amount !in 1L..500000L) {
                        localError = "Enter a valid price in ₹."
                    } else {
                        onSubmit(amount)
                    }
                }
            ) {
                Text("Send Counter", color = VioletPop, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun RateHandoverDialog(
    request: CampusRequest,
    onDismiss: () -> Unit,
    onSubmit: (score: Int, comment: String) -> Unit
) {
    var score by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        title = { Text("Rate Handover", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "How was your meetup with ${request.sellerName} for ${request.listingTitle}?",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { star ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (star <= score) OrangeLight else SurfaceSubtle)
                                .clickable { score = star },
                            contentAlignment = Alignment.Center
                        ) {
                            StarIcon(
                                filled = star <= score,
                                color = if (star <= score) SunsetOrange else TextSecondary,
                                size = 22.dp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                PillInputField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = "Short feedback (optional)",
                    singleLine = false
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(score, comment) }) {
                Text("Submit Rating", color = EmeraldGreen, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SmallActionPill(
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = bg,
        border = borderColor?.let { BorderStroke(1.dp, it) }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}


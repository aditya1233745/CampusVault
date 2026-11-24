package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CampusRequest
import com.example.model.RequestMessage
import com.example.model.UiState
import com.example.ui.components.BackIcon
import com.example.ui.components.PhoneIcon
import com.example.ui.components.PillInputField
import com.example.ui.components.SendIcon
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CanvasOffWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SkyLight
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletLight
import com.example.ui.theme.VioletPop

@Composable
fun MeetupChatScreen(
    request: CampusRequest,
    currentUserId: String,
    messagesState: UiState<List<RequestMessage>>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val canSend = request.status in listOf("pending", "accepted")
    val isSeller = request.sellerId == currentUserId
    val peerName = if (request.buyerId == currentUserId) request.sellerName else request.buyerName

    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val quickReplies = remember(request.meetupSpot) {
        listOf(
            "On my way!",
            "Meet at ${request.meetupSpot}?",
            "Free after 4 PM today",
            "Here now!"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasOffWhite)
            .imePadding()
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
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceSubtle)
                        .testTag("chat_back_button")
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    BackIcon(color = TextPrimary, size = 18.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        peerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${request.listingTitle} • ${request.meetupSpot}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isSeller && !request.buyerPhone.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(EmeraldLight)
                            .testTag("chat_call_buyer_button")
                            .clickable {
                                val cleanPhone = request.buyerPhone.filter { it.isDigit() || it == '+' }
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")))
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PhoneIcon(color = EmeraldGreen, size = 14.dp)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Call",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(EmeraldLight)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        request.listingPrice,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreen
                    )
                }
            }
        }

        if (request.status == "accepted" && !request.handoverPin.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VioletLight)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    if (isSeller) "Ask ${request.buyerName} for their 4-digit Pickup PIN to verify handover."
                    else "Your 4-digit Pickup PIN: ${request.handoverPin}  •  Share at meetup",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VioletPop
                )
            }
        } else if (request.negotiationStatus == "countered" && request.counterPriceAmount != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VioletLight)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    "Seller Counter-Offer: ₹${request.counterPriceAmount} (accept in Requests tab)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VioletPop
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SkyLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Initial Pickup Note",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SkyBlue
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(request.message, fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }

            when (messagesState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SkyBlue)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(
                            messagesState.message,
                            color = AlertCrimson,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    items(messages, key = { it.id }) { msg ->
                        val isMine = msg.senderId == currentUserId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isMine) 18.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 18.dp
                                ),
                                color = if (isMine) EmeraldGreen else PureWhite,
                                shadowElevation = 1.dp,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    if (!isMine) {
                                        Text(
                                            "${msg.senderName} • ${msg.senderBatch}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SkyBlue
                                        )
                                        Spacer(Modifier.height(2.dp))
                                    }
                                    Text(
                                        msg.text,
                                        fontSize = 14.sp,
                                        color = if (isMine) PureWhite else TextPrimary
                                    )
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
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (canSend) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(quickReplies) { reply ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(SurfaceSubtle)
                                    .clickable { onSendMessage(reply) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(reply, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillInputField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = "Message $peerName...",
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (draft.isNotBlank()) EmeraldGreen else SurfaceSubtle)
                                .testTag("chat_send_button")
                                .clickable(enabled = draft.isNotBlank()) {
                                    val toSend = draft.trim()
                                    draft = ""
                                    onSendMessage(toSend)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            SendIcon(
                                color = if (draft.isNotBlank()) PureWhite else TextSecondary,
                                size = 20.dp
                            )
                        }
                    }
                } else {
                    Text(
                        "This request is ${request.status}. Chat is now read-only.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

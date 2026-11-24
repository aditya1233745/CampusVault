package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SellerRating
import com.example.model.SellerTrustStats
import com.example.model.UserListingRef
import com.example.model.UserProfile
import com.example.model.VaultListing
import com.example.ui.components.CategoryArtIcon
import com.example.ui.components.ShieldIcon
import com.example.ui.components.StarIcon
import com.example.ui.components.VerifiedCheckIcon
import com.example.ui.components.categoryAccent
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CanvasOffWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.ProfileGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SkyLight
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletLight
import com.example.ui.theme.VioletPop

private enum class ProfileTab(val label: String) {
    ACTIVE("Active"),
    SOLD("Sold"),
    DOWNLOADS("Downloads"),
    SAVED("Saved")
}

@Composable
fun ProfileScreen(
    user: UserProfile,
    allListings: List<VaultListing>,
    savedRefs: List<UserListingRef>,
    claimedRefs: List<UserListingRef>,
    myTrust: SellerTrustStats?,
    myReviews: List<SellerRating>,
    onEditProfile: () -> Unit,
    onItemClick: (VaultListing) -> Unit,
    onEditListing: (VaultListing) -> Unit,
    onMarkSold: (String) -> Unit,
    onDeleteListing: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ProfileTab.ACTIVE) }

    val myListings = remember(allListings, user.userId) {
        allListings.filter { it.sellerId == user.userId }
    }
    val myActive = remember(myListings) { myListings.filter { !it.isSold } }
    val mySold = remember(myListings) { myListings.filter { it.isSold } }
    val listingLookup = remember(allListings) { allListings.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasOffWhite),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = PureWhite,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onEditProfile)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(ProfileGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        user.fullName.take(1).uppercase().ifBlank { "M" },
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PureWhite
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            user.fullName.ifBlank { "Member" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        VerifiedCheckIcon(color = EmeraldGreen, size = 16.dp)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${user.batchTag} • Tap to edit",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                "Sign Out",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertCrimson,
                                modifier = Modifier
                                    .testTag("sign_out_button")
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onSignOut)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(OrangeLight.copy(alpha = 0.6f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StarIcon(filled = true, color = SunsetOrange, size = 16.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (myTrust != null && myTrust.reviewCount > 0)
                                        "${String.format("%.1f", myTrust.averageScore)} Campus Trust (${myTrust.reviewCount} reviews)"
                                    else "New Verified Campus Seller",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SunsetOrange
                                )
                            }
                            if (myReviews.isNotEmpty()) {
                                Text(
                                    myReviews.first().comment.ifBlank { "Verified handover" },
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatColumn(myActive.size.toString(), "Active", SkyBlue, Modifier.weight(1f))
                            ProfileStatColumn(mySold.size.toString(), "Passed On", EmeraldGreen, Modifier.weight(1f))
                            ProfileStatColumn(claimedRefs.size.toString(), "Downloads", VioletPop, Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileTab.entries.forEach { tab ->
                        val active = tab == selectedTab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_tab_${tab.name.lowercase()}")
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedTab = tab },
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) EmeraldGreen else PureWhite,
                            border = BorderStroke(1.dp, if (active) EmeraldGreen else BorderLight)
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) PureWhite else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 11.dp, horizontal = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        when (selectedTab) {
            ProfileTab.ACTIVE -> {
                if (myActive.isEmpty()) {
                    item { EmptyProfileState("Nothing in Active") }
                } else {
                    items(myActive, key = { it.id }) { item ->
                        MyListingRowCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onEdit = { onEditListing(item) },
                            onMarkSold = { onMarkSold(item.id) },
                            onDelete = { onDeleteListing(item.id) }
                        )
                    }
                }
            }
            ProfileTab.SOLD -> {
                if (mySold.isEmpty()) {
                    item { EmptyProfileState("Nothing in Sold") }
                } else {
                    items(mySold, key = { it.id }) { item ->
                        MyListingRowCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onEdit = {},
                            onMarkSold = {},
                            onDelete = { onDeleteListing(item.id) }
                        )
                    }
                }
            }
            ProfileTab.DOWNLOADS -> {
                if (claimedRefs.isEmpty()) {
                    item { EmptyProfileState("Nothing in Downloads") }
                } else {
                    items(claimedRefs, key = { it.listingId }) { ref ->
                        SavedOrClaimedRowCard(
                            ref = ref,
                            onClick = { listingLookup[ref.listingId]?.let(onItemClick) }
                        )
                    }
                }
            }
            ProfileTab.SAVED -> {
                if (savedRefs.isEmpty()) {
                    item { EmptyProfileState("Nothing in Saved") }
                } else {
                    items(savedRefs, key = { it.listingId }) { ref ->
                        SavedOrClaimedRowCard(
                            ref = ref,
                            onClick = { listingLookup[ref.listingId]?.let(onItemClick) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatColumn(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
    }
}

@Composable
private fun EmptyProfileState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 110.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(VioletLight),
                contentAlignment = Alignment.Center
            ) {
                ShieldIcon(color = VioletPop, size = 28.dp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun MyListingRowCard(
    item: VaultListing,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onMarkSold: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${if (item.isSold) "Passed On" else item.price} • ${item.category}",
                    fontSize = 12.sp,
                    color = if (item.isSold) TextSecondary else EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!item.isSold) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SkyLight)
                            .clickable(onClick = onEdit)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkyBlue)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(EmeraldLight)
                            .clickable(onClick = onMarkSold)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("Sold", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AlertCrimson.copy(alpha = 0.1f))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertCrimson)
                }
            }
        }
    }
}

@Composable
private fun SavedOrClaimedRowCard(
    ref: UserListingRef,
    onClick: () -> Unit
) {
    val accent = categoryAccent(ref.category)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryArtIcon(category = ref.category, tint = accent, size = 20.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    ref.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${ref.sellerName} • ${ref.batchTag}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text(
                if (ref.freeItem) "FREE" else ref.price,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (ref.freeItem) EmeraldGreen else TextPrimary
            )
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.CampusImageProcessor
import com.example.model.CampusConstants
import com.example.model.PrefillListingDraft
import com.example.model.SellerTrustStats
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.VaultListing
import com.example.model.WantedPost
import com.example.ui.components.BookmarkIcon
import com.example.ui.components.CategoryArtIcon
import com.example.ui.components.PillInputField
import com.example.ui.components.SearchIcon
import com.example.ui.components.ShieldIcon
import com.example.ui.components.SpringActionButton
import com.example.ui.components.StarIcon
import com.example.ui.components.VerifiedCheckIcon
import com.example.ui.components.categoryAccent
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CanvasOffWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GreenActionGradient
import com.example.ui.theme.OrangeAccentGradient
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

@Composable
fun HomeFeedScreen(
    user: UserProfile,
    listingsState: UiState<List<VaultListing>>,
    wantedState: UiState<List<WantedPost>>,
    sellerTrustMap: Map<String, SellerTrustStats>,
    savedIds: Set<String>,
    isBusy: Boolean,
    onToggleSave: (VaultListing, Boolean) -> Unit,
    onItemClick: (VaultListing) -> Unit,
    onListingIdClick: (String) -> Unit,
    onNavigateToSell: (PrefillListingDraft?) -> Unit,
    onCreateWanted: (
        title: String,
        description: String,
        category: String,
        maxBudget: String,
        meetupSpot: String
    ) -> Unit,
    onFulfillWanted: (wantedId: String, listingId: String) -> Unit,
    onDeleteWanted: (wantedId: String) -> Unit,
    onRetry: () -> Unit = {}
) {
    var boardSegment by remember { mutableIntStateOf(0) } // 0 = Marketplace, 1 = Looking For
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedQuickFilter by remember { mutableStateOf("All") }
    var showCreateWantedDialog by remember { mutableStateOf(false) }
    var fulfillingPost by remember { mutableStateOf<WantedPost?>(null) }

    val activeItems = remember(listingsState) {
        when (listingsState) {
            is UiState.Success -> listingsState.data.filter { !it.isSold }
            else -> emptyList()
        }
    }

    val myActiveListings = remember(activeItems, user.userId) {
        activeItems.filter { it.sellerId == user.userId }
    }

    val filtered = remember(activeItems, searchQuery, selectedCategory, selectedQuickFilter) {
        activeItems.filter { item ->
            val matchesCat = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuick = when (selectedQuickFilter) {
                "Free" -> item.isFree
                "Digital" -> item.isDigital
                "Under ₹200" -> item.pricingMode == "Paid" && item.priceAmount in 1..199
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.sellerName.contains(searchQuery, ignoreCase = true) ||
                item.studyTags.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCat && matchesQuick && matchesSearch
        }
    }

    val wantedPosts = remember(wantedState) {
        when (wantedState) {
            is UiState.Success -> wantedState.data
            else -> emptyList()
        }
    }

    val filteredWanted = remember(wantedPosts, searchQuery, selectedCategory) {
        wantedPosts.filter { post ->
            val matchesCat = selectedCategory == "All" || post.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                post.title.contains(searchQuery, ignoreCase = true) ||
                post.description.contains(searchQuery, ignoreCase = true) ||
                post.requesterName.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

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
                    .background(PureWhite)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CampusVault",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = EmeraldLight,
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.28f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VerifiedCheckIcon(color = EmeraldGreen, size = 14.dp)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                user.batchTag,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardModePill(
                        label = "Marketplace",
                        selected = boardSegment == 0,
                        accent = EmeraldGreen,
                        onClick = { boardSegment = 0 },
                        modifier = Modifier.weight(1f).testTag("home_tab_marketplace")
                    )
                    BoardModePill(
                        label = "Looking For (${wantedPosts.count { !it.isFulfilled }})",
                        selected = boardSegment == 1,
                        accent = VioletPop,
                        onClick = { boardSegment = 1 },
                        modifier = Modifier.weight(1f).testTag("home_tab_wanted")
                    )
                }

                Spacer(Modifier.height(12.dp))

                PillInputField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = if (boardSegment == 0) "Search listings, notes, #tags..." else "Search student requests...",
                    leadingIcon = { SearchIcon(color = SunsetOrange, size = 18.dp) },
                    modifier = Modifier.testTag("home_search_input")
                )

                Spacer(Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CampusConstants.CATEGORIES) { cat ->
                        val active = cat == selectedCategory
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedCategory = cat },
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) SunsetOrange else PureWhite,
                            border = BorderStroke(1.dp, if (active) SunsetOrange else BorderLight)
                        ) {
                            Text(
                                cat,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) PureWhite else TextPrimary,
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }

        if (boardSegment == 0) {
            if (activeItems.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickFilterPill("Free", selectedQuickFilter == "Free", SunsetOrange, Modifier.weight(1f)) {
                            selectedQuickFilter = if (selectedQuickFilter == "Free") "All" else "Free"
                        }
                        QuickFilterPill("Digital", selectedQuickFilter == "Digital", SkyBlue, Modifier.weight(1f)) {
                            selectedQuickFilter = if (selectedQuickFilter == "Digital") "All" else "Digital"
                        }
                        QuickFilterPill("Under ₹200", selectedQuickFilter == "Under ₹200", VioletPop, Modifier.weight(1f)) {
                            selectedQuickFilter = if (selectedQuickFilter == "Under ₹200") "All" else "Under ₹200"
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Campus Listings", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${filtered.size} available",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            when (listingsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EmeraldGreen)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = PureWhite
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    listingsState.message,
                                    color = AlertCrimson,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                                TextButton(onClick = onRetry) {
                                    Text("Retry", color = EmeraldGreen, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 88.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyFeedState(
                                    hasActiveFilters = searchQuery.isNotBlank() || selectedCategory != "All" || selectedQuickFilter != "All",
                                    onResetFilters = {
                                        searchQuery = ""
                                        selectedCategory = "All"
                                        selectedQuickFilter = "All"
                                    },
                                    onPostClick = { onNavigateToSell(null) }
                                )
                            }
                        }
                    } else {
                        item {
                            val rowCount = (filtered.size + 1) / 2
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((rowCount * 284).dp)
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalItemSpacing = 12.dp,
                                userScrollEnabled = false
                            ) {
                                items(filtered, key = { it.id }) { item ->
                                    val isSaved = item.id in savedIds
                                    VaultGridCard(
                                        item = item,
                                        trust = sellerTrustMap[item.sellerId],
                                        isSaved = isSaved,
                                        onToggleSave = { onToggleSave(item, isSaved) },
                                        onClick = { onItemClick(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Looking For Board", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Post textbooks, gear, or notes you need",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(OrangeAccentGradient)
                            .testTag("post_wanted_button")
                            .clickable { showCreateWantedDialog = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text("+ Request", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = PureWhite)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            when (wantedState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = VioletPop)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = PureWhite
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    wantedState.message,
                                    color = AlertCrimson,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                                TextButton(onClick = onRetry) {
                                    Text("Retry", color = VioletPop, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    if (filteredWanted.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 72.dp),
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
                                        "No Student Requests Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Need lab gear, notes, or a book? Post a request for seniors to see.",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    SpringActionButton(
                                        text = "Post What You Need",
                                        gradient = OrangeAccentGradient,
                                        onClick = { showCreateWantedDialog = true },
                                        modifier = Modifier.width(210.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredWanted, key = { it.id }) { post ->
                            WantedPostCard(
                                post = post,
                                isOwner = post.requesterId == user.userId,
                                onFulfillClick = { fulfillingPost = post },
                                onViewLinkedListing = { listingId -> onListingIdClick(listingId) },
                                onDelete = { onDeleteWanted(post.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateWantedDialog) {
        CreateWantedDialog(
            isBusy = isBusy,
            onDismiss = { showCreateWantedDialog = false },
            onSubmit = { title, desc, cat, budget, spot ->
                onCreateWanted(title, desc, cat, budget, spot)
                showCreateWantedDialog = false
            }
        )
    }

    fulfillingPost?.let { post ->
        FulfillWantedDialog(
            post = post,
            myActiveListings = myActiveListings,
            onDismiss = { fulfillingPost = null },
            onSelectListing = { listing ->
                onFulfillWanted(post.id, listing.id)
                fulfillingPost = null
            },
            onCreateNewListing = {
                fulfillingPost = null
                onNavigateToSell(
                    PrefillListingDraft(
                        title = post.title,
                        description = "Fulfilling campus request: ${post.description}",
                        category = post.category,
                        meetupSpot = post.meetupSpot
                    )
                )
            }
        )
    }
}

@Composable
private fun BoardModePill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) accent else SurfaceSubtle
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) PureWhite else TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp)
        )
    }
}

@Composable
private fun QuickFilterPill(
    label: String,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (active) accent else PureWhite, tween(180), label = "pillBg")
    val fg by animateColorAsState(if (active) PureWhite else TextPrimary, tween(180), label = "pillFg")
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (active) accent else BorderLight, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VaultGridCard(
    item: VaultListing,
    trust: SellerTrustStats?,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClick: () -> Unit
) {
    val accent = categoryAccent(item.category)
    val hasPhoto = item.photoUrls.isNotEmpty()
    val cardHeight = if (hasPhoto) 266.dp else if (item.isDigital || item.isFree) 202.dp else 224.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .testTag("listing_card_${item.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = PureWhite,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (hasPhoto) {
                    val coverModel = remember(item.photoUrls.first()) {
                        CampusImageProcessor.toCoilModel(item.photoUrls.first())
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                    ) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                                .background(SurfaceSubtle)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PureWhite.copy(alpha = 0.92f))
                                .clickable(onClick = onToggleSave),
                            contentAlignment = Alignment.Center
                        ) {
                            BookmarkIcon(
                                filled = isSaved,
                                color = if (isSaved) SunsetOrange else TextSecondary,
                                size = 15.dp
                            )
                        }
                        if (item.hasPriceDrop) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(EmeraldGreen)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("DROP", fontSize = 9.sp, fontWeight = FontWeight.Black, color = PureWhite)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, top = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryArtIcon(category = item.category, tint = accent, size = 22.dp)
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSaved) SunsetOrange.copy(alpha = 0.14f) else SurfaceSubtle)
                                .clickable(onClick = onToggleSave),
                            contentAlignment = Alignment.Center
                        ) {
                            BookmarkIcon(
                                filled = isSaved,
                                color = if (isSaved) SunsetOrange else TextSecondary,
                                size = 16.dp
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${item.sellerName} • ${item.batchTag}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (trust != null && trust.reviewCount > 0) {
                            Spacer(Modifier.width(5.dp))
                            StarIcon(filled = true, color = SunsetOrange, size = 11.dp)
                            Spacer(Modifier.width(2.dp))
                            Text(
                                String.format("%.1f", trust.averageScore),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SunsetOrange
                            )
                        }
                    }
                    if (item.pricingMode == "Borrow") {
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SkyLight)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Return: ${item.borrowWindowLabel ?: "7 Days"}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SkyBlue
                            )
                        }
                    }
                    if (item.studyTags.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            item.studyTags.take(2).forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(VioletLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("#$tag", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VioletPop)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (item.isFree) "FREE" else item.price,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (item.isFree) EmeraldGreen else TextPrimary
                    )
                    if (item.hasPriceDrop && item.originalPriceAmount != null) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "₹${item.originalPriceAmount}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        item.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun WantedPostCard(
    post: WantedPost,
    isOwner: Boolean,
    onFulfillClick: () -> Unit,
    onViewLinkedListing: (String) -> Unit,
    onDelete: () -> Unit
) {
    val accent = categoryAccent(post.category)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(post.category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (post.isFulfilled) EmeraldLight else OrangeLight)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (post.isFulfilled) "Fulfilled" else post.maxBudget,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (post.isFulfilled) EmeraldGreen else SunsetOrange
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(post.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(post.description, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${post.requesterName} • ${post.batchTag} • ${post.meetupSpot}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                when {
                    post.isFulfilled && !post.fulfilledListingId.isNullOrBlank() -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SkyLight)
                                .clickable { onViewLinkedListing(post.fulfilledListingId) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text("View Listing", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkyBlue)
                        }
                    }
                    isOwner -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AlertCrimson.copy(alpha = 0.1f))
                                .clickable(onClick = onDelete)
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertCrimson)
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(EmeraldLight)
                                .clickable(onClick = onFulfillClick)
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text("I Have This", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateWantedDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, desc: String, category: String, budget: String, spot: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Books") }
    var budget by remember { mutableStateOf("Under ₹500") }
    var meetupSpot by remember { mutableStateOf("Central Library") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        title = { Text("Post What You Need", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                PillInputField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "e.g., Casio fx-991EX or DBMS Notes"
                )
                Spacer(Modifier.height(10.dp))
                PillInputField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Edition, semester, or urgency",
                    singleLine = false
                )
                Spacer(Modifier.height(12.dp))
                Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CampusConstants.POST_CATEGORIES) { cat ->
                        val active = cat == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) VioletPop else SurfaceSubtle)
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) PureWhite else TextPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Budget", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CampusConstants.WANTED_BUDGETS) { b ->
                        val active = b == budget
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) SunsetOrange else SurfaceSubtle)
                                .clickable { budget = b }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(b, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) PureWhite else TextPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Preferred Meetup", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CampusConstants.MEETUP_SPOTS) { spot ->
                        val active = spot == meetupSpot
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) EmeraldGreen else SurfaceSubtle)
                                .clickable { meetupSpot = spot }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(spot, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) PureWhite else TextPrimary)
                        }
                    }
                }
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, fontSize = 12.sp, color = AlertCrimson, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy,
                onClick = {
                    if (title.isBlank()) error = "Please enter what you need"
                    else onSubmit(title.trim(), description.trim(), category, budget, meetupSpot)
                }
            ) {
                Text("Post Request", color = EmeraldGreen, fontWeight = FontWeight.ExtraBold)
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
private fun FulfillWantedDialog(
    post: WantedPost,
    myActiveListings: List<VaultListing>,
    onDismiss: () -> Unit,
    onSelectListing: (VaultListing) -> Unit,
    onCreateNewListing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        title = { Text("Fulfill Request", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    "${post.requesterName} is looking for \"${post.title}\". Link an existing listing or create a new one:",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                if (myActiveListings.isEmpty()) {
                    Text(
                        "You have no active listings yet.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    myActiveListings.take(4).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceSubtle)
                                .clickable { onSelectListing(item) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(item.price, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateNewListing) {
                Text("Post New Listing", color = SunsetOrange, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun EmptyFeedState(
    hasActiveFilters: Boolean,
    onResetFilters: () -> Unit,
    onPostClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 320.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(VioletLight),
            contentAlignment = Alignment.Center
        ) {
            ShieldIcon(color = VioletPop, size = 28.dp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (hasActiveFilters) "No Matching Listings" else "No Listings Yet",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(18.dp))
        if (hasActiveFilters) {
            SpringActionButton(
                text = "Clear Filters",
                gradient = GreenActionGradient,
                onClick = onResetFilters,
                modifier = Modifier
                    .width(200.dp)
                    .testTag("clear_filters_button")
            )
        } else {
            SpringActionButton(
                text = "Post Item",
                gradient = GreenActionGradient,
                onClick = onPostClick,
                modifier = Modifier
                    .width(200.dp)
                    .testTag("empty_post_item_button")
            )
        }
    }
}

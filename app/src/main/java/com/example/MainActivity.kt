package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AuthManager
import com.example.data.CampusImageProcessor
import com.example.data.CampusVaultRepository
import com.example.data.configureAppCheck
import com.example.model.CampusRequest
import com.example.model.UiState
import com.example.model.VaultListing
import com.example.ui.CampusVaultViewModel
import com.example.ui.components.AnimatedFloatingBottomBar
import com.example.ui.screens.AuthFlowScreen
import com.example.ui.screens.CampusProfileSetupScreen
import com.example.ui.screens.EditListingScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.MeetupChatScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RequestsScreen
import com.example.ui.screens.SellUploadScreen
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CampusVaultTheme
import com.example.ui.theme.CanvasOffWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            CampusVaultTheme {
                CampusVaultRoot()
            }
        }
    }
}

@Composable
fun CampusVaultRoot() {
    val context = LocalContext.current
    val db = remember(context) {
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    }
    val authManager = remember(db) { AuthManager(db) }
    var firebaseUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val activeUser = firebaseUser
    if (activeUser == null) {
        AuthFlowScreen(
            authManager = authManager,
            onAuthSuccess = { firebaseUser = Firebase.auth.currentUser }
        )
    } else {
        key(activeUser.uid) {
            val viewModel: CampusVaultViewModel = viewModel(
                key = activeUser.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val firestore = FirebaseFirestore.getInstance(databaseId)
                        val imageProcessor = CampusImageProcessor(app)
                        CampusVaultViewModel(
                            repository = CampusVaultRepository(firestore, imageProcessor),
                            currentUserId = activeUser.uid,
                            imageProcessor = imageProcessor
                        )
                    }
                }
            )
            AuthenticatedCampusVaultShell(
                firebaseUser = activeUser,
                viewModel = viewModel,
                onSignOut = {
                    authManager.signOut()
                    firebaseUser = null
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedCampusVaultShell(
    firebaseUser: FirebaseUser,
    viewModel: CampusVaultViewModel,
    onSignOut: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val listingsState by viewModel.listingsState.collectAsStateWithLifecycle()
    val savedState by viewModel.savedState.collectAsStateWithLifecycle()
    val claimsState by viewModel.claimsState.collectAsStateWithLifecycle()
    val requestsState by viewModel.requestsState.collectAsStateWithLifecycle()
    val wantedState by viewModel.wantedState.collectAsStateWithLifecycle()
    val ratingsState by viewModel.ratingsState.collectAsStateWithLifecycle()
    val sellerTrustMap by viewModel.sellerTrustMap.collectAsStateWithLifecycle()
    val chatMessagesState by viewModel.activeChatMessagesState.collectAsStateWithLifecycle()
    val draftAiState by viewModel.draftAiState.collectAsStateWithLifecycle()
    val studyGuideState by viewModel.studyGuideState.collectAsStateWithLifecycle()
    val prefillDraft by viewModel.prefillDraft.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var editingItemId by remember { mutableStateOf<String?>(null) }
    var openChatRequestId by remember { mutableStateOf<String?>(null) }
    var isEditingProfile by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearFeedback()
    }

    LaunchedEffect(selectedItemId) {
        viewModel.clearStudyGuide()
    }

    LaunchedEffect(openChatRequestId) {
        viewModel.setOpenChatRequest(openChatRequestId)
    }

    val savedRefs = (savedState as? UiState.Success)?.data ?: emptyList()
    val claimedRefs = (claimsState as? UiState.Success)?.data ?: emptyList()
    val allListings = (listingsState as? UiState.Success)?.data ?: emptyList()
    val requests = (requestsState as? UiState.Success)?.data ?: emptyList()
    val allRatings = (ratingsState as? UiState.Success)?.data ?: emptyList()

    val savedIds = remember(savedRefs) { savedRefs.map { it.listingId }.toSet() }
    val claimedIds = remember(claimedRefs) { claimedRefs.map { it.listingId }.toSet() }
    val ratedRequestIds = remember(allRatings, firebaseUser.uid) {
        allRatings.filter { it.raterId == firebaseUser.uid }.map { it.requestId }.toSet()
    }
    val myReviews = remember(allRatings, firebaseUser.uid) {
        allRatings.filter { it.sellerId == firebaseUser.uid }
    }
    val selectedItem: VaultListing? = remember(allListings, selectedItemId) {
        selectedItemId?.let { id -> allListings.firstOrNull { it.id == id } }
    }
    val editingItem: VaultListing? = remember(allListings, editingItemId) {
        editingItemId?.let { id -> allListings.firstOrNull { it.id == id } }
    }
    val openChatRequest: CampusRequest? = remember(requests, openChatRequestId) {
        openChatRequestId?.let { id -> requests.firstOrNull { it.id == id } }
    }
    val pendingIncomingCount = remember(requests, firebaseUser.uid) {
        requests.count { it.sellerId == firebaseUser.uid && it.status == "pending" }
    }

    val loadedProfile = (profileState as? UiState.Success)?.data

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasOffWhite)
    ) {
        when (val pState = profileState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldGreen)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = PureWhite,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Could not load your CampusVault profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                pState.message,
                                fontSize = 13.sp,
                                color = AlertCrimson,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Make sure Firestore Database and updated security rules are published in Firebase Console, then tap Retry.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.retryConnections() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Retry Connection", fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = onSignOut) {
                                Text("Sign Out", color = TextSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            is UiState.Success -> {
                if (loadedProfile == null || isEditingProfile) {
                    val existing = loadedProfile
                    CampusProfileSetupScreen(
                        initialName = existing?.fullName ?: firebaseUser.displayName ?: "Member",
                        collegeEmail = existing?.collegeEmail ?: firebaseUser.email ?: "student@campus.edu",
                        initialBranch = existing?.branch ?: "CSE",
                        initialYear = existing?.gradYear ?: "2026",
                        initialPhone = existing?.phoneNumber.orEmpty(),
                        isEditing = existing != null,
                        isBusy = isBusy,
                        onBack = if (existing != null) ({ isEditingProfile = false }) else null,
                        onSaveProfile = { name, branch, year, phone ->
                            viewModel.saveProfile(
                                fullName = name,
                                collegeEmail = existing?.collegeEmail ?: firebaseUser.email ?: "student@campus.edu",
                                branch = branch,
                                gradYear = year,
                                phoneNumber = phone,
                                isExistingProfile = existing != null,
                                onSuccess = { isEditingProfile = false }
                            )
                        }
                    )
                } else if (openChatRequest != null) {
                    MeetupChatScreen(
                        request = openChatRequest,
                        currentUserId = firebaseUser.uid,
                        messagesState = chatMessagesState,
                        onBack = { openChatRequestId = null },
                        onSendMessage = { text ->
                            viewModel.sendChatMessage(
                                requestId = openChatRequest.id,
                                senderName = loadedProfile.fullName,
                                senderBatch = loadedProfile.batchTag,
                                text = text
                            )
                        }
                    )
                } else if (editingItem != null) {
                    EditListingScreen(
                        listing = editingItem,
                        isBusy = isBusy,
                        onBack = { editingItemId = null },
                        onSave = { title, desc, cat, mode, amount, borrowDays, borrowLabel, cond, spot, link, keptUrls, newUris ->
                            viewModel.editListing(
                                existing = editingItem,
                                title = title,
                                description = desc,
                                category = cat,
                                pricingMode = mode,
                                priceAmount = amount,
                                borrowDurationDays = borrowDays,
                                borrowWindowLabel = borrowLabel,
                                condition = cond,
                                meetupSpot = spot,
                                resourceLink = link,
                                keptPhotoUrls = keptUrls,
                                newPhotoUris = newUris,
                                onSuccess = { editingItemId = null }
                            )
                        }
                    )
                } else if (selectedItem != null) {
                    ProductDetailScreen(
                        item = selectedItem,
                        currentUserId = firebaseUser.uid,
                        currentUserPhone = loadedProfile.phoneNumber.orEmpty(),
                        sellerTrust = sellerTrustMap[selectedItem.sellerId],
                        isSaved = selectedItem.id in savedIds,
                        isClaimed = selectedItem.id in claimedIds,
                        isBusy = isBusy,
                        studyGuideState = studyGuideState,
                        onBack = { selectedItemId = null },
                        onToggleSave = { viewModel.toggleSave(selectedItem, selectedItem.id in savedIds) },
                        onClaim = { viewModel.claimOrDownload(selectedItem) },
                        onSendRequest = { message, buyerPhone, offeredPrice ->
                            viewModel.sendRequest(
                                listing = selectedItem,
                                buyerName = loadedProfile.fullName,
                                buyerBatch = loadedProfile.batchTag,
                                buyerPhone = buyerPhone,
                                message = message,
                                offeredPriceAmount = offeredPrice
                            )
                        },
                        onEditClick = { editingItemId = selectedItem.id },
                        onMarkSold = { viewModel.markSold(selectedItem.id) },
                        onDelete = { viewModel.deleteListing(selectedItem.id) { selectedItemId = null } },
                        onGenerateStudyGuide = { viewModel.generateStudyGuide(selectedItem) }
                    )
                } else {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 28 })
                                .togetherWith(fadeOut(tween(150)))
                        },
                        label = "tabTransition"
                    ) { tab ->
                        when (tab) {
                            0 -> HomeFeedScreen(
                                user = loadedProfile,
                                listingsState = listingsState,
                                wantedState = wantedState,
                                sellerTrustMap = sellerTrustMap,
                                savedIds = savedIds,
                                isBusy = isBusy,
                                onToggleSave = { item, saved -> viewModel.toggleSave(item, saved) },
                                onItemClick = { selectedItemId = it.id },
                                onListingIdClick = { listingId -> selectedItemId = listingId },
                                onNavigateToSell = { draft ->
                                    if (draft != null) viewModel.setPrefillDraft(draft)
                                    selectedTab = 2
                                },
                                onCreateWanted = { title, desc, cat, budget, spot ->
                                    viewModel.createWantedPost(
                                        requesterName = loadedProfile.fullName,
                                        batchTag = loadedProfile.batchTag,
                                        title = title,
                                        description = desc,
                                        category = cat,
                                        maxBudget = budget,
                                        meetupSpot = spot
                                    )
                                },
                                onFulfillWanted = { wantedId, listingId ->
                                    viewModel.fulfillWantedPost(
                                        wantedId = wantedId,
                                        listingId = listingId,
                                        fulfilledByName = loadedProfile.fullName
                                    )
                                },
                                onDeleteWanted = { wantedId -> viewModel.deleteWantedPost(wantedId) },
                                onRetry = { viewModel.retryConnections() }
                            )
                            1 -> RequestsScreen(
                                currentUserId = firebaseUser.uid,
                                requestsState = requestsState,
                                ratedRequestIds = ratedRequestIds,
                                onUpdateStatus = { reqId, status ->
                                    viewModel.updateRequestStatus(reqId, status)
                                },
                                onAcceptWithPin = { req ->
                                    viewModel.acceptRequestWithPin(req)
                                },
                                onCounterOffer = { reqId, amount ->
                                    viewModel.counterOffer(reqId, amount)
                                },
                                onAcceptCounterOffer = { req ->
                                    viewModel.acceptCounterOffer(req)
                                },
                                onCompleteWithPin = { req, enteredPin ->
                                    viewModel.completeHandoverWithPin(req, enteredPin)
                                },
                                onMarkReturned = { reqId ->
                                    viewModel.markBorrowReturned(reqId)
                                },
                                onOpenChat = { req -> openChatRequestId = req.id },
                                onSubmitRating = { req, score, comment ->
                                    viewModel.submitRating(
                                        request = req,
                                        raterName = loadedProfile.fullName,
                                        score = score,
                                        comment = comment
                                    )
                                },
                                onRetry = { viewModel.retryConnections() }
                            )
                            2 -> SellUploadScreen(
                                user = loadedProfile,
                                isPosting = isBusy,
                                errorMessage = null,
                                prefillDraft = prefillDraft,
                                draftAiState = draftAiState,
                                onConsumePrefill = { viewModel.setPrefillDraft(null) },
                                onRequestAiAssist = { title, desc, cat, attachName, link, photoUri ->
                                    viewModel.generateDraftAssist(title, desc, cat, attachName, link, photoUri)
                                },
                                onClearAiAssist = { viewModel.clearDraftAi() },
                                onPublish = { title, desc, cat, mode, amount, borrowDays, borrowLabel, cond, spot, digital, attachName, link, photoUris, tags, aiSummary ->
                                    viewModel.publishListing(
                                        sellerName = loadedProfile.fullName,
                                        batchTag = loadedProfile.batchTag,
                                        title = title,
                                        description = desc,
                                        category = cat,
                                        pricingMode = mode,
                                        priceAmount = amount,
                                        borrowDurationDays = borrowDays,
                                        borrowWindowLabel = borrowLabel,
                                        condition = cond,
                                        meetupSpot = spot,
                                        digitalItem = digital,
                                        attachmentName = attachName,
                                        resourceLink = link,
                                        photoUris = photoUris,
                                        studyTags = tags,
                                        aiSummary = aiSummary,
                                        onSuccess = { selectedTab = 0 }
                                    )
                                }
                            )
                            else -> ProfileScreen(
                                user = loadedProfile,
                                allListings = allListings,
                                savedRefs = savedRefs,
                                claimedRefs = claimedRefs,
                                myTrust = sellerTrustMap[firebaseUser.uid],
                                myReviews = myReviews,
                                onEditProfile = { isEditingProfile = true },
                                onItemClick = { selectedItemId = it.id },
                                onEditListing = { editingItemId = it.id },
                                onMarkSold = { viewModel.markSold(it) },
                                onDeleteListing = { viewModel.deleteListing(it) },
                                onSignOut = onSignOut
                            )
                        }
                    }

                    AnimatedFloatingBottomBar(
                        selectedTab = selectedTab,
                        pendingRequestCount = pendingIncomingCount,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 86.dp, start = 16.dp, end = 16.dp)
        )
    }
}

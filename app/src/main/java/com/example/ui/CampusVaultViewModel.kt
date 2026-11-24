package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CampusImageProcessor
import com.example.data.CampusVaultRepository
import com.example.data.StudyAiHelper
import com.example.model.AiUiState
import com.example.model.CampusRequest
import com.example.model.ListingDraftAssist
import com.example.model.PrefillListingDraft
import com.example.model.RequestMessage
import com.example.model.SellerRating
import com.example.model.SellerTrustStats
import com.example.model.StudyGuideResult
import com.example.model.UiState
import com.example.model.UserListingRef
import com.example.model.UserProfile
import com.example.model.VaultListing
import com.example.model.WantedPost
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CampusVaultViewModel(
    private val repository: CampusVaultRepository,
    val currentUserId: String,
    private val imageProcessor: CampusImageProcessor? = null,
    private val aiHelper: StudyAiHelper = StudyAiHelper()
) : ViewModel() {

    private companion object {
        const val TAG = "CampusVaultVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val _retryTrigger = MutableStateFlow(0)

    fun retryConnections() {
        _retryTrigger.value += 1
    }

    private fun isTransientFirestoreError(error: Throwable): Boolean {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull() ?: return false
        return firestoreError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            firestoreError.code == FirebaseFirestoreException.Code.UNAVAILABLE
    }

    private fun <T> toUiStateFlow(label: String, sourceProvider: () -> Flow<T>): StateFlow<UiState<T>> =
        _retryTrigger.flatMapLatest {
            sourceProvider()
                .retryWhen { cause, attempt ->
                    if (attempt < 2 && isTransientFirestoreError(cause)) {
                        delay(650L * (attempt + 1))
                        true
                    } else {
                        false
                    }
                }
                .map<T, UiState<T>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing $label", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load $label"))
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val profileState: StateFlow<UiState<UserProfile?>> =
        toUiStateFlow("profile") { repository.observeUserProfile(currentUserId) }

    val listingsState: StateFlow<UiState<List<VaultListing>>> =
        toUiStateFlow("listings") { repository.observeCampusListings() }

    val savedState: StateFlow<UiState<List<UserListingRef>>> =
        toUiStateFlow("saved listings") { repository.observeSavedListings(currentUserId) }

    val claimsState: StateFlow<UiState<List<UserListingRef>>> =
        toUiStateFlow("claimed listings") { repository.observeClaimedListings(currentUserId) }

    val requestsState: StateFlow<UiState<List<CampusRequest>>> =
        toUiStateFlow("campus requests") { repository.observeRequests(currentUserId) }

    val wantedState: StateFlow<UiState<List<WantedPost>>> =
        toUiStateFlow("wanted board") { repository.observeWantedPosts() }

    val ratingsState: StateFlow<UiState<List<SellerRating>>> =
        toUiStateFlow("seller ratings") { repository.observeRatings() }

    val sellerTrustMap: StateFlow<Map<String, SellerTrustStats>> =
        ratingsState.map { state ->
            val ratings = (state as? UiState.Success)?.data ?: emptyList()
            ratings.groupBy { it.sellerId }.mapValues { (_, list) ->
                SellerTrustStats(
                    averageScore = list.map { it.score }.average(),
                    reviewCount = list.size
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    private val _activeChatRequestId = MutableStateFlow<String?>(null)
    val activeChatMessagesState: StateFlow<UiState<List<RequestMessage>>> =
        combine(_activeChatRequestId, _retryTrigger) { reqId, _ -> reqId }
            .flatMapLatest { reqId ->
                if (reqId.isNullOrBlank()) {
                    flowOf(UiState.Success(emptyList()))
                } else {
                    repository.observeRequestMessages(reqId)
                        .retryWhen { cause, attempt ->
                            if (attempt < 2 && isTransientFirestoreError(cause)) {
                                delay(650L * (attempt + 1))
                                true
                            } else {
                                false
                            }
                        }
                        .map<List<RequestMessage>, UiState<List<RequestMessage>>> { UiState.Success(it) }
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            emit(UiState.Error((firestoreError ?: error).message ?: "Could not load chat"))
                        }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _draftAiState = MutableStateFlow<AiUiState<ListingDraftAssist>>(AiUiState.Idle)
    val draftAiState: StateFlow<AiUiState<ListingDraftAssist>> = _draftAiState.asStateFlow()

    private val _studyGuideState = MutableStateFlow<AiUiState<StudyGuideResult>>(AiUiState.Idle)
    val studyGuideState: StateFlow<AiUiState<StudyGuideResult>> = _studyGuideState.asStateFlow()

    private val _prefillDraft = MutableStateFlow<PrefillListingDraft?>(null)
    val prefillDraft: StateFlow<PrefillListingDraft?> = _prefillDraft.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun setOpenChatRequest(requestId: String?) {
        _activeChatRequestId.value = requestId
    }

    fun clearDraftAi() {
        _draftAiState.value = AiUiState.Idle
    }

    fun clearStudyGuide() {
        _studyGuideState.value = AiUiState.Idle
    }

    fun setPrefillDraft(draft: PrefillListingDraft?) {
        _prefillDraft.value = draft
    }

    fun saveProfile(
        fullName: String,
        collegeEmail: String,
        branch: String,
        gradYear: String,
        phoneNumber: String? = null,
        isExistingProfile: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            _feedbackMessage.value = null
            val result = repository.saveUserProfile(
                fullName = fullName,
                collegeEmail = collegeEmail,
                branch = branch,
                gradYear = gradYear,
                phoneNumber = phoneNumber,
                isExistingProfile = isExistingProfile
            )
            _isBusy.value = false
            result.onSuccess { onSuccess() }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to save profile" }
        }
    }

    fun publishListing(
        sellerName: String,
        batchTag: String,
        title: String,
        description: String,
        category: String,
        pricingMode: String,
        priceAmount: Long,
        borrowDurationDays: Long? = null,
        borrowWindowLabel: String? = null,
        condition: String,
        meetupSpot: String,
        digitalItem: Boolean,
        attachmentName: String?,
        resourceLink: String?,
        photoUris: List<Uri>,
        studyTags: List<String>,
        aiSummary: String?,
        onSuccess: () -> Unit
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            _feedbackMessage.value = null
            val result = repository.createListing(
                sellerName = sellerName,
                batchTag = batchTag,
                title = title,
                description = description,
                category = category,
                pricingMode = pricingMode,
                priceAmount = priceAmount,
                borrowDurationDays = borrowDurationDays,
                borrowWindowLabel = borrowWindowLabel,
                condition = condition,
                meetupSpot = meetupSpot,
                digitalItem = digitalItem,
                attachmentName = attachmentName,
                resourceLink = resourceLink,
                photoUris = photoUris,
                studyTags = studyTags,
                aiSummary = aiSummary
            )
            _isBusy.value = false
            result.onSuccess {
                _draftAiState.value = AiUiState.Idle
                _prefillDraft.value = null
                _feedbackMessage.value = "Listing published to CampusVault!"
                onSuccess()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to publish listing"
            }
        }
    }

    fun editListing(
        existing: VaultListing,
        title: String,
        description: String,
        category: String,
        pricingMode: String,
        priceAmount: Long,
        borrowDurationDays: Long? = null,
        borrowWindowLabel: String? = null,
        condition: String,
        meetupSpot: String,
        resourceLink: String?,
        keptPhotoUrls: List<String>,
        newPhotoUris: List<Uri>,
        onSuccess: () -> Unit = {}
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.editListing(
                existing = existing,
                title = title,
                description = description,
                category = category,
                pricingMode = pricingMode,
                priceAmount = priceAmount,
                borrowDurationDays = borrowDurationDays,
                borrowWindowLabel = borrowWindowLabel,
                condition = condition,
                meetupSpot = meetupSpot,
                resourceLink = resourceLink,
                keptPhotoUrls = keptPhotoUrls,
                newPhotoUris = newPhotoUris
            )
            _isBusy.value = false
            result.onSuccess {
                val dropped = existing.pricingMode == "Paid" && pricingMode == "Paid" && priceAmount < existing.priceAmount
                _feedbackMessage.value = if (dropped) "Price drop saved!" else "Listing updated!"
                onSuccess()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not update listing"
            }
        }
    }

    fun toggleSave(listing: VaultListing, currentlySaved: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleSavedListing(listing, currentlySaved)
            result.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not update bookmark"
            }
        }
    }

    fun claimOrDownload(listing: VaultListing) {
        viewModelScope.launch {
            val result = repository.claimListing(listing)
            result.onSuccess {
                _feedbackMessage.value = "Added to your Downloads & Claims!"
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not claim item"
            }
        }
    }

    fun sendRequest(
        listing: VaultListing,
        buyerName: String,
        buyerBatch: String,
        buyerPhone: String,
        message: String,
        offeredPriceAmount: Long? = null,
        onSuccess: () -> Unit = {}
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createRequest(
                listing = listing,
                buyerName = buyerName,
                buyerBatch = buyerBatch,
                buyerPhone = buyerPhone,
                message = message,
                offeredPriceAmount = offeredPriceAmount
            )
            _isBusy.value = false
            result.onSuccess {
                val offerSent = listing.pricingMode == "Paid" &&
                    offeredPriceAmount != null &&
                    offeredPriceAmount != listing.priceAmount
                _feedbackMessage.value = if (offerSent) {
                    "Offer ₹$offeredPriceAmount sent to ${listing.sellerName}!"
                } else {
                    "Pickup request sent to ${listing.sellerName}!"
                }
                onSuccess()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not send request"
            }
        }
    }

    fun acceptRequestWithPin(request: CampusRequest) {
        viewModelScope.launch {
            val result = repository.acceptRequestWithPin(request)
            result.onSuccess {
                _feedbackMessage.value = "Accepted! Ask ${request.buyerName} for their 4-digit PIN at meetup."
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not accept request"
            }
        }
    }

    fun counterOffer(requestId: String, counterAmount: Long) {
        viewModelScope.launch {
            val result = repository.counterOfferRequest(requestId, counterAmount)
            result.onSuccess {
                _feedbackMessage.value = "Counter-offer ₹$counterAmount sent!"
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not send counter-offer"
            }
        }
    }

    fun acceptCounterOffer(request: CampusRequest) {
        viewModelScope.launch {
            val result = repository.acceptCounterOffer(request)
            result.onSuccess { pin ->
                _feedbackMessage.value = "Counter-offer accepted! Your handover PIN is $pin."
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not accept counter-offer"
            }
        }
    }

    fun completeHandoverWithPin(request: CampusRequest, enteredPin: String) {
        viewModelScope.launch {
            val result = repository.completeHandoverWithPin(request, enteredPin)
            result.onSuccess {
                _feedbackMessage.value = if (request.pricingMode == "Borrow") {
                    "Handover verified! Borrow return due timer started."
                } else {
                    "Handover verified & completed!"
                }
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not verify PIN"
            }
        }
    }

    fun markBorrowReturned(requestId: String) {
        viewModelScope.launch {
            val result = repository.markBorrowReturned(requestId)
            result.onSuccess {
                _feedbackMessage.value = "Item marked as returned!"
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not mark item returned"
            }
        }
    }

    fun updateRequestStatus(requestId: String, newStatus: String, sellerNote: String? = null) {
        viewModelScope.launch {
            val result = repository.updateRequestStatus(requestId, newStatus, sellerNote)
            result.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not update request"
            }
        }
    }

    fun sendChatMessage(
        requestId: String,
        senderName: String,
        senderBatch: String,
        text: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.sendRequestMessage(requestId, senderName, senderBatch, text)
            result.onSuccess { onSuccess() }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to send message" }
        }
    }

    fun markSold(listingId: String) {
        viewModelScope.launch {
            val result = repository.markListingSold(listingId)
            result.onSuccess {
                _feedbackMessage.value = "Marked as Passed On / Sold."
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not mark item sold"
            }
        }
    }

    fun deleteListing(listingId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteListing(listingId)
            result.onSuccess {
                _feedbackMessage.value = "Listing removed."
                onDeleted()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not remove listing"
            }
        }
    }

    fun createWantedPost(
        requesterName: String,
        batchTag: String,
        title: String,
        description: String,
        category: String,
        maxBudget: String,
        meetupSpot: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createWantedPost(
                requesterName = requesterName,
                batchTag = batchTag,
                title = title,
                description = description,
                category = category,
                maxBudget = maxBudget,
                meetupSpot = meetupSpot
            )
            _isBusy.value = false
            result.onSuccess {
                _feedbackMessage.value = "Posted to Looking For board!"
                onSuccess()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not post request"
            }
        }
    }

    fun fulfillWantedPost(
        wantedId: String,
        listingId: String,
        fulfilledByName: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.fulfillWantedPost(wantedId, listingId, fulfilledByName)
            _isBusy.value = false
            result.onSuccess {
                _feedbackMessage.value = "Linked your listing to the request!"
                onSuccess()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not link listing"
            }
        }
    }

    fun deleteWantedPost(wantedId: String) {
        viewModelScope.launch {
            val result = repository.deleteWantedPost(wantedId)
            result.onSuccess { _feedbackMessage.value = "Wanted post removed." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Could not remove post" }
        }
    }

    fun submitRating(
        request: CampusRequest,
        raterName: String,
        score: Int,
        comment: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.submitRating(request, raterName, score, comment)
            _isBusy.value = false
            result.onSuccess {
                _feedbackMessage.value = "Thanks for rating ${request.sellerName}!"
                onSuccess()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not submit rating"
            }
        }
    }

    fun generateDraftAssist(
        title: String,
        description: String,
        category: String,
        attachmentName: String?,
        resourceLink: String?,
        firstPhotoUri: Uri?
    ) {
        viewModelScope.launch {
            _draftAiState.value = AiUiState.Loading
            try {
                val bitmap: Bitmap? = if (firstPhotoUri != null && imageProcessor != null) {
                    imageProcessor.decodeSmallBitmap(firstPhotoUri, maxDim = 640)
                } else null
                val assist = aiHelper.generateListingAssist(
                    title = title,
                    description = description,
                    category = category,
                    attachmentName = attachmentName,
                    resourceLink = resourceLink,
                    previewBitmap = bitmap
                )
                bitmap?.recycle()
                _draftAiState.value = AiUiState.Success(assist)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _draftAiState.value = AiUiState.Error(e.localizedMessage ?: "AI suggestion failed.")
            }
        }
    }

    fun generateStudyGuide(listing: VaultListing) {
        viewModelScope.launch {
            _studyGuideState.value = AiUiState.Loading
            try {
                val previewBitmap: Bitmap? = if (listing.photoUrls.isNotEmpty() && imageProcessor != null) {
                    imageProcessor.decodeDataUriBitmap(listing.photoUrls.first())
                } else null
                val guide = aiHelper.generateStudyGuideAndQuiz(listing, previewBitmap)
                previewBitmap?.recycle()
                _studyGuideState.value = AiUiState.Success(guide)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _studyGuideState.value = AiUiState.Error(e.localizedMessage ?: "Could not generate study guide.")
            }
        }
    }
}

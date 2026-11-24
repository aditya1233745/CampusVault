package com.example.data

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.model.CampusConstants
import com.example.model.CampusRequest
import com.example.model.RequestMessage
import com.example.model.SellerRating
import com.example.model.UserListingRef
import com.example.model.UserProfile
import com.example.model.VaultListing
import com.example.model.WantedPost
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CampusVaultRepository(
    private val db: FirebaseFirestore,
    private val imageProcessor: CampusImageProcessor? = null,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        imageProcessor = CampusImageProcessor(context.applicationContext)
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    private fun <T> wrapFlowErrors(
        upstream: Flow<T>,
        operationType: OperationType,
        path: String,
        uid: String
    ): Flow<T> = upstream.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Refusing cross-user profile observation." }
        val path = "users/$userId"
        val docFlow = db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
        emitAll(wrapFlowErrors(docFlow, OperationType.GET, path, uid))
    }

    fun observeCampusListings(): Flow<List<VaultListing>> = flow {
        val uid = requireUserId()
        val path = "listings"
        val queryFlow = db.collection("listings")
            .whereEqualTo("visibility", "campus")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    VaultListing::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        emitAll(wrapFlowErrors(queryFlow, OperationType.LIST, path, uid))
    }

    fun observeSavedListings(userId: String): Flow<List<UserListingRef>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Refusing cross-user saved observation." }
        val path = "users/$userId/saved"
        val queryFlow = db.collection("users").document(userId).collection("saved")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserListingRef::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        emitAll(wrapFlowErrors(queryFlow, OperationType.LIST, path, uid))
    }

    fun observeClaimedListings(userId: String): Flow<List<UserListingRef>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Refusing cross-user claims observation." }
        val path = "users/$userId/claims"
        val queryFlow = db.collection("users").document(userId).collection("claims")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserListingRef::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        emitAll(wrapFlowErrors(queryFlow, OperationType.LIST, path, uid))
    }

    fun observeRequests(userId: String): Flow<List<CampusRequest>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Refusing cross-user requests observation." }
        val path = "requests"
        val queryFlow = db.collection("requests")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CampusRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
        emitAll(wrapFlowErrors(queryFlow, OperationType.LIST, path, uid))
    }

    fun observeRequestMessages(requestId: String): Flow<List<RequestMessage>> = flow {
        val uid = requireUserId()
        require(requestId.isNotBlank()) { "Request ID is required." }
        val path = "requests/$requestId/messages"
        val queryFlow = db.collection("requests").document(requestId)
            .collection("messages")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RequestMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
            }
        emitAll(wrapFlowErrors(queryFlow, OperationType.LIST, path, uid))
    }

    fun observeWantedPosts(): Flow<List<WantedPost>> = flow {
        val uid = requireUserId()
        val path = "wanted"
        val queryFlow = db.collection("wanted")
            .whereEqualTo("visibility", "campus")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WantedPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        emitAll(wrapFlowErrors(queryFlow, OperationType.LIST, path, uid))
    }

    fun observeRatings(): Flow<List<SellerRating>> = flow {
        val uid = requireUserId()
        val path = "ratings"
        val queryFlow = db.collection("ratings")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SellerRating::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        emitAll(wrapFlowErrors(queryFlow, OperationType.LIST, path, uid))
    }

    suspend fun saveUserProfile(
        fullName: String,
        collegeEmail: String,
        branch: String,
        gradYear: String,
        phoneNumber: String? = null,
        isExistingProfile: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanName = fullName.trim().take(80)
            val cleanEmail = collegeEmail.trim().take(120)
            val cleanPhone = phoneNumber?.trim()?.take(20)?.takeIf { it.isNotEmpty() }
            require(cleanName.isNotEmpty()) { "Full name is required." }
            require(cleanEmail.length >= 3) { "Valid email is required." }
            require(branch in CampusConstants.BRANCHES) { "Invalid branch." }
            require(gradYear in CampusConstants.GRAD_YEARS) { "Invalid graduation year." }

            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existingSnapshot = runCatching { docRef.get().await() }.getOrNull()
            val alreadyExists = isExistingProfile || (existingSnapshot?.exists() == true)

            if (alreadyExists) {
                val updates = mutableMapOf<String, Any>(
                    "fullName" to cleanName,
                    "branch" to branch,
                    "gradYear" to gradYear,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanPhone != null) {
                    updates["phoneNumber"] = cleanPhone
                } else if (existingSnapshot?.contains("phoneNumber") == true) {
                    updates["phoneNumber"] = FieldValue.delete()
                }
                docRef.update(updates).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "fullName" to cleanName,
                    "collegeEmail" to cleanEmail,
                    "branch" to branch,
                    "gradYear" to gradYear,
                    "verified" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanPhone != null) {
                    payload["phoneNumber"] = cleanPhone
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (isExistingProfile) OperationType.UPDATE else OperationType.CREATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun createListing(
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
        aiSummary: String?
    ): Result<String> {
        var docPath = "listings"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanDesc = description.trim().ifBlank { "Available on campus at $meetupSpot." }.take(1000)
            val cleanSellerName = sellerName.trim().ifBlank { "Member" }.take(80)
            val cleanBatch = batchTag.trim().ifBlank { "CSE '26" }.take(20)
            val cleanSpot = meetupSpot.trim().ifBlank { "Central Library" }.take(80)
            val cleanAttachmentName = attachmentName?.trim()?.takeIf { it.isNotEmpty() }?.take(120)
            val cleanLink = resourceLink?.trim()?.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.take(500)
            val cleanTags = studyTags.map { it.trim().take(24) }.filter { it.isNotEmpty() }.take(6)
            val cleanAiSummary = aiSummary?.trim()?.takeIf { it.isNotEmpty() }?.take(500)

            require(cleanTitle.isNotEmpty()) { "Title is required." }
            require(category in CampusConstants.POST_CATEGORIES) { "Invalid category." }
            require(pricingMode in CampusConstants.PRICING_MODES) { "Invalid pricing mode." }
            require(condition in CampusConstants.CONDITIONS) { "Invalid condition." }
            require(priceAmount in 0L..500000L) { "Price out of allowed range." }
            if (pricingMode == "Paid") {
                require(priceAmount > 0L) { "Enter a valid price greater than 0." }
            }

            val finalAmount = if (pricingMode == "Paid") priceAmount else 0L
            val formattedPrice = when (pricingMode) {
                "Free" -> "Free"
                "Borrow" -> "Borrow"
                else -> "₹$finalAmount"
            }
            val freeItem = pricingMode == "Free"

            val docRef = db.collection("listings").document()
            docPath = docRef.path

            val encodedPhotos = if (photoUris.isNotEmpty()) {
                val processor = imageProcessor
                    ?: throw IllegalStateException("Image processor is unavailable.")
                processor.encodePhotosAsDataUris(photoUris)
            } else {
                emptyList()
            }

            val rawPayload = mutableMapOf<String, Any?>(
                "sellerId" to uid,
                "sellerName" to cleanSellerName,
                "batchTag" to cleanBatch,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to category,
                "price" to formattedPrice,
                "priceAmount" to finalAmount,
                "pricingMode" to pricingMode,
                "freeItem" to freeItem,
                "digitalItem" to digitalItem,
                "condition" to condition,
                "meetupSpot" to cleanSpot,
                "status" to "active",
                "visibility" to "campus",
                "attachmentName" to cleanAttachmentName,
                "resourceLink" to cleanLink,
                "aiSummary" to cleanAiSummary,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (pricingMode == "Borrow") {
                rawPayload["borrowDurationDays"] = (borrowDurationDays ?: 7L).coerceIn(1L, 365L)
                rawPayload["borrowWindowLabel"] = (borrowWindowLabel?.trim()?.takeIf { it.isNotEmpty() } ?: "7 Days").take(32)
            }
            if (encodedPhotos.isNotEmpty()) {
                rawPayload["photoUrls"] = encodedPhotos
            }
            if (cleanTags.isNotEmpty()) {
                rawPayload["studyTags"] = cleanTags
            }

            val payload = rawPayload.filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun editListing(
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
        newPhotoUris: List<Uri>
    ): Result<Unit> {
        var docPath = "listings/${existing.id}"
        return try {
            val uid = requireUserId()
            require(existing.sellerId == uid) { "Only the seller can edit this listing." }
            require(!existing.isSold) { "Sold listings cannot be edited." }

            val cleanTitle = title.trim().take(120)
            val cleanDesc = description.trim().ifBlank { "Available on campus at $meetupSpot." }.take(1000)
            val cleanSpot = meetupSpot.trim().ifBlank { "Central Library" }.take(80)
            val cleanLink = resourceLink?.trim()?.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.take(500)

            require(cleanTitle.isNotEmpty()) { "Title is required." }
            require(category in CampusConstants.POST_CATEGORIES) { "Invalid category." }
            require(pricingMode in CampusConstants.PRICING_MODES) { "Invalid pricing mode." }
            require(condition in CampusConstants.CONDITIONS) { "Invalid condition." }
            require(priceAmount in 0L..500000L) { "Price out of allowed range." }
            if (pricingMode == "Paid") {
                require(priceAmount > 0L) { "Enter a valid price greater than 0." }
            }

            val finalAmount = if (pricingMode == "Paid") priceAmount else 0L
            val formattedPrice = when (pricingMode) {
                "Free" -> "Free"
                "Borrow" -> "Borrow"
                else -> "₹$finalAmount"
            }
            val freeItem = pricingMode == "Free"

            val newEncoded = if (newPhotoUris.isNotEmpty()) {
                val remainingSlots = (3 - keptPhotoUrls.size).coerceAtLeast(0)
                if (remainingSlots > 0) {
                    val processor = imageProcessor
                        ?: throw IllegalStateException("Image processor is unavailable.")
                    processor.encodePhotosAsDataUris(newPhotoUris.take(remainingSlots))
                } else emptyList()
            } else emptyList()
            val finalPhotos = (keptPhotoUrls + newEncoded).take(3)

            val docRef = db.collection("listings").document(existing.id)
            docPath = docRef.path

            val updates = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to category,
                "price" to formattedPrice,
                "priceAmount" to finalAmount,
                "pricingMode" to pricingMode,
                "freeItem" to freeItem,
                "digitalItem" to (existing.digitalItem || category == "Notes" || !cleanLink.isNullOrBlank()),
                "condition" to condition,
                "meetupSpot" to cleanSpot,
                "photoUrls" to finalPhotos,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (pricingMode == "Borrow") {
                updates["borrowDurationDays"] = (borrowDurationDays ?: existing.borrowDurationDays ?: 7L).coerceIn(1L, 365L)
                updates["borrowWindowLabel"] = (borrowWindowLabel?.trim()?.takeIf { it.isNotEmpty() } ?: existing.borrowWindowLabel ?: "7 Days").take(32)
            } else {
                if (existing.borrowDurationDays != null) updates["borrowDurationDays"] = FieldValue.delete()
                if (existing.borrowWindowLabel != null) updates["borrowWindowLabel"] = FieldValue.delete()
            }
            if (cleanLink != null) {
                updates["resourceLink"] = cleanLink
            } else if (existing.resourceLink != null) {
                updates["resourceLink"] = FieldValue.delete()
            }

            val previousAnchor = existing.originalPriceAmount ?: existing.priceAmount
            if (pricingMode == "Paid" && existing.pricingMode == "Paid" && finalAmount in 1L until previousAnchor) {
                updates["originalPriceAmount"] = previousAnchor
            } else if (existing.originalPriceAmount != null && (pricingMode != "Paid" || finalAmount >= previousAnchor)) {
                updates["originalPriceAmount"] = FieldValue.delete()
            }

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markListingSold(listingId: String): Result<Unit> {
        var docPath = "listings/$listingId"
        return try {
            requireUserId()
            require(listingId.isNotBlank()) { "Listing ID is required." }
            val docRef = db.collection("listings").document(listingId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to "sold",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        var docPath = "listings/$listingId"
        return try {
            requireUserId()
            require(listingId.isNotBlank()) { "Listing ID is required." }
            val docRef = db.collection("listings").document(listingId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSavedListing(listing: VaultListing, currentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid
        var docPath = "users/${uid ?: "unauthenticated"}/saved/${listing.id}"
        return try {
            val activeUid = requireUserId()
            require(listing.id.isNotBlank()) { "Listing ID is required." }
            val docRef = db.collection("users").document(activeUid)
                .collection("saved").document(listing.id)
            docPath = docRef.path
            if (currentlySaved) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "userId" to activeUid,
                    "listingId" to listing.id,
                    "title" to listing.title.take(120),
                    "category" to listing.category,
                    "price" to listing.price.take(24),
                    "sellerName" to listing.sellerName.take(80),
                    "batchTag" to listing.batchTag.take(20),
                    "freeItem" to listing.freeItem,
                    "digitalItem" to listing.digitalItem,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (currentlySaved) OperationType.DELETE else OperationType.CREATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun claimListing(listing: VaultListing): Result<Unit> {
        val uid = auth.currentUser?.uid
        var docPath = "users/${uid ?: "unauthenticated"}/claims/${listing.id}"
        return try {
            val activeUid = requireUserId()
            require(listing.id.isNotBlank()) { "Listing ID is required." }
            val docRef = db.collection("users").document(activeUid)
                .collection("claims").document(listing.id)
            docPath = docRef.path
            val payload = mapOf(
                "userId" to activeUid,
                "listingId" to listing.id,
                "title" to listing.title.take(120),
                "category" to listing.category,
                "price" to listing.price.take(24),
                "sellerName" to listing.sellerName.take(80),
                "batchTag" to listing.batchTag.take(20),
                "freeItem" to listing.freeItem,
                "digitalItem" to listing.digitalItem,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createRequest(
        listing: VaultListing,
        buyerName: String,
        buyerBatch: String,
        buyerPhone: String,
        message: String,
        offeredPriceAmount: Long? = null
    ): Result<Unit> {
        var docPath = "requests"
        return try {
            val buyerId = requireUserId()
            require(listing.id.isNotBlank()) { "Listing ID is required." }
            require(listing.sellerId.isNotBlank()) { "Seller ID is required." }
            require(buyerId != listing.sellerId) { "You cannot request your own listing." }

            val cleanPhone = buyerPhone.trim().take(20)
            val phoneDigits = cleanPhone.filter { it.isDigit() }
            require(phoneDigits.length >= 7) { "Enter a valid phone number so the seller can call you." }

            val cleanMessage = message.trim().ifBlank { "Hi, I'd like to pick this up at ${listing.meetupSpot}." }.take(500)
            val isOffer = listing.pricingMode == "Paid" &&
                offeredPriceAmount != null &&
                offeredPriceAmount in 1L..500000L &&
                offeredPriceAmount != listing.priceAmount
            val negotiationStatus = if (isOffer) "offered" else "none"

            val docRef = db.collection("requests").document()
            docPath = docRef.path
            val payload = mutableMapOf<String, Any>(
                "listingId" to listing.id,
                "listingTitle" to listing.title.take(120),
                "listingCategory" to listing.category,
                "listingPrice" to listing.price.take(24),
                "pricingMode" to listing.pricingMode,
                "negotiationStatus" to negotiationStatus,
                "meetupSpot" to listing.meetupSpot.take(80),
                "buyerId" to buyerId,
                "buyerName" to buyerName.trim().ifBlank { "Student" }.take(80),
                "buyerBatch" to buyerBatch.trim().ifBlank { "CSE '26" }.take(20),
                "buyerPhone" to cleanPhone,
                "sellerId" to listing.sellerId,
                "sellerName" to listing.sellerName.take(80),
                "participantIds" to listOf(buyerId, listing.sellerId),
                "message" to cleanMessage,
                "borrowReturnStatus" to "none",
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (listing.pricingMode == "Paid") {
                payload["askingPriceAmount"] = listing.priceAmount.coerceIn(0L, 500000L)
                if (isOffer && offeredPriceAmount != null) {
                    payload["offeredPriceAmount"] = offeredPriceAmount
                }
            }
            if (listing.pricingMode == "Borrow") {
                payload["borrowDurationDays"] = (listing.borrowDurationDays ?: 7L).coerceIn(1L, 365L)
            }
            docRef.set(payload).await()

            try {
                db.collection("users").document(buyerId)
                    .update(
                        mapOf(
                            "phoneNumber" to cleanPhone,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort profile phone auto-save
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun acceptRequestWithPin(
        request: CampusRequest,
        sellerNote: String? = null
    ): Result<String> {
        var docPath = "requests/${request.id}"
        return try {
            requireUserId()
            require(request.id.isNotBlank()) { "Request ID is required." }
            val pin = (1000..9999).random().toString()
            val docRef = db.collection("requests").document(request.id)
            docPath = docRef.path
            val updates = mutableMapOf<String, Any>(
                "status" to "accepted",
                "handoverPin" to pin,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!sellerNote.isNullOrBlank()) {
                updates["sellerNote"] = sellerNote.trim().take(500)
            }
            if (request.negotiationStatus == "offered" && request.offeredPriceAmount != null) {
                updates["agreedPriceAmount"] = request.offeredPriceAmount
                updates["listingPrice"] = "₹${request.offeredPriceAmount}"
                updates["negotiationStatus"] = "accepted"
            } else if (request.pricingMode == "Paid" && request.askingPriceAmount != null) {
                updates["agreedPriceAmount"] = request.askingPriceAmount
                updates["negotiationStatus"] = "accepted"
            }
            docRef.update(updates).await()
            Result.success(pin)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun counterOfferRequest(
        requestId: String,
        counterAmount: Long,
        sellerNote: String? = null
    ): Result<Unit> {
        var docPath = "requests/$requestId"
        return try {
            requireUserId()
            require(requestId.isNotBlank()) { "Request ID is required." }
            require(counterAmount in 1L..500000L) { "Enter a valid counter-offer between ₹1 and ₹5,00,000." }
            val docRef = db.collection("requests").document(requestId)
            docPath = docRef.path
            val updates = mutableMapOf<String, Any>(
                "counterPriceAmount" to counterAmount,
                "negotiationStatus" to "countered",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!sellerNote.isNullOrBlank()) {
                updates["sellerNote"] = sellerNote.trim().take(500)
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun acceptCounterOffer(request: CampusRequest): Result<String> {
        var docPath = "requests/${request.id}"
        return try {
            requireUserId()
            require(request.id.isNotBlank()) { "Request ID is required." }
            val counterAmount = request.counterPriceAmount
                ?: throw IllegalStateException("No counter-offer price found.")
            require(counterAmount in 1L..500000L) { "Invalid counter-offer amount." }
            val pin = (1000..9999).random().toString()
            val docRef = db.collection("requests").document(request.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to "accepted",
                    "negotiationStatus" to "accepted",
                    "agreedPriceAmount" to counterAmount,
                    "listingPrice" to "₹$counterAmount",
                    "handoverPin" to pin,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(pin)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun completeHandoverWithPin(
        request: CampusRequest,
        enteredPin: String
    ): Result<Unit> {
        var docPath = "requests/${request.id}"
        return try {
            requireUserId()
            require(request.id.isNotBlank()) { "Request ID is required." }
            val expectedPin = request.handoverPin?.trim()
            if (!expectedPin.isNullOrEmpty()) {
                require(enteredPin.trim() == expectedPin) {
                    "Incorrect 4-digit handover PIN. Ask ${request.buyerName} for their PIN."
                }
            }
            val docRef = db.collection("requests").document(request.id)
            docPath = docRef.path
            val updates = mutableMapOf<String, Any>(
                "status" to "completed",
                "completedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (request.pricingMode == "Borrow") {
                val days = (request.borrowDurationDays ?: 7L).coerceIn(1L, 365L)
                val dueAt = Timestamp(Date(System.currentTimeMillis() + days * 86_400_000L))
                updates["borrowDueAt"] = dueAt
                updates["borrowReturnStatus"] = "borrowed"
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markBorrowReturned(requestId: String): Result<Unit> {
        var docPath = "requests/$requestId"
        return try {
            requireUserId()
            require(requestId.isNotBlank()) { "Request ID is required." }
            val docRef = db.collection("requests").document(requestId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "borrowReturnStatus" to "returned",
                    "returnedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatus(
        requestId: String,
        newStatus: String,
        sellerNote: String? = null
    ): Result<Unit> {
        var docPath = "requests/$requestId"
        return try {
            requireUserId()
            require(requestId.isNotBlank()) { "Request ID is required." }
            require(newStatus in listOf("accepted", "declined", "completed", "cancelled")) {
                "Invalid status transition."
            }
            val docRef = db.collection("requests").document(requestId)
            docPath = docRef.path
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!sellerNote.isNullOrBlank()) {
                updates["sellerNote"] = sellerNote.trim().take(500)
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendRequestMessage(
        requestId: String,
        senderName: String,
        senderBatch: String,
        text: String
    ): Result<Unit> {
        var docPath = "requests/$requestId/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(500)
            require(requestId.isNotBlank()) { "Request ID is required." }
            require(cleanText.isNotEmpty()) { "Message cannot be empty." }

            val docRef = db.collection("requests").document(requestId)
                .collection("messages").document()
            docPath = docRef.path
            val payload = mapOf(
                "requestId" to requestId,
                "senderId" to uid,
                "senderName" to senderName.trim().ifBlank { "Student" }.take(80),
                "senderBatch" to senderBatch.trim().ifBlank { "CSE '26" }.take(20),
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createWantedPost(
        requesterName: String,
        batchTag: String,
        title: String,
        description: String,
        category: String,
        maxBudget: String,
        meetupSpot: String
    ): Result<String> {
        var docPath = "wanted"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanDesc = description.trim().ifBlank { "Looking for this on campus." }.take(600)
            require(cleanTitle.isNotEmpty()) { "Title is required." }
            require(category in CampusConstants.POST_CATEGORIES) { "Invalid category." }

            val docRef = db.collection("wanted").document()
            docPath = docRef.path
            val payload = mapOf(
                "requesterId" to uid,
                "requesterName" to requesterName.trim().ifBlank { "Student" }.take(80),
                "batchTag" to batchTag.trim().ifBlank { "CSE '26" }.take(20),
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to category,
                "maxBudget" to maxBudget.trim().ifBlank { "Flexible" }.take(32),
                "meetupSpot" to meetupSpot.trim().ifBlank { "Central Library" }.take(80),
                "status" to "open",
                "visibility" to "campus",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun fulfillWantedPost(
        wantedId: String,
        listingId: String,
        fulfilledByName: String
    ): Result<Unit> {
        var docPath = "wanted/$wantedId"
        return try {
            requireUserId()
            require(wantedId.isNotBlank()) { "Wanted post ID is required." }
            require(listingId.isNotBlank()) { "Listing ID is required." }
            val docRef = db.collection("wanted").document(wantedId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to "fulfilled",
                    "fulfilledListingId" to listingId,
                    "fulfilledByName" to fulfilledByName.trim().ifBlank { "Member" }.take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteWantedPost(wantedId: String): Result<Unit> {
        var docPath = "wanted/$wantedId"
        return try {
            requireUserId()
            require(wantedId.isNotBlank()) { "Wanted post ID is required." }
            val docRef = db.collection("wanted").document(wantedId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitRating(
        request: CampusRequest,
        raterName: String,
        score: Int,
        comment: String
    ): Result<Unit> {
        var docPath = "ratings/${request.id}"
        return try {
            val uid = requireUserId()
            require(request.id.isNotBlank()) { "Request ID is required." }
            require(request.buyerId == uid) { "Only the buyer can rate a handover." }
            require(request.status == "completed") { "Only completed handovers can be rated." }
            require(score in 1..5) { "Rating must be between 1 and 5 stars." }

            val docRef = db.collection("ratings").document(request.id)
            docPath = docRef.path
            val payload = mapOf(
                "requestId" to request.id,
                "listingId" to request.listingId,
                "listingTitle" to request.listingTitle.take(120),
                "raterId" to uid,
                "raterName" to raterName.trim().ifBlank { "Student" }.take(80),
                "sellerId" to request.sellerId,
                "score" to score,
                "comment" to comment.trim().take(300),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}

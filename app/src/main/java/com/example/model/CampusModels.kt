package com.example.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
@Immutable
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val collegeEmail: String = "",
    val branch: String = "CSE",
    val gradYear: String = "2026",
    val phoneNumber: String? = null,
    val verified: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val batchTag: String
        get() = "$branch '${gradYear.takeLast(2)}"
}

@Keep
@IgnoreExtraProperties
@Immutable
data class VaultListing(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val batchTag: String = "CSE '26",
    val title: String = "",
    val description: String = "",
    val category: String = "Notes",
    val price: String = "Free",
    val priceAmount: Long = 0L,
    val originalPriceAmount: Long? = null,
    val pricingMode: String = "Free",
    val freeItem: Boolean = false,
    val digitalItem: Boolean = false,
    val condition: String = "Good",
    val meetupSpot: String = "Central Library",
    val status: String = "active",
    val visibility: String = "campus",
    val borrowDurationDays: Long? = null,
    val borrowWindowLabel: String? = null,
    val attachmentName: String? = null,
    val resourceLink: String? = null,
    val photoUrls: List<String> = emptyList(),
    val studyTags: List<String> = emptyList(),
    val aiSummary: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isFree: Boolean
        get() = freeItem

    @get:Exclude
    val isDigital: Boolean
        get() = digitalItem

    @get:Exclude
    val isSold: Boolean
        get() = status == "sold"

    @get:Exclude
    val hasPriceDrop: Boolean
        get() = originalPriceAmount != null && pricingMode == "Paid" && priceAmount in 1 until originalPriceAmount
}

@Keep
@IgnoreExtraProperties
@Immutable
data class UserListingRef(
    @DocumentId val id: String = "",
    val userId: String = "",
    val listingId: String = "",
    val title: String = "",
    val category: String = "Notes",
    val price: String = "Free",
    val sellerName: String = "",
    val batchTag: String = "CSE '26",
    val freeItem: Boolean = false,
    val digitalItem: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
@Immutable
data class CampusRequest(
    @DocumentId val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingCategory: String = "Notes",
    val listingPrice: String = "Free",
    val pricingMode: String = "Paid",
    val askingPriceAmount: Long? = null,
    val offeredPriceAmount: Long? = null,
    val counterPriceAmount: Long? = null,
    val agreedPriceAmount: Long? = null,
    val negotiationStatus: String = "none",
    val meetupSpot: String = "Central Library",
    val buyerId: String = "",
    val buyerName: String = "",
    val buyerBatch: String = "CSE '26",
    val buyerPhone: String? = null,
    val sellerId: String = "",
    val sellerName: String = "",
    val participantIds: List<String> = emptyList(),
    val message: String = "",
    val sellerNote: String? = null,
    val handoverPin: String? = null,
    val borrowDurationDays: Long? = null,
    val borrowDueAt: Timestamp? = null,
    val borrowReturnStatus: String = "none",
    val completedAt: Timestamp? = null,
    val returnedAt: Timestamp? = null,
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
@Immutable
data class RequestMessage(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderBatch: String = "CSE '26",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
@Immutable
data class WantedPost(
    @DocumentId val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val batchTag: String = "CSE '26",
    val title: String = "",
    val description: String = "",
    val category: String = "Notes",
    val maxBudget: String = "Under ₹300",
    val meetupSpot: String = "Central Library",
    val status: String = "open",
    val visibility: String = "campus",
    val fulfilledListingId: String? = null,
    val fulfilledByName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isFulfilled: Boolean
        get() = status == "fulfilled"
}

@Keep
@IgnoreExtraProperties
@Immutable
data class SellerRating(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val raterId: String = "",
    val raterName: String = "",
    val sellerId: String = "",
    val score: Int = 5,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Immutable
data class SellerTrustStats(
    val averageScore: Double = 0.0,
    val reviewCount: Int = 0
)

@Immutable
data class StudyQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@Immutable
data class StudyGuideResult(
    val summary: String,
    val keyTopics: List<String>,
    val quiz: List<StudyQuizQuestion>
)

@Immutable
data class ListingDraftAssist(
    val summary: String,
    val polishedDescription: String,
    val tags: List<String>
)

@Immutable
data class PrefillListingDraft(
    val title: String = "",
    val description: String = "",
    val category: String = "Notes",
    val meetupSpot: String = "Central Library"
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiUiState<out T> {
    data object Idle : AiUiState<Nothing>
    data object Loading : AiUiState<Nothing>
    data class Success<T>(val data: T) : AiUiState<T>
    data class Error(val message: String) : AiUiState<Nothing>
}

object CampusConstants {
    val BRANCHES = listOf("CSE", "ECE", "MECH", "CIVIL", "ISE", "AIML")
    val GRAD_YEARS = listOf("2026", "2027", "2028", "2029")
    val CATEGORIES = listOf("All", "Notes", "Books", "Laptops & Tech", "Calculators & Gear", "Hostel", "Uniforms")
    val POST_CATEGORIES = listOf("Notes", "Books", "Laptops & Tech", "Calculators & Gear", "Hostel", "Uniforms")
    val PRICING_MODES = listOf("Paid", "Free", "Borrow")
    val CONDITIONS = listOf("New", "Like New", "Good", "Fair")
    val MEETUP_SPOTS = listOf("Central Library", "Main Canteen", "Student Center", "Hostel Block A")
    val WANTED_BUDGETS = listOf("Free", "Under ₹200", "Under ₹500", "Under ₹1500", "Flexible")
    val BORROW_WINDOWS = listOf(
        "3 Days" to 3L,
        "7 Days" to 7L,
        "14 Days" to 14L,
        "End of Sem" to 120L
    )
}

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CAROL_UID = "carol_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

function validProfilePayload(uid) {
  const now = pastDate(10000);
  return {
    userId: uid,
    fullName: "Alice Student",
    collegeEmail: "alice@campus.edu",
    branch: "CSE",
    gradYear: "2026",
    verified: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validListingPayload(sellerId, overrides = {}) {
  const now = pastDate(10000);
  return {
    sellerId,
    sellerName: "Alice Student",
    batchTag: "CSE '26",
    title: "Operating Systems Notes",
    description: "Complete handwritten unit 1-5 PDF notes.",
    category: "Notes",
    price: "₹150",
    priceAmount: 150,
    pricingMode: "Paid",
    freeItem: false,
    digitalItem: true,
    condition: "Like New",
    meetupSpot: "Central Library",
    status: "active",
    visibility: "campus",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validRequestPayload(listingId, buyerId, sellerId) {
  const now = pastDate(5000);
  return {
    listingId,
    listingTitle: "Operating Systems Notes",
    listingCategory: "Notes",
    listingPrice: "₹150",
    meetupSpot: "Central Library",
    buyerId,
    buyerName: "Bob Student",
    buyerBatch: "ECE '27",
    sellerId,
    sellerName: "Alice Student",
    participantIds: [buyerId, sellerId],
    message: "Hi Alice, can we meet at Central Library at 4pm?",
    status: "pending",
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write profiles, listings, or requests", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("listings").where("visibility", "==", "campus").get()
  );
  await assertFails(
    unauthDb.collection("listings").doc("listing_1").set(validListingPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("requests").where("participantIds", "array-contains", ALICE_UID).get()
  );
});

test("User profile: owner CRUD succeeds; cross-user, ghost fields, and immutable field mutations fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Partial update of mutable profile fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "Alice R.",
      branch: "AIML",
      gradYear: "2027",
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost field on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validProfilePayload(BOB_UID),
      isAdmin: true,
    })
  );

  // Reject cross-user read or write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      fullName: "Hacked",
      updatedAt: pastDate(1000),
    })
  );

  // Reject immutable email mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      collegeEmail: "spoofed@evil.com",
      updatedAt: pastDate(1000),
    })
  );
});

test("Campus listings: Kotlin create, filtered campus query, mark sold, and negative guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("listings").doc("listing_1").set(validListingPayload(ALICE_UID))
  );

  // Reject `@DocumentId` id field inside payload
  await assertFails(
    aliceDb.collection("listings").doc("listing_2").set({
      ...validListingPayload(ALICE_UID),
      id: "listing_2",
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("listings").doc("listing_future").set(
      validListingPayload(ALICE_UID, {
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
    )
  );

  // Campus filtered query succeeds for another signed-in student; unfiltered fails
  await assertSucceeds(
    bobDb.collection("listings").where("visibility", "==", "campus").get()
  );
  await assertFails(bobDb.collection("listings").get());

  // Cross-user update/delete fails
  await assertFails(
    bobDb.collection("listings").doc("listing_1").update({
      status: "sold",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(bobDb.collection("listings").doc("listing_1").delete());

  // Seller marks sold (terminal state)
  await assertSucceeds(
    aliceDb.collection("listings").doc("listing_1").update({
      status: "sold",
      updatedAt: pastDate(1000),
    })
  );

  // Cannot mutate after sold
  await assertFails(
    aliceDb.collection("listings").doc("listing_1").update({
      title: "Re-opened",
      updatedAt: pastDate(500),
    })
  );
});

test("Saved and claimed subcollections: strictly isolated to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const refPayload = {
    userId: ALICE_UID,
    listingId: "listing_1",
    title: "Operating Systems Notes",
    category: "Notes",
    price: "₹150",
    sellerName: "Bob",
    batchTag: "ECE '27",
    freeItem: false,
    digitalItem: true,
    createdAt: pastDate(2000),
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved").doc("listing_1").set(refPayload)
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("claims").doc("listing_1").set(refPayload)
  );

  // Cross-user access denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("claims").doc("listing_1").set(refPayload)
  );
});

test("Campus requests: buyer creates, participants query, seller accepts/completes, terminal lock enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const carolDb = testEnv.authenticatedContext(CAROL_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("listings").doc("listing_1").set(validListingPayload(ALICE_UID))
  );

  // Bob creates a pickup request for Alice's listing
  await assertSucceeds(
    bobDb.collection("requests").doc("req_1").set(
      validRequestPayload("listing_1", BOB_UID, ALICE_UID)
    )
  );

  // Self-request fails
  await assertFails(
    aliceDb.collection("requests").doc("req_self").set(
      validRequestPayload("listing_1", ALICE_UID, ALICE_UID)
    )
  );

  // Both participants can list with array-contains filter; third party cannot
  await assertSucceeds(
    aliceDb.collection("requests").where("participantIds", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("requests").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertFails(carolDb.collection("requests").doc("req_1").get());
  await assertFails(
    carolDb.collection("requests").where("participantIds", "array-contains", ALICE_UID).get()
  );

  // Buyer cannot accept their own request
  await assertFails(
    bobDb.collection("requests").doc("req_1").update({
      status: "accepted",
      updatedAt: pastDate(1000),
    })
  );

  // Seller accepts then completes
  await assertSucceeds(
    aliceDb.collection("requests").doc("req_1").update({
      status: "accepted",
      sellerNote: "Sure, see you at 4pm!",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("requests").doc("req_1").update({
      status: "completed",
      updatedAt: pastDate(500),
    })
  );

  // Terminal state prevents any further mutation
  await assertFails(
    bobDb.collection("requests").doc("req_1").update({
      status: "cancelled",
      updatedAt: pastDate(100),
    })
  );
});

test("Listing edit & price drop, request chat messages, wanted board, and ratings rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const carolDb = testEnv.authenticatedContext(CAROL_UID).firestore();

  // 1. Listing with photos/tags + price drop edit
  await assertSucceeds(
    aliceDb.collection("listings").doc("listing_edit").set(
      validListingPayload(ALICE_UID, {
        photoUrls: ["https://example.com/p1.jpg"],
        studyTags: ["OS", "Sem4"],
        aiSummary: "Clean OS summary.",
      })
    )
  );
  await assertSucceeds(
    aliceDb.collection("listings").doc("listing_edit").update({
      price: "₹100",
      priceAmount: 100,
      originalPriceAmount: 150,
      updatedAt: pastDate(1000),
    })
  );

  // 2. Request + live chat messages (participants only, read-only after completed)
  await assertSucceeds(
    bobDb.collection("requests").doc("req_chat").set(
      validRequestPayload("listing_edit", BOB_UID, ALICE_UID)
    )
  );
  await assertSucceeds(
    bobDb.collection("requests").doc("req_chat").collection("messages").doc("msg_1").set({
      requestId: "req_chat",
      senderId: BOB_UID,
      senderName: "Bob Student",
      senderBatch: "ECE '27",
      text: "Are you free at 4 PM?",
      createdAt: pastDate(900),
    })
  );
  await assertSucceeds(
    aliceDb.collection("requests").doc("req_chat").collection("messages").get()
  );
  await assertFails(
    carolDb.collection("requests").doc("req_chat").collection("messages").get()
  );

  // Seller accepts & completes, then buyer submits rating
  await assertSucceeds(
    aliceDb.collection("requests").doc("req_chat").update({
      status: "accepted",
      updatedAt: pastDate(800),
    })
  );
  await assertSucceeds(
    aliceDb.collection("requests").doc("req_chat").update({
      status: "completed",
      updatedAt: pastDate(700),
    })
  );
  // Cannot send chat message after completed
  await assertFails(
    bobDb.collection("requests").doc("req_chat").collection("messages").doc("msg_2").set({
      requestId: "req_chat",
      senderId: BOB_UID,
      senderName: "Bob Student",
      senderBatch: "ECE '27",
      text: "Thanks!",
      createdAt: pastDate(600),
    })
  );

  // 3. Ratings: buyer can rate once, third-party or seller cannot
  await assertFails(
    carolDb.collection("ratings").doc("req_chat").set({
      requestId: "req_chat",
      listingId: "listing_edit",
      listingTitle: "Operating Systems Notes",
      raterId: CAROL_UID,
      raterName: "Carol",
      sellerId: ALICE_UID,
      score: 5,
      comment: "Fake review",
      createdAt: pastDate(500),
    })
  );
  await assertSucceeds(
    bobDb.collection("ratings").doc("req_chat").set({
      requestId: "req_chat",
      listingId: "listing_edit",
      listingTitle: "Operating Systems Notes",
      raterId: BOB_UID,
      raterName: "Bob Student",
      sellerId: ALICE_UID,
      score: 5,
      comment: "Fast handover at Central Library!",
      createdAt: pastDate(500),
    })
  );
  await assertSucceeds(carolDb.collection("ratings").get());

  // 4. Wanted board: Bob posts, Alice fulfills with her active listing, Carol cannot hijack
  await assertSucceeds(
    bobDb.collection("wanted").doc("want_1").set({
      requesterId: BOB_UID,
      requesterName: "Bob Student",
      batchTag: "ECE '27",
      title: "Need OS Notes",
      description: "Looking for sem 4 notes",
      category: "Notes",
      maxBudget: "Under ₹200",
      meetupSpot: "Central Library",
      status: "open",
      visibility: "campus",
      createdAt: pastDate(900),
      updatedAt: pastDate(900),
    })
  );
  await assertSucceeds(
    aliceDb.collection("wanted").doc("want_1").update({
      status: "fulfilled",
      fulfilledListingId: "listing_edit",
      fulfilledByName: "Alice Student",
      updatedAt: pastDate(400),
    })
  );
  await assertFails(carolDb.collection("wanted").doc("want_1").delete());
});

test("Handover PIN, borrow return tracker, price offer/counter-offer, and buyerPhone rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const carolDb = testEnv.authenticatedContext(CAROL_UID).firestore();

  // 1. Paid listing with offer -> counter-offer -> buyer accepts counter + PIN
  await assertSucceeds(
    aliceDb.collection("listings").doc("listing_paid").set(validListingPayload(ALICE_UID))
  );
  await assertSucceeds(
    bobDb.collection("requests").doc("req_offer").set({
      ...validRequestPayload("listing_paid", BOB_UID, ALICE_UID),
      pricingMode: "Paid",
      askingPriceAmount: 150,
      offeredPriceAmount: 110,
      negotiationStatus: "offered",
      buyerPhone: "9876543210",
    })
  );
  // Third party Carol cannot read Bob's phone in the private request
  await assertFails(carolDb.collection("requests").doc("req_offer").get());

  // Seller Alice sends counter-offer
  await assertSucceeds(
    aliceDb.collection("requests").doc("req_offer").update({
      counterPriceAmount: 130,
      negotiationStatus: "countered",
      sellerNote: "Can do ₹130 at Central Library",
      updatedAt: pastDate(2000),
    })
  );
  // Buyer Bob accepts counter-offer and generates 4-digit PIN
  await assertSucceeds(
    bobDb.collection("requests").doc("req_offer").update({
      status: "accepted",
      negotiationStatus: "accepted",
      agreedPriceAmount: 130,
      listingPrice: "₹130",
      handoverPin: "4829",
      updatedAt: pastDate(1500),
    })
  );
  // Invalid PIN format fails
  await assertFails(
    aliceDb.collection("requests").doc("req_offer").update({
      handoverPin: "abc",
      updatedAt: pastDate(1000),
    })
  );

  // 2. Borrow listing + completed handover + lender marks returned
  await assertSucceeds(
    aliceDb.collection("listings").doc("listing_borrow").set(
      validListingPayload(ALICE_UID, {
        title: "Casio fx-991EX Calculator",
        category: "Calculators & Gear",
        price: "Borrow",
        priceAmount: 0,
        pricingMode: "Borrow",
        freeItem: false,
        digitalItem: false,
        borrowDurationDays: 7,
        borrowWindowLabel: "7 Days",
      })
    )
  );
  await assertSucceeds(
    bobDb.collection("requests").doc("req_borrow").set({
      ...validRequestPayload("listing_borrow", BOB_UID, ALICE_UID),
      listingTitle: "Casio fx-991EX Calculator",
      listingCategory: "Calculators & Gear",
      listingPrice: "Borrow",
      pricingMode: "Borrow",
      borrowDurationDays: 7,
      borrowReturnStatus: "none",
      buyerPhone: "9876543210",
    })
  );
  await assertSucceeds(
    aliceDb.collection("requests").doc("req_borrow").update({
      status: "accepted",
      handoverPin: "7391",
      updatedAt: pastDate(2000),
    })
  );
  const futureDue = new Date(Date.now() + 7 * 86400 * 1000);
  await assertSucceeds(
    aliceDb.collection("requests").doc("req_borrow").update({
      status: "completed",
      completedAt: pastDate(1500),
      borrowDueAt: futureDue,
      borrowReturnStatus: "borrowed",
      updatedAt: pastDate(1500),
    })
  );
  // Buyer Bob cannot mark his own borrow returned
  await assertFails(
    bobDb.collection("requests").doc("req_borrow").update({
      borrowReturnStatus: "returned",
      returnedAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
  // Lender Alice marks returned
  await assertSucceeds(
    aliceDb.collection("requests").doc("req_borrow").update({
      borrowReturnStatus: "returned",
      returnedAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
});



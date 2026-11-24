<div align="center">
  <img src="screenshots/welcome-screen.jpg" alt="CampusVault welcome screen" width="230" />
  <h1>CampusVault</h1>
  <p><strong>Your campus. One marketplace.</strong></p>
  <p>A student-focused Android marketplace to buy, sell, borrow, and share campus essentials.</p>
</div>About
CampusVault brings a campus community marketplace into one Android app. Students can list items they no longer need, discover materials posted by others, request items, negotiate a price, and coordinate a campus handover. Students can also publish Looking For posts when they need something that is not currently listed.

The app is built with Kotlin and Jetpack Compose. Firebase provides account authentication and a shared cloud database, so marketplace data can be accessed by signed-in users across devices rather than remaining in a separate local database on each phone.
Screenshots
The screenshots below show the app's welcome and sign-in flow, the campus marketplace, listing creation, and the student's profile and listing management screens.
<p align="center">
  <img src="screenshots/welcome-screen.jpg" alt="CampusVault welcome screen" width="155" />
  &nbsp;&nbsp;
  <img src="screenshots/login-screen.jpg" alt="CampusVault login screen" width="155" />
  &nbsp;&nbsp;
  <img src="screenshots/home-feed-screen.jpg" alt="CampusVault marketplace empty state" width="155" />
  &nbsp;&nbsp;
  <img src="screenshots/profile-screen.jpg" alt="CampusVault profile empty state" width="155" />
</p><p align="center"><em>Welcome · Login · Marketplace · Profile</em></p><p align="center">
  <img src="screenshots/marketplace-listings.jpg" alt="CampusVault marketplace with a student listing" width="175" />
  &nbsp;&nbsp;
  <img src="screenshots/create-listing.jpg" alt="CampusVault create listing screen with AI study tag assistance" width="175" />
  &nbsp;&nbsp;
  <img src="screenshots/profile-with-listing.jpg" alt="CampusVault profile showing active listing management" width="175" />
</p><p align="center"><em>Marketplace with listing · Create a listing · Profile with active listing</em></p>Features
Campus marketplace
- Browse campus listings and search by keyword or category.
- Create listings for notes, books, laptops and tech, calculators and gear, hostel items, and uniforms.
- Set a listing as Paid, Free, or Borrow.
- Add a description, item condition, price, campus meetup location, and borrowing duration where applicable.
- Add up to three listing photos; images are resized and compressed before being saved.
- Add an optional external resource link for digital material.
- Edit a listing, mark it as sold, or delete it.
Looking For board
- Publish a post describing an item you are looking for.
- Choose a category, maximum budget, and preferred meetup spot.
- Browse other students' wanted posts.
- Mark a wanted post as fulfilled when a suitable listing is found.
Requests, negotiation, and meetup chat
- Send a request to a listing's seller with an introductory message.
- Make an offer and respond to counter-offers.
- Accept or decline requests and track request status.
- Use a handover PIN as part of the in-app completion flow.
- Track borrowed items and mark them as returned.
- Exchange messages in a chat associated with a request.
Student profiles and trust
- Create a profile with name, college email, branch, graduation year, and optional phone number.
- View your active and sold listings, saved items, and claimed/downloaded items.
- View seller ratings and review information.
AI study and listing assistance
- Generate a concise listing summary, improved description, and suggested study tags.
- Generate a study guide with key topics and multiple-choice practice questions from a study listing.
- Uses Firebase AI Logic with the Gemini model name configured in StudyAiHelper.kt.
Technology Stack
Area	Technologies
Language	Kotlin
UI	Jetpack Compose, Material 3
App architecture	ViewModel, repository pattern, Kotlin Coroutines and Flow
Authentication	Firebase Authentication (email and password)
Cloud database	Cloud Firestore
AI features	Firebase AI Logic (Gemini)
App attestation	Firebase App Check integration
Image rendering	Coil
Build system	Gradle Kotlin DSL, Version Catalog


CampusVault uses Firebase as its managed backend. This repository does not contain a separate FastAPI, Node.js, or Spring server.
Architecture
The app separates UI, state management, and Firebase data access:
Jetpack Compose screens
        |
        v
CampusVaultViewModel
        |
        v
CampusVaultRepository -------- AuthManager
        |                           |
        v                           v
Cloud Firestore              Firebase Authentication
        |
        +---- Live snapshot streams (Kotlin Flow)

StudyAiHelper -------- Firebase AI Logic / Gemini
CampusImageProcessor - Resize and encode listing photos
AppCheckInit --------- Firebase App Check setup
Main Firestore collections
users/{userId}
  ├── saved/{listingId}
  └── claims/{listingId}

listings/{listingId}

requests/{requestId}
  └── messages/{messageId}

wanted/{wantedPostId}

ratings/{requestId}
The app observes listings, wanted posts, requests, messages, saved items, claims, and ratings through Firestore snapshot streams. This is the basis for updates appearing on other signed-in devices.
Getting Started
Requirements
- Android Studio with Android SDK installed.
- JDK 17 or newer.
- Android SDK Platform 36 (the project requests compile SDK 36.1).
- A Firebase project.
- An Android device or emulator running Android 7.0 / API 24 or newer.
1. Clone the repository
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd CampusVault-main
Open the project in Android Studio and allow Gradle to sync.
2. Connect Firebase
The Android application ID is:
com.campus.CampusVault
In the Firebase Console:
1. Create or select a Firebase project.
2. Register an Android app using the exact application ID above.
3. Download that project's google-services.json and place it at app/google-services.json (replace the included configuration when connecting your own Firebase project).
4. In Authentication → Sign-in method, enable Email/Password.
5. Create the Cloud Firestore database using the default database ID.
6. Enable Firebase AI Logic if you intend to use the AI listing and study tools.
7. Configure Firebase App Check for your Android app before enforcing App Check in production.
The app reads the Firestore database ID from app/src/main/res/values/firebase_applet_config.xml. It is currently set to (default). The Firebase CLI configuration in firebase.json is aligned to that same database.
3. Deploy Firestore security rules
Install the Firebase CLI, sign in, and select your Firebase project:
npm install -g firebase-tools
firebase login
firebase use --add
Choose the same Firebase project used by google-services.json, then deploy the rules:
firebase deploy --only firestore:rules
The rules in firestore.rules validate document fields and restrict access to authenticated users. User profiles and saved/claimed records are owner-scoped; request and chat access is limited by the request's participants; listing changes are restricted by seller ownership and allowed state transitions.
4. Configure Firebase App Check
AppCheckInit.kt installs a debug App Check provider for debug builds and a reCAPTCHA-based provider for the release path. Configure the matching Android app and provider in Firebase Console. Replace the recaptcha_site_key value in firebase_applet_config.xml with the key configured for your own project when necessary.

For local development, register the debug token shown by the Firebase App Check SDK in the Firebase Console. Do not publish debug tokens. Do not enforce App Check until the app has been tested with the correct provider on every build variant you plan to distribute.
5. Run the app
Use Android Studio's Run action, or build a debug APK from the project root:
./gradlew :app:assembleDebug
On Windows PowerShell:
.\gradlew.bat :app:assembleDebug
Firestore Rules Tests
The repository includes firestore.test.js, a Firebase Rules Unit Testing suite covering unauthenticated access, profile isolation, listing ownership, saved and claimed items, requests, chat messages, wanted posts, ratings, PIN handover, borrowing returns, and offer/counter-offer rules.

To run the tests locally, install Node.js and the Firebase test dependencies:
npm install --save-dev firebase-tools @firebase/rules-unit-testing firebase
npx firebase emulators:exec --only firestore --project demo-campusvault "node --test firestore.test.js"
The Firestore emulator is configured on 127.0.0.1:8085 in firebase.json, matching the test file's default emulator address. The tests use a demo project and do not require production user data.
Important Implementation Notes
- No Firebase Storage upload flow is present in this source revision. Listing photos are converted into compressed JPEG data URIs (up to three photos) and stored in the Firestore listing document. The code limits image size to help keep documents within Firestore's document-size limit.
- For digital material, the current listing model stores an attachment name and an optional external HTTP/HTTPS resource link. It does not upload arbitrary PDF files to Firebase Storage.
- The signup flow collects a college email and academic details, but the current source does not enforce a college-email domain allowlist or verify student identity with a college ID provider. Do not interpret the profile's verified field as proof of institutional verification without adding that validation flow.
- Email/password authentication and password reset are implemented. The login screen's visual design should not be taken as evidence that other providers are enabled.
- AI generation requires Firebase AI Logic to be configured for the Firebase project. The model identifier is set in StudyAiHelper.kt; change it there if your project uses a different supported model.
- Firestore security rules must be deployed to the Firebase project. Merely having firestore.rules in the repository does not secure a remote database.
Repository Layout
CampusVault-main/
├── app/
│   ├── google-services.json
│   └── src/main/
│       ├── java/com/example/
│       │   ├── MainActivity.kt
│       │   ├── data/          # Auth, Firestore repository, AI, App Check, images
│       │   ├── model/         # Firestore and UI data models
│       │   └── ui/            # ViewModel, Compose screens, components and theme
│       └── res/               # Android resources
├── screenshots/               # README screenshots
├── firestore.rules            # Firestore access-control rules
├── firestore.test.js          # Rules unit tests
├── firebase.json              # Firebase CLI and emulator configuration
├── gradle/                    # Gradle wrapper and dependency catalog
├── build.gradle.kts
└── settings.gradle.kts
Contributing
Contributions and suggestions are welcome. For changes involving Firebase data, update the repository implementation and Firestore rules together, and add or update Rules Unit Tests for the relevant access paths.
License
No license file is currently included. Add a license before redistributing this project or accepting external contributions under specific reuse terms.

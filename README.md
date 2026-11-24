 <div align="center">
  <img src="screenshots/welcome-screen.jpg" alt="CampusVault welcome screen" width="230" />
  <h1>CampusVault</h1>
  <p><strong>Your Campus. One Marketplace.</strong></p>
  <p>A student-focused Android marketplace to buy, sell, borrow, and share campus essentials.</p>
</div>

---

## About CampusVault

CampusVault is a student-focused campus marketplace designed to make college life easier, more affordable, and more connected.

It provides a common platform where students can buy, sell, borrow, and share academic materials and everyday essentials within their campus community.

Students can list items they no longer need, discover products posted by others, request items, negotiate prices, and coordinate campus pickups. Students can also publish **Looking For** posts when they need something that is not currently listed.

Built with Kotlin, Jetpack Compose, and Firebase, CampusVault provides a centralized platform where students can access shared marketplace data across multiple devices.

## Screenshots

### App Screens

<p align="center">
  <img src="screenshots/welcome-screen.jpg" alt="CampusVault welcome screen" width="155" />
  &nbsp;&nbsp;
  <img src="screenshots/login-screen.jpg" alt="CampusVault login screen" width="155" />
  &nbsp;&nbsp;
  <img src="screenshots/home-feed-screen.jpg" alt="CampusVault marketplace screen" width="155" />
  &nbsp;&nbsp;
  <img src="screenshots/profile-screen.jpg" alt="CampusVault profile screen" width="155" />
</p>

<p align="center">
  <em>Welcome Screen · Login · Marketplace · Profile</em>
</p>

### Marketplace and Listing Management

<p align="center">
  <img src="screenshots/marketplace-listings.jpg" alt="CampusVault marketplace with student listing" width="175" />
  &nbsp;&nbsp;
  <img src="screenshots/create-listing.jpg" alt="CampusVault create listing screen" width="175" />
  &nbsp;&nbsp;
  <img src="screenshots/profile-with-listing.jpg" alt="CampusVault profile with active listing" width="175" />
</p>

<p align="center">
  <em>Campus Marketplace · Create Listing · Manage Listings</em>
</p>

---

## Features

### 1. Campus Marketplace
- Browse campus listings and search by keyword or category.
- Discover notes, books, laptops, technology, calculators, hostel items, uniforms, and other essentials.
- Filter listings by price, category, free items, and digital materials.
- Create listings with descriptions, prices, condition, and meetup locations.
- Add up to three listing photos.
- Set listings as Paid, Free, or Borrow.
- Edit, mark as sold, or delete your listings.
- Add optional external resource links for digital materials.

### 2. Looking For Board
- Publish posts describing items you are searching for.
- Specify a category, maximum budget, and preferred meetup location.
- Discover what other students are looking for.
- Mark wanted posts as fulfilled when an item is found.

### 3. Requests and Negotiation
- Send item requests directly to sellers.
- Include introductory messages with requests.
- Make offers and respond to counter-offers.
- Accept or decline requests.
- Track request status.
- Coordinate item handovers through the application.

### 4. Meetup Chat
- Chat with other students associated with a request.
- Exchange messages during pickup coordination.
- Use handover PINs as part of the item completion process.
- Track borrowing and returned items.

### 5. Student Profiles
- Create student profiles with name, college email, branch, graduation year, and optional phone number.
- View active and sold listings.
- Manage saved items.
- View claimed or downloaded items.
- Access seller rating and review information.
- Manage your own marketplace listings.

### 6. AI-Powered Features
- Generate listing summaries and improved descriptions.
- Automatically suggest study-related tags.
- Generate study guides from study listings.
- Extract key topics for revision.
- Generate multiple-choice practice questions.

AI features use Firebase AI Logic with Gemini.

---

## Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Kotlin |
| UI Framework | Jetpack Compose |
| UI Components | Material 3 |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| AI Integration | Firebase AI Logic (Gemini) |
| App Security | Firebase App Check |
| Image Processing | Custom Android image processor |
| Image Rendering | Coil |
| Architecture | MVVM + Repository Pattern |
| Asynchronous Operations | Kotlin Coroutines and Flow |
| Build System | Gradle Kotlin DSL |
| Dependency Management | Gradle Version Catalog |

---

## System Architecture

CampusVault uses Firebase as its centralized backend instead of maintaining separate databases on individual devices.

This allows multiple students to access shared marketplace information from their own devices.

### Architecture Diagram

```text
             CAMPUSVAULT ANDROID APP
                       |
                       |
              Jetpack Compose UI
                       |
                       |
                ViewModel Layer
                       |
                       |
               Repository Layer
                       |
          +------------+-------------+
          |            |             |
          v            v             v
     Firebase       Firestore      Firebase
 Authentication    Database       AI Logic
          |            |             |
          v            v             v
       Student      Listings       Gemini
       Accounts     Requests       AI Tools
                    Messages
                    Wanted Posts
                    Ratings
                       |
                       |
               Real-Time Updates
                       |
                       v
                Other Students
```

### Firebase Services

**Firebase Authentication**
- Student registration.
- Email and password login.
- Password reset functionality.
- Authentication across multiple devices.

**Cloud Firestore**
- Student profiles.
- Marketplace listings.
- Looking For posts.
- Pickup requests.
- Chat messages.
- Saved items.
- Claims and ratings.

**Firebase AI Logic**
- AI listing assistance.
- Automatic study tags.
- Study guides.
- Practice questions.

**Firebase App Check**
- App integrity protection and Firebase request attestation.

---

## Database Structure

CampusVault uses Cloud Firestore for centralized data management.

### Main Collections

```text
users/
   |
   |-- {userId}
          |
          |-- saved/
          |-- claims/


listings/
   |
   |-- {listingId}


requests/
   |
   |-- {requestId}
          |
          |-- messages/


wanted/
   |
   |-- {wantedPostId}


ratings/
   |
   |-- {requestId}
```

### Collection Description

| Collection | Purpose |
|---|---|
| users | Stores student profile information |
| listings | Stores marketplace listings |
| saved | Stores students' saved listings |
| claims | Tracks claimed or downloaded listings |
| wanted | Stores Looking For posts |
| requests | Stores item requests and negotiation details |
| messages | Stores chat messages |
| ratings | Stores seller ratings and reviews |

Firestore snapshot listeners and Kotlin Flow provide real-time data updates across authenticated devices.

---

## Getting Started

Follow these steps to run CampusVault locally.

### Prerequisites

Make sure you have:

- Android Studio installed.
- JDK 17 or newer.
- Android SDK installed.
- Android SDK Platform 36 (the project requests compile SDK 36.1).
- A Firebase account.
- An Android device or emulator running Android 7.0 (API 24) or newer.

### 1. Clone the Repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
```

Navigate into the project:

```bash
cd CampusVault-main
```

Open the project in Android Studio and allow Gradle to synchronize.

### 2. Configure Firebase

Open the Firebase Console:

https://console.firebase.google.com/

Create a new Firebase project or select an existing one.

The Android application ID is:

```text
com.campus.CampusVault
```

Register your Android application in Firebase using the exact application ID.

Download the `google-services.json` file and place it inside:

```text
app/google-services.json
```

Replace the sample Firebase configuration with the configuration generated for your own Firebase project.

### 3. Enable Firebase Services

In Firebase Console, enable the following services.

**Authentication**
1. Open Authentication.
2. Select Sign-in method.
3. Enable Email/Password authentication.

**Cloud Firestore**
1. Open Firestore Database.
2. Create the database.
3. Use the default database ID: `(default)`.

**Firebase AI Logic**

Enable Firebase AI Logic if you want to use the AI-powered listing and study features.

**Firebase App Check**

Configure App Check for your Android application before enforcing it in production.

### 4. Configure Firestore Security Rules

Install Firebase CLI:

```bash
npm install -g firebase-tools
```

Login:

```bash
firebase login
```

Connect your Firebase project:

```bash
firebase use --add
```

Select the same Firebase project associated with your Android application.

Deploy Firestore rules:

```bash
firebase deploy --only firestore:rules
```

The repository includes `firestore.rules` containing access-control rules for student profiles, listings, requests, messages, wanted posts, and other application data.

### 5. Configure Firebase App Check

The project includes an App Check initialization component.

- Debug builds use a debug App Check provider.
- The release configuration uses a reCAPTCHA-based provider.

Configure the corresponding provider in Firebase Console.

For development, register your debug token in Firebase Console.

Do not publish debug tokens or enforce App Check until all build variants have been tested with their configured providers.

### 6. Run the Application

You can run CampusVault directly from Android Studio by selecting your device and clicking **Run**.

Alternatively, build the debug APK using:

**Linux/macOS**

```bash
./gradlew :app:assembleDebug
```

**Windows PowerShell**

```powershell
.\gradlew.bat :app:assembleDebug
```

---

## Firestore Security Testing

The project includes a Firestore security rules test suite:

```text
firestore.test.js
```

The tests cover:

- Unauthenticated access restrictions.
- Student profile isolation.
- Listing ownership.
- Saved and claimed items.
- Requests and chat messages.
- Looking For posts.
- Seller ratings.
- PIN-based handover.
- Borrowing and returns.
- Offers and counter-offers.

### Run the Tests

Install Node.js and the required dependencies:

```bash
npm install --save-dev firebase-tools @firebase/rules-unit-testing firebase
```

Run the Firestore emulator tests:

```bash
npx firebase emulators:exec --only firestore --project demo-campusvault "node --test firestore.test.js"
```

The emulator is configured to use:

```text
127.0.0.1:8085
```

The tests use a demo Firebase project and do not require production student data.

---

## Project Structure

```text
CampusVault-main/
│
├── app/
│   │
│   ├── google-services.json
│   │
│   └── src/main/
│       │
│       ├── java/com/example/
│       │   │
│       │   ├── MainActivity.kt
│       │   │
│       │   ├── data/
│       │   │   ├── AuthManager.kt
│       │   │   ├── CampusVaultRepository.kt
│       │   │   ├── StudyAiHelper.kt
│       │   │   ├── CampusImageProcessor.kt
│       │   │   ├── AppCheckInit.kt
│       │   │   └── FirestoreErrorHandler.kt
│       │   │
│       │   ├── model/
│       │   │   └── CampusModels.kt
│       │   │
│       │   └── ui/
│       │       │
│       │       ├── CampusVaultViewModel.kt
│       │       │
│       │       ├── screens/
│       │       │   ├── AuthScreens.kt
│       │       │   ├── HomeFeedScreen.kt
│       │       │   ├── SellUploadScreen.kt
│       │       │   ├── ProductDetailScreen.kt
│       │       │   ├── ProfileScreen.kt
│       │       │   ├── RequestsScreen.kt
│       │       │   ├── MeetupChatScreen.kt
│       │       │   └── EditListingSheet.kt
│       │       │
│       │       └── components/
│       │           └── CampusComponents.kt
│       │
│       └── res/
│
├── screenshots/
│   ├── welcome-screen.jpg
│   ├── login-screen.jpg
│   ├── home-feed-screen.jpg
│   ├── profile-screen.jpg
│   ├── marketplace-listings.jpg
│   ├── create-listing.jpg
│   └── profile-with-listing.jpg
│
├── firestore.rules
├── firestore.test.js
├── firebase.json
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Important Implementation Notes

1. **Image storage:** Listing photos are resized and compressed into JPEG data URIs and stored directly in Firestore documents, with a maximum of three photos. This source revision does not implement Firebase Storage uploads.

2. **Digital materials:** The application supports an attachment name and optional external HTTP/HTTPS resource link. Arbitrary PDFs are not currently uploaded to Firebase Storage.

3. **Student verification:** Registration collects college email and academic details, but the current implementation does not enforce a college email domain allowlist or institutional identity verification. The profile's `verified` field should not be considered proof of college verification.

4. **Authentication:** Email/password authentication and password reset are implemented.

5. **Firebase security:** Deploy the included Firestore security rules to your own Firebase project. Keeping a rules file in the repository alone does not secure the remote database.

6. **AI features:** AI generation requires Firebase AI Logic to be configured. The Gemini model identifier is configured in `StudyAiHelper.kt`.

7. **Configuration:** Replace the sample Firebase configuration with your own Firebase project's configuration before running the application.

---

## Future Improvements

Potential improvements for future versions include:

- College email or student ID verification.
- Firebase Storage integration for photos and documents.
- Push notifications for requests and chat messages.
- Advanced marketplace filters.
- Improved seller reputation and trust indicators.
- Offline caching and synchronization.
- More AI-powered academic assistance.
- Admin dashboard for campus marketplace management.

---

## Contributing

Contributions, suggestions, and feedback are welcome.

To contribute:

1. Fork the repository.
2. Create a new feature branch.
3. Implement your changes.
4. Test your changes.
5. Submit a Pull Request.

For changes involving Firebase data, update the repository implementation, Firestore security rules, and relevant security tests together.

---

## License

No license file is currently included.

Add an appropriate open-source license before redistributing this project or accepting contributions under specific reuse terms.

---

<div align="center">
  <h3>CampusVault</h3>
  <p><strong>Buy. Sell. Borrow. Share.</strong></p>
  <p>Making campus life more connected, convenient, and affordable.</p>
</div>

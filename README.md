<div align="center">

# CampusVault

### Your Campus. One Marketplace.

**Buy. Sell. Borrow. Share.**

A student-focused marketplace to buy, sell, borrow, and share
academic materials and everyday essentials within your campus community.

</div>

---

## About CampusVault

CampusVault is a student-focused campus marketplace designed
to make college life easier, more affordable, and more connected.

It provides a common platform where students can buy, sell,
borrow, and share academic materials and everyday essentials.

Students can list unused items, discover products posted by
other students, request items, negotiate prices, and coordinate
campus pickups.

CampusVault also includes a **Looking For** feature that allows
students to post requests for items they need but cannot find
in the marketplace.

Built using Kotlin, Jetpack Compose, and Firebase, CampusVault
connects students through a centralized marketplace accessible
across multiple devices.

---

## Screenshots

### App Screens

| Welcome Screen | Login Screen |
|---|---|
| ![Welcome](screenshots/welcome-screen.jpg) | ![Login](screenshots/login-screen.jpg) |

| Home Marketplace | Student Profile |
|---|---|
| ![Home](screenshots/home-feed-screen.jpg) | ![Profile](screenshots/profile-screen.jpg) |

### Marketplace and Listing Management

| Marketplace Listings | Create Listing |
|---|---|
| ![Listings](screenshots/marketplace-listings.jpg) | ![Create Listing](screenshots/create-listing.jpg) |

| Profile with Active Listing |
|---|
| ![Profile Listing](screenshots/profile-with-listing.jpg) |

---

## Features

### 1. Campus Marketplace

- Browse campus listings.
- Search items by keywords and categories.
- Discover notes, books, laptops, electronics, calculators,
  hostel items, and other essentials.
- Filter listings by category and price.
- Find free and digital materials.
- Create listings with descriptions, prices, and conditions.
- Add up to three listing photos.
- Set listing types as Paid, Free, or Borrow.
- Edit, mark as sold, or delete listings.

### 2. Looking For Board

- Publish posts describing items you need.
- Specify categories and maximum budgets.
- Discover what other students are looking for.
- Mark requests as fulfilled.

### 3. Requests and Negotiations

- Send item requests to sellers.
- Include introductory messages.
- Make offers and counter-offers.
- Accept or decline requests.
- Track request status.
- Coordinate item handovers.

### 4. Chat and Campus Pickup

- Chat with other students.
- Coordinate pickup arrangements.
- Use handover PINs as part of the item completion flow.
- Track borrowing and returned items.

### 5. Student Profiles

- Manage student profile information.
- View active and sold listings.
- Manage saved items.
- View claimed or downloaded items.
- Access seller ratings and reviews.

### 6. AI-Powered Features

- Generate listing descriptions and summaries.
- Suggest study-related tags.
- Generate study guides.
- Extract important study topics.
- Generate multiple-choice practice questions.

AI features use Firebase AI Logic with Gemini.

---

## Technology Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| UI Components | Material 3 |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| AI Integration | Firebase AI Logic (Gemini) |
| Security | Firebase App Check |
| Image Processing | Android image processor |
| Image Rendering | Coil |
| Architecture | MVVM + Repository Pattern |
| Asynchronous Operations | Kotlin Coroutines and Flow |
| Build System | Gradle Kotlin DSL |

---

## System Architecture

CampusVault uses Firebase as its centralized backend.

This allows students to access shared marketplace data
from different devices.

```text
             CAMPUSVAULT ANDROID APP
                       |
                       v
                Jetpack Compose
                       |
                       v
                 ViewModel
                       |
                       v
                  Repository
                       |
          +------------+------------+
          |            |            |
          v            v            v
       Firebase     Cloud        Firebase
       Auth         Firestore    AI Logic
          |            |            |
          v            v            v
       Student     Listings       Gemini
       Accounts    Requests       AI Tools
                   Messages
                   Wanted Posts
                   Ratings
                       |
                       v
                Real-Time Updates
                       |
                       v
                  Other Students
```

### Firebase Services

**Firebase Authentication**
- Student registration.
- Email and password login.
- Password reset.
- Multi-device authentication.

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
- Application integrity protection and request attestation.

---

## Database Structure

CampusVault uses Cloud Firestore for centralized data management.

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

| Collection | Purpose |
|---|---|
| users | Student profile information |
| saved | Saved listings |
| claims | Claimed or downloaded items |
| listings | Marketplace listings |
| wanted | Looking For posts |
| requests | Item requests and negotiations |
| messages | Chat messages |
| ratings | Seller reviews and ratings |

---

## Getting Started

### Prerequisites

- Android Studio
- JDK 17 or newer
- Android SDK
- Android SDK Platform 36
- Firebase account
- Android device or emulator (API 24+)

### 1. Clone the Repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>

cd CampusVault-main
```

Open the project in Android Studio and synchronize Gradle.

### 2. Configure Firebase

Open:

https://console.firebase.google.com/

Create a Firebase project and register your Android application.

The application ID is:

```text
com.campus.CampusVault
```

Download `google-services.json` and place it inside:

```text
app/google-services.json
```

Use the configuration generated for your own Firebase project.

### 3. Enable Firebase Services

Enable the following services in Firebase Console:

- Firebase Authentication
- Email/Password sign-in
- Cloud Firestore
- Firebase AI Logic

Configure Firebase App Check for your application.

### 4. Configure Firestore Rules

Install Firebase CLI:

```bash
npm install -g firebase-tools
```

Login:

```bash
firebase login
```

Connect your project:

```bash
firebase use --add
```

Deploy Firestore rules:

```bash
firebase deploy --only firestore:rules
```

### 5. Run the Application

Open the project in Android Studio and click Run.

Alternatively, build the debug APK.

**Windows:**

```powershell
.\gradlew.bat :app:assembleDebug
```

**Linux/macOS:**

```bash
./gradlew :app:assembleDebug
```

---

## Security

CampusVault uses Firebase Authentication and Firestore
security rules to protect student data.

Security considerations include:

- Authenticated access.
- User-specific data ownership.
- Listing ownership checks.
- Restricted request and message operations.
- Controlled access to student profile information.
- Firebase App Check configuration.

Always deploy and test security rules before production.

---

## Future Improvements

- College email and student ID verification.
- Firebase Storage integration for images and PDFs.
- Push notifications.
- Advanced search and filters.
- Improved seller reputation system.
- Offline caching and synchronization.
- More AI-powered academic tools.
- Campus administrator dashboard.

---

## Contributing

Contributions, suggestions, and feedback are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Implement your changes.
4. Test your changes.
5. Submit a Pull Request.

---

## License

No open-source license is currently included.
Choose and add an appropriate license before distributing the project.

---

<div align="center">

## CampusVault

**Buy. Sell. Borrow. Share.**

Making campus life more connected, convenient, and affordable.

</div>

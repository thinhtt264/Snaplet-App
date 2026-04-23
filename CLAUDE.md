# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Agent Rules

## Clarification
- Never guess when there is a concern — ask immediately and wait for confirmation before proceeding
- If the source differs from the plan or requirements → describe the difference and ask, do not decide unilaterally
- Do not assume behavior of code that has not been read

## Reading Source
- Only read files directly related to the current task — do not scan the entire codebase
- Always read before writing any code
- If a required file cannot be found → ask, do not guess the path

## Reuse & Convention
- Prefer reusing existing components, utilities, and patterns in the codebase
- Do not create new abstractions if an equivalent already exists
- Respect existing naming conventions — if reality differs from the plan, follow reality
- Do not change existing behavior unless explicitly instructed

# SnapletApp — Codebase Context

Android app: **Kotlin + Jetpack Compose + Hilt**.

Main data flow: `ApiService` → `RepositoryImpl` (`safeApiCall`) → `UseCase` → `ViewModel` (`StateFlow`) → Composable.

**Rule:** Infer impacted area(s) from the prompt and read only those folders plus direct dependencies. Do not scan the whole codebase.

Base package: `app/src/main/java/com/thinh/snaplet/`

## Build Commands

```bash
# Debug build (development flavor)
./gradlew assembleDevelopmentDebug

# Release build (development flavor, used for testing)
./gradlew assembleDevelopmentRelease

# Release build (production)
./gradlew assembleProductionRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew testDevelopmentDebugUnitTest --tests "com.thinh.snaplet.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedDevelopmentDebugAndroidTest
```

CI distributes via Fastlane: `fastlane android build_test` builds `assembleDevelopmentRelease` and distributes to Firebase App Distribution. GitHub Actions triggers on PRs labeled `build-for-test`.

**Product flavors:** `development` (`IS_DEVELOPMENT=true`) and `production`. Both share the same OAuth Web Client ID.

## Architecture

Main data flow: `ApiService` → `RepositoryImpl` (`safeApiCall`) → `UseCase` → `ViewModel` (`StateFlow`) → Composable.

**Error handling:** All API calls go through `safeApiCall` in `utils/network/NetworkExtensions.kt`, which wraps results in `ApiResult<T>`. Use `ApiResult.Success` / `ApiResult.Error` when consuming repository results in use cases.

**Overlays:** Global modals and bottom sheets are managed by `OverlayEventBus` — emit an `OverlayEvent` from anywhere and `OverlayHost` (in `MainScreen`) will render it. Don't show dialogs directly from screens.

**Navigation:** Single-activity with Compose Navigation. All routes are defined in `NavScreen`. Deep links are handled by `DeepLinkManager` which emits `DeepLinkEvent` consumed in `NavGraph`.

**Auth lifecycle:** `AuthRepository` manages token state. `TokenAuthenticator` (OkHttp) handles 401 refresh automatically. `SessionController` coordinates force-logout events.

**Real-time socket:** `SocketManager` wraps Socket.io for app-level events (friend requests, new post notifications). Chat uses its own `ChatSocketManager` on the `/chat` namespace with conversation-scoped auth.

## Package Map

| Path | Role | Read when… |
|------|------|------------|
| `MainActivity.kt`, `HiltApplication.kt` | App entry | app start, Application class |
| **ui/app/** | App-level state, auth bootstrap | startup, auth state, splash |
| **ui/screens/MainScreen.kt** | Main host + bottom nav | main layout, tabs |
| **ui/screens/home/** | Home screen + state | home, feed, reactions, unread posts |
| **ui/screens/home/components/** | `FriendBottomSheet`, `ReactionsBottomSheet`, `QuickChatBar`, `MediaPage`, `PostGridView`, `PostAudienceSelector`, `PostActivityBar`, `NewPostsBanner`, `CameraPage`, `CameraActions`, `HomeBottomContent`, `TopAction`, `BottomAction`, `EmptyMediaPage`, `CameraPermissionDenied`, `MediaItemDimensions` | camera/media/friend UI, bottom sheets |
| **ui/screens/login/** | Login screen + ViewModel + UIEvent | login |
| **ui/screens/login/components/** | `LoginEmailPage`, `LoginPasswordPage`, `LoginPageContent` | login UI |
| **ui/screens/register/** | Register screen + ViewModel + UIEvent | register |
| **ui/screens/register/components/** | `RegisterEmailPage`, `RegisterUsernamePage`, `RegisterPasswordPage`, `RegisterPageContent` | register UI |
| **ui/screens/my_profile/** | `MyProfile`, `EditDisplayNameBottomSheet`, `WidgetGuideBottomSheet` | profile, avatar, display name, widget guide |
| **ui/screens/image_crop/** | ImageCrop | crop/cắt ảnh |
| **ui/screens/onboarding/** | Onboarding + OnboardingViewModel | onboarding |
| **ui/screens/friend_request/** | Friend request overlay + ViewModel | friend request |
| **ui/screens/friend_request/components/** | `UserProfileCard`, `ActionButtons` | friend request UI |
| **ui/screens/spotlight_post/** | SpotlightPost + ViewModel + UiState | spotlight post detail, deeplink/notification open |
| **ui/screens/post_register_widget/** | `PostRegisterWidgetPromoScreen` | post-register widget promo/upsell |
| **ui/screens/chat/** | `ChatScreen`, `ChatViewModel`, `ChatUiState` + components | chat screen, messaging |
| **ui/screens/chat/components/** | `ChatHeader`, `ChatInputBar`, `MessageBubble`, `TypingIndicator` | chat UI components |
| **ui/components/** | `BaseText`, `BaseTextField`, `Avatar`, `AvatarGroup`, `PrimaryButton`, `FormTextField`, `CameraPreview`, `PermissionHandler`, `AppIconButton`, `StepAnimatedContent`, `CappedCountBadge`, `EmojiFloatCanvas`, `EmojiFloatController` | shared UI components, emoji float animation |
| **ui/components/image/** | `AsyncImage`, `AsyncImageConfig` | image loading (Coil) |
| **ui/theme/** | Theme, Color, Typo, MotionTokens | theme, colors, typography, animation tokens |
| **ui/overlay/** | `OverlayHost`, `OverlayViewModel`, `OverlayState`, `OverlayEvent`, `OverlayEventBus` | global overlay, dialogs, bottom sheets |
| **ui/overlay/modal/** | `GlobalModal`, `ConfirmDialog`, `ForceLogoutModal`, `FriendRequestModal` | modals, dialogs |
| **ui/overlay/bottom_sheet/** | `GlobalBottomSheet`, `OptionsSheet` | bottom sheets |
| **ui/common/** | `UiText`, `CommonImages`, `ComposableExt` | UI common utilities |
| **ui/widget/** | `SnapletWidget`, `SnapletWidgetContent`, `SnapletWidgetReceiver`, `SnapletWidgetStateKeys`, `WidgetDisplayData`, `WidgetImageLoader` | home screen widget UI/state |
| **navigation/** | `NavGraph`, `NavScreen`, `NavParam`, `NavActions`, `NavTransitions` | navigation, routes, deep links |
| **di/** | Hilt modules: Network, Repository, Overlay, Permission, ImageLoading, PhotoPicker, Share, WorkManager, WidgetUpdateEntryPoint | DI, modules, injection |
| **data/datasource/remote/** | `ApiService` | API endpoints (Retrofit) |
| **data/datasource/local/datastore/** | `DataStoreManager`, `DataStoreKeys` | local storage (DataStore) |
| **data/repository/** | `MediaRepository(Impl)`, `UserRepository(Impl)` | media/user repositories |
| **data/repository/auth/** | `AuthRepository`, `AuthRepositoryImpl`, `AuthState` | auth, login state, token refresh |
| **data/repository/chat/** | `ChatRepository`, `ChatRepositoryImpl` | chat REST + socket coordination |
| **data/repository/device/** | `DeviceRepository`, `DeviceRepositoryImpl`, `DeviceInfo` | device fingerprint/identifiers |
| **data/repository/post/** | `PostRepository`, `PostRepositoryImpl` | posts, feed, reactions, unread |
| **data/model/** | `BaseResponse`, auth/request DTOs, `Relationship` | API contracts, shared DTOs |
| **data/model/chat/** | `Message`, `Conversation`, `MessageReadEvent`, `ChatTypingEvent` | chat payloads + real-time events |
| **data/model/user/** | `UserProfile`, `UserSearchResult`, `AvatarUpload`, `AvatarUrls`, `DisplayName`, `UpdateFcmTokenRequest` | user payloads |
| **data/model/media/** | `Media`, `MediaUpload`, `ImageTransform` | media payloads |
| **data/model/post/** | `Post`, `PostActivity`, `PostAudience`, `PostReactionUser`, `ReactToPostRequest`, `ReactToPostResponse`, `NewPostUpdate` | post/feed/reaction payloads |
| **data/model/emoji/** | `EmojiEntry`, `EmojiLoader` | emoji data |
| **domain/post/** | `UploadPostUseCase`, `ValidateUploadPostUseCase`, `DeletePostUseCase`, `CreateTempPostUseCase`, `GetAvailablePostActionsUseCase`, `ValidateRetryUploadUseCase`, `BuildPostShareContentUseCase`, `MapPostReactionUsersUseCase`, `PostCreateAudience` | post upload, delete, share, reactions |
| **domain/relationship/** | `RemoveFriendUseCase`, `RemoveRelationshipUseCase`, `GetRelationshipsByStatusesUseCase`, `AcceptFriendRequestUseCase`, `ResolveRelationshipActionUseCase`, `GetRelationshipActionUseCase`, `FormatFriendSearchResultsUseCase`, `ObserveFriendRequestReceivedUseCase` | friend graph, relationship flows, realtime friend requests |
| **domain/chat/** | `SendMessageUseCase`, `LoadInitialMessagesUseCase` | chat message sending (optimistic), initial load |
| **domain/media/** | `ValidateCaptureReadinessUseCase` | capture readiness |
| **domain/feed/** | `ShouldTriggerLoadMoreUseCase`, `GetNewsfeedUseCase`, `FetchNewerFeedUseCase`, `ObserveNewPostEventUseCase` | feed, load more, new post events |
| **domain/user/** | `UploadAvatarUseCase` | avatar upload |
| **domain/notification/** | `RegisterFcmTokenUseCase`, `PushNotificationType` | FCM token registration, notification type |
| **domain/model/** | `PostAction`, `RelationshipAction`, `NewerFeedResult`, `FriendSearchActionItem`, `CaptureReadiness`, `EmojiParticle`, `ReactionUserUi`, `UploadAvatarResult`, `UploadPostResult` | domain/UI models, mappers |
| **platform/GoogleSignInManager.kt** | Google Sign-In integration | Google login, OAuth |
| **platform/photo_picker/** | `PhotoPickerManager` | photo picker |
| **platform/permission/** | `PermissionManager`, `PermissionState` | runtime permissions |
| **platform/share/** | `ShareManager` | share intent |
| **platform/deeplink/** | `DeepLinkManager`, `DeepLinkEvent`, `DeepLinkUtils` | deep links |
| **platform/notification/** | `NotificationHelper`, `FcmTokenRegistrar`, `SnapletFirebaseMessagingService` | push notifications, FCM |
| **platform/network/** | `ConnectivityObserver` | network connectivity |
| **platform/socket/** | `SocketManager`, `SocketEvent`, `SocketMessage`, `SocketConfig`, `SocketConnectionState` | realtime socket (WebSocket) — app-level events |
| **platform/socket/ChatSocketManager.kt** | Chat-specific Socket.io manager (`/chat` namespace) | chat real-time events |
| **platform/widget/** | `WidgetAddLauncher`, `WidgetPinnedReceiver`, `WidgetUpdateManager`, `WidgetUpdateWorker`, `WidgetWork` | widget scheduling/update workers |
| **network/** | `TokenAuthenticator`, `TokenRefreshCoordinator`, `SessionController`, `FingerprintInterceptor` | token refresh, session lifecycle, auth headers |
| **utils/** | `Logger`, `CrashlyticsLogger`, `FileUtils`, `CommonExtensions`, `ValidationConstants`, `InviteConstants`, `EmojiParticleEngine`, `MinLoadingTime`, `UtcDateDeserializer` | utilities, validation, logging, emoji animation engine |
| **utils/network/** | `ApiResult`, `ApiError`, `ApiErrorCode`, `NetworkExtensions`, `JsonHolder` | `safeApiCall`, API error mapping |

## Routing Hints

- **Chat screen / messaging** → `ui/screens/chat/` + `domain/chat/` + `data/repository/chat/` + `platform/socket/ChatSocketManager.kt`
- **Friend search / accept / remove** → `domain/relationship/` + `data/repository/UserRepository(Impl)` + `ui/screens/home/components/FriendBottomSheet.kt`
- **Friend request received (realtime)** → `domain/relationship/ObserveFriendRequestReceivedUseCase` + `platform/socket/` + `ui/overlay/modal/FriendRequestModal.kt`
- **Reactions / unread / new posts** → `data/repository/post/` + `domain/feed/` + `ui/screens/home/`
- **Token / session / logout** → `data/repository/auth/` + `network/` + `data/datasource/local/datastore/`
- **Widget** → `ui/widget/` + `platform/widget/` + `di/WorkManagerModule.kt`
- **Push notification / FCM** → `platform/notification/` + `domain/notification/` + `data/repository/UserRepository(Impl)` + `AndroidManifest.xml`
- **Deep link → spotlight post** → `platform/deeplink/` + `navigation/NavScreen.kt` + `navigation/NavGraph.kt` + `ui/screens/spotlight_post/`
- **Google Sign-In / OAuth** → `platform/GoogleSignInManager.kt` + `ui/screens/login/` + `data/repository/auth/`
- **Emoji float / quick chat animation** → `ui/components/EmojiFloatCanvas.kt` + `EmojiFloatController.kt` + `utils/EmojiParticleEngine.kt` + `domain/model/EmojiParticle.kt`
- **Avatar upload** → `domain/user/UploadAvatarUseCase.kt` + `data/repository/MediaRepository(Impl)` + `ui/screens/my_profile/`
- **Post share** → `domain/post/BuildPostShareContentUseCase.kt` + `platform/share/ShareManager.kt`
- **Post-register widget promo** → `ui/screens/post_register_widget/PostRegisterWidgetPromoScreen.kt`
- **Crashlytics / release logging** → `HiltApplication.kt` + `utils/CrashlyticsLogger.kt` + Gradle Firebase config

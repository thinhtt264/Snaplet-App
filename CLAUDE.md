# SnapletApp — Codebase Context

Android app: **Kotlin + Jetpack Compose + Hilt**.

Main data flow: `ApiService` → `RepositoryImpl` (`safeApiCall`) → `UseCase` → `ViewModel` (`StateFlow`) → Composable.

**Rule:** Infer impacted area(s) from the prompt and read only those folders plus direct dependencies. Do not scan the whole codebase.

Base package: `app/src/main/java/com/thinh/snaplet/`

## Package map

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
| **ui/screens/friend_request/** | Friend request overlay + ViewModel | friend request, lời mời kết bạn |
| **ui/screens/friend_request/components/** | `UserProfileCard`, `ActionButtons` | friend request UI |
| **ui/screens/spotlight_post/** | SpotlightPost + ViewModel + UiState | spotlight post detail, deeplink/notification open |
| **ui/screens/post_register_widget/** | `PostRegisterWidgetPromoScreen` | post-register widget promo/upsell |
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
| **data/repository/device/** | `DeviceRepository`, `DeviceRepositoryImpl`, `DeviceInfo` | device fingerprint/identifiers |
| **data/repository/post/** | `PostRepository`, `PostRepositoryImpl` | posts, feed, reactions, unread |
| **data/model/** | `BaseResponse`, auth/request DTOs, `Relationship` | API contracts, shared DTOs |
| **data/model/user/** | `UserProfile`, `UserSearchResult`, `AvatarUpload`, `AvatarUrls`, `DisplayName`, `UpdateFcmTokenRequest` | user payloads |
| **data/model/media/** | `Media`, `MediaUpload`, `ImageTransform` | media payloads |
| **data/model/post/** | `Post`, `PostActivity`, `PostAudience`, `PostReactionUser`, `ReactToPostRequest`, `ReactToPostResponse`, `NewPostUpdate` | post/feed/reaction payloads |
| **data/model/emoji/** | `EmojiEntry`, `EmojiLoader` | emoji data |
| **domain/post/** | `UploadPostUseCase`, `ValidateUploadPostUseCase`, `DeletePostUseCase`, `CreateTempPostUseCase`, `GetAvailablePostActionsUseCase`, `ValidateRetryUploadUseCase`, `BuildPostShareContentUseCase`, `MapPostReactionUsersUseCase`, `PostCreateAudience` | post upload, delete, share, reactions |
| **domain/relationship/** | `RemoveFriendUseCase`, `RemoveRelationshipUseCase`, `GetRelationshipsByStatusesUseCase`, `AcceptFriendRequestUseCase`, `ResolveRelationshipActionUseCase`, `GetRelationshipActionUseCase`, `FormatFriendSearchResultsUseCase`, `ObserveFriendRequestReceivedUseCase` | friend graph, relationship flows, realtime friend requests |
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
| **platform/socket/** | `SocketManager`, `SocketEvent`, `SocketMessage`, `SocketConfig`, `SocketConnectionState` | realtime socket (WebSocket) |
| **platform/widget/** | `WidgetAddLauncher`, `WidgetPinnedReceiver`, `WidgetUpdateManager`, `WidgetUpdateWorker`, `WidgetWork` | widget scheduling/update workers |
| **network/** | `TokenAuthenticator`, `TokenRefreshCoordinator`, `SessionController`, `FingerprintInterceptor` | token refresh, session lifecycle, auth headers |
| **utils/** | `Logger`, `CrashlyticsLogger`, `FileUtils`, `CommonExtensions`, `ValidationConstants`, `InviteConstants`, `EmojiParticleEngine`, `MinLoadingTime`, `UtcDateDeserializer` | utilities, validation, logging, emoji animation engine |
| **utils/network/** | `ApiResult`, `ApiError`, `ApiErrorCode`, `NetworkExtensions`, `JsonHolder` | `safeApiCall`, API error mapping |

## Routing hints

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

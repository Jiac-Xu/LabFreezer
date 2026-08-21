set 限制解除 [跳至主要内容](https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#main-content)

1.  [使用前须知](https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#0)
2.  [获取起始代码](https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#1)
3.  [让应用在 Android 15 上采用无边框设计](https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#2)
4.  [以向后兼容的方式使 SociaLite 采用无边框设计](https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#3)
5.  [可选：有关无边框的其他注意事项](https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#4)
6.  [查看解决方案代码](https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#5)

## [5\. 可选：有关无边框的其他注意事项](https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#4)

## 针对不同架构处理边衬区

### 组件

您可能已经注意到，在我们更改了目标 SDK 值后，SociaLite 中的许多组件并没有发生变化。这是因为 SociaLite 遵循了最佳实践进行架构设计，因此处理这种平台变更非常简单。最佳实践包括以下几点：

-   **使用 Material Design 3 组件** ([`androidx.compose.material3`](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary?hl=zh-cn))，例如 `TopAppBar`、`BottomAppBar` 和 `NavigationBar`，因为它们[会自动应用边衬区](https://developer.android.com/jetpack/compose/layouts/insets?hl=zh-cn#material3-components)。
-   如果应用使用的是 Compose 中的 Material 2 组件 ([`androidx.compose.material`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary?hl=zh-cn))，这些组件本身并不会自动处理边衬区。不过，您可以获得边衬区的访问权限，然后手动应用边衬区。在 [`androidx.compose.material 1.6.0`](https://developer.android.com/jetpack/androidx/releases/compose-material?hl=zh-cn#1.6.0-alpha03) 及更高版本中，使用 `windowInsets` 参数可为 [`BottomAppBar`](https://developer.android.com/reference/kotlin/androidx/compose/material/BottomAppBar.composable?hl=zh-cn#BottomAppBar(androidx.compose.foundation.layout.WindowInsets,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Shape,androidx.compose.ui.unit.Dp,androidx.compose.foundation.layout.PaddingValues,kotlin.Function1))、[`TopAppBar`](https://developer.android.com/reference/kotlin/androidx/compose/material/TopAppBar.composable?hl=zh-cn#TopAppBar(androidx.compose.foundation.layout.WindowInsets,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.unit.Dp,androidx.compose.foundation.layout.PaddingValues,kotlin.Function1))、[`BottomNavigation`](https://developer.android.com/reference/kotlin/androidx/compose/material/BottomNavigation.composable?hl=zh-cn#BottomNavigation(androidx.compose.foundation.layout.WindowInsets,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.unit.Dp,kotlin.Function1)) 以及 [`NavigationRail`](https://developer.android.com/reference/kotlin/androidx/compose/material/NavigationRail.composable?hl=zh-cn#NavigationRail(androidx.compose.foundation.layout.WindowInsets,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.unit.Dp,kotlin.Function1,kotlin.Function1)) 手动应用边衬区。同样，请为 [`Scaffold`](https://developer.android.com/reference/kotlin/androidx/compose/material/Scaffold.composable?hl=zh-cn#Scaffold(androidx.compose.foundation.layout.WindowInsets,androidx.compose.ui.Modifier,androidx.compose.material.ScaffoldState,kotlin.Function0,kotlin.Function0,kotlin.Function1,kotlin.Function0,androidx.compose.material.FabPosition,kotlin.Boolean,kotlin.Function1,kotlin.Boolean,androidx.compose.ui.graphics.Shape,androidx.compose.ui.unit.Dp,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,kotlin.Function1)) 使用 `contentWindowInsets` 参数。否则，请手动应用边衬区作为内边距。
-   如果应用使用了 View 和 Material 组件 ([`com.google.android.material`](https://developer.android.com/reference/com/google/android/material/classes?hl=zh-cn))，则大多数基于 View 的 Material 组件（例如 [`BottomNavigationView`](https://developer.android.com/reference/com/google/android/material/bottomnavigation/BottomNavigationView?hl=zh-cn)、[`BottomAppBar`](https://developer.android.com/reference/com/google/android/material/bottomappbar/BottomAppBar?hl=zh-cn)、[`NavigationRailView`](https://developer.android.com/reference/com/google/android/material/navigationrail/NavigationRailView?hl=zh-cn) 和 [`NavigationView`](https://developer.android.com/reference/com/google/android/material/navigation/NavigationView?hl=zh-cn)）都会处理边衬区，因此可能不需要执行额外的操作。不过，如果使用的是 [`AppBarLayout`](https://developer.android.com/reference/com/google/android/material/appbar/AppBarLayout?hl=zh-cn)，则需要添加 `android:fitsSystemWindows="true"`。
-   如果应用使用的是 View 和 `BottomSheet`、`SideSheet` 或自定义容器，请使用 [`ViewCompat.setOnApplyWindowInsetsListener`](https://developer.android.com/develop/ui/views/layout/edge-to-edge?hl=zh-cn#handle-overlaps) 应用内边距。对于 `RecyclerView`，请使用此监听器应用内边距，同时添加 `clipToPadding="false"`。
-   对于复杂的界面，请**使用** [`Scaffold`](https://developer.android.com/develop/ui/compose/components/scaffold?hl=zh-cn)（或 `NavigationSuiteScaffold`/`ListDetailPaneScaffold`），而非 `Surface`。`Scaffold` 可让您轻松放置 `TopAppBar`、`BottomAppBar`、`NavigationBar` 和 `NavigationRail`。

### 滚动内容

应用可能包含一些列表；受 Android 15 变更的影响，列表中的最后一项可能会被系统的导航栏遮挡。

![最后一个列表项被三按钮导航栏遮挡的应用。](https://developer.android.com/static/codelabs/edge-to-edge/img/4f3c69624ab53c4.png?hl=zh-cn)

上图显示了列表中的最后一项被三按钮导航栏遮挡住。

#### 使用 Compose 滚动内容

在 Compose 中，您可以使用 `LazyColumn` 的 [contentPadding](https://developer.android.com/develop/ui/compose/lists?hl=zh-cn#content-padding) 为最后一项内容增加空间，但使用 `TextField` 的情况除外：

```
Scaffold { innerPadding ->    LazyColumn(        contentPadding = innerPadding    ) {        // Content that does not contain TextField    }}
```

![最后一个列表项未被三按钮导航栏遮挡的应用。](https://developer.android.com/static/codelabs/edge-to-edge/img/784470a373da2125.png?hl=zh-cn)

上图显示了列表中的最后一项未被三按钮导航栏遮挡住。

对于 `TextField`，请使用 `Spacer` 在 `LazyColumn` 中绘制最后一个 `TextField`。如需了解详情，请参阅[边衬区使用](https://developer.android.com/develop/ui/compose/layouts/insets?hl=zh-cn#inset-consumption)。

```
LazyColumn(    Modifier.imePadding()) {    // Content with TextField    item {        Spacer(            Modifier.windowInsetsBottomHeight(                WindowInsets.systemBars            )        )    }}
```

#### 使用 View 滚动内容

对于 `RecyclerView` 或 `NestedScrollView`，请添加 `android:clipToPadding="false"`。

```
<androidx.recyclerview.widget.RecyclerView    android:id="@+id/recycler"    android:layout_width="match_parent"    android:layout_height="match_parent"    android:clipToPadding="false"    app:layoutManager="LinearLayoutManager" />
```

您可以使用 `setOnApplyWindowInsetsListener` 从窗口边衬区提供左侧、右侧和底部内边距：

```
ViewCompat.setOnApplyWindowInsetsListener(binding.recycler) { v, insets ->    val i = insets.getInsets(        WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()    )    v.updatePadding(        left = i.left,        right = i.right,        bottom = i.bottom + bottomPadding,    )    WindowInsetsCompat.CONSUMED}
```

### 使用 LAYOUT\_IN\_DISPLAY\_CUTOUT\_MODE\_ALWAYS

在指定 SDK 35 为目标平台之前，横屏模式下的 SocialLite 如下图所示：左侧边缘有一个为摄像头刘海屏预留的大白框。在三按钮导航中，按钮会位于右侧。

![横屏模式下的 SociaLite 应用。](https://developer.android.com/static/codelabs/edge-to-edge/img/c196c0ee2fa75c70.png?hl=zh-cn)

在指定 SDK 35 为目标平台之后，SociaLite 将如下图所示：左侧边缘不再有为摄像头刘海屏预留的大白框。为了实现这种效果，Android 会自动设置 [LAYOUT\_IN\_DISPLAY\_CUTOUT\_MODE\_ALWAYS](https://developer.android.com/reference/android/view/WindowManager.LayoutParams?hl=zh-cn#LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)。![横屏模式下的 SociaLite 应用。](https://developer.android.com/static/codelabs/edge-to-edge/img/d0a7078fa5bcf49c.png?hl=zh-cn)

根据您的应用，您可能需要在此处处理边衬区。

如需在 SociaLite 中执行此操作，请按以下步骤操作：

1.  在 `ui/ContactRow.kt` 文件中，找到 Row 可组合函数。
2.  修改内边距，以适应刘海屏。

```
@Composablefun ChatRow(   chat: ChatDetail,   onClick: (() -> Unit)?,   modifier: Modifier = Modifier,) {   // Add layoutDirection, displayCutout, startPadding, and endPadding.   val layoutDirection = LocalLayoutDirection.current   val displayCutout = WindowInsets.displayCutout.asPaddingValues()   val startPadding = displayCutout.calculateStartPadding(layoutDirection)   val endPadding = displayCutout.calculateEndPadding(layoutDirection)   Row(       modifier = modifier           ...           // .padding(16.dp) // Remove this line.           // Add this block:           .padding(               PaddingValues(                   top = 16.dp,                   bottom = 16.dp,                   // Ensure content is not occluded by display cutouts                   // when rotating the device.                   start = startPadding.coerceAtLeast(16.dp),                   end = endPadding.coerceAtLeast(16.dp)               )           ),       ...   ) { ... }
```

处理刘海屏后，SociaLite 将如下所示：

![横屏模式下的 SociaLite 应用。](https://developer.android.com/static/codelabs/edge-to-edge/img/fefe1046c96b3a57.png?hl=zh-cn)

您可以在[**开发者选项**](https://developer.android.com/studio/debug/dev-options?hl=zh-cn)屏幕的**刘海屏**下测试各种刘海屏配置。

如果应用具有使用 [`LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams?hl=zh-cn#LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT)、[`LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams?hl=zh-cn#LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER) 或 [`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams?hl=zh-cn#LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES) 的非浮动窗口（例如 Activity），则从 Android 15 Beta 2 开始，Android 会将这些刘海模式解读为 [`LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams?hl=zh-cn#LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)。以前，在 Android 15 Beta 1 中，您的应用会崩溃。

### 标题栏也是系统栏

标题栏也是一种系统栏，用于描述自由窗口的系统界面窗口装饰，例如顶部标题栏。您可以在 Android Studio 的桌面模拟器中查看标题栏。在下面的屏幕截图中，标题栏位于应用顶部。

![显示标题栏的模拟器。](https://developer.android.com/static/codelabs/edge-to-edge/img/4397f5ae495b66d5.png?hl=zh-cn)

在 Compose 中，如果您使用的是 [Scaffold](https://developer.android.com/develop/ui/compose/components/scaffold?hl=zh-cn) 的 `PaddingValues`、[`safeContent`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/package-summary?hl=zh-cn#(androidx.compose.foundation.layout.WindowInsets.Companion).safeContent())、[`safeDrawing`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/package-summary?hl=zh-cn#(androidx.compose.foundation.layout.WindowInsets.Companion).safeDrawing()) 或内置的 [`WindowInsets.systemBars`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/package-summary?hl=zh-cn#(androidx.compose.foundation.layout.WindowInsets.Companion).systemBars())，您的应用将按预期显示。不过，如果您使用 [`statusBar`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/package-summary?hl=zh-cn#(androidx.compose.foundation.layout.WindowInsets.Companion).statusBars()) 处理边衬区，那么应用内容可能无法按预期显示，因为状态栏不会考虑标题栏。

在 View 中，如果您使用 [`WindowInsetsCompat.systemBars`](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type?hl=zh-cn#systemBars()) 手动处理边衬区，您的应用将按预期显示。如果您使用 [`WindowInsetsCompat.statusBars`](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type?hl=zh-cn#statusBars()) 手动处理边衬区，您的应用可能无法按预期显示，因为状态栏并非标题栏。

### 处于沉浸模式的应用

处于[沉浸模式](https://developer.android.com/develop/ui/views/layout/immersive?hl=zh-cn)的屏幕基本不受 Android 15 强制执行的无边框措施的影响，因为沉浸式应用已经采用无边框设计。

### 保护系统栏

您可能希望应用在手势导航时使用透明栏，而在三按钮导航时使用半透明或不透明栏。

在 Android 15 中，默认使用半透明的三按钮导航栏，因为该平台会将 `window.isNavigationBarContrastEnforced` 属性设置为 `true`。手势导航则保持透明。

<table><tbody><tr><td colspan="1" rowspan="1"><p><img alt="采用三按钮导航的应用。" src="https://developer.android.com/static/codelabs/edge-to-edge/img/d618ecf78dd21d86.gif?hl=zh-cn"></p></td></tr><tr><td colspan="1" rowspan="1"><p>默认情况下，三按钮导航栏是半透明的。</p></td></tr></tbody></table>

一般来说，半透明的三按钮导航栏应该就足够了。不过，在某些情况下，应用可能需要不透明的三按钮导航栏。此时，请先将 `window.isNavigationBarContrastEnforced` 属性设置为 `false`。然后，使用 [`WindowInsetsCompat.tappableElement`](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type?hl=zh-cn#tappableElement())（针对 View）或 [`WindowInsets.tappableElement`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/WindowInsets.Companion?hl=zh-cn#(androidx.compose.foundation.layout.WindowInsets.Companion).tappableElement())（针对 Compose）。如果这些值为 0，则表示用户正在使用手势导航。否则，用户使用的是三按钮导航。如果用户使用的是三按钮导航，请在导航栏后面绘制一个视图或框。Compose 示例可能如下所示：

```
class MainActivity : ComponentActivity() {    override fun onCreate(savedInstanceState: Bundle?) {        super.onCreate(savedInstanceState)        setContent {            window.isNavigationBarContrastEnforced = false            MyTheme {                Surface(...) {                    MyContent(...)                    ProtectNavigationBar()                }            }        }    }}// Use only if required.@Composablefun ProtectNavigationBar(modifier: Modifier = Modifier) {   val density = LocalDensity.current   val tappableElement = WindowInsets.tappableElement   val bottomPixels = tappableElement.getBottom(density)   val usingTappableBars = remember(bottomPixels) {       bottomPixels != 0   }   val barHeight = remember(bottomPixels) {       tappableElement.asPaddingValues(density).calculateBottomPadding()   }   Column(       modifier = modifier.fillMaxSize(),       verticalArrangement = Arrangement.Bottom   ) {       if (usingTappableBars) {           Box(               modifier = Modifier                   .background(MaterialTheme.colorScheme.background)                   .fillMaxWidth()                   .height(barHeight)           )       }   }}
```

<table><tbody><tr><td colspan="1" rowspan="1"><p><img alt="采用三按钮导航的应用。" src="https://developer.android.com/static/codelabs/edge-to-edge/img/b9719f10d00c26e4.gif?hl=zh-cn"></p></td></tr><tr><td colspan="1" rowspan="1"><p>不透明的三按钮导航栏</p></td></tr></tbody></table>
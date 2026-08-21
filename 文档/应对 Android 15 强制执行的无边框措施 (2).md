## [6\. 查看解决方案代码](https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#5)

`MainActivity.kt` 文件的 `onCreate` 方法应如下所示：

```
class MainActivity : ComponentActivity() {   override fun onCreate(savedInstanceState: Bundle?) {       installSplashScreen()       enableEdgeToEdge()       window.isNavigationBarContrastEnforced = false       super.onCreate(savedInstanceState)       setContent {           Main(               shortcutParams = extractShortcutParams(intent),           )       }   }}
```

`ChatScreen.kt` 文件中的 `ChatContent` 可组合函数应处理边衬区：

```
private fun ChatContent(...) {   ...   Scaffold(...) { innerPadding ->       Column {           ...           InputBar(               input = input,               onInputChanged = onInputChanged,               onSendClick = onSendClick,               onCameraClick = onCameraClick,               onPhotoPickerClick = onPhotoPickerClick,               contentPadding = innerPadding.copy(                    layoutDirection, top = 0.dp                ),               sendEnabled = sendEnabled,               modifier = Modifier                   .fillMaxWidth()                   .windowInsetsPadding(                       WindowInsets.ime.exclude(WindowInsets.navigationBars)                    ),            )       }   }}
```

可在 main 分支中获取解决方案代码。如果您已下载 SociaLite，请执行以下命令：

`git checkout main`

如果还未下载，您可以再次下载该代码，以[直接](https://codeload.github.com/android/socialite/zip/refs/heads/main)或通过 git 查看 main 分支：

`git clone git@github.com:android/socialite.git`
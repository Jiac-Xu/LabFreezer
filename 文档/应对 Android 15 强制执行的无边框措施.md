## [4\. 以向后兼容的方式使 SociaLite 采用无边框设计](https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#3)

现在，SociaLite 已在 Android 15 上采用无边框设计，但在旧版 Android 设备上却并非如此。若要让 SociaLite 在旧版 Android 设备上采用无边框设计，请先调用 [`enableEdgeToEdge`](https://developer.android.com/reference/androidx/activity/ComponentActivity?hl=zh-cn#(androidx.activity.ComponentActivity).enableEdgeToEdge(androidx.activity.SystemBarStyle,androidx.activity.SystemBarStyle))，然后再设置 `MainActivity.kt` 文件中的内容。

```
class MainActivity : ComponentActivity() {    override fun onCreate(savedInstanceState: Bundle?) {        installSplashScreen()        enableEdgeToEdge() // Add this line.        window.isNavigationBarContrastEnforced = false        super.onCreate(savedInstanceState)        setContent {... }    }}
```

`enableEdgeToEdge` 的导入内容为 `import androidx.activity.enableEdgeToEdge`。依赖项为 [AndroidX Activity 1.8.0](https://developer.android.com/jetpack/androidx/releases/activity?hl=zh-cn#1.8.0) 或更高版本。

如需深入了解如何以向后兼容的方式使应用采用无边框设计以及如何处理边衬区，请参阅以下指南：

-   [Compose 中的窗口边衬区](https://developer.android.com/jetpack/compose/layouts/insets?hl=zh-cn)
-   [在应用中全屏显示内容](https://developer.android.com/develop/ui/views/layout/edge-to-edge?hl=zh-cn)

该开发者在线课程中关于无边框的部分到此就结束了。下一部分为可选内容，讨论了有关无边框的其他注意事项，可能也适用于您的应用。
# LabFreezer UI 设计规范总结

> 基于源代码完整分析（Theme.kt + 12 个 Screen 文件）

---

## 总体设计语言

**基于 Material Design 3 的清爽商务风格** — 使用 Jetpack Compose Material3 组件库，全量自定义 `lightColorScheme` / `darkColorScheme` 和 5 级 `Shapes` 系统，以 iOS 风格蓝色（`#007AFF`）为主色调，无自定义字体，强调清晰的信息层级与扁平化的卡片式列表。

---

## 1. 配色方案

来源：`Theme.kt:17-81`

### 浅色主题（`HyperLightColorScheme`）

| Token | 色值 | 用途 |
|---|---|---|
| `primary` | `#007AFF` | 主色调（按钮、FAB、链接、选中态图标） |
| `onPrimary` | `#FFFFFF` | Primary 上的文字/图标 |
| `primaryContainer` | `#D6EAFF` | 选中态卡片背景、容器 |
| `onPrimaryContainer` | `#001C3A` | primaryContainer 上的文字 |
| `secondary` | `#545F70` | 辅助按钮、次要强调 |
| `onSecondary` | `#FFFFFF` | Secondary 上的文字 |
| `secondaryContainer` | `#D8E3F8` | Tag chip 选中态背景 |
| `onSecondaryContainer` | `#111C2B` | secondaryContainer 上的文字 |
| `tertiary` | `#6E5676` | 第三强调色（网格占位格） |
| `error` | `#BA1A1A` | 删除操作、错误文字 |
| `onError` | `#FFFFFF` | Error 上的文字 |
| `background` | `#F8F9FA` | 全局背景 |
| `onBackground` | `#1A1C1E` | 背景上的主要文字 |
| `surface` | `#F8F9FA` | 页面/卡片表面色 |
| `onSurface` | `#1A1C1E` | 表面上的主要文字 |
| `surfaceVariant` | `#EDF0F5` | 网格空格背景 |
| `onSurfaceVariant` | `#43474E` | 次要文字 |
| `outline` | `#73777F` | 轮廓线、未选中图标 |
| `outlineVariant` | `#C3C6CF` | 分割线、边框 |
| `surfaceContainerLow` | `#FFFFFF` | 列表卡片背景（最低层级表面） |
| `surfaceContainer` | `#F2F3F5` | 中间层级表面 |
| `surfaceContainerHigh` | `#ECEDF0` | 较高层级表面（Tag chip 未选中态） |

### 深色主题（`HyperDarkColorScheme`）

| Token | 色值 |
|---|---|
| `primary` | `#A1CAFF` |
| `onPrimary` | `#00315C` |
| `primaryContainer` | `#004982` |
| `onPrimaryContainer` | `#D6EAFF` |
| `secondary` | `#BCC7DB` |
| `onSecondary` | `#263140` |
| `secondaryContainer` | `#3C4758` |
| `onSecondaryContainer` | `#D8E3F8` |
| `tertiary` | `#DBBCE2` |
| `error` | `#FFB4AB` |
| `background` | `#111318` |
| `onBackground` | `#E2E2E6` |
| `surface` | `#111318` |
| `onSurface` | `#E2E2E6` |
| `surfaceVariant` | `#43474E` |
| `onSurfaceVariant` | `#C3C6CF` |
| `outline` | `#8D9199` |
| `outlineVariant` | `#43474E` |
| `surfaceContainerLow` | `#1A1C21` |
| `surfaceContainer` | `#1F2126` |
| `surfaceContainerHigh` | `#292C31` |

### 主题切换

- **模式**：`LIGHT` / `DARK` / `SYSTEM`（默认值 2 = SYSTEM）（`ThemeState.kt:9`）
- **持久化**：`SharedPreferences`，key = `theme_prefs` / `theme_mode`（`ThemeState.kt:12-22`）
- **状态栏/导航栏**：背景色跟随 `colorScheme.background`，图标浅色/深色自适应（`Theme.kt:106-117`）

---

## 2. 圆角规格

### 全局形状系统

来源：`Theme.kt:83-89`

| Shapes 级别 | 代码 | 解析值 |
|---|---|---|
| `extraSmall` | `RoundedCornerShape(8.dp)` | 8dp |
| `small` | `RoundedCornerShape(12.dp)` | 12dp |
| `medium` | `RoundedCornerShape(16.dp)` | 16dp |
| `large` | `RoundedCornerShape(20.dp)` | 20dp |
| `extraLarge` | `RoundedCornerShape(28.dp)` | 28dp |

### 各组件实际圆角

| 组件 | 圆角 | 代码位置 |
|---|---|---|
| 列表卡片（设备/层/盒子/标签） | 16dp | `DeviceListScreen.kt:205`, `DeviceDetailScreen.kt:207`, `LayerDetailScreen.kt:211`, `TagManageScreen.kt:146` |
| 设置条目卡片 | 12dp | `SettingsScreen.kt:249` |
| 对话框内位置选择卡片 | 12dp | `DeviceDialogs.kt:154`, `LayerDialogs.kt:129` |
| 搜索栏 | 12dp | `MoveBrowserScreen.kt:238`, `StartPagePickerScreen.kt:164` |
| FAB | `CircleShape`（完全圆形） | `DeviceListScreen.kt:120` |
| Tag Chip | 20dp | `SampleEditScreen.kt:273`, `SearchScreen.kt:124` |
| 底部导航栏 | 24dp | `MainScreen.kt:234` |
| 网格单元格 | 4dp | `BoxGridScreen.kt:246`, `MoveBrowserScreen.kt:416` |
| 照片清理缩略图 | 8dp | `ImageCleanupScreen.kt:266` |
| 选中复选框 | `CircleShape`（直径24dp） | `DeviceListScreen.kt:216` |
| 颜色选择器色块 | `CircleShape`（直径36dp） | `TagManageScreen.kt:178` |
| 详情页照片卡片 | 12dp | `SampleEditScreen.kt:160` |
| 确认按钮（移动页） | 12dp | `MoveBrowserScreen.kt:465` |
| 设备类型卡片 | 12dp | `DeviceTypeManageScreen.kt:105` |
| 启动页选择器卡片 | `shapes.small` = 12dp | `StartPagePickerScreen.kt:276` |
| 树形节点指示点 | `CircleShape`（直径8dp） | `ImageCleanupScreen.kt:232` |

---

## 3. 阴影与抬升

| 组件 | elevation | 代码位置 |
|---|---|---|
| 列表卡片（设备/层/盒子/标签/搜索结果） | `1.dp` | `DeviceListScreen.kt:210`, `DeviceDetailScreen.kt:212`, `LayerDetailScreen.kt:216`, `TagManageScreen.kt:148`, `ImageCleanupScreen.kt:267` |
| 设置条目卡片 | `0.dp`（平面） | `SettingsScreen.kt:251` |
| 对话框内选择卡片 | `0.dp` | `DeviceDialogs.kt:156`, `LayerDialogs.kt:131` |
| 启动页选择器卡片 | `0.dp` | `StartPagePickerScreen.kt:280` |
| 设备类型卡片 | `0.dp` | `DeviceTypeManageScreen.kt:107` |
| 详情页照片卡片 | `4.dp` | `SampleEditScreen.kt:161` |
| 底部导航栏 | `tonalElevation = 6.dp`, `shadowElevation = 8.dp` | `MainScreen.kt:232-233` |
| 网格单元格 | 未设置（≈0dp 默认值） | `BoxGridScreen.kt:246` |
| 空状态图标 | 无 elevation | `DeviceListScreen.kt:132` |

- **阴影颜色**：未自定义，使用 Material3 系统默认（黑色半透明）
- **点击态抬升变化**：未定义，所有卡片点击时 elevation 不变
- **自定义阴影参数**：`shadowColor` / `shadowRadius` / `dx` / `dy` 均未使用
- **底部导航栏特殊处理**：额外叠加竖向渐变背景层（`MainScreen.kt:240-247`，`surface` 色从 alpha 0.9 到 0.7）

---

## 4. 字体与排版

- **字体家族**：未自定义，使用 Material3 默认（Android 系统 `sans-serif`）
- **排版系统**：`MaterialTheme.typography` 预设 style，未覆写 `Typography`

| 用途 | style | fontWeight | 示例文件:行号 |
|---|---|---|---|
| TopAppBar 标题 | `MaterialTheme.typography` 未指定（系统默认） | `SemiBold` | `DeviceListScreen.kt:81` |
| 卡片标题 | `titleMedium` | `Medium` | `DeviceListScreen.kt:228` |
| 设置区段标题 | `titleSmall` | `Medium` | `SettingsScreen.kt:111` |
| 空状态提示标题 | `titleMedium` | 默认 | `DeviceListScreen.kt:134` |
| 列表条目/搜索项 | `bodyLarge` | `Medium` | `MoveBrowserScreen.kt:306` |
| 辅助/副标题 | `bodyMedium` | 默认 / `Medium` | `MoveBrowserScreen.kt:197` |
| 次要说明/备注 | `bodySmall` | 默认 | `DeviceListScreen.kt:230` |
| 底部导航标签 | `labelSmall` | `SemiBold`(选中) / `Normal`(未选中) | `MainScreen.kt:287-288` |
| Tag chip 文字 | `labelMedium` | 默认 | `SampleEditScreen.kt:289` |
| 网格单元格文字 | `fontSize = 10.sp` / `11.sp`（硬编码） | `Bold`(占用) / `Normal`(空) | `BoxGridScreen.kt:251`, `MoveBrowserScreen.kt:424` |
| 空状态辅助文字 | `bodyMedium` | 默认 | `DeviceListScreen.kt:136` |
| 颜色选择器标签 | `bodyMedium` | 默认 | `TagManageScreen.kt:172` |
| 位置信息文字 | `bodyMedium` | `Medium` | `SampleEditScreen.kt:223` |
| 树形节点文字 | `bodyLarge` | `Medium` | `ImageCleanupScreen.kt:237` |
| 样本位置标签 | `labelSmall` | 默认 | `ImageCleanupScreen.kt:292` |
| 图片清理按钮文字 | `titleSmall` | `Medium` | `SettingsScreen.kt:111` |

- **行间距 / letterSpacing**：未自定义

---

## 5. 间距系统

| 规则 | 数值 | 代码位置 |
|---|---|---|
| 列表页面水平 padding | `16.dp` | `DeviceListScreen.kt:141` |
| 列表 items 垂直间距 | `12.dp` | `DeviceListScreen.kt:142` |
| 列表 contentPadding 上下 | `12.dp` | `DeviceListScreen.kt:143` |
| 卡片内部 padding | `16.dp` | `DeviceListScreen.kt:212` |
| 卡片内部水平 padding（设置卡片） | `horizontal=16.dp` | `SettingsScreen.kt:254` |
| 卡片内部垂直 padding（设置卡片） | `vertical=14.dp` | `SettingsScreen.kt:254` |
| 设置条目垂直间距 | `4.dp` | `SettingsScreen.kt:247` |
| 网格单元格间距 | `2.dp` | `BoxGridScreen.kt:183-184` |
| 网格容器 padding | `4.dp` | `BoxGridScreen.kt:182` |
| 底部导航栏外间距 | `start=16.dp, end=16.dp, bottom=16.dp` | `MainScreen.kt:228` |
| 搜索框外间距 | `horizontal=16.dp, vertical=8.dp` | `MoveBrowserScreen.kt:227` |
| 设置区块间隔 | `16.dp`（Spacer height） | `SettingsScreen.kt:162` |
| 对话框字段间距 | `8.dp` / `12.dp` | `DeviceDialogs.kt:74, 99` |
| 对话框内选择卡片与外字段间距 | `12.dp` | `DeviceDialogs.kt:180` |
| 按钮文字与图标间距 | `4.dp` / `8.dp` | `SampleEditScreen.kt:195`, `SettingsScreen.kt:125` |
| Tag chip 水平内边距 | `horizontal=12.dp` | `SampleEditScreen.kt:280` |
| Tag chip 垂直内边距 | `vertical=6.dp` | `SampleEditScreen.kt:280` |
| Tag chip 间距 | `8.dp` | `SampleEditScreen.kt:268` |
| 列表条目行 padding | `horizontal=16.dp, vertical=14.dp` | `MoveBrowserScreen.kt:300` |
| 面包屑栏内 padding | `horizontal=16.dp, vertical=10.dp` | `MoveBrowserScreen.kt:187` |
| 空状态图标大小 | `72.dp` | `DeviceListScreen.kt:132` |
| 选中复选框大小 | `24.dp` | `DeviceListScreen.kt:214` |
| 颜色选择器色块大小 | `36.dp` | `TagManageScreen.kt:178` |
| 颜色选择器间距 | `8.dp` | `TagManageScreen.kt:174` |
| 树形节点缩进基准 | `indent * 28.dp` | `ImageCleanupScreen.kt:226` |
| 启动页选择器缩进基准 | `indent * 32.dp` | `StartPagePickerScreen.kt:271` |
| 启动页选择器内边距 | `start=12.dp+indent, end=4.dp, top=12.dp, bottom=12.dp` | `StartPagePickerScreen.kt:283` |
| 标签详情页 contentPadding | `12.dp` | `TagDetailScreen.kt:59` |
| 设备类型列表间距 | `8.dp` | `DeviceTypeManageScreen.kt:99` |
| 照片网格间距 | `8.dp` | `ImageCleanupScreen.kt:258-259` |
| 编辑页内容水平 padding | `16.dp` | `SampleEditScreen.kt:155` |
| 详细页照片卡片高度 | `180.dp` | `SampleEditScreen.kt:163` |
| 编辑页字段间距 | `12.dp` | `SampleEditScreen.kt:199, 232, 248` |
| 树节点上下间距 | `top=6.dp, bottom=6.dp` | `ImageCleanupScreen.kt:228` |
| 移动页确认按钮高度 | `48.dp` | `MoveBrowserScreen.kt:464` |
| 搜索列表高度 | `200.dp` | `MoveBrowserScreen.kt:255` |
| 底部导航栏高度 | `64.dp` | `MainScreen.kt:252` |

---

## 6. 动效/过渡

| 类型 | 实现 | 代码位置 |
|---|---|---|
| 页面切换动画 | `NavHost` 使用 `slideIntoContainer` / `slideOutOfContainer`（方向随导航栈进出切换） | `MainScreen.kt:125-136` |
| Tab 滑动切换 | `HorizontalPager` + `rememberPagerState`，手势滑动 | `MainScreen.kt:328-341` |
| 涟漪效果 | Material3 内置 `ripple()` | 全应用 |
| 展开/收起箭头旋转 | `rotate(90f/0f)` | `ImageCleanupScreen.kt:243`, `StartPagePickerScreen.kt:300` |
| 过渡时长 | 未自定义，使用系统默认 | — |

---

## 7. 布局策略

| 维度 | 详情 | 代码位置 |
|---|---|---|
| 主要布局方式 | 全 Compose：`Scaffold` + `Column`/`Row`/`Box`/`LazyColumn`/`LazyVerticalGrid`；未使用 `ConstraintLayout` | — |
| 列表页 | `LazyColumn` + `Card`；水平 padding 16dp，item 间距 12dp | `DeviceListScreen.kt:140-143` |
| 网格页（BoxGrid） | `LazyVerticalGrid` + `GridCells.Fixed(cols)` + `aspectRatio(1f)` | `BoxGridScreen.kt:180-184` |
| 导航模式 | 底部 3 Tab + `NavHost` 混合；主 Tab 由 `HorizontalPager` 承载，详情页推栈 | `MainScreen.kt:103-210` |
| 底部导航实现 | 自定义 `FloatingBottomNav`：浮动 `Surface` + `tonalElevation` + `shadowElevation` + 渐变背景层 | `MainScreen.kt:216-299` |
| 层级导航（移动选择） | 面包屑栏 + Device → Layer → Box → Grid 四级 | `MoveBrowserScreen.kt:103-173` |
| 响应式 | `LocalConfiguration.current.screenWidthDp.dp` 计算 DropdownMenu 偏移 | `SettingsScreen.kt:74, 132` |
| 边到边 | `enableEdgeToEdge()` + `navigationBarsPadding()` | `MainActivity.kt:14`, `MainScreen.kt:124` |
| Tag 布局 | `FlowRow` + 自适应换行，间距 8dp | `SampleEditScreen.kt:268` |
| 树形展开（图片清理） | 设备→层→盒 三级展开，缩进 `indent * 28.dp` | `ImageCleanupScreen.kt:132-173` |
| 启动页选择器 | 树形展开，缩进 `indent * 32.dp` | `StartPagePickerScreen.kt:208-253` |
| 顶部导航栏 | `TopAppBar` + `TopAppBarDefaults.topAppBarColors(containerColor = surface)` | 全部 Screen |

---

> **注意**：本规范所有内容均基于源代码直接提取，未包含任何经验性补充。如后续需要新增组件，请参考上述 Token 值和尺寸定义以保持视觉一致性。

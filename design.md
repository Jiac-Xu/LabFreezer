# LabFreezer UI 设计规范

> 基于当前源代码完整分析（Theme.kt + 全部 Screen/组件文件）
> 覆盖：配色、圆角、阴影、排版、间距、图标、按钮、卡片、对话框、动效、布局、i18n

---

## 1. 总体设计语言

**基于 Material Design 3 的清爽商务风格** — 使用 Jetpack Compose Material3 组件库，全量自定义 `lightColorScheme` / `darkColorScheme` 和 5 级 `Shapes` 系统，以 iOS 风格蓝色（浅色 `#007AFF` / 深色 `#006FE8`）为主色调，无自定义字体，强调扁平化的卡片式列表与清晰的信息层级。

关键特征：
- 卡片统一使用 `surfaceContainerLow` 底色 + 1dp 抬升，圆角 12~16dp 两级
- 交互主色一律 `primary`，删除/危险操作用 `error`，次强调用 `secondary` / `tertiary`
- 列表、对话框、表单、搜索栏共用同一套「圆角 12dp + outline 半透明边框」视觉语言
- 支持浅色 / 深色 / 跟随系统三模式（默认跟随系统），持久化于 SharedPreferences
- 全量 i18n（简体中文 / 英文 / 繁体中文）

---

## 2. 配色方案

来源：`Theme.kt:17-81`

### 2.1 浅色主题（`HyperLightColorScheme`）

| Token | 色值 | 用途 |
|---|---|---|
| `primary` | `#007AFF` | 主色调（FAB、按钮内容、选中态、链接、聚焦边框、Switch 轨道、Slider） |
| `onPrimary` | `#FFFFFF` | primary 上的文字/图标 |
| `primaryContainer` | `#D6EAFF` | 选中卡片/格子/选择项背景 |
| `onPrimaryContainer` | `#001C3A` | primaryContainer 上的文字 |
| `secondary` | `#545F70` | 层级图标、次要强调 |
| `onSecondary` | `#FFFFFF` | secondary 上的文字 |
| `secondaryContainer` | `#D8E3F8` | Tag chip / 筛选 chip 选中态背景 |
| `onSecondaryContainer` | `#111C2B` | secondaryContainer 上的文字 |
| `tertiary` | `#6E5676` | 盒子图标、第三强调色 |
| `onTertiary` | `#FFFFFF` | tertiary 上的文字 |
| `tertiaryContainer` | `#F8D8FF` | 网格 COMPLETE 格底色 |
| `onTertiaryContainer` | `#271430` | tertiaryContainer 上的文字 |
| `error` | `#BA1A1A` | 删除操作、错误文字、删除按钮 |
| `onError` | `#FFFFFF` | error 上的文字 |
| `errorContainer` | `#FFDAD6` | 全宽危险按钮底色 |
| `onErrorContainer` | `#410002` | errorContainer 上的文字 |
| `background` | `#F8F9FA` | 全局背景（SampleEdit 容器、页面底） |
| `onBackground` | `#1A1C1E` | 背景上的主要文字 |
| `surface` | `#F8F9FA` | 页面/顶栏/底栏/面板表面色 |
| `onSurface` | `#1A1C1E` | 表面上的主要文字 |
| `surfaceVariant` | `#EDF0F5` | 网格空格背景、Switch 未选轨道、无图占位 |
| `onSurfaceVariant` | `#43474E` | 次要文字、SpeedDial 图标 |
| `outline` | `#73777F` | 轮廓线、未选中图标、编辑按钮、副标题文字 |
| `outlineVariant` | `#C3C6CF` | 分割线、边框 |
| `surfaceContainerLow` | `#FFFFFF` | 列表/设置/选择卡片统一底色 |
| `surfaceContainer` | `#F2F3F5` | 中间层级表面 |
| `surfaceContainerHigh` | `#ECEDF0` | 未选中 chip、SpeedDial 选项标签 |

### 2.2 深色主题（`HyperDarkColorScheme`）

| Token | 色值 |
|---|---|
| `primary` | `#006FE8` |
| `onPrimary` | `#00315C` |
| `primaryContainer` | `#004982` |
| `onPrimaryContainer` | `#D6EAFF` |
| `secondary` | `#BCC7DB` |
| `onSecondary` | `#263140` |
| `secondaryContainer` | `#3C4758` |
| `onSecondaryContainer` | `#D8E3F8` |
| `tertiary` | `#DBBCE2` |
| `onTertiary` | `#3D2947` |
| `tertiaryContainer` | `#553F5F` |
| `onTertiaryContainer` | `#F8D8FF` |
| `error` | `#FFB4AB` |
| `onError` | `#690005` |
| `errorContainer` | `#93000A` |
| `onErrorContainer` | `#FFDAD6` |
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

### 2.3 主题切换与系统栏

- 模式：`ThemeMode { LIGHT, DARK, SYSTEM }`，默认 SYSTEM（`ThemeState.kt:9`）
- 持久化：SharedPreferences，`theme_prefs` / `theme_mode`（`ThemeState.kt:15-22`）
- 解析：`LabFreezerTheme` 按 `LocalThemeMode.current` 决定（`Theme.kt:96-100`）
- 系统栏：`WindowCompat.setDecorFitsSystemWindows(window, false)`；`navigationBarColor` 动态取 `colorScheme.background`；状态栏/导航栏图标深浅跟随主题（`Theme.kt:104-118`）

---

## 3. 圆角规格

### 3.1 全局形状系统

来源：`Theme.kt:83-89`

| Shapes 级别 | 解析值 |
|---|---|
| `extraSmall` | 8dp |
| `small` | 12dp |
| `medium` | 16dp |
| `large` | 20dp |
| `extraLarge` | 28dp |

### 3.2 各组件实际圆角

| 组件 | 圆角 | 代码位置 |
|---|---|---|
| 列表卡片（设备/标签/独立盒子/分组头/最近盒子） | 12dp | `DeviceListScreen.kt:473,536,589` |
| 列表卡片（层详情盒子/设备详情子项/标签详情样本） | 16dp | `LayerDetailScreen.kt:236`, `DeviceDetailScreen.kt:293`, `TagDetailScreen.kt:95` |
| 搜索结果卡片 | 16dp | `SearchScreen.kt:509` |
| 移动页列表卡片 | 16dp | `MoveBrowserScreen.kt:318` |
| 设置条目卡片 | 12dp | `SettingsScreen.kt:236` |
| OCR 设置卡片 | 12dp | `OcrSettingsScreen.kt:94,138,183` |
| 对话框内位置/设备选择卡片 | 12dp | `DeviceDialogs.kt`, `LayerDialogs.kt` |
| 表单输入框 / 搜索栏 | 12dp | 各 `fieldShape`，`SearchScreen.kt:141`, `MoveBrowserScreen.kt:247` |
| 移动页确认按钮 | 12dp | `MoveBrowserScreen.kt:494` |
| 照片操作按钮（样本编辑） | 默认实心按钮 | `SampleEditScreen.kt:299-365` |
| FAB / SmallFAB | `CircleShape` | `SpeedDialFAB.kt:70,147,198` |
| SpeedDial 选项文字标签 | 12dp | `SpeedDialFAB.kt:181` |
| Tag Chip / 筛选 Chip | 20dp | `SampleEditScreen.kt:451`, `SearchScreen.kt:433` |
| 搜索历史项 | 20dp | `SearchScreen.kt:489` |
| 底部导航栏 | 24dp | `MainScreen.kt:407` |
| 缩放滑块容器 | 24dp | `BoxGridScreen.kt:363` |
| 网格单元格 | 4dp | `BoxGridScreen.kt:456`, `MoveBrowserScreen.kt:445` |
| 照片清理缩略图 | 8dp | `ImageCleanupScreen.kt:332` |
| 照片清理删除确认对话框 | 16dp | `ImageCleanupScreen.kt:232,271` |
| 详情页照片卡片 | 12dp | `SampleEditScreen.kt:258` |
| 选中圆圈 / 颜色色块 | `CircleShape` | 各处 |

---

## 4. 阴影与抬升

| 组件 | elevation | 代码位置 |
|---|---|---|
| 列表/搜索/移动页卡片 | `1.dp` | `DeviceListScreen.kt:541`, `SearchScreen.kt:510`, `MoveBrowserScreen.kt:318` 等 |
| 照片清理缩略图 | `1.dp` | `ImageCleanupScreen.kt:333` |
| SpeedDial 选项标签 | `1.dp` | `SpeedDialFAB.kt:185` |
| 设置 / OCR 设置条目卡片 | `0.dp`（平面） | `SettingsScreen.kt:238`, `OcrSettingsScreen.kt:97` |
| 对话框内选择卡片 | `0.dp` | `DeviceDialogs.kt`, `LayerDialogs.kt` |
| 启动页选择器卡片 | `0.dp` | `StartPagePickerScreen.kt:491` |
| 分组头卡片 | `0.dp` | `DeviceListScreen.kt:591` |
| 详情页照片卡片 | `4.dp` | `SampleEditScreen.kt:259` |
| 底部导航栏 | `tonalElevation = 6.dp`, `shadowElevation = 8.dp` | `MainScreen.kt:405-406` |
| 缩放滑块容器 | `tonalElevation = 6.dp`, `shadowElevation = 8.dp` | `BoxGridScreen.kt:361-362` |
| 网格单元格 | 未设置（≈0dp） | `BoxGridScreen.kt:450` |

- 阴影颜色：Material3 系统默认，未自定义
- 点击态抬升：无变化，全部卡片点击时 elevation 不变
- 特殊渐变背景：底部导航栏与缩放滑块容器均叠加 `surface` 竖向渐变（0.9 → 0.7 alpha）

---

## 5. 字体与排版

- 字体家族：未自定义，Material3 默认（系统 `sans-serif`）
- 排版系统：`MaterialTheme.typography` 预设，未覆写 `Typography`

| 用途 | style | fontWeight | 代码位置 |
|---|---|---|---|
| TopAppBar 标题 | 默认（`titleLarge`） | `SemiBold` | 全部 Screen |
| 卡片主标题（设备/盒子/标签/独立盒子） | `titleMedium` | `Medium` | `DeviceListScreen.kt:559`, `LayerDetailScreen.kt:257` |
| 区块标题 / 分组头 / 最近盒子名 | `titleSmall` | `Medium` | `DeviceListScreen.kt:320,597`, `SearchScreen.kt:355` |
| 设置区段标题 | `titleSmall`（带 `"  "` 前缀） | `Medium`，色 `primary` | `SettingsScreen.kt:115` 等 |
| 列表条目 / 搜索结果 / 设置卡标题 | `bodyLarge` | `Medium` | `MoveBrowserScreen.kt:328`, `SearchScreen.kt:516`, `SettingsScreen.kt:247` |
| 面包屑 / 位置文本 / 搜索结果标题 | `bodyMedium` | `Medium` | `MoveBrowserScreen.kt:206`, `SampleEditScreen.kt:399` |
| 副标题 / 备注 / 计数 | `bodySmall` | 默认，色 `outline` | `DeviceListScreen.kt:562`, `SearchScreen.kt:589` |
| chip 标签文字 | `labelMedium` | 默认 | `SampleEditScreen.kt:467`, `SearchScreen.kt:443` |
| 底栏文字 | `labelSmall` | 选中 `SemiBold` / 未选 `Normal` | `MainScreen.kt:458-463` |
| SpeedDial 选项文字 | `labelLarge` | `Medium` | `SpeedDialFAB.kt:189` |
| 网格单元格文字 | `fontSize = 10.sp` | EMPTY `Normal` / COMPLETE `Bold` | `BoxGridScreen.kt:461,475` |
| 移动页网格文字 | `fontSize = 11.sp` / `8.sp`(✕) | 选中 `Bold` | `MoveBrowserScreen.kt:453-464` |
| 应用名（关于页） | `headlineMedium` | `Bold` | `AboutScreen.kt:147` |

- 行间距 / letterSpacing：未自定义
- 对话框标题、确认按钮文字一律 `SemiBold`；卡片标题 `Medium`

---

## 6. 间距系统

### 6.1 页面级

| 规则 | 数值 | 代码位置 |
|---|---|---|
| 列表页水平 padding | `16.dp` | 各 LazyColumn |
| 列表 items 垂直间距 | `12.dp` | `DeviceListScreen.kt:248` 等 |
| 列表 contentPadding 顶部 | `12.dp` | 各页 |
| 列表 contentPadding 底部（FAB 预留） | `100.dp` | `DeviceListScreen.kt:249`, `SettingsScreen.kt:218` |
| 详情页内容垂直 padding | `12.dp` | `DeviceDetailScreen.kt:178` |
| 设置页底部预留 | `100.dp` | `SettingsScreen.kt:218` |
| 图片清理底部预留 | `80.dp` | `ImageCleanupScreen.kt:193` |

### 6.2 卡片内部

| 组件 | padding | 代码位置 |
|---|---|---|
| 列表卡片内容 | `16.dp`（四周） | `DeviceListScreen.kt:543` 等 |
| 最近盒子卡 | `12.dp` | `DeviceListScreen.kt:481` |
| 设置/OCR/选择卡片 Row | `horizontal 16, vertical 14` | `SettingsScreen.kt:241`, `OcrSettingsScreen.kt:100` |
| Switch 卡 Row | `horizontal 16, vertical 10` | `PersonalizationScreen.kt:328` |
| 分组头 / 开关卡 Row | `horizontal 16, vertical 10` | `DeviceListScreen.kt:594`, `DeviceTypeManageScreen.kt:120` |
| 图标与文字间距（普通卡） | `12.dp` | `DeviceListScreen.kt:351` |
| 图标与文字间距（设置卡） | `16.dp` | `SettingsScreen.kt:245` |
| 标题与操作按钮间距 | `8.dp` | `DeviceListScreen.kt:566` |
| 标题与 note 副标题间距 | `2.dp` | `DeviceListScreen.kt:561` |

### 6.3 对话框

| 规则 | 数值 |
|---|---|
| 字段间距 | `12.dp` |
| 位置卡与字段间距 | `16.dp` |
| 选择列表 LazyColumn 间距 | `8.dp` |
| 选择卡 Row padding | `horizontal 16, vertical 14` |
| 确认按钮 `enabled` 校验 | 名称非空 / 行列数字合法 |

### 6.4 输入控件与按钮

| 规则 | 数值 |
|---|---|
| 按钮文字与图标间距 | `4.dp`（照片操作/管理标签）/ `8.dp`（全宽按钮） |
| 确认按钮高度 | `48.dp` |
| 照片操作按钮容器行间距 | `4.dp` |
| 照片操作按钮图标 | `20.dp` |
| 编辑/删除 IconButton 图标 | `20.dp` |
| 卡片前置图标 | `24.dp` |
| 对话框选择卡图标 | `22.dp` |
| 设置卡图标 | `22.dp` |
| 输入模式按钮图标 | `18.dp` |

### 6.5 Chip 与标签

| 规则 | 数值 |
|---|---|
| chip 圆角 | `20.dp` |
| chip 内边距 | `horizontal 12, vertical 6` |
| chip 间距 | FlowRow `8.dp` / `4.dp` |
| 标签色点 | `10.dp` |
| 颜色选择器色块 | `36.dp` |
| 颜色选择器间距 | `8.dp` |

### 6.6 网格与树形

| 规则 | 数值 |
|---|---|
| 网格单元格间距 | `2.dp`（BoxGrid）/ `4.dp`（移动页） |
| 网格容器 padding | `4.dp` |
| 单元格圆角 | `4.dp` |
| 树形缩进（图片清理） | `indent * 28.dp` |
| 树形缩进（启动页选择器） | `indent * 32.dp` |
| 树形色点 | `8.dp` |
| 照片缩略图网格 | 3 列，行高 140dp |

### 6.7 其他关键尺寸

| 组件 | 数值 |
|---|---|
| 空状态图标 | `72.dp` |
| 底栏高度 | `64.dp` |
| 缩放滑块高度 | `64.dp` |
| 底栏外间距 | `16.dp` 左右 + 16dp 底 |
| 照片卡片高度 | `180.dp` |
| 旋转按钮 | `36.dp` 圆 + 图标 20dp |
| 选中圆圈（列表） | `24.dp` + 对勾 16dp |
| 选中徽标（网格） | `22.dp` + 对勾 14dp |
| 最近盒子卡宽度 | `160.dp` |
| 移动页搜索列表高度 | `200.dp` |
| 关于页图标 | `80.dp` |
| 选中项指示器内边距（底栏） | `4.dp` |
| 滑动切页阈值 | `150f` px（主 Tab）/ `50.dp`（样本编辑） |

---

## 7. 图标规格

| 用途 | 图标 | size | tint | 位置 |
|---|---|---|---|---|
| 设备（FREEZER） | `DeviceHub` | 24dp | `primary` | `DeviceDetailScreen.kt:312` |
| 层级（LEVEL） | `Layers` | 24dp | `secondary` | `DeviceDetailScreen.kt:313` |
| 盒子（BOX） | `Inventory2` | 24dp | `tertiary` | `DeviceDetailScreen.kt:314`, `LayerDetailScreen.kt:254` |
| 独立盒子 | `Inventory2` | 24dp | `tertiary` | `DeviceListScreen.kt:350` |
| 最近盒子 | `Inventory2` | 24dp | `primary` | `DeviceListScreen.kt:497` |
| 编辑按钮 | `Edit` | 20dp | `outline` | 各处 |
| 删除按钮 | `Delete` | 20dp | `error @ 0.7f` | 各处 |
| 位置/设备选择卡 | `Layers`/`DeviceHub` | 22dp | `primary` | `LayerDialogs.kt`, `DeviceDialogs.kt` |
| 位置卡右箭头 | `ArrowForwardIos` | 16dp | `outline` | `SampleEditScreen.kt:401` |
| 列表项右箭头 | `ChevronRight` | 默认 | `outline` | `MoveBrowserScreen.kt:329` 等 |
| 面包屑分隔符 | `ChevronRight` | 18dp | `outline` | `MoveBrowserScreen.kt:213` |
| 照片操作按钮 | `CameraAlt`/`PhotoLibrary`/`DocumentScanner`/`Delete` | 20dp | 随按钮 | `SampleEditScreen.kt:325-362` |
| 旋转照片 | `RotateRight` | 20dp | `White` | `SampleEditScreen.kt:278` |
| 设置/OCR 卡图标 | 各类 | 22dp | `primary` | `SettingsScreen.kt:244`, `OcrSettingsScreen.kt:106` |
| SpeedDial 选项 | `Inventory2`/`Layers` | 24dp | `onSurfaceVariant` | `SpeedDialFAB.kt:112,130` |
| SpeedDial 主 FAB | `Add` | 默认 | `onPrimary` | `SpeedDialFAB.kt:74` |

---

## 8. 按钮与输入控件

### 8.1 按钮体系

| 类型 | 样式 | 用途 | 位置 |
|---|---|---|---|
| FAB / SmallFAB | 圆形、`primary`/`onPrimary` 或 `surfaceContainerHigh`/`onSurfaceVariant` | 主操作、SpeedDial | `SpeedDialFAB.kt` |
| SpeedDial 选项 | 标签 Card（12dp 圆角、1dp、`surfaceContainerHigh`）+ SmallFAB | 创建盒子/层级 | `SpeedDialFAB.kt:168-205` |
| 实心 Button | `containerColor = surfaceContainerLow`、无边框、内容色 `primary`（危险操作用 `error`）；禁用态保持底色、内容 `outline @ 0.5f` | 照片操作 4 按钮 | `SampleEditScreen.kt:299-365` |
| 实心 Button | `containerColor = primary` / `onPrimary` | 导入确认、批量 OCR、更新 | `MainScreen.kt:750`, `OcrSettingsScreen.kt:298` |
| 实心 Button | `errorContainer` / `onErrorContainer` | 图片清理全宽按钮 | `ImageCleanupScreen.kt:114` |
| 实心 Button | `containerColor = error` / `onError` | 危险确认（删除/覆盖） | `MainScreen.kt:657`, `ImageCleanupScreen.kt:216` |
| 对话框按钮 | `TextButton`，确认文字 `SemiBold` | 对话框确认/取消 | 全部对话框 |
| 多选操作 | 图标按钮：全选 `TextButton`(Medium)、移动 `OpenWith`(primary)、删除 `Delete`(error) | 批量操作 | 各多选屏 |

### 8.2 输入控件

| 控件 | 样式 | 位置 |
|---|---|---|
| OutlinedTextField | `fieldShape = RoundedCornerShape(12.dp)`；`focusedBorderColor = primary`，`unfocusedBorderColor = outline @ 0.3f` | 全部对话框/搜索栏/表单 |
| 文本字段 | `singleLine`；备注 `maxLines 3`（对话框）/ `maxLines 8`（样本编辑） | 各处 |
| 行/列字段 | 两列各 `weight(1f)`，仅数字过滤 | `LayerDialogs.kt:226` |
| 类型/设备选择字段 | `readOnly` + 全屏点击触发 DropdownMenu | `DeviceDialogs.kt` |
| Switch | `checkedThumbColor = White`，`checkedTrackColor = primary`，`uncheckedThumbColor = outline`，`uncheckedTrackColor = surfaceVariant` | `PersonalizationScreen.kt:341`, `OcrSettingsScreen.kt:126` 等 |
| Slider | thumb/activeTrack `primary`，inactiveTrack `surfaceVariant`，thumb 16dp 圆点 | `BoxGridScreen.kt:388-402` |
| 下拉菜单 | `DpOffset(x = screenWidth - 196.dp)` 右对齐 | `PersonalizationScreen.kt:127` |

---

## 9. 列表卡片与多选模式

### 9.1 卡片通用结构

```
Row(fillMaxWidth.padding(16.dp), CenterVertically)
├─ [选中圆圈 24dp]        （仅多选）
├─ [前置图标 24dp]
├─ Column(weight 1f)
│   ├─ 标题 titleMedium + Medium（单行省略）
│   └─ 副标题 bodySmall + outline（note/计数/路径，非空才显示）
└─ [编辑/删除 IconButton 20dp]（非多选时）
```

- 选中容器色：`primaryContainer`；未选：`surfaceContainerLow`
- 编辑按钮 `Edit` + `outline`；删除按钮 `Delete` + `error @ 0.7f`
- 设备详情 `VisibleChildCard` 的编辑/删除/note 副标题仅 LEVEL/BOX 显示

### 9.2 多选模式（isSelecting）

- TopAppBar 标题变为「已选 N 项」`SemiBold`
- 返回箭头作为取消，actions = 全选/取消全选 TextButton → 移动 `OpenWith` → 删除 `Delete`
- 选中圆圈：24dp CircleShape + `primary` 背景 + `Check` 16dp `White`；未选透明
- 多选时 FAB 隐藏
- 批量删除统一 AlertDialog，确认按钮 `error` 色

### 9.3 独立盒子区块

- 区块标题「盒子」`titleSmall` + Medium + `primary`（前 Spacer 4dp / 后 12dp）
- 卡片：圆角 12dp、elevation 1dp、`Inventory2` 24dp `tertiary`、note 副标题

---

## 10. 对话框规范

| 规则 | 值 |
|---|---|
| AlertDialog 圆角 | 主题默认（Material3 系统 shape） |
| 标题 | `SemiBold` |
| 确认按钮 | `TextButton` + `SemiBold`，带 `enabled` 校验 |
| 危险确认按钮 | `Button` `error` 色 或 `TextButton` + `error` 文字 |
| 字段样式 | 12dp 圆角 + primary 聚焦边框 |
| 位置/设备选择卡 | 12dp 圆角、`surfaceContainerLow`、elevation 0、Row padding 16/14、图标 22dp |

删除确认存在两处实现：
- `devices/DeviceDialogs.kt` 版：使用资源字符串
- `layers/DeleteConfirmDialog.kt` 版：硬编码中文（历史遗留，建议统一）

---

## 11. 动效与过渡

| 类型 | 实现 | 位置 |
|---|---|---|
| 页面切换 | NavHost `slideIntoContainer` / `slideOutOfContainer`，`tween(350, FastOutSlowInEasing)`；样本编辑互跳方向由 `NavAnimState.isSwipePrevious` 决定 | `MainScreen.kt:227-250` |
| Tab 切换 | `AnimatedContent`：`slideInHorizontally + fadeIn` / `slideOutHorizontally + fadeOut`，`tween(300)` | `MainScreen.kt:488-525` |
| Tab 手势 | `detectHorizontalDragGestures`，阈值 150f px | `MainScreen.kt:508-524` |
| SpeedDial 展开 | 主 FAB 加号旋转 45°；选项 `AnimatedVisibility` + `fadeIn + slideInVertically` | `SpeedDialFAB.kt:85-99` |
| 筛选面板折叠 | `AnimatedVisibility` + `expandVertically` / `shrinkVertically`；滚动自动收起 | `SearchScreen.kt:385-389` |
| 网格详情遮罩 | `AnimatedVisibility` + `fadeIn + expandVertically` | `BoxGridScreen.kt:480-483` |
| 树形箭头旋转 | `rotate(90f/0f)` | `StartPagePickerScreen.kt:514`, `ImageCleanupScreen.kt:309` |
| 样本编辑滑动 | 横向拖动切换相邻样本，阈值 50.dp | `SampleEditScreen.kt:186-201` |
| 涟漪 | Material3 内置 `ripple()` | 全应用 |
| 设备分组展开 | 无动画（条件渲染） | `DeviceListScreen.kt:271-281` |

---

## 12. 布局策略与导航

| 维度 | 详情 | 位置 |
|---|---|---|
| 布局方式 | 全 Compose：`Scaffold` + `Column`/`Row`/`Box`/`LazyColumn`/`LazyVerticalGrid` | — |
| 主导航 | 主屏 `NavHost`（含 MainTabs + 全部子屏），另一 `NavGraph.kt` 为遗留未引用 | `MainScreen.kt:222-251` |
| Tab 承载 | 自定义 `MainTabPager`（AnimatedContent），非官方 HorizontalPager | `MainScreen.kt:474-541` |
| 底部导航 | 自定义 `FloatingBottomNav`：24dp 圆角浮动 `Surface` + tonal/shadow + 渐变层 + 选中指示器 | `MainScreen.kt:387-472` |
| 层级导航（移动） | 面包屑栏 + Device → Layer → Box → Grid 四级 | `MoveBrowserScreen.kt` |
| 树形导航（选择/清理） | 设备→层→盒 逐级展开，缩进 28/32dp/级 | `StartPagePickerScreen.kt`, `ImageCleanupScreen.kt` |
| 网格页 | `LazyVerticalGrid` + `GridCells.Fixed(cols)` + `aspectRatio(1f)`，横向滚动，`cellWidth = screenWidth / visibleCols` | `BoxGridScreen.kt:277-301` |
| 启动页 | `StartPagePreference` 决定首页；启动 Tab 由 `BottomTabPreference` 决定 | `MainScreen.kt:118-172` |
| 冷启动恢复 | `FairMemoryReceiver` 保存路由 JSON，恢复时 `popUpTo(0)` + `launchSingleTop` | `MainScreen.kt:176-219` |
| 边到边 | `enableEdgeToEdge()`（MainActivity）+ `navigationBarsPadding()` | `MainActivity.kt:19` |
| 标签布局 | `FlowRow` + 自适应换行 | `SampleEditScreen.kt:446` |
| TopAppBar | 统一 `containerColor = surface`，`titleContentColor = onSurface` | 全部 Screen |

路由结构（`NavRoutes.kt`）：DeviceList / DeviceDetail / LayerDetail / BoxGrid / SampleEdit / SampleCreate(遗留空实现) / Search / TagManage / TagDetail / Settings / StartPagePicker / ImageCleanup / OcrSettings / MoveBrowser / DeviceTypeManage / About / BottomBarEdit / Personalization / MainTabs

---

## 13. 空状态

| 屏幕 | 图标(72dp) | 主文案 | 提示 |
|---|---|---|---|
| 设备列表 | `DevicesOther` | 「暂无设备」`titleMedium` `outline@0.7` | `bodyMedium` `outline@0.5` |
| 设备详情 | `Layers` | 同上 | 同上 |
| 层详情 | `Inventory2` | 「添加盒子」 | 无提示 |
| 盒子网格 | 无 | 「加载中」`bodyLarge` | — |
| 标签管理 | `Tag` | 「暂无标签」 | 硬编码中文提示 |
| 标签详情 | 无 | 「暂无样本」 | — |
| 搜索 | `Search`/`SearchOff` | `bodyMedium` `outline@0.7` | — |

统一规格：图标 `outline @ 0.5f`，居中 Column，图标与文字间距 16dp。

---

## 14. i18n 规范

- 语言资源：`values/strings.xml`（简体中文）、`values-en/strings.xml`、`values-zh-rTW/strings.xml`
- 约 230+ 个 key，分 20+ 注释段（Common / Speed Dial / Bottom Tab / Start Page Picker / Dialogs / Settings / Import / OCR / About / Move / Search / Sample Edit / Tag / Box Grid 等）
- 屏幕内一律 `stringResource`；非 Composable 上下文用 `context.getString`
- 样本编辑副标题/筛选摘要提供 Composable 与非 Composable 双实现（`buildBrowseSubtitle` / `buildBrowseSubtitleText`）

已知硬编码待补（历史遗留）：`layers/DeleteConfirmDialog.kt` 全部文案、`TagManageScreen.kt` 空状态提示、`SearchScreen.kt` 筛选折叠/展开 contentDescription、`SampleEditScreen.kt` 旋转按钮 contentDescription "Rotate"。

---

> **注意**：本规范基于当前源码提取，行号随代码演进可能偏移，以「组件 + 样式」描述为准。新增组件时请优先复用本规范中的 Token 值、圆角、间距与字体组合以保持视觉一致性。

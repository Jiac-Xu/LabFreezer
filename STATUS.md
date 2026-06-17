# LabFreezer 开发状态

## 项目概况
- **应用名**: LabFreezer — 实验室样本冷冻存储管理系统
- **包名**: com.labfreezer
- **版本**: 1.1.0 (versionCode=2)
- **最低支持**: Android 8.0 (API 26)
- **目标 SDK**: Android 15 (API 36)
- **APK 大小**: ~20MB

## 开发环境

| 工具 | 版本 |
|------|------|
| Android Studio | Latest (D:\Program Files\AndroidStudio) |
| JDK | OpenJDK 21 (Android Studio JBR) |
| Gradle | 8.12 |
| AGP | 8.9.1 |
| Kotlin | 2.1.20 |
| KSP | 2.1.20-1.0.31 |
| Compose BOM | 2025.03.01 |
| Room | 2.6.1 |
| Hilt | 2.55 |
| compileSdk | 36 |

### 构建命令
```powershell
$env:JAVA_HOME = "D:\Program Files\AndroidStudio\jbr"
./gradlew assembleDebug
```

### ADB 路径
```
C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

---

## 功能完成状态

### Phase 1: 项目脚手架 + 数据库 ✅
- Gradle 构建系统 (Compose + Room + Hilt + CameraX + Coil)
- Room Entity × 6 (StorageDevice, StorageLayer, StorageBox, SamplePosition, Tag, SampleTag)
- DAO × 6, AppDatabase (v2)
- Hilt DI 模块
- Material 3 HyperOS 风格主题 (亮/暗色)
- 导航路由 (9 路由)
- ✅ BUILD SUCCESSFUL

### Phase 2: 设备管理 CRUD ✅
- Repository × 4 (Device, Layer, Box, Sample)
- ViewModel × 3 (DeviceList, DeviceDetail, LayerDetail)
- Dialogs (Device, Layer, Box, DeleteConfirm)
- DeviceListScreen / DeviceDetailScreen / LayerDetailScreen
- 完整层级：设备 → 层 → 盒子

### Phase 3: 冻存盒网格 + 相机 ✅
- BoxGridViewModel (动态网格、状态判断、拍照流程)
- BoxGridScreen (N×M 动态网格、三色状态)
- 系统相机 Intent 拍照集成
- PhotoManager (压缩、命名、删除)
- FileProvider + file_paths.xml

### Phase 4: 样本编辑 ✅
- SampleEditViewModel (SavedStateHandle)
- SampleEditScreen (照片预览、拍照/重拍、DatePicker、标签选择)
- 标签多选 (FlowRow + AssistChip)

### Phase 5: 搜索 ✅
- SamplePositionDao JOIN 4 表查询 (SampleWithPath)
- 搜索名称+备注 (name LIKE OR note LIKE)
- SearchViewModel (300ms 防抖)
- SearchScreen (自动聚焦、实时搜索、完整路径)

### Phase 6: 导出 ✅
- ExportEngine: CSV + Markdown + PDF (Android PdfDocument)
- ExportViewModel (直接导出+分享，无 BottomSheet)
- 数据库导出/导入 (.zip)
- 系统分享菜单 (Intent.ACTION_SEND)

### 附加: 标签系统 ✅
- TagEntity + SampleTagEntity (多对多)
- TagManageScreen (CRUD + 16色选择)
- TagDetailScreen (查看标签下所有样本)
- SampleEditScreen 标签选择

---

## 项目结构 (48 个 .kt 文件)

```
com.labfreezer/
├── MainActivity.kt
├── LabFreezerApp.kt
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── entity/   (6个: Device, Layer, Box, Sample, Tag, SampleTag)
│   │   └── dao/      (6个)
│   ├── repository/   (4个: Device, Layer, Box, Sample)
│   └── file/
│       └── PhotoManager.kt
├── export/
│   └── ExportEngine.kt
├── di/
│   ├── AppModule.kt
│   └── DatabaseModule.kt
└── ui/
    ├── MainScreen.kt
    ├── theme/
    │   ├── Theme.kt (HyperOS 风格 Material 3)
    │   └── ThemeState.kt
    ├── navigation/
    │   ├── NavRoutes.kt
    │   └── NavGraph.kt
    └── screens/
        ├── devices/   (DeviceList/Detail + VMs + Dialogs)
        ├── layers/    (LayerDetail + VM + Dialogs + DeleteConfirm)
        ├── boxgrid/   (BoxGrid + VM)
        ├── sample/    (SampleEdit + VM)
        ├── search/    (Search + VM)
        ├── tags/      (TagManage/Detail + VMs)
        ├── settings/  (SettingsScreen)
        └── export/    (ExportViewModel)
```

---

## 已知问题

### 主题切换不生效
- **现象**: 设置页选择深色/浅色模式后，界面不变化
- **原因**: `activity.recreate()` 在 Compose 环境中可能不触发主题重新加载。`ThemePreferences.getMode()` 在 `remember` 中只读取一次
- **建议修复**: 使用 `LocalConfiguration` + 手动切换 `isSystemInDarkTheme`，或使用 `AppCompatDelegate.setDefaultNightMode()`

### LiquidGlass 库无法接入
- **原因**: LiquidGlass v2.0.0 需要 Kotlin 2.3.0 + compileSdk 37，但 Hilt 2.55 仅支持到 Kotlin 2.2.0 metadata
- **结论**: 当前工具链下无法集成，需等待 Hilt 更新

### 状态栏颜色
- 浅色模式下状态栏可能仍为白色（`enableEdgeToEdge` + `WindowCompat` 配置可能未完全生效）

---

## 数据库表结构 (v2)

```
StorageDevice 1 → N StorageLayer 1 → N StorageBox 1 → N SamplePosition
Tag N ←→ N SamplePosition (通过 SampleTag 关联表)
```

- `storage_device`: 设备 (名称, 类型 FREEZER_M80/LIQUID_NITROGEN)
- `storage_layer`: 层 (设备下)
- `storage_box`: 冻存盒 (层下, 行数×列数)
- `sample_position`: 样本位置 (盒内, 行/列, 照片路径, 名称, 备注, 日期)
- `tag`: 标签 (名称, 颜色)
- `sample_tag`: 样本-标签关联 (多对多)

---

## 核心交互流程

```
首页(库) → 点击设备 → 设备详情(层列表)
  → 点击层 → 层详情(冻存盒网格)
    → 点击盒子 → 盒子网格(N×M)
      → 点击空格 → 拍照 → 照片压缩保存
      → 点击已拍照格子 → 编辑样本(名称/日期/备注/标签)
      → 长按格子 → 删除样本+照片

标签页 → 标签管理(增删改+颜色) → 标签详情(该标签下所有样本)

设置页 → 主题模式(弹窗选择) / 导出(CSV/PDF/MD) / 数据库导入导出(.zip)
```

## 照片管理
- 命名: `{boxId}_{row}_{col}.jpg` (避免跨盒子冲突)
- 压缩: 最大 1080px, JPEG 质量 80%
- 删除联动: 删除样本时同时删除物理照片文件

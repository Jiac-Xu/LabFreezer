# LabFreezer 功能·设计·数据库总结（Windows 移植参考）

> 基于 Android 版 v1.2.4（Room DB v3）源代码分析  
> 分析日期：2026-06-24

---

## 一、概述

LabFreezer（冰盒）是一个实验室冻存样本管理工具，帮助科研人员记录和管理**冻存设备 → 层 → 冻存盒 → 样本位置**的四级层级结构。支持拍照记录、OCR 文字识别、标签分类、搜索、多格式导出（CSV/MD/PDF）和数据库备份恢复。

### 核心使用场景

1. **冷库前快速采集**：在冷库前点击空格→拍照→保存，快速记录样本位置和外观
2. **办公室后补充信息**：回到实验室后点击格子补充名称、日期、备注等信息
3. **全局搜索**：快速查找样本并定位到具体位置
4. **导出归档**：将样本清单导出为 CSV/Markdown/PDF

---

## 二、功能清单

### 2.1 核心层级导航

| 页面 | 功能 | 说明 |
|------|------|------|
| **设备列表** | 首页，展示所有存储设备 | 按设备类型分组（可折叠），最近查看盒子横向滚动，FAB 添加设备 |
| **设备详情** | 展示设备下的所有层 | FAB 添加层，多选批量删除/移动到其他设备 |
| **层详情** | 展示层下的所有冻存盒 | 2列网格布局，FAB 添加盒子，多选批量删除/移动到其他层 |
| **盒子网格** | 核心页面，N×M 网格 | 每个格子独立状态，点击空格→相机拍照，点击已有样本→编辑 |
| **样本编辑** | 编辑样本详情 | 照片预览/重拍，名称/日期/备注，标签选择，左右滑动切换相邻样本 |

### 2.2 辅助功能

| 功能 | 说明 |
|------|------|
| **全局搜索** | 实时搜索（300ms 防抖），按设备/层/盒/样本分类结果，标签筛选 |
| **标签管理** | 16色标签 CRUD，标签下样本列表 |
| **设备类型管理** | 自定义设备类型预设（-80°C冰箱、液氮罐等） |
| **移动样本** | 层级浏览选择目标位置（设备→层→盒→格子） |
| **启动页设置** | 可选打开 App 时直接跳转到指定设备/层/盒 |
| **底部栏编辑** | 拖拽排序/显隐底部 4 个 Tab |

### 2.3 设置与工具

| 功能 | 说明 |
|------|------|
| **主题切换** | 浅色/深色/跟随系统 |
| **OCR 设置** | 启用/禁用拍照后自动 OCR，批量处理未命名样本 |
| **数据导出** | CSV / Markdown（含图片ZIP）/ PDF |
| **数据导入** | CSV / Markdown（含图片ZIP） |
| **数据库备份** | 完整备份（.db + 照片打包 ZIP），支持恢复 |
| **图片清理** | 按设备→层→盒浏览所有照片，批量删除 |
| **PGYER 更新检查** | OTA 版本更新 |

---

## 三、数据库设计

### 3.1 ER 关系

```
DeviceType (设备类型预设)
     │
StorageDevice (存储设备) 1 ──< N StorageLayer (层) 1 ──< N StorageBox (冻存盒) 1 ──< N SamplePosition (样本位置)
                                                                                          │
                                                                                          │ (M:N)
                                                                                     Tag ──< SampleTag >── SamplePosition
```

### 3.2 建表 SQL（完整 7 表）

```sql
-- 1. 设备类型预设表
CREATE TABLE device_type (
    id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name        TEXT NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

-- 2. 存储设备表
CREATE TABLE storage_device (
    id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name        TEXT NOT NULL,
    type        TEXT NOT NULL DEFAULT 'FREEZER_M80',  -- FREEZER_M80 | LIQUID_NITROGEN
    note        TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  INTEGER NOT NULL,       -- epoch millis
    updated_at  INTEGER NOT NULL
);
CREATE INDEX index_storage_device_name ON storage_device(name);

-- 3. 存储层表
CREATE TABLE storage_layer (
    id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    device_id   INTEGER NOT NULL REFERENCES storage_device(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    note        TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL
);
CREATE INDEX index_storage_layer_device_id ON storage_layer(device_id);

-- 4. 冻存盒表
CREATE TABLE storage_box (
    id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    layer_id    INTEGER NOT NULL REFERENCES storage_layer(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    rows        INTEGER NOT NULL,        -- 行数（每个盒子独立配置）
    cols        INTEGER NOT NULL,        -- 列数
    note        TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL
);
CREATE INDEX index_storage_box_layer_id ON storage_box(layer_id);

-- 5. 样本位置表（核心表）
CREATE TABLE sample_position (
    id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    box_id      INTEGER NOT NULL REFERENCES storage_box(id) ON DELETE CASCADE,
    row         INTEGER NOT NULL,        -- 0-based
    col         INTEGER NOT NULL,        -- 0-based
    photo_path  TEXT,
    name        TEXT,
    note        TEXT,
    date        TEXT,                     -- yyyy-MM-dd
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL,
    UNIQUE(box_id, row, col)             -- 一个位置只能一个样本
);
CREATE INDEX index_sample_position_box_id ON sample_position(box_id);
CREATE INDEX index_sample_position_name ON sample_position(name);
CREATE UNIQUE INDEX index_sample_position_box_id_row_col ON sample_position(box_id, row, col);

-- 6. 标签表
CREATE TABLE tag (
    id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name        TEXT NOT NULL,
    color       TEXT NOT NULL DEFAULT '#1565C0',
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL
);

-- 7. 样本-标签关联表（多对多）
CREATE TABLE sample_tag (
    id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    sample_id   INTEGER NOT NULL REFERENCES sample_position(id) ON DELETE CASCADE,
    tag_id      INTEGER NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    UNIQUE(sample_id, tag_id)
);
CREATE INDEX index_sample_tag_sample_id ON sample_tag(sample_id);
CREATE INDEX index_sample_tag_tag_id ON sample_tag(tag_id);
```

### 3.3 关键 SQL 查询

**带完整路径的搜索（核心查询）：**
```sql
SELECT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, 
       sp.name, sp.note, sp.date, sp.photo_path AS photoPath,
       sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName
FROM sample_position sp
INNER JOIN storage_box sb ON sp.box_id = sb.id
INNER JOIN storage_layer sl ON sb.layer_id = sl.id
INNER JOIN storage_device sd ON sl.device_id = sd.id
WHERE sp.name LIKE '%' || :query || '%' 
   OR sp.note LIKE '%' || :query || '%'
ORDER BY sp.name ASC
```

**带标签筛选的搜索：**
```sql
SELECT DISTINCT sp.id AS sampleId, ...
FROM sample_position sp
INNER JOIN storage_box sb ON sp.box_id = sb.id
INNER JOIN storage_layer sl ON sb.layer_id = sl.id
INNER JOIN storage_device sd ON sl.device_id = sd.id
LEFT JOIN sample_tag st ON sp.id = st.sample_id
WHERE (sp.name LIKE '%' || :query || '%' OR sp.note LIKE '%' || :query || '%')
  AND (:tagCount = 0 OR st.tag_id IN (:tagIds))
ORDER BY sp.name ASC
```

### 3.4 数据库版本迁移

- **当前版本**：v3
- **v1→v2**：无 Migration（fallbackToDestructiveMigration）
- **v2→v3**：添加 `device_type` 表
  ```sql
  CREATE TABLE IF NOT EXISTS `device_type` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `sort_order` INTEGER NOT NULL DEFAULT 0
  )
  ```
- **降级策略**：`fallbackToDestructiveMigration()` — 不匹配时销毁重建

### 3.5 移植建议（Windows）

- **推荐数据库**：SQLite（保持兼容），可使用 Microsoft.Data.Sqlite（.NET）或 sql.js（Electron）
- **无需 TypeConverter**：所有列均为原始类型（Long/Int/String）
- **CASCADE DELETE**：SQLite 默认不启用外键约束，需执行 `PRAGMA foreign_keys = ON`
- **时间字段**：`created_at`/`updated_at` 使用 Unix epoch millis（Int64）

---

## 四、界面设计规范

### 4.1 配色方案

**主色调：** iOS 蓝 `#007AFF`

| Token | 浅色 | 深色 | 用途 |
|-------|------|------|------|
| `primary` | `#007AFF` | `#A1CAFF` | 主按钮、FAB、链接 |
| `onPrimary` | `#FFFFFF` | `#00315C` | primary 上文字 |
| `primaryContainer` | `#D6EAFF` | `#004982` | 选中态卡片背景 |
| `background` | `#F8F9FA` | `#111318` | 全局背景 |
| `surface` | `#F8F9FA` | `#111318` | 表面色 |
| `surfaceContainerLow` | `#FFFFFF` | `#1A1C21` | 列表卡片背景 |
| `surfaceVariant` | `#EDF0F5` | `#43474E` | 网格空格背景 |
| `error` | `#BA1A1A` | `#FFB4AB` | 删除/错误 |
| `outline` | `#73777F` | `#8D9199` | 轮廓、未选中图标 |

### 4.2 圆角系统（5级）

| 级别 | 数值 | 使用场景 |
|------|------|---------|
| `extraSmall` | 8dp | 照片缩略图 |
| `small` | 12dp | 搜索栏、设置卡片、标签选择卡片 |
| `medium` | 16dp | 列表卡片（设备/层/盒/标签） |
| `large` | 20dp | Tag Chip |
| `extraLarge` | 28dp | 底部导航栏浮起 |

特殊：网格单元格 4dp，FAB/复选框完全圆形。

### 4.3 间距系统

| 场景 | 数值 |
|------|------|
| 列表页水平 padding | 16dp |
| 列表 item 垂直间距 | 12dp |
| 卡片内 padding | 16dp |
| 网格单元格间距 | 2dp |
| 网格容器 padding | 4dp |
| Tag chip 间距 | 8dp |
| 设置区块间距 | 16dp |
| 底部导航外间距 | start=16, end=16, bottom=16 |

### 4.4 阴影与抬升

| 组件 | elevation |
|------|----------|
| 列表卡片 | 1dp |
| 设置卡片 | 0dp（平面） |
| 照片卡片 | 4dp |
| 底部导航 | tonalElevation=6dp, shadowElevation=8dp |
| 网格单元格 | 0dp |

### 4.5 排版

- 使用系统默认字体（不自定义）
- 卡片标题：`titleMedium` + Medium 字重
- 辅助文字：`bodyMedium` / `bodySmall`
- Tab 标签：`labelSmall`（选中 SemiBold / 未选中 Normal）
- 网格格子文字：10-11sp（Bold 或 Normal 根据是否占用）

---

## 五、架构设计

### 5.1 整体架构

```
UI Layer (Compose Screens)
    ↕  collectAsStateWithLifecycle()
ViewModel Layer (13个 @HiltViewModel)
    ↕  StateFlow / suspend
Repository Layer (7个 @Singleton)
    ↕  DAO Flow / suspend
Data Layer (Room DAO + SQLite + 文件系统)
```

### 5.2 数据流模式

每个 ViewModel 采用以下模式：

```kotlin
// ViewModel 内
private val _state = MutableStateFlow<UiState>(initial)
val state: StateFlow<UiState> = _state.asStateFlow()

fun doSomething() {
    viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        try {
            val result = repository.someOperation()
            _state.value = _state.value.copy(data = result)
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.message)
        }
    }
}

// Screen 内
val state by viewModel.state.collectAsStateWithLifecycle()
```

### 5.3 ViewModel 清单（13个）

| ViewModel | 数据源 | 核心状态 | 说明 |
|-----------|--------|----------|------|
| `DeviceListViewModel` | DeviceRepo + LayerRepo + RecentRepo | devices, groupedDevices, selectedIds | 首页设备列表，分组，最近查看 |
| `DeviceDetailViewModel` | DeviceRepo + LayerRepo | layers, device, selectedIds | 设备下层的 CRUD |
| `LayerDetailViewModel` | LayerRepo + BoxRepo | boxes, layer, selectedIds | 层下盒子的 CRUD |
| `BoxGridViewModel` | BoxRepo + SampleRepo + PhotoMgr + OcrEngine | gridCells, zoomLevel | 核心网格，相机触发，OCR |
| `SampleEditViewModel` | SampleRepo + TagRepo + PhotoMgr + OcrEngine | editState, sampleTags | 样本编辑，相邻样本导航 |
| `SearchViewModel` | 所有 Repository | query, results, selectedTags | 300ms 防抖搜索 |
| `MoveBrowserViewModel` | 所有 Repository | breadcrumbs, currentLevel, items | 移动目标层级浏览 |
| `TagManageViewModel` | TagRepo + DeviceTypeRepo | tags, deviceTypes | 标签和设备类型 CRUD |
| `TagDetailViewModel` | TagRepo | tag, samples | 标签下样本列表 |
| `ExportViewModel` | ExportEngine | 导出文件 URI | CSV/MD/PDF 导出 |
| `OcrSettingsViewModel` | OcrPreferences + OcrEngine | settings, models, batchProgress | OCR 开关/批量处理 |
| `ImageCleanupViewModel` | SampleRepo | photoGroups, selectedPhotos | 照片浏览/清理 |
| `StartPagePickerViewModel` | 所有 Repository | 透传 | 启动页选择 |

### 5.4 Repository 清单（7个）

| Repository | DAO | 说明 |
|------------|-----|------|
| `StorageDeviceRepository` | StorageDeviceDao + StorageLayerDao | 提供 `DeviceWithCount(device, layerCount)` |
| `StorageLayerRepository` | StorageLayerDao + StorageBoxDao | 提供 `LayerWithCount(layer, boxCount)` |
| `StorageBoxRepository` | StorageBoxDao | 纯委托 |
| `SamplePositionRepository` | SamplePositionDao | 纯委托，封装搜索 |
| `TagRepository` | TagDao + SampleTagDao | 管理 M:N 关系 |
| `DeviceTypeRepository` | DeviceTypeDao | 纯委托 |
| `RecentlyViewedRepository` | SharedPreferences（JSON） | 最近查看的盒子列表（最多20个） |

---

## 六、网格核心算法

### 6.1 位置编号

```
行 → 字母（A=0, B=1, C=2, ...）
列 → 数字（1=0, 2=1, 3=2, ...）

A1 = (row=0, col=0)
A2 = (row=0, col=1)
B1 = (row=1, col=0)

转换函数：
positionToLabel(row, col) = "${'A' + row}${col + 1}"
labelToPosition(label)    = (label[0] - 'A', label.drop(1).toInt() - 1)
```

### 6.2 网格状态判断

```kotlin
enum class GridCellStatus {
    EMPTY,        // 无数据
    PHOTO_ONLY,   // 有照片（photoPath != null），无文字描述
    COMPLETE      // 有照片且有名称
}
```

- 空格背景色：`surfaceVariant`
- 拍照态：显示照片缩略图，右下角相机图标
- 完整态：显示照片缩略图，右下角文字标签

### 6.3 网格交互

- **点击空格** → 创建 SamplePosition → 启动系统相机 → 保存照片 → 刷新网格
- **点击已有样本** → 导航到 SampleEditScreen
- **长按** → 多选模式（勾选框 + 底部操作栏）
- **缩放滑块** → 调整网格单元格大小

---

## 七、照片管理

### 7.1 存储方案

- **路径**：`AppData/photos/{boxId}_{row}_{col}.jpg`
- **压缩规则**：最长边 ≤ 1080px，JPEG quality 80%，根据 EXIF 旋转
- **命名**：`{boxId}_{row}_{col}.jpg`（或 `{sampleId}.jpg` 旧版）

### 7.2 管理类：PhotoManager

| 方法 | 说明 |
|------|------|
| `createPhotoUri(context)` | 创建临时 URI 供系统相机写入 |
| `compressAndSavePhoto(context, uri)` | 压缩保存（BitmapFactory + Matrix 旋转） |
| `deletePhoto(context, photoPath)` | 删除照片文件 |
| `getPhotoFile(context, photoPath)` | 获取照片文件 |

### 7.3 相机流程

1. 点击空格 → ViewModel 创建空的 SamplePosition → 获取 ID
2. PhotoManager 创建拍照 URI（FileProvider）
3. 启动 `ActivityResultContracts.TakePicture()`
4. 拍照返回 → PhotoManager 压缩保存
5. 更新 SamplePosition.photoPath
6. 若 OCR 启用 → OcrEngine 自动识别 → 填写 name/date

---

## 八、OCR 识别

### 8.1 技术方案

- **引擎**：PaddleOCR v4（ch_PP-OCRv4 模型）
- **模型文件**（打包在 assets/models/ch_PP-OCRv4/）：
  - `det.nb` — 文字检测模型
  - `rec.nb` — 文字识别模型
  - `cls.nb` — 文字分类模型
- **集成方式**：`com.github.equationl/paddleocr4android` v1.2.9

### 8.2 识别流程

```
拍照完成
  → Bitmap 传入 OcrEngine.recognize(bitmap)
  → PaddleOCR 识别文字
  → OcrEngine.parseResult(text)
    → 正则提取样本名称（第一行文字）
    → 正则提取日期（yyyy-MM-dd / yyyyMMdd / yyyy年M月d日 等格式）
  → 自动填入 SampleEditScreen 的 name 和 date 字段
```

### 8.3 OCR 设置

- 用户可开关「拍照后自动 OCR」
- 可执行批量 OCR：对所有「有照片但无名称」的样本执行识别

---

## 九、导出/导入

### 9.1 导出引擎（ExportEngine）

| 格式 | 实现 | 内容 |
|------|------|------|
| **CSV** | 文本写入（带 BOM 兼容 Excel） | 位置、名称、日期、备注、完整路径 |
| **Markdown** | 文本写入 + ZIP打包图片 | 表格展示，同名 .jpg 图片附在 ZIP 内 |
| **PDF** | Android `PdfDocument` + Canvas 绘制 | 3列布局表格，标题 + 表头 + 数据行 |

### 9.2 导出范围

全部 / 按设备 / 按层 / 按盒 / 按标签

### 9.3 数据库备份/恢复

- **备份格式**：ZIP 压缩包
- **备份内容**：`labfreezer.db` + `labfreezer.db-wal` + `labfreezer.db-shm` + `photos/` 目录
- **恢复**：解压 ZIP → 替换数据库文件 → 替换照片目录

---

## 十、导航设计

### 10.1 路由结构（17条）

```
MainTabs (Tab容器)
├── Tab 0: DeviceList   (设备列表)
├── Tab 1: TagManage    (标签管理)
├── Tab 2: Search       (搜索)
└── Tab 3: Settings     (设置)

层级路由（推栈）：
DeviceList → DeviceDetail/{deviceId}
           → LayerDetail/{layerId}
                        → BoxGrid/{boxId}
                                   → SampleEdit/{sampleId}
                                   → SampleCreate/{boxId}/{row}/{col}

工具路由（推栈）：
Search → BoxGrid/{boxId}
Settings → About / ImageCleanup / OcrSettings / BottomBarEdit
         → StartPagePicker
         → DeviceTypeManage
TagManage → TagDetail/{tagId}
BoxGrid → MoveBrowser (选择器模式)
```

### 10.2 底部导航（4个Tab，可配置）

| # | 默认标题 | 图标 | 可隐藏 | 说明 |
|---|---------|------|--------|------|
| 0 | 库 | `home` / `kitchen` | 否 | 设备列表首页 |
| 1 | 标签 | `label` | 是 | 标签管理 |
| 2 | 搜索 | `search` | 是 | 全局搜索 |
| 3 | 设置 | `settings` | 否 | 设置页 |

### 10.3 导航动画

- Tab 切换：`AnimatedContent` 水平滑动 (slideInHorizontally/slideOutHorizontally)
- 层级导航：`NavHost` 默认动画
- 底部导航浮起浮落：仅在 MainTabs 路由时显示，其他路由隐藏

---

## 十一、UI 模型数据类（非数据库实体）

以下数据类用于 UI 层的状态管理，Windows 版需对应实现：

| 类 | 字段 | 说明 |
|----|------|------|
| `GridCell` | row, col, label, status, sampleId, photoPath, sampleName | 网格单个单元格 |
| `SampleWithPath` | sampleId, boxId, row, col, name, date, note, photoPath, deviceName, layerName, boxName | 带完整路径的样本（DAO JOIN 结果） |
| `DeviceWithCount` | device, layerCount | 设备+层数统计 |
| `LayerWithCount` | layer, boxCount | 层+盒数统计 |
| `RecentBox` | id, name, deviceName, layerName | 最近查看的盒子 |
| `BreadcrumbItem` | label, level, id | 移动浏览器的面包屑 |
| `BottomTab` | id, label, icon, enabled | 底部 Tab 配置 |
| `ThemeMode` | LIGHT / DARK / SYSTEM | 主题模式枚举 |
| `GridCellStatus` | EMPTY / PHOTO_ONLY / COMPLETE | 单元格状态枚举 |

---

## 十二、移植要点总结

### 12.1 需要重新实现的 Android 特有功能

| 功能 | Android 实现 | Windows 替代方案 |
|------|-------------|-----------------|
| Room 数据库 | `androidx.room` | SQLite（任意语言） |
| ViewModel + StateFlow | `androidx.lifecycle` | MVVM 模式的对应实现 |
| Compose UI | `androidx.compose` | WPF / WinUI 3 / MAUI / Electron |
| Navigation | `navigation-compose` | 对应框架的导航方案 |
| Hilt DI | `dagger-hilt` | 依赖注入容器或不使用 |
| 系统相机 Intent | `ActivityResultContracts.TakePicture` | Windows.Media.Capture / OpenCV |
| 照片压缩旋转 | `BitmapFactory` + EXIF | SkiaSharp / System.Drawing |
| FileProvider | `androidx.core.content.FileProvider` | 直接文件路径 |
| SharedPreferences | 键值存储 | JSON 文件 / 注册表 / SQLite 配置表 |
| PDF 导出 | `android.graphics.pdf.PdfDocument` | iTextSharp / QuestPDF |
| 内容分享 | `Intent.ACTION_SEND` | 系统打开/保存对话框 |

### 12.2 可以直接迁移的

- **数据库 schema**（完全相同的 SQLite SQL）
- **业务逻辑**（Repository 层的 CRUD 逻辑）
- **导航层次**（Device → Layer → Box → Sample 四级结构）
- **导出格式**（CSV / Markdown 格式定义）
- **网格坐标算法**（positionToLabel / labelToPosition）
- **OCR 集成**（如果用同一套 PaddleOCR 模型，需要找 Windows SDK）

### 12.3 文件结构建议（Windows）

```
LabFreezer/
├── LabFreezer.sln
├── src/
│   ├── LabFreezer.Core/          # 业务逻辑（可跨平台）
│   │   ├── Models/               # 实体模型（对应 Room Entity）
│   │   ├── Repositories/         # 仓储（对应 Repository）
│   │   ├── Services/             # 服务（OCR、导出、照片管理）
│   │   └── Database/
│   │       └── LabFreezerContext  # DbContext / 数据库上下文
│   ├── LabFreezer.Data/          # 数据访问
│   │   ├── Entities/             # SQLite 实体
│   │   ├── Migrations/           # 数据库迁移
│   │   └── SQL/                  # 建表 SQL（可直接用 3.2 节）
│   └── LabFreezer.App/           # UI 层
│       ├── ViewModels/           # 对应 ViewModel（13个）
│       ├── Views/                # 对应 Screen（17个）
│       ├── Converters/           # 值转换器（状态→颜色等）
│       └── Navigation/           # 导航路由
└── docs/
    └── database.md               # 本文档的数据库部分
```

---

## 十三、验证方案

Windows 版本开发完成后，验证以下核心路径：

1. **层级创建**：添加设备 → 添加层 → 添加盒子（配置 rows×cols）→ 看到网格
2. **拍照记录**：点击空格 → 拍照 → 照片显示在网格中
3. **信息编辑**：点击格子 → 编辑名称/日期/备注/标签 → 保存 → 格子状态更新
4. **搜索定位**：搜索样本名称 → 显示完整路径 → 点击定位
5. **数据导出**：导出 CSV → 用 Excel 打开正确；导出 Markdown → 预览正确
6. **标签管理**：创建标签 → 分配给样本 → 按标签筛选搜索
7. **数据库兼容**：直接加载 Android 版导出的 .db 文件，数据完整

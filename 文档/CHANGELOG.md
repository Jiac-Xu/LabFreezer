# 更新日志

## v1.3.2 (2026-08-28)

### 交互与体验优化
- **BoxGrid 缩放条吸附点与档位快速切换** — 缩放滑块新增吸附点机制，支持最小值（3 列放大）、详情展开档位（5 列）及最大值（全局概览）的磁吸吸附；支持点击吸附点一键平滑过渡切换视图。
- **LiquidSlider 组件增强** — 引入 `snapPoints` 机制与轨道刻度指示圆点，支持平滑弹性阻尼过渡与吸附交互。

### 液态玻璃与 UI 统一重构
- **LiquidButton 精简与增强** — 移除拖拽位移阻尼，保留原位弹性按压缩放与跟手光斑；新增 `enabled` 禁用态支持
- **页面主体操作按钮升级 LiquidButton** — 样本编辑照片操作按钮、关于页面检查更新按钮、图片清理扫描/清理按钮、批量 OCR 按钮全面升级为 `LiquidButton`
- **SpeedDial 文字按钮适配 LiquidGlass** — 展开文字标签升级为 `LiquidButton`，与右侧 SmallFAB 共享统一的模糊、磨砂光影及按压微交互
- **统一模糊接口与通用组件提取** — 新增通用 `LiquidButton` 组件，`LiquidSlider` 统一接入 `LocalGlassBackdrop` 自动采样机制
- **底栏“设置”入口防误关与强制恢复** — 底栏编辑中“设置”标签锁定不可关闭；应用启动与升级时若检测到未开启设置则自动强制加回底栏末尾，确保核心设置入口永不丢失
- **GlassScaffold 重构** — `GlassFabScaffold` 重命名为通用 `GlassScaffold`，全屏统一提供背景采样层，`BoxGridScreen` 针对网格层与缩放条解耦避免递归采样闪退

## v1.3.1 (2026-08-08)

### 系统适配
- **适配 Android 17 (API 37)** — compileSdk、targetSdk 升级至 37，版本号升级至 **1.3.1**（versionCode = 14）
- predictive back 已开启，符合 Android 17 对返回手势的强制要求

### 构建与工程
- 修复 `gradlew` 脚本缺失 APP_HOME 解析段落导致的启动失败
- 显式指定 build-tools 36.0.0，避免 AGP 默认版本触发联网下载

## v1.3.0 (2026-08-01)

### 视觉树架构重构
- **引入 `TreeTransformer` 统一视觉树处理** — 移除 `SampleWithPath` 扩展属性，所有页面统一走 TreeTransformer 构建 `VisualTree`
- **数据层统一处理 hidden 过滤** — `__hidden__` 占位节点改为单例复用模式，并支持默认设备组 i18n
- 搜索/导出/导入全面适配 hidden：导出不再过滤 `__hidden__`，导入查找包含 hidden 条目
- 修复多项视觉树适配问题（TagDetail 过滤 `__hidden__` 层名显示、DeviceList 独立盒子 DAO 过滤等）

### 设备列表与盒子
- **DeviceList 支持直接盒子** — 显示设备下直接挂载的盒子，新增独立盒子区块（hidden device 下的盒子）
- **统一 BoxDialog** — DeviceList 创建盒子改为独立盒子，BoxGrid 移除 FAB，简化创建流程
- SpeedDialFAB 单操作自动降级为普通 FAB，并修复展开闪烁（Spacer 移入 AnimatedVisibility 内部）
- DeviceListScreen FAB 底部导航栏遮挡修复，BoxGridScreen 新增 FAB
- DeviceListScreen 独立盒子长按选中 TopAppBar 适配
- 最近浏览卡片高度统一，DeviceDetail 移动目标按类型适配

### 移动与编辑修复
- 修复第一层/第二层盒子移动至第一层、第一层长按移动按钮及编辑弹窗位置选择功能
- 修复层详情页编辑/删除盒子时构造假实体导致备注丢失的问题，改为按 ID 加载真实实体
- 恢复盒子/层级卡片备注副标题显示（VisibleTreeNode 携带 note），修复设备详情编辑层丢失备注
- StorageBoxEntity 构造缺少 rows/cols 参数，改为 ViewModel 异步获取实体

### 样本编辑
- **标签即时保存** — 样本编辑页点击标签立即保存到数据库，无需手动保存
- 照片操作按钮配色调整（最终确定为 primary 图标文字、禁用灰色的 TextButton 风格）

### 系统适配
- **开启 predictive back 支持** — 保留 Android 16 侧滑预览效果

### i18n
- 补充 SpeedDialFAB 按钮在英文与繁体中文下的多语言资源
- 同步 `move_confirm_to_device` 到 values-en 和 values-zh-rTW

### 技术调整
- 版本号升级至 **1.3.0**（versionCode = 13）

## v1.2.9 (2026-07-28)

### 个性化设置
- **新增个性化页面** — 重构设置页结构，新增「偏好」分组，将主题/启动页/底栏编辑移入个性化页面
- 新增 样本录入方式切换（拍照/相册/手动录入），支持单次临时切换
- 新增 样本编辑页自动保存开关
- 新增 盒子页缩放控制栏开关
- 新增 历史搜索记录开关
- 统一个性化页面 Switch 组件配色

### 样本浏览上下文
- **新增浏览上下文** — 样本编辑页顶部显示当前位置（设备 → 层 → 盒），支持在搜索/标签/盒子页间无缝跳转
- 样本切换导航（上/下一个）根据上下文自动定位

### 录入方式增强
- 盒子页支持三种录入方式：拍照（默认）、相册选择、手动输入文字
- 相册模式调用 Android Photo Picker，支持批量选择多张照片
- 手动模式点击空格直接跳转样本编辑页
- 样本编辑页按钮布局优化为 2×2 网格（拍照/相册/识别文字/删除图片）

### 界面优化
- 设置项「检查更新」更名为「关于」
- 统一全应用术语：「样品」改为「样本」

### 技术调整
- 版本号升级至 **1.2.9**（versionCode = 12）
- 提取 `InputMode` 为独立枚举及 `InputModePreferences` Hilt Bean

## v1.2.8 (2026-07-22)

### 搜索增强
- **新增实验命名缩写识别** — 搜索时自动识别样本名称末尾的实验缩写（WT/NC/OE/SH/KO/KD/MUT/CTRL），生成带分隔符的变体，例如 `BTKSH` 可匹配 `BTK SH`、`BTK-SH`、`BTK_SH`，`HL60WT` 可匹配 `HL60 WT`、`HL60-WT` 等
- **搜索功能重构** — 引入 `SearchNormalizer` + `SearchQueryParser`，实现名称模糊搜索（忽略分隔符差异）和日期智能搜索（自动识别 yyyyMMdd/yyMMdd/MMdd 等多种格式）
- 修复搜索提示文字、空状态文案，支持备注字段搜索

### 照片与图片处理
- **新增样本照片旋转功能** — 样本编辑页图片支持 90° 旋转，旋转后自动刷新显示
- 修复旋转后图片缓存未刷新问题（使用文件时间戳强制 Coil 重载）
- 禁用 `SampleEditScreen` 的 Coil 缓存，避免显示陈旧图片
- 新增旋转操作 Toast 的多语言资源

### 标签管理
- 修复标签管理页样本计数在标签变更后未立即更新的问题

### 导入功能
- **导入入口自动识别类型** — 使用 `ZipInspector` 自动识别导入的 .zip 文件类型
- 升级 `versionCode` 9 → 10
- 将 `.gitignore` 中 `公平内存*.md` 改为 `**/公平内存*.md` 防止误提交

### 公平运行内存适配
- 浅度接入公平运行内存管理，提升设备兼容性

### 关于页
- 修复更新描述中 `\n` 换行符未渲染的问题
- 更新至最新版本时显示"打开网站"按钮

### 技术调整
- 版本号升级至 **1.2.8**（versionCode = 11）
- 构建工具链：Gradle 8.12 / AGP 8.9.1 / Kotlin 2.1.20 / Room 2.6.1 / Hilt 2.55
![图片](https://mmecoa.qpic.cn/sz_mmecoa_jpg/V3B0hKlKW8r3V0CEVwZUFjnApeRLDtQSYLrI7Keic6ZdM4nUXHZB9NicQrX8OGPgCnWw5bRqkAU8fhjd0N7ibibz7o0L0Wntt81tIzmbmAs085U/640?wx_fmt=jpeg&from=appmsg&tp=webp&wxfrom=10005&wx_lazy=1#imgIndex=0)

尊敬的开发者，您好！

顺应 AI 时代的发展趋势，系统底部导航条特性应运而生。该特性具备三大价值：智慧化功能入口、便捷的交互方式、提升视觉简洁性。金标联盟理事长成员（荣耀 | OPPO | vivo | 小米）现邀请您了解并参与"谷歌Android导航条适配"共建，烦请帮忙评估是否有意愿，以及具体的适配计划。感谢！

**1**

**我们共同面对的挑战**

导航条几乎覆盖所有 APP 界面包括高频交互场景（如直播页、评论页、转发页、搜索页、菜单页、对话框页等）。随着越来越多终端设备默认开启系统导航条，当前导航条 UI 背景色与 APP 界面存在的反差与割裂感，已影响海量，并容易让用户产生 APP 与系统界面不够精致的负面印象。

谷歌在 Android 系统的持续演进中，也已着重强调边缘到边缘（Edge-to-Edge）显示与窗口 insets 适配的规范，为整个生态的健康发展设定了明确方向。（参阅谷歌原文：https://developer.android.com/develop/ui/views/layout/edge-to-edge?hl=zh-cn）。

**2**

**解决方案:**

**采用谷歌 Android**

**统一导航条适配规范**

为应对上述挑战，金标联盟牵头推进采用谷歌"统一导航条适配规范"，根据安卓版本分为两种情况：

2.1. 针对安卓版本号大于等于15，谷歌Android 统一导航条适配规范如下：

导航条沉浸式适配指导文档：https://developer.android.com/codelabs/edge-to-edge?hl=zh-cn#5

通过 Jetpack Libraries 实现沉浸式的参考代码文档：https://developer.android.com/reference/androidx/activity/ComponentActivity#(androidx.activity.ComponentActivity).enableEdgeToEdge(androidx.activity.SystemBarStyle,androidx.activity.SystemBarStyle)

2.2. 针对安卓版本号小于15，谷歌Android 统一导航条适配规范如下：

第一步：应用界面布局到导航栏、状态栏

https://developer.android.com/reference/android/view/Window#setDecorFitsSystemWindows(boolean)

第二步：将导航栏、状态栏背景设置为透明

https://developer.android.com/reference/android/view/Window#setAttributes(android.view.WindowManager.LayoutParams)

https://developer.android.com/reference/android/view/Window#setStatusBarColor(int)

第三步：应用内容view避让导航栏、状态栏https://developer.android.com/reference/android/view/View#setPadding(int,%20int,%20int,%20int)

**3**

**适配时间与支持**

为保障适配工作有序推进，使适配成果尽早惠及用户，**请各位开发者务必在2026年10月31日前完成安卓导航条适配。**届时，针对还未适配的APP，金标联盟理事长成员（荣耀 | OPPO | vivo | 小米）四家将在应用市场进行打标处理，向用户进行风险提示等措施。

我们期待与您携手，共同提升应用品质，打造更精致的移动生态。感谢您的支持与贡献！
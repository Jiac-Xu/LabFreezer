# AppUpdateChecker



本仓库包含了 `PGYER` (蒲公英) 检查更新功能的代码片段，适用 `Android App` 、 `iOS App` 和 `uni-app`。

通过使用相应的代码片段, 可以在目标 `App` 中快速集成 `PGYER` (蒲公英) 检查更新功能。

### 实现细节



通过调用 `UpdateChecker` 的 `check` 方法, 间接调用 `PGYER` (蒲公英) 的后端 API。

具体参照: [PGYER API 2.0: 检测 App 是否有更新](https://www.pgyer.com/doc/view/api#appUpdate)

### 使用方式



1. 根据项目类型拷贝 `iOS` / `Android` / `uni-app` 目录下的文件 (目录内只有一个文件) 到项目相应位置。
2. 按照下方代码示例在业务层面需要调用检查更新的地方添加调用代码即可。

- Android 项目调用示例 (Java)

```
import <code_path>.UpdateChecker;
...
new UpdateChecker("<API_KEY>")
  .check(
    "<APP_KEY>",
    "<(可选)APP版本号>",
    <(可选)(Integer)使用蒲公英生成的自增 Build 版本号>,
    "<(可选)渠道 KEY>",
    new UpdateChecker.Callback() {
      @Override
      public void result(UpdateChecker.UpdateInfo updateInfo) {
      }

      @Override
      public void error(String message) {
      }
    }
  );
```



> Android 代码调用时, 如果无需传递可选参数，用 null 代替即可。
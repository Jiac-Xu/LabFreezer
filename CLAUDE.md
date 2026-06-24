Android Studio在D:\Program Files\AndroidStudio
adb在C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe
JAVA_HOME用Android Studio的JBR：D:\Program Files\AndroidStudio\jbr

每次修改完成后请用adb推送到手机上测试，同时git保存

除非主动要求，否则全部打release包（`./gradlew.bat assembleRelease`）

设计可以读取工作目录下的design.md

# 重要规则 - 请严格遵守

## 用户数据第一位
1. 任何涉及数据库、文件存储、SharedPreferences等用户数据的操作前，必须先adb pull备份到本地
2. 推送到手机前必须确认不会丢失数据（签名变更需要卸载前先备份数据库）
3. 除非用户明确要求，不得修改数据库结构（Room entity、DB版本号、Migration）
4. 需要变更数据库时，必须先提醒用户导出数据，获得确认后再操作

## 备份操作流程
```powershell
# 备份数据库
adb shell "run-as com.labfreezer cat /data/data/com.labfreezer/databases/labfreezer.db" > backup/labfreezer.db
adb shell "run-as com.labfreezer cat /data/data/com.labfreezer/databases/labfreezer.db-wal" > backup/labfreezer.db-wal
adb shell "run-as com.labfreezer cat /data/data/com.labfreezer/databases/labfreezer.db-shm" > backup/labfreezer.db-shm
```

## 数据库变更检查清单
- [ ] 是否真的需要改数据库？能否避免？
- [ ] 已通知用户并获确认？
- [ ] 已备份当前数据？
- [ ] 改完后做了充分测试？

## Windows 分支规则（严格遵守）
- Windows 版所有改动**只允许提交到 `windows-experiment` 分支**
- **不允许在 `master` 分支上提交任何 Windows 相关文件**
- 工作流程：`git checkout windows-experiment` → 改代码 → 提交 → 测试
- `master` 分支只包含 Android 版代码
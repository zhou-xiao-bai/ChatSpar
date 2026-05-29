# 自用 APK 安装说明

日期：2026-05-27

## APK 产物

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`app/build/outputs/apk/release/app-release.apk`

当前 release 包使用本机 debug key 签名，仅用于自用安装测试；对外分发前需要替换为独立 release keystore。

## 重新生成 APK

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

## 安装到设备

先确认设备已连接：

```bash
adb devices
```

首次安装 release 包：

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

覆盖安装并保留数据：

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

卸载并清空本地数据：

```bash
adb uninstall com.chatspar.app
```

安装后可用以下命令启动：

```bash
adb shell am start -n com.chatspar.app/.MainActivity
```

## 使用检查

1. 打开 App 后进入设置页，配置 API 地址、API Key 和模型。
2. 回到练习页，选择一个场景开始练习。
3. 完成对话后结束练习并查看复盘。
4. 如需确认卸载重装会清空本地数据，先执行 `adb uninstall com.chatspar.app`，再重新安装 APK。

说明：release 包不包含 debug-only 的本地 cleartext 网络配置。真实设备回归建议使用 HTTPS API 地址。

# T-404 全流程回归测试记录

日期：2026-05-27

环境：

- 设备：Android Emulator `emulator-5554`
- 包名：`com.chatspar.app`
- APK：`app/build/outputs/apk/debug/app-debug.apk`
- API 配置：`http://localhost:18080`，模型 `local-regression`
- 说明：本轮未提供真实 API 凭证，手动场景回归使用本地 OpenAI-compatible 回归服务替代真实 API。

## 自动化检查

- `./gradlew testDebugUnitTest --tests com.chatspar.app.data.practice.PracticeRepositoryTest`：通过
- `./gradlew test`：通过
- `./gradlew assembleDebug`：通过

## 手动回归结果

- 打开 App、进入练习 Tab、查看场景列表：通过。
- 场景筛选与场景详情：通过。
- 场景 `朋友聚餐里有不熟的人`：
  - 开始练习、完成 6 轮对话、结束并复盘：通过。
  - 复盘结果页显示总体评价、维度评分、问题、推荐表达：通过。
  - 收藏推荐表达后按钮显示 `已收藏`：通过。
  - 进入复盘历史列表和历史详情：通过。
  - 进入表达库复制表达，显示 `已复制`：通过。
- 设置页测试连接：
  - 正常本地服务：显示 `连接状态：连接成功` 和 `连接测试成功`。
  - 错误 API Key 模拟：显示 `AI 请求失败：HTTP 401，invalid api key`。
  - 服务不可达模拟：显示 `网络请求失败，请检查 API 地址和网络连接`。
- 清空数据：
  - 清空练习记录后，`practice_sessions/messages/reviews` 均为 0，表达保留；表达库来源显示 `来源已删除`：通过。
  - 清空表达库后，`phrases` 为 0，表达库空态显示 `还没有收藏表达`：通过。
  - 历史记录空态显示 `还没有练习记录`：通过。
- 场景 `和朋友的朋友第一次见面`：
  - 使用本地兼容服务完成 6 轮对话、结束并复盘：通过。
  - 复盘结果页正常加载，无 `复盘加载失败`。
  - 数据库确认：`practice_sessions=1`、`messages=13`、`reviews=1`、`phrases=0`，session 状态为 `completed`。

## 修复清单

- 修复复盘生成后立即加载失败的问题。
  - 根因：`PracticeSessionDao.upsert()` 使用 `OnConflictStrategy.REPLACE` 更新 session 状态时会先删除原行再插入，触发外键级联删除 child `reviews/messages`。
  - 修复：新增 `PracticeSessionDao.update()`，`PracticeRepository.updateSession()` 改为使用 `@Update`，避免状态更新删除关联数据。
  - 测试：新增 `PracticeRepositoryTest.statusUpdates_preserveMessagesAndReview` 覆盖状态更新后消息和复盘不被删除。
- 增加 debug-only 本地回归网络配置。
  - `app/src/debug/AndroidManifest.xml`
  - `app/src/debug/res/xml/debug_network_security_config.xml`
  - 仅允许 debug 包对 `localhost` 和 `10.0.2.2` 使用 cleartext，便于模拟器连接本地 OpenAI-compatible 回归服务。

## 遗留说明

- T-404 手动回归要求“使用真实 API 完成至少 2 个不同场景”。当前环境未提供真实 API 凭证，本轮改用本地 OpenAI-compatible 回归服务完成 2 个不同场景的核心闭环。
- 未发现 P0/P1 阻断问题。

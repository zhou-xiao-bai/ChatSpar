# 社交练习生 App 任务拆分

## 1. 使用说明

本文档用于后续逐个开发任务。每个任务都必须可以单独领取、单独开发、单独验收、单独回归。

任务编号规则：

- `T-0xx`：基础设施和工程能力。
- `T-1xx`：场景与练习。
- `T-2xx`：AI 与复盘。
- `T-3xx`：历史与表达库。
- `T-4xx`：异常、测试、发布。
- `T-5xx`：多渠道模型配置。
- `T-6xx`：语音输入与播报。
- `T-7xx`：练习效果增强。
- `T-8xx`：实时语音陪练。

每个任务完成后，必须更新任务状态：

```text
状态：未开始 / 进行中 / 已完成 / 阻塞
```

## 2. 通用回归测试要求

每个任务完成后默认执行：

```bash
./gradlew test
./gradlew assembleDebug
```

涉及 UI 的任务，还要人工回归：

- 页面能进入和返回。
- Loading、空状态、错误状态可见。
- 按钮不会重复提交。
- 文本不溢出。
- 底部导航状态正确。

涉及数据库的任务，还要人工或单元测试回归：

- 新增、查询、删除可用。
- App 重启后数据仍存在。
- 删除父数据后不会导致页面崩溃。

涉及 AI 的任务，还要人工回归：

- API 未配置时不发请求。
- API 请求失败可重试。
- AI 返回空内容不崩溃。
- AI 返回非标准 JSON 时不崩溃。

## 3. UI 参考图路径

生成图片后统一放到：

```text
docs/ui-reference/
```

需要的图片：

```text
docs/ui-reference/01-scenario-list.png
docs/ui-reference/02-scenario-detail.png
docs/ui-reference/03-chat.png
docs/ui-reference/04-review-result.png
docs/ui-reference/05-history-list.png
docs/ui-reference/06-review-detail.png
docs/ui-reference/07-phrase-library.png
docs/ui-reference/08-settings.png
docs/ui-reference/09-empty-error-states.png
docs/ui-reference/10-app-flow.png
```

具体提示词见 `plan.md` 的“UI 参考图规范”。

## 4. 任务列表

### T-001 初始化 Android 项目

状态：已完成

目标：创建可运行的 Android Kotlin + Compose 项目。

输入：

- `prd.md`
- `plan.md`

开发范围：

- 创建 Android 工程。
- 配置 Kotlin、Compose、Material 3。
- 配置基础包名，例如 `com.chatspar.app`。
- 创建 `MainActivity`。
- App 启动后显示一个临时首页。

不做：

- 不做真实页面。
- 不接数据库。
- 不接 AI API。

交付物：

- `settings.gradle.kts`
- 根目录 `build.gradle.kts`
- `app/build.gradle.kts`
- `MainActivity.kt`
- 基础 Compose 主题。

验收标准：

- App 可以在模拟器或真机启动。
- 首屏显示中文占位内容。
- 无编译错误。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动检查：

- 安装 Debug APK。
- 打开 App 不闪退。

UI 参考图：

- 暂不需要单独生成。

---

### T-002 建立导航与底部 Tab

状态：已完成

目标：实现 App 的基础页面导航结构。

输入：

- T-001
- `prd.md` 第 5 章页面结构

开发范围：

- 添加 Compose Navigation。
- 实现底部导航：`练习`、`复盘`、`表达库`。
- 添加设置入口。
- 创建页面壳：
  - `ScenarioListScreen`
  - `HistoryScreen`
  - `PhraseLibraryScreen`
  - `SettingsScreen`

不做：

- 不实现真实业务数据。
- 不接 Room。

交付物：

- `ui/navigation/AppNavGraph.kt`
- `ui/navigation/AppBottomBar.kt`
- 页面壳文件。

验收标准：

- 3 个 Tab 可切换。
- 设置页可从右上角进入。
- Android 返回键行为正常。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动检查：

- 切换三个 Tab。
- 从练习页进入设置页再返回。

UI 参考图：

- `docs/ui-reference/01-scenario-list.png`
- `docs/ui-reference/05-history-list.png`
- `docs/ui-reference/07-phrase-library.png`
- `docs/ui-reference/08-settings.png`

---

### T-003 定义领域模型

状态：已完成

目标：定义 PRD 中的数据模型，供页面、数据库和 AI 层复用。

输入：

- `prd.md` 第 7 章数据模型

开发范围：

- 定义 Kotlin data class：
  - `Scenario`
  - `PracticeSession`
  - `PracticeMessage`
  - `Review`
  - `ReviewScores`
  - `ReviewProblem`
  - `KeyMoment`
  - `SuggestedExpression`
  - `Phrase`
  - `AppSettings`
- 定义枚举：
  - `ScenarioCategory`
  - `SessionStatus`
  - `MessageRole`

不做：

- 不建数据库表。
- 不写页面。

交付物：

- `domain/model/*.kt`

验收标准：

- 模型字段覆盖 `prd.md` 中的数据结构。
- 枚举值能覆盖所有业务状态。
- 代码可编译。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

UI 参考图：

- 不需要。

---

### T-004 内置 12 个固定场景

状态：已完成

目标：把 PRD 中 12 个场景落成可读取的本地配置。

输入：

- T-003
- `prd.md` 第 3 章核心场景

开发范围：

- 创建 `app/src/main/assets/scenarios.json`。
- 实现 `ScenarioRepository`。
- 从 assets 读取并解析场景。
- 提供：
  - 获取全部场景。
  - 按分类获取场景。
  - 按 ID 获取场景详情。

不做：

- 不做用户自定义场景。
- 不把场景写入数据库。

交付物：

- `scenarios.json`
- `data/scenario/ScenarioRepository.kt`
- 场景解析单元测试。

验收标准：

- 能读取 12 个场景。
- 每个场景包含 PRD 要求的字段。
- 分类筛选数据正确。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

重点测试：

- 场景数量等于 12。
- 每个场景 `id` 唯一。
- 每个场景 `suggestedRounds` 大于等于 6。

UI 参考图：

- 不需要。

---

### T-101 场景列表页

状态：已完成

目标：实现可浏览和筛选的场景列表。

输入：

- T-002
- T-004
- UI 图：`docs/ui-reference/01-scenario-list.png`

开发范围：

- 实现 `ScenarioListScreen`。
- 展示标题、设置入口、分类筛选、场景卡片。
- 点击场景进入详情页。
- 空数据时展示空状态。

不做：

- 不做开始练习。
- 不做 AI 请求。

交付物：

- `ui/practice/ScenarioListScreen.kt`
- `ui/practice/ScenarioCard.kt`
- `ui/practice/CategoryFilter.kt`
- `ScenarioListViewModel`

验收标准：

- 默认展示全部 12 个场景。
- 点击分类后列表正确变化。
- 点击卡片能进入对应详情页。
- 底部导航高亮 `练习`。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 依次点击全部分类。
- 点击至少 3 个不同场景进入详情。
- 返回列表后筛选状态不异常。

UI 图片提示词：

```text
基于 Material 3 生成安卓“练习”场景列表页，顶部标题“练习”，右上角设置图标，分类筛选为“全部、陌生人、半熟人、职场、亲戚/饭局”，主体为场景卡片列表，卡片包含场景名称、分类、背景摘要、训练目标、难度，底部导航高亮“练习”，中文真实内容，清爽工具型风格。
```

图片保存路径：

```text
docs/ui-reference/01-scenario-list.png
```

---

### T-102 场景详情页

状态：已完成

目标：展示单个场景的背景、角色和训练目标。

输入：

- T-101
- UI 图：`docs/ui-reference/02-scenario-detail.png`

开发范围：

- 实现 `ScenarioDetailScreen`。
- 展示：
  - 场景名称
  - 背景
  - AI 角色
  - 训练目标
  - 可能遇到的问题
  - 建议轮数
  - 难度
- 添加 `开始练习` 按钮。

不做：

- 暂不创建真实会话。
- 暂不判断 API 配置。

交付物：

- `ui/practice/ScenarioDetailScreen.kt`
- `ScenarioDetailViewModel`

验收标准：

- 从列表进入详情时内容对应正确。
- 返回列表正常。
- `开始练习` 按钮可点击，但可以先跳转到占位对话页。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 从 3 个不同场景进入详情，确认内容不同。
- 横向检查长文本不溢出。

UI 图片提示词：

```text
生成安卓“场景详情”页，展示“朋友聚餐里有不熟的人”，包含场景背景、AI 扮演角色、本轮训练目标、可能遇到的问题、建议轮数、难度，底部固定主按钮“开始练习”，左上角返回，Material 3 中文工具型界面。
```

图片保存路径：

```text
docs/ui-reference/02-scenario-detail.png
```

---

### T-005 建立 Room 数据库

状态：已完成

目标：实现本地数据库基础设施。

输入：

- T-003
- `prd.md` 第 7 章数据模型

开发范围：

- 添加 Room 依赖。
- 建立 Entity：
  - `PracticeSessionEntity`
  - `MessageEntity`
  - `ReviewEntity`
  - `PhraseEntity`
- 建立 Dao：
  - `PracticeSessionDao`
  - `MessageDao`
  - `ReviewDao`
  - `PhraseDao`
- 建立 `AppDatabase`。
- JSON 字段先用字符串保存。

不做：

- 不实现页面调用。
- 不做复杂迁移。

交付物：

- `core/database/*`
- Room in-memory 单元测试。

验收标准：

- 数据库可创建。
- 会话、消息、复盘、表达可增删查。
- 单元测试覆盖基础 CRUD。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

重点测试：

- 插入会话后能查询。
- 插入多条消息后能按轮次排序。
- 删除表达后查询不到。

UI 参考图：

- 不需要。

---

### T-006 设置存储与 API Key 加密

状态：已完成

目标：完成 AI 配置的本地保存和读取。

输入：

- T-003

开发范围：

- 添加 DataStore。
- 保存：
  - API 地址
  - 模型名称
  - 是否完成新手引导
- 使用 Android Keystore 加密保存 API Key。
- 实现 `SettingsRepository`。

不做：

- 不实现设置页 UI。
- 不测试真实 AI 连接。

交付物：

- `core/datastore/SettingsDataStore.kt`
- `core/security/ApiKeyStore.kt`
- `data/settings/SettingsRepository.kt`

验收标准：

- 设置保存后可读取。
- API Key 不以明文形式直接展示。
- 清空配置后读取为空。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

重点测试：

- API 地址更新。
- 模型名称更新。
- API Key 保存、读取、清空。

UI 参考图：

- 不需要。

---

### T-103 设置页

状态：已完成

目标：实现 AI 配置和数据管理入口。

输入：

- T-002
- T-006
- UI 图：`docs/ui-reference/08-settings.png`

开发范围：

- 实现 `SettingsScreen`。
- 输入：
  - API 地址
  - API Key
  - 模型名称
- 支持 API Key 显示/隐藏。
- 支持保存配置。
- 支持测试连接按钮占位或调用后续 AI 测试接口。
- 支持清空练习记录、清空表达库入口。

不做：

- 不要求真实 AI 对话。
- 不做账号系统。

交付物：

- `ui/settings/SettingsScreen.kt`
- `SettingsViewModel`

验收标准：

- 设置可输入、保存、重新进入后回显。
- API Key 默认隐藏。
- 清空数据操作有二次确认。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 保存配置后退出设置页再进入。
- 切换 API Key 显示/隐藏。
- 点击清空按钮出现确认弹窗。

UI 图片提示词：

```text
生成安卓“设置”页，包含 API 地址输入框、API Key 输入框带显示隐藏图标、模型名称输入框、连接状态、保存配置、测试连接、数据管理区域、清空练习记录、清空表达库，Material 3 中文表单页面，简洁稳定。
```

图片保存路径：

```text
docs/ui-reference/08-settings.png
```

---

### T-201 AI 服务接口与 Fake 实现

状态：已完成

目标：先用 Fake AI 跑通对话和复盘流程，避免 UI 开发依赖真实 API。

输入：

- T-003

开发范围：

- 定义 `AiService` 接口：
  - `generateReply`
  - `generateReview`
  - `testConnection`
- 实现 `FakeAiService`。
- Fake 对话返回固定但合理的中文回复。
- Fake 复盘返回结构化 `Review`。

不做：

- 不接真实网络。
- 不做 Prompt。

交付物：

- `data/ai/AiService.kt`
- `data/ai/FakeAiService.kt`
- AI 模型请求/响应类。

验收标准：

- 不配置 API 也能在开发环境用 Fake 跑通。
- Fake 复盘结构完整。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

重点测试：

- Fake 回复不为空。
- Fake 复盘包含 5 项评分和至少 2 条推荐表达。

UI 参考图：

- 不需要。

---

### T-104 创建练习会话

状态：已完成

目标：从场景详情页点击开始后创建练习会话。

输入：

- T-005
- T-102

开发范围：

- 实现 `PracticeRepository`。
- 点击 `开始练习` 时创建 `PracticeSession`。
- 写入 AI 开场白为第一条 assistant 消息。
- 跳转到对话页。

不做：

- 不接真实 AI。
- 不生成复盘。

交付物：

- `data/practice/PracticeRepository.kt`
- 会话创建逻辑。
- 对话页路由参数。

验收标准：

- 每次开始练习生成新会话 ID。
- 开场白保存为消息。
- 对话页能根据 sessionId 读取消息。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 连续开始同一个场景两次，应生成两个不同会话。
- 返回后重新进入不会覆盖旧会话。

UI 参考图：

- `docs/ui-reference/03-chat.png`

---

### T-105 对话练习页 UI

状态：已完成

目标：实现文字对话页面。

输入：

- T-104
- T-201
- UI 图：`docs/ui-reference/03-chat.png`

开发范围：

- 实现 `ChatScreen`。
- 展示顶部场景名、AI 角色、当前轮数。
- 展示 AI/用户气泡。
- 实现输入框和发送按钮。
- 实现 `结束并复盘` 按钮。
- 用 Fake AI 返回回复。

不做：

- 不接真实 AI API。
- 不生成真实复盘。

交付物：

- `ui/chat/ChatScreen.kt`
- `ChatViewModel`
- `ChatBubbleList`
- `ChatInputBar`

验收标准：

- 用户可输入并发送文本。
- 用户消息和 AI 回复都能展示。
- 发送中输入框禁用。
- 空输入不能发送。
- 消息持久化。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 发送 3 条消息。
- 退出再进入，会话消息仍存在。
- 快速连续点击发送不会重复提交。

UI 图片提示词：

```text
生成安卓“对话练习”页，顶部显示场景名、AI 角色、当前轮数，中间为中文聊天气泡，AI 左侧、用户右侧，底部为文本输入框、发送图标按钮和“结束并复盘”按钮，展示 AI 正在回复的加载状态，Material 3 清爽工具型界面。
```

图片保存路径：

```text
docs/ui-reference/03-chat.png
```

---

### T-202 真实 AI 对话请求

状态：已完成

目标：把 Fake AI 替换为可配置的真实 OpenAI-compatible API 请求。

输入：

- T-006
- T-103
- T-105

开发范围：

- 实现 `OpenAiCompatibleService`。
- 从设置读取 API 地址、API Key、模型名。
- 构造对话 Prompt。
- 发送历史消息和场景信息。
- 解析 AI 文本回复。
- 实现测试连接。

不做：

- 不做流式输出。
- 不做多模型选择页。

交付物：

- `data/ai/OpenAiCompatibleService.kt`
- `data/ai/ChatPromptBuilder.kt`
- 网络错误映射。

验收标准：

- 配置有效 API 后可获得 AI 回复。
- API 未配置时提示去设置。
- 请求失败时页面可重试。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 未配置 API，点击发送应提示配置。
- 配置错误 Key，点击发送应展示失败和重试。
- 配置正确 Key，至少完成 3 轮对话。

UI 参考图：

- `docs/ui-reference/09-empty-error-states.png`

---

### T-203 复盘 Prompt 与结构化解析

状态：已完成

目标：实现对话结束后的 AI 复盘生成。

输入：

- T-202
- `prd.md` 第 7.4 节 Review 数据模型

开发范围：

- 实现 `ReviewPromptBuilder`。
- 要求 AI 返回固定 JSON 结构。
- 实现 JSON 解析到 `Review`。
- 保存 `raw_response`。
- 解析失败时允许重试。

不做：

- 不做复盘页面视觉细节。

交付物：

- `data/review/ReviewRepository.kt`
- `data/ai/ReviewPromptBuilder.kt`
- `ReviewJsonParser`
- 解析单元测试。

验收标准：

- 输入完整对话后能生成结构化复盘。
- 复盘字段完整。
- JSON 解析失败不会崩溃。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

重点测试：

- 标准 JSON 可解析。
- 缺少字段时有明确错误。
- 非 JSON 返回时保存 raw response 并提示失败。

UI 参考图：

- `docs/ui-reference/04-review-result.png`
- `docs/ui-reference/09-empty-error-states.png`

---

### T-204 结束练习并生成复盘

状态：已完成

目标：从对话页完成“结束并复盘”的流程。

输入：

- T-105
- T-203

开发范围：

- 点击 `结束并复盘` 后检查用户消息数量。
- 少于 3 条时弹窗提示。
- 更新会话状态为 `reviewing`。
- 调用复盘生成。
- 保存复盘。
- 更新会话状态为 `completed`。
- 跳转复盘结果页。

不做：

- 不做表达库页面。

交付物：

- 对话页结束流程。
- 复盘生成状态。
- 会话状态更新逻辑。

验收标准：

- 对话足够长时能直接复盘。
- 对话太短时有提示，可继续或仍然复盘。
- 复盘失败可重新生成。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 只发送 1 条消息后点击复盘，应出现短对话提示。
- 发送 3 条以上后复盘。
- 复盘请求失败后重试。

UI 参考图：

- `docs/ui-reference/04-review-result.png`
- `docs/ui-reference/09-empty-error-states.png`

---

### T-106 复盘结果页

状态：已完成

目标：展示本次练习复盘结果。

输入：

- T-204
- UI 图：`docs/ui-reference/04-review-result.png`

开发范围：

- 实现 `ReviewResultScreen`。
- 展示：
  - 总体评价
  - 5 个维度评分
  - 暴露的问题
  - 关键片段分析
  - 推荐表达
  - 下次练习建议
- 支持查看完整对话。
- 支持 `再练一次`。
- 支持 `返回场景列表`。

不做：

- 收藏表达可以先接占位，具体写库在 T-301。

交付物：

- `ui/review/ReviewResultScreen.kt`
- `ScorePanel`
- `ProblemList`
- `KeyMomentCard`
- `SuggestedPhraseCard`

验收标准：

- 复盘字段完整展示。
- 完整对话可展开。
- 再练一次能创建同场景新会话。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 完成一次对话并进入复盘。
- 展开完整对话。
- 点击再练一次。

UI 图片提示词：

```text
生成安卓“复盘结果”页，包含总体评价、敢开口、接话能力、分寸感、话题推进、自然度五个评分，暴露的问题列表，关键片段分析卡片展示用户原话、问题说明、更好说法，可收藏表达卡片，以及“再练一次”“返回场景列表”按钮，中文 Material 3 工具型界面。
```

图片保存路径：

```text
docs/ui-reference/04-review-result.png
```

---

### T-301 表达收藏写入

状态：已完成

目标：支持从复盘页收藏推荐表达。

输入：

- T-005
- T-106

开发范围：

- 实现 `PhraseRepository`。
- 复盘页点击收藏表达后写入 `phrases`。
- 已收藏状态展示为 `已收藏`。
- 避免同一 review 中重复收藏同一句。

不做：

- 不实现表达库列表页。

交付物：

- `data/phrase/PhraseRepository.kt`
- 复盘页收藏逻辑。

验收标准：

- 点击收藏后按钮状态变化。
- 数据库能查到收藏表达。
- 重复点击不会重复写入。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 收藏一条表达。
- 退出复盘页再进入，收藏状态仍存在。

UI 参考图：

- `docs/ui-reference/04-review-result.png`

---

### T-302 历史记录页

状态：已完成

目标：展示已完成复盘的练习记录。

输入：

- T-204
- UI 图：`docs/ui-reference/05-history-list.png`

开发范围：

- 实现 `HistoryScreen`。
- 按时间倒序展示历史记录。
- 卡片展示：
  - 场景名称
  - 练习时间
  - 总体评分
  - 主要问题摘要
  - 对话轮数
- 支持删除记录。
- 无记录时展示空状态。

不做：

- 不做云同步。

交付物：

- `ui/history/HistoryScreen.kt`
- `HistoryViewModel`
- `HistoryCard`

验收标准：

- 完成复盘后历史页出现记录。
- 记录倒序排列。
- 删除前二次确认。
- 空状态正确。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 完成两次练习，确认排序。
- 删除一条记录。
- 删除全部记录后显示空状态。

UI 图片提示词：

```text
生成安卓“复盘”历史记录页，顶部标题“复盘”，主体为历史记录卡片列表，卡片展示场景名称、练习时间、总体评分、主要问题摘要、对话轮数，带删除图标，底部导航高亮“复盘”，Material 3 中文工具型界面。
```

图片保存路径：

```text
docs/ui-reference/05-history-list.png
```

---

### T-303 历史复盘详情页

状态：已完成

目标：从历史记录进入完整复盘详情。

输入：

- T-302
- T-106
- UI 图：`docs/ui-reference/06-review-detail.png`

开发范围：

- 实现 `ReviewDetailScreen`。
- 复用复盘结果组件。
- 展示完整对话。
- 支持删除记录。
- 支持再练一次。

不做：

- 不做编辑复盘。

交付物：

- `ui/history/ReviewDetailScreen.kt`

验收标准：

- 点击历史卡片能进入详情。
- 复盘内容和原记录一致。
- 完整对话可查看。
- 删除后返回历史页。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 进入历史详情。
- 展开完整对话。
- 删除记录。
- 点击再练一次。

UI 图片提示词：

```text
生成安卓“历史复盘详情”页，展示已完成练习的评分、问题、关键片段、推荐表达和完整对话记录，顶部返回和删除图标，底部“再练一次”按钮，Material 3 中文界面，清爽实用。
```

图片保存路径：

```text
docs/ui-reference/06-review-detail.png
```

---

### T-304 表达库页

状态：已完成

目标：展示和管理收藏表达。

输入：

- T-301
- UI 图：`docs/ui-reference/07-phrase-library.png`

开发范围：

- 实现 `PhraseLibraryScreen`。
- 标签筛选：
  - 全部
  - 开场
  - 接话
  - 拒绝
  - 转移话题
  - 收尾
  - 长辈
  - 职场
- 展示表达内容、标签、来源场景、收藏时间。
- 支持复制。
- 支持删除。
- 无收藏时展示空状态。

不做：

- 不做手动新增表达。
- 不做云同步。

交付物：

- `ui/phrase/PhraseLibraryScreen.kt`
- `PhraseLibraryViewModel`
- `PhraseCard`

验收标准：

- 收藏后表达库可见。
- 标签筛选正确。
- 点击复制写入剪贴板。
- 删除前二次确认。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 收藏 2 条不同标签表达。
- 按标签筛选。
- 复制表达。
- 删除表达。

UI 图片提示词：

```text
生成安卓“表达库”页，顶部标题“表达库”，标签筛选为全部、开场、接话、拒绝、转移话题、收尾、长辈、职场，主体为表达卡片，卡片包含表达内容、适用场景标签、来源场景、收藏时间，右侧有复制和删除图标，底部导航高亮“表达库”，Material 3 中文界面。
```

图片保存路径：

```text
docs/ui-reference/07-phrase-library.png
```

---

### T-401 异常状态统一处理

状态：已完成

目标：补齐 PRD 中要求的异常状态。

输入：

- T-103
- T-202
- T-204
- T-302
- T-304
- UI 图：`docs/ui-reference/09-empty-error-states.png`

开发范围：

- API 未配置提示。
- 网络失败提示和重试。
- AI 返回空内容提示。
- 复盘 JSON 解析失败提示。
- 对话太短提示。
- 空历史记录。
- 空表达库。
- 中途退出确认。

不做：

- 不做复杂错误上报平台。

交付物：

- 通用 `EmptyState`
- 通用 `ErrorState`
- 通用确认弹窗组件。
- 页面异常分支。

验收标准：

- PRD 第 8 章异常状态全部覆盖。
- 异常发生时页面不崩溃。
- 用户有明确下一步操作。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 清空 API 配置后发送消息。
- 断网后发送消息。
- 无历史记录时进入复盘 Tab。
- 无表达时进入表达库。
- 对话中按返回。

UI 图片提示词：

```text
生成安卓 App 异常状态拼图，包含无历史记录、无收藏表达、API 未配置、网络请求失败四种状态，每种状态都有中文说明和操作按钮，例如去练习、去设置、重试，Material 3 清爽工具型风格。
```

图片保存路径：

```text
docs/ui-reference/09-empty-error-states.png
```

---

### T-402 Prompt 质量优化

状态：已完成

目标：提高 AI 对话真实感和复盘具体性。

输入：

- T-202
- T-203
- `prd.md` 第 3 章核心场景

开发范围：

- 优化对话 Prompt：
  - AI 必须扮演指定角色。
  - 回复长度适中。
  - 不直接教学。
  - 适当制造真实社交压力。
- 优化复盘 Prompt：
  - 必须引用用户原话。
  - 必须输出具体替代表达。
  - 不输出空泛鼓励。
  - 固定 JSON Schema。
- 为 12 个场景添加 Prompt 测试样例。

不做：

- 不做无限场景生成。
- 不做语音语气评价。

交付物：

- `ChatPromptBuilder` 优化。
- `ReviewPromptBuilder` 优化。
- Prompt 样例文档或测试。

验收标准：

- 同一场景连续测试 3 次，对话不明显跑题。
- 复盘能指出至少 1 个具体问题。
- 复盘引用用户原话。
- 推荐表达能直接用于真实社交。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

人工样例测试：

- S001 朋友聚餐场景。
- S008 领导同桌场景。
- S010 长辈问对象场景。

UI 参考图：

- 不需要。

---

### T-403 数据清理与删除一致性

状态：已完成

目标：保证删除历史、清空数据时不会留下异常状态。

输入：

- T-005
- T-103
- T-302
- T-304

开发范围：

- 删除单条历史记录时删除：
  - session
  - messages
  - review
- 表达收藏是否保留按产品规则处理：
  - 建议保留表达，来源记录删除后显示“来源已删除”。
- 设置页清空练习记录。
- 设置页清空表达库。

不做：

- 不做云端数据处理。

交付物：

- 数据删除事务。
- 清空数据逻辑。
- 删除一致性测试。

验收标准：

- 删除历史后历史页不再显示。
- 删除历史后表达库不崩溃。
- 清空练习记录不影响 API 设置。
- 清空表达库不影响历史记录。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 完成练习并收藏表达。
- 删除历史记录。
- 查看表达库。
- 清空表达库。
- 清空练习记录。

UI 参考图：

- `docs/ui-reference/08-settings.png`

---

### T-404 全流程回归测试

状态：已完成

目标：对 MVP 核心闭环做一次完整回归。

输入：

- T-001 至 T-403

开发范围：

- 不新增大功能。
- 修复阻断核心流程的问题。
- 补充必要测试。

核心回归路径：

1. 打开 App。
2. 进入练习 Tab。
3. 筛选场景。
4. 进入场景详情。
5. 开始练习。
6. 完成至少 6 轮对话。
7. 结束并复盘。
8. 收藏推荐表达。
9. 进入历史记录查看复盘。
10. 进入表达库复制表达。
11. 设置页测试连接。
12. 清空数据。

不做：

- 不新增后端。
- 不新增语音。
- 不新增账号。

交付物：

- 回归测试记录：`docs/t-404-regression.md`
- 修复清单。

验收标准：

- 核心回归路径全部通过。
- 没有 P0/P1 阻断问题。
- Debug APK 可稳定安装使用。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 使用真实 API 完成至少 2 个不同场景。
- 使用错误 API Key 验证失败状态。
- 断网验证失败状态。

UI 参考图：

- `docs/ui-reference/10-app-flow.png`

---

### T-405 生成自用 APK

状态：已完成

目标：打包可安装的自用版本。

输入：

- T-404

开发范围：

- 配置 App 名称和图标占位。
- 配置 release build。
- 生成 APK。
- 整理安装说明。

不做：

- 不上架应用商店。
- 不做正式隐私政策页面。
- 不做商业化配置。

交付物：

- Debug APK。
- Release APK。
- 简短安装说明。

验收标准：

- APK 可在安卓设备安装。
- 安装后可完成完整练习闭环。
- 卸载重装后本地数据按预期清空。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

手动回归：

- 真机安装 release APK。
- 配置 API。
- 完成一次对话和复盘。

完成记录：

- 已生成 Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 已生成 Release APK：`app/build/outputs/apk/release/app-release.apk`。
- Release APK 使用本机 debug key 签名，仅用于自用安装测试。
- 已在模拟器 `emulator-5554` 验证卸载重装、安装 release APK 和冷启动成功。
- 当前环境未连接真机，且未提供真实 API 凭证；真机完整对话与复盘回归需在真实设备/API 上补充执行。
- 安装说明见 `docs/install-apk.md`。

UI 参考图：

- 不需要。

---

### T-501 定义多渠道模型配置

状态：已完成

目标：把单一 `AppSettings` 中的 API 配置升级为多渠道模型配置模型。

输入：

- `prd.md` 第 11.1 节
- `plan.md` 阶段 8
- T-006
- T-202

开发范围：

- 新增领域模型：
  - `AiProviderConfig`
  - `AiProviderType`
  - `AiProviderPreset`
- 配置字段包含：
  - 渠道 ID
  - 渠道类型
  - 展示名称
  - API 地址
  - API Key alias
  - 对话模型名
  - 复盘模型名
  - 是否默认对话渠道
  - 是否默认复盘渠道
  - 是否启用
  - 创建/更新时间
- 保留兼容旧 `AppSettings` 的读取能力，避免升级后已有配置直接失效。

不做：

- 不改设置页 UI。
- 不接真实请求路由。
- 不做云端同步。

交付物：

- 多渠道领域模型。
- 多渠道配置序列化方案。
- 配置迁移设计。
- 单元测试。

验收标准：

- 可以表达多个渠道配置。
- 可以区分默认对话渠道和默认复盘渠道。
- 可以从旧单一配置迁移出一个自定义渠道。
- 空配置时仍能给出“未配置”的业务状态。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

重点测试：

- 旧配置迁移为单个自定义渠道。
- 多渠道配置保存和读取。
- 删除默认渠道后能检测到默认缺失。

UI 参考图：

- 暂不需要。

完成记录：

- 已新增 `AiProviderConfig`、`AiProviderType`、`AiProviderPreset` 和首批国产模型预设。
- 已在 DataStore 中增加多渠道配置 JSON 字段。
- 已在 `SettingsRepository` 中增加多渠道读写、默认对话/复盘渠道读取和旧单一配置 fallback。
- 现有单一 API 配置保存时会同步生成一个兼容的自定义 OpenAI-compatible 渠道。
- 已补充设置仓库单元测试，覆盖旧配置迁移、多渠道保存、默认渠道选择和空配置状态。
- 已通过 `./gradlew test` 和 `./gradlew assembleDebug`。

---

### T-502 多 API Key 加密存储

状态：已完成

目标：支持按渠道独立保存、读取和删除 API Key。

输入：

- T-501
- T-006

开发范围：

- 扩展 `ApiKeyStore` 能力：
  - 按 alias 保存 API Key
  - 按 alias 读取 API Key
  - 按 alias 删除 API Key
  - 清空全部渠道 Key
- 保留旧单一 API Key 的迁移读取能力。
- 删除渠道时同步删除对应 Key。

不做：

- 不在数据库或 DataStore 中明文保存 API Key。
- 不实现后端代理。

交付物：

- 多 Key 存储接口。
- Android Keystore 实现。
- Fake/Test KeyStore 实现。
- 单元测试。

验收标准：

- 不同渠道的 Key 互不覆盖。
- 删除一个渠道 Key 不影响其他渠道。
- 清空设置时能清理所有渠道 Key。
- 旧单一 Key 能迁移到默认 alias。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

重点测试：

- 保存两个 alias 后分别读取。
- 删除一个 alias 后另一个仍可读取。
- 清空设置后所有 alias 都不可读取。

UI 参考图：

- 不需要。

完成记录：

- 已扩展 `ApiKeyStore`，支持按 alias 保存、读取、删除和清空全部 API Key。
- 已保留旧 `saveApiKey/getApiKey/clearApiKey` 调用，默认映射到 legacy alias。
- 已更新 Android Keystore 实现，使每个 alias 使用独立密文和 IV。
- 已保留旧版本单一 preference key 的读取和清理兼容。
- 已更新测试 Fake KeyStore，并补充多 alias 独立读写、删除和清空测试。
- 已通过 `./gradlew test` 和 `./gradlew assembleDebug`。

---

### T-503 设置页模型渠道管理

状态：已完成

目标：把设置页从单一 API 表单升级为模型渠道管理页面。

输入：

- T-501
- T-502
- `prd.md` 第 11.1 节

开发范围：

- 设置页展示模型渠道列表。
- 支持新增渠道。
- 支持从预设渠道创建配置：
  - DeepSeek
  - 通义千问/Qwen
  - Kimi
  - 智谱 GLM
  - 豆包
  - 自定义 OpenAI-compatible
- 支持编辑：
  - 展示名称
  - API 地址
  - API Key
  - 对话模型名
  - 复盘模型名
- 支持启用/禁用渠道。
- 支持设为默认对话渠道。
- 支持设为默认复盘渠道。
- 支持删除渠道。
- 删除默认渠道时提示用户重新选择默认渠道。

不做：

- 不做服务商账号登录。
- 不自动拉取模型列表。
- 不做云端配置同步。

交付物：

- 设置页渠道列表 UI。
- 渠道编辑表单。
- 预设渠道选择入口。
- `SettingsViewModel` 多渠道状态管理。

验收标准：

- 用户可以新增并保存至少 2 个渠道。
- 重新进入设置页后配置能回显。
- 可以分别设置默认对话渠道和默认复盘渠道。
- 删除渠道前有确认。
- API Key 默认隐藏，可切换显示。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 新增 DeepSeek 配置。
- 新增自定义配置。
- 分别设为默认对话和默认复盘。
- 删除默认渠道并检查提示。
- 退出设置页后重新进入。

UI 参考图：

- 需要新增：`docs/ui-reference/11-provider-settings.png`

UI 图片提示词：

```text
生成安卓“模型渠道设置”页，顶部标题“模型渠道”，主体是已配置渠道列表，每个渠道卡片显示渠道名称、类型、对话模型、复盘模型、连接状态，并有“测试”“编辑”“设为默认”操作。页面右下角或底部有“新增渠道”按钮。新增渠道弹窗包含 DeepSeek、通义千问、Kimi、智谱、豆包、自定义。Material 3 中文工具型界面。
```

完成记录：

- 已将设置页从单一 API 表单升级为模型渠道列表。
- 已支持 DeepSeek、通义千问/Qwen、Kimi、智谱 GLM、豆包和自定义 OpenAI-compatible 预设入口。
- 已新增渠道编辑弹窗，支持展示名称、API 地址、API Key、对话模型名、复盘模型名、启用状态和默认用途设置。
- 已支持启用/禁用、默认对话、默认复盘、删除确认和默认渠道删除提示。
- 已让 `SettingsRepository.getSettings()` 读取默认对话渠道对应 alias 的 API Key，保证旧 AI 请求入口继续可用。
- 已扩展 `AiService.testConnection`，支持按单个 provider 配置独立测试连接。
- 已新增 `docs/ui-reference/11-provider-settings.png` 并更新 `docs/ui-reference/ui-reference.html` 的第 11 页。
- 已补充设置仓库和 Fake AI 服务测试，覆盖默认渠道归一化、默认渠道 API Key 读取和 provider 测试接口。
- 已通过 `./gradlew test` 和 `./gradlew assembleDebug`。

---

### T-504 AI Provider 路由与真实请求

状态：未开始

目标：让 AI 请求根据用途选择对应渠道，并继续复用 OpenAI-compatible 请求实现。

输入：

- T-501
- T-502
- T-503
- T-202
- T-203

开发范围：

- 新增 `AiProviderRepository` 或等价配置读取层。
- 新增请求用途：
  - `chat`
  - `review`
  - `connection_test`
- 对话请求使用默认对话渠道和对话模型。
- 复盘请求使用默认复盘渠道和复盘模型。
- 默认复盘渠道缺失时回退默认对话渠道。
- 请求失败时错误信息包含渠道名称。
- 连接测试支持指定渠道。

不做：

- 不做请求失败自动切换渠道。
- 不做流式响应。
- 不做非 OpenAI-compatible 特殊协议。

交付物：

- Provider 路由逻辑。
- OpenAI-compatible 请求参数适配。
- 连接测试按渠道执行。
- 单元测试。

验收标准：

- 对话请求使用默认对话模型。
- 复盘请求使用默认复盘模型。
- 指定渠道测试连接可成功或返回明确错误。
- 没有可用渠道时提示进入设置。
- 原有聊天和复盘流程不退化。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 使用同一渠道完成对话和复盘。
- 使用不同渠道分别完成对话和复盘。
- 禁用默认渠道后尝试发送消息。
- 配置错误 Key 后测试连接。

UI 参考图：

- `docs/ui-reference/11-provider-settings.png`

---

### T-505 多渠道配置迁移与回归

状态：未开始

目标：确保已有单一 API 配置用户升级后不丢配置，且全流程稳定。

输入：

- T-501 至 T-504

开发范围：

- 启动时检测旧配置。
- 将旧 API 地址、模型名、API Key 迁移为一个自定义渠道。
- 迁移后设为默认对话渠道和默认复盘渠道。
- 保留迁移幂等性，避免重复生成渠道。
- 整理多渠道回归记录。

不做：

- 不迁移云端数据。
- 不做跨设备迁移。

交付物：

- 配置迁移逻辑。
- 迁移单元测试。
- 回归记录：`docs/t-505-provider-regression.md`

验收标准：

- 已有单一配置升级后仍可测试连接。
- 迁移重复执行不会产生重复渠道。
- 清空设置能清理旧配置和新配置。
- 核心对话和复盘闭环通过。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 从旧配置版本升级。
- 检查渠道列表。
- 完成一次对话。
- 完成一次复盘。
- 清空设置后重新配置。

UI 参考图：

- 不需要。

---

### T-601 语音输入

状态：未开始

目标：在对话页支持用户通过语音生成文本输入。

输入：

- `prd.md` 第 11.2 节
- `plan.md` 阶段 9
- T-105

开发范围：

- 添加麦克风权限声明。
- 对话输入区增加麦克风按钮。
- 请求运行时麦克风权限。
- 使用 Android SpeechRecognizer 将语音转文字。
- 将识别结果填入输入框。
- 识别中展示状态。
- 识别失败时展示错误提示。

不做：

- 不自动保存原始音频。
- 不做实时语音连续对话。
- 不默认识别完成后自动发送。

交付物：

- 语音输入控制逻辑。
- 对话页麦克风入口。
- 权限和错误状态处理。
- ViewModel/组件测试或可测试封装。

验收标准：

- 用户点击麦克风后可以开始识别。
- 识别结果进入文本输入框。
- 用户可以编辑识别文本后发送。
- 拒绝权限时页面不崩溃并提示原因。
- 识别失败不丢失当前输入框内容。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 首次授权麦克风。
- 拒绝麦克风权限。
- 识别一段中文并发送。
- 识别失败后手动输入发送。

UI 参考图：

- 需要新增：`docs/ui-reference/12-voice-input-chat.png`

UI 图片提示词：

```text
生成安卓对话练习页，保留原有聊天气泡和输入框，输入区右侧有发送按钮，左侧或输入框内有麦克风图标。页面展示“正在听...”状态和可取消录音按钮。整体是中文社交陪练工具界面，Material 3 风格，信息清晰，不要营销装饰。
```

---

### T-602 AI 回复 TTS 播报

状态：未开始

目标：支持播放 AI 回复，并提供自动播报开关。

输入：

- T-601
- `prd.md` 第 11.2 节

开发范围：

- 集成 Android TextToSpeech。
- AI 回复气泡增加播放/停止按钮。
- 对话页增加“自动播报 AI 回复”开关或设置项。
- 保存自动播报开关。
- 新 AI 回复生成后按设置自动播报。
- 离开页面时停止播报并释放资源。

不做：

- 不做真人音色。
- 不做语音克隆。
- 不做音频文件保存。

交付物：

- TTS 控制器。
- 播放状态 UI。
- 自动播报设置。
- 生命周期释放逻辑。

验收标准：

- 用户可以点击播放单条 AI 回复。
- 播放中可以停止。
- 自动播报开启后，新回复自动播放。
- TTS 不可用时给出提示，不影响文字对话。
- 返回或退出对话页后停止播报。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 播放一条历史 AI 回复。
- 播放中停止。
- 开启自动播报后发送消息。
- 播报中返回上一页。

UI 参考图：

- `docs/ui-reference/12-voice-input-chat.png`

---

### T-603 语音偏好设置与回归

状态：未开始

目标：补齐语音功能的用户偏好和完整回归。

输入：

- T-601
- T-602

开发范围：

- 设置页增加语音偏好：
  - 自动播报 AI 回复
  - 识别完成后是否自动发送
  - TTS 语速
- DataStore 保存语音偏好。
- 对话页读取语音偏好。
- 整理语音功能回归记录。

不做：

- 不做语音供应商配置。
- 不做音频上传。

交付物：

- 语音设置项。
- 设置读写逻辑。
- 回归记录：`docs/t-603-voice-regression.md`

验收标准：

- 语音偏好重启后仍生效。
- 自动发送关闭时只回填输入框。
- 自动发送开启时识别完成后发送文本。
- TTS 语速设置生效。
- 文字对话主流程不受影响。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 修改语音设置后重启 App。
- 验证自动播报。
- 验证识别后自动发送。
- 验证语速变化。

UI 参考图：

- 可复用 `docs/ui-reference/08-settings.png`，后续更新为新版设置图。

---

### T-701 本轮训练重点

状态：未开始

目标：让用户在开始练习前选择本轮训练重点，并让对话和复盘围绕该重点展开。

输入：

- `prd.md` 第 11.4 节
- T-102
- T-202
- T-203

开发范围：

- 定义训练重点枚举：
  - 开场
  - 接话
  - 拒绝
  - 转移话题
  - 收尾
  - 分寸感
- 场景详情页增加训练重点选择。
- 创建会话时保存本轮训练重点。
- 对话 Prompt 注入训练重点。
- 复盘 Prompt 注入训练重点。
- 复盘页展示本轮训练重点。

不做：

- 不做复杂训练计划。
- 不做自动推荐重点。

交付物：

- 训练重点模型。
- 会话保存字段或可兼容存储方案。
- UI 选择控件。
- Prompt 调整。
- 单元测试。

验收标准：

- 用户开始练习前可以选择训练重点。
- 已选重点能在对话和复盘 Prompt 中体现。
- 历史复盘能看到当时选择的重点。
- 未选择时使用场景默认目标。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 选择“拒绝”开始亲戚/饭局场景。
- 完成对话并检查复盘是否聚焦拒绝表达。
- 未选择重点时完成默认流程。

UI 参考图：

- 可新增：`docs/ui-reference/13-training-focus.png`

---

### T-702 表达卡片复习

状态：未开始

目标：让表达库从“收藏列表”升级为可重复练习的表达卡片。

输入：

- T-304
- T-701

开发范围：

- 表达详情或卡片操作增加：
  - 跟读
  - 改写
  - 换场景使用
- 支持从表达卡片发起短练习。
- 短练习可复用现有对话和复盘流程。
- 记录表达复习次数和最近复习时间。

不做：

- 不做完整记忆曲线算法。
- 不做复杂打卡系统。

交付物：

- 表达复习入口。
- 表达复习记录字段。
- 短练习启动逻辑。
- 表达库 UI 更新。

验收标准：

- 收藏表达可以发起复习。
- 复习后表达记录更新最近复习时间。
- 表达可以被迁移到另一个场景练习。
- 删除表达后复习入口不可用且不崩溃。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 收藏一条表达。
- 从表达库发起复习。
- 完成短练习。
- 删除表达后检查页面状态。

UI 参考图：

- 可新增：`docs/ui-reference/14-phrase-review.png`

---

### T-703 复盘导出与请求日志

状态：未开始

目标：提供自用排查和长期整理能力。

输入：

- T-504
- T-303

开发范围：

- 复盘详情页增加导出 Markdown。
- 导出内容包含：
  - 场景信息
  - 训练重点
  - 完整对话
  - 评分
  - 问题
  - 关键片段
  - 推荐表达
- 记录 AI 请求日志：
  - 时间
  - 渠道
  - 模型
  - 用途
  - 是否成功
  - 耗时
  - 错误摘要
- 可选记录 token 或字符数估算。

不做：

- 不记录 API Key。
- 不记录完整请求体到日志。
- 不做云端日志上传。

交付物：

- Markdown 导出能力。
- 请求日志本地存储或文件记录。
- 日志清理入口。

验收标准：

- 用户可以导出一次复盘为 Markdown。
- 请求日志不包含 API Key。
- 设置页可以清理请求日志。
- 请求失败时日志能辅助定位渠道和错误类型。

回归测试：

```bash
./gradlew test
./gradlew assembleDebug
```

手动回归：

- 完成一次复盘并导出。
- 触发一次错误 API 请求。
- 查看日志摘要。
- 清理日志。

UI 参考图：

- 不需要。

---

### T-801 实时语音陪练模式设计

状态：未开始

目标：在开发实时语音前明确状态机、页面和边界，避免直接堆功能导致状态失控。

输入：

- `prd.md` 第 11.3 节
- T-601
- T-602

开发范围：

- 设计语音练习状态机：
  - idle
  - listening
  - recognizing
  - thinking
  - speaking
  - error
- 设计页面结构。
- 设计打断/停止播报规则。
- 设计退出确认和资源释放规则。
- 补充任务拆分和验收标准。

不做：

- 不写正式功能代码。
- 不接实时音频 API。

交付物：

- 设计文档：`docs/realtime-voice-mode.md`
- 状态机图或文字说明。
- 后续实现任务清单。

验收标准：

- 状态流转覆盖正常路径和错误路径。
- 明确哪些状态允许用户说话、停止、退出。
- 明确是否保存音频和保存哪些文字记录。
- 明确复用现有复盘流程的方式。

回归测试：

- 文档评审，不需要运行 Gradle。

UI 参考图：

- 可新增：`docs/ui-reference/15-realtime-voice.png`

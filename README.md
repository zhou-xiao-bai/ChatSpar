# ChatSpar｜社交练习生

社交练习生是一个自用 Android 文字对话陪练 App。它通过模拟中国现代真实社交场景，让用户练习开场、接话、分寸感、拒绝和收尾，并在对话后生成可执行复盘，沉淀可复用表达。

## 当前定位

- 平台：Android 原生应用
- 交互：文字输入为主
- AI：OpenAI-compatible 接口，用于角色扮演、对话推进和复盘
- 数据：本地存储为主，不做账号体系
- 包名：`com.chatspar.app`
- 显示名称：`社交练习生`

## 已实现能力

- 固定社交场景库，包含陌生人、半熟人、职场、亲戚/饭局等场景
- 场景详情页，展示背景、AI 角色、训练目标和压力点
- 文字对话练习，支持 AI 开场、用户输入、AI 回复和手动结束
- 练习复盘，展示评分、问题、关键片段和推荐表达
- 历史记录，本地查看和删除过往练习
- 表达库，收藏、复制和删除推荐表达
- 设置页，配置 API 地址、API Key、模型名称，支持测试连接和清空本地数据

## 技术栈

- Kotlin
- Jetpack Compose / Material 3
- Navigation Compose
- Room
- DataStore
- Android Keystore
- Kotlinx Serialization
- Robolectric / JUnit
- Gradle Kotlin DSL

## 目录说明

```text
app/src/main/java/com/chatspar/app
├── core        # 数据库、DataStore、安全存储
├── data        # AI、场景、练习、复盘、历史、表达和设置仓储
├── domain      # 领域模型
└── ui          # Compose 页面、导航和主题

app/src/main/assets/scenarios.json  # 内置场景配置
app/src/test                        # 单元测试
docs                                # 安装说明、回归记录、UI 参考图
prd.md                              # 产品需求文档
plan.md                             # 开发计划
tasks.md                            # 任务拆分
```

## 本地构建

要求：

- JDK 17
- Android SDK
- Gradle wrapper 使用仓库内置 `./gradlew`

常用命令：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

APK 安装说明见 [docs/install-apk.md](docs/install-apk.md)。

## AI 配置

首次打开 App 后进入设置页，配置：

- API 地址
- API Key
- 模型名称

API Key 只保存在本机加密存储中，不写入仓库。测试代码里的 `sk-test` 是假值。

## Git 上传范围

仓库已提交项目源码、测试、Gradle wrapper、资源文件、文档和 UI 参考图。

以下内容不上传：

- `.gradle/`
- `.kotlin/`
- `**/build/`
- `local.properties`
- `.DS_Store`
- `.idea/`
- `*.apk`
- `*.aab`
- `*.ap_`
- `*.dex`
- `*.class`
- `*.log`
- `captures/`

这些都是本地缓存、IDE 配置、构建产物或安装包，不属于源码交付物。

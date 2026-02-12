# PillRing

PillRing 是基于 Android + Jetpack Compose 的多计划吃药提醒应用。

## 功能概览

- 多计划提醒：支持添加、编辑、启用/停用、排序和删除提醒计划。
- 精确时间触发：支持每日提醒，或“间隔 X 日 + 起始日期”的周期提醒（如起始日 1 月 1 日、间隔 1 日时在 1/1、1/3、1/5... 触发）。
- 提醒确认页：收到提醒后可进入确认页面，长按约 1.2 秒确认本轮已服药并停止当前提醒。
- 兜底提醒机制：通知被划掉且未确认时，会在 30 秒后触发一次兜底提醒。
- 权限健康检查：在设置页集中检查通知权限、精确闹钟、电池优化、后台限制等影响提醒可靠性的项。
- 应用内语言切换：支持跟随系统、英文、简体中文。
- 版本更新检查：在“关于”页检查 GitHub Releases 最新稳定版本。

## 技术栈与运行要求

- 语言与框架：Kotlin、Jetpack Compose、Material 3。
- 构建工具：Gradle（Wrapper 当前为 9.1.0）。
- Android 配置：
  - `minSdk = 35`
  - `targetSdk = 36`
  - `compileSdk = 36`
- Java/JDK：建议 JDK 17 及以上（仓库包含 toolchain 配置，当前配置为 JDK 21）。
- 开发环境建议：Android Studio 最新稳定版 + 已安装 Android SDK 35/36 相关组件。

## 快速开始（开发者）

1. 克隆仓库并进入项目目录。
2. 使用 Android Studio 打开项目根目录。
3. 等待 Gradle 同步完成。
4. 连接 Android 15+ 真机或启动 Android 15+ 模拟器。
5. 运行 `app` 模块（Run `app`）。

命令行构建（Windows）：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

命令行构建（macOS/Linux）：

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## 使用说明

1. 首次打开应用后，为确保正常使用，请在系统设置中授予应用所需各项权限（如通知权限、精确闹钟权限、电池优化相关设置与后台运行相关设置）。
2. 在首页点击“添加计划”，设置计划名称、提醒时间与周期模式（每日或间隔 X 日）；若选择间隔模式需设置起始日期。
3. 打开计划开关启用提醒。
4. 收到提醒通知后，点击通知可进入提醒确认页。
5. 服药后在确认页长按确认按钮（约 1.2 秒）停止该计划本轮提醒。
6. 若通知被误划掉，应用会在 30 秒后尝试再次提醒；也可从首页点击处于提醒中的计划卡片进入确认页。
7. 若提醒不稳定，进入设置页查看“权限健康检查”，按提示逐项处理。

## 项目结构

关键目录（省略非核心资源）：

```text
app/src/main/java/io/github/c1921/pillring/
├── MainActivity.kt                # 主界面与页面导航、核心交互
├── notification/                  # 提醒计划、闹钟调度、通知与广播接收
├── permission/                    # 权限健康检查与系统设置跳转
├── update/                        # 版本检查、缓存与版本比较逻辑
├── locale/                        # 应用语言选择与生效管理
└── ui/                            # UI 测试标签与主题等
```

测试目录：

- `app/src/test/...`：本地单元测试。
- `app/src/androidTest/...`：设备端仪器测试（含 Compose UI 测试）。

## 许可证状态

本项目采用 `GNU GPL v3.0` 协议。

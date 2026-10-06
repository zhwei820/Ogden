# Panda English

一款面向中文英语学习者的原生 Android 单词 App，基于 Kotlin + Jetpack Compose 构建。它围绕 Panda English 的 850 个基础词展开，把词库、发音、闯关和复习都放在离线可用的移动端体验里。

> 本项目是一次二次创作。我在 X 上看到 [Ogden's Basic English](https://ogden.munch.love/) 的作品后很受触动，并在征得原网站作者同意的情况下，将其内容与气质重新设计为 Android 学习应用。原作地址：[https://ogden.munch.love/](https://ogden.munch.love/)

欢迎大家下载、使用并反馈。作者 X 账号：[@Skivein](https://x.com/Skivein)

## Screenshots

| Challenge | Library | Review |
| --- | --- | --- |
| ![Challenge](docs/screenshots/challenge.jpg) | ![Library](docs/screenshots/library.jpg) | ![Review](docs/screenshots/review.jpg) |

## Features

- 内置 Panda English 850 词，另加 450 个儿童生活常用拓展词和 98 个变形词（my、went、is 等），支持离线学习。
- 词库分为 Operations、General Things、Things（物品词）、Qualities、Opposites。
- 支持搜索、分类筛选、US/UK 发音切换、简体/繁体显示。
- 课文：三级 × 10 个主题 × 5 单元，每课含课文、句型和本课单词；点句子朗读，长按单词查看释义（自动还原 boxes→box 等词形）并收藏；课文、译文和单词中文释义均有内置朗读音频；每个主题有主题练习。
- 每 10 个词为一关，分类逐步解锁。
- 练习包含听音选词、看中文选英文、例句填空、拼写挑战、近义词配对。
- 本地保存学习进度、收藏、错词、熟练度和连续学习天数。
- 复习中心提供错词本和收藏夹。
- 内置 US / UK 两套单词音频，优先离线播放。

## Word Data

词库总数为 1398（Ogden 850 + 拓展词 450 + 变形词 98）：

- Operations: 100
- General Things: 400
- Things（物品词）: 200
- Qualities: 100
- Opposites: 50
- Extended（拓展词）: 450
- Word Forms（变形词）: 98

主要数据文件：

- `app/src/main/assets/ogden_words.json`
- `app/src/main/assets/ogden_ipa.json`
- `app/src/main/assets/speeches.json`
- `app/src/main/assets/audio/us`
- `app/src/main/assets/audio/uk`

## Build

用 Android Studio 打开本目录，等待 Gradle 同步后运行 `app`。

命令行构建：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-parallel
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- App 首版以离线学习为主，不接入账号、云同步或排行榜。
- 英文朗读（单词、课文、例句、练习）都使用内置音频：单词在 `assets/audio/{us,uk}`，其余在 `assets/tts/<语言>/<sha1>.mp3`；中文不预生成，播放时在线请求 Azure 并缓存在本机；缺失或失败时回退系统 TTS。
- 新增或修改了会朗读的内容后，重新生成音频（单元测试 `TtsCoverageTest` 会列出缺失项）：
  ```
  ./gradlew :app:testDebugUnitTest --tests '*TtsCoverageTest.writeManifest'
  python3 scripts/gen_word_audio.py && python3 scripts/gen_tts_assets.py
  ```
- 生成脚本与运行时备用合成都需在 `local.properties` 配置 `azure.speech.key` 与 `azure.speech.region`（构建时注入 APK，仅适合自用）。

## License

本项目以 [MIT License](LICENSE) 开源。

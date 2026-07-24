# Task: 修复展厅公司语音生成 414 Request-URI Too Large

## Goal

修复 `展厅 -> 展厅公司` 点击 `生成语音` 时，阿里云 NLS TTS 因请求 URI 过长返回 `414 Request-URI Too Large` 的问题。修复后，长公司介绍也必须按真实文本成功生成中英文音频，或在前置条件缺失时明确失败，不得静默降级、截断成功或 fallback。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\main\java\cn\iocoder\yudao\module\ai\service\tts\AliyunNlsTtsSynthesizer.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\test\java\cn\iocoder\yudao\module\ai\service\tts\AliyunNlsTtsSynthesizerTest.java`
- 如回归证明 showroom 侧仍需补充契约测试，则包含最小相关测试
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-narration-audio-414-fix\**`

## Non-Scope

- 不修改公司页面按钮、目标字数控件或文案。
- 不改用其他 TTS 服务、mock 音频或本地占位音频。
- 不顺带重构 showroom 匿名展示合同或审批流。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-public-display-pure-frontstage\task.md`
- Status before this task: `Blocked on 2026-05-21`
- Impact: 上一同仓任务已按当前用户优先级显式阻塞，避免与本次紧急语音生成缺陷并行推进造成任务记录冲突；其未完成状态继续影响 Website 纯展示匿名合同，但不阻塞本次 TTS 修复。

## Milestones

- [x] M1: 处理上一同仓任务状态并创建本次任务文档。
- [x] M2: 先补 RED，锁定长文本语音请求不得走超长 URI，且长文本必须可分段生成 WAV。
- [x] M3: 最小修复阿里云 NLS 合成器，改为适配长文本的稳定请求方式。
- [x] M4: 跑通定向 GREEN，并补充必要证据与风险说明。

## Expected Verification

- `mvn --% -pl yudao-module-ai -Dtest=AliyunNlsTtsSynthesizerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-narration-audio-414-fix\bug-regression-evidence.md`
- 若本地运行库允许，再补真实 `http://localhost:8081/showroom/company` 点击 `生成语音` 回放

## Current Status

- Status: Completed
- Completed work:
  - 已定位当前根因之一是 `AliyunNlsTtsSynthesizer` 使用 `GET /stream/v1/tts` 并将全文放进 query string。
  - 已确认当前公司页面会把长中文介绍直接提交到后端生成语音。
  - 已将阿里云 NLS 请求改为 `POST` JSON，并保留 `X-NLS-Token` 与 `X-Request-ID` 请求头。
  - 已实现超过 300 字符时按标点优先分段请求并合并 WAV 音频。
  - 已完成 `AliyunNlsTtsSynthesizerTest` 定向 RED/GREEN，并通过 bug regression validator。
- Remaining blockers:
  - 无本任务代码 blocker。

## Final Verification Result

- PASS: `mvn --% -pl yudao-module-ai -Dtest=AliyunNlsTtsSynthesizerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-narration-audio-414-fix\bug-regression-evidence.md`
- BLOCKED: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomAliyunNlsAudioGenerationAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test`，被当前脏工作区中的无关 showroom 测试编译错误阻断，详见 `execution-log.md`

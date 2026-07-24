# 任务：展厅公司语音生成与保存支持手改英文介绍

## Goal

调整展厅公司语音生成与保存接口，使前端在 `AI生成介绍` 后手动修改英文介绍时，后端能够基于当前中文与英文介绍生成双语音频，并在 `保存语音` 后把本次英文介绍文本与重新生成的中英文音频一起发布为 live 版本。整个链路必须显式使用用户当前输入，不得静默重新翻译覆盖用户手改英文。

## Scope

- 调整公司语音生成接口契约，允许显式传入英文介绍文本。
- 调整公司语音生成实现，中文走原文、英文走用户传入文本；若英文为空则明确失败，不做静默 fallback。
- 补齐公司语音生成/保存的后端集成测试与任务证据。
- 保持公司文字保存接口与产品讲解接口不变。

## Non-Scope

- 不改动产品讲解稿/语音接口。
- 不改动公司审批流与版本发布规则。
- 不新增自动兼容旧字段的隐藏分支或默认成功返回。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-ai-script-timeout\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一任务已修复公司 AI 介绍生成超时；本次继续在同一业务链路上扩展“手改英文后生成/保存语音”的契约，不回退超时修复。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库可能存在其他模块未提交改动与既有任务文档。
- Impact: 本任务仅允许修改 showroom 公司语音相关控制器、运行时、测试与本任务文档，不触碰无关模块。

## Milestones

- [x] M1: 确认上一同仓任务状态并创建本任务文档。
- [x] M2: 先补后端 RED 测试，锁定“生成语音使用当前英文介绍、保存后 live 英文介绍保持本次文本”的行为。
- [x] M3: 最小改动公司语音请求契约与运行时实现，通过显式英文文本驱动英文语音生成。
- [x] M4: 运行定向后端测试并记录 GREEN。
- [x] M5: 更新后端证据、执行 closeout preview，并准备同仓提交。

## Expected Verification

- `mvn clean "-Dsurefire.failIfNoSpecifiedTests=false" -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#companyNarrationGenerateAndPublishShouldAllowLoggedInUserWithoutChangingCompanyRevision" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260521-showroom-company-editable-english-narration/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-company-editable-english-narration --mode preview`

## Current Status

Completed on 2026-05-21.

已完成公司语音生成契约调整。当前后端会显式要求 `introTextEn`，并直接使用前端提交的英文介绍生成英文 narration；保存后 live `EN` narration 保持本次手改英文，不再在音频生成阶段重新翻译中文覆盖用户输入。

## Blockers And Impact

- Blocker: none.
- Impact: none.

## Final Verification Result

- PASS: `mvn clean "-Dsurefire.failIfNoSpecifiedTests=false" -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#companyNarrationGenerateAndPublishShouldAllowLoggedInUserWithoutChangingCompanyRevision" test`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260521-showroom-company-editable-english-narration/backend-api-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-company-editable-english-narration --mode preview`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-company-editable-english-narration --mode apply`

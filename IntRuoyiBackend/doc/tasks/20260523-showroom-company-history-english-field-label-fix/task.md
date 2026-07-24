# 任务：修复公司历史英文字段标签 unknown company field

## Goal

- 修复公司页面进入或查看公司历史时出现 `SHOWROOM_TARGET_NOT_FOUND: unknown company field development_history_en` 的问题。
- 保证公司 revision / 审批差异预览在遇到公司英文字段（如 `development_history_en`）时能正常生成标签并返回。
- 保持 fail-fast 语义，不对未知字段做静默吞错或 fallback。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\foundation\meta\ShowroomFieldDisplaySupport.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\foundation\ShowroomCompanyFieldLabelContractTest.java`
- 如需要，再补充 `ShowroomHttpApiIntegrationTest.java` 中的公司历史回归用例
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-history-english-field-label-fix\**`

## Non-Scope

- 不混入当前工作区内其它 showroom / dcc / infra 在途改动。
- 不修改公司页面视觉或前端交互。
- 不放宽真正未知字段的异常语义。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-product-batch-publish-narration-source-fix\task.md`
- Status before this task: `Completed on 2026-05-23`
- Impact on this task:
  - 当前 worktree 中存在该任务及其它任务留下的未提交 showroom 改动；
  - 本次修复必须仅在理解现状后做最小增量，不能回滚或混提它们。

## Milestones

- [x] M1：创建任务文档并确认上一同仓任务状态。
- [x] M2：确认真实失败链路并补充 BDD 场景。
- [x] M3：先写失败回归测试，锁定公司英文字段标签合同。
- [x] M4：最小修复字段标签映射并验证 GREEN。
- [x] M5：回写证据、执行 cleanup 预览，并评估是否可安全提交当前任务文件。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomCompanyFieldLabelContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 如新增集成回归：`mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#..." "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-history-english-field-label-fix\execution-log.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260523-showroom-company-history-english-field-label-fix --mode preview`

## Current Status

Completed on 2026-05-23.

## Blockers

- 暂无；若真实失败链路依赖未提交的其它 showroom 改动，需要先记录精确影响范围。

## Current Findings

- `development_history_en` 不是非法字段；它已存在于 schema、持久化读写、前端 `contracts.ts` 以及现有集成测试数据中。
- 真正缺失的是 `ShowroomFieldDisplaySupport.fieldLabel("COMPANY", fieldCode)` 对公司 `_en` 字段的中文标签映射。
- 已通过合同测试先做 RED，再以最小改动补齐公司 `_en` 字段的中文标签，统一为 `xxx(英文)` 风格。
- 当前已验证：
  - `mvn -pl yudao-module-showroom "-Dtest=ShowroomCompanyFieldLabelContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomCompanyFieldLabelContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-history-english-field-label-fix\execution-log.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260523-showroom-company-history-english-field-label-fix --mode preview`

## Cleanup Keep

- `doc/tasks/20260523-showroom-company-history-english-field-label-fix/task.md`
- `doc/tasks/20260523-showroom-company-history-english-field-label-fix/execution-log.md`

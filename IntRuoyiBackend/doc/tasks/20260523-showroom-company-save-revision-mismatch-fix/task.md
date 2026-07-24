# 任务：修复公司信息保存时 narration source revision mismatch

## Goal

- 修复展厅公司信息保存时抛出 `SHOWROOM_TARGET_NOT_FOUND: live company ZH narration source revision mismatch` 的问题。
- 保持保存链路遵循现有发布数据约束，不引入 fallback、兼容分支或静默降级。
- 为该回归补充可复现的失败测试与通过验证。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-save-revision-mismatch-fix\**`

## Non-Scope

- 不修改 `yudao-module-infra/**` 的未完成 runtime control 面板任务产物。
- 不改动与本次公司信息保存错误无关的发布脚本、菜单或前端样式。
- 不引入 mock 成功、默认值兜底或 source revision 不一致时的隐式放过。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-infra-runtime-control-panel\task.md`
- Status before this task: `Blocked on final frontend regression and local menu application`
- Impact on this task: 该阻塞任务位于 `infra` 模块，当前 bug 修复仅限 `showroom` 模块，可并行处理，但不得混入其未完成改动。

## Milestones

- [x] M1：创建任务文档并确认上一同仓任务状态。
- [x] M2：补充 BDD 场景并复现保存报错。
- [x] M3：先写失败回归测试，确认 RED。
- [x] M4：最小修复保存链路并验证 GREEN。
- [x] M5：回写证据、执行 cleanup 预览，并评估是否可安全提交当前任务文件。

## Expected Verification

- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#updateCompanyPersistsNarrationWhenLiveNarrationPointsToPreviousRevision test`
- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-save-revision-mismatch-fix\execution-log.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260523-showroom-company-save-revision-mismatch-fix --mode preview`

## Current Status

Completed on 2026-05-23.

## Blockers

- 暂无；若本地 showroom 集成测试依赖缺失或历史脏数据导致无法复现，需立即记录精确前置条件与影响。

## Current Findings

- 根因位于 `ShowroomApiRuntime.publishCompany`：公司保存发布时只切换了 `company.currentRevisionId`，没有同步迁移当前 live 公司讲解版本。
- 当 live 公司讲解仍指向旧 `sourceRevisionId` 时，后续读取链路会把它判定为 `live company ... narration source revision mismatch` 并失败。
- 已补充公司发布回归测试，先确认 RED，再通过最小修复让 live 公司中英文讲解在保存后跟随到新 revision。
- 当前已验证：
  - `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#companyPublishShouldCarryForwardLiveNarrationsWhenSavingNewRevision test`
  - `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test`

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#companyPublishShouldCarryForwardLiveNarrationsWhenSavingNewRevision test`
- PASS: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test`
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-save-revision-mismatch-fix\execution-log.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260523-showroom-company-save-revision-mismatch-fix --mode preview`

## Cleanup Keep

- `doc/tasks/20260523-showroom-company-save-revision-mismatch-fix/task.md`
- `doc/tasks/20260523-showroom-company-save-revision-mismatch-fix/execution-log.md`

# 任务：展厅英译术语统一为 int-medical

## Goal

为展厅中英文翻译链路增加统一术语提示词：遇到中文 `瑛泰医疗` 时，英文必须统一翻译成 `int-medical`。该规则不仅作用于公司信息翻译，也必须作用于展厅产品信息与产品讲解稿翻译。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\narration\ShowroomCompanyNarrationTranslationService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\narration\ShowroomProductNarrationCodexService.java`
- 与本次术语提示词统一直接相关的 showroom 翻译单测
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-translation-int-medical-glossary\**`

## Non-Scope

- 不修改公司/产品翻译接口入参出参契约。
- 不改产品字段持久化、发布、音频生成或前端调用链路。
- 不引入 fallback、别名兜底或静默后处理替换。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-bilingual-tabs\task.md`
- Status before this task: `Blocked on 2026-05-21`
- Impact: 上一同仓任务已显式阻塞在模块编译放行，不影响本次仅针对 showroom 翻译 prompt 规则补充与定向单测收敛。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行中的 showroom 相关未提交改动。
- Impact: 本任务只允许修改翻译 prompt 相关 service、定向测试与本任务文档，不能覆盖产品双语、封面、审批等并行改动。

## Milestones

1. 建立任务文档并记录 BDD/TDD 场景。
2. 先补 RED，锁定公司翻译与产品翻译 prompt 都必须包含 `瑛泰医疗 -> int-medical` 术语规则。
3. 用最小后端改动统一 showroom translation prompt 规则。
4. 跑通定向测试、更新证据并执行 closeout preview。

## Expected Verification

- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomTranslationPromptGlossaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-translation-int-medical-glossary\backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-translation-int-medical-glossary\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-translation-int-medical-glossary --mode preview`

## Milestone Status

1. Completed. 已建立任务文档并记录 BDD/TDD 场景。
2. Completed. 已新增 `ShowroomTranslationPromptGlossaryTest`，并拿到 company/product prompt 双 RED 失败证据。
3. Completed. 已新增共享 glossary helper，并让公司翻译与产品翻译都复用 `瑛泰医疗 -> int-medical` 术语规则。
4. Completed. 已完成定向测试、证据校验与 closeout preview，任务可进入 task-scoped commit。

## Current Status

Completed on 2026-05-21.

## Final Verification

- PASS: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomTranslationPromptGlossaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-translation-int-medical-glossary\backend-api-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-translation-int-medical-glossary\bug-regression-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-translation-int-medical-glossary --mode preview`

## Remaining Blockers

- None.

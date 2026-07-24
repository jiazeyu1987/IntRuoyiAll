# 执行日志：展厅英译术语统一为 int-medical

BDD: 公司字段英译使用统一术语 -> Given 用户在展厅后台触发公司字段或公司讲解稿中文转英文 / When 中文内容中出现 `瑛泰医疗` / Then 翻译 prompt 必须显式要求统一翻译成 `int-medical`。

BDD: 产品字段与产品讲解稿英译使用统一术语 -> Given 用户在展厅后台触发产品名称、产品字段或产品讲解稿中文转英文 / When 中文内容中出现 `瑛泰医疗` / Then 翻译 prompt 必须显式要求统一翻译成 `int-medical`。

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomTranslationPromptGlossaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `ShowroomCompanyNarrationTranslationService` 与 `ShowroomProductNarrationCodexService` 的翻译 system prompt 均未包含 `瑛泰医疗 -> int-medical` 术语规则，新加单测在断言 prompt 内容时失败。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomTranslationPromptGlossaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 公司翻译与产品翻译 prompt 均显式要求把 `瑛泰医疗` 统一翻译成 `int-medical`，两条单测均通过。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-translation-int-medical-glossary\backend-api-evidence.md` -> PASS, backend evidence 满足 `Scope / Contract / Validation / BDD / RED / GREEN / Verification / Blockers` 校验契约。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-translation-int-medical-glossary\bug-regression-evidence.md` -> PASS, bug regression evidence 满足 `Bug / Expected / Reproduction / Root Cause / RED / GREEN / Verification / Blockers` 校验契约。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-translation-int-medical-glossary --mode preview` -> PASS, closeout preview 建议保留 `task.md` 与 `execution-log.md`，并把两份临时 evidence 文档作为可清理附属产物移除。

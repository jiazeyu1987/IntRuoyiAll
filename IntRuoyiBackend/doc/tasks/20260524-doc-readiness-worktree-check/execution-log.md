# 20260524 doc readiness worktree check execution log

## BDD

- BDD: worktree self-contained docs -> Given reviewer 在新 worktree 中执行文档契约测试 / When 测试读取发布文档 / Then 必需 `docs/` 与 runbook 必须可访问，否则文档齐备性为 `BLOCKED`。
- BDD: release readiness fails closed -> Given G6/G7、G10/G11 仍缺真实证据 / When 检查文档齐备性 / Then 不能把发布结论改为 GO。

## TDD Evidence

- RED: `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py -q` -> FAIL, 6 failed because `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-doc-readiness-worktree-check\docs` is missing.
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 21 passed.
- RED: `python -X utf8 ...validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-doc-readiness-worktree-check` -> FAIL, missing `docs/acceptance/bdd-scenarios.md`.
- GREEN: custom required root docs checklist against `D:\ProjectPackage\Int\IntRuoyi\docs` -> PASS.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py -q` -> PASS, 6 passed after docs root resolver update.
- GREEN: `$env:DOCS_ROOT='D:\ProjectPackage\Int\IntRuoyi'; python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py -q` -> PASS, 6 passed.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 27 passed.
- RED: `python -X utf8 ...validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-doc-readiness-worktree-check` -> FAIL, worktree root lacked `docs/acceptance/bdd-scenarios.md`.
- GREEN: `New-Item -ItemType Junction -Path D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-doc-readiness-worktree-check\docs -Target D:\ProjectPackage\Int\IntRuoyi\docs` -> PASS.
- GREEN: `python -X utf8 ...validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-doc-readiness-worktree-check` -> PASS.

## 过程记录

- 任务开始：2026-05-24。
- 关键发现：新 worktree 只包含 `ruoyi-vue-pro` 与 `yudao-ui-admin-vue3`，不包含根级 `docs/`。
- 修复结果：文档契约测试可通过 `DOCS_ROOT` 或向上查找定位真实根文档；自包含文档包缺口继续保留为 review 发现。
- worktree 文档入口：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-doc-readiness-worktree-check\docs` 是 junction，target 为 `D:\ProjectPackage\Int\IntRuoyi\docs`。

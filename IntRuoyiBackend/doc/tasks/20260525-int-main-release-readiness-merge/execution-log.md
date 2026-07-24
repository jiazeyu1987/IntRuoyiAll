# 执行日志：融合发布门禁验证工具到 int_main

BDD: 后端发布门禁工具进入 int_main -> Given reviewer 要在主分支复用 G6-G11 发布门禁, When 后端任务分支融合进 `int_main`, Then `script/release-readiness` 工具、模板和对应 pytest 契约测试必须存在并通过。
BDD: 前端无重复融合 -> Given 前端任务分支已经被 `int_main` 包含, When 执行融合检查, Then 不产生新的前端 merge commit 或重复改动。
BDD: 发布结论不被合并掩盖 -> Given G6-G11 仍缺真实登录、责任人、webhook 或高风险动作确认, When 合并后运行阻塞态验证, Then 工具必须返回 `BLOCKED` 且不得发送 webhook 或执行生产动作。

RED: `python -X utf8 -m pytest script\tests\test_release_readiness_g6_g7_tooling.py script\tests\test_release_readiness_g8_g9_contracts.py script\tests\test_release_readiness_g10_g11_contracts.py -q` -> FAIL, 当前 `int_main` 缺少 `script\tests\test_release_readiness_g6_g7_tooling.py`，证明发布门禁契约测试尚未融合。
GREEN: `git merge --no-ff --no-commit task/20260524-release-readiness-gates-dev` -> PASS, 后端自动合并无冲突并停在提交前检查状态。
GREEN: `git -C ..\yudao-ui-admin-vue3 merge-base --is-ancestor task/20260524-release-readiness-gates-dev int_main` -> PASS, 前端任务分支已是 `int_main` 祖先，无需重复合并。
GREEN: `python -X utf8 -m pytest script\tests\test_release_readiness_g6_g7_tooling.py script\tests\test_release_readiness_g8_g9_contracts.py script\tests\test_release_readiness_g10_g11_contracts.py script\tests\test_release_go_no_go_contract_docs.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 54 passed。
GREEN: `.\script\release-readiness\verify-g6-g7-playwright.ps1` -> PASS, 无真实证据时按预期返回 `G6/G7 BLOCKED`，exit 2。
GREEN: `.\script\release-readiness\validate-g8-g9-confirmations.ps1 -ConfirmationPath .\script\release-readiness\templates\g8-g9-confirmation.example.json` -> PASS, 模板证据按预期返回 `decision=BLOCKED`，exit 2。
GREEN: `.\script\release-readiness\validate-g10-g11-confirmations.ps1 -ConfirmationPath .\script\release-readiness\templates\g10-g11-confirmation.example.json` -> PASS, 模板证据按预期返回 `decision=BLOCKED`、`sendsWebhook=false`，exit 2。
GREEN: `rg -n "backup-ops\.ps1|Invoke-RestMethod|Invoke-WebRequest|docker compose|publish-int-ruoyi|Start-Process" script\release-readiness` -> PASS, no matches，发布门禁脚本目录未包含生产发布、回滚、恢复或 webhook 发送入口。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi` -> PASS。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-int-main-release-readiness-merge --mode preview` -> PASS, status ready，delete none，blocked none。
GREEN: `git reset --mixed HEAD` + targeted cleanup of prior merge residue -> PASS, 保留无关 OnlyOffice 工作区改动，恢复当前 `int_main` 的 EDHR 合并文件，避免提交错误删除。
GREEN: `git merge --no-ff --no-commit 059ca7015e` -> PASS, 在最新 `int_main` 上重新融合发布门禁提交，无冲突。
GREEN: `python -X utf8 -m pytest script\tests\test_release_readiness_g6_g7_tooling.py script\tests\test_release_readiness_g8_g9_contracts.py script\tests\test_release_readiness_g10_g11_contracts.py script\tests\test_release_go_no_go_contract_docs.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 最新 `int_main` 合并结果 54 passed。
GREEN: G6/G7、G8/G9、G10/G11 阻塞态验证在最新 `int_main` 合并结果上复跑 -> PASS, 均按预期 exit 2 且保持 `BLOCKED`。
GREEN: `git commit -m "合并: 发布门禁验证工具"` with `TDD_TASK_DIR=doc/tasks/20260525-int-main-release-readiness-merge` -> PASS, merge commit `fa935dfd3b`。
GREEN: release-readiness paired worktree cleanup -> PASS, `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-release-readiness-gates-dev` removed；backend/frontend task branches retained as safety references。

NOTE: `validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 返回缺少 `docs/acceptance/bdd-scenarios.md`；该校验脚本按项目根 `D:\ProjectPackage\Int\IntRuoyi` 的文档布局运行已通过，因此未作为本次后端仓库合并阻塞。

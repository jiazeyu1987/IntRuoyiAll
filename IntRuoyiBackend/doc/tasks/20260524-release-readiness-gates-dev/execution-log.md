# 20260524 release readiness gates dev execution log

## BDD

- BDD: G6/G7 real frontend path fails closed -> Given 正式服或测试服缺真实 Playwright 登录证据、样例文件前端路径证据或正式前端仍指向错误 backend / When reviewer 运行验证工具 / Then G6/G7 必须输出 `BLOCKED`，不得用 direct URL 或 API shortcut 代替真实前端路径。
- BDD: G8/G9 confirmation input fails closed -> Given rollback-app 或 restore-data 缺触发条件、目标选择规则、责任人批准、影响范围或验证证据 / When reviewer 校验确认输入 / Then G8/G9 必须输出 `BLOCKED`，不得执行实际回滚或恢复。
- BDD: G10/G11 confirmation input fails closed -> Given webhook、target、owner、发送证据或角色矩阵缺失 / When reviewer 校验告警和责任人确认 / Then G10/G11 必须输出 `BLOCKED`，不得把候选人或 pending channel 当作 GO。

## TDD Evidence

- RED: `python -X utf8 -m pytest script\tests\test_release_readiness_g6_g7_tooling.py -q` -> FAIL, G6/G7 validator did not exist.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_readiness_g6_g7_tooling.py -q` -> PASS, 4 passed.
- RED: `python -X utf8 -m pytest script\tests\test_release_readiness_g8_g9_contracts.py -q` -> FAIL, G8/G9 validator missing and later accepted failed status/code.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_readiness_g8_g9_contracts.py -q` -> PASS, 7 passed.
- RED: `python -X utf8 -m pytest script\tests\test_release_readiness_g10_g11_contracts.py -q` -> FAIL, G10/G11 validator missing and later accepted example/placeholder inputs.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_readiness_g10_g11_contracts.py -q` -> PASS, 11 passed.
- Worker A RED: `python -X utf8 -m pytest script\tests\test_release_readiness_g6_g7_tooling.py -q` -> FAIL, 4 failed because `verify-g6-g7-playwright.ps1` did not exist.
- Worker A GREEN: `python -X utf8 -m pytest script\tests\test_release_readiness_g6_g7_tooling.py -q` -> PASS, 4 passed.
- Worker B RED: `python -X utf8 -m pytest script\tests\test_release_readiness_g8_g9_contracts.py -q` -> FAIL, 5 failed because the G8/G9 validator was not implemented.
- Worker B GREEN: `python -X utf8 -m pytest script\tests\test_release_readiness_g8_g9_contracts.py -q` -> PASS, 5 passed.
- Reviewer RED for Worker B: same command -> FAIL, 2 failed because failed status/code were accepted.
- Worker B REVIEW GREEN: same command -> PASS, 7 passed.
- Worker C RED: `python -X utf8 -m pytest script\tests\test_release_readiness_g10_g11_contracts.py -q` -> FAIL, 7 failed because the G10/G11 validator and template were not implemented.
- Worker C GREEN: same command -> PASS, 7 passed.
- Reviewer RED for Worker C: same command -> FAIL, 3 failed because example webhook URLs and placeholder tokens were accepted, and prodOwnerCandidates blocked fully approved roles.
- Worker C REVIEW GREEN: same command -> PASS, 11 passed.
- REGRESSION: `python -X utf8 -m pytest script\tests\test_release_readiness_g6_g7_tooling.py script\tests\test_release_readiness_g8_g9_contracts.py script\tests\test_release_readiness_g10_g11_contracts.py script\tests\test_release_go_no_go_contract_docs.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 50 passed.
- REGRESSION: `python -X utf8 ...validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-release-readiness-gates-dev` -> PASS.
- REVIEW: `rg -n "backup-ops\.ps1|Invoke-RestMethod|Invoke-WebRequest|docker compose|publish-int-ruoyi|Start-Process|Remove-Item|Set-Content|Out-File" script\release-readiness` -> PASS, no matches.

## 过程记录

- 任务开始：2026-05-24。
- 主 reviewer 退回 Worker B 一次，要求强制 success status 和 `INTBK-0000` code。
- 主 reviewer 退回 Worker C 一次，要求拒绝 example webhook/placeholder token，并修正候选责任人语义。
- 当前工具开发已放行；正式发布仍保持 `BLOCKED`。

# Execution Log

## Intent

- 用户请求：把本地芋道源码的生产用料清单同步到测试服务器。
- 任务解释：同步本地源码中相关功能到测试服务器 `172.30.30.58`，默认不执行业务数据同步。

## BDD

- BDD: 测试服务器源码同步 -> Given 本地源码包含生产用料清单相关实现，When 执行授权的测试服同步/发布流程，Then 测试服务器前端可访问且后端健康检查通过，并能承载当前源码版本。

## Milestone Log

- 2026-07-31：读取 `docs/server-access.md`、`docs/release-backup-restore.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md` 与 CI/CD skill 指南。
- 2026-07-31：确认当前分支 `int_main`，`origin` 为 `https://github.com/jiazeyu1987/IntRuoyiAll.git`，当前状态为 `ahead 18` 且存在既有脏改动，需记录为发布前风险边界。
- 2026-07-31：读取测试服发布门禁、发布构建经验、数据库规则、worktree 限制、分支端口矩阵、后端规则和前端规则。
- 2026-07-31：定位生产用料清单源码链路：`20260613_erp_production_material_list_menu.sql`、`20260613_mes_kingdee_production_material_list.sql`、`KingdeeProductionMaterialListSyncJob`、`MesKingdeeProductionMaterialList*`、前端 `/erp/production-material-list` API 与相关列表页。
- 2026-07-31：创建本轮干净 release worktree `D:\IntRuoyiWorktree\pml-test-r260731`，冻结提交 `363a887f03200bf58c6e8c649b8805c0fe66b06b`；`git status --porcelain --ignored=no` 无输出，说明发布输入 clean。
- 2026-08-01：`release-20260731-production-material-list-test-r260731pml-r1` 构建在 v1 manifest 生成阶段失败；本 tag 标记为 rejected，不用于测试服部署。
- 2026-08-01：在 release worktree 创建 `codex/sync-production-bom-test-r260731pml`，修复发布脚本 `Get-ReleaseObjectPropertyText` 以支持 `[ordered]` sourceRepos 条目，并提交 `f95edbb88`。

## Verification Evidence

- GREEN: experience-preflight -> PASS，已按测试服发布、code-only、worktree、服务器访问、发布备份恢复、PowerShell/Git、数据库、后端和前端门禁建立任务证据。
- RED: build-release-r1 -> FAIL，`[FAIL] Release source repo entry must include pathRole or name before git change comparison`；本地包缺少严格 v1 `manifest.json`，禁止部署。
- RED: python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q -k ordered_dictionary_manifest_entries -> FAIL，复现 `[ordered]` sourceRepos 条目无法通过 `pathRole` 识别。
- GREEN: python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q -> PASS，103 passed。
- GREEN: python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260731-sync-production-bom-test\bug-regression-evidence.md -> PASS。
- RED: git push -u origin codex/sync-production-bom-test-r260731pml -> FAIL，`Failed to connect to github.com port 443 via 127.0.0.1`。
- RED: git -c http.https://github.com.proxy= ls-remote origin HEAD -> FAIL，`Recv failure: Connection was reset`。
- RED: ssh -T -o BatchMode=yes git@ssh.github.com -p 443 -> FAIL，`Permission denied (publickey)`。

## Blockers

- GitHub push 前置阻塞：全局 GitHub HTTPS proxy `http://127.0.0.1:7890` 未监听，直连 HTTPS 被 reset，SSH 443 未授权；修复提交尚未推送，按项目规则不得继续部署测试服。

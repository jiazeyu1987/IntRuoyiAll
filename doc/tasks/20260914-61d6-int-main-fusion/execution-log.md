# Execution Log

## Rebase Integration Verification

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，10 tests；DCC 153 tests、server 4 tests 的最新 PASS 结果保持有效。
- GREEN: 合并后保留主分支运行配置静态断言并补充 Java 参数数组校验，`node scripts/tests/start-branch-backend-dcc-encryption-static.spec.cjs` -> PASS。
- GIT: 检查本任务 Git 目录的零字节 index.lock 超过两分钟且无 git.exe 进程后，清除失效锁并暂存本任务文件；未处理其他 worktree 的锁。
- CLOSEOUT: 状态设为 ready_for_closeout。用户任务归属规则明确并行无关改动不阻止完成；清理脚本将使用 `--worktree-closeout off` 限定文件清理，随后单独核对主线脏文件与本任务 diff 不重叠再执行已授权的 ff-only 融合。

- REGRESSION: 最新 DCC 定向回归 -> PASS，131 query + 22 route tests；MES 10 tests 中冻结测试仍使用另一分支旧动作名 `PQC更正`，mock 未命中而失败。统一为合并后动作名 `PQC检验更正`，保留错误码和禁止写入断言，重跑 MES 定向回归。

- BDD: 图纸缺少当前 PDF -> Given 工作版本源件为图纸且配套 PDF 为空 When 请求预览元数据 Then 抛出 CONTROLLED_FILE_DRAWING_PDF_REQUIRED，且不开放源件预览。
- RED: DCC/MES 定向 Maven 回归 -> FAIL，153 项 DCC 测试中 1 项失败：`getPreviewMetadata_workingDrawingWithoutCurrentPdfRejectsBeforeSourcePreview` 未抛异常；保留主分支的缺失 PDF 显式校验后重跑。
- GREEN: `mvn -pl yudao-server "-Dtest=UploadMultipartLimitConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests，冲突后重新验证。

- COMMIT: 原实现提交 `aab08ef0d7447bb0b5996b85ccb7dc26e4d1e904`，随后 rebase 到 `453207c784da92511894db079aa9bc48c6bf848a`，逐项合并冲突。下载记录保持主分支 `download_status` schema；合并图纸配套 PDF、直接下载审计、重复审批阶段及 PQC 冻结校验测试。
- RED: `mvn -pl yudao-module-dcc,yudao-module-mes -am "-Dtest=DccControlledFileQueryServiceTest,DccApprovalRouteAdminServiceImplTest,MesProcessPoolPqcInspectionCorrectionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，冲突拼接留下 copyForCheckin 参数不匹配及 allowed 重复声明；修复后重新验证。
- GREEN: 冲突解决后重跑三项 Python 脚本测试，显式 `--basetemp .pytest-tmp/20260914-61d6` -> PASS，154 tests。
- GREEN: 下载运行配置 PowerShell / Node 合同、前端 API 合同 10 tests、检入流程 5 tests、E2E 脚本静态合同与语法检查、`pnpm ts:check` -> PASS；未执行真实 E2E。
- OBSERVED: rebase 中 HEAD detached，端口门禁取分支名称失败；将在 rebase 完成后以实际分支重新执行，不把此次运行记为 PASS。
- OBSERVED: 主工作区存在并行任务文档改动，遵守用户任务归属规则，只核对本任务合并文件是否重叠，不纳入本任务提交。

BDD: 提交并融合当前 worktree -> Given 当前 `61d6` worktree 含待提交改动且主分支为 `int_main` When 创建任务分支、完成提交前验证并提交 Then `int_main` 能以 fast-forward 方式包含本任务提交且工作区保持干净。

## 2026-09-14

- READ: `AGENTS.md` 指令、`docs/task-closeout-rules.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`。
- READ: `task-closeout-cleanup` 与 `project-experience-consolidation` 技能说明。
- OBSERVED: 当前 worktree 为 detached HEAD，基线提交 `b4303b4ed`；`int_main` 当前 `bc640fd96`。
- OBSERVED: 当前 worktree 含多项前后端、脚本、测试与文档改动，需先建立分支再提交。
- OBSERVED: 当前分支已为 `codex/20260914-61d6-int-main-fusion`；`int_main` 位于 `53f969d59`，`origin/int_main` 位于 `6002b84fc`，本分支基于 `b4303b4ed`。
- REGRESSION: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，当前分支/int_main profile 为 frontend `8160`、backend `48160`。
- REGRESSION: `git diff --check` -> PASS，仅输出 CRLF 工作区提示，无 whitespace error。
- REGRESSION: `pytest IntRuoyiBackend\script\tests\test_publish_int_ruoyi_to_test_tooling.py IntRuoyiBackend\script\tests\test_restart_int_ruoyi_local_schema.py IntRuoyiBackend\script\tests\test_runtime_control_scripts.py` -> FAIL，pytest setup 阶段无法访问默认 `%TEMP%\pytest-of-BJB110`，测试断言未执行失败。
- GREEN: `pytest --basetemp .pytest-tmp\20260914-61d6 IntRuoyiBackend\script\tests\test_publish_int_ruoyi_to_test_tooling.py IntRuoyiBackend\script\tests\test_restart_int_ruoyi_local_schema.py IntRuoyiBackend\script\tests\test_runtime_control_scripts.py` -> PASS，154 passed。
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\tests\test_dcc_download_encryption_runtime_config.ps1` -> PASS。
- GREEN: `node scripts\tests\start-branch-backend-dcc-encryption-static.spec.cjs` -> PASS。
- GREEN: `node scripts\dcc-frontend-api-fail-closed-contract.test.mjs`、`node src\views\dcc\controlled-file\browser\checkin-main-flow.spec.cjs`、`node tests\e2e\dcc-controlled-file-protection.contract.test.js`、`node --check tests\e2e\dcc-controlled-file-protection.e2e.js` -> PASS，DCC 前端静态/语法合同通过。
- GREEN: `pnpm ts:check` -> PASS。
- REGRESSION: `mvn -pl yudao-module-dcc,yudao-module-mes,yudao-server -am "-Dtest=..." -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，PowerShell 将未加引号的 `-Dsurefire...` 拆分为 Maven lifecycle phase。
- REGRESSION: `mvn -pl yudao-module-dcc,yudao-module-mes,yudao-server -am "-Dtest=..." "-Dsurefire.failIfNoSpecifiedTests=false" test` -> DCC 346 tests PASS，MES 19 tests PASS；yudao-server 在既有 `maven-dependency-plugin:unpack` reactor 生命周期处阻断，未进入 server Surefire。
- GREEN: `mvn -pl yudao-server "-Dtest=cn.iocoder.yudao.server.UploadMultipartLimitConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests。
- EXPERIENCE: 按 `project-experience-consolidation` 合并到既有 `docs/powershell-memory.md` 与 `docs/experience-index.md`，记录 pytest basetemp 与 yudao-server reactor unpack 测试门禁。

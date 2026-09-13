# Execution Log

## Scope

- Task: DCC-STATIC-027 剩余部分，仅处理清空 `fileNumber` 后旧身份索引未清除的问题。
- Source: `docs/bugs/20260912-dcc-90-step-static-audit.md`。
- Baseline: 本任务从 `origin/int_main` = `49a6ec8b20670fec0862849519fce016c787cc07` 的干净 detached HEAD 开始；后续创建任务分支 `codex/20260914-dcc-static-027-clear-file-number-identity`。

## Rule Reads

- Read: `docs/task-closeout-rules.md`
- Read: `docs/backend-development.md`
- Read: `docs/test-release-preflight.md`
- Read: `docs/worktree-restrictions.md`
- Read: `docs/powershell-encoding.md`
- Skill: `bug-regression-fix-loop`
- Skill reference: `bug-regression-fix-loop/references/bug-contract.md`
- Skill: `task-closeout-cleanup`
- Skill reference: `task-closeout-cleanup/references/closeout-rules.md`

## BDD / TDD Evidence

BDD: DCC-STATIC-027 clear file number identity -> Given 已发布正式文件的 Master 保存旧 `fileNumber` 和 `normalizedFileNumber`，When 文控通过基础信息修改将文件编号清空或改为空白，Then 当前文件行和 Master 权威身份均清空编号投影，旧编号查询不得命中新文件，旧编号不再占用冲突检查。

BDD: DCC-STATIC-027 rename file number identity still works -> Given 已发布正式文件编号为 OLD，When 文控通过基础信息修改将编号改为 NEW，Then Master 权威身份同步为 NEW，NEW 可命中当前版本，OLD 不得命中且不得占用冲突检查。

RED: `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-027-clear-file-number-identity-contract.spec.cjs` -> FAIL, 旧实现仍通过 MyBatis Plus `updateById` 更新 Master；静态合同在 `metadata update must not clear nullable Master identity fields through MyBatis Plus updateById` 断言处失败。

GREEN: `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-027-clear-file-number-identity-contract.spec.cjs` -> PASS, `DCC-STATIC-027 clear file number identity contract passed`。

GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileMetadataUpdateServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 18 tests run, 0 failures, 0 errors, BUILD SUCCESS。

REGRESSION: `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileMetadataUpdateControllerTest,cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowServiceImplTest#getCurrentVersionByFileNumber_rejectsMasterActiveFileIdentityMismatch" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests run, 0 failures, 0 errors, BUILD SUCCESS。

STATIC: `git diff --check` -> PASS, no whitespace errors; only touched Java files' LF/CRLF warnings were reported.

## Notes

- `DccControlledFileMetadataUpdateControllerTest` 已存在断言：`fileNumber` 没有 `@NotBlank`，因为不是每个受控文件都有编号。因此本任务按“允许清空编号”路径修复，不改为拒绝清空。

## Implementation

- `DccControlledFileMasterMapper` 新增 `updateMetadataIdentity(...)` 显式 SQL，逐字段写入 `file_number`、`normalized_file_number` 及元数据身份投影，允许空白编号对应的 `normalizedFileNumber=null` 真正写入数据库。
- `DccControlledFileMetadataUpdateServiceImpl` 改为调用该显式更新，并要求返回行数为 1；否则抛出既有业务冲突异常，不静默成功或降级。
- 服务单测改为验证显式 Mapper 调用，覆盖普通改编号、清空编号、分类/目录失败和 Master 更新失败路径。
- 新增正式 `src/test/js` 静态合同，验证“允许清空编号”边界、显式 Mapper 方法和关键 SQL 字段。

## Closeout

- 经验归宿检查：已有 `docs/backend-development.md#MyBatis Plus 空值更新门禁`，无需新建长期经验文档；本任务仅在任务记录中保留具体证据。
- cleanup preview：已执行并返回 BLOCKED；keep 为 `task.md`、`execution-log.md`、`verification-report.md`，delete 为空；因当前 worktree 为 detached HEAD，脚本无法解析当前分支。正式静态合同位于 `src/test/js`，不得清理。
- Git implementation commit（rebase 前）：`4cd77e18a`；rebase 到当前 `int_main` 后为 `207600902`，message 为 `fix: clear DCC file number identity projection`；仅包含 3 个生产/Java 测试文件和 1 个正式 `src/test/js` 静态合同。
- Git closeout commit（rebase 前）：`d0feb6002`；rebase 到当前 `int_main` 后为 `6d337c286`。
- Git push：已推送旧基线；rebase 后需使用 `--force-with-lease` 更新任务分支远端。
- 任务分支 runtime profile：已登记为 `int_main` slot 18，前端 8099、后端 48099，提交钩子已通过。
- cleanup preview（rebase 前）：BLOCKED；keep 为三份任务记录，delete 为空；阻塞原因为任务分支不能 fast-forward 到本地 `int_main`，且 `E:\IntRuoyi` 主工作区存在其他并行未提交改动。
- rebase：PASS；任务分支已基于本地 `int_main` 重放，`git merge-base --is-ancestor int_main HEAD` 通过。
- cleanup apply、主分支合并、worktree 删除：待保护主工作区并行改动后执行。
- 任务状态：`ready_for_closeout`。
- E2E、服务启动/重启、数据库写入、远程服务器操作：未执行。

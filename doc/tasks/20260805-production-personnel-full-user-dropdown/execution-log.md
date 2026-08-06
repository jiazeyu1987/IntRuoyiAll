# Execution Log

## 2026-08-05

- User intent: 将生产组长新增人员中的正式工下拉改为全量系统用户。
- Scope: 后端正式工候选搜索与关联校验；前端远程搜索接口和交互保持不变。
- BDD: 全量用户姓名搜索 -> Given 当前生产组长负责部门之外存在昵称匹配的有效系统用户；When 组长在正式工姓名下拉输入关键字；Then 候选接口返回该用户且最多返回 20 条匹配结果。
- BDD: 跨部门正式工关联 -> Given 候选用户是有效系统用户但不属于当前组长负责部门；When 组长提交关联；Then 创建当前组长的正式工档案并保留重复关联与显示名唯一校验。
- BDD: 空关键字校验 -> Given 搜索关键字为空白；When 请求正式工候选；Then 返回空候选，不执行全量无条件扫描。
- BDD: 权限校验 -> Given 登录用户缺少维护权限；When 调用正式工候选或关联接口；Then 由现有 Controller 权限注解拒绝请求。
- BDD: 无效系统用户失败 -> Given 提交不存在或不可用的系统用户编号；When 关联正式工；Then 沿用系统用户校验错误，不创建人员档案且不返回默认成功。
- BDD: 重复关联失败 -> Given 当前组长已关联同一系统用户；When 再次提交关联；Then 在数据库写入前返回正式工重复关联业务错误。

## Command Intent

- 读取现有前后端调用链和系统用户 API，确认最小正式数据源变更。
- 在修改生产代码前新增并运行目标失败测试。
- 实现后运行目标测试、相邻 Controller 回归、证据校验和差异检查。

## RED / GREEN Evidence

- RED: `mvn -pl yudao-module-system,yudao-module-mes -am "-Dtest=AdminUserApiImplPostIdsTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`AdminUserApiImpl` 缺少 `getUserListByNickname(String)`，符合全量用户正式 API 尚未实现的预期。
- GREEN attempt: 合并运行 system + MES Reactor 超时；任务自有 Maven PID `31152` 的 `jcmd Thread.print` 显示卡在 `JavacFileManager.close -> ZipFileSystem.close -> WindowsPath.toRealPath`，停止的仅为该任务 PID `31152` 及父 `cmd` PID `19692`，未停止其它 Maven/Java 进程。
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=AdminUserApiImplPostIdsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2。
- REGRESSION discovery: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before MES Surefire because BPM 测试手写 `RecordingAdminUserApi` 未实现新增正式接口；已按编译契约补齐显式 `UnsupportedOperationException`，未增加默认空列表 fallback。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 13。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 24。
- CONTRACT: task-owned `git diff --check` -> PASS。
- EVIDENCE: 首次直接启动 validator 超时后对应任务进程已自行退出；使用同一 Python 解释器加 `-S` 重跑 `validate_backend_api.py --evidence doc/tasks/20260805-production-personnel-full-user-dropdown/backend-api-evidence.md` -> PASS，输出 `Backend API evidence is valid.`。

## Baseline

- Baseline commit: `3db8a7030 chore: preserve dirty worktree baseline`.
- Baseline scope: 39 个既有前端、静态测试和并行任务文档文件；本任务目录未进入基线提交。
- Baseline check: `git show --name-status --oneline -1` 已核对；提交后并行任务继续产生新的非本任务改动，本任务后续只选择性暂存后端代码、后端测试和本任务记录。
- Baseline diff note: 既有生产人员重复错误任务的 3 个 Markdown 文件存在 EOF 空行告警；为保留用户原始脏改动，基线提交未改写这些非本任务文件。

## Shared Branch Concurrency

- Concurrent commit: `633361dde chore: baseline pre-existing worktree changes` 在 Maven 验证期间纳入了本任务全部后端实现、回归测试和初始任务记录。
- Verification: `git show --name-status 633361dde -- <task-owned paths>` 已确认 7 个后端源码/测试文件和 3 个任务文件均在该提交中；后续 `origin/int_main` 已包含该提交。
- Boundary: 不重写、不 amend、不 reset 该并行提交；本任务仅继续提交经验、验证报告和 closeout 记录。
- Pre-commit index: 另一个并发任务的 `doc/tasks/20260805-production-leader-process-config-unification/` 文件已在暂存区；本任务不移出、不修改这些暂存项，使用显式任务路径和 `git commit --only` 保持提交边界。

## Experience And Cleanup

- EXPERIENCE: 已按 `project-experience-consolidation` 将“候选范围与关联校验范围必须一致、禁止前端全量过滤和默认空列表兼容 fallback”合并到现有 `docs/backend-development.md`，并更新 `docs/experience-index.md`；未新建长期经验文档。
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260805-production-personnel-full-user-dropdown --mode preview` -> PASS；保留 `task.md`、`execution-log.md`、`verification-report.md`，仅计划删除 `backend-api-evidence.md`，无 blocked/warnings。
- CLEANUP APPLY: `task_closeout.py --task-id 20260805-production-personnel-full-user-dropdown --mode apply` -> PASS；仅删除 `backend-api-evidence.md`，三个核心任务记录均保留，无 blocked/warnings。

## Git Closeout Blocker

- `git add -- <本任务 6 个显式路径>` -> FAIL：`Unable to create 'E:/IntRuoyi/.git/index.lock': File exists`。
- 首次检查时存在并发 `git commit -m "docs: initialize production leader process config task"`，因此未删除锁、未停止对方 Git 进程。
- 等待并发 commit 与后续 worktree-add 进程退出后，`E:\IntRuoyi\.git\index.lock` 仍存在，长度为 `1,441,792` 字节，最后写入时间未继续变化。
- 项目陈旧锁恢复门禁只允许删除“零字节、超过 60 秒、无活动 Git 进程”的精确锁文件；本锁非空，因此 fail fast，不删除、不覆盖、不使用备用 index 绕过。
- Impact: 功能实现与测试已由 `633361dde` 进入 `origin/int_main`；本任务 cleanup/经验/验证收尾记录尚未形成最终提交和推送，M4 保持未完成。

## 2026-08-06 Runtime Regression

- User evidence: 截图显示“新增人员 > 正式工姓名”输入 `陈` 后下拉为 `No data`。
- RED: 本机登录态只读请求 `GET /mes/pro/process-pool/team-leader/employee-profile/formal-candidates?keyword=陈` -> `code=0,count=0`。
- Control: 同一登录态请求 `/system/user/simple-list` 后本地过滤昵称、账号或手机号包含 `陈` 的用户 -> 89 条，示例包含 `陈秀丽`、`陈红艳`、`陈家傲`。
- Root cause: 当前 48081 运行包 `backend-runtime-control-acm04-pqc-source-context-20260805.jar` 及现有人员 hotpatch 包的嵌套 `yudao-module-mes/system` class 均不包含 `getUserListByNickname` 常量，仍包含 `getUserListBySubordinate`；源码已修复但运行态未刷新。

# Execution Log

## 任务范围

- 用户需求：提交推送前后端代码，然后启动前后端；如果有错误先修复。
- 仓库：`E:\IntRuoyi`
- 当前分支：`int_main`
- 任务目录：`doc/tasks/20260919-submit-push-start-local-runtime/`

## 规则与前置检查

- 已读取：`AGENTS.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/local-runtime.md`、`docs/task-closeout-rules.md`、`docs/test-release-preflight.md`、`docs/release-agent-checklist.md`、`docs/branch-runtime-ports.md`、`docs/worktree-restrictions.md`、`docs/experience-index.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`。
- 已确认当前分支为 `int_main`，远端为 `origin`。
- 当前分支在执行任务前相对 `origin/int_main` 已领先 10 个提交。
- 当前工作区已有后端、前端和 `docs/release-backup-restore.md` 未提交改动；`intrruoyi-runtime/worktree-ports.json` 是本机端口登记文件，不属于代码提交范围。
- 当前 `8081`、`48081` 无监听进程。

## BDD / TDD 记录

- 当前任务不新增产品行为；代码验证以现有后端回归、前端类型检查、前端构建和启动健康检查为主。
- 若发现并修复代码错误，在此处追加精确的 `BDD:`、`RED:`、`GREEN:` 和回归证据。
- `RED: python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_backup_ops_linux_runtime_ports.py -q -> FAIL, pytest 无法访问默认 Windows 临时目录，未进入测试逻辑。`
- 环境原因：`PermissionError: [WinError 5]` on `C:\Users\BJB110\AppData\Local\Temp\pytest-of-BJB110`。
- 处理边界：使用本任务目录下的独立 pytest basetemp 重跑，不修改产品代码、不降低测试范围。

## 里程碑记录

### 里程碑 1：范围与基线

- 状态：完成。
- 证据：任务目录已创建；规则、分支、远端、当前改动和端口状态已核对。

### 里程碑 2：验证与修复

- 状态：完成。
- `RED: python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_backup_ops_linux_runtime_ports.py -q -> FAIL, 默认 pytest 临时目录权限拒绝，未进入测试逻辑。`
- `GREEN: python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_backup_ops_linux_runtime_ports.py -q --basetemp E:\IntRuoyi\doc\tasks\20260919-submit-push-start-local-runtime\tmp-pytest -> PASS, 45 passed in 32.42s`
- `GREEN: mvn -pl yudao-module-infra -am test "-Dtest=RuntimeRestoreCandidateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -> PASS, 16 tests, BUILD SUCCESS`
- `GREEN: pnpm ts:check -> PASS`
- `GREEN: pnpm build:local -> PASS`
- `GREEN: pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1 -> PASS`
- `GREEN: git diff --check -> PASS`
- 合并远端改动后再次验证：
  - `GREEN: pnpm ts:check -> PASS`
  - `GREEN: mvn -pl yudao-server -am "-DskipTests" package -> PASS, 31 个模块全部 SUCCESS，yudao-server-exec.jar 已生成，BUILD SUCCESS`
- 未发现需要修改的产品代码错误；仅使用任务专用 pytest 临时目录解决 Windows 测试环境权限问题。
- 提交前复扫发现并行未暂存改动新增于：
  - `IntRuoyiBackend/script/backup-ops/scripts/modules/UseCases/Rehearsal.psm1`
  - `IntRuoyiBackend/script/tests/test_backup_ops_linux_runtime_rehearsal_tooling.py`
  - `IntRuoyiBackend/yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeControlOperationActionBackupConfirmTest.java`
  - `IntRuoyiBackend/yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeControlServiceImplTest.java`
- 上述并行改动未进入本次暂存区，保持在工作区，不回滚、不覆盖、不混入本次提交。

### 里程碑 3：提交与推送

- 状态：完成；本次实现已提交、合并并推送。
- 代码提交：`db8e6c8fc`，提交信息 `feat: harden backup restore candidate governance`。
- 提交文件：12 个后端、前端和 `docs/release-backup-restore.md` 文件；暂存清单已核对，无本机端口登记文件。
- 提交钩子再次通过 `scripts\preflight\branch-runtime-port-guard.ps1`。
- 本次 pytest 环境经验已归入既有 `docs/powershell-memory.md#pytest-任务自有-basetemp-门禁`，未新增长期经验文档。
- 提交后复扫：并行改动仍未提交，`intrruoyi-runtime/worktree-ports.json` 仍为未跟踪本机运行态文件。
- 推送预检：`git ls-remote origin HEAD` 可达；待推送历史无超过 100 MB 的 blob。
- `git push origin int_main` 首次被远端拒绝：`fetch first`，远端 `int_main` 已包含本地未包含的提交。未执行强推、reset 或历史重写；先获取远端并检查分叉。
- 远端获取使用 `git -c maintenance.auto=false -c gc.auto=0 fetch --no-tags origin +refs/heads/int_main:refs/remotes/origin/int_main` 成功，远端从 `5fc6e7a08` 更新至 `cc0770cba`。
- `merge-base` 为 `5fc6e7a08`；远端 5 个提交与本地 `db8e6c8fc` 分叉，改动存在运行控制台前后端文件重叠。
- `git merge --no-commit origin/int_main` 自动合并成功，无冲突；远端改动已暂存等待 merge commit，并行未提交改动保持未暂存。
- 合并提交：`bb7d0b33a`，提交信息 `Merge origin/int_main into int_main`，提交钩子端口门禁通过。
- `git push origin int_main -> PASS`，远端从 `cc0770cba` 更新至 `bb7d0b33a`。
- 推送完成后另一作者在本地创建 `a04a9143f`（`任务: 完善备份恢复证据绑定`），包含 6 个并行备份恢复文件；该提交不属于本任务，未被本任务推送或修改。

### 里程碑 4：启动与运行态验证

- 状态：完成。
- 标准命令：`pwsh -NoProfile -File .\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -> PASS`，完成 31 模块打包并派发前后端启动。
- `GREEN: http://127.0.0.1:8081/ -> HTTP 200`
- `GREEN: http://127.0.0.1:48081/actuator/health -> status=UP`
- 前端监听 PID `41104`，后端监听 PID `8288`；后端启动日志包含 `Started YudaoServerApplication`。
- 后端运行 Jar 为 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260919-120334.jar`，SHA-256 为 `7043D347CAA51C348C9D01F28CD5CA04F42D74CC2F9997A8AFF6EB98D84DECA1`，且 Jar 修改时间早于进程启动时间。
- 前端首次探测期间 Vite 进行了依赖优化，短暂未返回 200；等待服务就绪后复测通过，无产品代码错误。

### 里程碑 5：收尾

- 状态：完成。
- 已先将 `task.md` 标记为 `ready_for_closeout`。
- cleanup preview：通过；保留 `task.md`、`execution-log.md`、`verification-report.md`，仅删除任务专用 `tmp-pytest/`。
- cleanup apply：通过；任务专用 `tmp-pytest/` 已删除。
- cleanup 后已将 `task.md` 标记为 `completed`。

## 提交证据

- 基线提交：任务开始时本地 `HEAD=2d7b0997f`，`origin/int_main=5fc6e7a08`。
- 实现提交：`db8e6c8fc`，已完成前后端实现提交。
- merge 提交：`bb7d0b33a`，已推送至 `origin/int_main`。
- 收尾提交：已创建，提交信息 `docs: close out local runtime submission task`；当前分支在本任务推送后被并行提交 `a04a9143f` 追加，未在未获授权情况下继续推送该并行提交。

## 验证证据

- 后端：目标测试 16 项通过；合并后标准包构建 31 个模块全部成功。
- 前端：`pnpm ts:check`、`pnpm build:local` 通过；合并后 `pnpm ts:check` 再次通过。
- 端口门禁：提交钩子及手动检查均通过。
- 推送：`git push origin int_main -> PASS`，推送时远端为 `bb7d0b33a`。
- 运行态：8081 HTTP 200；48081 health `UP`；PID 和运行 Jar 已核对。
- cleanup preview/apply：均通过；核心任务记录保留，任务专用临时产物已删除。

## 阻塞项

- 本任务代码无阻塞项。
- 并行提交 `a04a9143f` 在本任务推送后出现，未纳入本任务推送；当前本地分支相对远端因此额外领先 1 个并行提交。

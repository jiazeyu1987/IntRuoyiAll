# Execution Log

## User Intent

- 用户要求：重启前后端。
- 执行口径：仅重启当前 `E:\IntRuoyi` 的 `int_main` 本地前端 `8081` 和后端 `48081`，不触碰其他 profile、worktree 或共享依赖。

## Preflight Evidence

- 已读取：`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/branch-runtime-ports.md`。
- 已读取：`docs/database-rules.md`、`docs/experience-index.md`，并核对 tokenless Runner 和本地重启相关既有门禁。
- 端口预检：`8081` 由 PID `21760` 的本工作区 Vite 进程监听；`48081` 由 PID `44100` 的 `E:\IntRuoyi\output\runtime\int_main` 后端 Jar 监听。
- 依赖预检：`int-ruoyi-mysql`、`docker-minio-1` 均处于运行状态；`127.0.0.2:23306` 和 `127.0.0.2:26379` 可达；DCC 下载加密所需环境变量已存在，未记录其值。
- 重启前状态：`show-int-ruoyi-local-status.ps1 -Component full -Json` 返回前端 HTTP `200`、后端 HTTP `200`。

## BDD

- `BDD: 本地前后端重启后可访问 -> Given 当前 int_main 端口由本工作区旧进程占用, When 停止旧进程并按标准配置启动前后端, Then 8081 返回 HTTP 200 且 48081 health 状态为 UP`

## RED/GREEN Evidence

- `RED: N/A -> N/A, 本任务未修改生产代码或产品行为；重启流程中旧 48081 监听已被替换。`
- `GREEN: E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -> PASS, Maven reactor BUILD SUCCESS 后完成前后端启动分派。`

## Execution Evidence

- 标准本地重启脚本的 PowerShell parser -> PASS。
- `mvn -pl yudao-server -am -DskipTests package` -> PASS，`BUILD SUCCESS`，完成于 `2026-08-07T08:23:13+08:00`。
- 新运行 Jar：`E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-082313.jar`。
- 新运行 Jar SHA256：`A44E10178B4C2F5A428E153E3399F81BA800970BA154990D16263CFBFDB1B9F6`。
- 新后端监听：PID `38500`，`48081`，运行 Jar 位于 `E:\IntRuoyi\output\runtime\int_main`。
- 新前端监听：PID `51364`，`8081`，Vite 命令路径位于 `E:\IntRuoyi\IntRuoyiFronted`。

## Verification Evidence

- `show-int-ruoyi-local-status.ps1 -Component full -Json` -> PASS，前端 HTTP `200`、后端 HTTP `200`、两个端口均处于 listening。
- `Invoke-WebRequest http://127.0.0.1:8081/` -> PASS，HTTP `200`。
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，`status=UP`。
- `python -X utf8 -m pytest script\tests\test_runtime_control_scripts.py -q` -> PASS，15 passed。
- `python -X utf8 -m pytest script\tests\test_restart_int_ruoyi_local_schema.py script\tests\test_restart_ruoyi_frontend_vite_emfile_config.py -q` -> PASS，21 passed。
- `script\tests\test_dcc_download_encryption_runtime_config.ps1` -> PASS。
- `node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js` -> PASS。
- `node tests\e2e\codex-test-runner-http-client-static.spec.js` -> PASS。
- Tokenless Runner 受控探针：未发送 `X-Codex-Runner-Token` 的注册和心跳请求均返回 HTTP `200`、业务码 `0`；会话心跳 age `25` 秒、小于 `60` 秒阈值，`currentRunningCount=0`；任务专属探针会话已软删除，影响行数 `1`。

## Observed Runtime Warning

- 后端启动后记录到 DCC 临时上传文件清理定时任务异常：目标文件不存在。该异常未阻止 Tomcat 启动、健康检查或前端入口恢复，但不属于本次“重启前后端”范围，未被静默忽略。

## Experience Consolidation

- 已按 `project-experience-consolidation` 检索 `docs/local-runtime.md`、`docs/e2e-rules.md` 和 `docs/experience-index.md`。
- 本次执行未产生新的可复用经验；既有固定端口、tokenless Runner、进程归属和健康检查门禁已覆盖，未新建或修改长期经验文档。

## Ownership Boundary

- 运行期间发现并发任务修改 `doc/tasks/20260806-hide-review-copy-columns/` 并新建 `doc/tasks/20260807-submit-frontend-backend-code/`；这些文件不属于本任务，未读取后修改、未暂存、未提交。
- 共享分支存在非本任务提交和并发工作区改动；本任务未建立包含并发文件的基线，未将其纳入本任务提交，改用 `git commit --only` 保持提交边界。

## Git Evidence

- 本任务收尾提交：`2435458c7`，文件清单为 `doc/tasks/20260806-restart-local-frontend-backend/task.md`、`doc/tasks/20260806-restart-local-frontend-backend/execution-log.md`、`doc/tasks/20260806-restart-local-frontend-backend/verification-report.md`。
- 提交前使用 `git commit --only` 选择性提交，仅包含上述三份本任务文件；`doc/tasks/20260806-hide-review-copy-columns/` 的三个文件仍保留在工作区，未进入本任务提交。
- 最终收尾提交：`78b6dcea5`，仅更新本任务 `execution-log.md`。
- `git push origin int_main` -> PASS：远端从 `0e2874e96` 快进至 `78b6dcea5`。
- 推送后复扫确认本任务文件已提交并推送；并发任务文件仍未纳入本任务提交。

## Milestone Updates

- 本机前后端已重启并通过验证，状态已更新为 `ready_for_closeout`。
- `task_closeout.py --mode preview` -> PASS：保留 `task.md`、`execution-log.md`、`verification-report.md`；delete、blocked、warnings 均为空。
- `task_closeout.py --mode apply` -> PASS：当前工作区为主工作区（`linked=False`），无需合并或移除 worktree；`deleted_paths` 为空。
- 收尾完成后，任务状态已更新为 `completed`。

## Blockers

- 无阻塞本次重启成功的前置条件。
- DCC 临时上传文件清理定时任务的“文件不存在”异常已记录为范围外运行态风险。

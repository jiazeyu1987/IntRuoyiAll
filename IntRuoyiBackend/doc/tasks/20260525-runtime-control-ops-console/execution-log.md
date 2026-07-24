# 执行日志：运行控制台集成发布备份回滚能力

BDD: 发布测试服可审计执行 -> Given 运维人员拥有 `infra:runtime-control:operate` 权限, When 在运行控制台提交发布测试服并填写原因, Then 后端只调用受控测试发布脚本，记录 operationId、操作者、动作、参数、日志路径和执行状态。

BDD: 提升正式服必须 PROD 确认 -> Given 运维人员准备把测试服提升到正式服, When 缺少原因或 `PROD` 确认, Then 请求必须失败且不得执行脚本；When 确认完整, Then 才调用受控提升脚本并记录审计。

BDD: 备份回滚恢复动作失败关闭 -> Given 运维人员发起立即备份、回滚版本或恢复数据, When 必需参数、配置或脚本缺失, Then API 返回明确错误并记录失败/阻塞状态，不返回默认成功。

BDD: 运行控制台可查看日志 -> Given 运维动作已生成日志路径, When 前端请求日志尾部内容, Then 后端只允许读取运行控制台状态目录或已登记操作日志文件，并返回 tail 内容和 EOF 状态。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> FAIL, expected missing `RuntimeControlActionReqVO` and `RuntimeControlLogRespVO` because operation action/log contracts are not implemented yet.

RED: `python -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> FAIL, expected missing `RuntimeControlOperationAction.java` whitelist and missing `infra:runtime-control:operate` menu permission.

RED: `python -m pytest script\tests\test_runtime_control_scripts.py -q` -> FAIL, expected restart script does not pass `--yudao.runtime-control.repo-root` and `--yudao.runtime-control.state-dir`, so worktree runtime-control status can point at the main workspace ports.

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> FAIL, expected local overview cannot consume worktree `frontendPort` and `backendPort` fields from the status script payload.

RED: `python -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> FAIL, expected SQL menu script can create a duplicate runtime-control parent instead of reusing the existing menu id.

GREEN: `python -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> PASS, operation action whitelist and operate menu permission are declared.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, backend runtime-control action dispatch, PROD guard, required rollback parameter, operation audit and registered log tail contracts pass.

GREEN: `python -m pytest script\tests\test_runtime_control_ops_scripts.py script\tests\test_runtime_control_scripts.py -q` -> PASS, operation script contracts, menu parent reuse, and local restart runtime-control worktree arguments pass.

GREEN: Playwright real page path `http://127.0.0.1:8087/infra/monitors/runtime-control` -> PASS, admin user sees enabled operation buttons after permission SQL is applied locally, PROD confirmation blocks high-risk submission without executing the action, local restart creates operation `7e8e34eb-eb06-4556-b284-49840aa80c2c`, and the log dialog reads the registered operation log.

GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260525-runtime-control-ops-console\backend-api-evidence.md` -> PASS, backend evidence file contains required Scope, Contract, Validation, BDD, RED, GREEN, Verification and Blockers markers.

BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-ops-console --mode preview` -> BLOCKED, closeout preview found the backend main worktree dirty, current branch not fast-forward mergeable into `int_main`, and uncommitted task files still present; no cleanup, merge, or commit was performed.

GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\show-int-ruoyi-local-status.ps1 -Component full -Json` -> PASS, worktree `20260525-runtime-control-ops-console` reports frontend `8087` and backend `48087` as HTTP 200/listening.

GREEN: Playwright real page path `http://127.0.0.1:8087/infra/monitors/runtime-control` -> PASS, current worktree frontend and backend show enabled 发布测试服/提升正式服/立即备份/回滚版本/恢复数据 buttons, invalid 提升正式服 submit sends 0 action POST requests, and 查看日志 opens operation `7e8e34eb-eb06-4556-b284-49840aa80c2c`.

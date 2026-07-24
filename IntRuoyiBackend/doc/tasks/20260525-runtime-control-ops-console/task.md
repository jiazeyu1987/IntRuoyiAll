# 任务：运行控制台集成发布备份回滚能力

## 任务目标

- 在运行控制台后端增加可审计的运维动作 API，覆盖发布到测试服、提升正式服、立即备份、回滚版本、恢复数据、查看日志。
- 运维动作必须复用仓库内已存在且受控的脚本入口，不新增静默降级、mock 成功或绕过真实执行的分支。
- 正式环境相关动作必须执行 PROD 确认；破坏性或高风险动作必须记录原因、操作者、参数、日志路径和执行结果。

## 非目标

- 不改现有发布、备份、恢复脚本的真实业务行为。
- 不直接执行正式发布、正式恢复或正式回滚。
- 不把旧命令行菜单删除；本任务是在 Web 运行控制台上增加受控入口。

## 前置任务检查

- 新 worktree 分支：`task/20260525-runtime-control-ops-console`。
- 最近已跟踪后端任务 `20260525-nas-backup-real-verification` 状态为 `completed`。
- 主工作区未跟踪旧任务 `20260525-test-showroom-company-revision-schema-hotfix` 已在主工作区记录为 blocked，原因是测试服 schema 热修不属于本任务授权范围。

## 里程碑

- [x] M1：创建独立 worktree，建立任务记录和 BDD 场景。
- [x] M2：补齐后端 RED 契约测试，证明当前 API 缺少运维动作、日志读取和 PROD guard。
- [x] M3：实现后端运维动作枚举、权限、确认、审计、日志读取和脚本白名单执行。
- [x] M4：运行后端单元/契约测试并记录 GREEN 证据。
- [x] M5：配合前端完成真实页面联调与最终验证。
- [x] M6：执行 closeout 预览，提交本任务后端改动。

## BDD 场景

- BDD: 发布测试服可审计执行 -> Given 运维人员拥有 `infra:runtime-control:operate` 权限, When 在运行控制台提交发布测试服并填写原因, Then 后端只调用受控测试发布脚本，记录 operationId、操作者、动作、参数、日志路径和执行状态。
- BDD: 提升正式服必须 PROD 确认 -> Given 运维人员准备把测试服提升到正式服, When 缺少原因或 `PROD` 确认, Then 请求必须失败且不得执行脚本；When 确认完整, Then 才调用受控提升脚本并记录审计。
- BDD: 备份回滚恢复动作失败关闭 -> Given 运维人员发起立即备份、回滚版本或恢复数据, When 必需参数、配置或脚本缺失, Then API 返回明确错误并记录失败/阻塞状态，不返回默认成功。
- BDD: 运行控制台可查看日志 -> Given 运维动作已生成日志路径, When 前端请求日志尾部内容, Then 后端只允许读取运行控制台状态目录或已登记操作日志文件，并返回 tail 内容和 EOF 状态。

## 预期验证

- RED：`mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` 先失败，证明缺少运维动作和日志读取契约。
- RED：`python -m pytest script/tests/test_runtime_control_ops_scripts.py -q` 先失败，证明脚本白名单/参数契约尚未覆盖。
- GREEN：同上后端测试通过。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260525-runtime-control-ops-console/backend-api-evidence.md` 通过。

## 当前状态

- 状态：completed
- 已完成：
  - 已创建独立后端 worktree。
  - 已建立任务文档和 BDD 场景。
  - 已记录后端 RED：缺少运维动作 VO、日志 VO、动作白名单和操作权限 SQL。
  - 已实现并验证后端运维动作 API、PROD guard、审计记录和日志 tail API。
  - 已修正本地 worktree 重启脚本的运行控制台仓库根与状态目录参数，避免本地页面误指向主工作区端口。
  - 已配合前端在 `http://127.0.0.1:8087/infra/monitors/runtime-control` 走真实页面路径，验证按钮权限态、正式动作 PROD 确认拦截、最近操作表和日志弹窗。
  - 已重新跑最终后端验证、前端类型检查和真实页面验证；前端全量 `pnpm ts:check` 在释放本任务前端进程后通过。
- 阻塞与影响：
  - 无功能阻塞。
  - closeout 预览已执行；自动清理/合并曾因主工作区脏与分支快进条件阻塞，本任务未执行自动删除或自动合并。

## 最终验证

- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 8 tests.
- GREEN: `python -m pytest script\tests\test_runtime_control_ops_scripts.py script\tests\test_runtime_control_scripts.py -q` -> PASS, 6 tests.
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260525-runtime-control-ops-console\backend-api-evidence.md` -> PASS.
- GREEN: Worktree local status -> PASS, frontend `8087` and backend `48087` both HTTP 200.
- GREEN: Playwright real page path -> PASS, operation buttons, PROD guard, operation audit table and log dialog verified with backend `48087`.

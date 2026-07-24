# 任务：傻瓜式运维十项能力代码实现与 reviewer 放行

## 任务目标

- 在新的后端、前端成对 worktree 中完成代码实现。
- 使用多个子 agent 按任务图开发；主 agent 只做 supervisor/reviewer。
- 每个阶段必须 BDD + 严格 TDD：先 RED，再 GREEN，再 REGRESSION。
- 只有所有 AC-01 到 AC-11 完成、独立测试通过、reviewer 判定符合文档要求后才最终放行。

## Worktree

- 后端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-implementation\ruoyi-vue-pro`
- 前端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-implementation\yudao-ui-admin-vue3`
- 分支：`task/20260526-foolproof-ops-implementation`

## 权威文档

- `doc/tasks/20260526-foolproof-ops-docs/review-report.md`
- `doc/tasks/20260526-foolproof-ops-docs/prd.md`
- `doc/tasks/20260526-foolproof-ops-docs/dev-plan.md`
- `doc/tasks/20260526-foolproof-ops-docs/test-plan.md`
- `doc/tasks/20260526-foolproof-ops-docs/subagent-output/*.md`
- 当前实现任务规范计划：`doc/tasks/20260526-foolproof-ops-implementation/development-plan.md`
- 当前实现任务最新 review 汇总：`doc/tasks/20260526-foolproof-ops-implementation/review-report.md`
- 当前实现 P1-P7 的权威证据范围仅限本 paired worktree 及本目录任务文档；根仓库或历史部署任务文档只作为外部上下文/前置条件来源，不能替代或否定本轮 P1-P7 的实现证据。

## 里程碑

- [x] M1：创建新的实现 worktree。
- [x] M2：建立实现任务文档和任务状态。
- [x] M3：T0 契约校准和 RED 测试骨架。
- [x] M4：T1 站内信告警与责任人矩阵。
- [x] M5：T2/T3 后端能力分波次实现。
- [x] M6：T4 日志磁盘、备份演练和事故闭环。
- [x] M7：T5 前端 API、组件和真实路径。
- [x] M8：T6 独立测试、review-fix-loop、收尾预览和提交。

## 当前状态

- 状态：completed / pass_with_scope_waiver
- 当前阶段：T6
- 放行范围：用户已明确允许“允许不执行真实 DR，仅按当前非破坏性证据放行。”因此本轮不再把真实 destructive DR 作为最终放行前置条件；真实 DR 未执行的事实保留为已豁免残余风险，不能声明生产级 DR readiness 已验证。
- 原阻塞项处理：`REAL_DR_APPROVAL_AND_TAG` 已从当前阻塞项移入 scope waiver 残余风险；后续若要声明真实 DR readiness，仍需按“真实 DR 后续补验条件（本次已豁免）”执行并记录证据。
- 当前补充发现：已只读发现回滚候选标签 `20260524_035800`，并在本地 current-code 后端 `http://127.0.0.1:48098` 上通过真实测试租户 HTTP 验证候选接口：回滚候选 `rollback:20260525-103432` 为 `AVAILABLE`，恢复候选 `restore:20260525-103432` 因缺少演练报告和现场快照为 `BLOCKED`。PowerShell 与 Linux backup-ops 已补齐真实演练成功后写回 `manifest/rehearsal-report.json` 和 `manifest/现场快照.md` 的正式路径；但尚未在 current-code Linux 测试环境执行真实 rehearsal，因此仍没有可用于 `RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID` 的已演练恢复候选。
- 当前补充发现：Worker BE 已完成 backup-ops manifest 端口 fail-fast 修复；Linux `backup-now` / `restore-data` / `rollback-app` 不再缺省旧 `48081/8081`，PowerShell `New-BackupOpsManifest` 从 `deploy/runtime.env` 读取 `BACKEND_HOST_PORT` / `FRONTEND_HOST_PORT`，缺失时 blocked。相关 pytest、PowerShell 模块测试与 `py_compile` 已通过；未执行真实备份、恢复或回滚，该缺口已按用户 scope waiver 移入残余风险。
- 当前补充发现：Worker FE 已完成 paired worktree 端口与 post-action health proof 显式 URL 门禁；`.env.local` 指向 `http://127.0.0.1:48098` / `8098`，publish-test 与 real DR 脚本要求 `RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL`、`RUNTIME_CONTROL_TEST_FRONTEND_URL`、`RUNTIME_CONTROL_TEST_WEBSITE_URL`、`RUNTIME_CONTROL_TEST_SHOWROOM_URL`，缺失时在提交操作前 fail-fast，`HEALTH_OK` 会输出实际 URL。主 reviewer 已复跑静态合同、语法检查、缺审批/缺恢复候选/缺健康 URL fail-fast；未执行真实发布或真实 DR。
- 当前补充发现：Linux 与 PowerShell rehearsal 已收紧 latest 语义。手工/外部调用缺 `SelectedBackupId` / `selected_backup_id` 会在任何 latest 选择或 Docker/恢复动作前 `blocked`；只有注册计划任务显式传入 `-OperatorName "scheduler"` 时，才允许按既有周计划对最新备份执行 rehearsal。相关 RED/GREEN 已记录在 `execution-log.md`。
- 当前补充发现：PowerShell `Invoke-RehearsalUseCase` 的默认 `OperatorName` 已从 `scheduler` 改为空字符串；未传 operator 且未传 `SelectedBackupId` 现在同样 `blocked`，只有调用方显式传入 `scheduler` 才能触发 latest rehearsal。
- 最新非破坏性回归：backup-ops pytest 25 项、PowerShell rehearsal evidence、PowerShell manifest ports、Linux `py_compile`、前端 runtime-control foolproof 静态合同和 7 个 Node 语法检查均通过；前后端 `git diff --check` 通过，仅有 LF/CRLF 工作区提示。
- 最新 reviewer 复审：`019e6516-1208-7342-b135-9236499be642` 返回 `pass_current_scope_blocked_external_dr`，确认当前可修复代码/文档阻塞为无；用户随后明确允许不执行真实 DR，因此最终结论调整为 `PASS_WITH_SCOPE_WAIVER`。

## 放行标准

- AC-01 到 AC-11 均有代码实现、执行证据和独立测试证据。
- 上一阶段 `doc/tasks/20260526-foolproof-ops-docs/review-report.md` 的 canonical contract 被代码和测试采用。
- 不存在 fallback、默认成功、mock 成功或静默降级。
- 现有运行控制台基础功能不回归。
- reviewer 放行单 `final_decision=PASS_WITH_SCOPE_WAIVER`。

## 用户范围豁免

- 用户在 2026-05-27 明确授权范围变更：`允许不执行真实 DR，仅按当前非破坏性证据放行。`
- 本次结论为 `PASS_WITH_SCOPE_WAIVER`，不是 `REAL_DR_VERIFIED`。
- 本次允许提交和收尾的证据范围限于当前已通过的非破坏性自动化、只读候选验证、fail-fast 门禁测试、静态契约、语法检查、`git diff --check` 与独立 reviewer 复核。
- 真实备份、恢复数据、回滚版本串联仍是后续补验项；补验前不得对外宣称该链路已经实际执行通过。

## 真实 DR 后续补验条件（本次已豁免）

- 后续补验必须由用户明确批准在测试服执行真实备份、恢复数据和回滚版本串联。
- 后续补验必须设置 `RUNTIME_CONTROL_ALLOW_REAL_DR=1`。
- 后续补验必须显式设置 `RUNTIME_CONTROL_E2E_BASE_URL` 为当前 worktree 前端 `http://127.0.0.1:8098` 或已部署当前分支前端；脚本不再提供远端测试服默认前端地址。
- 后续补验必须显式设置 `RUNTIME_CONTROL_E2E_ACTION_ORIGIN` 为当前代码后端 origin `http://127.0.0.1:48098` 或先将当前后端部署到测试服；脚本不再提供远端测试服默认后端地址。
- 后续补验必须显式设置 post-action health proof URL：`RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL`、`RUNTIME_CONTROL_TEST_FRONTEND_URL`、`RUNTIME_CONTROL_TEST_WEBSITE_URL`、`RUNTIME_CONTROL_TEST_SHOWROOM_URL`；脚本不再硬编码旧测试服 `172.30.30.58:48081/8081/8083` 健康检查地址。
- 后续补验必须在获批真实 DR 命令中显式设置 `RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG=20260524_035800`，并在执行前由 current-code 后端确认该标签仍出现在服务端回滚候选中。
- 后续补验必须提供已校验、已演练、服务端可选的恢复候选 `RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID`；真实 DR 脚本不再把刚刚 `backup-now` 生成的备份点直接用于恢复，避免绕过 AC-05 的已演练门禁。
- 已只读发现一个候选标签：`20260524_035800`，来源为测试服备份点 `/mnt/nas/备份/20260525-103432`，`deploy/image-tag.txt` 与 `manifest.deploy.imageTag` 一致；该发现只减少标签缺口，不代表已获准执行真实 DR。
- 已只读确认 `/mnt/nas/备份/20260525-103432` 未包含 `manifest/rehearsal-report.json` 和 `manifest/现场快照.md`，不能作为当前实现下的可用恢复候选；现已实现真实 rehearsal 后自动写回这两份证据的路径，但仍需在 current-code Linux 目标上实际执行 rehearsal 才会产生可选恢复候选。
- 手工/外部 rehearsal 必须显式传入恢复候选；计划任务 rehearsal 的 latest 选择必须来自注册脚本显式 `-OperatorName "scheduler"`，不得把 latest 作为人工恢复链路 fallback。
- 必须使用当前 worktree 后端端口 `48098` 作为本地只读 current-code 验证 origin，或先将当前后端部署到测试服 Linux；只使用远端测试服旧后端会返回 `No static resource admin-api/infra/runtime-control/rollback-candidates`，不能证明当前代码。
- 执行命令必须在前端 worktree `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-implementation\yudao-ui-admin-vue3` 中运行：

```powershell
$env:RUNTIME_CONTROL_ALLOW_REAL_DR='1'
$env:RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG='20260524_035800'
$env:RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID='<已演练可恢复备份点>'
$env:RUNTIME_CONTROL_E2E_BASE_URL='http://127.0.0.1:8098'
$env:RUNTIME_CONTROL_E2E_ACTION_ORIGIN='http://127.0.0.1:48098'
$env:RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL='http://127.0.0.1:48098/actuator/health'
$env:RUNTIME_CONTROL_TEST_FRONTEND_URL='http://127.0.0.1:8098/'
$env:RUNTIME_CONTROL_TEST_WEBSITE_URL='<当前测试服或本地网站根地址>'
$env:RUNTIME_CONTROL_TEST_SHOWROOM_URL='<当前测试服或本地展厅地址>'
$env:RUNTIME_CONTROL_E2E_TENANT='测试租户'
$env:RUNTIME_CONTROL_E2E_USERNAME='aoteman'
$env:RUNTIME_CONTROL_E2E_PASSWORD='admin123'
node tests\e2e\runtime-control-real-dr-flow.e2e.js
```

- 通过证据必须同时包含：`BACKUP_ID`、`RESTORE-DATA_SUCCEEDED` 或等价 restore 成功日志、`ROLLBACK-APP_SUCCEEDED` 或等价 rollback 成功日志、四个带实际 URL 的 `HEALTH_OK`、以及 `PASS: runtime control real test-server backup restore rollback flow`。

## Cleanup Keep

- `doc/tasks/20260526-foolproof-ops-implementation/request-analysis.md`
- `doc/tasks/20260526-foolproof-ops-implementation/prd.md`
- `doc/tasks/20260526-foolproof-ops-implementation/development-plan.md`
- `doc/tasks/20260526-foolproof-ops-implementation/test-plan.md`
- `doc/tasks/20260526-foolproof-ops-implementation/task-state.json`
- `doc/tasks/20260526-foolproof-ops-implementation/execution-log.md`
- `doc/tasks/20260526-foolproof-ops-implementation/test-report.md`
- `doc/tasks/20260526-foolproof-ops-implementation/review-report.md`
- `doc/tasks/20260526-foolproof-ops-implementation/verification-report.md`
- `doc/tasks/20260526-foolproof-ops-implementation/task.md`

# Execution Log

## User Intent

对 DCC 文控“培训/阅读确认”进行真实 Playwright E2E 验证：发布任务自有需培训/阅读确认的受控文件，非 admin 培训对象通过真实页面完成确认，管理视图与只读 API/DB 证明完成率、未完成名单、确认时间和当前有效版本来源。

## Preflight

- 已读取 `AGENTS.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/frontend-development.md`。
- 已补读 `docs/task-closeout-rules.md`、`docs/local-runtime.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取 Playwright 技能 `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`，确认本任务应通过真实浏览器页面验证。
- `docs/experience-index.md` 存在；适用门禁已摘录到 `task.md`。
- Git 前置状态：`git status --short --branch` 显示当前 `int_main...origin/int_main` 下已有大量非本任务脏改动和未跟踪目录；本任务不得回滚或覆盖并行改动。
- npx 前置：`where.exe npx` 返回 `D:\Programs\npx` 和 `D:\Programs\npx.cmd`。

## BDD

- BDD: Published controlled file creates training/read confirmation task -> Given 上传/DCC 非 admin 账号创建任务自有受控文件并配置 needTraining/培训要求, When 文件通过真实发布链路成为当前 ACTIVE 版本, Then 培训/阅读确认对象在待办或培训入口收到来自该版本的任务。
- BDD: Assigned user completes confirmation from real page -> Given 培训对象账号登录系统, When 通过真实页面打开目标文件并完成阅读确认/培训签收/考试通过等系统要求动作, Then 该对象状态变为已完成且确认时间可见。
- BDD: Management view tracks completion and pending list -> Given 至少两个培训对象被纳入范围, When 一名对象完成确认且另一名对象未完成, Then DCC 或培训管理视图显示完成率、已完成账号确认时间和未完成名单。
- BDD: Read-only backend verification matches UI -> Given 页面路径已完成并保留目标文件 ID/版本/对象账号, When 只读 API/DB 查询培训任务与人员状态, Then 文件版本、任务来源、完成状态、确认时间与 UI 证据一致。

## RED/GREEN

- RED: `Playwright real DCC training/read confirmation path` -> FAIL, 当前缺少非 admin 登录密码环境变量，无法安全登录上传/DCC 账号、培训对象账号和审批账号完成真实页面写入型 E2E。
- GREEN: `node --check doc/tasks/20260802-dcc-training-read-confirm-e2e/dcc-training-read-confirm-e2e.cjs` -> PASS，本轮任务自有 Playwright 包装脚本语法有效。
- GREEN: `Playwright real applicant training record upload + final approval` -> PASS, `pengyunfeng` 通过真实详情处理态上传培训记录，`wangsiyu` 通过真实审批页面完成第四级文控批准，文件进入 `TRAINING_IN_PROGRESS` 并生成培训任务。
- GREEN: `Playwright real training acknowledgement for zhaomingyu` -> PASS, `zhaomingyu` 通过 `/dcc/controlled-file/training-task/1036` 真实页面累计阅读 625 秒后点击“确认培训完成”，确认时间写入 `2026-08-02 18:37:37`。
- RED: `Playwright full all-recipient completion and ACTIVE release` -> FAIL, 系统生成 9 名培训对象，其中 7 名对象没有 `dcc:controlled-file:training:mine` 权限，无法通过真实培训入口完成确认；文件仍为 `TRAINING_IN_PROGRESS`，未达到任务要求的当前 `ACTIVE` 版本。
- GREEN: `Permission grant for dcc:controlled-file:training:mine` -> PASS, 按用户要求新增角色 `910430 / dcc_training_mine_e2e`，绑定菜单 `980121 / dcc:controlled-file:training:mine`，并赋予 `chenchen`, `sunrongrong`, `liuru`, `xuejianxia`, `tengweihua`, `shihaisong`, `malingling`；仅刷新权限缓存，未改培训状态。
- GREEN: `Playwright real training task route for chenchen after permission grant` -> PASS, `chenchen` 非 admin 登录 `/dcc/controlled-file/training-task/1028`，页面显示文件编号、`V1.0`、标题与“当前会话：计时中”，证据 `permission-grant-task-verify-chenchen.json`。
- GREEN: `Playwright real all-recipient training acknowledgement` -> PASS, 剩余 8 名对象均通过真实培训任务页累计阅读并点击“确认培训完成”；最终 9 名对象均有确认时间，两个培训部门均为 `ACKNOWLEDGED`。
- RED: `Playwright real manual release to ACTIVE` -> FAIL, 文件进入 `PENDING_MANUAL_DISTRIBUTION` 后，`wangsiyu` 非 admin 管理详情页无“正式下发”按钮；只读 DB 显示类别 `906104` 的 `DISTRIBUTE` 规则仅授予 `USER=1`（admin），按用户要求未使用 admin 或 API-only/SQL 下发。

## Milestone Updates

- M1 completed: 规则读取和任务文档创建完成。
- M2 blocked: 本机 `http://127.0.0.1:8081/` 返回 HTTP `200`，`http://127.0.0.1:48081/actuator/health` 返回 `UP`，`int-ruoyi-mysql` 容器存在；但非 admin 密码环境变量缺失，真实页面登录前置不满足。
- M2 resumed: 用户提供的 PowerShell 表达式可在当前进程注入 `DCC_E2E_PASSWORD`，不记录明文；本轮复核前端 `8081=200`、后端 `48081=UP`、MinIO ready `200`、Chrome 与 npx 均可用。
- M3 in progress: 只读 DB 核对租户 1 类别 `其他`（ID `906104`）已开启 `training_required=1`、`distribution_required=1`，上传账号 `pengyunfeng` 对该类别有 `UPLOAD` 权限，审批路由为 `zhaohaichen -> zhaojie -> zhaomingyu -> wangsiyu` 四级；最终审批将通过真实页面选择 `质量体系部` 与 `生产计划` 两个单人培训部门，避免使用带 `super_admin` 角色的 `aoteman`。
- M3 completed: 任务自有文件 `2054545668044070281 / CODX-DCC-TRAIN-20260802093955 / V1.0` 已通过真实页面上传并开启 `needTraining=1`；前三阶段审批已完成，申请人培训记录门使用真实处理态详情页上传附件，第四级文控批准通过真实审批页面完成。
- M4 partial completed: `zhaomingyu` 使用非 admin 账号进入“我的培训/培训任务”真实页面，验证文件编号、版本、标题，并完成 10 分钟阅读计时后的培训确认。
- M5 partial completed: `wangsiyu` 管理详情页培训状态区显示 `zhaomingyu` 已确认且时间可见，`zhaojie` 和其他对象仍待确认；`zhaojie` “我的培训”页面显示目标文件未完成。
- M6 completed with BLOCKED result: 只读 DB 核验文件版本、培训任务、人员完成状态、确认时间和权限状态；因 7 名系统生成培训对象缺少培训入口权限，无法全员完成并转 ACTIVE。
- M7 completed: 用户授权的培训入口权限补齐完成，权限缓存刷新后 `chenchen` 真实培训任务页验证通过。
- M8 completed: `chenchen`, `sunrongrong`, `liuru`, `zhaojie`, `xuejianxia`, `tengweihua`, `shihaisong`, `malingling` 均通过真实 Playwright 页面完成培训确认；加上此前 `zhaomingyu`，全员完成。
- M9 blocked: 文件状态已从 `TRAINING_IN_PROGRESS` 推进到 `PENDING_MANUAL_DISTRIBUTION`；正式下发按钮对非 admin 文控账号不可见，因为类别分发权限仅授予 `USER=1`。

## Verification Evidence

- Runtime: frontend HTTP `200`，backend health `UP`，本机 Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe` 存在，`npx` 存在。
- Credential gate: `DCC_E2E_PASSWORD`、`DCC_TRAINING_E2E_PASSWORD`、`DCC_TRAINING_E2E_UPLOADER_PASSWORD`、`DCC_TRAINING_E2E_RECIPIENT_PASSWORD`、`DCC_TRAINING_E2E_APPROVER_PASSWORD` 均为 `MISSING`。
- Existing script convention: 已通过只读源码确认既有 DCC 上传 E2E 使用 `DCC_E2E_PASSWORD` 环境变量，并在缺失时 fail fast。
- Read-only DB schema check: `dcc_controlled_file_training`、`dcc_controlled_file_training_assignment`、`dcc_controlled_file_training_progress`、`dcc_controlled_file_training_view_session` 等表存在；该查询仅用于确认能力存在，不作为页面 E2E 通过证据。
- Read-only DB sample: 本地库存在历史 training progress 行，但最新样本不是本任务新建的 ACTIVE 当前有效版本，且无法在无非 admin 密码环境变量下通过真实页面使用。
- 2026-08-02 resume runtime: frontend HTTP `200`，backend health `UP`，MinIO `http://127.0.0.1:9000/minio/health/ready=200`，`docker-minio-1` healthy，Chrome 和 npx 存在。
- 2026-08-02 read-only data planning: `aoteman` 在租户 122 带 `super_admin` 角色，不作为本轮对象；选择租户 1 的非 admin 培训对象 `zhaomingyu` 与 `zhaojie`，二者拥有 `wenkong` 角色并可访问 `dcc:controlled-file:training:mine`。
- Task-owned file: `2054545668044070281`, file number `CODX-DCC-TRAIN-20260802093955`, title `Codex DCC 培训阅读确认 20260802093955`, version `V1.0`, status `TRAINING_IN_PROGRESS`, `need_training=1`, `published_file_id=9198354916366`, `stamped_file_id=9198354916366`, approved at `2026-08-02 18:18:05`.
- Real page evidence: applicant training record upload and fourth approval are recorded in `e2e-result.json`; manager training status screenshot is `manager-training-status-CODX-DCC-TRAIN-20260802093955.png`; pending recipient page screenshot is `pending-training-mine-zhaojie-CODX-DCC-TRAIN-20260802093955.png`.
- Completed recipient: `zhaomingyu` / progress `1036` completed from real training task page after accumulated view seconds reached `625 / 600`; DB confirmation time is `2026-08-02 18:37:37`.
- Pending recipient proof: `zhaojie` / progress `1031` remains pending with `25 / 600` seconds and no confirmation time; the “我的培训” page shows the target file row and “还需 9分35秒”.
- Full pending list from read-only DB: `chenchen`, `sunrongrong`, `liuru`, `zhaojie`, `xuejianxia`, `tengweihua`, `shihaisong`, `malingling` remain unconfirmed; only `zhaomingyu` is confirmed.
- Permission check: `zhaojie` and `zhaomingyu` have `dcc:controlled-file:training:mine`; `chenchen`, `sunrongrong`, `liuru`, `xuejianxia`, `tengweihua`, `shihaisong`, `malingling` do not have that permission, so they cannot complete the real page path without permission/test-data correction.
- Permission grant evidence: role `910430 / dcc_training_mine_e2e`, menu `980121 / dcc:controlled-file:training:mine`, granted users `chenchen`, `sunrongrong`, `liuru`, `xuejianxia`, `tengweihua`, `shihaisong`, `malingling`; cache refresh deleted only permission-related keys.
- Real page permission evidence: `permission-grant-training-task-chenchen-CODX-DCC-TRAIN-20260802093955.png` and `permission-grant-task-verify-chenchen.json`.
- All-recipient completion evidence: `training-ack-chenchen-...png`, `training-ack-sunrongrong-...png`, `training-ack-liuru-...png`, `training-ack-zhaojie-...png`, `training-ack-xuejianxia-...png`, `training-ack-tengweihua-...png`, `training-ack-shihaisong-...png`, `training-ack-malingling-...png`; DB confirmation times are recorded in `final-readonly-db-verification-after-permission-grant.json`.
- Manager after-all-ack evidence: `manager-training-status-after-all-ack-CODX-DCC-TRAIN-20260802093955.png`, `workbench-pending-manual-distribution-CODX-DCC-TRAIN-20260802093955.png`, `manager-after-all-ack-page-evidence.json`.
- Final read-only DB evidence: file status `PENDING_MANUAL_DISTRIBUTION`, all 9 progress rows have `acknowledged_at`, trainings `108 / 生产计划` and `109 / 质量体系部` are `ACKNOWLEDGED`, category `DISTRIBUTE` rule only targets `USER=1`.

## Blockers

- Resolved blocker: 原缺少非 admin 密码环境变量已由用户提供的进程级 PowerShell 表达式解除；仍禁止记录明文、禁止使用 admin、禁止 API-only 或 SQL 改培训状态。
- Resolved blocker: 7 名培训对象缺少 `dcc:controlled-file:training:mine` 已按用户授权补齐为角色授权，并用 `chenchen` 真实培训任务页验证通过。
- 按用户要求未采取的绕过：未使用 admin 账号，未直接调用 API 完成确认，未 SQL 修改培训进度/确认时间，未 SQL 修改文件状态/发布状态，未使用历史任务数据冒充本轮任务自有文件。
- Current blocker: 正式下发/转 `ACTIVE` 仍缺非 admin 类别 `DISTRIBUTE` 权限。当前类别 `906104 / 其他` 的 `DISTRIBUTE` 规则仅授予 `USER=1`（admin），`wangsiyu` 详情页无“正式下发”按钮。影响：培训确认场景已完成，但文件停留在 `PENDING_MANUAL_DISTRIBUTION`，不能在不使用 admin 或额外授权的前提下完成 ACTIVE/受控浏览最终验收。

## Final Resume - Distribute Role Fix

- GREEN: `DISTRIBUTE permission role grant` -> PASS, 新增 `910431 / dcc_distribute_e2e`，为类别 `906104 / 其他` 增加 `DISTRIBUTE` 规则 `2623`，并赋给非 admin 文控账号 `wangsiyu`。
- GREEN: `Playwright real manual release after distribute role` -> PASS, `wangsiyu` 登录真实详情页，看到“正式下发”按钮，点击确认后 `/admin-api/dcc/controlled-files/2054545668044070281/manual-release` 返回 `code=0`。
- GREEN: `Final read-only DB active verification` -> PASS, 文件状态 `ACTIVE`，master `current_active_controlled_file_id=2054545668044070281`，9/9 培训对象均有 `acknowledged_at`。
- Final evidence: `manual-release-after-distribute-role-page-evidence.json`, `manual-release-after-role-before-CODX-DCC-TRAIN-20260802093955.png`, `manual-release-after-role-after-CODX-DCC-TRAIN-20260802093955.png`, `final-readonly-db-verification-after-distribute-role.json`, `e2e-result.json`, `verification-report.md`。
- Final result: Training/read confirmation PASS; current effective ACTIVE release PASS. 未使用 admin、未 API-only 确认/下发、未 SQL 修改培训完成状态或文件状态。

## Full Rerun - 2026-08-02 19:48 +08:00

- User request: 重新完整复跑 DCC 文控培训/阅读确认真实 Playwright E2E。
- BDD: Full rerun creates a new task-owned training file -> Given 非 admin 上传/DCC 账号通过真实页面上传并开启培训要求, When 该 V1.0 文件通过四级审批、申请人培训记录和文控批准, Then 系统生成来自该当前版本的培训/阅读确认任务。
- BDD: Full rerun proves completed and pending states -> Given 至少两个培训对象收到任务, When 第一名对象完成确认且其他对象暂未确认, Then 管理视图和只读 DB 同时显示已完成对象、确认时间和未完成名单。
- BDD: Full rerun completes all real-page acknowledgements and release -> Given 所有对象均通过真实培训任务页打开目标文件, When 阅读计时满足后逐一点击确认并由非 admin 文控账号正式下发, Then 文件为 `ACTIVE`、master 指向本轮文件、所有 progress 均有 `acknowledged_at`。
- Planned result isolation: use a new `DCC_E2E_RUN_ID`, `DCC_E2E_FILE_NUMBER`, `DCC_E2E_RESULT_PATH`, and `DCC_E2E_ARTIFACT_PREFIX` so this rerun does not overwrite prior PASS evidence.

# DCC 文控培训/阅读确认真实 E2E 验证

## Task Goal

验证受控文件发布后，相关人员能收到培训/阅读确认任务，至少一名对象可通过真实页面完成阅读确认或培训签收，管理视图可追踪完成率、未完成名单和确认时间，并用只读 API/DB 核验任务来源于当前有效版本。

## Milestones

1. 规则与前置门禁读取完成，建立任务自有文档与证据目录。
2. 确认本机前后端运行态、非 admin 测试账号、密码环境变量和 Playwright 浏览器前置。
3. 使用上传/DCC 账号通过真实页面准备并发布任务自有 ACTIVE 文件，配置 needTraining/培训要求或等价字段。
4. 使用培训对象账号通过真实页面打开培训/阅读确认入口，验证文件编号、版本、标题为当前有效版并完成确认。
5. 回到 DCC/培训管理视图验证已完成状态、完成时间、完成率和未完成名单。
6. 用只读 API/DB 核验培训任务、人员完成状态、确认时间和文件版本，输出 verification-report.md。

## Expected Verification

- Playwright 真实页面路径覆盖登录、受控文件发布、培训/阅读确认任务领取与完成、管理视图状态核验。
- 只读 API/DB 仅用于最终核验，不直接修改完成状态。
- 证据记录文件 ID、版本、培训对象、完成账号、完成时间、未完成名单。
- 若缺页面入口、任务生成或权限，记录 E2E BLOCKED 并明确阻塞项。

## Current Status

ready_for_closeout - full rerun PASS on 2026-08-02 22:18:56 +08:00; verification complete, task-owned cleanup/commit closeout not yet performed.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本任务为真实 E2E 验证，不以 API-only、DB 直改或 mock 替代页面路径。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- DCC 文控审批处理入口门禁：发布链路必须证明真实处理态、审批/发布写接口响应和当前 ACTIVE 版本，不得用只读 viewer 或 API-only 替代。
- Playwright 浏览器可执行文件门禁：优先使用本机 Chrome/Edge 或已安装 Playwright 浏览器，缺失时记录 E2E 前置缺口。
- Playwright 目标链路归因门禁：区分本机前后端、DCC/培训目标接口与外部资源异常，不能全局忽略 console/network 错误。
- Playwright 快照与 daemon 收尾门禁：任务自有截图、trace、snapshot 必须保存在任务目录或受控输出目录，收尾时清理敏感/临时 artifact。
- Element Plus 上传/选择/表格门禁：文件上传、人员范围选择、列表行操作必须按页面可见业务唯一文本定位并断言选择结果。
- 数据库只读核验门禁：只读核验必须先确认 schema/表名来源，禁止写 SQL 或直接修改完成状态。

## Cleanup Keep

doc/tasks/20260802-dcc-training-read-confirm-e2e/task.md
doc/tasks/20260802-dcc-training-read-confirm-e2e/execution-log.md
doc/tasks/20260802-dcc-training-read-confirm-e2e/verification-report.md
doc/tasks/20260802-dcc-training-read-confirm-e2e/dcc-training-read-confirm-e2e.cjs

## Current Run Notes

2026-08-02 full rerun：用户要求“重新完整的走一遍E2E验证流程”。本轮将生成新的任务自有文件编号，复跑真实页面上传、四级审批/签名、申请人培训记录、培训任务领取与确认、未完成名单证明、正式下发、受控当前有效版和只读 DB 核验；仍禁止 admin、API-only/SQL 改培训完成或文件状态。

2026-08-02 resume：用户已提供安全的 PowerShell 环境变量注入方式，原“非 admin 密码环境变量缺失”阻塞解除。本轮通过真实 Playwright 页面完成任务自有文件上传、申请人培训记录上传、第四级文控批准、培训任务生成、`zhaomingyu` 首个培训确认，以及后续所有培训对象确认。

2026-08-02 permission fix：按用户要求新增并授权角色 `dcc_training_mine_e2e`（ID `910430`），绑定菜单权限 `dcc:controlled-file:training:mine`（菜单 ID `980121`），并赋予 `chenchen`、`sunrongrong`、`liuru`、`xuejianxia`、`tengweihua`、`shihaisong`、`malingling`。仅刷新权限缓存键，未修改培训进度、确认时间、文件状态或发布状态。

2026-08-02 final state：9 名培训对象均通过真实培训任务页完成确认，两个培训部门状态均为 `ACKNOWLEDGED`，文件进入 `PENDING_MANUAL_DISTRIBUTION`。最终 `ACTIVE`/正式下发仍 BLOCKED：本地类别 `906104 / 其他` 的 `DISTRIBUTE` 类别权限仅授予 `USER=1`（admin），非 admin 文控账号 `wangsiyu` 详情页无“正式下发”按钮；按用户要求未使用 admin、未 API-only 下发、未 SQL 修改状态。

2026-08-02 distribute fix：按用户要求新增并授权角色 `dcc_distribute_e2e`（ID `910431`），为类别 `906104 / 其他` 绑定 `DISTRIBUTE` 规则（ID `2623`），并赋予非 admin 文控账号 `wangsiyu`。刷新 `wangsiyu` 相关权限缓存后，`wangsiyu` 通过真实页面看到并点击“正式下发”，接口返回 `code=0`，只读 DB 核验文件进入 `ACTIVE`，master 当前有效版本指向 `2054545668044070281`。

2026-08-02 full rerun completion：在后端恢复后继续本轮任务自有文件 `2054545668044070298 / CODX-DCC-TRAIN-RERUN-20260802195426 / V1.0`，所有 9 名培训对象均通过真实培训任务页完成阅读计时与“确认培训完成”；文件由非 admin 文控账号 `wangsiyu` 真实页面点击“正式下发”后进入 `ACTIVE`，master 当前有效版本指向本轮文件。未使用 admin，未 API-only/SQL 修改培训完成、确认时间、文件状态或发布状态。

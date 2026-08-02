# DCC 文控旧版本自动作废/失效真实 E2E 验证

## Task Goal

按用户最新确认的口径验证 DCC 文控“作废/废止”链路：不走手动作废审批，而是通过文件升版发布后，旧的当前有效版本自动失效为 `SUPERSEDED`，master 当前有效版本切换到新版本，受控浏览不再把旧版本作为当前有效文件展示，并能在版本历史/追溯中看到升版原因、审批人与签名证据。

## Milestones

1. 读取项目规则、登录/E2E/前端/收尾规范与 Playwright 技能。
2. 确认本机前后端入口、非 admin 测试账号、浏览器与任务自有数据前置。
3. 复用任务自有已完成原版发布与升版发布链路证据，确认 V1/V2 文件 ID、master 和发布审批实例。
4. 通过真实 Playwright 页面进入受控浏览，确认当前有效行只返回 V2 `ACTIVE`，不返回旧 V1。
5. 从受控浏览打开 V2 追溯详情页，确认页面展示 V2 `ACTIVE`、版本历史包含 V1/V2 和升版原因。
6. 使用只读 DB 核验 V1 `SUPERSEDED`、V2 `ACTIVE`、master 当前有效版本指向 V2、审批任务与签名证据。
7. 输出 verification-report.md 并记录 BDD、RED/GREEN 和验证证据。

## Expected Verification

- Playwright 真实页面验证：登录非 admin 用户、打开受控浏览、搜索任务自有文件号、确认当前有效行是 V2、旧 V1 不作为当前有效行返回、打开 V2 追溯详情并验证版本历史。
- 只读 DB 核验：文件状态、master 当前指针、V1 successor、发布审批实例、上传/升版审批任务、DCC 电子签名证据。
- 报告记录：文件 ID、升版前/后状态口径、审批签名证据、受控浏览验证结果、版本历史和追溯证据。

## Current Status

ready_for_closeout

## Current Verification State

- 用户明确变更验收口径：`作废先不走审批, 文件升版本的时候老的版本自动作废, 走这条链路`。
- 手动作废审批链路保留为前置记录：真实页面可打开“作废当前版本”弹窗，但运行态缺少 `DCC / DCC / CONTROLLED_FILE / OBSOLETE` 发布策略，无法继续；该路径已不作为当前验收链路。
- 最新完整复跑结论：`PASS`。2026-08-02 22:27:23 +08:00 本轮新建任务自有文件 `CODX-DCC-REV-FULL-20260802-20260802222723`，通过真实 Playwright 完成 V1 原版上传、V1 四级审批/签名、V2 升版上传、V2 四级审批/签名、发布申请与四级发布审批。
- 最新关键文件：V1 `2054545668044070307` / `V1.0` / `SUPERSEDED`；V2 `2054545668044070308` / `V2.0` / `ACTIVE`；master `2054545668044062911` / `ACTIVE_CHAIN` / 当前有效版本 `2054545668044070308`。
- 最新受控浏览：`status=ACTIVE` 搜索当前文件号只返回/打开 V2，未将 V1 作为当前有效行返回；追溯详情版本历史包含 V1 `SUPERSEDED`、V2 `ACTIVE` 和升版原因 `升版 E2E 20260802222723`。
- 最新审批/签名：V1 和 V2 上传/升版审批共 8 个 DCC 完成任务；V1/V2 DCC 电子签名人为 `zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu`，均为 `PASSWORD`、`passwordVerified=1`、`evidenceStatus=VALID`；发布 BPM 实例 `76f6dfd2-8e7e-11f1-aa29-00155d2984a0` 已完成 4 个非 admin 审批任务。
- 干净门禁：包装层 `full-rerun-e2e-result-20260802222723.json` 与底层 `chain-result.json` 均为 `PASS`，`targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`。
- 历史阻塞覆盖：`20260802212823` 发布审批 HTTP 500 与后续本地后端启动阻塞已在用户确认后端恢复后由 `20260802222723` 全新真实 E2E PASS 覆盖；未使用 API-only、SQL 改状态、删除文件或 admin 账号绕过。

## Current Rerun

- 2026-08-02 19:30:25 +08:00：按用户要求进行一次“修改之后”的完整真实 Playwright E2E，不复用旧链路结果。
- 本轮范围：新建任务自有 V1 原版文件、完成四级审批/签名、发布/升版为 V2、再次完成四级审批/签名与发布审批、验证 V1 自动 `SUPERSEDED`、V2 `ACTIVE`、master 指向 V2、受控浏览不再返回 V1 当前有效行。
- 约束：不使用 admin，不走手动作废审批，不通过 API-only/SQL 改状态，不删除文件；API/DB 只用于最终只读核验。
- 2026-08-02 19:31:42 +08:00：完整业务状态链路跑通，结果 `full-rerun-e2e-result.json` 为 `PASS`，新建任务自有文件号 `CODX-DCC-REV-FULL-20260802-20260802193142`，V1 `2054545668044070293` 自动 `SUPERSEDED`，V2 `2054545668044070294` 为 `ACTIVE`，master `2054545668044062902` 指向 V2，受控浏览只返回 V2 当前有效行。但底层链路结果 `doc/tasks/20260802-dcc-revision-publish-real-e2e/chain-result.json` 仍记录审批页 `pageErrors`：`Cannot read properties of null (reading 'nextSibling')`，不满足本任务“全链路 pageErrors=0”干净门禁。
- 2026-08-02 19:40:27 +08:00：再次执行全新任务自有文件完整链路并显式检查底层链路错误。结果 `full-rerun-e2e-result-20260802194027.json` 为 `BLOCKED`，脚本退出码 `1`；底层链路在 V1 首个非 admin 审批账号 `zhaohaichen` 的真实审批详情页阻塞，页面抛出 `Cannot read properties of undefined (reading 'visible')`，`text=审批阶段进度` 未出现并超时。
- 最新阻塞影响：本轮新建 V1 `2054545668044070296` 当前停在 `PENDING_DOC_CONTROL_REVIEW`，待办任务为 `DOC_CONTROL_REVIEW`、assignee `376`；尚未进入 V2 升版、发布审批、旧版自动 `SUPERSEDED` 或受控浏览最终验证。未使用 API/SQL/admin 绕过，也未删除文件。
- 2026-08-02 20:10:23 +08:00：完成修复后全新执行完整真实 Playwright 链路。结果 `full-rerun-e2e-result-20260802201023.json` 为 `PASS`；底层链路 `chain-result.json` 为 `PASS`；V1 `2054545668044070300` 自动 `SUPERSEDED`，V2 `2054545668044070301` 为 `ACTIVE`，master `2054545668044062907` 指向 V2，受控浏览当前有效行为 V2，追溯详情版本历史包含 V1/V2 与升版原因；`targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`。
- 2026-08-02 21:28:23 +08:00：按用户“进行一次完整的 E2E 验证”再次执行全新任务自有文件完整链路。结果 `full-rerun-e2e-result-20260802212823.json` 与底层 `chain-result.json` 均为 `BLOCKED`；发布审批第二节点 `zhaojie` 对 `/admin-api/bpm/task/approve` 返回 HTTP 500，链路停止，未用 API/SQL/admin 绕过。
- 2026-08-02 21:32:06 +08:00：后端重启失败，日志记录 `APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED: SHOWROOM`，`48081` 后续健康检查拒绝连接。该 blocker 属于当前运行态前置，不属于 DCC 作废/升版验收链路本身；按用户要求仅记录 BLOCKED，不顺手修 SHOWROOM。
- 2026-08-02 22:27:23 +08:00：用户确认后端已连上后再次执行全新任务自有文件完整链路。结果 `full-rerun-e2e-result-20260802222723.json` 为 `PASS`；底层链路 `chain-result.json` 为 `PASS`；V1 `2054545668044070307` 自动 `SUPERSEDED`，V2 `2054545668044070308` 为 `ACTIVE`，master `2054545668044062911` 指向 V2，受控浏览当前有效行为 V2，追溯详情版本历史包含 V1/V2 与升版原因；`targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`。

## 经验门禁

- DCC 文控审批处理入口门禁：升版/发布链路必须从真实页面和真实审批处理态完成；禁止 API-only、SQL 改状态或 admin 绕过。
- 受控浏览链路门禁：必须证明 ACTIVE browser-page 只返回/默认打开当前有效版本，旧版本不作为当前有效行返回，版本历史/追溯可见旧版状态和升版原因。
- Playwright 浏览器可执行文件门禁：使用本机 Chrome 显式路径；不得把浏览器缺失写成业务失败。
- Playwright 目标链路异常归因门禁：目标 DCC/审批链路 `targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]` 才可作为本轮 PASS。
- E2E 基本门禁：使用任务自有测试文件和非 admin 账号；API/DB 只用于最终只读核验。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务只验证用户确认的正式升版自动失效链路；未修改业务状态或产品逻辑。
- `是否存在临时补丁或绕过`：否。验证脚本仅按当前真实页面的 `traceability=1&from=browser` 追溯详情入口调整等待锚点，未放宽文件状态、master、受控浏览或签名断言。

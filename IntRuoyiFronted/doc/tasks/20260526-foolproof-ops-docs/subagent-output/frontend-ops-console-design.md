# 子 agent C：前端运行控制台傻瓜式运维 UI/交互/API 接线设计

## 0. 现状检查

本设计基于以下轻量检查：

- `src/views/infra/runtime-control/index.vue` 已存在运行控制台，当前集中承载状态矩阵、操作按钮、重启弹窗、发布/备份/回滚/恢复弹窗、日志弹窗；回滚镜像标签和恢复备份点仍为手填输入。
- `src/api/infra/runtimeControl/index.ts` 当前只提供 `overview`、`restart`、`actions`、`operations`、`operation log` 五类接口。
- 项目已有站内信能力：`src/api/system/notify/message/index.ts`、`src/api/system/notify/template/index.ts`、顶部铃铛 `src/layout/components/Message/src/Message.vue`、我的站内信路由 `MyNotifyMessage`。
- `tests/e2e` 已有运行控制台 Playwright 风格脚本和静态契约脚本；`package.json` 当前未发现 Vitest 或 `@vue/test-utils` 组件测试依赖，后续若执行组件测试必须先显式补齐测试基础设施，不能静默退化为只跑静态检查。

## 1. 目标、范围、非目标

### 目标

把当前“懂脚本的人可运维”的运行控制台，升级为“公司 IT 每天登录后能按页面判断、处理、复核”的傻瓜式运维入口。前端必须把站内信告警、责任人、向导、候选约束、巡检报告、业务健康、探针、日志/磁盘、备份演练和事故闭环整合成可扫描、可阻断、可追溯的页面能力。

### 范围

- 扩展运行控制台页面信息架构和交互。
- 拆分 `src/views/infra/runtime-control/index.vue`，避免继续堆叠单文件大组件。
- 扩展 `src/api/infra/runtimeControl/index.ts` 的 DTO、状态枚举和 API 接线。
- 复用现有站内信顶部铃铛和我的站内信页面，同时在运行控制台内提供运维告警摘要入口。
- 设计前端组件测试、静态契约测试和 Playwright 真实路径验证。

### 非目标

- 前端不负责发送站内信；站内信事件、模板、发送状态由后端负责。
- 第一阶段不接短信、邮件、钉钉、Webhook、电话等外部实时告警。
- 不新增自动修复、自动恢复、自动清理生产数据能力。
- 不用 mock 成功、默认 PASS、测试专用控件或隐藏失败状态替代真实证据。
- 不改变现有 `PROD` 确认语义；只能在其上增加候选、责任人和证据校验。

## 2. 信息架构

运行控制台建议保持单一路由 `/infra/monitors/runtime-control`，内部拆为 10 个业务区和 1 个操作记录区：

1. 站内信告警入口：展示未读运维告警数、最近 5 条告警、发送状态、严重级别、责任人和“查看我的站内信”入口；明确提示站内信适用于每天登录处理，不承诺夜间外部实时叫醒。
2. 责任人状态：展示发布负责人、备份负责人、数据负责人、验收人、告警负责人、reviewer；缺必填角色时用 `BLOCKED` 状态阻断生产发布、生产回滚、数据恢复和告警关闭。
3. 决策向导：按应用异常、数据异常、发布前检查、发布后观察、备份演练、磁盘风险展示推荐动作、所需证据、风险提示和阻断原因。
4. 候选选择：回滚只能选可回滚镜像候选；恢复只能选已校验、已演练、manifest 完整的备份点；不可手填未知值。
5. 巡检报告：发布前检查和发布后观察展示红黄绿结论：`PASS`、`WARN`、`BLOCKED`、`NO-GO`；缺关键证据时不能显示 `PASS`。
6. 业务健康：展示登录、ERP、MES、文件对象、API 错误、慢请求、任务失败状态和最近采样时间。
7. 探针状态：展示 backend、frontend、website 的 HTTP 探针结果、耗时、HTTP 状态、失败原因、最近告警关联。
8. 日志/磁盘：展示日志目录增长、磁盘容量、水位阈值、趋势、最近告警；只读，不提供页面删除日志按钮。
9. 备份演练：展示最近备份、最近演练、可恢复点、manifest、checksum、报告路径、最近验证时间、不可用原因。
10. 事故闭环：从告警或高危操作进入事故详情，展示处理动作、验证结果、责任人、剩余风险、复盘状态、关闭人；关闭前必须校验必填证据。
11. 最近操作：保留现有操作表和日志弹窗，但应与事故、巡检报告、候选 ID 建立链接。

## 3. 组件拆分

后续 worker 不应继续在 `index.vue` 里追加大块模板和逻辑。建议拆分如下：

- `index.vue`：页面编排容器，只负责加载聚合数据、权限注入、刷新和布局。
- `components/OpsAlertInboxCard.vue`：站内信告警摘要、未读数、最近告警、发送状态、跳转我的站内信。
- `components/OpsOwnerMatrixPanel.vue`：责任人矩阵、缺失角色阻断提示、只读/可编辑状态。
- `components/OpsDecisionWizard.vue`：场景选择、推荐动作、证据要求、下一步入口。
- `components/OpsCandidatePicker.vue`：回滚镜像候选和恢复备份点候选，统一处理不可选原因。
- `components/OpsInspectionReportPanel.vue`：发布前检查、发布后观察、红黄绿报告、检查项明细。
- `components/OpsBusinessHealthPanel.vue`：登录、ERP、MES、文件对象、API 错误、慢请求、任务失败。
- `components/OpsProbeStatusPanel.vue`：backend/frontend/website 探针结果。
- `components/OpsLogDiskRiskPanel.vue`：日志增长、磁盘容量、阈值和趋势。
- `components/OpsBackupDrillPanel.vue`：备份点、演练状态、manifest、checksum、报告路径。
- `components/OpsIncidentDrawer.vue`：事故详情、验证证据、复盘状态、关闭确认。
- `components/OpsActionConfirmDialog.vue`：复用发布、回滚、恢复、备份、高危确认逻辑，保留 `PROD`。
- `components/OpsStatusBadge.vue`：统一 `PASS/WARN/BLOCKED/NO-GO/UNKNOWN` 展示。
- `composables/useRuntimeControlFoolproof.ts`：聚合查询、刷新、加载/失败状态。
- `composables/useRuntimeControlAction.ts`：动作提交、确认校验、日志轮询。

## 4. API 接线与状态枚举

### 枚举

前端统一使用以下窄枚举，后端返回未知值时显示为 `UNKNOWN` 并暴露原始值，不转成成功：

```ts
export type OpsEnvironment = 'local' | 'test' | 'prod'
export type OpsStatus = 'PASS' | 'WARN' | 'BLOCKED' | 'NO_GO' | 'UNKNOWN'
export type OpsSeverity = 'INFO' | 'WARN' | 'CRITICAL'
export type OpsAction = 'publish-test' | 'promote-prod' | 'backup-now' | 'rollback-app' | 'restore-data' | 'preflight-check' | 'post-deploy-watch'
export type CandidateStatus = 'SELECTABLE' | 'DISABLED' | 'EXPIRED' | 'UNVERIFIED' | 'BLOCKED'
export type IncidentStatus = 'OPEN' | 'HANDLING' | 'VERIFYING' | 'REVIEWING' | 'CLOSED' | 'BLOCKED'
```

### 聚合查询

新增或扩展聚合接口，优先由后端提供一个只读聚合端点，减少前端并发拼装导致状态不一致：

- `GET /infra/runtime-control/foolproof-overview`
  - 返回告警摘要、责任人矩阵、推荐向导、巡检报告、业务健康、探针、日志磁盘、备份演练、事故摘要。
  - 缺任一关键数据时，对应 section 返回 `status=BLOCKED` 和 `blockedReason`，不得省略或返回 PASS。

### 候选接口

- `GET /infra/runtime-control/rollback-candidates?environment=prod`
  - 返回 `candidateId`、`imageTag`、`builtAt`、`sourceManifest`、`verificationStatus`、`drillStatus`、`selectable`、`blockedReason`。
- `GET /infra/runtime-control/restore-candidates?environment=prod`
  - 返回 `backupId`、`createdAt`、`manifestPath`、`checksumStatus`、`drillStatus`、`snapshotRequired`、`selectable`、`blockedReason`。

前端提交时只传候选 ID，不传手填标签：

```ts
export interface RuntimeControlActionReqVO {
  action: OpsAction
  reason: string
  prodConfirmText?: string
  publishScope?: 'code-only' | 'with-data'
  selectedImageCandidateId?: string
  selectedBackupCandidateId?: string
  ownerConfirmUserId?: number
  inspectionReportId?: string
  incidentId?: string
}
```

### 巡检与事故

- `POST /infra/runtime-control/inspection-reports`：创建发布前检查或发布后观察，只读采样。
- `GET /infra/runtime-control/inspection-reports/{id}`：读取报告和明细。
- `GET /infra/runtime-control/incidents`：读取事故列表。
- `GET /infra/runtime-control/incidents/{id}`：读取事故详情。
- `POST /infra/runtime-control/incidents/{id}/close`：关闭事故，必须提交验证结果、剩余风险、关闭说明和 `PROD` 确认（生产事故）。

### 站内信

- 运行控制台不直接调用 `sendNotify`。
- 页面可复用 `getUnreadNotifyMessageCount()` 和 `getUnreadNotifyMessageList()` 展示顶部铃铛一致的未读状态。
- 运维告警摘要应来自 `foolproof-overview.alerts`，因为它需要包含环境、服务、严重级别、责任人、runbook、操作入口和发送状态。
- “查看全部”跳转 `MyNotifyMessage`；如果后端支持查询参数，可带 `templateCode=OPS_ALERT`，否则仅跳转我的站内信，不假装过滤成功。

## 5. 权限态、确认态与异常态

### 权限

- `infra:runtime-control:query`：查看运行控制台聚合状态。
- `infra:runtime-control:operate`：提交发布、备份、回滚、恢复等动作。
- `infra:runtime-control:restart`：重启组件，保留现有权限。
- `infra:runtime-control:owner:update`：维护责任人矩阵。
- `infra:runtime-control:incident:close`：关闭事故。

无权限时展示真实只读状态和缺少权限编码；不得隐藏面板导致用户误判系统正常。

### 危险确认

- 生产发布、生产回滚、数据恢复、生产事故关闭必须输入 `PROD`。
- 回滚必须选择 `SELECTABLE` 镜像候选。
- 数据恢复必须选择 `SELECTABLE` 备份候选，并展示现场快照要求。
- 缺原因、缺责任人、缺候选、缺报告、缺验证证据时按钮禁用并展示阻断原因。
- 后端拒绝时前端展示后端返回的真实错误，不改写成通用成功或继续轮询。

### 页面状态

- 加载：各 section 用骨架或加载状态，按钮禁用。
- 空态：显示“暂无真实数据”和所需前置条件；不得显示绿色成功。
- 失败：显示接口错误、HTTP 状态或后端 `blockedReason`；保留刷新按钮。
- 只读：展示数据和阻断原因，隐藏或禁用提交按钮，并显示所需权限。
- 不可选：候选行展示 `blockedReason`、缺失 manifest、未演练、checksum 失败或过期原因。
- 未知：未知枚举展示 `UNKNOWN` 和原始值，交给 reviewer 判断，不默认归类 PASS。

## 6. BDD 场景

BDD: 站内信告警可进入运行控制台 -> Given IT 每天登录系统且存在备份失败站内信, When IT 打开运行控制台, Then 页面展示未读运维告警数、严重级别、责任人、runbook 和操作入口，并提示站内信不是夜间外部实时告警。

BDD: 缺责任人阻断高危操作 -> Given 生产回滚缺少数据负责人或 reviewer, When IT 打开回滚确认弹窗, Then 回滚按钮不可提交并显示缺失责任人，不发送任何操作请求。

BDD: 应用异常通过向导进入回滚候选 -> Given backend/frontend 探针异常且数据健康, When IT 在决策向导选择应用异常, Then 页面推荐应用回滚并只展示可回滚镜像候选。

BDD: 数据异常通过向导进入恢复候选 -> Given 文件对象异常或数据误删事故已创建, When IT 在决策向导选择数据异常, Then 页面推荐数据恢复并只展示已校验、已演练、manifest 完整的备份点。

BDD: 回滚不能手填未知镜像 -> Given 后端返回两个可回滚候选, When IT 打开回滚版本, Then 页面没有自由输入镜像标签控件，只能选择候选；未选择候选时提交被阻断。

BDD: 恢复前必须展示现场快照要求 -> Given 备份点可恢复且 `snapshotRequired=true`, When IT 选择该备份点, Then 确认弹窗展示现场快照要求和未完成阻断原因。

BDD: 发布前检查缺关键证据为 BLOCKED -> Given MySQL 或备份演练证据缺失, When IT 点击发布前检查, Then 报告状态为 BLOCKED 或 NO-GO，不能显示 PASS。

BDD: 业务健康聚合暴露真实失败 -> Given ERP 同步失败且 API 错误数超过阈值, When IT 查看业务健康面板, Then ERP 与 API 错误项显示 WARN 或 NO-GO 并展示采样时间和观察值。

BDD: 探针失败进入站内信告警 -> Given website 探针连续失败并生成告警事件, When IT 查看探针状态, Then website 行展示失败原因和关联站内信发送状态。

BDD: 日志磁盘风险不可被静默忽略 -> Given 磁盘使用率超过阈值, When IT 查看日志/磁盘面板, Then 页面显示 WARN 或 NO-GO、趋势、阈值和最近告警，不提供删除日志的临时修复按钮。

BDD: 事故关闭必须有验证证据 -> Given 事故处于 REVIEWING 且缺少验证结果, When reviewer 点击关闭事故, Then 关闭按钮被阻断并展示缺少验证证据；补齐后才允许提交关闭请求。

## 7. 严格 TDD 计划

### 测试基础设施前置

当前 `package.json` 未发现组件测试依赖。前端 worker 必须先明确采用项目批准的组件测试方案，例如 Vitest + `@vue/test-utils` + jsdom，并提交测试脚本；如果依赖安装或审批不可用，应标记 BLOCKED，不能用静态脚本冒充组件测试。

### RED

- RED: `pnpm exec vitest run src/views/infra/runtime-control/**/*.spec.ts` -> FAIL，预期原因：尚未实现 `OpsDecisionWizard`、`OpsCandidatePicker`、`OpsStatusBadge` 等组件，候选不可手填和阻断态断言失败。
- RED: `node tests/e2e/runtime-control-foolproof-static.spec.js` -> FAIL，预期原因：API 类型中缺少 `foolproof-overview`、候选 ID、`PASS/WARN/BLOCKED/NO_GO` 枚举和事故 DTO。
- RED: `node tests/e2e/runtime-control-foolproof-real-flow.e2e.js` -> FAIL，预期原因：真实运行控制台页面没有站内信告警摘要、决策向导、候选选择、巡检报告和事故抽屉。

### GREEN

- GREEN: 完成 API DTO、组件拆分和页面接线后，组件测试通过，覆盖加载、空态、失败、只读、不可选、危险确认、未知枚举。
- GREEN: 静态契约脚本通过，证明 `index.vue` 不再承载全部业务块，新增组件和 API 类型存在。
- GREEN: Playwright 使用真实登录路径进入 `http://localhost:8081` 或测试服入口，查看运行控制台，执行只读巡检，验证高危动作在缺条件时被阻断且没有发送操作请求。

### REGRESSION

- REGRESSION: 现有 `tests/e2e/runtime-control-static.spec.js` 仍通过，保留 Local/Test/Production、组件矩阵、`PROD` 确认、重启权限。
- REGRESSION: 现有 `tests/e2e/runtime-control-ops-static.spec.js` 仍通过，保留发布测试服、提升正式服、立即备份、回滚版本、恢复数据、日志查看和 `publishScope`。
- REGRESSION: `pnpm ts:check` 或当前任务约定的 TypeScript 检查通过。
- REGRESSION: Playwright 不修改芋道源码租户数据；开发调试只用测试租户，最终只做只读验证。

## 8. Playwright 真实路径要求

- 使用真实前端入口 `http://localhost:8081`；联调测试服时按 `docs/login-access.md` 和 `docs/server-access.md` 使用测试租户。
- E2E 通过页面登录、菜单进入、点击运行控制台，不直接调用 API 完成前置动作；API 只用于最终只读校验。
- 不为测试新增页面隐藏按钮、测试专用 query、测试专用 DOM 文案。
- 若真实后端尚未提供候选、巡检或事故接口，E2E 必须失败并记录缺失接口，不允许 mock 成功。

## 9. Subagent-driven 后续实现分工

- FE-A 架构/API agent：扩展 `src/api/infra/runtimeControl/index.ts`、新增枚举和 DTO、编写静态契约 RED/GREEN。
- FE-B 向导/候选 agent：实现 `OpsDecisionWizard`、`OpsCandidatePicker`、`OpsActionConfirmDialog`，覆盖 AC-03、AC-04、AC-05。
- FE-C 巡检/健康 agent：实现 `OpsInspectionReportPanel`、`OpsBusinessHealthPanel`、`OpsProbeStatusPanel`，覆盖 AC-06、AC-07、AC-08。
- FE-D 容量/备份/事故 agent：实现 `OpsLogDiskRiskPanel`、`OpsBackupDrillPanel`、`OpsIncidentDrawer`，覆盖 AC-09、AC-10、AC-11。
- FE-E 测试 agent：补齐组件测试、静态契约测试和 Playwright 真实路径，不改生产逻辑。
- Reviewer：审查接口自洽、危险确认、权限态、副作用、TDD 证据和 AC-01 到 AC-11 覆盖；未满足不得放行。

## 10. 副作用控制

- 不新增测试专用控件、隐藏入口或只给 E2E 使用的假数据。
- 不隐藏真实失败，不把接口失败、站内信发送失败、缺责任人、缺 manifest、未演练转成 PASS。
- 不绕过生产 `PROD` 确认，不绕过原因字段，不绕过责任人和候选校验。
- 不提供“删除日志”“直接清库恢复”“忽略演练继续恢复”等短期按钮。
- 不修改现有站内信顶部铃铛语义；运行控制台只补充运维告警摘要。
- 不误导用户把站内信当作夜间实时外部告警；文案必须说明“每日登录可见，非外部叫醒”。
- 不修改芋道源码租户数据做调试；真实 E2E 按测试租户执行。
- 不因后端接口缺失而前端造默认数据；缺接口即 BLOCKED。

## 11. AC 映射

| AC | 前端落点 | API 接线 | 测试覆盖 |
| --- | --- | --- | --- |
| AC-01 | `OpsAlertInboxCard` 展示运维站内信告警、严重级别、责任人、runbook、发送状态 | `foolproof-overview.alerts` + 现有站内信未读接口 | 组件测试 + Playwright 查看告警入口 |
| AC-02 | `OpsOwnerMatrixPanel` 展示责任人矩阵并阻断高危动作 | `foolproof-overview.ownerMatrix`，动作提交校验错误回显 | 组件测试缺责任人禁用提交 + E2E 无请求 |
| AC-03 | `OpsDecisionWizard` 按场景推荐动作和证据 | `foolproof-overview.decisionHints` | 组件测试六类场景 + E2E 应用/数据异常路径 |
| AC-04 | `OpsCandidatePicker` 镜像候选单选，不再手填镜像标签 | `GET rollback-candidates`，提交 `selectedImageCandidateId` | 组件测试无输入框 + E2E 非法候选阻断 |
| AC-05 | `OpsCandidatePicker` 备份候选单选，展示快照要求 | `GET restore-candidates`，提交 `selectedBackupCandidateId` | 组件测试未演练不可选 + E2E 恢复阻断 |
| AC-06 | `OpsInspectionReportPanel` 展示发布前/后红黄绿报告 | `POST/GET inspection-reports` | 组件测试缺证据 BLOCKED + Playwright 只读巡检 |
| AC-07 | `OpsBusinessHealthPanel` 展示登录、ERP、MES、文件、API、慢请求、任务失败 | `foolproof-overview.businessHealth` | 组件测试观察值/采样时间 + E2E 面板可见 |
| AC-08 | `OpsProbeStatusPanel` 展示 backend/frontend/website 探针和告警关联 | `foolproof-overview.probes` | 组件测试探针失败 + 静态契约 |
| AC-09 | `OpsLogDiskRiskPanel` 展示日志增长、磁盘阈值、趋势、站内信告警 | `foolproof-overview.capacityRisks` | 组件测试阈值 WARN/NO-GO + 不出现删除按钮 |
| AC-10 | `OpsBackupDrillPanel` 展示备份、演练、可恢复点、manifest、checksum、报告 | `foolproof-overview.backupDrills` + 候选接口 | 组件测试 manifest 缺失不可选 + E2E 可恢复点 |
| AC-11 | `OpsIncidentDrawer` 展示事故处理、验证、复盘、关闭人 | `GET incidents`、`GET incidents/{id}`、`POST close` | 组件测试缺验证不可关闭 + Playwright 事故详情 |

## 12. Reviewer 放行标准

前端实现文档可放行的条件：

- AC-01 到 AC-11 均有明确 UI 落点、API 契约和测试路径。
- 文档采用 BDD + RED/GREEN/REGRESSION + Subagent-driven 分工。
- 逻辑自洽：站内信由后端发送，前端只展示；候选只能选择；高危动作继续保留 `PROD`；缺证据一律阻断。
- 接口清晰：每个组件消费的数据结构和动作提交参数可由后端 worker 实现。
- 副作用受控：不新增 fallback、不造 mock 成功、不新增测试专用控件、不误导外部实时告警。

## 13. Blockers

- 当前前端仓库未发现组件测试依赖；若后续 worker 无法新增或确认组件测试工具链，应阻塞组件测试实施，不能用静态检查替代。
- 新增聚合、候选、巡检、事故接口需要后端文档和实现配合；后端未提供前，前端不得伪造 PASS 数据。

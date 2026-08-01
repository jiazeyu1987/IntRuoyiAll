# PQC 活跃订单切换来源实现

## Task Goal

实现 PQC 检验员切换订单、工序、员工的正式数据来源：

- 订单来源必须是当前活跃订单。
- 工序来源必须是所选活跃订单对应产品的工艺路线工序。
- 员工来源必须是所有 PQC 员工 + PQC 组长。
- PQC 组长列表查看、判定、修正和日志能力不得与生产组长任务冲突。

## Milestones

- [x] 梳理现有 PQC 填写页、组长工作台、活跃订单和工艺路线数据链路。
- [x] 按 BDD 写出订单、工序、员工来源的 RED 测试。
- [x] 实现后端/前端最小正式数据链路，不引入默认全量列表或静默降级。
- [x] 运行定向验证并记录 GREEN/REGRESSION 证据。
- [x] 完成收尾状态与验证报告。
- [x] 优化 PQC 检验提交为正式工序池 PQC 事件，供 PQC 组长列表读取同一份提交明细。

## Expected Verification

- 前端静态契约覆盖 PQC 订单、工序、员工选择来源。
- 前端静态契约覆盖 PQC 检验员提交必须调用正式持久化接口，并保留 `pqcDraft/pqcPieceValues` 供 PQC 组长列表逐项展示。
- 后端定向测试覆盖活跃订单、产品路线工序和 PQC 人员来源。
- 后端定向测试覆盖 PQC 工序池事件、PQC 组长列表、判定、修订和日志链路。
- `pnpm ts:check` 或记录无关历史阻塞。
- `mvn -pl yudao-module-mes -am` 定向测试或记录缺失前置阻塞。

## Current Status

ready_for_closeout

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：本任务使用专用静态合同覆盖 PQC 选择来源，避免被无关宽合同影响。
- 命中 `docs/powershell-memory.md#powershell-maven--d-参数引号门禁`：Maven `-Dtest`、`-Dsurefire.failIfNoSpecifiedTests=false` 均使用 PowerShell 安全引号。
- 命中 `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：已生成 backend/frontend evidence 和 verification-report。

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补齐正式来源接口/调用链。
- `是否存在临时补丁或绕过`：否。

## Closeout Notes

- 实现和验证已完成，但当前工作区存在并行无关改动与分支 ahead 状态；为避免混入其它任务改动，本任务未执行提交/推送。
- 2026-08-01 复核时，近期 DCC 基线提交已将本任务部分实现和证据连同其它任务改动纳入同一类基线提交；未改写历史，本轮全链路审计发现正式提交断点后，当前任务改为 `blocked`。
- 本任务代码改动与生产组长任务不冲突：生产模式仍走设备账号授权工序/员工绑定链路，PQC 模式新增独立的活跃订单、路线工序和 PQC 人员链路。

## 2026-08-01 Full Chain Audit

- 结论：PQC 选择链路与 PQC 组长列表/判定/修订/日志的局部链路通过；PQC 检验员“提交 -> 正式落库 -> PQC 组长列表一致展示”全链路不通。
- 断点：`FrontlineFixedTemplatePanel.vue` 的提交按钮仍只调用 `FrontlineTemplateApi.validatePayload(...)`，随后提示 `已提交`，没有调用 `/mes/pro/feedback/frontline/submit` 或正式 PQC 提交接口。
- 已新增 RED 契约：`IntRuoyiFronted/tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js`，用于锁定“不能 validate-only 后提示已提交”的链路要求。
- 阻塞前置：正式 `frontlineSubmit` 需要 `feedbackPayload`、`recordbookPayload`、`processPoolContext`、`actualEmployeeId`、`signatureId`、`signatureEmployeeId`、`rawPayload`；当前 PQC 上下文未提供 `taskId`、`deviceAccountUserId`、正式签名、`recordbookId` 等来源，不能伪造或默认成功。
- 当前状态改为 `blocked`：在补齐 PQC 正式提交上下文、签名采集和记录本/工序池来源前，不得宣称整条 PQC/PQC 组长链路通顺。

## 2026-08-01 Submit Chain Optimization

- 已新增 PQC 专用正式提交接口：POST /mes/pro/feedback/frontline/device-account/pqc/submit。
- PQC 前端提交流程改为：模板 payload 校验 -> PQC 正式提交 -> 成功提示。
- 后端提交继承当前活跃工序池最新事件的 deviceAccountId/deviceId/workstationId/feedbackSource/recordbookSource，缺任一正式来源即 fail fast。
- PQC rawPayload 顶层保留 pqcDraft、pqcPieceValues、fieldValues、inspectionResult 和选择上下文，PQC 组长列表继续按 originalPayloadJson 解析同一份明细。
- 真实写入型 Playwright E2E 未执行；本轮完成静态契约、TypeScript 和后端 JUnit 定向验证。

## 2026-08-01 Closeout Boundary

- 当前工作区仍存在其它任务并行改动和未跟踪目录；本轮未提交/推送，避免混入非本任务文件。
- project-experience-consolidation 已复核：无新增长期经验文档需求，现有 no-default-success、正式持久化、API-only 禁止和静态契约隔离门禁已覆盖本次可复用经验。

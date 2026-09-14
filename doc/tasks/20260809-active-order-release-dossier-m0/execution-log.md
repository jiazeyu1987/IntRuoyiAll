# M0 执行日志

## User Intent

- 用户要求先完成 V4 的 M0 契约冻结。
- 本轮只完成 M0，不启动 A1-A6 开发，不修改生产代码、数据库或运行环境。

## Milestone Evidence

### M0-00 任务初始化

- 2026-08-09：创建独立 M0 任务目录。
- 2026-08-09：确认 V4 要求 M0 由主 Agent 完成，M0 未通过前不得启动 6 个子 Agent 并行开发。
- 2026-08-09：本任务是文档和代码事实核对，不改变可执行行为，因此不要求生产代码 RED/GREEN；使用结构验证和现有静态合同作为验证证据。

## Observations

- 工作区存在无法读取的历史构建目录 `IntRuoyiBackend/yudao-module-mes/target_corrupt_m4_20260802_1327`；本任务不读取、不修改、不清理该非任务产物。若后续验证工具递归进入该目录导致失败，将按真实环境 blocker 记录。
- 当前 apply 已有接口、申请表、请求/业务双幂等、批次 open/create、release precheck 和 `submitForApproval`，但没有调用三类 writer，成功摘要固定 `signatureEvidenceCount=0`。
- 当前检验进度会把 PQC `SUBMITTED` 计入 100%，M0 冻结为必须 `CONFIRMED` 且存在结构化汇集明细。
- 当前 source hash 主要拼接对象 ID 和 blocker type，不能证明来源值、QA 版本、映射与签名；M0 冻结 `AO_RELEASE_SOURCE_V1` canonical JSON SHA-256。
- 当前路线传统报表任务已支持 `PROCESS_INSPECTION` 和 `LOSS_REPORT` 技术类型并落 `mes_pro_batch_record_execution`；该链路与动态 `formBindings` 分离。
- 当前损耗模板 normalizer 不能证明明确无损耗确认字段，因此 M0 首个成功 fixture 使用正损耗，零损耗在正式字段/映射缺失时阻塞。

### M0-01 接口契约

- 2026-08-09：冻结请求三字段、响应字段、`dossierSummary.sourceSnapshotHash`、blocker 基础/定位字段、双幂等和申请状态。
- 2026-08-09：确认申请表只表达 `BLOCKED/PENDING_RELEASE_APPROVAL`；负责人最终结果继续由 eDHR release transaction 权威表达。

### M0-02 writer 契约

- 2026-08-09：冻结三个小 writer 的公共输入/输出、无副作用 plan/validate 和原子写入约束。
- 2026-08-09：批记录只使用逐工序 BATCH 正式绑定；过程检验只使用 CONFIRMED PQC 汇集和发布 QA 版本；损耗使用正式 feedback + 已签名事件损耗明细精确对账。
- 2026-08-09：冻结 A4/A5 目标为当前批次任务下的传统正式报表 execution，禁止动态 FormCenter/formBindings 替代。

### M0-03 fixture 契约

- 2026-08-09：冻结完整 manifest 字段、正式配置/造数入口和八段真实页面验收路径。
- 2026-08-09：fixture 禁止直接 SQL、直接改进度、mock 和 API-only；缺账号、签名、模板、映射或菜单入口必须 fail fast。

### M0-04 运行时与工程决策

- 2026-08-09：冻结双 100% 正式事实、三 writer 前后顺序、完成性、precheck、`submitForApproval` 和 `RELEASE_APPROVE` 唯一来源。
- 2026-08-09：冻结生成事务整体回滚、BLOCKED 独立短事务和基础设施异常继续抛出的边界。
- 2026-08-09：M0 文件完成，进入结构验证；未启动 A1-A6。

## Verification Evidence

- GREEN: `node yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-static.spec.cjs` -> PASS。
- 首次 schema 验证命令误写文件名，得到预期 `MODULE_NOT_FOUND`；通过 `rg --files` 定位实际文件名后纠正，不把不存在命令记录为 PASS。
- GREEN: `node yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-schema-static.spec.cjs` -> PASS。
- GREEN: `node src/api/mes/pro/processpool/teamLeaderReleaseApplication.static.spec.cjs` -> PASS。
- GREEN: task、execution-log、M0 contract 严格 UTF-8 解码 -> PASS。
- GREEN: M0 必需章节与 15 个关键契约 token 检查 -> PASS。
- GREEN: `git diff --check -- doc/tasks/20260809-active-order-release-dossier-m0` -> PASS。
- 2026-08-09：状态更新为 `ready_for_closeout`。

## Experience Consolidation

- 已按 `project-experience-consolidation` 搜索长期经验归宿。
- 本次可复用规则已经由 `docs/backend-development.md` 的“工序结束放行负责人必须来自 RELEASE_APPROVE”“活跃订单申请放行资料必须只使用正式来源”“PQC 过程检验汇集必须形成最终确认明细”及 `docs/frontend-development.md` 的“前端写入成功与列表刷新失败分层门禁”覆盖。
- 本次新增内容是具体 M0 契约和 Agent 分工，属于任务文档，不重复写入长期经验文档，也不新建经验文件。

## Closeout Evidence

- `task-closeout-cleanup` preview：PASS；keep 4 个任务正式文档，delete/blocked/warnings 均为空。
- `task-closeout-cleanup` apply：PASS；当前是主工作区 `int_main`，不是 linked worktree；未删除文件，未执行 merge/worktree removal。
- 2026-08-09：任务状态更新为 `completed`。

## Blockers

- 当前无 M0 blocker，等待代码事实核对。

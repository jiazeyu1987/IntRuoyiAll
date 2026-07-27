# 关系图批记录表单正式来源修复

## Task Goal

修复工艺路线“流转关系图”中点击“批记录表单”后各工序均显示“未配置”的问题。关系图必须按 `routeProcessId` 读取“工序设置”中绑定的正式批记录表单，并与表单槽位、工序开始特殊节点负责人保持严格隔离。

## Milestones

1. `completed`：保存既有脏工作区基线并同步 `int_main`。
2. `completed`：补充批记录表单、表单槽位和特殊节点来源隔离的 RED 回归测试。
3. `completed`：恢复草稿正式批记录表单快照的保存、元数据读取和逐路线工序契约。
4. `completed`：实现关系图字段值、链接和节点状态统一读取正式批记录表单。
5. `completed`：完成后端、前端、静态合同和真实页面验证。
6. `pending`：沉淀经验、执行 closeout、提交并推送。

## Expected Verification

- 后端测试证明每个 `routeProcessId` 返回自己的正式批记录表单，不读取 `formBindings`。
- 前端静态合同证明“批记录表单”的值、链接和节点状态不再调用表单槽位构建函数。
- 回归测试证明“表单槽位”仍只读取 `formBindings`。
- 回归测试证明“工序开始”特殊节点负责人仍只读取 `batchRecordAttachmentOwners`。
- 前端类型检查、目标 ESLint、MES 目标测试和必要编译通过。
- 通过真实流转关系图页面选择“批记录表单”，验证有绑定工序显示名称且未绑定工序显示“未配置”。

## Current Status

ready_for_closeout

## 业务边界

- `批记录表单`：每个工序在“工序设置”里绑定的正式批记录表单，按对应 `routeProcessId` 读取。
- `表单槽位`：特殊表单或动态表单中心模板绑定，只使用 `formBindings`。
- `工序开始`：特殊节点批记录附件上传人，只使用 `batchRecordAttachmentOwners`。
- 三类来源不得合并、互相替代或提供 fallback。

## Root Cause

- 草稿保存路径 `normalizeCandidateUseConfigSnapshot` 会把请求中的 `batchRecordReports` 强制改成空列表，同时把 `batchRecordBindingSnapshotExplicit` 标记为 `true`。
- 后续读取草稿时会优先读取这份显式快照，因此每个工序的正式批记录表单均变为空。
- 草稿显式快照与当前工序配置混合读取时，报表元数据集合只从当前绑定收集，未包含快照中的正式批记录报表 ID。
- 前端 `buildRecordBindingValue('MAIN')`、`buildRecordBindingLinks('MAIN')` 和节点状态又把 `formBindings` 与 `batchRecordReports` 合并，违反三类配置隔离契约。
- 上述读写修复后，用户新建 V18 草稿仍为空。只读数据库复核确认：路线 `922119` 的当前工序设置和 V5-V18 路线快照均不存在正式 `batchRecordReports`，因此页面按正式来源显示“未配置”是准确结果。
- 批记录导入页只勾选“批记录表单”而未勾选“工艺流程”时，后端只生成批记录版本，不生成携带逐工序正式绑定的路线候选版本；批记录版本升级还没有继承来源版本的 `routeId`，导致后续版本与路线关联逐步丢失。
- 回归区间复核：`d083d962 chore: baseline remaining batch record report updates` 引入已有路线 Word 升级创建候选版本链路，但候选快照未形成完整正式 `batchRecordReports`；`229fad52 融合记录本 worktree 到 int_main` 的路线配置/记录本/表单槽位融合暴露候选快照规范化清空 `batchRecordReports` 的风险。
- 运行态阻塞：V19 候选快照包含正式批记录、表单槽位和附件负责人后超过 MySQL `TEXT` 64KB 上限，本地保存失败；`mes_pro_route_version.route_snapshot_json` 必须升级为 `MEDIUMTEXT`。

## 经验门禁

- 草稿 BATCH 配置必须保持保存与读取对称；不得用当前配置、默认 MAIN 或空列表掩盖草稿快照缺失。
- 前端新增专用最小静态合同，避免依赖无关的大型契约测试。
- 同一 `processId` 可能出现在多个路线工序中，展示契约必须以 `routeProcessId` 为键，不能依赖工序级聚合结果。
- 缺少正式批记录绑定时必须明确显示“未配置”，不得读取 `formBindings` 作为替代。
- 批记录 Word 升版只更新“批记录表单”时也必须生成正式路线候选版本，并把每个工序对应的正式 `batchRecordReports` 写入候选快照；不得只生成批记录版本。
- 保存草稿配置必须保留 `batchRecordReports`，同时保留但隔离 `formBindings` 和 `batchRecordAttachmentOwners`。
- 正式批记录、表单槽位和附件负责人同时进入路线候选快照时可能超过 64KB，路线版本快照列必须支持大于 `TEXT` 的容量。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；除隔离三类读取来源外，在批记录导入生命周期中生成正式路线候选绑定，不在关系图展示层推断或补齐。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260727-route-flow-batch-record-form-source/bug-regression-evidence.md
- doc/tasks/20260727-route-flow-batch-record-form-source/database-schema-evidence.md
- doc/tasks/20260727-route-flow-batch-record-form-source/verification-report.md

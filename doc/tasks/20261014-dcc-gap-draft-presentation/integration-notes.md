# G08 Integration Notes

## A03 最新状态覆盖

以下清单保留第一阶段设计语境，不代表当前全部未接入。Root A03 已授权其中 10 文件接线并实测通过；DetailApplicationHistory 在未改动时归还 G11。当前已实现/未完成/合同缺口与日志可达证据请以 a03-review-notes.md 和 verification-report.md 的 A03 结果为准。未授父页/API 依然不得写。

## 最小端到端合同

正式响应的 `versionNo` 和 `id/controlledFileId/selectedIterationId` 原样进入业务状态。只在用户可见文本调用：

```ts
import { getDccVersionDisplayText } from '../shared/version-display'

getDccVersionDisplayText(selected.versionNo) // A/1-2 -> 草稿；A/1 -> A/1
```

- 模板版本单元格、正文标题、选项 label、确认文案、成功提示和历史摘要均使用相同 helper。
- 选项 `value`/`key` 保留精确原 ID；两个“草稿”选项靠既有编制时间/说明/来源信息区分，不把文案当身份。若现有选项仅有 versionNo，需 Root 核准正式字段再完善辨识信息，不猜默认记录。
- 响应 VO、签名 `objectVersion`、版本对比、状态/权限判定、文件版本校验、快照一致性、请求载荷、筛选和排序均保留原始身份。禁止 `file.versionNo = '草稿'`，禁止改写 `selectedIterationId` 或用 label 寻找首个版本。
- helper 不解析权限、不决定是否可读、不调用 API。G07 的日常禁止草稿正文仍需独立验证。
- 当前 helper 识别 slash 格式的 `-正整数` 工作尾号，涵盖配置多段身份。缺值明确为“未记录”；非字符串明确抛合同错误；正式或已存历史标签保留原文。它不是后端版本合法性校验器。

## Root 待转交清单（全部尚未授权写入）

路径前缀：`IntRuoyiFronted/src/views/dcc/controlled-file/`。下面锚点来自本轮源码；接手必须重读，不能沿用并行 owner 改前行号。

| 具体路径 | 当前锚点 / 必要差异 | 验收影响 / 排期依赖 |
|---|---|---|
| browser/presentation.ts | getBrowserVersionSummary 的 versionText；仅将显示值交 helper，原版本比较不改 | 目录/搜索名称和编号摘要；待浏览排期 |
| browser/index.vue | metadata.versionNo 文本、版本选项 item.versionNo label、checkinRefreshPending 文本、submitCheckin 中 versionIdentity 成功提示 | 目录/搜索/检入提示；value 使用 item.id，检入返回校验及 merge 原始 versionNo 不改；G07/G06 明确窗口后转交 |
| browser/ProjectFolderFileTable.vue | prop="file.versionNo" 版本列改 slot | 项目/文件夹列表；行 file 原对象和点击 ID 不改 |
| detail/index.vue | viewer/详情头版本，版本历史列，签名版本列，getVersionHistoryIdentityText 里的版本提示 | 正文/详情/历史；G10 交还后；签名 objectVersion/selectedVersion 请求原值不改 |
| shared/ControlledFileBasicInfoPanel.vue | 当前执行版本文本 | G10 交还后；正式当前 A/1 不变；核对 G10 是否另加入所选版版本文本 |
| detail/ControlledFileBrowseRecords.vue | 查看版本/最新版本/选用正文小版本；历史 versionNo、selectedIterationVersionNo 列 | G10 交还后；openVersion(row.id) 和 metadata 请求版本值保持原始 |
| revision/DccRevisionPanel.vue | facts.selectedIterationVersionNo、item.versionNo 选项 label | 升版选择；value=String(item.id) 和正式命令不改 |
| detail/DetailApplicationPanel.vue | file.versionNo、所选 iteration 文本，confirmation 及关联显示文案 | 申请送审确认框；G01/G03/G06 公共申请串行窗口；body.versionNo 对比和命令不得改 |
| detail/DetailApplicationHistory.vue | evidence.versionNo 的所选轮次摘要 | 历史申请；controlledFileId/申请/轮次不改 |
| detail/presentation.ts | fileVersionText、版本生命周期时间线 title | 详情/history 字符串摘要；事实 ID、排序不改 |

## “所有用户页面”额外可见点（需另行授权或由当前 owner 接线）

只读检索表明以下已存在版本展示。不能在核心四页接线后就声称全项完成。全路径与锚点列出供 Root 分配；没有修改这些文件。

| 具体路径（同上前缀） | 锚点 / 最小展示接线 |
|---|---|
| browser/ProjectFileRelationsDialog.vue | source.versionNo 文本；selected.versionNo !== expected.versionNo 的身份核验保持原值 |
| detail/DetailRelationsPanel.vue | currentSource.versionNo 文本；G11 owner 串行或自行接线 |
| detail/DetailRelationArrangements.vue | 版本列 prop=versionNo；关系请求与安排身份不改 |
| detail/DetailRelationStageEvidence.vue | versionNoSnapshot 展示列；快照校验和不可变事实不改 |
| relations/DccFileRelations.vue | 关联受控版本 prop=versionNo 列 |
| relations/DccProjectReferences.vue | prop=selectedVersion.versionNo 列 |
| detail/DetailObsoleteApplication.vue | 来源申请/文件版本文案、确认文案、关系表版本；scopeKey/快照一致性不改 |
| detail/DetailLifecycleProjectionRepairDialog.vue | preview.versionNo 的租户/版本展示；修复预检合同不改 |
| detail/PublicationFollowupPanel.vue | followup.versionNo 展示；发布状态/动作合同不改 |
| workbench/presentation.ts | file/item 的 versionNo 展示投影；需确认消费者再限定显示字段，不能污染处理身份 |
| logs/index.vue | currentLog.versionNo 展示；审计原始版本和筛选不改 |
| signatures/index.vue | record versionNo 可见列；签名记录、验证、查询版本及 objectVersion 全部不改 |
| training/task/index.vue | task.versionNo 标题；任务精确版本仍原样请求 |

其它匹配 versionNo 的文件可能只用于身份或正式受控数据；转交时追踪真实模板，不盲目替换所有 `versionNo`。培训、下发、引用等正常受控业务显示正式版本必须保留原值，非法状态仍准确报错。

## 接线真实 RED 方案

1. 在 Root 转交的明确文件范围内，增加实际 Vue SFC 模板渲染测试，输入 A/1-2 并断言可见文本为“草稿”、没有 A/1-2；以当前未接线模板运行应 FAIL。不使用源码包含字符串冒充页面行为。
2. 目录/搜索：分别渲染存储行摘要、项目文件夹版本列、版本选择；选择两个 label 同为“草稿”的不同 ID，执行真实 handler/正式 API wrapper，断言载荷和 URL 保留所选 string ID。日常权限拒绝另由 G07 验证。
3. 正文/详情：实际渲染 G10 新布局；审批/历史传入精确旧 ID，与 current 版本不同；点击“详情”后仍同 ID。正文读取和历史快照不可改到 current。
4. 升版：实际 DccRevisionPanel 的下拉 label 及 submit handler；版本选中后 buildRevisionCommand 保留 selectedIterationId。正式 A/1 的目标及来源显示正常。
5. 历史/审批/提示：真实摘要、历史列和确认 handler 的用户可见文案不泄漏工作版本号；签名上下文仍 raw objectVersion。
6. 更新旧 `dcc-browser-selected-version-display.test.cjs` 中要求 A/1-1 可见的历史断言，同时保留选中版本 != master 当前版本及 ID 精确性断言。`dcc-browser-checkin-minor-only.test.cjs` 的多字母检入成功文案断言也需改为“草稿”，不能删去 MINOR/精确身份/晚失败回归。

上述页面测试尚未实施或执行，因为共享文件未转交；现有 helper 和独立命令/导航单测不代替真实页面接线验收。

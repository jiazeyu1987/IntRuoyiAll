# G10 接入与依赖

## Root A12正式接收
2026-10-15 Root独立17/17 PASS，审查A04后接收G10软件范围。生产三文件冻结；index.vue已按A12转G08 generic显示→G07 context→G01确认任务，BrowseRecords/BasicInfoPanel暂Root保护。G10仅归档，不重跑回归/写源码tests。交还hash与历史执行时点见changed-files/verification-report，不用旧PASS覆盖后续新source。

## A04 已完成 / 文件归还（2026-10-15）
- 两个父入口已正式接入冻结事件，import relation-contract.ts 的唯一DetailRelationApplicationSelection/assertRelationApplicationSelection。selectedRelationApplication初始null，通过computed callback捕获本次文件对象与context；旧context、旧文件、尚未读取成功均不能接受成功事件。
- index.vue使用applicationRoundContextKey和正式loaded sequence；BrowseRecords使用contextKey/同file刷新generation。同步watch清null，History按context重建，卸载清理。明确有nativeBPM或请求BPM/task是申请上下文；否则为明确version view（prop undefined）。
- 本次锁定申请BPM仍受原applicationRoundSelection/History read合同约束，不通过status/version显示文案推断applicationType或attributeRound。传给关系组件的对象是核验后的原四字段，raw版本不改。
- 当前归还index.vue/BrowseRecords/BasicInfoPanel，Root后续按G08 generic显示→G07正文读context→G01确认待办接线。本线程后续不写生产，除非Root再次明确转交。
- 下方“等待冻结”段落为初始设计历史，已被本节覆盖；G11事件接入缺口已完成。G07公共API/Protected viewer上下文接入未由G10承担。

## 导航合同
- 精确正文仍由 route.params.id/getControlledFile(id)/ProtectedPdfViewer controlled-file-id 读取。
- openViewerTraceability 只在 workingBrowserLoadedContext 匹配本次路由/请求序列和 ID 后导航同一个 controlledFileId，保留原 query 中的申请、任务、轮次和来源。traceability=1 是所有来源的只读详情，from 不再被覆盖为 browser。
- 详情返回编码的原正文 fullPath；正文 returnTo 仍为原入口。无显式来源用浏览器真实 history.back；非法显式来源报错而不跳任意列表。
- UI 不用 canPreview=false 推断无详情权限（格式不支持也可能不可预览）；正式 getControlledFile 的服务端详情读取授权及 Protected viewer/附件 API 各自校验仍是权限边界。

## G08 已获准接线
- imported 唯一 shared/version-display.ts getDccVersionDisplayText；没有新增第二 helper，没有覆盖原版本/ID。
- 正文、BrowseRecords 和 BasicInfoPanel 可见文案已用该合同；BrowseRecords usageSource.versionNo 保留原值作为正式权限/关系合同，不能改为草稿文案。
- generic 管理详情中的原版本 slot/成功签名证据展示仍需 G08 统一显示接线（父页后续转交）；不是 G10 的原 viewer 分支修复范围。

## G11 正式事件接线位置（等待冻结）
- index.vue：DetailApplicationHistory（紧接 DetailSignoffAssignment 后）on selection-change → 任务自有 ref；DetailRelationsPanel 传 applicationSelection；reloadAll 开始即 null，避免旧申请关系被保留到新读取。
- ControlledFileBrowseRecords.vue：DetailApplicationHistory on selection-change → 自有 ref；DetailRelationsPanel 传 applicationSelection；contextKey/file watcher 立即 null，卸载清空。
- 当前 relation-contract.ts 已有 DetailRelationApplicationSelection（controlledFileId/applicationType/bpmRound/attributeRound）和 assertRelationApplicationSelection。只接受已核验 History 正式事件，不从 route/query/status 猜选择；选择中/失败/卸载事件 null 直接清理。
- 引用统计仍 getProjectReferenceUsage(selected id)；不要求整份引用清单冻结在提交时刻，不新增 snapshot。

## 验证边界
未获 Root 唯一验证票，不执行 Maven/全类型/大构建；无 E2E 当轮授权，无运行态验收。Root 可在唯一票下全类型/构建，并在用户后续授权真实页面时从列表/搜索/审批/历史验证来源往返与服务端拒绝。

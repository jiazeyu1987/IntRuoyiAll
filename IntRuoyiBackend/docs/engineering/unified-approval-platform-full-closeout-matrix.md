# 统一审批平台 Phase9 全量接入矩阵

## 结论

Phase final 将统一审批平台收口状态分为 `已完成`、`已分类`、`已退役`。`/approval-center` 继续作为跨模块审批任务列表唯一正式入口；业务模块继续保留正式处理页、业务校验、特殊签名、发布、归档、生效和领域回调。

本矩阵不把所有业务详情页强行合并为一个页面，也不把普通业务状态动作伪装成审批任务。缺少真实审批人待办来源、SLA 或外部依赖时，不得 mock 成功、fallback 或静默降级；未形成审批任务的领域动作必须以代码化治理分类固定，防止后续误接或自建审批中心。

## 全量接入矩阵

| 分类 | 对象 | 状态 | 证据 | 缺口 / 边界 | 后续动作 |
| --- | --- | --- | --- | --- | --- |
| 模块 | BPM | 已完成 | `bpm-native-approval` provider；`ApprovalModuleCode.BPM`；统一中心待办/已办/我发起/抄送 | 无 | 季度复核 provider、菜单和真实查询 |
| 模块 | DCC | 已完成 | `dcc-controlled-file-approval` provider；DCC 正式页 `/dcc/controlled-file/detail` | 业务动作保留 DCC 正式页 | 保持特殊签名、发布回调和证据账本在 DCC 域内 |
| 模块 | EDHR | 已完成 | `edhr-work-task-approval` provider 覆盖工作任务审批；`EDHR_DOMAIN_ACTIONS_CLASSIFIED` 固化统一变更、作废、重开、补充、发布为正式页领域动作 | 仅在后续形成真实审批人待办来源时扩展 sourceTaskType | 保持正式页业务校验、特殊签名、归档/发布回调 |
| 模块 | SHOWROOM | 已完成 | `showroom-approval` provider；正式页 `/showroom/approval` | 审批动作保留 Showroom 正式页 | 继续防止 Showroom 自建跨模块审批中心 |
| 模块 | SRM | 已完成 | `srm-supplier-portal-approval` provider 覆盖供应商门户审核；`SRM_DOMAIN_ACTIONS_CLASSIFIED` 固化 supplier-access、procurement-plan、tender expert 为正式页领域动作 | 仅在后续形成真实审批人待办来源时新增 sourceTaskType | 保持 SRM 正式页业务校验、主档生成和准入回调 |
| 模块 | MES_FEEDBACK | 已完成 | `mes-feedback-approval` provider；真实 E2E 证明统一中心跳 `/mes/pro/feedback`；`DECLARED_CAPABILITY_BOUNDARY` 固化只声明真实具备的 `TIMELINE`、`AUDIT` | 未实现的能力不进入声明，不在 UI 或接口中伪装为已具备 | 后续增加通知、签名、证据账本、SLA/催办前必须先补真实规则、实现、测试和 E2E |
| 模块 | CRM_BACKLOG | 已退役 | `CRM_BACKLOG_RETIRED`：CRM 合同/回款提交审批创建 BPM 流程实例；私有待审核合同/回款列表从 backlog 退役 | 审批任务由 BPM provider 进入统一审批中心 | 不得恢复 CRM 私有审批列表 |
| 模块 | ERP_APPROVAL_ACTIONS | 已分类 | `ERP_DOMAIN_ACTIONS_CLASSIFIED`：ERP 审核/反审核是单据状态动作，没有独立审批人待办来源 | 不创建 ERP provider，不伪装为审批任务 | 后续若 ERP 引入真实审批队列，再按统一平台接入 |
| 页面 | `/approval-center` | 已完成 | `src/views/approval-center/index.vue` | 无 | 唯一跨模块审批任务列表入口 |
| 页面 | DCC `approval-tasks` | 已退役 | Phase5 retirement SQL 和退休清单 | 作为菜单入口隐藏，领域详情页保留 | 升级后复查菜单不恢复 |
| 页面 | eDHR `feedback/edhr-approval` | 已退役 | Phase5 retirement SQL 和退休清单 | 作为任务列表隐藏，正式处理页保留 | 升级后复查菜单不恢复 |
| 页面 | Showroom 内部审批中心 | 已退役 | 退休清单标记隐藏菜单 | 正式 `/showroom/approval` 页面保留 | 只作为统一中心跳转目标 |
| 接口 | `/approval-center/tasks/page` | 已完成 | `ApprovalCenterController` | provider 失败必须暴露 | 加入稳定期监控 |
| 接口 | `/approval-center/tasks/timeline` | 已完成 | `ApprovalCenterController` | 未声明 `TIMELINE` 的模块必须失败 | 空轨迹作为异常处理 |
| 接口 | CRM 合同/回款审核接口 | 已退役私有列表 | 提交接口创建 BPM 流程实例；待审核私有列表退役 | 跨模块任务入口只走 `/approval-center` 的 BPM 任务 | 保留 CRM 正式详情页和提交接口 |
| 菜单 | `bpm/task/todo/index` | 已退役 | `20260624_unified_approval_phase5_retire_legacy_menus.sql` | 无 | 防止升级恢复旧菜单 |
| 菜单 | `controlled-file/approval-tasks` | 已退役 | `20260624_unified_approval_phase5_retire_legacy_menus.sql` | 无 | 防止升级恢复旧菜单 |
| 脚本 | `script/unified_approval/governance_scan.py` | 已完成 | Phase9 v2 输出 closeout inventory | 无 | CI/review/季度审计固定运行 |
| 脚本 | `script/tests/test_unified_approval_phase9_closeout.py` | 已完成 | Phase9 回归测试 | 无 | 阻断 DTO/bridge/private center 回归 |

## 稳定期分类项

- `CRM_BACKLOG_RETIRED`：CRM 私有待审核合同/回款列表退役，合同和回款审批任务通过 BPM provider 进入统一中心。
- `ERP_DOMAIN_ACTIONS_CLASSIFIED`：ERP 审核/反审核保持为单据状态动作。
- `EDHR_DOMAIN_ACTIONS_CLASSIFIED`：eDHR 扩展审批动作保留在正式页，除非后续产生真实审批人待办来源。
- `SRM_DOMAIN_ACTIONS_CLASSIFIED`：SRM 扩展审核动作保留在正式页，除非后续产生真实审批人待办来源。
- `DECLARED_CAPABILITY_BOUNDARY`：MES_FEEDBACK 只声明已真实实现的 `TIMELINE` 和 `AUDIT`。

## Fail-Fast 闭环

- 新增私有审批中心、私有待办、重复审批 DTO 或兼容桥接时，`governance_scan.py` 必须返回非零并输出 BLOCKER。
- 新模块涉及审批能力但缺 `ApprovalModuleCode`、`ApprovalModuleIntegrationDeclarations`、`ApprovalTaskProvider`、正式处理页或真实 E2E 时，不得上线审批能力。
- 任何模块声明 `REMINDER`、`SIGNATURE_AUTHORIZATION`、`EVIDENCE_LEDGER` 或 `NOTIFICATION` 前，必须先有真实实现、测试和运行证据。

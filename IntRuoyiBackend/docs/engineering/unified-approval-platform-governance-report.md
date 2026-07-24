# 统一审批平台 Phase7 治理报告

## 结论

统一审批平台已经具备组织级唯一正式标准的基础：`BPM`、`DCC`、`EDHR`、`SHOWROOM`、`SRM`、`MES_FEEDBACK` 均已进入 `ApprovalModuleCode`、`ApprovalModuleIntegrationDeclarations` 和 provider 启动期门禁。`/approval-center` 是跨模块审批任务列表的唯一正式入口，业务模块保留正式处理页和领域动作。

Phase7 的治理重点不是继续扩功能，而是防止后续模块绕过统一平台重新自建审批中心、私有待办或私有审批状态机。

## 已合规模块

| 模块 | provider | 正式处理页 | 治理状态 |
| --- | --- | --- | --- |
| BPM | `bpm-native-approval` | `/bpm/process-instance/detail` | 已合规 |
| DCC | `dcc-controlled-file-approval` | `/dcc/controlled-file/detail` | 已合规 |
| EDHR | `edhr-work-task-approval` | `/mes/pro/edhr-work-task` | 已合规 |
| SHOWROOM | `showroom-approval` | `/showroom/approval` | 已合规 |
| SRM | `srm-supplier-portal-approval` | `/srm/supplier-portal-review` | 已合规 |
| MES_FEEDBACK（MES 报工审批） | `mes-feedback-approval` | `/mes/pro/feedback` | 已合规 |

## 已闭环模块

| 模块 | 风险级别 | 当前证据 | 整改要求 |
| --- | --- | --- | --- |
| CRM 待办 | CLOSED | `CRM_BACKLOG_RETIRED`：合同/回款审批走 BPM provider，backlog 私有待审核列表退役。 | 禁止恢复私有 CRM 审批列表。 |
| ERP 审核/反审核动作 | CLASSIFIED | `ERP_DOMAIN_ACTIONS_CLASSIFIED`：当前按业务单据状态动作治理，没有独立审批任务源。 | 后续只有在产品冻结真实审批任务合同时才新增 ERP provider。 |

## 违规点

当前 Phase7 基线扫描未发现新的 BLOCKER 级私有审批中心、私有待办中心或私有审批状态机。后续若扫描出现以下命中，必须阻断 CI 和代码评审：

- `NO_PRIVATE_APPROVAL_CENTER`
- `NO_PRIVATE_TODO_CENTER`
- `NO_PRIVATE_APPROVAL_STATE_MACHINE`
- `APPROVAL_PROVIDER_DECLARATION_REQUIRED`
- `APPROVAL_MODULE_CODE_REQUIRED`

## 风险级别

- BLOCKER：新增私有审批中心、私有待办、私有审批状态机，或声明审批能力但缺 `ApprovalTaskProvider`。
- HIGH：已有业务审批语义但尚未完成统一平台接入建模。
- MEDIUM：疑似待办或审核语义，需产品分类确认。
- LOW：文档、菜单或命名存在历史痕迹，但不会形成正式入口。

## 运营巡检

Phase7 固化 `script/unified_approval/governance_scan.py`：

```powershell
python script\unified_approval\governance_scan.py --backend-root . --frontend-root ..\yudao-ui-admin-vue3 --format json
```

巡检输出包括：

- 已合规模块。
- BLOCKER 违规点。
- 已闭环风险项。
- 平台级运营检查：provider 启动期 guard、统一前端入口、运营 runbook、SLA/催办能力边界。

## CI / review / design gate

后续模块只要涉及审批能力，评审必须提供：

- 模块码、声明、provider、provider 测试。
- 前端统一中心识别和正式处理页跳转证据。
- 真实 E2E 或明确 blocker。
- 不自建审批中心声明。

缺任一项不得合入。

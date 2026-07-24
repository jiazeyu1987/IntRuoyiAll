# 统一审批平台运营化 Runbook

## 目标

Phase 6 起，统一审批平台不仅是任务聚合入口，也是后续审批模块接入、监控、审计、告警、SLA、超时和催办验证的统一门禁。任何新模块不得绕开 `ApprovalTaskProvider` 自建审批中心，也不得用 mock 成功、默认成功、空列表成功或静默降级掩盖接入缺口。

## 接入运营门禁

每个新模块上线前必须提供以下证据：

1. `ApprovalModuleCode`、`ApprovalModuleIntegrationDeclarations` 和唯一 provider 已注册。
2. provider 单元测试覆盖待办、已办、我发起的、正式处理页跳转、权限边界和轨迹。
3. 前端静态测试证明统一审批中心识别模块来源并跳转模块正式页。
4. Playwright 真实 E2E 从 `/approval-center` 打开真实业务任务到模块正式处理页。
5. `execution-log.md` 记录 `BDD`、`RED`、`GREEN` 和真实 E2E 证据。
6. 缺真实数据、菜单、权限、外部依赖或 SLA 规则时必须记录 blocker，不得 mock 成功。

## 监控

平台运行监控至少检查：

- `/approval-center/modules` 能列出所有声明 provider。
- `/approval-center/tasks/page` 对每个已声明模块至少可执行一次真实查询。
- provider 不得返回 `null` page、`null` list 或无真实业务标识的摘要。
- provider 查询失败必须抛出可诊断错误，不得吞异常或返回默认空成功。

建议验证命令：

```powershell
mvn -pl yudao-module-bpm,yudao-module-srm -am "-Dtest=ApprovalModuleIntegrationGuardTest,ApprovalTaskProviderGovernanceTest,SrmSupplierPortalApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## 审计

审计证据必须能追溯到真实领域对象：

- `ApprovalTaskSummary.sourceTaskType/sourceTaskId/businessKey` 必须定位真实业务记录。
- `ApprovalTaskTimelineEntry.evidenceType/domainReferenceId` 必须指向 BPM 历史任务、签名记录、证据账本或模块真实业务记录。
- SRM 供应商门户审核的审计证据来自 `srm_supplier_portal_application` 的提交和审核字段。
- 缺少提交时间、审核人、审核时间等必需审计证据时必须显式失败。

## 告警

告警策略按 fail-fast 原则执行：

- 启动期 `ApprovalModuleIntegrationGuard` 失败即阻塞服务发布。
- provider 元数据、视图、能力、正式页边界缺失即阻塞。
- 统一中心轨迹为空时前端必须显示错误，不能静默显示成功。
- 真实 E2E 缺菜单、权限、数据或路由时必须在任务日志记录 blocker。

## SLA、超时和催办

统一审批平台只展示和验证模块明确声明的运营能力：

- BPM 原生任务可沿用 BPMN 超时、提醒和消息机制。
- 非 BPM 模块只有在已有真实 SLA 规则、超时状态和催办动作或通知证据时，才允许声明 `REMINDER`。
- 没有真实催办实现或通知链路的模块不得声明 REMINDER；不得声明 REMINDER 后用空方法、默认成功或 mock 成功代替。
- SRM Phase 6 仅声明 `TIMELINE` 和 `AUDIT`，不声明 `REMINDER`、`SIGNATURE_AUTHORIZATION` 或 `EVIDENCE_LEDGER`，因为供应商门户审核当前没有已冻结的 SLA/催办/签名/证据账本业务合同。
- 后续如果业务提供 SRM SLA 或催办规则，必须先补 BDD、RED、GREEN、真实 E2E，再更新 provider 能力。

## Phase 6 SRM 验收点

SRM 供应商门户审核必须满足：

- `SRM` 来源出现在统一审批中心模块列表。
- 待办只展示 `SUBMITTED` 供应商门户申请。
- 待办查询必须校验 `srm:supplier-portal:review` 或 `srm:supplier-portal:audit`。
- 统一中心打开任务时跳转 `/srm/supplier-portal-review?applicationId=<id>`。
- SRM 正式页读取 `applicationId` 并定位真实申请。
- 统一中心不得出现 SRM 通过、驳回按钮。
- 轨迹必须展示提交和审核证据；缺证据时失败。

## Phase 9 上线后稳定期

Phase9 后统一审批平台进入上线后稳定期。稳定期治理不再默认扩大抽象平台能力，而是持续证明每个已声明能力真实可运行；未声明能力必须保持可见边界，不能被文案、空实现或兼容桥接伪装为已完成。

### 异常监控

- 每日检查 `/approval-center/modules`、`/approval-center/tasks/page`、`/approval-center/tasks/timeline`。
- 每个已声明 provider 至少执行一次查询探针；provider 异常、空 `page`、空 `list`、缺 `businessKey` 必须告警。
- `MES_FEEDBACK` 当前只声明 `TIMELINE` 与 `AUDIT`；不得声明 REMINDER、`SIGNATURE_AUTHORIZATION`、`EVIDENCE_LEDGER` 或 `NOTIFICATION`，除非后续补齐真实业务规则、实现、测试和 E2E。

### 失败审计

- 统一中心请求失败必须保留模块码、sourceTaskType、sourceTaskId、登录用户、租户和 providerCode。
- 轨迹为空、业务对象缺失、正式页路由缺失或权限不足时，必须记录为失败审计事件，不得返回默认成功。
- 审计复盘必须能定位到 provider 单测、真实 E2E 或阻塞项。

### 超时/SLA 与催办

- 只有声明 `REMINDER` 的模块才允许展示或触发催办。
- 未冻结 SLA 规则的模块保持 `半完成` 或 `阻塞`，不得用空提醒、默认成功或 BPM 通用提醒冒充领域催办。
- BPM 可沿用 BPMN 超时和消息机制；DCC、eDHR、Showroom 如声明催办，必须有领域规则和证据账本；SRM 与 MES_FEEDBACK 当前不得声明 REMINDER。

### 告警

- `ApprovalModuleIntegrationGuard` 启动失败为发布阻断级告警。
- `governance_scan.py` 出现 `NO_PRIVATE_APPROVAL_CENTER`、`NO_PRIVATE_TODO_CENTER`、`NO_PRIVATE_APPROVAL_DTO`、`NO_COMPATIBILITY_APPROVAL_BRIDGE` 或 `NO_PRIVATE_APPROVAL_STATE_MACHINE` 时，阻断合入。
- 已退役菜单重新出现时，按 P0 回归处理。

### 值守排查清单

1. 确认用户是否从 `/approval-center` 进入，而不是旧待办入口。
2. 确认模块是否在 `ApprovalModuleIntegrationDeclarations` 中声明。
3. 确认 provider 是否注册且能力声明与真实实现一致。
4. 确认任务摘要的 `sourceTaskType/sourceTaskId/businessKey` 能定位真实业务记录。
5. 确认正式处理页路由存在，且业务动作仍在模块正式页完成。
6. 确认失败是否已进入审计记录；若没有，先补失败暴露，再继续排障。

### 问题闭环

- 发现模块绕开统一审批平台时，先运行治理扫描并记录 BLOCKER。
- 如果是审批任务，补 `ApprovalModuleCode`、声明、provider、RED/GREEN 测试和真实 E2E。
- 如果不是审批任务，必须在全量接入矩阵中标记为领域状态动作并说明不接入原因。
- 闭环完成后更新 `unified-approval-platform-full-closeout-matrix.md`、治理报告和季度审计记录。

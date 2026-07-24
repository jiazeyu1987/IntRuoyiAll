# 统一审批平台 Adapter 接入标准

## 适用范围

自 Phase 4 起，任何声明审批能力的新模块或改造模块，都必须接入统一审批平台；不得再自建审批中心、私有待办中心或跨模块审批聚合入口。

统一审批平台负责：

- 待办、已办、我发起的、抄送我的、签名待处理。
- 统一任务摘要 DTO。
- 来源模块识别。
- 统一轨迹入口。
- 通知、催办、审计、签名授权、证据账本能力入口。
- adapter/provider 接入合同和启动期 fail-fast 门禁。

业务模块继续负责：

- 自己的业务校验。
- 特殊签名动作。
- 发布、归档、生效和撤回回调。
- 领域详情页和正式处理页。

## 强制代码合同

每个审批型模块必须完成以下接入点：

1. 在 `ApprovalModuleCode` 中拥有稳定来源模块码。
2. 在 `ApprovalModuleIntegrationDeclarations` 中声明模块、视图、能力和正式处理页边界。
3. 实现且注册唯一的 `ApprovalTaskProvider`。
4. 通过 `ApprovalModuleIntegrationGuard` 的启动期校验。
5. 提供 provider 单元测试、统一中心契约测试和真实 E2E 证据。

`ApprovalModuleIntegrationGuard` 是 Phase 4 起的强制门禁：

- 已声明模块没有 provider：fail fast。
- `ApprovalModuleCode` 没有声明：fail fast。
- provider 未满足声明的视图：fail fast。
- provider 未满足声明的能力：fail fast。
- provider 元数据缺失：fail fast。

不得使用 fallback、mock 成功、空成功或静默降级掩盖接入缺口。

## Phase 6 新模块接入基线

Phase 6 已将 `SRM` 纳入统一审批平台。SRM 的接入样例是 `供应商门户审核`：

- 模块码：`ApprovalModuleCode.SRM`。
- provider：`srm-supplier-portal-approval`。
- 正式处理页：`/srm/supplier-portal-review`。
- 路由参数：`applicationId` 必须定位真实 `srm_supplier_portal_application.id`。
- 权限边界：SRM 待办列表必须校验 `srm:supplier-portal:review` 或 `srm:supplier-portal:audit`，不得把角色审核任务暴露给任意统一中心用户。
- 审批动作：通过、驳回、ERP 供应商主档创建、准入基础档案生成仍由 SRM 正式处理页和 SRM 服务负责，不进入统一审批中心。

后续任何新模块都必须按同样路径接入：先声明模块码和 provider，再用真实领域数据输出 `ApprovalTaskSummary`，最后由统一中心跳转模块正式处理页。未完成声明、provider、契约测试、真实 E2E 或运营化证据的模块不得上线审批能力。

## Provider 输出要求

`ApprovalTaskProvider.page(...)` 必须返回真实领域数据生成的 `ApprovalTaskSummary`：

- `moduleCode` 必须等于 provider 模块码。
- `sourceTaskType/sourceTaskId/businessKey` 必须能定位真实业务任务。
- `businessTitle/businessStatus/currentNodeName` 必须来自真实业务或流程状态。
- `detailRoute/detailQuery` 必须跳转到模块正式处理页。
- `availableActions` 只能表达统一中心可做的入口动作，业务审批动作保留在模块正式页。
- `capabilities` 必须与 provider 声明一致。

`ApprovalTaskProvider.listTimeline(...)` 只有在声明 `TIMELINE` 时才能提供，且必须来自真实业务轨迹、BPM 历史任务、签名记录或证据账本。轨迹缺失必须显式失败。

## 页面边界

统一审批中心只能提供统一摘要、来源筛选、轨迹入口和正式页跳转。不得把 DCC、eDHR、Showroom 或后续模块的审批通过、驳回、发布、归档、生效按钮复制到统一中心。

允许存在模块领域页，例如：

- DCC 正式详情/审批页。
- eDHR 工作任务或批记录审批页。
- Showroom `/showroom/approval` 正式处理页。

这些页面不能升级为新的跨模块审批中心。

## 新模块接入验收

新模块任务文档必须包含：

- BDD 场景：统一中心摘要、来源识别、正式页跳转、轨迹或明确失败。
- RED：provider/声明/摘要/轨迹/页面入口缺失时失败。
- GREEN：声明与 provider 满足 `ApprovalModuleIntegrationGuard`，真实数据可查询。
- REGRESSION：跨模块统一中心回归通过。
- E2E：Playwright 使用真实测试租户登录统一审批中心。

## Phase 7 治理门禁

Phase 7 起，所有涉及审批能力的改动都必须运行仓库级治理扫描：

```powershell
python script\unified_approval\governance_scan.py --backend-root . --frontend-root ..\yudao-ui-admin-vue3 --format json
```

扫描命中以下规则时必须 fail fast，禁止合入：

- `NO_PRIVATE_APPROVAL_CENTER`：新增私有审批中心。
- `NO_PRIVATE_TODO_CENTER`：新增私有待办中心。
- `NO_PRIVATE_APPROVAL_STATE_MACHINE`：新增私有审批状态机。
- `APPROVAL_PROVIDER_DECLARATION_REQUIRED`：provider 缺声明。
- `APPROVAL_MODULE_CODE_REQUIRED`：模块缺稳定来源码。

扫描输出的 `risk_items` 不是通过条件的替代品；HIGH / MEDIUM 风险必须进入治理报告并明确整改路径。

## 当前 Phase 4 基线

- `BPM`：原生 BPM 发起/抄送入口。
- `DCC`：受控文件审批摘要、轨迹、正式页跳转。
- `EDHR`：工作任务审批摘要、轨迹、正式页跳转。
- `SHOWROOM`：变更单审批摘要、轨迹、`/showroom/approval` 跳转。
- `SRM`：供应商门户审核摘要、轨迹、`/srm/supplier-portal-review?applicationId=...` 跳转。

# 统一审批平台新模块 Adapter 模板

## 1. 任务文档清单

在 `doc/tasks/<task-id>/task.md` 中先写：

- 任务目标。
- 经验门禁。
- 设计约束检查。
- BDD 场景。
- RED/GREEN/REGRESSION 验证计划。
- 业务正式处理页边界。
- 不自建审批中心声明。

## 2. RED 先行

先写失败测试：

- `ApprovalModuleIntegrationGuard` 缺声明时失败。
- `ApprovalModuleIntegrationGuard` 缺 provider 时失败。
- provider 缺少声明视图时失败。
- provider 缺少声明能力时失败。
- 统一中心摘要缺 `detailRoute` 或真实业务标识时失败。
- 轨迹声明为 `TIMELINE` 但返回空或 mock 数据时失败。

示例：

```java
IllegalStateException ex = assertThrows(IllegalStateException.class, guard::validate);
assertTrue(ex.getMessage().contains("APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED"));
```

## 3. 模块码与声明

在 `ApprovalModuleCode` 中增加稳定模块码，然后在 `ApprovalModuleIntegrationDeclarations` 中增加声明：

```java
ApprovalModuleIntegrationDeclaration.required(
        ApprovalModuleCode.MY_MODULE,
        "我的模块审批",
        "my-module-approval",
        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE),
        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.AUDIT),
        "/my-module/approval")
```

Phase 6 的真实新模块样例：

```java
ApprovalModuleIntegrationDeclaration.required(
        ApprovalModuleCode.SRM,
        "SRM 供应商门户审核",
        "srm-supplier-portal-approval",
        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE,
                ApprovalTaskViewType.MY_INITIATED),
        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.AUDIT),
        "/srm/supplier-portal-review")
```

SRM 样例的处理页参数是 `applicationId`，provider 必须把它映射为真实 `srm_supplier_portal_application.id`。待办查询必须校验 `srm:supplier-portal:review` 或 `srm:supplier-portal:audit`，不得仅按状态返回所有待审核记录。

## 4. Provider 骨架

```java
@Component
public class MyModuleApprovalTaskAdapter implements ApprovalTaskProvider {

    @Override
    public ApprovalModuleCode getModuleCode() {
        return ApprovalModuleCode.MY_MODULE;
    }

    @Override
    public Set<ApprovalTaskViewType> getSupportedViewTypes() {
        return Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE);
    }

    @Override
    public Set<ApprovalTaskCapability> getCapabilities() {
        return Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.AUDIT);
    }

    @Override
    public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
        // Query real domain tasks and map them to ApprovalTaskSummary.
        // Do not return mock, placeholder or default-success rows.
    }

    @Override
    public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
        // Query real domain timeline/evidence records.
        // Throw explicit error if required evidence is missing.
    }
}
```

## 5. Frontend 边界

统一中心只需要识别模块来源、展示摘要、展示轨迹并跳转：

- `detailRoute` 指向模块正式页。
- `detailQuery` 传递真实业务标识。
- 不在统一中心新增模块审批通过、驳回、发布、归档等业务按钮。

SRM 供应商门户审核示例：

- `detailRoute`: `/srm/supplier-portal-review`
- `detailQuery.applicationId`: 真实门户申请 ID
- 正式处理页保留 `通过`、`驳回`、主档生成和准入档案生成逻辑

## 6. GREEN 与回归

必须通过：

- provider 单元测试。
- `ApprovalModuleIntegrationGuard` 合同测试。
- 统一中心跨模块摘要/轨迹测试。
- 前端静态契约测试。
- Playwright 真实 E2E。
- Phase 7 治理扫描：`python script\unified_approval\governance_scan.py --backend-root . --frontend-root ..\yudao-ui-admin-vue3 --format json`。

缺真实数据、权限、菜单或依赖时记录 blocker；禁止 mock 成功。

## 7. 接入验收清单

- [ ] 模块没有新增私有审批中心、私有待办或私有审批状态机。
- [ ] `ApprovalModuleCode` 增加稳定模块码。
- [ ] `ApprovalModuleIntegrationDeclarations` 增加 provider 声明。
- [ ] provider 输出真实 `ApprovalTaskSummary`，不得 mock、空成功或静默降级。
- [ ] `detailRoute/detailQuery` 指向模块正式处理页。
- [ ] 统一中心不复制模块通过、驳回、发布、归档、生效动作。
- [ ] provider 单元测试、统一中心静态契约和真实 E2E 已记录 RED/GREEN。
- [ ] `execution-log.md` 写入 BDD、RED、GREEN 和真实 E2E 证据。

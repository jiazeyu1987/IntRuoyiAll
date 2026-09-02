# PQC 提交不合格审查入口迁移记录

## Request Summary And Source

- 来源：用户 2026-08-31 当前线程确认。
- 需求：`PQC_SUBMISSION` 的“不合格审查”入口不应再放在一线 PQC 填写页，而应放到 `PQC组长 -> PQC管理` 列表的行操作中，由组长在提交后对对应条目发起。

## Current Baseline Reviewed

- 当前一线 PQC 填写页存在直接发起“不合格审查”的按钮。
- 当前组长页的 `PQC管理` 行操作区已有 `详情 / 复核 / 修改`，但没有 `不合格审查`。
- 当前不合格评审创建接口对 `PQC_SUBMISSION` 需要正式 `batchExecutionId`。
- 当前批次详情页仍保留 `PQC_RELEASE` 的不合格审查入口，属于另一条正式业务链路。

## Classification

- Requirement change: `PQC_SUBMISSION` 不合格审查入口迁移到组长管理页。
- UX behavior change: 入口从一线填写端改为提交后的管理端行操作。
- Data contract change: `PQC管理` 列表必须携带行级 `batchExecutionId`。

## Impact Analysis

### Product Impact

- 一线 PQC 不再直接发起提交类不合格审查。
- 组长在 `PQC管理` 列表可直接对提交后的条目发起不合格审查。
- 入口位置与业务职责一致，减少重复入口。

### Design Impact

- 前端页面需要新增行级按钮与点击跳转。
- 前端一线填写页需要移除提交类审查按钮。
- 批次详情页保留放行链路，不再承担提交类入口。

### Data Impact

- 组长列表必须补出 `batchExecutionId`，来源应来自工单、工艺路线、批号对应的正式 eDHR 批次执行记录。
- 不新增临时字段或默认猜测值。

### API Impact

- `PQC管理` 列表响应增加 `batchExecutionId`。
- 不合格评审创建仍使用 `sourceType=PQC_SUBMISSION`、`sourceId=提交事件ID`、`batchExecutionId=对应批次ID`。

### Test Impact

- 静态契约需要覆盖：一线入口移除、组长行操作新增、`batchExecutionId` 透出、路由参数正确。
- 真实路径验证需要覆盖组长页点击入口进入统一评审页。

### Release And Operations Impact

- 不需要新增外部依赖。
- 需要同步更新前端、后端、权限迁移和静态契约后再做回归验证。
- 若行按钮源码已存在但 `PQC管理` 页面仍不显示，应优先核对 `PQC组长` 角色是否拥有不合格审查隐藏查询/创建按钮权限；不要为显示行按钮而授予独立不合格评审页面菜单或 QA 处置权限。

## Follow-up Experience

- 2026-09-01 复核：`PQC管理` 行操作按钮存在但被 `v-hasPermi` 隐藏时，根因可能是 `pqc_leader_permission` 缺少 `mes:pro-edhr-nonconformance-review:create`。正式修复应给 PQC 组长补隐藏查询/创建按钮菜单，保持入口在行操作区，不新增独立页面菜单，不授予 `dispose` 处置权限；迁移执行后需重新登录或刷新权限缓存。

## Decision

ACCEPT。用户已明确确认新的业务位置和点击路径。

## Required Approvals

- 产品口径：用户当前消息已批准。

## Downstream Skill Reruns

- Frontend feature delivery：迁移入口、补行级按钮、移除一线按钮。
- Backend API delivery：补 `PQC管理` 列表的 `batchExecutionId`。
- BDD/TDD：更新静态契约与真实路径场景。
- Playwright：回归验证组长页点击路径。

## Blockers And Next Action

- 当前无业务阻塞。
- 本轮真实 E2E 前置阻塞：`8081` 前端可访问，但 `48081` 后端无监听，且 `NCR_E2E_PASSWORD` 为空；已完成静态、类型和后端编译验证。

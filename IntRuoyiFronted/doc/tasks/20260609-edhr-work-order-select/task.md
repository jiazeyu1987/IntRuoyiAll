# eDHR 批次执行工单选择改造任务

- Task ID: `20260609-edhr-work-order-select`
- Status: `completed`
- Branch: `int_main`

## 任务目标

将 eDHR 批次执行的“工单ID”手输框改为可搜索下拉选择，只展示有效且未冻结的生产工单；用户可输入工单号关键字远程查询，选中后系统提交真实工单 ID，避免手输不存在的 ID。

## 里程碑

1. RED：新增静态测试，验证弹窗必须使用远程工单选择器、查询已确认且未冻结工单。
2. GREEN：前端弹窗改为远程可搜索下拉，展示工单号/名称/产品/批次/ID，选择后提交真实 ID。
3. GREEN：后端 openOrCreate 对非已确认或已冻结工单 fail fast。
4. REGRESSION：运行前端静态测试、TS 检查和后端 eDHR 批次执行相关测试。

## 预期验证

- `node tests/e2e/edhr-batch-work-order-select-static.spec.js`
- `pnpm e2e:edhr:batch-execution:check`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；查询失败显示错误，不回退到手填无效 ID。
- `是否从根因和长期维护角度解决`：是；前端选择有效工单，后端阻止无效/冻结工单。
- `是否存在临时补丁或绕过`：否；复用现有生产工单分页接口和冻结状态字段。

## 当前状态

- 状态：已完成。
- 已完成：生产工单手填 ID 改为远程可搜索下拉；仅查询已确认且未冻结工单；未选择有效工单时禁止提交。
- 已确认：生产工单分页接口支持 `status` 与 `temporaryFrozen` 筛选；已确认状态为 `MesProWorkOrderStatusEnum.CONFIRMED = 1`。
- 证据：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-work-order-select\result.json`。

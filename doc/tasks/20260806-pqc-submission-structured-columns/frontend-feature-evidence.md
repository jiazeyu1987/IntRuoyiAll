# Frontend Feature Evidence

## Feature Goal

- 报工管理与 PQC 管理提交主列表改为结构化字段展示。
- 删除主列表红框列“生产工单”“PQC”“提交内容”。
- PQC 超出冻结上下限的样本值允许提交，但在列表标红提示。

## Non-Goals

- 不修改后端接口契约。
- 不修改 PQC 提交动作、提交校验或复核接口。
- 不引入 mock 数据、默认成功、静默降级或前端本地伪造字段。

## Requirements And Acceptance

- REQ-1: 提交列表默认列和表格列不再包含 `workOrderCode`、`pqcResult`、`submissionContent`。
- REQ-2: 提交列表包含完成/检验数量、损耗数量、损耗明细、设备和参数明细结构化列。
- REQ-3: PQC 列表参数明细读取 `pqcItemDetails/itemResults`，展示设备、编号、接收标准、检验方法、样本值和判定。
- REQ-4: PQC 数值样本超出 `standardLowerLimit/standardUpperLimit` 时，仅展示红色异常提示，不阻止提交。

## UI Entry Points

- 入口：生产组长报工管理页签、PQC 组长 PQC 管理页签。
- 组件：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 验证：`IntRuoyiFronted/tests/e2e/pqc-submission-structured-columns-static.spec.js`。

## API Contracts And Data States

- 使用现有 `getTeamLeaderSubmissionPage` API。
- 报工字段从正式事件 `originalPayloadJson` 中的 `outputQuantity`、`lossQuantity`、`lossReason*`、`equipmentParameters` 和原始 payload 结构读取。
- PQC 字段从 `originalPayloadJson` 中的 `actualInspectionQuantity`、`pqcDraft.scrapQuantity`、`nonconformanceDescription`、`pqcItemDetails/itemResults` 读取。

## BDD Scenarios

- BDD: 提交列表删除红框列 -> Given 组长打开报工管理或 PQC 管理提交列表 / When 列表渲染提交记录 / Then 主表不再显示“生产工单”“PQC”“提交内容”三列，操作和复核列继续保留。
- BDD: 报工/PQC 主列表展示结构化参数 -> Given 员工提交完成数量、损耗数量、损耗原因、设备和参数 / When 组长查看主列表 / Then 列表以完成/检验数量、损耗数量、损耗明细、设备、参数明细等结构化列展示，不能只展示汇总文本。
- BDD: PQC 超限值红色提示且不阻止提交 -> Given PQC 样本值来自冻结项目明细且超出标准上下限 / When 组长查看 PQC 管理列表 / Then 超限样本值在参数明细列标红显示，且该展示逻辑不改变提交接口或提交校验。

## RED

- `node tests\e2e\pqc-submission-structured-columns-static.spec.js` -> FAIL, expected because the old submission main table still renders the red-box `label="生产工单"` column and unified `提交内容` column.

## GREEN

- pending

## Responsive Accessibility Loading Empty Error Permission

- 响应式：保持现有标准列表模板和列配置，不新增无关布局体系。
- 可访问性：异常参数仍显示具体数字，颜色只是额外提示。
- Loading/Empty/Error：沿用现有列表加载、空态和请求错误处理。
- 权限：不改变路由、菜单、页签或按钮权限。

## E2E Or Component Verification Path

- 当前以静态合同覆盖列表列结构、解析函数和超限展示标记。
- 如需真实页面 E2E，应使用已确认运行态、PQC/生产组长账号和测试租户，不以 API-only 替代。

## Blockers And Follow-Up Skills

- pending

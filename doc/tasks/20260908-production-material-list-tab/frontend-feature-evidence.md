# Frontend Feature Evidence

## Feature goal and non-goals

目标：在活跃订单工序提交详情页新增“生产用料清单”主 tab，按当前显示生产订单号读取 ERP 正式生产用料清单，并按单据展示单据头、明细、创建审核信息。

非目标：不修改 ERP 生产用料清单后端合同；不复制或补造生产用料清单数据；不使用 BOM、领料单、补料单作为替代来源。

## Requirements and acceptance ids

Acceptance:

- PML-001：详情页存在“生产用料清单”主 tab。
- PML-002：按当前显示生产订单号查询 `/erp/production-material-list/page`。
- PML-003：同一生产订单多张生产用料清单按 `sourceBillNo` 分组逐张展示。
- PML-004：显示生产订单号、生产车间、单据编号、产品代码、产品名称、规格型号、单位、生产数量、生产组织。
- PML-005：明细显示序号、物料编码、物料名称、规格型号、单位、图号、应发数量、实际用量、需求日期、仓库、发料方式。
- PML-006：显示创建人、创建日期、审核人、审核日期；源数据没有的字段保持空白。
- PML-007：缺少正式生产用料清单时显示空态，不 fallback。

## UI entry points, routes, components, and owned files

- 入口：生产组长页面 -> 活跃订单池 -> 详情 -> 工序提交详情页。
- 路由：`/mes/pro/process-pool/production-leader` 到 `/mes/pro/process-pool/active-order/:activeOrderId/submission-detail`。
- 页面：`IntRuoyiFronted/src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue`。
- 组件：`IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue`。
- 测试：`IntRuoyiFronted/tests/e2e/team-leader-active-order-production-material-list-tab-static.spec.cjs`。
- 真实页面验证脚本：`doc/tasks/20260908-production-material-list-tab/production-material-list-tab-real.e2e.cjs`。

## API contracts and data states

- API：`ErpProductionMaterialListApi.getPage({ pageNo, pageSize, productionOrderNo })`。
- 返回列表字段使用 `ErpProductionMaterialListVO`，包括 `sourceBillNo`、`productionOrderNo`、`productCode`、`childMaterialCode`、`childMaterialName`、`childMaterialSpecification`、`childUnitName`、`requiredQuantity`、`issueMethod`、`demandTime`、`createTime`。
- 多页数据按分页累计读取。
- 空数据：显示“暂无生产用料清单”。
- 接口异常：显示错误提示。

## BDD scenarios

BDD: 详情页展示生产用料清单 -> Given 当前活跃订单对应的生产订单号在 ERP 生产用料清单列表中存在一张或多张正式单据 When 用户打开活跃订单工序提交详情页并切换到“生产用料清单”tab Then 页面按单据逐张展示单据头、明细表和制单审核信息，明细来自生产用料清单正式列表，不得使用领料单、补料单或 BOM 推断。

## RED command and expected failure

RED: `node IntRuoyiFronted\tests\e2e\team-leader-active-order-production-material-list-tab-static.spec.cjs` -> FAIL，预期失败原因：详情页尚未加载生产用料清单，面板没有“生产用料清单”tab 和分组展示逻辑。

## GREEN command and passing result

- GREEN: `node IntRuoyiFronted\tests\e2e\team-leader-active-order-production-material-list-tab-static.spec.cjs` -> PASS。
- GREEN: `node IntRuoyiFronted\node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p IntRuoyiFronted\tsconfig.relaxed.json` -> PASS。
- GREEN: `PML_E2E_WORK_ORDER_CODE=KDMO-309748-1416202028 node doc\tasks\20260908-production-material-list-tab\production-material-list-tab-real.e2e.cjs` -> PASS。
- GREEN: `PML_E2E_WORK_ORDER_CODE=SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891 node doc\tasks\20260908-production-material-list-tab\production-material-list-tab-real.e2e.cjs` -> PASS after authorized local data copy.

## Responsive, accessibility, loading, empty, error, and permission checks

- Responsive：生产用料清单使用固定表格布局和自动换行，避免左右溢出。
- Accessibility：主 tab 使用 Element Plus tab 语义；E2E 使用 role=tab 定位。
- Loading：生产用料清单加载期间显示 `v-loading`。
- Empty：正式接口返回空列表时显示“暂无生产用料清单”。
- Error：接口异常时显示错误 alert。
- Permission：沿用 `erp:production-material-list:query` 接口权限，未新增前端权限绕过。

## E2E or component verification path

Verification:

- 正向真实页面：租户“芋道源码”，账号 `admin`，通过前端登录，进入生产组长页签，打开活跃订单 `KDMO-309748-1416202028` 的详情，切换“生产用料清单”tab，确认接口返回 1 条且页面显示单据。
- 用户指定订单：经用户授权复制本地测试数据后，`SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891` 真实页面触发正确接口，接口返回 11 条，页面显示单据 `SIM-PML-CODX-PQC-WO05-001`。

## Blockers and follow-up skills

阻塞：无当前阻塞。历史阻塞为目标生产订单没有正式生产用料清单数据，已根据用户授权通过本地库复制测试数据解决。

后续技能：如授权写入本地数据，应使用 `database-schema-delivery` 或对应数据修复流程记录数据变更证据。

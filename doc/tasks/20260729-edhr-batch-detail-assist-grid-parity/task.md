# 20260729 eDHR batch detail assist grid parity

## Task Goal

修复 eDHR 批次执行详情页切换“辅助模式”后主区域显示为扁平字段列表的问题，使其按辅助表单配置中的责任主体、正式行列尺寸、行列坐标和空单元格渲染只读网格，与“辅助表单预览”保持一致。

## Milestones

- [x] 建立任务目录并记录 BDD、验收边界和适用经验门禁。
- [x] 新增聚焦回归合同并取得 RED。
- [x] 实现批次详情辅助模式只读网格。
- [x] 补齐辅助表格行列尺寸保存、响应和运行快照链路。
- [x] 完成定向回归和类型检查。
- [x] 完成真实页面视觉前置核验并记录当前旧样本数据阻塞。
- [x] 完成 cleanup。
- [ ] 完成提交与推送。

## Expected Verification

- `node tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js`
- `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-configured-grid-static.spec.js`
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest#getAndSaveCellRules_suggestsAndPersistsReviewedTypedMetadata" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_freezesAssistRowsInExecutionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsUnopenedBatchRecordWithExecutionSnapshotAssistRows" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 使用 Playwright 从真实批次执行详情页切换辅助模式，核对网格、责任主体、空单元格、文本适配和控制台错误。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；配置保存正式持久化辅助表格行列尺寸，运行快照冻结 `assistGridRowCount/assistGridColumnCount`，详情页按正式 `assistRows` 的责任主体、尺寸和辅助格坐标构建只读网格，不再扁平化字段。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

### eDHR 辅助模式当前工序 assistRows 路由门禁

- Trigger: 批次详情辅助模式、辅助表格预览、`assistRows`、辅助格 rowKey、扁平字段列表。
- Preflight check: 只使用当前执行快照中的正式 `assistRows` 和 `assistGridRowCount/assistGridColumnCount`，按配置生成的辅助格 rowKey 恢复责任主体和行列。
- Blocker: rowKey 无法解析、责任主体无法区分、正式辅助尺寸缺失却宣称与配置预览一致、辅助格被继续扁平化，或测试不能证明网格定位。
- Verification: 聚焦静态合同覆盖 rowKey 解析、责任主体分组、正式尺寸读取、网格容器、空格子和字段定位；真实页面核对配置预览与详情页一致。
- Forbidden action: 禁止用 `formBindings`、默认 `MAIN`、当前登录人、快照全量字段或宽松推断替代正式 `assistRows`。
- Evidence: `docs/frontend-development.md#eDHR 辅助模式当前工序 assistRows 路由门禁`。

### 前端静态契约隔离门禁

- Trigger: 现有宽合同包含并行任务或历史行为，无法稳定证明当前缺陷。
- Preflight check: 新增任务专用最小静态合同，只锁定批次详情辅助网格行为。
- Blocker: 专用合同不能稳定先 RED 后 GREEN，或必须改动无关合同才能通过。
- Verification: 单独记录专用合同 RED/GREEN，并运行相邻合同回归。
- Forbidden action: 禁止修改无关断言掩盖当前缺陷。
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`。

## Ownership Boundary

- Owned:
  - `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue` 中辅助模式只读预览相关区块。
  - `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue` 中辅助表格尺寸保存/回读。
  - `IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts` 中填写规则接口类型。
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/vo/BatchRecordReportCellRulesReqVO.java`。
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/vo/BatchRecordReportCellRulesRespVO.java`。
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java`。
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordCellRuleSupport.java`。
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordRuntimeSnapshotSupport.java`。
  - `IntRuoyiFronted/tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js`。
  - `doc/tasks/20260729-edhr-batch-detail-assist-grid-parity/`。
- Protected:
  - 数据库结构、真实业务数据、写入型数据修复和无关接口。
  - 同一源码文件中现有的批次未提交内容预览、自动打开表单等并行任务改动。

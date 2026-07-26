# Strict TDD Plan

## Purpose and Scope

本计划把 `bdd-scenarios.md` 的行为拆成严格的测试先行顺序。每一步先新增或修改聚焦测试并取得符合预期原因的 RED，再实现最小生产行为取得 GREEN，最后运行相关回归。计划不要求一次性大改现有编辑器或执行页。

## Evidence Reviewed

- 后端现有测试：`MesProBatchRecordCellRuleSupportTest`、`MesProBatchRecordReportControllerTest`。
- 填写责任测试：`MesProEdhrProcessFormPermissionRuleControllerContractTest`、`MesProEdhrProcessFormPermissionRuleServiceImplTest`。
- 执行与任务测试：`MesProBatchRecordExecutionServiceImplTest`、`MesProEdhrBatchExecutionServiceTest`、`MesProRouteFlowConfigServiceImplTest`。
- 现有字段写入范围实现 `MesProBatchRecordExecutionFieldAuditServiceImpl`。
- 前端现有静态合同：`edhr-cell-rules-static.spec.js`、`batch-record-cell-rule-editor-mode-static.spec.js`、`edhr-batch-record-form-list-filler-static.spec.js`、`edhr-assist-fill-mode-static.spec.js`、`edhr-shared-form-binding-static.spec.js`。
- 现有真实辅助填写路径与工作任务入口。

## TDD Sequence

### T01 辅助行模型与规则保存

先在 `MesProBatchRecordCellRuleSupportTest` 增加：

- 合法 `edhrAssistRows` 保存和读回。
- 重复坐标、重复/空 `rowKey`、空描述、空字段、无效坐标和不可填写坐标拒绝。
- 所有可填写单元格的辅助行覆盖校验。
- 移动单元格后只有一个归属。

最小实现目标：

- 增加辅助行 VO 和 JSON 读写支持。
- 扩展 `BatchRecordReportCellRulesReqVO/RespVO`。
- 在现有单元格规则保存服务中集中校验，不创建新存储表。

### T02 类型纠错、下拉框和签名约束

先扩展 `MesProBatchRecordCellRuleSupportTest` 和 `MesProBatchRecordReportControllerTest`：

- `STRING/NUMBER/DATE/SIGNATURE` 保存读回。
- 下拉框使用 `STRING + single + options`。
- 下拉框少于两个有效选项、空值、重复值失败。
- 签名类型缺少启用 `edhrSignature` 失败。

最小实现目标：

- 复用现有规则和签名标记。
- 只增加编辑与校验，不新增 `DROPDOWN` 值类型。

### T03 按辅助行保存填写责任

先扩展 `MesProEdhrProcessFormPermissionRuleControllerContractTest` 和 `MesProEdhrProcessFormPermissionRuleServiceImplTest`：

- `fillAssignments[]` 请求/响应合同。
- 服务端按 `scopeKey` 从 `edhrAssistRows` 生成精确坐标。
- 一个辅助行一条启用规则。
- 缺失辅助行、重复 `scopeKey`、无效候选来源、空解析结果失败。
- 混用 `fillRule` 与 `fillAssignments` 失败。

最小实现目标：

- 在现有权限表增加 `scope_key` 和 `fillable_scope_json`。
- 复用现有候选人解析、版本范围和 `get-by-report/save-by-report`。

### T04 旧规则一次性迁移

先增加迁移合同或数据库测试：

- 旧单条规则转换成 `scopeKey=ALL`。
- `fillable_scope_json` 来自对应不可变版本的可填写字段。
- 迁移后不存在需要运行时双读的旧记录。
- 缺少版本字段源时迁移失败并报告记录 ID。

最小实现目标：

- 提供一次性 SQL/Java 迁移。
- 迁移完成后删除旧请求结构的生产读取分支。

### T05 工作任务责任快照

先扩展 `MesProEdhrBatchExecutionServiceTest`：

- 一个表单仍只创建一个填写任务。
- 候选用户是所有辅助行候选用户并集。
- `responsibilityScopeJson` 冻结 `scopeKey`、解析用户和精确坐标。
- 角色成员随后变化不改变已创建任务。
- 责任快照缺失或损坏时任务打开失败。

最小实现目标：

- 在现有工作任务增加一个责任快照 JSON 字段。
- 不创建每辅助行一个任务。

### T06 当前用户有效范围与后端写入授权

优先在现有字段审计测试类中增加聚焦测试；若没有对应类，再创建 `MesProBatchRecordExecutionFieldAuditServiceImplTest`：

- 当前用户有效范围是路线/批次外层范围与责任单元格范围的交集。
- 同一行不同列可分别授权给不同员工。
- 合法坐标写入成功并产生审计。
- 越权坐标写入失败且值和审计均不变化。
- 不合法责任快照失败，不读取当前模板回退。

最小实现目标：

- 将现有行范围判断扩展为 `sourceTableIndex + rowIndex + columnIndex`。
- 任务打开继续返回现有 `fillableScopeJson` 字段。

### T07 执行快照与辅助模式数据

先扩展 `MesProBatchRecordExecutionServiceImplTest` 和 `MesProEdhrBatchExecutionServiceTest`：

- 执行快照在 `fields` 旁冻结 `assistRows`。
- V2 配置不改变 V1 执行。
- 缺少 `assistRows` 时返回明确的未配置状态，不从字段推导。
- 当前用户只获得自己的辅助行。

最小实现目标：

- 复用现有执行快照 JSON。
- 不新增辅助值表或独立草稿。

### T08 前端统一编辑器

先创建聚焦静态合同 `tests/e2e/edhr-visual-fill-config-static.spec.js`：

- 列表入口文案为“填写配置”。
- 复用 `BatchRecordCellRulesConfirmDialog`。
- 原表与辅助行双向高亮。
- 新建、移动、上移/下移辅助行。
- 类型配置包含文本、数字、日期、签名、下拉框。
- 下拉框选项编辑和签名标记复用。
- 辅助行填写人复用现有 API。
- 一次保存的两步失败不得提示整体成功。

然后扩展 `edhr-assist-fill-mode-static.spec.js`：

- 辅助模式从快照 `assistRows` 渲染。
- 只展示当前用户责任行。
- 原表按精确范围只读。
- 两种模式继续使用 `draftFieldValues`。
- 缺少辅助行时显示“未配置辅助模式”。

最小实现目标：

- 改造现有组件和现有执行页。
- 不创建新的页面、草稿状态或权限客户端缓存。

### T09 真实用户路径

先创建 `tests/e2e/edhr-visual-fill-config-real-flow.e2e.js` 并让它因入口或新行为缺失而 RED，再完成最小实现使其通过。真实路径覆盖管理员配置、员工甲/乙分别填写、越权拒绝、模式共享值、版本隔离和历史执行不变。

## RED Commands

后端第一轮聚焦 RED：

```powershell
mvn "-Dtest=MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am
```

填写责任与任务 RED：

```powershell
mvn "-Dtest=MesProEdhrProcessFormPermissionRuleControllerContractTest,MesProEdhrProcessFormPermissionRuleServiceImplTest,MesProEdhrBatchExecutionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am
```

运行态权限与快照 RED：

```powershell
mvn "-Dtest=MesProBatchRecordExecutionFieldAuditServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am
```

前端聚焦 RED：

```powershell
node tests/e2e/edhr-visual-fill-config-static.spec.js
```

真实 E2E RED：

```powershell
node tests/e2e/edhr-visual-fill-config-real-flow.e2e.js
```

## Expected Failures

| 阶段 | RED 必须失败的原因 |
| --- | --- |
| T01 | VO 尚无 `assistRows`，服务尚未保存或验证 `edhrAssistRows` |
| T02 | 下拉选项和签名联动校验尚未接入统一保存 |
| T03 | `save-by-report` 仍只接受单条 `fillRule` |
| T04 | 旧规则迁移及新字段尚不存在 |
| T05 | 工作任务尚无 `responsibilityScopeJson` |
| T06 | 当前授权仍只比较行号，无法区分同一行不同列 |
| T07 | 执行快照尚无 `assistRows`，辅助模式仍可能从字段推导 |
| T08 | 现有弹窗没有辅助行和按行填写人编辑能力 |
| T09 | 真实 UI 尚不能完成已定义的完整路径 |

若测试因数据库、依赖、登录、端口或历史无关编译错误失败，不得登记为有效 RED；必须单独记录 blocker。

## GREEN Commands

每个阶段使用对应 RED 命令取得 GREEN。完成所有阶段后运行后端聚合回归：

```powershell
mvn "-Dtest=MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportControllerTest,MesProEdhrProcessFormPermissionRuleControllerContractTest,MesProEdhrProcessFormPermissionRuleServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am
```

运行前端聚焦和现有回归：

```powershell
node tests/e2e/edhr-visual-fill-config-static.spec.js
node tests/e2e/edhr-cell-rules-static.spec.js
node tests/e2e/batch-record-cell-rule-editor-mode-static.spec.js
node tests/e2e/edhr-batch-record-form-list-filler-static.spec.js
node tests/e2e/edhr-assist-fill-mode-static.spec.js
node tests/e2e/edhr-shared-form-binding-static.spec.js
```

运行真实用户路径：

```powershell
node tests/e2e/edhr-visual-fill-config-real-flow.e2e.js
```

## Refactor Checks

- 辅助行坐标解析只有一个后端权威实现，权限请求不重复提交坐标。
- 原表与辅助模式只维护一个 `draftFieldValues`。
- 下拉框继续使用 `STRING`，没有新增重复值类型。
- 一个表单仍只有一个填写任务，没有按辅助行制造任务数量膨胀。
- 运行时只读责任快照和执行快照，不读取当前模板作为替代。
- `fillableScopeJson` 的 v1 行范围和 v2 精确单元格解析边界清晰；迁移完成后不保留旧 `fillRule` 双读。
- 前端错误只提示一次，第二步保存失败不能被第一步成功覆盖。
- 新代码没有吞异常、默认成功、模拟数据或扩大权限的 fallback。

## Evidence Log Template

```text
BDD: <scenario id and name> -> Given <precondition> / When <action> / Then <observable result>
RED: <exact command> -> FAIL, <expected missing behavior>
GREEN: <exact command> -> PASS
REGRESSION: <exact command> -> PASS
E2E: <exact command> -> PASS, <test tenant and task-owned data id>
BLOCKER: <precondition check> -> <exact missing prerequisite and impact>
```

有效证据必须记录命令、退出码、目标测试名和失败原因；不能只记录“测试过了”。

## Test Blockers

- 若 `MesProBatchRecordExecutionFieldAuditServiceImplTest` 当前不存在，实现阶段必须先建立聚焦测试类，不能跳过后端权限测试。
- 数据库迁移测试需要包含新增字段和唯一约束的测试 schema。
- Maven 指定测试必须使用带引号的 `-Dtest` 和 `-Dsurefire.failIfNoSpecifiedTests=false` 参数。
- 真实 E2E 缺少正式 UI 入口、测试账号、租户、菜单、运行态或业务数据时必须 fail fast。
- 并发任务正在修改 `ExecutionPage.vue` 时，实现任务必须先隔离到合规 worktree 或等待冲突解除，不能覆盖并发改动。

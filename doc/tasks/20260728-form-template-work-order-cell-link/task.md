# 表单模板生产工单字段链接

## Task Goal

在表单中心模板预览工具栏增加“链接”按钮，复用批记录单元格链接工作台，使生产工单字段可以链接到当前表单模板单元格，并在 MES 动态表单实例创建时按规则预填。

## Milestones

1. 建立任务文档、记录 BDD/TDD 计划和适用门禁。
2. 先补充 RED 静态/后端测试，锁定表单模板链接入口、工作台上下文和运行态预填行为。
3. 实现前端模板页入口和链接工作台参数传递。
4. 实现后端 `FORM_TEMPLATE_VERSION` 作用域、模板单元格解析、规则保存校验和动态表单预填。
5. 运行定向前端静态合同、后端 JUnit/编译验证，完成收尾文档、提交并推送。

## Expected Verification

- `node tests/e2e/form-center-static.spec.js`
- `node tests/e2e/form-template-button-interaction-parity-static.spec.js`
- `node tests/e2e/form-template-independent-button-actions-static.spec.js`
- `node tests/e2e/mes/batch-record-cell-link-static.spec.js`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkControllerTest,MesProEdhrBatchExecutionServiceImplTest" test`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，新增正式 `FORM_TEMPLATE_VERSION` 作用域，不伪装批记录表单，不使用 `formBindings` 替代。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 表单模板三按钮领域边界门禁：表单模板与批记录表单没有直接关系，本任务只新增链接配置入口，不增加批记录绑定字段，不改 `batchRecordBindingStatus/batchRecordReportId`。
- 批记录单元格链接预填落库边界：生产工单字段链接必须在运行态写入正式表单数据链路，不能只靠前端 draft hydrate 或预览接口。
- Schema-backed E2E 迁移与字段可选态门禁：单元格链接工作台必须同时验证字段可见和可选/可保存状态。
- PowerShell / Git 基线门禁：当前任务前已有并行脏改动，已按规则保存为独立基线提交。

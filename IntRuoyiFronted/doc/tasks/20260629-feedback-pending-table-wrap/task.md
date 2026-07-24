# 任务：报工待归属表格长文本改为多行展示

## 任务目标

- 将 `/mes/pro/feedback` 页面中“待归属”列表的长文本列从单行省略号改为多行换行展示。
- 保持正式报工列表、批次摘要区和其他页面现有样式不变。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-schedule-calendar-shift-toggle-buttons\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成并记录最终验证，本次仅处理生产报工待归属列表的表格展示问题，不混入排程日历逻辑。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - PowerShell 读取和写入中文文件时必须显式使用 UTF-8 路径，避免任务文档和测试脚本乱码。
  - 前端改动先写 RED 测试，再做最小实现，不得直接跳到样式修补。
  - 页面样式需保持 IntPP 运维台表格风格；本次仅放开待归属表格目标列的换行能力，不引入额外降级、兜底或整页重绘。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过移除待归属表统一的溢出省略策略，并为目标列增加局部换行类，直接解决单元格文本被截断的问题。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 待归属列表长工单与产品信息可多行展示 -> Given 待归属列表包含长工单号、长产品编码或长产品名称 When 页面渲染该行 Then 单元格内容按多行换行展示，不再以省略号截断。`
- `BDD: 待归属列表长工序信息可多行展示 -> Given 待归属列表的工序列由工序编码和工序名称组成且文本较长 When 页面渲染该列 Then 工序信息允许换行显示完整内容。`
- `BDD: 待归属列表结果说明可多行展示 -> Given 归属结果列包含状态、正式报工编号、归属时间和跳过说明 When 页面渲染 Then 说明信息可按多行展示，不出现统一单行省略。`

## 里程碑

1. M1：创建任务文档、执行日志与前端证据，补 RED 静态契约。`COMPLETED`
2. M2：调整待归属表局部列样式为多行换行。`COMPLETED`
3. M3：运行静态测试与证据校验，回填结果。`COMPLETED`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-pending-table-wrap-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-import-diagnostics-hidden-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-feedback-pending-table-wrap\frontend-feature-evidence.md`

## 当前阻塞

- 无。

## 最终验证结果

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-pending-table-wrap-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-import-diagnostics-hidden-static.spec.js` -> PASS

## 完成结果

- 待归属表已去掉整表统一省略策略，改为仅对长文本目标列启用多行换行。
- 工单、产品编码、产品名称、规格、工序、归属结果这些信息现在显示不下时会自动换行，不再依赖省略号。
- 正式报工页签和待归属批次摘要区未受影响，改动范围保持在当前表格内部。

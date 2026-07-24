# 任务：展厅产品资料 Excel 导入导出补齐音频与关键词中英对照（前端）

- Task ID: `20260701-showroom-product-excel-audio-keyword-roundtrip`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

同步更新展厅产品管理导入弹窗和相关前端合同说明，使其与新的产品资料工作簿结构一致，明确提示：

- 导出/导入包含产品与奖项双语讲解/音频信息；
- 工作簿新增关键词中英对照页签；
- 现有产品、奖项、封面与展柜回导说明不失真。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-zhaojie-feedback-attribution-success-permission-toast-fix\task.md`
- 状态：`completed`
- 处理说明：上一前端任务已完成，不阻塞本次产品资料导入导出说明更新。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 仅收口导入提示和合同文案，不改展厅产品管理既有布局与风格。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。前端提示直接与后端正式合同一致，不保留旧的“奖项英文名、讲解和语音导入后在奖项页签维护”误导文案。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 导入说明与新合同一致 -> Given 用户打开产品 Excel 导入弹窗 / When 页面渲染提示说明 / Then 文案明确包含产品/奖项音频信息和关键词中英对照页签。`

## Milestones

1. M1：建立前端任务文档并确认当前导入说明的旧合同文案。`completed`
2. M2：更新导入提示与必要前端合同说明。`completed`
3. M3：补 evidence 并完成定向核查。`completed`

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-showroom-product-excel-audio-keyword-roundtrip\frontend-feature-evidence.md`

## Current Blockers

- 暂无。

## Final Verification Result

- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-showroom-product-excel-audio-keyword-roundtrip\frontend-feature-evidence.md` -> `PASS`
- 静态核对 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\product\ShowroomProductImportForm.vue` 标准导入说明 -> `PASS`

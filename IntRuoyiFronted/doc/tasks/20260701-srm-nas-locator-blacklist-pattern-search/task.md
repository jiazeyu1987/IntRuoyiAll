# 任务：SRM NAS定位 黑名单与通配搜索（前端）

- Task ID: `20260701-srm-nas-locator-blacklist-pattern-search`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

在 `NAS定位` 页面工具栏新增 `黑名单` 按钮和规则弹框，接入黑名单配置 API；同时给搜索框补充 `*` 通配提示，保持现有单输入交互不变。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-zhaojie-feedback-attribution-permission-fix\task.md`
- 状态：`blocked`
- 处理说明：用户切换到 NAS定位 新需求，已显式阻塞上一前端任务后再开展本任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 工具栏、表格和弹框保持现有运维台样式，只做必要增量。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。页面直接消费正式黑名单接口，不做浏览器本地缓存替代。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 黑名单按钮打开规则弹框 -> Given 用户进入 NAS定位 页面 / When 点击黑名单按钮 / Then 页面弹出黑名单规则编辑弹框，并展示每行一条规则与 *.pyc、*MO13*.pdf 示例。`
- `BDD: 保存黑名单后只提示下次刷新生效 -> Given 用户修改黑名单规则 / When 保存成功 / Then 页面提示“黑名单已保存，刷新索引后生效”，且不自动触发 refresh。`
- `BDD: 搜索框提示保留关键词标签并补充通配说明 -> Given 用户查看 NAS定位 搜索栏 / When 页面渲染 / Then 标签仍为关键词，placeholder 明确支持 *MO13*.pdf 这类通配示例。`

## Milestones

1. M1：建立前端任务文档并确认现有 NAS定位 结构。`completed`
2. M2：补静态合同 RED。`completed`
3. M3：实现 API 与弹框交互。`completed`
4. M4：GREEN 验证并补 evidence。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js`

## Current Blockers

- 暂无。

## Current Status

completed

## Cleanup Candidates

- `doc/tasks/20260701-srm-nas-locator-blacklist-pattern-search/frontend-feature-evidence.md`

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js` -> `PASS`

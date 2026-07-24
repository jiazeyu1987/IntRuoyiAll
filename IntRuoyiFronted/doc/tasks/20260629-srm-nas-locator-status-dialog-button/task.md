# 任务：SRM NAS定位 状态区改为弹框入口（前端）

- Task ID: `20260629-srm-nas-locator-status-dialog-button`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

将 `NAS定位` 页面顶部状态区改为弹框承载，并在“刷新”按钮右侧新增一个 2 字文案按钮作为入口，按钮尺寸与“刷新”按钮一致。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-srm-nas-locator-enter-search-no-autorefresh\task.md`
- 状态：`completed`
- 处理说明：上一前端任务已完成 Enter 搜索与去轮询修复，本次继续在同一页面做交互结构调整。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本次命中 `docs/powershell-memory.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文文档、测试断言和命令输出统一按 UTF-8 处理。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持既有工具栏、表格和分页的 IntPP 风格，仅调整状态区承载方式与按钮布局。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式弹框承载状态区，避免页面首屏被大面积状态块占用。
- `是否存在临时补丁或绕过`：否。
- `按钮文案假设`：采用 2 字文案 `详情`，满足用户“按钮描述 2 个”的要求。

## BDD 场景

- `BDD: 详情按钮打开状态弹框 -> Given 用户进入 NAS定位 页面 / When 点击“详情”按钮 / Then 页面弹出状态弹框，并显示共享范围、索引根路径、任务状态、运行进度与提示条。`
- `BDD: 详情按钮与刷新按钮保持同尺寸 -> Given 页面渲染工具栏 / When 用户观察刷新与详情按钮 / Then 两个按钮使用同一尺寸样式，且详情按钮位于刷新右侧。`

## Milestones

1. M1：建立任务文档与前端证据骨架。`completed`
2. M2：更新静态契约并执行 RED。`completed`
3. M3：实现弹框、按钮和样式。`completed`
4. M4：执行 GREEN 并回填证据。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-srm-nas-locator-status-dialog-button\frontend-feature-evidence.md`

## Current Blockers

- 无。

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-srm-nas-locator-status-dialog-button\frontend-feature-evidence.md` -> PASS

## Completion Result

- 页面新增“详情”按钮，位于“刷新”右侧，使用统一按钮宽度。
- 原状态卡片与提示条已迁入 `NAS索引状态` 弹框中。
- 真实 E2E 脚本已同步通过弹框读取状态，避免后续回归依赖旧 DOM。

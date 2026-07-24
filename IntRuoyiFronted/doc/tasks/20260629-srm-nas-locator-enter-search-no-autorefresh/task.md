# 任务：SRM NAS定位 回车触发搜索且取消前端定时刷新

- Task ID: `20260629-srm-nas-locator-enter-search-no-autorefresh`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

修复 `NAS定位` 页关键词输入框回车行为，使其触发搜索提交而不是触发页面原生刷新；同时移除本页前端定时轮询状态刷新逻辑。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-showroom-award-generate-cover-version\task.md`
- 状态：`blocked`
- 处理说明：已按用户优先级切换将上一任务显式阻塞；本次只处理 SRM NAS定位 页单点回归，不混入奖项任务改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本次命中 `docs/powershell-memory.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文任务文档、测试断言与命令记录统一按 UTF-8 处理。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持既有工具栏布局和操作台视觉，只修复交互语义，不改页面风格。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式表单提交语义替代偶发的原生刷新行为，并移除前端轮询定时器，不保留兼容分支。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 用户按回车时执行搜索提交 -> Given 用户位于 NAS定位 页并在关键词输入框输入关键字 / When 用户按下 Enter / Then 页面执行搜索查询，并保持当前 SPA 页面不发生原生刷新。`
- `BDD: 页面状态仅在显式动作下更新 -> Given 用户打开 NAS定位 页 / When 页面首次加载完成后用户未主动操作 / Then 前端不应自动启动固定间隔的状态刷新。`

## Milestones

1. M1：建立任务文档、门禁和 RED 目标。`completed`
2. M2：修改静态契约并执行 RED。`completed`
3. M3：修复页面表单提交与轮询逻辑。`completed`
4. M4：运行 GREEN、更新证据与收尾。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js`

## Current Blockers

- 无。

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-srm-nas-locator-enter-search-no-autorefresh\frontend-feature-evidence.md` -> PASS

## Completion Result

- 搜索表单已统一为 `submit` 语义，回车与点击“搜索”使用同一查询入口。
- “刷新”按钮已明确为普通按钮，不再作为默认提交目标。
- 页面前端轮询已移除，相关真实 E2E 等待逻辑已同步改为显式 reload。

# 任务：排程日历月格休息/上班按钮改造

## 任务目标

- 将 `/mes/pro/schedule-calendar` 的月格交互从“整卡点击可进入班次编辑”调整为“整卡点击只选中并刷新右侧详情”。
- 在月格底部 `短缺` 按钮右侧新增显式 `休息 / 上班` 切换按钮，沿用现有 `保存规则` 提交流程。
- 保持夜班仍由工艺排产路线配置，不改后端接口与班次枚举。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-schedule-calendar-past-card-gray\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成历史卡片灰底；本次继续在同一月格区域调整点击与班次切换交互，不混入其他页面改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 必须先写 RED 测试，再做最小前端实现。
  - 真实 Playwright 或登录验证前，必须先记录 `GREEN: experience-preflight -> PASS`，并先跑官方登录预检。
  - 前端样式需保持 IntPP 运维台风格，按钮和卡片状态使用克制的浅色语义样式。
  - 页面交互修复不得靠隐藏点击、吞异常或兼容分支掩盖错误行为。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。将班次切换入口显式收口到底部按钮，减少月格整卡点击与隐藏编辑层的语义冲突。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 月格卡片点击只切换选中日期 -> Given 用户打开排程日历 / When 点击某个日期卡片本体 / Then 页面只更新选中日期与右侧详情，不再打开本地班次编辑层。`
- `BDD: 月格底部休息按钮切换成上班 -> Given 今天或未来日期当前生效班次为白班 / When 点击底部休息按钮 / Then 页面把该日期待保存班次改为休息，并将按钮文案切换为上班。`
- `BDD: 月格底部上班按钮切换成休息 -> Given 今天或未来日期当前生效班次为休息 / When 点击底部上班按钮 / Then 页面把该日期待保存班次改为白班，并将按钮文案切换为休息。`
- `BDD: 历史日期不提供班次切换按钮 -> Given 某个日期早于今天 / When 用户查看该日期卡片 / Then 卡片保持历史态淡灰视觉，且不提供可点击的休息/上班切换按钮。`

## 里程碑

1. M1：建立前端任务台账并补 RED 静态契约。`COMPLETED`
2. M2：实现月格整卡点击与休息/上班按钮改造。`COMPLETED`
3. M3：完成静态回归与规则 E2E 回归。`COMPLETED`
4. M4：回填证据、命令记录和收尾预览。`COMPLETED`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-shift-toggle-buttons-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-month-metric-dialog-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-detail-cards-only-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-tabs-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-rules-real-flow.e2e.js`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/schedule-calendar --target-text 排程规则`

## 最终验证结果

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-shift-toggle-buttons-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-month-metric-dialog-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-detail-cards-only-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-tabs-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/schedule-calendar --target-text 排程规则` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-rules-real-flow.e2e.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-schedule-calendar-shift-toggle-buttons\frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-schedule-calendar-shift-toggle-buttons\bug-regression-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260629-schedule-calendar-shift-toggle-buttons --mode preview` -> PASS

## 当前阻塞

- 无。

## 完成结果

- 月格整卡点击已改为“选中日期 + 切回右侧详情 + 刷新当日数据”，不再通过本地点击打开隐藏班次编辑层。
- 月格底部已形成 `短缺` + `休息/上班` 双按钮布局；今天及未来日期可切换，历史日期仍保持淡灰只读态且不暴露切换按钮。
- `休息/上班` 按钮切换继续复用 `rulesForm.dateShiftModeByDate` 与现有 `保存规则` 提交流程，不改后端接口、不改夜班规则来源。

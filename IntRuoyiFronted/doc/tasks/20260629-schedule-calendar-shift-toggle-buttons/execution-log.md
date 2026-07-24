# 执行日志：20260629-schedule-calendar-shift-toggle-buttons

BDD: 月格卡片点击只切换选中日期 -> Given 用户打开排程日历 / When 点击某个日期卡片本体 / Then 页面只更新选中日期与右侧详情，不再打开本地班次编辑层。

BDD: 月格底部休息按钮切换成上班 -> Given 今天或未来日期当前生效班次为白班 / When 点击底部休息按钮 / Then 页面把该日期待保存班次改为休息，并将按钮文案切换为上班。

BDD: 月格底部上班按钮切换成休息 -> Given 今天或未来日期当前生效班次为休息 / When 点击底部上班按钮 / Then 页面把该日期待保存班次改为白班，并将按钮文案切换为休息。

BDD: 历史日期不提供班次切换按钮 -> Given 某个日期早于今天 / When 用户查看该日期卡片 / Then 卡片保持历史态淡灰视觉，且不提供可点击的休息/上班切换按钮。

RED: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-shift-toggle-buttons-static.spec.js -> FAIL，旧实现仍依赖整卡点击打开本地编辑层，缺少显式班次切换按钮。

GREEN: apply_patch -> PASS，月格底部新增显式班次切换按钮，整卡点击改为只选中日期并切回右侧详情，历史日期继续保持只读态。

GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-shift-toggle-buttons-static.spec.js -> PASS

GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-month-metric-dialog-static.spec.js -> PASS

GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-detail-cards-only-static.spec.js -> PASS

GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-tabs-static.spec.js -> PASS

GREEN: experience-preflight -> PASS，node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/schedule-calendar --target-text 排程规则

REGRESSION: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-rules-real-flow.e2e.js -> FAIL，脚本未先切到排程规则标签，且卡片点击后右侧未显式切回详情标签。

GREEN: apply_patch -> PASS，真实规则流脚本改为显式切换排程规则标签后再保存，selectCalendarDate 会切回详情标签并关闭本地编辑层。

GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-rules-real-flow.e2e.js -> PASS

GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-schedule-calendar-shift-toggle-buttons\frontend-feature-evidence.md -> PASS

GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-schedule-calendar-shift-toggle-buttons\bug-regression-evidence.md -> PASS

GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260629-schedule-calendar-shift-toggle-buttons --mode preview -> PASS，默认保留 task.md / execution-log.md，evidence 文件被识别为可清理附属产物。

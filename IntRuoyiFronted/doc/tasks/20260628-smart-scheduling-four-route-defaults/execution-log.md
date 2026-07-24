# 执行日志：智能排产四条路线默认值补齐 前端实现

BDD: 工作台前端可维护默认排产值 -> Given 排产员打开排产员工作台 / When 需要设置路线补齐默认值 / Then 默认产能模式、默认有限产能、默认夜班规则和默认人工资源值必须在工作台前端可维护。

BDD: 工艺排产路线可用默认值补齐缺项 -> Given 某条路线当前没有完整 SCHEDULE 配置 / When 排产员打开工艺排产路线并应用默认值 / Then 前端必须把正式值写回用途配置和排产策略，而不是依赖后端偷偷补数。

BDD: 前端不再隐藏排产默认值 -> Given 当前页面仍存在 `10.5` 一类隐式默认值 / When 用户维护排产数据 / Then 页面必须去掉这类隐藏默认值，改为显式读取正式配置或显式暴露缺项。

GREEN: previous-task-blocked -> PASS，上一前端任务 `20260628-srm-nas-locator` 已标记为 `BLOCKED`。
GREEN: frontend-scope-discovery -> PASS，已确认本轮前端范围集中在排产员工作台、工艺排产路线和资源维护页。
RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-policy-settings-static.spec.js` -> FAIL，排产员工作台尚未显示正式默认排产字段。
RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-smart-scheduling-four-risk-static.spec.js` -> FAIL，`RouteProcessList.vue` 仍保留 `MACHINERY_CAPACITY_SHIFT_HOURS = 10.5` 隐式默认值。
RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-route-use-config-display-static.spec.js` -> FAIL，排产用途配置弹窗尚未提供“应用工作台默认值”入口。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-policy-settings-static.spec.js` -> PASS，工作台已暴露正式默认排产字段。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-smart-scheduling-four-risk-static.spec.js` -> PASS，已移除 `10.5` 隐式默认并补齐显式默认入口。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-route-use-config-display-static.spec.js` -> PASS，排产用途配置弹窗已提供“应用工作台默认值”入口。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-shift-hours-static.spec.js` -> PASS，班次小时仍只在工作台统一维护。
GREEN: tenant1-write-authorization -> PASS，用户已明确授权使用 `芋道源码/admin` 的真实登录会话对 `tenant_id=1` 目标路线执行正式补数。
GREEN: experience-preflight -> PASS，已在前端真实登录/写入前完成授权记录、登录文档门禁核对和现有 MES 请求契约核对。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password admin123 --target-path /mes/pro/scheduler-workbench --target-text 排产设置 --timeout 90000` -> PASS，前端真实登录身份与目标页面已确认。
GREEN: tenant1-real-api-write -> PASS，已按前端正式接口契约完成 `tenant_id=1` 工作台默认值、路线 `SCHEDULE` 用途、路线排产配置和默认人工补齐。

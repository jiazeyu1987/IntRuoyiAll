# 任务：隐藏公司工作台头部指定元素

## 任务目标

- 按截图要求，隐藏公司工作台头部红框内的展示元素：状态标签、“进入版本中心”按钮、发布 scope 标签。
- 保留“编辑公司”和“手动发布展厅”按钮现有能力。
- 不改变后端接口、路由定义、发布 scope 常量或手动发布请求参数。

## BDD 场景

- BDD: 公司工作台头部不显示红框元素 -> Given 用户进入公司信息工作台 / When 页面头部动作区渲染 / Then 不显示状态标签、“进入版本中心”按钮和 `yingtai-showroom / TEST` scope 标签。
- BDD: 隐藏展示元素后仍可编辑和手动发布 -> Given 用户具备原有权限 / When 页面头部动作区渲染 / Then “编辑公司”和“手动发布展厅”仍按原有条件显示并调用原接口。

## 里程碑

- [x] M1：确认上一前端任务已完成，定位公司工作台头部组件和既有测试。
- [x] M2：补充失败测试，约束红框元素不得在头部动作区渲染。
- [x] M3：移除公司工作台头部的指定展示元素，保留编辑与手动发布。
- [x] M4：运行静态测试、类型检查和真实页面验证。
- [x] M5：记录证据、运行收尾清理预览并提交本任务改动。

## 预期验证

- `node --test scripts/showroom-company-header-visibility.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-manual-release-button.test.mjs`
- `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- Playwright 真实页面验证：`http://127.0.0.1:8081/showroom/company`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260529-showroom-company-hide-header-elements/frontend-feature-evidence.md`

## 当前状态

completed

## 当前进展

- 已确认上一前端任务 `20260529-runtime-probe-target-url-visible` 状态为 `completed`。
- 已定位截图对应组件：`src/views/showroom-admin/company/CompanyWorkbench.vue`。
- 已补充 RED 测试，当前旧代码按预期失败。
- 已完成代码修改，头部动作区已移除状态标签、版本中心入口和 release scope 展示。
- 已完成静态测试、类型检查和真实页面验证。
- 已完成证据校验与 task-closeout-cleanup 预览，预览结果无删除项。

## 验证结果

- RED: `node --test scripts/showroom-company-header-visibility.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，旧代码仍显示红框元素。
- GREEN: `node --test scripts/showroom-company-header-visibility.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-manual-release-button.test.mjs` -> PASS。
- GREEN: `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: Playwright 真实页面 `http://127.0.0.1:8081/showroom/company` -> PASS，测试租户登录后仅显示“编辑公司”和“手动发布展厅”。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260529-showroom-company-hide-header-elements/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-showroom-company-hide-header-elements --mode preview` -> PASS，delete/blocked/warnings 均为空。

## Cleanup Keep

- doc/tasks/20260529-showroom-company-hide-header-elements/task.md
- doc/tasks/20260529-showroom-company-hide-header-elements/execution-log.md
- doc/tasks/20260529-showroom-company-hide-header-elements/frontend-feature-evidence.md

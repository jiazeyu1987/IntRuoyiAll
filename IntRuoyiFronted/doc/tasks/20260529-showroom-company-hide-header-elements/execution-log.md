# 执行日志：隐藏公司工作台头部指定元素

BDD: 公司工作台头部不显示红框元素 -> Given 用户进入公司信息工作台 / When 页面头部动作区渲染 / Then 不显示状态标签、“进入版本中心”按钮和 `yingtai-showroom / TEST` scope 标签。

BDD: 隐藏展示元素后仍可编辑和手动发布 -> Given 用户具备原有权限 / When 页面头部动作区渲染 / Then “编辑公司”和“手动发布展厅”仍按原有条件显示并调用原接口。

## 记录

- 2026-05-29 M1：上一前端任务 `20260529-runtime-probe-target-url-visible` 已完成；当前需求限定为公司工作台头部展示调整。
- 2026-05-29 RED：`node --test scripts/showroom-company-header-visibility.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，当前 `CompanyWorkbench.vue` 头部动作区仍渲染状态标签、“进入版本中心”和 `releaseScope.siteKey / releaseScope.stage` scope 标签。
- 2026-05-29 GREEN：`node --test scripts/showroom-company-header-visibility.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-manual-release-button.test.mjs` -> PASS，头部动作区仅保留“编辑公司”和“手动发布展厅”。
- 2026-05-29 GREEN：`node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- 2026-05-29 GREEN：Playwright 真实页面 `http://127.0.0.1:8081/showroom/company` -> PASS，测试租户 `测试租户 / aoteman / admin123` 登录后，头部动作区文本为 `编辑公司 手动发布展厅`，不再显示状态标签、`进入版本中心`、`yingtai-showroom` 或 `TEST`。
- 2026-05-29 INFO：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260529-showroom-company-hide-header-elements/frontend-feature-evidence.md` 首次执行提示证据缺少精确 `RED:` / `GREEN:` 标记；已修正文档格式后重跑。
- 2026-05-29 GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260529-showroom-company-hide-header-elements/frontend-feature-evidence.md` -> PASS。
- 2026-05-29 GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-showroom-company-hide-header-elements --mode preview` -> PASS，keep 仅包含任务文档、执行日志和前端证据，delete/blocked/warnings 均为空。

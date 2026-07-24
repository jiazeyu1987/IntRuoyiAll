# 工艺路线流转图工序可见 Item 持久化执行日志

BDD: 用户可见 item 跨工序保持一致 -> Given 用户在流转关系图选择一个工序并添加“批记录表单” / When 切换到其他工序或刷新页面 / Then 左侧仍显示相同可见 item 列表，内容按当前工序更新。

BDD: 用户可见 item 跨设备保持一致 -> Given 用户已经保存可见 item 配置 / When 同一用户换浏览器或换电脑重新打开工艺路线流转图 / Then 从服务端恢复相同可见 item。

BDD: 不同用户互不影响 -> Given 用户 A 和用户 B 分别登录 / When 用户 A 修改可见 item / Then 用户 B 的可见 item 配置不变。

BDD: 配置接口失败时失败可见 -> Given 用户配置接口加载或保存失败 / When 用户尝试修改可见 item / Then 页面明确报错并阻止继续修改，不使用本地缓存兜底。

RED: `node tests/e2e/mes-route-flow-detail-visible-items-static.spec.js` -> FAIL，`RouteFlowGraphDesigner.vue` 尚未接入服务端用户配置 API，缺少 `loadProcessDetailFieldConfig`。

GREEN: `node tests/e2e/mes-route-flow-detail-visible-items-static.spec.js` -> PASS，流转关系图左侧详情 item 已通过 `mes.pro.route.flow.detailFields` 复用用户配置 API 保存。

GREEN: `node tests/e2e/mes-route-flow-selectable-detail-fields-static.spec.js` -> PASS，字段添加/删除和单项删除契约仍通过。

GREEN: `node tests/e2e/mes-route-flow-link-return-state-static.spec.js` -> PASS，链接返回状态仅保存选中工序与 flow tab，不再把可见 item 写入 URL query。

GREEN: `node tests/e2e/mes-route-flow-default-first-field-static.spec.js` -> PASS，字段选择器默认与回补逻辑仍通过。

GREEN: `node tests/e2e/mes-route-flow-selected-process-detail-static.spec.js` -> PASS，选中工序详情与跳转链接契约仍通过。

GREEN: `node tests/e2e/mes-route-flow-graph-static.spec.js` -> PASS，流转关系图基础静态契约仍通过。

GREEN: `node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/RouteFlowGraphDesigner.vue tests/e2e/mes-route-flow-detail-visible-items-static.spec.js tests/e2e/mes-route-flow-link-return-state-static.spec.js` -> PASS。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-flow-detail-visible-items/frontend-feature-evidence.md` -> PASS，前端功能证据有效。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-flow-detail-visible-items --mode preview` -> PASS，仅建议删除临时 `frontend-feature-evidence.md`。

BLOCKER: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-flow-detail-visible-items --mode apply` -> FAIL，任务文档缺少脚本可识别的 `Current Status: completed`。

GREEN: 补充 `## Current Status / completed` 后重跑 `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-flow-detail-visible-items --mode apply` -> PASS，已删除临时 `frontend-feature-evidence.md`。

GREEN: `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/route --target-text 工艺` -> PASS，真实登录已进入本机工艺路线目标页。

RED: `node tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js` -> FAIL，初版真实 E2E 登录租户选择方式未复用登录前置脚本，未触发 `/system/auth/login` 响应。

GREEN: 修正真实 E2E 登录租户选择逻辑后，`node --check tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js` -> PASS。

RED: `node tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js` -> FAIL，业务断言已通过但脚本把图标与统计请求的 `net::ERR_ABORTED` 也计为失败。

GREEN: 收窄真实 E2E 请求失败断言，仅把 `/system/user-table-column-config` 与非取消失败作为阻塞；`node tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js` -> PASS。验证内容包括测试租户真实登录、添加/读取 `批记录表单` 可见 item、切换工序保持同一组 item、刷新页面保持配置、新浏览器上下文重新登录后仍从服务端恢复。

GREEN: `node node_modules\eslint\bin\eslint.js tests\e2e\mes-route-flow-detail-visible-items-real.e2e.js` -> PASS。

BLOCKER: Git commit -> BLOCKED，当前前端仓存在大量既有脏改，且本任务目标文件与前置任务存在文件级重叠；为避免混入非本任务 hunk，未创建提交。

# 工艺路线流转图工序可见 Item 持久化

## 任务目标

- 工艺路线“流转关系图”左侧工序详情的可见 item 配置，按当前登录用户和租户保存到服务端。
- 同一用户下次打开、换浏览器或换电脑登录后，仍看到相同可见 item；不同用户配置互不影响。
- 任意工序添加或删除可见 item 后，同一张图切换其他工序也使用同一组可见 item，内容仍按当前选中工序实时加载。

## 里程碑

- [x] M1 创建任务记录，读取 PowerShell、经验索引、前端交付和统一前端样式门禁。
- [x] M2 新增 RED 静态契约，锁定服务端用户配置持久化行为。
- [x] M3 在流转关系图组件接入现有用户配置 API 并移除 URL/localStorage 作为字段配置来源。
- [x] M4 运行目标静态测试、类型检查和证据校验。
- [x] M5 更新任务文档、执行日志和提交边界。

## 预期验证

- `node tests/e2e/mes-route-flow-detail-visible-items-static.spec.js`
- `node tests/e2e/mes-route-flow-selectable-detail-fields-static.spec.js`
- `node tests/e2e/mes-route-flow-link-return-state-static.spec.js`
- `node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/RouteFlowGraphDesigner.vue tests/e2e/mes-route-flow-detail-visible-items-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-flow-detail-visible-items/frontend-feature-evidence.md`
- `node --check tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js`
- `node tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js`
- `node node_modules/eslint/bin/eslint.js tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js`

## BDD 场景

- BDD: 用户可见 item 跨工序保持一致 -> Given 用户在流转关系图选择一个工序并添加“批记录表单” / When 切换到其他工序或刷新页面 / Then 左侧仍显示相同可见 item 列表，内容按当前工序更新。
- BDD: 用户可见 item 跨设备保持一致 -> Given 用户已经保存可见 item 配置 / When 同一用户换浏览器或换电脑重新打开工艺路线流转图 / Then 从服务端恢复相同可见 item。
- BDD: 不同用户互不影响 -> Given 用户 A 和用户 B 分别登录 / When 用户 A 修改可见 item / Then 用户 B 的可见 item 配置不变。
- BDD: 配置接口失败时失败可见 -> Given 用户配置接口加载或保存失败 / When 用户尝试修改可见 item / Then 页面明确报错并阻止继续修改，不使用本地缓存兜底。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次只调整左侧详情 item 持久化行为，不做视觉重设计。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；复用现有后端用户配置接口，不新增后端契约，不引入 mock、fallback 或吞异常。
- 高风险动作：真实 E2E 前已完成登录门禁；仅使用本机 `http://localhost:8081`、测试租户 `aoteman` 真实路径验证，不操作测试服/正式服。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。复用现有按租户和登录用户持久化的 `system_user_table_column_config` 能力，避免本地缓存导致跨设备不一致。
- 是否存在临时补丁或绕过：否。

## 当前状态

COMPLETED：流转关系图工序详情可见 item 已改为服务端用户配置持久化；目标静态契约、相邻回归、ESLint、TypeScript 检查和测试租户真实 E2E 均已通过。

## Current Status

completed

## 最终验证结果

- RED: `node tests/e2e/mes-route-flow-detail-visible-items-static.spec.js` -> FAIL，缺少服务端用户配置接入。
- GREEN: `node tests/e2e/mes-route-flow-detail-visible-items-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-selectable-detail-fields-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-link-return-state-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-default-first-field-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-selected-process-detail-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-graph-static.spec.js` -> PASS。
- GREEN: `node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/RouteFlowGraphDesigner.vue tests/e2e/mes-route-flow-detail-visible-items-static.spec.js tests/e2e/mes-route-flow-link-return-state-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-flow-detail-visible-items/frontend-feature-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-flow-detail-visible-items --mode preview` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-flow-detail-visible-items --mode apply` -> PASS，已删除临时 `frontend-feature-evidence.md`。
- GREEN: 登录前置检查 `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/route --target-text 工艺` -> PASS。
- GREEN: `node --check tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js` -> PASS，测试租户真实登录后添加/读取 `批记录表单` 可见 item，切换工序、刷新页面、新浏览器上下文重新登录均保持服务端配置。
- GREEN: `node node_modules/eslint/bin/eslint.js tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js` -> PASS。

## 提交状态

- BLOCKED：当前 `yudao-ui-admin-vue3` 子仓存在大量既有脏改；本任务修改的 `src/views/mes/pro/route/RouteFlowGraphDesigner.vue`、`tests/e2e/mes-route-flow-link-return-state-static.spec.js` 与前置任务存在文件级重叠。
- 为避免把非本任务 hunk 或前置未归属改动混入提交，本轮未执行 git commit。

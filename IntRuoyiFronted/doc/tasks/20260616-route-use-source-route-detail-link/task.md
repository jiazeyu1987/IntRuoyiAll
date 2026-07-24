# 工艺排产/批记录路线源工艺路线钻取

## 任务目标

- 在工艺排产路线与工艺批记录路线列表中，将路线名称做成源工艺路线详情链接。
- 点击路线名称复用现有 `RouteForm.open('detail', row.id)` 打开只读工艺路线详情弹框。
- 路线编码保持现有用途配置入口；负责人继续只读展示 `route/page` 返回的 `ownerName`。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端表格遵循 IntPP 操作台风格，保持紧凑、可扫描、固定列宽。
  - 本次不新增 fallback、降级、静默错误或模拟数据。
  - 真实 Playwright E2E 前必须先在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`；若测试租户登录或真实数据缺失，记录 blocker 并停止真实 E2E。

## 上一任务检查

- 前端仓上一任务 `20260616-route-use-1000-time-formula` 已在本轮标记 `BLOCKED`。
- 阻塞原因：用户切换到本任务，旧任务已完成 RED 但未完成 GREEN，不能与本任务混做或混提交。
- 仓内未跟踪历史任务目录 `20260615-frontend-build-babel-helper-missing/` 保持不修改、不提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。复用现有源工艺路线详情弹框，避免维护第二套详情 UI 和第二负责人来源。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 列表路线名称打开源工艺路线详情 -> Given 用户进入工艺排产路线或工艺批记录路线列表 / When 点击某条路线的路线名称 / Then 系统打开该路线的只读“工艺路线详情”弹框。
- BDD: 路线编码继续打开用途配置 -> Given 用户进入用途路线列表 / When 点击路线编码 / Then 系统仍打开当前用途配置弹框。
- BDD: 负责人来源保持工艺路线 -> Given 用途路线列表从 `route/page` 加载源路线 / When 页面渲染负责人列 / Then 负责人展示 `ownerName`，不从用途配置读取或保存负责人。
- BDD: 详情钻取不写原始路线 -> Given 用户从用途路线列表点击路线名称查看详情 / When 弹框加载源工艺路线数据 / Then 不调用原始路线或工序的新增、修改、删除接口。

## 里程碑

1. M1：建立任务文档、经验门禁和证据草稿。`DONE`
2. M2：RED：新增静态契约测试，确认当前用途页缺少源工艺路线详情弹框入口。`DONE`
3. M3：GREEN：实现列表路线名称详情链接并复用 `RouteForm`。`DONE`
4. M4：REGRESSION：运行静态回归、类型检查、证据校验和收尾预览。`BLOCKED`

## 预期验证

- `node tests/e2e/mes-route-use-source-route-detail-link-static.spec.js`
- `node tests/e2e/mes-process-use-route-tabs-static.spec.js`
- `node tests/e2e/mes-route-use-config-display-static.spec.js`
- `node tests/e2e/mes-edhr-multi-batch-route-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260616-route-use-source-route-detail-link/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260616-route-use-source-route-detail-link --mode preview`

## 当前状态

- 状态：BLOCKED。
- 已完成：前端实现、静态契约、静态回归、类型检查均通过。
- 阻塞原因：真实 Playwright E2E 前置登录失败，`测试租户/aoteman` 使用文档密码 `admin123` 返回“账号密码不正确”。
- 影响：无法完成用户计划中的测试租户真实页面点击链路；不得静默切换租户或账号替代。

## Cleanup Keep

- `doc/tasks/20260616-route-use-source-route-detail-link/frontend-feature-evidence.md`

# 20260803-dcc-category-tabs-first-load

## Task Goal

优化截图红框中 6 个页签（类别列表、审阅矩阵、查看矩阵、目录授权、分发规则、培训规则）首次进入时的加载时间，减少首次进入页面时不必要的子页签初始化和接口请求，提高用户体验。

## Milestones

- [x] 定位 6 个页签所属页面、路由、组件和当前首次加载行为。
- [x] 记录 BDD 场景并补充最小 RED 静态合同，证明未激活页签不应首屏加载。
- [x] 实施最小前端优化，按需加载未激活页签并保留错误显式暴露。
- [x] 运行目标静态合同和必要前端验证，记录结果。
- [x] 完成收尾记录、经验沉淀、提交和推送前准备。

## Expected Verification

- `node tests/e2e/dcc-category-tabs-first-load-static.spec.js`
- `pnpm e2e:dcc:category-tabs-first-load:static`
- `node tests/e2e/dcc-redbox-first-open-performance-static.spec.js`
- `node tests/e2e/dcc-permission-tabs-merge-static.spec.js`
- `node tests/e2e/dcc-permission-distribution-training-tab-static.spec.js`
- `node tests/e2e/dcc-access-rule-menu-retire-static.spec.js`
- `node tests/e2e/dcc-basic-data-global-submenu-static.spec.js`
- `node tests/e2e/dcc-project-code-basic-data-static.spec.js`
- `node tests/e2e/dcc-menu-upload-approval-admin-only-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260803-dcc-category-tabs-first-load/frontend-feature-evidence.md`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本任务只减少未激活页签的首屏加载，不隐藏真实请求错误。
- `是否从根因和长期维护角度解决`：是；计划从页签初始化和组件挂载边界优化，而不是用 loading 文案或延时假象掩盖慢加载。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/frontend-development.md`、`docs/powershell-memory.md`。
- 已读取 `frontend-feature-delivery` 技能及其 `references/frontend-contract.md`。
- `docs/experience-index.md` 已存在；命中并采用 `docs/frontend-development.md#前端静态契约隔离门禁`、`docs/frontend-development.md#前端延迟辅助加载错误归属门禁`、`docs/task-closeout-rules.md#技能证据文件清理前归档门禁`、`docs/powershell-memory.md#同文件并行改动选择性暂存门禁`、`docs/powershell-memory.md#提交后残余改动复扫门禁`。
- 已执行经验沉淀：在 `docs/frontend-development.md#前端页签首屏按需挂载门禁` 合并本次首屏页签懒挂载与可见行查询经验，并在 `docs/experience-index.md` 增加关键词路由。

# 任务：隐藏运行控制台 IntRuoyi 整套行

## 任务目标

运行控制台组件状态矩阵不再显示 `IntRuoyi 整套` 聚合行，只展示可直接访问或重启的实际组件行；保留后端 `intruoyi-full` 状态用于当前发布包等聚合信息计算。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-runtime-control-rollback-target-ui/task.md`
- 状态：`in_progress`
- 处理：该任务为当前工作区已有未跟踪任务，且与本次隐藏聚合行需求无直接生产代码冲突；本任务只修改运行控制台聚合行显示、对应静态回归测试和本任务证据，不改动该任务目录与测试文件。

## BDD 场景

- BDD: 聚合组件行不显示 -> Given 操作员进入运行控制台 / When 查看组件状态矩阵 / Then 表格只显示 `IntRuoyi 前端`、`IntRuoyi 后端`、`Website 前端`，不显示 `IntRuoyi 整套` 聚合行。
- BDD: 隐藏行不丢失发布包来源 -> Given 后端仍返回 `intruoyi-full.currentReleaseTag` / When 页面计算 Test、Production、Backup 的当前发布包 / Then 页面仍优先读取该聚合状态作为发布包来源。

## Milestones

- [x] M1：建立任务文档，确认现有工作区状态与前序任务状态。
- [x] M2：新增 RED 静态回归测试，证明当前页面仍渲染聚合行。
- [x] M3：最小修改运行控制台组件行配置，隐藏 `IntRuoyi 整套` 行。
- [x] M4：运行目标测试、相关回归与 frontend feature evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并按本任务范围提交。

## Expected Verification

- RED/GREEN：`node tests/e2e/runtime-control-full-row-hidden.spec.js`
- GREEN：`node tests/e2e/runtime-control-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js`
- GREEN：frontend feature evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅调整前端显示行，不吞掉接口错误，不切换数据来源。
- `是否从根因和长期维护角度解决`：是。把聚合状态从可见组件行中移除，并用静态回归测试固定契约。
- `是否存在临时补丁或绕过`：否。不新增测试专用控件，不绕过真实接口。

## 当前状态

completed

## 验证结果

- VERIFY：当前工作区存在未跟踪前序任务 `20260604-runtime-control-rollback-target-ui` 与 `20260604-runtime-control-ops-cards-visible`；本任务不修改其文件。
- RED：`node tests/e2e/runtime-control-full-row-hidden.spec.js` -> FAIL，`displayComponentRows` 仍包含 forbidden `intruoyi-full`。
- GREEN：`node tests/e2e/runtime-control-full-row-hidden.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-runtime-control-hide-full-row/frontend-feature-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260604-runtime-control-hide-full-row --mode preview` -> PASS，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 剩余阻塞

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-hide-full-row/frontend-feature-evidence.md`

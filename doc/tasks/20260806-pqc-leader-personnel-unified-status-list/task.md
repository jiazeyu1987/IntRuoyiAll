# PQC 组长人员管理统一启停状态列表

## Task Goal

- 在 PQC 组长的人员管理中删除“启用/禁用”分组或筛选。
- 禁用与未禁用人员显示在同一个列表中。
- 禁用人员姓名以红色展示，同时保留现有状态文字，避免只依赖颜色表达状态。

## Milestones

- [x] 建立任务文档、BDD 场景和前端证据文件。
- [x] 编写专用静态合同，先验证旧行为 RED。
- [x] 修改 PQC 人员管理列表查询与展示逻辑。
- [x] 运行定向静态合同、相邻回归和类型检查。
- [ ] 完成收尾记录、清理预检和提交推送。

## Expected Verification

- `node tests\e2e\pqc-personnel-unified-status-list-static.spec.cjs`
- `node tests\e2e\pqc-leader-personnel-tab-static.spec.js`
- `node tests\e2e\pqc-leader-module-tabs-static.spec.js`
- `node tests\e2e\pqc-leader-standard-list-template-static.spec.js`
- `node tests\e2e\production-personnel-unified-status-list-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/pqc-personnel-unified-status-list-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-leader-personnel-tab-static.spec.js doc/tasks/20260806-pqc-leader-personnel-unified-status-list`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --self-test`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-pqc-leader-personnel-unified-status-list/frontend-feature-evidence.md`

## Applicable Gates

- 前端功能交付：先记录 BDD，再执行 RED/GREEN/REGRESSION，保持现有 API、路由和 UI 模式。
- 前端静态契约隔离门禁：若全量检查存在无关历史问题，使用任务专用最小静态契约证明本需求。
- UTF-8/PowerShell 门禁：中文文档和命令输出按 UTF-8 处理，PowerShell 不使用 `&&`。
- 经验索引：`docs/experience-index.md` 已存在；本任务命中前端静态合同、PowerShell/Git 并发提交相关门禁，按需读取对应文档。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除 PQC 人员状态筛选并使用正式全量列表查询。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

- 功能实现和定向验证已完成：PQC 人员管理删除启用状态筛选，`getPqcPersonnelList()` 改为全量请求，禁用人员姓名使用红色类显示。
- Git closeout 阻塞：并发基线提交 `c4675d197 chore: baseline pre-existing dirty worktree` 已把本任务实现文件和多个非本任务文件一起提交，本任务无法在不重写历史或推送无关文件的前提下形成独立实现提交。
- 未执行 cleanup apply、最终完成标记和 push；需用户决定是否接受当前混合 baseline、另建干净分支重做独立提交，或由当前并发任务统一处理推送。

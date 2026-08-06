# PQC 组长人员管理统一启停状态列表

## Task Goal

- 在 PQC 组长的人员管理中删除“启用/禁用”分组或筛选。
- 禁用与未禁用人员显示在同一个列表中。
- 禁用人员姓名以红色展示，同时保留现有状态文字，避免只依赖颜色表达状态。

## Milestones

- [ ] 建立任务文档、BDD 场景和前端证据文件。
- [ ] 编写专用静态合同，先验证旧行为 RED。
- [ ] 修改 PQC 人员管理列表查询与展示逻辑。
- [ ] 运行定向静态合同、相邻回归、类型检查和证据校验。
- [ ] 完成收尾记录、清理预检和提交推送。

## Expected Verification

- `node tests\e2e\pqc-personnel-unified-status-list-static.spec.cjs`
- `node tests\e2e\pqc-leader-personnel-tab-static.spec.js`
- `node tests\e2e\pqc-leader-module-tabs-static.spec.js`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/pqc-personnel-unified-status-list-static.spec.cjs doc/tasks/20260806-pqc-leader-personnel-unified-status-list`
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

in_progress

- 已创建任务目录，准备补充 BDD 和 RED 静态合同。

# Execution Log

## 2026-08-05

- User intent: 生产组长人员管理删除禁用分组，禁用与未禁用人员显示在同一个列表，禁用人员姓名显示红色。
- Boundary: 仅修改生产人员列表的状态过滤、查询参数和显示名状态样式；保护后端 API、PQC 人员管理、人员写操作、权限、菜单和数据。
- BDD: 禁用与未禁用人员统一展示 -> Given 当前生产组长同时关联已禁用和未禁用人员 When 打开人员管理列表 Then 页面不显示状态分组筛选，请求不按 enabled 过滤，两类人员在同一个分页列表中展示。
- BDD: 禁用人员姓名红色提示 -> Given 统一列表中存在 `enabled === false` 的人员 When 列表渲染显示名 Then 该人员显示名使用红色文字，未禁用人员保持普通文字。
- Preflight: 已读取 `frontend-feature-delivery`、`frontend-contract.md`、`docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md` 和 `docs/experience-index.md`。
- Root cause: `productionPersonnelQuery.enabled` 默认值为 `true`，`refreshProductionPersonnel` 将该字段传给 `getProductionPersonnelList`，模板操作区提供“未禁用 / 已禁用”选择器，导致两类人员分组显示。
- Git preflight: `int_main` 领先 `origin/int_main` 1 个提交；工作区存在多项非本任务并行改动，目标 Vue 文件当前无未提交改动。
- RED: `node tests\e2e\production-personnel-unified-status-list-static.spec.cjs` -> FAIL，生产人员区域仍渲染 `productionPersonnelQuery.enabled` 状态分组控件。
- Concurrent edit: RED 前发现另一个任务正在同一 Vue 文件修改“新增人员同名错误”弹窗区域；文件曾短暂处于 0 字节写入窗口后恢复。已确认其 hunks 与本任务查询、姓名列和样式 hunk 可区分，未覆盖或回滚并行改动。
- Implementation: 删除生产人员状态筛选模板和 `productionPersonnelQuery.enabled`；`refreshProductionPersonnel` 改为无过滤调用 `getProductionPersonnelList()`；显示名按 `row.enabled === false` 增加红色状态类，状态文字列保持不变。

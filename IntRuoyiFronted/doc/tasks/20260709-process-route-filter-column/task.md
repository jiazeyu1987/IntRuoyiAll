# Task: 工序列表展示并筛选所属工艺路线

## 任务目标

- 在 `src/views/mes/pro/process/index.vue` 的工序设置列表增加“所属工艺路线”列。
- 在快速筛选中增加“工艺路线”下拉项，选项来自现有工艺路线精简列表接口。
- 保留 `UnifiedListTemplate`、列配置保存、分页、重置、导出、设备和产能列的既有行为。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文文件读写必须显式 UTF-8，命令不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；列表保持紧凑运维控制台样式。
- 高风险动作：本任务只修改本机前后端源码、静态测试和任务文档；不操作服务器、不修改真实租户数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；前端只消费后端返回的正式路线关系字段，不自造映射。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 工序列表展示所属工艺路线 -> Given 一个工序属于多个工艺路线 / When 打开工序设置列表 / Then “所属工艺路线”列展示全部路线名称。
- BDD: 按工艺路线筛选工序 -> Given 选择工艺路线“压力泵” / When 查询工序列表 / Then 仅展示通过 `mes_pro_route_process` 关联到该路线的工序。
- BDD: 无路线工序仍可展示 -> Given 工序没有任何路线关联 / When 未选择路线筛选 / Then 工序仍显示且路线列为空。

## 里程碑

- [x] M1：建立任务记录并读取经验门禁。
- [x] M2：补 RED 前端静态契约。
- [x] M3：实现路线列与快速筛选。
- [x] M4：运行静态测试和类型检查。
- [x] M5：记录证据和最终状态。

## 预期验证

- `node tests/e2e/mes-pro-process-route-filter-static.spec.js`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-process-route-filter-column/frontend-feature-evidence.md`

## 当前状态

COMPLETED_WITH_COMMIT_BLOCKER：工序设置列表已新增“所属工艺路线”列和“工艺路线”快速筛选，分页请求会传递 `routeId`。前端静态契约、前端类型检查、后端定向测试、前端 evidence 校验和 closeout preview 均已通过。提交被当前混合工作区阻塞：本任务依赖且修改的前端目标文件在本轮开始前已存在未提交改动，无法安全整文件提交而不夹带前序任务内容。

## Cleanup Keep

- `doc/tasks/20260709-process-route-filter-column/frontend-feature-evidence.md`

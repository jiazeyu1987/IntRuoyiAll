# Task: 工序列表展示并筛选所属工艺路线

## 任务目标

- 在“工序设置”列表新增“所属工艺路线”列，展示一个工序关联的多个工艺路线。
- 在快速筛选中新增“工艺路线”下拉筛选，按现有 `mes_pro_route_process` 关系筛选工序。
- 不新增关系表，不引入 fallback、降级或静默兼容逻辑。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文文件读写必须显式 UTF-8，PowerShell 命令不使用 `&&`。
- 项目经验索引：已读取 `docs/experience-index.md`；本任务命中 PowerShell 与前端页面 / 表格 / 样式门禁。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；列表改动必须保持标准列表模板、紧凑表格和既有操作逻辑。
- BDD/TDD：本任务按 RED -> GREEN 记录后端测试、前端静态契约与类型检查证据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；复用现有 `mes_pro_route_process` 关系反查路线，不新增重复关系。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 工序列表展示所属工艺路线 -> Given 一个工序属于多个工艺路线 / When 打开工序设置列表 / Then “所属工艺路线”列展示全部路线名称。
- BDD: 按工艺路线筛选工序 -> Given 选择工艺路线“压力泵” / When 查询工序列表 / Then 仅展示通过 `mes_pro_route_process` 关联到该路线的工序。
- BDD: 无路线工序仍可展示 -> Given 工序没有任何路线关联 / When 未选择路线筛选 / Then 工序仍显示且路线列为空。

## 里程碑

- [x] M1：建立任务记录并读取经验门禁。
- [x] M2：补 RED 后端测试与前端静态契约。
- [x] M3：实现后端分页筛选与路线列表返回。
- [x] M4：实现前端路线列与快速筛选。
- [x] M5：运行目标验证、closeout preview 并记录最终状态。

## 预期验证

- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests/e2e/mes-pro-process-route-filter-static.spec.js`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-process-route-filter-column/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260709-process-route-filter-column/backend-api-evidence.md`

## 当前状态

COMPLETED_WITH_COMMIT_BLOCKER：后端分页已支持 `routeId` 筛选并返回 `routeList`，前端工序设置列表已新增“所属工艺路线”列和“工艺路线”快速筛选。后端定向测试、前端静态契约、前端类型检查、后端/前端 evidence 校验和 closeout preview 均已通过。提交被当前混合工作区阻塞：本任务依赖且修改的后端/前端目标文件在本轮开始前已存在未提交改动，无法安全整文件提交而不夹带前序任务内容。

## Cleanup Keep

- `doc/tasks/20260709-process-route-filter-column/backend-api-evidence.md`

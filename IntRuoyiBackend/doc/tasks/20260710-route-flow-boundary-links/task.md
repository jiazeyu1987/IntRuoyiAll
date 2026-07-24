# 工艺路线边界关系持久化与校验

## 任务目标

- 新增 START/END 边界关系持久化契约，并纳入现有流转关系图查询、校验、保存、复制和删除事务。
- START 至少连接一个首工序且可连接多个；END 只能连接唯一末工序。
- 普通工序允许多个前置汇合、最多一个后续；所有工序必须从 START 可达并可到达 END。
- 新增正式 SQL 迁移和确定性历史数据回填，不运行时推断、不静默修复无效图。
- 迁移正式移除与多前置汇合模型冲突的旧目标唯一索引，并保留普通查询索引。
- 路线版本号按末尾数字排序，确保 V10 之后连续保存生成 V11，而非再次生成 V10。

## 前置任务检查

- 当前隔离分支从后端 `int_main` 干净 HEAD 创建。
- 当前 HEAD 未包含同一流转图服务的未完成任务文档；主工作区无关脏改不进入本分支。

## 工作区与运行目标

- 分支：`codex/20260710-route-flow-boundary-links`
- 后端端口：`48094`
- 数据库：`127.0.0.1:23306/ruoyi-vue-pro`
- Redis：`127.0.0.1:26379`
- 不操作测试服、正式服或备份服。

## BDD 场景

- BDD: 保存多个开始边界关系 -> Given 路线有多个无普通前置工序且最终汇合 / When 保存 START 到全部首工序及唯一 END 关系 / Then 校验通过并原子持久化。
- BDD: 允许多前置汇合 -> Given 两条开始分支指向同一汇合工序 / When 校验普通工序关系 / Then 多前置合法且普通工序仍最多一个后续。
- BDD: 拒绝边界不一致 -> Given START 未覆盖全部首工序或 END 不对应唯一末工序 / When 校验关系图 / Then 返回明确错误且不写入。
- BDD: 回填已有有效线性图 -> Given 历史路线已有唯一首尾和普通关系 / When 执行迁移 / Then 创建 START 和 END 边界关系；无关系或多首多尾路线不回填成功关系。
- BDD: 复制与删除边界关系 -> Given 来源路线已有边界关系 / When 复制或删除路线 / Then 边界关系按新工序 ID 映射复制或同步删除。

## 里程碑

1. [completed] 完成任务记录和 RED 服务/SQL 契约测试。
2. [completed] 新增 SQL、DO、Mapper 和 API VO。
3. [completed] 实现查询、校验、保存、复制、删除和草稿 ID 映射。
4. [completed] 运行定向单测、Schema 契约和 migration policy gate。
5. [completed] 启动隔离后端并支持真实前端 E2E。
6. [completed] 完成提交、快进融合和融合后复验。
7. [completed] 清理任务附属产物、停止隔离服务并移除 worktree。

## 预期验证

- `mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessFlowServiceImplTest,MesProRouteProcessFlowBoundaryEdgeServiceTest" -DskipITs test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionMapperTest,MesProRouteVersionAndCopyTest,MesProRouteProcessFlowServiceImplTest,MesProRouteProcessFlowBoundaryEdgeServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 -m pytest script/tests/test_mes_route_process_flow_boundary_edge_sql.py -q`
- `mvn -pl yudao-server -am -DskipTests package`
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root <absolute-sql-root> --sql-file <base> --sql-file <graph-dependency> --sql-file <boundary-migration> --output doc/tasks/20260710-route-flow-boundary-links/migration-policy-gate.json`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260710-route-flow-boundary-links/backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260710-route-flow-boundary-links/database-schema-evidence.md`

## 经验门禁

- PowerShell / UTF-8：已读取 `docs/powershell-memory.md`，SQL 和 Markdown 使用 UTF-8 无 BOM，修改后回读验证。
- Worktree：已读取 `docs/worktree-memory.md`，后端运行端口固定 `48094`，不接管主工作区 `48081`。
- 数据库：实施 SQL 前以真实本机数据库 `SHOW TABLES` / `DESCRIBE` 只读核对现有表结构。
- 发布迁移：SQL 必须声明 release-migration 元数据、显式依赖和回滚方式，并通过 migration policy gate。
- BDD/TDD：先扩展失败测试，再最小实现，记录 RED、GREEN、REGRESSION。
- 无 fallback：边界关系缺失或不一致时明确校验失败，不从普通边静默推导运行结果。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；新增独立边界关系实体并纳入统一图版本事务，同时消除旧单前置索引和字符串版本排序两个真实阻塞。
- 是否存在临时补丁或绕过：否。

## 当前状态

COMPLETED：后端实现与迁移已快进融合到 `int_main`；22 个定向测试、5 个 SQL 契约测试、migration policy gate、打包和真实 E2E 均通过；测试路线恢复原拓扑，隔离服务、任务产物、worktree 和任务分支均已清理。

## Current Status

completed

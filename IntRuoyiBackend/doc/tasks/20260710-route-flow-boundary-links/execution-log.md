# 执行日志

INFO: experience-index -> matched `docs/powershell-memory.md`, `docs/worktree-memory.md`, backend API contract, database schema contract, and BDD guidance.

GREEN: experience-preflight -> PASS，已确认后端分支与 worktree 干净、端口 `48094` 未监听，数据库只允许本机只读核对和测试租户验证，不操作远端环境。

BDD: 保存多个开始边界关系 -> Given 路线有多个无普通前置工序且最终汇合 / When 保存 START 到全部首工序及唯一 END 关系 / Then 校验通过并原子持久化。

BDD: 允许多前置汇合 -> Given 两条开始分支指向同一汇合工序 / When 校验普通工序关系 / Then 多前置合法且普通工序仍最多一个后续。

BDD: 拒绝边界不一致 -> Given START 未覆盖全部首工序或 END 不对应唯一末工序 / When 校验关系图 / Then 返回明确错误且不写入。

BDD: 回填已有有效线性图 -> Given 历史路线已有唯一首尾和普通关系 / When 执行迁移 / Then 创建 START 和 END 边界关系；无关系或多首多尾路线不回填成功关系。

BDD: 复制与删除边界关系 -> Given 来源路线已有边界关系 / When 复制或删除路线 / Then 边界关系按新工序 ID 映射复制或同步删除。

BLOCKER: none

RED: `mvn -pl yudao-module-mes -Dtest=MesProRouteProcessFlowBoundaryEdgeServiceTest -DskipITs test` -> FAIL，边界关系 ReqVO、DO、Mapper 尚不存在，符合预期。

RED: `python -X utf8 -m pytest script/tests/test_mes_route_process_flow_boundary_edge_sql.py -q` -> FAIL，迁移 `20260710_mes_route_process_flow_boundary_edge.sql` 尚不存在，符合预期。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessFlowServiceImplTest,MesProRouteProcessFlowBoundaryEdgeServiceTest" -DskipITs test` -> PASS，11 tests PASS；覆盖多 START 汇合、唯一 END、重复边、自环、循环、边界不一致、不可达、复制与删除。

GREEN: `python -X utf8 -m pytest script/tests/test_mes_route_process_flow_boundary_edge_sql.py -q` -> PASS，4 tests PASS；覆盖迁移元数据、表结构、确定性回填和仅删除新表的回滚契约。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，`yudao-server-exec.jar` 构建成功。

INFO: migration policy gate 首次因只传目标迁移、未包含声明依赖而失败；改为显式传入基础迁移、`20260709_mes_route_process_flow_graph.sql` 和本次迁移后继续验证，不跳过依赖门禁。

GREEN: `run-release-migration-policy-gate.py` -> PASS，输出 `migration-policy-gate.json`，migrationCount=3。

GREEN: 本机 MySQL `127.0.0.1:23306/ruoyi-vue-pro` 执行迁移 -> PASS；新表存在，确定性历史回填统计 `START=4`、`END=4`。

GREEN: 隔离后端 `http://127.0.0.1:48094/actuator/health` -> HTTP 200，进程仅监听任务端口。

GREEN: 测试租户真实 E2E -> PASS；路线 `RT000017` 保存两个 START 目标汇合到同一工序、唯一 END、刷新恢复并通过 API 持久化断言，随后通过页面恢复原拓扑。

REGRESSION: `nextProcessId` 继续只由普通工序关系计算；边界关系不使用负数伪 ID，不从普通关系运行时推断。

GREEN: backend API evidence validator -> PASS。

GREEN: database schema evidence validator -> PASS。

RED: 重放真实多前置汇合保存 -> FAIL，真实本机库存在旧索引 `uk_mes_route_process_flow_target`，第二个前置写入同一目标工序时触发唯一键冲突；该索引与本次正式允许多前置汇合的模型直接冲突。

RED: `python -X utf8 -m pytest script/tests/test_mes_route_process_flow_boundary_edge_sql.py -q` -> FAIL，新增契约要求迁移必须显式识别并删除旧目标唯一索引，初始迁移不满足。

GREEN: 边界迁移增加 `information_schema.statistics` 检查，存在时删除 `uk_mes_route_process_flow_target`，并确保普通索引 `idx_mes_route_process_flow_edge_target` 存在；回滚不恢复与正式模型冲突且可能无法重建的旧唯一索引。

GREEN: `python -X utf8 -m pytest script/tests/test_mes_route_process_flow_boundary_edge_sql.py -q` -> PASS，5 tests PASS；`run-release-migration-policy-gate.py` -> PASS，migrationCount=3。

RED: 真实 E2E 完成第一次保存后再次通过页面恢复并保存 -> FAIL，路线已有 `V10` 时 `MAX(version_no)` 按字符串返回 `V9`，服务再次生成 `V10` 并触发 `uk_mes_pro_route_version_no` 唯一键冲突。

RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增回归测试确认 Mapper 仍使用 `MAX(version_no)`，不具备数字后缀排序。

GREEN: `MesProRouteVersionMapper.selectMaxVersionNoByRouteId` 改为按 `CAST(SUBSTRING_INDEX(version_no, 'V', -1) AS UNSIGNED) DESC, id DESC` 排序并取一条，连续保存可从 V10 正确生成 V11。

GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionMapperTest,MesProRouteVersionAndCopyTest,MesProRouteProcessFlowServiceImplTest,MesProRouteProcessFlowBoundaryEdgeServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，22 tests PASS。

GREEN: `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS，修复后的 `yudao-server-exec.jar` 构建成功并在隔离端口 `48094` 健康启动。

GREEN: 修复后真实 E2E -> PASS；第一次保存多 START 汇合并刷新恢复，重新打开同一路线后第二次保存恢复原线性拓扑，最终 API 断言与初始拓扑完全一致。

GREEN: 后端提交 `e5ddf55455` 已快进融合到 `int_main`，主工作区 HEAD 与任务分支一致。

GREEN: 融合结果验证 -> `MesProRouteVersionMapperTest,MesProRouteVersionAndCopyTest,MesProRouteProcessFlowServiceImplTest,MesProRouteProcessFlowBoundaryEdgeServiceTest` 共 22 tests PASS；SQL 契约 5 tests PASS。

GREEN: task-closeout-cleanup preview -> PASS；保留 `task.md`、`execution-log.md`，删除 backend/database/bug evidence 和 migration policy JSON，未发现越界或受保护文件。

INFO: task-closeout-cleanup apply 已按预览删除附属 evidence 和 migration policy JSON；自动收尾提交首次因未传 `TDD_TASK_DIR` 被提交门禁阻止，随后显式设置任务目录并完成收尾提交，不绕过门禁。

GREEN: 隔离后端 `48094` 与前端 `8094` 已停止，端口不再监听。

GREEN: 后端收尾提交已快进融合到 `int_main`；任务 worktree、任务根目录和分支 `codex/20260710-route-flow-boundary-links` 已删除。

GREEN: final verification -> COMPLETED，保留正式生产代码、SQL 迁移、回归测试、`task.md` 和 `execution-log.md`。

# eDHR 批次与历史查询：数据库分页

本次改造取消列表请求中的 `selectScanWindow` 循环。查询沿现有正式数据源执行，不新增异步投影、缓存数据源或历史回填。

## 查询与权限

1. 人员反查先通过原 `MesProEdhrTraceIdentityMapper.userIds` 解析部门/本人数据权限，构造服务器端 `MesProEdhrTraceSearchScope`，不接受客户端传入授权用户集合。
2. `MesProEdhrBatchVisibilityQueryService` 在数据库中检查实际可达的异常权限配置，解析实际涉及的不同表单规则。查询数量与批次数不再一一对应。
3. 交接撤权、冻结办理候选、正式来源持有人、PQC 候选、当前工序及正式绑定规则生成同一可见性谓词；count 与 page 使用相同筛选。
4. `MesProEdhrTraceSearchSql` 从正式来源关系和冻结 JSON 的结构化字段定位批次；物料与批号必须命中同一条明细。已返工退役的放行申请按既有 `current_batch_execution_id` 口径排除。
5. `MesProEdhrBatchSearchMapper` 直接执行 COUNT 与 LIMIT/OFFSET，应用只接收当前页；证据读取器只检查当前页命中项，最多128条一块。检索结果与正式证据不一致明确失败，不通过删行伪造 total。

当前 JSqlParser 不支持 JSON_TABLE。只有专用 SearchMapper/VisibilityMapper 方法豁免自动 SQL 改写，外表、子查询均显式绑定租户与软删除条件。系统用户身份查询仍执行原数据权限拦截器；不得将这些豁免复制到其他查询或全局配置。

## 完整性校验边界

用户已确认：未命中的坏证据不再阻断搜索。历史页新增“完整性校验”，显式检查全部可见的已放行/已作废历史批次，不受搜索条件影响。首块固定最大ID，按升序游标每次最多128条，可取消；逐批证据异常可查看，数据库或权限异常使本次操作失败。新增记录不进入已经开始的这一轮检查。

列表读取不补建任务；原创建、详情与正式操作仍负责各自前置检查。完整性校验不改变批次状态。

冻结候选快照要求系统写入的十进制数字格式，允许符号、前导零及既有空白；溢出和非规范Unicode数字明确报错，不提供逐批Java扫描退路。

## 验证与复现

- Java/H2测试覆盖旧可见性逻辑对比、规则优先级、交接撤权、总数/分页、命中异常以及独立完整性操作。
- `MesProEdhrTraceSearchMysqlIT` 在隔离 MySQL 8.0.39 中使用真实 MyBatis 租户/数据权限/分页插件，覆盖1万批次、结构化反查、正常角色权限、人员数据范围、退役轮次及完整性游标。
- 前端独立校验组件行为测试：`node IntRuoyiFronted/tests/edhr-integrity-check-behavior.spec.cjs`。
- 后端测试 profile：`mes-edhr-list-performance-tests`。集成测试必须显式指定 `-Dtest=MesProEdhrTraceSearchMysqlIT`，缺少测试容器会失败，不会跳过并宣称通过。

密集权限基准（`MesProEdhrBatchVisibilityBenchmark`）：每批1条正式来源、3个工序任务，持有人/经理/表单规则权限约各占三分之一；同一租户，共用1条表单规则，经理批次各有最新事务与冻结任务。使用仓库已有索引，数据量调整后执行 ANALYZE，预热2次、测量3次。仅测量四个异常探针、规则查询、count和page七个生产SQL阶段，不含身份API、MyBatis、HTTP或浏览器，也不代表冷缓存。

| 批次数 | 七个SQL阶段合计 | count | page | 返回行数 |
| --- | --- | --- | --- | --- |
| 500 | 73.29–78.50ms | 15.85–17.19ms | 3.40–3.99ms | 20 |
| 5000 | 540.21–563.20ms | 126.79–136.09ms | 3.46–3.87ms | 20 |

两种规模的总数均准确。未使用额外索引或优化器提示；此表不是生产首屏SLO。

MySQL IT仅允许带 `codex.task=edhr-rootfix` 标签且端口绑定127.0.0.1的任务自有容器，使用 `edhr_rootfix` 测试schema。创建命令：

```powershell
docker run --detach --name edhr-rootfix-mysql-20261027 --label codex.task=edhr-rootfix --publish 127.0.0.1::3306 --env MYSQL_ALLOW_EMPTY_PASSWORD=yes --env MYSQL_DATABASE=edhr_rootfix mysql:8.0.39
```

这是一次性本地测试数据库，不得将其配置用于业务环境。测试结束后只删除对应标签的本任务容器。

验证不代表目标服务器已更新。真实数据下仍需关注数据库统计信息、索引计划、准确总数计算和JSON处理成本；本次消除的是应用逐批读取全量候选这一结构性问题，不是对任意数据规模作固定耗时承诺。

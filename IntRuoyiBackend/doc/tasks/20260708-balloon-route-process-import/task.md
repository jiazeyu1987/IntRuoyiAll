# Task: 球囊/棘突球囊工艺路线工序导入

## 任务目标

基于桌面文件 `C:\Users\BJB110\Desktop\球囊扩张导管工序(1)(2).xlsx` 的 `Sheet1`，把 `球囊扩张导管` 与 `棘突球囊扩张导管` 两条工艺流程的工序挂入 MES 工艺路线。

## 经验门禁

- PowerShell：已读取 `docs/powershell-memory.md`，后续中文文档、SQL、测试和命令必须显式 UTF-8；PowerShell 5.1 不使用 `&&`。
- 项目经验索引：已读取 `docs/experience-index.md`，本任务命中 PowerShell / Windows shell / 中文 SQL 文本门禁。
- 数据库变更：已读取 `database-schema-delivery` 与 `references/database-contract.md`，迁移必须包含数据安全、失败路径、回滚/恢复说明和验证证据。
- BDD/TDD：已读取 `bdd-tdd-acceptance-planner` 与 `references/acceptance-structure.md`，生产 SQL 变更前必须先补 RED 契约测试。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；缺失工序、路线冲突、顺序冲突、数量不符时直接失败。
- 是否从根因和长期维护角度解决：是；路线工序只保存 `process_id` 关系，工序展示继续由工序主数据联动。
- 是否存在临时补丁或绕过：否；采用正式 SQL 迁移与契约测试。

## BDD 场景

- BDD: 普通球囊路线挂入 23 道工序 -> Given Excel Sheet1 包含 `球囊扩张导管` 23 道唯一工序 / When 执行迁移 / Then `球囊扩张导管工艺路线` 拥有 23 条按 Excel 顺序排列的路线工序。
- BDD: 棘突球囊路线挂入 26 道工序 -> Given Excel Sheet1 包含 `棘突球囊扩张导管` 26 道唯一工序 / When 执行迁移 / Then `棘突球囊扩张导管工艺路线` 拥有 26 条按 Excel 顺序排列的路线工序。
- BDD: 跨产品同编码不串线 -> Given 两个产品都包含 `Z2630` / When 迁移挂载路线 / Then 工序按 `tenant_id + product_name + code + name` 唯一定位，不通过名称 fallback。
- BDD: 异常数据 fail fast -> Given 目标工序缺失、路线重复或已有路线工序顺序冲突 / When 执行迁移 / Then SQL 失败并暴露明确错误，不静默覆盖。

## 里程碑

- [x] M1：建立任务记录并读取经验门禁。
- [x] M2：补 RED SQL 契约测试覆盖路线挂载缺口。
- [x] M3：实现路线与路线工序迁移 SQL。
- [x] M4：运行目标测试与迁移契约验证。
- [x] M5：记录 closeout preview 与最终状态。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_mes_balloon_process_device_capacity_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260708-balloon-route-process-import/database-schema-evidence.md`
- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerTest,MesProRouteServiceImplTest,BalloonProcessDeviceMappingImportServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-balloon-route-process-import --mode preview`

## 当前状态

COMPLETED_WITH_COMMIT_BLOCKER。路线挂载 SQL、SQL 契约测试、数据库 evidence 校验、目标 Maven 回归和 closeout preview 均已通过。针对页面仍显示工序为空的问题，已在本机运行库 `127.0.0.1:23306/ruoyi-vue-pro` 修复现有两条路线 `ROUTE-XLSX-00001` / `ROUTE-XLSX-00002` 的 `mes_pro_route_process.process_id` 与 `next_process_id`，并回查确认普通球囊 23 道、棘突球囊 26 道工序均可关联到当前工序主数据，`blank_count=0`。提交被混合工作区阻塞：`sql/mysql/20260708_mes_balloon_process_device_capacity.sql` 与 `script/tests/test_mes_balloon_process_device_capacity_sql.py` 在本任务开始前已是未跟踪文件，本轮在其上继续补路线挂载，无法在不夹带前序未提交内容的情况下安全单独提交。

## Current Status

completed_with_commit_blocker. Runtime database repair and verification are complete. Commit remains blocked by pre-existing untracked migration/test files that cannot be safely separated from prior work.

## Cleanup Keep

- `doc/tasks/20260708-balloon-route-process-import/database-schema-evidence.md`

# 任务：排产工单可选列导出

## 任务目标

为排产工单新增 `/mes/pro/schedule-order/export-excel` 后端导出接口，按当前查询条件导出全部匹配数据，并通过列白名单支持 `exportColumns` 精确控制导出列。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：中文文件读写和 Maven 命令需显式 UTF-8 / 参数安全。
- 已读取 `docs/experience-index.md` 和 `docs/agent-memory/project-error-prevention.md`：排产导出字段必须复用列表权威口径，不用 mock、空值或默认成功掩盖问题。
- 已读取 `backend-api-delivery` 与 `database-schema-delivery`：新增导出接口和权限 SQL 需记录契约、权限、RED/GREEN 和发布链路影响。
- 当前后端仓库存在无关 DCC、MES 工单同步等脏改；本任务不回退、不暂存无关改动。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；后端以固定白名单拒绝非法列，避免反射任意字段。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 默认可见列导出 -> Given 请求未传 `exportColumns` / When 调用导出接口 / Then 后端使用默认可见业务列。
- BDD: 自定义列导出 -> Given 请求传入合法列集合 / When 导出 / Then Excel 只包含这些列。
- BDD: 非法列拒绝 -> Given 请求包含非法列 / When 导出 / Then 返回明确参数错误且不生成文件。

## 验证结果

- RED: 后端导出接口契约 -> EXPECTED FAIL，原控制器缺少 `/export-excel`、导出 VO、列白名单和 `exportColumns` 参数。
- GREEN: `mvn -pl yudao-framework/yudao-spring-boot-starter-excel -DskipTests install` -> PASS。
- GREEN: `python -m pytest script/tests/test_mes_schedule_order_export_permission_sql.py` -> PASS，3 passed。
- GREEN: 本机测试库应用 `20260708_mes_schedule_order_export_permission.sql` -> PASS，`aoteman` 具备 `mes:pro-schedule-order:export`。
- GREEN: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS。
- BLOCKER: `mvn -pl yudao-module-mes "-Dtest=MesProScheduleOrderControllerTest" test` -> FAIL，MES 模块 testCompile 存在既有缺类测试，例如 EDHR、Feedback、Calendar 相关测试引用缺失类型，目标测试执行前被阻塞。

## 当前状态

`COMPLETED_WITH_BLOCKERS`：后端生产代码、Excel 动态列支持、权限 SQL 和 SQL 契约验证已完成；完整 Maven 目标测试受既有 testCompile 缺类阻塞。

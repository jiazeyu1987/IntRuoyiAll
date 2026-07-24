# 路线用途启停联动

## 任务目标

- 为工艺路线用途配置增加用途级启用状态，支持排产用途和批记录用途分别启停。
- 禁用的基础工艺流程不得被重新启用用途配置，必须提示用户先启用工艺流程。
- 路线分页响应返回排产用途和批记录用途启用状态，供前端列表直接展示和操作。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文输出、Markdown 读取和命令日志均显式 UTF-8。
- SQL / 发布链路：新增 `sql/mysql/20260707_mes_route_use_config_enabled.sql` 属于发布契约改动，必须补脚本契约测试并验证迁移字段定义。
- BDD + 严格 TDD：先补用途启停行为测试和 SQL 契约测试，再实现服务、接口、响应字段和迁移。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。基础路线禁用时直接拒绝启用用途，不做静默修正。
- 是否从根因和长期维护角度解决：是。新增用途级 `enabled` 字段，服务端统一读写并透出分页状态。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 用途启用受基础路线状态约束 -> Given 基础工艺流程已禁用 When 用户尝试启用排产或批记录用途 Then 后端拒绝并返回“工艺流程已经禁用，请先启用工艺流程”。
- BDD: 排产用途和批记录用途独立启停 -> Given 同一路线存在排产和批记录用途配置 When 用户只切换排产用途 Then 只更新排产用途记录，不影响批记录用途。
- BDD: 用途启停字段可发布迁移 -> Given 旧库缺少用途级启用字段 When 执行发布迁移 Then `mes_pro_route_use_config.enabled` 以非空默认禁用状态创建。

## 里程碑

- [x] M1：补齐用途启停 BDD、RED 测试和 SQL 契约测试。
- [x] M2：实现后端用途启停接口、服务逻辑、响应字段和迁移脚本。
- [x] M3：执行后端定向测试和 SQL 契约测试。
- [x] M4：完成任务记录并提交后端改动。

## 预期验证

- `mvn.cmd -pl yudao-module-mes -Dtest=MesProRouteUseConfigServiceImplTest test`
- `python -m pytest script/tests/test_mes_route_use_config_enabled_sql.py`

## 当前状态

- 状态：completed
- 验证：后端定向测试已通过；SQL 契约测试补齐后执行通过。

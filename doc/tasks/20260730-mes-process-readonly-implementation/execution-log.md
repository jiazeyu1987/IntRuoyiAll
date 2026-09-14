# Execution Log

## User Intent

用户要求实现只读“MES工序”页签并进行 E2E 验证。当前确认范围：

- 位于“工序设置”和“工艺流程”之间。
- 只显示 `二代压力泵` 工作表数据。
- 不考虑维护。
- 只关联设备和执行工序。
- 其它数据只列出，不建立关联或业务联动。

## BDD Scenarios

BDD: 只读 MES 工序目录可见 -> Given 用户具有 MES 工序查询权限 / When 用户打开生产菜单 / Then “MES工序”位于“工序设置”和“工艺流程”之间，页面显示只读列表。

BDD: 二代压力泵数据完整展示 -> Given 二代压力泵工作表包含有效 MES 工序行 / When 页面加载列表 / Then 显示产品、设备、MES 工序、设备数量、日产能、人力、MES 工序编码、单价、报工标记、批记录标记、批记录工序名称和执行工序。

BDD: 多设备结构化展示 -> Given 一个 MES 工序关联多个设备编码 / When 查询该目录行 / Then 接口返回多个设备对象，页面分别显示设备编码和可选名称。

BDD: 只保留两类关联 -> Given 一个目录行包含其它来源字段 / When 查询或打开页面 / Then 只有设备和执行工序是结构化关联，其它字段是普通展示快照。

BDD: 页面不存在维护入口 -> Given 用户打开 MES 工序列表 / When 检查页面命令 / Then 不显示新增、编辑、删除、启停或导入操作，也不发送 MES 工序写请求。

BDD: 关联前置缺失时明确失败 -> Given 来源设备编码或执行工序无法唯一匹配现有主数据 / When 应用目录种子 / Then 迁移失败并指出缺失或歧义对象，不创建猜测关联。

## TDD Sequence

1. RED：迁移契约测试先断言目录表、设备关联表、菜单、权限、二代压力泵种子和 fail-fast 校验存在。
2. GREEN：实现最小正式迁移。
3. RED：后端 schema/服务/Controller 测试先断言只读分页合同和多设备聚合。
4. GREEN：实现 DO、Mapper、Service、Controller 和 VO。
5. RED：前端静态合同先断言菜单组件、API、只读列和禁止维护操作。
6. GREEN：实现 API wrapper 和页面。
7. REGRESSION：后端定向测试、前端静态合同、TypeScript、构建和 migration policy gate。
8. E2E：从真实登录页面打开生产菜单和 MES 工序列表，验证菜单顺序、二代压力泵行、多设备、执行工序、无维护按钮、无写请求、无控制台错误。

## Command And Evidence Log

- GREEN: requirement triage -> PASS, decision `ACCEPT` recorded in `docs/changes/20260730-mes-process-tab.md`.
- GREEN: project rules and delivery skills -> PASS, database/backend/frontend/E2E contracts loaded.

## Blockers

- None at task start. Missing local schema, menu permission, existing device/process mapping, runtime, login or E2E prerequisites must fail fast if discovered.


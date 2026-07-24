# 任务：DCC 基础数据项目代码支持 CRUD 与查询

## 任务目标

为 DCC 项目代码基础数据补齐后端正式维护能力，并保证：

- 支持新增项目代码；
- 支持更新项目代码；
- 支持删除未被受控文件引用的项目代码；
- 支持按查询条件筛选分页；
- 继续保持导入、导出、详情和文控数字升序合同；
- 删除已被受控文件引用的项目代码时必须 fail fast 明确报错；
- 不改数据库 schema，不引入 fallback 或兼容兜底。

## 当前状态

status: completed

## Current Status

completed

## 前一任务检查

- 后端最近同页任务 `20260625-dcc-basic-data-main-code-doc-control-order` 已标记为 `completed`，允许继续本任务。
- 当前后端仓库存在其他未归属脏改动；本任务只修改 DCC 项目代码 CRUD/查询相关代码、SQL 种子、定向测试与本任务文档，不覆盖其他改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：本任务先做本机源码、控制器测试和服务单测，不执行真实 E2E、服务器写入、数据库 schema 变更或其他高风险动作，因此当前不触发 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。删除受引用数据、查询不到记录、校验失败都必须显式报错。
- `是否从根因和长期维护角度解决`：是。CRUD 由后端正式接口提供，删除约束由后端统一校验 `dcc_controlled_file.dcc_project_code_id` 引用关系。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 可创建项目代码 -> Given 管理员提交合法项目代码请求 When 调用创建接口 Then 返回新记录编号且分页可查询到该记录。`
- `BDD: 可更新项目代码 -> Given 已存在项目代码记录 When 管理员提交更新请求 Then 详情与分页返回更新后的字段值。`
- `BDD: 未被引用项目代码可删除 -> Given 某条项目代码未被受控文件引用 When 调用删除接口 Then 该记录被删除。`
- `BDD: 被引用项目代码禁止删除 -> Given 某条项目代码已被 dcc_controlled_file 引用 When 调用删除接口 Then 系统明确报错并保留该记录。`
- `BDD: 分页筛选支持项目名称项目代码类别状态 -> Given 列表存在多条不同属性项目代码 When 按条件分页查询 Then 仅返回符合条件的记录。`
- `BDD: 菜单权限种子补齐维护权限 -> Given DCC 基础数据菜单存在 When 同步菜单权限种子 Then create/update/delete 权限同时存在于同一菜单下。`

## 里程碑

1. M1：创建任务文档并补齐门禁、BDD、验证目标。`DONE`
2. M2：先补控制器/服务 RED 测试，锁定 CRUD、删除约束与权限合同。`DONE`
3. M3：实现请求 VO、controller、service、mapper 校验与 SQL 种子。`DONE`
4. M4：运行定向测试并补齐证据、收尾。`DONE`

## 预期验证

- `mvn -pl yudao-module-dcc "-Dtest=DccProjectCodeServiceImplTest,DccProjectCodeControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260625-dcc-basic-data-crud-query/backend-api-evidence.md`

## Cleanup Keep

- `doc/tasks/20260625-dcc-basic-data-crud-query/task.md`
- `doc/tasks/20260625-dcc-basic-data-crud-query/execution-log.md`
- `doc/tasks/20260625-dcc-basic-data-crud-query/backend-api-evidence.md`

## 最终验证结果

- `mvn -pl yudao-module-dcc "-Dtest=DccProjectCodeServiceImplTest,DccProjectCodeControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS

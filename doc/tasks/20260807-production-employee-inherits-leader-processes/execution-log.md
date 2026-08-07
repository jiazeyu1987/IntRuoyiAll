# Execution Log

- Task ID: `20260807-production-employee-inherits-leader-processes`
- Created: `2026-08-07`

## User Intent

- 用户要求生产员工与工序不绑定。
- 用户要求生产员工可以看到其生产组长负责的所有工序。
- 截图明确指向生产组长“班组配置”中的“生产人员工序绑定”卡片，应移除该入口。

## Rule And Skill Reads

- 已读取 `backend-api-delivery`、`frontend-feature-delivery` 及其 evidence contract。
- 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/branch-runtime-ports.md` 与 `docs/experience-index.md`。
- 适用经验：`docs/backend-development.md#MES 生产人员档案正式工重复关联门禁`。

## Existing-System Evidence

- `TeamLeaderWorkbenchPage.vue` 仍显示“生产人员工序绑定”卡片，并调用 `/process-employee-binding/save`。
- `MesProcessPoolTeamLeaderController` 同时保留 `/employee-binding/add`、`/employee-binding/disable` 与 `/process-employee-binding/save` 两套员工—工序写入接口。
- `MesFrontlineRuntimeConfigServiceImpl` 的员工选项已按当前登录组长人员档案读取，但仍查询员工—工序绑定以推断运行配置组长范围。
- `MesFrontlineDeviceAccountContextServiceImpl#listSwitchableProcesses` 只识别当前登录人为生产组长或设备/岗位账号，尚未通过生产人员档案的 `systemUserId -> leaderUserId` 让生产员工继承组长负责工序。
- 共享 `int_main` 初始状态领先 `origin/int_main` 2 个提交，并存在一个已修改后端测试和两个未跟踪并行任务目录；这些均不属于本任务。

## BDD Scenarios

- BDD: 生产员工继承组长全部工序 -> Given 启用生产人员档案将员工账号归属唯一生产组长，且该组长在正式工序开始配置中负责多条路线 / When 员工打开一线生产工序列表 / Then 返回该组长负责路线下的全部启用工序，且不查询员工—工序绑定。
- BDD: 禁止员工工序绑定配置 -> Given 生产组长打开班组配置 / When 页面和 API 合同加载 / Then 不显示员工—工序绑定卡片，也不存在新增、保存或禁用员工—工序关系的写入接口。
- BDD: 员工候选与工序无关 -> Given 员工或组长打开其负责范围内任一工序 / When 读取运行配置或校验提交员工 / Then 候选均为该生产组长启用人员档案集合，不按工序、岗位或设备缩小。
- BDD: 员工归属异常时失败 -> Given 登录账号对应禁用生产人员档案、多个不同生产组长或组长没有正式负责工序 / When 请求一线生产工序 / Then 返回明确业务错误，不回退到账号岗位、设备绑定或历史员工—工序关系。

## TDD Evidence

- RED: `node tests\\e2e\\production-employee-inherits-leader-processes-static.spec.cjs` -> FAIL，首个预期失败为班组配置仍包含“生产人员工序绑定”。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineProductionEmployeeLeaderProcessScopeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，生产员工账号未解析人员档案中的生产组长，错误进入设备账号路线来源并抛出 `1040760100`。
- GREEN: pending.
- REGRESSION: pending.

## Milestone Evidence

- M0 in progress：任务合同已建立，等待共享脏工作区基线提交。

## Blockers

- 无需求 blocker。

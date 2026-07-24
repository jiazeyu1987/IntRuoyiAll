# 任务：排产员工作台全量数据包导入导出

- Task ID: `20260629-scheduler-workbench-full-config-package`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

为排产员工作台提供正式的“导出全部数据包 / 导入全部数据包”能力。数据包需聚合岗位配置包、角色配置包、排产工艺路线配置包，以及工作台真实使用所需的用户角色绑定快照，避免前端通过多个独立请求伪装“全量包”。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-smart-scheduling-smoke-one-shot-package\task.md`
- 状态：`completed`
- 处理说明：上一任务已确认真实 smoke 的最小正式准备范围为岗位包 + 角色包 + smoke 用户角色绑定 + 路线包；本次将该正式范围沉淀为工作台可直接操作的全量数据包接口。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：PowerShell 5.1 与中文文件读写必须显式 UTF-8，命令不得使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文任务文档、执行日志与测试脚本输出均按 UTF-8 处理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接补正式聚合导出/导入接口，统一后端封装全量包，不在前端串多个接口伪装成功。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工作台可导出单个全量数据包 -> Given 当前租户已有岗位、角色、用户角色绑定与排产路线配置 / When 在工作台执行导出全部数据包 / Then 后端返回一个可追溯的聚合包，而不是多个零散文件。`
- `BDD: 工作台可导入单个全量数据包 -> Given 用户持有从工作台导出的全量数据包 / When 在目标租户执行导入全部数据包 / Then 后端按正式顺序导入岗位、角色、用户角色绑定与排产路线配置。`
- `BDD: 导入会覆盖同业务键冲突数据 -> Given 目标租户已存在同编码岗位、角色或同用户名角色绑定 / When 导入全部数据包 / Then 正式导入逻辑以最新包内容覆盖冲突配置，不要求人工预清理。`
- `BDD: 接口权限与工作台入口一致 -> Given 具备工作台查询或更新权限的用户 / When 访问全量包导出导入功能 / Then 导出复用 query 权限，导入复用 update 权限，并在前端显示对应按钮。`

## Milestones

1. M1：创建任务文档并锁定全量数据包合同。`completed`
2. M2：补后端 RED 测试并实现聚合接口。`completed`
3. M3：补前端 RED 静态检查并接入工作台按钮。`completed`
4. M4：运行前后端验证并记录结果。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchControllerPermissionContractTest" test`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -DskipTests compile`

## Final Result

- 已新增 `/mes/pro/scheduler-workbench/full-config/export` 与 `/mes/pro/scheduler-workbench/full-config/import` 两个正式聚合接口。
- 全量数据包当前聚合岗位配置包、角色配置包、排产工艺路线配置包，以及用户角色绑定快照；导入时按正式顺序回放，并覆盖同业务键冲突配置。
- 后端定向验证已通过：
  - `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system -am -Dmaven.test.skip=true install` -> PASS
  - `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchControllerPermissionContractTest test` -> PASS
- 本机 `芋道源码/admin` 真实链路验证已通过：
  - `GET http://127.0.0.1:48081/admin-api/mes/pro/scheduler-workbench/full-config/export` -> HTTP `200`
  - `POST http://127.0.0.1:48081/admin-api/mes/pro/scheduler-workbench/full-config/import` -> HTTP `200`，响应 `{"code":0,"msg":"","data":{"userRoleBindingCount":27,"assignedRoleCount":41}}`
  - 页面 toast：`导入完成；用户角色绑定 27 条；分配角色 41 条`
- 本轮真实导出文件与截图产物：
  - `D:\ProjectPackage\Int\IntRuoyi\output\playwright\admin-scheduler-workbench-full-config-export.json`
  - `D:\ProjectPackage\Int\IntRuoyi\output\playwright\admin-scheduler-workbench-full-config-import-result.png`

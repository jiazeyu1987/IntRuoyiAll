# Execution Log：排产员工作台全量数据包导入导出

BDD: 工作台可导出单个全量数据包 -> Given 当前租户已有岗位、角色、用户角色绑定与排产路线配置 / When 在工作台执行导出全部数据包 / Then 后端返回一个可追溯的聚合包，而不是多个零散文件。
BDD: 工作台可导入单个全量数据包 -> Given 用户持有从工作台导出的全量数据包 / When 在目标租户执行导入全部数据包 / Then 后端按正式顺序导入岗位、角色、用户角色绑定与排产路线配置。
BDD: 导入会覆盖同业务键冲突数据 -> Given 目标租户已存在同编码岗位、角色或同用户名角色绑定 / When 导入全部数据包 / Then 正式导入逻辑以最新包内容覆盖冲突配置，不要求人工预清理。
BDD: 接口权限与工作台入口一致 -> Given 具备工作台查询或更新权限的用户 / When 访问全量包导出导入功能 / Then 导出复用 query 权限，导入复用 update 权限，并在前端显示对应按钮。
RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchControllerPermissionContractTest test` -> FAIL，初始状态缺少 full-config 聚合接口与 service，且全量包合同未建立。
GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system -am -Dmaven.test.skip=true install` -> PASS，本地安装最新 `yudao-module-system` 产物，消除 `mes` 子模块对岗位/角色配置包接口的旧快照依赖。
GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchControllerPermissionContractTest test` -> PASS，full-config 聚合导入导出合同与权限合同通过。
GREEN: experience-preflight -> PASS，已按 `docs/login-access.md` 先确认本机 `http://localhost:8081` 的 `芋道源码/admin` 真实登录路径，并在执行前核对 `http://127.0.0.1:48081/actuator/health`。
BLOCKER: local-runtime-stability -> FAIL，本机 `48081` 一度被旧缓存包 `E:\Int\CacheData\IntRuoyi\runtime\backend-20260630-100842.jar` 自动接管，导致前端按钮真实验证阶段出现过 `404` 与导入超时噪音；需先切回最新源码运行态再做正式结论。
GREEN: local-runtime-stability -> PASS，本轮先停掉本地维护控制台干扰进程并添加 `E:\Int\CacheData\IntRuoyi\runtime\local-runtime-restart.guard`，随后以 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-server\target\yudao-server-exec.jar` 重新拉起 `48081`，`/actuator/health` 返回 `{"status":"UP"}`。
GREEN: real-admin-full-config-export -> PASS，真实点击 `芋道源码/admin` 排产员工作台“导出全部数据包”，命中 `GET /admin-api/mes/pro/scheduler-workbench/full-config/export`，HTTP `200`，导出文件 `D:\ProjectPackage\Int\IntRuoyi\output\playwright\admin-scheduler-workbench-full-config-export.json` 大小 `1750192` bytes。
GREEN: real-admin-full-config-import -> PASS，真实点击 `芋道源码/admin` 排产员工作台“导入全部数据包”，向隐藏文件输入框 `index=1` 喂入导出文件后命中 `POST /admin-api/mes/pro/scheduler-workbench/full-config/import`，HTTP `200`，响应 `{"code":0,"msg":"","data":{"userRoleBindingCount":27,"assignedRoleCount":41}}`，页面 toast 为 `导入完成；用户角色绑定 27 条；分配角色 41 条`。

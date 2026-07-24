# 执行日志：排产员工作台导入错误改为具体可定位报错

- Task ID: `20260701-scheduler-workbench-import-specific-errors`
- Current Status: `in_progress`

## BDD

- `BDD: 全量包格式非法时返回具体原因 -> Given 导入的排产员工作台全量 JSON 结构非法或缺少必填段 / When 执行导入 / Then 接口返回正式业务异常，并明确指出缺失字段或非法 JSON，而不是系统异常。`
- `BDD: 目标环境引用缺失时返回具体业务键 -> Given 导入包引用了目标环境不存在的用户、角色、路线、工位或设备 / When 执行导入 / Then 接口返回正式业务异常，并明确指出缺失的用户名、角色编码、路线编码、工位编码或设备编码。`

## Milestone Progress

- `M1` 读取 `docs/experience-index.md`、`docs/powershell-memory.md`、`bug-regression-fix-loop`、`backend-api-delivery`，确认本轮必须先补任务台账并用 TDD 收口导入错误合同。
- `M1` 检查 `MesProSchedulerWorkbenchFullConfigPackageServiceImpl`、`MesProSchedulerWorkbenchRouteConfigPackageServiceImpl`、`GlobalExceptionHandler`、前端 axios 拦截器，确认当前大量 `IllegalArgumentException` 会落入默认 500，最终页面显示“系统异常”。

## RED / GREEN

- `RED: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchRouteConfigPackageServiceTest" test` -> FAIL，先被 `yudao-module-mes` 内既有无关测试编译错误阻塞（`MesKingdeeProductionMaterialListControllerTest`、`MesKingdeeProductionMaterialListSyncServiceImplTest`、`MesKingdeeProductionOrderSyncServiceImplTest`）；同时确认本轮新增回归断言尚未落地前，工作台导入服务仍大量抛 `IllegalArgumentException`。`
- `GREEN: git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --check -- yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchFullConfigPackageServiceImpl.java yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.java yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchFullConfigPackageServiceTest.java yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchRouteConfigPackageServiceTest.java` -> PASS。`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system -Dtest=PostConfigPackageServiceImplTest,RoleConfigPackageServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。`

## Blockers

- 当前仅能做本机代码与单测验证；测试服务器运行态是否包含本修复需后续发布后复核。

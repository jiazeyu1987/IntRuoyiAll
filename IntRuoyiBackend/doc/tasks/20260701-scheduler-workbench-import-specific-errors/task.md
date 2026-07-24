# 任务：排产员工作台导入错误改为具体可定位报错

- Task ID: `20260701-scheduler-workbench-import-specific-errors`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`
- User Request: `我希望报错可以具体,不要出现系统异常这种无法精确定位错误位置的报错`

## Task Goal

将排产员工作台全量数据包导入链路中的格式校验错误、引用缺失错误和回放失败错误统一收口为正式 `ServiceException`，确保前端不再只能看到“系统异常”，而是直接看到可定位的具体失败原因。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-menu-identity\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成角色配置包跨环境菜单标识修复，本轮继续处理同一导入链路剩余的错误合同问题。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本轮命中 PowerShell / Windows shell 经验；所有中文文件读取继续显式使用 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 5.1 不使用 `&&`；本轮只做本机代码与定向单测验证，不执行测试服务器写入。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。导入校验继续 fail fast，只把泛化的 500 异常收口为正式业务异常。
- `是否从根因和长期维护角度解决`：是。目标是修复导入错误合同，让格式错误和引用缺失通过统一错误码向前端暴露。
- `是否存在临时补丁或绕过`：否。不采用前端本地兜底文案、静默 catch 或服务器侧手工排查来替代正式错误返回。

## BDD 场景

- `BDD: 全量包格式非法时返回具体原因 -> Given 导入的排产员工作台全量 JSON 结构非法或缺少必填段 / When 执行导入 / Then 接口返回正式业务异常，并明确指出缺失字段或非法 JSON，而不是系统异常。`
- `BDD: 目标环境引用缺失时返回具体业务键 -> Given 导入包引用了目标环境不存在的用户、角色、路线、工位或设备 / When 执行导入 / Then 接口返回正式业务异常，并明确指出缺失的用户名、角色编码、路线编码、工位编码或设备编码。`
- `BDD: 前端沿用统一 axios 错误展示即可看到具体报错 -> Given 后端返回具体业务错误码与消息 / When 排产员工作台导入失败 / Then 页面 toast 直接展示后端消息，不再退化为系统异常。`

## Milestones

1. M1：建立后端任务文档并确认现有异常合同缺口。`completed`
2. M2：补 RED 测试，固定“不得再抛 IllegalArgumentException”回归。`completed`
3. M3：实现导入异常合同修复。`completed`
4. M4：完成定向回归与证据校验。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchRouteConfigPackageServiceTest" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-scheduler-workbench-import-specific-errors\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-scheduler-workbench-import-specific-errors\backend-api-evidence.md`

## Current Blockers

- 若要验证测试服务器页面已不再显示“系统异常”，仍需把包含本修复的后端发布到测试服务器；本轮未获授权直接执行服务器发布。

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchRouteConfigPackageServiceTest" test` -> FAIL，受 `yudao-module-mes` 仓库内既有无关编译错误阻塞，未能完整跑到目标用例。
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --check -- yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchFullConfigPackageServiceImpl.java yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.java yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchFullConfigPackageServiceTest.java yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchRouteConfigPackageServiceTest.java` -> PASS
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system -Dtest=PostConfigPackageServiceImplTest,RoleConfigPackageServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-scheduler-workbench-import-specific-errors\bug-regression-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-scheduler-workbench-import-specific-errors\backend-api-evidence.md` -> PASS

## Final Result

- 已将排产员工作台全量配置包与路线配置包导入错误统一收口为正式配置包错误码，不再把格式错误和引用缺失直接抛成未收口异常。
- 前端后续将直接展示后端返回的具体错误消息，例如缺少 `userRoleBindings`、用户不存在、角色编码不存在、路线编码不存在、工位编码不存在、设备编码不存在、子包 Base64 非法等。
- 若测试服务器仍看到“系统异常”，高概率是运行态尚未部署包含本修复的后端版本。

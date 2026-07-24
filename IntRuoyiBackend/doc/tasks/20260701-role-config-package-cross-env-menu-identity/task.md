# 任务：权限角色配置包跨环境菜单标识正式修复

- Task ID: `20260701-role-config-package-cross-env-menu-identity`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`
- User Request: `继续`

## Task Goal

修复权限角色配置包跨环境导入仍依赖 `menuIds` 数字菜单 ID 的合同缺陷，改为使用跨环境稳定的菜单业务标识导出与导入，避免本机导出的角色配置包在测试环境导入后出现菜单权限错绑、丢失或静默漂移。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-system-role-import\task.md`
- 状态：`completed`
- 处理说明：已修复系统类型角色在新增分支上的导入异常；当前继续收口同一链路中剩余的跨环境菜单标识风险。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本轮命中 PowerShell / Windows shell 经验；继续按严格 TDD 与任务台账执行。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文源码、文档与日志统一按 UTF-8 读取；PowerShell 5.1 不使用 `&&`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。导入必须显式按稳定业务键解析菜单；缺失时 fail fast，而不是静默沿用错误 ID。
- `是否从根因和长期维护角度解决`：是。目标是修正角色配置包合同，不再把跨环境不稳定的 DB 主键当作导入依据。
- `是否存在临时补丁或绕过`：否。不采用手工对齐菜单 ID、改测试库主键或靠同步 SQL 临时兜底。

## BDD 场景

- `BDD: 跨环境不同菜单 ID 仍可正确导入 -> Given 导出包记录的是菜单稳定业务标识而非目标环境主键 / When 在菜单 ID 不同但业务标识一致的环境导入角色配置包 / Then 角色最终绑定到目标环境对应菜单。`
- `BDD: 导入包引用的菜单在目标环境缺失时明确失败 -> Given 导入包中的菜单业务标识在目标环境不存在 / When 导入角色配置包 / Then 导入明确报错并指出缺失菜单标识，不写入部分错误权限。`
- `BDD: 同环境往返导出导入保持可用 -> Given 当前环境存在角色与菜单 / When 导出后原样导入角色配置包 / Then 角色基础字段与菜单权限仍可完整回放。`

## Milestones

1. M1：建立任务文档并确认当前菜单标识合同与风险。`completed`
2. M2：先写 RED 回归测试，证明 `menuIds` 不能满足跨环境合同。`completed`
3. M3：实现稳定菜单标识导出导入与失败校验。`completed`
4. M4：运行 GREEN 回归与证据校验。`completed`
5. M5：提交当前任务最小变更。`completed`

## Expected Verification

- RED：
  - `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test`
- GREEN / REGRESSION：
  - `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test`
  - `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-menu-identity\bug-regression-evidence.md`
  - `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-menu-identity\backend-api-evidence.md`

## Current Blockers

- 无代码阻塞；若要关闭用户现场“测试服务器导入系统异常”，仍需把包含本修复的 `ruoyi-vue-pro` 后端发布到测试服务器并重新导入验证。

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test` -> FAIL（RED，新增 `menuKeys` 合同测试无法编译，证明当前结构仍停留在 `menuIds`）
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test` -> PASS
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest" test` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-menu-identity\bug-regression-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-menu-identity\backend-api-evidence.md` -> PASS

## Final Result

- 角色配置包正式升级为 `packageVersion=2`，角色菜单不再导出源环境 `menuIds`，而是导出跨环境稳定的 `menuKeys`。
- `menuKeys` 优先使用 `permission:<permission>`；无权限标识的菜单退回到由父链、类型、路由、组件、组件名和名称构成的稳定路由键。
- 导入阶段会先把 `menuKeys` 解析为目标环境菜单 ID，再调用 `assignRoleMenu(...)`；目标环境缺少任何菜单业务标识时，明确返回 `CONFIG_PACKAGE_REFERENCE_MISSING`，不再笼统抛系统异常。
- 排产员工作台全量包 `/mes/pro/scheduler-workbench/full-config/import` 已通过定向回归，确认其复用的角色配置包导入链路与本次合同升级兼容。

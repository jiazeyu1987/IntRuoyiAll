# 任务：权限角色配置包跨环境导入系统异常修复

- Task ID: `20260701-role-config-package-cross-env-system-role-import`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`
- User Request: `我在本机导出权限角色配置包.json,然后在测试服务器导入这个权限角色配置包.json,提示系统异常`

## Task Goal

修复权限角色配置包从本机导出后导入测试环境时，遇到“目标环境缺少包内系统类型角色”仍抛出系统异常的问题，保证合法导出包跨环境导入不会再命中系统内置角色更新禁令。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-mes-work-order-erp-snapshot-fields-release-idempotency\task.md`
- 状态：`completed`
- 处理说明：无未完成阻塞项；本轮聚焦独立的角色配置包跨环境导入异常。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本轮命中 PowerShell / Windows shell 经验与真实导入回归记录；先补回归测试，再做最小正式修复。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文文件与任务文档统一按 UTF-8 读取；PowerShell 5.1 不使用 `&&`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。继续保留异常暴露，只修正错误调用分支。
- `是否从根因和长期维护角度解决`：是。直接修复系统类型角色导入流程在“新增角色”路径上的错误数据权限回放。
- `是否存在临时补丁或绕过`：否。不采用删包内角色、改测试服数据或手工跳过导入步骤的临时方案。

## BDD 场景

- `BDD: 缺失的系统类型角色可跨环境导入 -> Given 权限角色配置包包含 system type 角色且目标环境不存在同 code 角色 / When 导入配置包 / Then 角色被创建并写入菜单与基础数据范围字段，且不再触发系统内置角色数据权限更新禁令。`
- `BDD: 已存在的系统类型角色仍可原样回导 -> Given 目标环境已存在同 code 的 system type 角色 / When 导入配置包 / Then 角色基础字段与菜单更新成功，且仍跳过 assignRoleDataScope 禁止路径。`

## Milestones

1. M1：建立任务文档并确认历史回归与当前代码差异。`completed`
2. M2：先写 RED 回归测试，锁定“新增系统类型角色”导入异常分支。`completed`
3. M3：实现最小正式修复，不改变非问题路径。`completed`
4. M4：运行 GREEN 回归验证并补证据。`completed`
5. M5：提交当前任务最小变更。`completed`

## Expected Verification

- RED：
  - `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test`
- GREEN / REGRESSION：
  - `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test`
  - `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-system-role-import\bug-regression-evidence.md`

## Current Blockers

- 无代码阻塞；若要验证测试服务器实际回导结果，还需要把包含本修复的后端包部署到对应测试运行态。

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test` -> FAIL（RED，新增 system type 角色分支仍错误调用 `assignRoleDataScope(...)`）
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-system-role-import\bug-regression-evidence.md` -> PASS

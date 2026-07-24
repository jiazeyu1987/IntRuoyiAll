# 任务：角色配置包导入错误具体化

- Task ID: `20260701-role-config-package-import-error-clarity`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `in_progress`
- User Request: `我希望报错可以具体,不要出现系统异常这种无法精确定位错误位置的报错`

## Task Goal

修复角色配置包导入链路在校验失败时直接抛原生异常、最终被全局处理器收敛成“系统异常”的问题，让 `权限角色`、`组织角色`、`审批角色` 三类配置包导入都返回明确业务错误码和具体失败原因，便于前端 toast 与接口响应直接定位问题。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-zhaojie-feedback-attribution-permission-fix\task.md`
- 状态：`blocked`
- 处理说明：用户已切换到更高优先级的角色配置包导入异常问题；上一任务已显式阻塞，避免并行串线。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本轮命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`；继续按任务台账、严格 TDD 和 UTF-8 读写执行。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文任务文档、测试日志和证据文件统一显式 UTF-8；PowerShell 5.1 不使用 `&&`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。继续 fail fast，但把失败映射为明确业务异常，而不是泛化 500。
- `是否从根因和长期维护角度解决`：是。根因是导入服务直接抛 `IllegalArgumentException`，本轮统一改成带错误码、带原因的 `ServiceException`。
- `是否存在临时补丁或绕过`：否。不通过前端二次拼装“系统异常”文案或 try/catch 吞错来掩盖后端问题。

## BDD 场景

- `BDD: 权限角色配置包 JSON 非法时返回明确原因 -> Given 用户上传损坏的权限角色配置包 / When 发起导入 / Then 接口返回明确业务错误码和“JSON 非法”原因，而不是系统异常。`
- `BDD: 权限角色配置包字段缺失时返回具体字段 -> Given 用户上传缺少 role code、role name、menuKeys 等必填字段的配置包 / When 发起导入 / Then 接口返回明确业务错误码并指出缺失字段与对应角色。`
- `BDD: 组织角色与审批角色导入校验失败时也不再落到系统异常 -> Given 用户上传非法的组织角色或审批角色配置包 / When 发起导入 / Then 接口返回具体业务错误，而不是统一 500。`
- `BDD: 目标环境引用缺失仍保留原有精确报错 -> Given 配置包引用了目标环境不存在的菜单标识 / When 发起权限角色导入 / Then 继续返回 `CONFIG_PACKAGE_REFERENCE_MISSING` 及具体缺失引用。`

## Milestones

1. M1：建立任务文档并确认“系统异常”真实来源。`completed`
2. M2：补 RED 回归测试，锁定三类角色配置包导入的错误合同。`completed`
3. M3：实现最小正式修复，统一错误码与具体原因输出。`completed`
4. M4：运行 GREEN 回归、回填证据与部署建议。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system,yudao-module-dcc "-Dtest=RoleConfigPackageServiceImplTest,PostConfigPackageServiceImplTest,DccApprovalPositionConfigPackageServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-import-error-clarity\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-import-error-clarity\backend-api-evidence.md`

## Current Blockers

- 暂无代码阻塞；若要验证测试服务器真实提示，还需要把本次修复部署到测试运行态后再导入复验。

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system,yudao-module-dcc -Dtest=RoleConfigPackageServiceImplTest,PostConfigPackageServiceImplTest,DccApprovalPositionConfigPackageServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS

## Final Result

- 三类角色配置包导入在非法 JSON、版本不支持、必填字段缺失时，不再落到全局 500/系统异常。
- 新增错误码 `CONFIG_PACKAGE_CONTENT_INVALID(1_002_030_009)`，用于表达“配置包内容非法，原因：{}”。
- 权限角色导入继续保留原有精确错误：
  - `CONFIG_PACKAGE_FORMAT_UNSUPPORTED` 用于版本不支持
  - `CONFIG_PACKAGE_REFERENCE_MISSING` 用于目标环境缺少菜单标识
- 组织角色与审批角色导入也已统一为明确业务错误，不再只给“系统异常”。

## Current Status

- `completed`

# 任务：eDHR 对象级权限支持 super_admin 放行

## 任务目标

- 修复 `super_admin` 用户在 eDHR 表单填写链路中被对象级权限错误拒绝的问题，覆盖 `RECORD_TABLE:*:FILL` 等对象能力校验。
- 保持对象级权限规则对普通用户、角色、部门的显式匹配逻辑不变，不引入 fallback、静默忽略或假成功。
- 通过严格 TDD 补齐后端回归测试、缺陷证据与接口证据，确保 `admin` 账号“拥有所有权限”的语义落在 eDHR 对象级权限评估链路中。

## 当前状态

已完成。

## Current Status

completed

## 前一任务检查

- 后端前一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-role-management-split-rename-navigation\task.md`
- 当前状态：`BLOCKED`
- 处理说明：上一任务已因用户切换优先级显式阻塞，不再占用本次提交范围；本任务仅修改 eDHR 对象级权限服务、定向单测与本任务文档。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中说明：
  - 本次仅做本机后端源码、定向单测与任务证据整理，不执行真实 E2E、数据库 schema 变更、服务器写入、发布、备份恢复或远端联调。
- 适用强制门禁：
  - 对象级权限修复必须 fail-fast；若无法明确识别 `super_admin` 角色语义，不得用默认放行、捕获异常后继续或伪造权限结果掩盖真实问题。
  - 不得修改普通用户的 DENY/ALLOW 优先级语义；仅允许在明确命中的 `super_admin` 身份上增加正式放行规则。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不会把权限异常改成静默成功，也不会把非超管默认放行。
- `是否从根因和长期维护角度解决`：是。直接在 eDHR 对象级权限评估服务中识别 `super_admin` 并统一放行，避免各业务点散落特判。
- `是否存在临时补丁或绕过`：否。不会只在单个字段审计入口或单个 `FILL` 调用点打补丁。

## BDD 场景

- `BDD: super_admin 可通过 eDHR 对象级填写权限校验 -> Given eDHR 记录表对象存在且当前用户具备 super_admin 角色 / When 调用对象级权限门禁校验 RECORD_TABLE 的 FILL 能力 / Then 后端放行，不再抛出对象级权限不足。`
- `BDD: 普通用户仍遵守显式对象规则 -> Given eDHR 对象存在且普通用户命中 DENY 规则 / When 评估对应 ability / Then 后端仍返回 DENY，不因本次修复被默认放行。`
- `BDD: 对象级权限审计保留真实决策结果 -> Given super_admin 或普通用户触发对象级权限评估 / When 服务记录操作审计 / Then 审计结果分别记录真实的 ALLOW 或 DENY，不隐藏权限判断来源。`

## 里程碑

1. M1：已完成。创建任务文档并确认前一任务状态、经验门禁与 BDD 场景。
2. M2：已完成。补充 `super_admin` 对象级权限 RED 回归用例，稳定复现当前拒绝缺陷。
3. M3：已完成。最小修复 eDHR 对象级权限评估逻辑，并保持普通规则语义不变。
4. M4：已完成。运行 GREEN 验证、回写缺陷证据与接口证据、执行收尾预览。

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProEdhrPermissionScopeServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-edhr-object-permission-super-admin\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-edhr-object-permission-super-admin\backend-api-evidence.md`

## 最终验证结果

- `mvn -pl yudao-module-mes -Dtest=MesProEdhrPermissionScopeServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-edhr-object-permission-super-admin\bug-regression-evidence.md`：PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-edhr-object-permission-super-admin\backend-api-evidence.md`：PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-edhr-object-permission-super-admin --mode preview`：PASS，`blocked/warnings=<none>`；预览建议仅保留 `task.md` 与 `execution-log.md`

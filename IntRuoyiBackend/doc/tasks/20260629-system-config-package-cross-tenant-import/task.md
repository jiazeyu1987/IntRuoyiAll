# 任务：系统配置包支持跨租户导入

- Task ID: `20260629-system-config-package-cross-tenant-import`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `in_progress`

## Task Goal

将现有 `system/config-package` 从“同租户覆盖恢复工具”收口为“同环境跨租户配置迁移工具”，支持从 `tenant_id=1` 导出系统配置包，在 `tenant_id=122` 预检并导入，保留目标租户现有密码，不要求目标租户预先存在相同用户 ID。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-edhr-word-import-table-collapse-fix\task.md`
- 状态：`in_progress`
- 处理说明：该任务与本次配置包导入链路无代码重叠，当前不混改，仅在 `yudao-module-system` 范围内执行本任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接修改正式 `system-config-package` 预检/导入语义，支持跨租户迁移，不新增一次性脚本旁路。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 配置包允许跨租户预检 -> Given tenant 1 导出的系统配置包 / When tenant 122 执行预检 / Then 不因 sourceTenantId 与 targetTenantId 不同而阻塞。`
- `BDD: preserve_existing_passwords 支持跨租户导入新用户 -> Given 配置包中的用户账号在目标租户不存在 / When tenant 122 以保留目标密码策略导入 / Then 系统按账号映射已存在账号并保留密码，不存在账号则创建新用户且不导入源密码摘要。`
- `BDD: 配置包导入只覆盖目标租户 owned scope -> Given tenant 122 执行导入 / When 覆盖系统配置 / Then 只重建目标租户相关用户、角色、部门、岗位、关系与租户套餐，不误删其他租户数据。`

## Milestones

1. M1：建立任务文档并补 RED 测试。`completed`
2. M2：最小修改 `SystemConfigPackageServiceImpl` 支持跨租户预检与导入。`completed`
3. M3：运行定向测试并记录证据。`completed`
4. M4：用真实租户导出/导入链路验证并回填主任务。`pending`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=SystemConfigPackageServiceImplTest" test`

## Current Blockers

- 现有 `SystemConfigPackageServiceImpl` 强制 `sourceTenantId == targetTenantId`。
- 现有 `preserve_existing_passwords` 还要求目标租户必须预先存在相同 `userId`，不能承载跨租户导入新账号。
- 现有导入会无条件 `DELETE FROM system_tenant_package`、`system_menu`、`system_dict_type`、`system_dict_data`，需要确认跨租户导入时只覆盖本能力 owned scope。

## Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=SystemConfigPackageServiceImplTest" test`：PASS

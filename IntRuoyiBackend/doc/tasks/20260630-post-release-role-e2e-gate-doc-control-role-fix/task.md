# 任务：修复发布后角色门禁对旧 wenkong 编码的硬依赖

- Task ID: `20260630-post-release-role-e2e-gate-doc-control-role-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `in_progress`

## Task Goal

修复 `sql/mysql/20260618_post_release_role_e2e_gate.sql` 在测试服真实发布链路上的角色契约阻塞：当 `tenant_id=1 / 芋道源码` 不再存在启用中的 `code='wenkong'` 角色，而真实 DCC 角色基线已演进为 `doc_control/文控` 与 `wenkong_download/文控下载` 时，发布门禁必须以正式、可持续的方式识别当前有效 DCC E2E 角色并继续准备 `wangsiyu` 账号，而不能要求人工先改测试库或让发布停在旧角色编码假设上。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-mes-auto-schedule-tenant-package-release-fix\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成自动排产动作菜单同步租户包的发布契约修复，并真实越过测试服对应阻塞点；本次进入新的发布后角色门禁契约阻塞修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
  - 命中发布经验索引，需读取构建发布预检经验。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
  - required SQL 在测试服失败时先只读核对真实库状态、角色基线和租户前置条件，禁止手工改测试库绕过。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`
  - 发布失败优先排查 required SQL 契约与真实角色/菜单/账号基线一致性；修复后需重新走主分支 `build-release -> publish-test`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。回到正式发布 SQL 契约兼容当前有效 DCC 角色编码，不手工改测试库、不放宽发布脚本失败门禁。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 发布后 DCC E2E 角色兼容当前角色编码基线 -> Given tenant_id=1 的芋道源码租户已不存在 code=wenkong 但存在启用角色 doc_control/文控 与 wenkong_download/文控下载 When 执行 20260618_post_release_role_e2e_gate.sql Then 迁移必须识别当前有效 DCC E2E 角色并继续完成 wangsiyu 账号绑定。`
- `BDD: 旧角色编码仍存在时门禁保持兼容 -> Given tenant_id=1 仍存在启用中的 wenkong 角色 When 重复执行 20260618_post_release_role_e2e_gate.sql Then 迁移继续复用现有角色，不重复创建脏角色，也不放宽其他 fail-fast 门禁。`

## Milestones

1. M1：建立任务文档并记录新的真实发布阻塞。`completed`
2. M2：补 RED 门禁测试，证明 SQL 当前只硬依赖旧 `wenkong` 编码。`completed`
3. M3：最小修复 SQL 并通过 GREEN 验证。`completed`
4. M4：提交后端主分支修复并回到主分支真实发布闭环。`pending`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_post_release_role_e2e_gate_sql.py -q`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql`

## Current Blockers

- 无代码阻塞；`20260618_post_release_role_e2e_gate.sql` 已完成对当前 DCC 角色编码基线的兼容修复，定向测试与 migration gate 已通过，下一步进入后端主分支提交并重新执行主分支真实发布。

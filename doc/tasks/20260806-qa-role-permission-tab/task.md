# QA 权限角色与页签可见性

## Task Goal

新增或补齐 QA 权限角色；若当前系统缺少 QA 选线角色，则把 QA 选线权限赋予 admin；并确保只有 QA 权限角色可以看到 QA 页签。

## Milestones

- [x] 记录任务目标、BDD 场景和设计约束。
- [x] 定位现有 QA 页签、权限判断、角色矩阵和菜单/角色种子。
- [x] 新增 RED 静态合同，先证明当前 QA 页签权限未被 QA 角色独占。
- [x] 实现最小前端权限门禁和必要的角色/菜单种子补齐。
- [x] 跑通 GREEN、相邻回归、证据校验和差异检查。
- [ ] 收尾清理、提交和推送，或记录阻塞原因。

## Expected Verification

- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `node tests/e2e/qa-regulation-role-permission-static.spec.cjs`
- `git diff --check -- <task-owned-files>`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-qa-role-permission-tab/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260806-qa-role-permission-tab/database-schema-evidence.md`
- `pnpm ts:check`

## Current Status

ready_for_closeout

实现和验证已完成；提交/推送暂未执行，因为共享工作区存在大量非本任务脏改动，不能安全做全量 dirty baseline 或 broad stage。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；按正式角色和权限判断控制 QA 页签可见性。
- `是否从根因和长期维护角度解决`：是；权限角色、admin 赋权和页签可见性需要由同一可验证权限契约约束。
- `是否存在临时补丁或绕过`：否。

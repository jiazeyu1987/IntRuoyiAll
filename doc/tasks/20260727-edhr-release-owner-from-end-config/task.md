# 修复放行负责人解析

## Task Goal

让 eDHR 批次工作台、放行预检/审批展示、电子签名授权和放行审批任务统一使用工艺路线“工序结束 > 放行责任人”的路线级 `RELEASE_APPROVE` 配置。未配置或配置无效时明确失败，不回退到关闭负责人、当前用户或静态“执行人”。

## Milestones

1. 建立 BDD 场景并补充后端、前端失败测试，记录 RED。
2. 后端返回放行负责人解析结果，并让正式放行授权复用 `RELEASE_APPROVE` 候选人解析。
3. 前端放行预检和放行审批展示 `releaseSummary.releaseOwnerLabel`，非放行阶段保持原逻辑。
4. 运行目标测试、编译、静态契约和可用的真实页面验证，记录 GREEN/REGRESSION。
5. 完成验证报告、closeout preview/apply、任务提交和远端同步。

## Expected Verification

- 后端覆盖具体用户、权限角色、签名放行、关闭负责人不得越权、缺失/无效放行配置 fail-fast。
- 前端静态契约确认放行阶段读取 `releaseOwnerLabel`，不对放行阶段静默回退到 `stageOwnerRole`。
- 使用真实登录和真实批次通过 Playwright 打开批次详情并核对放行节点负责人。
- 目标任务提交及其收尾记录提交到当前分支并推送 `origin`；不得混入其他任务改动。

## Current Status

blocked_on_real_e2e_runtime_reload_and_shared_build_slot

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；放行配置缺失或无效时保持明确失败。
- 是否从根因和长期维护角度解决：是；展示和授权共用路线级 `RELEASE_APPROVE` 来源与候选人解析。
- 是否存在临时补丁或绕过：否；不修改路线数据、不新增迁移、不用关闭负责人替代。

## 经验门禁

- eDHR 详情/工作台配置来源必须追溯到正式路线规则，不能从当前登录人、创建人、更新人或静态角色推断负责人。
- 真实 E2E 必须走真实前端用户路径；缺少登录、租户、运行服务或测试批次时 fail-fast，不以 API-only 或 mock 替代。
- 本任务不涉及数据库迁移、菜单权限、服务重启或 worktree 操作；如后续触发这些动作，先读取对应项目规则文件。

## Cleanup Keep

doc/tasks/20260727-edhr-release-owner-from-end-config/task.md
doc/tasks/20260727-edhr-release-owner-from-end-config/execution-log.md
doc/tasks/20260727-edhr-release-owner-from-end-config/verification-report.md

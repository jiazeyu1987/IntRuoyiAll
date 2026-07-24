# 任务：待归属人员回显改为 simple-list（前端）

- Task ID: `20260701-zhaojie-feedback-user-select-simple-list-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

修复 `UserSelectV2` 在待归属归属场景中按 ID 回显选中用户时仍调用 `system/user/list` 的前端合同问题，改为使用无需 `system:user:query` 的 `system/user/simple-list` 回显，从而避免 `zhaojie` 这类仅具备业务权限的账号在归属弹窗中再次触发无关的系统用户查询权限依赖。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-zhaojie-feedback-attribution-permission-fix\task.md`
- 状态：`completed`
- 处理说明：上一任务已收口待归属写入口权限；本轮继续修复归属弹窗内部人员回显对 `system:user:query` 的额外依赖。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 前端源码、静态测试与任务文档统一按 UTF-8 处理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接修正 `UserSelectV2` 的回显数据来源，不通过静默吞掉 403、伪造默认用户或跳过回显来掩盖权限问题。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 缺少 system:user:query 时仍能回显已选归属人 -> Given 业务页面只需要展示已选用户标签 / When UserSelectV2 根据 ID 解析当前值 / Then 组件应通过 system/user/simple-list 回显昵称，而不是请求 system/user/list。`
- `BDD: 回显用户按输入 ID 集合过滤 -> Given 组件收到单个或多个用户 ID / When simple-list 返回候选集 / Then 只保留命中的用户项，不因缺失项抛异常。`

## Milestones

1. M1：建立前端任务台账并确认 UserSelectV2 的旧回显合同。`completed`
2. M2：补 RED 静态合同，证明旧版仍依赖 `system/user/list`。`completed`
3. M3：实现 simple-list 回显并跑到 GREEN。`completed`
4. M4：回填证据与结论。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-user-select-permission-static.spec.js`

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-user-select-permission-static.spec.js` -> `PASS`
- 结论：
  - `UserSelectV2` 的按 ID 回显链路已改为 `UserApi.getSimpleUserList()`。
  - 组件不再继续调用 `UserApi.getUserList(ids)`，从而避免业务页面因缺少 `system:user:query` 再次误报权限。

## Current Blockers

- 无。

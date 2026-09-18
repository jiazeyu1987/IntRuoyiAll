# EDHR-STATIC-019 原始修订签名验真

## Task Goal

修复 EDHR-STATIC-019：原始记录修订入口不得信任客户端提交的修改人、签名用户、签名编号或签名快照。服务端必须基于当前登录用户和签名密码/正式签署服务生成修订签名证据，并将签名绑定到修订对象、签署含义、服务器时间和修订快照。

## Scope

- 仅处理 `EDHR-STATIC-019`。
- 修改范围限定在原始记录修订 `update-original` 链路及对应定向测试/任务证据。
- 不编辑共享缺陷总表；最终答复仅给出建议更新内容。
- 禁止 Playwright/E2E、数据库写入、启动/停止/重启服务、远程服务器操作。
- 2026-09-14 用户已明确授权 Git commit 与本地融合进 `int_main`；Git push 未获本轮明确授权。

## Milestones

| Milestone | Status | Notes |
| --- | --- | --- |
| M1 规则与缺陷定位 | completed | 已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、eDHR/电子签名/审计追踪相关规则与缺陷记录。 |
| M2 BDD 与 RED | completed | 已记录 Given/When/Then，并用前后端定向静态合同 RED 证明旧请求仍暴露客户端身份/签名字段。 |
| M3 最小修复 | completed | VO 仅接收业务字段、修改原因、签名密码；Controller 注入当前登录人；Service 调用正式签名服务生成修订签名证据，并落库返回的签名 ID、签名人、签名快照和服务端签署时间。 |
| M4 GREEN 与静态合同验证 | completed | 定向前端静态合同、后端 Maven 回归和 `git diff --check` 均已通过；未执行 E2E/DB/服务操作。 |
| M5 收尾记录 | completed | 已写入验证报告；本轮已解除 commit/本地融合限制，push 仍未获明确授权。 |
| M6 本地 `int_main` 融合 | completed | 当前分支实现提交已 cherry-pick 到 `int_main` 为 `795ed3063`，任务证据提交为 `57225bd13`；冲突仅发生在经验索引并已保留两侧关键词。 |
| M7 主线复验与最终状态 | blocked | `int_main` 主线复验通过；项目规则要求 push 后才能标记 completed，但 Git push 未获本轮明确授权。 |

## Expected Verification

- BDD/RED/GREEN 记录在 `execution-log.md`。
- 定向测试覆盖：
  - 请求 VO 不再含 `modifiedByUserId`、`revisionSignatureUserId`、`revisionSignatureId`、`revisionSignatureSnapshot`。
  - 请求 VO 必须含 `signaturePassword`。
  - Controller 必须将 `getLoginUserId()` 注入内部 BO。
  - Service 必须调用服务端签名能力，并使用返回的真实签名 ID、actor 和快照落修订记录。
  - Service 不得使用客户端提交的审计身份或签名快照。
- 禁止执行：Playwright/E2E、数据库写入、启动/停止/重启服务、远程服务器操作、Git push。

## Design Constraint Check

- No fallback：不保留旧字段兼容路径，不接受客户端自填签名证据。
- Fail fast：缺少当前用户、签名密码、事件、差异或签名服务结果时直接拒绝。
- Server-owned identity：修改人、签名人、签名 ID、签名快照由服务端生成或注入。
- Signature binding：签名主题绑定 `PROCESS_POOL_EVENT_REVISION`、事件 ID、签署含义、变更原因和修订后快照哈希。
- Audit integrity：保留原始 before payload、after payload、字段 diff、服务器修订时间和签名快照。
- Scope control：不处理 015/016/017/018/020+，不改共享缺陷总表。

## Current Status

blocked

- 实现与定向验证已完成；当前任务分支提交为 `14a804053`、`cf4403b40`。
- 本地 `int_main` 已融合本任务代码与证据，提交为 `795ed3063`、`57225bd13`，且两者均为当前 `int_main` 祖先。
- 主线复验通过：端口门禁 PASS、`git diff --check HEAD~2..HEAD` PASS、前端静态合同 PASS、后端 Maven 35 tests PASS。
- `task-closeout-cleanup` 在 `E:\IntRuoyi` preview 为 ready 且 delete 为空；未执行 apply/标记 completed，因为项目规则要求 Git push，而本轮未获明确 push 授权。
- 未执行 Playwright/E2E、数据库写入、服务启动/停止/重启、远程服务器操作或 Git push。

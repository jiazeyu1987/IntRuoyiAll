# 20260725 融合 jiluben worktree 残留差异

## Task Goal

在已完成 `merge-jiluben-worktree-20260724` 后，继续核对 `D:\IntRuoyiWorktree\jiluben_20260722_clean` 与当前 `int_main` 的内容级差异，补齐仍未融合的记录本/eDHR/批记录/路线表单/字段审计残留改动，同时保留上一轮已验证的冲突修复逻辑和 `int_main` 端口规则。

## Milestones

- [x] M1: 重新读取 worktree、端口、任务收尾、后端、前端、数据库、E2E、PowerShell 规则和相关技能。
- [x] M2: 用内容哈希比对源 worktree 与当前 `int_main`，确认仍有 40 个内容差异文件。
- [x] M3: 分析残留差异并补齐应进入 `int_main` 的正式实现、SQL、测试和前端合同。
- [x] M4: 运行前端定向验证、端口守卫和 Git 空白检查。
- [ ] M5: 经验沉淀、cleanup、提交、推送和最终收尾。

## Expected Verification

- 后端：本次残留融合未接受源 worktree 的后端旧差异；保留上一轮已验证的 `int_main` 审计、附件原因、工单操作记录和路线快照逻辑，因此不新增后端改动和 Maven 定向测试。
- 前端：运行 TypeScript 类型检查和受影响静态合同，覆盖执行填写工作区、记录本批次同步、时间格式、路线批记录保存、eDHR 批次详情右栏元信息。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过，确认 `int_main` 仍为 frontend `8081` / backend `48081`。
- `git diff --check` 无空白错误。
- 若残留差异与上一轮已验证冲突修复相冲突，必须保留已验证正式逻辑并记录不直接覆盖的原因。

## BDD Scenarios

- BDD: 残留记录本字段完整融合 -> Given 源 worktree 仍包含记录本批次字段、审计字段和上下文 VO 差异, When 用户打开或创建批记录/eDHR 执行任务, Then `int_main` 必须保留正式记录本上下文字段、审计 hash 输入和执行响应字段，不丢失批次同步证据。
- BDD: 路线表单批记录配置完整融合 -> Given 源 worktree 仍包含路线批记录绑定响应/保存字段差异, When 用户保存路线流程表单配置, Then 批记录和记录本相关配置必须随正式接口保存并在投影/任务创建时可追溯。
- BDD: 前端记录本合同与真实路径同步 -> Given 前端 API wrapper、批记录组件和 E2E 合同仍有残留差异, When 用户保存记录本批次或执行填写工作区, Then 请求字段、按钮路径和静态合同必须与当前页面真实路径一致，不保留废弃弹窗或 API-only 替代。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按内容差异补齐正式合同和测试，不用默认值掩盖缺失。
- `是否存在临时补丁或绕过`：否。
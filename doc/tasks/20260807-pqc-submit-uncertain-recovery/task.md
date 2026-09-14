# PQC 正式提交不确定响应恢复确认

## Task Goal

增强一线 PQC 正式提交按钮：当正式提交请求出现响应不确定或网络异常时，前端必须先通过只读状态确认当前 PQC 任务是否已经提交；若已提交则回填正式回执并锁定按钮，若未提交才允许用户看到失败并重试，避免用户因不确定失败重复点击。

## Milestones

- M1：核对现有 PQC 提交链路、可用只读接口和任务文档经验门禁。✅
- M2：补充 BDD 与 RED 静态合同，先证明当前提交 catch 缺少不确定状态恢复确认。✅
- M3：实现最小前端恢复确认逻辑，保持正式提交 API、签名和后端重复保护不变。✅
- M4：运行目标静态合同、类型检查和差异校验，归档证据。✅
- M5：完成验证报告与收尾状态。✅

## Expected Verification

- `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js`
- `pnpm e2e:frontline-formal-submit:static`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes -am "-DskipTests" compile`
- `git diff --check -- IntRuoyiFronted/src/api/mes/pro/feedback/index.ts IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js doc/tasks/20260807-pqc-submit-uncertain-recovery`

## Current Status

completed

实现、核心验证、evidence validator 与 task-closeout-cleanup 已完成；本任务已收尾。按项目 Git Policy，本轮未执行提交或推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；不确定响应恢复确认是正式只读状态确认，不替代失败、不伪造成功。
- `是否从根因和长期维护角度解决`：是；将提交成功后响应丢失的状态归属从“可重复点击”改为“只读确认后决定是否锁定”。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary

- 前端写入成功与刷新/后续确认失败必须分层；写请求成功或不确定时不得让用户盲目重复写入。
- 提交按钮失败必须终止在可见错误边界；失败后不得吞错、默认成功或继续发写请求。
- 写入响应不确定时必须优先用稳定业务 ID 做只读断点恢复判断，禁止盲目重放导致重复提交。

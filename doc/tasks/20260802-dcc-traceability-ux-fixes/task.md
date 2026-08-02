# DCC 签核追溯 UX 修复

## Task Goal

修复 DCC 受控文件详情中的签核追溯可读性问题：权限提示业务化、操作日志空态闭环、审批意见与签名证据合并展示、发布/盖章文件可点击验证、签名失败诊断可操作化。范围仅限本场景，不修复其它 DCC 或 MES 场景。

## Milestones

1. [x] 记录 BDD 与 RED 静态合同，锁定当前缺口。
2. [x] 修复受控文件详情页签核追溯与签名失败提示。
3. [x] 修复文控操作日志页目标文件空态说明。
4. [x] 运行静态合同、类型检查和相关回归。
5. [x] 通过真实 Playwright 页面路径验证已有任务自有受控文件追溯显示，并记录 PASS/BLOCKED 证据。

## Expected Verification

- `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS
- DCC 相关静态回归 -> PASS
- 真实 Playwright E2E -> 主查看链路 PASS；权限提示低权限触发和错误密码诊断为受控 BLOCKED，原因见报告。

## Current Status

ready_for_closeout

## Verification Summary

- Static RED: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> FAIL，原因为权限提示未业务化。
- Static GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- Type check: `pnpm ts:check` -> PASS。
- Regression: 文控日志、受控浏览详情入口、详情退休路由、签核追溯 UX 静态合同 -> PASS。
- Real E2E: `traceability-ux-real-e2e-result-20260802112712.json` -> status PASS，`dccWriteRequests=[]`。
- Secret scan: `NO_PASSWORD_LITERAL_FOUND`。

## Applicable Gates

- DCC 审批/追溯 E2E 必须走真实页面，API/DB 只用于最终只读核验。
- 主链路 PASS 与扩展诊断产物必须隔离，不能让失败诊断覆盖主链路证据。
- 前端静态合同需隔离当前需求，不能用大范围既有失败阻塞本次最小修复判断。
- 操作日志无数据时必须明确告知签核证据所在页面，不能让用户误判为证据缺失。

## Blockers

- 低权限上传人账号在受控浏览入口看不到目标文件行，无法真实触发“高级签名留痕需额外权限”的页面提示；代码和静态合同已验证文案，E2E 记录为 BLOCKED。
- 复用文件已 ACTIVE，页面无待办签名按钮；为避免破坏主链路，未创建新审批任务做错误密码写入型诊断，E2E 记录为 BLOCKED。
- 仓库存在大量非本任务脏改，未执行提交/推送；本任务代码和证据已完成，收尾集成需先处理共享工作区状态。

## Experience Consolidation

已按 `project-experience-consolidation` 技能复核。本次可复用经验已由现有 `docs/e2e-rules.md` 中“主链路与扩展诊断产物隔离”“DCC 文控审批处理入口门禁”覆盖；没有新增长期经验文档，避免记录一次性文件 ID 或临时账号状态。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接增强用户可见追溯字段、空态和错误诊断。
- `是否存在临时补丁或绕过`：否。

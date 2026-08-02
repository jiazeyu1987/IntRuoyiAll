# DCC 签核追溯 UX 修复

## Task Goal

修复 DCC 受控文件详情中的签核追溯可读性问题：权限提示业务化、操作日志空态闭环、审批意见与签名证据合并展示、发布/盖章文件可点击验证、签名失败诊断可操作化。范围仅限本场景，不修复其它 DCC 或 MES 场景。

## Milestones

1. [x] 记录 BDD 与 RED 静态合同，锁定当前缺口。
2. [x] 修复受控文件详情页签核追溯与签名失败提示。
3. [x] 修复文控操作日志页目标文件空态说明。
4. [x] 运行静态合同、类型检查和相关回归。
5. [x] 通过真实 Playwright 页面路径验证已有任务自有受控文件追溯显示，并记录 PASS 证据。
6. [x] 按用户授权创建新的任务自有原版文件，完成错误密码诊断、四级审批/签名、发布生效和低权限提示复验。

## Expected Verification

- `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS
- DCC 相关静态回归 -> PASS
- 真实 Playwright E2E -> 新任务自有文件原版上传、四级审批/签名、发布生效、低权限提示、错误密码诊断均 PASS。

## Current Status

ready_for_closeout

## Verification Summary

- Static RED: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> FAIL，原因为权限提示未业务化。
- Static GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- Type check: `pnpm ts:check` -> PASS。
- Regression: 文控日志、受控浏览详情入口、详情退休路由、签核追溯 UX 静态合同、详情页渲染安全静态合同 -> PASS。
- Real E2E create/sign/publish: `dcc-original-release-wrong-password-20260802115503.json` -> status PASS。
- Real E2E traceability: `traceability-ux-real-e2e-result-20260802120622.json` -> status PASS，`dccWriteRequests=[]`。
- Closeout cleanup: preview/apply -> PASS，`blocked=<none>`，`warnings=<none>`，仅清理旧轮次重复证据并保留最终脚本/JSON/截图/CSV。
- Task-owned controlled file: `CODX-DCC-TRACE-DIAG-20260802115503` / `2054545668044070299` / `V1.0` / `ACTIVE`。
- Low-permission prompt: non-admin `zhaojie` sees target row and page shows “当前可查看签核追溯摘要；高级签名留痕需 DCC 电子签名管理权限。”
- Wrong-password diagnostic: non-admin `zhaohaichen` first approval node returned `1080000022` and page showed reason, handling suggestion, and responsibility entry; correct password then completed the chain.
- Secret scan: `NO_PASSWORD_LITERAL_FOUND`。

## Applicable Gates

- DCC 审批/追溯 E2E 必须走真实页面，API/DB 只用于最终只读核验。
- 主链路 PASS 与扩展诊断产物必须隔离，不能让失败诊断覆盖主链路证据。
- 前端静态合同需隔离当前需求，不能用大范围既有失败阻塞本次最小修复判断。
- 操作日志无数据时必须明确告知签核证据所在页面，不能让用户误判为证据缺失。

## Blockers

- 原 E2E 缺口已解除：使用任务自有新文件完成错误密码诊断，并使用可见目标文件但无高级签名管理权限的非 admin 账号完成权限提示复验。
- 仓库存在大量非本任务脏改，未执行提交/推送；本任务功能验证和 cleanup 已完成，最终 `completed` 与集成提交需先处理共享工作区状态。

## Cleanup Keep

- doc/tasks/20260802-dcc-traceability-ux-fixes/dcc-original-release-with-wrong-password-e2e.cjs
- doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs
- doc/tasks/20260802-dcc-traceability-ux-fixes/dcc-original-release-wrong-password-20260802115503.json
- doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e-result-20260802120622.json
- doc/tasks/20260802-dcc-traceability-ux-fixes/signature-trace-ux-export-20260802120622.csv
- doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-detail-20260802120622.png
- doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-file-evidence-viewer-20260802120622.png
- doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-operation-logs-20260802120622.png
- doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-permission-prompt-20260802120622.png

## Experience Consolidation

已按 `project-experience-consolidation` 技能复核。本次可复用经验已由现有 `docs/e2e-rules.md` 中“主链路与扩展诊断产物隔离”“DCC 文控审批处理入口门禁”覆盖；没有新增长期经验文档，避免记录一次性文件 ID 或临时账号状态。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接增强用户可见追溯字段、空态和错误诊断。
- `是否存在临时补丁或绕过`：否。

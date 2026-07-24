# 20260711 直接报工导入展示未创建行明细

## Task Goal

修复直接报工导入结果大弹框在 `created=0、skippedRows>0` 时只显示“本次导入未创建报工明细”的问题；实际 Excel 已解析出导入行时，应展示导入信息和未创建原因，而不是让用户误以为没有导入信息。

## Milestones

1. 核对当前直接报工导入结果 DTO、后端构造逻辑和历史 UI 明细口径。
2. 补充回归测试，证明跳过/未创建行也会返回并展示导入明细。
3. 修改后端结果或前端展示，使弹框显示已解析导入行、状态和原因。
4. 执行目标测试、前端类型检查和真实导入 E2E。
5. 记录验证证据并提交任务相关改动。

## Expected Verification

- 后端目标测试覆盖 `created=0、skippedRows>0` 时返回 directWorkReportDetails。
- `node tests/e2e/mes-direct-work-report-import-result-static.spec.js`
- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- 真实导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 后弹框展示导入行信息。

## 经验门禁

- PowerShell / Windows shell / 中文编码：中文读写必须显式 UTF-8，禁止 Bash heredoc 和 `&&`。
- 项目级防错 / 报工导入旧工序：不得改变直接报工旧业务口径，不得把未创建行伪造成创建成功。
- 前端页面 / 表格 / 样式：沿用当前大弹框，不引入额外重设计。
- 真实 E2E：执行前必须使用测试租户真实路径验证，不使用 mock 或接口绕过。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，修正结果 DTO/展示契约，让“解析到但未创建”的导入行可追溯。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## Milestone Status

1. completed - 已核对 DTO、后端构造逻辑和当前大弹框展示口径。
2. completed - 已补充后端与前端回归测试，锁定跳过行也要展示导入明细。
3. completed - 已让后端返回 SKIPPED 行级明细，并在前端展示状态 / 原因。
4. completed - 已执行目标后端测试、前端静态合同、类型检查、登录前置和真实导入 E2E。
5. completed - 已记录验证证据并提交任务相关改动。


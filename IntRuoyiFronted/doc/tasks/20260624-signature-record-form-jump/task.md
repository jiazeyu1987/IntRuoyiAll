# 任务：签名记录跳转到对应表单

## 任务目标

修正电子签名一级页签下批记录签名记录的执行编号跳转：用户点击签名记录中的 `BRE...` 执行编号时，不进入执行详情摘要页，而是进入该签名所属执行记录的只读表单视图，直接展示电子批记录表单与签名证据。

## 里程碑

- [x] M1：确认上一轮统一电子签名任务已完成，读取经验门禁并创建任务文档。
- [x] M2：先写 RED 静态回归测试，锁定签名记录跳转必须进入表单视图。
- [x] M3：新增执行表单隐藏路由，并调整签名记录执行编号跳转 query。
- [x] M4：运行静态契约、类型检查与真实 E2E 登录页面验证。
- [x] M5：提交本任务相关前端改动，不包含主仓其它脏改动。

## 预期验证

- `node tests/e2e/edhr-signature-change-execution-entry-static.spec.js`
- `node tests/e2e/signature-governance-e2e-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check`
- 真实 Playwright：使用本机 `http://localhost:8081`、`测试租户/aoteman/111111` 打开 `/signature-governance?tab=batch-signatures`，点击真实 `BRE...` 签名记录，确认进入执行表单视图并展示 `电子批记录表单`，不停留在详情摘要。

## 当前状态

已完成。签名记录执行编号已跳转至表单入口 `/mes/pro/feedback/edhr-execution/form?id=<executionId>&viewMode=tracking`，真实 E2E 已验证。

## 前一任务检查

- 前端上一相关任务 `20260624-unified-electronic-signature-primary-tab` 已标记“已完成”，允许继续本任务。
- 当前前端仓库存在排程、报工、DCC 等其它任务脏改动，本任务只修改签名跳转相关文件、测试和任务文档。

## 经验门禁

- `docs/login-access.md`：真实 E2E 默认本机 `http://localhost:8081`，使用测试租户 `测试租户/aoteman/111111`；登录失败必须阻塞，不切换账号或环境。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：保持密集操作台风格，不做无关视觉重构；表格链接保持轻量文本入口。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少 executionId 时保持原有不可点击状态；加载失败由目标执行表单页显式暴露。
- `是否从根因和长期维护角度解决`：是。通过独立表单路由和只读表单视图表达签名入口语义，而不是在详情页内临时滚动或文案绕过。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 签名记录进入对应表单 -> Given 用户打开电子签名一级页签的批记录签名记录 / When 点击某条真实签名记录的执行编号 / Then 页面进入该执行记录的表单视图并展示电子批记录表单与签名证据。`
- `BDD: 签名入口不进入详情摘要 -> Given 签名记录行存在 executionId / When 前端构造跳转 / Then 使用 /mes/pro/feedback/edhr-execution/form 且携带 viewMode=tracking，不再使用 /mes/pro/feedback/edhr-execution/detail 作为签名入口目标。`

## 最终验证结果

- `GREEN: node tests/e2e/edhr-signature-change-execution-entry-static.spec.js -> PASS`
- `GREEN: node tests/e2e/signature-governance-e2e-static.spec.js -> PASS`
- `GREEN: node scripts/signature-governance-page-contract.test.mjs -> PASS`
- `GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check -> PASS`
- `GREEN: real Playwright E2E -> PASS`，`测试租户/aoteman` 点击真实签名记录 `BRE202606241216518420560` 后进入 `/mes/pro/feedback/edhr-execution/form?id=560&viewMode=tracking`。

## Cleanup Keep

- `doc/tasks/20260624-signature-record-form-jump/task.md`
- `doc/tasks/20260624-signature-record-form-jump/execution-log.md`
- `doc/tasks/20260624-signature-record-form-jump/bug-regression-evidence.md`
- `doc/tasks/20260624-signature-record-form-jump/frontend-feature-evidence.md`

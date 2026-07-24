# eDHR 批次执行前端实现任务

- Task ID: `20260608-edhr-batch-execution-full-flow`
- Status: `completed`
- Branch: `codex/edhr_batch`
- Source Spec: `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260608-edhr-batch-execution-full-flow\`

## 任务目标

实现 eDHR 批次执行工作台、批次详情、复盘/归档入口和 API helper，让用户从批次级流程进入工序表单填写、多人签名、审批、关闭、复盘和最终打印。

## 里程碑

1. RED：新增前端 API/page/E2E 静态检查，确认目标入口尚不存在。
2. GREEN：新增 API helper、列表页、详情页、复盘页、路由、真实 E2E 脚本。
3. REGRESSION：运行 `pnpm ts:check` 和现有 eDHR E2E 静态检查。
4. E2E：使用真实测试租户通过前端路径验证。

## 预期验证

- `pnpm ts:check`
- `node --check tests/e2e/edhr-batch-execution-real-flow.e2e.js`
- `pnpm e2e:edhr:batch-execution:submit-gate`
- 真实 Playwright E2E。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；页面展示后端阻塞项，不隐藏错误。
- `是否从根因和长期维护角度解决`：是；新增批次执行主入口，生产报工只保留辅助入口。
- `是否存在临时补丁或绕过`：否；不为 E2E 添加临时控件或 mock 数据。

## 当前状态

- 状态：已完成。
- 已完成：API helper、批次列表页、详情页、复盘页、hidden routes、真实路径 E2E 脚本；列表和详情支持归档生成、下载、打印入口；无可编辑字段模板的复核/提交门禁修复。
- 验证证据：
  - `pnpm install --frozen-lockfile`：通过。
  - `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`：通过。
  - `pnpm e2e:edhr:batch-execution:check`：通过。
  - `pnpm e2e:edhr:batch-execution:submit-gate`：通过。
  - Playwright 真实测试租户全流程：通过，批次 `EDHR-BATCH-122-FULL-0609020810` 完成打开、填写、15 张单表复核签名、主数据追溯校验、提交、审批、批次关闭、复盘查看、最终 PDF 下载和打印窗口打开。
  - 融入后 `int_main` Playwright 全流程：通过，批次 `EDHR-BATCH-122-MAIN-0609013248` 在合并后后端 `48081`、前端 `8081` 完成 15 张必填单表复核签名、主数据追溯、提交、审批、关闭、归档、最终 PDF 下载、打印窗口打开和复盘查看；最终 `status=40`、`taskApprovedCount=15`、`blockedCount=0`、归档 `SEALED`。

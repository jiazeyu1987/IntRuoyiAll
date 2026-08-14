# 生产组长报工管理列表为空排查修复

## Task Goal

修复用户截图中的“生产组长工作台 > 报工管理”列表为空问题。该页不同于 MES 报工总表，数据源是工序池时间线 `/mes/pro/process-pool/team-leader/submission/page`。

## Milestones

- [x] 识别截图页面和真实数据源
- [x] 复现当前日期筛选下接口返回
- [x] 增加回归合同，锁定默认日期空态显示口径
- [x] 实施最小修复
- [x] 运行定向验证和真实页面只读复验

## Expected Verification

- `node tests/e2e/team-leader-report-nearest-submit-date-static.spec.cjs`
- `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs`
- `node tests/e2e/production-leader-function-tabs-static.spec.js`
- `node tests/e2e/team-leader-production-report-history-tab-static.spec.cjs`
- `node tests/e2e/team-leader-report-allocation-static.spec.cjs`
- `pnpm ts:check`
- `node doc/tasks/20260808-team-leader-report-empty/verify-team-leader-report-nearest-date.cjs`

## Current Status

completed - 实现、定向验证、真实页面只读复验、技能证据校验和 task-closeout-cleanup apply 均已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；只用正式报工管理分页接口按日期查询，不造假行、不吞接口错误、不删除必填提交日期。
- `是否从根因和长期维护角度解决`：是；根因是默认提交日期固定为今天，今天无工序池报工时间线记录但最近历史日期有正式记录。
- `是否存在临时补丁或绕过`：否；未改数据库，未改后端契约，未用 API-only 冒充页面通过。

## Applicable Gates

- `docs/frontend-development.md`：前端行为变更需 RED/GREEN、定向静态合同、类型检查和真实页面验证。
- `docs/e2e-rules.md`：真实页面验证使用 Playwright，只读范围内记录目标接口和写请求数。
- `docs/login-access.md`：只读验证使用截图同一身份标签 `芋道源码/admin`，不记录密码。
- `docs/local-runtime.md`：本机入口为 `127.0.0.1:8081` 与 `127.0.0.1:48081`。

## Cleanup Candidates

- `doc/tasks/20260808-team-leader-report-empty/verify-team-leader-report-nearest-date.cjs`
- `doc/tasks/20260808-team-leader-report-empty/evidence/`
- `doc/tasks/20260808-team-leader-report-empty/bug-regression-evidence.md`
- `doc/tasks/20260808-team-leader-report-empty/frontend-feature-evidence.md`

## Cleanup Result

- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-team-leader-report-empty --mode preview`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-team-leader-report-empty --mode apply`
- Retained: `task.md`, `execution-log.md`, `verification-report.md`

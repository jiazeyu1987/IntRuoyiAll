# 20260725 测试管理手动重排 881MO 测试项

## Task Goal

在 `系统管理 > 测试管理` 中新增并验证一个真实前端测试项：使用 `芋道源码/admin` 对排产工单来源生产工单号 `881MO093613`、`881MO093615` 执行手动重排，并验证重排结果、橙色产品编号、最近一次成功排产时间以及生产排产甘特图范围。

## Milestones

- [x] 隔离任务开始前既有脏工作区改动，并记录基线提交证据。
- [x] 确认测试管理页签、接口、持久化表与既有排产手动重排 E2E 证据。
- [x] 通过真实前端或授权产品路径新增测试管理测试项及检查点。
- [x] 使用 Playwright 执行真实前端全量 E2E 验证。
- [ ] 记录 verification-report，完成清理、提交与推送。

## Expected Verification

- `系统管理 > 测试管理` 在 `芋道源码/admin` 下可见并可找到新增测试项。
- 测试项包含目标测试数据 `881MO093613,881MO093615` 和四个检查点。
- Playwright 真实前端路径完成手动重排，且只读核验满足：
  - 重排成功。
  - 只有来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单产品编号变成橙色。
  - 最近一次成功排产时间更新为本次排产时间。
  - 生产排产页签甘特图有且仅有这两个工单。

## 经验门禁

- Element Plus 表格选择门禁：手动重排前必须按页面可见业务唯一文本定位 881MO093613、881MO093615，限定可见 body 行复选框，写入动作前断言已选集合完全等于目标集合。
- Codex Runner 自动测试门禁：新增或运行 系统管理 到 测试管理 测试项前，必须确认本机前端/后端入口、目标租户账号、Runner/Playwright 前置条件与 parallelSafe=false 写入项的顺序执行要求。
- 官方登录前置与 admin-only 全量验证门禁：使用 scripts/preflight/login-preflight.mjs 和 芋道源码/admin 身份标签，密码不得写入日志；不得用 API-only、mock 或缺失 preflight 冒充 E2E 通过。
- 脏工作区基线门禁：当前任务文件不得混入既有脏区基线提交；记录 baseline commit hash、文件清单和后续 git status short branch。
## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务将测试项纳入测试管理并用真实前端 E2E 验证，不以 API-only 或 mock 代替。
- `是否存在临时补丁或绕过`：否。


## Cleanup Keep

- doc/tasks/20260725-test-management-manual-replan-881mo/test-management-manual-replan-full.e2e.cjs
- doc/tasks/20260725-test-management-manual-replan-881mo/artifacts/test-management-manual-replan-summary.json
- doc/tasks/20260725-test-management-manual-replan-881mo/artifacts/manual-replan/repair-verification-report.json
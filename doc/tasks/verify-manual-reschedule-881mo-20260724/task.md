# 验证排产工单手动重排 881MO093613/881MO093615

## Task Goal

验证在排产工单页签中选择来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单并执行“手动重排 -> 开始重排 -> 确认应用重排”后，真实前端路径是否满足以下目标：

- 重排成功。
- 只有来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单的产品编号变成橙色。
- 最近一次成功排产时间更新为本次排产时间。
- 生产排产页签里的甘特图有且仅有这两个工单。

## Milestones

- [x] 创建任务记录并确认前置规则。
- [x] 确认前端、后端、账号、权限、测试数据等真实验证前置条件。
- [x] 通过真实前端路径执行手动重排。
- [x] 采集并核验产品编号颜色、最近一次成功排产时间、甘特图工单范围。
- [x] 输出初始 verification-report.md。
- [x] 以严格 TDD 修复产品编号橙色状态。
- [x] 复测四项目标并更新验证报告。
- [ ] 完成二次收尾。

## Expected Verification

- 使用 Playwright 或真实浏览器自动化操作前端页面。
- 按用户授权使用本机 `芋道源码/admin` 身份标签验证；密码仅从前端 `.env` 读取，不写入任务文档或报告。
- 保留页面截图、命令输出、时间戳、关键 DOM/网络证据。
- 不使用 mock、不使用 API-only 替代真实前端路径；API 仅允许作为最终辅助核验。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；本任务是验证任务，按真实用户路径核验，不以替代路径冒充结果。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 任务创建时 `docs/experience-index.md` 不存在；用户已明确授权“在芋道源码里验证”，据此记录风险后继续。
- 收尾时已发现并读取 `docs/experience-index.md`；命中 `docs/e2e-rules.md` 和 `docs/login-access.md`。
- 适用门禁摘要：真实写入验证必须走 Playwright 前端路径，API 仅作辅助核验；使用本机入口和授权身份；失败必须保留页面、网络和截图证据；不得使用 SQL、API-only 或 mock 替代。
- 新增经验摘要：Element Plus `el-table` 勾选行复选框必须限定可见 body row，排除表头/thead，写入前断言已选业务唯一键集合与目标集合完全一致。

## Cleanup Candidates

- `doc/tasks/verify-manual-reschedule-881mo-20260724/manual-reschedule-verify.e2e.cjs`
- `output/playwright/verify-manual-reschedule-881mo-20260724/`

## Final Verification

- 初次真实验证：a/c/d 通过，b 失败。
- 2026-07-24 用户授权继续修复 b；本任务重新进入执行状态。
- 修复后真实验证：2026-07-24 17:32 使用 `manual-reschedule-repair-verify.e2e.cjs` 通过真实前端路径再次执行手动重排，a/b/c/d 全部通过。
- 关键证据：`output/playwright/verify-manual-reschedule-881mo-20260724-repair/repair-verification-report.json`、`after-replan-product-code.png`、`after-replan-gantt.png`。
- 当前状态为 `ready_for_closeout`：功能修复和必需验证已完成，二次收尾、提交与推送仍需处理。

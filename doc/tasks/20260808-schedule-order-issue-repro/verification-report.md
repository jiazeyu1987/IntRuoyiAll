# 排产工单交互问题复现验证报告

## Verification Scope

- 验证目标：确认用户列出的 6 类排产工单交互问题是否可在本机真实页面复现。
- 验证入口：前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`。
- 验证身份：租户 `芋道源码`，账号 `admin`；未记录密码或 token。
- 验证方式：Playwright 操作真实前端页面，采集 DOM、分页状态、目标接口请求参数、HTTP 状态、业务码、错误和写请求计数。
- 只读边界：未执行入池、重排发布、强制完成、调整交期提交、冻结、撤销或其它 MES 写操作；最终 `mesWriteRequests` 为 0。

## Summary

| 问题 | 结论 | 关键证据 |
| --- | --- | --- |
| 组合筛选删除一个条件会清空全部条件，并保留旧查询结果 | REPRODUCED | 删除前 2 个条件标签，删除后标签为空；删除后未发出新的排产工单 page 请求；旧结果行仍保留。 |
| 跳页输入框不生效，且与实际页码长期不同步 | NOT_REPRODUCED | 输入目标页 2 后发出 `pageNo=2&pageSize=20`；当前页码和 jumper 输入值均为 2。 |
| 表格固定列遮挡导致“当前工序”点击错位 | NOT_REPRODUCED | hit test 命中 `BUTTON`，文本为 `吹球囊成型`；点击后进入工艺路线编辑页。 |
| 详情窗口“报工对比”按钮无响应 | NOT_REPRODUCED | 详情窗口内 `报工计划对比` 可见；点击后发出 `/admin-api/mes/pro/schedule-order/daily-compare?scheduleOrderId=131`，HTTP 200，业务码 0。 |
| 反向承诺交期被静默清空并恢复全量数据 | REPRODUCED | 日期输入值变为空；查询请求仅含 `pageNo=1&pageSize=20`；结果 total 恢复为初始 52；无可见提示。 |
| 优先级、排序状态和详情图标存在校验或可访问性缺陷 | REPRODUCED | 优先级表头 class 变为 `ascending`，但 `aria-sort` 仍为空；未发出排序参数请求；优先级弹窗 input 无 `aria-label`。 |

## Target Request Evidence

- 初始排产工单列表：`GET /admin-api/mes/pro/schedule-order/page?pageNo=1&pageSize=20`，HTTP 200，业务码 0，total 52。
- 组合筛选后：`GET /admin-api/mes/pro/schedule-order/page?pageNo=1&pageSize=20&code=SCH-SMART-SCHED-20260630-RERUN8-MO-20260711-0001&erpWorkOrderCode=SMART-SCHED-20260630-RERUN8-MO`，HTTP 200，业务码 0，total 1。
- 组合筛选删除条件后：目标 page 请求新增数为 0，页面旧结果未刷新。
- 跳页验证：`GET /admin-api/mes/pro/schedule-order/page?pageNo=2&pageSize=20`，HTTP 200，业务码 0。
- 报工对比验证：`GET /admin-api/mes/pro/schedule-order/daily-compare?scheduleOrderId=131`，HTTP 200，业务码 0。
- 反向承诺交期验证：最新 page 请求仅包含 `pageNo=1&pageSize=20`，未携带承诺交期筛选参数，total 回到 52。

## Error And Write Safety

- `pageErrors`：0。
- `consoleErrors`：0。
- `mesWriteRequests`：0。
- `requestFailures`：存在导航中断造成的非目标请求失败，主要为系统列配置、通知、DCC 待办和外部统计资源；不属于当前排产工单目标链路失败。

## Artifacts

- 结果 JSON：`output/playwright/20260808-schedule-order-issue-repro/result.json`。
- 最终截图：`output/playwright/20260808-schedule-order-issue-repro/schedule-order-final.png`。
- 复现脚本：`doc/tasks/20260808-schedule-order-issue-repro/reproduce-schedule-order-issues.cjs`。

## Final Result

本次真实页面复现共确认 3 项可复现、3 项未复现。可复现项建议进入后续修复任务；未复现项保留当前证据，若用户有特定浏览器尺寸、数据样本、账号权限或操作路径，可再按该条件补充复现。

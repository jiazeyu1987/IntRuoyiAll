# DCC 电子分发确认倒计时

## Goal

为 DCC 电子分发确认弹框增加 10 秒确认按钮倒计时；确认动作继续要求本人电子签名，并确保后端分发确认日志与电子签名记录可追溯。

## Milestones

- 梳理现有电子分发确认、日志和签名链路。
- 先写 Given/When/Then 与最小 RED 验证。
- 实现前端 10 秒倒计时和后端日志可追溯断言。
- 执行定向验证并记录结果。

## Expected Verification

- 后端定向测试：`DccDistributionReceiptServiceImplTest` 覆盖确认后签名调用、recipient 时间和分发日志来源字段。
- 前端静态合同：确认弹框打开时倒计时从 10 秒开始，倒计时结束前确认按钮禁用，按钮文案显示剩余秒数。
- 不执行 E2E，除非用户当轮明确要求。

## Current Status

completed

Implementation, static checks, front-end type check, and the requested real front-end E2E validation are complete. Backend targeted verification remains blocked by an unrelated existing test compilation error in `DccControlledFileWorkflowServiceImplTest.java`; the production signing/logging path was not bypassed.

## BDD

- Given 当前登录用户是电子分发收件人，When 从个人工作台点击“文控分发”待办或在文件详情打开分发确认弹框，Then 签收弹框展示文件信息，确认按钮显示 10 秒倒计时且倒计时期间不可点击。
- Given 10 秒倒计时结束且用户输入本人签名密码，When 点击确认，Then 系统调用电子分发确认接口并刷新详情。
- Given 后端收到电子分发确认请求，When 身份、文件状态和签名密码校验通过，Then 写入 recipient 的 `readAt/acknowledgedAt/ackComment`，写入分发记录完成字段，并调用电子签名服务生成 `DISTRIBUTION_ACK` 签名记录。
- Given 文件日志查询分发历史，When 电子分发已确认，Then 日志能读取到分发记录的确认人和确认时间。

## Design Constraints Check

- 不把 10 秒按钮倒计时当作严格阅读时长或后端合规门禁。
- 不新增 fallback、模拟成功或吞异常；签名失败继续按正式错误展示。
- 不修改分发矩阵对象来源。
- E2E 仅在用户明确要求后执行，且只通过真实前端页面完成验收动作。
- 不执行数据库写入、服务重启或远程操作。
- 只处理当前任务相关文件，不回滚并行改动。

## Closeout

- `ready_for_closeout`: 2026-09-22 after static checks, front-end type check, E2E PASS, and diff whitespace check.
- `completed`: 2026-09-22 after task documentation was updated with the final verification state.

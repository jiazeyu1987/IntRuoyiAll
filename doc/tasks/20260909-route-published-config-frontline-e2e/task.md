# 发布后配置一线生产显示真实 E2E

## Goal
补一条真实页面 E2E，专门验证“工艺路线发布后的配置能在一线生产页面正确显示”。

## Milestones
- [x] 建立任务记录和 BDD 场景。
- [x] 新增真实页面 E2E 脚本，覆盖发布后配置在一线生产页面的设备、参数、控件类型和范围显示。
- [x] 增加 package.json 脚本入口并完成语法检查。
- [x] 在可用本地前后端运行态执行真实页面验证并记录结果。

## Expected Verification
- `node --check IntRuoyiFronted/tests/e2e/frontline-published-route-config-display-real.e2e.cjs`
- `pnpm e2e:frontline-published-route-config-display:real`

## Design Constraints Check
- E2E 只通过 Playwright 操作真实前端页面。
- API 响应监听仅用于证明页面自然请求状态，不作为用户动作替代。
- 登录凭据只能通过运行时环境或既有本机 E2E 约定提供，不写入文档。
- 缺少发布后活跃订单、工序、设备或参数时，脚本必须 fail fast 并输出明确 blocker。

## Current Status
ready_for_closeout：真实页面 E2E 已通过，等待任务收尾清理和提交推送。

## Cleanup Keep
- doc/tasks/20260909-route-published-config-frontline-e2e/task.md
- doc/tasks/20260909-route-published-config-frontline-e2e/execution-log.md
- doc/tasks/20260909-route-published-config-frontline-e2e/verification-report.md
- doc/tasks/20260909-route-published-config-frontline-e2e/e2e-artifacts/frontline-published-route-config-display-result.json
- doc/tasks/20260909-route-published-config-frontline-e2e/e2e-artifacts/frontline-published-route-config-display.png

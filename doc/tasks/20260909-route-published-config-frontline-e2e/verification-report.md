# Verification Report

## Summary
- 新增真实页面 E2E 脚本和 package 入口已完成。
- 语法、diff whitespace 和真实页面 E2E 检查通过。
- 真实页面验证证明：工艺路线发布后的 FROZEN 参数快照可以在一线生产页面正确显示，设备默认选中，参数范围、输入控件和下拉选项可见。

## Results
- `node --check IntRuoyiFronted/tests/e2e/frontline-published-route-config-display-real.e2e.cjs` -> PASS。
- `node -e "JSON.parse(require('fs').readFileSync('IntRuoyiFronted/package.json','utf8')); console.log('package-json-ok')"` -> PASS。
- `git diff --check -- IntRuoyiFronted/tests/e2e/frontline-published-route-config-display-real.e2e.cjs IntRuoyiFronted/package.json doc/tasks/20260909-route-published-config-frontline-e2e` -> PASS。
- `pnpm e2e:frontline-published-route-config-display:real` -> PASS。

## Runtime Evidence
- Backend `http://127.0.0.1:48081/actuator/health` -> `UP`。
- Frontend `http://127.0.0.1:8081/` -> HTTP 200。
- E2E identity label: `芋道源码/admin`。
- Active order: `KDMO-309748-1416202028`，`activeOrderId=150`，`routeId=922119`，`routeProcessId=9908090160`，`processId=922985`。
- Runtime parameter snapshot state: `FROZEN`。
- Device evidence: `deviceCount=1`，默认/目标设备 `B09393`，参数数量 `5`。
- Natural page requests: active orders、active-order processes、runtime-config 均为 HTTP 200。
- MES write requests: `0`，本次为只读显示验收。
- Console/page errors: `0`。
- Evidence JSON: `doc/tasks/20260909-route-published-config-frontline-e2e/e2e-artifacts/frontline-published-route-config-display-result.json`。
- Screenshot: `doc/tasks/20260909-route-published-config-frontline-e2e/e2e-artifacts/frontline-published-route-config-display.png`。

## Status
- 当前验证结论：PASS，任务证据已提交并推送到 `origin/int_main`，提交 `e9bd91bff`。

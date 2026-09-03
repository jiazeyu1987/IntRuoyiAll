# Feature
生产组长工作台的修改弹框和分配弹框按当前报工正式上下文展示多物料、设备和设备参数。

## Acceptance
- 修改弹框展示逐物料名称、完成数量、损耗数量，并允许按物料调整。
- 修改弹框展示当前报工设备和设备参数，设备参数区域具备真实 E2E 可断言锚点。
- 分配弹框展示当前报工的物料、设备、设备参数上下文。
- 真实前端 E2E 必须通过 Playwright 登录真实页面、点击真实按钮并截图验证。

## BDD
BDD: 修改弹框展示多物料设备参数 -> Given 生产组长列表中的生产报工包含 `materialDetails`、设备快照和设备参数 When 点击“修改” Then 弹框展示物料明细、设备、设备参数，并保留设备参数身份。

BDD: 分配弹框展示多物料上下文 -> Given 生产组长对多物料报工进行分配 When 打开“分配” Then 弹框展示物料、设备和设备参数上下文。

## RED
RED: `node IntRuoyiFronted\tests\e2e\team-leader-multi-material-device-dialogs-static.spec.cjs` -> FAIL，前端类型和页面缺少多物料弹框合同。

## GREEN
GREEN: `node IntRuoyiFronted\tests\e2e\team-leader-multi-material-device-dialogs-static.spec.cjs` -> PASS。

GREEN: `node IntRuoyiFronted\tests\e2e\route-process-input-output-materials-static.spec.cjs` -> PASS。

GREEN: `git diff --check` -> PASS，仅 CRLF 工作区提示。

## Verification
- 已创建真实前端 Playwright 脚本：`doc\tasks\20260903-team-leader-multi-material-device-dialogs\team-leader-multi-dialogs-real.e2e.cjs`。
- 已启动 worktree 前端 `http://127.0.0.1:8092`，后端代理 `48092`。
- 真实 E2E 脚本会点击生产组长页面真实“修改”和“分配”按钮，断言并截图 `data-production-report-correction-materials`、`data-production-report-correction-devices`、`data-production-report-correction-parameters`、`data-team-leader-allocation-material-context`、`data-team-leader-allocation-devices`、`data-team-leader-allocation-parameters`。

## Blockers
- `pnpm exec vue-tsc --noEmit --pretty false` 加大 Node heap 后仍失败于既有无关页面类型错误，本任务页面未在错误列表中。
- 真实 E2E 当前缺少 `TLW_USERNAME` / `TLW_PASSWORD`。按登录规则，用户名和密码不得写入 `.env` 或源码，必须通过本轮临时环境变量注入。

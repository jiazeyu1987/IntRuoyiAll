# 任务：运行控制台发布请求路由回归

## 任务目标

- 解释 `发布测试服` 点击确认后提示 `No static resource admin-api/infra/runtime-control/actions.` 的原因。
- 补充 E2E 回归测试，覆盖填写原因后的真实提交路径，但通过 Playwright route 中止请求，避免执行真实发布。
- 验证前端实际发出的 `发布测试服` 请求必须指向测试服后端 `48081/admin-api/infra/runtime-control/actions`，并携带 `publishScope=code-only`。

## BDD 场景

- BDD: 发布测试服确认提交必须命中后端动作接口 -> Given `芋道源码/admin` 打开运行控制台, When 点击 `发布测试服`、填写原因并确认执行, Then 浏览器发出的 POST 目标必须是测试服后端 `/admin-api/infra/runtime-control/actions`，请求体包含 `action=publish-test` 和 `publishScope=code-only`，测试中止该请求且不执行真实发布。

## 预期验证

- RED：新增回归文件前，专门覆盖提交请求路由的 E2E 命令失败。
- GREEN：新增回归 E2E 后，命令能捕获并中止请求，断言 URL 和 payload 正确。
- REGRESSION：原五个按钮 E2E 和静态契约仍通过。

## 当前状态

- 状态：completed
- 已完成：
  - 已复现测试覆盖缺口：上一轮 5 个 E2E 均停在前端校验层，没有填写原因后触发动作 POST。
  - 已安全探测当前测试服：直连 `http://172.30.30.58:48081/admin-api/infra/runtime-control/actions` 未登录返回 `401`，说明当前后端动作接口存在。
  - 已新增 `tests/e2e/runtime-control-publish-test-submit-route.e2e.js`，填写原因后捕获并中止发布请求，断言 URL 和 payload 正确。
  - 已回归原五个按钮 E2E 与静态契约测试。
- 阻塞与影响：
  - 暂无。

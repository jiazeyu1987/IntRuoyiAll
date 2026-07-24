# 任务：公司工作台手动发布展厅 E2E

## 任务目标

- 为后台公司工作台 `手动发布展厅` 入口补一条真实用户路径 E2E。
- 真实打开 `/showroom/company`，点击并确认“手动发布展厅”。
- 拦截发布请求并断言请求体显式包含规范 `siteKey` 与 `stage`，证明链路不依赖隐式 scope。

## BDD 场景

- BDD: 公司工作台手动发布展厅请求显式携带发布 scope -> Given 测试租户用户进入 `/showroom/company` 且能看到“手动发布展厅” / When 用户点击按钮并在确认框中确认发布 / Then 前端必须发出 `/admin-api/showroom/release/publish` 请求，且请求体显式包含 `siteKey=yingtai-showroom` 与 `stage=TEST`。
- BDD: 手动发布展厅链路不得依赖缺省 scope -> Given 当前页面已加载公司工作台真实数据 / When 用户走完整个手动发布点击链路 / Then 测试必须仅以拦截到的真实请求体作为成功依据，若缺少 `siteKey` 或 `stage` 则直接失败，不接受静默补齐或默认值推断。

## 里程碑

- [x] M1：确认现有公司工作台发布链路与 E2E 风格，补齐任务文档。
- [x] M2：执行 RED，记录当前缺少目标 E2E 覆盖的失败证据。
- [x] M3：新增公司工作台手动发布真实路径 E2E。
- [x] M4：运行目标 E2E 并记录 GREEN 证据。
- [x] M5：回填执行日志与任务状态。

## 预期验证

- `node tests\e2e\showroom-company-release-publish.e2e.js`

## 当前状态

completed

## Current Status

completed

## 当前进展

- 已创建任务目录与任务文档。
- 已完成 `showroom-company-release-publish.e2e.js`，覆盖真实登录、真实 `/showroom/company` 路由、点击确认发布、拦截 `/admin-api/showroom/release/publish` 并校验显式 `siteKey/stage`。
- 已根据 reviewer 反馈修复真实页面时序问题：不再强依赖单次 `/showroom/company/current` 响应捕获，而是按真实工作台可见状态判定页面 ready。
- 已完成真实 E2E 运行与脚本语法校验。

## 验证结果

- RED: `node tests\e2e\showroom-company-release-publish.e2e.js` -> FAIL，目标 E2E 文件尚不存在，当前切片缺少公司工作台手动发布真实路径覆盖。
- GREEN: `node --check tests\e2e\showroom-company-release-publish.e2e.js` -> PASS。
- RED: reviewer rerun `node tests\e2e\showroom-company-release-publish.e2e.js` -> FAIL，旧脚本对 `/admin-api/showroom/company/current` 的等待窗口过于严格，真实页面已加载完成时会误判失败。
- GREEN: `node tests\e2e\showroom-company-release-publish.e2e.js` -> PASS，真实路由 `/showroom/company` 点击“手动发布展厅”后成功拦截 `/admin-api/showroom/release/publish`，请求体显式包含 `siteKey=yingtai-showroom` 与 `stage=TEST`。
- GREEN: `node --check tests\e2e\showroom-company-release-publish.e2e.js` -> PASS。

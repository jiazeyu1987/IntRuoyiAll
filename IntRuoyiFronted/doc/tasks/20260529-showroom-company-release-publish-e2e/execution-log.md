# 执行日志：公司工作台手动发布展厅 E2E

BDD: 公司工作台手动发布展厅请求显式携带发布 scope -> Given 测试租户用户进入 `/showroom/company` 且能看到“手动发布展厅” / When 用户点击按钮并在确认框中确认发布 / Then 前端必须发出 `/admin-api/showroom/release/publish` 请求，且请求体显式包含 `siteKey=yingtai-showroom` 与 `stage=TEST`。

BDD: 手动发布展厅链路不得依赖缺省 scope -> Given 当前页面已加载公司工作台真实数据 / When 用户走完整个手动发布点击链路 / Then 测试必须仅以拦截到的真实请求体作为成功依据，若缺少 `siteKey` 或 `stage` 则直接失败，不接受静默补齐或默认值推断。

## 记录

- 2026-05-29 M1：已确认目标页面为 `/showroom/company`，发布按钮位于 `CompanyWorkbench.vue`，请求 payload 由 `buildReleasePublishPayload()` 显式构造。
- 2026-05-29 RED: `node tests\e2e\showroom-company-release-publish.e2e.js` -> FAIL，当前仓库缺少该 E2E 文件，尚未覆盖公司工作台“手动发布展厅”真实点击链路。
- 2026-05-29 M3：已新增 `tests\e2e\showroom-company-release-publish.e2e.js`，脚本使用 Node + Playwright 风格真实登录 `测试租户 / aoteman / admin123`，打开 `/showroom/company`，点击并确认“手动发布展厅”，拦截 `/admin-api/showroom/release/publish` 后断言请求体显式包含 `siteKey` 与 `stage`。
- 2026-05-29 GREEN: `node --check tests\e2e\showroom-company-release-publish.e2e.js` -> PASS。
- 2026-05-29 BLOCKED: `node tests\e2e\showroom-company-release-publish.e2e.js` -> FAIL-FAST，当前 worktree 缺少 `node_modules`，Node 无法解析 `playwright`；缺失前置会阻塞真实前端 GREEN 证据，不可用 mock 或静默跳过替代。
- 2026-05-29 RED: reviewer rerun `node tests\e2e\showroom-company-release-publish.e2e.js` -> FAIL，旧脚本把 `GET /admin-api/showroom/company/current` 当作必须在当前等待窗口内捕获到的单次响应，真实页面已完成加载时会因时序差异触发 `company workbench must load real current-company data before publish`。
- 2026-05-29 M4：已将工作台就绪判定改为页面真实可见状态（`公司信息` 标题、`编辑公司`、`手动发布展厅`）并保留对 `/admin-api/showroom/company/current` 的被动成功追踪；同时移除对前后端同源 URL 与成功 toast 文案的脆弱假设，仅断言真实发布 endpoint path 与显式 `siteKey/stage`。
- 2026-05-29 GREEN: `node tests\e2e\showroom-company-release-publish.e2e.js` -> PASS，真实输出 `PASS: showroom company manual publish emits explicit scope yingtai-showroom/TEST; companyCurrentSeen=true; companyCurrentOk=true`。
- 2026-05-29 GREEN: `node --check tests\e2e\showroom-company-release-publish.e2e.js` -> PASS。

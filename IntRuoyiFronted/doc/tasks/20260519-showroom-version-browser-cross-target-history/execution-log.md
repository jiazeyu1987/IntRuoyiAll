# Execution Log

## Previous Task Check

- Checked previous frontend task: `doc/tasks/20260519-showroom-owner-company-mapping-log-fix/task.md`
- Status at start: blocked due user priority switch
- Impact: owner-company mapping log fix remains out of scope for this task

## BDD

- BDD: 版本页跨目标浏览公司与产品历史 -> Given 公司历史与产品历史接口都已存在 When 用户进入 `/showroom/history` 并切换目标类型 Then 页面必须在同一工作台内承接公司历史与产品历史，而不是只固定展示公司历史。
- BDD: 版本页不伪造缺失的讲解与预览资产历史 -> Given 后端没有讲解历史列表与预览资产历史列表接口 When 用户切换到讲解或预览资产 Then 页面必须明确显示契约缺口与影响，不得伪造版本列表。
- BDD: 产品历史不再只藏在详情内抽屉 -> Given 产品已有 `GET /showroom/product/history` 真实接口 When 用户从版本页选择某个产品 Then 页面必须直接展示该产品的 revision 与字段 diff 浏览能力。

## RED

- RED: `node --test scripts/showroom-admin-version-browser-history.test.mjs` -> FAIL, 当前 `/showroom/history` 仍只承接公司历史，既没有跨目标浏览控件，也没有讲解/预览资产契约缺口提示。

## GREEN

- GREEN: `node --test scripts/showroom-admin-version-browser-history.test.mjs` -> PASS
- GREEN: `pnpm exec eslint src/views/showroom-admin/history scripts/showroom-admin-version-browser-history.test.mjs` -> PASS

## Notes

- 版本页真实可用的后端历史链路当前仅有 `company/history` 与 `product/history`。
- 讲解与预览资产在本轮只读取最新快照或 live 预览图，不伪造任何历史版本列表。
- 额外执行 `scripts/showroom-admin-company-dashboard-history.test.mjs` 时发现共享脏工作区中的 `company/**` 断言仍有未收口变更，未作为本任务放行门禁。

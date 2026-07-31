# Frontend Feature Evidence

## Feature

- 让测试管理页面在项目筛选、项目列、创建/编辑表单和项目标签解析中支持 `工艺路线`。

## Acceptance

- 项目下拉、项目快速筛选和项目列均使用标准 `CODEX_TEST_PROJECT_OPTIONS`，包含 `工艺路线`。
- 新增/编辑测试项时必须显式选择 `工艺路线`，保存后列表和详情接口都回读该项目值。
- 真实页面新增/更新 4 条工艺路线测试项，每条包含 4 个测试目标项。

## Non-goals

- 不改工艺路线业务页面本身。
- 不执行测试项 Runner，不新增 mock 数据或 API-only 写入入口。

## UI Entry Points

- `系统管理 > 测试管理`。
- 测试项新增/编辑弹窗的“项目”选择框。
- 测试项列表项目列和项目快速筛选。

## API Contract

- 前端 `CodexTestProject` 与后端合法项目集合一致，新增 `工艺路线`。
- 保存接口继续使用 `/system/codex-test-case/create` 和 `/system/codex-test-case/update`。

## BDD

- BDD: 项目下拉显示工艺路线 -> Given 打开测试项弹窗，When 展开项目下拉，Then 可见文本 `工艺路线` 并可选择。
- BDD: 工艺路线列表标签稳定 -> Given API 返回 `project=工艺路线` 的测试项，When 页面渲染列表，Then 项目列显示 `工艺路线` 且不回落成其它分类。

## RED / GREEN

- RED: `node IntRuoyiFronted\tests\e2e\system-codex-test-management-static.spec.js` -> FAIL，前端 `CodexTestProject` 和项目选项未包含 `工艺路线`。
- GREEN: `node IntRuoyiFronted\tests\e2e\system-codex-test-management-static.spec.js` -> PASS。
- GREEN: `node doc\tasks\20260726-codex-test-process-route-case\ensure-process-route-codex-test-items.e2e.cjs` -> PASS，真实页面新增并回读 4 条 `工艺路线` 测试项。

## Verification

- `Invoke-RestMethod http://127.0.0.1:48082/actuator/health` -> `UP`。
- `Invoke-WebRequest http://127.0.0.1:8082/` -> HTTP 200。
- `node --check doc\tasks\20260726-codex-test-process-route-case\ensure-process-route-codex-test-items.e2e.cjs` -> PASS。
- `pnpm ts:check` -> PASS。
- E2E 摘要：`artifacts/process-route-codex-test-items-summary.json`，IDs `18`、`19`、`20`、`21`。

## Blockers

- 无。

# QA Test Suite Evidence：芋道源码 admin 展柜编辑 E2E

## Scope And Release Or Feature Under Test

- Scope: `展柜管理 -> 编辑展柜` 双语字段展示与保存前 payload 构造验证。
- Feature under test: `20260525-showroom-hall-bilingual-save` 修复后的前端行为。
- Account: `芋道源码 / admin / admin123`。
- Environment: 本地真实前端 `http://127.0.0.1:8081` 与本地真实后端 `http://127.0.0.1:48081`。
- Data policy: 本次账号属于 `芋道源码` 租户，按项目基线只做只读 E2E，不发送真实更新请求。

## Requirement-To-Test Matrix

| Requirement | Test Path | Result |
| --- | --- | --- |
| admin 可登录并进入展柜管理 | 前端登录页选择 `芋道源码`，使用 `admin/admin123` 登录，打开 `/showroom/hall` | PASS |
| 展柜列表加载真实数据 | 页面自身请求 `/admin-api/showroom/hall/page`，返回 8 条展柜 | PASS |
| 编辑弹窗展示双语字段 | 点击首个展柜“编辑”，检查标签 `展柜编码/展柜名称/英文名称/描述/英文描述` | PASS |
| 英文名称非空 | 读取弹窗 `英文名称` 输入框 | PASS，`Cardiac Intervention Implant Hall` |
| 保存前 payload 含必填英文名 | 用弹窗当前值构造 dry-run payload，检查 `nameEn` | PASS |
| 不修改芋道源码租户数据 | 全程监听 `PUT /showroom/hall/update` | PASS，update 请求数 `0` |

## Test Types Used And Not Applicable Reasons

- E2E: 使用 Playwright 真实浏览器操作登录页、菜单页、编辑弹窗。
- Smoke: 覆盖登录、路由可达、列表加载、弹窗打开。
- Regression: 覆盖此前 `hall name_en is required` 的前端缺字段风险。
- Accessibility: 验证 `el-form-item` 标签文本可见，未做完整无障碍审计。
- Performance: 本次不涉及性能验收。

## Test Data And Fixtures

- Tenant: `芋道源码`。
- User: `admin`。
- Hall row observed from real frontend-loaded data:
  - `hallId`: `1`
  - `name`: `心内介植入展厅`
  - `nameEn`: `Cardiac Intervention Implant Hall`
  - `description`: empty string
  - `descriptionEn`: empty string
- No mock data and no API shortcut were used for the E2E path.

## RED Evidence For Newly Added Tests

- RED: Not applicable. This task adds no persistent automated regression test file; it executes a one-off QA E2E validation requested by the user after the fix was already committed.
- Prior RED for the underlying defect is recorded in `doc/tasks/20260525-showroom-hall-bilingual-save/bug-regression-evidence.md`.

## GREEN Evidence For Passing Verification

- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-yudao-admin-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260525-showroom-hall-yudao-admin-e2e\scripts\verify-showroom-hall-yudao-admin-readonly.mjs` -> PASS。
- Result:
  - `account`: `芋道源码/admin`
  - `url`: `http://127.0.0.1:8081/showroom/hall`
  - `hallCount`: `8`
  - `labels`: `展柜编码`, `展柜名称`, `英文名称`, `描述`, `英文描述`
  - `dryRunPayload`: `{"hallId":1,"name":"心内介植入展厅","nameEn":"Cardiac Intervention Implant Hall","description":"","descriptionEn":""}`
  - `updateRequestCount`: `0`

## Failed, Skipped, Flaky, Or Blocked Tests

- Failed: none.
- Skipped: real `PUT /showroom/hall/update` submit was intentionally not executed because project baseline forbids modifying `芋道源码` tenant data during tests.
- Flaky: none observed.
- Blocked: none.

## Blockers

- None.

## CI Impact And Release Recommendation

- CI impact: none; this was a local one-off Playwright E2E validation and did not add persistent CI tests.
- Release recommendation: PASS for the requested `芋道源码/admin` read-only E2E check. Full write-path E2E should continue to use the allowed test tenant.

## Verification

- PASS: `芋道源码/admin` real frontend login.
- PASS: `/showroom/hall` route loaded 8 real halls.
- PASS: edit dialog displayed required bilingual fields.
- PASS: dry-run payload included `nameEn` and `descriptionEn`.
- PASS: no hall update request was sent.

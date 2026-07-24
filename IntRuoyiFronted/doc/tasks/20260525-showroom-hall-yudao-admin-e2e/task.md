# 任务：芋道源码 admin 展柜编辑 E2E 验证

## 任务目标

- 使用 `芋道源码 / admin / admin123` 账号，通过真实前端路径验证展柜编辑弹窗已支持英文名称与英文描述。
- 验证前端保存前构造的展柜更新 payload 包含后端必填字段 `nameEn`，并保留 `descriptionEn`。
- 严格遵守“测试不得修改芋道源码租户数据”约束，本次不点击真实保存接口，不写入芋道源码租户数据。

## 非目标

- 不修改生产代码。
- 不修改芋道源码租户数据。
- 不使用 API 代替前端路径登录或页面操作。
- 不引入 mock、fallback 或静默成功路径。

## 前置任务检查

- 上一个前端任务：`20260525-showroom-hall-bilingual-save`。
- 上一任务状态：`completed`。
- 影响：上一任务已完成，不阻塞本次 E2E 验证。

## 里程碑

- [x] M1：建立任务记录并确认上一同仓任务已完成。
- [x] M2：编写只读 E2E 验证脚本。
- [x] M3：使用 `芋道源码 / admin` 通过真实前端路径执行验证。
- [x] M4：记录 QA 证据、执行 closeout 预览并按策略提交任务记录。

## BDD 场景

- BDD: 芋道源码 admin 可进入展柜管理 -> Given 用户使用 `芋道源码 / admin / admin123` 登录后台, When 打开 `/showroom/hall`, Then 页面应显示展柜管理列表与编辑入口。
- BDD: 编辑弹窗显示双语字段 -> Given admin 位于展柜管理页, When 点击任一展柜的“编辑”, Then 弹窗应显示 `展柜名称 / 英文名称 / 描述 / 英文描述`，并回填非空英文名称。
- BDD: 只读验证保存 payload -> Given 芋道源码租户数据禁止测试写入, When 在页面上下文调用同一份前端表单 payload 逻辑, Then 生成的 payload 必须包含 `hallId/name/nameEn/description/descriptionEn`，但不得发送真实更新请求。

## 预期验证

- `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-yudao-admin-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260525-showroom-hall-yudao-admin-e2e\scripts\verify-showroom-hall-yudao-admin-readonly.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence doc/tasks/20260525-showroom-hall-yudao-admin-e2e/qa-test-suite-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260525-showroom-hall-yudao-admin-e2e --mode preview`

## Current Status

completed

## 当前状态

- 状态：completed
- 已完成：
  - 已确认上一前端任务完成。
  - 已建立本任务记录。
  - 已完成 `芋道源码 / admin` 真实前端只读 E2E。
  - 已通过 QA evidence 校验。
- 阻塞与影响：
  - 暂无阻塞。

## Final Verification Result

- PASS: `芋道源码 / admin / admin123` 真实前端登录。
- PASS: `/showroom/hall` 展柜管理页加载 8 条真实展柜。
- PASS: 编辑展柜弹窗展示 `展柜编码/展柜名称/英文名称/描述/英文描述`。
- PASS: dry-run payload 包含 `hallId/name/nameEn/description/descriptionEn`。
- PASS: `PUT /showroom/hall/update` 请求数为 `0`，未修改芋道源码租户数据。
- PASS: QA evidence 校验。

## Cleanup Keep

- `doc/tasks/20260525-showroom-hall-yudao-admin-e2e/task.md`
- `doc/tasks/20260525-showroom-hall-yudao-admin-e2e/execution-log.md`
- `doc/tasks/20260525-showroom-hall-yudao-admin-e2e/qa-test-suite-evidence.md`

## Cleanup Candidates

- `doc/tasks/20260525-showroom-hall-yudao-admin-e2e/scripts/verify-showroom-hall-yudao-admin-readonly.mjs`
- `doc/tasks/20260525-showroom-hall-yudao-admin-e2e/showroom-hall-yudao-admin-readonly.png`

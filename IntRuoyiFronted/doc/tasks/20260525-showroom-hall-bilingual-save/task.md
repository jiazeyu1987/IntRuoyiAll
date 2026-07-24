# 任务：修复展柜编辑保存缺少英文名

## 任务目标

- 修复 `展柜管理 -> 编辑展柜` 保存时报 `SHOWROOM_REQUIRED_FIELD_MISSING: hall name_en is required` 的问题。
- 让展柜编辑和新增表单显式维护后端必填的英文名称 `nameEn`。
- 保存展柜时同时携带英文描述 `descriptionEn`，避免编辑中文字段时清空已有英文描述。

## 非目标

- 不修改后端必填契约。
- 不用中文名称自动生成或代替英文名称。
- 不修改展柜产品映射、排序、发布或审批逻辑。
- 不引入 fallback、mock 或静默成功路径。

## 前置任务检查

- 上一个前端任务：`20260525-login-default-yudao-restore`。
- 上一任务状态：`completed`。
- 影响：上一任务已完成，不阻塞本次展柜编辑修复。

## 里程碑

- [x] M1：建立任务记录并确认上一同仓任务已完成。
- [x] M2：补充展柜双语保存 RED 回归测试。
- [x] M3：按后端契约补齐展柜表单、列表行归一化和保存 payload。
- [x] M4：执行 GREEN、类型检查与真实前端路径验证。
- [x] M5：更新证据、运行 closeout 预览并提交本任务变更。

## BDD 场景

- BDD: 编辑展柜必须提交英文名称 -> Given 后端 `PUT /showroom/hall/update` 要求 `nameEn`, When 用户在展柜管理编辑已有展柜并保存, Then 前端表单必须展示英文名称字段并把 `nameEn` 随请求提交。
- BDD: 编辑展柜不应清空英文描述 -> Given 后端展柜记录已有 `descriptionEn`, When 用户只修改中文名称或中文描述并保存, Then 前端仍随请求提交当前 `descriptionEn`，不得把已有英文描述隐式改成空值。
- BDD: 新增展柜必须填写英文名称 -> Given 后端 `POST /showroom/hall/create` 要求 `nameEn`, When 用户新增展柜, Then 前端缺少英文名称时应在提交前失败并提示必填项。

## 预期验证

- `D:\Programs\node.exe --test scripts\showroom-admin-product-hall-operability.test.mjs`
- `D:\Programs\node.exe --test scripts\showroom-admin-hall-list.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- 真实前端路径 `http://localhost:8081/showroom/hall`：编辑展柜弹窗可见“英文名称”和“英文描述”，保存请求包含 `nameEn`。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260525-showroom-hall-bilingual-save/frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260525-showroom-hall-bilingual-save --mode preview`

## Current Status

completed

## 当前状态

- 状态：completed
- 已完成：
  - 已确认上一前端任务完成。
  - 已建立本任务记录。
  - 已补充 RED 回归测试，当前失败点为展柜编辑缺少 `nameEn/descriptionEn`。
  - 已在展柜活动页面、列表行归一化、复用编辑弹窗和保存 payload 中补齐 `nameEn/descriptionEn`。
  - 已完成定向 GREEN、类型检查与真实前端路径验证。
  - 已通过前端证据与缺陷回归证据校验。
  - 已完成 task-closeout-cleanup 预览。
- 阻塞与影响：
  - 暂无阻塞。

## Final Verification Result

- PASS: `D:\Programs\node.exe --test scripts\showroom-admin-product-hall-operability.test.mjs`
- PASS: `D:\Programs\node.exe --test scripts\showroom-admin-hall-list.test.mjs`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- PASS: Playwright 真实前端路径 `/showroom/hall` 验证编辑展柜保存请求包含 `nameEn/descriptionEn`
- PASS: frontend feature evidence 校验
- PASS: bug regression evidence 校验
- PASS: task-closeout-cleanup preview

## Cleanup Keep

- `doc/tasks/20260525-showroom-hall-bilingual-save/task.md`
- `doc/tasks/20260525-showroom-hall-bilingual-save/execution-log.md`
- `doc/tasks/20260525-showroom-hall-bilingual-save/frontend-feature-evidence.md`
- `doc/tasks/20260525-showroom-hall-bilingual-save/bug-regression-evidence.md`

## Cleanup Candidates

- `doc/tasks/20260525-showroom-hall-bilingual-save/scripts/verify-showroom-hall-bilingual-save.mjs`
- `doc/tasks/20260525-showroom-hall-bilingual-save/showroom-hall-bilingual-save.png`

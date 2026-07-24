# 任务：修复公司核心制造能力与荣誉资质被过滤

## 任务目标

- 修复公司信息页把“核心制造能力 / 荣誉资质”从前端展示、编辑和英文翻译字段中排除的问题。
- 让中文字段和对应英文字段都能在公司编辑弹窗中编辑，并参与“Translate English Content”翻译请求。
- 保持现有后端字段契约，不新增 fallback、mock 或静默成功路径。

## 非目标

- 不修改后端接口契约。
- 不改公司语音生成、发布、版本中心或审批逻辑。
- 不调整展厅整体视觉风格。

## 前置任务检查

- 最近前端任务：`20260524-ebr-report-visual-fidelity`。
- 最近任务状态：已完成。
- 影响：上一任务不阻塞本次公司字段过滤修复。

## 里程碑

- [x] M1：建立任务记录并确认上一同仓任务已完成。
- [x] M2：补充公司字段可见、可编辑、可翻译 RED 回归测试。
- [x] M3：移除前端渲染和翻译路径中的公司字段过滤。
- [x] M4：执行 GREEN、类型检查与真实前端路径验证。
- [x] M5：更新证据、运行 closeout 预览并提交本任务变更。

## BDD 场景

- BDD: 公司编辑必须包含核心制造能力 -> Given 公司接口返回 `core_manufacturing_capability` 和 `core_manufacturing_capability_en`, When 用户打开公司信息编辑弹窗, Then 中文和 English tab 都能编辑该字段。
- BDD: 公司编辑必须包含荣誉资质 -> Given 公司接口返回 `honors_awards` 和 `honors_awards_en`, When 用户打开公司信息编辑弹窗, Then 中文和 English tab 都能编辑该字段。
- BDD: 公司英文翻译必须覆盖完整公司字段 -> Given 用户点击 English tab 的 `Translate English Content`, When 前端构造翻译请求, Then `fieldCodes` 和 `fields` 必须包含 `core_manufacturing_capability` 与 `honors_awards`。

## 预期验证

- `D:\Programs\node.exe --test scripts\showroom-admin-company-editable-fields.test.mjs`
- `D:\Programs\node.exe --test scripts\showroom-admin-frontend.test.mjs`
- `D:\Programs\node.exe --test scripts\showroom-admin-company-version-tab.test.mjs`
- `D:\Programs\node.exe --test scripts\showroom-admin-version-center.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm run ts:check`
- 真实前端路径 `http://localhost:8081/showroom/company`：编辑公司弹窗展示“核心制造能力 / 荣誉资质”，English tab 展示 `Core Manufacturing Capability / Honors and Awards`。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260525-showroom-company-editable-fields/frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260525-showroom-company-editable-fields/bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260525-showroom-company-editable-fields --mode preview`

## Current Status

completed

## 当前状态

- 状态：completed
- 已完成：
  - 已确认上一前端任务完成。
  - 已建立本任务记录。
  - 已补充 RED 回归测试，当前失败点为 `visibleCompanyFieldDefinitions` 仍过滤“核心制造能力 / 荣誉资质”。
  - 已删除公司字段可见列表过滤，展示、编辑、英文翻译请求统一使用完整 `companyFieldDefinitions`。
  - 已同步公司版本预览使用完整公司字段定义。
  - 已将既有公司双语入口回归断言收窄到编辑弹窗范围，避免与“手动发布展厅”企宣角色按钮冲突。
  - 已完成定向 GREEN、相关前端回归、公司版本回归、类型检查和真实前端路径验证。
  - 已通过前端功能证据和缺陷回归证据校验。
  - 已完成 task-closeout-cleanup 预览，无需删除文件。
- 阻塞与影响：
  - 暂无阻塞。

## Final Verification Result

- PASS: `D:\Programs\node.exe --test scripts\showroom-admin-company-editable-fields.test.mjs`
- PASS: `D:\Programs\node.exe --test scripts\showroom-admin-frontend.test.mjs`
- PASS: `D:\Programs\node.exe --test scripts\showroom-admin-company-version-tab.test.mjs`
- PASS: `D:\Programs\node.exe --test scripts\showroom-admin-version-center.test.mjs`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm run ts:check`
- PASS: Browser 真实前端路径 `http://localhost:8081/showroom/company`
- PASS: frontend feature evidence 校验
- PASS: bug regression evidence 校验
- PASS: task-closeout-cleanup preview

## Cleanup Keep

- `doc/tasks/20260525-showroom-company-editable-fields/task.md`
- `doc/tasks/20260525-showroom-company-editable-fields/execution-log.md`
- `doc/tasks/20260525-showroom-company-editable-fields/frontend-feature-evidence.md`
- `doc/tasks/20260525-showroom-company-editable-fields/bug-regression-evidence.md`

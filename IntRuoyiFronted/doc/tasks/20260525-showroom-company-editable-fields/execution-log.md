# 执行日志：修复公司核心制造能力与荣誉资质被过滤

BDD: 公司编辑必须包含核心制造能力 -> Given 公司接口返回 `core_manufacturing_capability` 和 `core_manufacturing_capability_en`, When 用户打开公司信息编辑弹窗, Then 中文和 English tab 都能编辑该字段。

BDD: 公司编辑必须包含荣誉资质 -> Given 公司接口返回 `honors_awards` 和 `honors_awards_en`, When 用户打开公司信息编辑弹窗, Then 中文和 English tab 都能编辑该字段。

BDD: 公司英文翻译必须覆盖完整公司字段 -> Given 用户点击 English tab 的 `Translate English Content`, When 前端构造翻译请求, Then `fieldCodes` 和 `fields` 必须包含 `core_manufacturing_capability` 与 `honors_awards`。

INFO: 已采用 `bug-regression-fix-loop` 与 `frontend-feature-delivery` 工作流。
INFO: 已确认上一前端任务 `20260524-ebr-report-visual-fidelity` 状态为已完成。
RED: `D:\Programs\node.exe --test scripts\showroom-admin-company-editable-fields.test.mjs` -> FAIL, `visibleCompanyFieldDefinitions` 仍过滤 `core_manufacturing_capability` 与 `honors_awards`，公司展示、编辑和翻译路径未使用完整字段定义。
INFO: 已移除 `visibleCompanyFieldDefinitions` 过滤列表，`CompanyProfileForm.vue` 与 `CompanyWorkbench.vue` 的展示、编辑、翻译源字段、翻译 fieldCodes 和翻译指纹统一使用 `companyFieldDefinitions`。
GREEN: `D:\Programs\node.exe --test scripts\showroom-admin-company-editable-fields.test.mjs` -> PASS。
INFO: 已同步 `CompanyVersionWorkbench.vue` 历史预览字段列表使用完整 `companyFieldDefinitions`，避免引用已删除的过滤列表。
INFO: 已将 `scripts/showroom-admin-frontend.test.mjs` 中公司双语入口无角色门禁的断言收窄到“编辑公司信息”弹窗和 `CompanyProfileForm.vue`，不再与页面工具栏的“手动发布展厅”企宣角色按钮冲突。
GREEN: `D:\Programs\node.exe --test scripts\showroom-admin-frontend.test.mjs` -> PASS。
GREEN: `D:\Programs\node.exe --test scripts\showroom-admin-company-version-tab.test.mjs` -> PASS。
GREEN: `D:\Programs\node.exe --test scripts\showroom-admin-version-center.test.mjs` -> PASS。
INFO: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> FAIL, 当前依赖布局下 `pnpm exec` 未解析到 `vue-tsc` 可执行文件；改用仓库脚本入口验证。
INFO: `pnpm run ts:check` -> FAIL, 未设置堆内存参数时触发 JavaScript heap out of memory。
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm run ts:check` -> PASS。
GREEN: Browser 真实前端路径 `http://localhost:8081/showroom/company` -> PASS, 编辑公司弹窗中文 tab 可见“核心制造能力 / 荣誉资质”，English tab 可见 `Core Manufacturing Capability / Honors and Awards`，`Translate English Content` 按钮可用。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260525-showroom-company-editable-fields\frontend-feature-evidence.md` -> PASS。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260525-showroom-company-editable-fields\bug-regression-evidence.md` -> PASS。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260525-showroom-company-editable-fields --mode preview` -> PASS, keep 为本任务 `task.md`、`execution-log.md`、`frontend-feature-evidence.md`、`bug-regression-evidence.md`，delete/blocked/warnings 均为空。

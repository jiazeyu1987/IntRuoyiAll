# Frontend Feature Evidence

## Feature

DCC 文件上传页优化：统一历史文件升版、新建 master、编号冲突、版本格式、生效日期和未分类目录提示状态，避免用户在冲突状态下继续提交。

## Acceptance

- 历史文件升版和新建 master 必须互斥；选择历史文件但找不到现行主档时直接阻断。
- 编号链冲突必须中文化，并同步到文件编号/版本预检和字段错误。
- 版本号格式必须与后端解析器一致，非法值 `abc` 在前端阻断。
- 过去生效日期按当前业务规则允许补录，但页面必须明确说明。
- 文控文件允许发布到“未分类”，页面表达为规则落位，不作为异常兜底。

## Owned Files

- `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`
- `IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts`
- `IntRuoyiFronted/tests/e2e/dcc-upload-optimization-static.spec.js`
- `IntRuoyiFronted/tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js`
- `IntRuoyiFronted/package.json`

## API Contracts

复用现有 `getControlledFileCurrentVersion`、`getControlledFileUploadRevisionCandidates` 和 `submitControlledFile` 契约；本任务未改变后端接口入参、出参或错误码。前端仅把已存在的英文错误和错误码归一化为中文用户提示。

## BDD

- BDD: 历史文件升版状态互斥 -> Given 用户选择历史文件 When 系统无法定位该历史文件对应的现行主档 Then 页面必须阻断升版并说明原因，不得显示将创建新的 master 主档。
- BDD: 编号冲突预检阻断 -> Given 文件编号命中已有逻辑版本链冲突 When 前端预检显示文件编号/版本状态 Then 状态必须为不可提交，并使用中文错误说明。
- BDD: 版本格式前端校验 -> Given 新文件版本号填写 `abc` When 用户触发预检或提交 Then 文件编号/版本必须显示格式错误，不能显示可提交。
- BDD: 生效日期规则明确 -> Given 生效日期选择过去日期 When 用户查看预检或提交 Then 页面必须显示允许补录历史生效日期。
- BDD: 未分类允许发布 -> Given 文件类别没有专属提交目录 When 系统落位到未分类 Then 页面显示这是允许规则，不作为阻断缺陷。

## RED

RED: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-optimization:static` -> FAIL，首个失败为未分类提示仍是自动兜底文案。

## GREEN

GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-optimization:static` -> PASS。

## Verification

- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-current-version:static` -> PASS。
- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-project-taxonomy-revision:static` -> PASS。
- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-category-taxonomy-binding:static` -> PASS。
- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-governance-ux:static` -> PASS。
- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-name-version-autofill:static` -> PASS。
- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-layout:static` -> PASS。
- `pnpm --dir IntRuoyiFronted ts:check` -> PASS。
- `git diff --check -- <task-owned files>` -> PASS。
- Responsive/accessibility/loading/empty/error states: loading 使用现有 skeleton 和 lookup loading；error 使用 `el-alert type="error"`；empty/new master 仅在非升版路径显示；未分类与历史日期用明确中文说明。

## Blockers

None。

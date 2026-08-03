# 20260803 FormCenter Route Missing

## Task Goal

修复 eDHR / FormCenter 运行态打开动态表单时误请求 `admin-api/form-center/templates/{templateId}/versions/{versionNo}` 导致“请求地址不存在”的问题，确保运行态优先使用 `openTask` 返回的模板快照，不依赖模板管理版本查询接口。

## Milestones

- [completed] 定位缺失请求的前端调用链和运行态入口
- [completed] 补充最小 BDD/TDD 回归契约，先复现当前错误风险
- [completed] 实施最小正式修复，不引入 fallback、吞异常或默认成功
- [completed] 运行定向验证并记录 RED/GREEN/REGRESSION 证据
- [completed] 完成收尾检查、经验沉淀和 Git 提交推送

## Expected Verification

- `node tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js`
- 新增或更新的 FormCenter 运行态静态契约 RED/GREEN
- 必要时运行相邻 `ActionFormPanel` / eDHR FormCenter 导航契约
- 若触及 TypeScript 逻辑，运行 `pnpm ts:check` 或记录明确阻塞

## Current Status

completed

实现、定向验证、证据验证器、经验沉淀、cleanup apply 和实现提交已完成。实现提交：`a4e9e1eda fix: use embedded FormCenter runtime templates`。主工作区存在大量非本任务脏改动，本任务未在主工作区直接修改，linked worktree 合并/删除未执行。

## Applicable Experience Gates

- `FormCenter 动态表单字段码渲染门禁`：动态表单运行态必须使用模板布局、识别字段、实例草稿共同渲染；模板既缺布局又缺识别字段时必须可见失败，不得画空壳。
- `FormCenter 嵌入模板对象类型契约门禁`：本地构造的嵌入模板对象必须满足正式 `FormTemplateListItemVO` 字段契约，不得用 `as any` 或放宽接口字段。
- `切换填写人 FormCenter 槽位导航门禁`：FormCenter 槽位运行态必须使用 `openTask` 返回的 `formTemplateJimuSchemaJson` / `formTemplateRecognizedFields`，不得把模板管理查询接口作为普通填写人运行态前置。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是运行态表单使用正式 `openTask` 模板快照，避免错误依赖模板管理版本接口。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- IntRuoyiFronted/tests/e2e/edhr-dynamic-form-action-panel-prefill-static.spec.js
- IntRuoyiFronted/tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js
- IntRuoyiFronted/tests/e2e/form-center-static.spec.js
- docs/experience-index.md
- docs/frontend-development.md

## Final Verification Result

- PASS: 定向静态契约、相邻回归、`pnpm ts:check`、证据验证器、`git diff --check` 和 branch runtime port guard 均已通过。
- PASS: cleanup apply 已完成并只删除临时 evidence 文件，核心任务记录和正式测试/经验文档保留。

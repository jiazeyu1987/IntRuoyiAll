# DCC 元数据联动体验优化

## Task Goal

优化 DCC 受控文件上传/升版后的元数据修改链路，让文件分类、DCC 项目代码、关联文档、权限诊断和变更追溯在页面上可见、可跳转、可验证。

## Milestones

1. `completed` - 建立 BDD/TDD 任务证据并冻结适用门禁。
2. `completed` - 增加最小静态合同，先覆盖当前缺失的前置预览、联动入口、权限诊断与追溯展示。
3. `completed` - 实现前端最小正式优化，不引入 fallback、不吞接口错误。
4. `completed` - 运行静态合同、类型/邻近验证和真实 Playwright E2E。
5. `completed` - 更新验证报告与收尾状态。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/dcc-controlled-file-metadata-linkage-ux-static.spec.cjs`
- 如可行，运行 `pnpm ts:check` 或记录无关历史阻塞。
- 使用 Playwright 真实前端路径，以非 admin 文控账号完成 DCC 文件项目代码/文件类型修改与恢复，验证页面联动入口、关联文档定位、变更追溯和权限诊断无新增错误。
- 只读 DB/API 复核目标文件最终恢复到原 DCC 项目代码和原文件分类。

## Applicable Gates

- `docs/frontend-development.md#DCC 基础条目关联文档分类树门禁`：文件分类树、项目代码关联文档、`Only doc control...` 权限缓存诊断必须在页面和验证中覆盖。
- `docs/e2e-rules.md#DCC 文控审批处理入口门禁`：涉及上传/升版审批链路时必须使用真实页面处理态，API 只能用于后置只读核验。
- `docs/e2e-rules.md#Playwright 目标链路与外部资源异常归因门禁`：区分本机/DCC 目标链路错误与外部资源错误。
- `docs/frontend-development.md#前端静态契约隔离门禁`：如全量类型检查先失败于无关历史问题，使用任务专用静态合同证明本次行为。

## Current Status

ready_for_closeout

## Completed Work

- 在受控文件元数据弹窗新增保存前“变更影响预览”，展示当前/目标 DCC 项目、当前/目标分类路径、当前受控目录和受控浏览落位。
- 在受控文件详情页新增“DCC 项目代码联动”区，展示当前项目、文件分类、关联文件 ID，并提供关联文档入口和修正追溯入口。
- 在项目代码详情关联文档页支持从详情页 query 定位当前文件和文件类型，并高亮“当前联动”行。
- 将 `Only doc control can update controlled file metadata` 类失败转换为可行动诊断，提示 `doc_control` 角色和 `user_role_ids` 缓存刷新/重新登录。
- 新增任务专用静态合同和 npm script：`e2e:dcc:metadata-linkage-ux:static`。

## Verification Result

- `node tests\e2e\dcc-controlled-file-metadata-linkage-ux-static.spec.cjs`：PASS。
- `node tests\e2e\dcc-project-code-associated-three-column-static.spec.js`：PASS。
- `node tests\e2e\dcc-upload-governance-ux-static.spec.js`：PASS。
- `node tests\e2e\dcc-upload-project-taxonomy-revision-static.spec.js`：PASS。
- `node tests\e2e\dcc-metadata-file-number-optional-static.spec.js`：PASS。
- `pnpm ts:check`：PASS。
- 真实 Playwright E2E：非 admin 文控账号 `wangsiyu` 完成 5 次已有文件项目代码/文件类型修改、项目代码详情三栏验证，并恢复原项目 `HGGW` 和原分类 `技术文档 / 设计和开发输出阶段 / 来料/过程/成品检验规范`。

## Closeout Notes

- 本任务实现和验证已完成，工作区存在大量非本任务 MES/EDHR/其它任务脏改动，未执行提交/推送，避免混入无关改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，前端显式呈现正式元数据联动、权限失败原因和追溯字段。
- `是否存在临时补丁或绕过`：否。

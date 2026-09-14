# 20260803-dcc-upload-project-code-hint

## Task Goal

修复 DCC 受控文件上传页中 DHF/DMR 类别下“产品编号”提示固定显示红色错误的问题：未绑定项目代码时提示错误，已自动带出项目代码时显示成功确认，避免员工误以为已选择 DCC 项目仍校验失败。

## Milestones

- [x] M0: 建立任务记录、读取前置规则，并隔离任务前脏工作区基线。
- [x] M1: 用静态回归测试复现当前红色提示固定显示的问题。
- [x] M2: 最小修改上传页提示逻辑与样式，不改变提交和后端绑定链路。
- [x] M3: 运行目标静态测试、相邻回归与必要前端检查。
- [x] M4: 更新验证报告、收尾状态和提交记录。

## Expected Verification

- `node tests/e2e/dcc-upload-project-code-hint-static.spec.js`
- `pnpm e2e:dcc:upload-project-code-hint:static`
- `node tests/e2e/dcc-product-category-rule-static.spec.js`
- `node tests/e2e/dcc-upload-product-autofill-static.spec.js`
- `node tests/e2e/dcc-original-release-ux-improvements-static.spec.js`
- `pnpm ts:check`（如受现有历史问题阻塞，记录首个阻塞并保留目标静态合同结果）

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 UI 状态区分未绑定错误与已绑定成功提示。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary

- `docs/frontend-development.md#DCC 上传类别权限投影门禁`：本次不改变类别权限、上传预览或 submit 权限链路；只调整已绑定/未绑定项目代码的提示状态。
- `docs/database-rules.md#DCC 项目代码 MDM 产品建档绑定门禁`：本次不改 schema、服务或受控文件提交来源；后端仍按 `dccProjectCodeId` 解析 DCC 项目代码/MDM 产品信息。
- `docs/frontend-development.md#DCC 基础条目关联文档分类树门禁`：本次不改文件分类树、元数据保存或项目代码详情关联文档链路。

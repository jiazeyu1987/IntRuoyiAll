# DCC 双版本文件上传与下载权限

## Task Goal

实现 DCC 文件上传的双版本模型：

- 不可编辑版本必须上传，固定作为在线浏览版本。
- 可编辑版本可选上传，不可编辑版本缺失时禁止提交。
- 可编辑版本和不可编辑版本使用两个独立下载权限。
- 在线浏览权限与两个下载权限相互独立。
- 不做自动转换、文件回退或静默兼容。

## BDD

BDD: 必须上传不可编辑版本 -> Given 用户提交受控文件 / When 不可编辑版本缺失 / Then 提交失败且不创建受控文件。

BDD: 可编辑版本可选 -> Given 用户已上传有效不可编辑 PDF / When 未上传可编辑版本并提交 / Then 提交成功且在线浏览使用不可编辑 PDF。

BDD: 双版本保存 -> Given 用户同时上传不可编辑 PDF 与可编辑源文件 / When 提交成功 / Then 两个文件分别保存，浏览只读取不可编辑文件。

BDD: 下载权限独立 -> Given 用户只拥有其中一个下载权限 / When 请求对应版本下载 / Then 仅授权版本可下载，另一个版本返回无权限。

BDD: 浏览权限独立 -> Given 用户只有在线浏览权限 / When 打开文件浏览器 / Then 可以在线浏览但不能获得任一下载文件。

## Milestones

1. RED: 添加双版本字段、purpose、下载端点和权限契约测试。
2. GREEN: 完成后端数据模型、上传校验、版本解析、下载鉴权和前端上传/下载入口。
3. REGRESSION: 运行后端 DCC 定向测试、前端静态契约测试和类型/编译验证。
4. CLOSEOUT: 生成验证报告，执行任务清理，再合并到 `int_main`。

## Expected Verification

- `node --test IntRuoyiFronted/tests/dcc-dual-version-upload-download-contract.test.cjs`
- DCC 后端模块定向 Maven 测试
- 前端定向 ESLint/TypeScript 检查
- `git diff --check`
- 合并后在 `int_main` worktree 复核状态和关键文件

## Current Status

completed

## Design Constraints

- 任务只覆盖 DCC 普通上传、在线浏览和版本下载入口；不改外审、签入和 NAS 导入的历史业务语义。
- 现有 `sourceFileId` 等字段继续服务旧流程；新双版本字段必须由新上传路径显式写入，不能用旧字段互相猜测。
- 后端必须 fail fast；禁止 fallback、自动 PDF 转换、静默吞错或用同一个文件填充两个版本。
- 不执行真实 E2E，因为用户本轮未明确要求 E2E；使用后端定向测试和前端静态/类型验证。
- 不操作真实数据库；SQL 仅作为正式迁移文件提交，并在静态合同中验证幂等守护和权限定义。

## 设计约束检查

- PASS: 普通上传使用必填 `readOnlyUploadTicket` 和可选 `editableUploadTicket`，不复用同一个 ticket。
- PASS: 在线浏览解析 `readOnlyFileId`，不读取可编辑版本。
- PASS: 下载接口拆分为不可编辑版本和可编辑版本两个权限。
- PASS: 未执行 E2E，符合用户本轮未明确要求 E2E 的限制。
- PASS: 使用仓库外便携 Java 17 / Maven 3.9.11 完成后端 DCC 反应堆编译验证。
- PASS: 已补齐本机 `task-closeout-cleanup` 脚本，待执行 cleanup preview/apply。

## Cleanup Keep

- doc/tasks/20260918-001/task.md
- doc/tasks/20260918-001/execution-log.md
- doc/tasks/20260918-001/verification-report.md
- IntRuoyiFronted/tests/dcc-dual-version-upload-download-contract.test.cjs

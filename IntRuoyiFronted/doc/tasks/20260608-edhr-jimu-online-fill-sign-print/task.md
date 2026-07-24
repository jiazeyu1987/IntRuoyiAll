# 任务：eDHR Jimu 在线填写、多人电子签名与最终打印前端

## 任务目标

在 `edhr_jimu` 前端 worktree 中补齐 eDHR 表单用户路径：在线填写字段、保存字段变更电子签名、查看一张表单的多人签名记录，并生成或下载最终 PDF 表单用于打印。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。沿用 eDHR 执行页、审批页、归档接口和签名记录页面，不新增绕过后端审计的前端保存路径。
- `是否存在临时补丁或绕过`：否。

## 里程碑

- [x] M1：创建 `edhr_jimu` 前端 worktree 和分支。
- [x] M2：补齐在线填写、多人签名、最终打印的前端验收测试。
- [x] M3：实现用户可见的签名汇总和最终表单打印入口。
- [x] M4：运行前端静态/目标测试和必要 Playwright 验证。
- [x] M5：运行 task-closeout-cleanup 预览并提交本任务改动。
- [x] M6：重新验证最终表单归档下载/打印真实 E2E。

## 预期验证

- eDHR 目标前端测试覆盖：字段填写保存、签名记录展示、归档生成和下载按钮状态。
- Playwright 真实页面路径：登录后进入 eDHR 执行详情，完成字段保存签名、查看多人签名、生成并下载最终 PDF。

## 当前状态

completed；执行页已接入 `FORM_REVIEW` 复核签名按钮、密码弹窗和 `/cosign` API，签名/追踪类型已支持 `FORM_REVIEW`，最终表单归档入口已明确为归档打印件，前端目标测试、类型检查和页面 smoke 均通过。2026-06-08 真实 E2E 追加验证完成：追踪/签名页、“复核签名”闭环、最终表单重新生成归档与下载打印件路径均通过；归档下载 Blob 为 `application/pdf`，大小和 SHA-256 与最新归档一致。

## Cleanup Keep

- `doc/tasks/20260608-edhr-jimu-online-fill-sign-print/e2e-archive-download-evidence.md`
- `doc/tasks/20260608-edhr-jimu-online-fill-sign-print/e2e-form-review-evidence.md`
- `doc/tasks/20260608-edhr-jimu-online-fill-sign-print/e2e-tracking-signature-evidence.md`
- `doc/tasks/20260608-edhr-jimu-online-fill-sign-print/scripts/edhr-archive-download-real-e2e.cjs`
- `doc/tasks/20260608-edhr-jimu-online-fill-sign-print/scripts/edhr-form-review-real-e2e.cjs`

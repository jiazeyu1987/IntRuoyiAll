# Verification Report

## Summary

本任务修复 DCC 受控预览在元数据返回 `previewUnavailableReason` 时的前端行为：所有预览类型先尊重不可用原因并停止二进制预览请求；非 Office 类型通过通用错误区域显示精确原因，Office 继续由 OnlyOffice 只读组件展示不可用原因。

## Root Cause

后端元数据已经能为 PDF、图片、视频、音频、文本、Office、下载型文件返回 `previewUnavailableReason`。前端 `ProtectedPdfViewer.loadPreview()` 仅对 `OFFICE` / `DOWNLOAD_ONLY` 提前返回，PDF/IMAGE/VIDEO/AUDIO/TEXT 会继续调用 `resolvePreviewBlob()`，进而请求二进制预览接口并暴露泛化加载失败。

## Fix

- 在 `loadPreview()` 中新增 `resolvedPreviewUnavailableReason.value` 统一短路，位置早于 `resolvePreviewBlob()`。
- 对非 Office 类型设置 `errorMessage.value = resolvedPreviewUnavailableReason.value`，复用已有页面顶部错误展示并抑制空内容提示。
- 保留 Office 的既有 `OnlyOfficeReadOnlyViewer :unavailable-reason` 展示链路，不新增降级或吞异常。
- 更新 `dcc-common-file-preview-source.spec.js` 并新增 `dcc-preview-unavailable-reason-static.spec.js` 锁定全类型不可用原因优先。

## Verification

- RED: `node tests/e2e/dcc-preview-unavailable-reason-static.spec.js` -> FAIL, `viewer must guard previewUnavailableReason before binary loading`。
- RED: `node tests/e2e/dcc-common-file-preview-source.spec.js` -> FAIL, `Protected viewer must short-circuit preview binary loading when previewUnavailableReason is present`。
- GREEN: `node tests/e2e/dcc-preview-unavailable-reason-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-common-file-preview-source.spec.js` -> PASS。
- GREEN: `node tests/e2e/unified-online-file-preview-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260803-dcc-preview-all-types-unavailable/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260803-dcc-preview-all-types-unavailable/bug-regression-evidence.md` -> PASS。
- GREEN: `rg -n "DCC 预览不可用|previewUnavailableReason|dcc-预览不可用原因短路门禁" docs/experience-index.md docs/frontend-development.md` -> PASS，长期经验已合并进现有文档。
- GREEN: cleanup preview/apply -> PASS，已删除临时 `bug-regression-evidence.md` 与 `frontend-feature-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- GREEN after concurrent baseline `a79a8c31d`: `node tests/e2e/dcc-preview-unavailable-reason-static.spec.js`、`node tests/e2e/dcc-common-file-preview-source.spec.js`、`node tests/e2e/unified-online-file-preview-static.spec.js`、`pnpm ts:check` -> PASS。

## Regression Notes

- `node tests/e2e/dcc-controlled-file-protection.contract.test.js` 仍失败在历史断言：旧契约要求 viewer 源码直接包含 `previewControlledFileWithWatermark`，但当前 viewer 已通过统一预览 API `previewOnlineFileWithWatermark` 委托。该失败与本次不可用原因短路修复无关。
- 本任务的源码/测试改动被并发基线提交 `a79a8c31d` 与其它 DCC 改动一起吸收；本任务不改写历史，剩余提交只包含任务记录和长期门禁文档。

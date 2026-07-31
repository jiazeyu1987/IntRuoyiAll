# Verification Report

## Summary

本任务已在测试服务器 `172.30.30.58` 使用 `芋道源码/wangsiyu` 账号完成一次真实 DCC 文件上传模拟。上传从前端页面发起，使用真实文件控件选择任务自有 DOCX 文件，并通过后端正式 `upload-preview` 和 `submit` 接口。

## Real Path Evidence

- Login: 用户授权后完成测试服滑块验证码，`wangsiyu` 登录成功。
- Upload entry: `http://172.30.30.58:8081/dcc/controlled-file/upload`。
- Test file: `doc/tasks/20260729-test-server-wangsiyu-file-upload-simulation/input/codex-upload-simulation-20260729.docx`，大小 `36872` 字节。
- Selected category: `908709 / 市场调研报告`，审批路线 `913747`。
- Upload preview: `/admin-api/dcc/controlled-files/upload-preview` -> HTTP 200，业务码 `0`，`previewKind=OFFICE`。
- Submit: `/admin-api/dcc/controlled-files/submit` -> HTTP 200，业务码 `0`，受控文件 ID `2054545668044083977`。
- Detail readback: `fileName=Codex file upload simulation 20260730035911679-431F9C`，`fileNumber=CODEX-UPLOAD-20260730035911679-431F9C`，`versionNo=V1.0`，`status=PENDING_DOC_CONTROL_REVIEW`。

## Temporary Configuration

- Category upload permission: temporarily added `USER/910250/UPLOAD` for category `908709` and restored original permission rules.
- Upload size policy: temporarily created policy ID `3` for `CATEGORY_PURPOSE/SOURCE`, `maxBytes=10485760`, then disabled it in cleanup.
- No temporary category permission or active upload size policy remains from this task.

## Data Impact

- Retained test business data: DCC controlled file `2054545668044083977`.
- Cleanup decision: left in test server workflow because it was successfully submitted into doc-control review; no page-safe delete action was available in scope.
- Evidence artifacts: `upload-evidence.json` and `upload-result.png`.

## Result

PASS: file upload simulation completed and verified.

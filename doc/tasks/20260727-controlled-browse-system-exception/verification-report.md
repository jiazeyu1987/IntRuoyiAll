# Verification Report

## Scope

只读分析测试服务器 `172.30.30.58` 上 `wangsiyu` 账号进入文控中心受控浏览文件详情时提示“系统异常”的真实原因。未修改业务数据、未重启服务、未发布。

## Reproduction

- Frontend: `http://172.30.30.58:8081`
- Backend release: `release-20260723-dcc-viewer-permission-r260723vp-r1`
- Login context: `芋道源码 / wangsiyu`
- Target file: `dcc_controlled_file.id=2054545668044071537`
- Viewer path: `/dcc/controlled-file/detail/2054545668044071537?viewer=1&from=browser`
- Visible result: protected viewer shows `系统异常` while detail side panel still loads file metadata.

## Evidence

- Frontend captured failure: `GET /admin-api/dcc/controlled-files/2054545668044071537/preview-metadata` returned application `code=500`, `msg=系统异常`.
- Backend log: `java.lang.IllegalArgumentException: fileNumber is required` at `DccControlledPreviewAccessService.requireNotBlank`, called by `prepareAccess`.
- Source path: `DccControlledFileQueryServiceImpl.getPreviewMetadata` passes `file.getFileNumber()` into `DccPreviewAccessRequest`.
- Source path: `DccControlledPreviewAccessService.requireRequest` requires `fileNumber` to be nonblank.
- Data check: target row has blank `file_number`; published file exists as `infra_file.id=9198354917321`, name `血液瓶瓶体清洗验证.pdf`.
- Scope check: tenant 1 has `15995` active/superseded controlled-file rows with blank `file_number`.

## Conclusion

The error is caused by a backend validation/data-contract mismatch, not by PDF rendering, browser permissions, or OnlyOffice. DCC metadata currently allows some controlled files to have no file number, but preview access/audit generation requires `fileNumber`, so blank-number files fail at `preview-metadata` and surface as the generic `系统异常`.

## Recommended Fix

Add a regression test for preview metadata on a controlled file with blank `fileNumber`, then change the preview/access audit path to use a stable nonblank display identifier such as file title/name or controlled-file id when `fileNumber` is blank, without weakening access permission checks or swallowing exceptions.

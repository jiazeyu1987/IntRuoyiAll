# Verification Report

## Scope

- 验证 DCC 受控文件 viewer 在 metadata 返回 `previewUnavailableReason` 时的真实页面表现。
- 只读范围：通过真实前端登录和受控浏览页面进入 viewer；脚本仅拦截 metadata 响应注入不可用原因，不创建业务数据，不调用 DCC 写接口。

## Real E2E Result

- 命令：`node tests/e2e/dcc-preview-unavailable-reason-real.e2e.js`
- 结果：PASS
- 本机入口：`http://127.0.0.1:8081`
- 登录身份标签：`芋道源码/admin`
- 目标文件：`Codex DCC 升版发布全链路 20260802222723`
- Viewer URL：`/dcc/controlled-file/detail/2054545668044070308?viewer=1&from=browser&returnTo=...`

## Assertions

- `PDF`：展示 `E2E-PREVIEW-UNAVAILABLE-PDF-20260803`，二进制预览请求数 `0`。
- `IMAGE`：展示 `E2E-PREVIEW-UNAVAILABLE-IMAGE-20260803`，二进制预览请求数 `0`。
- `VIDEO`：展示 `E2E-PREVIEW-UNAVAILABLE-VIDEO-20260803`，二进制预览请求数 `0`。
- `AUDIO`：展示 `E2E-PREVIEW-UNAVAILABLE-AUDIO-20260803`，二进制预览请求数 `0`。
- `TEXT`：展示 `E2E-PREVIEW-UNAVAILABLE-TEXT-20260803`，二进制预览请求数 `0`。
- `DOWNLOAD_ONLY`：展示 `E2E-PREVIEW-UNAVAILABLE-DOWNLOAD_ONLY-20260803`，未被“仅支持下载”空状态覆盖，二进制预览请求数 `0`。
- `OFFICE`：展示 `E2E-PREVIEW-UNAVAILABLE-OFFICE-20260803`，二进制预览请求数 `0`。

## Safety Evidence

- `dccWriteRequests=[]`
- `targetNetworkFailures=[]`
- `consoleErrors=[]`
- `pageErrors=[]`

## Supporting Checks

- `node --check tests/e2e/dcc-preview-unavailable-reason-real.e2e.js` -> PASS
- `node tests/e2e/dcc-preview-unavailable-reason-static.spec.js` -> PASS
- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> PASS
- `node tests/e2e/unified-online-file-preview-static.spec.js` -> PASS

## Notes

- 首次 E2E 执行失败原因是 Playwright 默认 `chromium_headless_shell` 缺失；脚本已改为显式使用当前 Playwright 安装报告的 Chromium 可执行文件，并在路径缺失时 fail fast。
- 临时 `real-e2e-result.json` 和截图只用于提取本报告证据，已按 closeout cleanup 默认规则删除。
- cleanup preview/apply 均 PASS，保留正式任务记录并删除本任务临时产物。

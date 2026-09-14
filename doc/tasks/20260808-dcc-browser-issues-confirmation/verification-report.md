# 20260808 DCC 受控浏览问题确认报告

## Verification Result

- Overall: PARTIAL CONFIRMED。
- Confirmed: 会话失效时筛选标签与表格数据不同步。
- Not reproduced in current local runtime: `预览` 和文件名称按钮无反馈。
- Not reproduced in current local runtime: 分页 `前往` 输入框 Enter 不跳转且保留错误页码。
- Scope: 只读验证；未确认下载，未保存、提交、删除、修改权限或写入业务数据。

## Runtime Evidence

- Local frontend: `http://127.0.0.1:8081`。
- Local backend: `http://127.0.0.1:48081`，前置核对为 `UP`。
- Browser executable: `C:\Program Files\Google\Chrome\Application\chrome.exe`。
- Login label: tenant `芋道源码`，user `admin`；密码未记录。
- DCC write requests: `[]` in all recorded runs。

## Finding 1 - Session Expired Stale Filter State

- Status: CONFIRMED。
- Evidence file: `doc/tasks/20260808-dcc-browser-issues-confirmation/session-result.json`。
- Target directory: `0 QM`, ID `906512`, path `质量管理/1. QMS documents/0 QM`。
- Baseline request returned 3 rows; all rows were `ACTIVE`, category ID `906104`, and had `publishedFileId` with `stampedFileId=null`。
- After filtering category `其他`, the page still had the same 3 rows.
- After expiring auth tokens and switching category to `市场调研报告`, the browser-page response returned HTTP `200`, business code `401`, message `账号未登录`。
- UI showed `登录超时,请重新登录!` and filter label `类别: 市场调研报告` while table rows remained the previous 3 `其他` category rows.
- Supporting source evidence: `handleQuery` syncs route before `getList()` in `IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue`; `getList()` updates rows only on success and does not clear or mark stale data on failed auth; quick-filter tab labels are built from the current draft condition.

## Finding 2 - Preview And File Name No Feedback

- Status: NOT REPRODUCED in current local runtime。
- Evidence file: `doc/tasks/20260808-dcc-browser-issues-confirmation/preview-result.json`。
- Exact target row was present: `INT∕QM(E∕2) 质量手册 Quality Manual-INT-2026.2.28生效.pdf`, ID `2054545668044052987`, `publishedFileId=9198354895793`, `stampedFileId=null`。
- Clicking `预览` opened a popup to `/dcc/controlled-file/detail/2054545668044052987?viewer=1&from=browser...`。
- Clicking the file name opened the same viewer popup.
- No page errors, console errors, or DCC write requests were recorded.
- Source risk remains: `openPreview()` only calls `window.open(...)` and has no user-visible feedback if the popup is blocked or the viewer route later fails.

## Finding 3 - Pagination Jumper Enter

- Status: NOT REPRODUCED in current local runtime。
- Evidence file: `doc/tasks/20260808-dcc-browser-issues-confirmation/pagination-preview-result.json`。
- Current local full-scope browser-page request used `latestVersionOnly=true` and returned total `15,917`, so the last page was `796`, not the reported `31,370` / page `1,569` condition.
- From page 2, entering page `796` in the jumper and pressing Enter emitted a browser-page request for `pageNo=796&pageSize=20`, changed the URL to `pageNo=796`, displayed 17 rows, and updated the jumper value to `796`。
- Source observation: the shared pagination component relies on Element Plus `current-change`; in this runtime the jumper Enter did trigger that event.

## Open Confirmation Items

- Session timeout policy is still a product/runtime configuration question. This task manually expired tokens to reproduce the boundary state; it did not measure access-token lifetime or refresh-token behavior.
- The 0 QM three current files were observed with `publishedFileId` populated and `stampedFileId=null`; whether quality manual and quality policy files should generate stamped versions remains a business rule confirmation, not a verified defect here.

## Commands

- `npx --version`
- `node --check doc\tasks\20260808-dcc-browser-issues-confirmation\pagination-preview-probe.cjs`
- `node doc\tasks\20260808-dcc-browser-issues-confirmation\pagination-preview-probe.cjs`
- `$env:DCC_BROWSER_CONFIRM_PHASE="session"; node doc\tasks\20260808-dcc-browser-issues-confirmation\readonly-confirmation.e2e.cjs`
- `$env:DCC_BROWSER_CONFIRM_PHASE="preview"; node doc\tasks\20260808-dcc-browser-issues-confirmation\readonly-confirmation.e2e.cjs`

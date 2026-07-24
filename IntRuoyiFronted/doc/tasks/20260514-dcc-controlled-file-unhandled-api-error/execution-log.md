BDD: controlled-file approval tasks page handles category load failures -> Given `/dcc/file-categories` may fail during initial page load, When the approval tasks page mounts, Then the page must show an explicit load error banner instead of emitting an uncaught promise error.

BDD: controlled-file directories page handles directory tree failures -> Given `/dcc/directories/tree` may fail during initial page load, When the directories page mounts, Then the page must show an explicit load error banner instead of emitting an uncaught promise error.

BDD: generic backend 500 messages are translated into page-specific blocker hints -> Given axios may surface only generic messages such as `系统未知错误，请反馈给管理员`, When a controlled-file read page formats the failure, Then the page should prefer a context-specific fallback message that names the failing prerequisite.

RED: source inspection before fix -> FAIL, `approval-tasks/index.vue` and `directories/index.vue` had no page-level load error banner or local request failure state for initial read errors.

RED: live backend classification probe -> FAIL for backend-bug assumption, because current `GET /admin-api/dcc/file-categories` and `GET /admin-api/dcc/directories/tree` both return `code=0`; therefore the confirmed defect is missing frontend error handling for failure cases.

GREEN: `node --test scripts/dcc-controlled-file-load-error.test.mjs` -> PASS

GREEN: `pnpm exec eslint scripts/dcc-controlled-file-load-error.test.mjs src/views/dcc/controlled-file/shared/utils.ts src/views/dcc/controlled-file/approval-tasks/index.vue src/views/dcc/controlled-file/directories/index.vue` -> PASS

GREEN: `pnpm build:local` -> PASS

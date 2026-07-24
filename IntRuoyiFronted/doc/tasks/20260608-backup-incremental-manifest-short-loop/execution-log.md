# Execution Log

BDD: 运行控制台展示备份策略 -> Given 后端返回最近备份点 / When 运维打开运行控制台 / Then 页面展示备份模式、保留策略、最近备份点和 imageTag。

BDD: 运行控制台展示对象增量统计 -> Given 后端返回对象 added/modified/deleted/reused 统计 / When 运维查看备份策略表格 / Then 页面展示新增、修改、删除、复用数量。

BDD: DCC 短闭环 E2E 脚本限定测试边界 -> Given 验收脚本会操作 DCC 新增、删除和恢复验证 / When 脚本启动 / Then 必须先硬断言不是 `172.30.30.57`、租户为 `测试租户`、账号不是 `admin`。

RED: `node tests\e2e\runtime-control-backup-policy-static.spec.js` -> FAIL，预期原因：`RuntimeControlBackupPointVO` 未声明 `retentionMaxNasUsedPercent`，页面也未展示 NAS 容量阈值。

GREEN: `node tests\e2e\runtime-control-backup-policy-static.spec.js` -> PASS。

GREEN: `node tests\e2e\runtime-control-static.spec.js` -> PASS。

GREEN: `node tests\e2e\runtime-control-remote-root-cleanup-static.spec.js` -> PASS。

EVIDENCE: `tests\e2e\dcc-restore-verify.e2e.js` 增加 JSON 下载响应文本输出，用于在 B4 恢复验证中暴露下载接口返回 `Current user cannot access this controlled file`，避免把 128 字节 JSON 包装误判为原始文件内容。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` after B3 restore -> PASS，A `2054545668044049602` 与 B V1.0 `2054545668044049603` 可访问，B V1.1/V1.2 不存在。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` after B4 restore -> PASS，B V1.2 `2054545668044049605` 可访问，`previewFileName=comments.docx`，下载接口 JSON 文本显示当前测试用户无下载权限；内容大小通过后端任务日志中的 MinIO/object-store 证据补充证明。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` after B5 restore -> PASS，A `2054545668044049602` 可访问，B V1.0/V1.1/V1.2 均不可访问。

REGRESSION: `node --check tests\e2e\dcc-restore-verify.e2e.js; node tests\e2e\dcc-backup-boundary-static.spec.js; node tests\e2e\runtime-control-backup-policy-static.spec.js; node tests\e2e\runtime-control-static.spec.js; node tests\e2e\runtime-control-remote-root-cleanup-static.spec.js` -> PASS。

REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-backup-incremental-manifest-short-loop --mode preview` -> PASS，无删除项、无阻塞、无警告。

GREEN: `node tests\e2e\dcc-upload-test-file.e2e.js` with `DCC_BACKUP_E2E_BASE_URL=http://172.30.30.58:8081`, tenant `测试租户`, user `aoteman`, source `comments.docx` -> PASS，真实前端 DCC 上传路径新增文件 B，`controlledFileId=2054545668044049603`，文件名 `codex-incremental-backup-B-202606081450.docx`，版本 `V1.0`。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` with present A `2054545668044049602` and B `2054545668044049603` -> PASS，A/B 均可访问，B `previewFileName=comments.docx`。

GREEN: `node tests\e2e\dcc-upload-test-file.e2e.js` with `DCC_BACKUP_E2E_BASE_URL=http://172.30.30.58:8081`, tenant `测试租户`, user `aoteman`, source `tables.docx`, same file name/number, version `V1.1` -> PASS，真实前端 DCC 上传路径修改文件 B，`controlledFileId=2054545668044049604`。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` with present A `2054545668044049602` and B V1.1 `2054545668044049604` -> PASS，B V1.1 `previewFileName=tables.docx`。

GREEN: `node tests\e2e\dcc-upload-test-file.e2e.js` with source `D:\IntRuoyi-BackupOps\tmp\dcc-b4-modified-source\comments.docx`, same file name/number, version `V1.2` -> PASS，真实前端 DCC 上传路径提交 B 同 path 修改版，`controlledFileId=2054545668044049605`。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` with present A `2054545668044049602` and B V1.2 `2054545668044049605` -> PASS，B V1.2 `previewFileName=comments.docx`。

GREEN: `node tests\e2e\dcc-withdraw-delete-file.e2e.js` for B ids `2054545668044049605`, `2054545668044049604`, `2054545668044049603` -> PASS，三个版本均在真实前端详情页撤回并删除。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` with present A `2054545668044049602` and absent B ids `2054545668044049603/604/605` -> PASS，A 可访问，B 三个版本均不可访问。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-backup-incremental-manifest-short-loop --mode preview` -> PASS，无删除项、无阻塞、无警告。

GREEN: `node tests\e2e\runtime-control-backup-policy-static.spec.js; node tests\e2e\runtime-control-static.spec.js; node tests\e2e\runtime-control-remote-root-cleanup-static.spec.js` -> PASS。

RED: `node tests\e2e\dcc-backup-boundary-static.spec.js` -> FAIL，预期原因：`dcc-withdraw-delete-file.e2e.js` 缺少禁止 `172.30.30.57`、限定 `测试租户` 和禁止 `admin` 的 E2E 边界断言。

GREEN: `node tests\e2e\dcc-backup-boundary-static.spec.js` -> PASS。

GREEN: `node tests\e2e\runtime-control-backup-policy-static.spec.js` -> PASS。

GREEN: `node tests\e2e\runtime-control-static.spec.js` -> PASS。

GREEN: `node tests\e2e\runtime-control-remote-root-cleanup-static.spec.js` -> PASS。

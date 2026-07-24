# 20260603 DCC Office 受控预览空白 - 执行日志

BDD: Office 预览成功必须有可见承载 -> Given 用户打开 DCC 受控文件详情 `viewer=1` 且预览类型为 `OFFICE` / When OnlyOffice 脚本加载并创建 DocEditor / Then 页面必须出现可见 iframe 或挂载状态，不能只显示水印背景和空白正文。

BDD: OnlyOffice 运行时错误必须显式暴露 -> Given OnlyOffice 脚本加载失败、DocEditor 构造失败或文档加载事件返回错误 / When 用户打开 Office 受控预览 / Then 页面必须显示错误提示，不能静默留白。

INFO: 上一前端任务 `20260602-dcc-nas-transfer-confirm-layer` 已在任务文档中标记 blocked，本任务只处理 Office 受控预览空白。

INFO: 只读数据定位 -> 本机 DCC 文件 `2054545668044047034` 对应 `JL-INT/GL/JY-010-01（A/0）《设施设备台账》.xlsx`，底层 `infra_file.type=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`，预期 `previewKind=OFFICE`。

RED: `node scripts/dcc-onlyoffice-readonly-config.test.mjs` -> FAIL，新增断言指出 `OnlyOfficeReadOnlyViewer` 没有在 `loadOnlyOfficeScript(baseUrl)` 外层使用 `try/catch`，也没有配置 DocsAPI `events.onError`。

RED: `python -m pytest script\tests\test_dcc_onlyoffice_local_runtime_config.py -q`（后端仓库）-> FAIL，`application-local.yaml` / `application-dev.yaml` 的 `DCC_ONLYOFFICE_BASE_URL` 默认值仍是 `http://127.0.0.1:8082`，与本机 OnlyOffice `8080` 不一致。

GREEN: `node scripts/dcc-onlyoffice-readonly-config.test.mjs` -> PASS，4 tests passed，覆盖只读权限、禁止依赖键盘拦截、脚本/挂载失败显式错误、DocsAPI `onError` 显式错误。

GREEN: `python -m pytest script\tests\test_dcc_onlyoffice_local_runtime_config.py -q`（后端仓库）-> PASS，2 tests passed，local/dev YAML 与本地重启脚本统一为 `http://127.0.0.1:8080`。

GREEN: `powershell -ExecutionPolicy Bypass -File .\script\tests\test_restart_ruoyi_script_onlyoffice.ps1`（后端仓库）-> PASS，本地重启脚本 OnlyOffice 配置契约通过。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。直接 `pnpm ts:check` 因 Node 默认堆内存约 4GB OOM 退出 134，扩大堆后类型检查通过。

E2E: `node doc\tasks\20260603-dcc-office-preview-blank\scripts\reproduce-dcc-office-preview-blank.mjs` -> PASS，真实登录 `芋道源码/admin` 打开 `http://localhost:8081/dcc/controlled-file/detail/2054545668044047034?viewer=1&from=detail`，预览元数据 `onlyofficeBaseUrl=http://127.0.0.1:8080`，页面出现 `http://127.0.0.1:8080/.../spreadsheeteditor/...` iframe，宽 1140、高 720，错误提示为空。

INFO: `node scripts/dcc-controlled-file-nonpdf-preview.test.mjs` -> FAIL，失败点为上传页“图纸 PDF”控件保留 `accept=".pdf,application/pdf"`，属于既有图纸 PDF 附件入口，与本次 Office 只读预览空白修复无关，未纳入本任务修改。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence .\doc\tasks\20260603-dcc-office-preview-blank\bug-regression-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-office-preview-blank --mode preview` -> PASS，delete includes task-local evidence/script/png/json only，blocked `<none>`，warnings `<none>`，未执行 apply。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-office-preview-blank --mode apply` -> PASS，已删除任务目录中的一次性复现脚本、截图、JSON 和临时 bug evidence，保留 `task.md` 与 `execution-log.md`。

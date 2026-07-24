# 20260525 本地 OnlyOffice 联调配置 - 执行日志

## BDD

- BDD: 本地后端重启自动携带 OnlyOffice 配置 -> Given 本机存在 Docker OnlyOffice `8080` 服务 / When 运行 `restart-int-ruoyi-local.ps1 -Component backend|full` / Then 后端进程参数应通过环境变量注入 `DCC_ONLYOFFICE_BASE_URL` 和 `DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL`。
- BDD: 本地状态脚本暴露 OnlyOffice 健康 -> Given 本机运行或未运行 Docker OnlyOffice / When 运行 `show-int-ruoyi-local-status.ps1` / Then 输出应包含 OnlyOffice 探针状态，帮助判断 Office 预览前置条件是否满足。

## TDD / Verification Evidence

- RED: `powershell -ExecutionPolicy Bypass -File .\script\tests\test_restart_ruoyi_script_onlyoffice.ps1` -> FAIL, `restart-int-ruoyi-local.ps1` 尚未配置 `DCC_ONLYOFFICE_BASE_URL`。
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\tests\test_restart_ruoyi_script_onlyoffice.ps1` -> PASS。
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\deploy\show-int-ruoyi-local-status.ps1 -WorktreeName int_main -Json` -> PASS, `onlyOffice.status=running`, `onlyOffice.httpStatus=HTTP 200`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-local-onlyoffice-runtime-config --mode preview` -> PASS, keep `task.md` 与 `execution-log.md`，delete `<none>`，blocked `<none>`。

## 当前状态

- 状态：completed
- 下一步：提交本任务变更。

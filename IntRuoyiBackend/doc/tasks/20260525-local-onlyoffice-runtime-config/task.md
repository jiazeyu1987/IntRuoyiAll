# 任务：固化本地 OnlyOffice 联调配置

## 任务目标

- 将 IntRuoyi 本地后端启动脚本固化为可直接联调 DCC Office 预览的 OnlyOffice 配置。
- 本地后端通过 `DCC_ONLYOFFICE_BASE_URL=http://127.0.0.1:8080` 对接现有 Docker `onlyoffice/documentserver`。
- 本地后端通过 `DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL=http://host.docker.internal:{backendPort}` 向 OnlyOffice 容器暴露 Office 文档回源地址。
- 本地状态脚本补充 OnlyOffice 健康信息，便于定位 Office 预览环境问题。

## 维护性评估

- 只修改本地运维脚本和测试，不修改 `application-local.yaml` 默认值，避免把机器依赖硬编码进共享配置。
- OnlyOffice 运行时配置跟随 worktree 端口动态计算，不破坏 `int_main=48081` 与其他 worktree 递增端口规则。
- 若本机 `onlyoffice/documentserver` 未运行，脚本保持失败快返或显式状态暴露，不做静默 fallback。

## 前序任务检查

- 已检查最近同仓任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-worktree-port-allocation\task.md`
- 上一任务状态：`已完成`
- 影响：上一任务已建立 worktree 端口分配，本任务在其基础上补充 OnlyOffice 本地联调配置，不阻塞执行。

## BDD 场景

- BDD: 本地后端重启自动携带 OnlyOffice 配置 -> Given 本机存在 Docker OnlyOffice `8080` 服务 / When 运行 `restart-int-ruoyi-local.ps1 -Component backend|full` / Then 后端进程参数应通过环境变量注入 `DCC_ONLYOFFICE_BASE_URL` 和 `DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL`。
- BDD: 本地状态脚本暴露 OnlyOffice 健康 -> Given 本机运行或未运行 Docker OnlyOffice / When 运行 `show-int-ruoyi-local-status.ps1` / Then 输出应包含 OnlyOffice 探针状态，帮助判断 Office 预览前置条件是否满足。

## 里程碑

- [x] M1：建立任务记录并确认当前 OnlyOffice 本地阻塞点。
- [x] M2：补充脚本级 RED 测试。
- [x] M3：实现本地重启脚本的 OnlyOffice 配置注入。
- [x] M4：扩展状态脚本输出 OnlyOffice 健康信息。
- [x] M5：运行验证、更新执行日志并收尾。

## 预期验证

- `powershell -ExecutionPolicy Bypass -File .\script\tests\test_restart_ruoyi_script_onlyoffice.ps1`
- `powershell -ExecutionPolicy Bypass -File .\script\deploy\show-int-ruoyi-local-status.ps1 -WorktreeName int_main -Json`

## 当前状态

- 状态：completed
- 当前阶段：本地 OnlyOffice 联调配置已固化进重启脚本，状态脚本可直接显示 8080 健康。

## Current Status

completed

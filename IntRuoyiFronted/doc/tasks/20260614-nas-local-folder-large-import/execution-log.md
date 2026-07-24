# 执行日志：NAS 本地大文件夹分批导入 DCC 前端

BDD: 大目录选择不按总大小拦截 -> Given 用户选择 `E:\Downloads\2.DHF` 这类超过 2GB 但单文件不超过后端单文件上限的本地文件夹 / When 前端读取浏览器返回的文件列表 / Then 前端必须保留相对路径并进入确认流程，不因目录总大小超过 2GB 拒绝。

BDD: 大目录分批上传 -> Given 用户确认导入本地文件夹 / When 文件数量或总大小很大 / Then 前端必须先创建导入会话，再按批次提交 `relativePaths[]` 和 `files[]`，最后调用完成接口触发后台 DCC 导入任务。

BDD: 上传失败 fail fast -> Given 任一批次上传失败、相对路径非法、文件数量不一致或完成接口返回错误 / When 前端处理导入 / Then 必须停止后续批次并展示后端错误，不显示成功任务、不静默重试到旧接口。

BDD: 后台任务状态展示 -> Given 分批上传完成并创建后台任务 / When 页面收到任务编号 / Then 复用现有任务结果和轮询展示，`LOCAL_FOLDER` 来源继续隐藏 NAS 权限恢复面板。

RED: node scripts/system-nas-management.test.mjs -> FAIL, 缺少 session/batch/complete 前端合同与分批 UI；当前页面仍缺 splitLocalFolderFilesIntoBatches 等分批上传逻辑。

GREEN: node scripts/system-nas-management.test.mjs -> PASS, 前端 API 暴露 session/batch/complete 三段式导入合同，NAS 管理页移除总大小拦截并展示本地文件夹分批上传进度。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS, NAS 管理页和 DCC workflow API 类型检查通过。

GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence yudao-ui-admin-vue3\doc\tasks\20260614-nas-local-folder-large-import\frontend-feature-evidence.md -> PASS

INFO: 2026-06-14T22:45:23+08:00 用户追加授权测试服 `172.30.30.58` 直接处理；本次必须通过测试服 NAS 管理真实选择并导入本机目录 `E:\Downloads\1. QMS documents\`，记录并解决服务器端遇到的问题。

BDD: 测试服页面分批导入 QMS documents -> Given 测试服务器前端加载最新 NAS 管理页面 / When 用户点击“导入文件夹”并选择 `E:\Downloads\1. QMS documents\` / Then 页面必须使用 session + batch + complete 接口上传本地文件夹，展示上传/任务进度，并在后端任务完成后展示成功状态。

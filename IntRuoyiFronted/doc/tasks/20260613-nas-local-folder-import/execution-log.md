# 执行日志：NAS 本地文件夹导入 DCC 前端

BDD: 本地文件夹导入入口 -> Given 用户具备 DCC 提交、目录管理、类别管理权限并打开 NAS 管理页 / When 用户点击 `导入文件夹` / Then 页面必须触发本地目录选择器，不要求用户先进入 NAS 目录选择模式。

BDD: 本地目录结构上传 -> Given 用户选择包含文件的本地目录 / When 用户确认导入到 DCC / Then 前端必须提交文件内容和浏览器提供的相对路径，不提交本机绝对路径。

BDD: 本地来源结果展示 -> Given 本地导入任务创建成功 / When 页面轮询任务状态 / Then 复用转移任务结果展示，但 `sourceType=LOCAL_FOLDER` 时不展示 NAS 权限恢复面板。

BDD: 本地导入 fail fast -> Given 用户取消选择、选择空文件夹、相对路径非法或总请求大小超过后端 multipart 上限 / When 前端准备提交 / Then 必须直接提示错误并停止，不创建任务、不静默改走 NAS 转移。

RED: `node scripts/system-nas-management.test.mjs` -> FAIL，`workflow.ts` 缺少 `sourceType: 'NAS' | 'LOCAL_FOLDER'`、`importLocalFolderToDcc` 与 `/dcc/controlled-files/local-folder-import`；`src/views/system/nas/index.vue` 缺少 `导入文件夹` 按钮、目录选择器和本地来源 UI 分支。

GREEN: `node scripts/system-nas-management.test.mjs` -> PASS，NAS 管理页包含 `选择` 与 `导出` 之间的 `导入文件夹`、`webkitdirectory multiple` 隐藏 input、本地导入 API、`sourceType` 和隐藏权限恢复面板分支。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

BLOCKED: `pnpm ts:check` -> FAIL，默认 Node heap 下进程 exit 134 / out of memory；使用 8192MB heap 后同一 TypeScript 检查通过，未发现类型错误。

BLOCKED: Playwright 真实路径 `http://localhost:8081/system/nas` -> FAIL，本地前端可打开并跳转登录页，但后端租户接口 `http://localhost:48081/admin-api/system/tenant/get-by-website?website=localhost:8081` 连接被拒绝；无法登录 `aoteman` 完成本地目录导入。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260613-nas-local-folder-import --mode preview` -> PASS，预览无删除项、无阻塞。

E2E: `E:\Downloads\1. QMS documents` 真实目录预检查 -> PASS，目录存在，包含 962 个文件，总大小 884703624 bytes，约 843.72MB。

E2E: Playwright 登录测试租户并打开 `http://localhost:8081/system/nas` -> PASS，使用租户 `测试租户`、账号 `aoteman`、密码 `admin123` 登录后，NAS 管理页展示 `导入文件夹` 按钮。

BLOCKED: Playwright 选择 `E:\Downloads\1. QMS documents` -> FAIL，真实点击 `导入文件夹` 能触发 `filechooser`，但 `chooser.setFiles('E:\\Downloads\\1. QMS documents')` 后页面内 `input.files.length=0`、`total=0`、未弹出导入确认框、未发起 `/dcc/controlled-files/local-folder-import` 请求；本次无法通过 Playwright 将该目录内容交给浏览器 input。

BLOCKED: Playwright 小型目录链路探针 -> FAIL，使用任务内小型目录可触发本地文件夹选择后的业务校验，但测试租户返回 `DCC 模板类别缺少启用的“其他”，请先在 DCC 文件类别中补齐后再转移`，未进入确认弹窗，未创建导入任务。

BLOCKED: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260613-nas-local-folder-import --mode apply` -> FAIL，工具要求任务状态 completed 才允许 apply，当前任务仍因真实 E2E 未完成而非 completed。

GREEN: 临时 E2E 产物清理 -> PASS，校验删除路径均位于 `doc/tasks/20260613-nas-local-folder-import` 下后，删除小型探针目录和 Playwright 截图。

BDD: 去除本地导入大小限制 -> Given 用户选择超过 300MB 的本地文件夹 / When 前端收到浏览器提供的文件列表 / Then 前端不因总大小超过 300MB 而拒绝，继续进入既有相对路径、类别绑定和提交流程。

RED: `node scripts/system-nas-management.test.mjs` -> FAIL，静态测试要求移除 `LOCAL_FOLDER_IMPORT_MAX_SIZE` 和 `本地文件夹导入大小超过 300MB` 文案，但页面仍包含限制。

GREEN: `node scripts/system-nas-management.test.mjs` -> PASS，`LOCAL_FOLDER_IMPORT_MAX_SIZE` 和 `本地文件夹导入大小超过 300MB` 文案已移除，页面保留空文件夹与相对路径校验。

BLOCKED: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> FAIL，失败来自非本任务 `src/views/showroom-admin/index.vue` 的奖项页字段缺失：`productManageTab`、`awardRows`、`awardKeyword`、`handleDeleteAward`、`openAwardEdit` 等属性不存在；未发现 NAS 管理页本次改动相关类型错误。

# eDHR 执行列表真实路径 E2E Evidence

- Task ID: `20260529-edhr-execution-list-real-e2e-gate`
- 生成时间：2026-05-30T16:22:28.250Z
- 前端 worktree：D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3
- 固定前端入口：`http://localhost:8081`
- 默认测试租户：`测试租户`
- 默认账号名：`aoteman`；密码由 `EDHR_EXECUTION_LIST_PASSWORD` 注入，不写入仓库证据。
- 真实 E2E 复跑命令：`pnpm e2e:edhr:execution-list`
- 静态语法检查命令：`pnpm e2e:edhr:execution-list:check`
- 证据文件：默认写入本任务目录 `doc/tasks/20260529-edhr-execution-list-real-e2e-gate/real-e2e-evidence.md`。
- 临时产物目录：`test-results/edhr-execution-list/`（截图、trace、result.json 与下载文件不提交）
- 当前状态：PASS
- executionId：`56`
- executionCode：`BRE202605281813460410056`
- batchCode：`EDHR-BATCH-122-E2E-APPROVE-STORAGE05281812`
- archiveId：`18`

## BDD

- BDD: 执行列表可查询 -> Given 测试租户存在真实 eDHR 执行记录和动态菜单 `eDHR执行列表` / When 用户登录并打开 `/mes/pro/feedback/edhr-execution?batchCode=<real-batch>` / Then 前端请求真实 `/mes/pro/batch-record-execution/page`，页面展示执行编号、生产工单、批次号、执行状态、绑定状态、打开能力和上下文证据。
- BDD: 最新归档状态可见 -> Given 目标执行记录已有真实 `SEALED` PDF 归档 / When 执行列表加载完成 / Then 前端请求真实 `/mes/pro/batch-record-execution-archive/latest`，页面展示 `已封存`、`V1`、`PDF` 或等价归档证据。
- BDD: 列表归档可下载 -> Given 用户具备归档下载权限且目标归档已封存 / When 用户点击列表行的“下载归档” / Then 前端调用真实 `/mes/pro/batch-record-execution-archive/download?id=<archiveId>`，浏览器下载文件，下载 SHA-256 与真实归档接口/数据库证据一致。
- BDD: 列表进入详情 -> Given 目标执行记录在列表中可见 / When 用户点击列表行“详情” / Then 前端进入 `/mes/pro/feedback/edhr-execution/detail?id=<executionId>&fromList=1`，详情页展示同一执行编号与只读/关闭态证据。
- BDD: 缺少真实前置即阻塞 -> Given 缺少测试租户密码、真实执行记录、归档、权限或前端入口 / When 执行 E2E / Then 脚本写入 `BLOCKED/FAIL` 证据并退出非零，不使用模拟数据、API-only 或测试专用 UI。

## GREEN

- GREEN: `pnpm e2e:edhr:execution-list` -> PASS, 真实执行列表查询、最新归档状态、归档下载和详情跳转已完成。
- 执行列表目标记录与最新归档可见 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-execution-list\01-execution-list.png`, archiveStatus=SEALED, archiveVersion=1, artifactType=PDF, archiveSha256=6fc3dd7ad0649ed4dbc206a6c3c76857699ef7454eb57f378f5df3d688246a26, downloadedSha256=--
- 执行列表归档下载 SHA-256 校验 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-execution-list\02-execution-list-downloaded.png`, archiveStatus=SEALED, archiveVersion=1, artifactType=PDF, archiveSha256=6fc3dd7ad0649ed4dbc206a6c3c76857699ef7454eb57f378f5df3d688246a26, downloadedSha256=6fc3dd7ad0649ed4dbc206a6c3c76857699ef7454eb57f378f5df3d688246a26
- 执行列表进入详情并保持同一执行编号 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-execution-list\03-execution-detail-from-list.png`
- Trace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-execution-list\trace.zip`

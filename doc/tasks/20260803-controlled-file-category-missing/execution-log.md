# Execution Log

## User Intent

用户反馈：受控文件提交页选择“文件分类”时报错 `Controlled file category does not exist`。截图显示页面为“受控文件提交”，提交范围中已选 DCC 项目，文件分类路径显示为“技术文档 / 设计和开发策划阶段 / 注册和临床路径分析报告”，随后文件类别下拉等待选择。

2026-08-03 补充需求：用户确认“技术文档 / 设计和开发输入阶段 / 专利检索与分析报告”中的叶子节点“专利检索与分析报告”就是业务上的文件类别；页面“文件类别”应自动取文件分类叶子节点，只显示，不让用户填写。

2026-08-03 追加需求：`当前文件类别未绑定提交目录，请先在 DCC 文件类别维护目录绑定` 也不应要求用户选择或维护；系统先自动放到“未分类”文件夹下。

## BDD

- BDD: 受控文件提交选择文件分类 -> Given 用户在受控文件提交页选择一个正式存在的文件分类 When 继续选择文件类别或提交范围 Then 前端发送的分类标识必须能被后端正式分类表识别，页面不得出现 `Controlled file category does not exist`。
- BDD: 文件分类切换不触发历史名称预加载 -> Given 用户在受控文件提交页已选择 DCC 项目 When 用户只切换“文件分类”taxonomy 且尚未操作“文件名称”下拉 Then 页面不得调用历史文件名称辅助接口并弹出 `Controlled file category does not exist`；历史名称只在用户聚焦/查询文件名称时按需加载。
- BDD: 文件类别只读显示叶子节点 -> Given 用户选择“技术文档 / 设计和开发输入阶段 / 专利检索与分析报告” When 页面展示“文件类别” Then 文件类别显示“专利检索与分析报告”，不可下拉、不可输入，正式 DCC `categoryId` 由该叶子节点唯一绑定的可上传类别自动解析。
- BDD: 文件类别未绑定提交目录 -> Given 用户选择的文件分类叶子节点唯一解析到一个可上传正式 DCC 类别且该类别未绑定提交目录 When 页面加载提交目录或提交文件 Then 后端自动使用正式 `UNCLASSIFIED / 未分类` 目录，前端只显示自动落位提示，不要求用户选择文件类别或维护目录绑定。
- BDD: 文件分类缺正式类别绑定 -> Given 用户选择的文件分类叶子节点没有唯一可上传正式 DCC 类别 When 用户准备上传或提交 Then 页面明确提示该文件分类尚未配置唯一可上传文件类别，不用其它分类、空值或 taxonomy id 替代。

## Command And Evidence Log

- 2026-08-03 用户补充截图：页面在“受控文件提交”中选择 `文件分类=技术文档 / 设计和开发策划阶段 / 技术调研报告` 后，`文件类别` 下拉为空并提示当前分类暂无绑定类别，同时页面右上角 toast 仍显示 `Controlled file category does not exist`。
- Root cause refinement: 第一轮修复已清理 stale `categoryId` 并按 taxonomy 过滤 DCC 正式类别，但 `handleFileTypeTaxonomyChange()` 仍会立即调用 `refreshUploadNameOptionsForProjectTaxonomy()`，触发辅助历史文件名称接口。该接口与当前“文件类别”选择无关，且在用户只切换文件分类时不应弹全局 category 错误。
- Read task-closeout rules: `Get-Content -Raw -Encoding UTF8 docs\task-closeout-rules.md` -> PASS。
- Read PowerShell encoding rules: `Get-Content -Raw -Encoding UTF8 docs\powershell-encoding.md` -> PASS。
- Read frontend/backend trigger rules: `Get-Content -Raw -Encoding UTF8 docs\frontend-development.md`, `Get-Content -Raw -Encoding UTF8 docs\backend-development.md` -> PASS，前端规则输出较长，后续按命中关键词读取精确门禁段落。
- Read experience index: `Get-Content -Raw -Encoding UTF8 docs\experience-index.md` -> PASS，命中 DCC 文件类别规则、DCC 上传类别权限、Element Plus 下拉选择相关门禁。
- Initial git status: `git status --short --branch` -> branch `int_main` ahead 1；存在未跟踪 `doc/tasks/20260803-edhr-batch-execution-record-config-missing/`，本任务不得混入无关文件。
- Root cause isolation: 上传页同时存在“文件分类”`fileTypeTaxonomyId` 和“文件类别”`categoryId`；后端 `getUploadDirectoryTree(categoryId)` 会校验 DCC 正式类别。旧前端允许跨 taxonomy 保留或选择类别，可能把非当前正式类别链路的 ID 传入后端，触发 `Controlled file category does not exist`。
- Implementation evidence: `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` 已按当前 taxonomy 分支过滤 `availableCategories`，并在 `handleFileTypeTaxonomyChange()` 中清空 `categoryId`、目录上下文和上传预览状态。
- Concurrent commit evidence: `git show --name-status --oneline --stat -4 -- <task paths>` 显示本任务实现已被共享分支基线提交纳入，包括 `f6e580dc3`、`7368660b6`、`ee95cf977`、`26284e3d8`；后续不能伪装为独立任务实现提交。
- Bug evidence validation: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260803-controlled-file-category-missing\bug-regression-evidence.md` -> PASS。
- Cleanup preview/apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-controlled-file-category-missing --mode preview` -> PASS，keep 4、delete none、blocked none；`--mode apply` -> PASS，deleted_paths none。
- Project experience consolidation: 已合并到 `docs\frontend-development.md#DCC 上传类别权限投影门禁`，并同步 `docs\experience-index.md` 关键词；未新建长期经验文档。
- Project experience consolidation update: 本轮将“未绑定提交目录 -> 后端正式 `UNCLASSIFIED / 未分类` 目录 + `defaultUnclassified` 明示 + 缺失 fail-fast”补入同一 DCC 上传类别权限投影门禁，并用 `rg defaultUnclassified docs\experience-index.md docs\frontend-development.md` 验证可定位。
- Continue closeout attempt: 用户要求“继续”后复查 `git status --short --branch --untracked-files=all` -> `int_main...origin/int_main [ahead 6]`，并发现更多非本任务脏改动和另一 DCC 任务目录改动；`git diff --name-status -- doc/tasks/20260803-controlled-file-category-missing docs/frontend-development.md docs/experience-index.md` 仅列出本任务收尾/经验文件。
- User scope update: 用户确认未绑定提交目录时也不要用户选择或维护，先自动放入“未分类”文件夹。
- Implementation evidence: 新增 `DccUploadDirectoryResolver`，查询目录树和提交服务共用唯一启用 `UNCLASSIFIED` 目录解析；`DccControlledFileUploadDirectoryTreeRespVO` 增加 `defaultUnclassified`；前端取消 `directoryId` 对可上传类别的过滤和校验阻塞，并展示“系统将自动提交到未分类目录”。
- SQL evidence: 新增 `20260803_dcc_unclassified_upload_directory_seed.sql`，使用 ASCII-safe hex 写入 `未分类`，非破坏性 `NOT EXISTS` seed，并对表缺失、重复启用、插入不完整 fail-fast。

## RED

- RED: `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` -> FAIL, expected reason: 新增静态契约要求存在 `selectedFileTypeTaxonomyCategoryIds`、按 taxonomy 分支过滤类别，并在文件分类切换时清空旧 `categoryId`；修复前上传页缺少该约束。
- RED: 待运行 `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js`，expected reason: 本轮新增契约要求受控文件上传页“文件类别”只读显示 `selectedFileTypeTaxonomyLeafName`，正式 `categoryId` 只从当前叶子节点唯一绑定的可上传类别自动解析；当前旧页面仍显示可手选 `el-select`。
- RED: `node tests/e2e/dcc-upload-category-permission-static.spec.js` -> FAIL, expected reason: 旧 `availableCategories` 仍过滤 `Boolean(category.directoryId)`，未绑定目录的可上传类别无法自动落位未分类。
- RED: `node tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js` -> FAIL, expected reason: 上传页缺少 `未分类目录/defaultUnclassified` 契约。
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_unclassified_upload_directory_seed_sql.py -q` -> FAIL, expected reason: 缺少 `20260803_dcc_unclassified_upload_directory_seed.sql`。
- RED: targeted Maven -> FAIL, expected reason: 缺少 `FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS` 和 `DccControlledFileUploadDirectoryTreeRespVO.defaultUnclassified`。

## GREEN

- GREEN: `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` -> PASS，补充断言文件分类切换不得主动调用 `refreshUploadNameOptionsForProjectTaxonomy()`。
- GREEN: `node tests/e2e/dcc-upload-name-version-autofill-static.spec.js` -> PASS，补充断言历史文件名称候选由 `ensureUploadNameOptionsLoaded()` 在用户查询“文件名称”时按需加载。
- GREEN: `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-category-permission-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-name-version-autofill-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-product-autofill-static.spec.js` -> PASS。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_unclassified_upload_directory_seed_sql.py -q` -> PASS。
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260513_dcc_base_schema.sql --sql-file IntRuoyiBackend\sql\mysql\20260803_dcc_unclassified_upload_directory_seed.sql --output doc\tasks\20260803-controlled-file-category-missing\migration-policy-gate-unclassified.json` -> PASS。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getUploadDirectoryTree_categoryWithoutBindingReturnsUnclassifiedDirectory,DccControlledFileWorkflowServiceImplTest#submitControlledFile_categoryWithoutDirectoryBindingUsesUnclassifiedDirectory" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: heartbeat Maven `DccControlledFileQueryServiceTest#getUploadDirectoryTree_categoryWithoutBindingAndUnclassifiedMissingFailsFast` -> PASS。
- GREEN: heartbeat Maven `DccControlledFileWorkflowServiceImplTest#submitControlledFile_bindingMissingAndUnclassifiedDirectoryMissing_throwsNotExists` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260803-controlled-file-category-missing\bug-regression-evidence.md` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue IntRuoyiFronted/tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js IntRuoyiFronted/tests/e2e/dcc-upload-name-version-autofill-static.spec.js doc/tasks/20260803-controlled-file-category-missing` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue IntRuoyiFronted/package.json IntRuoyiFronted/tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js IntRuoyiFronted/tests/e2e/dcc-upload-category-permission-static.spec.js doc/tasks/20260803-controlled-file-category-missing` -> PASS。
- GREEN: `git diff --check -- docs/frontend-development.md docs/experience-index.md doc/tasks/20260803-controlled-file-category-missing` -> PASS，仅出现 CRLF 提示。
- Real E2E preflight: `npx --version` -> `11.6.2`；`Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8081/` -> `FRONTEND_STATUS=200`；`Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `BACKEND_STATUS=UP`。
- Real E2E prerequisite repair: `node tests\e2e\dcc-upload-category-leaf-real.e2e.js` 首次失败，因为 Playwright Chromium 缺失；`npx playwright install chromium` -> PASS，下载到 `E:\Int\DevCache\playwright-browsers\chromium_headless_shell-1223`。
- Real E2E script check: `node --check tests\e2e\dcc-upload-category-leaf-real.e2e.js` -> PASS。
- Real E2E: `node tests\e2e\dcc-upload-category-leaf-real.e2e.js` -> BLOCKED，真实浏览器登录 `芋道源码/admin`，打开 `/dcc/controlled-file/upload`，选择真实未绑定目录分类 `技术文档 / 设计和开发策划阶段 / 技术调研报告` 后，`/admin-api/dcc/controlled-files/upload-directory-tree?categoryId=907212` 返回旧错误 `1080000007 File category is not bound to a directory`。证据：`output\playwright\20260803-controlled-file-category-missing\dcc-upload-category-leaf-real-evidence.json`，截图：`output\playwright\20260803-controlled-file-category-missing\dcc-upload-category-leaf-real.png`。
- Real E2E runtime diagnosis: `48081` PID `42064` 运行 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-int-main-6f5f52814.jar`；只读检查内嵌 `BOOT-INF/lib/yudao-module-dcc-2026.04-SNAPSHOT.jar`，`DccUploadDirectoryResolver.class=False`；本地 `yudao-module-dcc\target\classes` 中 `DccUploadDirectoryResolver.class` 存在。因此当前 E2E 阻塞在本机运行态未加载后端修复，不是 API-only 或脚本未执行。
- Isolated runtime setup: 创建隔离 worktree `D:\IntRuoyiWorktree\controlled-file-category-e2e-20260803`，使用 profile `int_main` slot `18`，前端端口 `8099`、后端端口 `48099`；迁移本任务 DCC 修复源码、SQL seed、测试与 E2E 脚本，避免从脏主工作区重建共享 `48081`。
- GREEN: `mvn.cmd -pl yudao-server -am "-DskipTests" package` in isolated worktree -> PASS，`yudao-server-exec.jar` 构建成功。
- Runtime Jar inspection: `yudao-server-exec.jar` SHA256 `4f3def41fe02d7b0d565e272821fc26fb00d58fdbd1d5cdbb6342e8f4bd5ca04`；内嵌 `BOOT-INF/lib/yudao-module-dcc-2026.04-SNAPSHOT.jar` 包含 `cn/iocoder/yudao/module/dcc/service/file/DccUploadDirectoryResolver.class` 和 `DccControlledFileUploadDirectoryTreeRespVO`。
- Runtime startup: 后端从稳定副本 `D:\IntRuoyiWorktree\controlled-file-category-e2e-20260803\output\runtime\int_main_slot18\backend-runtime-control-20260803-controlled-file-category.jar` 启动，PID `61288`，`http://127.0.0.1:48099/actuator/health` -> `UP`，运行 Jar 修改时间早于进程启动时间。
- Frontend dependency install: `pnpm install --frozen-lockfile` in isolated frontend -> PASS，未改 `package.json` 或 `pnpm-lock.yaml`；`pnpm exec vite --version` -> `vite/5.1.4 win32-x64 node-v24.12.0`。
- Frontend startup: isolated Vite on `http://127.0.0.1:8099/` -> HTTP `200`，代理后端 `http://127.0.0.1:48099`。
- Real E2E prerequisite data: first isolated E2E run returned `1080000196 Unclassified upload directory does not exist`，确认代码已进入新 fail-fast 分支；schema check `DESCRIBE dcc_file_directory` -> PASS；执行本任务幂等 seed `20260803_dcc_unclassified_upload_directory_seed.sql` against local Docker MySQL `int-ruoyi-mysql` -> PASS，active `UNCLASSIFIED` rows created for tenants `0/1/122` with `HEX(name)=E69CAAE58886E7B1BB`。
- GREEN: `DCC_UPLOAD_CATEGORY_LEAF_E2E_BASE_URL=http://127.0.0.1:8099 node tests\e2e\dcc-upload-category-leaf-real.e2e.js` -> PASS。真实浏览器登录 `芋道源码/admin`，进入 `/dcc/controlled-file/upload`，选择真实未绑定目录候选 `技术文档 / 设计和开发策划阶段 / 技术调研报告`，接口返回 `bindingDirectoryPath=未分类`、`defaultUnclassified=true`；页面文件类别只读显示 taxonomy 叶子节点，提交目录显示未分类自动落位提示；无 DCC 写请求、无 target network failures、无 console/page errors。证据：`D:\IntRuoyiWorktree\controlled-file-category-e2e-20260803\output\playwright\20260803-controlled-file-category-missing\dcc-upload-category-leaf-real-evidence.json`，截图：`D:\IntRuoyiWorktree\controlled-file-category-e2e-20260803\output\playwright\20260803-controlled-file-category-missing\dcc-upload-category-leaf-real.png`。
- Project experience consolidation: 已合并到 `docs\frontend-development.md#DCC 上传类别权限投影门禁` 与 `docs\experience-index.md`，新增 `1080000196 Unclassified upload directory does not exist` / 未分类目录 seed 缺失时的正式处理规则：执行幂等 seed 并核对唯一 active `UNCLASSIFIED / 未分类`，不得改代码绕过。
- Cleanup: 已精确停止本任务自有隔离进程 PID `2320`（Vite `8099`）和 PID `61288`（Java `48099`）；`Get-NetTCPConnection` 复查 `8099/48099` listener 数均为 `0`。隔离 worktree、slot 18 登记和 E2E 证据保留。

## Blockers

- 当前工作区在任务开始前已 `ahead 1` 且存在未跟踪其他任务目录；后续又出现非本任务脏改动：`IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileNasTransferServiceTest.java`、`IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue`、`IntRuoyiFronted/tests/e2e/dcc-controlled-browser-ux-optimization-static.spec.js`。
- 当前分支已 `ahead 6`，且本任务实现已混入历史基线提交；在不重写历史、不提交无关改动的前提下，无法完成独立任务实现提交、最终推送和 `completed` 状态。
- 继续收尾时新增/仍存在的非本任务脏改动包括：`IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileNasTransferServiceTest.java`、`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java`、`IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue`、`IntRuoyiFronted/tests/e2e/dcc-controlled-browser-ux-optimization-static.spec.js`、`doc/tasks/20260801-role-requirement-matrix-implementation/execution-log.md`、`doc/tasks/20260803-dcc-upload-project-code-hint/*`、以及未跟踪 NAS import Controller/Test；按共享分支门禁，本任务不得宽泛暂存或提交这些文件。
- `pnpm ts:check` -> FAIL in unrelated `src/views/dcc/controlled-file/detail/index.vue`: missing `pagedRouteSnapshotRows`, `distributionStatusRows`, `pagedDistributionStatusRows`。
- Full migration policy gate over all SQL -> FAIL before this migration on unrelated `IntRuoyiBackend\sql\mysql\20260730_mes_process_pool_team_leader.sql` missing release metadata；DCC base + unclassified seed chain passed.
- Shared runtime E2E remains blocked on `48081` because the shared backend Jar does not contain the backend resolver class from this fix. The workspace currently has extensive unrelated dirty changes and branch-ahead state, so the shared backend cannot be safely rebuilt/restarted from the mixed workspace under the local runtime gate. Real E2E for this fix was completed instead on isolated slot 18 (`8099/48099`) with a verified Jar containing the fix.

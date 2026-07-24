# 任务：DCC 受控浏览页批量识别产品名称编号

## 任务目标

在 DCC 受控浏览页增加批量识别产品名称/编号能力，支持按当前浏览上下文创建服务端异步识别任务，并显示实时进度、覆盖策略和最终结果。

## 当前状态

BLOCKED

## Current Status

BLOCKED

## 上一任务检查

- 上一个 backend 任务：`D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\doc\tasks\20260623-dcc-short-code-recognition-hardening\task.md`
- 状态：`COMPLETED`
- 处理：短编码文件名识别硬化与前端代理超时修复已收口，本任务基于其结果继续实现批量识别。

## 经验门禁

- 已读取：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- 本任务适用强制门禁：
  - 前后端成对分支与 worktree 已建立，后续只在当前 task worktree 修改。
  - 不得新增 fallback、静默吞错或浏览器端逐条同步识别伪装后台任务。
  - 批量识别默认不覆盖已有值，只有用户显式勾选才允许重写。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## BDD 场景

- `BDD: 浏览页可创建批量识别任务 -> Given 文控角色进入 DCC 受控浏览页 / When 点击批量识别按钮并确认当前范围与覆盖策略 / Then 后端创建一个异步批量识别任务并返回初始进度。`
- `BDD: 当前目录模式扫描目录及全部子目录 -> Given 当前范围为当前目录且已选中目录 / When 创建任务 / Then 候选文件集合必须只来自该目录及其子目录，并遵循当前筛选条件但忽略分页。`
- `BDD: 默认不覆盖已有值 -> Given 某文件已存在 dccProjectCodeId、productCode 或 productName / When 用户未勾选覆盖已有值 / Then 该文件直接计入跳过数量，不进入识别调用。`
- `BDD: 识别成功完整同步写回 -> Given 某文件识别命中 DCC 基础数据 / When 后端完成识别 / Then 必须同步写回 fileName、title、productName、productCode、dccProjectCodeId 与主表 file_name。`
- `BDD: 进度弹窗显示真实统计 -> Given 批量任务运行中 / When 前端轮询任务状态 / Then 必须显示总数、已处理、成功、失败、跳过、剩余、当前状态与最后错误。`

## 里程碑

1. 建立任务台账并补齐前后端 RED 契约。`COMPLETED`
2. 实现后端批量识别任务、接口与持久化。`COMPLETED`
3. 实现前端浏览页按钮、弹窗与进度轮询。`COMPLETED`
4. 跑通前后端定向回归并补齐证据。`COMPLETED`

## 预期验证

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-batch-recognition-browser\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileBatchRecognition*Test,DccControlledFileProjectCodeRecognitionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-batch-recognition-browser\doc\tasks\20260623-dcc-browser-batch-recognition\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-batch-recognition-browser\doc\tasks\20260623-dcc-browser-batch-recognition\frontend-feature-evidence.md`

## 本地完成结果

- 后端已新增批量识别任务建单接口、状态查询接口、任务表、调度器与串行执行服务。
- 查询候选集复用了浏览页现有“最新版本 + 权限 + 范围 + 过滤条件”语义，并忽略分页。
- 前端浏览页已接入任务确认、进度轮询和终态刷新，完成本地静态验证与类型检查。

## 本地核验

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-batch-recognition-browser\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileBatchRecognitionControllerTest,DccControlledFileBatchRecognitionServiceTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `node tests/e2e/dcc-browser-batch-recognition-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyiWorktrees\yudao-ui-admin-vue3-dcc-batch-recognition-browser\doc\tasks\20260623-dcc-browser-batch-recognition\verify-dcc-browser-batch-recognition-admin.e2e.mjs` -> PASS，真实目录样本 `质量管理/3.DMR/10.产品技术要求` 触发批量任务 `taskId=1`，`success=7 / failed=17 / skipped=0`
- `docker exec int-ruoyi-mysql mysql -uroot -p123456 ... SELECT ... FROM dcc_controlled_file ...` -> PASS，确认成功样本已写回 `dcc_project_code_id / product_name / product_code / project_code_recognition_type`

## 剩余阻塞

- 还未把本地前后端改动与 SQL 迁移发布到测试服务器，也未执行真实浏览页联调。
- 当前前端 worktree 因坏锁文件前置问题产生 `pnpm-lock.yaml` diff；若进入提交/发布阶段，需要先明确是否纳入本次提交。
- 本地样本目录仍有 `17` 条记录因源文件对象缺失返回 `S3 404 The specified key does not exist`；进入测试服务器前需确认是否接受该样本数据问题，或先换目录/补文件验证。
- 2026-06-24 测试服已完成真实部署与浏览页联调，但仍未全量放行：
  - `release-20260623-dcc-batch-recognition-test-v1` 证明原始测试服仍回退 `cmd.exe /c codex.cmd`
  - `release-20260624-dcc-batch-recognition-codex-v2` 已修复 Linux Codex 启动链路，但同一 `33` 条目录样本仍 `33/33` 失败，现阶段阻塞为 `Codex CLI timed out after 120 seconds` / `returned no DCC basic-data match`

## 阻塞影响

- 当前任务已具备“浏览页批量识别入口 + 异步任务 + 进度统计 + Linux Codex 启动链路”本地与测试服链路证据，但测试服真实内容识别仍被 `Codex CLI timed out after 120 seconds` 与 `returned no DCC basic-data match` 阻塞，未满足完整联调收口条件。
- 在该阻塞解除前，后端仓不能继续把本任务视为 `IN_PROGRESS` 的可直接收尾状态；本轮按仓库规则显式转为 `BLOCKED`，避免阻塞后续 DCC 任务启动。

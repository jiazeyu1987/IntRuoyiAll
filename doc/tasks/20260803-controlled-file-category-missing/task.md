# 受控文件提交选择文件分类报错修复

## Task Goal

修复受控文件提交页在选择文件分类时出现 `Controlled file category does not exist` 的问题，并按用户确认将“文件类别”改为自动显示文件分类叶子节点，不再允许用户手工选择。若正式 DCC 文件类别未绑定提交目录，系统按用户要求自动落位到正式 `UNCLASSIFIED / 未分类` 目录；该目录缺失或不唯一时必须 fail-fast。后端提交仍必须使用正式 DCC 类别 `categoryId`，不能把 taxonomy id 当作 category id，不引入默认成功或吞异常。

## Milestones

- [x] 定位受控文件提交页文件分类选择前后端链路和错误来源。
- [x] 先补充可复现的回归测试或静态契约，记录 RED 失败。
- [x] 实施最小正式修复，保持文件分类数据契约清晰。
- [x] 运行定向 GREEN 与相关回归验证。
- [x] 更新任务证据、收尾状态和最终验证结论。
- [x] 按用户确认将文件类别改为只读叶子节点显示，并自动解析唯一正式 DCC 类别。
- [x] 将未绑定提交目录的正式类别自动落位到正式 `UNCLASSIFIED / 未分类` 目录，并保留缺失/重复目录 fail-fast。
- [x] 补充 RED/GREEN 静态契约、后端单测和 SQL 种子契约。
- [x] 执行真实 Playwright E2E，并记录共享运行态未加载后端修复的 blocker。
- [x] 创建隔离 worktree 运行态，刷新后端 Jar 与前端依赖后完成真实 Playwright E2E 复验。

## Expected Verification

- BDD 场景记录在 `execution-log.md`。
- RED/GREEN 证据覆盖“选择文件分类不会把不存在分类 ID 提交给后端”。
- RED/GREEN 证据覆盖“文件类别只读显示文件分类叶子节点，不再出现可手选下拉”。
- RED/GREEN 证据覆盖“前端只在叶子节点唯一绑定可上传正式类别时自动写入 `categoryId` 并加载提交目录；缺失或多绑定时明确阻塞”。
- RED/GREEN 证据覆盖“文件类别缺少提交目录绑定时，后端返回/提交到正式 `UNCLASSIFIED / 未分类` 目录；该目录缺失时明确失败”。
- 定向前端/后端契约或单元测试通过。
- 如本地运行态前置齐备，再通过真实页面路径验证；若缺少运行态、账号或数据，按项目规则记录 blocker，不用 API-only 冒充 E2E。

## 适用经验门禁

- DCC 文件类别规则种子门禁：分类匹配和分类 ID 必须来自正式规则或分类数据，不得直接 SQL 修业务文件、不用空值或默认分类掩盖配置缺失。
- DCC 上传类别权限投影门禁：上传/提交类别下拉必须投影当前用户可用类别；接口失败需要明确暴露，禁止静默降级。
- Element Plus 下拉选择门禁：下拉选择验证要按真实 DOM 和当前显示值核对，不能只断言 API 包装层。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；“未绑定提交目录 -> 未分类目录”是用户明确要求的正式落位规则，依赖 seeded `UNCLASSIFIED` 目录，目录缺失/重复时仍 fail-fast。
- `是否从根因和长期维护角度解决`：是；从分类选择契约和后端校验链路定位。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

本轮实现已完成：文件类别只读取文件分类叶子节点；正式 `categoryId` 自动解析；未绑定提交目录的类别通过后端正式解析到 `UNCLASSIFIED / 未分类` 目录，并新增 SQL seed 与 fail-fast 单测。定向前端静态契约、SQL 契约、迁移链门禁和后端方法级测试通过。

E2E PASS：真实 Playwright E2E 已在隔离 worktree `D:\IntRuoyiWorktree\controlled-file-category-e2e-20260803` 完成复验。后端使用 slot 18 的 `48099`，前端使用 `8099`，构建 Jar SHA256 为 `4f3def41fe02d7b0d565e272821fc26fb00d58fdbd1d5cdbb6342e8f4bd5ca04`，内嵌 DCC 模块包含 `DccUploadDirectoryResolver.class`。本地测试库已执行幂等 `20260803_dcc_unclassified_upload_directory_seed.sql`，生成唯一 active `UNCLASSIFIED / 未分类` 目录。E2E 证据显示真实页面选择未绑定目录的文件分类叶子节点后，目录树返回 `defaultUnclassified=true`、`bindingDirectoryPath=未分类`，页面没有旧阻塞文案，且无 DCC 写请求、无目标网络失败、无 console/page error。证据在 `D:\IntRuoyiWorktree\controlled-file-category-e2e-20260803\output\playwright\20260803-controlled-file-category-missing\dcc-upload-category-leaf-real-evidence.json`。

未标记 `completed`：共享 `48081` 运行态仍是旧 Jar 且主工作区存在多项非本任务脏改动和分支 behind 状态，不能安全重建共享后端、提交或推送；`pnpm ts:check` 当前失败在无关详情页 `src/views/dcc/controlled-file/detail/index.vue` 缺少 `pagedRouteSnapshotRows`、`distributionStatusRows`、`pagedDistributionStatusRows`；全量迁移门禁失败在无关历史 SQL `20260730_mes_process_pool_team_leader.sql` 缺少 release metadata；隔离 worktree、slot 18 登记和 E2E 证据文件保留用于复查，`8099/48099` 任务自有进程已停止并释放端口。

## Cleanup Keep

- doc/tasks/20260803-controlled-file-category-missing/bug-regression-evidence.md
- doc/tasks/20260803-controlled-file-category-missing/frontend-feature-evidence.md
- doc/tasks/20260803-controlled-file-category-missing/backend-api-evidence.md
- doc/tasks/20260803-controlled-file-category-missing/database-schema-evidence.md
- doc/tasks/20260803-controlled-file-category-missing/migration-policy-gate-unclassified.json

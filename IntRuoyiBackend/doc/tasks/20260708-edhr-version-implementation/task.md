# eDHR 批记录版本升版审批开发

## 任务目标

- 在 `edhr_version` worktree 内按 PRD、开发文档、测试文档实现批记录 Word 导入升版审批能力。
- 按“一期 -> 二期 -> 三期”顺序推进，不允许跳过一期 P0 数据契约和测试门禁。
- 本地写入型开发和 E2E 使用本机测试租户 `测试租户/aoteman`；最终只读复验使用本机 `芋道源码/admin`。
- 不访问、不修改测试服、正式服或外部服务器。
- 完成后合并进入前后端 `int_main`，通过合并后验证，再删除 `edhr_version` worktree。

## 里程碑

- [x] 阶段 0：建立 worktree 门禁、运行态台账、BDD/TDD 计划和经验预检。
- [x] 阶段 1：安全升版最小闭环，后端 P0 定向回归、前端导入审批入口契约、测试租户真实 E2E、迁移 SQL 与分角色审批链路已通过。
- [x] 阶段 2：迁移体验增强，包含结构化 diff、`CONFIRM_REQUIRED` 授权确认、草稿重传体验和真实 E2E。
- [x] 阶段 3：扩展治理能力，包含附加表单槽位版本化、受控回滚、影响面分析、治理看板和真实 E2E。
- [x] 主控 review：逐项核对 PRD、开发文档、测试文档和真实 E2E 证据，不符合即退回修改。
- [x] 合并与清理：提交、合并前后端 `int_main`、合并后复验、清理 `edhr_version` worktree。

## 预期验证

- 严格 BDD/TDD：每个功能点先有失败测试，再有最小实现和回归证据。
- 后端测试覆盖版本主数据、P0 数据契约、迁移证据、审批幂等、并发导入和并发审批。
- 前端测试覆盖导入预检、版本列表、审批状态、差异展示、人工确认、治理页面。
- 每个功能点都有真实 Playwright E2E，先在本机测试租户写入验证，再用本机 `芋道源码/admin` 只读验证。
- 合并进入 `int_main` 后重新运行相关验证，失败不得清理 worktree。

## 运行态台账

| worktree | task | frontend | backend | fe_port | be_port | base_url | actuator | db | redis | tenant | status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| edhr_version | 20260708-edhr-version-implementation | `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version\yudao-ui-admin-vue3` | `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version\ruoyi-vue-pro` | 8096 | 48096 | `http://127.0.0.1:8096` | `http://127.0.0.1:48096/actuator/health` | `127.0.0.1:23306/ruoyi-vue-pro` | `127.0.0.1:26379/0` | 测试租户写入，芋道源码只读复验 | task_scope_pass_merge_blocked |

## 经验门禁

- PowerShell / Windows shell / 中文编码：已在主控启动阶段读取 `docs/powershell-memory.md`；后续中文文本写入优先 `apply_patch`，PowerShell 命令显式 UTF-8。
- worktree / 分支 / 合并 / 清理：已读取 `docs/worktree-memory.md`；前后端成对分支 `codex/edhr_version`，路径为标准 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version\...`。
- 登录 / 租户 / 真实 E2E：已读取 `docs/login-access.md`；写入型 E2E 使用 `测试租户/aoteman/111111`，最终 `芋道源码/admin` 只读复验。
- 批记录 Word 表单识别：后续实现前必须读取并应用 `docs/experience/batch-record-form-recognition.md`，保留表格结构、单元格规则、签名位和视觉网格证据。
- 高风险动作：真实 E2E、合并、清理前必须在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS` 或 `BLOCKER: experience-preflight -> <原因>`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。迁移失败、权限失败、版本契约缺失必须 fail fast。
- 是否从根因和长期维护角度解决：是。以不可变版本快照、当前版本指针、审批门禁、版本引用和迁移证据解决根因。
- 是否存在临时补丁或绕过：否。不得通过 mock、接口绕过、默认值、静默跳过或切换租户掩盖问题。

## 当前状态

- completed: 阶段一、阶段二、阶段三真实 E2E 与任务范围门禁均已通过；后端已融合进 `int_main`，合并后任务范围复验通过；`edhr_version` worktree 已进入清理。


## 阶段一进展

- 已实现批记录定义/版本快照/current_version_id 唯一事实源的后端 P0 契约。
- 已实现执行任务 batch_record_version_id、路线用途绑定 batch_record_version_id、新执行快照 definition_id + version_id + report_id + route_id + route_process_id 的定向回归。
- 已实现新旧 reportId/routeId/routeProcessId 隔离、同 hash 幂等、迁移阻断、拒绝不切换、审批回调幂等、并发审批定向回归。
- 已补前端导入页一期最小闭环：导入后展示版本号/版本ID/状态，提供“提交升版审批”入口，并调用真实后端审批提交接口。
- 已完成本地运行态归属校验：后端 http://127.0.0.1:48096/actuator/health 返回 UP，前端 http://127.0.0.1:8096 返回 200，进程指向 edhr_version worktree。
- 已完成测试租户真实 E2E：`测试租户/aoteman` 导入 Word 并提交升版审批，`测试租户/smokeappr1` 审批通过并重复回调幂等；最新复验输出 definitionId=11、versionId=11、versionNo=V1.0、routeId=922072、approvalInstanceId=BRV-11-1783519172627。
- 已完成本机 SQL 迁移应用和权限基线验证：`phase-one-sql-apply -> PASS statements=43`，`approval-permission-baseline -> PASS user=smokeappr1 tenant=122 count=1`。
- BLOCKER: `芋道源码/admin/111111` 官方登录预检失败，接口返回账号密码不正确；按门禁未猜测密码、未切换账号、未修改 admin 租户数据，最终只读复验暂不能完成。

## 阶段二 / 阶段三 review 修复进展

- 已修复 `CONFIRM_REQUIRED` 门禁口径：审批和巡检阻断统一为 `BLOCKER + 未确认 CONFIRM_REQUIRED`。
- 已修复草稿重传契约：接口改为真实 multipart Word 文件 + productNames + remark，后端复用 `recognizeUploadedRoute`，前端使用 `FormData` 上传，禁止手工 SHA/文件名伪重传。
- 已补阶段三统一变更三张表结构和测试资源清理顺序。
- 已补版本治理菜单权限 SQL：页面权限 `mes:pro-batch-record-version:governance-query`，按钮权限 `confirm / import / rollback-request`。
- 已完成阶段三测试租户真实受控回滚写入：definitionId=12 当前 V2.0，targetVersionId=15/V1.0，生成 `EDHR-CHANGE-20260709012217`，状态 `DRAFT`，事件 `CREATE -> DRAFT`。
- BLOCKER: `芋道源码/admin/111111` 官方登录预检仍失败，无法执行最终只读复验和写请求断言；按门禁当前暂停合并与清理。

## 最终验证与融合门禁 - 2026-07-09

- GREEN: admin-readonly-login-admin123 -> 用户提供本机 `芋道源码/admin` 密码 `admin123` 后，官方登录预检通过。
- GREEN: phase2-real-e2e -> 测试租户真实页面导入 V1、同名升级 V2、治理页真实 Word 草稿重传 V3、授权确认 `CONFIRM_REQUIRED`，并用 `芋道源码/admin` 验证治理写请求数 `0`。
- GREEN: phase3-admin-readonly-e2e -> 测试租户真实页面创建受控回滚申请，`芋道源码/admin` 登录同一路径并验证治理写请求数 `0`。
- GREEN: backend-task-commit -> `codex/edhr_version` 后端提交 `e952894902`。
- GREEN: frontend-task-commit -> `codex/edhr_version` 前端提交 `0cafa8c56`。
- BLOCKER: frontend-int-main-merge-tree -> `git merge-tree --write-tree int_main codex/edhr_version` 退出码 `1`，冲突文件 `src/api/mes/pro/scheduleorder/index.ts`。
- BLOCKER: frontend-main-dirty-overlap -> 主工作区前端脏改 `89` 个文件，与本任务重叠 `2` 个：`src/api/mes/pro/scheduleorder/index.ts`、`src/views/mes/pro/batchrecordtemplate/index.vue`。
- BLOCKER: backend-main-dirty-overlap -> 后端 merge-tree 无冲突，但主工作区后端脏改 `71` 个文件，与本任务重叠 `1` 个：`yudao-module-mes/src/test/resources/sql/create_tables.sql`。
- RELEASE GATE: 按 worktree 融合门禁，重叠归因/处理前不得融合 `int_main`，不得删除 `edhr_version` worktree。


## 完成收尾 - 2026-07-09 09:51:50

- GREEN: backend-int-main-merge -> PASS，merge commit `9c0fd4471e`。
- GREEN: backend-post-merge-contracts -> PASS，SQL 契约 6 tests 与 Maven 16 tests 均通过。
- GREEN: overlap-restore -> PASS，保留主工作区工艺主数据测试表字段并保留 eDHR 版本测试表结构，无冲突标记。

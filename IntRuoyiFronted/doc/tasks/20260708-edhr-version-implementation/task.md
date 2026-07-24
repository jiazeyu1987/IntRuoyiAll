# eDHR 批记录版本升版审批前端开发

## 任务目标

- 在 `edhr_version` 前端 worktree 内实现批记录 Word 升版审批的页面、API、交互和真实 E2E。
- 配合后端完成“一期 / 二期 / 三期”阶段开发。
- 所有写入型 E2E 使用本机测试租户，最终 `芋道源码/admin` 只读复验。

## 里程碑

- [x] 阶段 0：建立前端任务门禁、运行态配置和 E2E 基线。
- [x] 阶段 1：导入预检、版本列表、审批状态、当前版本标识、P0 契约 E2E。
- [x] 阶段 2：结构化 diff、`CONFIRM_REQUIRED` 授权确认、草稿重传体验。
- [x] 阶段 3：附加槽位版本化、回滚审批入口、影响面分析和治理看板。
- [x] 前端真实 E2E：每个功能点均有测试租户写入用例和 `芋道源码/admin` 只读复验。

## 预期验证

- `pnpm ts:check` 或任务范围等价类型检查通过。
- 静态契约测试覆盖路由、权限、API、按钮、弹窗、状态文案和只读复验。
- Playwright E2E 使用 `http://127.0.0.1:8096`，接口命中 `http://127.0.0.1:48096`。
- admin 只读复验期间不得出现 MES 写请求。

## 经验门禁

- worktree：前端路径 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version\yudao-ui-admin-vue3`，分支 `codex/edhr_version`，不得复用主工作区 `8081/48081` 做证据。
- 登录：写入型 E2E 使用 `测试租户/aoteman/111111`；最终只读复验使用 `芋道源码/admin`。
- E2E：先跑官方登录预检，再跑业务长链路；不能用接口或 localStorage 绕过登录。
- 中文与 PowerShell：脚本和证据文件使用 UTF-8；避免 PowerShell here-string 直接传中文租户名。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。页面必须暴露真实后端错误、权限错误和迁移阻断。
- 是否从根因和长期维护角度解决：是。前端只承载状态展示和操作入口，不能用隐藏差异替代后端契约。
- 是否存在临时补丁或绕过：否。

## 当前状态

- completed: 阶段一、阶段二、阶段三真实 E2E 与任务范围门禁均已通过；前端已融合进 `int_main`，合并后任务范围复验通过；`edhr_version` worktree 已进入清理。

## 阶段三 review 修复进展

- 已补治理页真实权限入口：`governance-query / confirm / import / rollback-request`。
- 已修复真实 E2E 选择器和异步时序：等待全部治理接口返回后再填写回滚表单，并断言实际 POST `targetVersionId=15`。
- 已完成测试租户真实写入段：`pnpm e2e:edhr:version-governance` 通过页面创建 `EDHR-CHANGE-20260709012217`，状态 `DRAFT`，事件 `CREATE -> DRAFT`。
- RESOLVED: 用户提供本机 `芋道源码/admin` 密码后，官方登录预检与治理页 admin 只读网络写请求断言均已通过。

## Phase 2 真实 E2E 恢复 - 2026-07-09

- `pnpm e2e:edhr:batch-version-phase2:check` -> PASS。
- `pnpm e2e:edhr:batch-version-phase2` -> PASS，测试租户真实页面导入 V1、审批生效、同名升级 V2、治理页重传草稿生成 V3、授权确认 `CONFIRM_REQUIRED`，并用 `芋道源码/admin` 登录同一路径确认治理写请求数为 `0`。
- Evidence: `doc/tasks/20260708-edhr-version-implementation/phase2-migration-real-e2e-evidence.md`。

## 融合门禁审计 - 2026-07-09

- GREEN: frontend-task-commit -> PASS，`codex/edhr_version` 前端提交 `0cafa8c56`。
- GREEN: backend-task-commit -> PASS，`codex/edhr_version` 后端提交 `e952894902`。
- BLOCKER: frontend-int-main-merge-tree -> `git merge-tree --write-tree int_main codex/edhr_version` 退出码 `1`，冲突文件 `src/api/mes/pro/scheduleorder/index.ts`。
- BLOCKER: frontend-main-dirty-overlap -> 主工作区前端脏改 `89` 个文件，与本任务重叠 `2` 个：`src/api/mes/pro/scheduleorder/index.ts`、`src/views/mes/pro/batchrecordtemplate/index.vue`。
- BLOCKER: backend-main-dirty-overlap -> 后端 merge-tree 无冲突，但主工作区后端脏改 `71` 个文件，与本任务重叠 `1` 个：`yudao-module-mes/src/test/resources/sql/create_tables.sql`。
- RELEASE GATE: 按 worktree 融合门禁，重叠归因/处理前不得融合 `int_main`，不得删除 `edhr_version` worktree。


## 完成收尾 - 2026-07-09 09:51:50

- GREEN: frontend-int-main-merge -> PASS，merge commit `2cb8413a8`。
- GREEN: frontend-post-merge-contracts -> PASS，阶段一、阶段二、治理页三项前端任务范围契约检查通过。
- GREEN: overlap-restore -> PASS，保留主工作区 Word 路线识别改动并保留 eDHR 升版审批面板，无冲突标记。

# 当前 HEAD 测试服发布 SQL Collation 修复

## Task Goal

修复仅测试服发布中 `20260726_dcc_codex_test_items_seed.sql` 在测试服 required-sql 阶段触发的 `ERROR 1267 Illegal mix of collations`，提交正式代码修复后重新基于当前分支最新已提交 HEAD 构建并仅发布测试服。

## Milestones

- [x] 记录发布阻塞证据、BDD 场景和 RED 测试。
- [x] 修复 seed SQL 的临时表/真实表字符列 collation 比较。
- [x] 运行目标 SQL 契约测试和发布前迁移门禁。
- [ ] 提交 task-owned 实现变更。
- [ ] 创建新的 clean release worktree，构建新的 releaseTag。
- [ ] 仅执行 `publish-test`，验证测试服真实运行态和运行控制台版本变更说明。
- [ ] 沉淀可复用前置经验，完成 closeout。

## Expected Verification

- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_codex_test_items_seed.py` 先因 collation 断言失败，再在修复后通过。
- SQL 中 `tmp_dcc_codex_test_case_seed.name`、`tmp_dcc_codex_test_checkpoint_seed.case_name`、`tmp_dcc_codex_test_checkpoint_seed.name` 的 collation 与目标表 `system_codex_test_case.name` / `system_codex_test_checkpoint.name` 一致或比较显式对齐。
- release migration policy gate 通过，且构建 manifest 中 sourceRepos 指向新的已提交 HEAD、dirty=false。
- 新 releaseTag 的 `build-release -> publish-test` 仅测试服链路成功；不执行 `mark-tested`、`promote-prod`、`promote-backup`。
- 测试服 `.env IMAGE_TAG`、backend/frontend 实际镜像 tag、容器状态、backend health、frontend HTTP 200、前端页面、release-info、运行控制台版本号和变更说明全部通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复 required-sql 与真实目标列 collation 不一致的根因，并补契约测试阻止复发。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### 数据修复临时表排序规则门禁

- Trigger: 数据修复、测试项种子、菜单/权限补齐等 SQL 使用临时表、字面量或用户变量与真实表字符列做 `JOIN`、`=`、`NOT EXISTS` 比较，尤其包含中文名称、权限字符串、表单名称、测试项名称。
- Preflight check: 写入前用 `information_schema.COLUMNS` 核对目标字符列 `COLLATION_NAME`；临时表字符串列必须声明与目标列一致的 `CHARACTER SET` 和 `COLLATE`，或在比较表达式上显式 `COLLATE` 到目标列排序规则。
- Blocker: MySQL 报 `ERROR 1267 Illegal mix of collations`，或发现临时字符串列与目标字符列排序规则不一致时必须停止并回滚当前事务。
- Verification: 重试前确认失败事务未提交；修复后记录目标字段排序规则、关键文本扫描结果和契约测试。
- Forbidden action: 禁止修改数据库默认排序规则、手改真实表排序规则、扩大 `WHERE` 范围、拆掉精确租户/删除标记条件，或把失败事务当作成功继续执行。
- Evidence: `E:\IntRuoyi\docs\database-rules.md#数据修复临时表排序规则门禁`；维护仓任务 `20260730-head-test-only-release` publish-test operation `op-2026-07-31T020623302348Z-69bf4670-0461-434a-beb1-d4efd1d6f369`。

### 仅测试服发布范围锁定

- Trigger: 仅测试服发布、`publish-test`、测试服运行态验收、运行控制台版本号与变更说明验证。
- Preflight check: 本轮只允许 `build-release -> publish-test -> 测试服真实运行态验证`；构建开始后固定使用 release worktree 内记录的目标提交。
- Blocker: preview/日志/操作参数出现正式服或备份服实际发布动作、`mark-tested`、`promote-prod`、`promote-backup`，或无法证明 releaseTag 与目标测试服一致。
- Verification: 记录 releaseTag、build operation、publish-test operation、preview 字段、远端 `.env IMAGE_TAG`、实际镜像、容器、health、HTTP、release-info 和运行控制台页面证据。
- Forbidden action: 不得拼接不同 releaseTag 的构建和发布结果，不得因“仅测试服”而跳过测试服运行态或版本说明验收。
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\test-release-preflight.md`。

## Current Status

in_progress

## Progress

- 2026-07-31：新增 `test_dcc_codex_test_items_seed_temp_tables_match_target_text_collation`，先复现旧 SQL 的 `utf8mb4_unicode_ci` 临时表问题，再将两个 seed 临时表改为 `utf8mb4_0900_ai_ci`。
- 2026-07-31：目标 pytest 5 passed，全量 release migration policy gate 400 migrations passed；单文件 gate 不带依赖时按预期 fail-fast，不能作为本修复失败依据。

# DCC 文件类别规则可维护化改造

## Task Goal

在干净 worktree `D:\IntRuoyiWorktree\20260731-dcc-file-category-rules` 中修复测试服 DCC 项目代码批量文件分类的根因：把文件类别识别规则从仅靠类别名与硬编码别名，扩展为可维护、可迁移、可测试的正式规则数据，减少 OQ/PQ、图纸、记录/表单等文件在官方批量分类链路中的 `AMBIGUOUS` / `UNCLASSIFIED`。

## Scope

- 后端范围：`IntRuoyiBackend/yudao-module-dcc` 的项目代码关联文件分类服务、Mapper/DO、测试夹具与迁移 SQL。
- 数据边界：只新增规则能力和可重复迁移，不直接修写 `dcc_controlled_file` 分类字段。
- 环境边界：本轮只在干净 worktree 做代码/迁移/测试，不操作测试服真实数据，不切换正式服或备用服。
- 禁止事项：不直接 SQL 修业务文件分类、不循环单文件 API 打补丁、不把 `AMBIGUOUS` / `UNCLASSIFIED` 当成功、不引入 fallback/降级/吞异常。

## Milestones

- [x] 创建新任务目录并记录目标、BDD、RED/GREEN 策略、经验门禁与授权边界。
- [x] 建立 RED：用后端测试证明 OQ/PQ 与图纸等测试服文件名在现有规则下仍会歧义或未分类。
- [x] 实现正式类别匹配规则表、迁移、DO/Mapper、测试夹具与服务读取逻辑。
- [x] GREEN：目标 JUnit 通过，保留原有泛化歧义仍为 `AMBIGUOUS` 的行为。
- [x] 完成 schema/backend evidence、验证报告与经验沉淀。
- [ ] 完成提交与推送。

## Expected Verification

- RED：`mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test` 在新增测试上失败，失败原因是现有硬编码别名无法被正式可维护规则覆盖或无法识别目标类别。
- GREEN：同一命令通过，并至少覆盖 OQ/PQ 明确规则优先于宽泛工艺规则、图纸扩展名/关键词规则可识别、泛化同分仍保持 `AMBIGUOUS`。
- Schema：迁移 SQL 有 release metadata，测试夹具包含新表，数据库证据记录数据安全、回滚策略与 migration gate 结果。
- Regression：相关 DCC 项目代码分类测试通过，不覆盖既有有效分类，不新增 fallback。

## Current Status

ready_for_closeout

## Authorization Scope

用户已确认可在一个干净 worktree/新任务环境里继续。当前 worktree 为 `D:\IntRuoyiWorktree\20260731-dcc-file-category-rules`，分支为 `codex/20260731-dcc-file-category-rules`，不触碰主工作区 `E:\IntRuoyi` 的脏状态。

## Blocker Policy

若出现缺少 schema 证据、迁移依赖无法确认、目标测试无法稳定 RED、Maven/JDK 依赖缺失、现有并行改动冲突、无法推送 origin、或实现需要直接修生产/测试服业务数据，立即停止并记录 blocker；不改用 SQL 修数、mock 成功、默认值、API 循环补丁或其它绕过方案。

## BDD Scenarios

- `BDD: 可维护规则消除 OQ/PQ 宽泛工艺歧义 -> Given 启用类别同时存在 OQ/PQ 验证类别和工序卡/作业指导书类别, When 文件名包含 OQ/PQ 明确验证方案或报告规则, Then 官方分类选择对应 OQ/PQ 类别并落入其阶段/文件类型, And 不因宽泛工艺关键词返回 AMBIGUOUS。`
- `BDD: 可维护规则识别图纸类未分类文件 -> Given 启用类别存在绑定文件类型的零配件图纸类别, When 项目代码关联文件名或标题包含受控图纸扩展名或图纸关键词, Then 分类结果写入零配件图纸的阶段/文件类型。`
- `BDD: 泛化同分仍显式歧义 -> Given 两个启用类别只有相同强度的泛化匹配规则, When 文件名同时命中两者且没有更明确规则, Then 分类结果仍为 AMBIGUOUS 并保留候选用于人工规则治理。`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是把测试服暴露的硬编码规则缺口收敛为正式 schema + 可维护规则 + 后端测试。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

### DCC lifecycle/schema 迁移全表门禁

- Trigger: 修改 DCC 分类、生命周期阶段、类别规则 schema、迁移 SQL 或发布门禁。
- Preflight check: 写迁移前核对现有迁移、测试夹具、DO/Mapper 与发布 metadata；涉及 NOT NULL 或必填字段时必须考虑全表历史归档行，不只看 `deleted=0`。
- Blocker: 缺 release metadata、缺真实依赖 migrationId、迁移只覆盖活动行导致历史归档行可阻塞、或需要手工改测试库才能通过时必须停止。
- Verification: 记录迁移 SQL、测试夹具、目标 JUnit、migration policy gate 或对应静态验证结果。
- Forbidden action: 禁止手工修改测试库、禁止直接更新 `dcc_controlled_file` 分类结果、禁止用空值/默认成功掩盖 schema 缺口。
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-03 不带数据测试服发布前置门禁`。

## Verification Summary

- `GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest,DccBaseSchemaTest#mysqlSchemaShouldSupportDccFileCategoryMatchRules" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, BUILD SUCCESS; Tests run: 27, Failures: 0, Errors: 0, Skipped: 0.`
- `GREEN: python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_sql.py -q -> PASS, 3 passed.`
- `GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output doc\tasks\20260731-dcc-file-category-rules\migration-policy-gate.json -> PASS, status=passed, migrationCount=401.`
- `GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260731-dcc-file-category-rules/backend-api-evidence.md -> PASS.`
- `GREEN: python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260731-dcc-file-category-rules/database-schema-evidence.md -> PASS.`
- Merge resolution: 已将 `int_main` 合入当前分支并解析 DCC 规则冲突；保留 fail-fast seed、唯一键、无物理删除入口，并兼容 `CONTAINS/EXACT/PREFIX/SUFFIX/EXTENSION`。
- `GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest,DccBaseSchemaTest#mysqlSchemaShouldSupportDccFileCategoryMatchRules" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS after merge, BUILD SUCCESS; Tests run: 27, Failures: 0, Errors: 0, Skipped: 0.`
- `GREEN: python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_sql.py -q -> PASS after merge, 3 passed.`
- `GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260731-dcc-file-category-rules\migration-policy-gate-after-merge.json -> PASS, status=passed, migrationCount=402.`
- `GREEN: scripts\preflight\branch-runtime-port-guard.ps1 -> PASS, frontend 8085, backend 48085.`
- `GREEN: git diff --check -> PASS after merge.`
- Experience consolidation: 已更新 `docs/database-rules.md#DCC 文件类别规则种子门禁` 与 `docs/experience-index.md` 路由。

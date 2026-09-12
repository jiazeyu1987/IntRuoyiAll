# Execution Log

- Task ID: `one-button-app-release-20260911`
- Workspace: `D:\IntRuoyiWorktree\r260911-release-button\a`
- Current status: `in_progress`

## User Request

继续完成维护仓 `doc/tasks/one-button-app-release-20260911` 中的开发验证任务；安装包固定为 without-data / `app-release`，不得包含业务数据、MinIO 快照或全量数据库。

## Experience Preflight

GREEN: experience-preflight -> PASS，已读取本任务维护仓状态、应用仓 `AGENTS.md`、PowerShell/worktree/release backup restore 规则、CI/CD 和数据库 schema 技能合同。本轮先处理 P3 target read-only preflight 缺口，不执行真实发布、数据库写入、MinIO、NAS 上传或正式服操作。

## BDD Scenarios

BDD: 目标数据依赖迁移必须有只读检查 -> Given app-release 发布计划中存在 data/menu/config/permission/seed 或 `requiresTargetPreflight=true` 的 schema 迁移 / When 构建前 target data preflight 运行 / Then 每条待应用迁移必须绑定一个 UTF-8 `.preflight.sql`，并在只读事务中返回精确 `TARGET_PREFLIGHT_PASS:<migrationId>`。

BDD: 结构演练不伪造业务数据 -> Given 迁移依赖测试服已有菜单、角色、业务路线或配置 / When schema-only rehearsal 运行 / Then 结构库只验证结构迁移，真实目标状态由 target preflight 只读查询证明，缺失检查时在 Maven 前阻断。

RED: `python -X utf8 -m pytest -q IntRuoyiBackend\script\tests\test_release_target_preflight_files.py --tb=short --basetemp <app-worktree>\.tmp-pytest-target-preflight-red` -> FAIL，目标预检目录为空，58 个 `*.preflight.sql` 缺失。

GREEN: target-preflight-file-contract -> PASS。已补齐 58 个绑定 `release-target-preflight` 元数据的只读 `SELECT`/`WITH` 检查文件，覆盖迁移表存在性、关键菜单/角色/通知模板/tenant package JSON、业务策略表和受控 schema 依赖。

RED: app-migration-policy-target-preflight-scan -> FAIL，`script\release\release_migration_policy_gate.py` 会把 `sql/mysql/target-preflight/*.preflight.sql` 当普通迁移扫描，并因缺少 `release-migration` 元数据失败。

GREEN: app-migration-policy-target-preflight-scan -> PASS。迁移 policy gate 与 manifest builder 均排除 `target-preflight/` 检查文件，检查文件不进入 required SQL 清单。

RED: requires-target-preflight-manifest-metadata -> FAIL，`release_migration_manifest.py` 不识别 `requiresTargetPreflight`，无法让目标只读门禁字段参与 manifest/plan 合同。

GREEN: requires-target-preflight-manifest-metadata -> PASS。manifest builder 显式解析 `requiresTargetPreflight=true/false`，默认 `false`，非法值 fail fast，并在条目中输出布尔字段。

RED: manifest-validator-fixture-real-bytes -> FAIL，`valid-v1` fixture 的 `backend/app.txt` 在 Windows worktree 中为 CRLF 真实字节，manifest 中 size/hash 仍是旧 LF 值，导致安全扫描被 `PACKAGE_FILE_SHA256_MISMATCH` 抢先拦截。

GREEN: app-preflight-regression -> PASS，`python -X utf8 -m pytest -q --tb=short --basetemp <app-worktree>\.tmp-pytest-app-preflight-final script\tests\test_release_target_preflight_files.py script\tests\test_release_manifest_migration_contract.py script\tests\test_release_migration_metadata.py script\tests\test_release_migration_policy_gate.py script\tests\test_release_preflight_plan.py script\tests\test_release_manifest_validator.py` -> 61 passed。

NOTE: 本轮未执行服务器写入、NAS 上传、Docker 构建、Maven package、MinIO 或正式服动作；测试临时目录清理命令被本机安全策略拦截，提交时仅精确暂存任务文件，临时目录不纳入。

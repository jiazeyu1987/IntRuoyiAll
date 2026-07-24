# 任务: 修复发布包跨环境 preflight 契约

## 任务目标

修复 `ruoyi-vue-pro` 中被维护仓真实页面发布链路直接调用的发布输入 / preflight 契约，使同一个页面构建发布包可以继续按真实页面流程完成 `测试服 -> 正式服 -> 备份服` 的跨环境推进，而不会在正式服 preflight 阶段把 test/backup-only 迁移误判为整包阻塞。

本任务只修改 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 中被维护仓发布链路直接读取的 release planner、policy gate、PowerShell 脚本镜像与对应测试；真实页面点击、运行控制台后端和维护仓部署脚本同步由维护仓主任务继续收口。

## 上一任务检查

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260621-srm-release-page-flow-sql-blockers\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已收口页面发布链路暴露的 SRM SQL / metadata 输入阻塞；本任务作为新的“发布包环境契约 / preflight 语义”问题单独建档。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- 本任务强制门禁摘录：
  - 发布包、manifest、schema 版本、迁移脚本、required SQL 与 `releaseTag` 必须作为同一发布契约处理，不能用页面成功提示或健康检查代替契约一致性。
  - 发布、构建、部署脚本与运行控制台发布链路的修改统一在 `D:\ProjectPackage\Int\IntRuoyiMaintance`；`ruoyi-vue-pro` 只能修改被维护仓直接读取的业务源码、SQL、preflight/planner 输入和对应测试，不得在本仓绕开维护仓独立实现另一套发布入口。
  - 缺少 manifest、迁移 metadata、依赖关系或环境兼容证据时必须 fail fast；不得用 mock 成功、静默跳过、自动降级或删 SQL 绕过页面真实发布链路。
  - 远端写入、发布、备份、恢复默认禁止；本任务仅做本机源码和脚本契约修复，不直接操作 `172.30.30.57/58/59`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。修复目标是让单一页面构建发布包在跨环境推进时显式保留并消费 `allowedEnvironments` 契约，而不是靠手工删迁移、手工改 manifest 或接口绕过。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 非目标环境迁移不得阻断同一发布包跨环境推进 -> Given 页面构建发布包会同时携带 test/backup-only 与 test/backup/prod 迁移 / When prod preflight 读取该发布包的 schemaMigrations / Then test/backup-only 迁移必须被显式跳过，而不是把整份 preflight-plan 判成 blocked。`
- `BDD: 迁移依赖的环境范围必须自洽 -> Given 某个迁移依赖另一个迁移 / When 子迁移允许的环境超出父迁移允许的环境 / Then 发布迁移 policy gate 必须 fail-fast 阻断该不自洽依赖，而不是把问题留到真实页面正式服阶段。`
- `BDD: 发布包读回 required SQL 元数据时不得丢失环境信息 -> Given release package 的 manifest 已声明每个 required SQL 的 allowedEnvironments / When deploy-release 从包内读取 required SQL 列表 / Then 必须保留并使用真实 allowedEnvironments，而不是硬编码成 test/prod/backup 全量允许。`

## 里程碑

1. 建立任务文档并固化真实页面正式服失败证据。`DONE`
2. 通过 RED 测试复现 preflight 环境阻塞、依赖环境不自洽与包内元数据丢失。`DONE`
3. 最小修复 release preflight / policy gate / publish script 契约。`DONE`
4. 运行 targeted regression 并验证维护仓可继续真实页面链路。`DONE`
5. 更新任务证据并准备选择性提交。`DONE`

## 预期验证

- `python -X utf8 -m pytest script\tests\test_release_preflight_plan.py -q`
- `python -X utf8 -m pytest script\tests\test_release_deploy_executor.py -q`
- `python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py -q`
- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- 维护仓真实页面失败证据：
  - `D:\ProjectPackage\Int\IntRuoyiMaintance\runtime\runtime-control\op-2026-06-21T050013146743700Z-46beb4fb-5f6c-4d2a-ba1c-4f1171a3f54b.json`
  - `D:\ProjectPackage\Int\IntRuoyiMaintance\runtime\runtime-control\logs\op-2026-06-21T050013146743700Z-46beb4fb-5f6c-4d2a-ba1c-4f1171a3f54b.log`
  - `E:\Int\CacheData\IntRuoyi\publish-int-ruoyi\release-20260621-page-full-flow-v4\preflight-plan.json`

## 当前状态

COMPLETED：源码侧契约修复、关联 SQL 发布迁移元数据补齐与 targeted regression 已完成。非目标环境迁移改为 `SKIP_ENV_NOT_ALLOWED`，dependency allowedEnvironments 必须被其父迁移覆盖，build-release 统一打包跨环境 required SQL，deploy-release 改为从包内 `manifest.json` 读回真实 `allowedEnvironments`。在本轮提交前复跑时，新的 policy gate 又暴露出 6 条 eDHR SQL 缺少 `release-migration` 元数据、2 条历史自定义前缀未纳入统一规则，以及 1 条把文档契约误写成 SQL migration 依赖的旧配置；这些契约缺口已一并修复并重新通过目标测试和 policy gate。维护仓运行时脚本也已同步；后续真实页面 `构建发布包 -> 部署测试服 -> 标记测试通过 -> 上线正式服 -> 上线备份服` 全链路验证由维护仓主任务继续收口。

## Final Verification Result

- `python -X utf8 -m pytest script\tests\test_release_preflight_plan.py -q` -> PASS，11 passed。
- `python -X utf8 -m pytest script\tests\test_release_deploy_executor.py -q` -> PASS，5 passed。
- `python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py -q` -> PASS，7 passed。
- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，85 passed。
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，`status=passed`，`migrationCount=186`。
- `git diff --check` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260621-release-preflight-environment-contract --mode preview` -> PASS，ready，无 blocked/warnings。

## Cleanup Keep

- `doc/tasks/20260621-release-preflight-environment-contract/bug-regression-evidence.md`

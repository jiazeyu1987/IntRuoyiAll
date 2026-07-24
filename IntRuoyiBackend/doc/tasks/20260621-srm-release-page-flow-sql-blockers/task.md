# 任务: 修复 SRM 发布页面链路 SQL 阻塞

## 任务目标

修复真实运行控制台页面链路暴露的 SRM 发布输入阻塞，使维护仓后续能够重新构建新发布包，并继续从页面真实执行“部署测试服 -> 标记测试通过 -> 上线正式服 -> 上线备份服”。

本任务只修改 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 中作为发布输入的 SQL 和回归测试，不通过手工改环境、绕过脚本或接口替代页面动作完成发布。

## 上一任务检查

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260620-post-release-role-e2e-gate-backend-closeout\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已完成发布后三角色 E2E 门禁收口，不存在未完成阻塞；本任务作为新的页面发布链路根因修复单独建档。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- 本任务强制门禁摘录：
  - 页面发布链路暴露的 SQL 或迁移元数据阻塞，必须修到源码发布输入中，不能靠手工改库、跳过门禁或环境残留掩盖。
  - `deploy-release` 的 required SQL 与 `mark-tested` 的 release-migration metadata 都属于正式发布契约，`code-only` 或页面点击不会跳过它们。
  - 真实页面失败必须先落到任务证据，再通过 `RED -> GREEN -> REGRESSION` 收口，不能直接改 SQL 试错。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。三处阻塞都通过正式 SQL/metadata 契约修复，并补齐对应回归测试。
- `是否存在临时补丁或绕过`：否。没有手工改测试库菜单、手工补列或跳过迁移门禁。

## BDD 场景

- `BDD: D7-2 路由守卫不得把查询按钮当页面路由 -> Given 测试库可能只残留 type=3 查询按钮而缺少对应 type=2 页面路由 / When 执行 20260619_srm_d7_2_supplier_access_risk / Then SQL 必须补齐缺失页面路由，而不是误判页面已存在后再被 fail-fast 校验拦住。`
- `BDD: Phase 1 access profile 幂等加列必须兼容真实发布目标库 -> Given 测试服真实 MySQL 不接受 ALTER TABLE ... ADD COLUMN IF NOT EXISTS 语法 / When 执行 20260620_srm_phase1_supplier_access_profile / Then SQL 必须用兼容目标库的正式幂等写法补齐缺失列，而不是因 1064 语法错误阻断页面发布。`
- `BDD: mark-tested 必须识别 Phase 1 portal 迁移元数据 -> Given 页面真实执行 mark-tested 时会扫描发布 SQL 的 release-migration metadata / When 读取 20260621_srm_phase1_supplier_portal.sql / Then 文件首行必须声明正式 metadata，不能被误判为迁移元数据缺失。`

## 里程碑

1. 建立任务文档并固化真实页面失败证据。`DONE`
2. 用 RED 测试复现 D7-2、Phase 1 profile syntax 与 portal metadata 阻塞。`DONE`
3. 最小修复正式 SQL/metadata 契约。`DONE`
4. 运行回归测试与迁移门禁。`DONE`
5. 更新任务证据并准备选择性提交。`DONE`

## 预期验证

- `python -X utf8 -m pytest script\tests\test_srm_d7_d10_sql_contract.py -q`
- `python -X utf8 -m pytest script\tests\test_srm_phase1_schema_sql.py -q`
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`
- 维护仓真实页面证据：
  - `D:\ProjectPackage\Int\IntRuoyiMaintance\runtime\runtime-control\logs\op-2026-06-21T002749215627300Z-ceee763a-3578-4831-8ff5-c075d7295215.log`
  - `D:\ProjectPackage\Int\IntRuoyiMaintance\runtime\runtime-control\logs\op-2026-06-20T231058149703700Z-a3e87030-a2c9-45fb-bbea-01cf147a8da9.log`
  - `D:\ProjectPackage\Int\IntRuoyiMaintance\runtime\runtime-control\logs\op-2026-06-21T030252892482500Z-0cf0dc70-5cbe-48bd-8d3c-740fbc777f96.log`

## 当前状态

COMPLETED：真实页面链路暴露的三处 SRM 发布输入阻塞已在源码仓按严格 TDD 修复并通过本地验证：

- `20260619_srm_d7_2_supplier_access_risk.sql` 已修复页面路由守卫，避免把 `type=3` 查询按钮误判成页面路由。
- `20260620_srm_phase1_supplier_access_profile.sql` 已改为基于 `information_schema.COLUMNS` 的正式幂等加列过程，不再依赖目标库不兼容的 `ADD COLUMN IF NOT EXISTS`。
- `20260621_srm_phase1_supplier_portal.sql` 已补齐 `release-migration` 元数据，供 `mark-tested` 正常识别依赖链。

当前本任务在源码仓侧的交付已完成；后续由维护仓重新通过真实页面构建新 release tag，并重走测试服/正式服/备份服发布链路。

## 最终验证

- `python -X utf8 -m pytest script\tests\test_srm_d7_d10_sql_contract.py -q` -> `13 passed`
- `python -X utf8 -m pytest script\tests\test_srm_phase1_schema_sql.py -q` -> `10 passed`
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> `status=passed`
- 维护仓真实页面证据已证明阻塞先后从 D7-2 路由守卫推进到 Phase 1 portal metadata、再推进到 Phase 1 access-profile 语法门禁；对应源码修复均已进入本任务提交范围。

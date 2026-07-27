# 测试服发布验证报告

## 结论

测试服发布仍未通过。`release-20260727-onlyoffice-test-r260727-1445`、`release-20260727-onlyoffice-test-r260727-1823`、`release-20260727-onlyoffice-test-r260727-1948` 和 code-only `r1` 至 `r3` 均已判废，不得复用。

`ROUTE-XLSX-00002` 非法第 26 道工序的清理迁移、发布预检稳定排序和 code-only 数据迁移依赖闭包隔离均已修复并通过回归；尚未使用 r4 完成最终测试服发布。

## 已通过

- 发布包本地与 NAS manifest、兼容清单、镜像 tar 哈希一致。
- r3 发布来源 commit 为 `8d940d17e99f3045b99018fac53491250289024d`，backend/frontend `dirty=false`。
- 目标服务器为 `172.30.30.58`，发布前无并发发布进程和 `RUNNING` 锁。
- 失败收口后测试服恢复为 `release-20260723-dcc-viewer-permission-r260723vp-r1`。
- 恢复后的 backend/frontend/onlyoffice 容器 running，backend health HTTP 200 且为 `UP`，frontend HTTP 200。

## 已解除的阻塞

required SQL `20260717_mes_balloon_excel_device_workstation_binding.sql` 在第 420 行失败，原因是脚本期望目标路线工序数量为 49，而测试服只读查询得到 50 条：`ROUTE-XLSX-00001=24`、`ROUTE-XLSX-00002=26`。

该问题属于测试服业务数据基线与迁移契约不一致。未手工修改业务数据，未跳过 required SQL，未执行 `mark-tested`、正式服发布或备用服发布。

修复方式：新增 `20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup.sql`，仅允许测试服执行，精确匹配 `tenant_id=1`、`ROUTE-XLSX-00002`、`sort=26`、`process=B320`，备份并软删除非法路线工序、前序链路、路线配置和 11 条无报工数量的派生排产快照。

发布预检排序修复：`release_preflight_plan.py` 改为按 Manifest 原始索引选择当前可执行迁移，回归 `117 passed`；重新生成计划已确认 cleanup 排在 workstation binding 前。

## 当前阻塞

- 最新失败 releaseTag：`release-20260727-onlyoffice-test-r260727-codeonly-r3`。
- 该 releaseTag 已判废，不能复用。
- 需使用包含 code-only 依赖闭包过滤修复的 r4，重新构建并发布到 `172.30.30.58`。

## Code-only 修复

- 发布包保留 required SQL 的 manifest 类型，部署阶段从 manifest 建立 `migrationId -> type` 映射。
- `publishScope=code-only` 时，`type=data` 迁移在远端 MySQL 执行前明确记录并跳过。
- 直接或间接依赖 `type=data` 的迁移也在远端 MySQL 执行前明确记录并跳过。
- APPLY 项缺少 manifest 类型或依赖映射时直接 fail fast，不手工修改测试库、不改写迁移类型。

## 本地修复验证

- `python -X utf8 -m pytest script\tests\test_mes_balloon_xlsx_route_00002_invalid_process_cleanup_sql.py script\tests\test_mes_balloon_excel_device_workstation_binding_sql.py -q` -> PASS，`11 passed`。
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260727-onlyoffice-test-server-release\migration-policy-gate-after-cleanup.json` -> PASS，`migrationCount=383`。
- `python -X utf8 -m pytest script\tests\test_code_only_required_sql_contract.py -q` -> PASS，`4 passed`。
- 扩展发布回归 -> PASS，`125 passed`；r3 真实 package 复算确认失败 seed 与三条业务数据迁移均未入队。

## 失败收口

- `infra_release_migration`：本轮失败 migration 已收口为 `FAILED`。
- `infra_release_operation_lock`：r3 发布锁已收口为 `FAILED`。
- 测试服 `.env IMAGE_TAG` 已恢复到失败前实际运行版本。
- 失败发布包保留在远端 release 目录作为排障证据，未删除共享存储内容。

# Bug Regression Evidence

## Bug Summary

静态分析确认并修复以下问题：

- DCC 数据快照在服务恢复后采集，无法与 MySQL/对象形成同一无写入窗口。
- MySQL、对象与 DCC 各自查找“上一点”，可能形成不同父点的伪连续链；新增量也未验证全部祖先载荷。
- 非交互生产确认曾退化为可推导文本，且计划任务未从受保护 secret 自动取值。
- 演练镜像探测将 SSH/Docker 故障误判为镜像缺失，`compose down` 使用 `|| true` 吞掉失败。
- binlog 导出未限定 `ruoyi-vue-pro`，会回放其它数据库事件。
- 对象 inventory 未保存父点；根 manifest 会吞掉链不一致异常并仍写 `status=success`。
- 生产 `.env` 曾先落入备份目录再脱敏，存在进程崩溃泄密窗口。
- 备份计划写配置后注册任务失败不会回滚；多任务注册部分成功时可能遗留启用任务。
- 证据导出对链文件重复读取，存在 TOCTOU；manifest 改变或格式非法时仍可能 PASS。
- 恢复链使用 `INTBK-7001`，但 MySQL 错误码 ValidateSet 不接受，真实异常会变成参数绑定错误。
- 只修改历史菜单迁移，已部署环境无法获得证据导出权限。

## Expected Behavior

- FULL/INCREMENTAL、恢复演练、保留和证据导出必须遵循 `minimal-development-plan.md`。
- 任一前置证据缺失时 fail fast，不允许 fallback 或默认成功。

## Reproduction And Root Cause

- 静态合同扫描和定向测试证明 DCC 调用顺序位于 `finally` 之后、父点函数独立枚举目录、演练含 `catch`/`|| true`、binlog 无 `--database`、计划配置无回滚、证据导出接受变化后的 manifest。
- 根因是首轮最小实现把各现有模块串联起来，但没有把“同一父点、同一停服窗口、同一证据快照”作为显式跨模块合同。

## RED / GREEN

- RED: `python -X utf8 -m pytest script\tests\test_backup_minimal_closure.py -q` -> FAIL，5 个后端静态行为和 2 个前端行为缺失。
- RED: `mvn.cmd -pl yudao-module-infra '-Dtest=BackupEvidenceExportServiceTest,BackupPlanServiceImplTest' ... test` -> FAIL，计划注册失败配置未回滚、变化后的 manifest 仍得到 PASS。
- RED: `python -X utf8 -m pytest script\tests\test_system_backup_plan_menu_sql.py -q` -> FAIL，缺少独立升级迁移。
- GREEN: `python -X utf8 -m pytest ... --basetemp .pytest-temp\m7-full -q` -> PASS，160 tests。
- GREEN: Java 备份计划、证据、调度、恢复投影与动作测试 -> PASS，46 tests。
- GREEN: 后端 package、10 个 PowerShell AST、前端两项静态合同、目标 ESLint、分支端口门禁 -> PASS。
- GREEN: migration policy gate（`20260725_system_backup_plan_menu` + `20260908_system_backup_evidence_export_permission`）-> PASS，2 migrations。

## Risk And Scope

- 范围限定为当前备份恢复最小闭环实现及其直接测试。
- 不执行远程服务器、数据库写入、真实备份或 Playwright E2E。
- 新增迁移只做静态合同验证，未在真实 MySQL 首次/重复执行。

## Blockers

- 真实环境恢复证据仍需后续明确授权。
- 全仓 TypeScript 仍有 4 个主工作区同样存在的 DCC 上传页基线错误，非本任务引入。

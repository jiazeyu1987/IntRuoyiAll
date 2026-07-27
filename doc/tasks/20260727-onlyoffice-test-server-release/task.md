# 本地 OnlyOffice 修复测试服发布

## Task Goal

将 DCC OnlyOffice 本地下载基址修复发布到测试服务器 `172.30.30.58`，本轮仅执行测试服发布与运行态验证，不执行正式服、备用服、`mark-tested`、`promote-prod` 或 `promote-backup`。

## Milestones

- [x] 创建发布任务记录，读取测试服发布、服务器、worktree、PowerShell、数据库、后端与 CI/CD 门禁。
- [x] 锁定发布基线、releaseTag、目标环境和禁止动作。
- [x] 创建干净发布 worktree，避免从脏主工作区构建。
- [x] 修复发布前置 migration metadata 门禁阻塞，并完成本地回归验证。
- [x] 构建 release package 并上传 NAS，完成本地/NAS manifest 与 artifact hash 校验。
- [x] 发布到测试服；旧 releaseTag 在 required SQL 目标数据前置条件不满足时失败并判废。
- [x] 冻结部分发布状态并将测试服恢复到部署前实际运行版本。
- [x] 经业务确认 `ROUTE-XLSX-00002` 第 26 道工序不是合法业务数据，并补 test-only 正式数据修复迁移。
- [ ] 使用新的 releaseTag 重建发布包并重新发布到测试服。
- [ ] 记录最终验证证据和收尾状态。

## Expected Verification

- 目标服务器：`172.30.30.58`。
- 发布范围：`test` only，组件范围 `intruoyi`。
- 初始冻结基线：`origin/int_main` commit `9562dca4982007f36c302aaa99847a59d6a4c28e`。
- 发布分支：`codex/20260727-onlyoffice-test-release`。
- 已判废 ReleaseTag：`release-20260727-onlyoffice-test-r260727-1445`。
- 重新发布 ReleaseTag：待生成，必须使用新标签。
- 测试服后端 `http://172.30.30.58:48081/actuator/health` 返回 `UP`。
- 测试服前端 `http://172.30.30.58:8081/` 返回 HTTP 200。
- 运行态 release tag、后端/前端镜像、manifest sourceRepos 与本轮发布一致。

## Current Status

blocked

## Current Blocker

- `release-20260727-onlyoffice-test-r260727-1948` 已通过稳定排序修复并越过原绑定迁移顺序问题，但在更早的 `20260709_mes_rt000006_batch_record_mapping.sql` 失败，错误为 `Missing RT000006 pressure pump route`。
- 测试库只读核对确认不存在 `id=922067` 或 `code=RT000006` 的路线，相关活动路线工序为 `0`，三类压力泵填写员有效角色也为 `0`；不是单纯 ID 或名称漂移。
- 该历史迁移当前要求缺少路线时 fail fast。继续前必须明确：路线不存在时将迁移改为正式 no-op，或通过新迁移完整重建路线、角色及映射数据。两种方案业务影响不同，禁止未确认时静默降级或手工改库。
- 本轮失败状态已收口，测试服继续运行 `release-20260723-dcc-viewer-permission-r260723vp-r1`，后端、前端和 OnlyOffice 均健康。

## Prior Blocker

- 2026-07-27 测试服 `172.30.30.58` 部署 `release-20260727-onlyoffice-test-r260727-1445` 时，在 required SQL `20260717_mes_balloon_excel_device_workstation_binding.sql` 第 420 行失败，错误为 `balloon Excel target route process count mismatch`。
- 只读核对目标租户 `tenant_id=1` 得到 `ROUTE-XLSX-00001=24`、`ROUTE-XLSX-00002=26`，合计 50 条路线工序；迁移脚本固定期望 49 条，阻塞发生在业务数据前置条件与迁移契约不一致，而不是 OnlyOffice 修复代码编译失败。
- 用户确认 `ROUTE-XLSX-00002` 第 26 道工序不是合法业务数据；该 releaseTag 已判废，禁止复用重试、`mark-tested`、`promote-prod` 或 `promote-backup`。
- 失败收口已完成：本次 migration 与 release lock 均为 `FAILED`；测试服 `.env`、backend/frontend 容器和 `/release-info.json` 已恢复并保持 `release-20260723-dcc-viewer-permission-r260723vp-r1`。

## Current Repair

- 只读核对非法记录为 `mes_pro_route_process.id=900173`，`route=ROUTE-XLSX-00002`，`sort=26`，`process=B320/球囊测漏及全检`。
- 引用核对显示活动引用包括：`mes_pro_route_flow_process_config=1`、`mes_pro_route_schedule_config=1`、`mes_pro_route_use_process_config_legacy_20260709=1`、`mes_pro_schedule_order_process=11`；11 条排产工序快照 `reported_quantity=0`，可由正式迁移随同软删除。
- 新增 `20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup.sql`，限定 `allowedEnvironments=test`，在 `20260717_mes_balloon_excel_device_workstation_binding.sql` 前执行，备份并软删除非法路线工序、前序链路、路线配置和派生排产快照。
- RED/GREEN 已完成：新增静态契约测试先失败于清理迁移缺失，补迁移后 `11 passed`；全量 migration policy gate 通过，`migrationCount=383`。
- `release-20260727-onlyoffice-test-r260727-1823` 复发后确认发布预检 FIFO 拓扑排序反转了 Manifest 稳定顺序；已改为按原始索引选择 ready 节点，相关发布回归 `117 passed`，migration policy gate 仍为 `383` 条通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本任务修复 release migration metadata 枚举错误并执行正式发布门禁，不绕过预检。
- `是否从根因和长期维护角度解决`：是；`infra_config` 配置类 SQL 使用脚本允许的正式 `type=config`，发布脚本优先识别当前仓库标准前端目录 `IntRuoyiFronted`，测试服非法路线工序通过带备份与 fail-fast 前置的正式迁移清理。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 测试服发布门禁：必须冻结发布基线、验证 manifest/sourceRepos、publish-test 目标字段、远端 `.env IMAGE_TAG`、实际镜像、backend health、frontend HTTP 和 release-info；不得拼接不同 releaseTag。
- 服务器访问门禁：仅允许访问测试服务器 `172.30.30.58`，不得操作正式服 `172.30.30.57` 或备用服 `172.30.30.59`。
- Worktree 门禁：不得从脏主工作区直接构建；发布 worktree 位于 `D:\IntRuoyiWorktree\onlyoffice-test-release-20260727`。
- Migration metadata 门禁：build-release 前必须执行全量策略门禁；`type` 只允许 `schema/data/menu/config/permission/seed`，发现 `config-seed` 必须阻塞并修复。
- PowerShell 门禁：不使用 `&&`；中文、JSON、SSH/stdin、日志证据使用 UTF-8；不得记录密码、token、私钥或连接串密钥。
- Git 门禁：当前主工作区 ahead/dirty 不作为本轮发布输入；发布分支只提交本任务文件。

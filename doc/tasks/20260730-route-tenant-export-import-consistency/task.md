# 20260730-route-tenant-export-import-consistency

## Task Goal

删除本机测试租户的所有工艺路线数据，从“芋道源码”租户导出当前全量工艺路线数据包，导入测试租户，并分析导入前后两租户工艺路线数据是否一致。

## Milestones

1. 只读确认本机运行态、数据库连接、源租户“芋道源码”和目标租户“测试租户”。
2. 备份并统计目标测试租户当前工艺路线相关数据。
3. 删除测试租户所有工艺路线相关数据，范围限定在目标租户。
4. 从源租户导出全量工艺路线数据包并导入测试租户。
5. 比对源/目标租户导入后的工艺路线、工序、关系图、产品、BOM 和配置数据一致性。

## Expected Verification

- 只读确认源/目标租户 ID 唯一。
- 删除前后记录目标租户影响范围与行数。
- 导出/导入使用正式本机接口或正式服务链路。
- 使用数据库只读查询比对源/目标租户关键表数据摘要和差异。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用当前正式全量导入导出链路和租户限定数据操作。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 数据写入必须限定测试租户，不得修改生产租户、admin 基线数据或无关租户。
- 删除前必须备份/统计目标范围，删除后必须核对影响范围。
- 如果无法唯一确认“芋道源码”或“测试租户”租户 ID，必须停止。
- 缺数据库连接、登录态、导出接口或导入接口时必须 fail fast，不得 mock 或 API-only 冒充页面验证。

## Blocker

- `2026-07-30 15:32` 使用测试租户正式账号导入 `source_tenant_1_route_export_latest.xlsx` 时，接口返回 `1040501417`：`工艺路线导入导出 Excel 的 工序BOM 第 2 行 工序编码 不能为空`。已按用户授权修复源租户正式 BOM 工序绑定并重新导出 `source_tenant_1_route_export_repaired.xlsx`。
- `2026-07-30 17:23` 使用修复后工作簿导入测试租户时，接口返回 `code=500`、`msg=系统异常`；后端日志显示 `MesProRouteWorkbookImportServiceImpl.validateProcesses` 调用 `processMapper.selectByCode` 时因测试租户 `mes_pro_process.code='Z3710'` 存在两条活跃记录触发 `TooManyResultsException`。
- 当前待处理阻塞点限定在测试租户工序主数据重复：`id=922795` 为原有正式 `Z3710 / 球囊裁剪`，`id=922865` 为 `codex` 在 `2026-07-06` 创建的重复测试种子，且其活跃引用集中在带 `SPIKE/spike-route-excel-resource-split` 标记的设备工序和工作站记录。

## User Authorization

- `2026-07-30`: 用户确认“恢复芋道源码的就可以，遇到问题解决问题”。本轮继续目标是把测试租户恢复为芋道源码导出的工艺路线数据；遇到导入阻塞时优先从正式历史、源租户版本快照或同路线唯一关系中找根因并修复，不做猜测性 fallback。

## Resolution

- `2026-07-30`: 用户授权直接使用本机正式链路 `48081` 测试。
- 已修复源租户 `ROUTE-XLSX-00001` / `ROUTE-XLSX-00002` 缺关键工序问题：当前表终点工序分别为 `Z830 / 纸塑袋封口（包装）`、`Z2620 / 球囊测漏及全检`，ACTIVE/DRAFT 快照同步为布尔型 `keyFlag=true`。
- 重新导出 `artifacts/source_tenant_1_route_export_key_repaired.xlsx` 并导入测试租户成功，接口返回 `code=0`。
- 源/目标语义比对已完成：基础信息、工序、关系图、关联产品、BOM、排产配置、流程配置、工序配置、批记录绑定均一致；详见 `artifacts/route_import_consistency_report.json`。

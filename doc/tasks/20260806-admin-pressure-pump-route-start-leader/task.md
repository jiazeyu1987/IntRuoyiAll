# 绑定 admin 为压力泵产线工序开始生产组长

## Task Goal

在本机 `int_main` 的 Docker MySQL `int-ruoyi-mysql / ruoyi-vue-pro` 中，将 `芋道源码/admin` 绑定为两条压力泵产线 active 路线版本的工序开始生产组长，解决生产组长工作台新增工序提示“当前账号没有可新增的路线工序”的问题。

## Milestones

- [x] 创建任务目录并记录 BDD / RED / GREEN 目标。
- [x] 只读核对目标租户、账号、路线、active version 与现有快照状态。
- [x] 备份两条 active version 的原始 `route_snapshot_json`。
- [x] 执行事务化数据修复，只更新 version `448` 和 `622`。
- [x] 复验 JSON 快照、候选路线工序和非目标租户/版本未变。
- [ ] 完成收尾、经验沉淀和最终状态记录。

## Expected Verification

- RED SQL 在写入前失败，原因是两个 active version 均缺少 `$.configSnapshots.routeStartProductionLeaders`。
- GREEN SQL 在写入后通过，确认 `candidateSourceType=USERS`、`candidateSourceIds=[1]`、`candidateSourceNames=["瑛泰管理员（admin）"]`，且 `productionLineId` 分别为 `922119` 和 `980091`。
- 只读 SQL 确认 tenant `122` route `922273`、draft version `490` 和其它历史版本未被更新。
- 登录态 API 验证生产组长新增工序候选包含 `922119` 和 `980091` 的路线工序。

## Current Status

ready_for_closeout

已完成本机数据写入和 SQL/API 验证；等待 evidence validator、清理与最终收尾。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本次只写正式 active route snapshot，不引入默认成功、fallback 或异常吞噬。
- `是否从根因和长期维护角度解决`：是。根因是前台新增候选读取 active `routeStartProductionLeaders` 快照，而非 draft 配置或普通角色权限。
- `是否存在临时补丁或绕过`：否。写入的是既有正式快照字段，范围精确限定为两条目标 active route version。

## Cleanup Keep

- doc/tasks/20260806-admin-pressure-pump-route-start-leader/task.md
- doc/tasks/20260806-admin-pressure-pump-route-start-leader/execution-log.md
- doc/tasks/20260806-admin-pressure-pump-route-start-leader/verification-report.md

# 任务：排产路线配置包支持跨租户导入

- Task ID: `20260629-smart-scheduling-smoke-route-config-cross-tenant`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `blocked`

## Task Goal

将 `mes/pro/scheduler-workbench/route-config` 从“源路线主键直写恢复”收口为“按路线编码 + 工序业务键 + 资源业务键映射到目标租户”的跨租户导入能力，使 `tenant_id=1` 导出的排产路线配置包可导入 `tenant_id=122` 并用于排产冒烟前置准备。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-system-config-package-cross-tenant-import\task.md`
- 状态：`completed` 到 M3，M4 待真实链路验证。
- 处理说明：上一任务已完成系统配置包跨租户导入能力；本次继续补齐路线配置包跨租户映射，目标共同服务于“一次导入包满足测试租户排产烟测”。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接修改正式 `route-config` 导入语义，按目标租户路线/工序/资源映射导入，不再依赖源租户主键。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 路线配置包允许跨租户导入 -> Given tenant 1 导出的 route-config 包 / When tenant 122 导入 / Then 系统按 routeCode 匹配目标路线，而不是要求 routeId 一致。`
- `BDD: 工序用途与排产配置按目标路线工序映射 -> Given 包内 useConfig/scheduleConfig 绑定源 routeProcessId / When tenant 122 导入 / Then 系统按目标路线同 sort 或同 processCode 的 routeProcess 映射后保存。`
- `BDD: 资源配置按目标工位和绑定关系映射 -> Given 包内 resource 绑定源 workstationId/workstationMachineId/workstationWorkerId / When tenant 122 导入 / Then 系统按目标租户工位编码与设备/人工绑定关系映射保存，不直接写入源主键。`

## Milestones

1. M1：建立任务文档并补 RED 测试。`completed`
2. M2：最小修改 `MesProSchedulerWorkbenchRouteConfigPackageServiceImpl` 支持跨租户导入映射。`completed`
3. M3：运行定向测试并记录证据。`completed`
4. M4：与真实 tenant 1 -> tenant 122 导入链路联调，回填主任务。`blocked`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchRouteConfigPackageServiceImplTest" test`

## Current Blockers

- 真实 `route-config import` 语义修复已完成，但缺少本轮继续推进所需的独立真实链路执行窗口；本次用户优先级切换到 NAS 配置工具扩展，故先将该任务显式阻塞留痕，避免并行混改。

## Verification Result

- `2026-06-29`：`mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchRouteConfigPackageServiceTest" test` 通过。
- 当前已收口：`route-config` 导入改为按 `routeCode -> targetRoute`、`processCode/sort -> targetRouteProcess`、`workstationCode/machineryCode -> target resource binding` 映射保存，不再直接写回源主键。
- `2026-06-29`：定位到 `MesProRouteScheduleConfigSaveReqVO.id -> BeanUtils.toBean(...) -> insert(id, ...)` 是主键冲突根因；已在保存层强制忽略外部 `id`，并补回归测试保护该行为。
- `2026-06-29`：进一步定位 `WORKER` 资源跨租户导入缺少 `postId` 业务键；已补导出字段，并在导入时改为优先按 `workstationId + postId` 匹配，缺失目标绑定时交由正式保存层创建。
- `2026-06-29`：确认芋道源码中部分 `mes_md_workstation_worker.post_id` 本身为空；已将导入语义补齐为“源 `postId` 为空时，目标工位有人工绑定则复用最早记录，没有则新建 `postId=null` 绑定”。
- `2026-06-29`：进一步定位测试租户存在大小写仅差异的重复 `process.code`；已把目标路线工序解析改为优先按目标路线 `sort` 命中，再按候选编码列表过滤目标路线，避免依赖租户内工序编码全局唯一。

## Blocked Result

- 本轮代码与定向测试已完成，剩余仅是真实链路继续联调与主任务回填。
- 因用户已切换到 `NAS 配置工具` 需求，本任务先按显式 `blocked` 收口；后续恢复时应从 M4 真实链路验证继续，而不是重复修改当前映射实现。

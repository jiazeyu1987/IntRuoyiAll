# 后续路线版本批记录表单快照完整性

## Task Goal

按用户确认的口径修复后续新生成或重新发布路线版本的批记录表单快照：

- 历史 V15 不强制回补。
- 后续版本快照必须将当前工序配置写入 `configSnapshots.batchUseConfigs[*].formBindings`。
- 新版本生成后通过测试核对快照中的工序绑定，避免流程图显示“未配置”。

## Milestones

- [x] 明确当前版本生成、批记录配置保存和发布投影的快照边界。
- [x] 添加后续版本快照必须包含批记录表单绑定的回归测试，并先取得 RED。
- [x] 实现最小生产代码修复，不修改历史 V15 数据。
- [x] 运行目标测试、相邻 MES 回归和必要的编译验证。
- [x] 更新任务证据并进入 closeout。

## Expected Verification

- `MesProRouteServiceImplTest` 覆盖新版本快照从当前工序配置写入 `batchUseConfigs[*].formBindings`。
- 发布投影相邻测试确认快照中的绑定可继续投影，不被丢弃。
- 目标 MES Maven 测试和 `-am` 编译通过，或记录精确阻塞。
- 历史 V15 不执行数据写入或回补。

## Current Status

ready_for_closeout

实现与验证已完成；最终提交/推送受共享工作区既有无关脏改动阻塞，未执行历史 V15 回补。

## Completed Work

- `MesProRouteVersionWorkflowServiceImpl#createCandidate` 改为从当前路线/工序配置构建候选版本快照，避免继续复制 stale active 快照。
- `MesProRouteServiceImpl#createDraftCandidateRouteVersion` 改为使用完整当前配置快照生成草稿候选版本，覆盖通过路线编辑触发的新版本链路。
- `MesProRouteVersionWorkflowServiceTest` 增加后续候选版本必须包含 `formBindings` 的回归断言。
- `MesProRouteVersionPlatformAdapterTest` 同步新快照生成契约，保持平台状态记录路径可验证。

## Verification Summary

- RED: `MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft` 在旧逻辑下失败，实际快照 `batchUseConfigs` 为空。
- GREEN: 目标测试、工作流测试类、快照构建测试、发布投影测试、平台适配测试、版本相关回归集合均已通过。
- Compile: `mvn -pl yudao-module-mes -am "-DskipTests" compile` 已通过。
- Known unrelated blocker: 全量版本集合包含 `MesProRouteVersionAndCopyTest` 时仍因测试类缺少 `routeOwnerPermissionService` mock 失败，非本任务改动引入。

## Remaining Blockers

- 仓库存在大量本任务外已修改/未跟踪文件，按项目提交策略需要先处理 dirty baseline；本任务未擅自提交或推送无关变更。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺失绑定继续显式暴露，不返回默认成功或空配置。
- `是否从根因和长期维护角度解决`：是；以版本快照生成契约和回归测试保证后续版本数据完整。
- `是否存在临时补丁或绕过`：否；不修改历史 V15 数据，不通过前端隐藏“未配置”。

## 经验门禁

- eDHR 批次任务配置来源：版本快照必须包含完整 `flowGraph.nodes` 与 `batchUseConfigs`，当前配置和发布快照边界必须可追溯。
- 草稿 BATCH 快照读写对称：显式保存的草稿绑定快照不能被当前工序配置覆盖或读空。
- 无 fallback：不得把缺失版本绑定回退为当前用户、默认 MAIN 或默认成功。

# DCC 类别删除同步清理矩阵与授权页

## Task Goal
修复类别列表删除类别后，审核矩阵、查看矩阵、目录授权三个 tab 仍残留对应 row 的问题。

## Current Status
completed

## Milestones
1. completed：已定位类别删除与三个 tab 数据来源；审阅矩阵、查看矩阵来自类别表，目录授权来自目录规则列表。
2. completed：已核对现有生产代码，类别删除链路已经清理目录绑定、查看矩阵、审阅矩阵路线、分发和培训规则；前端已有 `categoryRevision` 触发三个页签重载。
3. completed：无需改生产代码；补强后端回归，显式覆盖查看矩阵规则随类别删除一并清理，同时修正查看矩阵测试夹具缺少生命周期阶段的问题。
4. completed：定向、相关 DCC 后端回归和前端静态合同均通过。
5. completed：已记录证据并提交；清理 preview/apply 通过，无删除项、无阻塞项。

## Expected Verification
- RED：删除类别后关联矩阵/授权数据仍可查出的测试先失败。
- GREEN：删除类别后关联审核矩阵、查看矩阵、目录授权数据一并清理或不可再查。
- REGRESSION：相关 DCC 类别/矩阵/授权测试通过。

## 经验门禁
- PowerShell：显式 UTF-8；不使用 &&。
- DCC 审核矩阵/审批路线：不得改写 live 审核矩阵口径；只修复删除同步范围。
- 禁止 fallback：不得用前端过滤隐藏残留数据代替后端数据一致性修复。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，需定位删除链路与关联数据生命周期。
- 是否存在临时补丁或绕过：否。

## Bug Regression Evidence
- Bug summary：用户反馈类别列表删除 3 个类别后，审阅矩阵、查看矩阵、目录授权 tab 中仍看到对应 row。
- Expected behavior：类别删除成功后，类别维度的审阅矩阵与查看矩阵不应再出现该类别；目录授权不应因类别目录绑定残留而展示已删除类别的绑定口径。
- Root cause check：当前生产代码已在 `deleteCategory` 中清理类别目录绑定、权限规则、查看矩阵规则、审批路线与节点、分发和培训规则；前端类别页刷新后递增 `categoryRevision`，三个页签激活或修订变化时会重载。未发现需要改生产代码的残留逻辑。
- Regression added：`DccFileCategoryAdminServiceImplTest#deleteCategory_removesCategoryAndGovernanceRecords` 补充 `dcc_category_view_matrix_rule` 夹具与断言，防止后续删除链路漏清查看矩阵。
- Risk：目录授权 tab 本身维护的是目录访问规则，不是类别主数据；本次不删除独立目录访问规则，避免把“删除类别”扩大成“删除目录权限”。

## Closeout Evidence
- Commit：`fb85755806 任务: 补强DCC类别删除同步验证`。
- Cleanup：`task-closeout-cleanup` preview/apply 均通过，删除项为 `<none>`，blocked 为 `<none>`。
- Worktree：当前仓库为主工作区 `int_main`，非 linked worktree，无需融合或删除 worktree。

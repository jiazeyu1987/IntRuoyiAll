# 测试服全部角色权限差异同步变更请求

## Request Summary And Source

- 来源：用户在完成 `zhaojie / mes_scheduler` 定向权限同步后明确补充：“不仅仅是 zhaojie 的角色，而是所有的都平移过去”。
- 请求目标：把本机租户 1 的全部权限角色配置同步到测试服务器，而不是只修复排产员。

## Current Baseline Reviewed

- 当前任务：`doc/tasks/20260807-test-permission-role-differential-sync/`。
- 已完成范围：测试服正式版本菜单迁移，以及租户 1 `mes_scheduler` 的 `update/version-query` 权限恢复。
- 已确认约束：本机与测试服角色自增 ID 不一致；测试服存在环境专属角色和用户绑定；全量删除重灌会破坏引用关系。
- 当前恢复能力：已有目标迁移前后快照和精确回滚 SQL，但只覆盖原排产员定向范围，不能直接覆盖本次扩展范围。

## Classification

- 类型：数据与运维范围变更。
- 风险：高。同步对象从单角色扩大到租户 1 全部角色定义及角色菜单权限矩阵。

## Impact Analysis

- Product：测试服所有本机标准角色的菜单与按钮权限将按本机有效配置对齐。
- Design：无前后端产品设计变更；权限合同以角色编码和菜单权限稳定键表达。
- Data：涉及 `system_role`、`system_role_menu`，以及本机正式菜单在测试服缺失时的 `system_menu` / `system_tenant_package.menu_ids`；不修改 `system_user_role`。
- API：不修改接口合同；登录权限响应会随角色菜单配置变化。
- Test：必须生成全角色差异矩阵、危险权限差异、角色/用户绑定不变量、精确缓存失效和真实账号复验清单。
- Release：仅授权测试服务器；不得扩展到备份服或正式服。
- Operations：写入前必须重新建立覆盖全部目标行的精确快照和恢复脚本，并使用测试环境发布互斥锁。

## Decision

accept：接受“全部角色权限平移”，按以下正式边界执行：

- 源：本机 MySQL 租户 1 的未删除角色及其有效菜单权限。
- 目标：测试服务器 MySQL 租户 1。
- 角色身份：`tenant_id + role.code`；菜单身份优先使用唯一非空 `permission`，菜单目录节点使用经审计的稳定路径/组件合同，禁止复制自增关系 ID。
- 本机存在、测试服缺失的标准角色可在前置唯一性通过后创建；共享角色按本机有效权限矩阵差异同步。
- 测试服专属角色保留；不删除角色，不修改任何用户角色绑定，不同步其它租户，不覆盖测试服用户数据。
- 任一稳定键不唯一、菜单合同冲突、依赖缺失或恢复快照不完整时，写入阶段阻塞。

## Required Approvals

- 用户已在当前任务中明确授权测试服务器全部角色权限平移。
- 未授权备份服、正式服、角色删除、用户角色绑定变更或全库缓存清理。

## Downstream Skill Reruns

- `database-schema-delivery`：重新建立全角色差异、RED/GREEN、备份、恢复和数据库验证证据。
- 真实页面/E2E：按受影响账号清单重新登录验证；无登录前置时明确阻塞，不以 API-only 替代。
- `project-experience-consolidation`：任务最终收尾时更新长期经验，避免重复记录一次性角色数据。

## Blockers And Next Action

- 当前无审计阻塞。
- 下一步：只读导出本机/测试服租户 1 角色、菜单和有效角色菜单稳定键矩阵；审计结果通过后再冻结全量同步白名单和恢复范围。

# 910170 部门清空最小修复

## Scope

用户现唯一授权本线程：修复清空部门的必要前后端语义；只通过真实前端恢复用户 910170 / A4020140 / 商孟莹原空部门；核对审计与其余字段。禁止 API/SQL 恢复、改其他用户、重置密码、扩大权限、删除仍被引用部门。Root 独占 Maven、构建与服务重启。

Root 最新要求：先提交可对当前源码编译执行的测试，暂不改生产；真实 RED 后再通知 GREEN。此阶段只写测试、任务资产与只读恢复脚本。

## BDD

1. Given 用户已有部门，When 正式保存请求明确包含 deptId:null，Then 持久化部门为 NULL，其他字段/岗位/其他用户不变，审计从原部门变为空。
2. Given 用户已有部门，When 请求未包含 deptId，Then 保留原部门；客户端不能伪造 presence 字段触发清空。
3. Given 用户已有部门，When 密码、状态或登录信息走原局部更新，Then 部门保持不变。
4. Given 前端编辑已加载用户，When 使用部门清空控件并保存，Then 发送明确 null 而非遗漏字段，并保留其他已加载字段。
5. Given 用户 910170 经完整用户名筛选且弹窗昵称相符，When 清空错误部门并保存一次、重新打开及检查审计，Then 部门为空，岗位仍为空、角色及其他可见字段不变；不因成功 toast 提前判定恢复。
6. Given 更新请求省略 deptId 且用户原有部门，When 正式服务完成持久化并由生产审计模板执行真实 DiffParseFunction，Then 数据库保留部门，审计不出现部门删除；显式 null 的真实 diff 仍能识别清空。

## Inspected Semantics

- UserForm 的部门树选择器没有 clearable，初始 deptId 为字符串空值；API UserVO 将 deptId 声明为 number。
- UserSaveReqVO 的 Long deptId 未区分 absent/null。
- AdminUserServiceImpl.updateUser 经 BeanUtils 转换后使用 updateById；AdminUserDO.deptId 没有 ALWAYS 策略，因此 null 被跳过。
- 不改全局 DO 的更新策略，避免密码/状态/登录等 updateById 局部更新隐式清部门。
- 拟在请求 DTO 内记录部门字段是否由反序列化显式提供，忽略该内部标记的 JSON 读写；仅 updateUser 的明确 null 分支使用带用户 ID 条件的显式 null set。沿用事务、租户拦截和原审计链。

## Tests

- 专属后端 H2 测试 AdminUserDepartmentClearTest：显式 null 落库清空、遗漏保留、伪造 presence 忽略、密码/状态/登录局部更新保留、内部标记不出现在序列化中。测试只使用现有生产类型及方法，因此 RED 阶段不依赖新增方法签名。
- 前端 UserForm 实际提交函数的组件级测试与清空控件合同；这是单元验证，不替代真实 E2E。
- Root 精确命令与 ready 状态见 root-test-handoff.json。

## Status

audit_green_implementation — 原部门清空 RED/GREEN 已由 Root 完成。补充审计 RED 于 2026-10-16 19:27:01—19:27:18 +08:00 真实执行：6 tests / 1 failure / 0 errors，唯一失败为 omittedDepartmentDoesNotProduceClearAuditDiff，数据库保留 77，但审计误写删除部门。Root 已授权最小修复：仅在原请求省略部门时，于持久化完成后将原部门补入审计使用的请求对象；不改显式 null 和全局字段策略。Root 运行回归、构建、部署；维护窗口内页面写入暂停。

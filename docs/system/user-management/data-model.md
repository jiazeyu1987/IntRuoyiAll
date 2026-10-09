# 用户管理数据与事实归属设计

## Purpose and Scope

以 [架构](architecture.md) 基线为准。区分源码中的表映射、目标数据归属和现场部署状态；本轮未查询或写入数据库，不声称任何迁移、唯一索引、外键或回填已部署。第一阶段 UM-03 不改表结构、不写数据修复。

## Evidence Reviewed

已读 E05/E07/E08/E12/E19/E20 与 UserConvert.convertList/convertForUpdate；任职 JSON 与关联表都是现有来源，不是只看名称猜测。已读 E22/E23 当前昵称查询、E24 冻结提交身份、E25 签名 canonicalContentJson 写入和 E26 历史查询的旧回填分支。签名表名依据 `IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/dal/dataobject/ElectronicSignatureRecordDO.java` 的 @TableName。

## Entities

| 现有实体/表映射 | 当前事实 | proposed 归属与边界 |
| --- | --- | --- |
| AdminUserDO / system_users | tenant、稳定 id、username/canonicalUsername、资料、deptId、post_ids JSON、密码/凭据状态、失败锁、生命周期字段 | 逻辑职责分别拥有允许字段；共表不等于允许任意职责直接更新所有列 |
| UserPostDO / system_user_post | id/userId/postId，继承BaseDO带审计/软删除字段，DO本身没有tenantId | 用户任职正式集合的目标唯一源；迁移前必须和JSON对账；真实表租户列/注入需现场确认 |
| DeptDO / system_dept | 部门层级、状态、leaderUserId | 组织主数据；任职引用，不复制负责人到用户表 |
| PostDO / system_post | 岗位名称、状态等 | 组织岗位主数据；停用不自动删除既有任职 |
| UserRoleDO / system_user_role | 永久用户角色关联 | 权限授权；不以岗位 ID/名称推断角色 |
| RoleDO / system_role | 角色名称、code、状态、权限模型 | 授权主数据；列表展示永久关系不等于有效权限 |
| AdminUserPasswordHistoryDO / system_user_password_history | 旧密码哈希、变更时间与来源 | 凭据内部敏感数据，绝不进列表/跨模块 DTO |
| OAuth2AccessTokenDO / system_oauth2_access_token；OAuth2RefreshTokenDO / system_oauth2_refresh_token | 正式会话身份、租户、类型、失效时间 | 会话权威；Redis 只为缓存，不能独立证明授权 |
| ElectronicSignatureRecordDO / system_electronic_signature | 签名主体/版本/动作/原因/认证/摘要/证据与 canonicalContentJson | 位于 signature 模块，源码表映射虽然带 system 前缀，事实仍归签名模块；不假设现场存在 |
| MES 事件/维护审计/批记录与其签名投影 | 操作人 ID、业务动作、来源与部分已有冻结身份 | MES 自有业务事实，缺历史快照不能查当前资料补造 |

上述表名是源码 DO 的 TableName 映射，非现场 schema 清单。`IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/dataobject/dept/UserPostDO.java` 继承 BaseDO 而非 TenantBaseDO；测试建表 `IntRuoyiBackend/yudao-module-system/src/test/resources/sql/create_tables.sql` 的 system_user_post 有 tenant_id。`IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-biz-tenant/src/main/java/cn/iocoder/yudao/framework/tenant/core/db/TenantDatabaseInterceptor.java.computeIgnoreTable` 对已注册且无TenantIgnore的实体仍选择租户拦截，因此“DO无字段”不等于“SQL无租户条件”。真实表列、实际插入租户与拦截配置须开发前只读确认；测试schema不能证明现场schema已一致。新实体 UserIdentityView/AssignmentSnapshot/UserPageItemQueryVO 是 proposed DTO/读模型，不意味着增加持久化表。

## Relationships

稳定账号键为 `(tenantId,userId)`，账号登录唯一性以规范化账号为准，现有 canonicalizeUsername 使用 trim + Unicode NFC + lower-case；租户内已删账号是否永久禁止复用按现有规则保留，不以软删除自动释放身份。

用户当前有一个 deptId，多个岗位与多个永久角色。组织部门/岗位与授权角色彼此独立。用户名/昵称可以用于展示或受控搜索，不能替代稳定 userId 的关系键。tenantId 来自服务端上下文，不能由前端决定目标租户。

任职目标唯一来源为 system_user_post 的当前有效关系；JSON post_ids 将在完整调用清单和迁移验证后退出业务决定。第一阶段仍校验两者一致，既不只相信 JSON，也不自动补齐关联。未来查询投影/缓存必须是可重建派生数据，明确版本和构建规则；禁止形成第二份可独立写入的正式关系。

角色清单读永久关联；临时授权有独立有效期/授权证据，实际权限读取遵循现有正式权限聚合，不擅自用列表 roles 数组做菜单权限判断。业务/签名身份必须同时保留 domain、tenantId、signerId、operatorId，SYSTEM_USER 和 MES_EMPLOYEE_PROFILE 不共享编号空间。

## State Models

| 状态维度 | 当前表达 | 目标规则 |
| --- | --- | --- |
| 账号启用 | status 0/1 | 普通启停与生命周期停用分操作；停用撤销会话 |
| 生命周期 | 单据类型/号/时间、effectiveTime、deactivatedTime | 尚未生效与已经生效分别判断；生效后禁止普通手工启用；未来计划覆盖政策另明确 |
| 登录锁 | loginLocked/time/count/window | 有效锁计算包含时效，不能把标记 1 永久视作当前有效锁；UM-03不改现有显示 |
| 凭据状态 | ACTIVE / INITIAL / RESET_REQUIRED | 重置后必须改密，签名重新认证不得绕过；失效时间独立检查 |
| 岗位状态 | ENABLE / DISABLE | 任职保留与新增候选分别判断；既有停用不代表删除事实 |
| 签名身份 | canonicalContentJson.signatureIdentity 在部分路径存在 | 正式新事实由服务端认证生成冻结身份；历史读取验证来源、域、租户、actor 与证据 |

没有统一大枚举把“停用、锁定、改密、离职”混为同一状态。未知/非法状态明确拒绝；不能把缺失值解释为默认启用或成功。

### 历史身份与证据

proposed 业务冻结身份合同：`identityDomain, tenantId, signerId/actorId, operatorId（如有代操作）, displayName, occurredAt, sourceType/sourceId, objectVersion, evidenceReference`。签名身份进入被计算摘要的 canonicalContentJson，签名主体/动作/版本/原因/时间继续保留正式证据。

捕获姓名必须来自本次正式认证/正式主体读取，不信任前端传来的 displayName。SYSTEM_USER 的账号当前可用性只用于新动作资格；MES_EMPLOYEE_PROFILE 保留其自身档案与凭据治理。读取已经创建的历史事实时不再用用户启用状态决定姓名能否存在。

E24 正式提交已验证冻结身份，未来设计必须保留；不能把工序开始、批记录正式绑定与 formBindings 三条链互相补齐。E22/E23 的历史当前昵称和 E26 的缺快照当前回填仍是待实施，不能用本设计声称 UM-01 已修复。

## Migration Notes

后续每次迁移独立授权；下面是设计门禁，不是已生成或执行的 SQL。

1. **现场清单**：只读确认实际库/表/列/索引/租户/软删除/数据库版本、历史异常数量和来源。缺正式 schema 不能凭 DO 自动执行 DDL。
2. **全调用点枚举**：全仓枚举 AdminUserDO.postIds 的读写、UserPostMapper 的读写、SQL/导入/配置包/测试/API/MES/DCC/BPM 等消费者；逐项记录正式来源、租户、是否写入和迁移责任。当前明确有 E07 双写、E19 单条/批量不同来源、E04.importUserList/importDingTalkUserList 初始化 JSON，以及 MES 内部 DO/Service 直接消费。此列表不是全量完成证明；完整清单是下一阶段 blocker。
3. **只读差异报告**：逐租户比较原始 JSON 与有效关系，校验重复/小数/超范围/已删或跨租户岗位/名称与状态。不能 `new HashSet` 去重后掩盖非法 token；NULL 的合法空表示仅能在确实无正式关系时按现行合同接受。
4. **异常治理**：有差异的对象保持编辑拒绝。修复方案须指定权威事实、来源证据、审批者与审计；不默认任意一边“赢”，不把普通用户保存当作治理入口。需写数据时另外获得用户当轮授权。
5. **来源切换**：先让全部消费者改读正式关系，所有写用例只由任职职责维护关系；保持 UM-04 的完整目标集合、新增/保留/移除及事务约束。过渡期如仍需 JSON，则它只能是同事务派生投影，不能独立成为输入事实；迁移方案须明确各版本写入者，不长期保留双权威。
6. **验证后退役**：完成全调用者回归、差异为零、并发编辑/组织状态变更/逻辑删除覆盖后才移除 JSON 决定行为，最后在独立迁移移除列。禁止删列后靠默认 [] 让旧代码继续运行。
7. **可恢复性**：切换前取得授权备份及可验证恢复方案，保留迁移版本与对应差异报告。写入版本切换后若已经产生新正式关系，不得盲目回滚旧版本或覆盖新事实；回退先暂停任务自有写入口、核对变更窗口、恢复一致投影与版本，再验证。未经授权不停止主服务。回退失败要报告，不能静默切换回 JSON。

历史身份迁移另行处理：已有可验证冻结快照优先保留；只有可信的当时审计/签名证据可用于经审批的恢复，并记录恢复来源和不确定性。缺证明的旧姓名保持缺失，绝不从当前资料回填“当时姓名”。修改 canonicalContentJson 会影响哈希、验签及幂等合同，不能批量改旧证据；需要恢复时另设计版本化追加证据，不伪造原签名。

UM-02 导入绕过生命周期暂缓，不列为唯一源、历史或 UM-03 的前置。未来来源切换需把仍存在的导入写入口列入清单并提出独立批准的改动方案；在该阶段未经批准或无法处理这些调用者时阻断来源切换，不能假称已经完成。

## Data Integrity Rules

- proposed 有效关联唯一约束应覆盖 tenant/user/post 与软删除语义；准确 DDL 必须根据现场索引/多次逻辑删除策略制定，不能照搬含 deleted 的布尔唯一键导致重复历史删除冲突。
- 用户 canonicalUsername 的唯一约束须匹配现行“永久唯一”规则，并验证 Unicode/大小写；email/mobile 的空值和唯一政策单独确认，不推断必须全球唯一。
- 所有跨关系查询显式维护 tenantId，批量摘要只用已授权页内用户；主数据查不到不能忽略成空集合。
- 写入结果行数异常传播回滚；安全历史、令牌与用户状态不能部分成功。只读 page 采用一致快照，读后对账失败不写修复。
- 目标乐观版本可用于资料丢失更新，但版本列、预期版本错误与重放策略尚未实施；不把 UM-04 的行锁错误称为全系统已有版本冲突控制。

## Open Questions

- OQ-D1：实际 schema/索引/软删除和异常数据清单，影响唯一源迁移。
- OQ-D2：资料并发覆盖政策与 schema 版本，影响写用例拆分。
- OQ-D3：旧记录是否有可验证的当时身份来源、恢复审批与保留要求，影响 UM-01。
- OQ-ID1：全调用点 Long/string 边界，影响稳定身份 API；不以 Number 截断提供兼容。

## Design Blockers

后续迁移必须先有完整调用清单、现场只读 schema 证据、差异报告、独立授权的治理/回退方案；任一缺失不得切换唯一源。历史恢复缺可信来源不得恢复，正式证据缺身份时不得给当前昵称冒充历史。上述后续 blockers 不阻挡已限定范围的 UM-03 设计；本轮没有数据验证 PASS。

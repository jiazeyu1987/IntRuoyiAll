# 用户管理架构验收矩阵

本矩阵是未来验证设计，当前没有业务测试或 E2E PASS。设计评审只判断合同是否完整且与静态链相符。方法依据的 E 编号指向 [架构证据](architecture.md)；proposed 方法不冒充现有源码。D1为仅UM-03，R为D1保留回归，D2/D3/D4为独立授权后续阶段，Deferred不启动。

## G1 / P1：明确操作与单向依赖

| ID / 阶段 | 场景、输入与动作 | 预期结果 | 方法依据 | 未来验证方式 |
| --- | --- | --- | --- | --- |
| AC-G1-01 / D1 | 只有 user:query 的人员进入用户列表 | page/Q2完整显示允许数据；不调用 role/post管理分页或 assign-user-role读取 | E01—E05/E10/E13；proposed UserQueryService | 受限角色真实前端、网络记录及权限/SQL定向测试 |
| AC-G1-02 / D2 | 普通资料修改请求含 password/status/roleIds | 拒绝越界字段，不发生敏感写入 | E03/E07/E16；proposed明确DTO | 用例合同、真实持久化负例 |
| AC-G1-03 / D2-D3 | 抽出主体锁/租户配额端口，再执行开户、资料、停用 | 端口只读所属仓储/纯规则，无 user↔token、user↔tenant、user↔permission循环编排 | E16/E17/E21/E11 | 全依赖图与受影响事务/业务回归；@Lazy不作为通过证据 |
| AC-G1-04 / D2 | 一个保存同时修改资料与完整岗位目标，岗位写失败 | 同事务全部撤销；不出现两个HTTP请求部分成功 | E06/E07/E08 | 正式Spring事务与数据库失败注入 |
| AC-G1-05 / Deferred UM-02 | 导入覆盖既有账号status | 后续获授权后复用正式生命周期规则；当前仍待修，不写已通过 | E03.importExcel / E04.importUserList / E16 | 独立方案及真实导入前端验证，非D1前置 |

## G2 / P2、P4：单一来源与历史事实

| ID / 阶段 | 场景、输入与动作 | 预期结果 | 方法依据 | 未来验证方式 |
| --- | --- | --- | --- | --- |
| AC-G2-01 / D1 | 同页用户岗位JSON与正式关系相等，含已停用岗位 | 返回完整正式摘要，停用状态保留；不凭启用候选删关系 | E07/E08/E19/E20；proposed批量只读对账 | 查询合同和真实数据库投影 |
| AC-G2-02 / D1 | 同一一致快照有重复、非法、跨租户、已删或JSON/关联差异 | 正式完整性错误，零修复写入；不是空数组/跳过标签 | E07.parseStoredPostIds / E08；proposed Q1 | 数据库只读验证和异常分支，准备数据另授权 |
| AC-G2-03 / R UM-04 | 编辑保留、移除既有停用岗位；尝试新增未绑定停用岗位 | 前两者按完整目标集合保存；第三者拒绝且资料/关系未改 | E06/E07/E08 | 现有定向测试与必要真实UI回归 |
| AC-G2-04 / R UM-04 | 修改缺postIds/null、[]、小数/重复/越Long范围ID；正式来源冲突 | 缺失非法拒绝；[]明确清空；正式冲突拒绝且不治理；不发生JS精度截断 | E02/E06/E07及UserPostIdsDeserializer | HTTP合同、真实handler、事务与并发测试 |
| AC-G2-05 / D2 | 来源切换前枚举全部JSON/关联读写并检查差异 | 完整清单/差异治理/恢复方案齐备才能切换；旧调用者未迁移不能删列 | E07/E19/E20，现有导入/个人资料读取 | 全仓人工链审查、schema只读报告、迁移演练另授权 |
| AC-G2-06 / D4 | 用户完成正式签名后改名/转岗/停用/逻辑删除，再打开两种MES历史详情 | 当时姓名、域、时间与证据保持；历史不依赖当前用户存在 | E22/E23/E24/E25/E26 | 真实前端业务路径、签名证据只读核验 |
| AC-G2-07 / D4 | 正式历史缺冻结身份或验签/租户/actor/domain不匹配 | 明确失败或受控缺失状态；正式签名证据缺失阻断；不查当前昵称补造 | E24.frozenName/E26.toEvidence旧分支；proposed受控历史查询 | 历史证据负例、哈希/幂等回归与页面错误 |
| AC-G2-08 / D4 | SYSTEM_USER与MES_EMPLOYEE_PROFILE同数值编号、前端伪造displayName | 域分别认证；前端姓名不作为冻结来源；operator与signer身份分别绑定 | E24/E25.signInternal，AuthorizedSignatureIdentity | 两域认证、正式业务绑定和投影测试 |

## G3 / P3：权限、事务、锁、缓存与失败

| ID / 阶段 | 场景、输入与动作 | 预期结果 | 方法依据 | 未来验证方式 |
| --- | --- | --- | --- | --- |
| AC-G3-01 / D1 | 无user:query、另一租户ID、本人范围访问他人、非法deptId/roleId | 相应拒绝/范围内空页；count/聚合/page不泄露其他人员、节点或角色目录 | E03/E05/E13/E19；proposed Q1/Q2 | 真实数据库权限拦截、受限角色UI与负例 |
| AC-G3-02 / D1 | 同一筛选下存在范围外负责人/同名角色；批量摘要读取 | 只读已授权页内用户关系及同租户元数据；不把范围外负责人插入排序 | E04/E11/E12/E13 | 真实SQL隔离/计数/跨页测试 |
| AC-G3-03 / R UM-05/S-01 | 改密/重置/停用时access+孤立refresh存在，Redis删除报错 | 错误传播；DB与凭据历史按事务回滚；残留缓存不能授权 | E16/E17.removeAccessToken | 既有真实事务测试、DB/Redis状态核验 |
| AC-G3-04 / R UM-05/S-01 | 密码认证完成但发令牌前并发重置；刷新与撤销并发 | 用户→access→refresh统一锁序；旧认证不能发新会话；最终状态符合正式提交结果 | E17.createPasswordAccessToken / refreshLocatedAdmin | 两真实事务并发与认证过滤链测试 |
| AC-G3-05 / R UM-05/S-01 | 正式token不存在/过期/账号已停用，但Redis仍有旧值 | 受保护请求拒绝；cache无独立授权能力；保留正式refresh-as-bearer校验 | E17.readOfficialAccessToken / checkAccessToken；TokenAuthenticationFilter | 正式过滤器链、真实持久化与缓存反例 |
| AC-G3-06 / R 安全 | 重置/个人改密/签名请求成功与失败 | 返回/日志/审计不含密码、hash、令牌、原始凭据对象 | E03/UserProfileController/E16及日志配置 | DTO契约、实际脱敏输出检查 |
| AC-G3-07 / D3 | 角色分配/签名涉及GxP账本与用户锁 | 先账本政策幂等锁，再业务主体；完整锁图无反向边；失败不能缺审计成功 | E11/PermissionCommandProtocol/E25 | 全写入口锁图、真实并发与审计事务测试 |
| AC-G3-08 / D3 | 重新认证错误密码、过期、重置要求、停用、锁定 | 逐项拒绝，不发登录令牌；失败计数按独立确认政策保留；mock关闭 | E16.reauthenticateForSignature；proposed最小认证合同 | 真实事务/配置及签名全链 |
| AC-G3-09 / D1 | DB/依赖失败，或分页/摘要协议不完整 | 明确错误、无备用目录/mock成功；不输出敏感信息 | E01—E05；proposed Q1 | 失败注入与前端真实handler |
| AC-G3-10 / R 授权 | 普通用户被尝试授予受限角色；重复/冲突幂等键 | 既有高权限保护及精确回执行为保留，摘要查询不会赋权 | E09/E11及PermissionCommandProtocol | 现有权限协议回归，必要真实授权页面 |
| AC-G3-11 / D1 | 此次查询范围计算纳入的有效角色dataScope为null或未知值，发起page/Q2 | 查询适配器返回权限配置错误，不扩成ALL或默认SELF，零写入；不在本阶段全局改旧权限服务 | PermissionServiceImpl.getDeptDataPermission旧跳过/日志分支；proposed查询适配器及USER_QUERY_PERMISSION_CONFIGURATION_INVALID | 查询合同负例、真实权限聚合/DB零写入核验与页面错误；准备异常测试数据需另授权 |

## G4 / P5：批量查询与交互

| ID / 阶段 | 场景、输入与动作 | 预期结果 | 方法依据 | 未来验证方式 |
| --- | --- | --- | --- | --- |
| AC-G4-01 / D1 | 页大小1/10/200、首次进入、翻页、快速筛选 | 翻页/筛选1个page请求，初始化另1个Q2；最多7+H业务SQL且不随N增长，无管理全目录/N次角色请求 | E01/E04/E05/E12/E15；proposed Q1预算 | 浏览器请求记录+真实SQL计数，基础鉴权成本分列 |
| AC-G4-02 / D1 | 无部门条件、有部门含多子部门、所选部门无结果、负责人不在筛选、同组跨页 | 无部门id DESC；有部门选定组优先、其他组matched MAX(id) DESC、组内负责人优先/id DESC；global total一致，不丢人重人 | E04.sortUsersByDeptLeader/buildPagedResult；proposed数据库分页 | 现行算法对照+真实数据库全页连续测试 |
| AC-G4-03 / D1 | username/mobile模糊、status、createTime范围、deptId/roleId组合 | 现有筛选语义不变；roleId是永久关系；不新增UM-10筛选 | E02/E04/E05/UserPageReqVO | 查询参数和数据集合对照 |
| AC-G4-04 / D1 | 分别以ALL、CUSTOM、ONLY、AND_CHILD、SELF、无启用角色、混合有效角色、本人无部门加载树；含范围外父和停用节点 | ALL本租户启用部门；CUSTOM配置∪本人部门且不扩子；ONLY本人部门且不扩子；AND_CHILD本人部门∪正式子部门；SELF及无启用角色仅本人部门上下文而不授权同部门他人；混合部门并集/self取或/ALL覆盖；本人无部门且无其他授权部门时空树但可查本人；停用节点不返回、可见用户既有停用部门名称仍展示；范围外父不披露 | E13/E14/E15/PermissionServiceImpl.getDeptDataPermission/DataScopeEnum；proposed Q2 | 每个范围独立样本与混合样本、真实页面树搜索选择、跨范围/停用节点负例 |
| AC-G4-05 / D1 | 显示停用角色/岗位、多项长名称、无正式关联 | 关联事实准确且有溢出可读提示；无关联才显示“—”，不查候选目录推断 | E01/E07/E11；proposed roles/posts | DTO/单元格组件及真实UI |
| AC-G4-06 / D1 | 快速切换筛选、翻页、离页后旧响应到达 | 旧响应不覆盖list/total/error/loading；server order不二次重排 | E01.getList；proposed请求生命周期 | 真实handler/组件异步竞态 |
| AC-G4-07 / D1 | page失败、Q2失败、摘要字段缺失、合法空页 | 错误/独立状态准确，禁止旧数据冒充新结果、默认[]或备用全目录；真空页正常 | E01/E14；proposed前端合同 | 协议负例和页面状态验证 |
| AC-G4-08 / D1/R | 路由userId定位（含目标部门停用）、列配置、操作成功刷新、点击角色分配 | 定位尊重范围；停用部门明确拒绝并提示，不去掉deptId扩大查询；表头自定义排序保持禁用；分配入口仍独立受权限控制 | E01/E02/E09/UnifiedListTemplate.normalizeSortableColumn | 菜单及实际handler定向回归 |

## 验证证据与放行规则

开发验收记录必须包含具体输入/账号范围、真实动作、提交或拒绝结果、环境归属、代码版本、未来测试实际命令与证据位置。静态推导、替身测试、真实DB、真实UI/E2E分开；结构检查只证明文档完整。E2E只有未来用户当轮明确要求时才执行，业务动作全部Playwright真实页面，API/DB仅只读。

D1验收仅AC中标D1及必要R；后续阶段条目不成为本阶段功能完成门禁。主 Agent 应按G1—G4逐条判断设计/实现是否满足，不因测试总数多而忽略缺失权限、历史身份或事务证据。任何未确认前提须对应阶段保留为blocker，不能改成默认成功。

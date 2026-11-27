# 最新4份SQL发布登记与预检

合并后的以下SQL未列入release-sources.json，会触发正式构建的未分类SQL阻断。本次仅补齐登记、显式目标预检元数据及对应只读预检，不改SQL业务动作，不改旧迁移、历史账本或已封存的58回填执行包。

| 输入 | 类型与只读预检 |
| --- | --- |
| 20261126_dcc_project_access_reason_check_fix | schema；显式requiresTargetPreflight；仅接受已知varchar512及已知可选CHECK，未知CHECK阻断 |
| 20261126_srm_retirement | menu；强制同名预检；授权表须存在，套餐menu_ids须为合法JSON数组 |
| 20261126_system_signature_reason_capacity | schema；显式requiresTargetPreflight；只接受已知500/1000非空UTF8MB4列，未知或更宽列不缩窄 |
| 20261126_system_tenant_default_login | schema；显式requiresTargetPreflight；须恰有一个启用初始租户，default_login已存在时阻断，不猜测重放 |

现有依赖不变。SRM迁移按已有路径/权限撤销菜单、角色和套餐授权，不按固定站点ID操作；本次不额外新增菜单或权限。SQL业务内容相对原提交保持，仅3条schema头部增加预检标识。

## 验证

- 现有发布源注册4项单元检查通过，全部根SQL均有唯一处置。
- 正式 `intruoyi_build.migration_inputs` 解析930条输入；4个新增登记全部附同名、只读、摘要绑定的targetPreflight，通过未修改的正式只读合同验证。
- 隔离MySQL8.0.39、network=none、无公开端口，执行4个实际迁移：授权原因变可空且历史行不变；签名理由500扩1000且原签名不变；默认登录只选初始租户；SRM根/子菜单与授权撤销、无关菜单/授权/套餐保留。
- 负例覆盖未知原因CHECK/长度、签名更宽列、初始租户缺失/重复、已存在默认列、无效及非数组套餐JSON；均准确阻断。结果见 `release-source-registration-verification.json`。
- 新迁移尚未在58或57执行；57已有真实产品身份阻塞，不能凭本机输入检查宣称可发布。最新固定提交的干净CRLF检出与账本计划另记录，不复用此前5项计划。

固定源码提交 `975d82300bdf468b64048196082901da201113d5` 已用独立Git index读取并干净检出，SQL保持CRLF：930条迁移、1264个包输入、旧封包完整性通过。实际旧检查点855条测试账本与正式服851条本机缓存账本均通过正式audit，各计划9项：新多证、目录菜单、原因CHECK修正、移除CE材料、类型事件通知模板、FDA变更、SRM退役、签名理由扩容、默认登录租户。两个旧产品输入均SKIP_IMMUTABLE，添加FAILED历史负例仍阻断。详见 `latest-release-input-verification.json`。

这是固定源码及已保存账本的离线计划，不是当前58/57目标数据库的preflight或发布通过。维护线程应固定包含该源码提交的构建提交，再对真实候选包逐项预检；恢复/回填执行包保持已交付字节，不随此登记变化。

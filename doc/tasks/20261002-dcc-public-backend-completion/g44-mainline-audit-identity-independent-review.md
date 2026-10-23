# G44 正常页面审计身份独立审查

2026-10-04；结论：NO_OPEN_P1_P2_IN_FROZEN_MAINLINE_AUDIT_IDENTITY_SCOPE。最终3资产及保存验证已核对，修复符合真实正常UI的正式OAuth身份合同；没有发现新的确定P1/P2。源码Review不证明运行服务已加载或新页面事件已验收。

本Agent仅只读System GxpAuditServiceImpl、对应System测试、DCC Ledger fixture及正式用户API、Root安全诊断；只新增本报告。未运行Maven、DB、API、浏览器、服务或Git，没有修改生产/测试，不扩质量政策或其它平台。

## 真实触发

Root `g44-normal-ui-audit-identity.json` 明确6条实际正常页面动作（reviewer配置、folder模板、项目申请/审核/批准/完成）actorId=1且display匹配，但actorUsername为SYSTEM_ACTOR；无历史重写授权。正式OAuth用户info只有nickname/deptId，原Gxp username读取直接回退SYSTEM_ACTOR。前G43维护adapter补principal用户名只覆盖维护入口，不自动修复普通页面的统一审计。

## 最终源码边界

| 检查 | 正式实现与验证依据 | 结论 |
|---|---|---|
| human身份来源 | `GxpAuditServiceImpl#resolveActor(tenantId)` 对有LoginUser的正ID actor检查tenant context/nonignore、LoginUser tenant/ADMIN type；从正式 `AdminUserApi#getUser(id)`取得当前同ID/tenant/启用账号、非空username/nickname | human用户名由当前账号目录提供，不从缓存缺键、display名、admin默认或SYSTEM_ACTOR猜；当前用户缺失等明确拒绝 |
| 不变更登录/缓存 | resolver返回新的LoginUser及新的username/nickname map，不写原LoginUser.info，不设置新的SecurityContext，也不改OAuth/token/账号权限 | 正常页面已有认证和授权仍独立负责，审计仅保存准确操作者事实 |
| 事务和早拒绝 | append原正式事务中，validate命令/策略后先resolveActor，再idempotency查重、allocate sequence及insert | 无效human在任何新事件/序号写入之前失败，不能创建无主审计或fallback成功；真实H2测试核event及watermark均0 |
| 重放保留历史 | exact idempotency hash匹配时仍返回已保存event；没有UPDATE旧event或重新生成canonical/hash；先复核当前actor后再允许重放 | 当前账号改名不覆盖原事件操作者快照；旧6条实际缺陷事件保留，后续真实页面新事件核新用户名 |
| no-login系统任务 | 没有LoginUser时保留既有已明确system caller合同：有效tenant上下文，构造-1/ADMIN/SYSTEM_ACTOR | 这是原系统任务路径，未把invalid human降为system。不能解释为未认证HTTP可任意调用审计，公共调用/策略仍独立校验 |
| 显式-1系统身份 | 有LoginUser但id=-1时仍先要求contexttenant/nonignore/ADMIN，info.username及nickname均明确SYSTEM_ACTOR；其他负ID/nullID或伪造声明拒绝 | 容许已有显式system事实，不接受human错身份或actor查不到时转换-1 |
| 审计数据完整 | actual resolved actorId/username/display进入event及canonicalEvent，原正式append/hash/序号/事务链不改；DCC fixture只补真实测试申请人目录事实并用正式nickname/dept map | 与已有DCC OWNER、维护/登记、实际Gxp kernel接通，不用测试mock系统审计成功掩盖新守卫 |

`AdminUserApiImpl#getUser` 的正式实现读取userService并映射AdminUserRespDTO，明确关闭数据权限过滤以避免已授权操作者自身目录查询被范围过滤；新resolver随后严格复核实际ID/tenant/status。新实现没有扩账号授权或把读取到用户当审批人。

目录中的用户名和昵称是**本次新事件**操作者快照；此前event已经保存的姓名、哈希和canonical JSON保持原样。幂等命令的payload合同也保持，当前actor检查不是用今日身份重写历史。

## 有效RED与最终软件证据

有效RED是实际H2 Gxp kernel输入正式nickname/deptId LoginUser后落SYSTEM_ACTOR，1断言FAIL、无setup error。新增System测试实际写ledger核actorId/username/display/canonical及不改原不可变info；缺/停用/错ID/tenant/空名字/ignore/错误type等核zero event/watermark；directory改名后重放保历史字节并在目录消失时拒绝；无LoginUser和正确显式-1 SYSTEM路径通过、伪声明拒绝。

初受影响DCC回归的22个Ledger errors由原fixture申请人9未在AdminUserApi目录中提供引起，真实新守卫正确拒绝。有限fixture补启用tenant1账号9，使用正式nickname/deptId info并查真实event actor_username；没有生产fallback。最后11类184次执行、0fail/error/skip、BUILD SUCCESS；Kernel24继承22个registration用例，前System15及G43运行重叠，不相加为独立业务场景。

本Agent核对3个current assets bytes/SHA、4个保存日志（RED/systemGREEN/initialRelatedRegression/finalGREEN）SHA、11个当前XML逐类count，共184及all0。只读取XML统计字段，不输出其属性或秘密；未重新执行测试。最终截图/健康状态和实际新UI审计行仍由Root验明。

## 精确封存材料

| 资产 | SHA-256 |
|---|---|
| IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/gxpaudit/GxpAuditServiceImpl.java | 00d5532cffcb83672284a5b482a7bf641d8237c732111776b9ad215d96bf6546 |
| IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/gxpaudit/GxpAuditServiceImplTest.java | 6994c5761fd5d54c2faa678448c37fab5ac801b03bc15eafa331c5aef7a63886 |
| IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectProductLedgerTest.java | 01950677b726d4429f18887ee364afa4221b8046fbb2d398db9e5632a8769ed5 |
| detail task g44-human-audit-fingerprints.json | 0d4ea5a0fcf193b782bfcd0c8f8fb0d01d43a35caf29925c8309f05995a40407 |
| detail task g44-human-audit-verification-receipt.json | fa5c23b176baf221eedff7dc3b1b48661453d99e94edad448997b78fc0192e12 |

继续条件：Root将精确接受版本包装加载到当前任务服务，再由真实页面执行新的明确任务动作，只读核新event的actual actor username/display及原6event保持；不通过直接调用审计或重写旧事件证明修复。旧质量审批规则、本机开发豁免及业务电子签名合同未由本批更改。报告Cleanup Keep交Root统一纳入。

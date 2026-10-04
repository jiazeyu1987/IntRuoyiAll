# G43 本机开发维护入口独立审查

2026-10-04；结论：NO_OPEN_FINDINGS_IN_DELEGATED_DEVELOPMENT_ENTRY_SCOPE。本次有限审查未发现新的确定 P1/P2。取消开发阶段质量批准前置与用户指令一致，真实身份、配置、原件、schema及事务审计门禁仍存在。未启动维护或真实页面，不宣称登记或整条业务验收已完成。

仅新增本报告，未改生产/测试/SQL、未运行 Maven、未访问 DB/HTTP/浏览器/服务或 Git。沿用当前backend任务及既有规则。Review范围为G43冻结Gate/Command/Executor3生产源码、已有Runner/Auth边界和26开发SQL候选；保存的测试证据由Owner执行，本Agent只读核对。

## 代码与执行合同核对

| 核验点 | 实际实现 | 结论 |
|---|---|---|
| 开发豁免范围 | Command在读请求、stdin、Auth之前调用 `Gate#requireDevelopmentEnvironment`；要求local-maintenance且拒prod/production/backup/test/unit-test，真实DataSource JDBC URL只能127.0.0.1或localhost:23306/ruoyi-vue-pro；实际查询源库UUID和session +08:00 | 不是全局取消政策，也不对普通local启动自动生效；Executor事务内重复核同一共享DataSource身份，不能借配置字符串自称测试环境 |
| 请求字段统一 | Gate严格请求字段从qualityApproval改为developmentPolicy；Input record、read、descriptor重载、verifiedScope、require/currentDevelopmentPolicy均使用同一字段 | 不要求缺失的质量文件或批准记录；旧多余qualityApproval字段被严格未知字段拒绝，Root应生成新的准确request/SHA，不能原样复用G33请求 |
| 没有伪造批准 | developmentPolicy仅status DEVELOPMENT_TEST_ONLY、tenant1、固定policyVersion/policyHash/coverageHash五项；3生产类不再查询gxp_audit_policy_version或调用质量事实方法 | 没有以admin/今天/默认引用补造质量批准，开发scope声明与实际策略文本哈希仍严格绑定 |
| 仍需准确26配置 | currentDevelopmentPolicy从固定34-op声明中筛选26个本次DCC操作（排除旧publish），每operation只允许唯一当前行、逐项13个payload字段/版本/active/deleted匹配 | 缺项、错sourceLocator、范围/载荷冲突不能通过；没有以“已配26行”的count替代payload验证 |
| schema与源证据保留 | Gate要求3个InnoDB sidecar表、精确迁移ledger、固定contract/validator seal、实际FIRST/REPEAT journal材料和当前SHOW CREATE TABLE hashes；verifiedScope继续正式严格Factory并要求25 claim/39 evidence/13 name完整范围 | QA豁免没有简化原件/清单/历史/schema校验；注册服务仍按当前源事实与实际正文验证 |
| 真实授权保留 | 原AuthAdapter及Registration authorize保持：真实accessToken行和checkAccessToken一致、tenant1/ADMIN/expiry/currentenabled用户；真实doc_control及update权限 | 不创建LoginUser假身份或借admin超管、refresh token；token仍只stdin、logger OFF及MyBatis statement logger守卫保留 |
| 事务和失败处理 | Executor为真实Spring事务，先currentDevelopmentPolicy再同事务 `registration.activateVerifiedScope`；Gate实际连接用DataSourceUtils，原Gxp kernel与sidecar写链继续正式事务；Command一次性调用并安全报错 | 读门禁和最终写入不是分离的模拟成功；未引入吞异常、非事务降级或失败后自动重试 |
| 普通/正式运行入口关闭 | 原Runner仅local-maintenance且enabled=true注册，默认matchIfMissing=false，显式排除正式profiles；Gate为内部read端口，没有新增Controller/scheduler | bean常驻不等于执行豁免；普通启动不执行登记，正式profile不能启动该开发lane |

环境验证针对当前实际连接；“已有schema证明文件”也要与当前SHOW CREATE一致。Root此前授权26开发配置/历史登记的事实仍由请求authorization seal承载，developmentPolicy不是新的授权替代品。规则/业务电子签名、文件审批以及最终Gxp审计服务未被关闭。

## 26开发SQL及实际收据

候选 `g43-dev-config26.review.sql` SHA为 `cea0fcd4413dfee1f6175089fe35205ee167e9c1df8f05f38eded9e455ba7c23`。源码核：精确源库/UUID/MySQL8.0.40/session/字符排序及授权flag、sidecar/schema条件，锁定正式namespace；首次仅INSERT26 operation，重复全payload匹配后零写；部分既存/冲突明确拒绝，旧publish不修改。业务永久DML目标只有gxp_audit_policy_operation，另有自己的temporary candidate表；没有quality-version INSERT/UPDATE/DELETE，也不写文件/版本/签名。

Root `g43-dev-config-root-review.json` 当前SHA为 `2efcad1309d7e821e646651470d66ae0445d0368cce3123871b0068afee5632c`，它指向的真实protected execution receipt SHA已匹配。实际记录为FIRST26/REPEAT0、qualityVersionInserts0；不是本Agent执行，原批准记录保持。这证明准确开发配置已执行，不证明维护登记已执行或真实页面通过。

## 保存的软件验证与当前指纹

G43 manifest全部6变更资产及7个preserved资产bytes/SHA一致，manifestSHA `611d06410f55953ae4204089d27e10b730787a07e24e5a0e24a0e4c156f311d4`。最终regression log SHA与receipt一致，实际日志汇总7类130次执行、0fail/error/skip及BUILD SUCCESS。Kernel继承22个registration用例，不能把130次执行说成130个不同业务场景；环境/MySQL及外部OAuth端口是明确隔离，未读实际token或DB。

新增验证包含developmentPolicy不含quality字段可读、wrong声明/非开发environment在stdin/Auth之前拒绝、remote/wrong端口/UUID/profile拒绝、当前26完整payload匹配且不读quality registry、runtime payload冲突失败。真实Spring/H2 registration与Gxp事务旧回归继续通过；本Agent没有重跑。

| 最终3生产文件 | SHA-256 |
|---|---|
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceGate.java | 33fb904bce7b6b7c485c66eca02909706f074c85dc9a430c939d53bc091b44ee |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyNameRegistrationMaintenanceCommand.java | 6aa55a4f68ad9eaca45a1c32f15249ace3fff869442438cf5d357bdaada51bc1 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceExecutor.java | cb0f98dd3020c1e92f046aef55d4aa19a3eb807b8f0be028aae5a4e2a207fcf5 |

Root后续按最新同源Jar、实际保护request developmentPolicy/SHA、真实前端登录身份、安全stdin启动维护即可继续；不再请求质量批准。运行前置仍以现有g43-runtime-readiness的slot6/非任务writer/logging门禁为准，源码被新变更覆盖时重新核对本报告。

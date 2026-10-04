# G20 Review 收口与后续门禁

当前status in_progress，完整goal active。这里的“返工”是已有文件申请退回申请人修改的页面状态及驳回后修改重提场景，不指生产工序，不新增审批节点。上传/升版原主流程保持；D03建议不是新的确认依据。

## 当前源代码证据

独立只读Review g19-working-browser-navigation-review.md实际复现NAV-01/02：route换文件但旧detail尚在时误导航、相同HTTP条件下旧list接受新目标。Root在原实际AST handler上新增2个有效RED（11执行9PASS/2FAIL），修复后11/11GREEN。普通详情与返工入口都要求当前正式详情成功上下文，按钮与handler同时拒绝stale/failed读；加载开始清导航ready。列表请求冻结file/Master、fullPath/mode/cache/HTTP上下文，过期响应不替换list或loaded state，选版使用请求快照。

最终受影响16文件组合150执行0失败/跳过，g20-navigation-final-regression.log；正式vue-tsc原relaxed/8GB exit0（87354），三生产文件定向lint0/0。构建正在相同源码上完成；20项源指纹已固定并复核不变。Root当前后端560/11类、infra7项及最新任务Jar package成功（88585，12:58:59），不累计旧测试次数。

最终构建进程36923已确认exit0，日志Build successful。同20项源指纹再核验不变，非任务6项资产原字节hash全保持；g20-verification-receipt.json固定150/560/7测试、types/build/lint/package退出码、日志hash及当前Jar字节数/SHA256。原阶段build进行中说明由此收据替代。所有离线收据不作为真实页面或合入完成证据。

本机登记int_qms slot6为8067/48067，show runtime和branch port guard PASS。未启动服务、未占48081、未操作原四worker。真实Playwright和本地Git合入未执行，离线结果不能替代。

## 数据库升级范围

G20后端独立Review确认19SQL原哈希/内部拓扑/16原表及17新表准确。通用发布器对45闭包将计划执行40项并改旧ledger，不能用；单传19又有17项依赖阻断。真实最小闭包为19执行+25外部前置=44，另1paused activation仅在元数据45包中，本次不执行。

Root保存g20-external-prerequisite-checklist.json逐项25前置及证据来源，状态明确pending_exact_target_contract，不把缺账本自动记成APPLIED。后续只读补精确schema/index/generated/collation和BPM/policy/template/menu前置断言，准备19白名单执行器与旧行不变/新配置33行/新ledger19行的校验及精确恢复。数据库审批尚未答，不创建副本、不执行迁移、restore或ledger写入。

已有三份保护备份校验通过：完整922表schema，210表相关data，16受影响原表的独立schema+data。该范围仅迁移演练与选择恢复，不宣称全库业务恢复或附件灾备；实际writer/Event/事务快照与正式测试admin角色/签名已只读核对，但运行验收前仍需重查。

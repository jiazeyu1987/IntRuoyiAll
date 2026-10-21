# E2E17 R3：仅两旧夹具实际GREEN与原5项定向回归

## 当前范围

按`../20261016-dcc-24-e2e-coordination/repair-dispatch-20261019-r3.md`，继承R2两测试方法修正，本轮不新增生产改动。Maven顺序24→13→17→03→10，24当前独占；17须Root明确放行后执行下面唯一5项命令，不能读10月18日旧XML冒称本轮GREEN。

不重复三人签收，不操作5627，不再把49分40秒人工晚登记当产品失败。该时序结论和原自然触发证据继承`activation-delay-assessment-20261019-r2.md`；组织结论继承`overlap-parameter-assessment-20261019.md`。

## BDD与原RED

- Given Controller现行普通预览调用带明确DAILY context的重载，When 原Controller测试执行业务方法，Then fixture必须严格匹配`daily(900L)`并保留真实成功响应的所有头断言，旧无context重载不得被调用。
- Given 精确下发账号可读取其原下发版本、但未获普通DAILY授权，When 原W08测试两次显式下发读取并尝试用同token走DAILY，Then 原两次读取成功，DAILY拒绝且没有第三次storage/opened，两个ThreadLocal清空。
- Root实际原5项RED为3PASS/2ERROR，分别是Controller旧重载stub不匹配、W08误把普通DAILY入口作为下发上下文恢复。过程/原始XML已封存在`artifacts/repair-20261019-r2/`；R2原文件`.java.before`先于修改保存。详见`repair-20261019-r2-bdd.md`，本轮不再制造另一个RED或改生产去符合旧fixture。

## 最小修正与GREEN门禁

只有两个文件各一个既有方法：

1. `DccControlledFilePreviewDownloadApiTest#previewControlledFile_delegatesToQueryService`：stub/verify同一个DAILY context，旧重载never，水印/内容类型/Content-Disposition/Expose-Headers保留。
2. `DccW08DistributionPreviewTest#readsIssuedPublishedBodyAndSavedEventRestoresOnlyThisTask`：显式下发重读成功、普通入口明确ACCESS_DENIED，storage/opened总2次及上下文清理。未授fixture宽VIEW、未更改Query。

GREEN只接受Root串行放行后的真实命令exit0及**本次**XML中以下原5个方法全部运行、0fail/0error/0skip：上述2项、W08的`byteReadFailureAndOpenedWriteFailureCannotProduceSuccessfulReceiptOrBinary`、事务类的`actualBodySuccessPersistsHumanReadTimeAndReplayWritesNothing`及`lateBodyAuditMissingPolicyRollsBackReadAtWithoutSignatureOrNotificationFact`。

精确可执行命令在`repair-20261019-r3-handoff.json.testCommands`，工作目录IntRuoyiBackend，沿用仓库.runtime Maven。运行日志/过程和本次3份XML需复制到本例唯一`artifacts/repair-20261019-r3/`后再总结。排期未放行时结果明确`NOT_RUN_AWAIT_ROOT_SERIAL_MAVEN`；不需要部署新后端或E2E，因为本轮仅测试维护。若实际出现编译或新行为失败，保留原失败，按精确根因最小修复再原5项复跑，不修改共享生产或扩大回归。

## 业务决策项 D17-01

**问题**：当前已确认规则是“单deptId、保存直接部门的启用成员、不包含子部门”，原E2E-17却增加“同一人属于多个所选部门”的前提。当前合法数据无该输入，原参数保持`NOT_CONSTRUCTIBLE_WITH_CURRENT_FORMAL_ORG_MODEL`，不是已通过，也不是技术自行批准N/A。

业务需选一种口径：

- A（建议）：确认本版本沿用既定单部门/直接成员规则，撤回该跨部门成功前提或正式注明本版本不适用。原三人签收等PASS继承；异常重复身份拒绝、重放幂等可独立技术验收，不能称跨部门成功正例。
- B：坚持合法同人跨多个部门，另行定义正式多归属来源、有效期及每人一份/每部门一份义务；明确覆盖原直接成员规则后另行评估。当前不扩组织、不复制账号、不伪造名册、不创建新任务样本。

未收到业务定案前只登记D17-01为`DECISION_REQUIRED`，不改变原参数状态。该决策不阻碍本轮两夹具和原5项技术GREEN。

# G54 会签动作适用范围独立复核

Status: ready_for_closeout — 2026-10-05，main/int_qms有限只读审查完成。R01正常上传错误依赖整改加载的源码缺陷已关闭，真实页面签名验收由Root继续执行；本Agent没有改生产/测试、运行Maven/前端测试或操作DB/API/服务/Git。

## 结论

本次4份生产源码与对应封存指纹相同，未发现本次范围内新的P1。上传、作废指派不调用升版整改能力；升版保留真实安排读取、资格与签名载荷守卫。此结论是源码及已执行测试证据复核，不是本Agent执行的真实UI PASS。

此前Root真实File2054545668044084026的NEW会签因无条件整改GET失败而在页面产生0POST。现代码明确依据服务器当前任务定义分支，消除了该不适用依赖，不把错误响应冒充空集合或成功。

## 后端正式合同

- `DccWorkflowSignoffAssignmentService#requireCurrentProcessDefinitionKey`（252–267）：从真实当前Task的完整processDefinitionId调用正式`BpmProcessDefinitionService.getProcessDefinition(id)`；定义ID须完全相等，定义tenant须等于任务tenant，任务tenant须等于当前租户。只接受UPLOAD、REVISION、OBSOLETE三个正式key，不从文件原始上传key、字符串前缀或客户端动作推断。
- `assignmentContext`（39–72）：保留当前file/task/义务/leader/assigned资格校验，在响应中加入已验证的current processDefinitionKey。`DccSignoffAssignmentContext`仍将业务Long身份按字符串输出。
- `assign`（75–140）：事务、Master/File锁、签名原因、签名证据、精确义务、部门候选及岗位守卫保留。首次指派先于电子签名在104行拒绝非升版的非空安排。上传和作废合法空/省略安排仍必须真实签名、保存义务和调整任务，但136–138行只在当前REVISION调用`saveArrangements`。
- 升版调用继续传真实Task的processInstanceId，先保存义务真实round，再执行既有安排资格/关联快照/负责人/时间守卫；安排仍进入原签名payload hash，重放冲突仍拒绝。既有已签名同载荷重放的零写入行为未重写历史。

正式定义服务的方法（`BpmProcessDefinitionServiceImpl#getProcessDefinition`，59–60）直接按ID读取Flowable Repository定义；本次没有选择latest代替当前历史任务的定义，也没有修改关系权限策略。

## 前端正式合同

- `applicationRead.ts#getSignoffAssignmentContext`（194–224）：服务响应必须匹配原file/task，并含三个允许key之一以及准确义务、部门、资格和候选。缺少或未知key直接拒绝。
- `DetailSignoffAssignment.vue`（83–104）：校验当前processInstanceId及服务器key后，上传/作废直接进入不适用整改的合法ready状态；仅REVISION读取历史关联、负责人目录和已保存安排。模板19行也仅升版显示整改控件。
- `saveAssignment`（130–148）：保存前再次核file/task/当前轮次、canAssign及ready；升版必须取得控件真实validate结果。非升版使用新payload副本删除relationArrangements字段，最终POST省略该字段，没有发送伪空数组/null。
- 升版读错误（122–125）保持可见且ready为false，不能POST；没有catch403默认成功。切换上下文或卸载的generation守卫仍保留。

## 已有证据独立核对

当前12个源/测试资产均与BE初始manifest及R2追加、FE manifest的bytes/rawSHA匹配，0漂移。实际归档6份BE XML各自SHA匹配，合计69 tests、0失败/错误/跳过；另一个既有真实Flowable当前任务上下文case的归档XML为1 test、0失败/错误/跳过，也与其单独收据匹配，不将其写成新一次70项完整回归。

有效BE RED记录为4失败/0错误，证明旧非升版save调用及非空载荷接受；首次注入端口异常的旧证据仍保留，但不冒充正式权限守卫。FE最终日志及5份原日志SHA匹配，50 tests/50 pass/0 fail/0 skipped；离线测试执行实际SFC脚本和实际API wrapper，网络/目录端口为显式替身，不能据此宣称实际HTTP或真实用户签名已通过。Root另行报告全类型及构建退出0，本Agent未重跑。

H2事务测试保留真实Flowable任务和领域证据一起提交/晚失败回滚；签名验证端口使用测试替身，不能当成实际密码签名验收。新增既有上下文case读取真实引擎定义、正式定义服务和真实义务，但用户/BPM读取端口为显式隔离夹具。

## 当前生产指纹与封存入口

| 源码 | SHA256 |
|---|---|
| DccWorkflowSignoffAssignmentService.java | cb6f0010cb5f4fd60946071f5f4100f18c6fc798628dff13a1b21c78f3799754 |
| DccSignoffAssignmentContext.java | 63a5db055cab9d12183616796dba68f2fad8c9872750798fed550d68acfc6b4e |
| DetailSignoffAssignment.vue | 7ddce1e2f06563c697fd9966af568ba4b925bddbb4cf0933ba30152ee4eec513 |
| applicationRead.ts | 6fa4714ac2d023ec70ff7cdc4689f9d883c386a5cbbb815310ccb557465f507e |

BE `g54-signoff-fingerprints.json`：d595597800804270aaf6d964398023e340d31d5476222df15be4cce063715577；R2追加manifest：a66a1db79eb05101293a12977f51d7d64d900da0d5596a4ec4620aa5c58e2ba5。FE `g54-signoff-remediation-fingerprints.json`：faaada06f22d6ae0938304cc045be63aa19a98c72dde61a0ddf17c818eda0d50。完整相对文件路径、XML及原日志描述符见上述原收据，本审查不改其记录。

## 未纳入本次关闭

预受控NEW没有current relation set时，附属当前关系区域的`DCC_RELATION_CURRENT_SET_NOT_INITIALIZED`显示问题仍未关闭，本次没有GET初始化或改变受控生命周期。升版前指派的既有安排读取/轮次行为按原严格协议保留，真实升版页面验收仍待Root，不以本次NEW适用范围修复替代全部关系业务验收。原只读报告中的初始NULLround解释及证据更正继续保留，未把未发生的POST403写成事实。

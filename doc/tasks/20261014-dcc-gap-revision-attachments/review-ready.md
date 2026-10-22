# G06 软件域已正式接收 / M12冻结交接

## 最终全软件接收（M14补齐）

Root最终有效47份后端class报告1008/1008，0F/E/skip；11865输入0漂移。M14真实exit0/23reactor SUCCESS，G48声明18+原父类27共45/45已消除M12唯一45ERROR，其余45类963PASS无生产差异。F02 38/38、T04类型与B01构建通过，2429前端生产输入0漂移。

最终正式证据已只读核验并存final-software-acceptance.json；全软件域已接受。生产/tests继续冻结，没有重复任何套件或扩大范围。仅Git提交/推送收尾未授权，Root统一清理/报告；未要求E2E/部署不列软件失败，工程不completed。

下方M12剩余45ERROR等为历史中间状态，已关闭。

## M12当前正式结论

Root M12实际联合已经结束并接收本线程全部软件域。A34全6、附件37、上传8、Query272及旧5DB211均0F/E/skip；本线程已实际读取并核对9份fresh XML的SHA。记录输入为**11865份、0漂移**。

整体M12：1008 tests、963PASS、45ERROR、0FAIL/skip，reactor exit1只因G12 G48 BeforeEach缺required审批来源fixture；Root正仅复验该45。不能把整体M12称全量PASS，也不能把该G12错误作为G06待修业务。

m12-software-acceptance.json保存当前正式报告摘要、各相关class计数及global边界。生产/tests和a34-frozen-sources.json指纹一致；本次只更新证据，无执行任何套件。按Root指令继续冻结，无已知未修G06业务缺口；工程Git/cleanup未授权，task状态blocked仅指工程收尾。

## 当前状态与精确测试范围

2026-10-15，实际分支int_qms。本线程已按Root最后指令停止生产和自有测试写入；仅整理文档/SHA。当前没有已知未修的本轮授权业务缺口。G03负责完整custom驳回→检入→候选→重提组合；本线程6项不冒充全流程/运行验收。

Root读取的A34 5/5之后，在冻结消息到达前新增并已完成同范围公共Legacy保持用例，故**最终是6/6、javac0/JUnit0、0skip、drift=[]**。两生产文件没有因第6项再改。run_id=ec8e60d46b42401d82ff22baa8583be8，2026-10-15 13:26:40+08结束。没有再跑75/211或自跑Maven/完整类型/构建。

A34类：cn.iocoder.yudao.module.dcc.service.file.DccCustomDraftInheritanceIntegrationTest。直接继承BaseDbUnitTest，无继承业务测试；声明6方法：

1. a34_queryCheckinCallsActualTypedServiceAndPersistsNewVersionRoundCanonical
2. a34_formalCandidateCallsActualTypedServiceAfterCreatingItsRealReservedRound
3. a34_customCasFailureRollsBackCreatedVersionAndRoundThenRetries
4. a34_formalCandidateNeverPersistsCanonicalForAnotherFileEvenDuringItsInsert
5. a34_candidateCustomCasFailureRollsBackNewBodyAndReservedRoundThenRetries
6. a34_publicLegacySourceDoesNotAcquireCustomFieldsOrAnotherCustomRound

## 最终两源码SHA256

- QueryServiceImpl：6fd2262d22939dafcc5985c6ba6cda5e2b8d0f8bd171b81091ae16c6da9d98fb
- RevisionServiceImpl：a01c1019c8ca73555828b8113bef7b62760489d738f50bd632be8e57e1efbba8

完整路径、Git blob、测试hash、声明方法与冻结时间见a34-frozen-sources.json；A34改前见a34-before.json。其它M10/M11冻结生产保持，不改G03公共Workflow/DTO/API/Panel或G07 Guard。

## A34已落实的最小差异

- Query在真实INSERT/initializer/prepared之后调用G03 readInheritedCustomDraft，用原tenant+id+WORKING+nullBPM CAS写三字段并检查affected=1，本地next同步。custom缺typed结果明确失败，public返回null不写custom。
- Revision在INSERT前明确清空target三custom字段，绝不让源file/round canonical进入新target；源对象不改。实际custom来源才调用正式initializer建立target真实reserved round，然后typed服务重建canonical、原CAS写入三字段并同步本地target。原public通道不新增initializer，不动正文/manifest/权限策略。
- 必要新增Resource：Revision.workingApplicationDraftInitializer。G03手工构造Revision的组合fixture须wire真实initializer，不能用mock initializer或typedrecord替代真实target轮次；Spring正式构造按Resource注入。
- 真实typed服务、CustomMatrix canonical、V6 definition、initializer/round/SQL均参与。外部文件存储/目录/项目权限是明确测试端口；空附件是该fixture给定事实，完整附件与物理副本验证保留在M10/M11。
- CAS晚失败分别验证检入与正式候选的file/attributes/round回滚后重试；DB exact-file约束保留且各测试finally清理。单独新增公共Legacy例确认null custom继承/原prepared/public值不变。

## 已正式接收的既有软件checkpoint

- Root M11：23模块BUILD SUCCESS，附件37+引用16+权限集成13共66/66，0F/E/skip，3066源路径0漂移；formal receipt与fresh XML已实际读取。该票是A34前时点，不能覆盖新hunk。
- Root M10：539项中523通过、16资源cwd ERROR；仅修测试真实root解析后private16/16，M11附件整37已正式消除该16错误。没有复制/猜测schema；production未因路径修复再动。
- 私有旧5DB 211/211、新附件及相邻75/75；Root M09另正式Query272/新Body23通过。依Root指令，已过套件不再重复。
- canonical前端检入/附件相邻75/75；后续实际G03 helper+Panel与检入3文件28/28。Root A35旧确认fixture5/5已接收，之前两项缺helper错误已关闭。
- Root T04完整types、B01独立Vite test build已PASS；本线程没有并行大构建。源码阶段不同，不冒称当前Java最终联合已完成。

## 交还请求与边界

Root已按当前6方法与两源码hash完成M12正式联合并接收A34。此后没有Root新失败授权，不再改冻结资产、不重复套件。

无Git提交推送、E2E、服务重启、远端或真实业务库写入。生产DDL未执行实际库；隔离H2/Flowable不冒充真实用户运行验收。G06软件域已接收；任务blocked只因工程Git/cleanup未授权，完整12项与工程收尾由Root统一收口，不自动标completed。

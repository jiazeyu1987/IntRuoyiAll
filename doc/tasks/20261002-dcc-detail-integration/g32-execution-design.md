# G32 历史占用清单与内部执行入口准备

状态：`offline_builder_ready_for_root_review_actual_sources_incomplete`。无生产41/现有Jar修改、无Maven/应用启动、数据库/API业务写、对象恢复、GXP配置或Git。本批只task helper/离线builder/验证/设计；提取依赖及javac都属于任务工具，真实Java factory/rowHash直接来自已封存Jar81b56...0bc5。

## 清单生成门禁

实际g23原283facts（25claim/25Master/39版本/39原infra/39ownership/76ticket/39reference/runtime）精确scope。Root未来只读fresh采集与当前39全部实际GET匹配回执必须同一factsSHA，readerexit0/resultsSHA/status/每source hash和长度完全相同；一个缺源、重复ID、跨tenant、wrongname/config/路径或scope漂移均拒绝。

目前实际回执35MATCH/4missing：`g32-manifest.py`只输出`blocked-source-diagnostic.json`，不需要DBpreimage、不生成activation-manifest。这不是按数量忽略4份旧文件，不能把它们删掉或默认VERIFIED。未来Root获准恢复后重新全部GET/freshfacts，再继续。

离线builder输出严格`RootManifest`字段和Evidence，Long保留decimal字符串，旧claim formal nullable值保持NULL；不回填项目/编号/原名。verifiedAt由实际UTCfinishedAt转Asia/Shanghai截微秒，而非编造批准时间。scope identity canonical排序字符串字段与实际Java identityHash一致，真正compiled strict factory离线消费fakeallMATCH已PASS。

## 原预像哈希的真实协议

G29 protected baseline每rowSHA是SQL HEX-concat协议，**不是**`DccLegacyNameVerifiedScope.rowHash`，不能互换。本批使用task同package Java helper调用真正rowHash，Root可在显式只读模式对实际JdbcTemplate `SELECT *`四表取得Map，包括byte[]/Boolean/SQLTimestamp/Long/BigDecimal/NULL原类型，生成Java canonicalSHA与必要immutableidentity投影。builder逐fact对照身份，未把SHA自报字段视为原数据库事实。

helper使用正式YudaoJacksonAutoConfiguration customizer/module及application.yaml已确认相关Bootflags，`JsonUtils`nullprobe必须是`{"nil":null}`。全map键lowercase TreeMap；byte[]→lowerhex、SQLTimestamp→LocalDateTimeISO、其它String.valueOf完全由真实方法处理。单纯离线typedfixture也调用同方法，测试NULL存在和缺字段不同。**未启动的真实runtime mapper/profile尚未实测**；未来执行前Root必须核实际应用mainObjectMapper null inclusion/serialize flags与本probe一致，若不一致明确BLOCKED_PREIMAGE_PROTOCOL并重新取得匹配preimages，不能猜hash或改productionrowHash。这是当前可审准备的运行态限制。

readonlyhelper固定local source23306/DB/UUID/MySQL8.0.40、readOnly+REPEATABLE_READ；每组全部ID精确25/25/39/39；四表全原列，config28仅读取storage/非秘密locator投影，完整配置只内存，账号密码由标准输入、不argv/文件/输出。当前没有实际调用该模式。它也不会创建Spring Boot应用、登录账号、角色或业务对象。

## 具体输入和Root命令边界

`g32-input-contract.json`列全部必需事实。Root在当前sources不完整时可仅运行builder获得blocked诊断；不生成潜在可执行JSON。

```powershell
python -B -X utf8 doc/tasks/20261002-dcc-detail-integration/g32-manifest.py --facts <fresh-facts283.jsonl> --receipt <actual39GETreceipt.json> --results <actual39GETresults.jsonl> --output <new-protected-child>
```

完整来源匹配后再提供actual old userdecision、Java actualpreimages、scopeId/reason/requestId。新输出目录不得覆盖历史包；protected路径可能含源objectkey，只放任务受保护目录，不进Git/公开日志。canonical新bundle通过actualstrictJavafactory仅说明结构/来源摘要校验，不是已签名审批或已数据库登记。

## 无Controller的内部维护执行设计（尚未实现）

Root需要在新3表具体DDL、全部39正文、质量策略生效和新增名称登记范围都获具体授权后，审查独立本地一次性入口；不得通过APIseed实现E2E业务动作。建议未来仅当已有真实前端登录session的OAuth token由正式OAuth2TokenApi真实checkAccessToken并读取enabled同tenantAdminUser/真实doc_control+updatepermission时，才使用正式SecurityFrameworkUtils建立相同LoginUser（username/nickname从真实用户，不猜admin），然后正式Bean `DccLegacySourceNameRegistrationService.activateVerifiedScope`。

入口只接受Root已经review的sealedartifactID+exactSHA，factory验证真实facts/results/decision/scope后调用真实service；同正式DataSource/事务+GxpAuditService.append，未注册该真实operation/QA未批准则全rollback。token仅标准输入/内存，不写入manifest/日志/argv；真实用户显式动作与Root技术执行原因/request可追溯。不存在“userId=admin就认证”或SYSTEM_ACTOR替代。该入口没有在本批实现；它涉及真实维护认证机制与具体写入范围，必须先由Root核正式tokenAPI和授权，不扩平台/新增REST/审批节点。

本批结束仍只是准备：sources35/4不完整，actualpreimage collect0，actualactivation0；代码和task工具准备通过不表示整体业务验收、数据库演练或融合完成。

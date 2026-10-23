# G11 A04/A07 最终修改文件与 SHA256

指纹为最终定向验证时当前工作副本；改前指纹见 execution-log.md，新增资产无改前指纹。

|路径|SHA256|
|---|---|
|IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue|9AFC64409FBFED86E3FCF9158125F78C0F582F69EB283935D75E5451F6374A0D|
|IntRuoyiFronted/src/views/dcc/controlled-file/detail/relation-contract.ts|F06D1B495E436E5BE0B6E46780ED97017E52772B42F7499844C5CE5CAF8F9CC3|
|IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationHistory.vue|0A7D970F74E55437A65934A0296411B788138EE557CA4B83BDEC61C4B48236D6|
|IntRuoyiFronted/src/views/dcc/controlled-file/detail/browse-records.ts|0D9CA2479F6E9701B03CD53D2A1E2CF0FAD23B33B6DB230DA40ECBBDF423BBA5|
|IntRuoyiFronted/src/api/dcc/controlledFile/relations.ts|F548D1B93937AE1BDE16353ACB8E37FE31418C374AB7351BB85A98C2E0671B36|
|IntRuoyiFronted/src/views/dcc/controlled-file/relations/reference-usage.ts|2A14CC50E59FFDC1D9BA2D43EF2924BC58D5288A2C8EF7090D5F28B290B1FAC4|
|IntRuoyiFronted/src/views/dcc/controlled-file/relations/DccReferenceUsageDialog.vue|ECEA0975BA0CD12E87E7ACA8A4BC992F6CECCCA51D5E97492587EE8C35DC5FCB|
|IntRuoyiFronted/tests/unit/dcc-current-relation-source.test.cjs|32563165812563E2663D17D17C788F76762505DD66675F385C187F12742B52BC|
|IntRuoyiFronted/tests/unit/dcc-selected-version-relations.test.cjs|74D76FC1DCCC2C213AD85DB17EC3FB8B67DB7A9C6913FD423E1961789C58F725|
|IntRuoyiFronted/tests/unit/dcc-selected-application-context.test.cjs|01CFEBFA76D8BDDFA22639265211449B684C75E0DF2D08BA7DDFF863A1C9586E|
|IntRuoyiFronted/tests/unit/dcc-reference-usage.test.cjs|859DD5F542B2DA7EF8230B034511374B91D8774E6FF2A122E737C6183ECAB1AC|
|IntRuoyiFronted/tests/unit/dcc-reference-usage-dialog.test.cjs|A38E6DD4CF8BA00FEE3C096883818F5C1F4FA3197C85C29F55EEB222AB2163B5|
|IntRuoyiFronted/tests/unit/dcc-selected-version-reference-summary.test.cjs|7F39A1BE47449FBFA9A17C202609AFBB49FD4F9EF398068B6BF567F74B8EEA84|
|IntRuoyiFronted/tests/unit/dcc-detail-closure.test.cjs|798BB76762D32B772216FD4B9A787385D705240DD56BE2D52FCE426ED2069467|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccProjectReferenceService.java|BABB9D022912C90C20B3DE74E5557B84E9591E5481AFBB6EAB5FFDB27DB46F59|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccRelationStore.java|594170A82C42A51497C4240F071AD7514D031566C447CB1C62368F38080B2B63|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccRelationContracts.java|28D2972A37B937729A429BB14B006D8C84BD8B787A082665B13DD4682C952600|
|IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccExactVersionReferenceUsageTest.java|C631CB4BAEA0760F3162F72FDFD539CF7B2C4BB8B7ABC884078D4872050B1952|
|IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccReferenceUsagePageDatabaseTest.java|E19FE473EB3263163266FA15B3C39614EDEEDBBD2C2C359DEE7C690ADA7494EB|
|IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccProjectReferenceTransactionTest.java|33826836F2EC151DFD218B6675286007E54566D558CB5A224A1C213CDBDE3A14|

## 范围说明
- 仅上表生产/测试和本任务记录由 G11 修改。DetailRelationsPanel 初始已有脏改动，保留原只读业务边界；不能把整体 Git diff 等同全属 G11。
- browse-records.ts 初始已存在未跟踪文件；本轮在授权后收紧正式 summary source/tenant 校验。
- dcc-browser-html-alignment.test.cjs、父页 index.vue/ControlledFileBrowseRecords.vue 的接线/fixtures 是 G10 并行资产，G11 只读和运行回归，未写。
- DccProjectReferenceController.java 已授但无必要差异，当前源码已fresh私有编译，未修改；无需端点/请求字段/数据库迁移。
- 未改 Query、applicationRead、G08 DccProjectReferences/显示helper、签名/审计公共服务、生产schema或无关资产。
- 私有测试输出 private-reference-red/green/regression 仅任务目录内编译产物；清理应删除，保留 run-private-reference-tests.py 可复跑脚本。
- 任务记录：task.md、execution-log.md、verification-report.md、integration-notes.md、reference-schema-evidence.md、review-ready.md、maven-validation-request.md、changed-files.md。

## A33 latest source checkpoint

Current code hashes in a33-checkpoint.json; only the following A33 assets changed. Query/Guard were compiled read-only, not edited.

|Path|SHA256|
|---|---|
|IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue|A0D8B65233C81A034CB9499B6655F70AC7DA7B694D235EAB3A78F5BB71124ADD|
|IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationStageEvidence.vue|57FC56DC1374698707F7D3B7D89EAD4B8CEC1FDBCD64160001DBA795EF972F64|
|IntRuoyiFronted/src/views/dcc/controlled-file/relations/DccReferenceUsageDialog.vue|89DDD4B88ED1EACCF0C5ED96576EC3421808E573C8744F1EC00C69C8AE824EAD|
|IntRuoyiFronted/tests/unit/dcc-selected-version-relations.test.cjs|285FAA78D0CF7945997269581DF7DF1C34787DA60827AD2718C456F87BC28278|
|IntRuoyiFronted/tests/unit/dcc-relation-stage-evidence.test.cjs|0A61A5EC0EB5D385BA7C979803DE31805CC6646B88E49AB8467CF68B9068D053|
|IntRuoyiFronted/tests/unit/dcc-reference-usage-dialog.test.cjs|938A44FE43070D9CE08B97084839742FF43AA369A8707A459ACCCC2CCF8C755B|
|IntRuoyiFronted/tests/unit/dcc-fixed-version-preview-test-loader.cjs|35A3C24B57E4F51AB842F36FEDC5E8B92F571EFBB841ABBEB95107C57A294067|
|IntRuoyiFronted/tests/unit/dcc-current-relation-source.test.cjs|D0092D2C6C75057B0A58804E943FF7472320512D7538C088E9465285E074EDAD|
|IntRuoyiFronted/tests/unit/dcc-detail-closure.test.cjs|2E40ADAA3496CFEC3CBC11BB40CDEDD61ED17844DD032A88D5ECCC766679D0E8|
|IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccReferenceUsagePageDatabaseTest.java|120ECB8C189E050AE8CB007803A1E98845359D991EBC71FBACA6BA2CCC0EDC3A|

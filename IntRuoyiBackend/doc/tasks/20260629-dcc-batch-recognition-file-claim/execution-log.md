# Execution Log - 20260629-dcc-batch-recognition-file-claim

BDD: 两个并行批次包含同一文件时只能有一个执行器识别 -> Given 两个等待中的批量识别任务都包含同一 controlled_file_id / When 两个执行器并行轮到该文件 / Then 只有成功拿到数据库文件认领的一方调用正式识别，另一方不得重复识别该文件。

BDD: 单文件识别入口遇到文件已被其他执行器认领时暴露真实错误 -> Given 某文件已存在未释放的基础信息识别认领 / When 用户再次调用单文件 recognizeProjectCode / Then 服务直接报“文件识别中”类错误且不得写业务字段成功结果。

BDD: 文件识别完成后释放认领，后续任务可继续按版本规则判断跳过 -> Given 执行器已成功认领文件并完成识别落账 / When 同文件后续再次进入批量识别 / Then 已释放的认领不会阻塞后续任务，仍由成功账本与业务字段完整性共同决定是否跳过。

BDD: 识别失败后也必须释放认领 -> Given 执行器拿到文件认领后识别抛出异常 / When 失败路径结束 / Then 文件认领被释放，同时失败账本与真实异常照常保留。

RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" test` -> FAIL，新增并发互斥测试引用的 `DccControlledFileRecognitionClaimDO` 与 `DccControlledFileRecognitionClaimMapper` 尚未实现，证明当前代码库还没有文件级识别认领正式模型。

GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" test` -> PASS，单文件入口与批量入口均已接入数据库文件级 claim；同文件竞争时不会重复调用识别服务，识别完成或失败后可正常释放认领。

GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_sql_scripts.py -k recognition -q` -> PASS，新增 `dcc_controlled_file_recognition_claim` migration 与测试 schema 契约均已覆盖。

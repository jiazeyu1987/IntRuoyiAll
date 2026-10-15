# G02 Changed Files — 第一阶段

当前正式软件范围已Root接收：M07两G02类21PASS，四授权源码按g02-software-handback.json冻结归还Root；本线程不再写或重复私有测试。工程Git未授权不标completed。旧下文是阶段历史记录，当前hash以交还manifest为准。

最新M08同八类联合55PASS/BUILD SUCCESS，G02仍自身21项/四源码0漂移；只追加正式回执证据和更新自有记录，没有解除源码冻结或追加生产/测试改动。

最新公开组合阶段仅在原两专属test新增嵌套fixture和两publicCommand场景，未新建未授support生产/Java测试文件。两生产服务与A13hash相同，当前4文件hash以public-command-ready.json为准。新增私有runner方法selector与当前源码列表，所有输出仍自有private-junit，无sharedtarget。

## A13 当前授权四文件

- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileLifecycleService.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccG02ControlledSignatureTimeTransactionTest.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccG02DelayedFinalizationTransactionTest.java

两服务为G02修改、两测试修改前ABSENT；G02-only diff以Root source-baseline的内容SHA与G02改前SHA完全一致后生成，保存在a13-production.diff，4文件最终hash在a13-changed-source-manifest.json。没有并写G01或其他owner生产文件。

仅下列自有文件新增；修改前整个任务目录 ABSENT。没有本线程生产代码、共享测试、数据库、配置或共享协调文件变更。

- doc/tasks/20261014-dcc-gap-controlled-signature-time/task.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/execution-log.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/design.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/integration-notes.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/changed-files.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/review-ready.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/verification-report.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/baseline-fingerprints.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/verify-design.py
- doc/tasks/20261014-dcc-gap-controlled-signature-time/c01-end-event-test-design.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/c01-followup-baseline.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a07-interface-composition.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a07-integration-baseline.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/check-integration-source.py
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a11-source-checkpoint.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a13-production-before.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a13-production.diff
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a13-changed-source-manifest.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a13-implementation-evidence.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/PrivateJUnitRunner.java
- doc/tasks/20261014-dcc-gap-controlled-signature-time/run-private-junit.py
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-command-before.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-command-ready.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-command-verification.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-guards-before.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-guards-current.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-guards-verification.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-training-before.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-training-ready.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-training-verification.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/pre-m07-training-source.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/m07-formal-g02-evidence.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/g02-software-handback.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/m08-formal-joint-evidence.json

private-junit/内为私有编译/测试产物与命令/日志/source manifest；关键命令、结果、失败分类和限制已经归档a13-implementation-evidence.md/verification-report.md，classes不作为最终提交资产。当前未执行cleanup/Git收尾，不动并行目录。

baseline-fingerprints.json 保留首阶段只读证据；c01-followup-baseline.json 保存 C02 文档更改前资产指纹与新增时序源码证据。verify-design.py 只做本目录文档结构/UTF-8/源码路径锚点校验，不是行为测试。两生产文件/两测试已获 C02 授权，但 M01 冻结中，尚未修改；其他共享依赖未授。

A07 最新覆盖冻结背景：BE 冻结已释放，四文件 WRITE_GRANTED，Maven 暂无唯一票。a07-integration-baseline.json 记录 11 个已有自有资产修改前 hash、16 个生产/测试/依赖路径现状。唯一 G01 Evidence/确认服务/V6 图当前 ABSENT，新字段/Rule 尚未接入；本轮只新增完整接口组合文档，没有生产/Java测试改动。

A11 接续仅新增源可用性检查脚本/JSON checkpoint，更新自有状态。它记录当前源码身份与 API声明，不能证明签名/完成/业务。DO/date/Rule进度与仍未出现的 Evidence等按 checkpoint 为准，不覆盖A07历史证据。没有新增整套设计或历史测试重跑。

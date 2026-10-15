# G01 当前实现与验证报告

2026-10-14启动，2026-10-15持续实施；int_qms；HEAD=822c31fc0856516cf59ba9cd84bf2bc7f455d17f。当前WRITE_GRANTED生产接线及定向GREEN已有；尚无完整工程验收GREEN。最新具体测试与边界见execution-log.md后段。

## 正式软件范围验收（Root M08，覆盖旧待验证状态）

M08已真实exit0/67秒，23reactor BUILD SUCCESS，八目标55/55、0失败/错误/跳过。签名旧行为11、producer2、G02受控时间10、G02真实公开End11、公开确认service4、Evidence9、V6模型1、HMAC7；八个fresh XML各自计数不重复继承叠加。A31模型资源cwd错误已关闭。

official-m08-receipt.json保留Root票号、实际起止/命令、每类counts/XML SHA256和范围；独立冻结50文件重新核对无漂移。M08整仓仍有3并行source变化，Root正式接收的是G01/G02软件范围，不是custom/附件/所有12项。后续Root T03完整pnpm ts:check已exit0、无diagnostics，T01/T02旧错误关闭；类型诊断与业务PASS分别记录，不替代全项目最终冻结验收。本线程没有自行全types/Maven/sharedtarget并行。

当前软件已交付；工程Git当轮明确禁止，按项目收尾规则不得标completed。清理仅preview，结果独立归档；无部署/E2E/服务/实库/Git写入。

软件接收后的自有cleanup preview实际exit0：keep23、delete候选168、warnings0、required evidence保留、删除0。cleanup apply NOT_RUN，task最终blocked仅因Git禁令和工程收尾边界；不能将blocked写为业务FAIL，也不能将preview当apply PASS。

## 当前验证（覆盖下文初始SPEC记录）

2026-10-15；WRITE_GRANTED，生产代码及真实定向TDD已落地。主申请24文件已Root A23接收转G03/G06，G01停止写入。详情/签名/确认私有职责继续收口，未标工程completed。

最新统一私有checkpoint：Root M05已正式23reactor main/testCompile PASS且80tests通过后，G01改用其fresh依赖及全新private输出目录，对32个自有即时source执行--suite：23/23PASS、0skip、compile/junit exit0、sourceStable=true。receipt为private-java-current-source/suite-receipt.json；未再以A23之前旧mainclass替代当前依赖。此结果仍不是全source reactor最终联合票。

A30最后标签已完成：lifecycle新pending正式statuslist/中文标签、stage-name新stage中文；RED1→GREEN3，并与真实BasicInfo/严格datetime2项组合5/5PASS。缺actionProjection/正式blocked时敏感动作保持拒绝、四矩阵阶段/withdraw原表保持。独立文件已按independent-handback.json冻结给Root M07，不反复重跑自有大组合。

Root M07正式结果更新：signature11PASS，DCC44项43PASS/0FAIL/1ERROR；唯一DccWorkflowV6BpmnTest资源路径依赖private仓库cwd，在Maven模块cwd错误多拼IntRuoyiBackend。A31已用明确root属性/正式Maven模块basedir契约修复；目标两文件javac与路径探针从模块cwd执行exit0，缺/错配置明确拒绝。八条真实模型分支及断言保持，等待Root M08复验该唯一ERROR；没有重新跑23private冒正式模型通过。

Root M06实际旧ElectronicSignatureServiceImplTest PASS及main通过；DCCtestCompile因G06 fixture漏import结束，七DCC目标未执行，未把依赖PASS当新确认链正式PASS。Root后续M07同8scope仍是唯一正式reactor票。

| 项目 | 本轮实际证据 | 结论边界 |
|---|---|---|
| DELAYED/null与panel | 新submission/confirmation RED→GREEN12 | 实际helper/API载荷/真实Vue handler，S不截断、错误输入保留、迟到响应隔离 |
| 共享FE阶段checkpoint | 72/72PASS | wrapper3+新mode12+saved申请28+matrix25+G12可选原因4；只证明归还当时source，不冒称后续owner最终源码 |
| C04 actual metadata parser | RED1→GREEN2 | 非闰日、2月30、offset19/18:01、24点拒绝；合法Z/+08/18:00与unavailable原具体错保持；归还后G03维护 |
| A25 actual parent | RED→GREEN5 | 真实GET带attrRound7，缺/错/历史round零请求；watch前route变化亦不接受旧响应 |
| A27 native进度 | RED3→GREEN3，组合15PASS | V2/V6 exact、四阶段/文控后training/本轮actual确认；立即无确认、历史不借当前file；旧native8保持 |
| Java生产Selection | RED→GREEN | 即时javac调用DELAYED/null及非空date拒绝，无sharedtarget |
| 唯一Evidence | 缺类RED8→GREEN8，追加signedAuth后9PASS | actual verifier，Bound/BPM/crypto明确mock端口，不称全验签 |
| V6模型Flowable | 缺模型RED→GREEN1覆盖8分支 | H2真实部署、2动作×2mode×2training；candidate invoker明确port替身 |
| actual HMAC/Subject | RED2→GREEN3，扩为7PASS | actualCanonical/HMAC/Management重建verify/encode-decode-load；E/round篡改拒绝，旧v4无新字段 |
| 真实subject容量 | 5515字符/bytes插VARCHAR2048 RED→fixed399同表保存回读GREEN | 无扩subject列、无截断，完整意图为server one-call scope，reason500 |
| C05 producer/DB | 2/2PASS | 真实统一Service+DCCproducer+3Mapper+H2TIMESTAMP6+两真实验签器；统一result/DB/DCC/fileconfirmedAt/controlledTime=S；E改INVALID/S改MISMATCH；late tx两表0+retry成功。文件列SQL仅证明时间存储，不冒完整Lifecycle业务 |
| 公开确认service端口组合 | 4/4PASS | 实际service成功→CAS→audit→BPM；同载荷replay复用S/改E冲突；CAS/audit/BPM晚失败拒绝。persistence/sign/BPM/audit明确mock，不称DB事务回滚 |
| Root M04→M05 | M04 main全部编译PASS、旧double缺method；A28补后M05 23reactor成功/四类80PASS | A28只补旧Capturing精确抛错，原断言保持；80仅该四类，不替代G01新需求联合验证。本线程不Maven |

命令：FE cwd=IntRuoyiFronted，node --test执行G01专属dcc-delayed-effective-submission/confirmation、dcc-delayed-confirmation-round、dcc-delayed-progress、dcc-body-read-wrapper-g01、dcc-preview-time-parser-g01及已列相邻；BE cwd=根，node doc/tasks/20261014-dcc-gap-delayed-effective/run-private-java.cjs默认Evidence，或--model/--hmac/--roundtrip/--command。Java17参数文件仅写自有private-java-evidence。新receipt记录自有即时source hash/sourceStable/实际exit；A23归还main和外部依赖可使用前一checkpoint class，runner明示范围，不冒称当前全reactor。

G02 public-command-11实际producer→公开ConfirmationService→官方BpmTaskServiceImpl→End→Lifecycle/Finalization→真实GxP组合2PASS，Root已review接收阶段。本线程引用跨owner证据，不复制为独立复验。最终冻结-am/完整types仅Root发票，尚未完成全项运行验收。

新迁移只写文件：新6列及C05统一/DCC signed_at、file controlled_time三列DATETIME6 engine/nullability/schema守卫，无UPDATE/历史回填，未执行实库DDL。无E2E/服务/远程/发布/worktree/Git写入/子Agent。原始classpath/私有class/日志为临时证据，不随源码提交；默认保留报告、设计与可重现测试脚本。

## 初始SPEC阶段基线实测（历史，不代表当前缺实现）

| 命令与 cwd | 退出码/结果 | 可以证明 / 不可以证明 |
|---|---|---|
| git branch --show-current；仓库根 | 0 / int_qms | 实际分支；未切分支 |
| node --version；仓库根 | 0 / v24.19.0 | Node 可用；无安装依赖 |
| node --test doc/tasks/20261014-dcc-gap-delayed-effective/delayed-upload-red.cjs；仓库根 | 1 / 3 tests，2 PASS，1 expected FAIL | 实时生产 buildUploadEffectiveChoice 不认提案 DELAYED/null；当前 PRESET_DATE/null 拒绝，立即 mode/null 接受。失败为“请选择是否在文控标准化后生效的生效方式”；不证明后端运行复现 |
| node --test tests/unit/dcc-upload-ic05-application-options.test.cjs tests/unit/dcc-detail-application-ic05.test.cjs tests/unit/dcc-upload-effective-activation-times.test.cjs；IntRuoyiFronted | 0 / 53 tests，53 PASS、0 fail、0 skipped | 本轮实际执行相邻 helper、页面 handler、SFC 合同；它们维护旧预设日期需求，没有证明延迟新确认实现 |
| git check-ignore -v -- doc/tasks/20261014-dcc-gap-delayed-effective/delayed-upload-red.cjs；根 | 0 / .gitignore:103 | 脚本被忽略，已列 Cleanup Keep，未来需精确强制暂存 |

## 初始静态事实（后续实现见上文）

完整链路与路径/方法锚点见 design.md；源码规范化指纹见 source-baseline.json。核对实际上传、草稿保存/恢复、submit iteration、revision 前置、文控签名、TRAINING trigger、模型 End、Finalization、Lifecycle、签名 canonical/验签及前端 strict decoder。没有从旧任务 PASS 推断已完成。

## 初始SPEC未执行项（已由当前证据替代，保留历史）

- 后端/Flowable/隔离数据库 RED/GREEN/REGRESSION：本阶段 SPEC_ONLY，尚未授予共享生产/测试文件和 Root 唯一 Maven票。上述 RED 探针只覆盖生产 helper，不替代完整 TDD。
- 新确认 UI runtime：尚未授权生产组件；仅有设计和真实后续方案。
- E2E/业务库/API 操作/服务/远程/部署/worktree/Git 写入/子 Agent：本轮明确禁止。
- cleanup apply/工程 completed：有明确阶段与 Git 授权限制，未执行；只交第一阶段结果。

## 文档结构门禁

后续执行自有文件 UTF-8 回读、必备记录/BDD/状态/Keep、所有前缀路径/锚点存在性（新增文件为 ABSENT）、source-baseline SHA256 与命令结果对账。通过结论在 execution-log.md 追加，不伪称功能 GREEN。

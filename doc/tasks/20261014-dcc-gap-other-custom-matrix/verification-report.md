# G03 验证报告

## 最终软件验收已接受（当前结论）

已只读核对 Root final-verification.json、m14-verification.json、m14-reactor-receipt.json 和 M14 reactor 日志。M14 真实 exit 0、23 reactor SUCCESS，G48 声明18与原父类27独立fixture合计45/45、0 fail/error/skip；M12 其余45类963 PASS 无生产差异。最终有效47份class报告共1008/1008 PASS，11865输入零漂移，M12历史45 fixture ERROR已解除。

F02四suite38/38、T04类型检查exit0、B01构建exit0均通过；2429生产输入零漂移。G03自有53项（含最后NEW replay新断言）、A34 6项和各原父类已正式通过，全软件域由Root接受。

生产/tests/fixture/runner继续冻结，本轮只同步记录，不重复验证或扩大scope。工程状态为blocked，仅Git提交/推送未授权，不标completed；Root最终清理/报告统一办理。未要求的E2E、部署、真实库迁移或服务操作不作为软件失败项。正式汇总与证据SHA见official-verification-receipt.json。

## M12/F02 正式结果（历史，45 fixture ERROR已由M14解除）

Root 已实际完成 M12 `-am` 46 类：exit 1，1008 tests / 963 PASS / 0 FAIL / 45 ERROR / 0 skip；11865 输入零漂移。已只读核对 Root m12-verification.json、reactor receipt、source drift 和 F02 原始结果。

G03 自有 7 类 53 项全部正式通过：canonical 23、token 8、Workflow 9（含最后 direct NEW replay 新增断言）、rework 1、native guard 2、类别 4+6。A34 6 项及独立原父类 42/38/79 均通过。原“当前两源增量/最后新断言待验”限制已被正式 M12 解除，不重复私有套件。

唯一 45 ERROR 来自 G48StorageProjectionTest@BeforeEach 合法 Category fixture 漏 required approval_source_mode；Root A37 给 G12 只补显式 PUBLIC_MATRIX，不改生产/schema。M13 仅该类 45 复验，整体最终结论由 Root 给出。F02 四 suite 38/38 实际通过，source 未改；G08 只补旧宿主实际依赖，G03 未并写。

以上为M12当次历史结果，保留其exit1和45ERROR，不把它改写成当次全PASS。最终软件验收依据为本页顶段M12有效963 + M14独立45合计1008/1008。当前工程仅Git收尾未授权，Root统一清理/报告。

## M12 前冻结 checkpoint（历史，待验限制已由上述结果解除）

当时实际分支 int_qms，生产、自有 tests、声明方法、fixture 和 runner 保持冻结。以下保留 M12 前的真实输入与限制，不代表 M12 后仍待验。最新正式结论见上节。

- 已有实际结果：private Workflow regression-custom-13-public-v2 9/9、稳定 rework-10-stable 1/1、RF11 green 2/2、纯服务 23/23、token 8/8、类别 4+6、最新四文件 FE 12/12；均只保留其各自当次源码、执行和外部端口边界。
- 旧主链 9 项之后 Resolver / Execution 两个生产源因 rework 更新，WorkflowTest 最后新增 direct NEW 同载荷 replay / 异两账号载荷拒绝断言。旧主链 sources.json 与当前有这 3 处差异，不能写当前 9 项全源已 PASS。
- 稳定 rework-10 编译了当前生产源；与当前 sources.json 仅 WorkflowTest 有差异，该类不是其执行目标。JUnit 1/1、Java exit 0、Node runner exit 0 / SOURCE_STABLE 已实际确认。rework-9 曾 source drift，虽 JUnit 通过仍不计稳定 PASS。
- rework 使用真实 Query/Revision/SourceOwnership/typed 继承/Flowable；前置终止并持久 REJECTED 为显式 fixture，不宣称电子签名拒绝或真实 UI 动作已经验收。
- 本轮只读 SHA：54 当前文件、3 Root/G04 上传文件、2 G06 源依赖。自有 Java 7 类共 53 个声明 @Test。三份继承子类只选声明方法，原父类由 Root 独立完整回归。
- 旧 dcc-detail-application-ic05 宿主缺失 isPublicMatrixExecution 实际依赖由 Root A36 交 G08 修测试依赖；G03 不并写。任何补宿主后的真正产品 RED 只按 Root 精确返修。
- 本线程停止所有 private Java/Node 重复套件、共享 target、全类型和构建；未执行真实 E2E、服务、数据库、远端或 Git 提交/推送。任务保持 in_progress，未 cleanup apply / completed。

下文是历史阶段证据，保留原始命令与失败边界；其中“等待 G01 接管”“rework 尚未 GREEN”等阶段待办已被本节更新，不能据旧文覆盖当前事实。

## A20最新结果

专属实际SFC-template/UI按钮/正式类别读端口：测试载体初次微任务/空白定位修正后真实RED4fail（没有读saved source、缺失/失败还open、旧回执串弹框），GREEN4pass，新增旧错误晚回REGRESSION5pass。三文件23/23pass/0skip。

```powershell
# cwd=IntRuoyiFronted
node --test tests/unit/dcc-category-review-g03-source.test.cjs
node --test tests/unit/dcc-category-review-g03-source.test.cjs tests/unit/dcc-category-g03-approval-source.test.cjs tests/unit/dcc-upload-g03-custom-matrix-editor.test.cjs
node tests/e2e/dcc-review-matrix-hide-columns-static.spec.js
node tests/e2e/dcc-review-matrix-tab-static.spec.js
```

前三当前结果PASS；最后exit1在该旧script:143期待未改CategoryMatrixDialog.vue的label="标记"，不属于A20来源投影/Table类型修复，没有改Dialog或放宽测试。目录名e2e的两个脚本只是静态文件assert，没运行真实E2E。

全类型当前仅Root T01旧exit2定位；没有Root新票，本线程未跑全类型，也不宣称完整类型已通过。真实传CategoryVO来自精确saved类别，不optional/any/cast/defaultsource绕过。

## R03/preview最新实际证据

|范围|RED|GREEN / 回归|边界|
|---|---|---|---|
|真实类别projection/Controller|RED-r03-row: compile0，9started/4fail/5pass|GREEN9/9；REGRESSION-r03-row10/10，0skip/abort|显式private目录端口+真实当前source/H2类别端口；非完整reactor。|
|FE真实decoder/helper/CategoryForm|6started/3fail/3pass（blocked行被旧整页拒、helper丢字段、混合非法不拒）|GREEN6/6；新增技术失败后四文件32/32|实际compiled源方法/SFC，控件与网络显式隔离；非E2E。|
|纯服务明确preview/reserved|RED-preview: compile0，21started/5fail/16pass|GREEN21/21；REGRESSION-preview23/23，0skip/abort|没有file0/默认round/public route；digest不是签名proof。|

命令（cwd仓库根，证据与source-stable hash在对应task子目录）：

```powershell
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase RED -RunId r03-row
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase GREEN -RunId r03-row
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase REGRESSION -RunId r03-row
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-private-junit.ps1 -Phase RED -RunId preview
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-private-junit.ps1 -Phase GREEN -RunId preview
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-private-junit.ps1 -Phase REGRESSION -RunId preview
```

FE同上一轮4文件最终node命令本轮32/32pass，新增7项类别源码行为其中包括original技术错误对象传播。Response使用真实Jackson NON_NULL全局配置仍保持source/blocker显式null，完整候选坏A+合法B/C+levelD保留。

此前A14“坏leaf拒整页”已经Root R03明确移出门禁，旧7/8/29项对应历史行为不作为当前目标。公共V1/V2旧混合失败仍记录不覆盖；G01负责。完整申请保存/票据/主管HMAC/冻结/重提未接，本次不宣称该链PASS。V01共享schema/旧test由G05，不并写。

## A10/A14最新结果

- 真实CategoryForm模板/wrapper RED4fail→GREEN4pass；A14 leaf-null断言RED1fail→GREEN。最终4份FE定向测试29/29pass。
- 当前完整BE管理源码+H2真实category端口：缺字段初RED4fail，增加模型字段后真实业务RED2fail/2pass（缺严格校验），实现GREEN4pass；projection RED2fail→GREEN，A14缺映射leaf RED1fail→GREEN；当前Controller委派/权限注解回归最终8/8pass，所有0skip/abort，source稳定。
- 证据category-junit/<phase-run>/result.txt/exit.txt/sources.txt；脚本run-category-junit.ps1，未写target。框架/BaseDO/无关Mapper/tenant端口明确隔离，目标Admin/Taxonomy/Controller/CategoryDO/VO均当前完整源文件编译，真实Hutool复制与H2JDBC写读；不能称完整MyBatis、tenant/security拦截器、GxP审计事务或运行验收。
- SQL静态6/6pass；正式政策工具完整3文件闭包passed，a10-migration-policy.json（不是SQL执行）。错误basename参数及未缓存security版本fail-fast均记录日志，没有伪写PASS。
- 公共create_tables.sql新增来源列及老CategoryAdmin fixture必须由Root合入，当前未写；完整reactor无票未跑。

```powershell
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase RED
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase RED -RunId behavior
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase GREEN
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase RED -RunId projection
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase GREEN -RunId projection
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase RED -RunId leaf
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase GREEN -RunId leaf
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1 -Phase REGRESSION -RunId controller-2
python -X utf8 -m unittest IntRuoyiBackend/script/tests/test_dcc_category_approval_source_mode_sql.py
python -X utf8 IntRuoyiBackend/script/release/run-release-migration-policy-gate.py --sql-root IntRuoyiBackend/sql/mysql --sql-file IntRuoyiBackend/sql/mysql/20261015_dcc_category_approval_source_mode.sql --sql-file IntRuoyiBackend/sql/mysql/20260719_dcc_file_type_taxonomy.sql --sql-file IntRuoyiBackend/sql/mysql/20260513_dcc_base_schema.sql --output doc/tasks/20261014-dcc-gap-other-custom-matrix/a10-migration-policy.json
```

最终FE（cwd=IntRuoyiFronted）：

```powershell
node --test tests/unit/dcc-category-g03-approval-source.test.cjs tests/unit/dcc-upload-g03-custom-matrix-editor.test.cjs tests/unit/dcc-upload-free-file-type.test.cjs tests/unit/dcc-approval-matrix-shared-contract.test.cjs
```

类别DO已增加字段，未迁移真实库会缺列/既有fixture缺字段会拒绝；这是未部署/共享schema接入边界，不使用运行fallback。本轮不修改Master/Version/历史申请/公共矩阵，不执行实库。

## A08最新证据（2026-10-15）

独立typed后端和C01独立组件已有授权实现。下文SPEC证据为历史阶段，不表示当前只有设计；完整申请/管理/迁移/签名proof/驳回链仍未接入。

|阶段|本轮真实命令|退出/结果|证据|
|---|---|---|---|
|BE RED|run-private-junit.ps1 -Phase RED|javac=0；JUnit=1，11启动11失败0skip/abort|private-junit/red-result/exit/sources.txt|
|BE GREEN|run-private-junit.ps1 -Phase GREEN|javac=0；JUnit=0，11/11通过0skip/abort|private-junit/green-result/exit/sources.txt|
|BE REGRESSION|run-private-junit.ps1 -Phase REGRESSION|javac=0；JUnit=0，16/16通过0skip/abort|private-junit/regression-1-result/exit/sources.txt|
|C01 FE RED|node --test tests/unit/dcc-upload-g03-custom-matrix-editor.test.cjs|exit=1，初始9项缺组件；随后追加真实行为2失败（render崩溃/ID回显）|execution-log.md本轮实际输出摘要|
|C01 FE GREEN|同上|exit=0，11/11通过|execution-log.md|
|本轮FE相邻回归|组件 + upload-signoff-departments + upload-ic13-matrix + approval-matrix-shared-contract + upload-free-file-type|exit=1，60 tests/59 pass/1 fail/0skip|唯一旧IC16期待V1，而G01实际begin为V2；未改owner共享测试|

后端完整命令（仓库根、PowerShell，当前脚本默认RunId=1，历史RED/GREEN在追加RunId前执行）：

```powershell
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-private-junit.ps1 -Phase RED
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-private-junit.ps1 -Phase GREEN
& ./doc/tasks/20261014-dcc-gap-other-custom-matrix/run-private-junit.ps1 -Phase REGRESSION
```

本地Java17.0.20.1，当前授权生产源码/专属测试私有编译；Jackson2.17.2、JUnit5.12.2/Platform1.12.2第三方依赖。每阶段不同输出，无共享target、无旧项目jar；runner失败/skip/无测试均非0，脚本核对source前后hash。纯服务无模拟Workflow/DB/组织。PASS证明规范化与结构编译，不证明启用/岗位/权限/签名或真实保存事务。

FE完整命令（cwd=IntRuoyiFronted）：

```powershell
node --test tests/unit/dcc-upload-g03-custom-matrix-editor.test.cjs tests/unit/dcc-upload-signoff-departments.test.cjs tests/unit/dcc-upload-ic13-matrix.test.cjs tests/unit/dcc-approval-matrix-shared-contract.test.cjs tests/unit/dcc-upload-free-file-type.test.cjs
```

唯一失败位于dcc-upload-ic13-matrix.test.cjs:165，actual PUBLIC_MATRIX_NATIVE_V2 / expected V1；Root A06已授G01新V2合同。该共享测试与approval-matrix.ts尚归G01，G03未改，未把混合回归写PASS。自有组件故意注入错误props对象仅产生Vue类型告警，无Unhandled render错误。

没有Maven票，不跑reactor/全类型/大构建；E2E、服务、实库写入、远端、Git仍禁。任务保持in_progress待正式接线，不伪称completed。

## Scope

SPEC_ONLY。仅当前源码复核、已有相邻独立Node测试与自有文档结构检查。没有生产改动，不宣称G03实现或运行验收通过。

## 实际测试

工作目录 IntRuoyiFronted，Node v24.19.0：

```powershell
node --test tests/unit/dcc-upload-signoff-departments.test.cjs tests/unit/dcc-upload-ic13-matrix.test.cjs tests/unit/dcc-approval-matrix-shared-contract.test.cjs tests/unit/dcc-upload-free-file-type.test.cjs
```

本轮返回 exit=0，47 tests、47 pass、0 fail、0 skipped，耗时约1.8秒。

- 会签测试执行当前生产helper和实际页面handler：精确部门ID、明确空集合不恢复默认、晚响应不授权新选择、组织上下文切换。
- 公共矩阵测试执行当前生产状态机：冻结原基线、变更确认、精确准备定义、主管source凭证与失效；其中包含局部虚拟TypeScript检查，不是全工程检查。
- 草稿共享合同执行当前applicationRead包装：精确保存基线、空历史不猜、正式新业务基线就绪及保存；一项为H2 schema静态合同。
- 类型测试执行当前helper、校验和实际SFC handler，并含模板AST/静态断言：真实正式类型身份/类别映射，不表示支持其他自定义。

这些测试依赖显式隔离外部端口；非E2E、非线上服务或实库验收。相邻PASS只能冻结当前能力，不能替代G03新增需求测试。

## 相邻后端测试阅读

本轮读取了 DccPublicMatrixNativeDerivationTest、DccNativeUploadSupervisorWorkflowTest、DccWorkflowAttributesIntegrationTest、DccWorkflowRevisionIntegrationTest 的当前源代码。其方法为后续真实Workflow/H2/Flowable、冻结/事务/重放测试提供结构参考。没有执行这些测试，没有复制其历史PASS。

## 自有文档结构

已实际执行，工作目录仓库根：

```powershell
node doc/tasks/20261014-dcc-gap-other-custom-matrix/validate-documents.cjs --record-fingerprints
```

exit=0，PASS：36个现有源码/规则锚点、8份相邻测试源码、6条Given/When/Then及全部自有记录结构。生成UTF-8/LF规范化SHA-256指纹。此检查只验证设计记录，不称产品GREEN。

git check-ignore -v 确认 validate-documents.cjs 命中根 .gitignore:103 的 doc/tasks/**/*.cjs；已在 task.md 的 Cleanup Keep 中逐行登记。当前无Git授权，未暂存；将来提交时需Root对该自有必要脚本明确 git add -f。其他7份Markdown均在任务限定 git status --untracked-files=all 可见。

## 产品RED / GREEN / REGRESSION

未运行：SPEC_ONLY没有生产写授权与Root Maven唯一票。integration-notes.md 已列实际Workflow/Resolver/现有API与SFC方法的真实RED方案和隔离事务/重放回归；接入后才能执行。

## 未运行 / 待接入

- G03自定义类型身份、编辑、保存/恢复、提交、驳回沿用、公共矩阵零变化：待Root冻约及G01/G04转交。
- Maven、全类型、大构建：没有Root唯一票。
- E2E、服务启动/重启、远端、真实业务库写入、Git提交/推送：本轮明确禁止。
- 没有工程completed；最终产品及Git收尾待后续授权。

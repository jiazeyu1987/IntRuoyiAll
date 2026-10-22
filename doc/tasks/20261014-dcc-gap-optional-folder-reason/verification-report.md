# G12 阶段验证

## 最新结论
软件验收已由Root正式接受。本次最新Root回执：M14真实exit0、23 reactor SUCCESS，G48声明18+原父类27=45/45、0F/E/skip；与M12其余45类963 PASS组成有效47份class报告1008/1008，11865输入0漂移。F02 38/38、T04类型、B01构建也通过，2429生产输入0漂移。原M02 13/13 GREEN+5/5回归证据保留；M13混跑问题已由M14正确fixture范围验证解决，不是当前失败。所有生产/tests冻结，唯一未授权事项为Git收尾；清理和总报告由Root统一办理，未要求的E2E/部署不列软件失败。本线程只记录Root正式回执，未重跑任何套件。

## 已运行
- 改前相邻基线：node --test tests/unit/dcc-upload-actual-attributes.test.cjs tests/unit/dcc-upload-project-folder-context.test.cjs tests/unit/dcc-upload-single-project-folder.test.cjs → exit 0，17/17。
- RED：node --test tests/unit/dcc-upload-g12-optional-folder-reason.test.cjs → exit 1，6 FAIL / 5 PASS，真实空说明必填阻断。
- GREEN：同命令 → exit 0，11/11。
- REGRESSION：node --test tests/unit/dcc-upload-g12-optional-folder-reason.test.cjs tests/unit/dcc-upload-project-folder-context.test.cjs tests/unit/dcc-upload-single-project-folder.test.cjs tests/unit/dcc-upload-confirmation-summary.test.cjs tests/unit/dcc-upload-w03-safety.test.cjs tests/unit/dcc-upload-effective-activation-times.test.cjs tests/unit/dcc-upload-actual-attributes.test.cjs → exit 1，50 PASS / 1 FAIL。失败为 actual-attributes 的旧“空说明必须拒绝”断言；精确调整交 Root，未隐瞒/跳过。
- 公共 wrapper 接入 RED：node --test tests/unit/dcc-upload-g12-api-optional-folder-reason.test.cjs → exit 1，4 FAIL / 2 PASS。缺省说明被实际 wrapper 拒绝，501 字反而未拦截。共享生产保留 Root，测试交接。
- 完整 upload/index.vue 经 compiler-sfc parse / compileScript / compileTemplate → PASS；git diff --check 定向授权文件 → exit 0。

## 证据边界
实际执行生产submitter、属性及部门模块；页面冻结函数从现行SFC AST执行。A02正式wrapper已接入并真实函数6/6，最终57/57。后端使用真实服务/mapper/GxpAuditService/H2验证归档与真实actor/tenant/serverTime/before/after/空串原因，失败比较事务前后精确DB快照并可重试。真实前端E2E/部署运行验收未执行，隔离测试不替代运行验收。

## 未运行
本线程未重复执行类型/构建/套件；Root已正式确认T04/B01通过。E2E、部署、服务重启及真实业务库写入未执行，均不属当前软件验收必需门禁，不据此记录软件失败或阻塞。Git提交推送未授权，工程状态因此保留blocked、不写completed；最终清理和总报告由Root统一办理。

## A02/M01新增证据
- A02先再跑6项API RED：4FAIL/2PASS；改后同命令+原51项 →57/57，exit0。
- M01首轮：12项4failures/6errors/PASS2，含3个参数项目重名fixture错误；m01-red-junit.xml保留。
- M01 RED2：同-am/g12*命令，13项5failures/6errors/PASS2；m01-red-r2-junit.xml保留。参数唯一性及旧strict策略已修；其后2个整表COUNT夹具断言已在M02改为任务身份+精确事务快照并实际GREEN通过，不作为当前未解决项。
- 独立policy条目结构/ID唯一/精确sourceLocator、旧bind及人工目录REQUIRED保持 →PASS。全局审计服务未改。
- 前端交还hash及后端源码稳定性证据已归档，不含Git提交/真实数据写入。

## M02最终GREEN与回归
- 首次GREEN session77493：exit1，13项4FAIL/0ERROR/PASS9；4空说明参数预期误用基类username，实际归档记录当前类真实fixture storage-owner。仅修专属测试预期，原XML与日志保留。
- 最终GREEN session56689：2026-10-15T00:05:03.8805777+08:00至00:05:44.9298983+08:00，mvn -pl yudao-module-dcc -am "-Dtest=G48StorageProjectionTest#g12*" "-Dsurefire.failIfNoSpecifiedTests=false" test →exit0，13/13，m02-green-r2-junit.xml。
- REGRESSION session65204：00:14:32.7871966+08:00至00:14:54.8746539+08:00，带-am显式选择本类5方法→exit0，5/5，m02-regression-junit.xml。完整选择表达式及方法名见m02-receipt.md/process-receipt。
- 两次最终运行起止15份关键来源全Same=true，启动前/结束后活跃Maven0；profile=unit-test、jdbc:h2:mem:testdb。源码/fixture/pom/config清单与稳定性JSON完整保留，非旧target报告。
- 500/501、空/缺省/空白、错误项目/文件夹/tenant/失效、撤销EDIT、旧strict理由、重放不搬位置不改事件、缺server context、审计缺独立策略回滚及late caller全部DB快照回滚/重试均实际PASS。
- 审计动作upload-bind仅派生新上传使用OPTIONAL_TEXT；人工bind/目录维护仍真实非空理由，全局内核与共享基类未改。

## A37 fixture-only handoff
- M12统筹结果由Root提供：46类实际1008中963 PASS / 0 FAIL / 45 ERROR / 0 skip，11865输入0漂移；45 ERROR归因于本类继承fixture遗漏 `approval_source_mode`，不是产品RED。本线程不复跑M12。
- A37唯一修正：G48 `realActorDirectory()` 的自有 category INSERT 增加 `approval_source_mode='PUBLIC_MATRIX'`。改前SHA256 `FFD67BF735452C5FC3AF04D590FFD2E0C500A26F55FCCD1CD4C37E7476216FAA`，改后 `97A99DC4E34F987EBA42B3B2E8F7D06F76158F248DF5431D7EE234CD9AF02354`。
- BDD为fixture-only：保留45项测试、原权限/继承/负向断言，不改schema默认/nullable/生产；Root M13唯一 `-am` 整类复验。未运行Maven、服务、数据库、E2E或Git。
- Root已确认G48相对M12仅该一条INSERT变化，生产全无差异；M12其余963 PASS已归档。M13句柄41579由Root运行中，正式结果未回传前不判定45项通过，本线程不重跑。
- M13正式结果（Root回执）：39/45 PASS、2 FAIL、4 ERROR、0 skip。失败/错误均来自父类 `DccProjectFormalCombinationTest` 方法错误地在G48子类 fixture上下文执行：父方法再插目录101/1001冲突，且父方法空属性表假设与G48额外事实冲突。分类为验证设计问题，不能归为产品缺陷或A37 hunk失败。
- M14由Root统一执行：G48声明测试及18参数case单独覆盖；父类原整类27项在正确独立fixture运行。G12线程不改父fixture/业务断言、不运行Maven；正式M14 XML/receipt待Root回传。

## M14最终Root回执（替代上段等待状态）
- Root正式确认M14真实exit0、23 reactor SUCCESS，G48全部声明18项与原父类整类27项在各自正确fixture覆盖45/45，0F/E/skip。原45项断言保留，M13继承fixture混跑的2FAIL/4ERROR不作为当前软件缺陷。
- M12其余45类963 PASS保持有效且生产无差异，最终合并有效47份class报告1008/1008，11865输入0漂移。
- Root另确认F02 38/38、T04类型及B01构建通过、2429生产输入0漂移，全软件域正式接收。该结论来自Root统一验证回执，不冒充本线程新执行结果。
- 本线程源码/tests保持冻结，无新代码或测试；仅Git收尾未授权。未要求E2E/部署不列软件失败；Root统一最终清理/总报告。

## Root最终接收与归还
- A07及本轮Root回执已独立确认freshXML 13/13和5/5、0F/E/skip、BUILD SUCCESS和最小diff，软件G12正式接收，M02已收回。
- 两服务/G48所属测试归还Root并冻结，policy YAML归还后由G01仅增confirmation条目，旧strict/upload-bind保留。A04前端已交G01，G12无共享生产写入或Maven票。
- m02-final-receipt.json引用原运行receipt、XML及source-stability，保留其SHA256与实际结果；不重跑、不把其他线程后续改动套用为G12已测版本。

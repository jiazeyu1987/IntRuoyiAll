# G12 执行记录

## 最新软件验收回执（2026-10-15，Root统一验证）
- 全软件域已由Root正式接受。M14真实exit0、23 reactor SUCCESS；G48声明18+原父类27=45/45，0F/E/skip，均在各自正确fixture验证，原断言全保留。
- 与M12其余45类963 PASS合计有效47份class报告1008/1008，11865输入0漂移；Root另确认F02 38/38、T04类型、B01构建通过及2429生产输入0漂移。
- M13父子fixture混跑分类及失败原报告保留，M14已解决验证范围问题，不作为当前软件失败。A37仅一条fixture INSERT hunk，其后G12源码/tests保持冻结。
- 本次只同步Root正式回执，无新代码/套件/服务/DB/Git写入或跨线程消息。工程仅Git收尾未授权而保持blocked，不标completed；清理和总报告由Root统一办理，未要求的E2E/部署不是软件失败/阻塞。

## 2026-10-14 源码复核与授权
- git branch --show-current：int_qms。未切换分支、未触碰既有并行改动。
- 已读取 AGENTS.md、docs/task-closeout-rules.md、docs/frontend-development.md 的通用与上传相关门禁、docs/backend-development.md 通用与审计门禁，以及协调目录 requirements.md/coordination.md。
- 当前源码：页面 freezeUploadApplicationDraft 和 submitter placementReason 都拒绝空说明。
- 当前后端：DccPublicUploadPlacementService#create 调 audit.validateReason；DccProjectFilePlacementService#bind 再次校验，并 reason.trim() 写审计。前端修复不能冒称已打通归档。
- 相邻 dcc-upload-actual-attributes.test.cjs 中有“empty placement reason must reject”的旧要求；不擅自改未明确授予的现有公共测试，向 Root 列精确调整点。
- 修改前 hash 见 task.md。先记录 BDD 再执行 RED/GREEN；不执行 Maven/全类型/大构建，无验证票。

## 前端 RED
- 基线：在 IntRuoyiFronted 执行 node --test tests/unit/dcc-upload-actual-attributes.test.cjs tests/unit/dcc-upload-project-folder-context.test.cjs tests/unit/dcc-upload-single-project-folder.test.cjs，17/17 PASS（包含旧“空说明应拒绝”断言，只作为改前基线）。
- node --test tests/unit/dcc-upload-g12-optional-folder-reason.test.cjs → exit 1，11 项中 6 FAIL / 5 PASS。空/缺省/空白载荷、save/submit service、实际页面冻结均抛出“请填写500字以内的文件夹登记说明”；属性/账号负向也被该必填提前遮挡。
- RED 已实际运行生产 submitter 模块与 AST 抽取的实际页面函数；没有修改生产后重建 RED，也不是字符串断言。

## 最小前端修复
- 页面冻结仅保留说明长度检查，界面提示“选填”。
- placementReason 对空值/空白返回 undefined，使 JSON 不发送该可选字段；真实非空说明仍 trim 且校验 500 字，不造默认说明。
- 额外依赖：IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts 的 assertControlledFileSubmitRequest 强制 assertRequiredString(projectFolderChangeReason)，需 Root 接入；未经授权未修改。

## A01 后端准备
- Root 已授予两个 placement 服务及精确所属测试；policy YAML/公共 API 保留 Root。真实 RED 前不改后端生产。
- 独立新动作 dcc.project-file-placement.upload-bind 的完整 policy 已写 integration-notes.md；旧 bind 与人工文件夹维护仍必填真实原因。
- 复核发现 Gxp reason 当前 schema 是 NOT NULL（生产迁移与 H2 一致）；审计缺省原因记真实空串，不记 null、不编造说明、不改 schema/全局内核。
- G48StorageProjectionTest 改前 SHA256：E277AC67AD9D12C6E9534120841D9418E3D566D57590AA0AEBCBDD7B040A6C35。将在该现行派生 storage fixture 中新增 g12* 行为，等待 Root 唯一验证票。
- 已新增 9 个 g12* 方法（12 个参数调用）：空/空白/缺省实际归档+真实 actor/tenant/time/before/after、500/501 字、错误项目/文件夹/租户/失效、撤销 EDIT、独立审计策略缺失回滚、重放不改事实/不搬位置、晚失败回滚并重试、人工目录维护/严格 bind 保留必填、无服务器 context 拒绝。未执行，不能称后端 RED/GREEN。

## 公共 wrapper 独立 RED 与结构验证
- node --test tests/unit/dcc-upload-g12-api-optional-folder-reason.test.cjs → exit 1，4 FAIL / 2 PASS：两个正式 wrapper 对缺省说明报 missing required field；两个 wrapper 也缺失 501 字上限校验。精确项目/文件夹/禁止 caller directory 测试 PASS。此新增专属测试供 Root 改 wrapper 后复跑，不写公共生产。
- Vue compiler-sfc parse / compileScript / compileTemplate 现行完整 upload/index.vue → PASS；只是局部 SFC 编译，不是全类型/构建。
- git diff --check（两个授权前端文件及所属 G48 测试）→ exit 0；只读取当前差异，无 Git 写入。
- 前端交还时 SHA256：index.vue=1E365E68A4DF696CD42100D93933B285FC8B8C5F3C280136B16EDD259ADEE41C；submitter.ts=2452F2502B61B6008508CB04DBA72A2A58E66B56D9AA8E486B34789C9A2F5DD0。
- 两个后端服务 SHA256 与改前一致，未先改生产来跳过真实 RED；本轮没有 Maven/验证票。

## A02 / M01 接收
- Root 核准 upload-bind OPTIONAL_TEXT 合同，并精确授权 workflow.ts 说明可选 type/length 锚点、actual-attributes 旧空说明断言、policy 仅新增条目；其它共享生产仍保护。
- A02 后先重跑公共 wrapper RED：6 项中 4 FAIL / 2 PASS，exit 1，同缺省及 501 字实际缺陷。
- M01 唯一 Maven RED 票：检查 Java/mvn/classworlds 活跃进程为 0；保存 m01-source-manifest.json 后启动带 -am 的 G48StorageProjectionTest#g12*；统一执行句柄 session_id=26257，日志 m01-red-maven.log。仅 RED，生产后端未修改，GREEN 尚未授权。
- A02 改前 SHA256：workflow.ts=173AC579623852C086E4C00A68005E87EFFCFF96790D381836FA0642EB0904AC；actual-attributes=F1C1E95C3F905E956E3AF997FF7BD62286A4AEB727309C6A3127DAEF0F2FCB9C；policy YAML=92ABE7BC7061FFDEB41A164C38279838C08B2B1CF095B82FBDDFB45A3B38C48C。
- A02接入wrapper仅原因锚点并更新旧说明断言后，实际8个测试文件57/57 PASS，exit0；独立新审计policy已新增，旧必填不变，结构/唯一ID/source定位验证PASS。
- M01 session26257真实exit1，12调用failures4/errors6/PASS2/skipped0；profile=unit-test、jdbc:h2:mem:testdb。fresh XML已复制m01-red-junit.xml，细节见m01-receipt.md。
- 参数空说明场景[2..4]被同名G12-EMPTY项目唯一索引拒绝；仅在专属测试改UUID项目名。不将夹具错误冒称业务RED；其它实际缺陷见receipt。
- Root读首轮日志后准许同M01票修fixture再RED：同时注册strict bind策略、有效原因到达真实ledger/late caller边界，新增1个独立真实ledger晚失败回滚场景（现13调用）。生产不改，无全局schema/清理变更。
- M01 R2执行前活跃Maven0；保存m01-red-r2-source-manifest.json，按相同-am/g12*命令运行，session_id=60701。GREEN未授权。
- 已封存a02-frontend-release-hashes.json，可将前端共享文件立即交还G01。新增M01审查证据纳入Cleanup Keep；整体仍in_progress，不提前收尾。
- A04 Root已正式接收并将三个前端文件转G01，本线程不再写，旧actual-attributes也归还。
- RED2 session60701真实exit1，13项failures5/errors6/PASS2，fresh XML m01-red-r2-junit.xml已复制。参数重名已解决，四空说明参数均真实必填RED。
- RED2两个晚失败测试在真实callback/ledger后失败于整表placement COUNT残留（跨项目1/4行）；fileId不存在和storage mapping回滚断言已走到。仅将G12 placement计数改project+tenant，不清公共schema/数据，不改共享基类；未第三次Maven，完整回滚测试尚未PASS。
- 详细命令/起止/退出/来源清单及剩余夹具分类均见m01-receipt.md RED2段。等待Root审查下一票。

## M02实现/GREEN票
- Root接受真实产品RED但保留混合分类，收回M01，授唯一M02 GREEN及G48本类专属5方法回归；其它BE冻结。
- 回滚测试补真实事务前后精确快照：当前任务project的file/master/placement/mapping、当前租户directory/ledger/sequence均比较；保留当前fileId不存在及项目计数。无公共schema清理/reset。
- DccPublicUploadPlacementService#create 将说明按实际trim，缺省记空串，仅限制500字，保留全部身份/归属/锁序/storage校验。
- bindDerived非空server context必需；derived可选说明使用独立upload-bind，普通bind仍audit.validateReason。所有derived tenant/actor/project/folder/storage/category检查移至重放分支之前，未改全局audit。
- G48仅自身正常上传policy fixture改新操作；缺policy失败按精确operation删除，不能任意删除第一行。未修改共享基类。
- A04前端三个文件不写；policy新条目已完成，可待Root收回供G01新增confirmation策略。
- M02 GREEN初次session77493实际exit1，13项4FAIL/0ERROR/PASS9；9项含全部精确DB快照回滚/重试已PASS。4空说明参数已实际归档并记录actor，但本测试预期基类username=b-combination-7，当前类BeforeEach正式账号fixture为storage-owner；只修自身预期为storage-owner，原失败XML保留m02-green-attempt1-junit.xml，不改生产账号来源/审计身份。
- 最终GREEN session56689实际exit0/BUILD SUCCESS，13/13，fresh XML m02-green-r2-junit.xml；15份关键来源起止全Same=true，起止及完整命令见process-receipt。
- 专属5方法回归session65204实际exit0/BUILD SUCCESS，fresh m02-regression-junit.xml=5/0/0/0；当前派生storage方法全PASS，无继承基类全量混入；15份来源全稳定。
- 最终真实验证包含空说明的真实ledger事实/actor/time、人工strict原因、权限/项目/tenant、重放和独立policy缺失/late caller的全DB快照回滚与重试。无mock替代ledger/真实mapper。
- Maven已退出，结束检查活跃Maven0。后端/policy/G48交还hash已封m02-release-hashes.json；Root可收M02票并将policy转G01。
- 已再读task-closeout-rules，记录实现验证ready_for_closeout；当轮Git收尾禁止，故最终Current Status=blocked（仅工程收尾/Rootreview，非代码失败），不completed。未执行cleanup/apply，所有首轮失败及最终PASS证据均保留，Root接管审查。

## 2026-10-15 Root最终接收与正式归还
- Root按A07已独立解析M02两份freshXML（13/13、5/5、0F/E/skip）、BUILD SUCCESS、来源稳定和最小服务diff，正式接收软件G12；本轮再次明确M02票已收回且无需重复测试。
- 两服务/G48所属测试正式归还Root并保持冻结；config/gxp-audit-policy.yaml正式归还后给G01仅新增confirmation条目，旧strict/upload-bind必须保留。A04前端及旧原因断言已归还，G12无共享生产写入权/验证票。
- 本轮仅补自有终态记录和m02-final-receipt.json证据索引，原source-stability/receipt/XML不覆盖；没有重复软件测试、Maven、Git写入、服务或实库操作。
- 分支仍int_qms。Current Status保持blocked，仅因工程Git/cleanup及运行验收未授权；Root软件review已完成，无待review/待实施的软件事项，不标completed。
- 最终文档结构/证据索引校验PASS：从既有XML/进程receipt核对13/13与5/5、exit0、两组各15份来源Same=true，生成m02-final-receipt.json及证据SHA256；正式归还清单、策略保留项、M02收回、非运行验收/非completed边界及Cleanup Keep齐全。仅记录验证，无软件重跑。

## 2026-10-15 A37 fixture-only 修正（M12结束后，等待M13）
- Root A37 指明 M12 共46类实际1008中963 PASS / 0 FAIL / 45 ERROR / 0 skip，45 ERROR仅G48继承测试的自有 `realActorDirectory()` fixture 未提供新必填 `approval_source_mode`，其它963类通过；本线程不得重跑M12或自行Maven。
- BDD fixture-only：Given `dcc_file_category.approval_source_mode` 为 NOT NULL，When G48 自有 category fixture 插入 `STORAGE-CAT`，Then 显式写入真实公共矩阵来源 `PUBLIC_MATRIX`，所有45测试继续使用原权限/继承断言；不修改生产/schema/default/负向缺值测试。
- 改前 SHA256：G48StorageProjectionTest=FFD67BF735452C5FC3AF04D590FFD2E0C500A26F55FCCD1CD4C37E7476216FAA。
- 唯一改动：`G48StorageProjectionTest#realActorDirectory` 的一条 INSERT 增加列和值；未运行Maven、服务、数据库、E2E或Git。
- Root M13将带 `-am` 复验整类；本线程冻结并归还，不把fixture-only修正称产品RED/GREEN。
- A37改后 SHA256：G48StorageProjectionTest=97A99DC4E34F987EBA42B3B2E8F7D06F76158F248DF5431D7EE234CD9AF02354。A37 handoff JSON已生成；只做文本/JSON/diff检查，M13未执行。
- Root已核对G48相对M12仅此一条INSERT hunk变化，生产代码/配置/其它测试无差异；M12其余963 PASS已归档。M13由Root唯一 `-am` 整类复验，句柄41579已启动；本线程不运行Maven/private大套，等待Root正式45项回执。
- M13正式回执已由Root提供：45项中39 PASS、2 FAIL、4 ERROR、0 skip；2 FAIL/4 ERROR均为继承的 `DccProjectFormalCombinationTest` 父方法在G48子类 fixture下运行，原因是父方法重复插入目录101/1001及父方法预期空属性表被G48额外事实影响。该结果是验证范围/fixture设计问题，不是A37生产或INSERT缺陷。
- M14范围由Root统一：G48全部声明 `@Test`/`@ParameterizedTest`（含18参数case）与原 `DccProjectFormalCombinationTest` 整类27项在各自正确fixture中分别执行，保留原45项断言覆盖。本线程不改父fixture/业务断言、不运行新测试，等待Root M14正式回执。

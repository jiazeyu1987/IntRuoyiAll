# E2E-10 矩阵变化与重提规则

## Goal

独立验证E2E-10，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — R3第二轮完整原合同A–E本轮独立5 PASS / 0 FAIL / 0 BLOCKED；自有矩阵/人员状态UI恢复回读，6context/browser关闭，runner32492实际exit0且消失，9trace CRC/1544动作核验PASS。业务已HOLD；无Git授权，工程blocked，cleanup preview实际exit3，apply未执行。正式交接rerun-20261020-r3-round2/handoff.json；旧结果与未知事故不覆盖。

## BDD

- R6历史返工 Given 原NEW工作稿已正式确认T1→T2并提交正式INITIAL后被原主管驳回，新正文工作稿的来源是该已提交驳回版；When 校验旧T1历史并再次分配INITIAL候选；Then 用真实绑定轮次/BPM验证已提交来源，保留原类型变更证明和所有历史，新增候选指向当前正文及精确失败前驱，以独立attempt分配；伪造BPM、重复/无关正式尝试、循环或迟到失败必须拒绝或整体回滚。随后在新包真实页面验证停用原批准人明确拦截、恢复后沿冻结矩阵提交并到原主管待办。

- R6原D续链 Given 原4206已通过正式恢复8714并提交4264，T2冻结批准人910442与原主管待办真实存在；When 原主管对4264签名驳回，原申请人从该驳回版正式检出/上传同名不同内容/检入，临时仅改自有CAT2公共批准候选为910443并停用原冻结批准人910442；Then 同类型重提仍保留4264执行T2原批准人及两份名单，不接受新公共矩阵；停用时最终确认后的实际提交明确拒绝且零正式申请创建/流程启动；UI恢复人员启用后保存/重开/提交仍沿旧矩阵并进入原主管本人新待办，最后UI恢复公共矩阵原候选与备注。原8712/8714及4206历史不重写；无法安全准备则首个具体阻塞停止。

- R4浏览恢复入口 Given 当前申请人自己的未送审/未检出NEW工作稿，已保存类型与存储冲突致详情无法打开；When 在真实受控浏览行操作区查看；Then 沿用既有类型动作的submit权限，有独立“核对类型存储”入口，打开仅预览，不调用故障详情Query；他人/历史/升版/检出/待处理申请不开放该入口。入口只提供正式服务核验，不在前端猜测恢复资格，不要求文控角色代替原申请人。
- R4恢复弹窗 Given 正式预览响应包含原proof精确摘要与当前观察摘要；When 预览取消、迟到响应、切换文件、重复确认、服务错误或成功；Then 取消零确认写入，旧响应不污染新对象，重复确认只一请求，错误保留内联且同键重试，成功仅在对象/证明响应匹配时关闭并刷新原行，不自动提交或重新选择矩阵。单元测试API端口仅隔离测试替身，不代表真实E2E。

- R3证据边界 Given 原4206的旧证明不可改写；When 文件/Master/placement均已移到正确目标目录但没有独立恢复证明，或恢复证明的操作者/原证明引用/观察hash/来源folder不符、存在重复恢复；Then 预览和普通保存继续明确拒绝；合法独立恢复证明必须保留原proof精确内容。仅H2声明证据读取不是恢复命令原子性或真实UI完成证据。
- R3路径可用性 Given 已保存目标或驳回执行矩阵，实际五级类型路径初始启用且绑定可解析；When 隔离H2中停用叶子或上级；Then 预览/确认/目标保存读取报告FILE_TYPE_TAXONOMY_INACTIVE、零业务写，不用当前公共新矩阵覆盖原canonical。Root实际RED前不修改生产。

- R2恢复 Given 原NEW工作稿已发生类型转换、原TYPE_CHANGE proof无folder且当前目录仍为旧类型，但当前/来源placement均明确同一项目folder，源hash/round/原驳回BPM可独立验证；When 原申请人在受控浏览预览并明确确认恢复；Then 原8712/原驳回版本/旧mapping零改写，仅当前File/Master/placement原子迁移，新增独立恢复审计引用原proof精确摘要，保存/提交须验证原proof与恢复事实，进入原主管新待办。预览取消零写，重放零新增，载荷变化/身份不符/正式受控/检出/晚失败明确拒绝或回滚。
- R2类型可用性 Given 已保存目标T2或已执行驳回T1矩阵不能被公共新版本覆盖；When 当前类别停用、类型路径停用或类别与叶子改绑；Then 预览、保存读取和最终确认均拒绝，零写入；配置仍可用但公共矩阵更新时继续沿用保存/执行矩阵，不重新取最新矩阵。

- 原D恢复核对 Given 最新Root复核保留4206/proof8712失败而D02已通过；When 只读核对旧proof、当前确认幂等合同、placement校验及正式UI调用链；Then 明确区分可重放已成功动作与缺失存储迁移，只有既有正式入口能安全修复原对象时才在Root READY后验证；无入口则保留失败样本并报告证据，不SQL/API修业务、不扩通用恢复平台。

- D02补充回归 Given Root批准一份独立DCC241016C10D02、复用原项目/账号/T1/T2且原D4206保持失败；When 放行后通过真实上传、主管签名驳回、同名不同内容检出检入、目标类型预览取消/确认及最终重提；Then 仅该新工作稿沿正式mapping到目标类别目录并进入原主管本人待办，单独记录source-fix regression；原D仍FAIL且恢复未完成。放行前仅本地准备，实际名称冲突在放行后由全部版本真实UI检查。

- R0610-13-storage Given 原NEW驳回工作稿已有正式项目/逻辑folder、旧类别derived mapping和本版本placement；When 明确确认目标公共类型；Then 使用既有Storage.resolve派生目标目录，原WORKING/current placement指向该目录且项目/folder不变，旧文件/旧mapping/旧placement不改；proof明确保留原目录和合法新目录，不放松其他源身份。
- R0610-13-storage Given 同一事务已创建目标derived目录/mapping并重绑定当前placement及类型；When 下游迟到失败；Then file/master/claim/proof/current placement/新mapping目录及审计整体回滚，旧来源原值不变。先真实H2行为RED，Root独占Maven；有效RED前不改生产。

- R0610-13 Given 原NEW已真实驳回、当前WORKING源件/属性轮次可证明且Master无正式受控版；When 原申请人预览T2并取消；Then 身份/矩阵/名单/历史/预留及审计零写入，其他输入保留。
- R0610-13 Given 相同源稿与源/目标UPLOAD、项目EDIT权限及目标启用叶子绑定均有效；When 明确核对原类型/hash/round与目标route/version/digest后确认；Then 同一WORKING和Master/当前claim原子重绑定，精确TYPE_CHANGE证明持久化，原驳回历史/来源ID与轮次不修改，后续预检/保存/连续检入/提交使用目标基线。
- R0610-13 Given 无证明或proof的actor/来源round/hash/旧类型/目标矩阵不符；When 尝试跨类型或重放；Then 明确拒绝且零部分写入；普通同类型C/E继续冻结原执行矩阵及检查当前人员资格。
- R0610-13 Given 非NEW、已有正式受控版、源稿已送审/检出、目标身份冲突或迟到失败；When 请求主动换类型；Then 拒绝或整体回滚，不创建替代申请/Master，不扩ACTIVE/升版重分类。

- R3 Given 原目录913887已存且admin受项目指派范围限制；When 为本例配置账号补目录维护权限并独立登录，读取同一目录及尝试类别绑定；Then 通过真实DOM核对原编码/名称并继续配置，目录权限问题与类别schema/公共待办错误分别归因，不重复新建目录。

- Given 分别准备已维护类型新草稿、“其他”草稿及已驳回申请
- When 通过配置页调整公共矩阵后提交各申请；驳回申请另测主动换类型与人员不可用
- Then 新申请展示公共矩阵变化并重新确认；“其他”沿用本次私有矩阵；驳回重提沿用原保存矩阵且核对人员可用性；仅主动变更类型时重新匹配，不能自动用新矩阵覆盖原申请。
- 修复 Given 原草稿v1、实际已执行驳回快照v2及当前公共v3；When 同类型真实provenance连续两次检入后预检与最终confirm；Then 均保持v2、无v3确认，原文件/快照不写回。首发WORKING复制及受控后的新REVISION继续比较最新；来源轮次、身份、循环、缺快照或原人员不可用须明确拒绝。Root唯一分配BaselineService及必要ExecutionContract小方法/定向测试，Maven由Root统一执行。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 六独立账号、项目、目录、类别、公共/私有矩阵及五分支样本真实页面准备完成。
4. [ ] Root review脚本/trace/可见页面断言，逐分支给出实际结论。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- R0610-13最新唯一文件授权：Controller；新增TypeChange请求/必要只读VO与具体动作Service；ExecutionContractService；NameClaimService/Mapper；前端workflow.ts及DetailApplicationPanel.vue。MasterMapper/MetadataChangeMapper只复用，不改DDL/真实库/旧历史；若另需文件先给Root准确理由，不自行扩张。先有效RED再GREEN；Maven/服务/运行包仍Root统一，无子Agent/Git授权。

- 2026-10-18最新Root授权例外：仅DccApprovalMatrixBaselineService.java、必要时DccApplicationExecutionContractService.java的小方法及对应定向测试可修改；先BDD/Root实际RED，再生产修复，Maven/构建/服务仍由Root统一持有。下方“不改生产源码”指原E2E阶段默认边界，此唯一例外优先，未分配源码仍不得改动。

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C10；最新 execution-authorization.md 已授权本例自行通过页面创建角色/菜单授权/账号/业务数据及本例专属矩阵，不改共享角色或业务矩阵，不等待额外 Root 放行。
- 本目录由本例线程维护，Root独占协调目录。当前发现真实页面前置故障并保留证据及夹具，不循环等待、不扩大生产修复。

## 子用例与页面合同

- C10-A `public-new-matrix`: 本例T1批准阶段候选由自有批准人改为自有文控审核人，保留完整前后版本/人员差异；A02已明确确认当前配置、实际提交并进入原主管待办，PASS。仅调整本例矩阵，固定岗位900332及既有岗位账号只读。
- C10-B `other-custom-matrix`: Root最新范围明确另一OTHER新申请不继承归E2E05，本例只验原私有配置保存/重开/实际提交。B01在公共矩阵变化后仍保留原自有批准人与私有文控人，实际提交并进入原主管待办，PASS；未把另一新申请扩成必需门禁。
- C10-C `rejected-resubmit-preserve`: 正式流程应从历史驳回记录检出、通过页面上传新正文、检入新工作稿，再完成重提并验证原矩阵。当前缺该完整任务自有夹具，脚本不执行猜测链路、独立 BLOCKED；查看原矩阵绝不算实际重提完成。
- C10-D `rejected-active-type-switch`: 在驳回申请正式返工入口主动切换到另一已维护类型，页面必须要求重新读取/确认矩阵；新类型矩阵仅在明确切换并确认后出现，不能因为公共矩阵变化自动覆盖原申请。切换前后的类型和矩阵摘要均来自可见页面。
- C10-E `rejected-unavailable-person`: 在驳回重提页保留原类型和原矩阵，选定已在正式账号目录中标记不可用的参与人，提交应由真实页面显示人员不可用/路线不就绪并阻止写入；禁止用管理员替代指定申请人或审批人。

## 精确入口与角色/数据要求

- 公共矩阵维护入口：`/dcc/controlled-file/categories` -> “审阅矩阵”页签 -> 任务自有类别行“编辑”；需要真实 `dcc:controlled-file:category:manage` 权限的文控账号。
- 新建/草稿入口：`/dcc/controlled-file/upload` 或 `/dcc/controlled-file/browser` -> 任务自有工作版本/草稿 -> 详情管理态“准备本次申请”；申请人必须是原草稿创建人。
- 驳回入口：同一浏览器的“历史申请 · 已驳回”版本视图 -> 任务自有行 -> “操作面板” -> 详情管理态；需页面实际出现申请人返工/重提控件。不存在该控件时记录 BLOCKED。
- 申请人与矩阵维护人用显式 actor 描述对象读取凭据；已各自首次改密并通过独立 context 登录。其他业务角色仅角色对象已创建，尚未创建/核验账号，不宣称全角色就绪。
- execute 检查 runtimeVerified=true、fixtureVerificationMode=PER_CASE_REAL_UI_MANIFEST、accountSetupGate=authorized、businessGate=execute_after_own_verified_fixture；不要求共享 roleFixtureVerified=true。本例 manifest 必须同时证明角色登录/菜单及业务数据，当前 PARTIAL_REAL_UI，准确反映页面前置故障。
- 每次运行在本目录 `runs/<UTC时间>/results.json` 写独立 JSON，并更新 `e2e-results.json`；逐例 PASS/FAIL/BLOCKED、原始错误及 cause/stack（脱敏）、矩阵恢复结果和残留分别保存，清理失败不覆盖原业务错误。
- 所有 context 登录完成并核对身份后才开启 trace，本脚本不输入签名。归档前用 `sanitize-trace.py` 去掉 network、响应体及敏感字段，保留 DOM/操作/截图；原始 trace 不作为交付证据。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-10/rerun-r3-round2-session.cjs

- doc/tasks/20261016-dcc-e2e-10/rerun-r2-round2-plan.md
- doc/tasks/20261016-dcc-e2e-10/rerun-r2-round2-session.cjs
- doc/tasks/20261016-dcc-e2e-10/rerun-r2-round2-flows.cjs
- doc/tasks/20261016-dcc-e2e-10/rerun-r2-round2-flows-ready.cjs
- doc/tasks/20261016-dcc-e2e-10/rerun-r2-round2-flows-enabled.cjs
- doc/tasks/20261016-dcc-e2e-10/rerun-r2-round2-E-baseline.cjs
- doc/tasks/20261016-dcc-e2e-10/rerun-r2-round2-E-disabled-submit.cjs
- doc/tasks/20261016-dcc-e2e-10/finalize-rerun-r2-round2.py
- doc/tasks/20261016-dcc-e2e-10/rerun-r2-round2-handoff.json
- doc/tasks/20261016-dcc-e2e-10/r6-original-D-final-result.json
- doc/tasks/20261016-dcc-e2e-10/r6-history-final-green.log
- doc/tasks/20261016-dcc-e2e-10/r6-history-production-delta.patch
- doc/tasks/20261016-dcc-e2e-10/r6-original-D-history-fix-handoff.json
- doc/tasks/20261016-dcc-e2e-10/runtime-start-r6history.json
- doc/tasks/20261016-dcc-e2e-10/r6-history-runtime-proof.json
- doc/tasks/20261016-dcc-e2e-10/r6-history-runtime-stop.json
- doc/tasks/20261016-dcc-e2e-10/r6-history-runtime-copy.json
- doc/tasks/20261016-dcc-e2e-10/r6-history-package-process.json
- doc/tasks/20261016-dcc-e2e-10/r6-history-package.log
- doc/tasks/20261016-dcc-e2e-10/r6-history-green.log
- doc/tasks/20261016-dcc-e2e-10/r6-initial-retry-red.log
- doc/tasks/20261016-dcc-e2e-10/r6-history-red.log
- doc/tasks/20261016-dcc-e2e-10/original-D-history-acceptance-session.cjs
- doc/tasks/20261016-dcc-e2e-10/start-history-runtime.py
- doc/tasks/20261016-dcc-e2e-10/prepare-history-runtime.py
- doc/tasks/20261016-dcc-e2e-10/r6-save-runtime-proof.json
- doc/tasks/20261016-dcc-e2e-10/r6-save-runtime-stop.json
- doc/tasks/20261016-dcc-e2e-10/runtime-start-r6save.json
- doc/tasks/20261016-dcc-e2e-10/r6-save-package.log
- doc/tasks/20261016-dcc-e2e-10/r6-save-package-process.json
- doc/tasks/20261016-dcc-e2e-10/r6-save-runtime-copy.json
- doc/tasks/20261016-dcc-e2e-10/original-D-save-acceptance-session.cjs
- doc/tasks/20261016-dcc-e2e-10/start-save-runtime.py
- doc/tasks/20261016-dcc-e2e-10/prepare-save-runtime.py
- doc/tasks/20261016-dcc-e2e-10/r6-original-D-continuation-result.json
- doc/tasks/20261016-dcc-e2e-10/r6-original-D-save-fix-handoff.json
- doc/tasks/20261016-dcc-e2e-10/r6-save-red.log
- doc/tasks/20261016-dcc-e2e-10/r6-save-green.log
- doc/tasks/20261016-dcc-e2e-10/r6-save-regression.log
- doc/tasks/20261016-dcc-e2e-10/r6-closeout-preview.txt


- doc/tasks/20261016-dcc-e2e-10/original-D-continuation-session.cjs
- doc/tasks/20261016-dcc-e2e-10/original-D-matrix-helper.cjs
- doc/tasks/20261016-dcc-e2e-10/original-D-account-helper.cjs

- doc/tasks/20261016-dcc-e2e-10/recovery-acceptance-session.cjs
- doc/tasks/20261016-dcc-e2e-10/r5-original-D-recovery-result.json

- doc/tasks/20261016-dcc-e2e-10/repair-20261019-r4-design.md
- doc/tasks/20261016-dcc-e2e-10/repair-20261019-r5-handoff.json
- doc/tasks/20261016-dcc-e2e-10/repair-20261019-r4-handoff.json
- doc/tasks/20261016-dcc-e2e-10/r4-front-entry-red.json
- doc/tasks/20261016-dcc-e2e-10/r4-front-recovery-green.json
- doc/tasks/20261016-dcc-e2e-10/r4-front-unmount-check.json
- doc/tasks/20261016-dcc-e2e-10/r4-front-regression.json
- doc/tasks/20261016-dcc-e2e-10/repair-20261019-r3-design.md
- doc/tasks/20261016-dcc-e2e-10/repair-20261019-r3-handoff.json
- doc/tasks/20261016-dcc-e2e-10/task.md
- doc/tasks/20261016-dcc-e2e-10/execution-log.md
- doc/tasks/20261016-dcc-e2e-10/verification-report.md
- doc/tasks/20261016-dcc-e2e-10/prepared.json
- doc/tasks/20261016-dcc-e2e-10/matrix-change-real.e2e.cjs
- doc/tasks/20261016-dcc-e2e-10/check-preparation.cjs
- doc/tasks/20261016-dcc-e2e-10/sanitize-trace.py
- doc/tasks/20261016-dcc-e2e-10/check-trace-sanitizer.py
- doc/tasks/20261016-dcc-e2e-10/e2e-results.json
- doc/tasks/20261016-dcc-e2e-10/runs
- doc/tasks/20261016-dcc-e2e-10/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-10/real-ui-results.json
- doc/tasks/20261016-dcc-e2e-10/live-ui-evidence.json
- doc/tasks/20261016-dcc-e2e-10/live-ui-session.cjs
- doc/tasks/20261016-dcc-e2e-10/finalize-real-ui-evidence.py
- doc/tasks/20261016-dcc-e2e-10/reproduce-prerequisite-real.cjs
- doc/tasks/20261016-dcc-e2e-10/product-blocker-bdd.md
- doc/tasks/20261016-dcc-e2e-10/blocked-directory-real-ui.png
- doc/tasks/20261016-dcc-e2e-10/blocked-public-matrix-real-ui.png
- doc/tasks/20261016-dcc-e2e-10/blocked-category-binding-real-ui.png
- doc/tasks/20261016-dcc-e2e-10/fixture-requester-independent.png
- doc/tasks/20261016-dcc-e2e-10/fixture-matrix-independent.png
- doc/tasks/20261016-dcc-e2e-10/fixture-types.trace.zip
- doc/tasks/20261016-dcc-e2e-10/fixture-category-binding.trace.zip
- doc/tasks/20261016-dcc-e2e-10/fixture-requester.trace.zip
- doc/tasks/20261016-dcc-e2e-10/blocked-prerequisites.trace.zip
- doc/tasks/20261016-dcc-e2e-10/directory-scope-recovery-real.cjs
- doc/tasks/20261016-dcc-e2e-10/directory-recovery-results.json
- doc/tasks/20261016-dcc-e2e-10/runtime-pause-checkpoint.json
- doc/tasks/20261016-dcc-e2e-10/directory-recovery-update.py
- doc/tasks/20261016-dcc-e2e-10/runtime-pause-guard.cjs
- doc/tasks/20261016-dcc-e2e-10/check-runtime-pause.cjs
- doc/tasks/20261016-dcc-e2e-10/runtime-pause-20261017.json
- doc/tasks/20261016-dcc-e2e-10/runtime-interruption-20261017.json
- doc/tasks/20261016-dcc-e2e-10/acceptance-session.cjs
- doc/tasks/20261016-dcc-e2e-10/acceptance-fixture-helpers.cjs
- doc/tasks/20261016-dcc-e2e-10/acceptance-project.cjs
- doc/tasks/20261016-dcc-e2e-10/acceptance-public-matrix.cjs
- doc/tasks/20261016-dcc-e2e-10/acceptance-permissions.cjs
- doc/tasks/20261016-dcc-e2e-10/acceptance-flows.cjs
- doc/tasks/20261016-dcc-e2e-10/acceptance-matrix-change.cjs
- doc/tasks/20261016-dcc-e2e-10/acceptance-supervisor.cjs
- doc/tasks/20261016-dcc-e2e-10/acceptance-rework.cjs
- doc/tasks/20261016-dcc-e2e-10/acceptance-account-status.cjs
- doc/tasks/20261016-dcc-e2e-10/detail-navigation-observation.md
- doc/tasks/20261016-dcc-e2e-10/rework-matrix-product-findings.md
- doc/tasks/20261016-dcc-e2e-10/fix-plan.json
- doc/tasks/20261016-dcc-e2e-10/update-current-evidence.cjs
- doc/tasks/20261016-dcc-e2e-10/acceptance-unavailable.cjs
- doc/tasks/20261016-dcc-e2e-10/fix-red-evidence
- doc/tasks/20261016-dcc-e2e-10/fix-green-evidence
- doc/tasks/20261016-dcc-e2e-10/fix-before-source-sha256.json
- doc/tasks/20261016-dcc-e2e-10/C-post-fix-result.json
- doc/tasks/20261016-dcc-e2e-10/D-type-switch-fix-plan.json
- doc/tasks/20261016-dcc-e2e-10/D02-preparation.json
- doc/tasks/20261016-dcc-e2e-10/acceptance-D02-regression.cjs
- doc/tasks/20261016-dcc-e2e-10/check-D02-preparation.cjs

## 2026-10-19 R5 原D恢复页面验收

- 当轮明确授权以协调目录 r5-runtime-proof.json 放行，取代旧 dispatch 的包身份；只运行原4206恢复闭环，A/B/C/E/D02及23/22项不重跑。
- Given 原4206/9175与proof8712保持失败样本；When 原申请人通过真实页面预览取消、重开确认、保存重读并提交；Then 新增独立恢复记录，当前存储归位且进入原直属主管本人待办，冻结矩阵与旧proof保持。
- 专用 recovery-acceptance-session.cjs 校验R5精确包摘要，不改共享dispatch；凭据由既有DPAPI加载，不输出明文。状态 in_progress；验收后仅更新本例资产，不Git/构建/服务/API/SQL。

## 2026-10-19 本轮范围与验证补充

- 用户本轮要求：继续原4206，按真实页面补齐公共矩阵变化、驳回重提及人员可用性，不重跑A/B/C/E/D02。前一轮恢复闭环PASS保持，新增断言独立记录，不把它们伪称上一轮已完成。
- 先只读核对新包PID/端口/hash、原4264页面和自有配置。原4264已送审，禁止重复恢复或直接重提4206；仅沿4264正式驳回后继链操作。
- 临时配置仅CAT2的批准规则和DCC241016C10Approver启停；每项先记录原页面值，再finally真实UI恢复并刷新核对。不改共享固定岗位900332或其他用户/矩阵。
- E2E操作与自然请求/DOM记录，API/DB仅可只读但本轮优先不用。保留脱敏trace与失败原因。新缺陷先根因定位、BDD/RED后最小修复，不扩架构。

## 2026-10-19 R6 原D重提保存缺陷

- BDD Given 原4206恢复后正式提交4264、主管真实签名驳回并检入4266，公共CAT2批准人已改变；When 页面沿冻结矩阵预览并保存本次申请；Then 保存接受该预览的真实主管来源凭证，保留冻结矩阵与历史，不读公共新矩阵替换来源；伪造凭证仍拒绝且零写入。
- 实际RED：R6真实页面4266预览正常，保存提示系统异常；日志NATIVE_SUPERVISOR_SOURCE_EXPECTATION_INVALID。saveWorkingApplication仅读显式类型变更proof，漏掉readRejectedDraftFacts正式provenance；preview/confirm已使用rejectedSource。先隔离H2定向RED，再只复用现有rejectedSource，不放宽token校验。
- 临时配置已真实UI恢复：CAT2批准人为原910442，账号910442启用。运行证据runs/acceptance-2026-10-19T17-27-20-768Z，浏览器已关闭、脱敏trace清理0错误。4266尚未提交。
- 当前有E2E-22在同一R6页面验收，禁止打断其运行；本轮先完成最小源码/隔离测试，换包前必须重新核对并行状态。

- R6最终定向验证：GREEN 3/3 + 新申请矩阵确认/回滚REGRESSION 1/1，总4/4，XML保存在fix-green-evidence/r6-save-*.xml；源码handoff见r6-original-D-save-fix-handoff.json。未换包，E2E-22状态仍active，故停止共享运行态操作。cleanup preview实际exit1因blocked状态前置拒绝，未执行apply/删除；Git未授权，任务不标completed。

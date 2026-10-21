# E2E-05 “其他”自定义矩阵

## Goal

独立验证E2E-05，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — 仅仓库closeout：R3当轮禁止Git提交推送，按规则不得completed。R3 round1业务已结束，C05-A/B/C/D全PASS，无失败/重试，新file2054545668044084389/A1单次提交待主管审核。21证据/4trace净化CRC/隐私/4context及browser/runner/wrapper关闭验证PASS。先ready_for_closeout后preview exit0，未apply/删除。唯一交接rerun-20261020-r3-round1/handoff.json；停止供Root review，不自动继续业务。

## BDD

- Given P1可选“其他”
- When 配置本次会签部门、批准人、文控职责，保存草稿重开并提交；另开新申请检查
- Then 本次自定义准确保存并用于本次流程；主管仍取组织；不新增或改写公共矩阵；另一申请不继承本次私有配置。

### 最新 fixture 自主授权（覆盖旧等待/禁止创建身份条款）

- 用户最新授权已在 execution-authorization.md 确认：本例可以通过真实页面创建本例账号、角色、菜单授权、组织及项目/类型等数据，准备且独立 context 核验后立即执行，无需等待 Root。全部新对象使用 DCC241016C05 前缀；不改共享角色、基线密码或公共矩阵。
- Given 只有管理员配置身份，When 从用户/角色/部门等真实页面创建任务自有对象并重新登录每个身份，Then manifest 记录真实角色/菜单/数据可见性与残留，凭据只存进程环境，不落 manifest、截图或 trace。创建部分对象不构成 UI_VERIFIED。
- Given dispatch.fixtureVerificationMode=PER_CASE_REAL_UI_MANIFEST，When 本例 manifest 完整通过，Then E2E 使用本例 manifest，不要求或伪改共享 false。

### Root review 补充 Given/When/Then

- Given 本次私有配置已经通过真实上传页保存为工作版本；When 离开详情，从项目浏览目标文件行“操作面板”重开并点击“准备本次申请”；Then 三个自定义控件逐项回显实际保存值，同一文件身份、工作版本、BPM 未绑定均须核对。
- Given 重开草稿已核对；When 点击“核对并提交初始申请”，在标题精确为“确认提交申请”的弹框点击“确定”；Then 同一业务编号必须呈现“待直属主管审核”、已绑定 BPM，且独立主管账号出现此前没有的目标待办。任一点击或断言失败必须中止，禁止 catch 后继续写 PASS。
- Given 本次类型已有真实类别编码；When 在同一配置账号、同一类型配置页分别于保存前和提交后读取；Then 类型到类别映射、公共审阅矩阵规则及配置存在性保持一致。只读空态仅证明无公共配置，不能证明其他未读取内容。
- Given 私有配置包含会签部门、批准人、文控审核人；When 同一账号新开 P1/其他申请；Then 三个控件全部为空，与配置前新表单一致，不能仅检查会签。
- Given 登录或电子签名会输入凭据；When 留存 trace；Then 登录完成并离开登录页后才开始 trace，snapshots=false 禁止网络载荷记录，不执行签名动作；出现敏感输入时先停止 trace，不截图或记录输入值。
- Given 页面可能已产生上传、工作版本或正式申请；When 正常或失败结束；Then 持续写入真实编号、文件身份、BPM 和页面位置、cleanupAttempted/cleanupCompleted/residual。Root review 前保留现场，业务断言与清理/收尾状态分别记录。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 本例账号、角色、组织、私有类别/目录/P1实际准备，原U路线4节点就绪。
4. [x] 四业务分支真实PASS，原私有配置正式重开/提交、独立主管待办、新申请隔离、公共矩阵前后核验完成。
5. [ ] Root review及现场清理/最终收尾；本线程保留现场与全部历史RED，不自动撤回/删除或Git写入。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## Root 委派的共享 V6 流程 UI 导入与独立发布

- 范围：tenant1 精确模型 dcc-controlled-file-upload-model-v4-tenant-1、dcc-controlled-file-revision-model-v4-tenant-1；正式文件位于 IntRuoyiBackend/config/dcc-workflow-v6/。Root 独占 Maven、运行配置和批准 SHA/测试结果。
- Given 两个精确模型及正式 V6 XML 已存在；When 仅通过真实页面查询、查看和打开导入弹框，再取消；Then 保存现有可见配置、模型 ID 自然请求映射、入口状态、影响说明及 ready JSON，不点击“保存流程图”或“发布”。
- 后续执行脚本必须在 Root 批准文件/原始 XML SHA/测试证据齐备后才允许 --execute；导入只替换 XML，保持表单与管理人等配置；每模型另点独立发布并验收新部署，旧资源与历史实例不改。不能以同 key 的任意同名模型或旧 V4/V5 XML 代替目标。
- 本阶段交接状态与 C05 业务状态独立；准备 ready 不能写 E2E PASS。所有脚本/计划/只读证据仍仅在本目录。

### 最新执行放行（覆盖上方暂不写入限制）

- Root 最新消息与 workflow-v6-deployment-approval.json ready=true 已明确放行两精确模型页面导入/保存/独立发布。批准包含原始 XML SHA、canonical digest、10 后端测试/5 前端导入测试 PASS；workflow-deployment-backup.json 固化 97 历史实例绑定和 10 旧定义资源。此授权不扩展为 API/SQL、服务、Maven 或 Git 写入。
- Given 批准文件、运行 PID、两个原始 XML SHA 和精确模型 ID/原定义均匹配；When 实际页面导入正式 XML、保存流程图并分别单次发布；Then 新定义版本/ID 与自然请求、截图写入 deployment-result.json，原表单/管理人等 metadata 由导入合同整体保留，旧定义资源与实例绑定由 Root 只读复核。
- Given 任一次保存/发布已经发生或结果不确定；When 后续读取失败；Then 保留写前日志并停止写入，只读盘点，不重复发布、不自动回滚。两个模型成功后用本例 U 复验路线已就绪，再执行 C05。
- 验证脚本先 --prepare 只读核对 UI；--execute 必须二次校验批准及输入 SHA，执行日志使用独占创建防止重跑发布；--inventory 仅只读回查。没有生产源码变更，不重复 Root 已通过的 Maven 测试。

## Parameterized Subcases

- `C05-A`（主链）：P1 + 已启用“其他”文件类型；会签部门以 `DCC_E2E_CUSTOM_SIGNOFF_DEPARTMENT_LABELS` 的 JSON 字符串数组逐项选入，批准人 `DCC_E2E_CUSTOM_APPROVER_LABEL`，文控审核人 `DCC_E2E_CUSTOM_DOCUMENT_CONTROL_LABEL`；保存工作版本、从项目浏览目标行“操作面板”重开、核对三个保存值后精确二次确认提交。
- `C05-B`（隔离）：同一 P1/“其他”类型新开申请；确认会签部门、批准人、文控审核人不从 `C05-A` 私有草稿继承。
- `C05-C`（来源）：确认框组织来源文字 + 组织主管姓名对应的路线行 + 独立主管账号新增待办（同一文件和 BPM）；该主管映射必须由 Root 的角色/组织只读基线确认。工作台只显示最近 5 条；未命中属于 BLOCKED，须正式审批中心核对，不把空行写成通过。
- `C05-D`（公共矩阵）：同一观察账号在写入前、提交后从“文件类型”按精确分类编码核对完整路径与“检查对应类别”，再从“审阅矩阵”按同一类别编码核对。已配置时只打开“预览”，比对实际保存的输入值、选择项、开关及规则；无配置时两次都须有同一类别行、“新增”及禁用的预览，证明没有新增公共矩阵。

## Formal Page Entry And Data Preconditions

- 上传入口：`/dcc/controlled-file/workbench` 的“上传文件”，最终路由 `/dcc/controlled-file/upload`。
- 本次矩阵控件：`[data-testid="dcc-custom-signoff-departments"]`、`[data-testid="dcc-custom-approver"]`、`[data-testid="dcc-custom-document-control"]`；保存“保存申请草稿”后没有通用保存确认框。详情“准备本次申请” -> “核对并提交初始申请” -> 标题“确认提交申请”内精确“确定”。上传页直接提交的“确认提交”属于另一入口，本例不混用。
- 结果页面：同一编号“待直属主管审核”、`dcc-detail-formal-version-facts` 中实际 BPM、`dcc-detail-route-snapshot-section` 三个阶段及会签名单。路线快照并没有固定“本次自定义”来源文字，不用未实现的文案冒充来源断言。
- 公共矩阵只读入口：DCC 类别管理 -> 审阅矩阵；编辑/新增/删除属于公共配置写入，本例禁止操作。
- 必需真实身份：申请人 U、组织主管 S、本次批准人 A、文控审核人 D、配置观察账号，以及所选会签部门负责人/成员。当前 U/S/A/D/观察账号已创建并分别独立登录；会签前置尚未完整核验。按最新授权仅创建/分配本例账号与角色，不用 admin 冒充业务身份，不改共享角色定义或基线密码。
- 必需真实数据：同一已确认租户内 P1、启用“其他”类型、P1 独立目录、可上传源件、至少一个可用会签部门、可用批准人和文控审核人；任务自有编号前缀 `DCC241016C05`。
- 执行脚本输入只来自环境变量；密码、token、cookie、用户 ID 和文件内容不写入任务文档或证据。

## Execution Inputs And Residual Policy

- 验收账号输入组：`DCC_E2E_APPLICANT_{USERNAME,PASSWORD,DISPLAY_NAME}`、`DCC_E2E_SUPERVISOR_{USERNAME,PASSWORD,DISPLAY_NAME}`、`DCC_E2E_MATRIX_OBSERVER_{USERNAME,PASSWORD,DISPLAY_NAME}`；新建测试身份的授权凭据在本机 DPAPI 私有文件与进程内处理，不写 manifest/trace/截图。本例配置账号具备目录管理、查询和类别管理，业务身份保持独立；仅本例角色菜单发生授权写入，Root 后续 review。
- 数据组：`DCC_E2E_TENANT`、`DCC_E2E_RUN_ID`（4—24 位字母数字且每次唯一）、`DCC_E2E_P1_LABEL`、`DCC_E2E_P1_FOLDER_LABEL`、`DCC_E2E_OTHER_TYPE_LABEL`（下拉完整路径）、`DCC_E2E_OTHER_TYPE_CODE`、`DCC_E2E_OTHER_CATEGORY_CODE`、`DCC_E2E_OTHER_CATEGORY_NAME`、`DCC_E2E_SOURCE_FILE`、会签 JSON 数组、批准/文控的 LABEL 与 NAME（LABEL 为真实选项显示，NAME 为路线姓名）。
- 证据目录 `evidence/<runId>/`，包含三个身份分别在登录后开启的 trace、关键页面截图及持续更新的 result.json。snapshots=false/sources=false；本例不办理签名，不记录密码。遇敏感输入先停 trace 并 BLOCKED，不能临时补签。
- 上传/保存前即登记可能残留；页面取得原工作文件、新文件、版本和 BPM 后逐项更新。Root review 前 `cleanupAttempted=false`、`cleanupCompleted=false`，整体不标 completed。后续只经目标详情“风险操作 -> 撤回申请”验证已撤回；“删除流程”须确认真实权限和自有对象；原工作版本、上传资产及审计保留分别记录，缺页面入口不能推断已删除。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C05，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## 旧轮实际运行与阻塞（修复前历史）

- 授权来源：`doc/tasks/20261016-dcc-24-e2e-coordination/execution-authorization.md`；dispatch `execute / authorized / execute_after_own_verified_fixture`，`PER_CASE_REAL_UI_MANIFEST`。脚本已经取消共享 `environment.roleFixtureVerified=false` 门禁，保留共享值不改。
- 真实 UI：本例角色分类 1、权限角色 5、账号 5、部门 1、直属主管关系 1、分类节点 7、存储目录 2（多余项已通过页面改名保留）。5 个账号均完成首次改密后独立登录、昵称/菜单/本例角色回显核验。没有上传 DCC 文件、没有工作版本/审批申请、没有实际文件版本号。
- 存储目录最初在管理员页面不可见；本例配置角色经授权补齐 `directory:manage` 与 `query` 对应菜单后，通过真实页面核验两个本例目录，并把多余目录更名为 `DCC241016C05存储目录重复1`。未调整管理员的既有分配关系。
- 自定义生命周期阶段被正确拒绝的旧 fixture 保留为未使用残留；后续改用已有正式“设计和开发策划阶段”，只新增本例私有 3—5 级节点。合法路径下类别保存仍“系统异常”，属于独立的运行库缺列阻塞，证据见 backend-schema-blocker.md。
- 项目入口可达且显示 4 名审核候选；未在角色管理正式页核对到精确既有 `dcc_project_reviewer` 行，未改共享角色/成员，项目申请未提交。P1、正式会签部门资格及源文件输入仍未达到全套验收前置；禁止把部分 fixture 标 UI_VERIFIED。
- 当前 `fixture-manifest.json.status=BLOCKED`，execution-result.json 四个子例均 BLOCKED。无须再次授权账号准备；待 Root 处理运行库与已验证 Jar 匹配后，按现有授权复验。

## PID43284 恢复轮的实际执行

- 新身份由最新 dispatch 与 unblock-final-runtime-health.json 相互核对；本例没有服务操作或数据库操作。共享 roleFixtureVerified=false 没有参与停止条件。
- 已有本例独立配置账号重新打开/创建 DCC241016C05CUSTOM，来源 APPLICATION_CUSTOM、合法五级分类与原私有存储目录实际回显正确。没有重复创建目录、修改管理员原项目指派或公共矩阵。
- 正式项目页面明确“项目审核员”职责；仅给本例批准账号增加该既有普通角色绑定，不改变共享角色定义。P1/产品/私有目录模板通过真实页面创建，批准账号独立登录审核，申请记录终态 COMPLETED。
- 本例审批岗位、S/A/D 岗位绑定、部门负责人、S/A/D 电子签名授权均由真实页面保存并回读；各账号独立登录上传自己的带任务标识的测试签名图片，页面显示“已启用”。密码段全程不在 trace 中。
- 本例申请人实际进入上传页，选择 P1、私有项目文件夹、其他类型、会签部门、本次批准人、本次文控人；这些选择均可见，路线预检却显示“系统异常”。自然 POST /admin-api/dcc/controlled-files/route-preview 为 HTTP200，不被当作成功。
- 新运行日志在同一 20:40:31—20:40:32 时段记录 EXECUTION_CONTRACT_UNREGISTERED_DEFINITION，栈经过 DccWorkflowDefinitionCatalog.require -> verify -> preparePublicPreview -> previewCustomRoute。源码要求 PUBLIC_MATRIX_NATIVE_V2 的精确注册图；只读模型/历史页显示该 key 版本至 v5。没有读取响应 JSON 代替页面结论，也没有发布/恢复共享流程。
- 分类恢复、项目终态和人员配置是前置结果；本例 fixture 没有标 UI_VERIFIED，未上传正文、未保存 DCC 工作稿、未提交正式 DCC 申请。C05-A/B/C/D 完整链路均 BLOCKED，页面配置预检为真实 RED。
- rounds/before-schema-unblock 保存旧 manifest/报告/结果；rounds/unblock-pid43284 保存本轮身份、manifest、执行结果及脱敏日志摘录。当前残留/关联清理项 40 个，保留给 Root review。

## 证据和清理说明

- `fixture-evidence/` 保存独立登录、配置失败截图和脱敏 trace；登录、创建密码及首次改密都在 tracing 停止时完成，snapshots=false/sources=false。
- `evidence-privacy-check.json` 记录已知凭据字节扫描及 ZIP 条目检查；不包含凭据值。临时 `.fixture-credentials.dpapi` 只作本机当前用户可解密的运行配置，禁止提交 Git 或作为最终证据分发。
- 最终清理尚未执行，现场保留给 Root；当前 residual 为 40 项（含实体和本例账号关联清理项），类别已经真实保存/回显，P1 请求已经 COMPLETED。旧 23 项是修复前历史，不能覆盖新结果。没有服务/迁移/Git 写操作。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/DCC241020C05-R3C05mvgs8u7b-A1.docx
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/evidence-privacy-check.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/evidence-validation.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/execution-log.md
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/execution-release.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/execution-started.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/handoff.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/plan.md
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/runner-closure-R3C05mvgs8u7b.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/seal-evidence.py
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/static-validation.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/trace-seal.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/ui-only-static-check.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/applicant-draft-reopened-actual-values.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/applicant-exact-submission-confirmation.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/applicant-fresh-application-empty-private-fields.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/applicant-private-configured.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/applicant-same-working-detail-loaded.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/applicant-submitted-route.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/applicant-trace.zip
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/detail-readonly-green.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/observer-after-matrix-row.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/observer-after-type-mapping.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/observer-before-matrix-row.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/observer-before-type-mapping.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/observer-trace.zip
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/organization-Rmva4c8zyapplicant.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/organization-Rmva4c8zyapprover.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/organization-Rmva4c8zydoccontrol.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/organization-Rmva4c8zyobserver.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/organization-Rmva4c8zysupervisor.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/organization-supervisor-relation.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/organization-trace.zip
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/result.json
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/supervisor-new-supervisor-todo.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/supervisor-todo-before.png
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/R3C05mvgs8u7b/supervisor-trace.zip
- doc/tasks/20261016-dcc-e2e-05/verify-rerun-20261020-r3-round1.mjs
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/verification-report.md
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/handoff.md
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/cleanup-preview.txt
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1/cleanup-status.json


- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1.real.e2e.mjs
- doc/tasks/20261016-dcc-e2e-05/run-rerun-20261020-r3-round1.mjs
- doc/tasks/20261016-dcc-e2e-05/sanitize-trace.py
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r3-round1


- doc/tasks/20261016-dcc-e2e-05/verify-rerun-20261020-r2-round1.mjs
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/DCC241020C05-R2C05mvg833oh-A1.docx
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/evidence-privacy-check.json
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/evidence-validation.json
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/execution-started.json
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/handoff.json
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/applicant-draft-reopened-actual-values.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/applicant-exact-submission-confirmation.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/applicant-fresh-application-empty-private-fields.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/applicant-private-configured.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/applicant-same-working-detail-loaded.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/applicant-submitted-route.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/applicant-trace.zip
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/detail-readonly-green.json
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/observer-after-matrix-row.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/observer-after-type-mapping.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/observer-before-matrix-row.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/observer-before-type-mapping.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/observer-trace.zip
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/organization-Rmva4c8zyapplicant.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/organization-Rmva4c8zyapprover.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/organization-Rmva4c8zydoccontrol.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/organization-Rmva4c8zyobserver.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/organization-Rmva4c8zysupervisor.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/organization-supervisor-relation.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/organization-trace.zip
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/result.json
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/supervisor-new-supervisor-todo.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/supervisor-todo-before.png
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/R2C05mvg833oh/supervisor-trace.zip
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/runner-closure-R2C05mvg833oh.json
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/verification-report.md
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/cleanup-preview.txt
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/cleanup-status.json

- doc/tasks/20261016-dcc-e2e-05/run-rerun-20261020-r2-round1.mjs
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-r2-round1.real.e2e.mjs
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/prepared.json
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/plan.md
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/prepare-validation.json
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/preparation-handoff.json
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020-r2-round1/execution-release.json


- doc/tasks/20261016-dcc-e2e-05/verify-rerun-20261020.mjs
- doc/tasks/20261016-dcc-e2e-05/rerun-20261020-result.json

- doc/tasks/20261016-dcc-e2e-05/rerun-20261020.real.e2e.mjs
- doc/tasks/20261016-dcc-e2e-05/run-rerun-20261020.mjs
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020/

- doc/tasks/20261016-dcc-e2e-05/evidence
- doc/tasks/20261016-dcc-e2e-05/final-result.json
- doc/tasks/20261016-dcc-e2e-05/final-evidence-structure.json
- doc/tasks/20261016-dcc-e2e-05/residual-register.json
- doc/tasks/20261016-dcc-e2e-05/detail-readonly-transaction-blocker.json
- doc/tasks/20261016-dcc-e2e-05/detail-readonly-transaction-blocker.md
- doc/tasks/20261016-dcc-e2e-05/detail-initialization-blocker.json
- doc/tasks/20261016-dcc-e2e-05/detail-initialization-blocker.md
- doc/tasks/20261016-dcc-e2e-05/extract-working-save-evidence.py
- doc/tasks/20261016-dcc-e2e-05/four-branch-script-review.md
- doc/tasks/20261016-dcc-e2e-05/working-save-blocker.json
- doc/tasks/20261016-dcc-e2e-05/working-save-blocker.md
- doc/tasks/20261016-dcc-e2e-05/diagnose-preview-transport.real.e2e.mjs
- doc/tasks/20261016-dcc-e2e-05/preview-transport-diagnostic.json
- doc/tasks/20261016-dcc-e2e-05/root-workflow-resume-result.json
- doc/tasks/20261016-dcc-e2e-05/matrix-identity-blocker.md
- doc/tasks/20261016-dcc-e2e-05/isolation-runtime-gate.mjs
- doc/tasks/20261016-dcc-e2e-05/check-isolation-runtime-gate.mjs
- doc/tasks/20261016-dcc-e2e-05/resave-v6-prepared.json
- doc/tasks/20261016-dcc-e2e-05/resave-v6-result.json
- doc/tasks/20261016-dcc-e2e-05/resave-v6-journal.json
- doc/tasks/20261016-dcc-e2e-05/verify-native-accounts.real.e2e.mjs
- doc/tasks/20261016-dcc-e2e-05/native-account-ui-result.json
- doc/tasks/20261016-dcc-e2e-05/native-account-ui-readback.json
- doc/tasks/20261016-dcc-e2e-05/restore-result.json
- doc/tasks/20261016-dcc-e2e-05/workflow-v6-restore-journal.json
- doc/tasks/20261016-dcc-e2e-05/post-deployment-route-readiness.json
- doc/tasks/20261016-dcc-e2e-05/workflow-v6-ui-import-publish.mjs
- doc/tasks/20261016-dcc-e2e-05/deployment-result.json
- doc/tasks/20261016-dcc-e2e-05/workflow-v6-ready.json
- doc/tasks/20261016-dcc-e2e-05/workflow-v6-execution-journal.json
- doc/tasks/20261016-dcc-e2e-05/deployment-evidence
- doc/tasks/20261016-dcc-e2e-05/task.md
- doc/tasks/20261016-dcc-e2e-05/execution-log.md
- doc/tasks/20261016-dcc-e2e-05/verification-report.md
- doc/tasks/20261016-dcc-e2e-05/prepared.json
- doc/tasks/20261016-dcc-e2e-05/e2e-05-custom-matrix.real.e2e.mjs
- doc/tasks/20261016-dcc-e2e-05/check-prepared.mjs
- doc/tasks/20261016-dcc-e2e-05/static-check-result.json
- doc/tasks/20261016-dcc-e2e-05/fixture-setup.real.e2e.mjs
- doc/tasks/20261016-dcc-e2e-05/fixture-explorer.mjs
- doc/tasks/20261016-dcc-e2e-05/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-05/fixture-explorer-result.json
- doc/tasks/20261016-dcc-e2e-05/execution-result.json
- doc/tasks/20261016-dcc-e2e-05/backend-schema-blocker.md
- doc/tasks/20261016-dcc-e2e-05/record-blocked.mjs
- doc/tasks/20261016-dcc-e2e-05/check-evidence-privacy.mjs
- doc/tasks/20261016-dcc-e2e-05/check-evidence-privacy.py
- doc/tasks/20261016-dcc-e2e-05/evidence-privacy-check.json
- doc/tasks/20261016-dcc-e2e-05/fixture-evidence
- doc/tasks/20261016-dcc-e2e-05/.fixture-credentials.dpapi
- doc/tasks/20261016-dcc-e2e-05/run-from-fixture.mjs
- doc/tasks/20261016-dcc-e2e-05/record-resumed-result.mjs
- doc/tasks/20261016-dcc-e2e-05/generate-fixtures.ps1
- doc/tasks/20261016-dcc-e2e-05/generate-source-fixture.py
- doc/tasks/20261016-dcc-e2e-05/rounds
- doc/tasks/20261016-dcc-e2e-05/fixtures

## 最新集成暂停边界
- Root 最新安排覆盖立即继续完整用例：两图部署后只回读并复验本例 U 原 route-readiness；暂缓正式文件上传保存、提交与审批。Root 独占新 V2 upload→revision 精确 ID 映射配置、48061 重启和健康放行，本例不操作服务。

## 紧急精确恢复授权

- Root 只读核对发现 seed model.editor_source_value_id 与旧部署资源别名，正式导入保存影响旧资源。立即停止预检/业务/发布；保留原部署日志。
- Given 两精确模型及 Root 备份旧 XML SHA 为 upload=19168160c63d827fa7a2adf613651724f434058e0e8a7511b468990fd271b07f、revision=832eeb34ac311d615115b4c49889b5910843b1f6a17b47d06f7b778370b8d759；When 真实导入页仅保存各自 workflow-definition-before/*-v5.bpmn20.xml；Then 单独 restore-result.json 记录两次保存且零发布，metadata 保留，现有新 v6 定义 ID/历史行不变，由 Root 只读核对旧资源字节恢复及新 v6 未变。
- 恢复后保持暂停，不重新导入 V6、不自行重启，不继续正式文件业务。Root 负责正式修复模型编辑资源隔离。

## 原生组织资格的 U/S 精确 UI 复核

- Root 已只读确认恢复 PASS：10 旧资源哈希、97 历史实例绑定及 metadata 不变，新两 v6 canonical 为 V2。22:46 原预检已经越过定义校验，真实新异常为 NATIVE_SUPERVISOR_ENABLED_ACCOUNT_REQUIRED；Root 指出 requireCustomOrganization 先检查申请人 U，再检查主管 S，同一错误不能直接判成 S 缺资格。
- Given 本例 U/S 精确用户名和已保存 U→S 关系；When 从真实用户页查看 ID、启用开关、修改弹框岗位，并重新打开 U 直属主管关系；Then 记录实际可见值，不反复修改正常配置，不解析 API JSON 替代页面证据。
- Given U 岗位为空；When 仅给本例 U 选择既有 DCC241016C05POST 并保存；Then 重新查询并打开 U 修改弹框核验岗位持久化和状态，其他账号不写入。若已有岗位则只读取消。主管数值 ID 若关系弹框不显示，明确标注仅由当前主管精确用户名与唯一 S 用户行 ID 对应，不猜测接口 DTO。
- 最新授权允许上述 U 岗位修正，仍不允许重跑预检/业务、导入/发布、服务操作；等待 Root 新包健康统一放行。结果单列 native-account-ui-result.json。

## 新隔离运行态下 V6 仅重保存的准备

- Root 已报告编辑资源隔离 3 项真实引擎和 25 项相关回归 GREEN，完整包正在构建。最新指令本轮仅准备脚本，不启动页面写入；proof 文件到位本身不替代 Root 维护结束/执行放行。
- Given 原批准两模型/XML SHA/两已发布 v6 定义 ID 保持不变，且 Root workflow-isolation-runtime-proof.json 的新 PID、新 Jar SHA、health=UP、testGreen=true 与最新 dispatch 精确一致；When 获 Root 放行后运行 --resave-v6-only；Then 通过正式导入页每模型保存一次、零发布，比较保存前后六条历史行与精确 v6 ID、可见配置不变，独立 resave-v6-result.json/journal 不覆盖旧批准/发布/恢复记录。
- Given proof 缺失、不健康、testGreen 不为 true、仍为旧 PID43284/旧 Jar 或与 dispatch 不一致；When 运行新模式；Then 浏览器和任何页面写入前明确失败。原模式仍保留旧批准 PID 精确门禁，禁止放宽成任意运行进程。
- 保存前后校验 proof/dispatch 与全部受保护证据指纹；任何已保存/结果不确定时禁止自动重保存。Root 负责只读核验编辑源已分离、canonical 与已发布 V2 一致，以及现有两 v6 部署资源/历史实例/metadata 不变。
- 本轮只运行语法/本地 gate 定向验证和 --check-resave-plan，不登录页面、不保存、不发布、不重启。未来重保存/Root 只读校验通过后再用原 U 预检；完整 C05 仍须 Root 正式放行。

## V6 重保存当轮执行放行
Root 明确放行 --resave-v6-only：PID34096、Jar e537370a8e6e4cf425ccd292d5856feb791e99f6124d9e19cad55697ac42f7ce、healthUP，3隔离+25相关回归PASS，新旧精确迁移映射共2行。已逐项核对 Root proof 与最新 dispatch，原 resave journal 不存在。仅执行两模型 UI 保存与回读，0发布；完成交 Root 只读验资源隔离/历史，不运行原 U 预检或完整05，等待后续明确放行。

## Root 只读核验 PASS 后恢复预检与完整05授权
Root 最新明确：两 editorSourceIsolationVerified=true 且 after digests V2 即为核验PASS，前置真实齐备后继续完整05无需再确认。已读取实际两核验文件，精确两个v6ID/原SHA/metadata/隔离/canonical全部匹配，97绑定/10资源保留。Given新proof+dispatch+核验文件匹配；When原U先重走路线预检；Then root-workflow-resume-result.json 如实记录定义错误是否解除和下一阻塞，路线就绪才能上传/保存/提交，随后按原四子例完成验证。最新准备/业务脚本改为绑定新Root proof及隔离核验，旧43284证据保留，不使用旧proof冒充新运行态。

## 自然响应技术诊断的最新授权

- Root 唯一负责生产 approval-matrix.ts/index.vue 修复及 RED/GREEN，本例不并行修改。保留原业务证据，仅为技术诊断读取真实页面自然预检响应，禁止主动接口请求。
- Given 原 U/本例配置与当前阻塞；When 原 U 从工作台进入上传页并选择既有 P1/类别/部门/A/D；Then 只采集自然响应的 approvalSourceMode、matrixId/routeId 是否存在及为空、演员 ID 类型、canonical ID 类型等最小字段，与同页 DOM 对照。业务 PASS/FAIL 仍只取 DOM，不以 JSON 作为验收 oracle；不记录 token/canonical 全文/cookie/完整响应，不上传或保存文件。
- 结果独立 preview-transport-diagnostic.json 与脱敏 trace/截图，原 manifest/resume result 不覆盖；本轮不声明完整05已通过。

## Root 自定义演员安全数字修复后继续
Root 唯一修改生产helper/index：安全正整数演员ID规范为string，不安全数字/错误身份仍拒绝，canonical/HMAC/公共矩阵严格校验不变，3组22testsPASS、Vite热更新、后端不重启。Given原U/P1/其他类型保持；When重新预检；Then只以DOM判就绪，更新root-workflow-resume-result；若具体阶段岗位/权限/签名缺口仅补本例最小资格，不冒充旧图错误；真实前置齐备后继续完整05，无需再次授权。

## 主管待办最小权限资格补齐
GivenS工作台缺我的审批待办且其自有角色已有bpm:task:query；源码明确面板同时要求bpm:process-instance:query，当前role.menuLabels缺流程实例的查询；When仅在本例S自有角色新增该既有权限菜单并新context登录；Then核验我的审批待办可见，保留原权限/其他账号/共享角色，不改生产代码。该操作属于最新授权的本例最小资格补齐，不是原图错误。

## 类别UPLOAD/VIEW最小资格
上传预览首先检查当前类别UPLOAD，缺权也抛项目负责人英文错误。Given本例自定义类别和U/S；When真实类别权限页只补U上传/查看、S查看并保存重读；Then现有规则逐项保留，REVIEW/APPROVE公共矩阵不动，其他类别/用户不动。这两查看权用于本用例草稿重开和主管待办详情核验，不办理额外审批。源件失败轮保留，后台未回原件身份时记录上传残留未知，不冒充成功。

## Root 接管审计阻塞后的暂停与补证

- Root 已接管 operation→正式迁移/策略核对，本例暂停任何保存重试/重新上传，不改生产/公共审计策略/SQL，不新增包装或无关防御。
- Given 原 U34096mvb5mr4i trace/截图/只读盘点仍在；When 仅离线检查保存动作后截图帧与已保存请求元数据；Then 补充真实页面瞬时错误、精确编号、可见资源事实和证据缺口，不为了获得业务码而重放业务。trace.network=0，若原业务响应未留存则明确不可从现有证据读取，不能把源码业务码标成自然响应实测。
- Given 四分支脚本及实际stopPosition；When 只读逐项审阅入口/参数/断言/残留；Then 区分已实际覆盖和仅脚本计划覆盖，记录具体缺口，等待Root修复后通知复现；原U查询0不证明DB无写。

## upload-bind正式测试策略修复后的恢复
Root明确恢复完整05；upload-placement-policy-repair.json核对operation精确、固定test-only迁移、两库首次/重复PASS、originalPolicyAndEventRowsUnchanged=true，Root确认旧失败精确fileNumber在DB无file且事务回滚。原runfinally已关闭全部browser/context，无原页面可恢复；原ticketID未落盘，状态不猜测，不主动API/SQL、不清理旧ticket、不重放旧save。按最新授权以原fixture新run重新UI上传并单次save，旧失败及临时资产另保留。Given修复证据/旧事务事实与原页面已关闭；When新run；Then保存落详情、正式重开三私有值、提交及主管待办/B隔离/D公共矩阵逐项验证，同类审计缺口交Root，不改其他策略。

## 同一已保存工作稿恢复验证
最新run U34096mvb8ugne已保存并真实跳详情fileId2054545668044084181，重开仅因编号规范大写与原run输入小写导致严格定位失败；失败DOM实际有大写编号和工作版本。Given已保存fileId/未绑定BPM/未提交/精确页面可见大写身份；When --resume-saved-draft仅从真实浏览目标行重开；Then不得重新上传/保存/换对象，独立resume子目录保留原result/trace，继续既有私有值回显及正式提交四分支，最终业务身份用真实大写编号。

## TDZ修复后的同草稿恢复放行
Root将currentUserId移至userStore后且首watch前，真实源顺序/实际Vuewatch RED已复现，Vite热更新，无后端变动。Given原fileId2054545668044084181/真实大写编号；When新context原U经浏览行重开；Then先只读验证详情/三私有配置/源件，正常后继续既有四分支。新pageerror精确记录并停止原草稿，禁止重传/重建/生产修改。仅在现有脚本补pageerror证据监听及提交前断言，不加通用包装。

## 延迟确认watch根因修复后的再次同稿复核
Root移watch注册至完整依赖链定义后，真实Vuewatch/源顺序受控ID RED到GREEN及相邻12testsPASS，Vite更新。Given原已保存fileId2054545668044084181；When新context原U经真实浏览行重开；Then在准备/提交前先保存detail-readonly-green.json，记录真实标题/附件区/未绑定BPM/无pageerror，随后三私有值及既有四分支。遇任何新pageerror停止原草稿，不能重传重建；生产Root独占。

## Root后端维护窗口安全暂停
Root即将更新48061，最新明确暂停新页面写入。当前恢复run已结束，finally已close全部context/browser，无在途保存/提交；同一fileId2054545668044084181、大写编号DCC241016C05-U34096MVB8UGNE-A、WORKING草稿保留，无BPM。最新可见事实/日志已保存detail-readonly-transaction-blocker.json，TDZ不再复现但详情数据未载入，私有值/附件未完成。维护中不打开页面、不触发业务、不将连接问题记业务FAIL；待Root新PID/SHA健康通知后从原fileId复验，不重建重传重复提交。

## PID16776 维护结束与同草稿续验
Root明确新PID16776/SHA494306625fb1d04261832e4145a8aa83c3734257c02069e9b1e7644749efcdec，healthUP+官方UI登录PASS；23缺负责人/14附件只读/12watch回归PASS，workflow proof已更新且旧版备份。已与dispatch精确核对，manifest仅记录新runtime并保留历史/原fixture，不因PID造数据。Given同草稿2054545668044084181；When原U新context经浏览行重开；Then先取实际header/附件/私有值GREEN，再继续原提交和4分支。旧未知ticket不清理不重传，不改生产/扩大权限。

## 同一文件已正式提交后的只读后置续验
原稿已单次最终确认提交，fileId2054545668044084181/BPM7c253fee-c992-11f1-8f08-b082e25ec548/大写编号A1待主管审核，C05-A真实PASS。S工作台已有精确编号新待办；脚本阶段旧文案不匹配实际直接主管独立审核与签名，保留失败原result后仅改本例断言。Given原已提交身份及A-PASS证据；When --resume-submitted；Then只读取新待办并打开核fileId/task/BPM，检查同U新表单三项为空、公共矩阵前后一致，不经过上传/保存/提交路径，不重复办理审批。独立post-submit证据，原A-PASS/提交记录保留。

## 2026-10-20 重跑保留资产

- doc/tasks/20261016-dcc-e2e-05/rerun-20261020.real.e2e.mjs
- doc/tasks/20261016-dcc-e2e-05/run-rerun-20261020.mjs
- doc/tasks/20261016-dcc-e2e-05/rounds/rerun-20261020/

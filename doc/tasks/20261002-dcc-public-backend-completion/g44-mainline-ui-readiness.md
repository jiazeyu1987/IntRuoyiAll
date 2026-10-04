# G44 项目→上传→审批最短正向页面清单

2026-10-04；状态：SOURCE_LOCATORS_REVIEWED_RUNTIME_DOM_CALIBRATION_REQUIRED。本 Agent仅读G27计划、G29/G30/G31现有runner/helper及当前正式SFC/API调用声明，没有实际浏览器、API、DB、服务、Maven、Git动作；只新增本文。不编新全面负向runner、不复查已通过的G43配置/维护门禁。

Root下一步应以8067真实登录后可见菜单为准，在正式页面执行以下主线。菜单标签由运行账号动态授权返回，源码不能证明必然可见，不能把组件路径拼成URL或注入route代替自然点击。

## 进入业务前最少要在页面核对的事实

| 事实 | 真实页面入口/源码 | 本次必要判断 |
|---|---|---|
| 项目/产品审核人 | DCC产品目录的 `ProjectReviewerConfiguration`；按钮“配置项目审核人”，弹框“项目及产品创建审核人”，选择启用账号/配置原因/保存配置及二次确认 | 页面当前配置必须configured且enabled；维护者是正式doc_control+project-code:update。按已存在正确配置可直接用，不拿admin替代缺配置 |
| 创建目录模板 | 产品创建弹框“文件夹模板库”或项目详情同名按钮 →`FolderTemplateLibraryEditor` | 启用模板包含至少一真实folder。已有正确模板直接选择；需task fixture时用本页面新增，名称/原因可追溯，不API造目录 |
| 项目正式负责人与编制权限 | 申请“项目负责人”正式账号；批准创建后G39初始化该唯一leader为USER/OWNER。项目详情“项目 OWNER/编制权限”打开正式规则编辑 | 初始leader可读项目并编制；若申请人不同，须由有正式配置权的操作者在页面明确加其EDIT/OWNER规则，不能因为登录是admin就绕过项目reader。不要把任意OWNER当引用唯一负责人 |
| **项目文件模板** | DCC项目代码列表→实际新项目详情→“项目文件模板”→“编辑模板”，`dcc-project-file-template-dialog` | 这是类型/文件名清单，和创建时的**文件夹模板**不同。新建项目没有自动生成此清单；必须用已有合法项目清单或在页面“新增文件项”选至少三级阶段/文件类型、填模板文件名、保存模板 |
| 类型/分类/正式目录 | “文件类型”页/实际项目模板cascader，分类唯一映射与默认存储目录；上传选择项目后页面正式读取 | 必须是启用叶子类型、唯一分类、合法默认NAS存储目录；逻辑项目folderId与NAS directoryId分别由页面选择/带出，不按名称相同推断 |
| 本动作审批矩阵 | 正式审批路线页 `routes/index.vue`→`routes/components/RouteForm.vue`，动作类型明确上传/升版/作废 | 上传必须NEW动作真实路线：会签DEPT leader、批准人、文控审核等按正式候选；旧传统LEGACY审阅矩阵不能充当NEW路线。已有正确配置直接用，不能后台接口指定假候选 |
| 实际审批人资格 | 上传页“提交前校验”/`dcc-upload-route-readiness` | 系统岗位、阶段权限、电子签名授权、有效签名图片全部通过；任何blocker按真实配置/本人页面处理。签名图片正式个人入口为统一签名治理“My Signature”pane，需真实图片，不用合成图片或mock通过 |

实际账号只有Root页面确认后才成为可用fixture。可以由同一账号兼任已有职责，但每项正式配置与任务义务仍需匹配，并逐个真实节点办理，不因开发阶段免质量批准而跳过文件业务签名。

## 最短正向操作

### 1. 新建项目和产品

进入实际“DCC产品目录”菜单→点击 `dcc-project-product-create-open`“新建项目代码及产品”→填任务自有项目名称/项目代码/产品编码/产品名称/分类→选择正式负责人账号→选择启用目录模板→填写目标市场、是否注册人、是否生产方、文件转移及条件说明→填写新建申请原因→核“本次审核人”→“确认申请信息”→`dcc-project-product-confirmation`核字段→“提交申请”。

通过 `dcc-project-product-records-open`“项目申请与审批”查精确projectCode+productCode行，当前表status直接显示原代码PENDING_REVIEW。真实配置审核人登录，在此行“审核通过”，接受页面自然 `window.prompt('请输入通过意见')`并填原因；刷新为PENDING_APPROVAL。既定项目批准账号登录，精确行“批准通过”、同prompt真实意见；刷新为COMPLETED。不能只凭成功toast判完成。

COMPLETED后进入“DCC项目代码”实际列表查新项目→详情核正式leader、三组默认和生成的项目存储folder；按上表补项目文件模板。若上传人和leader不同，通过正式权限页配置任务应有编制主体后再由该人进入上传，不写库准备权限。

### 2. 上传并正式送审

进入“上传文档”实际菜单，唯一 `.upload-form`：DCC项目选择完整真实option→等待项目默认属性、生成产品/目录等加载结束→核默认并编辑本申请实际属性→选逻辑“项目文件夹”→登记说明→阶段→文件类型→从 `dcc-upload-project-template-file-list`“文件列表”选择该项目模板名称→填文件编号/初始版本号/生效日期/提交备注。

在“受控文件”form-item唯一hidden `input[type=file]`上传真实任务自有源文件；不要求hidden输入本身visible，不选普通附件/培训输入。源文件全名、类型和预览必须真实成功；图纸需要配套PDF时在本页面相应输入补齐。按本次部门选择 `dcc-upload-signoff-departments`，核 `dcc-upload-matrix-approvers`实际批准人；培训默认不选，本条最短首轮可按实际任务选择无培训，随后另做需培训主线。需要关联时点击本表“关联”，用现有统一selector自然选择授权目标、确认带回，不能API补关系。

点击“创建受控文件”→核 `dcc-upload-submit-confirmation`所有实际字段→“确认提交”。提交有对象上传副作用和正式申请两个阶段，失败要区分是否preview已写，不自动重提。

成功后进入真实文件浏览器，切“项目文件与引用”，按新项目→folder→版本浏览范围“审批中 · 会签”或“全部版本”，再按精确文件编号+版本定位；新文件尚未受控，**不能用默认“最新受控版本”空结果判断失败**。列表显示中文“待会签审核”。点击该行“操作面板”核 `dcc-detail-formal-version-facts`文件ID/Master/编号/版本/BPM；记录真实身份后继续审批，不能猜BPM/task值。

### 3. 指派与真实会签

实际部门leader登录→“审批中心”实际待办页→按任务文件编号/版本/阶段精确行的 `[data-approval-action='view']`“查看”自然进入DCC处理详情。读取自然URL/页面里的taskId/BPM用于比对，不把手工拼URL当操作。确认当前部门与义务。

`DetailSignoffAssignment`/`SignoffAssignmentPanel`内选择“选择本部门会签人”、填“指派意见”、填“当前账号签名密码”→“签名确认指派”。同leader负责多个部门也要独立逐task办理。可以选本人但不能以指派签名代替下一步实际会签。

被指定会签人本人进入其真实待办→“查看”→当前阶段可用通过按钮→真实签名弹框核文件/版本/当前节点/taskID→填审批意见和登录密码→“确认签名”。每个部门完成后通过页面刷新待办/本版本申请历史确认正式记录，直至进入批准。

### 4. 批准、培训（如选）、文控审核和受控

实际矩阵批准人打开其待办→真实批准弹框，在 `dcc-approval-file-owner-picker`选择当前正式启用文件负责人→审批意见/登录密码→“确认签名”。此处选择负责人，不新增任务节点；作废批准不使用该picker。

若本申请勾培训，实际文控通过项目浏览版本范围“审批中 · 培训记录”找到精确文件→操作面板→“上传培训记录”弹框，“培训记录”唯一file input上传本次线下真实文件→文件名已显示后“确认上传”→刷新 `dcc-detail-training-record-evidence`核本次记录，进入文控审核。

文控审核人从其真实待办自然进入DCC详情，填写真实通过意见/登录密码及该正式任务要求的受控PDF/存入路径（如真实readiness要求）→签名确认。盖章PDF字段是“盖章 PDF”，存入路径确认testid `dcc-doc-control-confirmed-directory`。以当前正式readiness和页面证据决定要求，不能为了执行旧脚本擅自新增第四审批节点。

页面核实际controlledTime、生效日期分开、状态受控/受控待生效、正文和历史；文控从待下发列表再按正式部门/接收方式/人员下发。完整版本/关联/引用/作废后续采用现有G30/G31模块的真实入口，不能把首轮上传→受控部分当整目标完成。

## 现有runner源码匹配与具体校准缺口

| runner/阶段 | 已确认实际源码匹配 | 实际执行前的具体处理 |
|---|---|---|
| G29 project-product | create testid、真实表单标签、提交二次确认、records行、审核/批准native prompt、raw status都与当前SFC相符 | runner不创建模板、不维护reviewer/项目权限/项目文件模板；这些必须先由真实页面取得或准备。postconditions的project-records为整个dialog，若多个同状态旧行使exact状态命中多项，Root应按已定位task row断言，不能切第一个状态或放宽业务身份 |
| G29 upload-submit | `.upload-form`、项目folder/阶段/type/文件列表、源hidden input、部门/批准人/培训、二次确认真实存在 | runner成功后直接post/reload找fileNumber/version，不切ProjectBrowser的在途versionView；默认LATEST_CONTROLLED看不到新申请。必须自然切审批中/ALL并用中文状态，不能用PENDING_MATRIX_REVIEW当DOM原字符串 |
| G29 native-task | approval中心行“查看”、实际task/BPM自然query比对、指派密码/按钮、ownerpicker、真实签名弹框和taskID展示存在 | expectedTaskId/expectedBpmId/historyRoundLabel/stageLabel/openActionButton/dialogTitle/ownerOptionLabel只从实际上一步页面观察后填，不能来自假数据。当前中文阶段是会签审核/会签批准/文控审核；动态dialog/按钮用当前所见，不硬套旧代码值 |
| G29 training | 真实项目folder/versionView→操作面板、上传培训记录弹框/输入/确认、当前记录testid匹配 | 按实际需要培训的本轮BPM使用，培训文件实际已有且ticket完整；无培训首轮不执行该模式 |
| G30 version/relations/reference | ProjectBrowser行、detail自然存储导航、checkout/checkin、独立正式变更、关系selector和引用/取消callback均为真实入口 | 使用本轮实际file/Master/版本/leader及已保存source；c.expectedSubmittedStatus仍是DOM文本，应填中文状态，不能原代码。已有旧脚本不覆盖新增前置配置或G44当前项目创建动作 |
| G31 distribution/obsolete | 真实workbench/下发modal、controlledselector/作废申请modal及审批终态入口 | 仍需先由实际主线产生受控task data；脚本的终态读不等于直接提前创建/批准业务。保留其“需Root只读佐证20年/实际BPM终态”限制 |

G29/G30/G31代码使用Playwright实际Locator/click/fill/check/setInputFiles，登录一次page.goto，未检出fetch/APIRequest/evaluate/router注入/force-click执行桥。它们按任务标记、自然菜单与savedDOM核验、凭据trace暂停/清空控制运行，不mock业务成功。但离线29/26/25合同测试及源码匹配不能证明当前真实DOM/actor和业务动作已通过。

G27计划中35/39缺原件、未获对象授权、QA阻塞文字是历史准备状态，已被Root后续实际G39/G43证据覆盖，不再次作为运行前置。引用/升版/下发/作废细节和全部27项最终闭环仍保留，不扩新负向脚本。

## 运行证据和秘密边界

全部业务准备与动作经真实页面。监听自然业务请求可记method/path/status；不用API response JSON或数据库造数据作为业务动作/页面结果oracle。实际业务ID取页面/自然导航，Long按字符串保留。密码/电子签名操作时暂停trace与网络正文/截图捕获，输入清空后再恢复；真实用户名身份标签可记录，真实密码/token/Cookie不输出、不放contract/日志/storageState。先记录每步具体页面结果，失败后核已产生效果而非自动再提交。

本报告不执行validate-only或任何browser脚本，不修改runner、共享SFC或账号配置。Root按已观察DOM实施最短正向操作并收据验明后继续原目标。

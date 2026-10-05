# G53 — 四项修复的真实页面入口验收准备

Status: prepared_read_only_not_executed。依据`docs/product/dcc-final-requirements.html` v1.6的上传、项目产品创建、申请驳回重提与AC-01/02/06，以及主管理`g48-g49-mainflow-acceptance-plan.md`。用户对g50三项本机升级及真实页面验收已明确授权，实际执行只归Root。本Agent仅只读当前正式源码、旧g44页面操作helper与规则并写本计划，未运行Playwright/浏览器、HTTP/API、DB、服务或Git；源码存在和离线测试都不冒真实页面PASS。

## 唯一环境与执行前提

- 唯一仓库`C:/IntRuoyiAll-int_main`，当前`.git/HEAD`为`refs/heads/int_qms`；槽位0、前端`http://127.0.0.1:8061`、后端48061（`docs/branch-runtime-ports.md`）。目录名含int_main不代表业务运行分支。不能用旧8067/48067或48081代本次环境。
- Root先完成g50批准的四产品来源列/映射表/通知模板三个精确迁移，并核同源最终包、实际端口进程归属、启动成功与前端代理。最终g50源码包SHA已记录在Root交付，不把未启动包或旧界面当新实现运行证据。
- 真实测试租户使用已确认本机租户。申请人A、冻结审核人R、合法批准人P、选定负责人L使用实际启用同租户账号及已有正式权限；不自动授角色、猜密码或把admin当任意actor。当前后端合法项目批准人仍为同租户准确username=admin并有project query/update权限；R来自实际配置快照，L来自创建表单选择。Root只读核身份和权限，缺账号/可见菜单准确留前置，不绕过。
- 从真实登录和可见菜单进入模块。需要配置审核人时沿已有正式配置页面，不SQL造配置。自然网络只保方法/path/status和必要非敏感业务身份/字段，不保存token、cookie、密码、认证响应或全payload。Root若要声明逐步截图则遵守trace证据与敏感边界；原helper快照本身不能代替trace。

## 最短阳性页面顺序与准确定位

### 1. 项目产品申请 → 审核待办 → 批准待办 → 生成可用项目

1. A从菜单进入DCC产品目录，真实路由应为`/mdm/product-catalog`。Root已有`g49-route-template-readonly-preflight.json`证明menu990216/component，不凭旧截图拼旧component URL。
2. 核对后台审核人；当前入口`[data-testid="dcc-project-reviewer-config-open"]`，弹框“项目及产品创建审核人”可见“当前配置”。若已正确配置，读取即可；需修改时只沿可见“审核人/配置原因/保存配置”正式流程。新建弹框“本次审核人”应显示同一实际R。
3. 点击`[data-testid="dcc-project-product-create-open"]`。用当前可见dialog内`.el-form-item`按label定位输入：项目名称、项目代码、项目负责人（placeholder“选择系统账号”）、产品编码、产品名称、分类、目录模板（“选择启用模板”）、备注及新建申请原因。原源码使用label文本而无每字段testid，不以全页输入nth硬编码。
4. 创建一套任务自有名称/代码，产品代码独立于项目代码且合法，不强行凑成旧MDM14位格式。选择实际L和启用文件夹模板；选明确项目默认属性，例如NMPA国内、注册人是、生产方否、文件转移否。每个“是/否”radio必须按所属form-item scope，避免选错注册人/生产方/转移。
5. 点击“确认申请信息”，核对`[data-testid="dcc-project-product-confirmation"]`的真实项目、产品、负责人、模板、三组默认属性、配置审核人，再点击确认框“提交申请”。仅提交成功不等于项目已物化。
6. R真实登录 → 统一审批中心当前待办 → 用任务自有项目/产品标题定位准确行。`[data-testid="approval-center-dcc-key-fields"]`应显示项目代码与“当前审批节点：项目产品审核”，来源中文“项目产品审核”，不冒文件编号/版本/文件类型。
7. **当前中心native项目行的真实入口为该行`[data-approval-action="view"]`“查看”**，不是全页“审核”或不存在的“办理”。backend只给PROCESS_IN_MODULE，requiresSignature=false；“查看”经现openModuleDetail准确导航`/mdm/product-catalog?requestId=<原Long字符串>&requestOpen=records&from=approval-center`。项目不进入中心quick签名框。
8. 原“项目申请与审批”dialog自动打开准确request并展开快照。自然GET为`/admin-api/dcc/project-product-requests/{id}`；不能pending列表未命中后改查其他申请。核本项目资料、冻结审核人、模板/属性，点击同一准确记录“审核通过”；实际原window.prompt输入本次真实审核意见。状态和审核待办应更新；R已办保其结果。
9. P真实登录 → 本项目“项目产品批准”待办 → 同一行“查看” → 原申请准确records → “批准通过”并填写意见。页面刷新显示COMPLETED/生成结果，文件夹生成与产品目录实际列表可见；异步WRITING仅受理不能当成功，不重复点击。
10. L真实登录核已生成项目可进入。HTML明确选定L初始化正式USER/OWNER、其他人不自动获得权限；不靠A/R/P/admin身份推断访问。项目负责人可用事实必须通过正式项目选择/页面证明。

### 2. 新批准项目 → 只选项目文件夹 → 任意合法原件 → 送审

1. L或具有该项目正式上传权限的实际actor从可见上传文档入口进入正常CONTROLLED_FILE/NEW，定位`[data-testid="dcc-upload-single-page-workbench"]`。不要进入外部评审来绕项目合同。
2. 通过placeholder“请选择 DCC 项目”选择刚批准的准确项目。核只读“产品编号”和`[data-testid="dcc-upload-project-product-identity"]`显示实际产品名及“已批准项目产品目录”来源、目录/关系/申请原ID；不能显示projectCode冒productCode或把catalogID叫MDM ID。
3. 核项目三组默认属性实际带出，随后只修改本次申请一项并记录前后值。通过placeholder“请选择项目文件夹”选择模板生成的项目逻辑folder，填写“登记说明”。界面正常NEW不得出现label“提交目录”/额外NAS根叶选择；预检只显示逻辑folder已选与提交时配置，不能宣称内部映射已存在。
4. placeholder“请选择启用的文件类型”是**单一正式type下拉**，option完整路径；选择确实启用且唯一activecategory的类型。`[data-testid="dcc-upload-category-leaf-display"]`应来自正式category。不存在项目预设阶段/文件名必选资格；模板名称仅参考，没有模板名单的真实文件仍可选。
5. 原件用真实有效docx等支持格式，任务自有完整文件名且**不属于项目模板名单**，文件有有效实际字节。正文“选择文件”所属el-upload内`input[type=file]`执行setInputFiles（该input本来隐藏，输入选择仍是正式页面动作），避免误用图纸PDF或普通附件input；当前原件accept为doc/docx/xls/xlsx/pdf/dwg/sldprt/sldasm/slddrw。不借扩展名伪造格式；初轮选有效docx可避免额外图纸PDF合同。
6. 等自然预览请求结束，页面“文件名称”readonly显示实际含后缀完整名，预览不报`[data-testid="dcc-upload-preview-error"]`。核真实原件/会签部门/批准人；`dcc-upload-signoff-departments`、`dcc-upload-matrix-approvers`和`dcc-upload-route-readiness`可复用。readiness不通过应读准确阶段/姓名/ID/原因，不自动换人或隐藏错误。
7. 填任务自有文件编号、初始版本A/1及明确晚于当前日期的生效日期；`[data-testid="dcc-upload-need-training"]`默认不选择。培训若选，后续仍文控上传本次线下记录，不增加逐人线上确认。未关联也应在二次确认显示“未关联”，不是伪关联。
8. 点击`dcc-upload-section-submit`内“创建受控文件”，核`[data-testid="dcc-upload-submit-confirmation"]`包含项目/folder/type、真实全名、编号、版本、生效日期、申请实际属性、部门/批准人与培训。先“取消”证明确认前零业务创建且保输入，再重开“确认提交”；等待导航/列表出现真实文件后才证明已送审。
9. 正常NEW自然working/submit request须有准确projectFolderId、type、SOURCE票据/session、三组实际属性，**完全没有directoryId属性**（不能null/root/folder代理）；自然preview后type不加载第二NAS tree。Root可只读核实际文件内部storage mapping、位置及来源ID，API/DB不能替代上传/送审动作。再从项目默认回显核项目默认未被本次改值反写。
10. 本四修复至少证明上传进入真实会签。若按Root既有主链继续到培训/文控审核/受控/下发，沿真实既有办理入口及签名/负责人合同；不要把仅上传成功冒全部生命周期PASS。本计划不新增该旧生命周期runner。

### 3. 驳回通知 → 原申请修改重提 → 历史保留

1. 另建一个任务自有项目产品申请，避免改已批准项目/上传资料。R从其准确待办“查看”打开原request，点“审核驳回”；若Root覆盖批准驳回，再让审核通过后由P点“批准驳回”。两者均真实意见prompt，原因明确任务标识。
2. A真实登录，沿顶栏“我的站内信”或个人中心站内信查看**本人**收到的实际“DCC项目及产品申请通知”及真实eventName/原因。列表/顶栏content点击已有共享helper，消息详情还可用“查看项目及产品申请”按钮。
3. 点击准确消息应同源跳转`/mdm/product-catalog?requestId=<原requestID>&requestOpen=records&from=notification`，原ID保持Long字符串；readonly原记录显示rejectReason、提交时审核人、冻结三组属性/模板及申请链。不是file/task/BPM详情，也不能用管理员消息管理列表代收件人验收。
4. 原A点击同一记录“修改后重提”，表单标题“修改驳回申请后重提”，核旧字段恢复但实际可改、原驳回原因可见，填写新“重提说明”；点击“确认重提信息”→真实核对→“提交申请”。现门禁仍只原申请人、REJECTED、无后继。
5. 新申请有不同真实requestId、previousRequestId指向旧ID并重新冻结当次审核人，R收到新待办/消息。打开旧消息仍能准确读原REJECTED和resubmittedRequestId，原决定/原因/模板/属性不覆盖；旧申请不再给重复重提。当前展开区申请链是可见文本，别预设已有可点击“后继链接”。

## 旧g44-real-ui.cjs仅可复用的部分与必须调整项

| 旧helper事实 | Root新本机执行应处理 |
| --- | --- |
| 第7行repo固定已删除`C:/IntRuoyi/20261001-dcc-integration`，createRequire从该路径加载Playwright | 指向唯一main仓实际依赖；旧helper封存不原地改，本Agent不生成新平台。main分支/同源判断以当前Root证据 |
| startup硬断言registry slot6、8067/48067 | 本次是int_qms slot0 8061/48061，沿真实base归属门禁，不用降低guard或fallback旧端口 |
| output固定旧g44-real-ui-r4，目录已存在就失败 | 用本次任务独立证据目录，保历史不覆盖；快照/截图函数本身可复用 |
| locator支持role/name、placeholder、selector及scope；upload走setInputFiles | 能复用上述真实定位；按dialog/业务行scope，不全局first/nth冒真实身份 |
| clickPrompt只接受原因以旧g44前缀开头、并要求prompt.message含“意见” | 真实通过prompt是“请输入通过意见”，**驳回是“请输入驳回原因”**；旧判断会误拒驳回。Root有限适配明确允许当前正式两prompt和本次G53原因，不能盲accept任意dialog |
| 命令login与旧authenticated状态，未提供actor切换操作 | 本次必须实际A/R/P/L登录身份隔离；使用真实退出重登录或独立正式浏览器上下文，不通过storageState/token/API切换代账号 |
| snapshot收DOM、对password mask、自然response仅path/status | 可保这种安全边界；不打印credential正文、认证响应、token/cookie或秘密POST数据。需要精确requestId/项目字段仅提取目标安全事实 |
| chain catch把异常转安全失败信息且继续读stdin | 每一失败按实际page/网络/DOM定位、停止依赖动作；结果不确定先只读核原记录，不重复提交或改成功状态 |

审批中心新增行没有“办理”testid；源代码事实应优先于旧模板runner的动作名。API DTO/路由query只能供自然读回和身份核验，不能让脚本直接调用create/review/approve/resubmit/upload或注入router来承担动作。

## 证据与有限结果口径

Root应记录本次任务对象/实际账号标签、登录租户、菜单入口、原request/后继/生成项目ID、实际产品码和来源、folder/type及原件全名；账号标签与可见姓名分开。各阶段自然页面请求status和可见成功/失败状态与DOM/截图互证；内部映射/消息收件人/旧资料不变可由Root只读佐证。

准备文件结构核验只检查本计划标题、四项步骤/环境/边界和原定位存在；没有实际case PASS。旧source/lint/types/build与旧g44快照不累计成本次页面验收。遇缺配置/真实账号/不可见菜单/正式新GET错误或结果未终态，准确记录具体前置或产品失败并停止依赖路径，不改数据/role/API造成功，也不新增用户问题或开发平台。

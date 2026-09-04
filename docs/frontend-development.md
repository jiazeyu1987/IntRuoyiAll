# IntRuoyi Frontend Development Rules

## 触发场景

- 修改 `IntRuoyiFronted` 下的 Vue、TypeScript、TSX、路由、API 包装、状态管理、样式、构建配置或前端测试前，必须先读取本文件。
- 涉及真实页面验证时，还必须读取 `docs/e2e-rules.md`。
- 涉及本机端口、Vite、前后端代理或服务重启时，还必须读取 `docs/local-runtime.md`。
- 涉及菜单、权限、动态路由或租户绑定时，还必须读取 `docs/database-rules.md` 和 `docs/login-access.md`。

## 项目边界

- 前端根目录：`E:\IntRuoyi\IntRuoyiFronted`。
- 使用 Vue 3、Vite、TypeScript、Element Plus、Pinia、UnoCSS 和 pnpm。
- 必须使用 pnpm；不得混用 npm、yarn 或其他包管理器。
- 保持现有路由、API wrapper、权限、表格、表单、组件和样式模式，避免引入无关设计体系。

## 实施规则

- 先确认页面入口、路由、权限、API、状态和复用组件的现有契约。
- 功能、修复、重构和行为变更必须先记录 BDD，再执行 RED -> GREEN -> REGRESSION。
- 后端或请求失败必须通过 UI、网络、控制台或测试明确暴露。
- 不得使用空 `catch {}`、静默 toast、吞异常或默认成功状态掩盖请求失败。
- 写入接口若设置 `ignoreErrorMessage: true`，调用方必须在当前可见业务区域本地 `catch` 并展示正式错误；已处理的用户操作错误不得继续冒泡成全局“系统异常”。
- 不得为测试额外添加无产品价值的页面控件或绕过真实用户路径。

## 历史快照与当前配置合同隔离门禁

- Trigger: 页面支持在同一 DCC 项目或业务对象下切换当前版本、草稿版本、历史已发布版本或已作废版本，同时页面还会加载设备配置、人员配置、表单槽位等“当前配置”合同。
- Preflight check: 先区分所选版本的冻结快照展示和当前配置的可编辑辅助数据；历史已发布/已作废版本只能读取自身快照，不得把历史 itemCodes、processIds 或表单绑定身份传给按当前版本校验的配置接口。
- Blocker: 切换历史版本后仍调用当前配置接口并触发项目项名称、同名项完整性、设备配置一致性等合同校验，或用当前配置结果覆盖历史快照时必须停止。
- Verification: 静态合同应锁定历史版本分支不调用当前配置加载，且当前发布/草稿版本仍保留正式配置加载；真实页面验证时要覆盖至少一个历史版本和当前版本切换。
- Evidence: 任务 `doc/tasks/20260904-qa-regulation-version-status-dropdown/`，QA 规程版本下拉切换 B/2 历史版本时，历史 itemCodes 误触发当前 PQC 设备配置合同，报 `itemEquipmentConfig.itemCodes.itemNameMismatch`；修复为仅当前发布或草稿版本加载设备绑定。

## 前端入口态与详情状态判定门禁

- Trigger: 列表页存在多个入口进入同一个详情页，例如当前库、旧证库、授权查看、审批详情；同一个下载、预览或操作按钮需要按入口语义改变展示或文件名。
- Preflight check: 先同时核对路由 `mode`、业务对象 ID、版本 ID、详情接口状态字段和目标文件 ID。若用户日志只显示最终布尔值，必须补充入口模式、详情状态、路由版本 ID、详情主文件 ID 和实际点击文件 ID，避免只看到 `true/false` 无法定位。
- Blocker: 只依赖详情状态字段判断入口语义、未校验点击文件是否等于详情主文件、或把变更/延续附件误判为主注册证文件时必须停止。
- Verification: 静态合同应锁定入口模式与正式状态共同参与判定，并锁定文件 ID 等值限制；真实页面验证时应从目标列表入口进入，而不是直接拼详情 URL 或 API-only。
- Forbidden action: 禁止用默认失效、默认当前、文件名包含关键字、列表缓存或后端文件名猜测入口语义；禁止让预览链路、申请下载链路和正式下载链路互相替代验证。
- Evidence: 任务 `doc/tasks/20260902-registration-old-download-expired-filename-fix/`，老证详情下载日志出现 `expired: false` 后，将旧证判定从仅 `detail.status === 'OLD'` 补强为 `mode=old-detail` 或正式 OLD 状态，并且仅作用于详情主注册证文件。
- Detail action state extension: 详情附件区的“申请下载”这类状态型动作，应直接调用正式申请接口并原地切换按钮状态；审批结果、撤销授权、grant 下载等治理控件只属于明确的审批/工作台入口；若产品要求去除独立治理入口，静态合同必须锁定对应页签、testid、handler 和 API import 在源码中均不存在，而不是只断言普通详情页不可见。刷新后状态必须来自后端只读投影（如当前用户待处理申请 ID），不得靠路由 `mode`、滚动到面板或前端临时缓存冒充持久状态。若同页同时存在内联按钮和流程/访问申请面板，静态合同必须覆盖所有可提交同一申请的入口；项目代码等可选业务事实不得在任一入口被重新变成前端必填。Evidence: 任务 `doc/tasks/20260903-registration-download-request-inline-ux/`、`doc/tasks/20260903-registration-download-request-project-scope/`、`doc/tasks/20260904-registration-download-flow-alignment/`。

## 前端源码目录与 .gitignore 门禁

- Trigger: Vite 报 `[plugin:vite:import-analysis] Failed to resolve import`，且目标是前端源码目录中名为 `logs`、`runtime`、`output` 等容易命中忽略规则的业务页面目录。
- Preflight check: 先执行 `git check-ignore -v -- <目标源码文件>` 和 `git status --short -- <目标源码文件>`，确认文件不是被根 `.gitignore` 的通用产物规则隐藏。
- Blocker: 若业务源码文件被 `logs/`、`runtime/`、`output/` 等通用规则忽略，必须先补精确 `!` 例外或调整目录命名，再补组件文件；不得只在本地复制文件后宣称完成。
- Verification: 目标文件在 `git status --short -- <目标源码文件>` 中可见为待跟踪/已跟踪，相关静态合同或 Vite 构建检查通过。
- Forbidden action: 禁止用关闭 Vite overlay、改路由到占位页、复制未跟踪文件、或把动态路由/权限问题误判为组件不存在来绕过根因。
- Evidence: 任务 `doc/tasks/20260725-dcc-controlled-file-logs-import/`，DCC 文控日志页面目录被 `.gitignore` 的 `logs/` 规则隐藏。

## 前端静态契约隔离门禁

- Trigger: 当前任务需要 RED/GREEN 静态契约，但已有大契约或全量 `pnpm ts:check` 先失败在无关历史问题上。
- API 静态合同应容忍合法的函数签名换行、缩进和格式化差异，只锁定参数、正式 URL、载荷和错误行为；排版导致的失败应收窄正则并记录为无关噪声，不得修改生产 API 来迎合脆弱合同。
- SFC 静态合同包含负向断言时，必须先截取目标 handler、模板或配置块，再在该块内断言旧能力不存在；当旧能力在同页其它正式流程仍存在时，不得扫描整页、整段 `<script>`、所有 import 或共享 helper 名称。
- Preflight check: 先运行最接近的既有契约并冻结首个无关失败；若失败点不属于当前任务，新增或改用任务专用最小静态契约覆盖当前行为。静态合同从单个源码文件截取函数、模板或配置块时，结束锚点必须是“下一个明确同类块/函数名”或配对标记；测试列表、弹窗或标准组件时优先使用该组件自身的结束标签，不得借用相邻且可能被删除的展示区块标记作为结束边界；解析 TypeScript 数组声明如 `const x: Type[] = [...]` 时，必须先定位赋值号再寻找数组起始 `[`，避免把类型标注 `[]` 当作数组字面量；同文件后续可能追加相邻产品模板、角色模板或配置块时，禁止用宽泛的 `const qaRegulationItems`、`</script>`、文件结尾等远端锚点导致新增块被旧合同误计数。
- Blocker: 无法证明失败点与当前任务无关、或专用契约不能稳定先 RED 后 GREEN 时，不得宣称当前行为完成。新增相邻模板后，既有合同若出现行数翻倍、误报重复项或负向断言跨块命中，必须先收窄旧合同边界再判断业务是否回归，不得为了通过测试删除新模板或放宽计数断言。
- Verification: `execution-log.md` 同时记录无关 blocker、专用契约 RED/GREEN、以及全量回归命令的剩余阻塞摘要。
- Forbidden action: 禁止修改无关大契约来绕过历史失败；禁止把无关 `ts:check` blocker 当成本任务通过证据；禁止跳过当前需求的最小 RED/GREEN。
- Evidence: 任务 `doc/tasks/20260726-release-action-error-autohide/`，既有 eDHR 大契约先失败于历史模型断言，本任务改用 `edhr-release-action-error-autohide-static.spec.js` 隔离 5 秒自动隐藏行为。任务 `doc/tasks/20260806-qa-id-balloon-pressure-pump-pdf-items/`，新增 `PQC-ID-001` 相邻产品模板后，旧 `PQC-IDI-001` 静态合同原本用 `const qaRegulationItems` 作远端结束锚点，误把新 17 行计入旧 22 行合同；最终将旧合同结束锚点收窄到 `const createBalloonPressurePumpQaRegulationItems`，并新增 ID 专用合同。任务 `doc/tasks/20260806-qa-idi-pressure-pump-screenshot-pages-verify/`，逐页截图对表时必须同时锁定源码顺序、PDF 页码、`itemName` 和 `sourceOriginalItem`，避免图 4 `整体粘结 / 外观` 被后续图 5 `气密性` 合并单元格分组污染。任务 `doc/tasks/20260807-team-leader-review-leader-name/`，`data-production-leader-module-tab-report\b` 会把 `data-production-leader-module-tab-report-history` 一并计入，因为 `-` 是非单词字符；静态合同统计 `data-*` 前缀时必须使用 `(?=[\s/>])`、负向断言或完整属性边界。
- Evidence extension: 任务 `doc/tasks/20260830-nas-original-path-sync/`，NAS 未受控文件新增“按原路径同步”流程时，同页仍保留浏览器本地归类下载 `showDirectoryPicker`；静态合同必须抽取 `handleSyncNasOriginalPathFiles` 目标 handler 后再做负向断言，不能整页禁止 `showDirectoryPicker`。
- Field cardinality migration extension: 单选字段升级为多选字段时，前端静态合同必须同步从单值字段迁移到数组字段，并覆盖展示、编辑、提交载荷和历史快照解析四处边界；例如设备从 `selectedDevice` 升级为 `selectedDevices` 后，“修改报工内容”等回看/修改弹框不得仍按单设备平铺展示，需按物料和设备分组，设备参数必须按设备身份过滤展示，旧单设备字段只能作为历史快照读取来源，不能作为新提交合同。列表页遇到多物料、多设备、多参数这类一对多/多对多提交时，主表默认列应承载可读摘要和异常数量，完整明细放入展开行、抽屉或详情页，并用静态合同锁定主表不再横向平铺全部事实。若明细存在父子归属（如物料 -> 设备 -> 参数），展示也必须嵌套在父项下，设备行不能脱离物料另做跨物料汇总区块；每台设备宜独占一行承载设备名称、设备编号和本设备参数；参数为多个短字段时优先用中文分号串联成一行，避免逐参数换行导致列表被拉高，也避免用户误判设备属于哪个物料。

## 前端日期响应格式门禁

- Trigger: 前端页面、详情页、弹框、表格列或导出文件展示后端 Java 日期字段，尤其是 `LocalDate`、审批日期、生效日期、有效期、失效日期、旧证详情失效提示。
- Preflight check: 先确认目标接口真实日期响应形态，并在 API 类型或共享工具中表达合法格式；同一业务域内的列表、详情和弹框必须复用同一格式化函数，覆盖字符串日期和 Java `LocalDate` 数组 `[year, month, day]`。
- Blocker: 页面只声明 `string` 但真实接口可能返回数组、旧证详情/失效提示直接使用通用空值展示、月份/日期未补零、数组长度或日历日期非法仍继续渲染、或不同页面各自拼接日期导致同一字段显示不一致时必须停止。
- Verification: 聚焦静态合同先 RED 再 GREEN，锁定 API 日期联合类型、共享格式化函数、数组补零、非法日期显式失败，以及目标列表/详情/弹框均调用共享函数；真实 E2E 涉及旧证库时必须断言失效日期文字来自目标旧版本。
- Forbidden action: 禁止用空值、默认今天、字符串强转、前端猜日期、吞掉无效日期、只改某一页展示或 API-only 响应截图冒充页面显示正确。
- Evidence: 任务 `doc/tasks/20260829-registration-certificate-renewal-category-notify/`，注册证旧证详情在真实续证生效 E2E 前补齐旧版本 `versionId` 读取，同时统一处理 Java `LocalDate` 数组，旧证详情最终显示“已失效，失效日期 2026-12-31”。

## 编辑弹框当前值回显与提交能力一致性门禁

- Trigger: 列表行打开编辑、变更或审批提交弹框，用户要求选择字段后显示当前已设置数据并允许在原值上修改。
- Preflight check: 列表行只负责对象身份和入口展示；弹框打开后必须读取能覆盖全部可编辑字段、当前版本和并发版本号的正式详情接口，并先初始化所有字段当前值，再按用户所选 key 控制可见性。提交载荷、后端校验、审批事实和审批通过后的投影字段必须使用同一组 key；生产关系等复合字段必须与其主字段保存在同一审批事实中。提交前本地必填校验必须先于 loading 和正式请求，校验失败在弹框内保留稳定错误区域；接口失败也要在弹框内显示可追踪错误。
- Blocker: 只有少数字段生成输入框、输入框只有 placeholder 没有当前值、用列表简化字段补齐详情、详情失败仍允许提交、前端开放了后端会拒绝或忽略的 key、审批通过只更新前端可编辑字段的一部分、生产地址值与生产关系分离丢失、必填校验只依赖短暂 toast、或预期校验异常抛给事件系统导致用户认为“确认没反应”时必须停止。
- Verification: 前端聚焦合同覆盖全部 key、详情请求、当前值映射、加载失败可见、提交阻断、校验错误内联显示、校验失败不发起正式请求、接口失败错误保留和成功关闭刷新；后端定向测试覆盖提交前当前快照不变、审批后全部选中字段更新、未选字段保持不变、复合关系同步更新和历史明细完整；再运行相邻静态合同、类型检查和受影响服务测试。
- Forbidden action: 禁止用空字符串、列表值、placeholder 或本地缓存冒充当前详情；禁止只改前端让不可执行字段看似可编辑；禁止后端默默丢弃已选择字段或审批后只更新白名单子集；禁止把短暂 toast 或控制台异常当成弹框提交失败的唯一反馈。
- Evidence: `doc/tasks/20260901-registration-change-all-fields-prefill/verification-report.md`；`doc/tasks/20260902-registration-change-confirm-no-response/verification-report.md`。

## 前端重型设计器全局注册隔离门禁

- Trigger: 页面进入后加载 `form-designer`、富文本/流程/报表设计器、图形编辑器等重型设计器 chunk，且目标页面并不渲染对应设计器组件；或出现 `Maximum call stack size exceeded`、重复权限 toast、非目标 chunk pageerror。
- Preflight check: 先区分 runtime 表单能力和设计器能力。只服务 `<fc-designer>` 等专用页面的设计器包不得在 `setup*` 全局插件中 import/install；必须用聚焦静态合同同时锁定全局插件没有 designer import/install，并锁定实际设计器页面局部 import 与组件渲染入口仍存在。
- Blocker: 非设计器页面依旧通过全局插件、全局组件、动态路由副作用或共享入口加载 designer chunk；或者修复后 BPM/Infra 等实际设计器页面缺少局部 import 时，必须停止。
- Verification: 先让全局 designer import/install 静态合同 RED，再移除全局注册并 GREEN；运行相邻真实设计器入口、本机目标页面 Playwright 复验、`pnpm ts:check` 和 `git diff --check`。生产或测试服复现过的 chunk 问题还需部署后用同一路径复测。
- Forbidden action: 禁止用隐藏 console error、吞掉 pageerror、关闭全局错误处理、保留全局 designer 注册但改文案、或让非设计器页面继续加载重型 chunk 来冒充修复。
- Evidence: 任务 `doc/tasks/20260808-process-route-editor-stack-overflow-repro/`，测试服 MES 工艺路线编辑页每次进入新增 2 条 `RangeError`，stack 指向 `assets/form-designer-3YqQ_Q1F.js`；根因是全局 `setupFormCreate(app)` 安装 `@form-create/designer`，修复为仅 BPM/Infra 设计器页面局部 import。

## 用户可见描述与内部编码隔离门禁

- Trigger: 用户要求列表、卡片、标签、下拉选项或明细区域显示名称、描述、详情而不是编码，且同一响应同时存在 `*Name` / `*Description` 与 `*Code` / `*Id` 字段。
- Preflight check: 先按截图或稳定 DOM 锚点定位真实可见区域，再追溯该区域的映射函数、正式响应 VO 和提交载荷；明确描述字段只负责展示，编码和 ID 只负责 key、编辑定位或提交身份。
  - Blocker: 可见标签仍使用 `description || code || id`、描述字段缺失却准备用编码或编号占位掩盖、或修改展示时同时丢失 key/提交身份字段，必须停止；正式描述数据链路缺失时应暴露并补齐契约，不得前端猜测。
  - Verification: 聚焦静态合同应抽取目标可见区域与映射块，正向锁定可见标签直接读取正式描述字段，负向禁止编码/ID fallback；相邻提交合同必须证明编码和 ID 仍保留在正式载荷，再运行相关静态回归、`pnpm ts:check` 和 `git diff --check`。
  - Forbidden action: 禁止用 CSS 隐藏编码、用 tooltip 或占位文案冒充详情、把内部编码改写成描述、删除提交身份字段、或以其它页面显示正确代替截图目标区域验证。
  - Menu-name mapping extension: 配置页、同步列表或跨模块映射列展示“本地页签/本地列表/目标页面”时，必须读取当前系统真实菜单名或页面标题作为可见名称；不得按模块前缀拼接 `ERP*`、`MES*`、`金蝶*` 或用“只读列表”等内部说明替代真实页签。一个同步项写入多个正式页面时，可以用真实菜单名组合展示，例如 `产品信息 / 物料产品管理`，但组合中的每一段都必须来自正式菜单或页面名称。`syncType`、handlerName、API 路径只作为内部身份，不得直接决定可见页签名称。
  - Approval-summary extension: 审批中心所有页签的业务摘要都必须直接展示后端返回的正式标题；即使标题仍含英文，也不得用“未配置中文标题”替换已有内容。遇到“中文事项 + 英文/数字业务编号”（如工单号、批次号、执行单号、注册证申请号）时，编号是正式可见区分信息，不得被“业务编号已配置 / 未配置中文值”等占位文案隐藏；`businessContextTags` 属于通用摘要上下文，不得只按 DCC 模块条件展示。已知业务的中文事项、编号和上下文仍必须由后端按正式流程变量生成，前端只允许显示已有内容和执行确定性的既有标题映射，不得猜业务对象。若某类摘要明确不展示业务编号，后端必须返回显式 `businessIdentifierHidden` 契约；前端不得通过标题文本、标签前缀或内部 ID 组合推断隐藏条件。所有审批摘要标签和 DCC 固定摘要字段都必须先过滤空值、`--`、`null`、`undefined`、`未配置*`、`*已配置` 等占位值；缺失的注册证编号和其它可选摘要字段不渲染标签，有正式值才显示。
  - Approval-route extension: 流程模型或审批路线查看弹窗里的标题、“审批路线/批准环节”主展示必须来自流程模型正式显示名或后端正式路线名称；BPMN `userTask` 名称、`taskDefinitionKey`、流程标识和内部英文节点名只用于节点身份、分类或提交，不得作为业务用户可见路线名称。若 BPMN 候选策略是 `START_USER_SELECT` / “由发起人指定”，但业务服务在发起时通过正式角色、权限或组织规则写入 `startUserSelectAssignees`，查看弹窗必须按该业务正式候选来源显示审批对象；不得把平台策略直接展示成最终审核人，也不得显示空审核环节误导用户。候选规则本身已带业务类别标签时必须保留该标签，例如权限角色类候选统一显示为 `审批角色：角色名称`；不得再次外包成 `审批对象：审批角色：角色名称`。正式路线名称或正式候选来源缺失时应显示明确未配置/未识别状态并补齐数据链路，不得退回英文节点名、流程标识或平台策略冒充正常展示。
  - Approval-detail reviewer extension: 流程实例详情、审批时间线和顶部“当前处理人”必须优先使用后端返回的正式审核对象；当审核对象是权限角色时显示 `审批角色：角色名称`，并隐藏候选用户列表，禁止从角色成员、Flowable assignee 或 candidateUsers 中随机挑一个具体用户冒充最终审核人。列表、查看/流程详情、打印或摘要同时出现审核对象时，必须共用同一展示口径。
  - Process-detail copy extension: 流程实例详情的主标题、审批时间线、当前步骤、下一节点、退回节点、节点表单标题和打印内容必须共用同一确定性的显示名称规则。注册证访问等已知业务流程的英文实例标题和审批节点名应映射为正式中文；流程标识、任务定义键、流程编号、ID、用户昵称、审批意见和业务表单自由文本必须保持原值。打印描述若由后端拼接了节点名，只允许解析并替换节点名称段，不得对整段描述做全局英文替换，以免改写用户输入。历史流程快照同样在展示层使用该规则，不能要求补写或猜测历史业务数据。
  - Attachment-detail extension: 详情页需要展示已上传附件时，接口必须从同一正式业务文件绑定返回文件身份和原始文件名，页面直接显示原始文件名；`hasFile`、`hasAttachment` 等布尔状态只可用于筛选或缺失标记，不能冒充附件内容。文件 ID 存在但原始文件名缺失时必须暴露数据链路错误并阻塞，禁止用“已提供”、默认文件名、业务编号或前端拼接替代。验证需同时覆盖后端同一绑定查询、前端原始文件名展示、无附件空态、聚焦 RED/GREEN、类型检查和真实详情路径。
  - Ambiguous-device extension: 当同一类设备相关字段因为多个正式设备共存而难以区分时，页面应在可见标签里直接追加正式 `code/name` 提示或同等级别的可见区分信息；不得只保留笼统“设备编号 / 计量有效期”而把区分信息藏进 tooltip、占位说明或后台 ID。批记录单元格链接的“报工数据”设备字段必须按所选 DCC 项目代码和工序，从路线工序的正式 MES 工序身份读取工序设备绑定与设备主数据，再生成带设备分组作用域的来源字段，例如 `selectedDevice.deviceCode@deviceGroup:<deviceGroup>` 和 `选用设备编码（超声波清洗机）` 可见标签。设备参数目录只显示一线生产实际提交值字段，例如 `deviceParameterReadings.<parameterCode>.value@deviceGroup:<deviceGroup>` 对应的清洗次数、清洗介质、清洗功率、室温、清洗时间；同类多台设备只显示一套参数，不追加 B09393/B09392，不同设备类型的参数必须分别展示。单位、下限、上限、状态、参考标准、默认文本和默认值属于配置/校验信息，不得进入左侧可选目录，也不得用前端合成、通用字段、设备 ID 或空列表掩盖正式设备绑定缺失。
  - Admission-analysis extension: 列表需要解释“可否加入/提交”时，结论和阻断原因必须直接来自同一正式准入读模型（例如 `selectable`、`severity`、`reasonCode`、`message`），页面不得根据产品是否存在、默认状态或其它配置链路自行推断“可加入”。可加入状态必须同时满足统一选择门禁；不可加入状态必须显示服务端正式原因，并保持错误可见。
- Admission-analysis verification: 静态合同应同时锁定绿色可加入分支、红色正式原因分支和选择框的正式 `selectable` 门禁；若目标模块类型检查被无关历史错误阻断，应记录首个错误位置并保留聚焦合同 GREEN 证据，不得修改无关模块或把 API-only 结果冒充页面通过。
- Cell-type-display extension: 批记录、动态表单或类似可填写格子的类型区分，必须从正式字段类型、控件类型和约束解析；当前批记录填写配置采用颜色区分，不在可填写空白格子里显示“文本/数字/日期”等文字标签。不可填写或非正式规则格子必须保持白色背景，包含空格、普通文本格、选中态和 hover，不得使用接近可填写状态的浅蓝/浅灰背景。行列坐标只保留为内部身份或非规则空格子的兜底提示。明确控件类型应优先于通用约束和文本兜底，例如 `radio` 必须先于 `selectionMode=single` 识别，填写态的 `radio-group/option-group/single-choice` 必须进入单选控件，`signature/date/datetime/input-number/textarea/upload*` 必须进入对应控件，避免可映射格子的正式身份在填写页变成普通文本框。填写页模板布局、控件类型和错误提示等渲染派生逻辑必须保持 computed 纯计算，不得在 computed 内写 `parseError.value`、表单值或其它响应式状态，防止进入页面时反复更新或卡死。
- Cell-type-display verification: 聚焦静态合同应抽取目标类型颜色转换函数和填写态控件归一化函数，正向锁定类型颜色 class、正式控件分支、选项来源和非规则格子白底，负向锁定可填写格子不再显示类型文字标签、行列占位、非规则格子类型染色或把非文本控件落成普通文本输入，并断言明确控件类型优先级；不得用整页关键词扫描替代目标函数边界。
- Signature-governance extension: 电子签名记录、签名证据摘要或统一审计列表展示来源、来源表、签名动作、签名含义、证据状态、快照状态时，必须使用确定性的中文显示映射；`hash`、`ID`、`URL`、`PDF` 等技术词和正式业务编号可保留原文。`PASSWORD_VERIFIED`、`CAPTURED`、`APPROVE`、`PQC_SUBMIT`、`bpm_approval_signature_record`、`mes_pro_batch_record_execution_signature` 等内部状态或表名不得直接裸露给业务用户。未知编码必须显示“未识别...”并带原码，暴露配置缺口，不得伪装成已识别中文。
- Evidence: 任务 `doc/tasks/20260807-frontline-defect-description-display/`，一线生产“不良明细”原把 `reasonName || reasonCode || 编号占位` 作为可见标签，最终收敛为直接显示 `reasonName`，同时保留 `reasonId` 与 `reasonCode` 结构化提交。任务 `doc/tasks/20260829-dcc-process-pool-real-device-labels/`，批记录单元格链接“报工数据”设备字段从前端通用占位收敛为后端按 DCC 项目/工序读取正式工序设备和设备主数据，真实只读 E2E 验证多设备标签可区分且无 MES/DCC 写请求。任务 `doc/tasks/20260830-approval-route-name-display/`，流程模型查看审批路线弹窗从 BPMN 英文 `userTask` 节点名收敛为显示流程模型正式名称，并保留审批对象文本。任务 `doc/tasks/20260830-approval-route-dialog-title-name/`，审批路线弹窗标题同步显示 `审批路线：<流程名>`，避免用户只看到通用标题。任务 `doc/tasks/20260830-approval-role-wording-unification/`，权限角色类候选统一显示为 `审批角色：角色名`，避免重复显示 `审批对象：审批角色：角色名`。任务 `doc/tasks/20260901-approval-flow-reviewer-display-sync/`，流程详情时间线和顶部当前处理人改为优先显示后端正式审批角色，避免注册部经理多成员时随机显示具体人员。任务 `doc/tasks/20260831-registration-certificate-detail-attachment/`，注册证详情从附件布尔状态收敛为显示同一版本正式绑定文件的原始文件名。任务 `doc/tasks/20260901-signature-record-chinese-labels/`，统一电子签名记录页将来源、来源表、签名动作、签名含义和证据状态从英文枚举/数据库表名收敛为中文展示，并保留 hash 等技术原文。

## 前端按钮文案与行为一致性门禁

- Trigger: 用户要求将按钮改名、把“刷新/查询/打开”等按钮改成“新增/保存/提交”等动作按钮，或指出按钮显示动作与实际点击行为不一致。
- Preflight check: 静态合同必须同时锁定按钮可见文案、稳定 `data-*` 锚点、`@click` 绑定的新正式动作，以及禁止旧点击方法继续绑定；若新增动作需要选择对象或类型，还必须锁定对应弹窗、表单选择项和正式保存链路。按钮执行的是只读识别、预览、校验、导出等不落库动作时，必须把“可点击读取”和“可应用保存”拆成两个禁用条件，不能因为当前对象只读就误禁用读取按钮。按钮打开动态弹窗时，若使用 `<component :is>` 动态外壳，必须显式导入项目弹窗组件并传组件对象；不得依赖自动组件导入解析字符串名称，否则可能退回浏览器原生 `<dialog>` 导致内容存在但始终隐藏。
- Preflight check: 在已打开的业务 `el-dialog` 内触发行级删除、同步全部、提交等 `ElMessageBox.confirm` 时，必须显式使用项目统一高层级 `modalClass` 或业务专用同等级 overlay class，并用静态合同和真实点击验证确认框位于父弹窗之上；不得只断言确认框 DOM 存在或按钮文字可见。
- 外部助手启动按钮扩展：承载独立助手的页签必须把“助手未启动”作为正式页面状态，按钮文案、稳定锚点、启动请求和启动后重新申请票据进入必须在同一静态合同中锁定；不能先渲染 iframe 再等待错误，也不能点击后只刷新页面或伪造成功提示。
- 批记录单元格链接扩展：报工数据来源的“建立链接”按钮若后端正式要求汇总方式，前端必须在来源字段自动选中和用户点击来源字段时同步设置可见的正式默认汇总方式；数量字段默认求和，包含 `本次报工产出数量`、`本次报工损耗数量` 和后端返回的 `本次报工总量 / totalQuantity`，设备参数、设备身份、签名、时间和确认字段默认最后一笔。批记录表单真实签名位可能是 SIGNATURE 控件，也可能是 `操作人/日期`、`复核人/日期` 等普通文本格；前端必须按签名标记或签名语义标签识别目标格，并且只允许 `提交签名用户`、`审核人签名用户` 这类一线生产签名来源选中签名位，普通数量、设备和参数字段切换后必须取消不兼容签名目标格。不得让空汇总方式成为隐藏禁用条件，也不得仅启用按钮后在点击时再用 warning 阻断保存。
- Blocker: 只改按钮文案但继续调用旧刷新/查询/打开方法、只断言文案不验证点击处理器、用刷新后列表变化冒充新增入口、把只读对象上的纯读取动作误绑定到保存/编辑只读条件、动态弹窗 DOM 已生成但没有可见 overlay，或确认弹窗被父弹窗/固定列/工具栏拦截无法真实点击时必须停止。
- Verification: 先让按钮行为静态合同 RED，再实现正式入口并跑目标合同、相邻合同、`pnpm ts:check` 和 `git diff --check`。
- Forbidden action: 禁止把“新增”按钮继续绑定刷新方法；禁止用新增文案、toast 或 API-only 成功提示替代真实可操作入口。
- Evidence: 任务 `doc/tasks/20260806-process-config-refresh-to-add-button/`，工序配置头部按钮先从“刷新”改成“新增”但仍绑定 `loadProcessConfigRows`，补充合同后改为打开路线工序 + 新增类型选择弹窗并复用正式维护弹窗。任务 `doc/tasks/20260825-form-template-ai-autodetect-auto-draft-version/`，表单模板填写配置的 `AI 自动识别` 可在任意已保存版本执行，识别后自动生成或复用草稿版本；按钮不能被 `readonlyMode` 误禁用，但候选应用和保存仍只允许草稿。任务 `doc/tasks/20260829-form-template-v21-fill-config-no-response/`，表单模板填写配置动态外壳使用字符串 `Dialog`，自动组件导入无法解析，运行时退回原生 `<dialog>` 并保持隐藏，修复为显式导入项目 `Dialog` 组件对象。任务 `doc/tasks/20260829-form-template-v21-fill-config-open/`，V21 已发布版本点击“填写配置”不应隐式触发 `fill-rule-auto-detect`；填写配置入口只负责打开配置面板，规则识别只能由弹窗内显式按钮触发，避免识别接口失败或慢响应表现为点击无响应。任务 `doc/tasks/20260830-nas-original-path-sync/`，主干单文件 E2E 暴露统计弹窗内“移除同步记录”的确认框位于父弹窗下方，修复为原路径同步确认框统一使用高层级 modal class 并用真实点击复验。

## 流程模型人员配置入口门禁

- Trigger: 流程模型列表“新建模型”或“修改”、新建审批模型、审批模型人员维护、审核人/批准人配置、审批对象、或关系、和关系、用户/权限角色/部门/发起对象直属主管。
- Preflight check: 先确认用户要维护的是业务审批人员而不是 BPMN 设计图；“新建模型”和“修改”入口都必须打开审核人/批准人业务配置弹窗，并保存为正式 SIMPLE 流程模型节点。新建时允许用户手工输入任意流程名字，系统自动生成合法流程标识并调用正式模型创建接口，不要求用户进入旧新建流程页面或设计器。弹窗打开时必须先回显当前审批情况：已保存 SIMPLE 配置优先；没有 SIMPLE 配置时读取业务正式审批来源；再没有才解析旧 BPMN 用户任务候选人。审核人必须有至少一个审批对象，批准人可为空；多个对象的或关系与和关系必须在流程节点结构中可执行表达，不能只保存前端展示 JSON。
- Blocker: “新建模型”或“修改”仍进入旧模型编辑页、新建时不能手工输入流程名字、流程标识暴露给业务用户填写、审核人为空仍可保存、批准人空值被误判必填、当前已有审批对象却显示空白默认用户下拉、多对象关系只改标签不改节点、用户/权限角色/部门/直属主管任一类型只显示不保存、或保存后查看审批路线退回内部编码时必须停止。
- Verification: 静态合同必须同时锁定新建和修改按钮的新点击处理器、弹窗字段、流程名字输入、四类审批对象、当前配置回显、审核必填、批准可选、或/和关系、创建/更新 `simpleModel`、禁止旧编辑入口继续绑定；真实页面验证至少只读证明“新建模型”打开新建审批模型弹窗且可以手工输入名字、“修改”打开新弹窗并显示当前审批情况，写入验证必须使用已授权测试数据并能清理。
- Forbidden action: 禁止只改按钮文案、toast、前端临时缓存、BPMN 英文节点名或 API-only 成功提示冒充审批人员配置完成；禁止为了通过保存而扩大流程管理员或审批权限。
- Evidence: `doc/tasks/20260830-approval-model-participant-config/verification-report.md`；`doc/tasks/20260830-approval-model-current-display/verification-report.md`；`doc/tasks/20260831-approval-model-create-participant-config/verification-report.md`。

## 前端选择弹框即时反馈门禁

- Trigger: 用户反馈点击弹框选项后没有立刻选择、选中态停留、弹框过一会才消失、初始化期间点击候选列表为空白、多行选择允许重复业务对象，或修改工序/员工/角色/项目/订单等 picker、dialog、dropdown 的选中流程。
- Preflight check: 先区分“打开候选”和“确认选择”两个阶段。候选依赖正式异步请求时，该请求不得被无关目录、说明或装饰数据的串行 `await` 阻塞；弹框必须区分 loading、前置条件未满足、empty、error 和 ready，错误直接使用正式请求状态。多行选择同类业务对象时，必须明确正式业务身份，当前行保留自身选择，其他行已选业务身份从当前候选中排除，并在最终写请求构建前再次校验唯一性；页面显示编号与内部记录 ID 不等价时，禁止只按内部记录 ID 去重。若产品口径是“点选即关闭”，关闭弹框或隐藏候选面板必须发生在耗时异步请求、运行配置加载、员工/上下文切换之前，后续失败再通过正式错误提示暴露。若选择项需要像 PQC 登录人校验一样阻止非法切换，必须保留校验成功后关闭。PQC 待检工单 picker 还必须区分“暂无待执行 PQC 检验任务”和“关键字无匹配”，刷新后若已选工单不在正式待检列表中，应同步清理工单、工序、员工和模板上下文。若用户要求最大化或进入全屏后预加载切换缓存，只能预热当前时点已具备正式查询上下文的安全 GET 数据；未选择订单时可以预热待检工单和订单无关的人员候选，但不得遍历全部待检工单预热依赖 `workOrderId + routeId` 的工序列表；预加载入口和底层 API wrapper 都必须做运行时正式身份校验，TypeScript 必填类型或主选择路径校验不能替代预加载路径的 fail-fast。员工 `switch-employee` POST 只能在用户真实选择后缓存成功结果，不得批量预调用会改变当前上下文、签名、模板或提交状态的 POST。PQC 切换检验员 payload 必须从当前选中的 `pqcTaskOptions` 任务项读取 `regulationVersionId/qaProcessId/pqcTaskId`，与正式提交 payload 使用同一任务身份；一线 PQC 切换检验项目、检验类型或轮次若会清空旧人员上下文，必须立即按当前登录账号重新执行 PQC 人员/任务切换，切换期间禁用提交；禁止从父级工序卡片读取 QA 身份来拼接切换请求。
- Blocker: `openPicker` 只切换可见状态但正式候选请求尚未启动、候选仍在加载却渲染无说明空白列表、下游候选在上游对象未选择时没有前置状态、多行候选仍暴露其他行已选业务对象、提交前没有唯一性硬校验、点击后 option 已 active 但弹框仍等待接口或上下文切换、PQC 任务已切换但当前登录人员 switch 未按新任务身份重跑、PQC 待检列表为空却显示搜索无匹配或保留旧 selected active order、未选择上游订单时批量预热各订单工序并让任一无效订单阻断入口、最大化预热调用上下文 POST、预加载或 API wrapper 在缺少 `activeOrderId` 等正式身份时仍发送运行配置请求、吞掉预热 GET 失败、静态合同只断言最终关闭不检查请求/关闭顺序、或为了即时关闭而跳过必须的非法选择校验时必须停止。
- Verification: 聚焦静态合同同时锁定正式候选请求不受无关请求串行阻塞、picker 状态覆盖 loading/prerequisite/empty/error、正式候选数组仍驱动 option；多行选择还必须锁定逐行候选排重、当前行自身选择保留、按正式业务身份而非展示临时索引去重，以及最终写请求前的重复校验；确认选择场景还要断言即时关闭的 `closePicker()` / hide 位于目标 `await` 之前，并证明校验型场景仍在成功校验后关闭；PQC 待检工单空态需覆盖无待检文案、搜索无匹配文案、旧选择清理和旧活跃订单文案负向断言；PQC 任务按钮还必须锁定任务切换后重跑当前登录人员 switch、切换期间提交禁用和正式任务诊断共用；最大化预加载场景还要断言 `requestFullscreen()` 先于预热，预热只覆盖当前时点上下文完整的正式 GET 缓存，依赖订单选择的工序请求只能在选单后调用，且不调用上下文 POST，并锁定预加载入口和底层 API wrapper 在缺正式身份时先 fail-fast；真实 E2E 计数必须把同一工序同一员工的重复选择 POST 与切换到新工序后的首次员工上下文 POST 分开记录，后者不能误判为重复选择缓存失败。若上游工序选择会异步加载 `runtime-config` 并自动切换默认员工，Playwright 必须等待这两个正式响应完成后再打开员工 picker；选择目标员工后还必须等待该员工自己的 `switch-employee` 响应，不能以固定延迟、卡片文本或下游提交按钮状态代替请求完成信号；再运行相邻 picker/页签合同、`pnpm ts:check` 和 Vite 目标模块转换。
- Forbidden action: 禁止用固定延迟、loading 遮罩、延迟 toast、永久禁用按钮、mock/default 候选、吞掉异步错误、仅靠选项隐藏而省略提交校验、用内部行号或可重复记录 ID 冒充正式业务身份、前端私自过滤替代后端待检读模型、保留失效选择、把无副作用 GET 等同于可脱离选择上下文批量调用、用上下文 POST 预热模板或员工切换，或把所有模式统一提前关闭来掩盖正式加载和校验差异。
- Evidence: 任务 `doc/tasks/20260806-frontline-production-fullscreen-logic/`，一线生产选择工序时旧逻辑等待 `selectFrontlineProcess` 和默认员工切换后才关闭弹框，导致用户看到选中卡片停留；修复为生产模式点击即 `closePicker()`，一线 PQC 仍保留校验成功后关闭。任务 `doc/tasks/20260807-frontline-picker-initial-loading/`，生产页旧初始化先等待模板目录才请求工序，用户在此期间打开工序/员工 picker 只能看到空数组；修复为模板目录和生产选择上下文并行初始化，并显示正式加载、前置、空和错误状态。任务 `doc/tasks/20260807-frontline-pqc-fullscreen-preload/`，一线 PQC 最大化曾预热全部待检工单的工序列表；任务 `doc/tasks/20260810-pqc-entry-eager-process-preload-fix/` 进一步证明未选订单的正式 QA 上下文错误会因此阻断入口，最终收敛为入口只预热待检工单与订单无关人员，选中订单后再查询该订单工序。任务 `doc/tasks/20260807-frontline-maximize-runtime-cache/`，一线生产最大化后预热所有可切换工序的正式 `runtime-config` GET 缓存，员工切换 POST 只在首次真实选择成功后缓存复用。任务 `doc/tasks/20260808-frontline-employee-picker-immediate-close/`，一线生产员工选择也必须像工序选择一样在 `await switchFrontlineActualEmployee(...)` 前 `closePicker()`，PQC 登录人校验仍保留校验后关闭。任务 `doc/tasks/20260808-frontline-pqc-process-card-autoclose/`，一线PQC工序卡片选择也必须在 `await selectFrontlinePqcProcess(...)` 和默认员工切换前 `closePicker()`，不再要求点击返回。任务 `doc/tasks/20260809-fix-frontline-chenli-submit-system-error/`，真实写入 E2E 以 `runtime-config`、默认员工和目标员工三个正式响应作为交互同步信号，避免即时关闭 picker 与自动默认员工切换造成定位竞争。任务 `doc/tasks/20260819-frontline-device-account-runtime-refresh/`，一线生产最大化预加载和底层运行配置 API 必须在缺 `activeOrderId` 时前端 fail-fast，不能把缺身份请求打到后端设备账号授权门禁。任务 `doc/tasks/20260820-frontline-pqc-decouple-production-submit/`，一线PQC切换检验项目、类型或轮次后必须用当前登录账号重跑人员/任务切换，否则提交前置会提示人员和任务切换未完成。
- Duplicate-selection extension: 多行分配或绑定弹框不能让两个可编辑行选择同一正式业务对象。候选过滤负责即时阻止新重复，最终载荷校验负责拦截历史状态、并发刷新或异常赋值绕过；业务界面显示“订单编号”时，应以正式生产订单身份判重，而不是以可能因版本变化不同的活跃池记录 ID 判重。Evidence: 任务 `doc/tasks/20260813-team-leader-allocation-duplicate-order/`。

- Upstream-driven candidate extension: 若下游候选由上游业务对象决定，初始化必须先确定路由上下文指定对象或正式列表首项，再从该上游对象的正式数据链派生下游候选；当活跃订单已经锁定工艺版本和工序快照时，必须按 `activeOrderId` 请求订单冻结工序，不得只用订单 `routeId` 过滤当前发布路线工序。切换上游对象时必须先清空旧工序、员工、运行配置和模板，并使迟到请求失效；运行配置与员工切换请求也必须携带当前活跃订单身份。禁止根据旧下游选择拒绝用户切换正式上游对象。验证必须用至少两个不同正式身份的可执行状态测试，证明旧订单展示旧版工序、新订单展示新版工序、切换后只保留新身份候选、旧上下文被清空、缺正式映射显性失败且迟到响应令牌失效。Evidence: 任务 `doc/tasks/20260813-frontline-order-driven-process/`、`doc/tasks/20260817-frontline-active-order-frozen-route-submit/`。

- Async-candidate error extension: 上游对象变更触发的候选请求失败后，页面必须保留该正式请求错误；确认提交在候选仍 loading 时应明确阻止并提示等待，候选身份为空时不得先清空请求错误再改写成泛化的“请选择”校验。只有在候选请求成功或用户明确修改上游对象后，才允许清除旧错误。验证应先用静态合同证明“加载错误 -> 点击确认”仍保留原错误且不发送写请求，再覆盖唯一候选自动选择、多候选手动选择和正式空/错误状态。禁止用默认候选、空数组或提交校验文案掩盖上游请求失败。Evidence: 任务 `doc/tasks/20260825-edhr-batch-route-selection/`。

- First-switch snapshot extension: 若首次切换工序或员工短暂加载、再次切换相同目标不再加载，应优先检查最大化 GET 快照是否同时包含该工序全部可选员工的正式模板解析结果，以及前端是否用与真实员工切换完全相同的工序和员工键预填缓存；只预热默认员工或把首次真实切换 POST 当作快照预热均不完整。验证必须断言 runtime-config GET 响应携带全部可选员工正式模板快照、缓存时逐员工预填切换缓存，且最大化预热不批量调用 `switch-employee` POST。Evidence: 任务 `doc/tasks/20260812-frontline-fullscreen-first-switch-prewarm/`。

## 前端 PQC 缓存命中错误状态收敛门禁

- Trigger: 一线 PQC 订单、工序或员工候选接口曾失败并留下页面错误，用户重新选择已缓存的正式候选后页面仍显示“系统异常”或持续 loading。
- Preflight check: 将成功缓存命中视为正式成功状态，检查缓存分支是否同时写入候选数组、结束对应 loading 并清除旧错误；失败请求仍必须走原有显性错误链路。
- Blocker: 成功缓存数据已可用但旧错误仍可见、缓存命中未结束 loading、或为消除旧错误而吞异常、返回空候选、伪造成功时必须停止。
- Verification: 先用聚焦静态合同 RED 证明旧错误残留，再锁定订单和工序缓存命中后的错误/loading 收敛，并运行 PQC 默认样本、全部方法提交和提交后重开回归。
- Forbidden action: 禁止用 fallback、空数组、延迟清屏或关闭全局错误处理掩盖正式请求失败。
- Evidence: 任务 doc/tasks/20260821-frontline-pqc-entry-system-exception/。
## 前端确认提交上下文来源门禁

- Trigger: 确认分配、确认提交、复核、保存等写接口依赖 `leaderType`、角色类型、当前模块、当前页签或其它页面上下文，且页面同时存在 `queryParams`、筛选表单、多页签状态或多布局 props。
- Preflight check: 写接口载荷中的上下文字段必须来自当前页面/页签的正式状态，例如 `activeLeaderTab` 或路由 props；筛选参数只用于列表查询。新增或修改确认按钮时，静态合同必须锁定写接口调用不读取可清空、可重置或可跨页签漂移的筛选态上下文。
- Blocker: 写接口从 `queryParams.leaderType`、筛选表单、可清空条件或列表查询缓存读取必填上下文，导致后端收到缺失字段、`null`、旧页签值或跨角色值时必须停止。
- Verification: 先让聚焦静态合同 RED，证明旧写接口仍使用筛选态上下文；GREEN 后运行目标确认提交合同、相邻工作台合同、`pnpm ts:check` 和 `git diff --check`。
- Forbidden action: 禁止用默认角色、默认成功、后端宽松校验、吞掉参数异常、隐藏 toast 或只改错误文案来掩盖上下文来源错误。
- Evidence: 任务 `doc/tasks/20260808-team-leader-allocation-null-confirm/`，生产组长确认分配从筛选态 `queryParams.leaderType` 改为当前页签 `activeLeaderTab`，手工分配行不再预填潜在无效活跃订单。

## 复合输入控件交互保留门禁

- Trigger: 修改 `el-select`、`el-autocomplete`、远程搜索下拉或同类复合输入控件时，为其增加复制、上次选择恢复、只读回显、后缀按钮、标题栏紧凑布局、空点击加载候选或 `automatic-dropdown`。
- Preflight check: 先确认原控件承担的正式交互职责，例如下拉选择、远程搜索、清空、候选 `label/value`、正式加载方法和可复制展示；若空点击加载是正式交互，前端空关键字请求与后端参数绑定必须同源建模，后端查询参数应显式允许缺省或空值并进入正式候选查询逻辑；专用静态契约必须同时锁定原组件标签、关键交互属性、正式候选渲染、新增复制/回显标识和空关键字契约。
- Blocker: 控件被替换为纯 `el-input` 或文本、复制按钮遮挡点击、远程搜索方法或 `el-option` 候选丢失、下拉箭头不可见、无法改变当前选择、空下拉请求缺少 `keyword` 时后端参数绑定失败、或合同只断言可见文案/复制能力而未证明仍可选择时必须停止。
- Verification: 先补 RED 静态契约覆盖“复制不替代选择”和空点击加载契约，GREEN 后运行目标合同、相邻标题栏/页签合同、`git diff --check`；若改动触及类型、接口参数或运行态逻辑，再运行 `pnpm ts:check`、目标后端参数绑定单测或记录无关 blocker。
- Forbidden action: 禁止为了让内容可复制而把正式选择控件改成 disabled/read-only 输入框、隐藏候选下拉、移除远程搜索、只靠前端传空字符串而后端仍把 `keyword` 设为必填、用 API-only 或截图目测替代控件交互验证。
- Evidence: 任务 `doc/tasks/20260806-qa-project-selector-dropdown-copy/`，QA 规程项目代码字段在支持上次选择恢复和复制后，补充 `automatic-dropdown`、`remote-show-suffix` 和 `data-qa-regulation-project-dropdown`，静态契约锁定仍是可搜索下拉 `el-select`；任务 `doc/tasks/20260806-pqc-personnel-permission-candidates/`，PQC 新增人员空下拉加载候选时，后端 `keyword` 必填导致参数绑定异常，修复为 `required=false` 并用控制器单测和静态合同锁定。

### 远程多选搜索必须替换旧候选并以最新输入为准

- Trigger: `el-select` 多选远程搜索按编号或名称查询，输入后仍展示上一次候选、快速输入后列表跳回旧关键词结果，或已选标签与搜索候选共用同一个 options 状态。
- Preflight check: 区分“已选择且需要回显”的选项与“本次关键词查询”的候选。每次有效搜索必须用本次结果替换未选候选，只保留已选项供回显和删除；异步请求必须有递增标识或等价取消机制，只有最新请求可更新候选、加载态和错误提示。
- Blocker: 搜索结果通过合并函数不断累加、旧请求可以在新请求之后覆盖候选、空结果继续显示无关旧候选，或为了清空旧候选而导致已选标签丢失时必须停止。
- Verification: 先补 RED 静态或组件合同，锁定候选替换、已选项保留和最新请求优先；GREEN 后运行目标合同、`pnpm exec vue-tsc --noEmit --pretty false`、`git diff --check`。真实页面可用时，输入一个完整编号并确认仅显示匹配项，再快速切换两个关键词确认列表不回跳。
- Forbidden action: 禁止把搜索失败解释为物料不存在、在无匹配时保留旧列表掩盖空态、用前端静态全量列表代替正式搜索，或让旧请求错误提示覆盖当前输入。

### 已选项回显状态必须与搜索候选状态隔离

- Trigger: 远程搜索下拉已成功返回已选对象详情，但页面标签在刷新候选、切换关键词或重新加载组件后消失。
- Preflight check: 将“已选对象的正式 ID 与详情”作为独立回显状态保存，将“本次搜索候选”作为可替换状态维护；已选标签只从回显状态或其明确的正式详情映射读取，不能依赖会被搜索请求替换的 options 数组。
- Long ID extension: 用户、角色、物料、路线等正式 ID 可能因后端 Long 序列化策略在不同接口中呈现为字符串或数字；用于已选项回显、删除、禁选、预选高亮和提交去重前，必须先统一 ID 类型，再做匹配或 `includes` 判断。
- Blocker: 已选详情请求成功但标签仍为空、搜索结果替换导致已选项丢失、同一个 ID 因字符串/数字严格比较不相等而显示“未识别”，或为了显示标签把历史候选永久累加时必须停止。
- Verification: 静态合同锁定独立已选回显状态、详情加载后的赋值、候选替换不清空回显、ID 类型归一化后再匹配；真实页面可用时，先加载已有选项，再输入新关键词并确认原标签仍可见。
- Forbidden action: 禁止用旧候选缓存、默认物料、名称猜测或空列表掩盖正式详情回显失败；禁止让搜索候选数组同时承担已选标签的唯一数据源。

### 多选选择框外置标签不得隐藏搜索输入控件

- Trigger: `el-select` 多选控件将已选标签移到输入框外单独展示，但搜索框无法继续输入或点击。
- Preflight check: 先核对 Element Plus 多选结构中已选标签与搜索输入共用的内部选择项类名。外置标签时应使用组件 `#tag` 槽位输出空标签内容，并在外部维护真实标签与删除入口；不得按泛化 `selected-item` 类隐藏整个内部选择项，因为该类同时承载输入包装器和占位文本。
- Blocker: CSS 隐藏规则命中 `el-select__input-wrapper`、输入框不可聚焦/不可输入、外置标签删除不能更新正式选择值，或用 `pointer-events: none` 让问题表面消失时必须停止。
- Verification: 静态合同锁定 `#tag` 槽位、外置标签删除和不隐藏输入包装器；运行 `pnpm exec vue-tsc --noEmit --pretty false` 与 `git diff --check`。真实页面可用时，在已有选择的情况下输入一个新的物料号，确认输入值变化并出现新候选。

### Vue 模板普通 HTML 标签不得自闭合

- Trigger: Vite 或 `vite-plugin-eslint` 报 `vue/html-self-closing`，提示普通 `span`、`div` 或同类 HTML 标签不能自闭合。
- Preflight check: 区分 Vue 组件与原生 HTML 标签。原生 HTML 占位元素必须使用开始和结束标签；不要关闭 Vite 错误浮层、放宽 ESLint 规则或改为无语义元素绕过编译错误。
- Verification: 在目标 `.vue` 文件运行 `pnpm exec eslint <file>`，再运行受影响的静态合同和 `pnpm exec vue-tsc --noEmit --pretty false`。

### 名称/编号合并搜索必须复用正式查询条件并显性失败

- Trigger: 单个 `el-autocomplete` 输入框要求用户可输入名称或编号，并且候选项需要显示正式编号、名称、已关联/未关联状态或状态排序。
- Preflight check: 先核对现有后端是否已有正式 `code`、`name` 或其它语义清晰的查询条件。若已有正式条件，前端可以用同一关键词并行调用正式条件后按稳定业务 ID 去重、排序和标记；若任一正式请求失败，必须显示正式错误，不得只展示另一半结果冒充成功。保存链路仍必须使用候选正式 ID 或编码，不得用名称推断身份。
- Verification: 静态合同必须锁定同一关键词覆盖编号与名称查询、按正式 ID 去重、候选显示编号/名称/状态、状态排序和保存载荷身份字段；真实页面验证应输入名称关键字，确认绿色未添加排在前、红色已添加排在后且不产生业务写请求。
- Forbidden action: 禁止按输入字符猜测只查编号或只查名称，禁止一个查询失败后静默降级为另一个查询结果，禁止只改候选文案不显示正式编号，禁止按名称提交或解析产品身份。
- Evidence: 任务 `doc/tasks/20260813-process-route-product-name-status-suggestions/`，工艺路线关联产品输入框复用 MES 物料正式 `code` / `name` 查询并按 `item.id` 去重；真实只读 E2E 证明关键词“泵”下未添加绿色在前、已添加红色在后且无 MES 写请求。

## 多角色共享表格列池隔离门禁

- Trigger: 同一个 Vue 组件或 `UnifiedListTemplate` 表格按角色、页签、业务类型复用，并且存在用户列设置、默认列池、动态列显隐或专属业务列，例如生产组长/PQC 组长共用报工表。
- Preflight check: 先同时检查模板渲染 `v-if`、传给列设置组件的 `columns`、默认列定义、持久化 `tableKey` 和保存/重置处理；角色专属字段必须在列池层隔离，不能只在 `<el-table-column>` 上用 `v-if` 隐藏。若共享表格还支持拖拽列宽，必须同时检查保存宽度是否绑定到当前角色/页签的实际 `el-table-column width`，不得只改共享 hook 或只保留 `min-width`。
- Blocker: 当前角色的显示字段设置仍包含其它角色专属 label/key/marker、不同角色共享同一默认列池导致持久化配置串用、或静态合同只断言 DOM 渲染不覆盖列设置池时必须停止。
- Verification: 聚焦静态合同必须分别抽取各角色默认列池，断言当前角色不包含其它角色专属 key/label，并断言 active column control 按角色选择；涉及列宽持久化时，还要逐列断言已保存宽度绑定 `:width` 且默认 `:min-width` 仍存在；再运行相邻工作台合同和 `pnpm ts:check`。
- Forbidden action: 禁止只用 `activeTab`/`v-if` 隐藏表格列、禁止让列设置继续暴露其它角色字段、禁止复用旧共享列配置 key 掩盖角色字段串用。
- Evidence: 任务 `doc/tasks/20260806-production-reporting-submit-implementation/`，生产组长报工表模板已隐藏 PQC 列但显示字段设置仍来自共享列池，最终拆分 `productionSubmissionDefaultColumns` / `pqcSubmissionDefaultColumns` 与角色 active column control。任务 `doc/tasks/20260812-standard-list-column-width/` 证明共享报工列表保存列宽后，生产报工、报工历史、PQC 管理、PQC 历史必须分别用当前 active column control 绑定 `:width`，否则会保存成功但刷新后仍按默认最小宽度展示。

## Vue Scoped Slot 静态合同门禁

- Trigger: 静态合同用正则断言 Vue SFC 的具名 slot、`UnifiedListTemplate` 的 `#table`、`#actions`、或带作用域变量的模板，例如 `<template #table="{ ... }">`。
- Preflight check: 正则必须允许 slot props、换行和合法属性，例如使用 `<template\s+#table(?:\s*=\s*"[^"]*")?\s*>`，不得只匹配裸 `<template #table>`。
- Blocker: 页面源码已有合法 scoped slot 但静态合同报“缺少 table slot”、或合同只因 slot 作用域变量、CRLF/LF、属性顺序变化失败时，必须先修合同再判断业务行为。
- Verification: 修正后重跑目标静态合同，并确认合同仍断言内部关键锚点，例如 `data-user-table-key`、分页事件、列配置或正式 query 透传。
- Forbidden action: 禁止为通过静态合同删除 slot props、取消模板作用域变量、弱化为只查页面文件名，或把合法 scoped slot 误判成页面能力缺失。
- Evidence: 任务 `doc/tasks/20260805-production-personnel-audit-inline/`，表单日志合同旧正则只匹配裸 `#table`，误判已有 `#table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }"` 缺失。

## 前端截图样式块静态契约门禁

- Trigger: 用户基于截图要求调整局部颜色、选中态、高亮态、状态条、边框、背景或伪元素，尤其同一 SFC 中存在多个相似 `background`、`color`、`&::before`、`:hover`、`.active` 样式块，或父级 `.active` 背景被标题等子元素的 `:hover` 背景局部覆盖。
- Preflight check: 静态契约必须先锁定目标选择器和目标状态块；负向断言要先抽取 `.active`、`:hover`、`:focus-visible` 或对应子块再检查旧样式，不得用过宽 `[\s\S]*` 从目标块跨到后续无关样式。若截图只在文字或图标宽度内出现异色块，还必须核对实际 hover 命中的是完整交互容器还是内部标题子元素，并证明内部子元素不会绘制不透明背景覆盖父级状态。父级通过继承改变选中文字颜色时，还必须扫描内部 label/value 是否显式声明了普通态 `color`；这类子元素会覆盖继承色，必须在目标 `.active` 作用域内分别验证计算色。
- Blocker: 契约无法区分普通态与选中态、命中结果可能跨块包含相邻绿色/黄色/背景/伪元素样式、无法证明旧样式只在目标状态中被移除、只验证父级 active 背景而未排除子元素 hover 背景覆盖、或深色选中背景下子元素仍因显式普通态 `color` 显示深色文字时，必须先修正契约再声明 GREEN。
- Verification: 聚焦静态契约必须同时断言目标正向 token、目标状态块内不存在旧 token、相邻控件契约仍通过；父级 active 与子元素 hover 冲突还必须分别断言完整交互行拥有 hover 状态、标题子元素没有独立背景、active + hover 保持目标背景。选中态依赖文字继承色时，真实页面还必须读取内部 label/value 的计算色并核对背景对比，而不能只读按钮自身 `color`。涉及 Vue/SCSS 文件时再运行 `pnpm ts:check` 和 `git diff --check`。
- Forbidden action: 禁止只凭截图目测改 CSS、禁止用全局覆盖或删除共享伪元素冒充局部状态修复、禁止给文字/图标子元素单独补背景掩盖父级状态冲突、禁止用跨整文件泛正则做旧样式负向断言。
- Evidence: 任务 `doc/tasks/20260805-pqc-redbox-ui-prototype/`，PQC 检验项 tab 根据截图从白底绿字和绿色顶部条改为黄色选中背景；静态契约最终抽取 `.pqc-item-tab.active` 样式块，断言 active `&::before` 被隐藏且目标块内不存在旧绿色条。任务 `doc/tasks/20260807-sidebar-active-hover-background/` 中，侧边栏父级 `.el-menu-item.is-active` 已是统一浅色背景，但 `.v-menu__title:hover` 仍绘制白色背景，必须将 hover 背景归属完整菜单行并分别覆盖主菜单与 popper。任务 `doc/tasks/20260807-frontline-pqc-order-picker-summary/` 中，订单按钮 active 已设白色文字，但内部三行值显式深色覆盖继承；补充 active 子元素颜色合同与真实计算色断言后修复。

## 页面级全局布局元素隐藏门禁

- Trigger: 用户基于截图红框要求“底部区域不显示”“版权不显示”“页脚空白去掉”或同类看似在业务页内部、实际可能来自全局 Layout/Footer/TagsView 的区域调整。
- Preflight check: 先用真实 DOM 或稳定源码锚点确认红框区域归属，区分业务页面容器、列表组件和全局布局组件；若目标是单页隐藏全局元素，应通过路由 meta 或正式布局开关控制渲染，同时同步页面自身高度计算，不得只在业务页内用空白遮挡。
- Blocker: 不能证明红框区域来自哪个组件、隐藏会影响全局其它页面、页面仍预留全局元素高度、或只通过 CSS `display:none` 跨全局选择器压掉所有页面时必须停止。
- Verification: 静态合同必须同时锁定页面级 meta、布局组件渲染条件和目标页面高度不再预留全局元素；真实页面验证需断言目标页 DOM 文案/区域不可见并截图确认列表底部正常。
- Forbidden action: 禁止只改业务表格高度但继续渲染全局 Footer，禁止全局关闭所有页面页脚，禁止用截图裁切、滚动位置或 body 级样式覆盖冒充单页隐藏。
- Evidence: 任务 `doc/tasks/20260828-schedule-order-operation-copy/verification-report.md`，排产工单红框底部区域实际来自全局 Footer；修复通过 `hideFooter` 路由元信息控制 AppView 不渲染 Footer，并同步排产页高度移除 `--app-footer-height` 预留。

## 隐藏原生控件可见替身居中门禁

- Trigger: checkbox、radio 或 file input 通过 `position: absolute`、透明度或裁切隐藏原生控件，同时用 span、图标和文字作为可见替身，并要求替身组合在 flex/grid 容器中居中。
- Preflight check: 先区分参与正常文档流的可见子元素与绝对定位的隐藏原生控件；grid 显式列数只能对应实际参与布局的可见项，不得为已脱离文档流的 input 预留列。静态合同应锁定目标选择器的列定义和中心对齐规则，并负向禁止旧的多余列。
- Blocker: 可见替身组合中心偏移、不同文案长度下偏移量变化、隐藏 input 仍对应一个空 grid track、或只给单个图标 `margin` 做位置补偿时必须停止。
- Verification: 聚焦静态合同先 RED 后 GREEN；真实页面逐项读取容器、可见图标和文字的 `getBoundingClientRect()`，计算可见组合中心与容器中心的水平和垂直偏差。要求精确居中时偏差应不超过 1px，同时复验原生控件状态、aria 语义和点击行为。
- Forbidden action: 禁止把绝对定位 input 当作普通 grid item 计入列数；禁止用硬编码左外边距、按某个固定文案补偿、截图裁切或只看父容器 `justify-content: center` 冒充可见内容已居中。
- Evidence: 任务 `doc/tasks/20260811-center-frontline-device-card-footer/`，设备卡片计量效期区原使用 `18px 18px auto`，隐藏 input 造成空列；收敛为可见勾选框与文字的 `18px auto` 后，四张卡片水平和垂直中心偏差实测均为 0px。

## 固定画布跨行面板布局门禁

- Trigger: 固定画布或全屏操作台需要让左侧底部操作区只占一列，同时让右侧设备、详情或预览面板跨越内容行与操作行；跨行面板新增高度还必须只分配给内部某个弹性内容区。
- Preflight check: 先核对目标元素是否属于同一个 grid 父级；需要跨行的面板和限定列宽的操作区必须成为同一网格的直接布局项，必要时调整 DOM 所有权，不得用外层全宽 footer 再做视觉裁切。外层显式定义列、内容行和操作行，跨行面板使用明确 `grid-row`；内部轨道把固定头部、`minmax(0, 1fr)` 弹性内容区和按内容高度的尾部逐一分开。
- Blocker: 操作区仍位于主网格之外、跨行只能通过 absolute/负 margin/遮挡实现、面板新增高度平均拉伸卡片和尾部、提交按钮进入右列、或窄视口缩放后左右边界交叉时必须停止。
- Verification: 静态合同锁定 DOM 归属、外层两列两行、面板跨行和内部固定/弹性/auto 三段轨道；真实 E2E 至少覆盖常用与较窄桌面视口，读取左右面板、操作区、按钮和内部三段的 `getBoundingClientRect()` 与计算轨道，证明操作区边界与左面板一致、跨行面板底边与操作区底边对齐、内部无重叠且不越出视口。
- Forbidden action: 禁止用截图坐标硬编码 absolute、负 margin、扩大整页高度、隐藏溢出或让 footer 继续全宽后再覆盖右侧；禁止只看 1920 宽截图而不验证较窄桌面缩放边界。
- Evidence: 任务 `doc/tasks/20260811-frontline-submit-device-panel-layout/`，一线生产将提交栏纳入左列第二行，设备面板跨越右侧两行，内部保持 `118px minmax(0, 1fr) auto`；1920x1080 与 1440x900 的设备面板和操作区底边均精确对齐。任务 `doc/tasks/20260819-frontline-pqc-layout-after-submit-event-removal/`，一线 PQC 删除生产提交事件后，将提交栏纳入左列第二行，右侧填写面板跨越内容行与操作行，避免左侧空白浪费和右侧数量/不良说明区拥挤。

## 动态任务选择区内容增长门禁

- Trigger: 工序级任务拆分为项目级、班次级或轮次级后，同一类型下的任务按钮数量增加，选择区与下方填写表单位于固定高度操作台或同一网格中。
- Preflight check: 先用正式任务最大基数核对选择区的网格轨道、父级行高、按钮最小宽度和文字换行；列数不得直接等于任务总数，内容区不得依赖单行固定高度。按钮区增长后必须有明确的换行或滚动策略，并保证下方表单只位于正常文档流中的后续轨道。若 Tab 会先改变过滤上下文再派生当前任务，切换副作用必须比较原始选中任务身份，而不是比较已被新上下文重算后的 computed 当前任务，避免跳过草稿数量、轮次或任务快照刷新。若同一工序的检验方法按 item-scoped PQC 任务拆分，切换检验方法必须保留各任务自己的草稿和逐件值；正式提交必须按当前检验类型、业务日期、班次和轮次聚合当前工序全部检验方法任务，缺任一方法任务或样本数据时先 fail fast。若把合格/不合格等双按钮改为 Element Plus `el-switch`，不得直接用空字符串绑定 active/inactive 字符串模型，需用布尔显示模型显式映射正式值，避免未选择项被控件归一成默认“不合格”。
- Blocker: 按钮文字越过自身边界、选择区 `scrollHeight` 大于可见高度却被 `overflow: hidden` 裁切、下方表单与按钮边界相交、表单拦截按钮点击、或只用单项目样本验证时必须停止。
- Verification: 静态合同负向禁止按任务总数生成固定单行列数，正向锁定自适应列宽、正常换行和溢出策略；筛选型 Tab 还必须锁定切换时使用原始任务 id 判断是否刷新草稿；开关型替换需锁定空值不自动写入、`true/false` 到正式值的显式映射，以及按内容高度的卡片轨道；真实 Playwright 使用多项目、多班次样本逐个点击首行和末行按钮，读取按钮区、每个按钮和表单的 `getBoundingClientRect()`，并确认没有 pointer-event interception。
- Forbidden action: 禁止缩小到不可读字号、隐藏多余任务、用 `z-index` 覆盖命中顺序、扩大浏览器宽度或只验证接口返回来掩盖选择区容量问题。
- Evidence: 任务 `doc/tasks/20260817-repair-active-order-30-pqc-history/verification-report.md`，PQC 同工序首检、巡检按项目拆分后，旧固定单行任务区被下方检验表单遮挡；改为自适应换行和面板内部滚动后，真实页面逐项切换通过。任务 `doc/tasks/20260818-frontline-pqc-method-tabs/verification-report.md`，一线 PQC 检验方法 Tab 先改变检验方法过滤上下文，若用重算后的 `activePqcTaskOption` 比较会跳过草稿刷新，最终改为比较原始 `activePqcTaskOptionId` 并用静态合同锁定。任务 `doc/tasks/20260819-frontline-pqc-piece-switch-layout/`，逐件判断卡从两个按钮改为单个 `el-switch` 时使用布尔显示模型和显式值映射，避免空草稿被开关组件自动写成不合格，并将逐件卡片改为按内容高度布局。任务 `doc/tasks/20260820-frontline-pqc-submit-all-methods/`，同一工序三个检验方法分属 item-scoped PQC 任务时，提交入口改为构建当前工序同轮次全部方法 payload，并用 `frontline-pqc-submit-all-methods-static` 锁定切换不清草稿和全方法提交。

## 统一列表复合工具栏布局门禁

- Trigger: 修改 `UnifiedListTemplate`、快速过滤、批量操作栏、标准列表多维筛选、`TableMultiFilter`、或把新筛选控件接入已有业务列表。
- Preflight check: 先在真实业务列表确认快速过滤、操作栏、额外筛选和新增筛选控件的 flex/grid 关系；可折行控件必须有明确行宽、`min-width` 和静态合同覆盖，不得只在空模板或单控件示例中验证。单行工具栏增加操作按钮时，还必须核对容器的实际可用宽度、`scrollWidth` 和各 grid track；共享模板的筛选列若带固有最小宽度，页面必须用同元素选择器将主列约束为 `minmax(0, 1fr)`，并在筛选子项上明确 `min-width: 0`，不能只给外层再套一个无效覆盖。若用户要求列表表头、底部横向滚动条或分页留在同一页面内，目标 `UnifiedListTemplate` 必须有页面级有界高度和 `overflow: hidden`，表格外壳必须是 `flex: 1 1 auto` 且 `min-height: 0`，`el-table` 必须使用 `height="100%"` 和必要的 `scrollbar-always-on`，只允许表格 body 纵向滚动；同页其它列表必须用独立类名负向隔离。标准列表多维筛选要优先做成可增删条件 Tab 这类通用条件集合，不要靠页面级 `maxInlineFilters`、固定字段横铺或业务页特例控制可见条件；标准列表条件 Tab 默认必须为空，不得通过页面级 `.setCondition(...)` 或 query 初值预置隐藏业务筛选。若正式后端接口存在必填查询条件且首屏业务要求有默认值，例如当天提交日期，必须先区分产品口径：用户要求默认筛选可见时，默认条件必须在多维筛选中可见、可审计且有稳定 condition id；用户明确“默认没有过滤”且指结果范围也不受该字段限制时，正式请求必须省略对应参数，后端必填契约必须同步改为可选，禁止用隐藏内部参数伪装成无筛选。只有产品明确允许“可见条件为空但仍有明示的内置业务范围”时，才可保留内部参数，并且页面不能用“暂无筛选条件”让用户误解为未过滤。筛选条件采用“编辑后点击查询”模式时，必须显式区分正在编辑的草稿条件与最后一次已执行条件；条件 Tab、结果摘要或其它会被用户理解为当前结果口径的标签不得在查询尚未执行时先显示草稿值，除非同时提供明确的“待应用”状态。同一页面内多个页签或子列表即使都使用 `UnifiedListTemplate`，也必须逐个显式核对是否接入 `showMultiFilter`、多维 definitions/state/events；模板能力不会自动替换仍绑定旧 quick filter 的列表。
- Blocker: 新控件在真实页面中被快速过滤或操作栏挤压到 `0` 宽、被裁切、不可见、不可点击，工具栏 `scrollWidth` 大于可见宽度，列表长数据导致表头滚走、底部横向滚动条或分页落到视口外，静态合同只断言组件存在但不断言布局宽度和正式 query 透传，或同一个正式 query 参数可被多个条件 Tab 覆盖时必须停止。筛选标签已经显示新条件、列表却仍是上一次请求结果且页面没有“待应用”提示时，必须按筛选状态一致性缺陷处理。标准列表首屏请求仍带页面隐藏默认条件、目标页面还有其它标准列表页签仍保留旧 quick filter、重复状态开关、重复重置按钮或缺少多维筛选事件时，也不得宣称标准模板复用完成。
- Verification: 聚焦静态合同必须覆盖模板布局类、可收缩 grid track、关键子项 `min-width: 0`、props/events 透传、条件 Tab 增删、默认空条件、禁止 `.setCondition(...)` 预置、稳定 condition id、重复正式参数校验、草稿/已执行状态表达和正式请求参数；固定表头/尾部横向滚动条场景还必须锁定页面级有界高度、表格外壳 `min-height: 0`、`el-table height="100%"`、`scrollbar-always-on`、body 纵向滚动和同页其它列表负向隔离。若存在默认查询参数，必须额外断言它按产品口径处理：默认可见场景验证 UI 条件、重置恢复和请求参数一致；“无筛选即不限制结果范围”场景必须验证 `conditions/appliedConditions` 为空、字段下拉不默认选中、正式请求省略该参数、后端缺省参数查询不限范围；只有明示内置业务范围的场景才验证内部参数存在。真实 E2E 必须打开目标业务页面，在常用桌面窄视口（至少 `1280x720`）读取工具栏 `width/scrollWidth` 和关键控件边界，断言所有操作按钮完整可见且没有水平溢出；固定表头/尾部场景还要读取列表根节点、表头、body、横向滚动条和分页 `getBoundingClientRect()`，滚动 body 后确认表头不移动、横向滚动条和分页仍在视口内；同时验证首屏请求不携带未经声明的隐藏筛选、多个已填写 Tab 按交集提交、请求不携带临时参数、重置清空正式条件且目标写请求为 0。对于显式查询模式，还必须分别核对“修改条件但未查询”和“点击查询后”的可见标签、请求计数、正式参数与结果口径，禁止只断言标签或只断言请求。涉及同页多列表时，E2E 必须切换每个目标页签并分别断言旧 quick filter 可见数为 0、正式参数提交和重置清参。
- Forbidden action: 禁止用 API-only、临时测试页、隐藏旧快速筛选、移除业务操作按钮、硬编码当前页面宽度、页面级 inline filter 数量特例或前端本地过滤来冒充标准列表多维筛选完成。
- Evidence: 任务 `doc/tasks/20260804-standard-list-multi-filter/verification-report.md`，排产工单真实 E2E 暴露多维筛选在复合工具栏中被挤压为 `0` 宽，最终用模板级全行布局和静态合同锁定；后续用户反馈固定条件栏复用性差，改为条件 Tab + 加减号，并用真实 E2E 证明多个 Tab 按正式 query 参数交集提交；同步工单页签虽同样使用 `UnifiedListTemplate`，但因未显式接入多维 definitions/state/events 而保持旧 quick filter，最终按页签补齐静态合同和真实 E2E。任务 `doc/tasks/20260805-standard-list-empty-tabs/verification-report.md` 将当前系统 84 个标准列表模板扫描入清单，并锁定默认空条件 Tab、禁止页面级预置隐藏筛选、排产工单和同步工单首屏只带分页参数；任务 `doc/tasks/20260805-qa-regulation-publish-fix/verification-report.md` 新增 QA 规程 4 个标准列表后，将系统接入点更新为 88 个、显式隐藏筛选列表更新为 14 个。任务 `doc/tasks/20260808-team-leader-report-filter-empty-default/verification-report.md` 证明“可见条件为空但存在业务默认范围”的旧口径；任务 `doc/tasks/20260809-pqc-management-remove-hidden-date-filter/verification-report.md` 进一步区分“无筛选即不限制结果范围”，PQC 管理必须同时省略前端 `submitDate`、放宽后端日期契约，并用真实页面证明默认 82 条历史与显式日期 5 条。任务 `doc/tasks/20260809-batch-record-tab-test-all/verification-report.md` 在单行工具栏新增批量操作后，通过 `1280x720` 真实截图和计算样式发现筛选列固有最小宽度导致按钮裁切，最终用 `minmax(0, 1fr)`、子项 `min-width: 0` 以及 `width=scrollWidth` 证据锁定可收缩布局。任务 `doc/tasks/20260821-dcc-product-catalog-toolbar-single-row/verification-report.md` 证明 DCC 产品目录未启用 `single-line-toolbar` 时会让筛选条件栏独占整行、操作按钮换到下一行；修复需同时启用单行工具栏，并用页面级 `minmax(0, 1fr)` 与筛选子项 `min-width: 0` 锁定可收缩布局。任务 `doc/tasks/20260902-dcc-product-catalog-tree/verification-report.md` 证明三列业务层级不适合直接使用 Element 默认树表缩进；若用户要保留逐行展开，可由页面维护 `visibleRows` 做紧凑分组表格树；若用户要更清爽的浏览体验，应改为左树右表，左侧树只负责产品类别 I、产品类别 II、产品的选择，右侧表格只展示选中节点范围内的正式明细。产品目录新增来自其它正式表的派生展示列时，必须从产品目录行的稳定业务键精确关联权威来源，例如批记录识别 JSON 的来源是已启用 `dcc_project_code.project_code`，不是 `dcc_product_catalog` 自身列；无匹配时显示空态，不得按产品名称或当前页面选择推断。任务 `doc/tasks/20260830-registration-certificate-upload-flow-verification/verification-report.md` 证明注册证当前列表需要在同一视口内固定表头、底部横向滚动条和分页，最终用有界 `UnifiedListTemplate`、`el-table height="100%"`、`scrollbar-always-on`、中间 body 滚动区和真实 Playwright 尺寸断言闭环；相邻 `pro-feedback-fixed-middle-scroll-static` 的导入表格负向断言必须截到导入 `ContentWrap` 结束，不能扫到脚本和样式区造成误报。

## Vue Composable 模板顶层绑定门禁

- Trigger: Vue SFC 新增 composable/hook 包装对象后，模板直接绑定 `hook.state`、`hook.updateState`、`hook.removeCondition` 等成员，且开发态出现 `Cannot read properties of undefined`、HMR 后 render 崩溃或完整刷新后恢复。
- Preflight check: 先读取 Vite 当前编译模块，确认完整 setup 是否已创建并返回 hook 包装对象；若编译产物正确但错误发生在父组件 render，检查新 render 是否可能运行在仍持有旧 setup state 的热更新实例上。模板需要的 state 和事件方法应从 hook 返回值解构为顶层 setup binding，再直接传给子组件。
- Blocker: 模板仍在 render 阶段解引用新加入的 hook 包装对象、回归合同只验证 hook 返回值而不覆盖模板绑定、或准备用可选链/默认空包装对象隐藏 setup 不同步时必须停止。
- Verification: 聚焦静态合同必须断言模板绑定顶层 state/events 并禁止目标区域出现 `hook.state/updateState/removeCondition`；同时读取 Vite 编译模块确认 `$setup.<topLevelBinding>` 存在且不再出现 `$setup.<hook>.state`，再运行相邻组件合同与 `pnpm ts:check`。
- Forbidden action: 禁止用 `hook?.state`、`hook || {}`、空 state、强制整页刷新提示或吞掉 render 异常替代正式顶层绑定。
- Evidence: 任务 `doc/tasks/20260805-teamleader-multifilter-state-crash/verification-report.md`，班组长工作台多维筛选在热更新窗口直接读取 `submissionMultiFilter.state` 导致父组件 render 崩溃。

## 前端 Java 时间响应契约门禁

- Trigger: 前端 API wrapper、静态合同或页面报 `DCC response field has invalid type`、`cleanupTime`、`expireTime`、`serverSubmitTime.replace is not a function`、`businessDate.localeCompare is not a function`，后端响应 VO 使用 `LocalDateTime`/`LocalDate`，或涉及 `TimestampLocalDateTimeSerializer`/`LocalDateSerializer`。
- Preflight check: 先核对后端 Jackson/JsonUtils 的 Java 时间序列化口径；当前项目 `LocalDateTime` 可能序列化为 epoch millis 数字，`LocalDate` 可能序列化为 `[year, month, day]` 三元数组。前端原始响应类型、parser 和 formatter 必须声明并校验真实类型，不得凭字段名假定字符串日期。若真实接口合同明确存在字符串和当前序列化形态，必须以显式响应联合类型和专用投影正式归一化；页面与排序/展示/提交调用只读取归一化后的单一类型，不得直接调用 `.replace()` 或 `.localeCompare()`。
- Blocker: 前端仍用 `readOptionalString`、`string` 类型、字符串格式断言或直接字符串方法接收后端 Java 时间值，日期数组长度/整数/日历合法性未校验，或为了通过页面临时做空值吞错、默认当前时间、未知类型静默成功时必须停止。
- Verification: 新增或更新聚焦静态合同，同时断言后端源字段类型和全局 serializer、前端原始响应类型、显式 decoder/formatter、归一化后的页面类型、渲染与排序调用，以及旧 string-only 调用不再直接用于原始字段；`LocalDate` 至少覆盖三元数组、合法 ISO 字符串和不存在的日历日期；涉及引用方时再运行 `pnpm ts:check`。
- Forbidden action: 禁止把后端全局序列化器返回的数字时间戳改成前端局部字符串兜底；禁止为掩盖合同不一致添加 fallback coercion 或吞异常。
- Evidence: 任务 `doc/tasks/20260803-dcc-cleanup-time-response-type/`，`cleanupTime`/`expireTime` 由 `LocalDateTime` 经全局 serializer 输出数字时间戳，前端旧 string parser 触发 `DCC response field has invalid type: cleanupTime`；任务 `doc/tasks/20260807-frontline-pqc-formal-submit-write-e2e/`，PQC 正式提交候选/回执时间直接 `.replace()` 数字时间戳导致真实页面崩溃，修正为正式 formatter 和静态合同覆盖；任务 `doc/tasks/20260817-frontline-pqc-business-date-sort-fix/verification-report.md`，PQC `LocalDate` 以三元数组返回，旧比较器直接 `.localeCompare()` 导致页面崩溃，修正为原始响应联合类型和严格日期投影。

## 业务运行记录用户可读展示门禁

- Trigger: 新增或验收自动同步、定时任务、导入导出、审批执行等运行记录表格，或面向业务人员的修改/补正弹窗；页面出现数字状态、`AUTO/MANUAL`、epoch millis、英文内部字段名、内部 ID、原始 payload/差异/签名快照 JSON 或其它后端存储/传输值直出。
- Preflight check: 先核对后端正式状态枚举、触发类型枚举、时间序列化口径和业务修改命令；前端必须通过明确映射展示中文业务状态和触发来源，通过项目统一时间工具展示日期时间，并将内部字段名转换为用户可读列名。修改/补正弹窗只能收集用户能理解并负责的业务字段、原因和本人签名凭据，payload、字段差异、审计身份、签名 ID 与签名快照必须由服务端生成。当存在内部增量位置、游标或类似技术字段时，主表的最近执行类时间必须来自正式运行记录的开始或完成时间，不能把内部位置时间显示给用户。聚焦静态合同必须锁定用户可见字段和内部字段负向断言，真实 E2E 必须使用非空记录验证最终可见文本与交互。
- Blocker: 页面仍显示 `10/20/30`、`AUTO/MANUAL`、13 位毫秒时间戳、`failureMessage`、用户/签名 ID、payload/差异/签名快照 JSON，要求用户手工拼装内部协议，或静态合同只断言表格/字段存在但未验证可读展示时必须停止。
- Verification: 运行聚焦静态合同和 `pnpm ts:check`；真实页面需断言中文状态、中文触发来源、可读日期时间、用户可理解的业务字段、原始内部值/JSON 不可见、响应式布局无越界/遮挡且控制台错误为空。修改场景还必须验证无实际变化不发请求，写入身份和差异生成由后端测试覆盖。
- Forbidden action: 禁止用空运行记录、隐藏列、mock 数据、API-only、默认成功文案或把未知枚举映射成成功来绕过真实展示验证。
- Evidence: `doc/tasks/20260805-profile-erp-table-auto-sync/verification-report.md`，ERP 自动同步页面真实数据曾直出状态 `20`、触发类型 `AUTO`、毫秒时间戳和 `failureMessage`，补充 RED/GREEN 后由真实页面复验中文可读展示。

## ERP 表格同步 Job 链路门禁

- Trigger: 个人工作台、ERP 同步监控、生产工单、物料、库存、采购、销售、BOM 或生产用料清单等页面新增或调整 ERP 表格同步、自动同步、立即执行一次、每日同步配置。
- Preflight check: 先核对现有正式增量同步链路是否已通过 `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)` 和 `infra/job` 调度任务承载；配置类页面必须按 handlerName 查询正式 Job，使用 `JobApi.updateJob` 更新 cron，使用 `JobApi.updateJobStatus` 启停任务。后台只允许开启状态的 Job 修改 cron，因此从暂停改为启用时必须先 `updateJobStatus(NORMAL)`，再 `updateJob`；保持暂停的 Job 不得强行改 cron，避免为了修改配置而触发不该运行的同步。
- Blocker: 前端仍调用 `/erp/kingdee-table-auto-sync/**`、全量同步旧接口、禁用 ERP 模块 Controller、mock 成功、默认成功状态、缺少任一正式 handlerName 对应 Job，或从暂停状态启用时先调用 `JobApi.updateJob` 导致 `JOB_UPDATE_ONLY_NORMAL_STATUS` / `1001001004` 后中断启用时必须停止。
- Verification: 聚焦静态合同必须断言组件导入 `@/api/erp/sync`、`@/api/infra/job`、`InfraJobStatusEnum`，覆盖 `JobApi.getJobPage/updateJob/updateJobStatus`、`ErpKingdeeSyncApi.runIncrementalSyncJob`，并禁止 `kingdee-table-auto-sync`。保存配置回归必须覆盖“暂停任务被选中后可启用并更新 cron”和“未选中的暂停任务不会因为保存时间而被临时启用”。
- Forbidden action: 禁止通过开启禁用模块、复制旧自动同步 Controller、前端吞错、API-only 成功提示或硬编码假 Job ID 来冒充生产工单同款同步方式。
- Evidence: 任务 `doc/tasks/20260806-profile-erp-table-sync-use-job-api/verification-report.md`，Profile ERP 表格自动同步旧实现误走 `/erp/kingdee-table-auto-sync/**`，页面显示 ERP 模块禁用，最终改为复用正式 Job 增量同步链路并通过 RED/GREEN 验证；任务 `doc/tasks/20260831-erp-kingdee-auto-sync-daily-missed/verification-report.md`，测试服暂停 Job 从页面保存时先改 cron 触发 `1001001004`，需按状态先后顺序处理。

## Vue SFC 泛型箭头函数解析门禁

- Trigger: Vite 或 `vite-plugin-eslint` 在 `.vue` 文件中报 `Parsing error: Unexpected token. Did you mean {'>'} or &gt;?`，且报错行是 `<script setup lang="ts">` 内的 `<T>`、`<K, V>` 等泛型箭头函数。
- Preflight check: 先定位报错行是否是 `const fn = <T>(...) =>` 这类 SFC 易歧义写法；`vue-tsc` 通过不能证明 Vite/ESLint parser 可接受该语法，涉及 `.vue` 新增泛型 helper 时必须同时运行目标 SFC ESLint 或真实 Vite 模块转换。修复前新增或更新最小静态契约，让旧写法先 RED。
- Blocker: 直接关闭 Vite overlay、禁用 ESLint、移除 TypeScript 类型、改成 `any`、或只改测试不改源文件时，必须停止。
- Verification: 聚焦静态契约必须证明目标 SFC 不再使用歧义泛型箭头写法，并优先改为 `function fn<T>(...) {}`；再运行目标 SFC ESLint、`pnpm ts:check` 和真实 Vite 页面或模块转换，三者均通过后才可收口。
- Forbidden action: 禁止用配置降级、parser 替换、忽略规则或隐藏页面来绕过源代码解析错误。
- Evidence: 任务 `doc/tasks/20260803-dcc-controlled-file-detail-vue-parse/`，`getPagedDetailRows` 的 `const ... = <T>(...) =>` 触发 Vite/ESLint 解析错误，改为命名泛型函数并用静态契约 RED/GREEN 验证；任务 `doc/tasks/20260831-frontline-process-report-material-mvp/verification-report.md` 再次证明 `pnpm ts:check` 可通过但真实 Vite 页面仍因同类泛型箭头函数显示 overlay，改为命名泛型函数后 ESLint、类型检查和真实页面同时通过。

## Vue SFC 区块边界编译门禁

- Trigger: Vite/PostCSS 在 `.vue?vue&type=style` 报 `Unknown word`，错误 frame 指向 `const`、赋值、可选链或其它 TypeScript/JavaScript 语句。
- Preflight check: 先核对 `<script setup>`、`</script>`、`<style>`、`</style>` 的真实边界，并确认报错函数是否误落入 style block；同时对目标文件运行锚定 Git 冲突标记扫描。回归测试必须提取真实 style block 并交给项目现有 PostCSS 解析器，不能只搜索报错文本。
- Blocker: style block 仍包含脚本声明、赋值或模板逻辑，或者测试未实际解析 CSS 时必须停止；不得关闭 HMR overlay、放宽 PostCSS 配置或删除业务函数来绕过区块归属错误。
- Verification: 先取得 PostCSS 对同一 style 行号和 `Unknown word` 的 RED，再将完整逻辑原样移动回 script block；GREEN 后运行目标 CSS 编译合同、相邻业务合同、`pnpm ts:check` 和 `git diff --check`。
- Forbidden action: 禁止用正则删除疑似非法行、把函数改写成 CSS 注释、关闭 overlay、忽略构建错误或引入解析 fallback。
- Evidence: 任务 `doc/tasks/20260807-team-leader-workbench-vue-style-compile-fix/`，班组长工作台的两个异常上报函数误置于 scoped style，PostCSS 在赋值语句处报 `Unknown word`；函数原样移回 script 后定向样式编译合同与相邻合同通过。

## 前端 BPMN marker 高亮完整性门禁

- Trigger: BPMN/BPM 流程图、审批流程图、`canvas.addMarker`、`canvas.removeMarker`、`elementRegistry.get`、`Cannot read properties of undefined (reading 'markers')`、节点高亮、节点缺失。
- Preflight check: 对后端或流程实例返回的每个高亮节点 ID，必须先通过 `elementRegistry.get(id)` 确认当前 BPMN XML 中存在该元素，再调用 `canvas.addMarker/removeMarker`；后端 BPMN 模型视图响应也必须先按当前 `BpmnModel` 过滤不存在的任务节点和连线 ID，避免把历史残留 ID 当成正式高亮目标；缺失节点必须聚合成页面可见警告或明确错误归属。
- Blocker: 任一 marker 操作直接对未经校验的 ID 调用、缺失节点被静默忽略、页面只在控制台报错但用户不可见、或静态契约无法证明缺失节点不会触发 `markers` pageerror 时，不得宣称流程图稳定性修复完成。
- Verification: 聚焦静态契约必须断言安全 marker helper、`elementRegistry.get` 校验、可见 warning `data-testid`、无直接未校验 `canvas.addMarker/removeMarker`；涉及后端 BPMN 模型视图时，JUnit 必须构造“历史活动存在但当前 BPMN XML 缺失”的任务节点和连线 ID，并断言响应集合已过滤；涉及 Vue/TS 时运行 `pnpm ts:check`。
- Forbidden action: 禁止用 try/catch 吞掉 `markers` 异常、禁止隐藏流程图或禁用全部高亮冒充修复、禁止把 BPMN XML 与审批任务节点不一致解释为前端无责任而不提示用户。
- Evidence: 任务 `doc/tasks/20260802-dcc-revision-ux-final-fixes/`，DCC 升版发布审批页 BPM 流程图 marker 节点缺失需显示“流程图高亮不完整”并避免 pageerror；任务 `doc/tasks/20260804-bpm-process-instance-detail-errors/`，BPM 模型视图后端过滤当前 BPMN XML 不存在的历史任务和连线 marker ID。

## 前端 BPM 审批时间轴当前节点高亮门禁

- Trigger: BPM/Flowable 审批详情、`ProcessInstanceTimeline`、`ApprovalNodeInfo.status`、当前节点、进行中节点、待审批节点、审批通过中节点、时间轴圆点颜色、头像状态徽标、节点标题颜色。
- Preflight check: 先定位真实时间轴组件里主圆点、节点标题、时间轴状态色和头像小徽标是否各自独立渲染；若用户要求“当前节点/进行中节点用绿色显示”，必须按正式 `WAIT`、`RUNNING`、`APPROVING` 状态集合建模，不能只改截图中一个 CSS 类或一个小徽标。
- Blocker: 主圆点仍是固定蓝色、`RUNNING` 仍映射蓝色、节点标题未区分当前态、测试只能证明文案存在但不能证明当前态颜色、或改动会影响审批动作/权限/API 时必须停止。
- Verification: 聚焦静态契约必须同时断言当前态状态集合、主圆点状态感知颜色、节点标题绿色、`RUNNING` 状态徽标/时间轴色不再是蓝色，并运行相邻审批详情契约和 `pnpm ts:check`。
- Forbidden action: 禁止用全局 Element Plus 主题色、CSS 缩放/覆盖、只隐藏蓝色图标、API-only 断言或把全部节点统一标绿来冒充当前节点高亮。
- Evidence: 任务 `doc/tasks/20260804-current-node-green-highlight/`，BPM/DCC 审批详情旧时间轴主圆点固定 `bg-#3f73f7` 且 `RUNNING` 为 `#448ef7`，最终用静态契约覆盖当前态集合、主圆点、标题和徽标颜色。

## 前端服务端分页排序链路门禁

- Trigger: Element Plus 表格、统一列表模板、分页列表、`sort-change`、`sortColumnAttrs`、表头排序按钮、空单元格排序、展示计算列、跨页排序。
- Preflight check: 先区分本地全量列表和服务端分页列表；服务端分页列表的表头排序必须从表格事件进入统一排序状态，再映射成正式分页请求参数，并由后端白名单字段排序，不能只改当前页数组顺序。若用户要求空单元格在某一方向固定置顶或置底，后端排序必须显式增加空值标记表达式，不能依赖数据库默认 `NULL`/空字符串排序；表达式中的常量也必须写成 SQL 表达式，不能在 `ORDER BY` 中输出裸数字，避免 MySQL 把 `ORDER BY 0` 解析成无效结果列序号。对分页结果之后才计算出的展示状态、提醒状态、权限状态或聚合状态，必须先确认是否存在正式后端排序字段；没有正式字段时应明确保持不可排序，并用静态契约断言不会误接入 `sortColumnAttrs`。若后端已补齐正式排序或筛选字段，前端筛选项必须使用与后端一致的显式枚举；后端 page/count 查询必须复用同一固定表达式，不支持该派生状态的相邻列表应 fail fast 返回参数错误，不能静默忽略。
- Blocker: 表头有排序按钮但未绑定 `sortState`，`sort-change` 只更新组件内部状态，分页请求缺少 `sortField/sortOrder`，后端 Mapper 固定排序忽略请求字段，降序空单元格未被显式排到最后，空值只能在当前页集中，`ORDER BY` 空值分组表达式输出裸数字常量，或用有效期、状态码等近似字段替代提醒状态/派生状态排序时，不得宣称排序修复完成。
- Verification: 聚焦静态契约必须同时断言前端排序状态绑定、请求参数映射、后端请求 VO 字段、Mapper 白名单排序、空值置顶/置底表达式、稳定兜底排序、禁止 `ORDER BY 0` 裸数字常量，以及展示计算列不可排序；派生状态筛选还必须断言筛选参数、服务层枚举校验、page/count 同表达式和相邻不支持列表的失败行为；再运行相邻列表契约、`pnpm ts:check` 和目标后端分页测试。
- Forbidden action: 禁止用前端当前页 `Array.sort` 冒充跨页排序；禁止把任意前端字段直接拼 SQL；禁止用 `.last()` 拼接受用户控制的排序 SQL；禁止依赖数据库默认空值顺序满足用户指定语义；禁止只看表头箭头状态不看接口排序参数；禁止用其它近似字段冒充分页后计算状态的正式排序。
- Evidence: 任务 `doc/tasks/20260730-dcc-product-catalog-null-sort/`，DCC 产品目录“项目名称/项目代码”旧实现只触发统一列表内部排序状态，后端仍按 `dataSource/originalRowNo` 固定排序，最终补齐 `sortField/sortOrder` 与 Mapper 白名单排序；任务 `doc/tasks/20260830-registration-certificate-list-sort/`，注册证列表新增正式排序链路时将分页后计算的“提醒状态”显式保持不可排序，避免用有效期等近似字段冒充业务状态排序；任务 `doc/tasks/20260830-registration-certificate-reminder-sort-filter/` 在后端正式补齐 `sortField=reminder` 和 `reminderState` 后，再开放提醒状态排序与筛选，并锁定非法参数 fail fast；任务 `doc/tasks/20260830-registration-certificate-reminder-sort-filter-regression/`，提醒状态倒序曾因 `ORDER BY 0` 被 MySQL 解析为无效列序号而返回系统异常，修复为 SQL 表达式常量并补真实页面正序/倒序回归。

## 审批中心路由筛选可见性门禁

- Trigger: 审批中心待办/已办列表、`/approval-center/todo`、`/approval-center?moduleCode=...`、`keyword`、快速筛选控件、页面控件显示无筛选但列表为空。
- Preflight check: 列表请求使用 route query、缓存状态或快速筛选状态时，必须把生效的 `moduleCode`、`keyword` 等条件同步到用户可见控件；模块加载失败必须保留错误并抛出，不能被后续列表请求覆盖成有效 0。
- Blocker: URL/query 中的过滤条件仍会影响请求但页面控件为空、清空筛选未同步 route、模块列表接口异常后页面显示“0 个模块”、或静态合同只能证明接口参数存在但不能证明筛选可见时必须停止。
- Verification: 聚焦静态合同覆盖 route filter -> quick filter 可见状态，再复跑审批中心分页保页、列表区域和分页 payload 相邻合同；涉及请求/错误链路时同步复跑目标后端 JUnit。
- Forbidden action: 禁止在前端默认清空 query、吞掉模块错误、只隐藏空态、只改 badge 或用 API-only 证明列表正常。
- Evidence: `doc/tasks/20260804-approval-center-todo-empty-list/verification-report.md`。

## 前端截图字号调整静态契约门禁

- Trigger: 用户基于截图要求调整卡片、表格、弹窗或页面局部文字大小，尤其出现“文字大小”“字号”“放大 2 倍”“缩小一半”“卡片内文字”等表述。
- Preflight check: 先定位目标区域已有 SFC/CSS 选择器和相邻静态契约；若已有契约锁定字号或密度，必须先按用户口径更新该契约并跑出 RED，再改最小 CSS。若没有契约，新增任务专用静态契约断言目标选择器和具体字号，不得只凭截图目测。若目标区域处于 `transform: scale(...)`、缩放 stage 或 1920 原型画布内，关键操作按钮/输入文字必须按可见字号建模，必要时用 `原型字号 / scale` 的作用域 CSS 变量反向补偿，并在静态契约中锁定变量和选择器。
- Blocker: 找不到目标选择器、无法区分卡片内文字与页面其它文字、契约无法稳定 RED/GREEN、缩放容器导致关键操作按钮可见字号继续偏小、或改动会同时改变数据、权限、接口、保存/提交链路时必须停止补齐范围。
- Verification: 至少运行目标字号静态契约和一个相邻结构/显示契约；若改动触及 Vue/TS 逻辑或构建可受影响，再运行 `pnpm ts:check`。
- Forbidden action: 禁止用全局 `body`/Element Plus 泛选择器批量放大、禁止隐藏/缩放容器冒充字号变化、禁止把整体 stage 缩放后的关键操作按钮字号缩小当作“页面已适配”、禁止把截图局部需求扩大成整页重设计、禁止跳过 RED 直接改 CSS。
- Evidence: 任务 `doc/tasks/20260729-card-text-double/`，eDHR 填写辅助模式卡片原有半字号静态契约先 RED，再将网格卡片内标签、输入/占位、选择项、按钮、校验和单位文字提高为 2 倍；任务 `doc/tasks/20260806-frontline-production-fullscreen-logic/`，一线生产 1920 画布 stage 缩放完整显示后，最大化/主页、重填、提交按钮用 `42 / scale` 和 `54 / scale` 的 stage 作用域字号变量补偿，避免按钮文字随整体 transform 变小。

## 前端参考页面像素级布局比对门禁

- Trigger: 用户提供本地 HTML、原型页或明确要求“严格完全一致”“大小、排版完全一致”“像素级一致”，尤其目标页面已有真实前端实现和动态数据。
- Preflight check: 除静态合同锁定 DOM/CSS token 外，必须用 Playwright 在目标 viewport 同时打开参考页和真实路由，采集关键区域 `boundingBox()`，逐项比较 `x/y/width/height`；参考页的 body/字体/盒模型/媒体查询边界也要纳入合同。
- Blocker: 只靠静态 CSS token、截图目测或单张截图宣称像素级一致，未比对真实路由与参考页的关键区域尺寸；或真实路由仍被登录页、权限页、外层后台 layout、媒体查询、line-height、字体继承、共享弹窗挂载位置影响时必须继续修复或记录阻塞。
- Verification: 聚焦静态合同 PASS，真实 Playwright 路由截图尺寸等于目标画布尺寸，layout compare JSON 的 `diffCount=0` 且 `pageErrors=[]`，并复跑相邻静态合同、`pnpm ts:check` 和 `git diff --check`。
- Forbidden action: 禁止用缩放、截图裁剪、隐藏外层布局、硬编码假数据、API/mock 页面、截图 OCR 或只比较源码 token 冒充像素级一致。
- Evidence: 任务 `doc/tasks/20260806-frontline-production-pixel-parity/verification-report.md`，一线生产参考 HTML 对齐中，静态合同通过后真实 Playwright bounding box 仍发现顶栏按钮高度差 7px，最终定位到 `top-label` 的 line-height 差异并修到 `diffCount=0`。

## 前端截图按钮统一静态契约门禁

- Trigger: 用户基于截图要求统一、隐藏或不显示页面按钮、返回按钮、Scheme D 按钮样式、图标按钮，尤其出现“返回xxx”“返回列表”“返回上一页”“红框里的按钮统一”“黄框内按钮不显示”等表述。
- Preflight check: 先区分可见 UI 文案、源码注释和测试负向断言；对截图命中的同类按钮新增或更新任务专用静态契约，先让旧文案或旧按钮可见状态 RED，再按既有 handler 最小改文案、图标或调用方显示条件，不得重写返回路由、query、权限或保存/关闭链路。共享组件被多处复用时，优先断言目标调用方不再传 `show-*` 显示 props，保留非目标上下文的可复用能力。
- Blocker: 无法确认按钮作用域、同一文案也用于非按钮业务说明、静态契约无法稳定区分可见旧文案与负向断言、或改动会触碰 API/权限/状态流时，必须停止补齐定位和验收口径。
- Verification: 聚焦静态合同必须断言目标按钮图标、标准文案或隐藏后的调用方 props，并负向禁止旧长文案或目标按钮残留；再运行相邻页面静态契约、旧文案/按钮扫描、`pnpm ts:check` 和 `git diff --check`。
- Forbidden action: 禁止只改截图单页但不扫描同类头部按钮；禁止把测试里的旧文案负向断言当作页面残留；禁止为了隐藏某处按钮而删除共享组件能力、改返回目标、吞掉错误或扩大成无关重设计。
- Evidence: 任务 `doc/tasks/20260803-unify-header-return-buttons/`，头部“返回表单模板”等长文案先由专用静态契约 RED，随后统一为 `ep:arrow-left` + “返回”，并保留原路由行为；任务 `doc/tasks/20260803-hide-dcc-controlled-file-buttons/`，DCC 受控浏览 viewer 基础信息面板先由专用契约 RED，随后仅移除 viewer 调用方的 `show-info-actions`、`:show-edit`、`:show-product-recognition`，保留共享组件和普通详情页能力。

## 前端列表状态口径完整性门禁

- Trigger: 前端列表、版本工作区、状态表格、历史记录、候选版本、`只显示`、`仅展示`、`有效历史`、`已生效历史版本`、`取消的不显示`、`CANCELLED`、`DRAFT`、`ACTIVE`、`SUPERSEDED`。
- Preflight check: 先从用户原话或需求中拆出允许状态集合和禁止状态集合；若出现“只显示/仅展示”，必须按正向允许集合建模，而不是只排除截图里出现的一个异常状态。静态合同要同时断言允许集合、禁止集合和“不允许只写 `!== <badStatus>`”。
- Blocker: 过滤谓词只排除一个报错状态、测试只覆盖截图里出现的状态、文档把“只显示 A/B”改写成“隐藏 C”、或真实 E2E 没有证明至少一个非截图异常状态也被隐藏时，不得宣称完成。
- Verification: 聚焦静态合同必须包含一个负向断言，例如禁止 `version.lifecycleStatus !== 'CANCELLED'` 这种反向过滤；真实 E2E 若可运行，必须从页面断言允许状态可见、至少一个未生效候选状态和取消状态不可见，并记录无写请求。
- Forbidden action: 禁止把截图症状当作完整需求口径；禁止把“取消的不显示”当成唯一验收项而忽略前半句“只显示已生效的历史版本”；禁止用仅隐藏 `CANCELLED` 的实现替代 effective-only 列表口径。
- Evidence: 任务 `doc/tasks/20260727-route-version-list-active-history-only/`，首轮只隐藏 `CANCELLED` 后 completion audit 发现 `DRAFT` 仍可显示，最终改为 `ACTIVE/SUPERSEDED` 正向集合并用真实 E2E 证明 `V19 DRAFT` 与取消版本均隐藏。

## 前端同路由多入口分面门禁

- Trigger: 同一详情页、抽屉、弹窗或隐藏路由被多个业务入口复用，但用户要求“只显示/仅展示”某一类内容，尤其入口文案包含“追溯”“签核”“审批”“日志”“记录”“详情”。
- Preflight check: 先拆出每个入口的正式信息范围和非目标范围；复用同一路由时必须显式建模 mode/scope query、类型定义和解析函数，入口 helper 必须传入明确 scope，不得只依赖按钮文案、来源页面或默认详情状态推断。BPM 详情页通过 `formCustomViewPath` 嵌入业务详情组件时，也必须先判断审批人是否只需要业务审核摘要；若只需摘要，应在 BPM 层提供专用摘要和正式业务处理入口，不得无条件挂载完整业务详情页。BPM 详情页标题若来自英文流程定义名，必须使用正式中文映射展示，不能只在审批中心列表中文化而让详情页继续直出英文。
- Blocker: 两个入口仍生成同一 URL/query、详情页只隐藏局部标题但仍加载或渲染非目标区块、scope 缺类型约束、BPM 自定义业务表单仍无条件挂载完整业务详情组件、BPM 详情页标题仍直接渲染英文 `processInstance.name`、审批详情首屏无条件预加载流程图或重型辅助详情接口、用户明确删除某个 Tab 后源码仍保留该 Tab、隐藏 pane、组件 import、状态 watch 或专属 API 调用、静态合同不能同时证明入口 URL 和区块可见性，或用 CSS 隐藏/空数据冒充信息边界时必须停止。
- Verification: 聚焦静态合同必须断言入口 helper 参数、URL query、scope 解析、正向显示区块、负向隐藏区块和非当前分面辅助加载短路；BPM 自定义业务表单场景还必须断言审核摘要、当前节点、正式处理入口、完整业务组件条件挂载、详情页中文标题映射、流程图按 Tab 懒加载；若用户要求删除 Tab，静态合同必须负向断言 Tab、pane、组件 import、状态 watch 和专属 API 请求均不存在；涉及 Vue/TS 时运行 `pnpm ts:check`。
- Forbidden action: 禁止把多个业务入口继续合并成无差别详情页；禁止用默认 `traceability=1`、按钮文案、`from=browser` 或空数组推断分面；禁止吞掉非目标接口错误来掩盖区块仍在加载。
- Evidence: 任务 `doc/tasks/20260803-dcc-trace-signature-scope-split/`，DCC 受控浏览“追溯”和“签核”原先打开同一追溯详情，最终通过 `traceScope=trace/signature` 与 `showLifecycleTraceSections` / `showSignatureTraceSections` 拆分页面关注范围；任务 `doc/tasks/20260804-bpm-dcc-approval-compact-detail/`，BPM 流程详情旧实现通过 `BusinessFormComponent` 无条件嵌入完整 DCC 受控文件详情，导致审核人看到项目代码联动、受控浏览落位和排障信息，最终改为 BPM 层 DCC 审批摘要卡并保留文控处理页入口；任务 `doc/tasks/20260804-dcc-approval-detail-title-performance/`，BPM 详情页红框标题仍显示 `DCC Controlled File Approval` 且首屏同时请求流程图和 DCC 摘要，最终补中文标题映射、DCC 摘要独立加载，并在用户追加删除红框 Tab 后彻底移除流程图/流转记录 pane、组件和模型视图请求链路。

## 前端角色内容页签拆分口径门禁

- Trigger: 用户要求“某角色/某类内容专门做一个页签显示，不再显示在某工作台”，尤其涉及 `生产组长`、`PQC组长`、`leaderType`、`TeamLeaderWorkbenchPage`、eDHR 批记录页签、角色工作台页面内部功能模块 Tab 或其它角色型工作台拆分。
- Preflight check: 先从用户原话拆出“要拆出的角色/内容”和“原工作台保留的角色/内容”，并确认“页签”指动态菜单/主导航入口还是页面内部 `el-tabs`；当同名对象既可能是 Office 文件也可能是系统页面时，必须先用用户给出的路径、截图、路由或组件文本确认真实承载物，未定位到 Office 文件不得仅因用户使用“tab”一词就转向工作簿处理。用户说“类似批次执行”“放在 QA 下面”时，按 eDHR 父菜单下的独立主导航子菜单处理，不得误做成 eDHR 批次页内部 Tab。若用户明确说同一角色下“人员管理、报工管理、损耗管理、历史表单”等不同功能模块，则按该角色页面内部功能模块 Tab 处理，并先核对共享组件中其它角色复用的 content gate、默认激活页签、默认模块首次挂载的数据加载、每个非默认功能模块的数据加载触发和相邻静态合同。默认页签的状态值在 setup 阶段已经确定，不会触发非 immediate watcher；默认模块必须由 `onMounted`、immediate watcher 或等价的显式初始化入口加载正式数据，并与非默认页签切换加载使用同一查询契约。当前/历史页签拆分还必须把互斥边界落实到后端正式查询：当前页排除已进入历史终态的记录，历史页只读取该终态，同时明确未处理和退回待修改记录的归属；前端隐藏按钮不能替代列表读模型边界。共享组件内同一角色页签可能在人员、管理、详情、看板等多个模块块重复渲染，新增页签必须同步全部重复 tab 组、独立 tab key、显示 gate、查询触发、列池或状态隔离，不能只改当前可见块。再核对现有包装页、路由、页签 key、标题、权限和共享组件 props；若工作区已有相反方向的半成品拆分，必须先用 RED 静态合同锁定当前用户要求，不得沿用旧任务口径。
- Legacy route removal: 用户要求删除旧角色工作台“路由和页面”时，先区分可访问页面身份与仍被正式包装页复用的共享实现；应删除旧 route/name/direct navigation，并把真实流程脚本迁移到当前正式角色路由，但不得为追求文件名消失而复制或重命名超大共享组件，也不得误删仍在使用的 `/team-leader/**` API 和权限命名空间。动态菜单按旧 path/component/componentName 只读核对为零时，不新增无目标数据的删除迁移。
- Additional preflight check: 涉及提交后审查、复核或处置类按钮迁移时，先区分“一线填写/提交前页面”和“提交后的管理列表行操作”；只能提交后触发的动作必须落到管理列表对应业务行，并由该行携带正式来源 ID 和可用业务上下文，不能保留在一线填写页作为提前入口。PQC提交类不合格审查的最小入口是 `PQC_SUBMISSION + sourceId`，PQC管理入口不得携带 `batchExecutionId`，按钮可见性也不得依赖该字段。若源码已有行按钮但真实页面看不到，必须同步核对 `v-hasPermi`、目标角色 `role.code`、隐藏按钮 `type=3` 授权和登录后权限缓存；不得把权限缺失误判成组件缺失，也不得为显示行按钮而授予独立页面菜单或处置类权限。
- Blocker: 专门页签拆成了错误角色、把主导航页签误做成页面内部 Tab、把页面内部功能模块 Tab 误做成新菜单、原工作台仍传入目标角色 props、两个入口同时显示同一角色内容、同一终态记录同时出现在当前与历史列表、功能模块仍纵向混排、旧 route/tab key/页面关系图仍指向相反角色、默认功能模块已显示但仅依赖非 immediate watcher 导致首次挂载不加载、非默认功能模块只切换显示但没有 watcher/handler 触发正式列表加载、或静态合同只断言“有独立页签”但不验证角色 props、模块 gate、共享 gate、数据加载触发和原工作台负向隐藏时必须停止。
- Verification: 聚焦静态合同必须同时断言页签 label、tab key 或主导航菜单 sort、route path、route name/title、包装组件文件、共享组件 `leader-type` 或等价 props、原工作台 `doesNotMatch` 目标角色内容、页面关系图节点和相邻工作台合同；页面内部功能模块 Tab 还必须断言包装页显式启用模块 Tab、非目标角色未启用该专属 Tab、每个模块块由对应 computed gate 控制、默认数据列表在首次挂载时调用正式加载方法、非默认数据列表在 tab 选中时设置正式 query 角色/日期/分页并调用正式加载方法、共享 gate 未破坏相邻角色合同。当前/历史列表还必须分别锁定前端视图参数和后端互斥查询条件，并覆盖无终态、退回状态和已完成终态三类数据。真实页面验证默认列表时，必须在点击任何模块页签前监听正式列表请求，断言默认 tab 为选中态、请求成功且表格有可见行。涉及动态菜单时还必须断言 `system_menu`、租户套餐和角色菜单绑定；涉及 Vue/TS 时运行 `pnpm ts:check`。
- Additional verification: 提交后按钮迁移必须同时覆盖原一线入口负向断言、新管理行入口正向断言、路由参数或请求参数携带正式来源 ID、缺少批次执行时仍显示入口、目标角色拥有行按钮所需隐藏权限且不拥有无关菜单/处置权限，以及真实 E2E 脚本按新管理路径点击行级按钮。权限迁移后真实页面验证必须使用 fresh 登录或等价权限缓存刷新。
- Forbidden action: 禁止用 CSS 隐藏、仅隐藏操作按钮、前端本地过滤、空数据、路由别名、旧页签文案、内部 Tab 冒充主导航入口或保留旧反向 wrapper 冒充拆分完成；禁止把“PQC组长拆出去”与“生产组长拆出去”互换处理。
- Evidence: 任务 `doc/tasks/20260804-production-leader-tab/`，基线中已有相反的 `PQC组长` 独立页签，当前需求要求 `生产组长` 独立页签，最终用静态合同先 RED 再将 `BatchProductionLeaderWorkbenchPage`、`productionLeader` 路由和组长工作台 `leader-type="PQC"` 边界锁定；任务 `doc/tasks/20260804-pqc-leader-tab/`，用户纠正“不是 tab，是类似批次执行的页签”，最终锁定 `QA -> 生产组长 -> PQC组长 -> 批次执行` 主导航顺序，并从 `EdhrBatchRecordTabs.vue` 移除内部 leader tabs；任务 `doc/tasks/20260805-production-leader-function-tabs/`，用户要求生产组长内“人员管理、报工管理、损耗管理”等不同功能模块是不同 Tab，最终保留 `ProductionLeaderWorkbenchPage` 主导航入口，仅在共享工作台增加生产组长内部模块 Tab，并复跑 PQC 组长相邻合同防止共享 gate 破坏。任务 `doc/tasks/20260806-production-leader-feedback-random-data/`，生产组长内部默认页签为“人员管理”，用户点击“报工管理”后旧代码只切显示不触发 `getSubmissionList()`，最终补 `watch(activeProductionModuleTab)` 并用真实页面证明表格不再为空。任务 `doc/tasks/20260807-pqc-form-history-tab/`，PQC 组长新增“历史表单”时必须同步 4 组重复 PQC module tabs，新增 `history` 状态、`showPqcFormHistoryModule`、独立列池 tableKey、`APPROVED` 查询和只读操作边界，并更新生产报工历史相邻合同兼容共享逻辑。任务 `doc/tasks/20260809-batch-record-mapping-tab/`，用户通过截图澄清“批记录测试”是系统页面而不是 Excel 工作簿，最终按现有 `BatchRecordTestPage.vue` 内部 `el-tabs` 增加第五个页签。任务 `doc/tasks/20260810-pqc-management-initial-load-fix/`，PQC 默认页签初始值已是 `management`，非 immediate watcher 不会首轮执行，最终在首次挂载显式加载当前默认列表并用真实页面证明无需切换页签即可显示数据。任务 `doc/tasks/20260811-pqc-review-same-inspector-confirmation/`，PQC 历史表单虽已限定 `APPROVED`，但管理列表 `CURRENT` 未排除该终态，最终在正式 Mapper 中锁定互斥边界并保留未复核和 `REJECTED` 记录。任务 `doc/tasks/20260901-pqc-management-nonconformance-action-visible/`，PQC 管理行按钮源码已存在但因 `pqc_leader_permission` 缺少不合格审查创建权限被 `v-hasPermi` 隐藏，最终补隐藏查询/创建按钮授权并阻断独立页面菜单和 QA 处置权限误授。任务 `doc/tasks/20260902-pqc-management-nonconformance-button-visible/`，PQC 管理行记录可复核但缺少 `batchExecutionId` 时按钮也必须显示，前端以 `PQC_SUBMISSION + sourceId` 进入统一评审页，后端按提交事件解析工单并冻结工单。

- Evidence extension: 任务 `doc/tasks/20260904-remove-team-leader-workbench/` 删除旧班组长 route/name 和真实脚本直达地址，同时保留生产/PQC包装页共享实现及后端 API 命名空间。

## 前端多布局模式真实页面门禁

- Trigger: 同一 Vue 页面同时支持平铺模式和内部页签模式，存在 `show*Tabs`、`active*Tab` 或包装页 props，静态合同通过但真实页面缺少可见区域，或 Playwright 脚本假设某个仅在另一布局中存在的页签。
- Preflight check: 先确认真实路由包装页传入的布局 props，并分别定位平铺分支和页签分支的正式 DOM。静态合同必须锁定当前实际布局的可见区域；真实 E2E 必须按页面实际布局进入目标区域，不能用隐藏页签、API 响应或源码存在代替可见 UI。
- Blocker: 目标标记只存在于未启用的布局分支、真实页面要求点击不存在的页签、平铺模式与页签模式状态源不一致，或截图/断言只证明接口响应而未证明当前布局的可见控件时必须停止。
- Verification: 聚焦静态合同、`pnpm ts:check` 和真实 Playwright 均通过；Playwright 记录实际布局、可见目标文本、目标写请求数量、page error 和 console error。
- Forbidden action: 禁止为测试新增假页签、隐藏重复标记、把平铺页面强行切到页签模式、用 API-only 或静态源码命中冒充真实页面通过。
- Evidence: `doc/tasks/20260807-team-leader-responsible-route-source-fix/verification-report.md`。

## eDHR 表单追溯可视化历史详情门禁

- Trigger: eDHR 表单追溯、历史批记录入口隐藏、归档批次详情、`BatchExecutionTraceDrawer`、`review-timeline.executionReviews.formViewModel`、`EdhrExecutionReadonlyForm`、用户要求历史详情像批次执行填写页而不是纯文字。
- Preflight check: 先区分“独立历史批记录入口”“追溯抽屉”和用户实际点击的“详情”弹窗；若产品口径要求不显示独立历史批记录，必须同时检查页签、隐藏路由、批次详情卡片、页面关系图、详情弹窗和可见文案。可视化详情必须在表单追溯上下文内复用 `review-timeline` 的持久化执行快照、模板布局、单元格值和签名记录，并使用 `EdhrExecutionReadonlyForm` 或同等只读表格组件展示。
- Blocker: 仍存在可点击独立历史批记录入口、用户点击“详情”后看不到“批记录表单”页签、详情只展示 JSON/纯文字快照、表单追溯重新拉独立历史批次列表、历史详情依赖当前活动 BATCH 配置或当前 Jimu 报表、或为了隐藏入口删除历史数据时，必须停止。
- Verification: 聚焦静态契约必须同时断言旧入口无可见残留、“电子批记录变更详情”弹窗有“批记录表单”页签、表单追溯抽屉有“批记录表单”页签、存在工序/表单导航、只读表单接收 `formViewModel` 与 `signatureRecords`、并禁止保存/签名/放行/作废动作；涉及 Vue/TS 时运行 `pnpm ts:check`。
- Forbidden action: 禁止用 CSS 隐藏旧入口、禁止保留可搜索隐藏路由作为正式入口、禁止把 `executionSnapshotJson` 直接渲染成纯文本、禁止用 `formBindings` 或当前路线 BATCH 配置补历史批记录。
- Evidence: 任务 `doc/tasks/20260803-edhr-trace-visual-record-detail/verification-report.md`。

## 前端列表跨账号默认列布局统一门禁

- Trigger: 同一列表在不同浏览器、账号或租户显示不同字段，页面存在“显示字段”、`useUserTableColumns`、`data-user-table-key`、用户列配置接口，用户要求统一为 admin 默认布局，或要求收窄固定操作列并将操作按钮排成稳定行列。
- Preflight check: 先区分三类差异：个人列配置控制的字段可见性/列宽、`v-hasPermi` 控制的操作按钮、视口宽度造成的横向滚动。若需求是让既有用户统一采用新的默认列集合或固定操作列宽度，同时仍保留“显示字段”，必须升级稳定 table key，并同步标准列表模板、Element Plus 表格标识和 `useUserTableColumns` 调用；只修改默认 `visible` 或模板宽度不会覆盖旧服务端配置。若需求是删除某个列表列，必须同时检查 `<el-table-column>`、默认列定义、列设置池、该列专用的行级辅助请求/弹窗/样式，以及静态或真实 E2E 脚本；不得只删除 DOM 或只用 `v-if` 隐藏，避免显示字段或旧测试把已删除列带回。若需求是记住用户拖拽列宽，保存的 `columns[].width` 必须回填到实际 `el-table-column` 的 `width`，默认 `min-width` 只能作为无保存配置时的模板约束。收窄固定操作列并指定按钮行列数时，必须使用明确 grid 轨道，清除 Element Plus 相邻按钮默认外边距，并保持按钮文案不换行；不得依赖 flex 自由换行碰巧形成目标行数。若验收要求“列表行直接显示”关键业务信息，必须确认这些信息不只存在于可隐藏列、固定列或横向滚动外区域，应在至少一个默认稳定可见列中重复承载可读摘要。
- Role action visibility extension: 当业务明确要求某正式角色也能看到列表操作按钮，而该角色未必拥有按钮原权限码时，页面不能继续只用 `v-hasPermi` 隐藏入口；应使用共享 `checkPermi(...) || checkRole([...])` 计算属性表达“原权限码或目标角色均可见”，并保留原行级业务状态限制、原按钮处理器和后端正式权限门禁。
- Blocker: 仍读取旧 table key、只改默认列但历史用户配置继续覆盖、固定操作列仍用自由换行导致不同权限按钮数量下错位或文字裁切、关键验收信息只放在可隐藏列或固定列导致真实 E2E/普通用户无法在主列表行确认、为了视觉一致移除权限指令或给普通用户显示 admin 操作、通过清浏览器缓存或批量删数据库配置冒充正式迁移、或显示字段入口保存到与加载不同的 key 时必须停止。
- Verification: 聚焦静态合同必须断言新 key 在模板、表格标识和 hook 三处一致，旧 key 不再使用，默认显示/隐藏字段集合明确，关键验收信息位于稳定可见列，显示字段自动保存和既有权限码保留；删除列时必须负向断言模板列、默认列 key、列专用辅助请求/弹窗/样式和旧入口文案不存在，并正向断言相邻业务动作仍保留；涉及列宽时必须断言 hook 合并 `saved.width`、拖拽后自动保存、页面列同时绑定 `:width` 和默认 `:min-width`；涉及紧凑操作列时还要断言模板宽度与默认列定义一致、明确 grid 轨道、按钮无默认左外边距且原权限和处理器保留；涉及角色直显操作按钮时必须断言 `checkPermi` 原权限路径、`checkRole` 目标角色路径、原状态限制和禁止残留 permission-only directive。真实 E2E 可用时使用同一账号分别在两个浏览器验证表头、显示字段勾选、固定列实际宽度、按钮行数、文字不换行和相邻边界，并记录无业务写请求、无 console error。
- Forbidden action: 禁止引入 localStorage fallback、静默忽略列配置接口失败、扩大角色权限、删除业务字段定义、或用不同账号的按钮差异证明浏览器渲染不一致。
- Evidence: `doc/tasks/20260730-route-admin-list-layout-unification/verification-report.md`；`doc/tasks/20260802-dcc-controlled-browser-ux-optimization/verification-report.md`；`doc/tasks/20260812-standard-list-column-width/`；`doc/tasks/20260813-dcc-browser-operation-panel-two-row/verification-report.md`；`doc/tasks/20260813-production-report-operation-panel-half-width/verification-report.md`；`doc/tasks/20260830-batch-record-form-list-hide-filler-column/`；`doc/tasks/20260903-registration-manager-actions-visible/verification-report.md`。

## 前端权限页签正向授权门禁

- Trigger: 前端页面、动态菜单、顶部页签、左侧菜单、隐藏路由或入口默认页涉及“普通用户只能看到/仅显示某页签”、`activeMenu`、`redirect`、`permissionStore`、静态隐藏子路由合并。
- Preflight check: 先拆出普通用户允许页签集合和管理员允许页签集合；默认入口重定向必须来自已授权动态子路由或明确的普通用户页签，不得固定跳到管理员页签。隐藏静态子路由合并时，权限型壳路由不得把未授权的隐藏静态子路由补回普通用户路由表。
- Blocker: 普通用户仍可默认进入管理员列表、无权限页签组件会 mount 并触发接口 403、只改菜单 SQL 但前端静态路由仍补回未授权子路由、或只隐藏一个截图页签而未按正向允许集合建模时必须停止。
- Verification: 新增聚焦静态合同同时断言普通页签正向集合、管理员页签集合、默认重定向、组件 mount gate、动态权限路由合并边界和菜单/角色 SQL 授权边界；涉及 Vue/TS 路由逻辑时运行 `pnpm ts:check`。
- Forbidden action: 禁止用前端空白、吞掉 403、默认成功、API-only 断言、只改按钮可见性或只改后端菜单授权来冒充页签隔离完成。
- Evidence: 任务 `doc/tasks/20260730-electronic-signature-my-tab-only/`，电子签名普通用户旧实现固定进入“签名记录”并触发无权限列表，修正为普通角色只保留根入口和“我的签名”，前端按授权动态子路由重定向并禁止补回未授权治理页签。

## 全局与租户配置分区权限门禁

- Trigger: 同一配置页同时包含全系统配置和租户内规则、全局 Quartz 时间、租户角色/公司接收规则、个人中心“配置”总页签由多个业务权限共同决定。
- Preflight check: 把全局配置和租户配置拆成独立权限码、独立 API 和独立组件挂载条件；总配置页签只判断“是否拥有任一配置子权限”，每个分区只在自身权限成立时挂载并请求。全局写接口还需服务端校验系统管理员身份，租户规则接口只能作用当前租户。
- Blocker: 租户管理员能修改全局时间、一个宽泛权限同时控制两类配置、无权分区仍 mount 并发出 403 请求、为显示新配置而授予不相关高权限、或不同租户可各建一套本应全局唯一的时间时必须停止。
- Verification: 前端静态合同和真实多账号路径覆盖仅全局权限、仅租户规则权限、仅其它配置权限、无配置权限；断言总页签/分区可见性、请求数量和保存目标。后端控制器测试同时证明两类权限/API 不能互相越权。
- Forbidden action: 禁止复用不相关权限、只隐藏控件不拦 API、用 403 后空白页冒充隔离、或把全局系统配置保存成个人偏好/租户配置。
- Evidence: `doc/tasks/20260814-domestic-registration-certificate-lifecycle-design/verification-report.md`。

## 前端同集合弹窗导航上下文门禁

- Trigger: 弹窗内新增上一条/下一条、上一张/下一张、同版本/同产品/同集合切换，且候选集合不受当前列表筛选或分页限制。
- Preflight check: 先确认弹窗切换后父页面的当前对象、预览标题、操作区和详情上下文是否都能从候选集合解析；若候选可能不在当前列表页，`selected/current` 计算必须显式合并候选集合。
- Blocker: 切换后弹窗已加载新对象但主页面显示“未选择”、预览操作区消失、详情仍指向旧对象、或静态合同只断言 emit 事件而不覆盖父页面上下文同步时必须停止。
- Verification: 聚焦静态合同同时断言弹窗导航事件、候选集合来源和父页面 selected/current fallback；真实 E2E 可用时点击一次可用导航按钮，断言目标详情/规则请求的对象 ID 改变且无非预期写请求。
- Forbidden action: 禁止只更新弹窗局部 props 或 label 冒充切换完成；禁止把当前列表页筛选结果当作同集合候选全集；禁止用刷新页面或静默回到第一条掩盖上下文丢失。
- Evidence: 任务 `doc/tasks/20260729-batch-record-fill-config-navigation/`，批记录填写配置上一张/下一张候选不按当前列表筛选缩窄，需从导航候选集合补齐页面预览上下文。

## 前端填写配置红框区域隐藏门禁

- Trigger: 批记录填写配置、辅助表单映射、截图红框区域、`BatchRecordCellRulesConfirmDialog`、`data-fill-config-actions="primary"`、`data-fill-config-current-form="name-version"`、`batch-record-cell-rules-editor__panel-head`、`batch-record-cell-rules-editor__cell-rule`、`gridCell.sourceSummary`、左侧原表单说明栏、中间辅助表单预览说明栏、格子内“未映射/原表单”次级说明。
- Preflight check: 先区分“说明/装饰性红框”与“必要操作能力”；隐藏红框标题、顶部操作组或格子内次级说明时，必须保留原表格主文本、辅助表格主字段名、右侧映射控制栏和正式保存/重读/关闭链路，保存按钮可移到非红框固定操作区但不得改保存 API 或吞掉错误。若红框位置承载当前表单名称和版本，必须直接使用当前 `report.reportName || report.batchRecordName || report.reportId` 与正式 `report.versionNo`，不得从同产品同版本导航标签、表格单元格正文或 `formBindings` 推导。
- Blocker: 红框 DOM 仍存在、格子内规则类型/必填/未映射/原表单来源仍显示、保存/重读能力被一起删除、辅助格或原表格不可点击、当前表单名称版本复用导航标签或表单槽位来源、静态合同只检查隐藏不检查必要能力保留、或用 CSS 透明/遮挡伪装不显示时必须停止。
- Verification: 运行 `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js`、`node tests/e2e/edhr-visual-fill-config-static.spec.js` 和 `pnpm ts:check`。
- Forbidden action: 禁止通过隐藏整个填写配置工具、删除映射控制栏、关闭保存入口、改保存 payload、吞异常或 API-only 断言来满足截图红框隐藏。
- Evidence: 任务 `doc/tasks/20260729-fill-config-redbox-hide/`，补充反馈要求连同卡片内第二行次级说明一起隐藏；任务 `doc/tasks/20260729-fill-config-current-form-title/`，红框位置显示当前表单名称与版本时使用当前报表上下文并保留无写请求真实只读验证。

## 批记录填写配置批量属性与映射冲突门禁

- Trigger: 批记录填写配置、`BatchRecordCellRulesConfirmDialog`、多选格子、混合多选、批量字段类型、批量可填写、批量必填、批量控件类型、已有辅助表格映射、保留原映射、清除原映射、取消本次修改、操作人签名改数字、`data-fill-config-batch-field-type`、`data-fill-config-batch-component-flag`。
- Preflight check: 多选格子后的属性修改必须先解析正式多选集合，最后点击的格子只能作为焦点，不得作为唯一修改目标。字段类型、控件类型、可填写/不可填写、必填/可选等属性必须复用统一批量应用函数；右侧常用属性控件的 `model-value` 也必须按多选集合计算，混合类型、混合控件或部分不可填写时显示为空/未全选状态，让用户重新选择目标值并触发批量更新。批量修改字段类型前还必须识别所选格子是否已有辅助表格映射，已有映射代表历史业务关系，不得用“类型不兼容”直接硬阻止，页面必须让用户显式选择保留原映射、清除原映射或取消本次修改。
- Blocker: 多选后切换可填写、必填、字段类型或控件类型时只有最后一个焦点格变化，字段属性控件直接 `v-model` 到 `selectedRule`，或 `:model-value` 只取最后焦点格导致目标值等于焦点格当前值时 `@change` 不触发，已映射格子批量改类型时被直接提示不能修改，清除映射时误删非选中格子的辅助映射，保留映射时丢失原责任主体，取消后仍改变字段类型，或静态合同不能证明多选集合、混合多选显示值、单选回退和三种映射处理分支时必须停止。
- Verification: 运行 `node tests/e2e/batch-record-cell-rules-batch-properties-static.spec.js`、`node tests/e2e/batch-record-cell-rules-multiselect-field-type-static.spec.js`、`node tests/e2e/batch-record-cell-rules-empty-assist-save-conditional-static.spec.js`、`node tests/e2e/batch-record-cell-rules-save-assist-subject-guard-static.spec.js` 和 `pnpm ts:check`。
- Forbidden action: 禁止让右侧属性控件只写 `selectedRule`、禁用批量属性入口、用不兼容提示、自动清空映射、吞掉保存错误、默认保留或默认清除来替代用户显式选择。
- Evidence: 任务 `doc/tasks/20260828-batchrecord-fillable-cell-type-labels/verification-report.md`，用户确认采用方案2后，批量字段类型入口支持保留原映射、清除原映射和取消本次修改，已有映射不再被强阻止。任务 `doc/tasks/20260828-batch-record-cell-rules-batch-properties/verification-report.md`，多选后右侧属性控件从直接绑定焦点格改为统一应用到已选格子集合，覆盖可填写、必填、字段类型和控件类型。

## 批记录填写配置整格点击门禁

- Trigger: 批记录填写配置、`BatchRecordCellRulesConfirmDialog`、原表单元格、单元格空白区域点击、Ctrl/Command 多选、Shift 范围多选、辅助表格映射选格。
- Preflight check: 先区分表格单元格 `<td>` 的可点击命中区域和内部按钮/文字内容区域；如果单元格高度可能大于内容高度，外层 `<td>` 必须承接统一点击入口，内部按钮保留可访问语义并停止冒泡，修饰键事件必须原样传入选择函数。
- Blocker: 只有按钮或文字区域可点击、点击单元格空白区域不选中、内部按钮和外层单元格重复触发导致 Ctrl 多选反复切换、Shift 范围选择丢失事件，或辅助映射模式无法通过同一入口分流时必须停止。
- Verification: 静态合同必须锁定 `<td>` 整格点击入口、按钮 `@click.stop`、整格可点击指针和按钮铺满样式；同时运行批量多选、填写配置基础、辅助映射绿色边框相邻合同和 `pnpm ts:check`。
- Forbidden action: 禁止只扩大文字 padding、只用 z-index 覆盖、隐藏按钮、删除可访问按钮语义、或用截图目测代替点击入口合同。
- Evidence: 任务 `doc/tasks/20260828-batch-record-cell-rules-full-cell-click/verification-report.md`，填写配置原表单格子从仅内部按钮响应改为 `<td>` 整格响应，并用 `@click.stop` 防止重复切换。

## 前端保存链路重复错误提示门禁

- Trigger: 页面保存动作由父组件聚合多个子组件/API 保存，且子组件、父组件、axios response interceptor 都可能 `message.error`/`ElMessage.error`。
- Preflight check: 保存入口前先梳理错误传播链；内部 API 调用如果由外层统一 toast，必须显式传 `ignoreErrorMessage: true`，子组件 rethrow 前不得再次 toast。
- Blocker: 同一个失败在页面出现 2 条及以上相同错误提示，或静态合同无法证明 axios 自动提示、子组件提示、父组件提示不会叠加。
- Verification: 新增聚焦静态合同，并用 Playwright 拦截目标保存接口返回业务错误，断言保存错误提示只出现一次且错误文本来自真实响应。
- Forbidden action: 禁止用吞异常、默认成功、隐藏后端错误、只改文案、或关闭全局错误处理来减少 toast 数量。
- Evidence: 任务 `doc/tasks/20260726-route-flow-v15-save-system-exception/`，路线流转关系图保存失败曾由 axios、RouteFlowGraphDesigner、RouteFormContent 三层重复提示“系统异常”；任务 `doc/tasks/20260830-registration-certificate-upload-flow-verification/`，注册证上传失败曾由全局请求错误和上传弹框本地 `message.error` 重复提示。

## 前端主结果弹窗失败原因可见门禁

- Trigger: 保存、提交、审核、发布或签名动作失败后页面显示主结果弹窗、大号结果弹框、`result-dialog`、`提交失败`、`保存失败`、`发布失败` 或同类状态。
- Preflight check: 先梳理错误传播链，主结果弹窗必须承载外层 catch 已解析的真实错误文本；若 toast 保留，弹窗也必须展示同一失败原因，成功状态必须清空失败原因。
- Blocker: 主弹窗只显示“提交失败/保存失败”等状态、不显示后端 `msg/message` 或本地 fail-fast 原因，或静态合同无法证明失败原因字段从 catch 传入弹窗状态时必须停止。
- Verification: 新增聚焦静态合同断言失败原因字段、模板可见区域、catch 参数传递和成功状态清空；真实写入 E2E 可用时用真实失败响应断言弹窗可见文本，不得用 mock 或拦截替代。
- Forbidden action: 禁止只依赖短暂 toast、改成通用默认失败文案、隐藏后端错误、吞异常、用成功弹窗残留旧错误或关闭全局错误处理来满足截图。
- Evidence: 任务 `doc/tasks/20260729-submit-failure-reason/`，eDHR 提交失败弹窗曾只显示“刘子良 提交失败”，未显示具体失败原因。

## 前端命令按钮失败必须终止在可见错误边界门禁

- Trigger: `保存/提交/审核/发布/签名` 等原生命令按钮的事件处理器会执行本地 fail-fast 断言、异步上下文加载或正式写接口，且失败需要留在当前页面供用户修正。
- Preflight check: 先明确按钮事件处理器的错误归属；本地前置断言必须在事件处理器内捕获、显示真实原因并 `return`，异步错误由页面负责提示时也必须在提示后终止当前命令。只有上层确实存在统一错误边界时才允许继续抛出，不能默认依赖 Vue 原生事件处理器接住异常。
- Blocker: 点击按钮后控制台出现 `Unhandled error during execution of native event handler`、页面没有可见原因、失败后仍继续打开确认弹窗或发起写请求，或通过长期禁用按钮隐藏缺失前置时必须停止。
- Verification: 聚焦静态合同锁定 `catch -> message.error(real reason) -> return`；Playwright 使用缺前置的真实页面点击按钮，断言可见错误文本、目标写请求数为 0，且本次操作没有新增 `pageerror` 或未处理事件异常。
- Forbidden action: 禁止 catch 后默认成功、吞掉错误不提示、提示后继续执行、提示后 rethrow 到原生事件处理器、用 disabled 隐藏所有可诊断失败，或用 API-only 证明页面错误可见。
- Evidence: 任务 `doc/tasks/20260807-frontline-pqc-formal-submit/verification-report.md`，一线 PQC 提交前置断言最初直接逃逸到原生事件处理器；修复后真实页面显示正式前置缺失原因，不产生写入且没有新增未处理异常。

## 前端延迟辅助加载错误归属门禁

- Trigger: 列表首屏已加载成功，但后续延迟加载的行级权限、预览、候选人、补充状态或右侧详情接口失败，页面出现全局 `系统异常`、列表加载失败或首屏错误条。
- Preflight check: 先区分首屏主查询和延迟辅助查询；主查询失败才写全局列表错误，行级/预览级辅助查询失败必须落到对应行、卡片或预览区域，并保留真实错误文本。
- Blocker: 延迟辅助请求失败会清空主列表、覆盖 `listErrorMessage`、触发默认成功/空数据、或静态合同无法证明错误归属边界时，不得宣称修复完成。
- Verification: 新增聚焦静态合同覆盖主查询仍全局报错、辅助查询不污染全局错误、错误文本在行级或预览级可见，并运行相邻首屏延迟加载合同。
- Forbidden action: 禁止吞掉辅助接口错误、把真实失败改成空配置/未配置、关闭 axios 错误、或只隐藏全局 alert 而不展示错误归属。
- Evidence: 任务 `doc/tasks/20260727-edhr-batch-record-list-system-exception/`，批记录表单列表中填写人规则延迟加载失败曾在列表已成功渲染后污染全局 `listErrorMessage`。

## 前端主查询错误重复提示门禁

- Trigger: 页面主查询在 `catch` 中设置 `loadError`、结果弹窗或 `message.error`，同时请求走统一 Axios response interceptor，截图出现重复 `系统异常` toast 或同一错误既由全局提示又由页面提示。
- Preflight check: 先确认错误归属由全局 Axios 还是页面本身负责；若页面需要保留行内错误条、结果弹窗或定制提示，则该请求必须显式设置 `ignoreErrorMessage: true`，并由页面 catch 统一展示真实错误文本。
- Blocker: 同一失败出现 2 条及以上相同错误提示、页面隐藏真实错误只剩默认空数据、或静态合同无法证明 `ignoreErrorMessage` 与页面错误展示同时存在时必须停止。
- Verification: 聚焦静态合同断言目标 API wrapper 设置 `ignoreErrorMessage: true`，页面仍设置可见错误归属；再运行相邻页面静态合同和 `pnpm ts:check`。
- Forbidden action: 禁止关闭全局错误处理、吞异常、删除页面错误条、返回默认成功/空数据或仅改文案来消除重复提示。
- Evidence: 任务 `doc/tasks/20260803-dcc-controlled-file-log-system-exception/`，文控日志主查询失败时 Axios 与页面 catch 重复弹出“系统异常”，修复为页面拥有错误提示归属。

## 前端页签首屏按需挂载门禁

### 首屏页签懒挂载与可见行查询

- Trigger: 多页签页面首次进入慢、红框页签首屏卡顿、`el-tab-pane` 默认挂载多个重组件、页签内列表按类别或行执行 N+1 辅助请求。
- Preflight check: 先定位默认激活页签、未激活页签组件、父页面 `onMounted` 请求和子页签行级辅助请求；未激活页签必须用 `lazy` 加显式已访问集合控制挂载，行级辅助数据只为当前可见页或当前操作对象加载。
- Blocker: 未激活页签仍首屏 mount，父页面为非默认页签无条件拉取默认页签列表，子页签首次加载对全量类别/全量行发起 N+1 请求，或辅助数据未加载时直接显示“未配置”造成误判时必须停止。
- Verification: 新增聚焦静态合同断言 `el-tab-pane lazy`、`loadedTabNames`/`isTabPaneMounted` 挂载边界、父页面非目标页签不调用默认列表加载、可见行 ID 集合驱动辅助请求，并复跑相邻页签/菜单合同与 `pnpm ts:check`。
- Forbidden action: 禁止用延时器、空数据、mock、关闭错误提示、隐藏页签、删除功能入口或把未加载状态伪装成未配置来冒充首屏优化。
- Evidence: 任务 `doc/tasks/20260803-dcc-category-tabs-first-load/`，DCC 文控权限 6 个配置页签改为首次激活才挂载，分发/培训规则只加载当前可见类别行规则。

### 独立配置页签加载与错误归属

- Trigger: 有效列表对象进入详情或编辑页却显示系统异常/对象无效，同时网络记录显示失败请求属于未激活的独立配置页签、扩展面板或附属数据源。
- Preflight check: 先按页面职责拆分主编辑上下文、共享前置数据和各独立页签配置；只有主编辑必需数据可以在入口统一加载。独立配置必须在用户首次激活所属页签时请求，错误状态由该页签保存并明确显示，不能改变父编辑器对正式对象有效性的判断。
- Blocker: 未激活页签请求仍在父级 `open`/`onMounted` 无条件执行、附属配置失败被包装成对象无效、或准备通过空对象、默认绑定、吞异常、隐藏页签来让主页面看似成功时必须停止。
- Verification: 聚焦合同锁定父级入口不调用独立配置请求、页签切换触发正式加载、失败写入页签可见错误并提供明确重试；真实 Playwright 从列表进入默认页签，断言独立请求尚未发生且主内容可见，再打开目标页签断言请求发生、失败就地显示、父编辑器仍保留，最后核对目标写请求边界和控制台错误。
- Forbidden action: 禁止返回空绑定或默认成功掩盖后端/数据库错误，禁止放宽对象有效性校验、自动创建缺失表、删除功能入口、API-only 冒充页面验证，或用其它页签数据证明当前配置正确。
- Evidence: 任务 `doc/tasks/20260812-process-route-edit-valid-route-guard/`，真实 E2E 证明有效路线入口曾因未激活 DCC 项目代码页签提前请求缺失表而被误报为路线无效；修复后默认工艺流程先正常加载，DCC 失败仅在该页签激活后明确显示。

### 顶部菜单页签切回缓存

- Trigger: 动态菜单页面、顶部 TagsView 页签、红框菜单页签、`keepAlive`、`noCache`、`componentName`、`defineOptions({ name })`、`AppView` `keep-alive`、从一个已打开菜单页签切到另一个再切回时重复执行首屏 `onMounted`，或 keep-alive 已命中但页内 `route.fullPath` watcher 仍恢复加载目录树/列表。
- Preflight check: 先核对菜单 `componentName` 与 SFC `defineOptions({ name })` 是否一致，再核对 `routerHelper` 动态路由生成的 `meta.noCache`、`tagsViewKeyMode`、`AppView` 的 `keep-alive include` 和 route key；多个正式路由复用同一个 SFC 且路由名不等于组件名时，必须在路由元数据声明显式缓存身份（如 `keepAliveName`）并让 `AppView` 与 `TagsView` 同时使用该身份。对明确要求切回不重复加载的正式菜单页签，必须在正式路由元数据层固定 `noCache=false`，不能只依赖运行态菜单 `keepAlive` 当前值。若页面自身监听全局 `route.fullPath`，还必须比较正式有效 route state，并记录目录树/列表已成功加载状态；只有 effective state 变化才恢复加载。
- Blocker: 页签切回仍重新 mount、动态路由生成 `noCache=true`、组件名不匹配导致 `keep-alive include` 无法命中、route key 使用 `fullPath` 导致 query 变化重建实例、后台页签 watcher 在同一 effective state 切回时仍调用目录树或列表请求，或静态合同不能证明缓存链路从菜单到 AppView 和页内 route restore guard 闭合时必须停止。
- Verification: 聚焦静态合同必须同时断言菜单 componentName、SFC name、`routerHelper` 正式路径/组件集合、`meta.noCache=false`、`tagsViewKeyMode='path'`、`TagsView` 缓存集合和 `AppView` `keep-alive`；若页面有 `route.fullPath` watcher，还必须断言同状态切回 guard 在 `loadDirectories()`/`getList()` 前返回、列表成功加载状态 key 和目录树成功加载标记；再复跑相邻页签去重合同、首开性能合同和 `pnpm ts:check`。
- Forbidden action: 禁止用 localStorage、缓存查询结果、延时器、空数据、隐藏 loading、吞请求错误或强制刷新来掩盖页签重新挂载。
- Evidence: 任务 `doc/tasks/20260803-dcc-upload-browser-tab-cache/`，DCC“文件上传”和“受控浏览”正式菜单页签在 `routerHelper` 中强制 `noCache=false`，并在受控浏览页内按 effective route state 跳过同状态切回恢复加载，避免运行态菜单 `keepAlive` 异常或后台 `route.fullPath` watcher 导致切回受控浏览重复加载；任务 `doc/tasks/20260804-approval-center-tab-cache/`，审批中心四个路由复用 `ApprovalCenterWorkbench`，需用显式 `keepAliveName` 对齐共享组件缓存身份，并在页面内用 route name 与成功加载 state 阻止后台实例重复请求。

## DCC 上传类别权限投影门禁

- Trigger: DCC 受控文件上传页、外来文件评审页、系统 NAS 转移、本地文件夹导入、受控文件元数据编辑、文件分类 `fileTypeTaxonomyId`、文件类别 `categoryId`、模板类别、文件类别下拉、`upload-preview`、`Controlled file category does not exist`、`Current user cannot access this controlled file`、类别级 `UPLOAD` 权限、`审批链路不完整`、`approvalPositionIds`、`signoffPositionIds`。
- Preflight check: 先确认类别列表接口返回当前用户类别级 `canUpload` 投影；上传页和外来评审页必须过滤 `canUpload=false` 且表单校验能拦截旧选择，同时后端 `upload-preview` / submit 继续用 `DccControlledFileCategoryPermissionSupport` fail-fast。受控文件上传页还必须区分“文件分类”taxonomy 与正式 DCC“文件类别”category：文件类别只能来自当前 taxonomy 叶子节点唯一可上传正式类别，taxonomy 切换必须清空旧 `categoryId`、目录上下文和上传预览状态；若该正式类别未绑定提交目录，必须由后端解析正式 `UNCLASSIFIED / 未分类` 目录并通过 `defaultUnclassified` 明示，前端不得要求提交人维护绑定。NAS 转移、本地文件夹导入和元数据编辑遇到已存在但未绑定目录的类别时，也必须复用同一正式未分类解析；有绑定目录的类别仍按绑定子树校验。上传预检若读取 `approvalPositionIds` / `signoffPositionIds` 判断审批链路完整性，类别列表接口必须从当前有效分类审批矩阵路线节点投影这两个字段；不得只依赖类别主表、目录绑定或前端空数组兜底。
- Presentation check: 用户要求隐藏文件类别下方的辅助说明或权限提示时，必须先冻结权限职责的正式口径；“移除展示节点”本身既不能证明权限校验应保留，也不能证明权限校验应取消。静态合同至少要锁定只读文件类别值继续显示、目标模板块不再渲染指定 helper/alert；权限回归则按已批准的正式阶段独立覆盖。若另一项明确需求把类别权限从上传阶段移到审批阶段，必须分别证明上传不阻断和无权审批被拒绝，不能用提示消失代替任一行为测试。
- Blocker: 类别列表缺少 canUpload、前端只按 active/directoryId 展示类别并排除可上传但未绑定目录的正式类别、上传接口为了消除提示放宽 UPLOAD 校验、文件类别允许跨 taxonomy 选择或保留 stale categoryId、把 fileTypeTaxonomyId 当作后端目录查询 categoryId、后端缺少唯一启用 UNCLASSIFIED 目录却继续提交、NAS/本地导入/元数据编辑仍提示“请先绑定目录”或要求用户选择未绑定类别的目录、上传预检未调用服务端 routeReadiness 权威校验导致误报审批链路完整/不完整、或静态合同不能证明无权限/跨 taxonomy 类别不会进入上传。若当前实现已切换为服务端 routeReadiness，静态合同不得再强制要求前端类别 DTO 暴露旧 approvalPositionIds / signoffPositionIds。
- Verification: 运行 `node tests/e2e/dcc-upload-category-permission-static.spec.js`、`node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js`、`node tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js`；涉及未绑定提交目录时，还要运行 `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_unclassified_upload_directory_seed_sql.py -q`、DCC base + unclassified seed 迁移门禁，以及 `getUploadDirectoryTree` / submit 的未分类目录后端单测；涉及 NAS/本地导入或元数据编辑时，还要运行 NAS 管理页、元数据弹窗静态合同和对应后端单测，并通过真实 Playwright 页面路径证明自动未分类提示可见、目标 DCC 写请求边界符合本轮只读或写入范围。涉及上传预检审批链路时，运行 `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsActiveApprovalMatrixPositionIds,DccCategoryApprovalMatrixAdminServiceImplTest#getActiveMatrixPositionIdsByCategoryIds_readsLatestActiveRoutePositionNodes" "-Dsurefire.failIfNoSpecifiedTests=false" test`。真实 E2E 若返回 `1080000196 Unclassified upload directory does not exist`，说明代码已进入正式 fail-fast 分支但本地库缺 seed，应先执行幂等 `20260803_dcc_unclassified_upload_directory_seed.sql` 并核对唯一 active `UNCLASSIFIED / 未分类`，不得改代码绕过。
- Forbidden action: 禁止把菜单权限当类别上传权限、禁止前端展示无权限类别再依赖上传失败、禁止用 `directoryId` 缺失阻止用户提交、禁止 catch/默认成功/默认授权掩盖 `CONTROLLED_FILE_ACCESS_DENIED` 或缺失 `UNCLASSIFIED` 目录。
- Evidence: 任务 `doc/tasks/20260728-dcc-upload-controlled-file-access/`；任务 `doc/tasks/20260803-controlled-file-category-missing/`，受控文件提交页按当前文件分类 taxonomy 叶子节点自动解析正式文件类别并清空旧类别/目录/预览状态，未绑定提交目录时后端使用正式 `UNCLASSIFIED / 未分类` 目录，避免 `Controlled file category does not exist` 和提交人手工维护目录绑定；任务 `doc/tasks/20260804-dcc-unclassified-directory-consistency/`，NAS 转移、本地文件夹导入和元数据编辑统一使用正式未分类目录自动落位，旧“请先绑定目录”阻塞只保留在历史任务文档或测试负向断言中；任务 `doc/tasks/20260804-dcc-upload-approval-chain-projection/`，类别列表接口从当前有效审批矩阵路线节点投影 `signoffPositionIds` / `approvalPositionIds`，避免技术调研报告等已配置类别被上传预检误判为审批链路不完整。
- Evidence supplement: 任务 `doc/tasks/20260807-dcc-upload-hide-category-permission-hint/` 只负责移除只读文件类别区域的路径 helper 和橙色 preflight alert，并证明类别值与零写请求边界；后续任务 `doc/tasks/20260807-dcc-upload-permission-at-approval/` 独立负责“上传不限制、审批限制”的正式权限职责。禁止用“页面不显示提示”推断权限仍在上传阶段或已经取消。

## DCC 上传历史文件升版状态门禁

- Trigger: DCC 受控文件上传页、历史文件名称选择、上传升版、同编号现行版本、`revisionTargetControlledFileId`、`current-version`、新建 master、文件编号/版本预检、`Controlled file number conflicts with the existing logical document chain`、版本号 `abc`、过去生效日期。
- Preflight check: 历史文件名称只表示“候选升版”，必须先用同编号现行版本和上传专用升版候选定位当前主档；`REVISION` 与“创建新的 master 主档”必须互斥。当前版本查询错误、编号链冲突、历史文件找不到现行主档、升版目标缺失、版本号格式不合法或同编号流程修改中，都必须进入文件编号/版本阻断态。前端版本号格式必须跟后端 `DccControlledFileVersion.parse` 对齐；若业务允许过去生效日期，预检必须明确说明“允许补录历史生效日期”，不能让用户误以为未校验。
- Blocker: 已选择历史文件但页面仍显示“将创建新的 master 主档”、顶部错误和预检状态不一致、编号链冲突时仍显示“可提交”、英文冲突错误直接弹给用户、版本号 `abc` 能通过前端表单和预检、或过去生效日期没有明确允许/阻断说明时必须停止。
- Verification: 运行 `pnpm --dir IntRuoyiFronted e2e:dcc:upload-optimization:static`、`pnpm --dir IntRuoyiFronted e2e:dcc:upload-current-version:static`、`pnpm --dir IntRuoyiFronted e2e:dcc:upload-project-taxonomy-revision:static`、`pnpm --dir IntRuoyiFronted e2e:dcc:upload-name-version-autofill:static` 和 `pnpm --dir IntRuoyiFronted ts:check`；真实数据验收时还需通过 Playwright 选择目标历史文件，确认目标 DCC 写请求前阻断或提交状态一致。
- Forbidden action: 禁止用隐藏现行版本面板、删除历史文件提示、默认新建 master、吞掉 current-version 错误、前端本地猜测主档、仅提交后报错或只翻译 toast 来掩盖版本链状态不一致。
- Evidence: 任务 `doc/tasks/20260808-dcc-upload-optimization-fixes/`，上传页新增集中式升版阻断、编号链错误中文化、版本格式前端校验、生效日期允许补录提示和未分类允许规则文案。

## DCC 预览不可用原因短路门禁

- Trigger: DCC 受控浏览、受控文件详情预览、统一在线预览、`ProtectedPdfViewer`、`previewUnavailableReason`、`previewOnlineFileWithWatermark`、`previewControlledFileWithWatermark`、`PDF/IMAGE/VIDEO/AUDIO/TEXT/OFFICE/DOWNLOAD_ONLY` 预览类型。
- Preflight check: 先确认预览元数据接口是否可能返回 `previewUnavailableReason`；前端拿到该字段后必须在任何二进制预览请求前短路。Office 可继续交给 OnlyOffice 只读组件展示不可用原因；PDF、图片、视频、音频、文本和下载型文件必须用通用错误区域显示同一精确原因，不能继续请求 preview binary。
- Audit request extension: 统一在线预览新增业务文件来源时，必须核对目标元数据和二进制控制器的审计请求号合同；若后端要求 `X-DCC-Request-Id` 或同等级请求号，前端元数据与二进制请求必须分别显式生成并携带独立请求号，不能假设普通 Axios GET、全局拦截器或 viewer token 会自动补齐。
- Blocker: 元数据已返回 `previewUnavailableReason` 但页面继续调用二进制预览接口、非 Office 类型只显示“受控预览加载失败”等泛化错误、下载型文件用“仅支持下载”覆盖正式不可用原因、或静态合同不能证明短路发生在 `resolvePreviewBlob()` / `previewOnlineFileWithWatermark()` 之前时必须停止。
- Audit request blocker: 真实页面在线查看显示 `audit requestId is required`、元数据请求未发出、元数据成功但二进制因缺请求号失败，或只给其中一个请求补审计号时必须停止；不得移除后端审计门禁、复用固定请求号或在预览失败时降级下载。
- Verification: 新增或更新聚焦静态合同，至少断言 `resolvedPreviewUnavailableReason` 从 metadata 进入 viewer 状态、不可用原因 guard 早于二进制加载、非 Office 类型设置可见错误原因；运行目标静态合同、相邻统一预览合同和 `pnpm ts:check`。
- Forbidden action: 禁止用 catch 泛化错误、隐藏空状态、默认下载、空 Blob、OnlyOffice token fallback、API-only 断言或后端吞异常来掩盖预览产物缺失。
- Evidence: 任务 `doc/tasks/20260803-dcc-preview-all-types-unavailable/`，后端元数据已能给出缺失预览产物原因，但前端旧逻辑只让 Office/DownloadOnly 跳过二进制，导致 PDF/图片/视频/音频/文本继续请求 preview binary 并退化为泛化错误。

## DCC 上传项目代码提示状态门禁

- Trigger: DCC 受控文件上传页、DHF/DMR 类别、产品编号只读自动带出、`dccProjectCodeId`、`productCode`、红色提示误判、项目代码已绑定仍显示错误。
- Preflight check: 先区分“缺少 DCC 项目代码”的阻断状态和“已从所选 DCC 项目自动带出项目代码”的成功状态；DHF/DMR 类别可以保留必选提示，但提示颜色和文案必须跟随 `productCode` 是否已自动绑定切换，不能只由类别必选布尔值决定。
- Blocker: 已选择包含项目代码的 DCC 项目且产品编号已带出时仍显示红色错误、产品编号改回可手填、提示文案替代后端绑定校验、或静态合同不能证明成功/缺失两种状态的颜色边界时必须停止。
- Verification: 运行 `node tests/e2e/dcc-upload-project-code-hint-static.spec.js`、`node tests/e2e/dcc-upload-product-autofill-static.spec.js`、`node tests/e2e/dcc-product-category-rule-static.spec.js` 和 `pnpm ts:check`。
- Forbidden action: 禁止用隐藏提示、删除 DHF/DMR 必选校验、允许员工手填产品编号、默认项目代码、空 `productMasterId` 或只改文案不改状态颜色来掩盖绑定状态。
- Evidence: 任务 `doc/tasks/20260803-dcc-upload-project-code-hint/`，上传页旧实现只按 `isProductRequiredForSelectedCategory` 固定显示红字，导致已自动带出 `IDI` 仍被误读为错误；修正为缺失时红色阻断、已绑定时绿色确认。

## DCC 基础条目关联文档分类树门禁

- Trigger: DCC 基础条目、项目代码、关联文档三栏导航、`fileTypeTaxonomyId`、`fileTypeLevel2/fileTypeLevel3`、中间“文件类型”列、DCC 文件分类树、技术文档阶段展开、未分类文件或“未分类文件类型”自动归类、列表页按文件名批量归类、受控文件元数据修改后项目代码详情同步、`user_role_ids` 角色缓存。
- Preflight check: 先以 `DCC文件分类` 的正式树作为分类来源，按 `技术文档 / 阶段 / 文件类型` 解析阶段直接子分类；基础条目关联文件只能影响数量和右侧文件列表，不能反向决定中间列完整分类集合。若新增“按文件名归类未分类”能力，候选目标也必须来自同一正式分类树阶段直接子分类，并通过正式元数据保存接口写入 `fileTypeTaxonomyId` 与 `fileTypeLevel1/2/3`。若入口位于列表页且用户要求处理全部项目代码，必须按当前筛选条件从第 1 页遍历到总页数，不能只处理当前页已加载行。若真实 E2E 要用非 admin 文控账号写入元数据，除确认 `get-permission-info` 返回角色外，还必须确认后端 `PermissionService.hasAnyRoles` 使用的 `user_role_ids:{userId}` 缓存已刷新；直接补 DB 角色后需删除精确用户缓存或走正式角色分配接口触发缓存清理。
- Blocker: 中间文件类型列只从当前关联文件的 `fileTypeLevel3` 动态生成、已配置但当前无文件的正式子分类不显示、`fileTypeTaxonomyId` 已能解析第三级却被归入“未分类文件类型”、用“未分类文件类型”替代正式子分类、自动归类把文件写回未分类桶、缺正式分类候选时仍猜测目标、保存失败被吞掉、或 metadata PUT 返回 `Only doc control can update controlled file metadata` 且未核对 `user_role_ids:{userId}` 缓存时必须停止。
- Verification: 聚焦静态合同必须断言分类 helper 同时提供阶段、阶段直接子分类和 taxonomy path 第三级解析；页面合同必须断言中间列先由阶段直接子分类预置，再按文件归组计数；自动归类合同必须断言按钮、未分类筛选、正式候选来源、确定性相似度、正式 metadata 更新接口和失败显式暴露；列表页批量归类合同还必须断言保留当前筛选条件、全分页拉取项目代码、逐项目全分页拉取关联文件和批处理进度/失败可见。运行 `pnpm e2e:dcc:project-code-associated-three-column:static`、自动归类静态合同、相邻 DCC 文件分类静态合同和 `pnpm ts:check`。真实写入 E2E 只有在有批准的可写测试数据与清理责任时运行；写入 E2E 必须在每次 metadata PUT 后进入目标项目代码详情三栏，按阶段和文件类型可见文本断言目标文件出现，并最终恢复原项目与原文件类型。
- Forbidden action: 禁止用 `fileTypeLevel3`、当前关联文件列表、默认 `MAIN`、空值回填、`formBindings`、前端硬编码文案或随机算法替代正式 DCC 文件分类树；禁止把无匹配、缺分类树或保存失败静默降级成未分类成功。
- Evidence: 任务 `doc/tasks/20260731-dcc-project-code-associated-taxonomy-types/`，基础条目关联文档中间列旧实现按关联文件已有 `fileTypeLevel3` 生成，未与 DCC 文件分类阶段展开保持一致；任务 `doc/tasks/20260801-dcc-project-code-auto-classify-unclassified/`，未分类自动归类按钮复用正式分类树与 metadata 更新接口；任务 `doc/tasks/20260801-dcc-project-code-list-auto-classify-unclassified/`，列表页批量入口必须覆盖当前筛选条件下全部项目代码，包括未加载分页；任务 `doc/tasks/20260802-dcc-project-code-filetype-assignment-e2e/verification-report.md`，非 admin 文控账号通过真实页面 5 次修改已有文件到目标 DCC 项目代码和不同正式文件类型，并在项目代码详情三栏逐次验证同步，发现直接补 DB 角色后必须刷新 `user_role_ids:{userId}` 缓存。

## 前端草稿保存与提交发布解耦门禁

- Trigger: 受控版本、候选版本、草稿页、审批流对象或发布对象存在“保存草稿”和“提交发布/提交审批”两个动作。
- Preflight check: 普通保存成功处理只能标记已保存、清理本地退出标记或刷新草稿数据；提交发布必须由显式按钮/菜单触发，并保留提交前最新状态复查。
- Blocker: 普通保存成功后弹出“是否立即提交发布/提交审批”确认、调用 submit/publish API、把 DRAFT 推进 PENDING/ACTIVE，或静态合同无法证明保存不触发发布。
- Verification: 新增聚焦静态合同断言保存成功 handler 不调用提交函数，并用 Playwright 拦截保存接口成功响应，断言 submit-publish 请求数为 0 且页面仍停留在 DRAFT 草稿上下文。
- Forbidden action: 禁止把“保存成功顺手提交”当作便捷入口；禁止用 payload 标志、默认 true、隐式 watcher 或成功 toast 后确认框把保存和发布重新耦合。
- Evidence: 任务 `doc/tasks/20260726-route-flow-v15-save-system-exception/`，路线草稿 V15 普通保存后曾继续弹“草稿已保存，是否立即提交发布？”，用户确认后草稿进入审批/发布导致不可继续编辑。

## FormData 可选标量必须同时排除 null 与 undefined

- Trigger: 上传、预检后提交、`FormData.append`、后端可选数字/布尔参数、接口响应将缺省字段规范化为 `null`。
- Preflight check: 可选标量写入 FormData 前必须使用 `value != null` 或等价的双重非空判断；只判断 `!== undefined` 不足以处理接口返回的 `null`。
- Blocker: 请求载荷出现字符串 `"null"`、`"undefined"`，或后端对可选数字参数返回类型转换错误时必须停止并修正序列化边界。
- Verification: 静态合同断言非空门禁；真实上传路径在字段为空和有值两种状态下分别检查请求成功，并确认有值时携带预检冻结值。
- Forbidden action: 禁止后端把字符串 `null` 当缺省值兼容、吞掉参数错误、或用默认 ID 掩盖前端序列化缺陷。
- Evidence: `doc/tasks/20260811-word-route-existing-candidate-governance/verification-report.md`。

## 表单模板三按钮领域边界门禁

- Trigger: 表单中心模板预览区“打开/编辑/填写”、`openSelectedTemplate`、`editSelectedTemplate`、`openSelectedTemplateAction('edit')`、`openSelectedTemplateFill`、`TemplateViewDialog`、`FORMTPL:*` Jimu 保存，或用户要求“表单模板编辑与批记录表单右侧编辑一致”。
- Preflight check: 先区分“交互模式对齐”和“数据领域关联”；表单模板与批记录表单没有直接关系。`打开` 通过当前 `/mdm/form-center/template` 路由 query 切换同页全宽只读工作区，`编辑` 仍停留在 `/mdm/form-center/template`，并用当前模板自己的 `designerReportId`（必须是 `FORMTPL:*`）以 `mode=designer&reportMode=edit&reportId=FORMTPL:*` 进入表单模板 Jimu 编辑器；非草稿版本点击编辑必须先切换到同模板可写草稿。Jimu 原生保存只允许草稿版本落库，并由后端把最新画布同步回当前模板版本正式 `jimuSchemaJson.sheetLayoutJson`。`填写配置` 才维护可填写/不可填写、字段名称、字段类型等规则配置；`填写` 跳转独立 `/mdm/form-center/template/simulate` 页面。三者只使用 `templateId + versionNo + designerReportId + jimuSchemaJson` 等当前模板上下文，不得引入 `batchRecordReportId` 或批记录路由依赖。
- Blocker: 任一按钮仍打开 `TemplateViewDialog`、`form-template-rules-dialog`、`form-template-fill-dialog`，`编辑` 跳到 `/mes/pro/batch-record-form-list`、回退到 `templateMode=edit` 本地规则面板、丢失表单模板内容、缺少 `FORMTPL:*` 报表 ID、进入空白 Jimu 编辑页、Jimu 画布已绘制但外层 loading 遮罩仍可见、iframe 不是 `/jmreport/index/FORMTPL:*`、Jimu 保存后模板详情接口读回的 `sheetLayoutJson` 未更新，或保存覆盖外层填写规则/协助填写/签名配置时必须停止。
- Component isolation: 独立模拟填写路由若复用列表页组件，必须通过显式组件属性标识模拟页面实例，不得只依赖全局 `route.name`；否则路由切换期间旧列表实例和新页面实例会同时响应 watcher，重复加载模板版本。
- Verification: 至少运行 `node tests/e2e/form-template-button-interaction-parity-static.spec.js`、`node tests/e2e/form-template-independent-button-actions-static.spec.js`、`node tests/e2e/form-template-edit-designer-parity-static.spec.js`、`node tests/e2e/form-template-jimu-save-back-static.spec.js`、`node tests/e2e/form-center-static.spec.js` 和 `node tests/e2e/jmreport-designer-edit-row-height-static.spec.js`，并从真实 `/mdm/form-center/template` 页面用 Playwright 点击右侧“编辑”，确认 URL 保持在模板页且包含 `reportMode=edit&reportId=FORMTPL:*`，页面渲染 `/jmreport/index/FORMTPL:*` iframe，Jimu 画布有当前模板内容，外层 `.el-loading-mask` 不可见，iframe 截图非空，不出现 `.batch-record-cell-rules-editor`、无可见弹窗、无绑定错误和无模板编辑写请求；请求审计必须证明模板动作读取 `GET /form-center/templates/{templateId}/versions/{versionNo}`，不得用批记录接口代替；保存回写必须用真实 E2E 在 Jimu 画布临时新增或删除单元格并调用原生保存，通过表单模板正式详情接口确认 `jimuSchemaJson.sheetLayoutJson` 变化后恢复测试改动；`pnpm ts:check` 必须通过或记录明确阻塞。
- Forbidden action: 禁止把 UI/交互相似解释为共享 `reportId` 或跳到批记录模块；禁止用三个弹窗冒充批记录式页面流转；禁止伪造绑定、名称匹配、条件 fallback、跨域路由、前端缓存、直接 SQL、只改 Jimu 报表表、API-only 写模板版本或只隐藏错误提示而保留错误数据契约。
- Evidence: 任务 `doc/tasks/20260727-form-template-button-alignment-design/`、`doc/tasks/20260727-form-template-button-interaction-parity/`、`doc/tasks/20260828-form-template-edit-button-batch-record-designer/`；用户在 2026-07-27 明确澄清实际表单与批记录表单没有直接关系，并在 2026-08-28 继续确认“编辑按钮仍在表单模板页里，但右侧编辑逻辑要和批记录表单一致”。2026-08-29 复验发现 Jimu 内容已加载但外层 loading 遮罩未及时释放，修复后真实 E2E 证明 `/jmreport/index/FORMTPL:54` 有 47 行、222 个文本单元格且外层遮罩为 0；同日保存回写 E2E 在 Jimu 画布临时新增单元格并恢复，证明原生保存会同步模板版本 `jimuSchemaJson.sheetLayoutJson` 且保留外层配置。

## FormCenter 动态表单字段码渲染门禁

- Trigger: eDHR 批次详情、动态表单抽屉、损耗单、过程检验记录、`ActionFormPanel`、`EdhrExecutionTemplateEditableForm`、`FORM_TEMPLATE_VERSION` 单元格链接、`fieldCode`、`fieldIdentityMap`、实例 `form_data_json` 已有值但页面输入框为空。
- Preflight check: 先区分 FormCenter 正式数据键和电子表格坐标；FormCenter 实例草稿必须按模板识别字段 `fieldCode` 保存，例如 `field6`，前端渲染表格时才可用显式 `fieldIdentityMap` 把坐标 `5:3` 映射到 `field6`。打开动态表单时必须同时加载精确模板版本、实例最新 DRAFT 快照和模板规则；模板既缺布局又缺识别字段/规则时必须暴露配置缺失，不得画空壳。
- Blocker: 后端 `form_data_json` 已有 `fieldCode` 值但页面只显示快照 JSON、输入控件按坐标 key 读取导致为空、保存/提交仍使用父级批次元数据覆盖实例草稿、动态表单被误导到传统批记录 `batchRecordReportId` 链路、或静态合同不能证明 `5:3 -> field6` 映射时必须停止。
- Verification: 运行 `node tests/e2e/edhr-dynamic-form-action-panel-prefill-static.spec.js`、`node tests/e2e/form-center-static.spec.js`、`pnpm ts:check`；真实 E2E 必须通过批次详情页面打开动态表单，断言 `task/open` 返回 FormCenter 实例、DB `bpm_form_action_instance.form_data_json[fieldCode]` 和页面输入控件显示同一生产批号，并记录临时规则/待办/实例数据恢复。
- Forbidden action: 禁止把快照 JSON 文本当作表单控件展示通过；禁止同时保存 `fieldCode` 和坐标 key 冒充兼容；禁止用 API-only、直连实例、mock、默认空布局或传统批记录 PASS 替代损耗单/过程检验记录验证。
- Evidence: 任务 `doc/tasks/20260728-edhr-cell-link-main-e2e-repair/verification-report.md`，动态表单实例已落库 `field6`，旧页面因未渲染模板控件和缺少 `fieldIdentityMap` 导致页面为空。

## FormCenter 嵌入模板对象类型契约门禁

- Trigger: `ActionFormPanel`、FormCenter 动态表单嵌入快照、`resolveEmbeddedTemplateVersionForActionForm`、本地构造 `FormTemplateListItemVO`、`updatedTime`、`templateId/templateName/versionNo/status`、或 `pnpm ts:check` 报 `Property 'updatedTime' is missing`。
- Preflight check: 本地构造 FormCenter 模板列表项时，先核对 `src/api/form-center/template.ts` 的正式 `FormTemplateListItemVO` 必填字段；嵌入快照缺少展示型元数据时按契约显式补齐稳定空值，例如 `updatedTime: ''`，不得放宽接口类型。
- Blocker: 对象字面量缺少 `updatedTime` 等必填字段、通过 `as any`/`Partial<FormTemplateListItemVO>`/接口改可选来绕过类型检查、或嵌入模板对象字段不满足正式模板 API 契约时必须停止。
- Verification: 运行 `pnpm ts:check`；涉及动态表单渲染时继续运行 FormCenter/eDHR 相邻静态合同，证明模板布局和字段识别链路未被改写。
- Forbidden action: 禁止为了通过类型检查把正式 API 字段改成可选、禁止用 mock/default success 代替模板元数据、禁止吞掉嵌入快照缺失导致的真实渲染错误。
- Evidence: 任务 `doc/tasks/20260729-edhr-assist-topbar-action-reserve/verification-report.md`，全量类型检查因 `ActionFormPanel.vue` 构造的嵌入模板对象缺少 `updatedTime` 失败，按正式 `FormTemplateListItemVO` 补齐后 `pnpm ts:check` 通过。

## 前端静态合同仓库路径门禁

- Trigger: 前端静态合同读取后端 SQL、发布迁移、任务文档或跨端源码路径，尤其出现 `../ruoyi-vue-pro/sql/mysql`、`ENOENT`、`form-center-static.spec.js` 或旧双仓路径。
- Preflight check: 先用 `rg --files` 在当前 `E:\IntRuoyi` 工作区定位正式源文件；当前仓库后端 SQL 正式路径为 `../IntRuoyiBackend/sql/mysql`（相对 `IntRuoyiFronted`），不得沿用历史 `../ruoyi-vue-pro/sql/mysql`。
- Blocker: 静态合同仍引用旧仓目录、为了通过测试复制 SQL 到旧目录、或创建兼容目录/软链接伪装旧仓存在时必须停止。
- Verification: 运行对应静态合同；若路径修正涉及表单中心，至少运行 `node tests/e2e/form-center-static.spec.js`。
- Forbidden action: 禁止通过新增旧路径副本、吞掉 `ENOENT`、跳过 SQL 断言或改成可选读取来掩盖仓库结构漂移。
- Evidence: 任务 `doc/tasks/20260728-form-template-work-order-cell-link/`，`form-center-static` 曾因读取 `E:\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260717_bpm_form_center.sql` 失败，修正为当前正式路径后通过。

## 前端聚合新增默认分类门禁

- Trigger: 聚合字段编辑器新增子项，且页面摘要、徽标、保存 payload 或状态边框会按子项类型过滤，例如工艺路线表单槽位排除 `MAIN` 批记录槽位。
- Preflight check: 新增子项前先确认本地空对象的默认分类属于当前聚合字段的可统计/可保存范围；若存在排除分类，必须用静态合同锁定新增默认值不得落入排除分类。若产品口径要求“点击新增即计数”，计数 helper 不得再用必填配置项、模板 ID 或保存 payload 过滤掉本地新增行。
- Blocker: 新增项在右侧列表可见但摘要、徽标、状态或保存 payload 仍按旧数量计算，新增空对象默认使用了当前聚合字段明确排除的分类，或点击新增后的本地行因模板尚未选择而被数量 helper 排除。
- Verification: 目标静态合同必须同时断言新增空对象默认分类、计数 helper 的过滤口径，以及相邻状态/布局回归。
- Forbidden action: 禁止只在徽标侧硬加数量、按表单名称猜测业务类型、吞掉保存错误，或把被排除分类作为动态子项默认值。
- Evidence: 任务 `doc/tasks/20260726-route-flow-form-slot-live-count/`，工艺路线“表单槽位”新增第二个动态表单时默认 `MAIN`，导致节点数量仍显示 `1`。任务 `doc/tasks/20260726-route-flow-add-form-click-count/`，新增空行已是非 `MAIN` 但数量 helper 仍要求 `formTemplateId > 0`，导致点击新增后仍显示 `1`。

## 前端隐藏路由顶部页签状态门禁

- Trigger: 详情页或编辑页配置 `noTagsView: true`，同时通过 `meta.activeMenu` 归属现有菜单页签，用户需要从其他顶部页签切回并保留当前详情、编辑对象或 query 状态。
- Preflight check: 先核对 tags view 实际保存的 `path/fullPath/query` 和隐藏路由的 `activeMenu`；进入隐藏路由时必须受控替换现有菜单页签目标并保存原始快照，路由 query 变化时同步当前页签目标，只有显式返回菜单列表时恢复快照。
- Blocker: 切回菜单页签会打开列表、丢失对象 ID/query/子页签状态，或修复会新增第二个同名顶部页签、取消 `noTagsView`、依赖刷新重建状态时必须停止。
- Verification: 聚焦静态合同同时锁定 `noTagsView + activeMenu`、页签目标替换、query 更新和列表快照恢复；真实 Playwright 从隐藏详情/编辑页切到其他应用内页面，再点击原菜单顶部页签返回，断言 URL、对象上下文和目标视图保持。
- Forbidden action: 禁止通过取消隐藏路由、创建重复页签、localStorage 兜底、强制刷新、默认跳列表或吞掉路由错误掩盖状态丢失。
- Evidence: 任务 `doc/tasks/20260727-route-flow-tab-return-state/`，路线流转关系图从顶部页签切走再返回时曾因“工艺流程”页签仍保存 `/mes/pro/route` 而回到路线列表。

## 前端 VueFlow 只读图点击层级门禁

- Trigger: 前端页面使用 `@vue-flow/core`、`VueFlow`、`smoothstep`、`MarkerType.ArrowClosed` 展示只读关系图、页面关系图、流程图、节点跳转图，且节点本身需要点击进入详情、填写页或其它路由。
- Preflight check: 先区分图谱拖拽编辑和只读导航；只读导航图必须关闭不需要的 pane 拖拽，并确认 `.vue-flow__pane`、`.vue-flow__nodes` 不会覆盖节点点击，具体 `.vue-flow__node` 和节点内容仍可接收 pointer events。静态合同需锁定 `:pan-on-drag="false"` 或等价只读行为、节点稳定 `data-*` 选择器、边稳定证据选择器和禁止回退到手绘 SVG path。
- Blocker: Playwright 点击节点时提示 `.vue-flow__pane` 或 `.vue-flow__nodes` intercepts pointer events、节点视觉可见但无法进入目标路由、为了让点击通过而开启假路由/坐标点击/API-only 验证、或只验证节点文本不验证节点真实点击时必须停止。
- Verification: 运行目标静态合同、相邻页面合同、`pnpm ts:check`；真实 Playwright 必须从页面点击至少一个可路由节点并断言 URL 进入目标页面，同时统计写请求边界。
- Forbidden action: 禁止用 `force: true`、坐标点击、隐藏 VueFlow pane、删除节点按钮、API-only 跳转或吞掉路由错误来冒充节点可点击。
- Evidence: 任务 `doc/tasks/20260730-edhr-page-graph-tab/`，批记录页面关系图迁移到 VueFlow 后真实 E2E 首次失败于 `.vue-flow__pane/.vue-flow__nodes` 拦截 `production-fill` 节点点击，修正 pointer-events 后真实路由跳转通过。

## Element Plus 全屏弹框挂载门禁

- Trigger: 页面局部区域使用浏览器 `requestFullscreen()` / `:fullscreen` 做最大化，同时局部区域内按钮会打开 `el-dialog`、`ElMessageBox`、下拉面板、签名框、保存结果框或提交确认框。
- Preflight check: 先确认弹框是否必须在最大化状态可见；若必须可见，弹框组件必须作为 fullscreen 元素子树渲染，并显式设置 `:append-to-body="false"` 或同等非 body teleport 约束；自定义签名/确认弹框也必须给 fullscreen 根节点稳定 `data-*` 标识，并断言弹框 DOM 位于该根节点内部和未 teleport 到 body。
- Blocker: 弹框仍在 fullscreen 容器外、依赖 body 级 overlay、仅通过提高 `z-index` 解决浏览器 fullscreen top layer、或保存/提交按钮在最大化后弹框不可见时必须停止。
- Verification: 运行目标静态合同；有本地运行态和登录前置时，再用 Playwright 点击最大化后触发弹框，断言 `document.fullscreenElement` 是目标根节点、弹框可见且 `root.contains(dialog)`，并用 `elementFromPoint` 命中弹框确认未被覆盖。
- Forbidden action: 禁止用随机加大 `z-index`、退出全屏后再弹框、隐藏错误提示、改成普通 toast、或关闭浏览器 fullscreen 能力来绕过遮挡。
- Evidence: 任务 `doc/tasks/20260729-edhr-fill-workspace-redbox-hide/`，eDHR 填写页最大化后保存/提交弹框原先位于 `.edhr-fill-workspace` 外部，被浏览器全屏层遮挡，修正为在全屏工作区内部渲染并禁用 body teleport；任务 `doc/tasks/20260807-frontline-pqc-fullscreen-submit-dialog/`，一线PQC自定义提交签名弹框通过全屏根节点后代关系和 hit-test 验证避免被覆盖；任务 `doc/tasks/20260808-frontline-fullscreen-submit-confirm/`，一线生产正式提交确认从全局 `message.confirm` 改为 fullscreen root 内部自定义确认层，静态合同禁止 body-mounted MessageBox。
- Inline error extension: 浏览器全屏内需要持续可见的页面级错误不得只使用挂载到 body 的 `message.error`；应在 fullscreen 根节点子树内预留固定 `role="alert"` 错误区，由页面统一保存并显示本地校验、签名失败、加载失败和提交失败的真实原因。页面拥有错误归属时，对应 API wrapper 必须设置 `ignoreErrorMessage: true` 避免全局重复提示；原生命令事件显示错误后必须终止当前操作，不得继续写请求或 rethrow 为未处理事件。验证需分别在各业务模式的真实最大化页面触发缺前置错误，断言错误区可见、`document.fullscreenElement.contains(errorSlot)`、body 级错误提示为 0、正式写请求为 0，并人工核对错误区位于指定主面板内。Evidence: 任务 `doc/tasks/20260817-frontline-fullscreen-error-zone/`，一线生产未填完成数量与一线PQC前置未完成均在最大化左侧固定区域显示真实原因；任务 `doc/tasks/20260818-frontline-pqc-system-exception/`，一线PQC订单已出现但工序加载失败时，恢复页面内联错误区和一线接口 `ignoreErrorMessage`，避免顶部重复“系统异常”并保留具体失败原因。

## 动态菜单页签重命名门禁

- Trigger: 用户要求修改动态菜单页面、左侧菜单、顶部页签、页面标题或角色/套餐菜单树中的入口名称。
- Preflight check: 先同时定位 `system_menu.name` 的正式 SQL/迁移来源、页面内标题、真实 E2E 入口等待文本、角色菜单/租户套餐配置脚本；区分页签/入口名称和业务对象文案，避免把导入、弹窗、错误提示等非目标文案一并改名。若入口改名后仍需兼容旧搜索词，`RouterSearch` 必须登记明确别名，并且搜索过滤、历史解析和跳转不得缓存 setup 阶段的 `router.getRoutes()`，必须实时读取登录后的动态路由表。
- Blocker: 只改前端组件标题但未提供正式菜单迁移、只改 SQL 但真实路径脚本仍查旧名称、新增 SQL 缺少 `release-migration` 元数据和依赖门禁、或真实登录态菜单搜索仍基于旧路由快照导致动态菜单搜不到时必须停止。
- Verification: 新增聚焦静态契约同时读取页面标题、菜单迁移、真实路径脚本、搜索别名和动态路由新鲜度；运行角色/租户菜单相关静态契约；对新增菜单 SQL 使用目标 SQL + 依赖迁移执行 `run-release-migration-policy-gate.py --sql-file ...`；有本地运行态时用真实登录用户搜索关键词，断言命中新入口，并核对侧边栏、顶部页签或面包屑中的至少一个真实可见标题。`doc-alert` 可能被全局配置隐藏，不能作为唯一页面标题判据；Element Plus 菜单可能保留隐藏重复 DOM，Playwright 必须选择可见菜单节点。
- Forbidden action: 禁止用硬编码前端标题掩盖动态路由仍返回旧菜单名；禁止为了“统一”扩大修改权限按钮、导入导出、业务对象或跨模块选择文案。
- Evidence: 任务 `doc/tasks/20260728-rename-product-master-tab/`，产品主数据页签改为“展厅主数据”时需同步 `system_menu` 迁移、页面标题、角色菜单和租户套餐真实路径脚本；任务 `doc/tasks/20260730-standard-template-list-search-alias/`，标准模板列表兼容旧关键词 MES工序 时，`RouterSearch` 不能缓存登录前路由快照。

## 前端 Route Query ID 比较门禁

- Trigger: 前端用 `route.query` 中的 `id`、`userId`、`assistUserId`、`workTaskId`、`batchTaskId` 等标识判断当前项、高亮项、上下文 key、可编辑态或请求 payload，尤其字段来自 Element/Vue Router query 字符串但业务对象字段是 number。
- Preflight check: 先确认 query 解析函数返回类型；若 query ID 会参与对象 ID 比较，必须使用 `sameRouteQueryId(...)`、统一字符串化，或显式转成同一数值类型，不得直接用 `===` 比较 query 字符串和 number。
- Blocker: 切换对象后 URL query 已变化但页面 active 高亮、表单上下文、缓存 key 或可点击态仍停留在当前登录人/旧对象，或静态合同无法证明字符串/数字 ID 比较一致时必须停止。
- Verification: 聚焦静态合同必须覆盖“请求带所选 ID”“路由保存后端确认 ID”“active/highlight 用 route-id 语义比较”；真实 E2E 需在切换后重开弹窗或返回页面，断言高亮/上下文跟随所选 query ID。
- Forbidden action: 禁止用当前登录人、旧缓存 key、宽松 fallback、刷新页面或隐藏高亮状态掩盖 route query ID 类型不一致。
- Evidence: 任务 `doc/tasks/20260728-switch-filler-wangxin-e2e/`，`assistUserId` 从 route query 读取为字符串，旧 active 判断与数字 `item.userId` 严格等于，导致切换到任丹后重开弹窗不高亮。

## eDHR 辅助模式当前工序 assistRows 路由门禁

- Trigger: eDHR 填写页“填写辅助模式”、工作任务“处理”、批次详情打开填写、工序切换、填写人切换、`task/open`、`executionPageQuery.assistRows`、`assistGridRowCount`、`assistGridColumnCount`、`ASSIST_GRID_U`、辅助表格预览、辅助网格空列和填写页布局不一致。
- Preflight check: 先确认当前入口是否经过正式 `openTask`；进入 `/mes/pro/feedback/edhr-execution/form` 的路由 query 必须把后端返回的当前工序 `assistRows` 显式 JSON 序列化。执行页只按填写配置实际生成的 `ASSIST_GRID_U<userId>_R<row>_C<column>` 或正式 `ASSIST_GRID_USERS<id>/ASSIST_GRID_ROLE<id>` rowKey 恢复辅助表格行列，不得推断其它 rowKey 协议；当未开始任务通过 `task/preview` 返回完整快照时，必须先按当前选定填写人的正式责任主体筛选 assistRows，不能把多个填写人的相同行列坐标合并进同一 CSS Grid；角色主体缺少当前填写人的正式责任范围时必须 fail fast；筛选后同一网格坐标映射不同正式字段时必须报告配置冲突并停止构造字段，同一字段的重复引用只能构造一次，禁止用 `z-index`、层级或遮挡掩盖重叠。批次详情只读辅助预览还必须优先使用运行快照中的正式 `assistGridRowCount/assistGridColumnCount` 展开完整空白边界，不能只按已映射格最大坐标推断预览尺寸。运行页展示层可用 `assistGridVisibleColumnIndexes` 仅压缩未映射空列，但不得改写原始 rowKey、原始行列和位置说明。工序切换列表必须来自当前批次全部普通工序任务，列表展示不得按 `available/allowedActions/activeWorkTaskId` 过滤；点击可打开工序才走正式 `openTask`，已有 `executionId` 但不可打开的工序走只读执行页，尚未产生 `executionId/workTaskId` 的未开始工序必须仍留在 `/mes/pro/feedback/edhr-execution/form`，携带 `batchTaskPreview=1 + batchExecutionId + batchTaskId` 并通过正式 `task/preview` 加载只读查看上下文。
- Blocker: `assistRows` 作为对象数组直接展开进 route query、进入执行页后解析为空或 `[object Object]`、辅助表格 rowKey 被扁平化为字段列表、粗洗等当前工序显示成其它工序/默认字段、静态合同不能证明批次详情和执行页切换链路都保留当前工序 `assistRows`、批次详情运行快照缺少正式辅助表格尺寸却宣称与配置页 `12 × 9` 等尺寸一致、工序切换只展示可打开任务、未开始工序点击时报 `缺少可查看执行记录或工作任务`、或未开始工序切换离开执行页跳到批次详情时必须停止。
- Verification: 聚焦静态合同必须覆盖 `stringifyEdhrExecutionPageQuery`、批次详情和执行页切换调用、`parseAssistGridRowKey`、`edhr-fill-workspace__assist-grid`、`data-assist-grid-cell`、`resolveAssistFieldGridStyle(field)`；批次详情辅助预览必须覆盖 `assistGridRowCount/assistGridColumnCount` 从配置保存到运行快照再到只读网格展开；空列压缩必须覆盖 `assistGridVisibleColumnIndexes`、原始列到可见列的映射和禁止按最大原始列号撑开网格；工序切换还必须覆盖全部工序分组、状态背景、可打开任务 `openTask`、已有执行记录只读打开、无执行记录时执行页内 `batchTaskPreview=1` 与 `task/preview` 只读加载。真实 E2E 需用任务自有粗洗工序待办从页面按钮打开填写页，并断言辅助模式格子布局与配置预览一致。
- Forbidden action: 禁止用 `formBindings`、默认 `MAIN`、当前登录人、正式批记录字段、快照全量字段、空布局、宽松 rowKey 兼容、前端文案或伪造 `OPEN_FORM` 替代当前工序 `assistRows` 与正式工序入口。
- Evidence: 任务 `doc/tasks/20260729-edhr-assist-mode-process-form-mismatch/`，粗洗工序截图中配置预览是辅助表格，但填写页旧实现因 `assistRows` 未显式序列化且未恢复网格，显示为扁平字段列表；任务 `doc/tasks/20260729-edhr-process-switch-all-statuses/`，工序任务 `7169` 无执行记录/工作任务时旧实现直接报错，后续任务 `doc/tasks/20260729-edhr-process-switch-stay-fill-page/` 将未开始工序切换修正为留在执行页并用 `batchTaskPreview=1` + `task/preview` 只读查看；任务 `doc/tasks/20260729-edhr-assist-grid-compress-empty-columns/`，运行页旧实现按最大原始列号撑开 CSS Grid，导致未映射空列在左侧占宽，修正为只压缩展示层空列并保留原始行列来源。

## eDHR 产品信息虚拟 80 工序门禁

- Trigger: 批次执行详情、`BatchExecutionDetailPage.vue`、填写页 `ExecutionPage.vue`、左侧工序列表、右侧当前工序表单卡片、切换工序、切换填写人、产品信息表、`batchRecordSort=80`、后端任务保留来源 `routeProcessId`。
- Preflight check: 先区分“任务来源工序”和“页面显示工序”：产品信息成员任务可以保留源正式批记录绑定的 `routeProcessId/routeProcessSort/processName` 作为追溯来源，但批次详情和填写页都必须按 `MAIN + BATCH_RECORD + 产品信息/80` 识别成独立虚拟 `80 产品信息` 工序组；`processTaskGroups`、填写页工序切换分组和填写人候选范围不得只按 `routeProcessId || routeProcessSort || id` 合并所有任务。填写页“切换工序”“顶部当前工序标签”和“切换填写人”必须先按路由 `batchTaskId` 识别当前任务，再复用同一显示工序名称和 group key；顶部标签不得直接显示产品信息任务的来源 `processName`。
- Blocker: 产品信息任务仍显示在第 1 工序或任一来源工序右侧、左侧或“切换工序”缺少独立产品信息卡片、产品信息卡片混入来源工序任务、切换到产品信息后顶部“工序”仍显示粗洗等来源名称、粗洗“切换填写人”包含产品信息任务填写人、产品信息“切换填写人”包含粗洗或其它来源工序填写人，或静态合同不能证明产品信息专用 group key 与当前任务显示名称时必须停止。
- Verification: 运行 `node tests/e2e/edhr-batch-product-info-virtual-process-static.spec.js` 和 `node tests/e2e/edhr-assist-product-info-virtual-process-static.spec.js`；真实 E2E 需分别验证批次详情和填写页。填写页需打开“切换工序”，断言来源工序与“产品信息”各自只有一张独立卡片，点击产品信息后断言顶部“工序”为“产品信息”且该卡片为当前项，再打开“切换填写人”断言候选任务 ID 不跨显示工序分组，并记录无 MES 写请求、无 console error。
- Forbidden action: 禁止为了页面显示把后端来源 `routeProcessId/processName` 改成虚拟值、禁止用 `formBindings`/表单槽位/当前登录人推导产品信息、禁止隐藏第 1 工序卡片或硬插普通文本冒充 80 工序、禁止只修卡片分组而让顶部标签继续读取来源工序、禁止只修批次详情而保留填写页按来源工序分组、禁止 API-only 代替页面分组验证。
- Evidence: `doc/tasks/20260728-batch-execution-product-info-form-missing/verification-report.md`，产品信息任务后端 `batchRecordSort=80` 但 `routeProcessSort=1`，批次详情需独立虚拟分组；`doc/tasks/20260729-edhr-process-switch-product-info-virtual-process/verification-report.md`，填写页切换工序和填写人需复用同一产品信息虚拟分组边界；`doc/tasks/20260729-edhr-product-info-current-process-label/verification-report.md`，顶部当前工序标签必须按当前 `batchTaskId` 使用虚拟工序显示名称，不能直接读取来源 `processName`。

## eDHR 当前工序运行态展示门禁

- Trigger: 批次执行详情、`BatchExecutionDetailPage.vue`、左侧工序列表、批记录管理员、当前工序黄色背景、开始节点并行第一组、`WAITING`/待打开、`available`、`currentProcessRouteProcessId/currentProcessCode/currentProcessName`、`OPEN_FORM`、`canOpenTask`、`is-in-progress`。
- Preflight check: 先区分“运行态展示”和“填写操作权限”：当前工序高亮必须优先使用详情接口任务级 `available === true` 展示所有当前可执行工序组；开始节点直接后继存在并行第一组时，这一组只要后端任务门禁为可执行就都应显示黄色运行态。单值 `currentProcessRouteProcessId/currentProcessCode/currentProcessName` 只能作为兼容性补充，不得作为唯一展示来源；`WAITING` 当前工序也可显示黄色运行态，但打开填写仍只能由任务自身 `allowedActions` 是否包含 `OPEN_FORM` 决定。产品信息虚拟 `80` 工序必须先排除，避免复用来源正式工序身份造成误高亮。
- Blocker: 批记录管理员只读详情页看不到当前 `WAITING` 工序黄色运行态、开始节点并行第一组只标黄排序第一工序、状态展示依赖 `activeWorkTaskId`/`OPEN_FORM`/当前登录人是否为填写人、通过角色 ID 或填写人列表推断当前工序、或缺少静态合同证明展示权限未提升填写权限时必须停止。
- Verification: 运行 `node tests/e2e/edhr-batch-parallel-current-process-highlight-static.spec.js`、`node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js`、`node tests/e2e/edhr-batch-process-state-background-static.spec.js` 和 `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js`；真实 E2E 需用批记录管理员账号从批次执行列表进入详情，断言开始节点并行第一组当前可执行工序均显示黄色运行态、表单只读可见且无 MES 写请求。
- Forbidden action: 禁止为了解决高亮而放宽 `OPEN_FORM`、接管、跳过或提交权限；禁止用当前登录人、角色名、表单槽位、默认首个 `WAITING` 节点或前端文案推断当前工序；禁止把全部待打开工序统一标黄。
- Evidence: `doc/tasks/20260728-edhr-admin-current-process-highlight/verification-report.md`，批记录管理员只读当前工序通过详情接口 `currentProcess*` 投影为黄色运行态，填写动作仍受 `OPEN_FORM` 控制；`doc/tasks/20260729-edhr-parallel-start-process-highlight/verification-report.md`，开始节点并行第一组三个 `available=true` 工序在真实页面均为黄色运行态。

## 切换填写人 FormCenter 槽位导航门禁

- Trigger: eDHR 执行页“切换填写人”选择损耗单、过程检验单、参数记录表等 `formCenterInstanceId/formTemplateId` 表单槽位候选，尤其同一工序同时存在主批记录表单和 FormCenter 表单槽位，或切换后出现 `/form-center/templates/{id}/versions/{versionNo}` 业务 `403 没有该操作权限`、`请求地址不存在`。
- Preflight check: 先区分传统批记录任务和 FormCenter 表单槽位任务；FormCenter 候选必须先调用正式 `openTask` 校验所选 `assistUserId`，随后跳转批次详情并携带 `openRouteForm=1 + batchTaskId + workTaskId + assistUserId`，由详情页表单抽屉承载。抽屉渲染必须优先使用 `openTask` 返回的运行态模板快照，例如 `formTemplateJimuSchemaJson`、`formTemplateRecognizedFields`、模板元数据和实例草稿，不得把模板管理查询权限作为填写人运行态前置。
- Blocker: 切换填写人导航在检测 `formCenterInstanceId/formTemplateId` 前先要求 `executionId`、跳到 `/edhr-execution/form`、二次自动打开丢失 `assistUserId`、动态表单抽屉必须调用模板管理接口才能渲染、普通填写人因缺 `form:template:query` 权限看到空表单、403 或“请求地址不存在”、或出现“eDHR 批次缺少唯一批记录路线”时必须停止。
- Verification: 聚焦静态合同必须覆盖“FormCenter 分支先于 executionId guard”“跳转批次详情 openRouteForm=1”“详情页二次 openTask 透传 assistUserId”“ActionFormPanel 使用 openTask 嵌入模板快照渲染”，并复跑相邻切换填写人、损耗单打开和 FormCenter 动态表单合同。
- Forbidden action: 禁止把 FormCenter 槽位伪装成传统批记录 execution、禁止清空 `assistUserId` 让当前登录人代替所选填写人、禁止隐藏后端错误、禁止给普通填写人补模板管理权限来掩盖运行态契约缺失，或用刷新页面绕过。
- Evidence: 任务 `doc/tasks/20260728-edhr-scrap-assist-switch/`，选择“张可莹 / 损耗单”曾被传统执行页 `executionId` 要求和批记录路线校验拦住；任务 `doc/tasks/20260728-switch-filler-extra-form-candidates/`，真实 wangxin 路径证明附加表单候选可打开后，动态表单抽屉仍因依赖模板管理查询返回 403，需改用 `openTask` 运行态模板快照。

## 前端写入成功与列表刷新失败分层门禁

- Trigger: 新增、修改、删除成功后立即重新读取列表、详情或统一配置，正式提交/电子签名提交响应不确定，且用户可能在失败提示后重试写入；或同一设备页面需要连续创建多条不同员工、工序或业务上下文的独立正式记录。
- Inline error state: 页面拥有固定的内嵌错误提示时，正式写入成功分支必须在成功状态落地后清除本次会话残留的旧错误提示；失败分支和响应不确定分支必须继续保留真实错误或不确定提示，不能用成功后的清理逻辑覆盖失败信息。
- Preflight check: 写请求和后续刷新必须分层处理；写请求返回成功后立即记录已提交状态，并清理新增草稿或阻止同一次提交被原样重放。若业务正式允许连续创建多条独立记录，明确成功后应结束本次填写会话，清空本次业务输入、轮换幂等键，再恢复人员/工序选择和提交入口；失败或响应不确定时必须保留本次输入和原幂等键，不能把重试冒充成下一次独立提交。删除可见成功/失败回执块或要求提交后继续操作时，必须同步移除对应的隐藏回执锁定状态，不能让不可见状态继续禁用重填、提交、工序或人员入口。PQC 按任务身份提交成功后，必须同时同步当前工序、工序候选列表和同一活跃订单的 PQC 工序缓存；再次打开或重新选择工序必须重新读取/使用已更新任务状态，禁止继续用提交前缓存的旧 `pqcTaskId` 发起人员切换或提交。客户端幂等键还必须显式遵守后端持久化字段或接口契约的长度预算：优先使用固定短前缀加不可预测的会话 token，不得拼接路线、工序、工作站、员工等冗长展示标签；构造后在发请求前校验长度并 fail fast，不得依赖数据库截断。后续刷新失败时必须提示“写入已成功，但列表刷新失败”及正式错误，保留弹框业务上下文但不得把已提交写入误报为保存失败。若写请求已发出但前端收到网络异常、超时或响应不确定，必须先按稳定业务 ID 做只读回执/状态确认：已提交则按产品契约锁定本次会话或按成功处理并开始新的独立会话；未提交才显示原始错误并允许用户主动重试；确认本身失败时进入明确的不确定锁定态并提示人工确认。若页面为避免刷新首帧闪回旧默认值而维护浏览器快照，快照必须带版本并按当前登录租户隔离；正式写成功后先同步当前 UI 与快照再提示成功，setup 阶段同步水合，正式 GET 成功后按权威结果重建并校准快照，GET 失败仍进入正式错误态，快照不得变成读取失败 fallback。若写入动作会生成新的业务对象或测试对象，成功后详情、进度和后续引导必须锚定响应里的新对象稳定 ID 重新读取，不能让用户继续停留在模板对象、来源对象或旧列表行上误判为空。
- Blocker: POST/PUT 已成功但 GET 失败后仍保留新增草稿、提示“保存失败”并允许原样重试；连续提交页面明确成功后复用原幂等键、保留上一笔业务输入或仍被永久成功态锁定；PQC 提交成功后再次打开仍从旧 `processOptions` 或 `pqcProcessOptionsCache` 取提交前任务并触发 `task=null`、任务身份不一致或重复提交；DELETE 已成功却先提示成功再被外层 catch 提示删除失败；保存成功后缓存仍保留旧值导致硬刷新首帧闪回，或正式 GET 失败后继续显示缓存并冒充成功；正式提交缺少稳定业务 ID、只读回执查询或不确定锁定态，导致无法区分“服务端已提交但前端丢响应”和“确实未提交”；或代码无法区分写失败、写成功后刷新失败、下一次独立提交与响应不确定时必须停止。
- Verification: 聚焦静态合同锁定“写成功状态处理先于刷新”、两类错误文案、响应不确定时的只读回执 API/恢复函数/按钮锁定态；连续提交场景还必须锁定“复位只发生在成功响应之后”“下一次使用新幂等键”“失败/finally 不清空输入、不轮换幂等键”“成功结束后人员/工序入口恢复”“隐藏回执状态不会继续禁用入口”“PQC 提交后同步当前工序、工序候选和活跃订单工序缓存失效”和“幂等键长度预算及请求前越界阻断”。真实连续提交 E2E 必须记录每轮幂等键长度和唯一性，并核对后端正式记录数与回执映射。首帧快照场景还要锁定租户/版本 key、setup 同步水合、保存成功后同步快照、正式 GET 后校准和 GET 失败不使用缓存；真实 E2E 用 DOM 观察记录新值是否在旧默认值之前出现，并核对保存后、刷新后快照与正式响应一致。刷新失败或响应不确定场景若无正式可控环境，只记录静态分层合同，不得用 mock 成功冒充真实 E2E。
- Forbidden action: 禁止把写入和刷新放在同一通用失败分支、吞掉刷新异常、失败后本地伪造新行、使用全局未隔离缓存、把缓存升级为权威数据源、在 `finally` 中无条件清空草稿/轮换幂等键，或用保留新增草稿/重复点击让用户重复写入作为刷新或响应丢失补偿；也禁止仅解除按钮锁定却复用上一笔幂等键和业务输入冒充连续报工。
- Evidence: `doc/tasks/20260807-team-leader-loss-maintenance-dialog/`，损耗 POST/PUT/DELETE 与统一列表刷新分层，避免写成功后因刷新失败重复新增或误报删除失败；`doc/tasks/20260807-pqc-submit-uncertain-recovery/`，PQC 正式提交响应不确定后按 `pqcTaskId` 只读查询提交回执，已提交则恢复回执并锁定，确认失败则进入不确定锁定态；`doc/tasks/20260809-frontline-repeat-submit-reset/`，一线生产明确成功后清空本次填写并轮换幂等键，失败保留原会话，支持多人、多工序连续独立提交；真实四连提进一步证明幂等键需按后端 `varchar(128)` 预算构造，固定短前缀加 draft token 的四个键均为 45 字符且互不重复；`doc/tasks/20260819-frontline-pqc-continuous-submit/`，一线PQC删除红框回执后同步删除内部 `pqcSubmitReceipt` 锁，明确成功或只读确认已提交时进入下一次提交，只有确认失败才锁不确定态；`doc/tasks/20260820-frontline-pqc-reopen-after-submit/`，一线 PQC 提交成功后同步当前工序、工序候选列表并让同一活跃订单 PQC 工序缓存失效，防止再次打开复用旧 `pqcTaskId` 报 `task=null`；`doc/tasks/production-team-save-persistence-20260809/`，批记录测试描述保存后同步租户隔离首帧快照，正式读取随后校准且失败时不使用缓存降级。

## 前端提交前严格验证与草稿态计算隔离门禁

- Trigger: 页面同时存在草稿/预览结果计算和正式提交前置断言，例如 PQC `全部合格`、批量赋值、样本数量精确校验、`assert*ForSubmit`、`get*ForSubmit`、watch/computed 自动重算和提交按钮；或正式提交载荷含后端必填结构字段，如设备必填 PQC 项目的 `itemResults.*.selectedEquipmentId`。
- Preflight check: 严格提交断言只能放在显式提交 handler 或提交 preflight 内；草稿态、预览态、watch/computed、批量赋值中的结果计算只能读取当前已建立的表单状态，不得调用 submit-only 精确断言，也不得在用户补齐中间态时抛出提交级错误。正式提交结构字段必须在打开签名弹框和发出写请求前逐项校验，前端可选状态不得弱化后端正式必填字段；PQC 设备字段必须按发布 QA 项目的 `equipmentRequired` 与正式设备选项分支校验，一线 PQC 检验方法详情区有正式设备选项时必须显示“检验设备/设备编号”卡片，无正式设备选项时不得显示这两张卡片或“无需设备”占位；一线 PQC 检验类型卡片必须从当前工序正式 `pqcTaskOptions` 去重渲染，缺少 `FIRST` 或 `PATROL` 时不得留下 disabled 首检/巡检卡片。同一工序存在多个检验方法/任务时，提交粒度必须是当前工序全部可执行检验方法：提交前先保存当前方法草稿，再按正式任务身份逐方法构建独立 payload，每个方法使用自己的 `pqcTaskId/regulationVersionId/qaProcessId/inspectionItems`、数量、损耗、不良说明、逐件值和设备选择；未切换过的合格/不合格检验方法也必须在提交链路按任务计划数量物化默认 `合格` 样本，不能让原始空数组进入严格数量校验或 payload；最终正式 payload 的 `itemResults`、`rawPayload.pqcPieceValues` 和 `rawPayload.pqcItemDetails` 必须复用 exact 样本数组，禁止保留 relaxed/filter 样本路径把空数组过滤后再交给后端。正式任务筛选还必须保留失败原因，至少区分任务未生成、汇总/明细缺失、没有 `PENDING` 状态和 `PENDING` 任务必要快照字段无效；页面空态与提交拦截必须复用同一诊断结果。
- Production device default extension: 一线生产当前工序存在正式设备时，前端进入或刷新运行配置后必须默认选中第一台可见设备，使参数填写和 `selectedDevices[]` 身份从首屏开始明确；物料草稿恢复、工序/物料切换和运行配置刷新都必须复用同一默认选择归一化逻辑，避免空 `selectedDeviceKeys` 覆盖默认选择；已有有效选择不得被刷新覆盖，多选设备仍允许继续手动勾选或取消，无设备工序必须清空选择并显示无设备状态，禁止伪造设备。设备卡片已选状态必须在数据状态和可见勾选符号上同时成立，不能只靠 active 背景让操作者猜测是否已选择。
- Blocker: 点击批量操作、单元格选择或中间态重算时触发 submit-only 断言、页面出现 `Unhandled error during execution of native event handler`、提交按钮外的 watcher 阻断继续填写、正式写请求因本地可预检字段缺失才被后端拒绝、无设备 PQC 项目仍提示设备必填、有设备 PQC 项目不显示“检验设备/设备编号”卡片、无设备 PQC 项目显示“无需检验设备/无需设备编号”占位卡、无正式 `FIRST` 任务的工序仍显示首检卡片、同工序多检验方法提交时只提交当前选中方法或复用当前方法草稿/逐件值/PQC任务身份、PENDING 任务因字段缺失被过滤后只显示笼统“暂无任务”，或静态合同无法证明 submit-only 函数只从提交链路调用、类型卡片只从正式任务选项渲染、全方法提交不是逐任务独立 payload 时必须停止。
- Verification: 聚焦静态合同必须同时覆盖“草稿计算不调用 submit-only 函数”“提交 handler 仍调用严格断言”“正式必填结构字段在签名前校验”“无设备 PQC 项目不强制设备字段”“有设备项目显示检验设备/设备编号卡片、无设备项目隐藏卡片且无占位文案”“检验类型卡片从当前工序正式 `pqcTaskOptions` 动态渲染且无 disabled 占位卡”“当前工序全部检验方法逐任务构建提交 payload，禁止只读取 active task”“最终 payload 三处样本字段均使用 exact task samples，禁止 relaxed helper 残留”，以及任务未生成、明细不一致、非 PENDING 状态、PENDING 字段无效和完整 PENDING 放行五类任务诊断；真实写入 E2E 需覆盖中间态批量赋值、多检验方法最终提交、无 pageerror、提交前无目标写请求、提交后 DB 按每个检验方法生成独立正式记录。
- Forbidden action: 禁止用禁用按钮、吞异常、默认合格、减少样本数、API-only 写入、前端可选状态、签名后再报错、把所有任务过滤失败压缩成单一布尔空态，或把严格断言从提交链路移除来绕过页面中间态崩溃和正式载荷缺字段。
- Evidence: 任务 `doc/tasks/20260807-frontline-pqc-formal-submit-write-e2e/`，PQC 草稿结果计算曾在“全部合格”过程中调用 submit-only 样本数量断言，导致正式提交前页面崩溃；修正为草稿读取当前选择值，提交时仍执行严格样本断言。任务 `doc/tasks/fix-selected-equipment-id/`，一线提交曾让 `itemResults.CODX-AO5-QA-FINAL.selectedEquipmentId` 缺失进入后端，修正为签名前逐项校验设备身份。任务 `doc/tasks/20260808-pqc-optional-equipment-items/`，QA 规程项目 `equipmentRequired=false` 且无设备选项时，一线 PQC 不应强制设备选择。任务 `doc/tasks/20260808-pqc-hide-equipment-cards/`，用户最终确认有设备检验方法需要显示“检验设备”和“设备编号”卡片，无设备检验方法隐藏这两张卡片且不显示“无需设备”占位。任务 `doc/tasks/20260808-frontline-pqc-hide-first-inspection-card/`，无正式 `FIRST` PQC 任务的工序不显示首检卡片，类型卡片改由当前工序正式 `pqcTaskOptions` 动态渲染。任务 `doc/tasks/20260817-frontline-pqc-specific-task-error/`，页面任务过滤曾只保留可执行布尔值，改为由空态和提交拦截共用正式任务失败诊断。任务 `doc/tasks/20260820-frontline-pqc-submit-all-methods/`，同一工序存在“外观/撤压/无跳压”等多个检验方法时，提交按钮必须一次提交当前工序全部方法，并按任务身份隔离每套数据。任务 `doc/tasks/20260820-frontline-pqc-default-sample-values/`，未切换的“撤压/无跳压”等合格/不合格检验方法提交前必须物化计划数量的默认 `合格` 样本，避免出现样本数量 0 与任务计划数量不一致。任务 `doc/tasks/20260820-frontline-pqc-default-sample-values-recheck/` 进一步锁定最终 payload 必须使用 exact 样本数组，删除 relaxed 样本 helper，避免样本被过滤为空后才由后端报数量不一致。

- Snapshot submit extension: 一线生产最大化或其它快照切换场景的正式提交，提交 preflight 必须比较当前所选生产工序、运行配置快照、正式提交上下文和所选员工身份；服务端还必须在最大化 GET 时签发带有效期的快照编号与校验值，提交载荷必须原样携带，后端按当前租户和登录账号读取该快照并校验工序、工作站、员工、模板、设备、设备参数和损耗原因。提交阶段不得重新拉取 runtime-config、重新调用员工切换或实时读取上述配置来替代快照校验；快照缺失、过期、篡改或身份不一致必须明确拒绝。聚焦合同需同时断言前端携带服务端快照编号/校验值、后端授权不调用实时工序/员工/模板解析，并用单元测试证明参数与损耗原因校验不访问 Mapper。禁止只做前端对象比较却把后端继续实时查询称为“按快照提交”。Evidence: 任务 `doc/tasks/20260812-frontline-snapshot-submit-validation/`、`doc/tasks/20260812-frontline-fullscreen-first-switch-prewarm/`。

## 动态菜单真实可见性缓存门禁

- Trigger: 新增或调整动态菜单、隐藏静态路由、角色菜单绑定、租户套餐菜单、菜单排序、外部工具 iframe 入口，或用户反馈“admin 看不到新页签/菜单”。
- Preflight check: 先确认本机后端 health 可用，再用目标租户/账号真实登录态读取 `get-permission-info`，核对目标菜单的 `id/name/path/component/componentName/visible/parentId` 和父级路径；列表通过动态隐藏路由进入详情、历史或编辑页时，角色菜单必须同时绑定每个目标隐藏路由，不能只绑定可见主页并假设相同 permission 会自动生成其它路由；若菜单承载外部助手，还要核对目标路由使用正式 iframe 承载页、助手地址来自约定环境配置、助手服务未占用主系统前端端口，并且前端先申请 ERP 短期票据后再通过助手 `/auth/callback` 进入，不能直接把 iframe 指向助手首页；随后用 fresh Playwright 登录验证侧边栏实际菜单文本、目标页面锚点和 iframe 地址。
- 外部助手离线状态扩展：真实页面还必须覆盖助手已运行直接进入、助手未运行显示启动按钮、点击启动后进入以及启动配置缺失的明确错误；未运行时不得把浏览器的 `127.0.0.1` 连接拒绝内容当作页面业务状态。
- Blocker: 后端端口不可达、权限响应不含目标菜单、侧边栏缺目标菜单、主页可见但点击详情因隐藏路由未绑定而进入 404、目标页面只能直达但侧边栏无入口、目标 route/component/componentName 指向旧业务页、外部助手服务占用主系统前端端口、iframe 直接指向助手首页、助手未配置 ERP 校验地址、助手直连仍展示业务功能，或现有浏览器会话仍使用旧 `roleRouters` 却被误判为代码缺失时必须停止并分层归因。
- Verification: 证据必须同时包含后端 health、权限响应目标菜单链、fresh 登录侧边栏菜单列表、目标页 URL/DOM 锚点、`consoleErrors=[]` 和 `pageErrors=[]`；外部工具入口还必须记录票据请求、iframe 实际 URL/加载结果、助手直连 403 和伪造票据 403，且断言页面不包含旧业务文案。若仅旧会话不可见，说明需要刷新页面或退出后重新登录以重建前端菜单缓存。
- Forbidden action: 禁止只用 API-only、SQL 查询、隐藏路由存在、直接 URL 可打开或静态合同 PASS 代替侧边栏真实可见；禁止复用相邻业务页面、旧组件、旧权限码或旧文案冒充新入口；禁止让外部助手占用主系统前端端口；禁止只隐藏菜单但不拦截助手直连；禁止清空全库 Redis、硬编码前端入口、切换账号/租户或把旧会话缓存问题写成生产代码未实现。
- Evidence: 任务 `doc/tasks/20260808-edhr-batch-record-test-tab/verification-report.md`，`批记录测试` 菜单后端权限响应已包含 `900440`，fresh Playwright 登录后侧边栏可见并打开目标页；旧会话仍不可见需刷新或重新登录。任务 `doc/tasks/20260829-erp-invoice-print-role-permission/verification-report.md`，`发票凭证打印` 入口必须位于 `ERP 系统 / 财务管理 / 发票凭证打印`，fresh Playwright 证明 admin 可见并打开打印助手 iframe，未授权账号不可见，且页面不再指向“分贝通凭证”。任务 `doc/tasks/20260829-invoice-voucher-print-assistant-auth-gate/verification-report.md`，打印助手入口新增 ERP 短期票据，真实 E2E 证明直连助手 HTTP 403、admin 菜单签票进入、无权限账号不可见。

## 前端行级异步结果归属门禁

- Trigger: 表格每行可启动异步任务、轮询执行结果、展示历史回复或允许多行先后执行，尤其存在“重新测试清空本行、其它行保留、终态后可查看”的交互。
- Preflight check: 历史状态必须按稳定业务身份建表；启动时冻结该行 key、executionId 和本次 run token，轮询写入还要校验独立 poll token。重新启动只清空当前行，处理函数必须同步阻止快速连点，不能只依赖 Vue 下一帧刷新按钮 disabled。修改或删除行后必须丢弃该行旧历史，结果弹窗只能复制所点击行的终态快照。需要对当前 Tab 全量执行时，单行处理函数必须返回一个等待正式终态的 Promise，批量入口对完整 Tab 行集合逐行 `await`；不能调用一个只安排定时器便立即返回的旧处理函数来冒充顺序执行。
- Blocker: 结果只保存在全局弹窗、轮询只校验当前全局 executionId、旧请求可在后续行启动后回写、启动异常仍生成可点击历史、快速连点能创建多个执行批次，或批量循环在上一行尚未终态时启动下一行时必须停止。业务终态 PASS、FAIL、BLOCKED 必须与启动/查询传输异常分开：前者记录历史并继续下一行，后者明确指出失败行并停止后续执行。
- Verification: 聚焦合同必须覆盖清空时序、稳定 key、executionId、run/poll token、终态才置 ready、错误保持不可查看、同步防重入，以及批量循环对可等待单行结果的逐行 `await`；真实 E2E 要在源码与运行态稳定的连续会话中顺序执行至少两行，记录两个不同 executionId，断言 A 运行时仅 A 灰、A 终态绿、B 运行时 A 仍绿、B 终态后两行均绿，并重新打开 A 证明 executionId 与回复未串到 B。批量场景还必须观察 `已完成数/总数` 单调递增，证明非 PASS 业务终态不会中断后续行，且全部终态后每行历史均可查看；热更新会重建组件内存状态，发生热更新后的结果不能作为历史丢失缺陷或通过证据，必须待源码稳定后重跑。
- Forbidden action: 禁止用 localStorage、默认成功、最后一次全局回复、数组行号、弹窗当前 ID、固定延迟、并发 `Promise.all` 或不可等待的递归定时器替代正式逐行归属和终态顺序；禁止吞掉启动/读取错误或在非终态提前启用历史。
- Evidence: 任务 `doc/tasks/20260809-batch-record-test-row-history/verification-report.md`，真实测试租户顺序执行 `132`、`133`，证明两行灰绿状态、弹窗标题、执行编号和 Codex CLI 回复相互隔离。任务 `doc/tasks/20260809-batch-record-tab-test-all/verification-report.md`，生产组长 Tab 的 execution `139..143` 顺序完成，FAIL/BLOCKED 业务终态继续后续行，五行均保留正式 Codex CLI 回复。

## 固定业务参数配置应使用所见即所得专用面板

- Trigger: 某工序、设备或业务模板的参数集合、字段类型、选项、默认值和范围已经由正式需求固定，普通用户反馈通用“参数编码 / 值类型 / 原文标准 / 小数位数”弹窗需要逐条新增且难以操作。
- Preflight check: 先区分固定业务参数和可扩展通用参数；逐项确认专用控件与正式保存 VO 的 `parameterCode/valueType/lowerLimit/targetValue/upperLimit/optionValues/defaultText/decimalScale` 映射。专用面板按正式参数编码回显已有配置，非目标工序或设备继续使用通用入口。同一设备编码可能被多个工序复用时，运行态展示范围必须由正式 `routeProcessId/processId` 设备参数规则决定，不能只用 `deviceCode` 判断。已有路线可能仍持久化旧 `TEXT_STANDARD` 规则时，必须在实施前只读查询当前活跃路线工序的正式规则，并把数据迁移纳入同一任务。
- Blocker: 专用面板仍要求用户维护技术字段、固定参数不能一次完整展示和保存、既有正式配置无法按参数编码回显、保存只改前端预览未进入正式 API、专用入口覆盖了非目标设备的通用维护能力、按设备编码合成参数导致其它工序同时出现，或静态配置已变更但当前活跃路线仍返回旧 `TEXT_STANDARD` 时必须停止。不得把静态合同通过当作运行态页面已生效。
- Verification: 聚焦静态合同锁定固定参数数量、控件类型、初始配置、专用/通用入口路由、技术字段不出现在专用弹窗以及一次保存调用正式接口；跨工序复用设备编码时还必须断言页面不硬编码 `deviceCode` 合成参数，并由目标工序数据迁移或正式配置精确限定参数归属。运行相邻配置合同、`pnpm ts:check` 和 `git diff --check`。真实 Playwright 必须断言目标路线工序的后端运行态载荷和页面控件一致；既有旧规则场景还必须取得迁移前 RED、正式迁移后 GREEN，不能只验证源码静态配置。桌面与移动视口验证应覆盖弹窗边界、内容滚动、底部保存操作可达、控制台/页面异常；只读验证必须断言目标写请求为 0，写入验证必须使用确认的测试租户和可清理数据。
- Forbidden action: 禁止仅在界面硬编码五项但不保存正式规则，禁止按设备编码在运行态合成固定参数，禁止删除通用参数入口，禁止用默认成功、静默吞掉部分保存失败或列表刷新失败，禁止用 API-only 代替真实页面交互，也禁止用参数名称猜测替代正式参数编码身份。
- Evidence: `doc/tasks/20260810-rough-wash-config-parameters/`，粗洗工序超声波清洗机固定五项参数从逐条通用弹窗改为所见即所得专用面板，并保留其他设备的通用参数维护；`doc/tasks/2026-08-11-add-validity-checkbox/`，光固Ⅰ与光固Ⅱ复用 A05075/A05059 时，计量效期 checkbox 改由光固Ⅰ正式 BOOLEAN 参数规则决定，避免仅按设备编码跨工序展示；`doc/tasks/20260811-fine-wash-cleaning-params/`，精洗源码静态合同已通过但活跃路线仍保存旧 `TEXT_STANDARD`，最终通过正式 SQL 迁移和真实一线 Playwright 验证下拉框及默认 `26` 数字输入生效。

## 前端数据库 Long ID 路由解析门禁

- Trigger: 页面从列表、工作台或深链进入详情/编辑，路由参数或 query 携带数据库 Long 主键、路线版本 ID、批次 ID 等业务身份；或出现“列表对象有效但详情提示缺少有效编号/未找到对象/系统异常”。
- Preflight check: 从列表响应类型、路由构造、目标页解析、API wrapper 和子组件 props 逐段核对 ID 是否保持原始十进制字符串；路由值统一使用 `parsePositiveRouteQueryId` 或同类字符串校验工具，API 参数类型使用明确的 `number | string` 业务 ID，不得在进入请求前调用 `Number`、`parseInt` 或一元 `+`。
- Blocker: 任一数据库 Long ID 在路由入口被数字化、超过 `Number.MAX_SAFE_INTEGER` 后发生精度丢失、有效列表项进入详情时请求了不同 ID，或缺少正式 ID 却准备用默认值/首行/旧缓存继续加载时必须停止。
- Verification: 聚焦静态合同必须同时锁定目标页使用字符串 ID 解析工具、禁止旧数字化表达式、API wrapper 接受字符串业务 ID，以及无效入口仍显示明确阻断；再运行相邻导航合同、`pnpm ts:check` 和 `git diff --check`。本机前后端运行态可用时，从真实列表点击进入目标页并断言 URL 与详情请求保留同一 ID、页面无系统异常。
- Forbidden action: 禁止用 `Number` 后再转回字符串、截取尾数、默认第一条记录、缓存对象、隐藏错误提示或放宽无效入口守卫来冒充修复；禁止用 mock/API-only 成功替代真实列表入口验证。
- Evidence: 任务 `doc/tasks/20260812-process-route-edit-valid-route-guard/` 为路线与候选版本补充了字符串身份解析合同；真实 E2E 同时证明该任务截图中的 `routeId=980098` 未发生精度丢失，实际异常应按“独立配置页签加载与错误归属”门禁处理，不能仅凭提示文案断定 Long ID 是根因。

## 审批中心业务详情入口门禁

- Trigger: 审批中心“查看”打开注册证、批记录、工艺路线、eDHR 或其它业务详情时，列表摘要已有业务编号但详情页提示“编号无效 / 无法加载详情 / 缺少有效编号”。
- Preflight check: 先核对审批任务响应中的 `detailRoute/detailQuery`、`decisionDetailRoute/decisionDetailQuery`、`businessKey`、`sourceTaskId` 和目标详情页 props/route 解析；“查看”动作必须优先打开后端给出的正式业务详情入口，流程详情仅作为流程查看入口。
- Blocker: “查看”仍固定跳转 BPM 流程详情、详情页把流程实例 ID/业务键/展示用编号当业务主键、或从摘要标签文本反推主键时必须停止；后端缺少正式业务详情身份时应暴露缺口，不得用首行缓存、编号文本或默认值补齐。
- Verification: 聚焦静态合同同时锁定审批中心 `openModuleDetail` 使用 `decisionDetailRoute/decisionDetailQuery`、目标详情页识别正式业务键或 query、后端 provider 产出正式业务详情路由；相关后端单测和 `pnpm ts:check` 必须通过。
- Forbidden action: 禁止隐藏“编号无效”提示、放宽无效入口守卫、把业务摘要编号当主键、把 API-only 成功冒充页面跳转正确，或为了可打开详情退回通用流程详情。
- Evidence: 任务 `doc/tasks/20260901-approval-registration-detail-invalid-id/verification-report.md`，注册证上传审批“查看”从 BPM 流程详情切换为正式注册证详情入口，同时保留 BPM 嵌入场景通过 `DCC_REG_CERT_ACCESS:<requestId>` 解析正式 `certificateId`。

## 全量类型检查与静态合同边界门禁

- Trigger: 并发页面改动后全量 `vue-tsc` 报错较多、类型检查长时间无输出或 OOM；静态合同因统一列表、严格 ID 解析或局部类型收窄重构而失败，但正式行为未退化。
- Preflight check: 全量类型检查只启动一个任务自有进程，并使用项目既定 8 GB Node 堆；出现无输出时先按 PID 核对是否存在本任务重复 `vue-tsc`，不得继续叠加。静态合同必须锁定正式数据映射、成功/失败顺序和用户可观察行为；负向断言只截取目标函数或目标配置块，不扫描整页宽泛词汇。实现由旧输入框迁移到统一列表、由内联可空对象改为断言后的局部常量时，应更新合同到新的正式边界，不得为了旧正则恢复废弃函数或重复控件。新增虚拟来源字段或分组字段时，静态合同必须覆盖前端过滤条件和页面可见性条件，避免后端已返回正式字段但被旧物理身份校验吞掉。
- Blocker: 同一工作区存在多个本任务 `vue-tsc` 争用内存、类型检查因 4 GB 堆 OOM、静态合同只因函数名/局部变量名/旧 DOM 结构变化而失败，或宽泛正则命中无关错误文案时，必须先消除验证噪声并收窄合同，不能把它记成源码回归。真实 E2E 出现接口载荷已有目标字段但页面文本缺失，例如 `real_device_group_not_visible`，必须先检查 computed/filter 是否仍要求旧字段形态，不得用后端接口通过替代页面断言。
- Verification: 以单实例 `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit --pretty false` 的 exit 0 为严格类型证据；静态合同同时证明正式查询参数映射、请求成功后再提交 URL/缓存状态、失败继续抛出，以及无 fallback/默认成功/吞异常。虚拟/分组字段还必须证明前端过滤条件接受新字段正式身份，并用真实 Playwright 页面断言目标字段实际可见。最后运行 `git diff --check`。
- Forbidden action: 禁止并行重复启动全量类型检查、用提高内存掩盖真实 TypeScript 错误、关闭严格规则、排除目录、引入 `any` 绕过、恢复不可达旧实现只为满足字符串正则，或用整页关键词扫描替代目标代码块合同。
- Evidence: `doc/tasks/20260813-concurrent-regression-repair/verification-report.md`；`doc/tasks/20260830-dcc-process-device-type-parameter-catalog/verification-report.md`，`PROCESS_POOL_REPORT` 设备参数改为 `@deviceGroup` 后，后端静态和接口已正确返回设备组字段，但真实 E2E 先暴露前端旧过滤条件仍要求物理 `deviceId/deviceCode/deviceName`，最终补充静态合同和真实页面断言后通过。

### 空 SFC 与局部必填收窄门禁

- Trigger: Vite 构建直接报 `At least one <template> or <script> is required`，或 `vue-tsc` 在页面级分页、表单、prop、emit 上报 `number | undefined`、`string | number | undefined`、`possibly 'undefined'` 等严格错误。
- Preflight check: 先检查目标 `.vue` 是否为空文件或仅剩残片；遇到局部页面需要把可空分页/表单字段喂给严格子组件时，优先在页面边界创建本地必填类型、computed 包装或显式空值归一，不要放宽共享 `PageParam` / 公共 VO / emit 签名。
- Blocker: tracked `.vue` 为空、分页 `pageNo/pageSize` 仍以可空类型直接绑定到必填 `v-model`、或把可空表单字段直接传给严格参数时，必须停止并补正式模板/局部收窄。
- Verification: `pnpm exec vue-tsc --noEmit --pretty false`、`pnpm build:local`、`git diff --check` 均通过后再判定页面构建门禁恢复。
- Forbidden action: 禁止靠扩大公共类型、伪造默认值、保留空 SFC 占位、或用 fallback 文案掩盖构建失败；禁止把单页收窄问题变成全局类型放宽。
- Evidence: `doc/tasks/20260827-full-stack-compile-repair/verification-report.md`。

## 验证方式

- 优先运行受影响范围的验证：
  - `pnpm ts:check`
  - `pnpm build:local`
  - 对应的 `pnpm e2e:*` 脚本
- 涉及用户路径时，使用 Playwright 通过真实前端页面验证。
- 动态菜单页面必须同时核对组件文件、菜单配置、角色菜单绑定和登录后权限响应。

## 禁止做法

- 禁止绕过 pnpm 修改依赖或 lockfile。
- 禁止把权限、路由或接口失败误判为组件不存在。
- 禁止 API-only 代替页面 E2E。
- 禁止在缺少页面入口、租户、角色或运行态证据时宣称前端验证通过。

## 工艺路线附加表单显式全局联动门禁

- Trigger: 工艺路线 `formBindings` 附加表单需要跨普通工序复制、同步修改、关闭联动、删除整组或让新增工序继承，或历史代码按相同模板自动跨工序修改。
- Preflight check: 先确认修改对象是表单槽位 `formBindings`，不是逐工序批记录表单或工序开始配置；跨工序联动必须使用独立、可持久化的组身份，每道工序继续保留独立绑定身份。任一当前工序操作导致全局表单模板、填写人、权限、归档、备注或排序变化时，必须同步整组；复制普通表单时保留已有全局组，清空全局模板必须走整组删除确认。
- Blocker: 仍按模板 ID 推断跨工序联动、不同工序复用同一绑定身份、普通表单增删只改变当前工序里的全局排序、复制表单破坏全局组、或清空模板可绕过删除确认时必须停止。
- Verification: 静态合同覆盖显式组身份、开启/关闭/删除确认、同槽位替换、其他槽位保留、组内修改、新增工序继承、复制表单保留全局组和非全局同模板独立；类型检查通过后，用草稿路线在至少两个桌面视口核对开关不重叠，并在包含当前后端代码与迁移的隔离运行态完成保存、刷新和关闭回读。
- Forbidden action: 禁止用模板 ID、槽位、默认 `MAIN`、表单名称或 `formBindingKey` 猜测全局组；禁止在已发布版本写入，禁止用 API-only 或静态合同冒充保存后刷新证据。
- Evidence: `doc/tasks/20260817-route-form-global-sync/verification-report.md`。

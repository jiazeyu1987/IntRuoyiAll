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
- 不得为测试额外添加无产品价值的页面控件或绕过真实用户路径。

## 前端源码目录与 .gitignore 门禁

- Trigger: Vite 报 `[plugin:vite:import-analysis] Failed to resolve import`，且目标是前端源码目录中名为 `logs`、`runtime`、`output` 等容易命中忽略规则的业务页面目录。
- Preflight check: 先执行 `git check-ignore -v -- <目标源码文件>` 和 `git status --short -- <目标源码文件>`，确认文件不是被根 `.gitignore` 的通用产物规则隐藏。
- Blocker: 若业务源码文件被 `logs/`、`runtime/`、`output/` 等通用规则忽略，必须先补精确 `!` 例外或调整目录命名，再补组件文件；不得只在本地复制文件后宣称完成。
- Verification: 目标文件在 `git status --short -- <目标源码文件>` 中可见为待跟踪/已跟踪，相关静态合同或 Vite 构建检查通过。
- Forbidden action: 禁止用关闭 Vite overlay、改路由到占位页、复制未跟踪文件、或把动态路由/权限问题误判为组件不存在来绕过根因。
- Evidence: 任务 `doc/tasks/20260725-dcc-controlled-file-logs-import/`，DCC 文控日志页面目录被 `.gitignore` 的 `logs/` 规则隐藏。

## 前端静态契约隔离门禁

- Trigger: 当前任务需要 RED/GREEN 静态契约，但已有大契约或全量 `pnpm ts:check` 先失败在无关历史问题上。
- Preflight check: 先运行最接近的既有契约并冻结首个无关失败；若失败点不属于当前任务，新增或改用任务专用最小静态契约覆盖当前行为。
- Blocker: 无法证明失败点与当前任务无关、或专用契约不能稳定先 RED 后 GREEN 时，不得宣称当前行为完成。
- Verification: `execution-log.md` 同时记录无关 blocker、专用契约 RED/GREEN、以及全量回归命令的剩余阻塞摘要。
- Forbidden action: 禁止修改无关大契约来绕过历史失败；禁止把无关 `ts:check` blocker 当成本任务通过证据；禁止跳过当前需求的最小 RED/GREEN。
- Evidence: 任务 `doc/tasks/20260726-release-action-error-autohide/`，既有 eDHR 大契约先失败于历史模型断言，本任务改用 `edhr-release-action-error-autohide-static.spec.js` 隔离 5 秒自动隐藏行为。

## 前端列表状态口径完整性门禁

- Trigger: 前端列表、版本工作区、状态表格、历史记录、候选版本、`只显示`、`仅展示`、`有效历史`、`已生效历史版本`、`取消的不显示`、`CANCELLED`、`DRAFT`、`ACTIVE`、`SUPERSEDED`。
- Preflight check: 先从用户原话或需求中拆出允许状态集合和禁止状态集合；若出现“只显示/仅展示”，必须按正向允许集合建模，而不是只排除截图里出现的一个异常状态。静态合同要同时断言允许集合、禁止集合和“不允许只写 `!== <badStatus>`”。
- Blocker: 过滤谓词只排除一个报错状态、测试只覆盖截图里出现的状态、文档把“只显示 A/B”改写成“隐藏 C”、或真实 E2E 没有证明至少一个非截图异常状态也被隐藏时，不得宣称完成。
- Verification: 聚焦静态合同必须包含一个负向断言，例如禁止 `version.lifecycleStatus !== 'CANCELLED'` 这种反向过滤；真实 E2E 若可运行，必须从页面断言允许状态可见、至少一个未生效候选状态和取消状态不可见，并记录无写请求。
- Forbidden action: 禁止把截图症状当作完整需求口径；禁止把“取消的不显示”当成唯一验收项而忽略前半句“只显示已生效的历史版本”；禁止用仅隐藏 `CANCELLED` 的实现替代 effective-only 列表口径。
- Evidence: 任务 `doc/tasks/20260727-route-version-list-active-history-only/`，首轮只隐藏 `CANCELLED` 后 completion audit 发现 `DRAFT` 仍可显示，最终改为 `ACTIVE/SUPERSEDED` 正向集合并用真实 E2E 证明 `V19 DRAFT` 与取消版本均隐藏。

## 前端保存链路重复错误提示门禁

- Trigger: 页面保存动作由父组件聚合多个子组件/API 保存，且子组件、父组件、axios response interceptor 都可能 `message.error`/`ElMessage.error`。
- Preflight check: 保存入口前先梳理错误传播链；内部 API 调用如果由外层统一 toast，必须显式传 `ignoreErrorMessage: true`，子组件 rethrow 前不得再次 toast。
- Blocker: 同一个失败在页面出现 2 条及以上相同错误提示，或静态合同无法证明 axios 自动提示、子组件提示、父组件提示不会叠加。
- Verification: 新增聚焦静态合同，并用 Playwright 拦截目标保存接口返回业务错误，断言保存错误提示只出现一次且错误文本来自真实响应。
- Forbidden action: 禁止用吞异常、默认成功、隐藏后端错误、只改文案、或关闭全局错误处理来减少 toast 数量。
- Evidence: 任务 `doc/tasks/20260726-route-flow-v15-save-system-exception/`，路线流转关系图保存失败曾由 axios、RouteFlowGraphDesigner、RouteFormContent 三层重复提示“系统异常”。

## 前端延迟辅助加载错误归属门禁

- Trigger: 列表首屏已加载成功，但后续延迟加载的行级权限、预览、候选人、补充状态或右侧详情接口失败，页面出现全局 `系统异常`、列表加载失败或首屏错误条。
- Preflight check: 先区分首屏主查询和延迟辅助查询；主查询失败才写全局列表错误，行级/预览级辅助查询失败必须落到对应行、卡片或预览区域，并保留真实错误文本。
- Blocker: 延迟辅助请求失败会清空主列表、覆盖 `listErrorMessage`、触发默认成功/空数据、或静态合同无法证明错误归属边界时，不得宣称修复完成。
- Verification: 新增聚焦静态合同覆盖主查询仍全局报错、辅助查询不污染全局错误、错误文本在行级或预览级可见，并运行相邻首屏延迟加载合同。
- Forbidden action: 禁止吞掉辅助接口错误、把真实失败改成空配置/未配置、关闭 axios 错误、或只隐藏全局 alert 而不展示错误归属。
- Evidence: 任务 `doc/tasks/20260727-edhr-batch-record-list-system-exception/`，批记录表单列表中填写人规则延迟加载失败曾在列表已成功渲染后污染全局 `listErrorMessage`。

## DCC 上传类别权限投影门禁

- Trigger: DCC 受控文件上传页、外来文件评审页、文件类别下拉、`upload-preview`、`Current user cannot access this controlled file`、类别级 `UPLOAD` 权限。
- Preflight check: 先确认类别列表接口返回当前用户类别级 `canUpload` 投影；上传页和外来评审页必须过滤 `canUpload=false` 且表单校验能拦截旧选择，同时后端 `upload-preview` / submit 继续用 `DccControlledFileCategoryPermissionSupport` fail-fast。
- Blocker: 类别列表缺少 `canUpload`、前端只按 `active/directoryId` 展示类别、上传接口为了消除提示放宽 `UPLOAD` 校验、或静态合同不能证明无权限类别不会进入上传。
- Verification: 运行 `node tests/e2e/dcc-upload-category-permission-static.spec.js`，并运行 `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileUploadApiTest#uploadPreviewFile_withoutCategoryUploadPermission_deniesBeforePolicyOrStorage,DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsCurrentUserUploadPermission,DccFileCategoryControllerConfigPackageContractTest#getCategoryList_withoutLoginUserDoesNotGrantUploadProjection" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- Forbidden action: 禁止把菜单权限当类别上传权限、禁止前端展示无权限类别再依赖上传失败、禁止 catch/默认成功/默认授权掩盖 `CONTROLLED_FILE_ACCESS_DENIED`。
- Evidence: 任务 `doc/tasks/20260728-dcc-upload-controlled-file-access/`。

## 前端草稿保存与提交发布解耦门禁

- Trigger: 受控版本、候选版本、草稿页、审批流对象或发布对象存在“保存草稿”和“提交发布/提交审批”两个动作。
- Preflight check: 普通保存成功处理只能标记已保存、清理本地退出标记或刷新草稿数据；提交发布必须由显式按钮/菜单触发，并保留提交前最新状态复查。
- Blocker: 普通保存成功后弹出“是否立即提交发布/提交审批”确认、调用 submit/publish API、把 DRAFT 推进 PENDING/ACTIVE，或静态合同无法证明保存不触发发布。
- Verification: 新增聚焦静态合同断言保存成功 handler 不调用提交函数，并用 Playwright 拦截保存接口成功响应，断言 submit-publish 请求数为 0 且页面仍停留在 DRAFT 草稿上下文。
- Forbidden action: 禁止把“保存成功顺手提交”当作便捷入口；禁止用 payload 标志、默认 true、隐式 watcher 或成功 toast 后确认框把保存和发布重新耦合。
- Evidence: 任务 `doc/tasks/20260726-route-flow-v15-save-system-exception/`，路线草稿 V15 普通保存后曾继续弹“草稿已保存，是否立即提交发布？”，用户确认后草稿进入审批/发布导致不可继续编辑。

## 表单模板三按钮领域边界门禁

- Trigger: 表单中心模板预览区“打开/编辑/填写”、`openSelectedTemplate`、`openSelectedTemplateAction('edit')`、`openSelectedTemplateFill`、`TemplateViewDialog`，或错误“当前模板未绑定批记录表单”。
- Preflight check: 先区分“交互模式对齐”和“数据领域关联”；表单模板与批记录表单没有直接关系。交互必须对齐批记录管理：`打开/编辑`通过当前 `/mdm/form-center/template` 路由 query 切换同页全宽工作区，`填写`跳转独立 `/mdm/form-center/template/simulate` 页面；三者只使用 `templateId + versionNo + jimuSchemaJson` 等当前模板上下文。
- Blocker: 任一按钮仍打开 `TemplateViewDialog`、`form-template-rules-dialog`、`form-template-fill-dialog`，要求 `batchRecordBindingStatus`/`batchRecordReportId`，跳转 MES 批记录路由，或未绑定普通模板显示不可操作错误时必须停止。
- Component isolation: 独立模拟填写路由若复用列表页组件，必须通过显式组件属性标识模拟页面实例，不得只依赖全局 `route.name`；否则路由切换期间旧列表实例和新页面实例会同时响应 watcher，重复加载模板版本。
- Verification: 至少运行 `node tests/e2e/form-template-button-interaction-parity-static.spec.js`、`node tests/e2e/form-template-independent-button-actions-static.spec.js`、`node tests/e2e/form-center-static.spec.js`，并从真实 `/mdm/form-center/template` 页面用 Playwright 点击三个按钮，确认 URL、工作区、无可见弹窗、无绑定错误和无写请求；请求审计必须证明三个动作各只请求一次 `GET /form-center/templates/{templateId}/versions/{versionNo}`，不得先查模板池或重复请求；`pnpm ts:check` 必须通过或记录明确阻塞。
- Forbidden action: 禁止把 UI/交互相似解释为共享 `reportId`；禁止用三个弹窗冒充批记录式页面流转；禁止伪造绑定、名称匹配、条件 fallback、跨域路由或只隐藏错误提示而保留错误数据契约。
- Evidence: 任务 `doc/tasks/20260727-form-template-button-alignment-design/`、`doc/tasks/20260727-form-template-button-interaction-parity/`；用户在 2026-07-27 明确澄清实际表单与批记录表单没有直接关系，并继续确认三个按钮的页面行为必须与批记录管理对齐。

## FormCenter 动态表单字段码渲染门禁

- Trigger: eDHR 批次详情、动态表单抽屉、损耗单、过程检验记录、`ActionFormPanel`、`EdhrExecutionTemplateEditableForm`、`FORM_TEMPLATE_VERSION` 单元格链接、`fieldCode`、`fieldIdentityMap`、实例 `form_data_json` 已有值但页面输入框为空。
- Preflight check: 先区分 FormCenter 正式数据键和电子表格坐标；FormCenter 实例草稿必须按模板识别字段 `fieldCode` 保存，例如 `field6`，前端渲染表格时才可用显式 `fieldIdentityMap` 把坐标 `5:3` 映射到 `field6`。打开动态表单时必须同时加载精确模板版本、实例最新 DRAFT 快照和模板规则；模板既缺布局又缺识别字段/规则时必须暴露配置缺失，不得画空壳。
- Blocker: 后端 `form_data_json` 已有 `fieldCode` 值但页面只显示快照 JSON、输入控件按坐标 key 读取导致为空、保存/提交仍使用父级批次元数据覆盖实例草稿、动态表单被误导到传统批记录 `batchRecordReportId` 链路、或静态合同不能证明 `5:3 -> field6` 映射时必须停止。
- Verification: 运行 `node tests/e2e/edhr-dynamic-form-action-panel-prefill-static.spec.js`、`node tests/e2e/form-center-static.spec.js`、`pnpm ts:check`；真实 E2E 必须通过批次详情页面打开动态表单，断言 `task/open` 返回 FormCenter 实例、DB `bpm_form_action_instance.form_data_json[fieldCode]` 和页面输入控件显示同一生产批号，并记录临时规则/待办/实例数据恢复。
- Forbidden action: 禁止把快照 JSON 文本当作表单控件展示通过；禁止同时保存 `fieldCode` 和坐标 key 冒充兼容；禁止用 API-only、直连实例、mock、默认空布局或传统批记录 PASS 替代损耗单/过程检验记录验证。
- Evidence: 任务 `doc/tasks/20260728-edhr-cell-link-main-e2e-repair/verification-report.md`，动态表单实例已落库 `field6`，旧页面因未渲染模板控件和缺少 `fieldIdentityMap` 导致页面为空。

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

## 动态菜单页签重命名门禁

- Trigger: 用户要求修改动态菜单页面、左侧菜单、顶部页签、页面标题或角色/套餐菜单树中的入口名称。
- Preflight check: 先同时定位 `system_menu.name` 的正式 SQL/迁移来源、页面内标题、真实 E2E 入口等待文本、角色菜单/租户套餐配置脚本；区分页签/入口名称和业务对象文案，避免把导入、弹窗、错误提示等非目标文案一并改名。
- Blocker: 只改前端组件标题但未提供正式菜单迁移、只改 SQL 但真实路径脚本仍查旧名称、或新增 SQL 缺少 `release-migration` 元数据和依赖门禁时必须停止。
- Verification: 新增聚焦静态契约同时读取页面标题、菜单迁移和真实路径脚本；运行角色/租户菜单相关静态契约；对新增菜单 SQL 使用目标 SQL + 依赖迁移执行 `run-release-migration-policy-gate.py --sql-file ...`。
- Forbidden action: 禁止用硬编码前端标题掩盖动态路由仍返回旧菜单名；禁止为了“统一”扩大修改权限按钮、导入导出、业务对象或跨模块选择文案。
- Evidence: 任务 `doc/tasks/20260728-rename-product-master-tab/`，产品主数据页签改为“展厅主数据”时需同步 `system_menu` 迁移、页面标题、角色菜单和租户套餐真实路径脚本。

## 前端 Route Query ID 比较门禁

- Trigger: 前端用 `route.query` 中的 `id`、`userId`、`assistUserId`、`workTaskId`、`batchTaskId` 等标识判断当前项、高亮项、上下文 key、可编辑态或请求 payload，尤其字段来自 Element/Vue Router query 字符串但业务对象字段是 number。
- Preflight check: 先确认 query 解析函数返回类型；若 query ID 会参与对象 ID 比较，必须使用 `sameRouteQueryId(...)`、统一字符串化，或显式转成同一数值类型，不得直接用 `===` 比较 query 字符串和 number。
- Blocker: 切换对象后 URL query 已变化但页面 active 高亮、表单上下文、缓存 key 或可点击态仍停留在当前登录人/旧对象，或静态合同无法证明字符串/数字 ID 比较一致时必须停止。
- Verification: 聚焦静态合同必须覆盖“请求带所选 ID”“路由保存后端确认 ID”“active/highlight 用 route-id 语义比较”；真实 E2E 需在切换后重开弹窗或返回页面，断言高亮/上下文跟随所选 query ID。
- Forbidden action: 禁止用当前登录人、旧缓存 key、宽松 fallback、刷新页面或隐藏高亮状态掩盖 route query ID 类型不一致。
- Evidence: 任务 `doc/tasks/20260728-switch-filler-wangxin-e2e/`，`assistUserId` 从 route query 读取为字符串，旧 active 判断与数字 `item.userId` 严格等于，导致切换到任丹后重开弹窗不高亮。

## eDHR 辅助模式当前工序 assistRows 路由门禁

- Trigger: eDHR 填写页“填写辅助模式”、工作任务“处理”、批次详情打开填写、工序切换、填写人切换、`task/open`、`executionPageQuery.assistRows`、`ASSIST_GRID_U`、辅助表格预览和填写页布局不一致。
- Preflight check: 先确认当前入口是否经过正式 `openTask`；进入 `/mes/pro/feedback/edhr-execution/form` 的路由 query 必须把后端返回的当前工序 `assistRows` 显式 JSON 序列化。执行页只按填写配置实际生成的 `ASSIST_GRID_U<userId>_R<row>_C<column>` rowKey 恢复辅助表格行列，不得推断其它 rowKey 协议。工序切换列表必须来自当前批次全部普通工序任务，列表展示不得按 `available/allowedActions/activeWorkTaskId` 过滤；点击可打开工序才走正式 `openTask`，已有 `executionId` 但不可打开的工序走只读执行页，尚未产生 `executionId/workTaskId` 的未开始工序必须进入批次详情并携带 `batchTaskId` 选中该工序。
- Blocker: `assistRows` 作为对象数组直接展开进 route query、进入执行页后解析为空或 `[object Object]`、辅助表格 rowKey 被扁平化为字段列表、粗洗等当前工序显示成其它工序/默认字段、静态合同不能证明批次详情和切换链路都保留当前工序 `assistRows`、工序切换只展示可打开任务、或未开始工序点击时报 `缺少可查看执行记录或工作任务` 时必须停止。
- Verification: 聚焦静态合同必须覆盖 `stringifyEdhrExecutionPageQuery`、批次详情和执行页切换调用、`parseAssistGridRowKey`、`edhr-fill-workspace__assist-grid`、`data-assist-grid-cell`、`resolveAssistFieldGridStyle(field)`；工序切换还必须覆盖全部工序分组、状态背景、可打开任务 `openTask`、已有执行记录只读打开、无执行记录时批次详情 `batchTaskId` 选中。真实 E2E 需用任务自有粗洗工序待办从页面按钮打开填写页，并断言辅助模式格子布局与配置预览一致。
- Forbidden action: 禁止用 `formBindings`、默认 `MAIN`、当前登录人、正式批记录字段、快照全量字段、空布局、宽松 rowKey 兼容、前端文案或伪造 `OPEN_FORM` 替代当前工序 `assistRows` 与正式工序入口。
- Evidence: 任务 `doc/tasks/20260729-edhr-assist-mode-process-form-mismatch/`，粗洗工序截图中配置预览是辅助表格，但填写页旧实现因 `assistRows` 未显式序列化且未恢复网格，显示为扁平字段列表；任务 `doc/tasks/20260729-edhr-process-switch-all-statuses/`，工序任务 `7169` 无执行记录/工作任务时旧实现直接报错，修正为进入批次详情并按 `batchTaskId` 选中工序。

## eDHR 产品信息虚拟 80 工序门禁

- Trigger: 批次执行详情、`BatchExecutionDetailPage.vue`、左侧工序列表、右侧当前工序表单卡片、产品信息表、`batchRecordSort=80`、后端任务保留来源 `routeProcessId`。
- Preflight check: 先区分“任务来源工序”和“页面显示工序”：产品信息成员任务可以保留源正式批记录绑定的 `routeProcessId/routeProcessSort` 作为追溯来源，但页面左侧必须按 `MAIN + BATCH_RECORD + 产品信息/80` 识别成独立虚拟 `80 产品信息` 工序组；`processTaskGroups` 不得只按 `routeProcessId || routeProcessSort || id` 合并所有任务。
- Blocker: 产品信息任务仍显示在第 1 工序或任一来源工序右侧、左侧缺少独立 `80 产品信息`、点击 80 工序后右侧混入其它正式批记录表单、点击第 1 工序后右侧仍包含产品信息，或静态合同不能证明产品信息专用 group key 时必须停止。
- Verification: 运行 `node tests/e2e/edhr-batch-product-info-virtual-process-static.spec.js`；真实 E2E 需从批次执行列表进入目标详情，断言第 1 工序右侧不含“产品信息”，`80 产品信息` 独立可见且右侧仅显示“产品信息”，并记录无 MES 写请求、无 console error。
- Forbidden action: 禁止为了页面显示把后端来源 `routeProcessId` 改成虚拟 ID、禁止用 `formBindings`/表单槽位/当前登录人推导产品信息、禁止隐藏第 1 工序卡片或硬插普通文本冒充 80 工序、禁止 API-only 代替页面分组验证。
- Evidence: `doc/tasks/20260728-batch-execution-product-info-form-missing/verification-report.md`，产品信息任务后端 `batchRecordSort=80` 但 `routeProcessSort=1`，前端需独立虚拟分组。

## eDHR 当前工序运行态展示门禁

- Trigger: 批次执行详情、`BatchExecutionDetailPage.vue`、左侧工序列表、批记录管理员、当前工序黄色背景、开始节点并行第一组、`WAITING`/待打开、`available`、`currentProcessRouteProcessId/currentProcessCode/currentProcessName`、`OPEN_FORM`、`canOpenTask`、`is-in-progress`。
- Preflight check: 先区分“运行态展示”和“填写操作权限”：当前工序高亮必须优先使用详情接口任务级 `available === true` 展示所有当前可执行工序组；开始节点直接后继存在并行第一组时，这一组只要后端任务门禁为可执行就都应显示黄色运行态。单值 `currentProcessRouteProcessId/currentProcessCode/currentProcessName` 只能作为兼容性补充，不得作为唯一展示来源；`WAITING` 当前工序也可显示黄色运行态，但打开填写仍只能由任务自身 `allowedActions` 是否包含 `OPEN_FORM` 决定。产品信息虚拟 `80` 工序必须先排除，避免复用来源正式工序身份造成误高亮。
- Blocker: 批记录管理员只读详情页看不到当前 `WAITING` 工序黄色运行态、开始节点并行第一组只标黄排序第一工序、状态展示依赖 `activeWorkTaskId`/`OPEN_FORM`/当前登录人是否为填写人、通过角色 ID 或填写人列表推断当前工序、或缺少静态合同证明展示权限未提升填写权限时必须停止。
- Verification: 运行 `node tests/e2e/edhr-batch-parallel-current-process-highlight-static.spec.js`、`node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js`、`node tests/e2e/edhr-batch-process-state-background-static.spec.js` 和 `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js`；真实 E2E 需用批记录管理员账号从批次执行列表进入详情，断言开始节点并行第一组当前可执行工序均显示黄色运行态、表单只读可见且无 MES 写请求。
- Forbidden action: 禁止为了解决高亮而放宽 `OPEN_FORM`、接管、跳过或提交权限；禁止用当前登录人、角色名、表单槽位、默认首个 `WAITING` 节点或前端文案推断当前工序；禁止把全部待打开工序统一标黄。
- Evidence: `doc/tasks/20260728-edhr-admin-current-process-highlight/verification-report.md`，批记录管理员只读当前工序通过详情接口 `currentProcess*` 投影为黄色运行态，填写动作仍受 `OPEN_FORM` 控制；`doc/tasks/20260729-edhr-parallel-start-process-highlight/verification-report.md`，开始节点并行第一组三个 `available=true` 工序在真实页面均为黄色运行态。

## 切换填写人 FormCenter 槽位导航门禁

- Trigger: eDHR 执行页“切换填写人”选择损耗单、过程检验单、参数记录表等 `formCenterInstanceId/formTemplateId` 表单槽位候选，尤其同一工序同时存在主批记录表单和 FormCenter 表单槽位。
- Preflight check: 先区分传统批记录任务和 FormCenter 表单槽位任务；FormCenter 候选必须先调用正式 `openTask` 校验所选 `assistUserId`，随后跳转批次详情并携带 `openRouteForm=1 + batchTaskId + workTaskId + assistUserId`，由详情页表单抽屉承载。
- Blocker: 切换填写人导航在检测 `formCenterInstanceId/formTemplateId` 前先要求 `executionId`、跳到 `/edhr-execution/form`、二次自动打开丢失 `assistUserId`、或出现“eDHR 批次缺少唯一批记录路线”时必须停止。
- Verification: 聚焦静态合同必须覆盖“FormCenter 分支先于 executionId guard”“跳转批次详情 openRouteForm=1”“详情页二次 openTask 透传 assistUserId”，并复跑相邻切换填写人和损耗单打开合同。
- Forbidden action: 禁止把 FormCenter 槽位伪装成传统批记录 execution、禁止清空 `assistUserId` 让当前登录人代替所选填写人、禁止隐藏后端错误或用刷新页面绕过。
- Evidence: 任务 `doc/tasks/20260728-edhr-scrap-assist-switch/`，选择“张可莹 / 损耗单”曾被传统执行页 `executionId` 要求和批记录路线校验拦住。

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

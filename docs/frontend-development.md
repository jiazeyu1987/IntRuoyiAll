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

## 工艺路线批记录表单正式来源门禁

- Trigger: 工艺路线流转关系图“批记录表单”、`batchRecordFormNames`、节点红绿边框、批记录表单链接、Word 导入页只勾选“批记录表单”。
- Preflight check: 先区分三类入口：`batchRecordReports` 是逐工序正式批记录表单，`formBindings` 是表单槽位，`batchRecordAttachmentOwners` 是工序开始负责人；关系图字段值、链接和节点状态只能读取正式 `batchRecordReports`。导入页只升版“批记录表单”且存在唯一当前路线时，也必须明确提示将生成路线候选并提交当前路线 ID/版本 ID。
- Blocker: 源码仍通过 `buildRecordBindingValue('MAIN')`、`buildRecordBindingLinks('MAIN')`、默认 `MAIN` 槽位归类、`formBindings`、特殊表单或工序开始配置计算“批记录表单”时，不得声明页面显示正确。
- Verification: 至少运行 `node tests/e2e/mes-route-flow-batch-record-form-source-static.spec.js`、`node tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js` 和 `node tests/e2e/mes-batch-record-import-formal-route-binding-static.spec.js`；真实页面验证需点击“批记录表单”并确认各工序显示正式生产记录名称且不发起 MES 写请求。
- Forbidden action: 禁止把表单槽位当作批记录表单的补空来源；禁止只改前端展示文案、隐藏“未配置”或 API-only 断言代替页面点击验证。
- Evidence: 任务 `doc/tasks/20260727-route-flow-batch-record-form-source/verification-report.md`。

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

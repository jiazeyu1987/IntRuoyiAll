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

## 表单模板编辑与批记录绑定动作边界门禁

- Trigger: 表单中心模板预览区“打开/编辑/填写”、`openSelectedTemplateDesigner`、`openSelectedTemplateAction('edit')`、`resolveSelectedTemplateBatchRecordBinding`、`batchRecordBindingStatus`、`batchRecordReportId`、或错误“当前模板未绑定批记录表单”。
- Preflight check: 用户已明确确认表单模板页红框内“打开/编辑/填写”必须按批记录表单行为执行；三按钮必须先校验 `batchRecordBindingStatus === 'BOUND'` 且 `batchRecordReportId` 非空，再分别进入批记录设计器 preview、批记录设计器 edit 和批记录模板模拟填写路由。
- Blocker: 表单模板页“打开/编辑/填写”任一按钮回退到 `TemplateViewDialog`、本页规则编辑弹窗、本页模拟填写弹窗，或缺少 `BOUND + reportId` 双条件 fail-fast 校验时必须停止；不得用名称、文件名、版本号猜测 reportId，也不得静默降级到本页流程。
- Verification: 至少运行 `node tests/e2e/form-template-batch-record-button-alignment-static.spec.js`，并确认 `pnpm ts:check` 通过或记录无关阻塞。
- Forbidden action: 禁止给普通模板伪造 `reportId`、吞掉绑定错误、改文案掩盖失败、名称匹配、空值兜底、API-only 替代页面点击，或把批记录三按钮行为改回 FormCenter 本页弹窗。
- Evidence: 任务 `doc/tasks/20260727-form-template-button-alignment-design/`，用户最终确认“三个按钮按批记录表单执行”，本页三按钮恢复为稳定 `batchRecordReportId` 驱动的批记录同源路径。

## 前端聚合新增默认分类门禁

- Trigger: 聚合字段编辑器新增子项，且页面摘要、徽标、保存 payload 或状态边框会按子项类型过滤，例如工艺路线表单槽位排除 `MAIN` 批记录槽位。
- Preflight check: 新增子项前先确认本地空对象的默认分类属于当前聚合字段的可统计/可保存范围；若存在排除分类，必须用静态合同锁定新增默认值不得落入排除分类。若产品口径要求“点击新增即计数”，计数 helper 不得再用必填配置项、模板 ID 或保存 payload 过滤掉本地新增行。
- Blocker: 新增项在右侧列表可见但摘要、徽标、状态或保存 payload 仍按旧数量计算，新增空对象默认使用了当前聚合字段明确排除的分类，或点击新增后的本地行因模板尚未选择而被数量 helper 排除。
- Verification: 目标静态合同必须同时断言新增空对象默认分类、计数 helper 的过滤口径，以及相邻状态/布局回归。
- Forbidden action: 禁止只在徽标侧硬加数量、按表单名称猜测业务类型、吞掉保存错误，或把被排除分类作为动态子项默认值。
- Evidence: 任务 `doc/tasks/20260726-route-flow-form-slot-live-count/`，工艺路线“表单槽位”新增第二个动态表单时默认 `MAIN`，导致节点数量仍显示 `1`。任务 `doc/tasks/20260726-route-flow-add-form-click-count/`，新增空行已是非 `MAIN` 但数量 helper 仍要求 `formTemplateId > 0`，导致点击新增后仍显示 `1`。

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

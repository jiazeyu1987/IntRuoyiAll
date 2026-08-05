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

## 前端截图样式块静态契约门禁

- Trigger: 用户基于截图要求调整局部颜色、选中态、高亮态、状态条、边框、背景或伪元素，尤其同一 SFC 中存在多个相似 `background`、`color`、`&::before`、`:hover`、`.active` 样式块。
- Preflight check: 静态契约必须先锁定目标选择器和目标状态块；负向断言要先抽取 `.active`、`:hover`、`:focus-visible` 或对应子块再检查旧样式，不得用过宽 `[\s\S]*` 从目标块跨到后续无关样式。
- Blocker: 契约无法区分普通态与选中态、命中结果可能跨块包含相邻绿色/黄色/背景/伪元素样式、或无法证明旧样式只在目标状态中被移除时，必须先修正契约再声明 GREEN。
- Verification: 聚焦静态契约必须同时断言目标正向 token、目标状态块内不存在旧 token、相邻控件契约仍通过；涉及 Vue/SCSS 文件时再运行 `pnpm ts:check` 和 `git diff --check`。
- Forbidden action: 禁止只凭截图目测改 CSS、禁止用全局覆盖或删除共享伪元素冒充局部状态修复、禁止用跨整文件泛正则做旧样式负向断言。
- Evidence: 任务 `doc/tasks/20260805-pqc-redbox-ui-prototype/`，PQC 检验项 tab 根据截图从白底绿字和绿色顶部条改为黄色选中背景；静态契约最终抽取 `.pqc-item-tab.active` 样式块，断言 active `&::before` 被隐藏且目标块内不存在旧绿色条。

## 统一列表复合工具栏布局门禁

- Trigger: 修改 `UnifiedListTemplate`、快速过滤、批量操作栏、标准列表多维筛选、`TableMultiFilter`、或把新筛选控件接入已有业务列表。
- Preflight check: 先在真实业务列表确认快速过滤、操作栏、额外筛选和新增筛选控件的 flex/grid 关系；可折行控件必须有明确行宽、`min-width` 和静态合同覆盖，不得只在空模板或单控件示例中验证。标准列表多维筛选要优先做成可增删条件 Tab 这类通用条件集合，不要靠页面级 `maxInlineFilters`、固定字段横铺或业务页特例控制可见条件；标准列表条件 Tab 默认必须为空，不得通过页面级 `.setCondition(...)` 或 query 初值预置隐藏业务筛选。同一页面内多个页签或子列表即使都使用 `UnifiedListTemplate`，也必须逐个显式核对是否接入 `showMultiFilter`、多维 definitions/state/events；模板能力不会自动替换仍绑定旧 quick filter 的列表。
- Blocker: 新控件在真实页面中被快速过滤或操作栏挤压到 `0` 宽、不可见、不可点击，静态合同只断言组件存在但不断言布局宽度和正式 query 透传，或同一个正式 query 参数可被多个条件 Tab 覆盖时必须停止。标准列表首屏请求仍带页面隐藏默认条件、目标页面还有其它标准列表页签仍保留旧 quick filter、重复状态开关、重复重置按钮或缺少多维筛选事件时，也不得宣称标准模板复用完成。
- Verification: 聚焦静态合同必须覆盖模板布局类、宽度下限、props/events 透传、条件 Tab 增删、默认空条件、禁止 `.setCondition(...)` 预置、稳定 condition id、重复正式参数校验和正式请求参数；真实 E2E 必须打开目标业务页面，断言控件可见可操作、首屏请求不携带隐藏默认条件、多个已填写 Tab 按交集提交、请求不携带临时参数、重置清空正式条件且目标写请求为 0。涉及同页多列表时，E2E 必须切换每个目标页签并分别断言旧 quick filter 可见数为 0、正式参数提交和重置清参。
- Forbidden action: 禁止用 API-only、临时测试页、隐藏旧快速筛选、移除业务操作按钮、硬编码当前页面宽度、页面级 inline filter 数量特例或前端本地过滤来冒充标准列表多维筛选完成。
- Evidence: 任务 `doc/tasks/20260804-standard-list-multi-filter/verification-report.md`，排产工单真实 E2E 暴露多维筛选在复合工具栏中被挤压为 `0` 宽，最终用模板级全行布局和静态合同锁定；后续用户反馈固定条件栏复用性差，改为条件 Tab + 加减号，并用真实 E2E 证明多个 Tab 按正式 query 参数交集提交；同步工单页签虽同样使用 `UnifiedListTemplate`，但因未显式接入多维 definitions/state/events 而保持旧 quick filter，最终按页签补齐静态合同和真实 E2E。任务 `doc/tasks/20260805-standard-list-empty-tabs/verification-report.md` 将当前系统 84 个标准列表模板扫描入清单，并锁定默认空条件 Tab、禁止页面级预置隐藏筛选、排产工单和同步工单首屏只带分页参数；任务 `doc/tasks/20260805-qa-regulation-publish-fix/verification-report.md` 新增 QA 规程 4 个标准列表后，将系统接入点更新为 88 个、显式隐藏筛选列表更新为 14 个。

## 前端 LocalDateTime 响应契约门禁

- Trigger: 前端 API wrapper、静态合同或页面报 `DCC response field has invalid type`、`cleanupTime`、`expireTime`、后端响应 VO 使用 `LocalDateTime`，或涉及 `TimestampLocalDateTimeSerializer`。
- Preflight check: 先核对后端 Jackson/JsonUtils 的 `LocalDateTime` 序列化口径；当前项目默认响应序列化为 epoch millis 数字时，前端类型和 parser 必须声明/校验 `number`，不得凭字段名假定字符串日期。
- Blocker: 前端仍用 `readOptionalString`、`string` 类型或字符串格式断言接收后端 `LocalDateTime` 数字时间戳，或为了通过页面临时做 string/number 双路兼容、空值吞错、默认当前时间时必须停止。
- Verification: 新增或更新聚焦静态合同，同时断言后端源字段类型、前端响应类型、显式数字 timestamp decoder、parser 调用和旧 string decoder 不再用于目标字段；涉及引用方时再运行 `pnpm ts:check`。
- Forbidden action: 禁止把后端全局序列化器返回的数字时间戳改成前端局部字符串兜底；禁止为掩盖合同不一致添加 fallback coercion 或吞异常。
- Evidence: 任务 `doc/tasks/20260803-dcc-cleanup-time-response-type/`，`cleanupTime`/`expireTime` 由 `LocalDateTime` 经全局 serializer 输出数字时间戳，前端旧 string parser 触发 `DCC response field has invalid type: cleanupTime`。

## Vue SFC 泛型箭头函数解析门禁

- Trigger: Vite 或 `vite-plugin-eslint` 在 `.vue` 文件中报 `Parsing error: Unexpected token. Did you mean {'>'} or &gt;?`，且报错行是 `<script setup lang="ts">` 内的 `<T>`、`<K, V>` 等泛型箭头函数。
- Preflight check: 先定位报错行是否是 `const fn = <T>(...) =>` 这类 SFC 易歧义写法；修复前新增或更新最小静态契约，让旧写法先 RED。
- Blocker: 直接关闭 Vite overlay、禁用 ESLint、移除 TypeScript 类型、改成 `any`、或只改测试不改源文件时，必须停止。
- Verification: 聚焦静态契约必须证明目标 SFC 不再使用歧义泛型箭头写法，并优先改为 `function fn<T>(...) {}`；再运行相邻静态契约或可响应的 ESLint/类型检查。
- Forbidden action: 禁止用配置降级、parser 替换、忽略规则或隐藏页面来绕过源代码解析错误。
- Evidence: 任务 `doc/tasks/20260803-dcc-controlled-file-detail-vue-parse/`，`getPagedDetailRows` 的 `const ... = <T>(...) =>` 触发 Vite/ESLint 解析错误，改为命名泛型函数并用静态契约 RED/GREEN 验证。

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

- Trigger: Element Plus 表格、统一列表模板、分页列表、`sort-change`、`sortColumnAttrs`、表头排序按钮、空单元格排序、跨页排序。
- Preflight check: 先区分本地全量列表和服务端分页列表；服务端分页列表的表头排序必须从表格事件进入统一排序状态，再映射成正式分页请求参数，并由后端白名单字段排序，不能只改当前页数组顺序。若用户要求空单元格在某一方向固定置顶或置底，后端排序必须显式增加空值标记表达式，不能依赖数据库默认 `NULL`/空字符串排序。
- Blocker: 表头有排序按钮但未绑定 `sortState`，`sort-change` 只更新组件内部状态，分页请求缺少 `sortField/sortOrder`，后端 Mapper 固定排序忽略请求字段，降序空单元格未被显式排到最后，或空值只能在当前页集中时，不得宣称排序修复完成。
- Verification: 聚焦静态契约必须同时断言前端排序状态绑定、请求参数映射、后端请求 VO 字段、Mapper 白名单排序、空值置顶/置底表达式和稳定兜底排序；再运行相邻列表契约、`pnpm ts:check` 和目标后端分页测试。
- Forbidden action: 禁止用前端当前页 `Array.sort` 冒充跨页排序；禁止把任意前端字段直接拼 SQL；禁止用 `.last()` 拼接受用户控制的排序 SQL；禁止依赖数据库默认空值顺序满足用户指定语义；禁止只看表头箭头状态不看接口排序参数。
- Evidence: 任务 `doc/tasks/20260730-dcc-product-catalog-null-sort/`，DCC 产品目录“项目名称/项目代码”旧实现只触发统一列表内部排序状态，后端仍按 `dataSource/originalRowNo` 固定排序，最终补齐 `sortField/sortOrder` 与 Mapper 白名单排序。

## 审批中心路由筛选可见性门禁

- Trigger: 审批中心待办/已办列表、`/approval-center/todo`、`/approval-center?moduleCode=...`、`keyword`、快速筛选控件、页面控件显示无筛选但列表为空。
- Preflight check: 列表请求使用 route query、缓存状态或快速筛选状态时，必须把生效的 `moduleCode`、`keyword` 等条件同步到用户可见控件；模块加载失败必须保留错误并抛出，不能被后续列表请求覆盖成有效 0。
- Blocker: URL/query 中的过滤条件仍会影响请求但页面控件为空、清空筛选未同步 route、模块列表接口异常后页面显示“0 个模块”、或静态合同只能证明接口参数存在但不能证明筛选可见时必须停止。
- Verification: 聚焦静态合同覆盖 route filter -> quick filter 可见状态，再复跑审批中心分页保页、列表区域和分页 payload 相邻合同；涉及请求/错误链路时同步复跑目标后端 JUnit。
- Forbidden action: 禁止在前端默认清空 query、吞掉模块错误、只隐藏空态、只改 badge 或用 API-only 证明列表正常。
- Evidence: `doc/tasks/20260804-approval-center-todo-empty-list/verification-report.md`。

## 前端截图字号调整静态契约门禁

- Trigger: 用户基于截图要求调整卡片、表格、弹窗或页面局部文字大小，尤其出现“文字大小”“字号”“放大 2 倍”“缩小一半”“卡片内文字”等表述。
- Preflight check: 先定位目标区域已有 SFC/CSS 选择器和相邻静态契约；若已有契约锁定字号或密度，必须先按用户口径更新该契约并跑出 RED，再改最小 CSS。若没有契约，新增任务专用静态契约断言目标选择器和具体字号，不得只凭截图目测。
- Blocker: 找不到目标选择器、无法区分卡片内文字与页面其它文字、契约无法稳定 RED/GREEN、或改动会同时改变数据、权限、接口、保存/提交链路时必须停止补齐范围。
- Verification: 至少运行目标字号静态契约和一个相邻结构/显示契约；若改动触及 Vue/TS 逻辑或构建可受影响，再运行 `pnpm ts:check`。
- Forbidden action: 禁止用全局 `body`/Element Plus 泛选择器批量放大、禁止隐藏/缩放容器冒充字号变化、禁止把截图局部需求扩大成整页重设计、禁止跳过 RED 直接改 CSS。
- Evidence: 任务 `doc/tasks/20260729-card-text-double/`，eDHR 填写辅助模式卡片原有半字号静态契约先 RED，再将网格卡片内标签、输入/占位、选择项、按钮、校验和单位文字提高为 2 倍。

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
- Preflight check: 先从用户原话拆出“要拆出的角色/内容”和“原工作台保留的角色/内容”，并确认“页签”指动态菜单/主导航入口还是页面内部 `el-tabs`；用户说“类似批次执行”“放在 QA 下面”时，按 eDHR 父菜单下的独立主导航子菜单处理，不得误做成 eDHR 批次页内部 Tab。若用户明确说同一角色下“人员管理、报工管理、损耗管理”等不同功能模块，则按该角色页面内部功能模块 Tab 处理，并先核对共享组件中其它角色复用的 content gate 和相邻静态合同。再核对现有包装页、路由、页签 key、标题、权限和共享组件 props；若工作区已有相反方向的半成品拆分，必须先用 RED 静态合同锁定当前用户要求，不得沿用旧任务口径。
- Blocker: 专门页签拆成了错误角色、把主导航页签误做成页面内部 Tab、把页面内部功能模块 Tab 误做成新菜单、原工作台仍传入目标角色 props、两个入口同时显示同一角色内容、功能模块仍纵向混排、旧 route/tab key/页面关系图仍指向相反角色、或静态合同只断言“有独立页签”但不验证角色 props、模块 gate、共享 gate 和原工作台负向隐藏时必须停止。
- Verification: 聚焦静态合同必须同时断言页签 label、tab key 或主导航菜单 sort、route path、route name/title、包装组件文件、共享组件 `leader-type` 或等价 props、原工作台 `doesNotMatch` 目标角色内容、页面关系图节点和相邻工作台合同；页面内部功能模块 Tab 还必须断言包装页显式启用模块 Tab、非目标角色未启用该专属 Tab、每个模块块由对应 computed gate 控制、共享 gate 未破坏相邻角色合同。涉及动态菜单时还必须断言 `system_menu`、租户套餐和角色菜单绑定；涉及 Vue/TS 时运行 `pnpm ts:check`。
- Forbidden action: 禁止用 CSS 隐藏、空数据、路由别名、旧页签文案、内部 Tab 冒充主导航入口或保留旧反向 wrapper 冒充拆分完成；禁止把“PQC组长拆出去”与“生产组长拆出去”互换处理。
- Evidence: 任务 `doc/tasks/20260804-production-leader-tab/`，基线中已有相反的 `PQC组长` 独立页签，当前需求要求 `生产组长` 独立页签，最终用静态合同先 RED 再将 `BatchProductionLeaderWorkbenchPage`、`productionLeader` 路由和组长工作台 `leader-type="PQC"` 边界锁定；任务 `doc/tasks/20260804-pqc-leader-tab/`，用户纠正“不是 tab，是类似批次执行的页签”，最终锁定 `QA -> 生产组长 -> PQC组长 -> 批次执行` 主导航顺序，并从 `EdhrBatchRecordTabs.vue` 移除内部 leader tabs；任务 `doc/tasks/20260805-production-leader-function-tabs/`，用户要求生产组长内“人员管理、报工管理、损耗管理”等不同功能模块是不同 Tab，最终保留 `ProductionLeaderWorkbenchPage` 主导航入口，仅在共享工作台增加生产组长内部模块 Tab，并复跑 PQC 组长相邻合同防止共享 gate 破坏。

## eDHR 表单追溯可视化历史详情门禁

- Trigger: eDHR 表单追溯、历史批记录入口隐藏、归档批次详情、`BatchExecutionTraceDrawer`、`review-timeline.executionReviews.formViewModel`、`EdhrExecutionReadonlyForm`、用户要求历史详情像批次执行填写页而不是纯文字。
- Preflight check: 先区分“独立历史批记录入口”“追溯抽屉”和用户实际点击的“详情”弹窗；若产品口径要求不显示独立历史批记录，必须同时检查页签、隐藏路由、批次详情卡片、页面关系图、详情弹窗和可见文案。可视化详情必须在表单追溯上下文内复用 `review-timeline` 的持久化执行快照、模板布局、单元格值和签名记录，并使用 `EdhrExecutionReadonlyForm` 或同等只读表格组件展示。
- Blocker: 仍存在可点击独立历史批记录入口、用户点击“详情”后看不到“批记录表单”页签、详情只展示 JSON/纯文字快照、表单追溯重新拉独立历史批次列表、历史详情依赖当前活动 BATCH 配置或当前 Jimu 报表、或为了隐藏入口删除历史数据时，必须停止。
- Verification: 聚焦静态契约必须同时断言旧入口无可见残留、“电子批记录变更详情”弹窗有“批记录表单”页签、表单追溯抽屉有“批记录表单”页签、存在工序/表单导航、只读表单接收 `formViewModel` 与 `signatureRecords`、并禁止保存/签名/放行/作废动作；涉及 Vue/TS 时运行 `pnpm ts:check`。
- Forbidden action: 禁止用 CSS 隐藏旧入口、禁止保留可搜索隐藏路由作为正式入口、禁止把 `executionSnapshotJson` 直接渲染成纯文本、禁止用 `formBindings` 或当前路线 BATCH 配置补历史批记录。
- Evidence: 任务 `doc/tasks/20260803-edhr-trace-visual-record-detail/verification-report.md`。

## 前端列表跨账号默认列布局统一门禁

- Trigger: 同一列表在不同浏览器、账号或租户显示不同字段，页面存在“显示字段”、`useUserTableColumns`、`data-user-table-key`、用户列配置接口，或用户要求统一为 admin 默认布局。
- Preflight check: 先区分三类差异：个人列配置控制的字段可见性/列宽、`v-hasPermi` 控制的操作按钮、视口宽度造成的横向滚动。若需求是让既有用户统一采用新的默认列集合，同时仍保留“显示字段”，必须升级稳定 table key，并同步标准列表模板、Element Plus 表格标识和 `useUserTableColumns` 调用；只修改默认 `visible` 不会覆盖旧服务端配置。若验收要求“列表行直接显示”关键业务信息，必须确认这些信息不只存在于可隐藏列、固定列或横向滚动外区域，应在至少一个默认稳定可见列中重复承载可读摘要。
- Blocker: 仍读取旧 table key、只改默认列但历史用户配置继续覆盖、关键验收信息只放在可隐藏列或固定列导致真实 E2E/普通用户无法在主列表行确认、为了视觉一致移除权限指令或给普通用户显示 admin 操作、通过清浏览器缓存或批量删数据库配置冒充正式迁移、或显示字段入口保存到与加载不同的 key 时必须停止。
- Verification: 聚焦静态合同必须断言新 key 在模板、表格标识和 hook 三处一致，旧 key 不再使用，默认显示/隐藏字段集合明确，关键验收信息位于稳定可见列，显示字段自动保存和既有权限码保留；真实 E2E 可用时使用同一账号分别在两个浏览器验证表头和显示字段勾选一致，并记录无业务写请求、无 console error。
- Forbidden action: 禁止引入 localStorage fallback、静默忽略列配置接口失败、扩大角色权限、删除业务字段定义、或用不同账号的按钮差异证明浏览器渲染不一致。
- Evidence: `doc/tasks/20260730-route-admin-list-layout-unification/verification-report.md`；`doc/tasks/20260802-dcc-controlled-browser-ux-optimization/verification-report.md`。

## 前端权限页签正向授权门禁

- Trigger: 前端页面、动态菜单、顶部页签、左侧菜单、隐藏路由或入口默认页涉及“普通用户只能看到/仅显示某页签”、`activeMenu`、`redirect`、`permissionStore`、静态隐藏子路由合并。
- Preflight check: 先拆出普通用户允许页签集合和管理员允许页签集合；默认入口重定向必须来自已授权动态子路由或明确的普通用户页签，不得固定跳到管理员页签。隐藏静态子路由合并时，权限型壳路由不得把未授权的隐藏静态子路由补回普通用户路由表。
- Blocker: 普通用户仍可默认进入管理员列表、无权限页签组件会 mount 并触发接口 403、只改菜单 SQL 但前端静态路由仍补回未授权子路由、或只隐藏一个截图页签而未按正向允许集合建模时必须停止。
- Verification: 新增聚焦静态合同同时断言普通页签正向集合、管理员页签集合、默认重定向、组件 mount gate、动态权限路由合并边界和菜单/角色 SQL 授权边界；涉及 Vue/TS 路由逻辑时运行 `pnpm ts:check`。
- Forbidden action: 禁止用前端空白、吞掉 403、默认成功、API-only 断言、只改按钮可见性或只改后端菜单授权来冒充页签隔离完成。
- Evidence: 任务 `doc/tasks/20260730-electronic-signature-my-tab-only/`，电子签名普通用户旧实现固定进入“签名记录”并触发无权限列表，修正为普通角色只保留根入口和“我的签名”，前端按授权动态子路由重定向并禁止补回未授权治理页签。

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

## 前端保存链路重复错误提示门禁

- Trigger: 页面保存动作由父组件聚合多个子组件/API 保存，且子组件、父组件、axios response interceptor 都可能 `message.error`/`ElMessage.error`。
- Preflight check: 保存入口前先梳理错误传播链；内部 API 调用如果由外层统一 toast，必须显式传 `ignoreErrorMessage: true`，子组件 rethrow 前不得再次 toast。
- Blocker: 同一个失败在页面出现 2 条及以上相同错误提示，或静态合同无法证明 axios 自动提示、子组件提示、父组件提示不会叠加。
- Verification: 新增聚焦静态合同，并用 Playwright 拦截目标保存接口返回业务错误，断言保存错误提示只出现一次且错误文本来自真实响应。
- Forbidden action: 禁止用吞异常、默认成功、隐藏后端错误、只改文案、或关闭全局错误处理来减少 toast 数量。
- Evidence: 任务 `doc/tasks/20260726-route-flow-v15-save-system-exception/`，路线流转关系图保存失败曾由 axios、RouteFlowGraphDesigner、RouteFormContent 三层重复提示“系统异常”。

## 前端主结果弹窗失败原因可见门禁

- Trigger: 保存、提交、审核、发布或签名动作失败后页面显示主结果弹窗、大号结果弹框、`result-dialog`、`提交失败`、`保存失败`、`发布失败` 或同类状态。
- Preflight check: 先梳理错误传播链，主结果弹窗必须承载外层 catch 已解析的真实错误文本；若 toast 保留，弹窗也必须展示同一失败原因，成功状态必须清空失败原因。
- Blocker: 主弹窗只显示“提交失败/保存失败”等状态、不显示后端 `msg/message` 或本地 fail-fast 原因，或静态合同无法证明失败原因字段从 catch 传入弹窗状态时必须停止。
- Verification: 新增聚焦静态合同断言失败原因字段、模板可见区域、catch 参数传递和成功状态清空；真实写入 E2E 可用时用真实失败响应断言弹窗可见文本，不得用 mock 或拦截替代。
- Forbidden action: 禁止只依赖短暂 toast、改成通用默认失败文案、隐藏后端错误、吞异常、用成功弹窗残留旧错误或关闭全局错误处理来满足截图。
- Evidence: 任务 `doc/tasks/20260729-submit-failure-reason/`，eDHR 提交失败弹窗曾只显示“刘子良 提交失败”，未显示具体失败原因。

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
- Blocker: 类别列表缺少 `canUpload`、前端只按 `active/directoryId` 展示类别并排除可上传但未绑定目录的正式类别、上传接口为了消除提示放宽 `UPLOAD` 校验、文件类别允许跨 taxonomy 选择或保留 stale `categoryId`、把 `fileTypeTaxonomyId` 当作后端目录查询 `categoryId`、后端缺少唯一启用 `UNCLASSIFIED` 目录却继续提交、NAS/本地导入/元数据编辑仍提示“请先绑定目录”或要求用户选择未绑定类别的目录、已配置当前有效审批矩阵的类别列表仍缺少 `approvalPositionIds` / `signoffPositionIds` 导致误报审批链路不完整、或静态合同不能证明无权限/跨 taxonomy 类别不会进入上传。
- Verification: 运行 `node tests/e2e/dcc-upload-category-permission-static.spec.js`、`node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js`、`node tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js`；涉及未绑定提交目录时，还要运行 `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_unclassified_upload_directory_seed_sql.py -q`、DCC base + unclassified seed 迁移门禁，以及 `getUploadDirectoryTree` / submit 的未分类目录后端单测；涉及 NAS/本地导入或元数据编辑时，还要运行 NAS 管理页、元数据弹窗静态合同和对应后端单测，并通过真实 Playwright 页面路径证明自动未分类提示可见、目标 DCC 写请求边界符合本轮只读或写入范围。涉及上传预检审批链路时，运行 `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsActiveApprovalMatrixPositionIds,DccCategoryApprovalMatrixAdminServiceImplTest#getActiveMatrixPositionIdsByCategoryIds_readsLatestActiveRoutePositionNodes" "-Dsurefire.failIfNoSpecifiedTests=false" test`。真实 E2E 若返回 `1080000196 Unclassified upload directory does not exist`，说明代码已进入正式 fail-fast 分支但本地库缺 seed，应先执行幂等 `20260803_dcc_unclassified_upload_directory_seed.sql` 并核对唯一 active `UNCLASSIFIED / 未分类`，不得改代码绕过。
- Forbidden action: 禁止把菜单权限当类别上传权限、禁止前端展示无权限类别再依赖上传失败、禁止用 `directoryId` 缺失阻止用户提交、禁止 catch/默认成功/默认授权掩盖 `CONTROLLED_FILE_ACCESS_DENIED` 或缺失 `UNCLASSIFIED` 目录。
- Evidence: 任务 `doc/tasks/20260728-dcc-upload-controlled-file-access/`；任务 `doc/tasks/20260803-controlled-file-category-missing/`，受控文件提交页按当前文件分类 taxonomy 叶子节点自动解析正式文件类别并清空旧类别/目录/预览状态，未绑定提交目录时后端使用正式 `UNCLASSIFIED / 未分类` 目录，避免 `Controlled file category does not exist` 和提交人手工维护目录绑定；任务 `doc/tasks/20260804-dcc-unclassified-directory-consistency/`，NAS 转移、本地文件夹导入和元数据编辑统一使用正式未分类目录自动落位，旧“请先绑定目录”阻塞只保留在历史任务文档或测试负向断言中；任务 `doc/tasks/20260804-dcc-upload-approval-chain-projection/`，类别列表接口从当前有效审批矩阵路线节点投影 `signoffPositionIds` / `approvalPositionIds`，避免技术调研报告等已配置类别被上传预检误判为审批链路不完整。

## DCC 预览不可用原因短路门禁

- Trigger: DCC 受控浏览、受控文件详情预览、统一在线预览、`ProtectedPdfViewer`、`previewUnavailableReason`、`previewOnlineFileWithWatermark`、`previewControlledFileWithWatermark`、`PDF/IMAGE/VIDEO/AUDIO/TEXT/OFFICE/DOWNLOAD_ONLY` 预览类型。
- Preflight check: 先确认预览元数据接口是否可能返回 `previewUnavailableReason`；前端拿到该字段后必须在任何二进制预览请求前短路。Office 可继续交给 OnlyOffice 只读组件展示不可用原因；PDF、图片、视频、音频、文本和下载型文件必须用通用错误区域显示同一精确原因，不能继续请求 preview binary。
- Blocker: 元数据已返回 `previewUnavailableReason` 但页面继续调用二进制预览接口、非 Office 类型只显示“受控预览加载失败”等泛化错误、下载型文件用“仅支持下载”覆盖正式不可用原因、或静态合同不能证明短路发生在 `resolvePreviewBlob()` / `previewOnlineFileWithWatermark()` 之前时必须停止。
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
- Preflight check: 先确认弹框是否必须在最大化状态可见；若必须可见，弹框组件必须作为 fullscreen 元素子树渲染，并显式设置 `:append-to-body="false"` 或同等非 body teleport 约束；静态合同需同时断言弹框 DOM 位于 fullscreen 容器内部和未 teleport 到 body。
- Blocker: 弹框仍在 fullscreen 容器外、依赖 body 级 overlay、仅通过提高 `z-index` 解决浏览器 fullscreen top layer、或保存/提交按钮在最大化后弹框不可见时必须停止。
- Verification: 运行目标静态合同；有本地运行态和登录前置时，再用 Playwright 点击最大化后触发弹框，断言弹框可见、可操作且无控制台错误。
- Forbidden action: 禁止用随机加大 `z-index`、退出全屏后再弹框、隐藏错误提示、改成普通 toast、或关闭浏览器 fullscreen 能力来绕过遮挡。
- Evidence: 任务 `doc/tasks/20260729-edhr-fill-workspace-redbox-hide/`，eDHR 填写页最大化后保存/提交弹框原先位于 `.edhr-fill-workspace` 外部，被浏览器全屏层遮挡，修正为在全屏工作区内部渲染并禁用 body teleport。

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

# PQC 红框区域 UI 原型与正式实现

## Task Goal

基于用户截图中 PQC 填写页“检验内容”卡片内红框区域，先完成独立 HTML 原型设计，再按已确认方向落到正式 Vue 页面：检验项以最多 10 个完整显示的 2 行 x 5 列 tab 呈现，仅展开当前检验项详情，并将设备、编号、接收标准、检验方法改为与一线操作端一致的触控式信息卡；右侧填写区按最新黄框反馈隐藏说明文字和签名编号展示，字段标签压缩为“检验 / 损耗 / 不良”；正式系统 PQC 填写页必须与更新后的 HTML 预览关键效果一致，并在真实最大化画布中尽可能做到肉眼一致。

## Milestones

- [x] 定位 PQC 页面现有入口与红框区域源码。
- [x] 读取前端样式与任务规则，识别适用设计约束。
- [x] 产出独立 HTML 原型文件。
- [x] 完成结构性验证与 UTF-8 读取验证。
- [x] 新增正式 PQC tab 布局静态契约并跑出 RED。
- [x] 改造正式 `FrontlineFixedTemplatePanel.vue` 的 PQC 检验内容区域。
- [x] 完成 GREEN、相邻回归和任务证据更新。
- [x] 按最新截图反馈移除选中 tab 顶部绿色条，改为黄色背景表达选中态。
- [x] 对齐正式 PQC 填写页与更新后 HTML 预览的 tab 文字完整显示规则。
- [x] 对齐正式 PQC 最大化态画布、边距、阴影、顶部卡片比例、左右列间距和右侧数量控件尺寸。
- [x] 使用真实 Playwright 截图复验“肉眼完全一致”前置条件。
- [x] 按最新截图反馈隐藏右侧黄框内容，并将“检验数量 / 损耗数量 / 不良说明”缩短为“检验 / 损耗 / 不良”。

## Expected Verification

- 使用 UTF-8 方式读取原型与任务文档，确认中文无乱码。
- 静态检查原型包含推荐设计的关键结构：设备选择、设备编号、接收标准、检验方法、逐件选择。
- 运行 `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`，验证正式 PQC 页面使用 tab 网格、只展开当前项、10 个 tab 完整字段、设备/编号信息卡和无未样式化原生 select。
- 运行 `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`，验证选中 tab 使用黄色背景且不显示旧绿色顶部状态条。
- 运行 `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`，验证正式 tab 的要求/已填字段与更新后 HTML 预览一致，不通过省略号隐藏关键字段。
- 运行 `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js`，确认原有设备、编号、标准、方法提交与弹框契约仍保留。
- 运行 `node doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-real-visual-alignment.e2e.cjs`，在真实本机页面进入最大化画布，截图并确认是否具备与 10-tab HTML 预览完全一致的数据前置条件。
- 运行 `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`，验证右侧填写区不再显示黄框内说明文字和签名编号输入，且三个标签文案为“检验 / 损耗 / 不良”。
- 运行 `pnpm ts:check`；若存在与本任务无关历史阻塞，记录首个阻塞点，不把阻塞写成通过。

## Current Status

blocked

正式系统 PQC 填写页已与更新后的 HTML 预览关键样式对齐：最大化态画布为 1440×950 截图面，外边距 26px、主体宽 1388px、顶部卡片比例、左右列间距、黄色选中 tab、无绿色顶部条、要求/已填字段完整显示均已通过真实截图复验。最新右侧填写区反馈也已完成：黄框中的说明文字和签名编号输入不再渲染，标签已改为“检验 / 损耗 / 不良”，底层 `pqcSignatureId` 状态和提交校验未改写。仍不能声明“肉眼完全一致”：当前本地真实 PQC 数据最多只渲染 7 个检验项 tab，而 HTML 预览是 10 个 tab；最大化态按钮也按真实功能显示“主页”，不是静态预览中的“最大化”。在未获准创建任务自有 10 项 PQC 测试数据前，不得通过 mock、占位 tab 或修改正式业务数据伪造完全一致。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；正式实现不新增兜底成功、静默吞错或兼容降级路径。
- 是否从根因和长期维护角度解决：是；根因是红框内原生控件密度、纵向列表和视觉语言脱离 PQC 操作端主风格，正式实现改为同源的触控式信息卡和 10-tab 网格，并补齐最大化态与预览画布一致性。
- 是否存在临时补丁或绕过：否；按正式组件与静态契约落地，不以截图原型替代正式页面。

## Applicable Gates

- 前端样式门禁：红框区域属于前端页面局部 UI 设计，需遵循现有 PQC 操作端的粗边框、大字号、绿色操作台风格，不扩大成整页重设计。
- 前端截图样式块静态契约门禁：选中态颜色和状态条必须锁定 `.pqc-item-tab.active` 块做正负断言，避免跨块误命中其它绿色样式。
- UTF-8 门禁：任务文档和 HTML 原型包含中文，读写必须保持 UTF-8。
- Git 门禁：当前工作区存在非本任务脏改动 `doc/tasks/20260805-ac-m04-acceptance-sync/`；本任务不回滚、不覆盖并发改动，收尾提交/推送需先处理该阻塞。
- 真实视觉一致性门禁：当前本地正式 PQC 数据最多 7 个检验项，缺少与 HTML 预览一致的 10 项 PQC 任务；未获用户明确批准前，不创建或改写业务数据来凑数。
- 右侧填写区隐藏门禁：仅隐藏用户黄框指定的说明文字和签名编号展示，不删除 `pqcSignatureId` 状态、提交 payload 或正式必填校验链路。

## Cleanup Keep

- doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.html
- doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.png
- doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype-yellow-active.png
- doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-real-visual-alignment.e2e.cjs
- doc/tasks/20260805-pqc-redbox-ui-prototype/frontend-feature-evidence.md

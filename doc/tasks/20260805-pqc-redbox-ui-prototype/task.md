# PQC 红框区域 UI 原型与正式实现

## Task Goal

基于用户截图中 PQC 填写页“检验内容”卡片内红框区域，先完成独立 HTML 原型设计，再按已确认方向落到正式 Vue 页面：检验项以最多 10 个完整显示的 2 行 x 5 列 tab 呈现，仅展开当前检验项详情，并将设备、编号、接收标准、检验方法改为与一线操作端一致的触控式信息卡。

## Milestones

- [x] 定位 PQC 页面现有入口与红框区域源码。
- [x] 读取前端样式与任务规则，识别适用设计约束。
- [x] 产出独立 HTML 原型文件。
- [x] 完成结构性验证与 UTF-8 读取验证。
- [x] 新增正式 PQC tab 布局静态契约并跑出 RED。
- [x] 改造正式 `FrontlineFixedTemplatePanel.vue` 的 PQC 检验内容区域。
- [x] 完成 GREEN、相邻回归和任务证据更新。

## Expected Verification

- 使用 UTF-8 方式读取原型与任务文档，确认中文无乱码。
- 静态检查原型包含推荐设计的关键结构：设备选择、设备编号、接收标准、检验方法、逐件选择。
- 运行 `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`，验证正式 PQC 页面使用 tab 网格、只展开当前项、10 个 tab 完整字段、设备/编号信息卡和无未样式化原生 select。
- 运行 `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js`，确认原有设备、编号、标准、方法提交与弹框契约仍保留。
- 运行 `pnpm ts:check`；若存在与本任务无关历史阻塞，记录首个阻塞点，不把阻塞写成通过。

## Current Status

ready_for_closeout

正式 Vue 页面改造已完成，专用静态契约、相邻 PQC 契约、前端证据校验和 `pnpm ts:check` 均通过；收尾仍被当前环境缺少 closeout cleanup 命令以及工作区非本任务脏改动阻塞，暂不能按项目 Git 门禁提交/推送并标记 completed。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；正式实现不新增兜底成功、静默吞错或兼容降级路径。
- 是否从根因和长期维护角度解决：是；根因是红框内原生控件密度、纵向列表和视觉语言脱离 PQC 操作端主风格，正式实现改为同源的触控式信息卡和 10-tab 网格。
- 是否存在临时补丁或绕过：否；按正式组件与静态契约落地，不以截图原型替代正式页面。

## Applicable Gates

- 前端样式门禁：红框区域属于前端页面局部 UI 设计，需遵循现有 PQC 操作端的粗边框、大字号、绿色操作台风格，不扩大成整页重设计。
- UTF-8 门禁：任务文档和 HTML 原型包含中文，读写必须保持 UTF-8。
- Git 门禁：当前工作区存在非本任务脏改动 `doc/tasks/20260805-ac-m04-acceptance-sync/`；本任务不回滚、不覆盖并发改动，收尾提交/推送需先处理该阻塞。

## Cleanup Keep

- doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.html
- doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.png
- doc/tasks/20260805-pqc-redbox-ui-prototype/frontend-feature-evidence.md

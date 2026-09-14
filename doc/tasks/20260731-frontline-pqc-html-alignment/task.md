# PQC 填写页面对齐 HTML 原型

## Task Goal

将真实前端 `PQC填写` 页签下的 `FrontlineFixedTemplatePanel.vue` 对齐
`output/frontline-pqc-operator-1920.html` 的页面结构、视觉层级和逐件检验交互，同时保持现有路由、API、模板契约、员工/工序数据源和提交失败门禁不变。

## Milestones

- [x] 读取目标 HTML、当前 Vue 页面和适用前端/E2E规则。
- [x] 保存共享工作区既有改动基线。
- [x] 建立目标页面结构和交互的 RED 静态合同。
- [x] 实现 PQC 页面布局、逐件检验弹框和本地填写状态。
- [x] 完成聚焦合同、相邻合同、类型检查和浏览器主布局验证。
- [x] 根据 review 修正首屏默认态和 PQC 选择弹窗文案，使巡检默认数量与目标 HTML 一致。
- [ ] 完成任务收尾、提交和推送。

## Expected Verification

- PQC 顶部保持真实 `生产订单 / 工序 / 员工 / 主页` 上下文。
- 左侧与目标 HTML 一致显示长度、外观、密封、压力四项。
- 长度和压力点击后打开逐件数值弹框；外观和密封显示 `全部合格 / 全部不良 / 逐件选择`。
- 逐件弹框使用 5 列网格，提供返回和完成操作，数值项支持默认值、加减和手工输入。
- 检验类型、巡检次数、检验数量和损耗数量继续可操作。
- 首屏默认保持 `巡检 / 第 1 次 / 检验数量 30 / 损耗数量 1`，检验内容进度显示 `已填 0/30`。
- PQC 工序选择弹窗文案与目标 HTML 一致：标题 `选工序`，关闭按钮 `返回`。
- 底部显示 `重填 / 提交`；重填只清除当前 PQC 本地填写上下文。
- PQC 提交仍使用现有正式 fail-fast 门禁，不修改 API、DTO、后端或数据源。
- 目标静态合同、相邻静态合同和 `pnpm ts:check` 通过。
- 可用本地运行态下通过 Playwright 验证真实页签布局；缺少登录或运行态时明确记录 blocker。

## Current Status

ready_for_closeout

## Closeout Blocker

- `git push origin int_main` 失败：`Failed to connect to github.com port 443 via 127.0.0.1 ... Could not connect to server`。
- 当前任务实现和验证已完成并已本地提交，但按项目规则，远端推送成功前不得标记为 `completed`。

## Verification Blocker

- 真实浏览器主布局验证已通过并生成截图。
- 真实逐件弹框交互验证被正式前置阻塞：本机 `芋道源码/admin` 的 `/admin-api/mes/pro/feedback/frontline/device-account/processes` 返回无可选工序，页面无法建立逐件检验所需的正式工序上下文。
- 按 no-fallback 规则，未伪造工序、未 mock API、未改数据源、未绕过 `assertPqcPieceContext`。
- 本次代码实现、静态合同、相邻合同、类型检查和主布局真实浏览器验证均已完成；该 blocker 仅限制“正式数据下逐件弹框交互”复验，不是推送 blocker。

## Applicable Gates

- 目标视觉来自 `output/frontline-pqc-operator-1920.html`，只修改真实前端呈现层。
- 保护 `src/api/**`、后端、DTO、数据库、路由权限和现有正式提交契约。
- 不使用 mock API、默认成功、吞异常或接口 fallback。
- 目标 HTML 中没有正式数据来源的订单、员工和工序示例值不得写入真实页面。
- 逐件值仅作为当前页面本地填写状态；正式 PQC payload 缺失时继续 fail fast。
- 使用任务专用静态合同执行 RED/GREEN，并复跑相邻合同和类型检查。
- 中文文档和源码保持 UTF-8。

## Baseline Commits

- `1cf2294e3`：并行任务提交的现有工作区全量基线。
- `62cdf8de2`：本线程补充保存其后出现的班组长页签并行改动。
- `c3c244f22`：本线程补充保存实现期间出现的生产报工 Excel 并行改动。
- `cd638236e`：本轮继续修正前保存既有脏工作区基线。
- `7002d82d6`：本轮继续修正前保存并发工艺路线改动基线。
- `a9deae829`：并发 PQC 可见性任务基线提交吸收了本轮首屏默认态和弹窗文案修正；未改写历史，后续按当前 HEAD 复验。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；真实页面直接复用目标原型的结构和交互模型，不通过外层缩放或遮挡伪装一致。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260731-frontline-pqc-html-alignment/frontend-feature-evidence.md

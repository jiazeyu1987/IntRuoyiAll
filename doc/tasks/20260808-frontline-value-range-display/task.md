# 一线数值项目标范围展示

## Task Goal

在一线生产/PQC 数值输入控件中，当检验项存在上下限限制时，在名称下方显示目标值范围；同时加宽名称区域，保证约 8 个中文字可单行显示，避免截图中四字换行过窄的问题。

## Milestones

- [x] 定位截图对应的前端组件、字段来源和现有样式契约
- [x] 补充 BDD 场景和 RED 静态合同，先证明当前缺少目标范围展示/名称宽度不足
- [x] 最小实现名称下方目标范围展示和名称区域加宽
- [x] 运行目标合同、相邻合同、类型/格式检查，并记录验证结果
- [x] 更新收尾文档和验证报告
- [x] 执行本机真实 Playwright E2E 复验并记录运行态证据（BLOCKED：缺少可渲染的正式 activeOrder + 上下限数值参数样本）

## Expected Verification

- 目标静态合同覆盖：有上下限限制时渲染目标范围；无上下限时不渲染范围占位；名称区域宽度足以容纳 8 个中文字单行显示。
- 相邻前端合同覆盖：数值输入、加减按钮、单位、超限红框逻辑不被改写。
- 真实 E2E 覆盖：本机 `8081/48081` 真实页面登录后打开一线生产目标页面，看到正式上下限目标范围，记录目标写请求数为 0、目标 UI 边界和页面错误。
- 运行 `pnpm ts:check` 或记录与本任务无关的既有阻塞。
- 运行 `git diff --check`。

## Applicable Gates

- 前端截图样式块静态契约门禁：先锁定目标选择器和样式块，避免跨块误判；截图局部样式需求不得扩大成整页重设计。
- 前端截图字号/布局调整门禁：若没有既有契约，新增任务专用静态合同并先跑 RED，再改最小 CSS。
- 严格无 fallback：不得用空范围、默认范围、隐藏占位或吞掉缺失字段来冒充上下限展示；只有正式上下限数据存在时才展示目标范围。

## Current Status

blocked

真实 Playwright 已在本机 `8081/48081` 执行，但当前 `芋道源码/admin` 设备账号可见工序均无法取得正式 `productionSubmitContext.activeOrder`，因此不能打开带上下限数值参数的真实一线生产页面完成目标范围 UI 断言；未用 mock、API-only 或静态合同冒充真实 E2E 通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是复用正式上下限字段生成可见范围并锁定布局契约。
- `是否存在临时补丁或绕过`：否。

## E2E Artifacts

- `doc/tasks/20260808-frontline-value-range-display/frontline-value-range-real.e2e.cjs`
- `doc/tasks/20260808-frontline-value-range-display/e2e-artifacts/frontline-value-range-e2e-result.json`
- `doc/tasks/20260808-frontline-value-range-display/e2e-artifacts/frontline-value-range-e2e-failure.png`

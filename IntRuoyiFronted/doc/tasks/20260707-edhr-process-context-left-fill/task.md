# eDHR 工序栏上下文左侧铺满

## 任务目标

按截图反馈修正工序栏顶部上下文：红框位置不留空，生产工单号和批记录信息从左侧开始占用顶部区域，不再挤到右侧。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`，命令输出显式 UTF-8，不使用 `&&`。
- 前端页面 / 样式：沿用已读取的 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，保持紧凑运维台视觉。
- 前端复刻：只改当前前端模板与样式，不改接口、DTO、后端、mock 或数据源。
- BDD/TDD：先记录 Given/When/Then 和 RED/GREEN 证据；静态测试锁定上下文不右对齐。
- 禁止 fallback：不新增降级、兜底、mock 或静默吞错。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，修正上下文容器对齐方式而非补空白占位。
- 是否存在临时补丁或绕过：否。

## BDD 场景

BDD: 工序栏上下文左侧铺满 -> Given 工序栏顶部不显示 `工序` 标题 / When 查看当前批记录上下文 / Then 工单号和批记录号从左侧开始显示，红框位置不留空。

## 里程碑

- [x] M1：创建任务文档并记录 BDD、门禁和设计约束。
- [x] M2：新增 RED 静态测试，证明当前上下文仍右对齐。
- [x] M3：修正上下文容器为左侧铺满显示。
- [x] M4：运行静态测试、回归测试和 lint，记录 GREEN 证据。
- [x] M5：收尾清理预览并按范围提交。

## 预期验证

- `node tests/e2e/edhr-process-header-left-fill-static.spec.js` 先 RED 后 GREEN。
- `node tests/e2e/edhr-process-header-compact-context-static.spec.js` 保持通过。
- `node tests/e2e/edhr-process-header-context-static.spec.js` 保持通过。
- focused `eslint` 通过。

## 完成记录

- 实现：工序栏顶部容器从 `flex-end` 改为 `flex-start`，不再把上下文挤到右侧。
- 样式：当前批记录上下文从 `text-align: right` 改为 `text-align: left`，红框位置不再留空。
- 回归：新增左侧铺满静态契约，并验证紧凑上下文、值展示和标题隐藏契约继续通过。

## 当前状态

completed

# eDHR 工序栏上下文移除淡蓝背景

## 任务目标

按截图反馈，工序栏顶部生产工单号和批记录号区域不再显示淡蓝色背景，保留左侧铺满和紧凑字号。

## 经验门禁

- PowerShell / Windows shell：沿用已读取的 `docs/powershell-memory.md`，命令输出显式 UTF-8，不使用 `&&`。
- 前端页面 / 样式：沿用已读取的 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，只调整当前局部样式。
- 前端复刻：只改当前前端样式，不改接口、DTO、后端、mock 或数据源。
- BDD/TDD：先记录 Given/When/Then 和 RED/GREEN 证据；静态测试锁定上下文顶部无淡蓝背景。
- 禁止 fallback：不新增降级、兜底、mock 或静默吞错。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，覆盖当前上下文顶部背景来源而不是增加占位元素。
- 是否存在临时补丁或绕过：否。

## BDD 场景

BDD: 工序栏上下文无淡蓝背景 -> Given 工序栏顶部展示生产工单号和批记录号 / When 页面渲染当前批记录上下文 / Then 顶部区域不显示淡蓝背景，仍从左侧铺满展示。

## 里程碑

- [x] M1：创建任务文档并记录 BDD、门禁和设计约束。
- [x] M2：新增 RED 静态测试，证明当前样式仍继承淡蓝背景。
- [x] M3：移除/覆盖淡蓝背景并保留左侧铺满。
- [x] M4：运行静态测试、回归测试和 lint，记录 GREEN 证据。
- [x] M5：收尾清理预览并按范围提交。

## 预期验证

- `node tests/e2e/edhr-process-header-no-blue-bg-static.spec.js` 先 RED 后 GREEN。
- `node tests/e2e/edhr-process-header-left-fill-static.spec.js` 保持通过。
- focused `eslint` 通过。

## 完成记录

- 实现：最终生效的工序栏顶部样式覆盖为透明背景，并移除底边线。
- 保留：上下文继续左侧铺满、11px 紧凑字号、不显示 `工序` 标题。
- 回归：新增无淡蓝背景静态契约，并验证左侧铺满、紧凑上下文和上下文展示契约继续通过。

## 当前状态

completed

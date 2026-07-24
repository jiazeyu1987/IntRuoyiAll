# eDHR 工序复盘左右区域宽度互换

## 任务目标

按截图要求，将工序复盘主区域左右两侧宽度互换：左侧工序列表由 260px 调整为 156px，右侧当前工序摘要由 156px 调整为 260px，中间表单区域保持自适应。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`，命令输出显式 UTF-8，不使用 `&&`。
- 前端页面 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，保持紧凑运维台视觉。
- 前端复刻：已读取 `replicate-frontend-ui`，只改当前前端样式和对应静态契约，不改接口、DTO、后端、mock 或数据源。
- BDD/TDD：先记录 Given/When/Then 和 RED/GREEN 证据；静态测试锁定左右栏宽度互换。
- 禁止 fallback：不新增降级、兜底、mock 或静默吞错。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接调整三栏网格列宽定义并同步宽度契约。
- 是否存在临时补丁或绕过：否。

## BDD 场景

BDD: 左右栏宽度互换 -> Given 用户打开 eDHR 批次详情页 / When 查看工序复盘主区域 / Then 左侧工序列表使用 156px 宽度，右侧当前工序摘要使用 260px 宽度，中间表单区域保持自适应。

## 里程碑

- [x] M1：创建任务文档并记录 BDD、门禁和设计约束。
- [x] M2：新增 RED 静态测试，证明当前仍是左宽右窄。
- [x] M3：调整三栏网格为左窄右宽并同步旧宽度断言。
- [x] M4：运行静态测试、回归测试和 lint，记录 GREEN 证据。
- [x] M5：收尾清理预览并按范围提交。

## 预期验证

- `node tests/e2e/edhr-review-width-swap-static.spec.js` 先 RED 后 GREEN。
- `node tests/e2e/edhr-batch-basic-info-dialog-static.spec.js` 保持通过。
- `node tests/e2e/edhr-review-summary-right-rail-static.spec.js` 保持通过。
- `node tests/e2e/edhr-process-form-action-columns-static.spec.js` 保持通过。
- focused `eslint` 通过。

## 完成记录

- 实现：工序复盘三栏网格从 `260px minmax(0, 1fr) 156px` 调整为 `156px minmax(0, 1fr) 260px`。
- 保留：中间表单区域仍为 `minmax(0, 1fr)` 自适应，右侧摘要栏保持 sticky。
- 回归：同步基础信息弹框、右侧摘要栏、三栏布局和宽度互换静态契约。

## 当前状态

completed

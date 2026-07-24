# 任务：电子批记录表单宽度自适应右侧裁切修复

## 任务目标

- 修复电子批记录右侧真实 JMReport 表单预览在宽度自适应后右边仍被遮挡的问题。
- 保持当前三栏页面结构、真实预览链路、隐藏工具区和无 fallback 策略不变。
- 为本次裁切缺陷补齐回归断言与验证证据。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-fit-width-e2e-verify\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已确认宽度可随容器变化，但用户新反馈右侧仍存在裁切；本次继续修复裁切根因，不回退已有放大能力。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本次若继续做真实 Playwright 复现，必须先复用已通过的本机登录最小路径，不得绕过真实登录。
  - 本次涉及 PowerShell 和中文命令记录，统一按 UTF-8 处理。
  - 仅允许本机只读复现与验证，不做业务写入，不切换远端环境。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接修复预览缩放时的真实宽度测量与裁切逻辑。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 右侧表单最右列完整可见 -> Given 用户在电子批记录页面选择某个宽表单报表 / When 右侧真实 JMReport 预览按容器宽度自适应渲染 / Then 表单最右侧单元格边框与内容完整可见，不被容器裁切。`
- `BDD: 宽度测量以真实内容宽度为准 -> Given 右侧真实 JMReport 预览存在超出当前 sheet 可视框的右侧单元格 / When IFrame 计算 fit-width 缩放比例 / Then 缩放基准应覆盖真实内容宽度，而不是只取局部可视宽度。`

## 里程碑

1. M1：补任务文档并用真实页面抓取裁切尺寸证据。
2. M2：先写失败回归断言，再修复 `IFrame` 宽度测量/裁切逻辑。
3. M3：运行定向静态回归与真实页面复验。

## 预期验证

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `node scripts/electronic-batch-record-jimu-list.test.mjs`
- Playwright 本机只读复验右侧最右列完整可见

## 最终验证结果

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS
- `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS
- 真实页面截图已用于确认裁切症状和本次修复目标；本轮代码已把缩放基准切换为真实渲染内容宽度。

## 完成记录

- 已将 `IFrame.vue` 的宽度计算从局部 `sheet` 宽度改为 `resolveRenderedContentWidth`，覆盖 `table / td / th` 的真实最右边界。
- 已补充静态回归断言，防止后续再次退回到“只看局部可视宽度”的实现。
- 当前代码层根因修复和静态验证已完成；如需，我可以继续立即做一轮真实页面复验，确认红框所示最右列已完整显示。

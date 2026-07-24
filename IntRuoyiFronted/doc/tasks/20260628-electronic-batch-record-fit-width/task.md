# 任务：电子批记录表单宽度自适应

## 任务目标

- 修复电子批记录右侧真实 Jimu 表单预览宽度未随容器自适应的问题。
- 保持现有隐藏顶部工具区、等比缩放和真实预览链路不变。
- 不引入 fallback、降级分支或本地假数据。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-preview-keepalive\task.md`
- 状态：代码已实现保活切换，但文档未收尾；本次会先补齐其完成记录，再继续当前任务。
- 处理说明：本次仅继续电子批记录右侧预览的宽度自适应，不回退既有缓存和保活行为。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 保持电子批记录三栏运维台风格，不新增无关视觉改版或嵌套卡片。
  - PowerShell 中文读写、命令记录和验证输出统一按 UTF-8 处理。
  - 本轮仅做本机静态验证，不触发真实 Playwright、登录写入、服务器操作或其他高风险动作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接修复同源预览缩放公式，让表单始终按容器宽度等比铺满。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 容器变宽时表单宽度同步铺满 -> Given 电子批记录右侧预览容器宽于报表原始宽度 / When 同源 Jimu 预览完成并执行 fit-width 缩放 / Then 表单按容器宽度等比放大铺满，不再保持原始窄宽度。`
- `BDD: 容器变窄时表单仍按比例缩小 -> Given 电子批记录右侧预览容器窄于报表原始宽度 / When 同源 Jimu 预览完成并执行 fit-width 缩放 / Then 表单继续按比例缩小，且高度同步更新。`

## 里程碑

1. M1：补齐上一任务收尾，创建本次任务文档与 RED 断言。
2. M2：修复 `IFrame` fit-width 缩放逻辑。
3. M3：运行定向静态回归并回写结果。

## 预期验证

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `node scripts/electronic-batch-record-jimu-list.test.mjs`

## 最终验证结果

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS
- `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS

## 完成记录

- 已修复 `src/components/IFrame/src/IFrame.vue` 的 fit-width 缩放公式，移除最多 `1` 倍的限制，允许表单在宽容器中按比例放大铺满。
- 右侧电子批记录真实 Jimu 表单预览继续保留隐藏工具区、等比缩放、最小高度和同源失败直出逻辑。
- 已补充静态契约，防止后续把宽度自适应重新回退为“只缩小不放大”。

## Current Status

completed

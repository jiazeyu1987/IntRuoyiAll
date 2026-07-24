# 任务：排程日历历史卡片淡灰底色

## 任务目标

- 将 `/mes/pro/schedule-calendar` 中今天之前的日历卡片背景色调整为淡灰色。
- 保持今天及未来日期、选中态、班次编辑能力和其他业务数据展示逻辑不变。
- 用静态回归测试锁定“历史日期卡片 = 只读 + 淡灰底色”的规则，避免样式回退。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-schedule-order-process-dialog-width\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成；本次仅处理排程日历历史日期卡片底色，不混入其他 MES 页面或交互改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端样式需保持 IntPP 运维台风格，使用克制的浅灰蓝中性色，不做无关视觉重做。
  - PowerShell 读取和记录中文文件时必须显式使用 UTF-8。
  - 本次仅做本地前端静态回归与类型校验，不涉及真实登录、写入、服务器操作或长链路 E2E，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接在排程日历历史日期卡片样式层修正视觉状态，并以静态回归固定只读历史卡片的底色规则。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 历史日期卡片默认呈现淡灰底色 -> Given 用户打开排程日历 / When 某个日期早于今天且不可编辑 / Then 该卡片应显示淡灰色背景，便于与今天及未来日期区分。`
- `BDD: 历史日期卡片悬停时仍保持历史态视觉 -> Given 用户把鼠标移到今天之前的卡片 / When 卡片保持只读状态 / Then 悬停不应回退成普通白底，而应继续保持淡灰色背景。`

## 里程碑

1. M1：建立任务台账并补历史卡片样式 RED 静态回归。`COMPLETED`
2. M2：最小修改排程日历历史日期卡片样式。`COMPLETED`
3. M3：运行 GREEN 静态回归与定向类型校验。`COMPLETED`
4. M4：回填证据、命令记录和收尾预览。`COMPLETED`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-past-card-gray-static.spec.js`
- `pnpm ts:check`

## 最终验证结果

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-past-card-gray-static.spec.js` -> PASS
- `pnpm ts:check` -> BLOCKED，工作区现有 `pnpm-lock.yaml` 供应链策略校验失败，错误为既有 tarball URL mismatch。
- `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> BLOCKED，工作区现有无关类型错误位于 `src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue`。

## 完成记录

- 排程日历今天之前的卡片已改为淡灰底色 `#f3f4f6`。
- 历史日期卡片 hover 时保持同一淡灰底色，不再回退成普通白底。
- 静态回归已锁定 `is-readonly-past` class、默认底色与 hover 底色规则。

## Current Status

completed

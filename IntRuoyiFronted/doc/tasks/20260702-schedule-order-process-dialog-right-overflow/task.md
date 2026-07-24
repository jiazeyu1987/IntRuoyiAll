# 任务：排产工单工艺路线弹窗右侧显示完整修复

## 任务目标

- 修复 `/mes/pro/schedule-order` 中“工艺排产路线”弹窗右侧内容显示不全的问题。
- 保持现有工序汇总列、展开报工明细、状态颜色和数据来源不变，只修正弹窗承载宽度与表格可视区域。
- 桌面端必须完整显示右侧 `预计结束` 列；窄视口下通过横向滚动查看全部列，不直接裁切右侧内容。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260702-dcc-preview-css-zoom\task.md`
- 状态：`completed`
- 处理说明：本次只处理排产工单查看弹窗右侧裁切，不混入其他 DCC 或 MES 页面改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 保持 IntPP 运维台风格，延续浅边框、紧凑表格和稳定列宽，不做无关视觉重做。
  - 表格类页面必须使用显式列宽与稳定容器，避免右侧列被压缩或裁切。
  - PowerShell 读取和记录中文文件时必须显式使用 UTF-8。
  - 本轮先做静态契约和代码回归，不执行真实登录、写入或服务器操作，因此无需高风险 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只修正弹窗宽度、表格最小宽度与滚动承载，不增加兜底分支。
- `是否从根因和长期维护角度解决`：是。按当前实际 9 个工序汇总列重新约束可视宽度，而不是删除列或缩短标题绕过。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工艺排产路线弹窗完整显示右侧预计结束列 -> Given 用户在排产工单列表打开工艺排产路线 / When 弹窗渲染当前 9 个工序汇总列 / Then 右侧预计结束列必须完整纳入弹窗可视或滚动区域，不被页面边界裁切。`
- `BDD: 窄视口下工艺排产路线可横向查看全部列 -> Given 工艺排产路线列宽总和超过视口可用宽度 / When 用户打开弹窗 / Then 表格容器必须提供横向滚动，且主表最小宽度覆盖所有工序汇总列。`

## 里程碑

1. M1：建立任务台账并补充当前 9 列宽度 RED 静态回归。`COMPLETED`
2. M2：最小修改工艺排产路线弹窗宽度与表格容器。`COMPLETED`
3. M3：运行 GREEN 静态回归与相关列契约验证。`COMPLETED`
4. M4：回写执行证据、命令记录和收尾预览。`COMPLETED`

## 预期验证

- `node tests/e2e/mes-schedule-order-route-progress-dialog-width-static.spec.js`
- `node tests/e2e/mes-schedule-order-route-progress-columns-static.spec.js`
- `node tests/e2e/mes-schedule-order-route-progress-view-static.spec.js`

## 最终验证结果

- `node tests/e2e/mes-schedule-order-route-progress-dialog-width-static.spec.js` -> PASS
- `node tests/e2e/mes-schedule-order-route-progress-columns-static.spec.js` -> PASS
- `node tests/e2e/mes-schedule-order-route-progress-view-static.spec.js` -> PASS

## 完成记录

- “工艺排产路线”查看弹窗已扩大为 `width="min(1360px, calc(100vw - 24px))"`，优先使用桌面视口宽度。
- 工序汇总主表新增独立类名 `schedule-order-pool__process-summary-table`，主表最小宽度约束不再影响展开的历史报工明细表。
- 当前 9 个工序汇总列已收紧为稳定列宽，右侧 `预计结束` 列可通过弹窗可视区域或横向滚动完整查看。

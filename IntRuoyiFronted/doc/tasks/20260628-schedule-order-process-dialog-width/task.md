# 任务：排产工单工艺路线查看弹窗宽度修复

## 任务目标

- 修复 `/mes/pro/schedule-order` 中“查看 -> 工艺排产路线”弹窗宽度不足、右侧列内容显示不全的问题。
- 保持现有 7 个关键列、状态色和数据来源不变，只修正弹窗承载宽度与表格可视区域。
- 桌面端优先保证“预计结束”列完整可见，窄视口下也不能直接裁掉右侧列。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-mes-scheduler-workbench-policy-label-overlap\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成，本次只处理排产工单查看弹窗宽度与表格显示缺陷，不混入排产员工作台或其他 MES 页面改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 保持 IntPP 运维台风格，延续浅边框、紧凑表格和稳定列宽，不做无关视觉重做。
  - PowerShell 读取和记录中文文件时必须显式使用 UTF-8。
  - 本轮先做静态契约和代码回归，不执行真实登录、写入或服务器操作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只修正弹窗宽度与表格容器，不增加兜底分支。
- `是否从根因和长期维护角度解决`：是。直接修正工艺排产路线查看弹窗的可用展示宽度，并为窄视口保留横向查看能力，而不是单纯删除列或缩短标题。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工艺排产路线查看弹窗在桌面端完整展示右侧列 -> Given 用户在排产工单列表点击查看工艺排产路线 / When 弹窗渲染 7 个关键列 / Then 弹窗宽度必须足够让 预计结束 列在桌面视口内完整可见，不再被右侧裁掉。`
- `BDD: 窄视口下工艺排产路线仍可横向查看全部列 -> Given 工艺排产路线弹窗列宽总和大于可用视口 / When 用户打开查看弹窗 / Then 弹窗内容区必须提供明确的横向滚动承载，不能直接裁掉最后一列。`

## 里程碑

1. M1：建立任务台账并补弹窗宽度 RED 静态回归。`COMPLETED`
2. M2：最小修改排产工单查看弹窗宽度与表格容器。`COMPLETED`
3. M3：运行 GREEN 静态回归与定向验证。`COMPLETED`
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

- “工艺排产路线”查看弹窗已从固定 `960px` 调整为响应式更宽宽度，桌面端可完整承载右侧 `预计结束` 列。
- 弹窗表格新增专用横向滚动容器和最小表宽约束，窄视口下不再直接裁掉最后一列。
- 既有工艺路线查看静态契约已同步稳固到多行属性写法，不再因弹窗标签换行或前置弹窗误报。

## Current Status

completed

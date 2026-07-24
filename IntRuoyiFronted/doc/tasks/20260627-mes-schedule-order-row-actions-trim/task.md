# 任务：排产工单行操作收缩为查看、调整、冻结

## 任务目标

- 将排产工单列表行操作区收缩为仅保留 `查看`、`调整`、`冻结` 三个按钮。
- 删除行操作区中的 `解冻` 和 `更多` 下拉入口，不再从该位置暴露对比、快照、删除、追溯等其他行级按钮。
- 保持现有后端接口合同不变，仅调整前端行操作入口与对应静态回归。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端相关任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260627-mes-feedback-refresh-scheduleorder-after-submit\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成“确认报工成功后自动刷新排产工单”；本次继续收缩排产工单行操作区。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本轮只做本机前端模板、静态契约和任务文档，不进入真实登录写入或 Playwright 长链路。
  - 保持 IntPP 操作台式样，行操作继续使用紧凑的同排文本按钮，不引入无关视觉改造。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。按钮收缩后不新增兼容分支或静默兜底。
- `是否从根因和长期维护角度解决`：是。直接收缩行操作入口，避免继续通过下拉菜单暴露多余操作。
- `是否存在临时补丁或绕过`：否。不通过样式隐藏或权限绕过实现。

## BDD 场景

- `BDD: 排产工单行操作仅保留三个入口 -> Given 用户打开排产工单列表 / When 页面渲染任一行操作列 / Then 只显示查看、调整、冻结三个按钮，不再显示解冻或更多下拉。`
- `BDD: 已冻结排产工单不再显示解冻按钮 -> Given 排产工单已冻结 / When 页面渲染行操作列 / Then 仍只保留查看、调整、冻结三个入口，其中冻结按钮可见但不可再次执行。`

## 里程碑

1. M1：创建任务包并定位排产工单行操作当前入口。`COMPLETED`
2. M2：先补失败中的静态契约，再收缩操作区为查看、调整、冻结。`COMPLETED`
3. M3：回归静态测试并补齐证据文档。`COMPLETED`

## 预期验证

- `node tests/e2e/mes-schedule-order-row-actions-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260627-mes-schedule-order-row-actions-trim/frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/mes-schedule-order-row-actions-static.spec.js` -> PASS
- `node tests/e2e/mes-schedule-order-route-progress-view-static.spec.js` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260627-mes-schedule-order-row-actions-trim/frontend-feature-evidence.md` -> PASS

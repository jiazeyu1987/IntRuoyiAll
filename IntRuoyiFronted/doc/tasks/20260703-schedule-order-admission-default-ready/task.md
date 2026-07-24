# 20260703 排产待同步差异默认可入池

## 任务目标

- 将排产工单“待同步差异”弹窗中的“入池状态”默认值从“全部”修正为“可入池”。
- 保持用户手动清空后查询全部的能力不变，仅修正默认打开与重置后的筛选状态。

## 里程碑

1. 读取项目经验门禁、前端交付与缺陷修复契约。completed
2. 定位待同步差异弹窗的入池状态查询参数。completed
3. 补充 RED 静态回归，证明默认入池状态不能是空值。completed
4. 最小实现默认值与重置逻辑，并运行 GREEN 验证。completed
5. 更新任务文档、执行日志和收尾记录。completed

## 预期验证

- `node tests/e2e/mes-pro-schedule-order-admission-default-static.spec.js` 先 RED 后 GREEN。
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` 作为全量回归参考执行，若被既有无关断言阻塞则记录阻塞点。
- 聚焦检查 `src/views/mes/pro/scheduleorder/index.vue` 中默认查询参数和重置逻辑。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；后续中文读写使用 UTF-8 路径，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次不做视觉重设计，仅改筛选默认状态。
- 缺陷修复闭环：按复现、RED 回归、最小修复、GREEN 验证记录证据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，统一默认入池状态常量并复用到初始化与重置。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 待同步差异默认筛选可入池 -> Given 用户打开“待同步差异”弹窗 / When 弹窗首次加载待同步生产工单 / Then “入池状态”默认选中“可入池”，请求参数为 `READY_TO_ADMIT`。
- BDD: 重置后仍回到可入池 -> Given 用户修改或清空“入池状态”筛选 / When 点击“重置” / Then “入池状态”恢复为“可入池”，而不是“全部”。

## 当前状态

- 状态：completed
- 已完成默认“可入池”修复、独立 GREEN 验证和收尾预览；未提交，因前端仓存在多项非本任务既有改动，需避免误纳入。

## 验证结果

- RED: 聚焦静态断言 -> FAIL，缺少 `DEFAULT_WORK_ORDER_ADMISSION_STATUS`，且 `admissionStatus` 默认仍为 `undefined`。
- GREEN: 聚焦静态断言 -> PASS，`admissionStatus` 默认和重置后均为 `READY_TO_ADMIT`。
- GREEN: `node tests/e2e/mes-pro-schedule-order-admission-default-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> FAIL，既有无关断言 `Schedule order list must show completed quantity.` 阻塞在第 60 行，未进入本次新增断言区域。
- CLOSEOUT: `task_closeout.py --task-id 20260703-schedule-order-admission-default-ready --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。

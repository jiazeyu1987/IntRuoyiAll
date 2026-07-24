# 20260703 排产待同步差异默认可入池执行日志

## BDD

- BDD: 待同步差异默认筛选可入池 -> Given 用户打开“待同步差异”弹窗 / When 弹窗首次加载待同步生产工单 / Then “入池状态”默认选中“可入池”，请求参数为 `READY_TO_ADMIT`。
- BDD: 重置后仍回到可入池 -> Given 用户修改或清空“入池状态”筛选 / When 点击“重置” / Then “入池状态”恢复为“可入池”，而不是“全部”。

## 执行记录

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`bug-regression-fix-loop`、`frontend-feature-delivery` 和 `FRONTEND_STYLE.md` -> PASS。
- 定位 `src/views/mes/pro/scheduleorder/index.vue`：`workOrderAdmissionQueryParams.admissionStatus` 当前为 `undefined`，`resetWorkOrderAdmissionQuery` 仅调用表单重置，会回到“全部”。
- RED: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> FAIL，先被既有无关断言 `Schedule order list must show completed quantity.` 阻塞。
- RED: 聚焦静态断言 -> FAIL，`Admission diff dialog must define READY_TO_ADMIT as the default admission status.`
- GREEN: 聚焦静态断言 -> PASS，输出 `PASS: admission default READY_TO_ADMIT static contract`。
- REGRESSION: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> FAIL，仍为既有无关断言 `Schedule order list must show completed quantity.`。
- 新增独立静态回归文件 `tests/e2e/mes-pro-schedule-order-admission-default-static.spec.js`，避免本次验证被既有无关静态合同阻塞。
- GREEN: `node tests/e2e/mes-pro-schedule-order-admission-default-static.spec.js` -> PASS，输出 `PASS: MES schedule order admission default static contract`。
- CLOSEOUT: `task_closeout.py --task-id 20260703-schedule-order-admission-default-ready --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
- COMMIT: 暂不提交；前端仓存在多项非本任务既有改动（DCC browser、tree expand、旧任务目录等），为避免误提交无关变更，本轮仅保留已验证改动。

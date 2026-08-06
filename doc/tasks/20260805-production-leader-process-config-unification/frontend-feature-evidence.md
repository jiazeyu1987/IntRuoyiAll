# P3 Frontend Feature Evidence - Production Leader Unified Process Config

## Feature

- Feature goal: 将生产组长原“损耗管理”、工序设备映射和设备参数设置收敛到一个以 `routeProcessId` 为行键的“工序配置”入口。
- Non-goals: 不改设备档案主数据入口，不扩大生产组长授权范围，不引入旧接口别名、默认平均值、本地假回显或兼容双写。
- UI entry point: `TeamLeaderWorkbenchPage.vue` 的生产组长模块 Tab `工序配置 / processConfig`。
- Owned frontend files: `IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`、`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`、`IntRuoyiFronted/tests/e2e/team-leader-process-config-unified-static.spec.cjs`。

## Acceptance

- P3-AC1: 生产组长页面只保留一个“工序配置”统一入口；旧独立损耗表、裸 `processId/deviceId` 设备映射表单和裸 ID 参数表单不再作为可操作入口。
- P3-AC2: 统一表按路线工序展示损耗原因、映射设备、参数标准完成情况；设备参数展示下限、目标值、上限、单位、值类型、实际平均值、样本数和统计周期。
- P3-AC3: 损耗、设备映射和参数维护都从当前表格行进入，弹窗冻结 `routeProcessId`；设备使用可选列表，平均值、样本数和统计周期只读。
- P3-AC4: 前端提交前校验必填项和 `lower <= target <= upper`；保存成功后重新读取正式统一行数据。
- P3-AC5: `actualAverage=null` 显示为“暂无样本”，样本数显示 `0`，不使用目标值冒充平均值。
- P3-AC6: 关键控件提供稳定 `data-*` 选择器，统一表按 `routeProcessId` 设置行键，旧重复入口和旧写接口调用被移除。

## API Contracts And Data States

- Unified list: `GET /mes/pro/process-pool/team-leader/process-config/list` returns `TeamLeaderProcessConfigRowRespVO[]` with nested devices and parameters.
- Device binding: `POST /mes/pro/process-pool/team-leader/process-config/device-binding/save` accepts `routeProcessId + deviceId`.
- Parameter rule save: `POST /mes/pro/process-pool/team-leader/process-config/device-parameter-rule/save` accepts `routeProcessId + deviceId + parameterCode + lowerLimit + targetValue + upperLimit + valueType`.
- Read-only statistics: `actualAverage`, `sampleCount`, `statisticsStartTime`, `statisticsEndTime` and `statisticsWindowDays` are displayed from backend rows and are not editable in the parameter dialog.

## BDD Scenarios

- BDD: P3 单一工序配置入口 -> Given 当前用户为生产组长 / When 打开生产组长工作台 / Then 页面展示“工序配置”统一入口，不再提供独立“损耗管理”入口或裸 ID 配置卡片。
- BDD: P3 路线工序统一行展示 -> Given 后端返回授权路线工序配置行 / When 统一表加载 / Then 每行按 `routeProcessId` 展示损耗原因、映射设备、参数标准和统计字段。
- BDD: P3 行上下文维护 -> Given 用户从某一工序行点击维护动作 / When 打开设备或参数弹窗 / Then 弹窗冻结该行 `routeProcessId`，设备从候选列表选择，参数平均值和样本数只读。
- BDD: P3 参数区间校验 -> Given 用户填写参数上下限和目标值 / When `lower > target` 或 `target > upper` / Then 前端阻止提交并显示明确错误。
- BDD: P3 保存后正式刷新 -> Given 设备映射、参数规则或损耗原因保存成功 / When 弹窗关闭 / Then 页面调用统一列表重新读取正式行，不使用本地数组假回显。

## RED Command

- RED: `node IntRuoyiFronted\tests\e2e\team-leader-process-config-unified-static.spec.cjs` -> FAIL，预期原因为前端尚未暴露 `TeamLeaderProcessConfigRowRespVO`、统一 `process-config/list` API、`工序配置` Tab、`routeProcessId` 行上下文和 `targetValue` 参数表单合同。

## GREEN Commands

- GREEN: `pnpm ts:check` from `IntRuoyiFronted` -> PASS，Vue TypeScript 检查退出码 `0`。
- GREEN: `node IntRuoyiFronted\tests\e2e\team-leader-process-config-unified-static.spec.cjs` -> PASS，`team-leader-process-config-unified-static PASS`。
- GREEN: `node IntRuoyiFronted\tests\e2e\team-leader-workbench-static.spec.cjs` -> PASS，`PASS: team leader workbench static contract is wired`。
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS，`mes-process-pool-team-leader-static PASS`。
- GREEN: `node IntRuoyiFronted\tests\e2e\frontline-team-config-static.spec.cjs` -> PASS，`PASS: frontline team runtime config static contract is wired`。

## Verification

- Static contract confirms the frontend API no longer calls old `/process-device-binding/save` or `/runtime-device-parameter-rule/save` paths.
- Static contract confirms the old standalone `损耗管理 / loss` tab and old `processDeviceBindingForm` / `deviceRuleForm.defaultValue` UI contracts are absent.
- `TeamLeaderWorkbenchPage.vue` uses stable selectors for the unified root, table, loss reasons, devices, parameters, device binding, parameter edit and loss maintenance actions.
- Parameter save validates finite lower, target and upper values and blocks invalid ranges before calling the backend save API.
- Successful device binding, parameter save and loss reason maintenance reload the formal unified config rows via `await loadProcessConfigRows()`.

## Responsive Accessibility Loading Empty Error Permission Checks

- Responsive and testability: the unified table uses `routeProcessId` row keys and stable `data-*` selectors for desktop and mobile Playwright targeting.
- Loading state: `processConfigLoading` wraps the unified table and refresh button.
- Empty state: unified table renders normal Element Plus empty table state when no authorized process rows exist.
- Error state: frontend validation errors are surfaced through `message.error`; backend request errors are not swallowed.
- Permission boundary: frontend relies on P2 backend authorization and only calls route-process scoped endpoints.

## Blockers

- P3 frontend static and type gates are clear.
- P4 real-browser validation is not completed yet and must still verify live production leader configuration, formal frontline submission averages, screenshots, traces and task data cleanup.

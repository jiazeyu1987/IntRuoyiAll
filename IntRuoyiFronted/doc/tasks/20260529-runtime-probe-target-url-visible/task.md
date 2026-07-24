# 任务：运行控制台探针目标地址可见

## 任务目标

- 在运行控制台探针状态表中直接显示每条探针的目标地址，便于从页面定位 IP、端口和路径。
- 复用现有 `/infra/runtime-control/probes/latest` 与 `/probes/run` 响应中的 `url` 字段，不改变前后端接口结构。
- 保持运维控制台紧凑表格风格，不新增说明性文案或降级逻辑。

## BDD 场景

- BDD: 探针表格显示目标地址 -> Given 后端返回探针记录包含 `url` / When 用户打开运行控制台的探针状态区域 / Then 表格必须展示“目标地址”列，内容为该探针真实 URL。
- BDD: 探针目标地址缺失时不伪造地址 -> Given 探针记录没有 `url` / When 用户查看探针状态表 / Then 页面保留为空值展示，不生成默认 IP 或 mock 地址。

## 里程碑

- [x] M1：确认前端旧任务已完成，定位运行控制台探针组件和 API 类型。
- [x] M2：补充失败测试，约束探针表格必须展示目标地址列并渲染 `row.url`。
- [x] M3：实现探针表格目标地址展示。
- [x] M4：运行静态测试、类型检查和真实页面轻量验证。
- [x] M5：记录证据、运行收尾清理预览并提交本任务改动。

## 预期验证

- `node tests\e2e\runtime-control-foolproof-static.spec.js`
- `node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- Playwright 真实页面轻量验证：`http://127.0.0.1:8081/infra/monitors/runtime-control`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260529-runtime-probe-target-url-visible/frontend-feature-evidence.md`

## 当前状态

completed

## 当前进展

- 已在探针状态表新增“目标地址”列，绑定 `RuntimeControlProbeVO.url`。
- 已完成静态合约、类型检查和 Playwright 真实页面只读验证。

## 验证结果

- RED: `node tests\e2e\runtime-control-foolproof-static.spec.js` -> FAIL，缺少 `probe target URL table column`。
- GREEN: `node tests\e2e\runtime-control-foolproof-static.spec.js` -> PASS。
- GREEN: `node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: Playwright 真实页面只读验证 `http://127.0.0.1:8081/infra/monitors/runtime-control` -> PASS，探针卡片显示“目标地址”，可见 URL `http://127.0.0.1:48081/actuator/health`，未提交运行控制台动作请求。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260529-runtime-probe-target-url-visible/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-runtime-probe-target-url-visible --mode preview` -> PASS，delete/blocked/warnings 均为空。

## Cleanup Keep

- doc/tasks/20260529-runtime-probe-target-url-visible/task.md
- doc/tasks/20260529-runtime-probe-target-url-visible/execution-log.md
- doc/tasks/20260529-runtime-probe-target-url-visible/frontend-feature-evidence.md

# 执行日志：运行控制台探针目标地址可见

BDD: 探针表格显示目标地址 -> Given 后端返回探针记录包含 `url` / When 用户打开运行控制台的探针状态区域 / Then 表格必须展示“目标地址”列，内容为该探针真实 URL。

BDD: 探针目标地址缺失时不伪造地址 -> Given 探针记录没有 `url` / When 用户查看探针状态表 / Then 页面保留为空值展示，不生成默认 IP 或 mock 地址。

## 证据

- M1: 已确认 `src/api/infra/runtimeControl/index.ts` 的 `RuntimeControlProbeVO` 已包含 `url?: string`，`OpsProbeStatusPanel.vue` 当前探针表未展示目标地址。
- RED: `node tests\e2e\runtime-control-foolproof-static.spec.js` -> FAIL，缺少 `probe target URL table column`，探针组件尚未展示目标地址列。
- M3: 已在 `OpsProbeStatusPanel.vue` 增加“目标地址”列，直接绑定 `prop="url"`，长地址沿用表格溢出提示。
- GREEN: `node tests\e2e\runtime-control-foolproof-static.spec.js` -> PASS。
- GREEN: `node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: Playwright 真实页面只读验证 `http://127.0.0.1:8081/infra/monitors/runtime-control` -> PASS，探针卡片显示“目标地址”，可见 URL `http://127.0.0.1:48081/actuator/health`，未提交运行控制台动作请求。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260529-runtime-probe-target-url-visible/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-runtime-probe-target-url-visible --mode preview` -> PASS，delete/blocked/warnings 均为空。

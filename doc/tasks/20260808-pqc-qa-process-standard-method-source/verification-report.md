# Verification Report

## Scope

- 修正一线 PQC“接收标准 / 检验方法”弹框，使正文、卡片摘要和 `pqcItemDetails` 快照使用 QA 工序列别名 `acceptanceStandard/processInspectionMethod`。
- 后端一线 PQC 响应新增 `acceptanceStandard/processInspectionMethod`，由发布 QA 规程项目 `standardText/inspectionMethod` 显式映射。
- 保留旧 `standardText/inspectionMethod` 字段、设备选项、上下限、单位、精度、结果类型和提交身份字段。

## RED / GREEN Evidence

- RED: `node tests\e2e\frontline-pqc-qa-process-standard-method-source-static.spec.cjs` -> FAIL；失败点为 `PqcInspectionItem` 缺少 `acceptanceStandard`。
- GREEN: `node tests\e2e\frontline-pqc-qa-process-standard-method-source-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\frontline-pqc-fact-dialog-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\pqc-item-equipment-standard-method-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\pqc-active-title-method-display-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\pqc-tab-method-display-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\pqc-tab-item-name-display-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\edhr-frontline-pqc-html-alignment-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- GREEN: `git diff --check` -> PASS；仅 CRLF 工作区提示，无 whitespace error。
- GREEN: frontend feature evidence validator -> PASS。
- CLEANUP: `task_closeout.py --task-id 20260808-pqc-qa-process-standard-method-source --mode apply` -> applied；仅删除临时 `frontend-feature-evidence.md`。
- EXPERIENCE: 已合并长期经验到 `docs/backend-development.md#MES PQC 项目级检验快照门禁`。

## Runtime Page Probe

- BLOCKED: 2026-08-08 追加真实页面验证时，本机后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`，前端 `http://127.0.0.1:8081/` 返回 HTTP 200。
- BLOCKED: 临时 Playwright 只读探针登录 `芋道源码/admin` 后打开 `/mes/pro/feedback/edhr-batch-pqc-fill`，遍历页面可选 7 个待检 PQC 工单，`/admin-api/mes/pro/feedback/frontline/device-account/pqc/active-order/processes` 仍只返回旧字段 `standardText/inspectionMethod`，未返回新字段 `acceptanceStandard/processInspectionMethod`。
- BLOCKED: 当前本机运行态无法证明弹框读取 QA 工序列别名；需要刷新/重启到包含本次后端 VO 映射的运行态后，再做真实点击验证。

## Acceptance

- 接收标准弹框正文读取 `activePqcStandardItem.acceptanceStandard`，不再直接读取旧 `standardText`。
- 检验方法弹框标题和正文读取 `formatPqcMethodSummary(activePqcMethodItem)`，该 helper 只基于 `processInspectionMethod`。
- 卡片摘要与弹框正文同源；接收标准缺失时显示“未配置接收标准”，不再用上下限合成替代。
- `pqcItemDetails` 快照写入同一 QA 工序列文本，避免组长页与一线弹框展示来源不一致。

## Blockers

- 真实页面运行态验证 BLOCKED：本机接口尚未加载本次新增的 `acceptanceStandard/processInspectionMethod` 响应别名；静态合同、类型检查和后端编译已通过。

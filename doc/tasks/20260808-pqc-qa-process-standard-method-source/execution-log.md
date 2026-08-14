# Execution Log

## Intent

用户指出“接收标准”和“检验方法”应对应 QA 工序表格中当前工序 / 当前检验项目行的“接收标准”和“检验方法”列数据；当前弹框仍可能显示默认首检摘要或“符合/不符合”等不对应列数据的内容。

## BDD

- BDD: 接收标准弹框读取 QA 工序列 -> Given 当前 PQC 检验项目来自 QA 工序表格行，When 用户点击“接收标准”，Then 弹框正文显示该行“接收标准”列文本，不使用默认判定文案替代。
- BDD: 检验方法弹框读取 QA 工序列 -> Given 当前 PQC 检验项目来自 QA 工序表格行，When 用户点击“检验方法”，Then 弹框正文显示该行“检验方法”列文本，不使用默认首检摘要替代。
- BDD: 字段来源保持可追溯 -> Given 前端同时保留提交身份、结果类型、上下限等结构字段，When 调整弹框数据来源，Then 提交载荷和设备/样本校验不被改写。

## Milestone Updates

- 2026-08-08: 已建立任务目录、用户意图和 BDD。
- 2026-08-08: 已定位一线 PQC 弹框当前直接读取 `standardText/inspectionMethod`，并补充后端响应别名 `acceptanceStandard/processInspectionMethod` 来显式承载 QA 工序“接收标准/检验方法”列。
- 2026-08-08: 已将前端 `PqcInspectionItem` 视图模型、卡片摘要、弹框正文与 `pqcItemDetails` 提交快照切换到 QA 工序列别名；缺失时只显示“未配置”，不使用默认首检规则、判定值或上下限合成替代。
- 2026-08-08: 已同步相邻 PQC 静态合同，保留检验项身份、结果类型、上下限、单位和设备选择链路。

## Verification Evidence

- RED: `node tests\e2e\frontline-pqc-qa-process-standard-method-source-static.spec.cjs` -> FAIL, 预期失败原因为 `PqcInspectionItem` 尚未包含 `acceptanceStandard`。
- GREEN: `node tests\e2e\frontline-pqc-qa-process-standard-method-source-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\frontline-pqc-fact-dialog-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\pqc-item-equipment-standard-method-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\pqc-active-title-method-display-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\pqc-tab-method-display-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\pqc-tab-item-name-display-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\edhr-frontline-pqc-html-alignment-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- GREEN: `git diff --check` -> PASS；仅输出既有 CRLF 工作区提示，无 whitespace error。
- GREEN: frontend feature evidence validator -> PASS。
- BLOCKED: `node -e <inline Playwright PQC fact dialog runtime probe>` -> BLOCKED；本机 `48081` health 为 `UP`、`8081` 为 HTTP 200，真实页面登录后遍历 7 个待检 PQC 工单，`active-order/processes` 响应仍只有旧字段 `standardText/inspectionMethod`，缺少本次新增的 `acceptanceStandard/processInspectionMethod`，当前运行态无法证明弹框展示 QA 工序列别名。
- CLEANUP: `task_closeout.py --task-id 20260808-pqc-qa-process-standard-method-source --mode preview` -> ready；仅删除临时 `frontend-feature-evidence.md`。
- CLEANUP: `task_closeout.py --task-id 20260808-pqc-qa-process-standard-method-source --mode apply` -> applied；保留 `task.md`、`execution-log.md`、`verification-report.md`。
- EXPERIENCE: 已合并长期经验到 `docs/backend-development.md#MES PQC 项目级检验快照门禁`，补充一线弹框/卡片/组长列展示必须读取显式 QA 工序列字段或别名。

## Blockers

- 真实页面运行态验证阻塞：当前本机后端运行态尚未刷新到本次后端响应别名实现；静态合同、前端类型检查、后端编译和 diff 检查仍为 PASS。

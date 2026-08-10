# Execution Log

## Intent

用户反馈：一线 PQC 的 tab 描述里只显示截图红框里的描述，红框外其它说明不显示。

## BDD

- BDD: 一线 PQC tab 描述只显示红框内容 -> Given 一线 PQC 工序 tab 卡片存在正式规则标题和额外说明, When 页面渲染 tab 描述, Then 可见 tab 只展示红框内的正式标题/描述内容，不展示红框外额外说明。

## Evidence

- Rules loaded: `docs/frontend-development.md`, `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`.
- Skills loaded: `bug-regression-fix-loop`, `frontend-feature-delivery`.
- RED: `node tests/e2e/frontline-pqc-tab-description-redbox-only-static.spec.cjs` -> FAIL，旧 tab 模板仍渲染 `getPqcTabStateLabel(item)`、`data-pqc-tab-method` 和 `formatPqcMethodSummary(item)`。
- GREEN: `node tests/e2e/frontline-pqc-tab-description-redbox-only-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs` -> PASS，红框标题继续读取正式 `itemName`，不回退到内部编码。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS，仅提示既有 LF/CRLF 工作区警告。
- GREEN: frontend feature evidence validator -> PASS。
- GREEN: bug regression evidence validator -> PASS。
- Adjacent note: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` 失败于既有设备卡片断言 `pqc-select-card/data-pqc-equipment-card/data-pqc-equipment-number-card`；该区域已在当前工作区的其它未提交改动中移除，不属于本次 tab 描述改动。
- Adjacent note: `node tests/e2e/pqc-tab-method-display-static.spec.cjs` 失败于既有方法弹框标题断言，当前源码为 `activePqcMethodItem.label`；该断言不属于本次 tab 描述可见范围。
- Experience consolidation: 已核对现有 `docs/frontend-development.md#用户可见描述与内部编码隔离门禁` 和 `#前端静态契约隔离门禁` 足够覆盖本次经验，无需新增长期经验文档。
- Cleanup: `task-closeout-cleanup --mode preview` -> PASS，keep 三份正式记录，delete 两份中间 evidence，blocked/warnings none。
- Cleanup: `task-closeout-cleanup --mode apply` -> PASS，已删除中间 evidence 文件。

# Execution Log

## User Intent

- 用户反馈选择“产品信息”工序后，部分单元格出现两个输入框叠加，要求分析原因并解决。

## BDD

- BDD: 多填写人预览快照按当前填写人隔离 -> Given 产品信息预览快照同时包含多个个人责任主体的 `assistRows`, When 页面以某一当前填写人打开辅助模式, Then 只允许该填写人的辅助网格进入字段构造，其他填写人的同坐标字段不得渲染。
- BDD: 同一字段重复映射只渲染一次 -> Given 同一正式字段在同一辅助格被重复引用, When 填写辅助网格构造字段列表, Then 页面只渲染一个字段卡片和一个输入控件。
- BDD: 不同字段占用同一网格位置必须阻塞 -> Given 两个不同正式字段被配置到同一 `ASSIST_GRID` 位置, When 填写辅助网格构造布局, Then 页面明确报告网格位置冲突，不得叠加显示或静默选择其一。

## Investigation

- 模板对单个 `field.componentKind` 使用互斥 `v-if / v-else-if / v-else`，单个字段不会主动渲染两个输入控件。
- 真实只读产品信息页面共渲染 `125` 条辅助行；DOM 检出 `52` 个重复网格位置。每个重复位置由两个不同 `<article>` 占据完全相同的 `gridRow/gridColumn` 和矩形区域。
- 运行快照中的 `assistRows` 分属 `ASSIST_GRID_U795_*`（52 条）与 `ASSIST_GRID_U810_*`（73 条），不存在单条辅助行包含多个字段的情况。
- `buildAssistFieldsFromAssistRows` 原实现忽略 rowKey 中的责任主体，只取行列坐标并把所有填写人的辅助格铺进同一 CSS Grid，因此两个填写人的独立坐标空间发生叠加。
- 正式打开任务时后端 `executionPageQuery.assistRows` 已按工作任务责任范围过滤；未开始任务的 `batchTaskPreview=1` 路径直接使用完整快照，是本次问题暴露路径。

## Git Baseline

- `0e51c3c3 chore: baseline workspace before assist grid overlap fix`：记录本任务实施前的脏工作区及本任务初始文档。
- 后续并行任务基线提交 `5738a1f8 chore: baseline dirty workspace before dcc catalog cleanup` 不属于本任务；本任务不修改其 DCC 文件。

## Verification Evidence

- RED: `node tests/e2e/edhr-assist-grid-current-filler-isolation-static.spec.js` -> FAIL，缺少责任主体类型和当前填写人隔离合同。
- GREEN: `node tests/e2e/edhr-assist-grid-current-filler-isolation-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-assist-fill-mode-configured-grid-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js` -> PASS。
- BASELINE-FAIL: `node tests/e2e/assist-grid-per-user-mapping-static.spec.js` -> FAIL，当前配置弹框缺少既有静态合同标记 `isSourceCellDisabledForAssistMapping`；文件不在本任务改动范围。
- BASELINE-FAIL: `node tests/e2e/assist-grid-role-responsibility-static.spec.js` -> FAIL，当前列表页缺少既有静态合同文本 `rule.fillAssignments?.length`；文件不在本任务改动范围。
- 待记录类型检查和真实 E2E。

## Blockers

- 无。

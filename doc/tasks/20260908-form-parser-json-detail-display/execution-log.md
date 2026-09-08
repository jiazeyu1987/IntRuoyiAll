# Execution Log

## BDD Scenarios

BDD: 展示输入输出物料明细 -> Given 生产批记录解析 JSON 中每个工序包含 `inputs` 和 `outputs` / When 用户完成生产批记录 Word 解析 / Then 页面可展开查看输入物料名称编号、无编号来源标记、输出物料名称编号。

BDD: 展示设备参数明细 -> Given 工序 JSON 中包含 `equipmentGroups/equipmentOptions/parameters/ui` / When 用户查看工序解析结果 / Then 页面展示设备名称编号、选择模式、参数参考范围、默认值、最小值、最大值、步长、单位、控件类型、选项和实际值占位。

BDD: 保留完整 JSON 核对 -> Given 解析结果可能后续新增字段 / When 页面展示解析结果 / Then 用户可打开完整 JSON 和工序 JSON 进行核对，未显式建模字段不会被隐藏。

BDD: 展示输出物料与工序设备参数对应 -> Given `批记录总对应.json` 仅提供工序级 `equipmentGroups` 而没有单个输出物料到设备的一对一字段 / When 用户展开工序明细 / Then 页面明确提示该边界，并按同一工序设备组展示输出物料名称编号、设备名称编号、参数范围、默认值、单位和实际值。

## Evidence

- in_progress: 已建立任务记录，准备补前端静态合同 RED。
- RED: `node tests\e2e\form-parser-json-download-static.spec.cjs` -> FAIL, expected reason: 当前页面缺少 `type="expand"` 和输入/输出物料、设备参数、完整 JSON 明细展示入口。
- JSON analysis: `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\批记录总对应.json` -> root keys `product/schemaVersion/processes`; process keys `name/criticalProcess/inputs/outputs/equipmentGroups`; material keys `code/name/sourceCodeLabel`; equipment group keys `equipmentOptions/parameters/selectionMode`; equipment keys `code/name`; parameter keys `name/referenceValue/actualValue/ui`; ui keys `control/defaultValue/min/max/options/step/unit`。
- RED: `node tests\e2e\form-parser-json-download-static.spec.cjs` -> FAIL, expected reason: 新增合同要求 `输出物料-设备-参数对应` 和 JSON 一对一字段缺失提示，当前页面尚未实现。
- GREEN: `node tests\e2e\form-parser-json-download-static.spec.cjs` -> PASS, 静态合同确认生产批记录按钮仍调用批记录总识别 JSON parse-only API，并展示输入/输出物料、设备参数、完整 JSON、工序 JSON 和输出物料-设备-参数合并明细。
- GREEN: `pnpm ts:check` -> PASS, `vue-tsc --noEmit -p tsconfig.relaxed.json` 正常退出。
- GREEN: `git diff --check` -> PASS, 仅出现工作区既有 LF/CRLF 提示，无 whitespace error。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260908-form-parser-json-detail-display/frontend-feature-evidence.md` -> PASS, cleanup 前已验证前端证据完整。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260908-form-parser-json-detail-display --mode preview` -> PASS, keep 为 `task.md`、`execution-log.md`、`verification-report.md`，delete 为临时 `frontend-feature-evidence.md`，blocked/warnings 均为空。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260908-form-parser-json-detail-display --mode apply` -> PASS, 已删除临时 `frontend-feature-evidence.md`，保留核心任务记录。
- BLOCKED-NON-GATE: `pnpm exec eslint src\views\form-center\parser\index.vue tests\e2e\form-parser-json-download-static.spec.cjs` -> 持续运行超过正常时长且无输出，确认 ESLint 子进程几乎不耗 CPU 后仅终止本轮自有 ESLint 进程；该命令未列入当前任务完成门禁。
- Experience: 已把“批记录总识别 JSON 展示不得伪造输出物料到设备的一对一关系”的经验合并到 `docs/system/shared-word-template-parser-design.md`。

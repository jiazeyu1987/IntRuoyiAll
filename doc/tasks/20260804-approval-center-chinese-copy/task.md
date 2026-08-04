# 审批中心黄框英文中文化

## Task Goal

将审批中心截图黄框区域内的用户可见英文显示为对应中文，重点覆盖来源、业务摘要和节点等列表展示字段，不引入 fallback、降级或静默吞错。

## Milestones

- [x] M1 定位审批中心列表字段来源与现有显示逻辑。
- [x] M2 用最小正式映射修复黄框区域英文展示。
- [x] M3 补充或更新聚焦静态合同，先 RED 后 GREEN 验证英文不再出现在目标展示字段。
- [x] M4 运行聚焦验证并记录剩余阻塞。

## Expected Verification

- 聚焦静态合同覆盖审批中心列表的来源、业务摘要和节点显示中文化。
- 运行相关 Node 静态合同或前端检查命令。
- 复扫目标源码，确认用户可见英文不再直出到黄框区域。

## Current Status

ready_for_closeout

实现与定向验证已完成，cleanup preview/apply 待执行；最终提交/推送需先处理任务开始前已存在的共享工作区脏改动与本地 ahead 状态，避免混入非本任务内容。

## 经验门禁摘要

- 适用 `clear-frontend-copy`：用户可见英文优先中文化，保留 `ID` 等明确允许技术术语，不新增降级文案。
- 适用 `docs/frontend-development.md#前端静态契约隔离门禁`：若全量 `ts:check` 存在无关历史失败，应新增或运行任务专用最小静态合同，不用无关失败阻塞当前窄范围结论。
- 适用 `docs/e2e-rules.md#DCC 文控审批处理入口门禁`：DCC 审批中心链路不得用 BPM 原生行或只读截图替代真实 DCC 业务展示语义。
- 适用 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：保持现有表格密度和操作台风格，本任务只修文案展示，不做重设计。
- 当前工作区存在大量既有脏改动和本地 ahead 状态；本任务只修改任务自有文档与目标前端文案/合同文件，提交推送若受既有状态影响需单独记录 blocker。

## Verification Summary

- RED: `node tests/e2e/approval-center-chinese-copy-static.spec.js` -> FAIL，旧实现直接显示 `row.sourceTaskType`、`row.businessTitle`、`row.businessCode || row.businessKey || row.sourceTaskId` 和 `currentNodeCode`。
- GREEN: `node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/approval-center-bpm-detail-clickable-static.spec.js` + `node tests/e2e/approval-center-cc-standard-list-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- <task-owned files>` -> PASS。
- SCAN: `clear-frontend-copy` 复扫 `src/views/approval-center` 后 `mixed_language_copy=0`；剩余 4 项为模板中的函数名与枚举值误报，不是用户可见英文直出。
- GREEN: `task-closeout-cleanup --mode preview` -> PASS，keep 核心任务记录，delete/blocked/warnings 均为 `<none>`。
- GREEN: `task-closeout-cleanup --mode apply` -> PASS，deleted_paths 为 `<none>`。

## Closeout Blockers

- Git closeout 未执行：任务开始前 `int_main...origin/int_main [ahead 9]` 且工作区存在大量非本任务改动；本任务不创建包含无关改动的基线提交，不推送共享分支。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按显示字段正式映射或已有业务字典解决直出英文。
- `是否存在临时补丁或绕过`：否。

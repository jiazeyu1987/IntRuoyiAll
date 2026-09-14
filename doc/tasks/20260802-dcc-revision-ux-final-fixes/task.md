# 20260802 DCC 升版发布 UX 收口修复

## Task Goal

修复 DCC 文控“升版/修订发布”链路复查中剩余的 3 个前端问题：版本历史命名与变更说明展示、发布完成结果摘要、BPM 流程图 `markers` pageerror 防护。

## Milestones

- [x] M1：补充 BDD 与聚焦 RED 静态契约。
- [x] M2：修复版本历史弹窗/表格展示升版原因或变更说明。
- [x] M3：新增发布完成结果摘要，突出 V1 失效、V2 ACTIVE、master 切换和受控浏览落位。
- [x] M4：修复 BPM 流程图高亮缺失节点导致的 `markers` pageerror，并以可见警告呈现高亮不完整。
- [x] M5：运行聚焦与相邻回归验证，记录 PASS/BLOCKED 证据。
- [x] M6：运行只读真实 Playwright E2E 复验，覆盖发布摘要、受控浏览版本历史弹窗和 BPM markers pageerror。

## Expected Verification

- `node tests/e2e/dcc-revision-publish-ux-final-static.spec.cjs`
- `node tests/e2e/dcc-upload-governance-ux-static.spec.js`
- `node tests/e2e/dcc-controlled-file-detail-retired-static.spec.js`
- `node tests/e2e/dcc-browser-version-summary-static.spec.js`
- `pnpm ts:check`
- `$env:DCC_E2E_PASSWORD = -join (1..6 | ForEach-Object { [char]49 }); node doc\tasks\20260802-dcc-revision-ux-final-fixes\dcc-revision-ux-final-real-e2e.cjs`

## Current Status

ready_for_closeout

## Applicable Gates

- 前端静态契约隔离门禁：本任务新增聚焦静态契约覆盖当前 3 个缺口，不扩大到其它 DCC 场景。
- DCC 文控审批处理入口门禁：不使用 admin、API-only 或 SQL 改状态；本任务只改前端展示和 BPM viewer 防护。
- Windows 换行与脚本行为同步：静态契约使用稳定 marker 和归一化源码文本，不依赖缩进。
- 前端主结果弹窗失败原因可见门禁：发布完成摘要必须用真实详情字段推导，不以 toast 或默认成功替代。
- 前端 BPMN marker 高亮完整性门禁：流程图高亮节点必须先经 `elementRegistry.get` 校验，缺失节点需要页面可见警告。
- DCC 升版发布 UX 闭环门禁：复验必须同时覆盖发布完成摘要、受控浏览 viewer 版本历史弹窗、BPM 流程图 pageerror 和目标写请求为 0。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；BPM 高亮缺失节点改为页面可见警告，不静默忽略。
- `是否从根因和长期维护角度解决`：是；版本历史与发布结果从正式详情字段和版本历史字段推导。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260802-dcc-revision-ux-final-fixes/dcc-revision-ux-final-real-e2e.cjs
- doc/tasks/20260802-dcc-revision-ux-final-fixes/real-e2e-result.json
- doc/tasks/20260802-dcc-revision-ux-final-fixes/real-e2e-detail-publish-summary.png
- doc/tasks/20260802-dcc-revision-ux-final-fixes/real-e2e-preview-version-history.png
- doc/tasks/20260802-dcc-revision-ux-final-fixes/real-e2e-bpm-process-diagram.png

## Closeout Blocker

- 当前共享工作区已有大量非本任务脏改动，且 `int_main...origin/int_main [ahead 1]`；本任务未执行基线提交、任务提交或推送，避免把他人/其它任务改动混入本场景。

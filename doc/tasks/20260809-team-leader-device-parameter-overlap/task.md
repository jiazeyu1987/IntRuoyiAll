# 生产组长报工设备参数重叠修复

## Task Goal

修复生产组长工作台“报工管理”表格中设备参数名称、参数值和范围文字相互重叠的问题；长设备参数名称应在本单元格内自然换行，参数值和范围保持可读。

## Milestones

- [x] 定位截图对应页面、列模板和根因。
- [x] 新增聚焦静态合同并记录 RED。
- [x] 实施最小布局修复并记录 GREEN。
- [x] 完成目标回归、类型检查、差异检查和技能证据校验。
- [x] 完成任务清理与经验沉淀。

## Expected Verification

- `node tests/e2e/team-leader-device-parameter-overlap-static.spec.cjs`
- `node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs`
- `node tests/e2e/team-leader-hide-review-copy-columns-static.spec.cjs`
- `node tests/e2e/pqc-leader-list-fill-form-parity-static.spec.js`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/team-leader-device-parameter-overlap-static.spec.cjs doc/tasks/20260809-team-leader-device-parameter-overlap`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260809-team-leader-device-parameter-overlap/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260809-team-leader-device-parameter-overlap/frontend-feature-evidence.md`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；解除结构化参数列对整表单行溢出规则的继承，并让内部网格项具备明确的收缩和换行能力。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- `Element Plus 表格长文本换行与固定列边界门禁`：结构化参数列必须显式关闭单行 tooltip 溢出行为，并用专用样式锁定 `white-space: normal`、`overflow-wrap: anywhere`、`word-break: break-word` 和网格项 `min-width: 0`。
- `Windows 换行与脚本行为同步`：静态合同先归一化 CRLF/LF，再按稳定列属性和样式选择器截取目标块。

## Milestone Evidence

- RED 已确认：目标结构化参数列没有显式关闭表级单行 tooltip，聚焦合同按预期失败。
- GREEN 已确认：两个结构化参数列关闭表级单行 tooltip，内部网格允许收缩并对长名称自然换行；聚焦合同通过。
- 相邻合同现状：隐藏复核副本列合同通过；生产报工 payload 列合同因当前工作区另有“生产工单”列改动失败，与本任务修改的 tooltip/CSS 不相交，保持原样。
- 真实页面：本机 `int_main` 在 `1920×1080` 下展示 2 条报工、每条 3 个设备参数；所有名称/值/范围网格均无重叠，参数列右边界与操作列左边界均为 `1618px`。
- 技能证据校验：`bug-regression-fix-loop`、`frontend-feature-delivery` evidence validator 与 self-test 均 PASS。
- `project-experience-consolidation`：已检索长期经验，命中既有 `docs/e2e-rules.md#Element-Plus-表格长文本换行与固定列边界门禁` 和 `docs/experience-index.md` 路由；本任务没有新增通用经验，不修改长期文档。

## Cleanup Candidates

- output/playwright/20260809-team-leader-device-parameter-overlap/
- doc/tasks/20260809-team-leader-device-parameter-overlap/bug-regression-evidence.md
- doc/tasks/20260809-team-leader-device-parameter-overlap/frontend-feature-evidence.md

## Cleanup Keep

- IntRuoyiFronted/tests/e2e/team-leader-device-parameter-overlap-static.spec.cjs
- doc/tasks/20260809-team-leader-device-parameter-overlap/task.md
- doc/tasks/20260809-team-leader-device-parameter-overlap/execution-log.md
- doc/tasks/20260809-team-leader-device-parameter-overlap/verification-report.md

## Closeout

- `task-closeout-cleanup preview -> ready`：keep/delete 清单准确，blocked/warnings 均为 none。
- `task-closeout-cleanup apply -> applied`：删除两个临时技能 evidence 文件和本任务 Playwright 输出目录；保留生产代码、正式静态回归测试、`task.md`、`execution-log.md`、`verification-report.md`。
- 当前为主工作区 `int_main`，`linked=False`，未执行 worktree 合并或删除。
- Git 提交、推送不是本任务要求，未执行。

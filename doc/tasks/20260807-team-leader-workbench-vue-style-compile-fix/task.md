# Team Leader Workbench Vue Style Compile Fix

## Task Goal

修复 `TeamLeaderWorkbenchPage.vue` 的 Vite/PostCSS 编译错误，确保 TypeScript 逻辑只位于 `<script setup>`，样式块能够被 Vue SFC 编译器正常解析。

## Milestones

- [x] 定位 Vite overlay 指向的 SFC 区块边界与根因。
- [x] 新增样式块编译回归测试并记录 RED。
- [x] 最小移动误置函数并记录 GREEN。
- [x] 运行相关静态合同、类型检查和差异检查。
- [x] 完成任务清理与收尾记录。

## Expected Verification

- `node tests/e2e/team-leader-workbench-sfc-style-compile-static.spec.cjs`
- `node tests/e2e/work-order-abnormal-minimal-report-static.spec.js`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/team-leader-workbench-sfc-style-compile-static.spec.cjs doc/tasks/20260807-team-leader-workbench-vue-style-compile-fix`
- Bug regression evidence validator
- task-closeout-cleanup preview/apply

## Applicable Experience Gate

- `docs/experience-index.md` 存在。
- 按 `docs/e2e-rules.md` 的 Vite 编译错误门禁，先以 overlay 的文件、行号和 frame 定位编译区块，并扫描目标文件的锚定 Git 冲突标记；不得关闭 overlay 或用 API-only 结果冒充 SFC 编译通过。
- 当前目标是最小编译修复，不启动本地服务、不修改端口、不执行真实写入 E2E。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；将误置的 TypeScript 函数恢复到脚本区块，并新增直接编译样式块的回归测试。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed - 修复、回归验证、经验沉淀和任务清理均完成。

## Completed Work

- 核对 SFC 边界：`<script setup>` 在第 2490-5470 行，`<style scoped>` 从第 5472 行开始。
- 确认 `resetAbnormalForm` 与 `openAbnormalDialog` 当前位于样式块第 5580-5594 行。
- RED 复现 `CssSyntaxError ... ?style=0:110:3: Unknown word`。
- 已将两个函数原样移动到 `<script setup>` 中的 `submitAbnormal` 前，并从样式块移除。
- 已读取前端开发、任务收尾、编码安全和相关 Vite 编译经验门禁。

## Verification Evidence

- 用户提供的 Vite/PostCSS overlay：`Unknown word`，定位到 `abnormalForm.workOrderId = 0`。
- `node tests/e2e/team-leader-workbench-sfc-style-compile-static.spec.cjs` -> FAIL，`CssSyntaxError ... ?style=0:110:3: Unknown word`（预期 RED）。
- `node tests/e2e/team-leader-workbench-sfc-style-compile-static.spec.cjs` -> PASS。
- `node tests/e2e/work-order-abnormal-minimal-report-static.spec.js` -> PASS。
- `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- `pnpm ts:check` -> PASS；收尾前复跑仍 PASS。
- `pnpm build:local` -> 未完成；运行约 77 分钟无退出码且 CPU 基本停滞，已只中止本任务进程。此补充命令未记为 PASS，专用 PostCSS 合同直接覆盖报告的编译错误。
- 任务涉及的已跟踪文件 `git diff --check` -> PASS，仅有 LF/CRLF 转换提示。
- Bug regression evidence validator -> PASS。
- task-closeout-cleanup preview/apply -> PASS；仅删除任务临时 `bug-regression-evidence.md`，保留核心任务记录与正式回归测试。

## Remaining Blockers

- 无。

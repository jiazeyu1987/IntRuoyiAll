# 20260728 辅助表格按填写人映射

## Task Goal

将批记录填写配置中的“辅助表单映射”从辅助行列表改为按填写人切换的 M*N 辅助表格：用户先点击辅助表格单元格，再点击原表单元格建立映射；同一个原表单元格全局只能分配给一个填写人，已分配后在原表中灰化且不可点击，只有取消映射后才能重新分配。

## Milestones

1. 记录 BDD 场景与严格 TDD 验证路径。
2. 新增专用静态合同并先取得 RED 失败。
3. 在现有前端组件内实现按用户 M*N 表格映射，不新增后端接口。
4. 运行专用合同、相邻静态合同和类型检查，记录验证证据。
5. 完成证据文档并根据并行工作区状态判断是否可收尾提交。

## Expected Verification

- `node tests/e2e/assist-grid-per-user-mapping-static.spec.js`
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- `node tests/e2e/batch-record-cell-rule-editor-mode-static.spec.js`
- `node tests/e2e/batch-record-cell-rule-fillable-toggle-static.spec.js`
- `node tests/e2e/batch-record-cell-rule-dialog-size-static.spec.js`
- `pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260728-assist-grid-per-user-mapping/frontend-feature-evidence.md`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；复用现有 `assistRows` 与 `fillAssignments` 正式保存契约，用前端稳定 `rowKey` 表达用户与辅助表格格子。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：当前任务使用专用最小静态合同锁定“按用户辅助表格映射”和“原表单元格唯一分配”行为。
- 同文件并行改动选择性暂存门禁：目标组件和相邻静态合同已有并行工作区改动，提交前必须用显式 diff 区分本任务变更。
- PowerShell / UTF-8 门禁：中文任务文档、测试和 Vue 文案使用 UTF-8 写入；测试命令逐条运行，不用分号串联掩盖退出码。

## Verification Result

- PASS：专用静态合同、相邻填写配置静态合同、默认全屏静态合同和 `pnpm ts:check` 均已通过。
- Closeout note：当前工作区存在大量并行脏改动，提交/推送需要后续按选择性暂存门禁执行，不能使用宽泛 `git add -A`。

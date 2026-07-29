# 20260729 eDHR assist grid compress empty columns

## Task Goal

压缩 eDHR 填写辅助模式中未映射的辅助表格空列，让字段卡片从实际存在的映射列开始连续展示，消除左侧大片空白区域。

## Milestones

- [x] 创建任务目录并读取前端、任务收尾、PowerShell 编码和经验门禁。
- [x] 保存既有脏工作区基线提交，避免混入本任务实现。
- [x] 用静态合同先锁定空列压缩行为并得到 RED。
- [x] 修改执行页辅助网格列计算和定位逻辑。
- [x] 运行定向 GREEN/REGRESSION 验证并记录结果。
- [x] 收尾清理、经验沉淀、提交并推送当前分支。

## Expected Verification

- `node tests/e2e/edhr-assist-fill-mode-configured-grid-static.spec.js`
- `pnpm ts:check`（若存在无关历史或并行残余阻塞，记录首个阻塞点）

## Applicable Experience Gates

### eDHR 辅助模式当前工序 assistRows 路由门禁

- Trigger: 填写辅助模式、ASSIST_GRID_U、辅助表格预览、执行页辅助网格布局。
- Preflight check: 执行页只按正式 `assistRows` 与 `ASSIST_GRID_U<userId>_R<row>_C<column>` rowKey 恢复辅助表格，不引入其它 rowKey 推断。
- Blocker: 辅助表格 rowKey 被扁平化、当前工序显示错误、或静态合同不能证明 `parseAssistGridRowKey`、`edhr-fill-workspace__assist-grid`、`data-assist-grid-cell`、`resolveAssistFieldGridStyle(field)`。
- Verification: 聚焦静态合同覆盖执行页辅助表格解析、容器和定位逻辑。
- Forbidden action: 禁止用默认字段、当前登录人、表单槽位、空布局或宽松 rowKey 兼容替代当前工序 `assistRows`。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按已映射列集合生成连续展示列，不改变正式辅助表格 rowKey 和原始位置说明。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Final Verification Result

PASS. 定向静态合同、`pnpm ts:check`、前端证据校验、经验索引检索和 cleanup apply 均通过。

## Notes

- 既有脏工作区基线提交：`18563a16 chore: baseline dirty workspace before assist grid columns`。
- 基线提交后仍出现并行残余改动，当前任务只选择性暂存本任务文件。
- 经验沉淀：已合并到 `docs/frontend-development.md` 既有 eDHR 辅助模式门禁，并更新 `docs/experience-index.md` 关键词。

## Cleanup Keep

- doc/tasks/20260729-edhr-assist-grid-compress-empty-columns/frontend-feature-evidence.md

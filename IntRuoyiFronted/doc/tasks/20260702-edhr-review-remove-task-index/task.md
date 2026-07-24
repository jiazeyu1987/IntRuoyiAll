# 20260702-edhr-review-remove-task-index

## Task Goal
删除 eDHR 批次详情 / 工序复盘左侧空占位的“工序任务索引”模块，让左侧直接从“已填写表单”开始，保留右侧工序详情、证据链和操作入口。

## Experience Gate
- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，命令输出与中文文件读写显式使用 UTF-8。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次仅删除空索引模块，不引入新视觉体系。
- 前端交付契约：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，记录 BDD、RED / GREEN 和证据。

## Design Constraint Check
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接移除无有效信息的索引 UI 区块及其专属样式，保留现有数据与操作入口。
- 是否存在临时补丁或绕过：否。

## Milestones
1. M1：定位工序复盘页面与黄框对应模板。completed
2. M2：补充静态回归，证明不再渲染“工序任务索引”。completed
3. M3：删除索引 UI 与无用样式。completed
4. M4：运行验证、收尾预览并提交本任务改动。completed

## Expected Verification
- 静态回归测试确认页面模板不再包含“工序任务索引”和 `edhr-batch-detail__task-index-*` 索引 DOM 类。
- 页面代码仍保留“已填写表单”和右侧工序证据链入口。
- frontend-feature evidence 校验通过。
- task-closeout-cleanup 预览通过。

## Current Status
completed

## Current Blockers
- 无。

## Cleanup Keep
- doc/tasks/20260702-edhr-review-remove-task-index/frontend-feature-evidence.md

## Final Verification
- GREEN: 
ode tests/e2e/mes-edhr-batch-review-remove-task-index-static.spec.js -> PASS。
- GREEN: removed refs check -> PASS, no removed UI refs。
- GREEN: frontend feature evidence validation -> PASS。
- GREEN: task-closeout-cleanup preview -> PASS, no delete / blocked / warnings。

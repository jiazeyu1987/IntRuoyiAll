# 修复 Word 导入签名日期区域误识别 checkbox

## Task Goal

修复 Word 批记录表单导入后，`操作人/日期`、`复核人/日期` 等签名日期区域被识别成 checkbox 的问题，确保只有真实结果列选项生成 checkbox 控件，签名日期区域保持签名/文本/日期填写语义。

## Milestones

- [x] 建立缺陷复现与预期行为记录
- [x] 补充失败优先的回归测试
- [x] 实施最小正式修复
- [x] 运行目标验证并记录结果
- [x] 执行真实页面 E2E 复验
- [x] 修复空白/checkbox 签名日期格继承 checkbox 的真实 E2E 回归
- [x] 完成 E2E 收尾记录

## Expected Verification

- 新增 Word 表格列偏移场景的回归测试，先证明签名日期尾部区域会被误识别为 checkbox。
- 修复后新增测试通过，并保持既有签名日期 checkbox fragment 保护用例通过。
- 任务启动时记录 `docs/experience-index.md` 未命中；本次 E2E 复跑已按现有 `docs/experience-index.md`、E2E、登录、本机运行和 worktree 门禁核对，继续限定在本机真实前端路径。

## Current Status

ready_for_closeout；真实页面 E2E 已通过并于 2026-07-25 复跑确认。最终验证使用本机前端 `http://localhost:8081`、后端 PID `39380`、Jar SHA256 `1090219624699F708D9440DB71E5FDC1303B71C7787EE7F99330E6E827C8B99F`，导入 `pressure-pump-record.doc` 到 `数显球囊扩张压力泵（FDA)`，确认签名日期区域 `signatureDateCellsChecked=177` 均未渲染 checkbox。当前工作区存在大量其它任务改动，尚未执行提交/推送收尾。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## Cleanup Keep

- `doc/tasks/20260724-fix-word-signature-checkbox/bug-regression-evidence.md`

# DCC 截图需求逐条差距复核

## Task Goal

将用户提供的 DCC 截图需求逐条拆分为 R01-R14，并基于当前 `int_main` 代码、测试脚本、任务记录和必要运行证据，找出与图片要求仍存在差距的点。

## Milestones

- [x] M1：建立逐条复核任务记录。
- [x] M2：拆分图片需求为 R01-R14。
- [x] M3：核对当前前后端实现、测试和运行证据。
- [x] M4：输出只包含差距点的复核结论。

## Expected Verification

- 使用 `rg` 检索当前前端 `yudao-ui-admin-vue3` 与后端 `ruoyi-vue-pro/yudao-module-dcc`。
- 复用既有任务记录与 E2E 脚本证据。
- 本次只做独立复核，不修改生产代码。

## Current Status

Completed.

## Final Verification Result

与图片逐条核对后，当前主要差距点为：

- R01：受控文件源文件上传未限制为图片列出的可编辑源文件类型，当前前端提示支持任意类型。
- R02：后端已有密码强度和过期校验，但前端密码规则提示仍存在 4-16 位等旧规则，用户侧规则不统一。
- R05：申请人主动撤回后缺少“删除流程 / 重新提交”二选一入口和状态流转。
- R07：外来文件评审只是复用受控文件上传链路与 `processType=EXTERNAL_REVIEW`，未形成独立完整流程。
- R09：培训记录由第四节点文控审批时一并上传，未实现“第四节点前由申请人上传培训记录”的独立节点。
- R10：电子发放接收人当前按类别分发规则/部门解析，未看到文控在单个文件流程中手动选择接收人的入口。
- R11：纸质发放回收记录缺少图片要求的“发放人、发放日期”字段，当前使用确认人/确认时间。
- R12：流程打印和 Word 导出已存在，但未看到可上传 Word 模板的自定义打印模板配置能力。

详细证据见 `verification-report.md`。

## Cleanup Keep

- `doc/tasks/20260526-dcc-screenshot-full-gap-audit/verification-report.md`

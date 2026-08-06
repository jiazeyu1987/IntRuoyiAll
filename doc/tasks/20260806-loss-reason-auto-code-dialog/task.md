# 20260806-loss-reason-auto-code-dialog

## Task Goal

按截图要求调整生产组长工序配置里的“新增损耗原因”弹窗：新增时删除红框内的原因编码、启用状态和维护说明输入区，损耗原因编号/编码由系统自动生成；保留原因名称填写、正式保存接口和错误暴露。

## Milestones

- [x] 建立脏工作区基线，避免覆盖并行任务改动。
- [ ] 用 BDD + RED 静态/后端合同锁定新增弹窗隐藏字段与服务端自动编号。
- [ ] 实现前端弹窗隐藏与后端新增自动编码。
- [ ] 运行目标合同、类型/后端目标测试和证据校验。
- [ ] 更新任务状态、验证报告并完成提交推送。

## Expected Verification

- `node tests/e2e/team-leader-loss-reason-auto-code-dialog-static.spec.cjs`
- 后端目标测试覆盖新增损耗原因时请求无需 `reasonCode` 且服务端生成唯一编码。
- `pnpm ts:check` 或记录明确非本任务阻塞。
- 后端目标 Maven/JUnit 或静态合同通过。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-loss-reason-auto-code-dialog/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260806-loss-reason-auto-code-dialog/backend-api-evidence.md`

## Current Status

in_progress

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；前端隐藏输入框同时由后端正式生成编码，避免空编码或前端伪造编号。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：本任务新增最小静态合同锁定弹窗字段删除和提交 payload，不用截图目测替代验证。
- 生产组长工序配置维护权限门禁：本任务不改变维护权限、路线工序 scope 或授权来源，仅调整新增损耗原因字段与编码生成。
- 脏工作区基线门禁：已在本任务实施前保存既有脏改动基线提交 `8c55fbe51` 和残余任务文档基线提交 `8113d2715`。

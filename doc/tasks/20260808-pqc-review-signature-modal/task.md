# PQC 复核弹框电子签名缺失修复

## Task Goal

恢复 PQC / 生产组长复核弹框里的正式电子签名输入，确保用户复核时填写电子签名密码，而不是看到内部签名 ID、员工 ID 或签名快照字段。

## Milestones

- [x] 复现并锁定复核弹框缺少电子签名输入的问题。
- [x] 补充先失败的静态回归合同，覆盖复核弹框必须展示电子签名输入并隐藏内部签名字段。
- [x] 按现有正式签名链路实现最小修复，不引入 fallback、mock 签名或前端伪造签名。
- [x] 运行目标合同、相邻回归和类型检查，记录 GREEN / REGRESSION 结果。
- [x] 完成验证报告与收尾记录。

## BDD Scenarios

- BDD: 复核弹框展示电子签名 -> Given PQC 组长打开待复核提交记录的复核弹框, When 弹框渲染复核表单, Then 用户能看到标注为“电子签名”的密码输入框，且看不到“复核签名ID”“签名员工ID”“签名快照”等内部字段。
- BDD: 复核提交使用正式签名链路 -> Given 复核人填写复核意见和电子签名, When 点击通过或驳回复核, Then 前端必须先通过正式电子签名能力生成/校验签名载荷，再调用组长复核提交接口，不能由前端伪造签名 ID、签名员工或签名快照。

## Expected Verification

- `node tests/e2e/team-leader-review-signature-dialog-static.spec.cjs`
- `node tests/e2e/team-leader-report-allocation-dialog-hide-static.spec.cjs`
- `node tests/e2e/team-leader-pqc-review-gate-static.spec.js`
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureSchemaTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `pnpm ts:check`（若存在无关历史阻塞，记录首个阻塞并以定向合同作为本任务完成依据）
- `git diff --check`

## Current Status

completed

实现、定向验证、回归证据校验和 task-closeout-cleanup preview/apply 均已完成。未执行 Git 提交或推送，因项目当前 Git Policy 默认不要求提交，且工作区存在大量非本任务脏改动。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是恢复正式电子签名输入与正式签名载荷生成链路。
- 是否存在临时补丁或绕过：否。

## Experience Gate

- 已读取 `docs/experience-index.md`，命中与本任务相关的前端门禁包括“前端确认提交上下文来源门禁”“前端写入成功与列表刷新失败分层门禁”“业务运行记录用户可读展示门禁”。
- 已按 `project-experience-consolidation` 搜索既有长期经验，`docs\frontend-development.md#业务运行记录用户可读展示门禁` 与 `docs\backend-development.md#业务修订审计身份服务端归属门禁` 已覆盖本任务经验：用户只填写签名凭据，签名 ID、签名用户、签名快照和审计派生字段由服务端生成，不新建长期经验文档。

## Cleanup Keep

- doc/tasks/20260808-pqc-review-signature-modal/bug-regression-evidence.md

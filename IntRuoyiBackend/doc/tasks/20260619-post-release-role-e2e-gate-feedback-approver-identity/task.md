# 任务：发布后智能排产 smoke 审批人身份解析门禁修复

## 任务目标

修复测试服 `172.30.30.58` 在发布后三角色验收中，`芋道源码/zhaojie` 的智能排产 smoke 于 `/admin-api/mes/pro/feedback/import-record/attribute` 阶段因第三方报工导入 `工段长` 列解析到重复昵称 `eDHR矩阵-审批人` 而失败的问题，使真实 smoke 能把审批责任绑定到当前 supervisor 账号并继续完成归属、审批与后续校验；同时保持昵称缺失或重复时仍然 fail fast，不吞错、不静默选人。

## 前置任务检查

- 后端上一任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260619-post-release-role-e2e-gate-edhr-batch-trigger-gate\task.md` 已按其范围 `COMPLETED`：真实 smoke 已越过自动排产 `apply` 的 eDHR 误触发门禁，当前阻塞推进到报工导入审批人身份解析。
- 当前任务继续服务于维护仓 `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\task.md`，仅处理第三方报工导入审批人身份与 smoke 发布契约，不覆盖无关并行改动。

## 经验门禁

- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`：本次测试服真实复现和最终验收只允许使用用户明确授权的 `芋道源码` 租户真实账号；登录失败必须记录实际租户、账号、入口和影响，不得切换环境或账号掩盖。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`：测试服目标主机固定为 `172.30.30.58`，远端目录固定为 `/opt/intruoyi/runtime`；远端容器、`.env`、日志和数据库读取必须显式确认目标主机与授权范围。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`：修复后必须通过维护仓正式构建发布链进入测试服，验证 releaseTag、远端 `IMAGE_TAG`、运行时 smoke 参数和真实三角色 E2E 结果，不得用本地脚本结果替代发布后验收。

## BDD 场景

- `BDD: 第三方报工导入审批人支持唯一 username 解析 -> Given 导入文件的工段长字段填写的是启用用户的唯一 username / When 系统在归属阶段解析审批人 / Then 必须把审批责任绑定到该 username 对应的唯一用户，后续审批人登录与 approveUserId 保持一致。`
- `BDD: 唯一昵称仍按原契约可解析 -> Given 导入文件的工段长字段填写的是唯一 nickname / When 系统解析审批人 / Then 必须继续按原有昵称契约命中该用户，不回退行为。`
- `BDD: 重复昵称仍然 fail fast -> Given 导入文件的工段长字段填写的是重复 nickname，且没有与之同名的唯一 username / When 系统解析审批人 / Then 必须明确返回“匹配到多名用户”，不得静默任选其一或吞错继续。`
- `BDD: 发布后 smoke 审批人标识绑定当前 supervisor 账号 -> Given 维护发布链为智能排产 smoke 配置 supervisor 账号 / When 生成测试服运行时 `.env` / Then 第三方报工导入审批人字段必须显式绑定该 supervisor 的唯一标识，避免依赖历史昵称默认值。`

## 里程碑

1. M1：建立任务文档、记录经验门禁与真实根因。`DONE`
2. M2：RED：补报工导入审批人解析回归测试，先证明重复昵称场景阻塞真实 smoke。`DONE`
3. M3：GREEN：最小修复审批人唯一身份解析，并保持重复昵称 fail-fast。`DONE`
4. M4：维护发布链补 smoke 审批人环境契约并完成本地验证。`DONE`
5. M5：重新构建发布测试服并完整复跑三角色真实 E2E。`DONE`
6. M6：更新证据、收尾并提交本任务相关改动。`DONE`

## 预期验证

- 后端目标测试：`mvn -pl yudao-module-mes "-Dtest=MesProFeedbackImportRecordServiceImplTest" test`
- 维护发布契约：目标静态/脚本校验通过，确认运行时 `.env` 带有 `MES_SMOKE_FEEDBACK_APPROVER_NAME=<supervisor-username>`。
- 测试服真实结果：`gaomin` 展厅通过，`zhaojie` 智能排产 smoke 全链路通过，`wangsiyu` DCC 文控中心通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。审批人解析只新增正式的唯一身份识别规则；昵称重复时仍明确失败。
- `是否从根因和长期维护角度解决`：是。直接修正第三方报工审批人身份解析与 smoke 发布契约，使审批归属与真实登录审批账号保持同一唯一用户。
- `是否存在临时补丁或绕过`：否。不得通过修改 `芋道源码` 租户用户数据、手工删账号或跳过审批步骤掩盖问题。

## 当前状态

- 状态：COMPLETED。
- 本任务对应修复已随测试服发布 `release-20260619-2230-role-e2e-gate-feedback-approver-wrapper` 生效。
- 最终真实证据：
  - 测试服 `.env`：`MES_SMOKE_FEEDBACK_APPROVER_NAME=messmokesupervisor`。
  - 智能排产 smoke 运行配置：远端 `config.json` 显示 `feedbackApproverName=messmokesupervisor`，说明 scheduler smoke 容器已正确继承发布变量。
  - 智能排产 smoke 运行报告：`smoke-report.json` 为 `PASS`，其中导入记录 `approverName=messmokesupervisor`，归属创建的正式报工 `approveUserId=910260`、`approveUserNickname=eDHR矩阵-审批人`，说明审批责任已正确绑定到唯一 username 对应账号。
  - 维护仓三角色真实 E2E：`evidence/post-release-role-e2e-1781882555897.json` -> PASS，`zhaojie` 智能排产 smoke 不再在审批人解析阶段失败。

## Cleanup Keep

- `doc/tasks/20260619-post-release-role-e2e-gate-feedback-approver-identity/task.md`
- `doc/tasks/20260619-post-release-role-e2e-gate-feedback-approver-identity/execution-log.md`

# 20260802 DCC 上传升版 E2E

## Task Goal

使用 5 个非 admin 账号，通过本机真实前端页面验证 DCC 文控文件 V1 上传、四级审批发布、V2 升版上传、四级审批发布、发布申请审批、生效切换的完整业务链路。

## Scope

- 本机前端入口：`http://127.0.0.1:8081`
- 本机后端入口：`http://127.0.0.1:48081`
- 测试租户：tenant `1`
- 文件分类：`技术文档 / 设计和开发输出阶段 / 来料/过程/成品检验规范`
- 文件类别：`过程检验规程`
- 绑定目录：`质量管理 / 4.Ohter`
- 项目：`HGGW`
- 5 个非 admin 账号角色：
  - 上传人：`pengyunfeng`
  - 文控审核：`zhaohaichen`
  - 会签审核：`zhaojie`
  - 会签批准：`zhaomingyu`
  - 文控批准 / 发布申请：`wangsiyu`

## Milestones

1. 准备并核对运行态、账号权限、测试文件和任务自有文件编号。`completed`
2. 通过真实前端页面完成 V1.0 新文件上传提交。`completed`
3. 通过 4 个审批账号按 DCC 路由顺序完成 V1.0 审批发布。`completed`
4. 通过真实前端页面完成同一文件编号 V2.0 升版上传提交。`completed`
5. 通过 4 个审批账号按 DCC 路由顺序完成 V2.0 审批至待发布。`completed`
6. 通过发布申请人提交 V2.0 发布申请，并通过 4 个审批账号完成发布 BPM 审批。`completed`
7. 使用只读 DB 核验 master/current/version/审批记录，并输出验证报告。`completed`

## Expected Verification

- Playwright 操作真实前端页面完成上传、DCC 审批、升版、发布申请和发布审批，不使用 API-only 替代页面路径。
- V1.0 与 V2.0 使用同一 `fileNumber`，V2.0 为 `REVISION`。
- master 当前生效版本指向 V2.0，V1.0 状态变为 `SUPERSEDED`，V2.0 状态变为 `ACTIVE`。
- V1.0 与 V2.0 合计存在至少 8 个上传审批动作记录。
- 发布申请 `bpm_form_action_instance` 状态为 `EFFECTIVE`，发布 BPM 审批完成 4 个任务。
- 任务日志和报告不包含密码、授权头令牌或刷新令牌。

## Applicable Experience Gates

- DCC 上传类别权限：上传前核对目标分类对上传账号投影 `canUpload=true`，不得把菜单权限等同类别上传权限。
- Element Plus 上传控件：`setInputFiles` 后必须断言页面文件列表出现目标文件名或目标上传请求已发出，否则不得继续提交。
- int_main 运行态 URL：本轮只允许使用 `8081/48081` 成对入口，并记录前端 HTTP 200、后端 health UP。
- Playwright 浏览器：使用本机 Chrome 可执行文件，浏览器缺失必须记录为 E2E 前置阻塞。
- DCC 审批处理入口：DCC 上传审批必须使用 DCC 详情处理态；发布申请审批使用 BPM 流程详情页真实“通过”按钮和下一审批人选择。

## Test Data

- 任务自有文件编号：`CODX-DCC-REV-20260802-20260801193848`
- 文件名称：`Codex DCC 升版链路 20260801193848`
- V1 本地文件：`E:\IntRuoyi\resource\批记录节点-解析样本.docx`
- V2 本地文件：`E:\IntRuoyi\resource\过程检验记录.docx`
- 末节点盖章 PDF：`doc/tasks/20260802-dcc-upload-revision-e2e/stamped-approval-sample.pdf`

## BDD Scenarios

- BDD: V1 上传发布 -> Given 上传账号具有目标分类上传权限 When 上传账号通过前端提交 V1.0 新文档并四个审批账号依次审批 Then V1.0 成为该文件编号的当前生效版本。
- BDD: V2 升版发布 -> Given 同一文件编号已有 V1.0 当前生效版本 When 上传账号通过前端选择升版并提交 V2.0 且四个审批账号依次审批 Then V2.0 进入待发布状态且标记为升版。
- BDD: V2 发布生效 -> Given V2.0 已审批至待发布 When 发布申请账号通过详情页提交发布申请并四个审批账号依次完成 BPM 审批 Then V1.0 变为 `SUPERSEDED`、V2.0 变为 `ACTIVE` 且 master 指向 V2.0。

## 权限调整记录

- `role_id=910295` 增加 `bpm:task:update`，用于审批中心/BPM 任务更新。
- `role_id=910231` 增加 `form:instance:create`、`form:instance:submit`、`system:user:list`、`system:user:query`，用于发布申请与审批人选择。
- `role_id=910295` 增加 `bpm:process-instance:query`，用于发布 BPM 流程详情页。
- 已清理对应 Redis 权限缓存，不记录任何密码或 token。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，本任务使用真实前端入口补齐账号权限并验证正式业务链路。
- 是否存在临时补丁或绕过：否。脚本的 resume 参数仅用于断点续跑已通过阶段，不替代任何业务页面操作或最终 DB 核验。

## Cleanup Keep

- doc/tasks/20260802-dcc-upload-revision-e2e/task.md
- doc/tasks/20260802-dcc-upload-revision-e2e/execution-log.md
- doc/tasks/20260802-dcc-upload-revision-e2e/verification-report.md
- doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs
- doc/tasks/20260802-dcc-upload-revision-e2e/e2e-result.json
- doc/tasks/20260802-dcc-upload-revision-e2e/stamped-approval-sample.pdf

## Current Status

completed

## Final Verification Result

- Playwright E2E：PASS，结果文件 `doc/tasks/20260802-dcc-upload-revision-e2e/e2e-result.json`。
- V1 controlled file id：`2054545668044070260`，最终状态 `SUPERSEDED`。
- V2 controlled file id：`2054545668044070261`，最终状态 `ACTIVE`。
- Publish form instance id：`435`，状态 `EFFECTIVE`。
- Publish BPM process instance id：`8a6ea0e6-8de1-11f1-a558-00155d9fd668`。
- 上传审批任务完成数：`8`；发布审批任务完成数：`4`。
- 残留风险：BPM 流程详情页渲染期间出现非阻塞 pageerror `Cannot read properties of undefined (reading 'markers')`，未影响目标审批按钮、接口响应或最终业务状态。

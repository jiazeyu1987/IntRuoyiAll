# 20260802 DCC 上传升版 E2E Execution Log

## User Intent

用户要求使用 5 个非 admin 账号，通过真实 E2E 验证 DCC 文控上传 + 升版完整业务链路。账号密码仅用于本机执行，不记录明文。

## Gate Setup

- 已创建任务目录：`doc/tasks/20260802-dcc-upload-revision-e2e/`
- 已读取并应用 E2E、登录、数据库、PowerShell、运行态、任务收尾和 QA 相关规则。
- BDD: V1 上传发布 -> Given 上传账号具有目标分类上传权限 When 上传账号通过前端提交 V1.0 新文档并四个审批账号依次审批 Then V1.0 成为该文件编号的当前生效版本。
- BDD: V2 升版发布 -> Given 同一文件编号已有 V1.0 当前生效版本 When 上传账号通过前端选择升版并提交 V2.0 且四个审批账号依次审批 Then V2.0 进入待发布状态且标记为升版。
- BDD: V2 发布生效 -> Given V2.0 已审批至待发布 When 发布申请账号通过详情页提交发布申请并四个审批账号依次完成 BPM 审批 Then V1.0 变为 `SUPERSEDED`、V2.0 变为 `ACTIVE` 且 master 指向 V2.0。

## Command Intent Log

- 运行态预检：本机前端 `http://127.0.0.1:8081/` HTTP 200，本机后端 `http://127.0.0.1:48081/actuator/health` UP，本机 Chrome 可用。
- 账号确认：使用 `pengyunfeng`、`zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu` 五个非 admin 账号，tenant `1` 用户 ID 分别为 `151`、`376`、`1074`、`424`、`910250`。
- 权限补齐：按用户授权为非 admin 角色补齐发布申请、用户选择和 BPM 发布审批所需权限；未把 admin 作为业务角色。
- RED: 初始 DCC 上传审批入口曾阻塞于详情处理态不可达，后续当前代码/权限下真实 DCC 详情处理态可完成 V1/V2 四级审批。
- RED: 发布申请阶段首次失败于 `wangsiyu` 缺 `form:instance:create`、`form:instance:submit`、`system:user:query/list` 等发布申请/审批人选择权限。
- RED: 发布审批阶段首次失败于 BPM 发布审批未填写 `APPROVE_USER_SELECT` 后续审批人，后改为通过 BPM 流程详情页真实“通过”按钮选择下一节点审批人。
- GREEN: V1.0 上传、DCC 四级审批、V2.0 升版上传、DCC 四级审批、发布申请、BPM 四级发布审批、最终 DB 核验均通过。

## Milestone Status

- M1 运行态和数据前置：completed
- M2 V1.0 上传提交：completed
- M3 V1.0 DCC 四级审批发布：completed
- M4 V2.0 升版提交：completed
- M5 V2.0 DCC 四级审批至待发布：completed
- M6 V2.0 发布申请与 BPM 四级审批：completed
- M7 只读核验与报告：completed

## Verification Evidence

- `node --check doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs`：PASS。
- E2E PASS 结果：`doc/tasks/20260802-dcc-upload-revision-e2e/e2e-result.json`，`status=PASS`。
- 文件编号：`CODX-DCC-REV-20260802-20260801193848`。
- V1.0：`controlled_file_id=2054545668044070260`，最终状态 `SUPERSEDED`。
- V2.0：`controlled_file_id=2054545668044070261`，最终状态 `ACTIVE`，`changeType=REVISION`。
- master：`current_active_controlled_file_id=2054545668044070261`，`status=ACTIVE_CHAIN`。
- 发布申请：`bpm_form_action_instance.id=435`，`status=EFFECTIVE`，`bpmProcessInstanceId=8a6ea0e6-8de1-11f1-a558-00155d9fd668`。
- 上传审批完成数：`8`；发布审批完成数：`4`。
- 敏感信息扫描：按已知密码字面量、授权头、访问令牌和刷新令牌关键字扫描任务目录 -> 无命中。
- QA evidence validator：`validate_quality_assurance.py --evidence doc/tasks/20260802-dcc-upload-revision-e2e/verification-report.md` -> PASS。
- 经验沉淀：更新 `docs/e2e-rules.md#dcc-文控审批处理入口门禁` 与 `docs/experience-index.md`，补充发布申请权限、`UserSelectV2`、发布 BPM `APPROVE_USER_SELECT` 下一审批人选择门禁。
- Cleanup preview/apply：`task_closeout.py --task-id 20260802-dcc-upload-revision-e2e --mode preview/apply` -> PASS，删除项 `<none>`，保留任务记录、E2E 脚本、结果 JSON 和 PDF fixture。

## Residual Risks

- BPM 流程详情页加载时出现非阻塞 pageerror：`Cannot read properties of undefined (reading 'markers')`。本轮目标审批按钮、下一审批人选择、审批接口和最终 DB 状态均通过；该错误建议单独作为 BPM 流程图渲染问题跟进。

## Final Status

completed

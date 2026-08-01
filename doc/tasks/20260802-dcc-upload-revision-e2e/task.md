# 20260802 DCC 上传升版 E2E

## Task Goal

使用 5 个非 admin 账号，通过本机真实前端页面验证 DCC 文控文件 V1 上传、四级审批发布、V2 升版上传、四级审批发布的完整业务链路。

## Scope

- 本机前端入口：`http://127.0.0.1:8081`
- 本机后端入口：`http://127.0.0.1:48081`
- 测试租户：tenant `1`
- 上传分类：`DCC_OTHER_TEMPLATE_900250` / 其他
- 目录：`质量管理 / 4.Ohter`
- 项目：`HGGW`
- 5 个非 admin 账号角色：
  - 上传人：`pengyunfeng`
  - 文控审核：`zhaohaichen`
  - 会签审核：`zhaojie`
  - 会签批准：`zhaomingyu`
  - 文控批准：`wangsiyu`

## Milestones

1. 准备并核对运行态、账号权限、测试文件和任务自有文件编号。
2. 通过真实前端页面完成 V1.0 新文件上传提交。
3. 通过 4 个审批账号按路由顺序完成 V1.0 审批发布。
4. 通过真实前端页面完成同一文件编号 V2.0 升版上传提交。
5. 通过 4 个审批账号按路由顺序完成 V2.0 审批发布。
6. 使用只读 API/DB 核验 master/current/version/审批记录，并输出验证报告。

## Expected Verification

- Playwright 操作真实前端页面完成上传、审批、升版，不使用 API-only 替代页面路径。
- V1.0 与 V2.0 使用同一 `fileNumber`，V2.0 为 `REVISION`。
- master 当前生效版本指向 V2.0，V1.0 与 V2.0 属于同一 master。
- V1.0 与 V2.0 均存在 4 个非 admin 审批动作记录。
- 任务日志和报告不包含密码、授权头令牌或刷新令牌。

## Applicable Experience Gates

- DCC 上传类别权限：上传前核对目标分类对上传账号投影 `canUpload=true`，不得把菜单权限等同类别上传权限。
- Element Plus 上传控件：`setInputFiles` 后必须断言页面文件列表出现目标文件名或目标上传请求已发出，否则不得继续提交。
- int_main 运行态 URL：本轮只允许使用 `8081/48081` 成对入口，并记录端口归属、前端 HTTP 200、后端 health UP。
- Playwright 浏览器：优先使用本机 Chrome/Edge 可执行文件，浏览器缺失必须记录为 E2E 前置阻塞。
- OnlyOffice 容器链路：DOCX 发布链路涉及转换/盖章时，必须确认容器可访问 `http://host.docker.internal:48081/actuator/health`。

## Test Data

- 任务自有文件编号前缀：`CODX-DCC-REV-20260802`
- V1 本地文件：`E:\IntRuoyi\resource\批记录节点-解析样本.docx`
- V2 本地文件：`E:\IntRuoyi\resource\过程检验记录.docx`

## BDD Scenarios

- BDD: V1 上传发布 -> Given 上传账号具有目标分类上传权限 When 上传账号通过前端提交 V1.0 新文档并四个审批账号依次审批 Then V1.0 成为该文件编号的当前生效版本。
- BDD: V2 升版发布 -> Given 同一文件编号已有 V1.0 当前生效版本 When 上传账号通过前端选择升版并提交 V2.0 且四个审批账号依次审批 Then 当前生效版本切换到 V2.0 且 V2.0 标记为升版。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，本任务只验证真实业务链路，不修改产品逻辑。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep

doc/tasks/20260802-dcc-upload-revision-e2e/task.md
doc/tasks/20260802-dcc-upload-revision-e2e/execution-log.md
doc/tasks/20260802-dcc-upload-revision-e2e/verification-report.md
doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs
doc/tasks/20260802-dcc-upload-revision-e2e/e2e-result.json
doc/tasks/20260802-dcc-upload-revision-e2e/stamped-approval-sample.pdf

## Current Status

blocked

## Blocker Summary

- 真实前端已完成 V1.0 上传提交，但无法进入 DCC 详情审批处理态：`DccControlledFileDetail` 路由在无 `viewer=1` 时强制跳回受控浏览；有 `viewer=1` 时仅渲染只读 viewer，审批阶段进度和签名按钮不渲染。
- 因前端正式入口缺失，不能继续用 Playwright 真实用户路径完成 V1 四级审批、V2 升版上传和 V2 四级审批；按 E2E 规则不得改用 API-only 代替页面路径。

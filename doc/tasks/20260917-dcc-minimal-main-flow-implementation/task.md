# DCC 主流程最小实现开发验证

## Goal

按 `docs/dcc-minimal-main-flow/README.md`、`implementation.md` 和 `acceptance.md` 完成并验证 M01—M30 的最小主流程。当前工作区已经包含 M04/M05 及此前四项主流程修复；本次继续核对 P1—P4 的代码、服务/Mapper 回归、前端静态合同和文档交付边界，缺口必须补成可验证的最小实现，不把未执行的 E2E、数据库写入或部署写成通过。

## Milestones

1. 复核 M01—M05 项目、模板、目录、上传身份和名称唯一性。
2. 复核 M06—M16 审批路线、会签、资料核查、批注、驳回/撤回和轮次历史。
3. 复核 M17—M24 小/大版本、原子切换、作废影响确认和例外替换。
4. 复核 M25—M30 关联、跨项目引用、人工更新、通知、身份保护和生命周期投影。
5. 运行 DCC 定向 Maven 测试、前端静态合同、类型检查、文档结构校验和 `git diff --check`。
6. 对未完成或环境阻塞项保留明确证据，不把窄范围通过扩大成 M01—M30 完成。

## Expected Verification

- `DccSourceUploadContextTest` 覆盖“合法新名称 + 允许文件类型可以通过”。
- `DccProjectFileTemplateServiceImplTest` 覆盖“模板配置的文件类型允许自由名称”。
- `DccControlledFileNameClaimServiceTest` 覆盖“同租户同名阻止、已作废名称释放、同键重放幂等”。
- `DccControlledFileWorkflowServiceImplTest` 覆盖新建流程调用名称占用服务。
- 既有严格模板选择、审批和作废测试继续通过。
- P1—P4 对应的既有服务/Mapper 测试和静态合同必须分别记录通过或缺口。
- DCC 模块定向 Maven 测试、前端静态合同、类型检查、schema 合同和文档结构校验通过；未执行真实数据库写入、E2E、服务启停或远程操作。

## Current Status

blocked — M01—M30 文档对应的代码、静态合同、SQL 合同和文档结构定向验证已完成；正式 `task-closeout-cleanup` 命令在当前仓库和本机环境不存在，且本轮未授权 Git 提交/推送，因此按收尾规则保留验证证据但不标记 completed。

## BDD

### BDD-M04-free-name-upload

Given：启用项目已配置允许上传的文件类型，用户有项目编制权限。\
When：用户填写模板中没有出现过的合法文件名称，选择该文件类型并上传 PDF。\
Then：新建上传上下文通过，服务端只校验项目、目录/类型、权限和名称非空，不因名称未出现在旧模板清单而拒绝。

### BDD-M05-global-file-name-claim

Given：同一租户已有项目 P1 的未作废文件“检验规程”，另有项目 P2。\
When：用户在 P2 以同名新建文件，或在同一业务版本的待审/驳回/撤回状态再次点击新建。\
Then：服务端返回“文件名称已存在，请先走作废或者升版路线”；批准作废后释放名称，使用新 MasterID 重新上传并重新走文控与会签；同一请求键重放只返回原结果。

## BDD-M01—BDD-M30 交付边界

完整 BDD 以 `docs/dcc-minimal-main-flow/acceptance.md` 为唯一编号来源；本任务逐包记录 T1（M01—M05/M23/M29）、T2（M06—M13）、T3（M14—M16）、T4（M17—M20/M22/M24/M30）、T5（M25—M28）和 T6（M10/M21 及前端 PDF 交互）的证据。任何未执行的真实页面、数据库或部署验证必须保持 `NOT_RUN/BLOCKED`，不能改写为 PASS。

## Design Constraints

- 只处理当前任务涉及的 DCC 文件和任务记录，不回滚工作区已有改动。
- 新建上传与旧模板管理接口分开：`validateUploadLocation` 只校验允许类型；`validateUploadSelection` 保留给历史严格入口。
- 不增加 fallback、默认名称或静默成功；名称为空、项目未启用、类型不匹配、同租户重名和无权限仍明确失败。
- 名称占用按租户隔离，规范化只做 trim 与英文大小写统一；普通升版/返工不释放，批准作废释放。
- 不执行真实数据库写入、E2E、服务重启、远程操作、Git 提交或推送。

## Cleanup Keep

- doc/tasks/20260917-dcc-minimal-main-flow-implementation/task.md
- doc/tasks/20260917-dcc-minimal-main-flow-implementation/execution-log.md
- doc/tasks/20260917-dcc-minimal-main-flow-implementation/verification-report.md

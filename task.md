# eDHR 偏差管理开发文档任务

## Goal

完成已确认偏差需求的PRD、系统设计、前端交互、开发计划与BDD/TDD计划，并核对自洽性、纠正不符合用户要求的内容。本任务是文档交付，未来功能代码仍未开始。

## Scope

- 以用户最终确认优先于最初描述和参考表中的旧等级/流程；明确普通与重大（关键）、批记录关系、唯一处理记录、完整签名、正常关闭和直接转审例外。
- 新增前端交互和一致性审查，同步API、模型、权限、阶段、AC及测试，保留具体代码依据与实现前置。
- 新增任务内文档校验脚本；向已有docs/powershell-encoding.md追加一条真实换行校验经验。
- 不包含生产代码、Office原件改写、运行服务、数据库、E2E、Git提交/推送或子Agent。

## Milestones

- [x] P1 读取需求来源及现有代码边界。
- [x] P2 完成产品需求、用户流程和验收标准。
- [x] P3 完成前端/API/数据/权限设计。
- [x] P4 完成开发阶段与BDD/TDD计划。
- [x] P5 初版文档结构校验（其局限与错误已在本轮复审纠正）。
- [x] P6 新增前端交互、逐项审查并同步修正全文。
- [x] P7 运行增强文档校验并清理本任务临时脚本。

这里的P1～P7是文档任务里程碑；development-plan.md的P1～P6是未来功能实施阶段，不代表已完成代码。

## Expected Verification

- python -X utf8 doc/tasks/20260923-edhr-deviation-management/validate-docs.py
- 复用product/system/acceptance/node技能校验器，路径映射到本任务；检查真正的Markdown标题/换行、JSON、唯一AC、28项BDD逐项映射、签名与身份门禁及保留清单。
- 人工逐项需求复核记录在consistency-review.md；自动结构检查不代替语义审查或业务测试。
- cleanup preview→apply只删除本任务生成/改写临时脚本，保留16项正式文件。

## Current Status

ready_for_closeout

## Blockers

本轮文档修订无阻塞。后续功能实施必须证明未推送PQC时的正式批记录身份、四项动作来源和既有NCR三类处置；若当前运行态缺这些前置，按P1/P4解决，不猜关联、不改用其他数据来源。

根目录AGENTS.md要求当轮授权才能提交/推送，本轮未授权；docs/task-closeout-rules.md又把Git交付作为最终完成门禁。文档和cleanup通过后记录ready_for_closeout并单列文档完成事实，不执行未授权Git操作。

本轮文档交付及清理已完成：14份Markdown、1份机器状态、1份可复用校验脚本全部保留，3个临时改写脚本已清除。28项AC与28项BDD映射、技能校验器及定向自洽检查通过。总体状态仅因上述Git交付门禁保留ready_for_closeout，未来功能仍not_started。

## 设计约束检查

- 用户确认规则优先；不把审核表建议或工程取舍冒充新增业务要求。
- 不引入fallback/静默降级/假成功；失败、无权、身份缺失和真实空集合分别处理。
- 查询/处理/签署/评审处置不能被四项写门禁误拦；从正式来源解决身份，不能以不可用页面冒充功能。
- 只修改当前任务文件和编码规则的一条经验；不覆盖或处理其他任务源码/暂存区。

## Deliverables

先读[产品需求](prd.md)、[前端交互](frontend-interaction.md)、[一致性审查](consistency-review.md)。
实施按[开发计划](development-plan.md)、[测试计划](test-plan.md)和[验收标准](acceptance-criteria.md)推进；前后端/数据/权限设计在同目录。
当前实测范围和限制见[验证报告](verification-report.md)。

## Cleanup Keep

- doc/tasks/20260923-edhr-deviation-management/task.md
- doc/tasks/20260923-edhr-deviation-management/execution-log.md
- doc/tasks/20260923-edhr-deviation-management/verification-report.md
- doc/tasks/20260923-edhr-deviation-management/prd.md
- doc/tasks/20260923-edhr-deviation-management/user-flows.md
- doc/tasks/20260923-edhr-deviation-management/acceptance-criteria.md
- doc/tasks/20260923-edhr-deviation-management/frontend-design.md
- doc/tasks/20260923-edhr-deviation-management/frontend-interaction.md
- doc/tasks/20260923-edhr-deviation-management/backend-api-design.md
- doc/tasks/20260923-edhr-deviation-management/data-model.md
- doc/tasks/20260923-edhr-deviation-management/config-security-deployment.md
- doc/tasks/20260923-edhr-deviation-management/development-plan.md
- doc/tasks/20260923-edhr-deviation-management/test-plan.md
- doc/tasks/20260923-edhr-deviation-management/consistency-review.md
- doc/tasks/20260923-edhr-deviation-management/task-state.json
- doc/tasks/20260923-edhr-deviation-management/validate-docs.py

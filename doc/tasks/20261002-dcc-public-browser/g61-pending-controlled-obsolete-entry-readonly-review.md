# G61 AC20 — 受控待生效版本作废前端门禁只读复核

Status: read_only_review_complete。Root已实际完成独立文控线下培训上传到审批/受控待生效；BE owner修复待生效作废资格投影。本复核仅静态读取现源，没有运行真实UI/API或测试，也没有生产/测试编辑。

结论：当前前端详情作废入口没有独立“仅ACTIVE”限制；后端给出待生效版本正式canObsolete与allowedActions后，前端能按原合同接入。没有确定性FE缺口，本批无需改源码或强制启用按钮。不能据此声称实际作废E2E已通过。

| 正式链路 | 源锚点 | 实际判断 |
| --- | --- | --- |
| 详情读型动作 | detail/presentation.ts:resolveDccDetailActionProjection、getDetailActionState | OBSOLETE字段为canObsolete，直接读取正式boolean再resolve；status只用于canRetryFinalization，不按ACTIVE过滤作废 |
| Parent业务投影 | detail/index.vue:dccObsoleteActionProjection、canSubmitObsoleteAction | allowed来自detailActionState.canObsolete，再合activeObsoleteAction/error/pending锁，保原签名流程申请互斥 |
| 菜单和启动 | detail/index.vue更多危险菜单、openObsoleteDialog | 管理态危险菜单内v-if canSubmitObsoleteAction；handler同flag再验，无ACTIVE判断 |
| 作废申请弹框 | detail/DetailObsoleteApplication.vue:watch/submit | props.allowed与真实项目身份、矩阵解析、3属性正式快照、原因/部门/二次确认；无ACTIVE-only条件 |
| 共享动作列表 | shared/lifecycle.ts:getDccControlledFileAllowedActions、mapDccControlledFileProjection | 读取正式actionProjection.allowedActions，并保actionLocked/pending，未按所选status删除OBSOLETE |
| 工作流展示helper | workflow/workflow-actions.ts:lifecyclePresentation | ACTIVE只用于executable，ACTIVE/pending用于canDistribute；没有作废判定或作废caller依赖 |

两个响应投影不可互相伪补：详情目前消耗canObsolete，而共享browser/action区域消耗actionProjection.allowedActions。BE正式同源修复应分别保证一致，前端不以allowedActions推造缺失canObsolete。metadata、正文权限、管理/readonly gate和pending action锁维持原规则。

源路径均位于IntRuoyiFronted/src/views/dcc/controlled-file下；API workflow.ts没有额外ACTIVE/作废status filter。本批所有已冻结源码保持，Root负责加载新BE投影后真实管理详情入口和会签→批准结束的作废验收；无需增加角色或菜单能力。

## G61 后端新投影冻结后的有限复核

本轮只读Query新增helper及三个正式caller，未运行构建/测试/HTTP/UI。当前Query raw SHA256=9f2f14d389e989ec795af4368d64099227e87a5967a5628f41cadf6fc3bf31a1；Parent=7584fa5adca8b9d3b4e9fe2a3509db8485c6bd3a7554d5a84e41c42c13a36889；shared/lifecycle=045708aa798ffe9c09e2fe93b86cd0cb22a74f7f28b232df1e786207b24c1a63。

`isObsoleteControlledStage`仅新pending分支验证同当前租户、未删除、nativeUPLOAD/REVISION、受控时间、发布/盖章正ID、真实BPM、Master及正式非工作version；`matchesLockedFormalIdentity`核Master/file tenant/project/type/编号，Master不删除。ACTIVE沿原规则。detail/browser canObsolete仍同时要求原类别OBSOLETE权限；history action列表也用同条件，PRINT/MAJOR_REVISION仍原ACTIVE条件。未看到本有限范围的新角色/权限放宽、candidate仅贴pending状态就获得动作或取currentActive替代源。

`buildActionProjection`的pending作废申请lookup已从active扩大到这个严校验controlledStage；开放申请时OBSOLETE不入allowedActions，保pendingRequestId/申请人可撤回事实。旧总标识actionLocked=!active仍true，但前端并非把整份投影当禁用：shared/lifecycle:mapDccControlledFileProjection明确 `lockedForAction = actionLocked === true && !allowed`，所以正式允许OBSOLETE不会被该标识再次锁；其它未允许动作照常被锁。

实际管理详情作废更直接用canObsolete，不受总actionLocked过滤；Parent另只以真实activeObsoleteAction/error/已开放申请锁控重复入口。loadActiveObsoleteAction接收detail平台对象状态（含pending）但无ACTIVE限定。storage browser的isDccControlledFileActionUnlocked只用于元数据more和working提交资格，不包住detail作废menu/handler。上述两个消费者闭环无确定性FE剩余阻断，无需前端修复。

本次结论限静态接线：没有新P1/P2源码缺口，不等于新Jar已加载或实际作废申请成功。Root继续实际新BE/作废验收，正式后端命令仍是最终权限及并发守卫；本Agent只补此报告，不改任何source/test/权限/数据。

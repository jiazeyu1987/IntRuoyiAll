# 用户批准页面优化：方向3

- worktree：C:/Users/D01020/.codex/worktrees/user-approval-03/IntRuoyiAll-int_main
- 分支：codex/user-approval-03
- 共同基线：5305869c3590c4266092a6f6efba37df16296453
- 运行预约：int_qms slot 17，8078/48078；worktree 与预约保留。
- 基线包含协调者已有源码快照；本提交仅含方向3增量。未合并、推送、发布，未写数据库，未操作其他任务服务。

## 实现

- detail/index.vue：当前任务卡片使用“会签批准”，进度独立读取 MATRIX_APPROVAL completionText / isCompleted / isCurrent，缺失明确显示尚未读取；不取 MATRIX_REVIEW，不写死人数。
- 复用 SupervisorReviewActions 固定底栏与动态高度占位，右侧批准通过、驳回连接既有 openActionDialog；卡片排除重复操作。底栏在任务结束后随真实待办消失，弹窗可见/提交中禁用操作。
- 检查批准权限、账号/文件上下文、错误进度、申请轮次、任务归属及办理人。没有改签名弹窗或审批记录，也没有修改共享底栏实现。
- config/menu-migrations.json：登记复用现有 approve 权限的具体原因，无新增菜单、路由或权限标识。
- 新增 matrix 单测，更新原 supervisor 单测的卡片排除条件。

## 调用链核对

1. 审批中心 handling=approval/from=approval-center 进入共用办理布局；approvalTodoTask 为实际待办，MATRIX_APPROVAL 才展示本底栏。
2. matrixApprovalStage 从 displayStageProgressList 按阶段代码读取，动态进度；matrixApprovalBlockedReason 校验真实待办办理人、轮次、上下文与 approve 权限。
3. SupervisorReviewActions.vue 调用 getControlledFileTaskActionReadiness，出错显式显示；generation 排除旧响应，阻断批准但保留允许的驳回，submitting 同时禁用两按钮；ResizeObserver 根据底栏实际高度预留空间，固定定位 z-index 100 高于导航10。
4. detail/index.vue openActionDialog / submitActionDialog 复用当前上下文检查、提交锁、密码签名及结果检查；成功回待办中心或 reloadAll，刷新真实任务。
5. api/dcc/controlledFile/workflow.ts approve-task/reject-task → DccControlledFileController.java:871/879（既有权限）→ DccControlledFileWorkflowServiceImpl.java:2233/2288。validateTaskAction:3842 校验租户/文件/任务/办理人/轮次/状态，validateStagePermission:3988 校验批准权限；签名服务保存签名，bpmTaskService 完成/驳回任务，syncStatusAfterApprove 更新下游状态。此项为静态核对，未声称运行验收通过。

## 验证与证据

本地证据目录：.runtime/user-approval-03/（保留，未入提交）。

- node --test tests/unit/dcc-matrix-approval-actions.test.cjs tests/unit/dcc-supervisor-review-actions.test.cjs：11/11 PASS；unit-results.txt。
- 编译与导航扩展检查：6/7 PASS；唯一失败为 preview entry and attachment status 的既有静态断言。使用 git show 共同基线提取的 index.vue 重跑，同一失败复现：baseline-navigation-results.txt；未改预览区或放宽断言。
- node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json：PASS（typecheck-8gb.txt 空输出）。首次默认4GB堆内存耗尽；typecheck.txt 保留失败记录。
- python scripts/preflight/check-menu-migrations.py 与 --staged：PASS；menu-check.txt、menu-staged-check.txt。
- scripts/preflight/branch-runtime-port-guard.ps1：PASS，int_qms 8078/48078。
- git diff --check：PASS。
- Playwright 真实 Chromium 打开 http://127.0.0.1:8078：真实登录页可达；login-page.png、page-verification.json。未调用 API/fetch、未 mock、未执行业务动作。
- 服务阻塞：48078 无后端监听且本 worktree 无 yudao-server-exec.jar；真实页面租户请求500，Vite 明确记录 ECONNREFUSED 127.0.0.1:48078。无本任务业务数据，不能登录和验收批准/驳回、任务结束收起、导航滚动/预览展开后的底栏可见与末项空白。因此业务 E2E 未完成，不能标记 PASS。综合真实验收由协调者后续执行。

## 集成注意

index.vue 仅涉及任务卡片（约394）、固定底栏（约2506）、currentApprovalTaskName（约3120）、matrixApproval* computed（约3667）；其他方向可能同区冲突，合并时保留各自条件。config/menu-migrations.json 仅新增一个 permission 决策对象。SupervisorReviewActions.vue、签名弹窗、审批记录未变动。真实页面尚未验证，集成者需用本任务自有数据补验；不得对截图已有样本做批准/驳回。

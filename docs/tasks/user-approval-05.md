# 第 5 方向：会签批准通过 / 驳回

- worktree：`C:/Users/D01020/.codex/worktrees/user-approval-05/IntRuoyiAll-int_main`
- 分支：`codex/user-approval-05`
- 共同基线：`5305869c3590c4266092a6f6efba37df16296453`
- 仅本方向增量；未合并、推送或发布。worktree 与预约保留。
- 原子预约：int_qms，slot 15，前端 8076，后端 48076。启动前端口检查未发现监听；未启动或停止任何服务。
- 已读取项目 AGENTS.md、worktree 限制、ponytail 技能；全局及 worktree 祖先未发现额外 AGENTS.md。

## 实现

`IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`：增加 MATRIX_APPROVAL 专属标题与驳回轮次说明。批准提交前不按人数预测节点（本轮合同可能使用不同批准规则）；提交后只用签名响应 nextStatus 表述等待、实际文控审核或其他真实状态。请求快照记录 taskDefinitionKey，避免异步返回时使用已切换的任务身份。

保留既有共享布局：通用文件图标、实际文件名/编号/版本/当前签名人，意见在前、密码签名在后，批准意见选填，驳回原因必填，密码隐藏，确认左侧、取消最右。未新增页面、路由、权限或菜单，复用原详情菜单与审批权限，无新增迁移项。

## 静态业务链核对

以下为源码核对，不代表运行中的业务验收：

1. 用户入口与校验：详情页 `openActionDialog`、`submitActionDialog`；`approval-actions.ts:validateDccApprovalActionForm` 对密码与驳回原因 trim 后校验空白，批准意见允许为空。提交 busy 阻止重复点击，取消清理凭据，异步请求使用任务/文件/轮次快照。
2. API：`src/api/dcc/controlledFile/workflow.ts:2570` / `2600` 发送批准/驳回；`parseDccSignatureActionResp:2053` 检查必需签名字段、动作结果和 VALID 证据，不接受缺失结果模拟成功。
3. 角色权限：后端 `DccControlledFileController:871` / `879` 检查审批权限；`DccControlledFileWorkflowServiceImpl:3842` 校验文件租户、任务归属、实际流程实例、当前状态、合同与审批路线，再检查阶段权限。宽泛入口权限不会替代实际任务校验。
4. 电子签名：`DccSignatureVerificationServiceImpl:145` 调统一签名服务并要求完整 VALID 结果；`ElectronicSignatureServiceImpl:116` 调 `adminUserApi.reauthenticateForSignature`；`AdminUserServiceImpl:449` 锁定用户行并检查停用、锁定、真实密码，错误密码抛错。签名失败不会继续调用 BPM 推进。
5. 状态与下游：`DccControlledFileWorkflowServiceImpl:2204` 起事务；批准签名后调用 BPM，`syncStatusAfterApprove:4010` 按运行任务/执行合同同步数据库状态，响应 nextStatus；驳回在 `2287` 起事务，先签名再结束任务并保存 REJECTED、驳回原因与时间。前端仅对 `PENDING_MATRIX_APPROVAL` 显示等待，对 `PENDING_DOC_CONTROL_REVIEW` 显示文控审核。
6. 新轮保留：`DccControlledFileQueryServiceImpl:265` 的 `prepareApplicationRestart` 校验原申请人及驳回/撤回历史，再复制正文、附件形成新申请；`DccApplicationRoundService` 保留旧轮关联并为新轮预留和绑定真实 BPM 轮次。既有签名记录不由本弹窗删除或覆盖。

## 验证及证据

- `tests/unit/dcc-matrix-approval-dialog.test.cjs`：新增 5 项通过，涵盖专属标题、不同人数不预测节点、等待/进入文控/驳回三分支、其他阶段保持原行为、任务身份冻结、必填/空白原因、错误密码错误映射与不报告成功。传输替身仅用于单元边界，不作为真实业务验收。
- 已运行共享回归：`dcc-supervisor-action-dialog`、`dcc-approval-dialog-context`、`dcc-approval-success-inbox-cache`，31 项通过；覆盖模板编译、取消清理密码、重复点击、异步切换任务与刷新失败等。
- `tests/e2e/dcc-approval-reason-required-static.spec.cjs`：PASS（静态契约）。
- `scripts/preflight/branch-runtime-port-guard.ps1`：PASS，8076 / 48076。
- `scripts/preflight/check-menu-migrations.py` 及 `--staged`：均 PASS；暂存检查日志为 `C:/Users/D01020/AppData/Local/Temp/user-approval-05-menu-staged.log`。本分支共同基线含其他工作快照，检查输出中的既有 173 项不是本方向新增成果。
- `git diff --check`：PASS。
- 测试通过 `NODE_PATH=C:/IntRuoyiAll-int_main/IntRuoyiFronted/node_modules` 只读使用已安装的 TypeScript/Vue/Playwright；没有向主目录写入依赖或缓存。
- Playwright headless Chromium 实际访问 `http://127.0.0.1:8076/`：`page.goto: net::ERR_CONNECTION_REFUSED`。专属前后端没有监听；worktree 没有 node_modules 或后端 target JAR。页面连通性探测已执行，业务页面验收未执行。

## 真实验收限制与集成提醒

没有 E2E PASS。未登录或签署任何现有样本。仍待在服务就绪后由真实前端创建本任务数据，使用当前轮次实际批准人验证必填、空白原因、错误密码、取消、真实签名，以及“其他批准人未完成 / 实际进入文控审核 / 驳回后保留与新轮”三条分支。只有 admin 测试账号信息，其他批准人身份与本任务数据尚未建立。

集成时重点保留 index.vue 的 `actionDialogTitle`、`actionDialogSubmitFlowText`、`buildActionSuccessMessage`、`readActionDialogRequest` 及成功回调五处局部增量；其他方向可能同改这些函数。不得用提交前 routeSnapshots 下一项或人数推断替换本方向按提交响应显示结果的逻辑。仅 cherry-pick 本方向提交，不把共同基线的其他变更重复计入。

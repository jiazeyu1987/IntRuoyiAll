# 用户批准页面方向 04 验证记录

- 共同基线：`5305869c3590c4266092a6f6efba37df16296453`。
- 分支：`codex/user-approval-04`。
- worktree：`C:/Users/D01020/.codex/worktrees/user-approval-04/IntRuoyiAll-int_main`。
- 端口预约：int_qms / slot 18 / 前端 8079 / 后端 48079。预约前后均未启动或停止其他任务进程；启动检查未发现本槽位端口监听。目录和预约保留。

## 本次增量

仅将 `detail/index.vue` 无项目关联的空态，以及 `DetailRelationsPanel.vue` 已成功读取空集合的空态，统一为“暂无关联文件”。加载失败、权限不足、尚未核验申请轮次仍显示既有实际原因，不伪装为空集合。

共同基线已经具备申请信息、七列审批记录、指派与实际会签分名、人员部门对齐和固定版本读取；这些不是本次新增成果，保留原实现。没有新增权限、路由或页面，不触发新增菜单迁移；没有格式化整个 index.vue。

## 按业务节点静态核对

以下路径相对于仓库根目录，均是共同基线的既有实现。

1. 申请入口：`IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` 的 `DetailApplicationHistory` 传入文件版本、已核验申请类型、BPM 轮次、读取权限和正文上下文。`DetailApplicationHistory.vue` 通过 application-rounds/application-evidence 获取实际提交轮次；校验文件、版本、BPM、属性轮次后显示申请时间、市场与身份属性；变更字段取 selectedRound 的 revisionChangeType/revisionSourceVersionNo/changeDescription。历史属性未冻结或未记录明确提示，未借当前项目属性补齐。QMS 文件库延用既有“不适用”语义。
2. 接口及持久数据：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java` 的 application-evidence 进入 `DccControlledFileQueryServiceImpl.getApplicationEvidence`，校验申请读取权限、文件租户身份、正式 BPM 映射，读取冻结 actualAttributesJson。缺失、歧义和错轮次显式失败。
3. 原始记录：后端 `DccApplicationNodeRecordProjection.project` 逐签名匹配实际历史任务，指派取 assignmentSignatureId 绑定快照，分别提供实际办理人与义务部门；已结束且无签名的真实任务单独保留。前端 `approval-records.ts` 保留每条记录，不合并指派和实际会签；`DetailApprovalRecords.vue` 复用七列公共表，处理结果为文字，签名显示服务端状态；ResizeObserver 对齐姓名/部门主行高度。
4. 签名有效性：前端 `application-node-records.ts` 检查签名 ID、taskId、BPM 绑定；后端 `DccControlledFileQueryServiceImpl.projectSignatureBindingStatus` 使用签名事实，已发布副本绑定验证失败投影为 INVALID。前端不制造“有效”状态。未在本次修改签名服务或历史记录。
5. 本次批准：`index.vue` 的 submitActionDialog 等待 submitDccApprovalAction 返回成功后才关闭并刷新；失败保留真实错误。展示表从服务端证据生成，没有打开弹框或点击按钮即插入批准历史的乐观记录。
6. 固定关联入口：`DetailRelationsPanel.vue` 读取所选申请冻结集合或所选历史快照；openPreview 再核验目标 controlledFileId/masterId/projectId/versionNo/权限/状态，构造 VERSION_HISTORY + 固定 ID + expectedVersionNo。失败分支不请求最新版本。
7. viewer 与后端：`controlled-file/view/index.vue` 的 resolvedOnlinePreviewSource 延续 previewSource.bodyReadContext；`api/common/filePreview.ts` 保留规范化上下文，元数据及正文使用同一来源。后端 `withBodyReadContext` 按 ID 读文件并经 `DccControlledFileDetailAuthorizationGuard.requireBodyRead` 校验租户、角色、读取范围与状态；`DccControlledFileBodyReadContext.requireFile` 比对实际文件 ID 与 expectedVersionNo；正文读取走精确上下文授权。`DccControlledFileQueryServiceImpl.resolvePreviewReferenceId` 对保留受控版本读取该记录自己的 publishedFileId。

## 已运行检查

- Node 单元/组件测试 46/46 PASS：`dcc-approval-records.test.cjs`、`dcc-application-node-records.test.cjs`、`dcc-detail-application-round-context.test.cjs`、`dcc-selected-version-relations.test.cjs`。
- 测试包含真实 Vue SFC 编译/渲染、指派和会签分别保留、七列顺序、意见和签名状态、换行对齐、申请切换失效处理、固定旧版本存在新版时仍取旧版、权限失败不重试新版。测试隔离输入仅用于单元测试，不作为真实业务 E2E。
- `scripts/preflight/branch-runtime-port-guard.ps1` PASS：codex/user-approval-04 / int_qms / 8079 / 48079。
- `python scripts/preflight/check-menu-migrations.py` PASS（累计 173 项既有菜单/权限信号）。
- `python scripts/preflight/check-menu-migrations.py --staged` PASS，日志 `output/user-approval-04/menu-staged.log`。
- `git diff --check`、`git diff --cached --check` PASS。
- 测试使用 NODE_PATH 只读解析主目录已安装的依赖；没有在主目录安装依赖或写源码。

## 真实页面验收限制

Playwright 已尝试打开本任务 `http://127.0.0.1:8079`，实际返回 `net::ERR_CONNECTION_REFUSED`，状态 BLOCKED。当前 worktree 未安装前端依赖、未构建后端 executable jar，且本槽位无就绪服务；未切换到其他任务服务验收。没有登录，没有操作任何已有业务样本，没有批准、驳回、直接 API 或 DB 业务写入。**不声明 E2E PASS**。

保留现场证据（worktree 内，生成物不提交）：

- `output/user-approval-04/page-service-check.json`
- `output/user-approval-04/page-service-check.png`

待集成者在合并版本的真实环境，通过页面创建本任务自有数据，验证初始上传/变更属性、三条原始审批记录及新增批准成功后的记录、固定旧版关联预览和错误状态。

## 集成注意

只 cherry-pick 本方向相对共同基线的提交，不复制基线全量源码。`index.vue` 仅关联空态一行与其他方向共用文件，若冲突应保留其他方向布局并统一空态文字；`DetailRelationsPanel.vue` 仅两处空态文字。没有修改公共记录表或后端，不覆盖其他方向对此类文件的修改。未推送、发布或合并到 int_qms。

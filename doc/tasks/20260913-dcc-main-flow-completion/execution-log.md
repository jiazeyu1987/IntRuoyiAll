# Execution Log

## Request and Scope

- 用户要求：“重点考虑主流程，让主流程能走完整”；排除培训、分发。
- 已使用bug-regression-fix-loop并读bug-contract、根AGENTS、开发、编码、数据库及收尾规则。
- 本轮实现范围聚焦017正式主体、019重复环节、021图纸审批预览、030检出与送审竞争，主流程依赖按实际调用链核对。
- 未授权E2E、子Agent、Git提交推送、服务或运行库操作，不执行。

## BDD

- BDD: 有效项目授权 -> Given 有效项目及真实授权主体 When 保存OWNER/EDIT Then 有效授权可用于后续上传；不存在或停用主体保存失败且原规则不丢失。
- BDD: 固定四环节 -> Given 四环节审批策略 When 保存或解析重复环节 Then 提前拒绝，不静默丢弃候选人；恰好四个唯一环节正常送审。
- BDD: 图纸受控预览 -> Given 当前CAD源件及其有效配套PDF When 工作稿或当前审批人预览 Then 返回配套PDF并保持当前版本对应关系。
- BDD: 送审与检出并发 -> Given WORKING版本且没有编辑锁 When 送审先成功、等待的检出继续 Then 检出必须按当前状态拒绝，不留下审批中编辑锁。
- BDD: 主流程发布浏览 -> Given 合法项目模板、上传件及完整四环节 When 当前工作版本送审并完成审核批准和发布 Then 具体版本成为正式版且可受控浏览，越权及缺失材料明确失败。

## TDD Evidence

- GREEN: python C:/Users/BJB110/.codex/skills/bug-regression-fix-loop/scripts/validate_bug_regression.py --evidence doc/tasks/20260913-dcc-main-flow-completion/verification-report.md -> PASS。
- GREEN: 只读文档终检 -> PASS，15类/423项测试统计、17份源码SHA-256/行数/UTF-8、代码引用、33条bug索引和4项修复状态、任务文档格式/JSON均通过。
- project-experience-consolidation已将图纸独立预览副本和主流程夹具核对经验合并到既有docs/backend-development.md；未创建额外长期经验文档。
- task.md与task-state.json已先置ready_for_closeout，接着执行当前任务cleanup preview/apply。
- GREEN: task-closeout-cleanup preview -> PASS，保留task.md、execution-log.md、verification-report.md、task-state.json；delete/blocked/warnings均为none。
- GREEN: task-closeout-cleanup apply -> PASS，没有删除任何文件；当前为主工作区int_main，未执行worktree合并或删除。
- CLOSEOUT GATE: Git status显示`int_main...origin/int_main [ahead 13]`，且存在两个未提交前端合同文件；按项目规则，未经当轮明确授权不得执行Git提交/推送，因此任务保持ready_for_closeout，不能标记completed。
- RED: `pnpm exec node --test scripts/dcc-frontend-api-fail-closed-contract.test.mjs` -> FAIL，10项中1项失败，原因是前端合同测试已切到文控日志页但仍引用旧`auditApiSource`变量，测试自身在日志页检查处断链。
- GREEN: `pnpm exec node --test scripts/dcc-frontend-api-fail-closed-contract.test.mjs` -> PASS，10/10，日志页/API断言改为当前`logs.ts`、`logs/index.vue`和`controlled-file/logs`路由。
- GREEN: `pnpm exec node --test tests/e2e/dcc-controlled-file-protection.contract.test.js` -> PASS，1/1，确认受控文件保护合同仍匹配当前预览入口和请求ID头写法。
- CLOSEOUT AUTHORIZED: 2026-09-14 用户要求“提交并融合进int_main”，解除本任务Git提交/推送门禁。
- GREEN: `git push origin int_main` -> PASS；推送后`git rev-list --left-right --count HEAD...origin/int_main`为`0 0`，本地`int_main`与`origin/int_main`一致。
- SCOPE GUARD: 推送后仍存在MES/PQC相关未提交文件，不属于本DCC主流程任务，保持未暂存、未提交、未回滚。

- GREEN: 最终15类主流程回归命令（完整参数见verification-report.md）-> PASS，423 tests、0 failures/errors/skipped，15份Surefire XML逐项核对；包含新建工作版本后送审同一ID的串联服务测试、独立PDF副本与重放、受控预览保护和发布回归。
- GREEN: 最终13项Node静态合同 -> PASS；本次实现/测试文件git diff --check -> PASS，仅LF/CRLF配置提示。
- 017、019、021、030已在原bug记录索引与明细标记FIXED_REGRESSION_VERIFIED，其它问题状态保持各自原记录；没有将本轮结果解释为33项全修复。

- RED: 修正夹具后执行DccControlledFileQueryServiceTest的metadataOnlyDrawingCheckinCopiesCompanionToKeepPreviewOwnershipUnambiguous、metadataOnlyDrawingCopyFailureCleansPreparedSourceAndKeepsCheckout -> FAIL，2项均按预期失败：仍沿用旧PDF ID、没有进行PDF复制及失败回滚。
- RED: 同组加入metadataDrawingReplayVerifiesCopiedPdfContent -> FAIL，4项中3项暴露缺口、1项正确拒绝内容变化；同内容独立PDF副本仍被错误拒绝重放。随后实现独立副本、复制失败清理及内容一致性重放校验。

- Maven初次调用因未给-Dstyle.color=never整体加引号，被PowerShell拆为错误lifecycle phase；修正命令后重跑，不将命令解析失败算作RED。
- RED: mvn -pl yudao-module-dcc -am test "-Dtest=DccProjectAccessServiceImplTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dstyle.color=never" -q -> FAIL，158项中16项按预期失败、0 errors：主体校验8项、重复环节2项、CAD预览5项、送审后检出1项。
- 测试使用现有Mockito端口及H2内存夹具；所有旧用例通过，没有执行真实服务或运行数据库操作。
- 首轮修复回归：158项中157通过、1失败。新增16项已通过；旧“未来审批环节人员无权预览”用例发现图纸分类提前读取文件元数据。保留该断言，调整为先授权再解析实际PDF载体。
- RED: mvn -pl yudao-module-dcc -am test "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dstyle.color=never" -q -> FAIL，122项中仅新增“修改CAD缺少新PDF仍能检入”失败；原121项已通过，包含未授权预览保护。
- BDD: 图纸检入与重放 -> Given 当前CAD版本已有旧PDF When 检入修改后的CAD Then 必须绑定同会话新PDF；重放只能读取已绑定到相同用户/版本的PDF，不能复用其它版本票据。
- RED: pnpm exec node --test src/views/dcc/controlled-file/browser/checkin-main-flow.spec.cjs -> FAIL，4项中2项失败：缺PDF仍提交、请求漏传PDF票据；普通文档和仅备注检入通过。执行实际页面提交处理器及现有校验函数，API端口使用本地替身，不是E2E。
- RED: mvn -pl yudao-module-dcc -am test "-Dtest=DccControlledFileQueryServiceTest,DccUploadTicketServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dstyle.color=never" -q -> FAIL，新契约尚缺setDrawingPdfUploadTicket和resolveBoundFile，测试编译失败；前一轮已独立复现实际CAD行为缺口。本条是新增接口契约RED，不当作环境阻塞。
- GREEN: mvn -pl yudao-module-dcc -am test "-Dtest=DccProjectAccessServiceImplTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileQueryServiceTest,DccUploadTicketServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dstyle.color=never" -q -> PASS，185项，0失败/错误/跳过（17+20+124+24）。
- GREEN: pnpm exec node --test src/views/dcc/controlled-file/browser/checkin-main-flow.spec.cjs -> PASS，最初4项通过。
- RED: 同一前端命令增加迟到PDF上传响应场景 -> FAIL，旧请求清除了新源件已经上传的PDF；修复请求生命周期归属后GREEN，同一命令5项全部PASS。
- 前端首次pnpm ts:check PASS；迟到响应修复后需按最终源码复跑。
- GREEN: 最终前端pnpm ts:check -> PASS，exit 0；迟到响应修复后的5项Node回归也PASS。
- 扩大主流程回归：410项，13 failures/17 errors。定位为三个旧测试夹具组：Workflow路由工厂全部写ALL/100且部分只造一阶段，与已有固定四阶段政策不符；PreviewProtection缺AssignmentScope依赖；AtomicPublish静态断言仍查已移到独立失败服务的方法名。保持权限、Token和发布失败语义，补齐明确的四阶段夹具、作用域依赖和跨类失败落库断言，不放宽生产门禁。
- 第二轮主流程回归：412项，4 failures/0 errors；剩余均是Workflow测试预期仍为1阶段、一个无效会签人用例仅配置2阶段。按实际固定4阶段补齐，保留首阶段候选人和失败原因断言。
- GREEN: 13个与主流程有关的既有Node静态合同（001/002/003/006/007/008/009/010/013/014/015/023/025）全部PASS。
- BDD: 仅备注检入图纸 -> Given CAD和PDF属于当前版本 When 只改备注生成新小版本 Then 自动验证并复制PDF，避免多个DCC版本共享同一文件ID触发受控访问“归属不明确”；副本失败不得生成版本并清理本次源件副本。
- 两条新PDF副本测试初次夹具把PreparedSource的sourceFileId/originSourceFileId参数顺序颠倒，且缺少无票据检入CAS结果，导致到达了错误的失败点；已按实际record定义修正，未将这次夹具错误记为有效RED。

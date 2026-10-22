# Verification Report

E2E-18 升版申请与历史附件：INCOMPLETE / WAITING_ROOT_ELECTRONIC_RECIPIENTS_RED。新包200656/PID34440真实UI回读后已检入同名不同内容源件及两新附件，工作稿4246已创建，来源基线4232/A1保留；局部变更已选择4246/目标A2，尚未正式提交。新包全域查询仍30秒超时，不能关闭性能缺陷。Root唯一授权新增电子分发名单行为RED，测试已冻结、Maven待Root；生产未改。最新业务断点见business-checkpoint.json，测试交接见electronic-recipients-red-frozen.json，以下旧阶段文字仅为历史。

## 新包原页面与工作稿实测

- GET /admin-api/dcc/controlled-files/browser-page，自然参数pageNo=1/pageSize=20/keyword=DCC241016C18PARTIAL/latestVersionOnly=true，无project/folder；普通getControlledFileBrowserPage存储分支。发送2026-10-18 21:39:52.679 +08:00，21:40:22.688失败，30009ms，net::ERR_ABORTED。页面显示列表数据已失效/30秒timeout。
- 同一请求发出6秒时只读jcmd采到HTTP线程：hasActiveElectronicDistributionAccess → countActiveElectronicRecipientAccess，证据global-natural-request-window.json及本轮global-at-6s-dcc-http-threads.txt；不据旧热点改代码。
- 草稿2054545668044084246由真实检入创建，文件名仍DCC241016C18-PARTIAL-OLD-BODY.pdf；预览命中NEW正文标记且OLD无匹配，canvas685×969、625样点中7点非白，正文来源ID匹配4246。新两附件在详情可见，旧培训未被复制。
- 检入POST请求中无sourceOriginalFileName字段；成功源件UI文件名和本地同名路径已留证，当时未单独采input.files[0].name，不事后重传补造。正式版本列表和新正文证据证明草稿实际已生成，不能因另一个异常日志重复checkin。
- 原申请表单已选择局部变更、草稿4246、目标A/2及说明，尚未正式提交；原A/1培训不重复绑定。Root分配RED后仅只读存断点，浏览器/runner正常关闭、退出0、无残留。

## 本轮实际结果

- file2054545668044084232 / DCC241016C18PARTIAL，BPM0a3b05d4-cace-11f1-a035-b082e25ec548，申请人910503。真实完成S/L/C/A/D签名及U培训文件绑定，精确详情回查显示“受控（已生效）/ACTIVE/A/1”，证据文件DCC241016C18-PARTIAL-OLD-TRAINING.pdf。
- 培训绑定首次页面30秒超时，实际已保存；已以独立真实UI回查确认，禁止重传/重绑。RootDB事实不代替UI证据。自然绑定端点、ticket已返回布尔事实及准确点击时间2026-10-18 16:51:44.327495 +08:00均保存在business-checkpoint.json，不含ticket值。
- 当前运行SHA bf2fd4962d74a923cca3bbf869d87b5407031335f0c410c060c554975c07169f，新run为run-2026-10-18T07-43-21-112Z；旧SHA证据仍保留。
- 浏览全域页面实际30秒超时，Root诊断目录集合逐候选重复读取并唯一分配本例行为测试。测试方法getControlledFileBrowserPage_readsDirectoryGrantsOncePerRequestAndRechecksRevocation已写入现有Query测试类，静态diff检查PASS，未执行Maven/RED/GREEN，未改生产。完整原用例仍须继续，不以基线受控冒充升版PASS。

## 2026-10-18 更新窗口断点

- 真实页面最后核对：DCC241016C18P1 / DCC241016C18PRODUCT 项目申请状态COMPLETED，负责人DCC241016C18U（910503），目录模板ID25，批准账号DCC241016C18A（910507）；已关闭只读项目申请弹窗。
- 7个本人测试账号已创建并首次登录改密；业务U/S/L/C/A/D ID依次910503至910508，Config ID910483。角色、组织、岗位、直属主管、任务目录及项目已成功，不得恢复时重建。
- Config最后停在文件类型tab，当前可见树无本例前缀类型行。尚未建立本例文件类别/权限或任何受控文件、升版申请、BPM流程/任务；file/application/process/task ID均为空。
- 所有context检查均无选定文件，无临时源件上传在途；未重传、未假报清理。旧闲置context已自然退回登录页，恢复必须新context本人登录。
- 既有traceErrors中一次“Password fields remain populated; trace remains stopped”为先前首次登录脚本等待错误，保留原记录；它不是业务升版缺陷或本次关闭失败。trace关闭与runner进程确认结果另见runtime-pause.json。
- 恢复后先只读核对既有项目和页面，再按Root运行态释放继续专有类型/类别/权限、两条受控基线、培训及附件、局部/换版完整流程与版本归属验证。两个分支均未验收通过。
- 暂停收尾实测：12个context/browser正常关闭、12份final trace保存、关闭错误0；readline驻留后仅对本例PTY发送Ctrl+C，runner退出码1，原runner及浏览器子进程残留0。20份zip文本脱敏检查已核验3种已知凭据，命中0、归档读取错误0；此前DPAPI换行导致的无效扫描已明确作废重跑。runtime-pause active=true、browserClosed=true、runnerResidual=NONE，保留历史trace告警，不声明整体E2E PASS。

## Scope

- Given：存在受控版及可选择的小版本草稿，旧版有附件和培训记录。
- When：从真实受控浏览入口进入详情，发起局部变更或换版变更，选择草稿、填写实际变更类型和说明，完成审批流程并回看新旧详情。
- Then：升版详情展示来源受控版本、所选草稿、实际变更类型和说明；新旧附件、培训记录及申请信息分别归属原版本；草稿在用户页面显示为“草稿”，不暴露小版本号。

## Parameterized Cases

| Case | UI input | Required assertions | Status |
|---|---|---|---|
| `partial` | `局部变更` + `DCC241016C18-partial-change` | 来源受控版本、选中小版本、变更类型、说明、历史附件/培训记录隔离 | PREPARED_WAITING |
| `replacement` | `换版变更` + `DCC241016C18-replacement-change` | 来源受控版本、选中小版本、变更类型、说明、历史附件/培训记录隔离 | PREPARED_WAITING |

## Page And Role Contract

- Login page: `/login?redirect=/index`; tenant must be confirmed on the visible page. Username/password are runtime-only environment inputs and are not stored in this report or script defaults.
- Source/version entry: `/dcc/controlled-file/browser?browserMode=storage` -> file-number global filter -> row `追溯` -> selected-version detail `文件附件`, `培训记录`, `版本历史`, and application history.
- Revision write entry: browser version selector chooses `草稿` -> row `填写审批申请` -> detail `准备本次申请`/`发起局部 / 换版变更` -> `[data-testid="dcc-revision-panel"]` -> `选择本次送审正文` -> `[data-testid="dcc-revision-submit"]`.
- Required roles: real applicant, real revision approver/document-control operator, and any configured signoff/approval users. Exact accounts, role codes, menu permissions, and credentials remain Root preflight items; `admin` cannot substitute for them.

## Preparation Evidence

- Script: `doc/tasks/20261016-dcc-e2e-18/dcc-e2e-18-real-flow.cjs`.
- Prepare command: `node doc/tasks/20261016-dcc-e2e-18/dcc-e2e-18-real-flow.cjs`.
- Expected current output: `PREPARED_WAITING`, `browserStarted=false`, because dispatch case `E2E-18.gate=prepare_only`.
- Static syntax command: `node --check doc/tasks/20261016-dcc-e2e-18/dcc-e2e-18-real-flow.cjs`.
- Playwright source: `IntRuoyiFronted/node_modules/playwright`, loaded only after `execute` gate validation.
- Forbidden operations audited in script: no `fetch`, `axios`, `APIRequest`, SQL, mock route, localStorage login, or direct API business action. Natural page responses may be recorded as method/path/status only.
- The execute path requires at least two visible attachment rows on both the old controlled detail and the submitted revision detail, and records the visible training section before submission. No attachment or training fixture is fabricated by the script.
- Each execute branch requires runtime-injected visible facts for `OLD_VERSION_LABEL`, `OLD_BODY_MARKERS`, `OLD_ATTACHMENT_NAMES`, `OLD_TRAINING_FACTS`, `DRAFT_VERSION_LABEL`, `DRAFT_BODY_MARKERS`, `DRAFT_ATTACHMENT_NAMES`, and `DRAFT_TRAINING_FACTS`, plus `OLD_TRAINING_RECORD`. Old and draft body markers, attachment names, and training facts must be pairwise disjoint and are matched against visible fields/text; row counts alone are insufficient.
- The old and draft body paths open the page's real preview window, search each supplied marker through the PDF text-search UI, and assert the corresponding non-empty canvas dimensions and non-white pixel samples. A table row, preview container, or canvas node alone cannot satisfy the version-body assertion.
- Trace and cleanup evidence is fail-closed: `traceError` and `cleanupError` are persisted in `runtime-result.json`; either field forces `BLOCKED_TRACE_OR_CLEANUP` and can never leave a full-success status. Submitted objects remain recorded in `residual` with `cleanupAttempted=false` and `cleanupCompleted=false` during prepare-only review.
- Tracing starts only after the visible login redirect completes; login password fields and login responses are excluded from the trace window. Network evidence stores only redacted URL, method and status, never headers, cookies, passwords or signature values.
- Current script coverage ends at the applicant's real UI submission and records `BLOCKED_PENDING_APPROVAL`; it does not claim the requested full workflow. Exact multi-role approval/signature actions, new-version training record entry, document-control completion and page-based cleanup still need implementation after Root confirms the precise actors and lifecycle fixture.

## Blocking Gaps

- Root dispatch reports the int_qms runtime verified, but `roleFixtureVerified=false`; this case remains `prepare_only` and cannot start a browser.
- No confirmed task-owned identifiers for the ACTIVE baseline, selectable WORKING draft, submitted version, distinct body markers, old/new attachment names, or version-scoped training facts.
- Exact applicant/reviewer/document-control accounts and credentials are not available to this thread. No password guessing, reset, role assignment, or admin substitution was performed.
- The missing portions of the source requirements (E2E-06/E2E-08 tail rules and stamp detail) remain unknown and are not inferred here.

## Real Execution

`NOT RUN`: browser launch, page writes, uploads, signatures, approvals, final-state assertions, traces, screenshots, and cleanup are pending Root release.

## Cleanup

`NOT ATTEMPTED`: no business data was created or changed in this preparation phase.

## 2026-10-18 恢复执行

- 运行身份：int_qms 8061/48061，后端PID18204，Jar SHA a27b8d660697531b920a4728cb9f2d30fd55c1afd0cf40d131f7eb4e7523d402。
- Runner PID20780，工具进程handle63720；详情见checkpoint.json和run-2026-10-18T03-09-42-074Z。
- 本例配置账号及U/S/L/C/A/D六个业务身份已真实UI创建、分配最小角色、配置部门/签名岗位/直属主管，并分别在独立context完成首次登录。
- 本例目录DCC241016C18DIR已回查确认；本例项目DCC241016C18P1已由U真实提交，当前记录PENDING_REVIEW。
- 脚本授权弹窗角色身份等待错误已定位并修正，恢复本例Config菜单，不涉及共享角色；后续必须先断言弹窗精确角色标识再写。
- 本例业务主断言尚未完成，不能记PASS。

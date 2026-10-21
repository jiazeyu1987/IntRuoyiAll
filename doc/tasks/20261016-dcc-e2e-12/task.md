# E2E-12 部门指派与全部会签



## Goal



独立验证 E2E-12，业务唯一来源为 doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md 对应行；由 Root review 本例真实页面证据。



## Current Status



blocked — R3第二轮业务 PASS；当轮禁止 Git/清理，证据保留、工程未完成。
## 2026-10-20 第二轮最终范围



- Given：Root本轮plan放行E2E-12，真实运行态27908/48061、20976/8061、int_qms；既有本例R3账号各自仅属一个部门。

- When：PUBLIC创建3份唯一新申请，真实主管签名、负责人本人指派、会签人本人通过/驳回；CUSTOM另用新编号与正文填写本次D1并增补D2和独立批准人。

- Then：PUBLIC默认来源、无自动人员、部分阻断、全部通过推进、任一驳回阻断均以本次实际DOM验证。CUSTOM上传显示“当前用户没有该文件类别的上传权限”，未提交，后续5项检查全部BLOCKED，绝不沿用历史CUSTOM PASS。

- 本轮里程碑：[x] 运行证明/分支核对；[x] PUBLIC正向及C1/C2驳回三单；[x] CUSTOM适用入口和真实权限阻塞留证；[x] 独立结果/脚本错误/隐私/CRC/关闭回执；[ ] CUSTOM负责人指派、部分/全部会签及驳回门禁（缺少类别上传权限）。

- 设计约束：只在原任务目录保存记录，全部业务动作来自真实Playwright前端；没有API/DB动作、响应JSON oracle、mock、产品/公共配置变更、Git写入、重启、子Agent或下一轮。保留历史与本轮资产，不运行cleanup apply。

- 本轮结束为PARTIAL_BLOCKED。详情以独立result.json及handoff.md为准；以下历史里程碑和历史PASS不代表本轮CUSTOM已验收。



## 2026-10-19 r3 定向补证



- 来源：协调目录repair-dispatch-20261019-r3.md及本轮用户授权；沿用ponytail最小范围。

- Given：本例自有PUBLIC_MATRIX类别配置默认会签D1，并通过真实页面准备独立负责人及会签账号。

- When：申请首次自动带出D1，申请人仅增补D2；两部门负责人分别签名指派，两会签人分别通过。

- Then：提交快照保留默认D1与增补D2；C1通过后仍未进入批准，C2通过后才进入批准。禁止admin代签。

- RED：历史结果明确mode=APPLICATION_CUSTOM，D1由手工选择，不能证明PUBLIC来源；这是参数证据缺口，不伪称生产代码失败。

- GREEN：PUBLIC类别及默认D1会签矩阵已由本轮真实页面保存；D2补充后、部分/全部会签阻断批准的申请链路尚未运行，因此BDD仍未通过。唯一运行目录，context序号及segment防止trace覆盖。历史已覆盖trace不可重建为原始证据。

- 凭据检查：当前进程无DCC_E2E12凭据环境变量；原登录helper只读运行时环境，account-credential-phase.json仅保存已改密账号标签；不包含凭据路径。已检查本例及本机临时目录相关文件名，未找到可复用安全凭据文件。不猜测、不重置原账号；按本轮授权通过页面准备新参数账号。

- 里程碑：1规则/凭据核对；2专用参数配置及账号；3PUBLIC来源和全部门禁页面验证；4历史证据核查、r3交接。

- 预期验证：仅新PUBLIC分支；不占Maven/全量类型检查，不重启共享服务，不改共享Query/矩阵/租户job，不动他人资产，不Git提交推送；所有业务动作经Playwright真实页面。



## 2026-10-19 r4 定向补证



- 范围：仅r3自有PUBLIC类别、账号、项目绑定和上传权限；修正矩阵批准人为独立账号，再真实页面验证PUBLIC默认D1、申请增补D2、部分会签不能推进、全部通过才推进。CUSTOM既有PASS保留不重跑。

- Given：r3 PUBLIC类别的唯一上传者具备本例项目可见性和类别上传权限，矩阵会签D1、批准人为独立用户。

- When：真实上传页首次自动带出D1并增补D2；L1/L2分别为本部门指派；仅C1通过时回读审批待办未进入批准，C2也通过后回读推进到批准。

- Then：申请快照中D1默认与D2增补并存；用户指派、签名及全部同意门禁均由各自真实账号页面完成。不得用API/SQL/mock、管理员代签或旧包证据替代。

- 运行态边界：共享包由Root统一协调，本线程不执行Maven、不部署/重启；不得把旧包结果作为r4新GREEN。

- 派发材料更正：R4 delegation 中 `repair-dispatch-20261019-r4.md` 可读；此前因读取时文件名遗漏 `dispatch-` 而误记为缺失。R5 以 `repair-dispatch-20261019-r5.md` 为准。



## 2026-10-19 R5 定向补证



- 范围：CUSTOM 已由 Root 接受的三单/九项业务结果保留，不重跑；仅补本例自有 PUBLIC 配置、最小上传权限、项目可见性及其页面回读，再验证 PUBLIC 自动默认 D1、增补 D2、指派、部分未完成阻断、全部通过推进。

- Given：本例 PUBLIC 类别仅以专用账号/项目配置；矩阵批准人为独立用户 910521，不与 D1 负责人重复；回读矩阵预览无重复主体提示，上传者具备类别上传权限和本例项目可见性。

- When：使用真实 Playwright 登录的本例账号打开上传页，核对 PUBLIC 类型自动带出的 D1，仅增补 D2；L1/L2 分别指派 C1/C2，先完成部分会签并确认仍未推进，再完成剩余会签并确认才推进。

- Then：PUBLIC 默认来源与申请快照、部门指派及门禁由真实页面可见证据证明；账号/权限准备成功不算业务 E2E PASS。任何必要前置失败时记录明确 BLOCKED 原因及截图/trace。

- 禁止：不改既有公共矩阵、不重跑 CUSTOM、不使用 API/SQL/mock/状态注入；不执行 Maven、部署或重启共享运行态，不提交/推送 Git，不向其它线程发送消息。

- R5 里程碑：1) [x] 独立批准矩阵预览及类别上传权限 fresh 回读；2) [ ] 上传页默认值与最小申请（项目可见性前置 BLOCKED，未执行）；3) [ ] 负责人指派及部分/全部会签门禁（依赖前置，未执行）；4) [x] 更新 R5 handoff 区分旧 PASS、配置 GREEN、真实 E2E 未运行和阻塞。



## BDD



R7 项目权限前置：Given 原任务记录项目285/原OWNER DCC241016C12L1，When 在 Root 新运行证明后使用正式项目列表/详情 UI 回查，Then 只有显示真实项目名称/编码及权限规则才确认项目身份；被访问控制拒绝的空详情不证明项目存在或不存在。不改权限范围、不借账号别名混淆新旧负责人、不猜旧凭据。当前为前置 BLOCKED，尚无生产缺陷 RED，也没有生产代码修改。



- Given 本次会签包括保留的默认部门和增补部门。

- When L1/L2 分别签名并指派本部门 C1/C2；分别由部分人员及全部人员完成签名会签。

- Then 系统不自动选定具体会签人；各负责人完成本部门指派并签名；全部选入会签任务都须完成且通过，部分通过或任一未通过时不能进入批准。



## Milestones



1. [x] 五个参数子用例、正式入口、真实角色与数据前置清单。

2. [x] 业务脚本、trace 隐私/失败处理、精确任务绑定及结构检查。

3. [x] 最新授权后由真实页面创建账号，完成首次改密及四 context 身份/菜单核验。

4. [x] 本例项目/产品由真实页面提交并由 L1 本人审核到 COMPLETED；专用分类准备完成。

5. [x] 独立目录、类别APPLICATION_CUSTOM、主管与四个有效个人签名；保留本次D1并增补D2。采用正向、C1驳回、C2驳回三份申请覆盖原五项业务断言。

6. [x] 三份真实申请覆盖原五项业务断言及取消/缺签名边界，关闭浏览器并保存逐步证据。

7. [x] Root独立review：三单/9项检查按原证据边界接受，见协调目录e2e12-root-review.json。

8. [x] R6 PUBLIC_MATRIX定向真实页面验收：自动D1、仅增D2及下发独立，主管审核、两负责人签名指派、部分会签阻断、全部通过推进批准均通过。R5/R7/R8阻塞为历史；最新R9证据待Root审查，工程Git收尾另列。



## Expected Verification



- default-and-added-departments：保留本例专用类型的默认部门 D1，并在申请增补 D2；各负责人分别签名指派本部门人员。

- no-auto-selection：各部门指派下拉初始为空，不能自动选定 C1/C2。

- partial-signoff-blocks-approval：只有部分实际会签人通过时不能进入批准。

- all-signoff-approve：两部门全部选入任务都完成且通过后才进入批准。

- any-reject-blocks-approval：任一选入任务驳回时不能进入批准，保留原始意见/签名事实。

- 逐分支保存可见 DOM/导航、截图、trace、最终业务状态、清理与残留。准备/静态/账号核验不替代业务 E2E。



## 授权与执行门禁



- 最新 execution-authorization.md 已取代旧 prepare_only 和等待 Root 限制，无需再次申请同一授权。

- 本例共享 dispatch gate=execute，accountSetupGate=authorized，businessGate=execute_after_own_verified_fixture；只读共享 dispatch，不修改它。

- PER_CASE_REAL_UI_MANIFEST模式不因共享roleFixtureVerified=false停工。旧四申请脚本属于历史准备记录；本次以交互式真实Playwright runner的三申请记录为准，不重跑旧脚本。

- I04入口修复与本次业务PASS分别保留证据。APPLICATION_CUSTOM只验证本次原选D1后增补D2，未验证公共矩阵默认来源。

- page-execution-control.json已由Root健康恢复通知更新；本次结束后Root要求仅收口现有证据，不继续业务或资产清理。



## 已完成资产



- L1/L2/C1/C2：DCC241016C12L1/910364、DCC241016C12L2/910365、DCC241016C12C1/910366、DCC241016C12C2/910367。独立真实登录，非管理员。

- D1/D2：DCC241016C12D1/910342、DCC241016C12D2/910343；岗位 DCC241016C12Post/11；权限角色 DCC241016C12REVIEW/991253。

- L1另通过页面分配现有正式项目审核员角色，只改本例账号绑定，不改共享角色定义/菜单及其它账号。

- 项目 DCC241016C12P1，产品 DCC241016C12Product，申请#24页面终态 COMPLETED；使用本例文件夹模板 DCC241016C12Folders/#8。

- 当前有效类型DCC241016C12CUSTOMTYPE位于正式技术文档/设计和开发策划阶段；类别DCC241016C12OTHER/#908758，APPLICATION_CUSTOM，目录DCC241016C12Storage。旧T2/T3/T4/T5保留历史，未用于本次申请。完整事实见fixture-manifest.json。

- 早期树定位、首次改密和异步菜单等待问题已修正；旧探索失败记录不作为当前产品失败依据。



## C12-PRE-01 BDD / RED / GREEN



- Given：新 context 真实登录并打开 /dcc/controlled-file/directories。

- When：首次点击可见“新建目录”。

- Then：应出现含目录编码/名称的表单。

- RED：prepare-category-real-ui.cjs 退出1；pageerror“文档目录表单组件加载失败”、可见弹框0、目录创建 POST 0。trace/截图见 category-preparation.json。

- Source：IntRuoyiFronted/src/views/dcc/controlled-file/directories/index.vue:403，openForm 异步组件 ref 就绪检查；UTF-8/LF SHA256 d39e58368fc546d8fd8132a0b5032c56f65a5ea2c66b99e5ffa887bce2efa09e。

- GREEN：本轮获得窄范围生产授权后已完成，见i04-fix-report.md。真实新context首次点击1次可打开，取消后重开清空，目录写请求0；未使用第二次点击、接口建目录或组件方法注入规避失败。



## 设计约束检查



- 本轮最新授权允许修改 IntRuoyiFronted/src/views/dcc/controlled-file/directories/index.vue 及必要专属回归；其余生产范围仍不改。不建线程/子Agent，不改共享配置/数据库，不提交推送，不迁移，不重启/停止服务，不做全量构建。schema由Root处理。

- 所有账号、角色授权、项目、模板、分类的写入均来自 Playwright 真实页面；自然请求只记 method/path/status，不解析 JSON 作为通过依据。

- admin仅用于配置；项目审核由L1本人完成，实际部门指派由L1/L2独立context完成，实际会签由C1/C2完成，未使用管理员冒充。

- 凭据仅来自授权来源/运行时环境，首次改密仅针对本例新建账号。口令、token、cookie不写证据；登录/改密段无 trace。

- trace 登录后开始，签名段停止，密码清空/移除后重启；关闭网络/DOM快照，trace失败不能完整PASS，证据最后落盘。

- 负责人和实际会签人按文件/轮次/部门/任务/用户精确区分。缺第2尾部/第6、8规则不补造。



## 清理与残留



浏览器及context已关闭，r3 runner记录CLOSED且trace无错误。按Root最新要求不清理资产，保留四业务账号、配置账号、两部门、岗位/角色、项目/产品/模板、目录/类别/历史分类及三份旧申请：POS181待批准；R1181/R2181已驳回。r3新建PUBLIC_MATRIX类别908772，已关闭分发/培训并保存D1全部同意会签和L1批准矩阵；四个新账号个人测试签名启用。r3未取得上传权限/项目可见性最终核验，也未提交PUBLIC申请或实际会签；不得将其记为完整GREEN。原签名与审计历史保留。没有Git提交推送；早期cleanup-account-evidence.json不是本次清理证明。



## E2E-I04 修复 BDD（2026-10-16 当轮授权）



- Given：int_qms真实页面；全新浏览器context通过授权登录；DirectoryForm保持按需加载且尚未打开。

- When：首次且仅点击一次“新建目录”。

- Then：等待真实组件挂载后显示“新建目录”，目录编码/名称可填写，无“文档目录表单组件加载失败”，不需要第二次点击。

- Given：首次弹框已填入本例前缀测试值但未提交。

- When：点击取消，随后再次打开。

- Then：表单重新初始化，编码/名称为空；没有目录业务写请求；已挂载组件可继续正常打开。

- 验证：新增专属真实Playwright回归，red/green分别留结果、trace、截图；禁止API动作、mock、组件方法注入；保留既有懒加载性能、目录加载和工具栏合同。只打开/填写/取消，不保存目录，不改schema。





## 2026-10-20 PUBLIC四项附加边界最终交接

本轮唯一交接：public-boundaries-20261020-2111/handoff.md/.json；原始session位于 runs/20261019-r3/public-boundaries-20261020-2111/。该段是最新补证，优先于前面的PREPARE/HOLD历史记录；不覆盖历史NOT_RUN/ERROR或原四轮结果。

- Given：Root明确放行既有账号/矩阵/项目的四项PUBLIC边界；L1/L2真实待办step5/10无可复用自有L待指派任务，因此只新建一份PUBLIC，旧自有PDF逐字节复制，业务编号及basename均唯一。
- When：空选提交step63、选有效人员但空密码提交70；C1开签名框取消102、填意见但空密码确认109。必要最终上传42、主管通过53、D1有效指派85各一次；step93已出现C1正式待办，D2有效指派0，不做多余动作。
- Then：65/72/111分别显示原具体提示；66/73/105/112同fileId/BPM不推进，119刷新真实待办后122重新打开仍为0/2，只有主管#1265与D1指派#1266，没有C正式签名、驳回或独立批准。
- 目标：file2054545668044084385，BPM29de9ee5-cc88-11f1-ada5-b082e25ec548，编号DCC241020C12PB211134，A/1；新增4PASS，不将必要前置另计PASS。
- 里程碑：[x] 运行指纹与自有复用决定；[x] 最小前置及四项真实UI；[x] 122DONE/0UI ERROR、exit0；[x] 351条自然metadata与负例零正式业务请求；[x] 3context/浏览器及10捕获PID闭合、runner0；[x] 5trace/1337条目CRC、隐私0、26检查点截图；[x] 唯一交接/CRC/SHA清单/文档结构核对。
- 验证边界：本轮收尾只读既有证据，无新浏览器/业务或产品测试。seal-evidence.py为离线证据结构校验，PASS；截图不是逐动作截图或PDF视觉验收。当前4项为补充PASS，Root已独立接受ACCEPTED_PUBLIC_FOUR_BOUNDARIES；原31项重提/33步阶段修复及五项已有VM宿主失败保持原范围，不重跑。
- 错误：cleanRun=false，offline-issue-001为step73后GBK/UTF8离线队列读取失败、step74只读确认后仅消费未执行动作；offline-issue-002为关闭后的封存脚本误要求队列短断言等于整句，修正后仍独立检查完整DOM。均保留、无业务重放。Root误读不存在plan.json另属只读命令路径错误，不是产品缺陷。
- 设计约束检查：本轮生产/API/DB/构建/服务/Git写入/清理/子Agent/跨线程消息均无；不测错密码、不执行C通过/驳回/批准/后续。Root本次接受前封存核对时，原协调ledger及三份封存handoff共四份SHA全部相等，原NOT_RUN与四轮18PASS/3BLOCKED/3FAIL不改。
- 关闭依据为实际session/driver及closure.json，不能用runtime-closeout.json模板继承的browserStarted=false/businessActions=0/旧runner字段冒充关闭或动作计数。该文件仅用Jar/监听/七源码指纹，原文件保留；实际本轮runner/launcher/driver在handoff列出。
- 状态曾进入ready_for_closeout并完成证据核对，最终blocked：本轮明确禁止Git提交/推送与清理，故不满足仓库completed门禁；不运行cleanup preview/apply、不删除资产、不请求额外权限。当前不再开浏览器，交接后停止。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/artifact-sha256.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/structure-verification.json


- doc/tasks/20261016-dcc-e2e-12/r3-round2-ui-20261020.cjs
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/business-evidence.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/closure-index.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/DCC241016C12R3R2CUSTOMPOS225652.pdf
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/DCC241016C12R3R2CUSTOMREJC1225652.pdf
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/DCC241016C12R3R2CUSTOMREJC2225652.pdf
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/DCC241016C12R3R2PUBLICPOS225652.pdf
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/DCC241016C12R3R2PUBLICREJC1225652.pdf
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/DCC241016C12R3R2PUBLICREJC2225652.pdf
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/drive.cjs
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/driver-state.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/first-session-closure.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/handoff.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/handoff.md
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/issue-001.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/issue-002.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/issue-003.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/manifest.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/offline-issue-001.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/owned-processes-live.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/plan.md
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/privacy.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/queue.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/ready-for-closeout.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/remaining-five-parameter-plan.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/runner-stderr.log
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/runner-stdout.log
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/runtime-closeout.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/runtime-preflight.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/seal.py
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/start.ps1
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/trace-integrity.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/verify-privacy.ps1
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/c-boundary-network-after.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/c-boundary-network-before.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/closure.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/drive.cjs
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/driver-state.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/l-boundary-network-after.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/l-boundary-network-before.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/owned-processes-live.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/queue.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/runner-stderr.log
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/runner-stdout.log
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation1/start.ps1
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation2/closure.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation2/drive.cjs
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation2/driver-state.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation2/owned-processes-live.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation2/plan.md
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation2/queue.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation2/runner-stderr.log
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation2/runner-stdout.log
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation2/start.ps1
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation3/closure.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation3/drive.cjs
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation3/driver-state.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation3/owned-processes-before-close.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation3/owned-processes-live.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation3/plan.md
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation3/queue.json
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation3/runner-stdout.log
- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2-continuation3/start.ps1
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2/002-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2/006-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2/017-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2/027-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2/032-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2/034-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2/037-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2/039-first-error.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2/leader1-context1-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2/session.json
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/006-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/009-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/018-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/023-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/027-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/028-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/034-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/035-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/040-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/045-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/050-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/061-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/067-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/073-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/074-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/078-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/090-signer2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/094-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/104-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/107-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/108-first-error.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/approver-context5-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/approver-context6-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/leader1-context2-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/leader1-context2-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/leader2-context1-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/leader2-context1-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/leader2-context1-3-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/session.json
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/signer1-context3-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/signer1-context3-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/signer2-context4-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation1/signer2-context4-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/012-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/022-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/027-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/029-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/032-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/039-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/042-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/049-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/054-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/059-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/064-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/069-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/074-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/085-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/088-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/101-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/106-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/118-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/128-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/133-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/135-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/138-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/145-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/148-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/155-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/160-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/165-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/170-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/175-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/180-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/191-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/203-signer2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/207-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/222-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/227-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/239-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/243-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/251-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/261-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/265-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/268-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/275-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/278-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/285-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/290-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/294-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/300-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/301-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/306-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/311-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/316-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/327-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/333-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/337-first-error.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/approver-context4-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader1-context1-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader1-context1-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader1-context5-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader1-context5-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader1-context9-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader1-context9-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader2-context10-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader2-context10-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader2-context10-3-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader2-context2-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader2-context2-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader2-context2-3-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader2-context6-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader2-context6-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/leader2-context6-3-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/session.json
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/signer1-context11-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/signer1-context3-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/signer1-context3-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/signer1-context7-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/signer1-context7-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/signer2-context8-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation2/signer2-context8-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/006-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/009-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/023-signer2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/027-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/037-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/040-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/042-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/054-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/058-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/066-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/076-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/080-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/083-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/090-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/093-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/100-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/105-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/110-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/115-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/120-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/125-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/136-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/139-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/152-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/157-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/169-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/173-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/181-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/191-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/195-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/198-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/205-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/208-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/215-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/220-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/225-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/230-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/235-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/240-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/251-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/263-signer2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/267-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/282-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/287-approver.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/approver-context3-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/approver-context4-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/leader1-context5-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/leader1-context5-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/leader1-context8-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/leader1-context8-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/leader2-context6-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/leader2-context6-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/leader2-context6-3-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/leader2-context9-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/leader2-context9-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/leader2-context9-3-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/session.json
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/signer1-context1-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/signer1-context1-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/signer1-context10-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/signer1-context10-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/signer1-context7-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/signer1-context7-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/signer2-context11-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/signer2-context11-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/signer2-context2-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r3-round2-continuation3/signer2-context2-2-trace.zip


- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/DCC241020C12PB211134.pdf
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/c-boundary-network-after.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/c-boundary-network-before.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/closure.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/document-verification.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/drive.cjs
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/driver-state.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/fixture-decision.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/handoff.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/handoff.md
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/identity.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/l-boundary-network-after.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/l-boundary-network-before.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/network-audit.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/offline-issue-001.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/offline-issue-002.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/owned-processes-before-close.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/owned-processes-live.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/plan.md
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/preflight.ps1
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/privacy.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/queue.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/runner-stdout.log
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/runtime-closeout.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/runtime-closeout.ps1
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/runtime-preflight.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/seal-evidence.py
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/sealed-history-check.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/second-assignment-decision.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/sha256-manifest.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/start.ps1
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/trace-crc.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/verification.json
- doc/tasks/20261016-dcc-e2e-12/public-boundaries-20261020-2111/verify-privacy.ps1
- doc/tasks/20261016-dcc-e2e-12/public-boundary-ui-20261020.cjs
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/005-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/010-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/021-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/031-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/036-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/038-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/041-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/046-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/049-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/056-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/061-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/065-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/066-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/072-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/073-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/074-leader2.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/079-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/088-leader1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/093-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/097-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/105-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/111-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/112-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/116-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/119-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/122-signer1.png
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/leader1-context1-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/leader1-context1-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/leader2-context2-1-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/leader2-context2-2-trace.zip
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/session.json
- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-boundaries-20261020-2111/signer1-context3-1-trace.zip




- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/document-verification.json



- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/closeout.json



- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/custom-fixture.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/drive.cjs

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/driver-state.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/issue-002.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/owned-processes-live.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/plan.md

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/preflight.ps1

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/queue.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/rework/DCC241020C12R2R2CUSTOMREJC1.pdf

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/rework/render.png

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/runner-stderr.log

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation1/runtime-preflight.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation2/drive.cjs

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation2/driver-state.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation2/owned-processes-before-close.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation2/owned-processes-live.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation2/plan.md

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation2/preflight.ps1

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation2/queue.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation2/runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740-continuation2/runtime-preflight.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/closure.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/custom-result.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/drive.cjs

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/driver-state.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/issue-001.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/owned-processes-live.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/plan.md

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/preflight.ps1

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/privacy.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/product-finding.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/public-result.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/queue.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/route-snapshot-product-red.cjs

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/runner-stderr.log

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/runtime-closeout.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/runtime-preflight.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/trace-crc.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/verification.json

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/verify-evidence.py

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/verify-privacy.ps1

- doc/tasks/20261016-dcc-e2e-12/resubmit-20261020-1740/verify-runtime-closeout.ps1

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/baseline-read.cjs

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/baseline-regression.log

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/closure.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/coordination-ledger-proof.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/coordination-ledger.before.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/drive.cjs

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/driver-state.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/finalize-evidence.py

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/green.log

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/handoff.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/handoff.md

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/index.before.diff

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/index.before.vue

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/own-fix.diff

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/owned-processes-before-close.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/owned-processes-live.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/plan.md

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/preflight.ps1

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/privacy.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/product-resolution.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/queue.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/readonly-queue-proof.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/red.log

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/regression-static.log

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/regression-targeted.log

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/regression-unit.log

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/runtime-closeout.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/runtime-closeout.ps1

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/runtime-preflight.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/source-after.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/source-before.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/test-results.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/trace-crc.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/ui-result.json

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/verify-privacy.ps1

- doc/tasks/20261016-dcc-e2e-12/route-snapshot-stage-fix-20261020-1800/workspace-status-before.txt

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/007-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/012-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/017-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/023-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/028-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/040-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/052-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/062-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/066-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/070-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/074-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/081-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/084-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/087-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/095-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/099-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/104-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/106-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/114-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/119-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/122-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/126-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/DCC241016C12R3A-context5-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/leader1-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/leader1-context2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/leader2-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/leader2-context1-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/signer1-context3-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/signer1-context3-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/signer2-context4-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation1/signer2-context4-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/004-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/008-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/015-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/022-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/027-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/032-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/038-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/043-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/055-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/067-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/077-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/081-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/085-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/089-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/093-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/DCC241016C12R3A-context5-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/leader1-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/leader1-context2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/leader2-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/leader2-context1-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/leader2-context1-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/signer1-context3-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/signer1-context3-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/signer2-context4-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740-continuation2/signer2-context4-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/005-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/008-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/016-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/020-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/022-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/026-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/029-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/037-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/leader2-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/leader2-context2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/resubmit-20261020-1740/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/route-snapshot-stage-fix-20261020-1800/006-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/route-snapshot-stage-fix-20261020-1800/007-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/route-snapshot-stage-fix-20261020-1800/008-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/route-snapshot-stage-fix-20261020-1800/012-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/route-snapshot-stage-fix-20261020-1800/016-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/route-snapshot-stage-fix-20261020-1800/020-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/route-snapshot-stage-fix-20261020-1800/031-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/route-snapshot-stage-fix-20261020-1800/033-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/route-snapshot-stage-fix-20261020-1800/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/route-snapshot-stage-fix-20261020-1800/session.json



- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/checkpoint-recovery.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/checkpoint-recovery.md

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/closure.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/drive.cjs

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/driver-state.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/fixture.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/handoff.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/handoff.md

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/original/DCC241020C12SAME1650.pdf

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/original/render.png

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/plan.md

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/preflight.ps1

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/privacy.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/queue-before-step21-fix.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/queue.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/rework/DCC241020C12SAME1650.pdf

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/rework/render.png

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/runner-stderr.log

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/runtime-closeout.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/runtime-preflight.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/script-preflight-diagnosis.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/trace-crc.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/verification.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1650/verify-privacy.ps1

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/004-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/008-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/012-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/016-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/020-originalOwner.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/leader2-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/originalOwner-context5-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/signer1-context3-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1650/signer2-context4-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/closure.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/drive.cjs

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/driver-state.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/fixture.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/handoff.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/handoff.md

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/owned-processes-before-close.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/owned-processes-live.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/plan.md

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/planned-public-commands.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/preflight.ps1

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/privacy.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/queue.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/runtime-closeout.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/runtime-preflight.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/trace-crc.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-1658-business/verify-privacy.ps1

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/012-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/022-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/027-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/029-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/032-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/038-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/041-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/048-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/053-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/058-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/063-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/068-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/073-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/084-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/087-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/100-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/103-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/106-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/109-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/112-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/120-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/123-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/126-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/128-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/136-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/139-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/141-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/leader1-context1-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/leader2-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/leader2-context2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/leader2-context2-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/signer1-context3-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/checkin-same-name-20261020-1658-business/signer1-context3-2-trace.zip







- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-preflight/handoff.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-preflight/handoff.md

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-preflight/predecessor-check.json

- doc/tasks/20261016-dcc-e2e-12/checkin-same-name-20261020-preflight/closure.json





- doc/tasks/20261016-dcc-e2e-12/checkin-diagnosis-20261020.md

- doc/tasks/20261016-dcc-e2e-12/checkin-diagnosis-20261020.json



- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/verify-closeout.cjs



- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/hold.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal1/012-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal1/022-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal1/025-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal1/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal1/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal2/012-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal2/022-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal2/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal2/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/012-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/022-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/027-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/029-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/032-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/038-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/041-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/048-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/053-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/058-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/063-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/068-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/073-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/084-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/096-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/099-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/109-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/112-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/114-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/126-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/136-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/141-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/143-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/146-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/152-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/155-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/162-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/167-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/172-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/177-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/182-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/187-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/198-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/201-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/214-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/219-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/231-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/241-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/246-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/248-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/251-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/257-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/260-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/267-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/272-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/277-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/282-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/287-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/292-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/303-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/315-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/319-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/334-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/339-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/351-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/355-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/363-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/373-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/377-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/380-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/386-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/389-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/396-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/401-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/405-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/411-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/412-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/417-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/422-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/427-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/438-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/444-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/449-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/459-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/463-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/473-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/476-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/DCC241016C12R3A-context18-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/DCC241016C12R3A-context5-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/DCC241016C12R3A-context6-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader1-context1-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader1-context10-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader1-context10-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader1-context14-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader1-context14-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader1-context7-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader1-context7-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context11-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context11-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context11-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context15-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context15-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context15-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context2-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context8-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context8-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/leader2-context8-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer1-context12-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer1-context12-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer1-context16-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer1-context16-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer1-context3-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer1-context3-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer1-context9-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer1-context9-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer2-context13-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer2-context13-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer2-context17-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer2-context17-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer2-context4-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-cal3/signer2-context4-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/012-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/016-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/024-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/034-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/038-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/041-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/047-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/050-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/057-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/062-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/067-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/072-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/077-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/082-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/093-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/096-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/109-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/114-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/126-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/130-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/138-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/148-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/152-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/155-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/161-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/164-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/171-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/176-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/181-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/186-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/191-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/196-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/207-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/219-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/223-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/238-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/243-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/251-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/254-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/257-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/260-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/263-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/DCC241016C12R3A-context4-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/leader1-context1-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/leader1-context5-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/leader1-context5-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/leader2-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/leader2-context2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/leader2-context2-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/leader2-context6-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/leader2-context6-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/leader2-context6-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/signer1-context3-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/signer1-context3-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/signer1-context7-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/signer1-context7-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/signer2-context8-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation1/signer2-context8-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation2/004-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation2/007-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation2/013-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation2/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-continuation2/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-inspection/004-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-inspection/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-inspection/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-terminal-inspection/004-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-terminal-inspection/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-20261020-r2-round2-terminal-inspection/session.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/cleanup-preview.txt

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/structure-verification.json



- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/add-boundaries.cjs

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/business-results.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/cal1-driver-state.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/cal1-runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/cal2-driver-state.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/cal2-runner-stderr.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/cal2-runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/cal3-driver-state.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/cal3-queue.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/cal3-runner-stderr.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/cal3-runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/calibration.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-1.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-10.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-11.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-12.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-13.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-14.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-15.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-16.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-17.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-18.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-19.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-2.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-20.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-21.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-22.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-23.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-24.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-25.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-26.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-27.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-28.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-29.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-3.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-30.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-31.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-32.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-33.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-34.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-35.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-4.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-5.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-6.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-7.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-8.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-after-click-9.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/checkin-last-frame.jpeg

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/closure.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/continuation1-driver-state.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/continuation1-queue.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/continuation1-runner-stderr.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/continuation1-runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/continuation2-driver-state.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/continuation2-queue.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/continuation2-runner-stderr.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/continuation2-runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/crc.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/DCC241020C12R2R2CUSTOMPOS.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/DCC241020C12R2R2CUSTOMREJC1.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/DCC241020C12R2R2CUSTOMREJC1REWORK.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/DCC241020C12R2R2CUSTOMREJC2.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/DCC241020C12R2R2PUBLICPOS.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/DCC241020C12R2R2PUBLICREJC1.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/DCC241020C12R2R2PUBLICREJC1REWORK.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/DCC241020C12R2R2PUBLICREJC2.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/drive.cjs

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/driver-state.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/execute-runtime-ready.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/finalize-evidence.cjs

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/first-driver-state.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/first-runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/handoff.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/inspection-driver-state.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/inspection-owned-processes.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/inspection-queue.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/inspection-runner-stderr.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/inspection-runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/manifest.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/owned-processes.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/planned-commands.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/prepare-commands.cjs

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/privacy.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/process-closure.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/queue.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/runner-stderr.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/runner-stdout.log

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/verify-business.cjs

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r2-round2/verify-privacy.ps1

- doc/tasks/20261016-dcc-e2e-12/prepare-round2-20261020.ps1



- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/closure.json

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/DCC241020C12FIXCUSTOM-POS-145013.pdf

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/DCC241020C12FIXCUSTOM-REJC1-145013.pdf

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/DCC241020C12FIXCUSTOM-REJC2-145013.pdf

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/gate-before-local-script-stop.json

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/handoff.md

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/owned-processes-before-close.json

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/privacy.json

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/result.json

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/trace-crc.json

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/upload-pos-commands.json

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/verify-privacy.ps1

- doc/tasks/20261016-dcc-e2e-12/repair-custom-20261020-145013/verify-repair.cjs

- doc/tasks/20261016-dcc-e2e-12/repair-custom-ui-failfast-20261020.cjs

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145012/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/002-config.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/004-config.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/007-config.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/009-config.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/010-config.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/017-config.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/020-config.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/032-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/036-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/048-config.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/050-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/054-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/056-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/059-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/065-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/068-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/075-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/080-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/085-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/090-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/095-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/100-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/111-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/123-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/133-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/136-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/147-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/151-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/165-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/config-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/DCC241016C12R3A-context6-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/leader1-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/leader1-context2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/leader2-context3-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/leader2-context3-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/leader2-context3-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/signer1-context4-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/signer1-context4-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/signer2-context5-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-145013/signer2-context5-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/012-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/016-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/024-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/034-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/037-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/043-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/046-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/053-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/058-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/063-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/068-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/073-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/078-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/089-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/092-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/105-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/109-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/DCC241016C12R3A-context4-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/leader1-context1-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/leader2-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/leader2-context2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/leader2-context2-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/signer1-context3-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/repair-custom-20261020-rejects/signer1-context3-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/start-custom-ui-failfast-20261020.ps1



- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/closure.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/DCC241020C12R2-CUSTOM-POS-100612.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/DCC241020C12R2-POS-100612.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/DCC241020C12R2-REJC1-100612.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/DCC241020C12R2-REJC2-100612.pdf

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/handoff.md

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/owned-processes-before-close.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/privacy.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/result.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/trace-crc.json

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/verify-and-summarize.cjs

- doc/tasks/20261016-dcc-e2e-12/rerun-20261020-round2-100612/verify-privacy.ps1

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/009-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/013-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/021-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/024-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/030-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/034-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/037-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/038-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/040-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/044-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/048-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/049-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/054-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/059-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/064-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/069-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/074-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/079-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/090-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/102-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/106-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/115-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/118-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/129-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/138-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/140-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/143-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/150-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/161-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/166-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/175-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/180-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/189-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/197-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/200-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/203-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/206-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/207-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/208-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/210-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/216-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/228-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/237-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/247-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/250-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/251-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/253-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/255-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/258-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/265-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/276-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/281-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/290-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/295-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/304-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/315-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/317-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/326-DCC241016C12R3L1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/328-DCC241016C12R3L1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/331-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/334-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/345-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/351-DCC241016C12R3L1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/355-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/369-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/371-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/374-DCC241016C12R3L1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/377-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/381-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/DCC241016C12R3A-context5-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/DCC241016C12R3L1-context6-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader1-context1-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader1-context1-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader1-context1-4-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader2-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader2-context2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader2-context2-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader2-context2-4-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader2-context2-5-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader2-context2-6-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/leader2-context2-7-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/signer1-context3-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/signer1-context3-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/signer1-context3-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/signer1-context3-4-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/signer2-context4-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/signer2-context4-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/rerun-r2-20261020-100612/signer2-context4-3-trace.zip



- doc/tasks/20261016-dcc-e2e-12/repair-ui-session-20261019-r3.cjs

- doc/tasks/20261016-dcc-e2e-12/start-repair-ui-20261019-r3.ps1

- doc/tasks/20261016-dcc-e2e-12/public-business-results-r6.json

- doc/tasks/20261016-dcc-e2e-12/repair-20261020-r9-handoff.json

- doc/tasks/20261016-dcc-e2e-12/repair-20261019-r8-handoff.json

- doc/tasks/20261016-dcc-e2e-12/repair-20261019-r7-handoff.json

- doc/tasks/20261016-dcc-e2e-12/repair-20261019-r5-handoff.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/012-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/020-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/024-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/026-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/027-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/028-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/029-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/032-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/036-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/039-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/041-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/047-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/050-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/057-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/062-leader2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/067-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/072-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/077-leader1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/083-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/086-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/092-signer1.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/096-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/100-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/104-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/106-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/108-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/114-signer2.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/117-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/120-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/122-DCC241016C12R3A.png

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/DCC241016C12R3A-context5-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/leader1-context1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/leader1-context1-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/leader2-context2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/leader2-context2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/leader2-context2-3-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/privacy-and-structure-verification.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/signer1-context3-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/signer1-context3-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/signer2-context4-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261019-r3/public-r9-02/signer2-context4-2-trace.zip



- doc/tasks/20261016-dcc-e2e-12/ui-session-20261018.cjs

- doc/tasks/20261016-dcc-e2e-12/page-execution-control.cjs

- doc/tasks/20261016-dcc-e2e-12/page-execution-control.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/business-results.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/checkpoint-final.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/runtime-after-hmr.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/privacy-checks.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/historical-results-before-business.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/historical-prepared-before-business.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/historical-fixture-manifest-before-business.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/live-03/session.json

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/live-03/leader1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/live-03/leader1-4-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/live-03/leader2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/live-03/leader2-2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/live-03/signer1-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/live-03/signer2-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/runs/20261018/live-03/signer2-2-trace.zip



- doc/tasks/20261016-dcc-e2e-12/page-execution-control.json

- doc/tasks/20261016-dcc-e2e-12/page-execution-control.cjs

- doc/tasks/20261016-dcc-e2e-12/maintainer-pause-check.json

- doc/tasks/20261016-dcc-e2e-12/prepare-maintainer-real-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/maintainer-fixture.json

- doc/tasks/20261016-dcc-e2e-12/artifacts/maintainer/admin-1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/artifacts/maintainer/stop.png

- doc/tasks/20261016-dcc-e2e-12/i04-fix-report.md

- doc/tasks/20261016-dcc-e2e-12/i04-fix-result.json

- doc/tasks/20261016-dcc-e2e-12/i04-privacy.json

- doc/tasks/20261016-dcc-e2e-12/artifacts/i04-red/result.json

- doc/tasks/20261016-dcc-e2e-12/artifacts/i04-red/trace.zip

- doc/tasks/20261016-dcc-e2e-12/artifacts/i04-red/failure.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/i04-red/first-open.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/i04-red/reopen.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/i04-green/result.json

- doc/tasks/20261016-dcc-e2e-12/artifacts/i04-green/trace.zip

- doc/tasks/20261016-dcc-e2e-12/artifacts/i04-green/first-open.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/i04-green/reopen.png

- doc/tasks/20261016-dcc-e2e-12/privacy-checks.json

- doc/tasks/20261016-dcc-e2e-12/account-credential-phase.json

- doc/tasks/20261016-dcc-e2e-12/account-setup-evidence.json

- doc/tasks/20261016-dcc-e2e-12/account-verification.json

- doc/tasks/20261016-dcc-e2e-12/admin-route-discovery.cjs

- doc/tasks/20261016-dcc-e2e-12/admin-route-discovery.json

- doc/tasks/20261016-dcc-e2e-12/admin-ui-discovery.json

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/DCC241016C12C1-login-blocked.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/DCC241016C12C2-login-blocked.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/DCC241016C12L1-login-blocked.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/DCC241016C12L2-login-blocked.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/admin-own-account-roster.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/leader1-profile.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/leader1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/leader1-verification-stop.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/leader2-profile.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/leader2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/leader2-verification-stop.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/signer1-profile.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/signer1-trace.zip

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/signer1-verification-stop.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/signer2-profile.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/signer2-trace.zip

- doc/tasks/20261016-dcc-e2e-12/artifacts/account-verification/signer2-verification-stop.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/business-fixture/business-fixture-trace.zip

- doc/tasks/20261016-dcc-e2e-12/artifacts/business-fixture/own-category-form.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/business-fixture/own-type-creation-controls.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/business-fixture/preparation-stop.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/business-fixture/taxonomy-candidates.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/category-preparation/category-trace.zip

- doc/tasks/20261016-dcc-e2e-12/artifacts/category-preparation/preparation-stop.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/project-preparation/project-confirmation.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/project-preparation/project-preparation-stop.png

- doc/tasks/20261016-dcc-e2e-12/artifacts/project-preparation/project-preparation-trace.zip

- doc/tasks/20261016-dcc-e2e-12/artifacts/project-preparation/project-reviewer-trace.zip

- doc/tasks/20261016-dcc-e2e-12/assign-own-project-reviewer-real-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/business-fixture-evidence.json

- doc/tasks/20261016-dcc-e2e-12/category-preparation.json

- doc/tasks/20261016-dcc-e2e-12/cleanup-account-evidence.json

- doc/tasks/20261016-dcc-e2e-12/cleanup-accounts-real-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/configure-own-role-real-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/debug-dept-dialog.html

- doc/tasks/20261016-dcc-e2e-12/debug-dropdown-芋道源码.html

- doc/tasks/20261016-dcc-e2e-12/debug-user-dialog.html

- doc/tasks/20261016-dcc-e2e-12/dept-ui-inspection.json

- doc/tasks/20261016-dcc-e2e-12/discover-admin-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/e2e-12-department-signoff-real.e2e.cjs

- doc/tasks/20261016-dcc-e2e-12/execution-log.md

- doc/tasks/20261016-dcc-e2e-12/fixture-manifest.json

- doc/tasks/20261016-dcc-e2e-12/gate-result.json

- doc/tasks/20261016-dcc-e2e-12/inspect-dept-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/own-project-reviewer-binding.json

- doc/tasks/20261016-dcc-e2e-12/prepare-accounts-real-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/prepare-business-fixture-real-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/prepare-category-real-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/prepare-project-real-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/prepared.json

- doc/tasks/20261016-dcc-e2e-12/project-preparation.json

- doc/tasks/20261016-dcc-e2e-12/project-route-discovery.json

- doc/tasks/20261016-dcc-e2e-12/results.json

- doc/tasks/20261016-dcc-e2e-12/role-menu-verification.json

- doc/tasks/20261016-dcc-e2e-12/static-checks.json

- doc/tasks/20261016-dcc-e2e-12/task.md

- doc/tasks/20261016-dcc-e2e-12/verification-report.md

- doc/tasks/20261016-dcc-e2e-12/verify-accounts-real-ui.cjs

- doc/tasks/20261016-dcc-e2e-12/verify-preparation.cjs



## R8 原 OWNER 恢复与 PUBLIC 续验



- Given：fixture-manifest 原 OWNER DCC241016C12L1/910364，旧启动器仅环境注入，当前环境及任务可追溯私有路径无原凭据。

- When：真实 admin 用户页核对唯一账号 ID/任务标签；仅重置该账号登录密码，外部私有 vault 保存并安全注入；由原 OWNER 登录正式项目权限页追加必要 R3L1 EDIT，保留原规则。

- Then：不改签名密码/图片、组织角色，不猜凭据；若项目身份或权限仍无效，记录可见事实；恢复成功后完成 PUBLIC 默认D1/增补D2/负责人指派/部分阻断/全部推进。

- 测试工具 RED：现 runner 无独立 originalOwner 身份/凭据映射；不得沿用 R3 leader1 造成身份混淆。GREEN 需静态身份断言与真实本人登录证明；不涉及产品源码变更。



- R8 里程碑：安全来源核查、唯一账号核对、原OWNER本人登录、项目权限最小恢复、PUBLIC来源及部门默认值均完成；完整会签链因签名前置阻塞。

- R8 工具GREEN：Node语法/PowerShell解析及原OWNER独立身份静态断言通过；真实UI登录step24证明原账号，非R3别名。凭据外部DPAPI vault；1087 trace entries扫描无私有凭据原值/URL编码匹配。



- R8续验范围校正：R4允许仅修改本任务新配置；不改共享角色/公共矩阵，不改历史证明/签名。原OWNER恢复禁止改签名/组织，不扩展为禁止补新建R3账号。R3A补本任务岗位及最小批准权限、本人测试签名，R3五业务账号正式审计启用签名授权；默认矩阵不改。



## R8-02 最新检查点（2026-10-20，替代上方R8中间阻塞）



- 状态：in_progress；READY_FOR_ROOT_RESTART_BEFORE_PUBLIC_SUBMIT。按最新Root派发，最小前置已完成；提交及长业务链前回报安全暂停，未把前置当整例PASS。

- 真实页面补齐：step27五业务账号R3L1/L2/C1/C2/A电子签名授权已启用，Cfg仍未授权；step42 R3A岗位11；step61新建本任务最小批准角色991354，仅引用现有dcc:controlled-file:approve叶节点，不修改菜单定义或共享角色；step106保留R3Review991353并追加R8Approve991354。

- R3A本人真实首次登录改密（不录trace），本人上传并启用任务测试签名图片；step96已启用/版本1。原OWNER910364签名、组织、角色及历史未动。

- step118首次选择PUBLIC类型自动带出D1，未手选D1；119—121仅增补D2；126/127回读会签D1+D2，下发仍仅D1，独立批准人R3A/910521，审批路线已就绪共4节点；step128创建按钮enabled=true。

- 尚未点击创建/确认提交，未保存申请草稿、未创建PUBLIC业务文件或流程；负责人指派、无自动人选、部分完成阻断、全部通过推进均NOT_RUN。未发现当前新产品缺陷，旧7项夹具阻塞已解除。CUSTOM既有PASS保留不重跑。

- public-r8-02已CLOSED，128步，退出0；3份trace落盘，traceError/closeError=0。原始10条定位探索错误保留，不能称干净整例PASS。登录/改密段有意不录，截图viewport边界保留。

- 已检查118/127截图，结构断言与私有凭据内存扫描PASS，详见runs/20261019-r3/public-r8-02/checkpoint-verification.json。正式自然请求仅记method/path/status，无直接API/DB或响应JSON oracle。

- 当前可以安全暂停供Root重启：浏览器已关闭，无在途业务提交。恢复后须先取得新运行证明，重新真实登录并装填上传表单，复核自动D1/只增D2后提交一次，再走主管审核、负责人指派、C1部分门禁、C2全部推进；不能复用本次4节点前置作新包业务PASS。

- 最新交接：repair-20261019-r8-handoff.json；历史public-r8-01七缺口及旧runner保留previousIntermediate。未执行Maven、生产代码修改、服务操作、Git提交推送、跨线程消息或cleanup apply。



## R9 / R6 运行包最终 PUBLIC 业务证据（2026-10-20）



- 当前业务结果 PASS_SCOPED_REAL_UI_AWAITING_ROOT_REVIEW；索引 public-business-results-r6.json / repair-20261020-r9-handoff.json。替代R8提交前暂停点及更早夹具阻塞，CUSTOM原三单九项与证据边界保留不重跑。

- 运行来源：r6-runtime-proof.json，backend22076/48061，frontend20976/8061；Jar backend-workflow-v6-20261020-000513.jar，SHA256 478fff77d0b6d839c0a3466557df5232b9b12a60c35f00908c164cfaf949f24a。证明首次读取时尚未落盘，随后实际读到正式证明再运行，无绕过。

- 初次public-r9-01未开PTY，runner正常EOF关闭且0业务步骤；有效运行独立目录public-r9-02，不覆盖旧证据。

- step12 PUBLIC类型自动D1，13—15仅增D2；24保存的矩阵914030/v2及主管来源正式确认，29最终冻结确认显示会签D1+D2、下发仅D1、独立批准人R3A。下发验证仅名单独立快照，不声称实际下发。

- step30仅确认提交一次；自然submit POST仅1次。实际文件2054545668044084265、编号DCC241016C12R8PUB1、A/1、BPM7ea171f4-cbd8-11f1-8ef0-b082e25ec548。36主管待办与39详情均真实可见。

- R3L2本人主管签名1004；D2负责人R3L2指派C2签名1005；D1负责人R3L1指派C1签名1006；两部门初始人选为空已分别step51/68断言，不自动选人。83历史回读两条实际指派及精确账号/部门/签名VALID，未admin代签。

- C1本人通过签名1007后：step99断言1/2，100/106详情仍待会签审核，C2仍有实际会签任务；104独立批准人暂无审批任务。因此部分完成阻断PASS。

- C2本人通过签名1008后：117批准人出现本文件“批准并确认文件负责人/待批准”待办；122实际详情为批准阶段、同一BPM、五签名1004—1008均VALID，123—126断言通过。最终批准task7357c688-cbd9-11f1-8ef0-b082e25ec548。只推进批准，不执行批准/文控/发布等后续。

- runner已CLOSED/退出0，126步、10份trace、traceError/closeError=0。原始定位错误31/38/56保留：旧页面标题、旧stage-grid、将实际inline签名当modal；各自以32/39、58—62/83成功动作及页面证据纠正，不能说零错误干净全程PASS。

- trace与session私有凭据内存扫描原值/URL编码匹配0；5个授权凭据源不输出，详见runs/20261019-r3/public-r9-02/privacy-and-structure-verification.json。截图为viewport边界，DOM是业务主依据；登录及签名密码段有意不录trace，不声称每一步截图。

- 未新增配置或产品缺陷；复用既有R3夹具。原OWNER签名/组织/历史不动；生产/共享配置/数据库/Maven/服务/Git/跨线程消息未操作，API/DB/mock未承担业务动作、未读取响应JSON作oracle。

- 实现验证状态先ready_for_closeout；Git提交推送仍未授权，按task-closeout规则最终工程blocked，业务PASS分列；保留全部现有资产与证据，禁止cleanup apply删除审查材料。



## 2026-10-20 四轮后CUSTOM前置修复



- Given：类别908758/DCC241016C12OTHER由原任务创建，原CUSTOM三单由原L1上传；本次实际上传人为R3L1/910516，第二轮上传明确无权限，原项目285已保留OWNER并追加R3L1 EDIT。

- When：先用正式配置页核对类别/类型/UPLOAD及原项目实际绑定，记录原值，必要时仅追加本例类别给R3L1的UPLOAD；R3L1 fresh login后建立全新CUSTOM正向/C1驳回/C2驳回三单。

- Then：本次保留D1增补D2、两负责人本人指派且初始无人员、部分会签阻断、全部通过推进、任一会签驳回阻断五项各有真实DOM证据。PUBLIC五项不重跑。

- RED：原第二轮steps369/370/371为实际上传权限拒绝；新修复前先核对正式权限原值。GREEN必须为同账号fresh登录真实上传及后续五业务项；不把配置保存当业务通过。

- 里程碑：规则/来源核对；原值与最小配置修复；fresh CUSTOM五项；证据/隐私/浏览器关闭与新handoff。

- 边界：仅原任务资产；禁止Git写入、API/DB业务、子Agent、全局角色、全部类别授权、admin代验收、Maven/package/重启；无产品源码改动时不新增无关单测。旧FAIL/BLOCKED不修改。

## EXECUTE rerun-20261020-r2-round2



- Given: Root explicitly authorized full fresh E2E-12 on int_qms, backend41508/48061 frontend20976/8061 and verified Jar/frontend hashes; existing own R3 identities and PUBLIC/CUSTOM fixtures.

- When: Fresh independent PUBLIC/CUSTOM positive, C1 rejection and C2 rejection applications are created by real UI; department heads sign assignments; real signers perform actions; rejected own documents follow checkout/new-body checkin/resubmission UI.

- Then: Each mode retains D1 while adding D2; initial people are empty; both heads assign their own department; partial signoff blocks approval; all passes advance; either rejection blocks; rejection/resubmission preserves original frozen configuration and traceability. No historical PASS counts as fresh evidence.

- Milestones: runtime gate; fresh six branches; rejection/resubmission; evidence/privacy/closure; handoff HOLD. No product changes, build/Maven, service restart, Git mutation, DB/API business actions, subagents or third round. First failure retained; calibration separately recorded and bounded.



## EXECUTE rerun-20261020-r2-round2 最终封存



整体 BLOCKED / HOLD。六份新申请的主合同 PASS（52 条页面断言）；PUBLIC、CUSTOM 正向均到批准，四份独立驳回均为已驳回且不在批准待办。CUSTOM 四项缺人选/缺指派密码/取消签名/缺签名密码边界 PASS；PUBLIC 同类四项未在本轮执行，不能沿用历史 PASS。



PUBLIC 重提停在唯一一次检入：2026-10-20 13:48:33.505+08:00，L1 的修改说明有值、新正文已上传，正式页面却提示“检入请求缺少真实文件修改说明”。fileId 2054545668044084346，masterId 2054545668044079270，BPM 880311ed-cc48-11f1-ada5-b082e25ec548。HTTP200不作业务成功依据。只读复核旧 A/1 仍已驳回且仍检出；版本历史仅一条、正文哈希前缀48f52e7b6801、无后继版本。未重复检入、签名或提交。PUBLIC 重提 BLOCKED，CUSTOM 重提未执行/BLOCKED；矩阵冻结、重新会签及新旧证据隔离均未验收。



最早失败是离线生成器错误，最早UI失败是双确认框 strict locator；各会话首错保留于 calibration.json。检入会话15动作、14完成、1失败；两次后续只读核验均未发业务写请求，最后精确详情读取成功后正常关闭。8会话共823动作、817完成、6原始ERROR；失败会话exit1不改写。59 trace CRC PASS，9397解压条目+session私有凭据原值/URI编码匹配0；所有context/browser关闭，进程回执自有runner剩余0、20个已捕获进程全部消失。共享后端41508/48061、前端20976/8061保留。



逐模式/分支、首次失败、真实终态、证据及关闭回执见 rerun-20261020-r2-round2/handoff.json。完整证据保留，不改产品/配置、不构建、不重启、不Git、不DB/API动作、不子Agent、不清理任务资产、不启动第三轮。工程状态因本轮禁止Git为blocked；不标记completed。



## 2026-10-20 检入阻塞只读诊断



Given：原驳回文件及首次单次检入失败封存；When：只读前端提交链、后端校验及原trace；Then：确认字段/源身份约束，提出同名改正文夹具的最小TDD，不再操作原文件。



里程碑：只读字段链/trace核对完成；BDD及最小TDD提案完成；修复尚未实施。已证实本线程替换件错误追加REWORK后缀，违反现有同名源身份约束；该通用错误也用于文件名不一致，不能认定说明丢失。trace.network为0字节，未唯一确认运行时抛错行。详细事实、错误文案校正（含“或”字）、范围与指纹见checkin-diagnosis-20261020.md/.json。原R2 BLOCKED/CUSTOM重提NOT_RUN不变，HOLD；无新UI、共享源码修改、API/DB、构建、runtime、Git、清理或子Agent。离线证据及文档结构断言PASS；未执行修复RED/GREEN，不声称产品或E2E GREEN。



## 2026-10-20 新PUBLIC同名检入专项预检



- Given：Root接受原失败为异名夹具问题，授权仅新建一个本例专属PUBLIC对象自然驳回后，同名改正文检入一次；原失败对象保持不动。

- When：先核对前序关闭回执、int_qms、运行证明和当前前端来源。

- Then：只有来源一致且项目夹具真实页面可用才进入业务；不一致立即停止，保留阻塞，不借旧证明执行。



里程碑：前序关闭回执已读取；Jar哈希及进程身份已核对；上传页与最新Root证明不一致，页面/项目预检及业务NOT_RUN。



设计约束：只真实Playwright页面；不构造/查看API请求体，不API/DB代办；不改原失败文件、不重跑通过链、不构建/重启/Git/清理/子Agent。



上传页预期SHA256为9649a3c6561419a92fc9eceeba48ed91461c9230ee61d649f0490ffd6de14aa6，当前为877479ffcfbe4f9266094d0305f9ae9e0df2458c107729c3a79d3974eb769530。其余五份前端源码一致；最新Root来源完整性证明15:30仍记录旧源码全部一致。未发现更新的运行证明，不能自行改写共享证明或静默跳过来源门禁。



新对象未创建，浏览器/context/runner均未启动，业务动作0。原fileId2054545668044084346保持不动。此结果为环境来源预检BLOCKED，不是同名检入业务失败。Root核对上传页新修改并更新放行证据后，才从新对象开始；不是重试原失败对象。



逐项记录见 checkin-same-name-20261020-preflight/handoff.json。保持blocked/HOLD；尚未进行新业务，不能计入E2E通过或失败。





## 2026-10-20 16:50 PUBLIC 同名检入专项恢复



Given Root 当轮明确批准 upload/index.vue、detail/index.vue、detail/approval-actions.ts 三处指定指纹；When 重新严格只读 preflight 后仅新建一个 PUBLIC 对象自然驳回、同名改正文单次检入；Then 只按本轮真实页面后继版本/正文判定，有障碍即停，不碰原失败文件、不跑 CUSTOM。



计划、里程碑、验证和设计约束见 checkin-same-name-20261020-1650/plan.md。其余冻结来源不放宽；不 API/DB 代办、不请求体/响应 JSON oracle、不构建/重启/Git/清理/子 Agent。



## 2026-10-20 17:40 正式重提续作

Root明确继续4376准备/重提及保存矩阵、新旧签名隔离；随后仅已有CUSTOM驳回单。BDD/范围/里程碑见 resubmit-20261020-1740/plan.md。禁止重复PUBLIC检入、六分支回放及Git/清理。



## 2026-10-20 路线快照产品缺陷正式修复

Root明确纳入范围并授权最小前端修复及一次现有申请只读UI复验。新BDD/TDD/冻结基线见 route-snapshot-stage-fix-20261020-1800/plan.md。旧业务及首次失败证据原样保留。



## 2026-10-20 PUBLIC 四项附加边界只读映射



目标：按Root新指令只读原合同、局部fixture/runner及原始UI记录，逐项判断四边界覆盖；先核对已知对象最后节点，再提出最小续验方案。当前 PREPARE/HOLD，不开浏览器，不执行业务、生产修改、凭据解密、构建/重启、DB/API、Git或清理，不重跑原PASS，不改四轮历史或已封存handoff/ledger。



BDD：Given 原四轮明确PUBLIC四项NOT_RUN且CUSTOM四项PASS；When 比对原始动作、DOM及本例对象最后节点；Then 分开列明历史PUBLIC证据、本轮缺口与后续必需动作，不能把空初始选择、正常签名或CUSTOM结果当PUBLIC负向边界通过。



里程碑：原合同与四项名称核对；已有真实UI逐项映射；局部已知对象/L待办可复用性检查；最小必要计划及结构验证。预期验证仅离线文档结构/原始session断言，无产品RED/GREEN或新增E2E。设计约束及结论见 public-boundary-evidence-map-20261020.md/.json；业务续验须Root后续放行。


Root最终结论：ACCEPTED_PUBLIC_FOUR_BOUNDARIES；已独立核对四项DOM/111截图、5trace CRC/SHA/令牌0及真实关闭，并自行更新协调日志/report。本例不修改原封存历史；历史SHA为该核对时点。新增4PASS，cleanRun=false，两次离线SCRIPT错误保留；工程blocked，禁止Git/清理，交接后停止。

## R3 第二轮最终有限交接（2026-10-20）

业务 PASS：PUBLIC/CUSTOM 各 POS、REJC1、REJC2 六主参数；PUBLIC 四边界单列 PASS。两份正向对象停于独立批准待办，四份驳回对象均已驳回且无独立批准待办。各对象首次提交仅一次，负责人本人各签名指派一次，无自动具体会签人；部分 1/2 不进入批准，全部通过才进入批准。原始详情 URL、文件 ID/BPM、终态 DOM 及自然 method/path/status 计数见本轮 business-evidence.json 和 manifest.json。

771 页面步骤为 768 DONE / 3 ERROR，退出码 1/1/1/0，cleanRun=false。三次 UI SCRIPT 错误保留 issue-001/002/003.json，分别为提交后导航等待超时、重复标题定位冲突、重复空密码提示定位冲突；仅继续尚未发生的业务动作。另有 1 次离线 seal.py 预期主管账号误写断言，原始 metadata 显示主管为 leader2，修正后离线验证通过；offline-issue-001.json 保留，总 SCRIPT 错误 4，不隐去错误冒充干净运行。

57 trace、8043 条归档内容 CRC/SHA 核验通过，凭据匹配 0、原始 Bearer/JWT 匹配 0。四轮 session 上下文和浏览器均 await 关闭，采集 PID 清单均退出、自有 runner 0；不声称采集了所有瞬时 Chromium PID。2095 源码指纹及 Jar 指纹收尾复核一致，8061/48061 监听分别为 20976/41508。未调用 HTTP 健康探针、API/数据库业务动作或响应 JSON 判定，未改共享配置或生产代码。

状态按 in_progress → ready_for_closeout → blocked 记录，当前工程 blocked：当轮禁止 Git 写入及清理，未提交/推送、未 cleanup preview/apply，保留六业务对象和全部证据。没有执行独立批准、下游或后续轮次，历史未知事项不因本轮 PASS 而关闭。正式交接：doc/tasks/20261016-dcc-e2e-12/rerun-20261020-r3-round2/handoff.json。

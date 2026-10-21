# E2E-20 日常入口与正文权限

## Goal

独立验证E2E-20，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — 仅工程收尾：用户本轮禁止Git提交/推送，未执行cleanup apply。业务验收为PASS：原27参数27 PASS、0 FAIL、0 BLOCKED，Root于2026-10-19 05:36接受，10:20/10:40再次保留该结论。19 PASS/8 BLOCKED及P06待RED均为历史，已由真实检入工作稿2054545668044084251、原八项WORKING拒绝补验和Root测试证据替代。本轮只整理报告，不重跑E2E、不改生产/测试代码、不申请构建或运行态窗口。

## 2026-10-19 R2 文档修正范围与BDD

- Given：Root有效验收及parameter-results.json均为27 PASS，旧任务/报告仍写业务未执行或19 PASS/8 BLOCKED。
- When：按repair-dispatch-20261019-r2.md核对原证据、逐参数计数、工作稿身份及Root接受时间，并补充历史替代关系。
- Then：当前业务结论统一为PASS，工程状态独立为blocked；历史失败/阻塞和原运行包身份完整保留，不重放已通过动作、不把本次结构验证冒充新E2E。
- 本轮RED是文档不一致检查：当前状态存在19 PASS/8 BLOCKED、旧报告首段仍称未执行；GREEN仅为结构/证据引用复核。无生产行为修改，不新增或重跑Maven/Playwright测试。
- 业务权威证据：parameter-results.json、business-checkpoint.json的integratedOriginal27Final、artifacts/acceptance-resume/2026-10-18T20-20-07-652Z/original-27-permission-parameters-final.json；Root接受见协调目录verification-report.md的05:36、10:20和10:40检查点。
- 历史JSON不作当前恢复指令：pause-checkpoint.json、runtime-pause.json、runner-handle.json为19/8阶段，browser-closed.json为更早准备阶段，p06-sql-red-review.json为Root运行前的测试交接。后续业务结果以本节和上述最终JSON为准；实际最新会话落盘为checkpoint.json的STOPPED/run2026-10-18T20-20-07-652Z。本轮不据旧PID启动/关闭进程。

## 2026-10-19 R3 错误文件名临时上传证据核查

- 目标：只核查早期DCC241016C20-C.pdf上传是否已有可唯一定位的临时资产身份及精确清理证据；原27 PASS和R2报告结论不变。
- Given：原run2026-10-18T07-45-23-331Z中Manager向A1检入窗口上传了错误完整名C.pdf，随后会话退出，历史检查点标清理未确认。
- When：只读核对本例DOM、ui-actions、自然请求、trace及既有输入文件的身份；区分另一次已清理上传、后续改名成功检入及普通取消操作。
- Then：只有同一临时资产的精确清理证据才可关闭残留；缺身份/入口或回执时列具体缺口，不按同名或后续成功推断已清理。本轮不上传、检入、删除、触发job或重跑已通过E2E。
- 验证：文档结构、证据引用、可得身份及原27参数文件未变；此为离线证据核查，无生产改动，不新增RED/GREEN业务测试。无需Maven、全量类型检查、构建或运行态窗口。
- 状态：既有证据核查完成，清理结论UNCONFIRMED。已定位原上传场景及本地内容身份，但未找到远端临时件ID/会话映射或同次清理回执；详见repair-20261019-r3-handoff.json。工程仍blocked，不提交推送或执行cleanup apply。

## 2026-10-19 R4 新来源与证据边界

- Given：R3已定位错误名称C.pdf的历史上传场景，但远端临时件身份及同次清理回执未知。
- When：核对R4派发及最新R3交接是否给出新增正式来源；无新来源时不重复搜索历史trace、不新建上传或删除。
- Then：本次派发未提供新增远端ID、正式只读映射或清理回执，继续记UNKNOWN/UNCONFIRMED；不判当前存在，也不判已清理。后续同内容A.pdf成功检入及其它会话回执均不能补证。
- 本轮仅更新本例四份文档/JSON，结构与原参数文件哈希检查；原27 PASS保留，无新包验收、页面动作、XML、生产/测试代码改动或构建运行态需求。工程仍blocked，详见repair-20261019-r4-handoff.json。

### 历史检查点：2026-10-18 19 PASS/8 BLOCKED

以下原文由2026-10-19 05:36 Root接受的27参数最终证据替代，仅保留当时事实：

blocked — 同200656包264efbd4/PID14864恢复后完成原ACTIVE、待生效及真实撤权组合：19 PASS、8个WORKING参数因P06阻塞，0 FAIL。Reader VIEW及原备注已恢复并重新渲染A1正文；全部本例context/browser关闭、runner exit0、trace脱敏完成。A1仍ACTIVE，A2（2054545668044084245）仍待生效至2026-10-25 12:00:00，旧4241/4242已SUPERSEDED，不能冒充WORKING。Root已确认P06为检出SQL缺待生效状态；本例仅按唯一授权在既有DccWorkingSubmissionConditionTest新增真实MyBatis/私有H2行为RED，待Root独占Maven执行，未改Mapper/Query、未声称RED成立。

## BDD

- Given 同链存在已生效版、待生效版、草稿，账号有不同阅读权限
- When 从目录、搜索、详情入口及浏览器地址访问正文；已打开页面再通过真实页面撤销权限并操作
- Then 每个入口均校验阅读权限；日常入口不开放待生效和草稿正文；拒绝时不显示正文；已撤权后的重新读取/后续操作不继续取得正文，历史缓存不能冒充新的授权。

### 历史：P06 Root唯一测试授权（2026-10-18，后续由Root测试及工作稿4251证据替代）

- 仅允许在既有DccWorkingSubmissionConditionTest新增真实MyBatis/私有H2行为RED；原并发submission/checkout互斥测试保持原样。Mapper、Query、迁移、业务资格和Maven执行均不在本次授权内，Root独占执行RED。
- Given：真实VersionPolicy将CONTROLLED_PENDING_EFFECTIVE判为允许受控基线；私有H2中该版同租户、未删、未检出。
- When：通过真实DccControlledFileMapper执行checkoutByIdAndTenantWhenAvailable。
- Then：首次影响1行并读回本人、时间、原因；第二次影响0行且不覆盖首次锁。已锁、跨租户、删除和待审版本影响0行，锁保持不变。
- RED预期：当前生产Mapper状态IN缺CONTROLLED_PENDING_EFFECTIVE，正常待生效行影响0而断言期望1。必须由Root实际执行后再记RED成立，不mock Mapper返回1，不改既有并发用例。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置；沿原账号/项目建立同链ACTIVE、待生效及WORKING。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 原27项真实正文权限分支PASS，工程收尾单独blocked。
4. [x] Root已接受27参数结论；本轮只同步报告和历史替代关系。

## Upload Options GREEN Regression

- Given Root已恢复并核验int_qms新Jar，本例Manager及项目283/独立文件夹仍存在，原RED证据完整保留。
- When 本例Manager在独立context登录真实上传页，选择项目283及DCC241016C20Folder，打开文件类型下拉；不上传或提交文件。
- Then 文件类型错误不显示，真实DOM中候选标签非空且至少一个可选；页面自然GET upload-options业务码为0，HTTP200不能单独判PASS。
- 新脚本regress-upload-options-green.mjs使用独立且不覆盖旧RED的run目录；默认执行只输出PREPARED_WAITING_RUNTIME并exit20。实际执行还要求--run-after-root-restored、dispatch.runtimeVerified=true以及新Jar哈希不同于原RED运行包。
- GREEN已执行，runId=2026-10-16T11-43-48-641Z；新Jar SHA256=caafeb2e008f0980d5c499448628bace9f87a2a0c69344000135e2ba650427ff。结果PASS、可见70项/可选24项、业务码0、sourceUploads=0、controlledFileBusinessWrites=0，trace脱敏通过。
- 最新用户明确要求记录自然请求业务码，因此仅提取目标自然响应code，不读取data来代替DOM候选断言；不直接调用接口。
- 中断只影响exec退出码回传：恢复时session已不存在、Node/本例浏览器已关闭；PASS result/marker/截图/trace均已落盘。未为了重取退出码而重复登录或执行；详情见green-review-summary.json。

## Expected Verification

- 本轮仅做文档结构、27项计数/唯一性、证据路径/脱敏记录及Root接受引用核验；不重复已通过E2E，不执行Maven、全量类型检查或切包。原业务验证要求如下，已通过证据继续保留。
- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## Parameterized Subcases

- E20-01：项目浏览器目录、当前生效受控版本、reader 账号，行级“浏览”打开正文。
- E20-02：项目浏览器目录、详情操作面板、“预览受控文件”、reader 账号，详情入口打开同一 ACTIVE 正文。
- E20-03：全局文件搜索、当前生效受控版本、reader 账号，搜索结果打开正文。
- E20-04：目录浏览器选择“全部版本”后命中待生效版本，以及选择“草稿”后命中草稿；reader 账号的“浏览”动作必须禁用，不打开正文。
- E20-05：浏览器地址直接进入 `viewer=1` 的 ACTIVE 详情，reader 账号显示正文。
- E20-06：denied-reader 账号从目录行及 `viewer=1` 地址访问 ACTIVE，行级浏览禁用、viewer 显示可见拒绝提示且不显示正文。
- E20-07：reader 已打开 ACTIVE 正文后，manager 通过本例项目“正式负责人/编制权限”页面仅停用 `DCC241016C20Reader` 的 VIEW 规则；旧页重新读取/新开均拒绝正文，再恢复同一规则。不能把已有缓存画布或本地缩放算作新授权；正文重试入口还须单独覆盖。

## Formal Entry And Fixture Contract

- 目录与搜索正式页面：`/dcc/controlled-file/browser`，项目浏览器 `data-testid="dcc-project-browser"`；项目/文件夹选择、版本范围 `data-testid="dcc-project-browser-version-view"`、搜索输入“文件名称、编号或项目名称”、行级“浏览/操作面板”。
- 详情正文正式页面：`/dcc/controlled-file/detail/{id}?viewer=1`；版本身份 `data-testid="dcc-content-version-identity"`，正文面板 `data-testid="dcc-controlled-preview-file-pane"`，拒绝断言使用可见错误提示并确认正文面板不存在/不可见。
- 目录授权页已由Manager实际打开核验；本例撤权改用项目 `/mdm/project-code` 目标行“权限”中的精确Reader VIEW规则，容器 `dcc-project-code-access-rules-dialog`、保存 `dcc-project-code-access-rules-save`。不再操作目录规则的第一条开关。
- reader、denied-reader、manager 必须是同一测试租户内独立真实账号；不使用 admin 冒充角色，不把密码、token、cookie 写入脚本或证据。ACTIVE、待生效、DRAFT 必须是任务自有 `DCC241016C20` 链，并由Root确认精确 file id/number、项目/目录和版本状态。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 旧轮仅测试授权及其执行前快照保留在历史节；本轮明确仅文档修正，不再修改该测试或Mapper/Query，不跑Maven。
- 本轮核对实际分支int_qms；8061/48061是历史验收入口，不把旧PID/Jar当当前运行态。fixture-manifest.json同步原27项已验证事实，同时保留旧false/阻塞快照。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C20，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。最新授权无需再等待Root批准；遇真实产品/前置错误准确留证交Root review，不循环等待。

## 历史：2026-10-16 初期UI准备（由原27项最终证据替代）

- 用户：Reader 910348、Denied 910349、Manager 910350、Reviewer 910354，均以DCC241016C20开头；四账号均独立context完成登录/首次改密及页面昵称核对。
- 新角色：Reader 991237、Denied 991238、Manager 991239；Reviewer复用精确业务角色 `dcc_project_reviewer`，未修改共享角色定义。
- 项目283 `DCC241016C20` / `DCC241016C20Project`，产品 `DCC241016C20Product`；Manager提交、Reviewer真实页面审核，终态COMPLETED。
- 文件夹模板5 `DCC241016C20Template`、文件夹 `DCC241016C20Folder`；本例部门 `DCC241016C20Dept`、负责人/Manager直属主管为本例Reviewer。
- 项目OWNER/VIEW规则真实保存回读；正文权限差异、ACTIVE/待生效/DRAFT、旧页撤权尚未验证，manifest不能标true。
- 上传阻断：正式“文件类型”区域显示“系统异常”，自然GET `/admin-api/dcc/file-type-taxonomies/upload-options` HTTP200；未解析业务响应JSON，无源件上传、无受控版本提交。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-20/task.md
- doc/tasks/20261016-dcc-e2e-20/execution-log.md
- doc/tasks/20261016-dcc-e2e-20/verification-report.md
- doc/tasks/20261016-dcc-e2e-20/prepared.json
- doc/tasks/20261016-dcc-e2e-20/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-20/parameter-results.json
- doc/tasks/20261016-dcc-e2e-20/e2e20-daily-entry-body-permission.e2e.mjs
- doc/tasks/20261016-dcc-e2e-20/reproduce-upload-options-blocker.mjs
- doc/tasks/20261016-dcc-e2e-20/real-ui-session.mjs
- doc/tasks/20261016-dcc-e2e-20/sanitize-traces.py
- doc/tasks/20261016-dcc-e2e-20/regress-upload-options-green.mjs
- doc/tasks/20261016-dcc-e2e-20/upload-options-green-prepared.json
- doc/tasks/20261016-dcc-e2e-20/upload-options-green-evidence.json
- doc/tasks/20261016-dcc-e2e-20/green-review-summary.json
- doc/tasks/20261016-dcc-e2e-20/artifacts
- doc/tasks/20261016-dcc-e2e-20/.gitignore
- doc/tasks/20261016-dcc-e2e-20/resume-ui-runner.mjs
- doc/tasks/20261016-dcc-e2e-20/checkpoint.json
- doc/tasks/20261016-dcc-e2e-20/pause-checkpoint.json
- doc/tasks/20261016-dcc-e2e-20/runner-handle.json
- doc/tasks/20261016-dcc-e2e-20/runtime-pause.json
- doc/tasks/20261016-dcc-e2e-20/issues.md
- doc/tasks/20261016-dcc-e2e-20/business-checkpoint.json
- doc/tasks/20261016-dcc-e2e-20/live-acceptance-helpers.js
- doc/tasks/20261016-dcc-e2e-20/p06-sql-red-review.json
- doc/tasks/20261016-dcc-e2e-20/repair-20261019-r2-handoff.json
- doc/tasks/20261016-dcc-e2e-20/repair-20261019-r3-handoff.json
- doc/tasks/20261016-dcc-e2e-20/repair-20261019-r4-handoff.json

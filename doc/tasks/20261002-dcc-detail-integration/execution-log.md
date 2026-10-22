# 执行记录

- 2026-10-02 核对分支 codex/20261001-dcc-integration，读取根 AGENTS、frontend-development、task-closeout-rules、产品需求与主管理源目录共同合同。
- 正式详情缺会签部门/指派模式投影和 INITIAL 草稿 defaultSource+actual 读取；已向主管理和后端执行者协调，不从当前用户部门或数组位置猜测。
- 局部/换版可复用 revision-options 与 /{selectedIterationId}/submit；待后端确认该入口对基线身份校验合同。
- 后端最终确认 submit 服务已用所选迭代保存基线与 Master.latest 严格校验；前端保留准备上下文校验，请求仅发送既有 DTO 字段。
- RED: `node tests/e2e/dcc-detail-public-workflow-integration-static.spec.cjs` -> FAIL，详情未挂正式申请组件。
- 原 `pnpm exec node ...` 被环境 pnpm 自动安装检查阻塞（ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY），没有确认移除 node_modules、没有安装依赖；使用项目既有本地 node/CLI 执行同一脚本。
- GREEN: 上述静态合同 PASS。
- 单元 RED: 无效 submit=false 回执被错误报告为文件身份；新增断言 FAIL。修复后结果必须为有效真实 ID，成功先锁本次申请，再 emit。
- 单元 RED: Java LocalDate 数组未转换输入日期；新增断言 FAIL。修复后数组精确转 yyyy-MM-dd，非法日历日期明确拒绝。
- GREEN: `node --test tests/unit/dcc-detail-integration.test.cjs` -> 9 PASS，覆盖 INITIAL 原始草稿、部门缺失、较早正文/换版实际意图、确认竞争、取消保留、旧响应、非法回执、日期、同签名整改安排。
- REGRESSION: `node --test tests/unit/dcc-application-read-loader.test.cjs tests/unit/dcc-working-attributes-api.test.cjs tests/unit/dcc-detail-integration.test.cjs` -> 16 PASS。
- REGRESSION: workflow-components、workflow-distribution、workflow-ic1 三套既有测试 -> 25 PASS。
- 新增组件/applicationRead 完整 ESLint PASS；公共 index/workflow 局部 ESLint（只关闭已有全文件 prettier 规则）PASS，0 errors/0 warnings；git diff --check PASS。
- 全量 type/build 交 Root 单实例运行，未执行 E2E、服务、实库或 Git 提交推送。
- 当前进入 ready_for_closeout；未授权提交推送且仍有跨归属接口/全量验证门禁，最终状态按规则记录 blocked。
- cleanup preview PASS：4 个正式任务文档 keep，delete 空，warnings none；随后如实将状态设为 blocked。
- cleanup apply BLOCKED（exit 1）：任务状态 blocked；未删除任何文件，未将其写成 PASS。

## G03 Root reviewer configuration/public entry (2026-10-03)

- Real config API/model/component added; frozen reviewer identity gates request review and displays history. Independent records entry does not require creation template/user reads.
- g03-reviewer-red.log and g03-review-entry-red.log preserve missing API/entry failures. g03-review-entry-green.log: 8 PASS with actual SFC handlers and production API validation.
- g03-project-submit-context-red.log reproduces one write to a changed target during config fetch; g03-project-submit-context-green.log verifies zero write and retained correct target/payload. Parent reason/resubmit/approval scripts pass.
- g03-combined-frontend.log: 38 PASS across browser/reviewer/detail/read API cases. g03-final-lint.log: seven modified files full ESLint PASS. g03-post-review-types.log and g03-build-local.log: exact project type config and env.local build PASS, process75681 exit0.
- No live runtime/E2E/DB/Git mutation. Detail relation/current-history and selected-working-source review remain open; in_progress retained.

## G05 实际进展

BDD 已追加。RED: node --test tests/unit/dcc-detail-closure.test.cjs -> FAIL 4/4（正式轮次/关联权限 wrapper 与所选稿回显缺失）；补历史/关系边界后 RED 2/6（历史轮次组件和关系合同缺失）。GREEN: 同文件 7/7；详情既有行为迁移为真实选稿后，与 dcc-detail-integration 合跑 18/18 PASS。旧详情测试原先直接构造命令绕过所选正文读取，本轮按真实选择事件更新，保留原 INITIAL/确认/签名能力断言。

已挂 DetailApplicationHistory、DetailRelationsPanel。当前关系使用实际 source 并校验 Master，旧版本不可保存；历史保留 snapshot 的 relationId/projectCodeId/fileName/fileNumber/versionNo，正文始终预览所选历史 ID。申请列表来自正式 application-rounds，选择后精确 application-evidence，包括已结束作废。工作稿选择读取本人合法稿 defaultSource/actual，CE/FDA 不被当前 NMPA 覆盖，晚响应不会串稿。Root 负责类型/build、真实页面和最终 Git/收尾；本Agent只定向测试/lint。

工具记录：pnpm exec node 首次触发 pnpm 本机自动依赖一致性检查，因无TTY拒绝安装；未删除或重装依赖。改用现有 Node 运行真实项目单元测试，非 npm/yarn。一次组合验证前置测试 FAIL 后 PowerShell 最后命令 exit=0；已在正确 cwd 单独重跑当前单元命令得到 18/18 PASS，组合 exit 不作为证据。

G05 接线已完成到单元 GREEN：新详情父组件在当前关系 source 不同但同 Master 时只读展示；仅实际选中当前源可保存。所选工作正文回读CE/FDA并绑定selectedIterationId，确认指纹包含所选正文，晚响应清空阻断；申请历史展示正式轮次及本轮签名。SFC script/template 5 个组件 PASS，定向 ESLint Node API errorCount=0（已清除本轮 warning）。Revision组件18/18、关系真实Vue运行时23/23、detail合跑18/18 PASS；未跑真实E2E，不冒称整体完成。

待主管理/backend核对：VersionHistory响应目前没有processInstanceId（详情有），需后台真实透传后历史表才能显示每次A/2的BPM。workflow.ts公共类型目前未含projectFolderId/revisionAttemptNo/reworkPredecessorControlledFileId与history.processInstanceId；本Agent用局部事实类型接后端既有字段，不越权修改Root持有workflow.ts。upload Agent还需Root将route preview Long数组类型改成 number|string（正式批准stageCode=MATRIX_APPROVAL）。

G05 权限合同复核（仍待后端收口）：QueryService.getRevisionOptions 标注 OWNER canReplacement 可选非本人工作稿，但公共 submitWorkingIteration/working-attributes 均硬校验 requester=user；C createRevision 内部允许 OWNER 换版非本人稿。不能靠前端放宽草稿读取绕守卫。已将源码依据交 backend_closure 要求统一真实读取/提交合同或明确当前不可操作投影。

Root 最新分派确认 workflow.ts 也由 detail_closure 独占。已补项目文件夹、申请尝试号、返工前驱、历史BPM公共类型，以及 RoutePreview candidateSourceId/candidateSourceIds/resolvedUserIds 与 blocker.userId 的 Long联合类型。历史BPM后台透传待backend_closure完成；上传Agent已告知正式批准节点为 MATRIX_APPROVAL（不是 MATRIX_APPROVE）。

G05 回归最终命令（每段fail-fast）：node --test tests/unit/dcc-detail-closure.test.cjs tests/unit/dcc-detail-integration.test.cjs -> 18/18 PASS；node scripts/dcc-revision-component.test.mjs ->18/18 PASS；node scripts/dcc-relations-vue-runtime.test.mjs ->23/23 PASS。包含workflow.ts的8文件 ESLint Node API -> errorCount=0/warningCount=0。5个详情/升版SFC编译PASS。

G05 最新定向用例9/9 PASS（增加跨轮签名拒绝与公共入口/尝试事实接线），包含旧详情11项合跑20/20。完整evidence保留正式服务器signedAt，签名人取actorNicknameSnapshot（非猜提交人），详情当前BPM只在正式列表找到唯一mapping时自动选中，其余类型从正式application-rounds选择。8文件 lint errors=0/warnings=0。已补workflow公共字段，不再留Root接线待办；后端history BPM与OWNER非本人稿合同仍需后台唯一Owner完成。

G05 UTF-8 LF规范化SHA256（2026-10-03，待后端合同完成后以最终指纹覆盖）：
- workflow.ts: 171e5565a6c57606cd25a218cc7e885ad8056c38218411cc3ad8575f5798a760
- applicationRead.ts: 3f7eeeaffe482013bb0439ece893b55469c9c44a6d89fc160639ec074038607a
- DetailApplicationPanel.vue: 810b991ab2611162fd3604e8c2a37dcf1f47a4c658c7a2922beffba88eb22d4b
- DetailApplicationHistory.vue: 11c8188925e87e459d48b101b0ab881ade2b45b24f46d83b506d8585b18a1305
- DetailRelationsPanel.vue: a32eecd188d5e8574e7fb06cb46a8c9123d632cf9766fef4375accc9e9a3d698
- relation-contract.ts: 25d688c8f21d9bace9fb2c4f44e32b0a86f1d4c8f585f62a369b836208bc9d00
- DccRevisionPanel.vue: d63ba41aa2b60339f9e3f2ce56b979764fa0e51978add2713b5c63582c9fbd45
- detail/index.vue: 4aaec5fa64b4199d028ffbc7b2d603fcc2e1e5d18ec1b8c55b285b831eb63973

G05 当前写入归属释放通知前检查：公共workflow.ts/applicationRead.ts与新父组件均已落文件；单元20/20、lint 8文件0/0、5SFC编译PASS，git diff --check目标已跟踪文件PASS。当前不标ready_for_closeout，因为后台owner换版本人读取合同与history BPM仍需双方收口，Root全量type/build及E2E未执行。本Agent可继续依据后台最终合同补API与定向验证。

G05 当前source关系历史端点的Long字段仍在 DccControlledFileRelatedFileRespVO 以Long声明无显式STRING（relationId/controlledFileId/masterId/projectCodeId）；前端运行时严格拒绝已失精度number，不自造ID。应后台唯一Owner审查全局序列化或补STRING，确保跨9007199254740991边界仍可历史选择/预览。当前新单元使用真实字符串Long；不代表真实响应已证实。

G05 补充核对解除上条Long响应待核查：全局 YudaoJacksonAutoConfiguration 对 Long.class 安装 NumberSerializer；yudao-common/.../json/databind/NumberSerializer 会把超出安全整数范围Long序列化为字符串。历史关系VO无需另加STRING才能避免失精度，前端安全number|string验证与该正式全局规则一致。未把此项继续列后端缺口。

G06 RED：node --test tests/unit/dcc-detail-closure.test.cjs -> 4个新实际行为FAIL（replacement wrapper/intent handler不存在、轻量投影未校验、项目强读失败遮住历史）。GREEN：detail-closure13/13+既有detail-integration11/11合跑24/24 PASS。已明确REPLACEMENT按精确baseline+selected正文请求replacement-attributes，读取意图拥有独立generation和snapshotIntent；普通canSubmit=false不阻断新换版上下文。原稿保存/恢复在REPLACEMENT模板隐藏且handler阻止，无原工作稿写入。

relation-permissions已接P15完整正式轻量投影。当前source和历史target都用此投影核对tenant/Master/project/selected ID，历史name/version/status仍只来自snapshot。删除getControlledFile/getProjectDiscovery依赖；选中文件名称上下文先建立，当前集异常保留独立历史tab。只在真实可编辑源读取项目目录，失败在当前tab显示directoryError并关闭编辑，不遮挡历史。局部4文件lint0/0。完整SFC/回归及后端最终合同确认继续。

G06 后台合同已按源码/integration-notes P13/P15核对：replacement-attributes 返回同DccWorkingApplicationAttributes而不新增写能力；submit REPLACEMENT生成actor独立候选，普通draft请求本人限定保持。relation-permissions当前正式字段已实现，名称可见成功无需详情/项目详情；projectFolder缺正式placement时两字段均null。前端wrapper按这个合同验证，只用合法状态/permission，不自行按status伪造controlled=true。当前/历史关系父组件强详情/项目补充读均已删除。给backend_closure发送核对确认，无后台写入。

G06 最终UTF-8 LF SHA256：
- applicationRead.ts d856c5cb14a021b9608990541070b01324b89537ea5b35cf61d6f50528839ae2
- DetailApplicationPanel.vue 91a32c8060d2e191e8510b2f630407d9eb695dbc4a9d7636345861ab370af1f3
- DetailRelationsPanel.vue 8f546499f20f687ced2931be5325d3c8be20a9b0e1a627b545eeaf225359bcda
- DccRevisionPanel.vue f4e40082b58839cc0cf6805d0a8449cfd247ceff27af641e8c779f7270c47ec8
- dcc-detail-closure.test.cjs 07b20658c0134dff312910292c6bb23842ccd71dd0557acd0ab6f2b1a6a4aab2
- dcc-detail-integration.test.cjs dc63b686451b75dc7f48fe0273f335979805017d2192ca29c708d98f6c8fb2a8
G06定向收口交Root；不代Root改变总任务状态或执行cleanup/Git。

G07浏览wrapper：loadDccProjectBrowserPage第三参数ProjectBrowserOptions已落{latestVersionOnly?:boolean,status?:string}，默认true；按真实DccControlledFileStatusEnum状态精确转发，非法类型/非精确状态在请求前报错。服务器返回与请求status/最新ID错配明确报错，不客户端筛选或重排/伪造total。selectorScope/loadDccSelectorPage未改变。
RED：本轮wrapper行为默认false!=true，1项FAIL。GREEN：dcc-detail-closure16/16 PASS。请upload_closure按其显式all-version测试使用第三参数false；Root统一types/build。本Agent仅共享API与所属测试/任务记录。

G07接口已完成：export ProjectBrowserOptions.latestVersionOnly默认true，status使用DccProjectBrowserStatus正式联合类型（真实后端Enum精确集合），非法options/status在HTTP前失败。已有upload_closure公共浏览model/SFC测试10/10+7/7已使用真实wrapper通过。请browserVersionOptions返回类型改用ProjectBrowserOptions/DccProjectBrowserStatus（其现status?:string宽类型会触发TS）；只需类型对齐，无API改变。Root未跑第二份构建，本Agent没有types/build。

G07共享API最终SHA256(LF UTF-8)：applicationRead.ts 2a653ad94a72e98377117471f24150f161dd34af0c05e18134d208e2cbd099df。公开类型ProjectBrowserOptions.status为DccProjectBrowserStatus而非string；browserVersionOptions当前还写status?:string，需upload_closure唯一Owner更新函数返回类型以免Root类型门禁报错。仅类型指派，不本Agent越权修改browser。

G07 API wrapper BDD/GREEN/审查报告均已记录。当前状态保持in_progress，未执行cleanup/apply，未代Root修改状态。

G08 UI-04实际修复：index.vue不再直接以file.processInstanceId自动选属性轮次，传applicationRoundSelection；显式route BPM/task先等既有TaskApi真实读取返回且processInstanceId/指定task成员一致，通用BPM审批详情仍按原权限门禁（未额外强读）。父读取核验只绑定只读选择，不授予签名/办理资格；正式application-rounds/application-evidence继续由后端HistoryGuard按tenant/definition/对象版本最终鉴权。无审批上下文普通详情仍默认native；普通空任务列表不新增阻断。

DetailApplicationHistory新增contextKey/contextError/lockPrimaryRound，route/文件/任务变化同步清证据；办理面板锁定唯一实际轮次，禁止手动切换另一轮假装当前；native普通详情可明确历史浏览。缺失、重复、外来映射明确清旧证据/选中报错；旧任务/列表/evidence晚响应不落入新上下文。

RED：dcc-detail-application-round-context初跑6/6 FAIL，实际原行为允许办理中选native、缺主mapping不报错、pending保留列表、context变化不清证据、父页固定native及helper缺失。GREEN10/10（含actual父handler提取真实执行、task-only API门禁、外来task BPM拒绝、任务/列表/evidence晚响应）。受影响详情/训练合跑44/44，两个既有审批/公共接线static PASS；两个SFC编译PASS，3源码ESLint0/0。未执行E2E/type/build/服务/DB/Git。G07其它UI-01/02/03/05/06未扩写。

G08最终LF UTF-8指纹：detail/index.vue 35d139d1ca96a50b4b87094de26d84b46c910f4302a43818e1f38e3ac8846608；DetailApplicationHistory.vue d3db2a5389a3ef3bbacf04051b1496a876b4c6298a0e2562d4be15f68222dc72；application-round-context.ts 4836a76f0bfcf6aeab7f7bb4a749434e5b8215d32d5efcd42bba8fe5f9440344；dcc-detail-application-round-context.test.cjs c58073d3b649280f5756ce210b020995ad9544aa7f2b6c973909a6a9adc205b3。未改applicationRead/browser/reference/backend。

G11 UI-03实际修复：共用DccFileSelector左侧新增正式项目search/server page/total，独立SelectorProjectDirectoryState复用getProjectDiscoveryPage/getProjectFolders/buildProjectFolderTree与正式Long验证。不依赖旧props目录补空或NAS；开窗默认按Source精确folder定位，项目行选择只刷新候选tree；项目根不发project-only selector查询，只有folder或用户明确global才取文件。顶部Source/原已选/引用持久目标保持不变。

RED：专属真实SFC/正式wrapper初跑3/3业务FAIL（独立loader不存在、左侧无项目列表/跨项目入口）；首次渲染fixture漏table row导致测试工具错误已先修正后重跑上述真实RED，未把fixture报错当生产RED。GREEN：dcc-selector-project-directory7/7，含分页搜索、Long跨项目同名folder、source/target不变、取消零写、页/目录晚响应、关闭/unmount、无folder无非法请求、目录错误global可用。

受影响真实Vue回归：dcc-relations-vue-runtime25/25（包含Root G10 auto-open单次消费/只读不打开）；dcc-relations-components18/18；dcc-public-browser-vue14/14。旧selector专属fixture已加载实际新loader和正式API wrappers，并等待目录读取，无模拟成功或放宽业务断言。两个当前源码ESLint0/0；真实SFC模板已被上述运行/compile覆盖。

生产仅改DccFileSelector.vue、新selector-project-directory.ts；未改DetailRelationsPanel/G10消费语义、browser/upload/basic-data/backend/公共API。只编辑上述专属tests及既有任务记录；无服务/实库/E2E/Git/types/build。Root整体任务/cleanup统一，不冒称整套completed。

最终LF UTF8 SHA256：DccFileSelector.vue dae3edbe675d7713bce80f35088a7b98844eec905a32e6f0f094e12f8646da73；selector-project-directory.ts 1943f892d6c169af5d19ab312a0d147911d9d4de21a9bcf12db305f31f33e3bf；dcc-selector-project-directory.test.cjs f3a80a9c4ec030296ed87ac4d72e772d1c6527825da677be330f244b7b076852；dcc-relations-vue-runtime.test.mjs 9ea99b3f863fdae42ad95c086cd09a276bea5a3918b90c80cf7fc673a05fe234；dcc-relations-components.test.mjs 681542a4260260c0c006cc5bb438b0667faba0ecb31b41f28814990516ac55e4。

G13负责人只读Review已交g13-owner-ui-review.md：17/17既有PASS、Picker编译PASS；实际生产parent/helper转译Promise实验复现FE-01旧readiness续签新task（P1）及FE-02旧签名响应关闭新弹框（P2）。确认取消/确认期间owner及route漂移零签名、非适用载荷不含owner、Long/本人的输入密码已验证；停用/权限/签名绑定与历史事务只是后台源码核对，未运行后台测试。只写报告/记录，未编辑Root本批生产或tests，不跑全量type/build/Maven/服务/实库/Git/E2E。

G14公开批准handler修复有效RED：新dcc-approval-dialog-context初跑5FAIL/1PASS，复现readiness旧调用续签新task、ABA、旧成功关闭新弹框、旧错误覆盖新输入、readiness中改意见仍提交。GREEN新10/10 PASS；父handler按真实AST声明执行，approval-actions转译真实载荷，非重写逻辑。入口冻结file/task/BPM/route/actor/dialogGeneration及完整form/输出ticket/下发scope，readiness必须返回true且原上下文/表单未变；确认前后检查同指纹，payload只用冻结值。

Generation在打开/关闭/上下文/卸载均失效，异步close cleanup也不能清新弹框。readiness响应自己还核对原dialog/file/context，reset使旧requestSeq失效。late签名成功只准确提示原文件/任务已签名提交到原记录核对，保留新弹框/owner/busy；late错误仅提示原记录核对，不写新fieldErrors；accepted后刷新失败明确已提交，不保留提交态诱发重试。密码/凭据只内存快照，报告/日志不保存值。

生产仅detail/index.vue，未改变approval-actions/workflow公共载荷/后台或其它Owner范围。顺带把审批context watcher移到其computed声明之后，避免setup的watch getter先读const controlledFileId/currentUserId。新父handler10+Owner6+既有detail11+训练7+实际申请BPM10组合44/44，3静态合同PASS，真实index script/template compile PASS，index定向ESLint0/0。未type/build/Maven/E2E/服务/实库/Git。最终LF UTF8：detail/index.vue 804891c5de4c81f9c14606ab35c13196ab2ba2a1e04c7d1bb3e9541c39550d1f；dcc-approval-dialog-context.test.cjs 454131ef353d081908ccf02331fd9d722adc867df85405b12d28450669c0ff35。

G17迁移Review交付：依据主管理g13迁移包/complete ledger/schema、g14完整schema和g17历史来源/diff只读核对，44当前SQL原SHA全匹配。5当前账本匹配+2已精确历史来源共7applied，其余37不能缺登记即执行。17张真实新表/51个新增列缺，P1新identity列/索引已存在但旧master chain unique仍存在，需结构清理；root14项schema真实缺口、4项BPM/policy/template/activation需实际行值核验。

2hash旧源码本Agent从冻结diff反向重建并CRLF算SHA，均精确匹配原ledger，确认非仅换行；base route/checkout/assignee/batch-json后续变化实际已满足，但access_log.reason仍255需既有前向20260910容量迁移（不在44closure，建议显式增19root/45）；catalog INSERT前DDL/tenantguard完全相同，seed181→186且行号/内容重排，不重导/不改旧ledger/不acceptedEquivalent不同语义。

报告g17-migration-execution-review.md，机器manifest g14-migration-execution-review-manifest.json/结构evidence/来源proof齐。仅自有docs/json记录写入，无DB连接/DDL/DML/服务/Maven/type/build/Git；表格44逐项、17/51计数、2精确来源、catalogDDL等结构validator PASS。正式execution包需Root改用精确历史已应用依赖证据及当前forward根，不能直接把APPLY旧base/catalog的44包执行。参考报告最终授权/备份/前置/历史零改写步骤。

G19 RED/GREEN现状：初始真实guard测试7FAIL/2PASS复现了configure wrapper number/unsafe project/leader/foreign account/late context与reviewer权限绕过；审核组件当时已被并行Owner改为strict userStore tenant/role context，未覆盖该Owner生产。属性配置已修为精确Long和真实响应身份/账号目录校验、属性验证、关闭/切项目/卸载/保存ABA。独立属性/helper+API/真实SFC tests当前新增12属性相关用例、与已有reviewer 10项合计22/22 PASS。审核最新Owner版本的reviewer tests 6项均PASS；当前reviewer生产未被本Agent覆盖。

待确认/边界：ProjectAttributeConfigurationController后端实际要求 project-code:update + editor/owner，并enabled leader由DccProjectLeaderService，前端只读无法证明运行租户权限；project GET wrapper是强读取且后端作用域最终鉴权。审核最新组件checkPermi现在只接受真实permissions（含*）并userStore.getRoles doc_control、tenant/operator context，符合后端配置Controller hasPermission AND hasRole；没有改通用checkRole。历史属性保存成功但刷新失败提示原项目记录核对，避免新项目污染；wrapper拒绝false receipt。22/22离线，不跑服务/DB/E2E/types/build/Git。

G19 LF UTF-8 SHA256（审核组件指纹属于并行Owner最新源码，非本Agent覆盖）：
- ProjectAttributeConfigurationDialog.vue d0148e96b17ec49410531d832d0e3983384dd1a965019978e4dab41819db8972
- project-attribute-configuration.ts b921d8ed3cf9e402f2c0a49d1d2d01ac27f672a831bdbf6fc58261a438285ace
- projectAttributes.ts 82c04f34cf88fb224885ab5b0ae7e6e753ce892ac1ab8e69897a8e74559ed1ae
- dcc-project-configuration-guards.test.cjs 491e45eff2c9121e881582a1fa15dcf4cfa584f96b278b83cbdc16fc48fb9b9c
- ProjectReviewerConfiguration.vue 05d182b4e994267b0295a437ccf9ca902e7820cd0cb857bd282a0745e3c580a1

G19最终验证补充：4文件 ESLint Node API 0 errors/0 warnings；属性SFC compile PASS；测试22/22、既有属性unit/race及public browser regression通过。并行Owner审核组件当前指纹已记录，未被本Agent覆盖。未跑全量 FE 14185 之外的Root最终任务、未访问DB/服务。

G19导航只读Review：Root新working-browser-navigation9/9PASS，但实际AST实验复现NAV-01 stale fileDetail在route B期间仍导航旧A（P2）；NAV-02 actual getList+initial selector+真实helper晚响应key不含fullPath/exactfile/Master，同HTTP参数target21→22接受旧响应并选22（P2）。getList target missing实验明确throw/清list通过。报告g19-working-browser-navigation-review.md含源码锚点/最小测试修复；仅报告记录写入，无生产/Root tests修改，无type/build/Maven/DB/服务/Git/E2E。

G19导航Review LF UTF8指纹：working-browser-navigation.ts 50bdbe3ffac8302e0de50b74556dd2fd05f8ab014b506ebbb8197b84db9e0a45；detail/index.vue e780cd14dc7d02605ee74daf0cf376d674867c872ae6c55939c25a57dedaf28d；browser/index.vue 54f00bdfeaf2321eaeaa47b19c7349ffbd9c522e7cb4421d79a4493e9cde691c；dcc-working-browser-navigation.test.cjs 4c44660ac83124bbbd09c80203865da6696715904948010c28beb4bac1088f8b。若Root更新生产，按方法行为重新核对，不按旧line自动关闭。

G21 17结构前置准备已完成：严格扣除BackendOwner8项，contract17/41tables/562必要columns/110indexes/6原ledgerfacts。生成7只读SELECT JSONL collector与独立Python validator，protected dump SHA精确匹配receipt，非base声明shape/index先对正式迁移验证；base只要求当前必要旧结构，不提前要求19候选new lifecycle/attrs/source/owner/attempt/P1Master字段。menu_ids当前LONGTEXT保留，不为旧FormCenter脚本缩容。

离线13项正/负测试PASS，wrongsame-nameengine/colshape/default/nullable/collation/uniqueorder/prefix/generated/oldSHA/DB/数据标量均failClosed，reader拒绝非JSON错误行，不造APPLIED。旧日志524shape+424default/collation一致；protected dump提供system3表38列，旧metadata采集缺覆盖不判DB缺结构，fresh collector补；notifydupgroups待新SELECT。未连接库/服务/Maven/types/build/Git/生产改动。状态prepared_pending_fresh_collection，由Root执行readonly查询后validator才能实际targetPASS。报告g21-structure-prerequisite-review.md、receipt/contract/SELECT/validator/tests已列Cleanup Keep。

G21 postflight：Root 通知其新7 SELECT fresh17前置最终 PASS（主记录 g21-structure-runtime-facts-v2.jsonl / g21-structure-runtime-proof-final.json），此前 pending 只适用旧采集；本 Agent 不把 Root 采集当自己连接库证据。继而独立准备19候选最终 schema 工具，未触碰生产/正式SQL/Root文件。

BDD 已先记录：19候选→17新表/51 nullable 新列精确最终目标；错type/nullable/charset/collation/default/generated/index顺序拒绝；同一冻结runtime首跑/重跑 schema fingerprint一致。RED：test_g21_postflight_schema.py 首次9项业务FAIL（validator缺失）。GREEN：python doc/tasks/20261002-dcc-detail-integration/test_g21_postflight_schema.py ->12/12 PASS；REGRESSION：python doc/tasks/20261002-dcc-detail-integration/test_g21_structure_prerequisites.py ->13/13 PASS。离线fixtures明确不是MySQL执行证据。

精确合同26受影响表/463最终列/85索引形态/11匿名unique形态，完整保留protected旧9表结构并叠加19正式SQL，显式处理P1旧Master chain删除、A obligation带BPM/nullable operator、C binary source/number claim及最终VARBINARY(320) INITIAL+ATTEMPT表达式、owner/reviewer/round/placement/manualfolder、reason2000。旧base/catalog等6ledger原SHA保持，19新ledger、17空表、历史行、seed/config数据由driver独立核对。

SQL只读5 SELECT + 迁移前1 SELECT environment。未明确指定collation/engine的正式DDL引用冻结环境，不猜默认。validator CLI validate --facts FILE --environment FROZEN --result RESULT [--previous FIRST]；成功exit0/status POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE，失败exit1/status FAIL。重跑锁定schema/contract/environment三fingerprint；driver须固定query/env/validator/contract及import sibling解析器SHA，缺任何依赖拒绝。

最终structural verification PASS：receipt 7文件SHA/bytes全部一致；19候选顺序/ID/raw SQL SHA与Root g18候选逐项一致。g21-postflight-preparation-receipt.json status=prepared_exact_postflight_contract，readyForDriver=true，executionAuthorized=false，databaseConnected=false，runtimeTestsExecuted=false。固定指纹及调用边界已直接交 backend_closure；详 g21-postflight-review.md。本Agent不代Root整体completed/cleanup/Git。

G21 driver独立只读Review：先记录BDD，26现有driver tests真实离线PASS（3.616s）；Root支持history5+composer3 PASS。现有driver tests setUpClass确定性生成其prepared-inputs，结果与当前正式prepare相同，未手改任何被审tools/合同。Mock管线不等于MySQL演练，无actualdriver authorize/DB/服务/Git/Maven/types/build。

纯内存negative复现R01：新33主键、旧hash全保留与first/repeat一致仍可接受新info错form_id/print_template_setting，因为seed SELECT/expected未覆盖这些正式V4复制字段。R02：snapshot directmysql.read绕protected_read，private_error在exception但outer只写str，确切stderr丢且该部分raw无逐phase记录。已直接给Root/backend Owner；Owner接修但未以计划当GREEN。R03保留backup snapshot clone成功不等价当前源数据可迁移。独立报告g21-rehearsal-driver-review.md含源码锚点、8文件raw SHA、真实测试与静态/内存边界、最小修复及下一步；本Agent只记录/报告，不代Owner改tools。整体任务in_progress。

G21 R01/R02新冻结修复独立复核：31现有driver tests PASS(3.791s)；另手工正式SQL27info/10procdef完整清单+exact6V3 capture、独立NULL/valueHEX计算错误form/print/DGRM/HAS_START目标digest，first/repeat同33条件拒绝8次，fixed constants拒绝6次，共14negative。实际冻结snapshot helper经Recording adapter三phase raw/正确hash、repeat不动first、重stem拒绝、rows私有异常包含非UTF8精确bytes+stderr SHA留存均PASS；subprocess patch为raise，零transport且未调用actualauthorize。R01/R02 closed_offline，R03保留。新driver SHA86e9e8b5504f926dfd1d7ad529afe006393289ec811378232ded997c097e99a9；test SHAe29c15702fc5bf30f767aa517211b98c439cf9e0084a4cda01a1f91e411e1e79；contract/prepared原SHA未变。仅更新own review/记录；未DB/服务/Git/Maven/type/build。Root/backend Owner直接同步，新report最新状态offline_review_pass_r01_r02_runtime_pending。

G23只读历史claim前置：先读数据库/closeout规则、实际protected schema、name claim正式service/mapper/C migration/源ownership/infraFile源码。当前25active tenant1 legacy claim在C additive后原名仍NULL，countUnresolvedNames按tenant阻止公开NEW_UPLOAD/SOURCE预览票据链；不得改failClosed或暗增回填。

BDD→RED9缺validator→GREEN14。1条纯SELECT同statement采runtime/groupcounts、claim25/Master全部version/source infra metadata/ownership/ticket/crossrefs，无path/url/token/credentials。Root实际采283facts，原工具CLI真实拒绝LF内存SHA与CRLF落盘SHA差异；补rawchecksum有效RED1→GREEN15，prepare改成落盘原byteshash，SQL与facts原SHA未变、无需重采，不改Root旧INVALID结果。只读真facts发现多名未确认Master也与其它候选同名，补有效RED1→GREEN16，所有观察名纳入冲突（包含已UNCONFIRMED）。

最终真正事实判定：6 PROPOSED_VERIFIED_METADATA_MAPPING（claim19~24），19 UNCONFIRMED均有跨claim binaryexact源名冲突；其中7/18/28多版本不同源名。6/8/9/11/12/15额外正式编号身份不足。更早7/18初步分类已被完整历史交叉6/19替代，不继续引用旧数。没有猜sourceFileId/原名/sha，infra_file无tenant/hash正式列，body bytes仍未读取；source_bytes_verified=false/write_authorized=false。Root实际query/factsreceipt rawSHA均验证一致，查询8cecc9b7313117287ed6f67c2824a65bd9d9bf242cd4ae67e66117f44e2a6474，facts8c17e87327b31160e86be9d4c9424670c9a5e8097c1d909c8382f021819a416b。

自身g23-legacy-name-review.md、脱敏runtime-review-summary、pureSELECT/contract/validator/test/receipt列Keep；完整name结果只Root保护目录，不改Root输出/历史行/生产/正式SQL。未DB/Maven/types/build/服务/Git/E2E。Root下一步按具体身份冲突决定业务核对及单独数据配置授权，19additive迁移/历史零写边界保留，总任务in_progress。

G25用户新确认legacy口径设计：读主g25-user-authorization.json及当前NameClaim/Mapper、实际NEW/Revision/Query三调用点、20年Retention/39bytes helper输出合同。自己的g25-legacy-name-occupancy-proposal.md已形成可review完整3sidecar表scope/evidence/统一binarynamespace，30 Master-name边/13旧名/39版本，旧25claim/旧File/Master签名0写。新用户口径下旧19同名与本Master多名不当激活blocker；任一实际原资料unknown/bytes mismatch仍全拒。现有正式countUnresolved改成完整VERIFIED legacy覆盖判定，不能绕过NULL或metadata当MATCH；futureNEWexact严格拒，REVISION按actual ownMaster/selectedsource/C/权限，不一律误拒/同Master放行NEW。

proposal有typed3intent/server callsite、独立migration dependsOn精确C+sourceownership（不扩19）、rawbytesmanifest39coverage、actualsourceproof trust、Master-name transaction锁/unique竞争、新配置API真实doc_control+update权限、非QA配置审计、20年所有旧owner核验/不新增job、具体Java/SQL/test文件和有效RED/GREEN计划。Root/bodyOwner直接协调，无生产Java/SQL/Maven/DB/服务/types/build/Git；本轮只有设计结构验证，不宣称实现GREEN或sourcebytes已完成。Root review后唯一分配生产与精确DBscope，整体任务in_progress。
# G25 核心实施（2026-10-03，唯一 Java/SQL/Maven Owner）

Root批准按已审查3 sidecar模型实施；Root唯一真实数据库/运行态/Git Owner。本批不新增REST、维护UI、任务、QA登记，不修改冻结19SQL，不执行真实数据库。

BDD: 完整核验占用 -> Given NULL历史claim及每版真实源identity和全量MATCH证据，When 正式unresolved门禁读取，Then 仅完整VERIFIED范围排除；缺一项、源漂移、PREPARED、registry失配仍阻止NEW。
BDD: 三动作 -> Given 已核验多Master同名，When NEW/合法本Master REVISION/真实本草稿重放，Then NEW不能凭sameMaster绕过；REVISION精确验证所选版本并只读投影旧原名；重放只允许实际现代NEW草稿。
BDD: 保留 -> Given 同名多个旧owner，When 真实作废+20日历年到期释放，Then 仅最后合法owner释放全局占用；未知时间或在用版本不释放，历史原行不更新。
BDD: 事务/并发 -> Given NEW唯一占用竞争或后续异常，When 正式claim及registry同事务写入，Then 唯一winner且失败全回滚；保留现有完整binary唯一约束。

RED/GREEN/REGRESSION证据后续追加。此前design-only状态仅描述前一阶段，不代表本批已完成。
# G26 注册与作废审计投影 BDD

BDD: verified Root sealed registration -> Given authenticated actual enabled doc_control+update identity, same source DB UUID and immutable actual facts/39MATCH artifacts, When internal activation, Then exact sidecar scope insertion and official GxpAuditService.append share the real Spring transaction; missingpolicy/auditfailure/source404/contextmissing cause zero rows; replay unchanged returns original event receipt.
BDD: retention audit projection -> Given legacy20year sidecar retained and oldclaim retain_until remainsNULL, When existing obsolete/automatic-obsolete audit snapshots state, Then explicit actual deadline appears in official sameoperation state evidence; mixedmodern and legacy retained names are all represented without guessing a max.
新技术登记operation仅单独prospective26候选，当前正式25policy保持原bytes。源恢复未批准，实际数据库/对象零写。

## G26 本批真实验证状态（尚未最终冻结）

- RED actual NameClaim full evidence: expected0 unresolved but was1；随后完整JOIN覆盖服务GREEN。
- RED actual PREPARED secondscope污染: expected[SOP.pdf] but[SOP.pdf,UNVERIFIED.pdf]；exactouter scope修复后GREEN。
- RED actual obsolete audit sidecar retention: expected2046-10-03T12:00 butNULL；正式sameoperation read projection修复后occupancy39 GREEN。
- 原8类177 PASS；随后扩大旧Mockito服务口新增sourceprojection读取使原verifyNoInteractions错误，更新为明确零reservation/copy写；权限先guard由Rootreview要求保留。Query240+Revision63+occupancy39真实PASS。
- 注册初次setup Bean名environment/fileMapper、setLoginUser null request及全局JsonUtils舍弃errorCodeNULL/verifiedAt ISO等FAIL均是fixture/setup错误，未计业务RED/PASS；严格factory不降级。单独20registration真实PASS：g26-registration-green.log 20:33:48 exit0，真实GxpAuditServiceImpl+Spring/H2事务。
- 最终17类579包含新增first-registration concurrentNEW和missing-frozen-version tests，目前15145重新运行，未写最终PASS。
- MySQL新独立迁移静态5 contract PASS；完整policy闭包8 PASS；DATETIME(6)默认CURRENT_TIMESTAMP(6)和同精度UPDATE一致。运行first/repeat待Root授权，本Agent0actualDB。
- 原policy real coverage RED newoperation missing exit1；独立prospective仅原rawbytes+1operation GREEN operations34/annotations12/reportsha3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d。正式policy原始bytes保持，原25清单不改。新增configuration策略仍待真实质量批准，绝不注册。

Registration只内部方法，无REST/Runner/登录token plumbing；将来具体执行入口由Rootreview。actual35/39缺原件、用户回复尚未批准恢复，本Agent0objectwrites；不能用隔离测试生成实际可激活scope。20年后公开编号NEW被既有Master唯一/永久guard挡住，隔离claim/registry复用不是公开E2E成功，Root后续单独收口。
# G27 统一文件选择器核验原名投影 BDD

Given 已完整核验legacy本版原名且旧File仍NULL、真实当前latest受控指针与正式项目/leaf/编号，When 候选/keyword/count/page和公开selector服务查询，Then 按本版verified evidence搜索并在硬范围/名称授权后只读投影原名，不用title/infra猜名、不更新旧字段、不授予正文权限。
Given PREPARED/未知scope、外租户/无硬范围或缺正式identity，When selector查询，Then 不匹配猜名且明确拒绝非法投影，不通过metadata核验推导owner/body资格。
Root已接受前批17类579 PASS和冻结37资产回执，本增量只有SelectorMapper/Query/本任务自有测试，不改FE/历史/现代唯一/原19SQL。

## G27 最终软件验证收口

- Broad GREEN: Maven17指定类579/0fail/0error/0skip exit0（2026-10-03 20:51:24）；g26-final-regression.log原hash见g26receipt。
- Selector有效RED: 两个真实SQL/公开service expected1 but0（g27-selector-red.log，exit1）。GREEN REGRESSION: 最后5类324/0fail/0error/0skip exit0（21:09:17），含legacy46、registration22、selector15、browserHTTP1、Query240；与579重叠，不相加。
- 黑名单校验使用硬范围/name授权后独立copy verified source name；未核验PREPARED无fallback、不泄露正文资格；exact keyword/candidate/count/page同FROM EXISTS不增行、历史NULL保持；现代既有selector不增加历史formal门槛。
- Static schema5 PASS，independent policy完整closure8 PASS。MySQL实际新DDL first/repeat尚未执行；隔离H2并发不冒充MySQL MVCC验收。
- Root已review并采用仅开发的最终未批准候选policy（原33payload字段不变，仅新增legacy-name-occupancy.activate）。正式coverage34/12 PASS，sha3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d；原G22冻结25准备证据保留，不假装扩大旧实际数据库授权。actual质量批准仍尚未批准。
- 真实旧25Master latestControlledFileId全部NULL、6个缺formal未受控记录，Root独立只读核对；本批不回填指针、编号、项目/源名字，不把它们猜成可选受控文件。未来畸形已受控旧记录选择器strict报错的限制列明，读资格不推导编辑权限。
- 最终41source/test/DDL/policy资产与精确rawSHA见g27-legacy-final-delivery-fingerprints.json；g27-legacy-final-verification-receipt.json记录全部实际执行未完成项false。无服务/实际DB/object/Git操作、未packageJar。Root后续Review后完成集成/运行态/E2E/本地Git。

## G27 main application packaging (Root assigned sole Maven Owner)
Given final reviewed source seal41 unchanged and related579/324 tests green, When offline main app package with skipTests, Then new executable Jar must include reviewed modules and exact new artifact digest; no services/DB/Git. Preflight verified original G20 Jar507250868 SHAa4cf78f59d18ac333c64029dd86cf18bf4188c6f0e75762ac3a90eb1a516ef16 and archived copy under task-only protected g27-main-package-artifacts. Root source seal740678d518f7d635cdd3aecda437861fdaa7f1e631ce38eb43219533505bbec6 verified41 rows zero drift. Packaging is artifact verification; no new source/TDD behavior change.

## G27 offline main application package

PASS: mvn -o -pl yudao-server -am package '-DskipTests' -> exit0 BUILD SUCCESS (2026-10-03 21:29:00). Jar bytes 507297396 SHA 81b56aa3e1c518ae8f989498a8624826a1ad946d8e2640e652e4cb3a32f30bc5. New registration/sealed/environment/reservation/selector classes exist inside actual BOOT-INF DCC module. 41 frozen source assets remained unchanged; source seal 740678d518f7d635cdd3aecda437861fdaa7f1e631ce38eb43219533505bbec6. Exact artifact evidence g27-main-package-receipt.json. Old G20 Jar was verified and copied to task-owned protected backup; extracted old SDK evidence unchanged. Packaging skipped tests per Root because prior related software gates already green; no services/DB/object/QA/E2E/Git work was performed.

## G28 单迁移准备 BDD / RED / GREEN

Given 新1DDLscope/独立原事实与库身份，When plan，Then完整closure8仅execution1、其它7facts-only，schema3/70/8micro/2generated/6secondary+3PRIMARY/7CHECK exact，actualauthorizationfalse。
Given部分错schema/ledger/oldrow/Sqlhash/clonefakeproof、临写writers或fakeemptybackup，When执行验证，Then初始化transport前拒不完整授权；临写guard零写；SQL失败first停止，无repeat/retry/恢复。
RED: g28-schema-test.py 与g28-driver-test.py新工具缺失FAIL，独立5tests对wrongmaterialfakeclone和literalLEGACY_GROUP归一实际RED2；GREEN fixes已回读，不改旧G21parser/helper。
GREEN: own16driver+11schema；independent5当前PASS（另一Owner保存）。plan重算actualsource19材料rawhash/19completeIDs和旧行保护、当前RootreadinessSHA，生成1migration materials及SELECT-only合同，actualtransport0。
保护统一签名表来自正式ElectronicSignatureRecordDO@TableName system_electronic_signature，不猜signature_record。备份exact7table定义/data markers/singleRowINSERT counts+seal+realcommandtimes/baselineaggregate，emptygz/缺row拒绝；实际freshcollector由Root未来执行，本工具不假造这些proof。
未修改冻结41source/正式SQL/旧G21/G23helper；SQL仍621fe041...8de2。无Maven/服务/DB/object/Git。FirstCleanupKeep已补永久g26/g27receipt/plan/package和全部g28工具，rawlogs不进入Git（PASS摘要已归档）。

## G29 fresh read-only collector preparation

G29 collector is frozen for Root review:11 offline tests pass. Explicit Root --collect-readonly uses actual existing local Docker/MySQL only; no fallback/remote/create-clone/restore/DDL/object path. Every read prefixes and verifies actual same-session source UUID/DB/version, then preserves original facts body and stores original/envelopedquerySHA/rawoutput. Read boundary rejects OUTFILE/LOAD_FILE/GET_LOCK/SLEEP/BENCHMARK. Fixed mysqldump uses --skip-routines --skip-events, schema no-data or seven protected rows no-create-info/skip-extended-insert; sealed command receipts/completegz/rowcount/markers required. Writer counts strict integer0 before/after. Existing directory is never overwritten; failed collection only type-safe journal in directory actually created by this invocation.

Generated G28 request retains specificNewMigrationAuthorized=false/authnull. Actual new1DDL authorization must be separately bound by Root; collector itself cannot execute it. All actual collector/backup/DDL/runtime/object/Git invocations remain0. G28 frozen12 andproduction41 source unchanged; g29 collector fingerprints andverification receipt preserve offline evidence.

G29 final bounded Root review fix:12 offlinePASS, formal G21 schema/environment query read_bytes decode UTF8 preserves exactCRLF/rawSHA; schema capture projects actual last readreceipt envelope/hash/facts (no hardcoded source label). Explicit offlinefake adapter is marked offlineFixtureOnly and cannotbeproductionproof. Actual collector still0invocations.

## G29 Windows raw-fact preservation review fix (2026-10-04)

BDD: Given actual adapter LF fact bytes and sealed actual same-session capture receipt, When Windows collector persists schema/environment facts, Then bytes/SHA remain exactly identical without CRLF transformation or offline capture bypass. RED: g29-windows-facts-red.log actual Windows13tests1error `actual schema raw query/facts capture differs`; new case executes real RecordingAdapter.read with mocked subprocess (no actual DB), disables explicitOfflineTestAdapter, only post19 validator port is isolated. GREEN:13/13 PASS after schema/environment write_bytes(raw.encodeUTF8); original query rawCRLF preserved and actual capture hashes remain strict. No normalization/fallback/false success. Actual DB/collector/backup/DDL0invocations; source41/G28frozen12 unchanged. Newseal supersedes priorG29seal, metadata rawlogs not staged.

## G29 R2 generated-column actual-backup failure repair

Rootactualfirstcollection data dumpexit0 butoldregexcountFile0/claim0 caused strictbackupfail beforeexecutionrequest. NewversionedR2 files preserve alloldG28/G29assets/seals andfailedcapture directory. Parser exactschema/baseline ordered columns, generatedexclude, explicitcolumnprojection, quotedliteral escaping/valuearity/single-row boundaries; data stream count anddriver validation both use it before READYrequest. Actual preservedgz read-onlycounts41615/27 (other5exact) prove parser fix, notfreshrecollection orDDL. Tests7parser+13collectorR2+16driverR2 PASS, R2plan ownprefixonly. Finalnew14asset+9dependencyseal andremainingnoactualexecution gates in g29-r2-delivery-fingerprints.json/g29-r2-verification-receipt.json. No source41/SQL/G21helper drift, noactualDB/Maven/services/Git.

## G32 internal historical occupancy execution closure preparation

Task-only pure offline builder and Java canonical/factory helper, no production41/Jar/Maven/app/DB/APIbusiness/object/Git. Exact operator CLI reviewed283facts25claim25Master39versions39sources39ownership76tickets39reference; all sources mustactual MATCH0exit samefacts/results hash. BDD incomplete35/4 -> noactivationmanifest; actual protectedinputs pureoffline command created onlyblockeddiagnostic innewprotectedg32-incomplete-source-diagnostic. Javahelper uses actual compiled DccLegacyNameVerifiedScope.rowHash/identity/factory and officialBootYudaoJackson serializer configuration/nullprobe. G29 HEXconcat rowSHA is expressly notacceptedJavahash; freshJavaJdbcTemplate SELECTstar read-only output mustbeactualtyped currentidentity. Actual runtimeObjectMapper null/profile compatibility remainsunvalidated; no defaultguess.

RED newbuildermissing testsFAIL; GREEN9 tests including current35/4block, consistentrehashidentitymutationreject, wrongmetadata/locator/preimageprotocol, actualstrictJavafactoryfakecompletefixtureaccepted, NULL/Boolean/Bytes/SQLTimestamp canonicaldistinction, rawexceptionsecret sanitized loggerOFF. Task31libraries verified extracted from existingnewJar81b56; taskjavac only noMaven, actualreadonlycollectmode notinvoked. Concrete controllerless authenticatedmaintenance entry is DESIGN ONLY: realfrontlogin token viaOAuth2TokenApi/actualsameTenant enableduser/doc_control/update, Root artifactreview andsameGXPtxn; no userId-forged admin/SYSTEM_ACTOR/newREST. Allwritegates pendingRootreview andactual39complete/QA/newDDLscope.

Finalhelper/build contract/source/runtimeclass fingerprints and9testreceipt g32-delivery-fingerprints.json/g32-verification-receipt.json. Actualactivationmanifest0/DBconnections0/services0; currentgoalnotcompleted.

G32 finalRootprofileguard:10offline testsPASS. Actualpreimages require readOnly=true/RR/MySQL8.0.40/nullProbeIncludeALWAYS plus realUTCcapture<=900seconds beforebuild. Future/expiredtime rejected as technicalfreshness, notuserpermissionexpiry. Prior8f94seal supersededexplicitly;actual0JDBC/activation remains.
# G34 传统审阅矩阵与正式三动作隔离 BDD

Root授权本Agent唯一后端/MavenOwner：MatrixAdminServiceImpl及必要Mapper/test。Given同category启用LEGACY及NEW/REVISION/OBSOLETE，When旧审阅矩阵save/import/delete，Then只操作explicit LEGACY，三动作root/node/version/snapshot原payload保持、正式exactaction读取仍有效；没有NULL兼容/动作fallback/历史回填。Given批量legacy访问projection，When读取，Then只取已生效LEGACY，不从typed或futurelatest推断人员。Given同category并发legacy写或下游failure，When事务执行，Then锁category稳定版本、原scope回滚、typed完全不动。实际Root只读column action_type NOTNULL DEFAULT LEGACY/null0已确认。
有效RED先用真实H2root/node行为复现当前save/delete破坏三typedroute，GREEN有限fix和既有MatrixAdmin/read测试；无实际DB/服务/package/浏览器。用户最新先主流程/架构，纯文案/fullCRUD细节后置，本批仅直接阻塞审批主流程的范围隔离。

## G34 主审批路线阻塞修复最终证据

有效RED:3真实H2 tests fail3/error0，save explicitactionNULL、legacydelete使typedresolverlookupNULL、batchread混typed，g34-matrix-isolation-red.log保留SHA。有限GREEN:3类65全0；加入lateLegacyNodeFailure真实事务rollback、同Legacycat并发版本1/2+active1、future有效读取共3case，r2和最终3类68全0，Maven14178 exit0 BUILD SUCCESS2026-10-04 09:16:44。最终g34-final-regression.log独立不覆盖，rawsha及scope见g34-matrix-verification-receipt.json。

生产仅2文件AdminMatrixServiceImpl+CategoryMapper，测试仅原AdminMatrixTest增有效case。明确LEGACY peraction max含deletedhistory，categorytenant scopedlock(实际tenantrequired、非default)，save/import/delete只LEGACY停用；batchprojection只effectiveLEGACY，typedroots/nodes unchanged，formalactionresolver路径原样无fallback。VO/Controller/RouteMapper/SQL/G33不改，无新migration/原历史回填/实际DB/服务/Jarpackage/UI。

证据限制：早期同green.log路径先编译getTenantId错误、后65fixturetenant0错误被后来命令覆盖，原始stderr日志已失存，不能写“全失败raw保留”；它们是实现/fixture错误，不替代业务RED。testcategoryfixture只隔离H2把本次row tenant=required，生产guard没放宽。最终typedroots完整JSON比对/nodecount和staticsource无nodeUPDATE/DELETE支持保护结论，不夸测试已逐节点hash及真实E2E。category锁仅此Legacywriter互斥，不宣称其它formaltypedwriter共锁/全平台serializable。当前主scope闭合，基础资料全CRUD/UI纯文案/AC18扩验证按用户先主流程指令后置。

G35 existing2casefixture correction: Root组合84/11中2ReferenceError是旧canSubmitLatestWorkingIteration harness过时，不是新产品RED。唯一改tests/unit/dcc-browser-application-entry.test.cjs，AST提取actualcanEditVersion+canSubmitWorkingIteration+handleSubmitWorkingIteration，真实actor/selectedWORKING/checkout/actionLocked守卫；available精确id管理导航0submitAPI，lockedselection0navigation。node --test该文件exit0/2PASS，不用always-trueguard，不改生产，不跑types/build/Maven/DB/browser。SHAc309f5e061568a6395959e653fd4199c672a7bb4528416095c2b1fe0ddf78c3e。
# G39 批准创建项目的初始正式负责人OWNER BDD

用户actual答复已记录main g39-user-authorization.json：批准创建新project同事务为申请选定enabled同tenant leader初始化一条USER OWNER。Given批准WRITING请求/folder/defaults正式，When realwriteApprovedRequest，Then新Project→formalAccessMapper rule1/USER OWNER→publicreadFolder/listReadable正常，不授applicant/reviewer/admin/creator。Givenlatefolder/product/relation/completion/audit failure或disabled/foreigntenant leader，When写资产，Then所有Project/Product/Folder/Relation/OWNER/completed/event同txn回滚。GivenCOMPLETED回读、ACL后续正式调整，When重复内部write，Then只回既有资产不初始化/替换权限。无sourcehistory backfill/mapper绕路/新SQL/平台/G33G34G35修改。

## G39 selected project leader initial formal OWNER

User confirmed main g39-user-authorization: only selected enabledsameTenantleader gets one USER OWNER in approvednewproject transaction. EffectiveRED red-r2 actualWriter completesProject but formalAccess/readFolder rejects selectedleader7 ACCESS_DENIED; firstredtemplateeditedBy missing issetupfail notbusinessRED. GREEN direct1case actualWriter→AccessMapper→readFolders/listReadable/GxpKernel PASS; final8classes154/0fail/error/skipMaven79157 exit0 BUILD SUCCESS2026-10-04 12:14:35 specialized7cases prove access1/USER OWNER only/noapplicant-reviewer-admin, actualcompletionAuditactualrules, ownerwrite0/folder/relation/completion/auditlatefailureallrollback, disabled/foreignleaderreject, existingCOMPLETEDwrite STATUS_INVALID zeroACLoverwrite afterlaterVIEW adjustment. No creatorgrant, uniqueleader inferredfromOWNER, readerbypass oroldprojectbackfill.

Production4files Accessinterface/impl, CreateWriteService, existingProductAudit snapshot; test1specializedcombination. Mandatory Accessinit joins creation realtransaction, newprojectlocked matchesactualselectedleader anduserstatus/tenant, emptyaccessonlysingleINSERT rowcountstrict; reasonstoredactualwritereason. Existingoperation dcc.project-product.complete audit samephysicaltx includesactualprojectAccessRules, no newpolicy/migration/platform. TestkernelrealH2/formalexternaldirectoriesports, notactualDB/UI. Root review finalfingerprints/receipt; productionG33G34G35 unchanged, no package/runtime/Git.
# G39 维护Schema验证器正式指纹更新 BDD

Root批准有限pin更新：正式g39-driver.py rawSHA667a81edf468e1212afef8a01ae5cb996bd62737cfba7db6138e30259e0db2d4已经审查并支持实际CHECK协议；旧G33维护Gate仍绑定1560ee版本会拒未来真实G39验证回执。Given完整保护journal/合同/Rootreview source proof使用新已审validator，When核当前shape，Then接受exactnewpin；Given旧validator1560ee，即使其它资料自洽，Then在任何shapeSQL前拒绝。仅Gate常量+专属GateTest，fixedcontractf1ec/receiptshape/QA未批准/defaultdisabled保持；旧G33manifest原文保留，精确2资产superseded另记。无actualDB/token/services/package/Git。

## G39 maintenance validator pin final

FormalGate oldSCHEMA_VALIDATOR_SHA1560ee was pinned to oldR2 parser protocol; Rootexplicitlyapproved newreviewedg39-driver667a81 actualCHECK parser. OnlyGate constant+GateTest positivefixture pin changed, onecase supersededoldpinreject before currentshapeSQL. EffectiveRED17/1failure+1error:oldpinwronglyreachedshape/newpinrejected. GREEN3classes28/0fail/error/skipped exit0 BUILD SUCCESS2026-10-04 12:39:44, includesGate17 anddefaultRunner/absentQAcommand tests11. Sourcecontractf1ec/receiptshape/QAunapproved/defaultdisabled unchanged. OldG33 manifests kept immutable; these2files superseded by g39-maintenance-pin-fingerprints.json andexactreceipt. G39OWNER4production+specialtest assets stillzero drift. No tokens/actualDB/services/Jarpackage/Git; Rootunique nextpackage after allsourcesstable.
# G39 实际条件授权答复协议 BDD

Root发现formal维护Gate的actualAnswer白名单缺用户本轮真实“授权条件齐备后按方案执行（建议）”。本轮仅Gate与GateTest：Given本轮原始条件答复及exact25/39/13scope/hash，When读取授权receipt，Then认实际conditional授权而不篡改答复；Given尚未批准或质量前置未齐，Then仍拒执行，qualitypending Command在任何stdin/auth/activation前停止。validatorpin667a81/fixedcontractf1ec/默认关闭/所有实际qualityruntime条件不变。旧manifest原文保留，另交新superseding2source seal；无actualDB/token/service/package/Git，不重新问用户权限。

## G39 actual conditional authorization phrase final

OnlyexactactualAnswer whitelist adds userverbatim“授权条件齐备后按方案执行（建议）”, no originalreply rewrite andnoapprovaltextguard removal. EffectiveRED2casesfail(error1/fail1):actualscopeincorrectEXACT_REGISTRATION_APPROVAL_REQUIRED. Oneintermediatewrongcwdpatchfailed butMavenranoldguard andfailed; rawg39-conditional-answer-green.log preserved asTOOL_ERROR_NOT_GREEN. Actualpatchinrightcwd thenGREEN3classes30all0Maven46288exit0 BUILD SUCCESS2026-10-04 13:24:38. NewGatecase acceptsactualconditionalscope, 尚未批准rejects; sameactualconditionalanswer/pendingQA throughrealgatequalitycheck stopsCommandbeforestdin/auth/executor. Formal667a81validator/f1eccontract/defaultdisabled/exactqualityruntimegatesunchanged; noactualtoken/DB/services/package/Git. g39-conditional-answer-fingerprints.json supersedes previouspin2paths only, priorseals/records preserved; G39OWNER4production+specialtest zeroSHA drift.


## G43 本机开发入口范围更新与验证

用户取消开发阶段质量批准限制；精确开发 descriptor 替代质量资料，保持当前策略版本及26运行配置、固定Schema、39正文、实际OAuth/文控权限及真实Gxp同事务。3生产/3测试有限变更，原41和OWNER4/Auth/Runner/策略原文零漂移。有效RED新开发请求被旧Gate拒绝；首次GREEN测试map-key工具错误保留，最终130/7全通过（含Kernel继承22场景重复），无失败/跳过。详见g43-development-entry-bdd.md和fingerprints/verification-receipt.json。未执行任何实际DB、Token、服务、打包、Git或E2E，交Root review。


## G43 Auth 精确秒级到期身份

Root真实读取发现 DATETIME0与缓存epochMillis产生正常舍入差，原纳秒严格相等误拒。有限两源码修复：exact equality或精确half-up whole-second，原两侧future/真实token存在/租户/类型/scopes/当前账号/上下文校验不变。有效RED1旧身份拒绝→134/7相关回归全0→最终Entry12/1（实际JsonUtils roundtrip）全0，重复执行不合计独立场景。Connector9.7微秒中间舍入极窄499ms歧义明确拒绝，不设容差/截断/fallback。fingerprints/verification/bdd见g43-auth-expiry-*；未实际连接库/Redis/Token/服务/打包/Git。前G43、原41、OWNER4、策略和Runner零漂移。


## G43 正式用户目录审计身份

实际UI诊断证明正式token map只有nickname/deptId，旧username强制字段误拒。有限AuthAdapter/EntryTest修复使用同id/tenant启用目录的实际username/currentnickname构建新HashMap，仍拒昵称缺失/错和显式可选用户名错/空/null；缓存不改。有效RED1CURRENT_ACCOUNT_INVALID→137/7全通过，重复继承场景明确。expiryguard及前G43/原41/OWNER4/策略/Runner无变，原封存不覆盖。详见g43-auth-directory-bdd/fingerprints/verification-receipt；无实际DB/Redis/Token/服务/打包/Git，Root接Review/runtime。


## G44 正式页面人工作者审计

Root真实页面六事件误username SYSTEM_ACTOR。有限SystemGxpAuditServiceImpl修当前同id/tenant启用目录权威username/nickname，人actor在重放前校验且不改LoginInfo/历史，明确系统动作保留。有效RED1assertion→System15/2PASS→首次组合Ledger旧actor9缺目录22errors保留，唯一DCC测试补正式applicant9fixture和nickname/deptLoginInfo/真实eventusername断言→最终184/11全0（System15+DCC169，继承重复22明确）。生产1+测试2，原41/OWNER/G43/Auth/policy零漂移。g44-human-audit-*封存证据完整；无actualDB/Redis/Token/服务/打包/Git/浏览器，交RootReview/runtime。

# 注册证反馈确认结果（2026-11-22）

> 阅读提示：前面的确认记录保留当时事实；用户随后授权修复后的实施与验证状态，见文末追加章节。不能将前期“未改动”或“未核实”当作本次最终状态。

本次按“先确认”执行：复核原始记录、沿当前源码核对调用链，并通过 Playwright 从真实菜单只读检查本地测试1页面。未提交业务申请、审批、下载或保存配置，未新增代码修复。此前已经完成的修复不等于本次重新验收通过。

## 本次新增的直接证据

1. **当前确定异常：已办列表加载失败。** 测试1管理员从“审批中心→已办”进入，页面显示“系统异常”。浏览器实际请求 `GET /admin-api/approval-center/tasks/page?pageNo=1&pageSize=20&viewType=DONE&keyword=`，HTTP 200，响应 `{"code":500,"msg":"系统异常","data":null}`（本次浏览器请求772、847、856）。因此不能把空表当成没有历史记录，也无法从该正常入口重验578摘要。尚未取得对应后端堆栈，不定根因，不扩大为整个网站不可用。
2. **当前确定配置缺口：收件人及办理配置未补齐。** 配置主页面和通知弹窗均实际打开；30/8/2/1月四组提醒及变更确认仍均只有“徐翠（国际注册）”。办理负责人、项目上传期限、失效换证期限未配置，接口 `GET /dcc/registration-certificates/handling-tasks/config` 返回 `code:0,data:null`。这是相对已提供人员分工要求的配置问题，不是新证审批的额外前置，也不能由此证明通知发送算法错误。
3. **当前页面已改善：天/月文案一致。** 主页面显示30/8/2/1“个自然月”，收件人弹窗显示同数“个月”。当前后端提醒计算使用 `dueDate.minusMonths(...)`。可确认旧文案冲突当前未复现；没有执行自然到期调度，不能报调度端到端通过。
4. **当前仍有访问拒绝现象，但是否错拒未定。** 从“注册证→老证”中R01记录的“查看”按钮进入990823628、v1（versionId=990824372），返回 `code:1080000293,msg:注册证访问授权范围不合法`。本次账号为测试1管理员，不是历史文控账号。源码对OLD版本要求注册主管角色或有效旧证查看授权；管理员登录不自动证明满足该业务资格。没有原账号有效授权证据，不能直接改宽权限。

## 原反馈逐项结论

“历史确定”表示现象有留证，不表示当前必现；“源码改善”表示当前实现发生变化，不表示部署数据和全部历史样本均验收通过。

| 原编号 | 事项 | 本次结论 | 后续边界 |
|---|---|---|---|
| 2 | 工作台加载失败 | 历史确定异常；当前原账号、原触发分支未复现 | 本次管理员工作台可加载；通用日志字样不能定位原失败请求 |
| 3 | 592审核留档AxiosError | 历史确定请求异常，审批成功也有证据 | 缺原失败URL、响应和堆栈，不能断言审批失败或现已修复 |
| 4 | 反向日期未捕获异常 | 历史确定前端缺陷；此前已修改查询预校验 | 本次不把旧失败继续计作未经处理，也不冒充已完成页面回归 |
| 5 | 国内续证日期Object及必填标记 | 历史确定校验异常；此前已修正相关标记及提交错误展示 | 旧日期编辑/失焦分支仍需专门回归，不能宣布所有Object已消失 |
| 6 | 旧证列表与详情状态冲突 | 历史确定显示问题；当前源码已明确“被替换旧版本”和“证载状态（历史记录）” | 本次旧证详情被权限拦截，无法页面验收；失效日期取值仍需业务口径 |
| 7 | 天/月不一致 | 本次当前页面未复现，源码按月计算 | 自然到期实际调度尚未验收 |
| 8 | 提醒人员、办理负责人不符合要求 | **本次确认当前配置缺口仍在** | 按已给人员分工补配置；不能擅自指定人选或推断算法错误 |
| 9 | CE通知称国内注册证事件 | 历史确定文案错误；已有中性通知模板迁移 | 未确认当前库是否已应用；迁移明确不回写已送达历史消息 |
| 10 | 下载审批摘要缺公司、证名 | 历史578确定缺项；当前源码仍存在旧样本缺流程变量时摘要缺项的路径 | 正文能补齐不代表摘要同步补齐；当前已办500阻断原样本重验 |
| 11 | 历史英文节点 | 历史确定文案问题；当前留档组件已调用中文名称映射，包含旧英文名称 | 页面显示映射不等于改写历史数据库；原记录页面验收未完成 |
| 12 | CE延续未闭环、空证号提示 | 未闭环历史确定；此前已改必填及提示 | 没有当次请求证明唯一拦截原因；不改号如何沿用Rev及完整闭环仍待验证 |
| 13 | 申请人看本人下载流程被拒 | 历史确定访问失败；当前是否错拒仍未核实 | 接口仍有流程查询权限门槛，需要原申请人账号权限和正常入口复验 |
| 14 | 主管有待办却读不了流程 | 历史确定权限口径不一致；当前源码已纳入当前审批任务资格 | 需原主管实际候选分支复验，不能用管理员通过代替 |
| 15 | 文控看旧证被拒 | **本次管理员账号也复现拒绝；是否错误拒绝仍未定** | 必须核对原文控账号注册主管角色/有效旧证授权及公司范围 |
| 16 | CE编号展示与通用筛选差异 | 历史确定差异，尚不足定为丢号或查询缺陷 | 先明确Rev与通用证号字段及适用查询规则 |
| 17 | 新证提交后灰色遮罩 | 历史确定界面异常；此前已调整关闭与成功事件顺序 | 未重新提交，不能宣布历史遮罩根因已证实或所有分支已恢复 |
| 18 | 审核失败没有明确反馈 | 历史确定反馈不足，具体触发原因未定 | 代码存在上下文变化后不展示旧请求结果的分支；缺当次时序，不能据此直接定根因 |
| 19 | 593填表中断 | 历史确定操作中断，后续成功 | 页面中断还是操作引用失效未区分，不能直接算当前程序缺陷 |
| 20 | 历史500、503 | 历史确定中断且后续有成功证据 | 不等于当前网站不可用；本次已办500是独立新证据，不能冒充同一根因 |
| 21 | 新证审批绑定异常 | 历史确定、后续主链恢复 | 原根因及日志改进未全部验收，不继续计作当前阻断 |
| 22 | missing角色导致审批失败 | 历史确定、567后续批准 | 不能定为主管角色失效，也不能继续算当前阻断 |
| 23 | 空变更确认收件人导致审批失败 | 历史确定；当前保存端已明确拒绝空变更确认人员 | 历史配置保存/运行规则差异已有源码改善；异常分支仍需验证 |
| 24 | 文件存储不可用 | 历史确定、已有在线预览恢复证据 | 不能拿旧存储故障解释当前其他请求 |
| 25 | R03上午新证提交系统异常 | 历史确定、后续591—595有恢复证据 | 原堆栈缺失，不编造根因；本次未重新提交 |
| 26 | 首次详情失败 | 历史确定、后续有改善样本 | 当前是否仍偶发未核实，不列必现 |
| 27 | 技术枚举、时间戳展示 | 历史确定、已有可读化改善 | 不能继续全部列未修复，也不能宣称全量页面通过 |
| 28 | 业务留档与流程意见不同 | 历史确定、后续样本一致 | 旧意见是否补齐尚未核实 |
| 29 | CE额外生效日、改号依赖类别 | 历史限制已有改善证据 | 与第12项未闭环分开；不再将已改善限制计作当前缺陷 |
| 30 | 产品来源、14位编码 | 原来源断言证据不足，相关建议已撤回 | 后续可见字段正确不等于隐藏绑定全量核实 |
| 31 | 590资料不一致、589误点 | 资料与操作问题，非已确认程序缺陷 | 589仍按历史待审处理，本次未审批、撤销或下载 |
| 32 | 账号、内存、工具、Excel缺失 | 执行/资料条件，不足以认定业务软件缺陷 | 需要针对相应条件处理，不改业务规则绕过 |
| 33 | 已撤销标准及弱证据 | 不继续作为缺陷计数 | 不升版、晚1秒、通知待确认等本身不足证明错误 |
| 34 | 未验范围 | 验证缺口 | 不能报通过，也不自动算发现缺陷 |

原第1项是复核说明，第35项是证据索引声明，不是两项额外缺陷。

## 关键代码依据

- 旧证详情入口及校验：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateQueryServiceImpl.java:125` → `service/accesspolicy/DccRegistrationCertificateAccessPolicyService.java:107`（该service路径同属registrationcertificate）。先查询租户/公司范围，再校验版本预览资格；OLD走主管角色或有效授权。
- 旧证状态解释：`IntRuoyiFronted/src/views/dcc/registration-certificate/detail/index.vue:29`、`shared/FactsFields.vue:6`。
- 历史节点中文展示：`IntRuoyiFronted/src/views/dcc/registration-certificate/history/EvidencePanel.vue:11` → `IntRuoyiFronted/src/views/bpm/processInstance/detail/display-name.ts:11`。
- 下载正文补齐：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/approval/DccRegistrationCertificateApprovalDetailController.java:44`；摘要构建：`IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/NativeApprovalListFilterSql.java:160`、`:173`、`:208`。摘要读取流程变量，正文补值不会自动修改这些流程变量。
- 流程可读资格：`IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/task/BpmProcessInstanceController.java:187`、`:267`。接口权限与流程实例资格均需满足。
- 按月提醒：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/reminder/DccRegistrationCertificateReminderService.java:251`。
- 变更确认收件人保存校验：同模块 `service/config/DccRegistrationCertificateConfigService.java:219`。
- CE通知模板迁移：`IntRuoyiBackend/sql/mysql/20261120_dcc_registration_certificate_neutral_event_notify_template.sql:1`。

## 证据文件与验证限制

- 原始总索引：`workspace/协调部/注册协调员/output/注册证完整问题清单-20261121.md`。
- 原下载摘要只读证据：`workspace/协调部/注册协调员/output/R03/R03-主管下载页面只读核验.md`。
- 原权限/失败定位：`workspace/协调部/注册协调员/output/20261119-注册五项问题-只读复现与定位.md`。
- 本次配置截图：`workspace/registration-confirmation-config-20261122.png`。
- 本次旧证拒绝截图：`workspace/registration-confirmation-old-access-20261122.png`。
- 本次已办错误页面快照：`IntRuoyiFronted/.playwright-cli/page-2026-11-21T16-46-41-137Z.yml`。文件名使用工具自身时间，仅作为文件标识，不据此断言业务事件发生时间。
- 本次只读确认不等于完整E2E验收。原申请人/主管/文控账号权限、原错误请求、自然到期调度、CE延续闭环仍缺直接验证；未用管理员结果代替这些身份。


## 按“修复发现的问题”实施结果（2026-11-22追加）

本节是后续修复结果，不改变前面“先确认”阶段的事实记录。用户随后明确人员和期限可以通过现有入口配置；该项保留为配置事项，不再作为待修程序缺陷，也未擅自保存人员或期限。

### 已实施：578历史下载摘要补齐

- 测试1（tenantId=186）申请578，准确绑定流程 `843d37bb-e435-11f1-a7e4-00155d046496`。历史流程已结束、申请APPROVED、无运行执行实例；原流程根变量缺少公司和证名。
- 使用一次性修复工具 `scripts/repair/repair-registration-download-summary.py`，先只读生成修复计划，核对租户、流程定义、业务绑定、请求文件集合及申请时点版本快照后，只新增两个缺失历史展示变量：`ownerCompanyName=上海七木医疗器械有限公司`、`certificateName=按压式压力泵`。未加入运行期兼容或兜底逻辑。
- 公司来自该申请所属公司引用的修复时名称，证名来自请求文件990819441所属版本990824371在申请时点已有快照990824390。证据明确区分来源，不冒称原提交正文已有这两项。
- 修复后只读重查，两变量齐全；逐字段比对原申请整行与备份完全一致，未修改原申请正文、状态、审批结论、授权或文件。589等其他申请未改动。
- 原始数据、插入变量准确ID和回退边界留存于 `workspace/registration-download-578-repair-20261122.json`。工具默认只读，执行写入必须显式指定精确对象和独占证据文件；已有值冲突、资料缺失、跨租户或流程未结束直接失败。
- **数据库修复已实施并核验；页面摘要尚未验收通过**。正常“审批中心→已办”入口仍被下面的独立QA数据问题阻塞，不能用数据库结果冒充页面E2E通过。

### 已定位并修改明确报错：已办列表异常

- 本地运行后端控制台堆栈指向 `MesManagerReleaseCompletedActorResolver.readVerifiedQaReview`，经 `MesEdhrApprovalOutcomeReader.readAuthorizedHandoff` 和 `MesActiveOrderHandoffApprovalProjection.summary`进入审批汇总。
- 完成的QA_REVIEW交接任务2123151543621255168（tenantId=186）引用QA评审134；按ID只读查原始 `mes_pro_edhr_nonconformance_review`，该记录不存在。缺少原评审及可验证签名来源，不能由DONE交接状态推造审批通过，也不能跳过该条记录伪装完整已办列表。
- 已修改源码：缺少原评审时返回明确业务错误，包含评审编号及恢复原始评审、签名证据的要求，避免只显示“系统异常”。其余租户、内容、状态、签名校验继续保留。
- **当前列表未恢复**：需要从真实备份或原始证据恢复评审134及其关联完整证据；本次没有这些恢复输入。代码修改未部署，未重启共享int_main后端，当前页面仍报旧“系统异常”。
- 本次新浏览器会话 `regfix1122` 经真实前端登录测试1管理员，从菜单进入已办再次看到异常；截图 `output/playwright/registration-done-blocked-20261122.png`。本次未提交、批准、撤回或下载业务申请。

### 实际验证

- Python单元测试3项通过；真实MySQL 8集成测试3项通过。集成数据全部使用连接局部TEMPORARY表，与业务表隔离；验证只读不写、准确插入与重复执行、原正文状态不变、阻止运行中流程/缺文件/跨租户/错误绑定，以及证据文件禁止覆盖。
- Maven最终使用 `mes-edhr-work-routing-tests` 配置，`MesEdhrHistoricalReadContractTest` 31项、`MesEdhrApprovalOutcomeReaderTest` 53项，共84项通过、零失败零错误。新增缺失评审明确失败且不读取签名的回归；原测试仍mock旧prefix入口，已更新为实际window入口，保留原业务断言。首次使用另一profile未编译目标测试，读到旧测试类报错；最终已用正确profile重新编译执行，不能将首次结果视为通过。
- Maven输出：`workspace/registration-feedback-mes-tests-20261122.log`。本次任务相关源码 `git diff --check` 通过（Git换行格式提示不影响检查结果）。
- 此前已完成的日期反向查询预校验、续证必填与错误展示、上传弹窗关闭顺序修改保留；此前48项前端针对性检查及类型检查已通过，本次未再次修改或重复执行。未因此宣布CE延续完整链路、旧证权限、历史意见等未验事项已关闭。

## 继续修复与页面验证（2026-11-22追加）

### 当前页面通过的日期校验

- Playwright会话 `regfix1122`，测试1管理员，从真实菜单进入注册证。国内首次获证日期2026-11-21至2026-11-20查询显示具体起止错误；CE有效期反向查询同样正确提示。被拦截的反向日期未发送查询请求，控制台保持0 errors、原有1 warning。
- 国内延续留空批准日、生效日和有效期，点击提交后显示三项必填提示，生效日星号已显示。填写批准日晚于生效日并失焦后显示日期顺序错误，没有新增未捕获Object/Error。本次没有完成任何延续提交，也未据此宣称CE延续闭环通过。
- 截图：`output/playwright/registration-domestic-reverse-date-20261122.png`、`registration-ce-reverse-date-20261122.png`、`registration-domestic-renewal-required-20261122.png`（后两文件同目录）。

### CE通知文案：生效租户模板已修正，发布迁移已补齐

- 确认真实链路：业务事件通知器提供产品、事件、证号、生效日和有效期五项参数 → 通知服务 → `NotifyTemplateDO`实际映射 `system_tenant_notify_template`；`system_notify_template`仅供租户初始化复制。此前只查初始化表，不足以判断租户实际生效模板。
- 原20261120迁移仅更新初始化表name/content，未同步params，也未更新已存在租户模板。这能导致租户继续使用“国内注册证事件”，以及正文与参数契约不一致。未改写旧迁移，新增 `20261122_dcc_registration_certificate_event_template_params.sql` 并登记发布源，明确依赖原中性文案迁移及租户通知资源迁移。
- 新迁移同时更新初始化模板和已有租户同编码模板的name/content/params，保持启用状态、发送人、类别和备注。先检查前置条件，事务内更新，失败回滚并重新抛错；不修改已发送消息。尚未向业务库执行整项发布迁移，因此其他租户和初始化源表不声称已同步。
- 已通过正常“模板管理”页面保存：测试1tenant186模板4124，以及先前操作的芋道源码tenant1模板6774。测试1原始整行备份 `workspace/registration-event-template-test1-before-20261122.json`；保存后只读逐字段比对 `workspace/registration-event-template-test1-after-20261122.json`。除name/content/params及管理页面更新人、时间外均不变。
- 当前模板正文：产品《{productName}》的注册证{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。五个params与正文占位一致。
- 截图：`output/playwright/registration-test1-neutral-template-saved-20261122.png`。没有发送测试通知，没有重放575事件，没有改收件人；这证明模板保存和落库，不证明全部应收人员均收到通知。
- `scripts/tests/registration_event_template_mysql_check.py`真实MySQL 8临时表检查最终5项通过：初始化/多租户参数同步、无关项保留、重复执行、缺失/重复源模板拒绝、缺租户模板拒绝、租户更新失败时源模板回滚。迁移政策检查按17项完整依赖输入后通过，输出 `workspace/registration-template-migration-policy-20261122.json`；最初只输入新增SQL缺依赖的失败不是SQL执行结果。发布源登记检查1项通过。

### 572、573历史业务留档意见已准确补齐

- 进一步核对原任务局部变量，找到大小写精确名称 `TASK_REASON`，无需从中文格式化评论中截取或推测。572绑定534、573绑定535原reviewComment均为null，原审批人均为9908101013，原审批任务和完成时间对应一致。
- 一次性工具 `scripts/repair/repair-registration-review-comment.py` 默认只读；严格校验租户、原申请、绑定业务键、流程定义、审批完成状态、唯一任务、审批人、时间和原TASK_REASON，并以原审批评论原文交叉确认。只补reviewComment；已有冲突值、运行中流程、缺少来源或证据文件已存在均明确失败。
- 两项均已实施。原始绑定、申请及流程证据分别保存在 `workspace/registration-review-572-repair-20261122.json`、`registration-review-573-repair-20261122.json`。只读重查逐字段证实：原申请整行不变，原任务/变量/评论不变，业务JSON仅reviewComment一项变化；汇总 `workspace/registration-review-repair-verification-20261122.json`。
- 真实MySQL临时表3项检查最终通过，覆盖只读、准确补齐、幂等、不覆盖现有意见/证据、身份/租户/状态错误、运行中流程、缺原意见及交叉评论冲突拒绝。首次测试因检查自身SELECT未结束事务而失败，修正测试事务边界后3项通过，未放宽修复校验。
- 正常页面从注册证编号筛选进入990823626当前详情，打开573“查看本次申请与审核”，原意见完整显示；审核节点显示中文“注册证访问审批”。截图 `output/playwright/registration-review-573-restored-20261122.png`。当前详情仅列所属版本申请，572未在本次页面验收，不用573替代572页面结果。
- 上述数据库维护是已授权的数据修复，未计作通过前端执行的业务E2E；页面验证仅查看修复后的真实记录，没有重新批准历史申请。

### 先前已办列表恢复阻塞（下节已更新）

- QA134关联电子签名64662及CREATE/DISPOSE操作审计仍存在。签名保留原聚合哈希，但未保留完整评审payload；操作审计能佐证评审编号、来源、处置等，仍不足恢复完整原评审行及追溯快照。
- 已查的本地备份早于该评审创建；没有取得包含评审134完整行及原追溯字段的备份。不能据剩余字段拼造原始记录，也不能跳过该任务使已办列表看似正常。
- 已完成的明确错误提示源码修复保留，84项后端检查结果不变；未部署、未重启共享int_main后端，已办列表尚未恢复。578摘要数据库修复已核验，其正常已办页面验收继续受此问题阻塞。
- 人员/期限由现有配置入口设置，按用户回复保留为配置事项；原账号权限、未复现请求异常、CE延续完整闭环和原清单第34项仍不能自动判通过或按猜测修改业务规则。
- 补充恢复线索：目标MySQL开启ROW/FULL二进制日志，仍保留多个binlog文件，可能含QA134原行。目前Windows命令路径及已确认MySQL容器内均未提供 `mysqlbinlog`；尚未解析这些日志，不能断言已找到或已丢失原记录。继续恢复需要可用的原始binlog解析工具及相应日志覆盖证据；未安装工具、未停库、未回放日志。

### 上一阶段交付边界（以下文追加进展为准）

| 项目 | 当前结果 |
|---|---|
| 国内/CE反向日期查询 | 已修代码；当前页面负例通过，无新增未捕获异常 |
| 国内续证必填/日期顺序 | 已修代码；本次空日期提交和逆序失焦通过，未提交业务申请 |
| CE通知称国内 | 测试1和芋道源码当前模板已通过页面修正；租户与初始化表发布迁移已实现并验证，整库迁移未执行；通知送达未验收 |
| 578历史下载摘要 | 两项展示变量已补齐并只读核验；正常已办页面仍受独立数据缺失阻塞 |
| 572/573留档意见 | 已从唯一原任务意见补齐并只读核验；573页面原意见可见，572页面未验收 |
| 历史英文审核节点 | 573原节点已显示中文；不声称所有旧节点全量验收 |
| 已办列表缺QA134 | 明确错误提示源码已修，84项检查通过；未部署；原行恢复受原始数据与binlog工具条件限制，列表未恢复 |
| 其他历史现象/权限规则/未验范围 | 保留原分类；没有证据的根因不擅自修补，不计全部通过 |


## 原始QA证据恢复与最终边界（2026-11-22再次追加）

- 已取得可校验的MySQL ROW/FULL二进制日志，使用项目内固定版本解析依赖，只提取本次22个目标ID；每个事件CRC32、原生命周期及完整38列交叉核对。合并原始证据：`workspace/qa22-original-binlog-evidence-20261122.json`。没有全库回放。
- 已恢复21份原已关闭评审：109—117、122—132、134。每份取原DELETE完整行像，严格对应创建、处置更新、删除；核对唯一原交接、原审批人、时间、签名聚合摘要、签名content_hash和evidence_hash后，完整38列插入。没有重放审批、生产状态或通知；原电子签名整行、原交接任务整行均保持不变。
- 修复工具 `scripts/repair/restore-qa134-original-row.py` 默认只读，限定本次目标与租户，现存冲突不覆盖，证据文件独占创建；仅允许原已关闭且有匹配签名的记录。独立真实MySQL临时表4项检查通过，覆盖原行恢复、幂等、原行/签名/交接冲突拒绝与证据不覆盖。
- 逐字段只读复核：`workspace/qa21-original-restoration-verification-20261122.json`；各ID完整维护证据：`workspace/qa<ID>-original-row-restored-20261122.json`。所述签名核对范围为原协议摘要重算和原行不变，不冒充整个应用的签名验签审计。
- 来源缺失审计由86条交接/22份评审降至2条交接/1份评审133。133原为pending_review，无处置、closed_at、trace_snapshot或QA创建签名；原QA任务已CANCELED。另一聊天独立核查原周期1009200637及后续周期1009200638均不存在，工单仍在；恢复pending会被冻结统计纳入，可能重新冻结工单并产生无法办理的评审。
- 因此133保留原始binlog完整行，不写回在线表，不补造通过、签名或处置，不删除其两条历史引用；这是尚存历史完整性异常，不计作已关闭，也不能把QA21份恢复写成22份全量恢复。
- 防复发数据库保留约束由已授权协调聊天负责，已本地应用并通过其独立6项MySQL检查；本聊天没有重复修改、重复应用其SQL或检查工具。
- 最初标准启动因缺少包provenance被阻止；未绕过、未临时构建并行改动。随后发现另有受控构建产物，`backend-package.py verify`已通过源码与包校验；确认无Java/并发启动后，用原标准入口统一启动，再次双重验证并制作只读运行副本。运行结果和真实页面结论继续记录在下方。
- 提醒人员和期限按用户回复由现有配置入口设置，当前没有明确人员值，不猜填。CE延续闭环、旧证权限、未复现异常及清单第34项仍按原分类保留。


### 受控运行与新确认的签名身份问题

- 后端通过标准入口恢复，来源校验两次通过，运行PID29920，48081监听。日志：`workspace/registration-controlled-backend-start-20261122.out.log`。使用Playwright原会话regfix1122，从审批中心真实菜单进入已办，服务就绪后重载；仍显示系统异常，不能写页面PASS。
- 新故障为QA134所属已办任务2123151543621255168的“QA closure signature identity is inconsistent”。原签名64662.authentication_method为SESSION_PLUS_SELECTED_USER_PASSWORD；签名创建入口MesProBatchRecordExecutionSignatureService调用signAuthorized，ElectronicSignatureServiceImpl明确冻结SYSTEM_USER、signerId、operatorId、tenantId，并采用SELECTED_USER签名方式。
- 消费端MesManagerReleaseCompletedActorResolver.requireQaSignature却仅接受SESSION_PLUS_PASSWORD/PIN，误拒正式指定系统账号签名。已仅为QA处置读取接受两种明确系统账号上下文；租户、签名人、subject、原意见、摘要以及签名内核的冻结身份/完整性检查保持。未更改全局凭据策略、原签名或历史审批事实。
- 新增回归覆盖本人/指定系统账号PASSWORD/PIN通过，员工身份/会话确认/未知方式拒绝，指定账号缺冻结身份拒绝。首次新增通过样本遗漏冻结身份，正确被原协议拒绝；已修正测试样本携带真实协议身份，没有放宽协议校验。

- 签名上下文修复最终回归54项通过：MesEdhrApprovalCenterQueryContractTest 13项，MesEdhrHistoricalReadContractTest 41项（包含新增10项）。无失败、错误或跳过；日志 `workspace/registration-selected-signer-tests-20261122.log`。此次精确运行两类测试，与此前84项不同，不把两个数字混算。
- 已启动标准受控构建（backend-package.py build），原后端从只读不可变副本运行，构建不会覆盖在用JAR。未提交、未推送。

- 新受控包构建最终成功（3分25秒），源码与制品校验通过；构建日志 `workspace/registration-selected-signer-build-20261122.log`。严格核对本聊天启动的PID29920和不可变JAR路径及48081监听归属后，只停止该自有进程，再从原标准入口启动新包，启动前再次两次来源校验通过。新日志 `workspace/registration-selected-signer-backend-20261122.out.log`。


### 本轮最终页面验收结果

| 项目 | 实际结果与证据 |
|---|---|
| 受控后端 | 标准来源校验通过，PID71096，48081运行；未跳过provenance，未覆盖在用JAR |
| 测试1管理员已办 | 真实菜单进入，首屏20条、点击下一页余4条正常，共24条；QA134可读，无原来源/签名身份异常；快照 `workspace/registration-done-signer-fix-20261122.txt`、`workspace/registration-done-restored-page2-20261122.txt`，截图 `output/playwright/registration-done-restored-20261122.png` |
| 578历史摘要 | 从用户管理只读查得原审批人徐翠xucui，以现有测试凭据通过前端登录测试1，进入审批中心已办，原578摘要显示上海七木医疗器械有限公司、按压式压力泵；原流程ID843d37bb-e435-11f1-a7e4-00155d046496一致。截图 `output/playwright/registration-download-578-summary-restored-20261122.png` |
| 578申请正文 | 从原578行点击查看，requestId=578，当前正文公司、证名与摘要一致，已批准状态和原文件名可读；控制台0 errors、1 warning。快照 `workspace/registration-download-578-detail-verified-20261122.txt`，截图 `output/playwright/registration-download-578-detail-restored-20261122.png`。未下载、未审批或重发申请 |
| QA133 | 原始22份日志全部留存，在线仅恢复21份原已关闭签名评审。133及其2条历史引用保留未关闭，不恢复可能冻结工单的待评审，不改造历史决定 |
| 未验事项 | 原账号权限矩阵、CE延续闭环、自然提醒送达和清单第34项等仍不计通过；提醒人选与期限由现有配置入口设置 |

- 最新状态以上表及本轮原始证据为准，前文阻塞记录保留排查过程，不再作为当前已办和578页面仍失败的结论。所有维护均为本地已授权范围；未提交、未推送或发布远端。


## 原记录专项复核与578权限修复（2026-11-22最新追加）

本节优先于前文过时状态；复用R04报告，不重复新建、审批、下载，也未改589、603、605。人员与期限按用户回复保留为现有配置事项。

### 原对象直接证据

| 原编号 | 原对象与本次实际动作 | 最新结论与限制 |
|---|---|---|
| 2 | 徐翠原审批账号，个人中心刷新及整页重载；sources/context/tasks/page均HTTP200、code0 | 本次未复现工作台失败。原594期间失败URL、响应和堆栈仍缺失，根因未关闭；不把当前读取成功算原审批时序回归 |
| 3 | 原592所属旧v1=990825221，打开“查看本次申请与审核” | 原审批人、时间、通过及意见完整，节点中文，无新Axios/pageerror；该弹窗本次用已加载数据，打开时没有新增请求。原Axios来源仍未定，不声明全部请求异常已修复 |
| 6 | 原579旧v1=990824372：原列表与同版详情 | 列表“已失效”，详情明确“已被替换旧版本”并将“有效”标为“证载状态（历史记录）”。原显示歧义的页面复验通过；失效日期仍2027-11-19，原到期日/替换日规则未擅改 |
| 11 | 原576、592留档及原574流程，另有此前573留档 | 实际显示“注册证访问审批”；只证明这些原记录当前显示已改善，不声称所有历史节点已全量核实或数据库英文被改写 |
| 13 | 黄泽宇通过“我发起的”打开原578流程843d37bb-e435-11f1-a7e4-00155d046496 | 本次仍复现HTTP200/code403“没有该操作权限”。已确认是本人流程，原START_USER_ID=9908100500、租户186。上传角色只有bpm:task:query，缺通用流程查询权限，接口前置拦截先于发起人资格检查 |
| 14 | 徐翠正常已办入口打开原574流程5542cff6-e365-11f1-bcdd-00155d12d49b | 原通过记录、审批意见及中文节点可读，相关两接口code0。原未签收候选状态已经结束，无法重建当时分支；另一个现存候选流程能读也不替代原分支验收。当前候选资格采用同一待办查询口径，回归覆盖已通过 |
| 17、18 | 复核上传弹窗关闭→saved顺序及当前submitReview错误分支 | 已有关闭后再跳转的源码修复和R04新样本成功证据；当前业务拒绝明确提示、超时明确待确认的测试已存在。572旧灰遮罩及567/574旧无反馈的当次时序缺失，未重放原审批，历史根因仍未定 |

### 578本次代码变更与验证

- 未给上传角色补通用bpm:process-instance:query：该权限同时用于通用流程发起，超出本人注册证流程读取需要。先前准备的角色授权迁移和临时检查已撤回，未应用到任何业务库、角色或套餐；release-sources保留此前通知模板迁移，无新增权限迁移。
- 仅审批详情接口接受既有审批中心查询资格；缺通用流程查询权限时，必须是同租户、本人发起、注册证专属流程。无实例的流程预测、其他业务、他人实例、跨租户均拒绝；通用流程发起接口及其他接口权限不变。正常查询用户仍经过既有流程参与人/当前任务资格检查。
- 16项后端检查通过：BpmProcessInstanceCandidateAccessTest 11项、ApprovalProcessTaskAuthorityTest 5项，零失败、错误、跳过。包含新增7项本人读取及权限边界。日志：workspace/registration-applicant-process-read-tests-20261122.log。构建和原578页面验收结果见后续追加。
- 本次无新增菜单、权限标识、前端页面或路由；不扩大角色、公司、文件授权。

### R04复用及仍未关闭的范围

- 复用output/playwright/R04-修复与验证报告-20261122.md：新模拟证990824361的606→607→608→609已完成新建、仅换SCAN延续并承接附页、直接改号、申请下载；这是修复后新链路证据，不替代原CE延续未取得申请号的根因取证。原598当前v2缺ANNEX仍在，已由该报告保留单证方案，本聊天不重复写入。
- 已修并有页面证据：反向日期查询、国内续证空日期/逆序校验、旧证状态含义区分、578历史摘要和正文、573原意见、天/月文案一致；通知当前模板已修并有发布迁移，不能据此证明全部应收人送达。
- 仍需原始证据：594工作台失败、592原Axios、572灰遮罩、567/574旧无反馈、593表单中断、旧500/503、旧绑定异常和R03上午提交异常。缺当次请求/堆栈的原因保持待查，不编造根因。
- 仍需业务或资格证据：原文控旧证是否已有授权、失效日期口径、CE通用编号筛选适用规则。提醒人员和期限可配置，但具体值未提供，不猜填。
- 未验范围继续保留：完整角色/公司/文件矩阵、自然到期调度及颜色恢复、支持材料自动校验、未来生效切换、全部变更及隐藏字段、非三流程上传入口、下载次数/截止/跨文件控制等。未验不计缺陷，也不计通过。

### 本轮证据索引

- 原579列表/详情/请求：workspace/registration-579-original-old-list-20261122.txt、registration-579-original-old-detail-20261122.txt、registration-579-original-old-requests-20261122.txt；截图output/playwright/registration-579-original-old-status-20261122.png。
- 原576：workspace/registration-576-original-archive-20261122.txt；截图output/playwright/registration-576-original-archive-chinese-20261122.png。
- 原592：workspace/registration-592-original-old-detail-20261122.txt、registration-592-original-archive-20261122.txt、registration-592-original-archive-requests-20261122.txt；截图output/playwright/registration-592-original-archive-20261122.png。
- 原578错误：workspace/registration-origin-initiated-20261122.txt、registration-578-original-applicant-flow-20261122.txt、registration-578-original-applicant-requests-20261122.txt；截图output/playwright/registration-578-original-applicant-flow-denied-20261122.png。
- 原574：workspace/registration-574-original-completed-flow-20261122.txt、registration-574-original-flow-requests-20261122.txt；截图output/playwright/registration-574-original-completed-flow-20261122.png。
- 徐翠当前工作台：workspace/registration-xucui-workbench-current-requests-20261122.txt、registration-xucui-workbench-reload-requests-20261122.txt。


### 572原留档补验与受控包

- 从徐翠“注册证→老证”进入原990823626旧v1=990824369，原申请572“查看本次申请与审核”已显示准确原意见：“已从系统审批正文在线核对模拟附件及公司、证号、项目、DCC产品0313170202、分类和日期。本次仅同意本地注册流程模拟测试；非真实注册证、非监管批准、非真人亲签。型号规格为空，后续变更另行审核。”审批人徐翠、原时间及通过结果不变，节点中文。此前572页面未验收的缺口已补齐；573已在前轮验证。
- 证据：workspace/registration-572-original-archive-restored-20261122.txt；截图output/playwright/registration-572-original-archive-restored-20261122.png。只读打开留档，没有重新审批。
- 578修复受控构建成功（4分25秒），源码和JAR来源校验通过；日志workspace/registration-applicant-process-read-build-20261122.log。核对本聊天旧PID71096、不可变JAR及48081归属后，仅停止该进程；新标准启动入口再次两次来源校验通过，Java PID42940，仍在启动。其他进程未停止，未提交、推送或发布远端。


### 578最终页面结果（本轮最终状态）

| 项目 | 实际结果 |
|---|---|
| 当前运行 | 标准受控包来源校验通过，Java PID42940在48081监听；本聊天旧PID71096已按归属核对后停止，未停止其他进程 |
| 原申请人入口 | 黄泽宇登录测试1，从“审批中心→我发起的”找到原578，摘要公司/证名齐全；点击该行“流程” |
| 原流程 | URL仍为843d37bb-e435-11f1-a7e4-00155d046496；get-approval-detail实际HTTP200/code0，页面显示审批通过、原正文所属公司/证名/文件及原意见 |
| 页面权限与错误 | 本次打开控制台0 errors、1 warning，无新pageerror；可见操作仅在线查看等，没有审核、同意或驳回按钮。没有重新申请、审批或下载 |
| 回归 | 16项后端检查通过；受控构建成功，源码与制品校验通过；本次相关源码diff空白检查通过 |
| 未扩大授权 | 角色/菜单/租户套餐未写入；仅既有审批详情接口允许审批中心用户读取同租户本人注册证流程。通用发起权限、其他实例及文件授权不变 |

证据：workspace/registration-578-original-applicant-flow-restored-20261122.txt、workspace/registration-578-original-applicant-flow-restored-requests-20261122.txt；截图output/playwright/registration-578-original-applicant-flow-restored-20261122.png。启动日志workspace/registration-applicant-process-read-backend-20261122.out.log及.err.log。

**本轮确认关闭：原578本人流程误拒；补齐原572意见页面验收；原579旧证状态显示歧义页面验收。原574已结束流程可读、原576/572/592中文节点、原592留档及徐翠当前工作台均有当前原对象证据，但不据此关闭旧未捕获请求的根因或候选未签收历史分支。剩余事项以上节边界为准，不宣称全部历史异常和未验范围无问题。**


## 剩余项统一索引（2026-11-22）

已按原反馈2—34共33项及原88条逐项归并至[注册证剩余核验与归属清单](<D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/注册证剩余核验与归属清单-20261122.md>)。该清单复用R04和最新610页面/消费审计证据，明确每项已覆盖、待验、缺原证据、业务口径及归属；不把旧CSV整体改判通过。原598缺ANNEX仍由R04待单证历史写入授权，本轮未补写、未重批或重复新建业务测试。最新具体剩余边界以该清单为准，原证据过程保留。

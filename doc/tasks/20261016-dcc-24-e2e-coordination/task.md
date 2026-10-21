## 2026-10-21 当前监督范围

blocked — 本轮新文件延迟生效、并发指派及培训上传定向验证已通过；原间歇性超时/死锁根因仍OPEN。隔离死锁方案与脚本已review、驱动已私有准备、离线check PASS，等待用户明确授权新测试库dcc_e18_lock_test_20261021的验证及清理。无建库/DML、生产变更、重启或Git提交推送。详见new-files-review-20261021.md。

## Current Status

blocked — 本轮新文件延迟生效、并发指派及培训上传定向验证已通过；原间歇性超时/死锁根因仍OPEN。隔离死锁方案与脚本已review、驱动已私有准备、离线check PASS，等待用户明确授权新测试库dcc_e18_lock_test_20261021的验证及清理。无建库/DML、生产变更、重启或Git提交推送。详见new-files-review-20261021.md。

### 范围变更前的阻塞快照（历史，不再作为当前门禁）

### 旧监督范围

blocked — 连续三轮复核同一阻塞未变化，停止自动续轮，目标未达成。恢复条件：确定全局调度实际影响对象及允许范围、取得可靠历史流程原件，或补回原超时/锁等待现场证据。已通过业务保留不重跑。E03累积业务PASS、E09既有PASS及E22 Main/R6/非空附件隔离证据保留不重跑。E15调度隔离最小修复28/28、Root review及候选包内容核验通过，已仅更新int_qms后端32436/48061 healthUP（frontend20976/8061不变）；原key/2映射/schema=false/startup-sync=false保持。真实调度运行态仍待原流程证明，历史V4源资源漂移仍独立未关闭。E17原R3延迟夹具仅放行一次真实页面只读前置核对，未签收/重提/触发job。最新e15-isolation-runtime-proof.json；全24目标未达成。无Git提交推送。

### 旧状态

blocked — 2026-10-21 第三轮阻塞复核：E15及E13原线程cursor不变、idle/completed，流程源报告和调度页面报告无新增证据；无可等待执行或可安全触发的限定范围。需外部证据或执行范围变化后恢复，保持全24目标。持续监督未闭环问题，已通过业务不重跑。当前后端32436/48061运行backend-e15-isolation-20261021.jar（SHA BB73D941…930321），healthUP；前端20976/8061不变。E15逐文件隔离及真实失败传播28/28定向测试、Root review和打包内容核验通过，真实调度运行证据仍待核对。E03原4392/A2、新旧正文及4附件隔离已接受；E22历史正文及AttachIso非空两版本附件隔离已接受；E09定向关联搜索及E17/E24既有证据保留。E17本次4418/A1只读确认已生效并通知三人，计划与实际相差1小时28分16秒，发生于新包之前，不能作为新包修复证明；无签收重放。E15调度只读窗口已关闭：旧48167成功状态含异常，新包执行未获证明；原U正确4417/A1当前终态及正文已接受，实际晚于计划4842秒，不能归因新包。E17的16项已有4369同分支PASS，无新增签收需要。各线程HOLD，不触发任务。历史V4资源漂移及未定位的旧请求异常独立保留。各线程gpt-6.1-sol/low，不改历史数据、不扩权、不提交推送；全24目标未达成。

## 2026-10-20 四轮后定向修复（历史）

blocked — E12四项补验已封存，剩余02/03/13/14/18历史现场证据不足连续3轮未变化。第三轮实际线程工具再次确认五者idle/completed，相关任务无新增现场，日志仍止于20:56且不含原关联证据；不存在可等待的执行或有证据支持的必要修复。根目标按阻塞门槛停止自动续轮，未达成，不降低全24验收目标。解除条件是补回旧临时件可追溯终态、原请求终结/耗时/中止现场及死锁双方锁环，或原异常再次出现时捕获这些证据。保留所有已通过结果，不重复业务、不猜测修改、不提交推送。

最新补证：E12 PUBLIC四项附加边界已全部真实UI通过并Root独立接受，入口../20261016-dcc-e2e-12/public-boundaries-20261020-2111/handoff.json。4385/A1最终仍0/2；仅提交、主管及必要D1各一次，D2/C正式签名0。5trace CRC/SHA/令牌检查及实际关闭通过，两个离线SCRIPT错误保留；未改生产，旧四轮计数不覆盖。历史02/03/13/14/18证据不足问题仍在，目标未完成。

最新补证：E23音视频普通附件专项6身份/媒体组合及2次真实下载字节一致已Root验收PASS，新4384/A1已生效，10trace隐私/CRC与正常关闭通过，33源码不变。初次名称占用400、导航/控件SCRIPT和扫描占位符误报均留档，不重跑旧PASS、不改产品凑通过。该缺口现已覆盖；历史02/03/13/14/18不确定项仍按下文保留，不能宣称24全部问题已解决。

新增检查点：E18两新申请4382/4383各一次并发指派正常，实际事务重叠/tenant账本竞争但stage间隙不同、149帧无新deadlock；正常关闭与13份trace SHA/CRC/空网络载荷经Root复核，原R2-08仍OPEN，未据猜测改锁序。此前遗漏的音视频附件专项现已由上文E23新证据关闭，旧NOT_RUN保留历史属性。根目标保持in_progress，不能因正文媒体已排除而省略附件权限验收。

in_progress — 目标仍为E2E01–24发现问题修复并通过。原四轮18 PASS/3 FAIL/3 BLOCKED是不可覆盖的历史，当前结论须合并后续独立复验：08两条取消切换已PASS；12 PUBLIC/CUSTOM重提31项及阶段身份33步已PASS；23 A→B迟到响应隔离已PASS；24真实MySQL并发RED1213/40001、GREEN3项及原Config18 DWG A/1阅读启动/124→134秒定向UI已PASS，专库/账号和浏览器均已清理。06原合同按正式UI隔离已覆盖，非法HTTP输入不冒充页面动作。14错误展示59项PASS，原#1198历史正文/详情只读复验PASS（212→221、VALID，无残留弹框），原提交瞬间中止原因仍UNKNOWN。02剩余三项PASS，但旧临时件标识/终态无法恢复，停止无据追查；03等待器正确但响应终结事件缺失，18死锁缺完整双方锁环，均不得据猜测修改。13原Axios30秒超时慢因仍OPEN；原待培训4218单次真实确认补证已正常（唯一POST约3.752秒finished、弹窗关闭、原U培训附件及ACTIVE/A1匹配），不把未复现写成历史缺陷修复；浏览器/runner已关闭。已通过不重跑、不重签/重删，未提交推送，不以工程blocked等同业务失败，不宣称目标完成。

## 2026-10-20 新四轮顺序重跑 R2（历史）

blocked — 用户要求按顺序每次6例重跑4轮，已真实执行01–24并Root逐例审核，结果18 PASS/3 FAIL/3 BLOCKED。汇总、问题及逐例handoff见rerun-four-rounds-20261020-r2.md、rerun-r2-open-issues.md和rerun-r2-dispatch-ledger.json。服务使用冻结Jar 709e78bd…，四轮结束后仍一致；各线程浏览器/context/runner均关闭。E23 A→B迟到响应仍失败；02清理未知、06非法跨项目反向边界未触发、12 PUBLIC检入失败等未关闭。真实MySQL并发未获准执行。无产品/数据库/服务变更、Git提交推送或cleanup apply；工程按仓库规则保持blocked，不标completed。

## 2026-10-20 四轮完整重跑（历史）

### 四轮后恢复持续修复目标

in_progress — 用户持续目标已恢复为监督原线程修复01–24并全部验收通过。四轮结果14/4/6原样保留；修复分工见repair-after-four-rounds-20261020.md。只修直接根因，未通过分支定向复验，Maven/运行更新由Root协调；无Git提交推送。

blocked — 本次01–24每轮六例、四轮顺序重跑及结果收集已交付，14例合同通过、4例未通过、6例阻塞；03/08/23附带问题保留。全部原线程及测试浏览器已关闭。结构核验PASS：24行、24独立证据入口、缺失0、23 JSON可解析。完整业务验收及工程归档仍未完成，不因汇总完成标全部PASS；无Git授权、cleanup命令不可用，未apply。权威入口：rerun-four-rounds-20261020.md。

## 2026-10-20 当前监督范围（历史）

in_progress — 2026-10-20用户已确认每人只能归属一个部门、音视频仅作为附件。E17原同人多部门前提移出本版本验收，E24音视频正式正文准入/翻页/搜索移出验收；附件查看下载权限要求保留，不将未经验证的音视频附件能力记PASS。交原17/24线程仅核对已有证据并更新结论，不重跑已通过业务、不改生产、不提交推送。七缺失格式原验收及历史失败保留，待Root最终核对整例结论。以下为历史。

## 2026-10-20 当前范围（R6）

in_progress — 完整目标仍为 E2E01–24 全部验收，不以定向测试代替真实UI。03/10/22既有本轮业务已接受；16-R7自然生效与原U18正文、5628恢复暂停经Root核对并接受，不重跑。12 PUBLIC 专项链已真实页面通过并经Root复核接受（文件4265，止于批准待办）；CUSTOM 不重跑。13 R6 关联PASS但正文读取在 bodyDenied 守卫被拒，尚未证明具体拒绝条件；原线程收到继续只读定位、最小TDD修复和失败分支复测指令，待新包放行后再复验。17同人多部门缺正式来源；24 PNG/TXT主链PASS但原件全字节无正式入口且WEBM/WAV口径仍缺，均不得标整例通过；17/24原线程已分别接手未通过范围，不重跑已有PASS。未提交推送、未改int_main。最新运行证明r6-runtime-proof.json。下列各轮为历史。

## 2026-10-19 第五轮当前范围

in_progress — 完整目标为 E2E01–24 发现的问题修复与原路径验收，保留已通过证据。03 后端20项、10后端22项已由Root核对；r5-reviewed-package构建exit0，独立运行副本230954/SHA3e0486e5…及本轮嵌套class逐字节核对通过。48061已从精确归属的25124切换启动19844，尚待health与真实页面登录后放行03/10/13/22/24。12继续排查真实项目列表可见性；16拒绝全局Quartz重构，先核对自有5627及4228幂等前提，新包后仅有界复用并精确恢复。17业务规则冲突保留，不改PASS。无Git提交推送。

## 2026-10-19 第四轮当前范围

in_progress — 最新用户授权原线程修复与定向复验。方案和资源归属见repair-dispatch-20261019-r4.md；17先独占Maven，随后Root放行03、10；03/13/22/24新包由Root统一。保留已通过分支；不提交推送。派发回执另记日志，未声称已修复或新包已发布。

# DCC 24个E2E线程启动与验收Review

## 2026-10-19 第三轮当前范围

in_progress — 最新审计后再次授权修复与定向复验，03/10/12/13/14/16/17/20/22/24十原线程均已成功派发 gpt-6.1-sol/high。以 repair-dispatch-20261019-r3.md 为最新分工：24先独占Maven、14类型检查、16任务自有精确定向调度。核心323与19旧测试等已GREEN，不沿用旧失败。已通过E2E不整体重跑。尚未宣称新包或本轮验收通过。

## 2026-10-19 全量问题审计后的第二轮定向派发

in_progress — 最新用户要求已由只读汇总转为原线程修复并定向复验。方案见 repair-dispatch-20261019-r2.md。已实际发送17个原线程，全部工具返回成功，模型 gpt-6.1-sol/high；03/10/13/16/17/22/24处理主要未闭环，06/12/14/19/23处理遗漏证据或测试/静态风险，02/09/15/20/21只整理报告。13/16/22/24已通过线程快照确认 active。已通过分支保留；共享构建由Root排期，13先执行后端测试，其他交接待放行。尚无本轮修复通过或新包部署结论。以下旧范围与状态均为历史记录。

## 2026-10-19 原线程恢复后的当前范围

in_progress — 用户要求对应原E2E线程修复未闭环问题、修后定向复验，已通过不再次整体运行。10/13/16/17/22/24六线程已实际恢复为gpt-6.1-sol/high，Root负责唯一生产文件归属、review、串行Maven及运行包；分工见recovery-dispatch-20261019.md。本机已恢复前端20976/8061、后端25124/48061，原120105包ba0a8ead…，health与真实UI登录PASS，32原文件/0待办启动前后相同，5627暂停；reboot1019-runtime-proof.json。新修复未部署：22有效RED4项3FAIL后最小GREEN5/5；13前端14/14且后端名称链有效RED1业务拒绝，正在原线程窄点实现；24限定PNG/TXT真实内容→PDF→原真章链六文件及显式字体资源方案，先后端行为RED，不实施音视频原生发布模式。16独立TIME19仅补严格前后时间证据。10原失败D无正式UI恢复路径、17同人多部门与单dept模型冲突单列，不重跑原通过分支。无Git提交推送。

## 当前持续目标：E2E16–24（2026-10-19）

以Current Status、e2e16-24-acceptance.md和dispatch.json为准，下列06–15段落保留历史。Root直接执行原账号/原对象UI集成（当前无原线程工具，不假称已发线程消息）。20原27参数、21原10参数、23原六参数已接受；18原草稿4246已送审4252/A2，S/L/C/A/D及U新培训完成并ACTIVE，正核对新旧附件正文；22原Main4249 ACTIVE、固定Rel1/后补Rel2已验证，余下引用/来源升级等；24原Office4240保持A1执行，4253/4254为工作稿，版本正文及114→115阅读隔离通过，四格式限制保留。R26两入口批量授权读取已部署并实际请求1.177秒/3.593秒；R27检入成功后的精确版本导航已31项及原UI回归；R28旧详情迟到通知已40项回归，待沿签名复验。运行30756/48061、49700/8061，060301/3b2de8ff…；无Git提交/推送，整体in_progress。

## 2026-10-18 当前范围：E2E06–15验收与修复

用户将持续目标扩展为06–15。权威范围、约束及检查点见e2e06-15-acceptance.md；06–10旧阶段说明保留历史。06、07、08已Root复核接受（06保留UI未触发跨项目HTTP提交边界），09原四分支接受。10 A/B/C/E证据已接受，D后端七类396次执行通过、前端最小入口实施中；14完整业务已Root接受，11/12/13/15保留原checkpoint待统一运行更新后恢复。运行态见e2e10-rework-matrix-runtime-proof.json。所有前置/业务仅真实UI；生产修改唯一归属、Root统一测试和运行更新；无Git提交推送。

## 2026-10-17 当前范围：E2E06–10验收与修复

用户新增要求继续完成06–10；执行计划见 e2e06-10-acceptance.md。01–05已完成业务验收，旧段落为历史，不重开。五个原线程已以gpt-6.1-sol/high恢复，Root负责问题归因、生产文件所有权、运行更新及最终证据review；本轮不提交推送。

## 2026-10-16 前置阻塞修复（当前范围）

### 2026-10-17 持续目标：完成01～05并监督范围

10:40续修：03文控最终签名实际失败DCC_CONTROLLED_EVENT_INVALID。直接源码和实际库证明受控时间微秒合同未同步到CONTROLLED事件/关系消费者：recordControlled拒绝任何小数，事件两表仍datetime0；延迟分支emit还用发布now而非实际受控签名S。只追该生产/消费/存储链，保留旧事件/签名/实例，不truncate整秒消错。BDD：Given真实精确受控事实S，When文控完成或延迟确认完成，Then事件精确携带S并按微秒保存和重放；错时间仍拒，历史原列值不变。Root负责定向迁移、Maven和运行更新，E2E15唯一补生产/消费者及专属RED/GREEN；03仅只读冻结现状直到修复放行。

用户要求观察执行线程，避免扩大化和过度防御；每次发现记录并打印，先根据直接证据辨明根因，再最小修复，沿原流程复验。用户睡眠期间不咨询已授权事项。原线程gpt-6.1-sol/high复用夹具执行各自完整用例；Root审核证据、统一生产修复/必要运行更新。禁止用宽权限或默认成功掩盖缺口，不重复造项目/账号、不以预检通过代替完整业务PASS。

当前核对：01回传10/10完整PASS待Root证据审查；02/04需在已修环境恢复实际参数验证；03源件拒绝需沿类别UPLOAD授权根因处理，不猜OWNER；05草稿保存审计缺策略需查操作码和正式策略来源，Root接管，禁止跳过审计。

05审计交付BDD：Given 本机tenant1只部署旧策略bind，而已核准软件合同/YAML新增upload-bind，When 正常NEW草稿通过服务器派生storage落位，Then 必须存在精确upload-bind审计策略并在同事务记事件；缺策略保存回滚，不生成文件/落位。仅为本机E2E/test补这一个正式登记项，保留旧strict bind和全部旧事件，不标质量批准，不扩大到prod。实际库缺行及C05保存失败为RED；备份后副本首次/重复及原行摘要核对、实际库同契约GREEN，再沿真实保存流程验证。用户当前“必要时同步纠正现有数据”、完成01～05及不咨询指令授权此直接必要本机配置修正，不扩业务写入或其他策略。

详情初始化BDD：Given 03/05草稿已真实保存并导航详情，When setup注册延迟确认watcher并读取当前用户上下文，Then 当前用户计算值已初始化，页面正常读取同一fileId详情；用户变化仍更新上下文。Root只调整当前用户声明顺序，不关闭watcher、吞ReferenceError或伪造文件数据。先以实际声明/真实Vue watch取得RED，再最小移动声明GREEN，再真实重开既有两草稿。

04保存回显BDD：Given 名单成员相同而服务器按ID规范排序，When 草稿保存后回读，Then 成员一致应成功，不因顺序变化报“回显不一致”；两份名单分别比较，不同成员仍拒绝，NULL与空数组仍区分。Root限定DetailApplicationPanel保存回读比较与实际handler回归，不更改后端落位/名单数据、公共矩阵或重复提交。

### 当前续修：01/03/05 流程定义合同拒绝

用户要求 Root 处理共同的 EXECUTION_CONTRACT_UNREGISTERED_DEFINITION。保留本轮 schema/账号修复成果；Root核对真实活动定义、完整模型digest和已登记v6源文件，优先通过正式流程管理前端对齐部署，不放宽catalog、不改历史实例、不直接SQL/API部署。原线程续发必须gpt-6.1-sol/high。

Given 新上传需要 PUBLIC_MATRIX_NATIVE_V2，When 活动部署的实际模型摘要不匹配正式登记，Then 精确诊断拒绝原因并记录RED；在正式v6模型通过定向合同验证、部署内容与影响核对后，通过真实前端完成必要部署，原申请重开预检须GREEN，既有历史定义/实例不改写。01/03/05随后使用原自有fixture继续，不能把预检GREEN当完整业务PASS。

里程碑：[x] 实际定义与摘要核对；[x] v6模型与catalog定向回归；[x] 前端部署与精确回读；[x] 三个原用例路线预检复验均GREEN。根协调任务仍in_progress，原完整业务用例继续。

新增已证实问题：2026-10-16正式UI导入/发布后，历史原列校验发现旧seed模型editor_source与旧v4部署资源共用ACT_GE_BYTEARRAY身份，正常addModelEditorSource原地更新了两份旧部署XML。新v6已发布，但不能记历史保护PASS。C05负责通过UI导入精确原XML仅保存恢复（不新发布），Root校验十份旧资源/97旧实例绑定。E2E15唯一负责BpmModelServiceImpl编辑源隔离及真实私有Flowable RED/GREEN，Root独占Maven和运行包更新。

Given 模型编辑源意外引用既有部署资源，When 正式更新模型XML，Then 为编辑源建立独立资源、历史部署原bytes与digest不变；metadata/tenant/model及既有流程绑定保持。实际部署已发现RED，修复必须补可重复的真实引擎RED，不通过SQL直接覆盖运行库。恢复与再导入均沿用用户处理该阻塞授权，禁止重放发布或修改历史申请身份。

新增05前端传输缺陷BDD：Given 默认Long序列化将安全账号ID返回为JSON数字、canonical保存字符串，When APPLICATION_CUSTOM预检返回同一组实际账号，Then 只把安全正整数规范为精确字符串再核对来源/持久化票据；不安全数字、错误身份、公共矩阵身份仍拒绝，保存/提交选择保持字符串身份。Root唯一修改upload/approval-matrix.ts及专属回归，先实际RED再GREEN。

用户要求 Root 解决各线程 BLOCKED 的前置问题。本节及 execution-authorization.md 优先于下文历史准备阶段限制；沿用真实前端 E2E、准备自有账号和更新重启本机 int_qms 的授权，不提交或推送。

- Root 负责实际运行库结构只读盘点、正式迁移依赖审查、备份及历史原列保护方案、公共目录回读归因、集成验收与放行。
- E2E-12 唯一负责目录首次打开异步组件问题的生产修复和 RED/GREEN。
- E2E-14 唯一负责用户部门清空能力的最小修复及 ID 910170 前端精确恢复；不得改其余身份、角色、密码或组织资料。
- E2E-09 负责登录租户选择脚本定位与重验，不根据单次超时修改产品。
- Given 真实运行库缺少源码所需列，When 完整核对正式迁移闭包及现状，Then 给出精确增量和原列保护证据，不通过业务降级绕过缺列。
- Given 目录表单首次懒加载，When 用户首次点击新建目录，Then 正确打开唯一表单，无未处理异常，不靠第二次点击或固定等待。
- Given 用户部门可选且原值为空，When 从精确用户编辑页清空并保存，Then 再读为空且其余原始字段保持，不采用 SQL/API 代替前端恢复。
- Milestones: [ ] 运行库盘点及迁移方案；[ ] 目录首次打开修复；[ ] 精确恢复账号；[ ] 公共页面真实复验；[ ] 通知受影响原线程并 review。
- 状态 in_progress；后续 schema 写入以 database-rules 明确授权和备份门禁为准，当前先只读核对，不重放历史种子。

## Goal

按用户2026-10-16指令“每个E2E启动一个开发线程来验证，你来启动和review”，为已定义E2E-01至24分别创建一个实际Codex开发线程，准备并执行真实页面验证，Root统一安排数据、运行环境及证据review。

## 本次结果收集

- 用户最新要求收集已完成E2E用例出现的问题。本次读取24线程当前状态、已回传报告、实际结果与必要trace/截图，按真实业务缺陷、准备过程产品阻塞、测试脚本问题及操作残留分别记录，重复问题合并并保留受影响用例。
- 不重新运行E2E、不新增测试数据、不修改生产或并行线程资产；正在执行的原线程继续。本次形成有证据路径和核对时点的问题清单，不把线程结束或fixture准备结束称为完整业务用例通过。
- 文档验证检查24用例状态覆盖、问题编号唯一、引用证据存在、报告和JSON结论一致，并保留无法确定根因的边界。

## Current Status



in_progress — Resume active repair goal: supervise original execution threads, diagnose R3 failures before minimal TDD repairs, then verify original UI flows. R3 12 PASS / 5 FAIL / 7 BLOCKED remains historical validation evidence; not all passed. No new Git/DB/server authorization inferred.

### 01–05历史工程收尾状态

blocked — 本轮01～05业务验证已全部完成并Root接受（e2e01-05-final-root-review.json）；本状态只表示工程收尾未完成：本轮不新增Git提交/推送，而仓库completed规则要求推送，且清理preview会删除仍需保留的审查证据，未执行apply。原24例其它不在本轮验收，不宣称全24通过。全部临时菜单/角色已恢复、任务浏览器/driver已关闭，业务测试数据与备份保留；当前40784/48061健康，最终平台原6消息不重复。补充自然refresh未验证及非阻塞展示备注见最终报告。

## Milestones

1. [x] 核对实际分支、最新用户授权、E2E/登录/运行端口/收尾规则与24个既定场景。
2. [x] 建立每例独立任务目录，创建24个实际开发线程并登记真实threadId。
3. [ ] 完成前端、运行Jar来源、Playwright及角色数据前置核对（运行/浏览器已通过；多角色数据待确认）。
4. [ ] 按互斥范围分批运行真实页面验证；隔离公共配置与任务自有数据。
5. [ ] Root逐例review脚本、trace/页面断言、失败归因与清理结果，形成24项汇总。

## Expected Verification

- 24个用例与24个真实threadId一一对应，任务目录唯一，覆盖全部Given/When/Then及参数化分支。
- 脚本使用真实Playwright；前端页面是全部被验收动作和业务断言的来源，禁用fetch/apiGet/直接HTTP与DB写入、mock和localStorage造登录。
- 每例有运行命令、实际退出码、环境及账号标签、任务数据编号、trace和截图/DOM断言、参数化结果及残留/清理记录。
- Root只接受当前运行代码及真实页面证据，静态检查不算E2E通过；BLOCKED、FAIL、PASS分别记录。

## BDD

- R30（22原P2F1固定引用真实失败）：Given MainA2已生效、P2F1仍固定MainA1且Reader有历史VIEW资格；When 在原目录选全部版本并点击引用行的正文浏览；Then 新窗口携固定A1的VERSION_HISTORY上下文、真实显示旧标记并拒新版串入。当前真实页面显示A1/已作废但正文bodyDenied，URL无bodyReadContext；使用明细入口有正确上下文并已实际通过。仅修ProjectBrowserPanel中previewReference的导航上下文，保持普通文件日常入口、引用权限复查及迟到结果检查；不改后端守卫或引用记录。先执行实际Vue回调行为RED。

- R29（原22源Rel1B1生效后的实际失败）：Given Main原关联固定Rel1A1现已OBSOLETE，Reader具有当前项目范围及正式VIEW资格；When 读取冻结关联证据和该精确A1权限投影并查看固定正文；Then 原冻结集合不变、A1名称和正文按既有retained-version资格可读，仍显示作废状态、不可编辑、不混B1；无VIEW/跨项目/分配范围不符仍拒，DAILY旧版正文仍拒。实际Query.assertRelationNameVisible走日常canViewFileName使证据读取整个失败，getRelationPermissions.canPreview也走日常正文；仅统一这些关联/引用读取为既有历史资格，禁止扩大日常或历史写授权。先复用真实Query/guard的历史回归fixture取RED。

- R28/R1624-11真实根因修正（2026-10-19 11:25）：原Rel1B主管签名后通知仍出现，L自然页面响应精确定位POST reading-session/stop返回1080000012，非task-action-readiness。Given 已有本人精确任务/版本阅读会话及有效源绑定访问凭证，且签名完成使当前任务资格结束；When 仅结束该既有会话；Then 核对租户/用户/文件/版本/事件/水印/token及会话精确上下文后关闭，不再次要求仍有待办；继续阅读、新建会话、正文和汇总仍走当前权限验证。错人、错版本、错任务/事件、失效或变造凭证、不存在会话均拒绝且无写。只修该关闭入口，不放宽正文守卫、不吞失败，不以ignoreErrorMessage隐藏阅读关闭失败。先服务及Query行为RED，最终原C/A/D签名及页面读数复验。

- Given 24个已定义场景与用户明确E2E授权，When 每例分配一个真实线程，Then 每例有独立归属、任务文档、执行状态及Root审查结论，不以一个线程代替24例。
- Given 当前int_qms源代码包含未提交修复且旧后端仍在运行，When 核对实际Jar关键类及前置数据，Then 不把旧包结果写成当前修复验收；缺口具体记录，不擅自重启共享服务或变更角色权限。
- Given 多线程使用同一测试环境，When 需要修改矩阵/项目或完成审批，Then 使用任务自有数据、独立浏览器上下文及Root分配的执行许可，不改其它用例数据。

## 设计约束检查

- 当前分支int_qms；不切分支、不提交推送、不改生产源码、不启用子Agent、不自动新建worktree、不操作远端或真实数据库。
- 每例可在自己的任务目录编写验证脚本与证据，Root独占本目录；不得修改其它线程脚本或共享运行配置。发现业务问题留存现场，由Root审查后派修。
- 启动线程不等于24例E2E已通过。先准备脚本与前置清单，Root确认运行代码及账号数据后按dispatch.json分批放行；未放行不启动浏览器或业务写入，不轮询空等。
- int_qms基准端口8061/48061，不使用int_main的8081/48081。当前前端未监听，48061为2026-10-14启动的外部旧Jar，需核对关键类。
- 已知仅AGENTS中的芋道源码/admin凭据。需要多角色时先只读核对真实账号和精确角色，缺少凭据或需新建/分配权限时明确报告；不得用admin替代职责分离，不猜密码、不扩大权限。
- 最新需求缺第2项尾部、第6/8项；不得根据旧冲突文档补造生效/盖章/重提验收。

## Cleanup Keep
- doc/tasks/20261016-dcc-24-e2e-coordination/e18-new-file-lock-observation-20261021.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e18-new-file-observer-gate-20261021.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e18-new-file-master-readonly-20261021.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e18-real-table-ddl-readonly-20261021.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e18-isolated-preapproval-review-20261021.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e18-new-file-root-review-20261021.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e13-new-file-root-review-20261021.json
- doc/tasks/20261016-dcc-24-e2e-coordination/new-files-review-20261021.md
- doc/tasks/20261016-dcc-24-e2e-coordination/e13-current-log-source-20261021.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e18-runtime-identity-20261021.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e18-new-file-schema-readonly-20261021.txt
- doc/tasks/20261016-dcc-24-e2e-coordination/e15-new-file-root-review-20261021.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e18-observe-new-files-20261021.py
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-structural-validation.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-cleanup-preview.txt

- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-dispatch-ledger.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-e03-diagnostic-note.md
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-e03-supervisor-stack.txt
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-final-runtime-check.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-issues.md
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-plan.md
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-results.md
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-round1-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-round2-e12-zip-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-round2-review-partial.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-round2-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-round3-review-partial.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-round3-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-round4-review-partial.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-round4-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-runtime-proof.json

- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-after.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-login-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-21-browser-and-replacement-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-21-browser-and-replacement-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-21-browser-replacement-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-21-browser-replacement-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-green-21-projection-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-green-21-projection-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-preview-stale-notification-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-preview-stale-notification-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-r28-countersign-failure-readonly-diagnostic.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-readiness-stale-notification-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-readiness-stale-notification-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-stale-read-notification-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-stale-read-notification-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-stale-read-notification-regression.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e24-authoring-successor-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e24-authoring-successor-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e24-working-route-final-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e24-working-route-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e24-working-route-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/login-preflight-before-project.json

- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-assignment-query-package-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-assignment-query-package-runtime-copy.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-assignment-query-package.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-complete-binding-package-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-complete-binding-package-runtime-copy.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-complete-binding-package.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-complete-checkin-package-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-complete-checkin-package.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-history-checkout-package-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-history-checkout-package-runtime-copy.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-history-checkout-package.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-pending-checkin-package-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-pending-checkin-package-runtime-copy.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-pending-checkin-package.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-project-query-package-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-project-query-package-runtime-copy.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-project-query-package.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-after.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-before.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-candidate-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-candidate-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-candidate-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-candidate-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-login-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccApprovalTaskAdapterTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccAttachmentDownloadHttpContractTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccAttachmentDownloadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccControlledFileBodyReadContextTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccControlledFileBodyReadCurrentSourceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccControlledFileBodyReadHttpContractTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccControlledFileDetailAuthorizationGuardTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccControlledFileQueryServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccDownloadPolicyServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccHistoricalAttachmentReadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccPaperDistributionAckServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccSelectedDetailReadHttpTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccSelectedDetailReadServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-DccWorkingSubmissionConditionTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-green-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-regression-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-query-regression-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-assignment-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-complete-after.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-complete-before.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-complete-binding-login-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-complete-binding-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-core-green-DccApprovalTaskAdapterTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-core-green-DccAttachmentDownloadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-core-green-DccControlledFileQueryServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-core-green-DccHistoricalAttachmentReadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-core-green-DccWorkingSubmissionConditionTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-frontend-attachment-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-frontend-attachment-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-history-checkout-core-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-history-checkout-core-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-history-checkout-regression-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-history-checkout-regression-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-integrated-login-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-integrated-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-integration-after.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-integration-before.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-integration-red-and-history-candidate-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-integration-red-and-history-candidate.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-lifetime-after.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-lifetime-before.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-lifetime-login-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-lifetime-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-pending-after.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-pending-before.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-pending-login-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-pending-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-authorization-green-DccControlledFileAssignmentScopeServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-authorization-green-DccControlledFileQueryServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-authorization-green-DccHistoricalAttachmentReadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-authorization-green-DccProjectBrowserScopeIntegrationTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-authorization-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-authorization-green-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-authorization-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-authorization-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-authorization-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-project-before.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-red-and-history-candidate-DccApprovalTaskAdapterTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-red-and-history-candidate-DccAttachmentDownloadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-red-and-history-candidate-DccHistoricalAttachmentReadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-red-and-history-candidate-DccWorkingSubmissionConditionTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccApprovalTaskAdapterTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccAttachmentDownloadHttpContractTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccAttachmentDownloadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccControlledFileBodyReadContextTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccControlledFileBodyReadCurrentSourceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccControlledFileBodyReadHttpContractTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccControlledFileDetailAuthorizationGuardTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccControlledFileQueryServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccDownloadPolicyServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccHistoricalAttachmentReadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccSelectedDetailReadHttpTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccSelectedDetailReadServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-DccWorkingSubmissionConditionTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-regression-green-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-final-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-final-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccApprovalTaskAdapterTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccAttachmentDownloadHttpContractTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccAttachmentDownloadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccControlledFileBodyReadContextTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccControlledFileBodyReadCurrentSourceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccControlledFileBodyReadHttpContractTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccControlledFileDetailAuthorizationGuardTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccControlledFileQueryServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccDownloadPolicyServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccHistoricalAttachmentReadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccPaperDistributionAckServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccSelectedDetailReadHttpTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccSelectedDetailReadServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-DccWorkingSubmissionConditionTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e1624-shared-history-green-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-binding-green-DccCheckinDraftInitializationDatabaseTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-binding-green-DccControlledFileUploadApiTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-binding-green-DccControlledFileWorkflowServiceImplTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-binding-green-DccRevisionAttachmentsDatabaseTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-binding-green-DccWorkingSubmissionConditionTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-binding-green-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-checkin-green-DccCheckinDraftInitializationDatabaseTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-checkin-green-DccControlledFileUploadApiTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-checkin-green-DccControlledFileWorkflowServiceImplTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-checkin-green-DccRevisionAttachmentsDatabaseTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-checkin-green-DccWorkingSubmissionConditionTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-checkin-green-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-checkin-regression-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-complete-checkin-regression-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-complete-binding-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-complete-binding-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-full-checkin-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-full-checkin-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-real-attachment-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-real-attachment-red-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-real-attachment-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-real-schema-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-real-schema-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-source-context-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-source-context-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-source-context-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-source-context-red-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-source-context-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-source-context-valid-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-source-context-valid-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-source-required-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-pending-source-required-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-source-context-first-green-DccCheckinDraftInitializationDatabaseTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-source-context-first-green-DccControlledFileUploadApiTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-source-context-first-green-DccControlledFileWorkflowServiceImplTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e20-source-context-first-green-DccWorkingSubmissionConditionTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-job-lifetime-green.json
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-job-lifetime-red.json
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-job-lifetime-test.py
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-start-e2e1624assignment.json
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-start-e2e1624binding.json
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-start-e2e1624integration.json
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-start-e2e1624lifetime.json
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-start-e2e1624pending.json
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-start-e2e1624project.json

- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-history-link-and-23-read-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-history-link-and-23-read-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-history-link-23-read-compiled-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-history-link-23-read-compiled-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-23-compiled-red-DccApprovalTaskAdapterTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-23-compiled-red-DccHistoricalAttachmentReadTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e22-supplement-entry-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e22-supplement-entry-red-process.json

- doc/tasks/20261016-dcc-24-e2e-coordination/browser-replacement-recovery-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/browser-replacement-recovery-before.json
- doc/tasks/20261016-dcc-24-e2e-coordination/browser-replacement-recovery-after.json
- doc/tasks/20261016-dcc-24-e2e-coordination/browser-replacement-recovery-login-preflight.json

- doc/tasks/20261016-dcc-24-e2e-coordination/signature-bind-test-policy-repair.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-electronic-access-red-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-electronic-access-red.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-electronic-access-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-electronic-and-receipt-green-DccControlledFileQueryServiceTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-electronic-and-receipt-green-DccDistributionSignatureTransactionTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-electronic-and-receipt-green-DccElectronicReceiptBindingTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-electronic-and-receipt-green-DccDistributionAckBindingConsumerTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-electronic-and-receipt-green.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-electronic-and-receipt-green-process.json

- doc/tasks/20261016-dcc-24-e2e-coordination/repair-receipt-test-policy.py
- doc/tasks/20261016-dcc-24-e2e-coordination/receipt-test-policy-repair.json
- doc/tasks/20261016-dcc-24-e2e-coordination/body-opened-test-policy-repair.json
- doc/tasks/20261016-dcc-24-e2e-coordination/browser-replacement-restart-snapshot.py
- doc/tasks/20261016-dcc-24-e2e-coordination/browser-replacement-restart-before.json
- doc/tasks/20261016-dcc-24-e2e-coordination/browser-replacement-restart-after.json
- doc/tasks/20261016-dcc-24-e2e-coordination/browser-replacement-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/browser-replacement-login-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-selected-neighbor-green-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e17-policy-regression-DccDistributionSignatureTransactionTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e17-policy-regression-DccW08DistributionPreviewTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e17-policy-regression-DccControlledFilePreviewDownloadApiTest.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e17-body-open-policy-regression.log
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e17-body-open-policy-regression-process.json

- doc/tasks/20261016-dcc-24-e2e-coordination/e2e16-24-acceptance.md
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e19-category-upload-diagnostic-red-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e19-category-upload-diagnostic-green-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/category-upload-message-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/category-upload-message-restart-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/category-upload-message-login-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-directory-permission-red-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-projection-preflight-failure-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-directory-permission-green-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-replacement-custom-red-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e18-browser-neighbors-green-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-custom-regression-green-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e21-selected-neighbor-manifest-failure-surefire.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/targeted-activation-lease.json

- doc/tasks/20261016-dcc-24-e2e-coordination/e2e06-15-cleanup-preview-result.json

- doc/tasks/20261016-dcc-24-e2e-coordination/e2e06-15-final-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e13-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e15-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/training-owner-metadata-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/targeted-activation-final-isolation-proof.json

- doc/tasks/20261016-dcc-24-e2e-coordination/e2e10-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/training-owner-metadata-red-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/training-owner-metadata-green-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/training-owner-current-duty-green-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/targeted-activation-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/targeted-activation-unrelated-files-before.json
- doc/tasks/20261016-dcc-24-e2e-coordination/targeted-activation-unrelated-files-after4216.json

- doc/tasks/20261016-dcc-24-e2e-coordination/targeted-activation-job.md
- doc/tasks/20261016-dcc-24-e2e-coordination/targeted-activation-job-green-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/delayed-activation-job-readonly.json
- doc/tasks/20261016-dcc-24-e2e-coordination/delayed-activation-runtime-flags.json
- doc/tasks/20261016-dcc-24-e2e-coordination/delayed-activation-last-job-logs.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e10-type-storage-runtime-proof.json

- doc/tasks/20261016-dcc-24-e2e-coordination/e2e11-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e12-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e10-type-storage-green-summary.json

- doc/tasks/20261016-dcc-24-e2e-coordination/fix-delayed-confirm-policy.py
- doc/tasks/20261016-dcc-24-e2e-coordination/delayed-confirm-policy-red.json
- doc/tasks/20261016-dcc-24-e2e-coordination/delayed-confirm-policy-repair.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e10-type-storage-red-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e10-type-storage-failure-readonly.json

- doc/tasks/20261016-dcc-24-e2e-coordination/review-bpm-todo-ui.cjs
- doc/tasks/20261016-dcc-24-e2e-coordination/bpm-todo-ui-after-fix.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e10-type-and-bpm-page-runtime-proof.json

- doc/tasks/20261016-dcc-24-e2e-coordination/review-06-08-evidence.py
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e06-08-evidence-integrity.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e06-15-acceptance.md
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e06-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e07-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e08-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e09-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e10-root-partial-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e14-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e06-10-review-findings.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e11-15-review-findings.json
- doc/tasks/20261016-dcc-24-e2e-coordination/prepare-workflow-runtime.py
- doc/tasks/20261016-dcc-24-e2e-coordination/run-unblock-maven.py
- doc/tasks/20261016-dcc-24-e2e-coordination/bpm-todo-pagination-fix.md
- doc/tasks/20261016-dcc-24-e2e-coordination/bpm-todo-exclusion-test-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e10-type-action-green-r2-summary.json

- doc/tasks/20261016-dcc-24-e2e-coordination/e2e01-05-final-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e03-root-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e03-notification-final-readonly.json
- doc/tasks/20261016-dcc-24-e2e-coordination/overnight-issues.json
- doc/tasks/20261016-dcc-24-e2e-coordination/platform-evidence-runtime-proof.json
- doc/tasks/20261016-dcc-24-e2e-coordination/platform-evidence-green-summary.json
- doc/tasks/20261016-dcc-24-e2e-coordination/auth-session-cache-regression-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/auth-session-cache-types-process.json

- doc/tasks/20261016-dcc-24-e2e-coordination/e2e-issues-20261016.md
- doc/tasks/20261016-dcc-24-e2e-coordination/e2e-issues-20261016.json
- doc/tasks/20261016-dcc-24-e2e-coordination/task.md
- doc/tasks/20261016-dcc-24-e2e-coordination/execution-log.md
- doc/tasks/20261016-dcc-24-e2e-coordination/verification-report.md
- doc/tasks/20261016-dcc-24-e2e-coordination/thread-registry.json
- doc/tasks/20261016-dcc-24-e2e-coordination/dispatch.json
- doc/tasks/20261016-dcc-24-e2e-coordination/thread-contract.md
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/inspect-runtime.py
- doc/tasks/20261016-dcc-24-e2e-coordination/node-preflight.mjs
- doc/tasks/20261016-dcc-24-e2e-coordination/login-preflight-run.mjs
- doc/tasks/20261016-dcc-24-e2e-coordination/login-preflight-result.json
- doc/tasks/20261016-dcc-24-e2e-coordination/root-syntax-review.json
- doc/tasks/20261016-dcc-24-e2e-coordination/root-review-round1.md
- doc/tasks/20261016-dcc-24-e2e-coordination/start-verified-runtime.py
- doc/tasks/20261016-dcc-24-e2e-coordination/README.md
- doc/tasks/20261016-dcc-24-e2e-coordination/runtime-health.json
- doc/tasks/20261016-dcc-24-e2e-coordination/int-qms-backend-build-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/int-qms-backend-build-r2-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/int-qms-runtime-copy.json
- doc/tasks/20261016-dcc-24-e2e-coordination/int-qms-runtime-copy-r1.json
- doc/tasks/20261016-dcc-24-e2e-coordination/wiring-red-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/wiring-red-junit.xml
- doc/tasks/20261016-dcc-24-e2e-coordination/wiring-green-process.json
- doc/tasks/20261016-dcc-24-e2e-coordination/wiring-verification-results.json
- doc/tasks/20261016-dcc-24-e2e-coordination/root-review-round2.md
- doc/tasks/20261016-dcc-24-e2e-coordination/root-syntax-review-r2.json
- doc/tasks/20261016-dcc-24-e2e-coordination/root-syntax-review-r3.json
- doc/tasks/20261016-dcc-24-e2e-coordination/existing-account-preflight.mjs
- doc/tasks/20261016-dcc-24-e2e-coordination/existing-account-preflight.json
- doc/tasks/20261016-dcc-24-e2e-coordination/existing-account-preflight-selector-error.json
- doc/tasks/20261016-dcc-24-e2e-coordination/existing-account-preflight-old-profile-error.json
- doc/tasks/20261016-dcc-24-e2e-coordination/existing-account-role-preflight.mjs
- doc/tasks/20261016-dcc-24-e2e-coordination/existing-account-role-preflight.json

## 本轮运行授权与缺陷归属更新

- 用户已明确允许更新并重启int_qms；只涉及48061，不再要求重复确认，正式业务E2E授权仍有效。任务专用账号/角色/菜单变更的第二个问题仍待答复。
- 完整reactor package通过，运行切换实际揭示Delayed服务@Resource按字段名误注入infra mapper，追加E2E-15线程唯一生产一行及专属Spring注入回归归属；必须Root实际目标RED后才可GREEN，其他线程仍只改自身验证资产。
- Root早期一次性PowerShell启动器未正确复用现有环境，已删除以免再次运行。仅使用start-verified-runtime.py复用原环境/审批签名key/定义转换；不生成新密钥、不叠加local profile、不启用bean覆盖或新的共享Quartz任务。既有失败保留在运行日志与执行记录。


## 2026-10-21 新文件问题处理已启动

in_progress — 用户“开始处理”授权继续未闭环问题。当前确认int_qms，48061/32436、8061/20976仍在监听。使用ponytail最小根因修复方式，不重跑通过分支、不动历史文件/流程、不提交推送。

里程碑：
- E15原线程gpt-6.1-sol/low：唯一浏览器窗口，仅正式页面只读核对精确定向handler的任务行、参数、状态与日志，确认调度前置；未放行新文件/触发/全局同步。Root review恢复方案后再新文件自然生效验证。
- E13原线程同模型：离线审核training-record真实调用链及此前约20秒新样本证据，准备单个新文件的一次提交关联诊断；批准负责人PASS保留。
- E18原线程同模型：离线审核原死锁与锁序、现有149帧竞争证据，准备最小新文件并发诊断；原升版/附件PASS保留。

BDD：Given 重启后的RAM调度可能缺注册；When 核对正式定向任务；Then 区分数据库启用状态与真实执行注册，禁止把空结果或理论cron当正常运行。Given 新文件培训/并发指派单次请求；When 超时或死锁；Then 关联同请求/时间窗诊断，不重放签名、不加重试掩盖根因。

设计约束检查：真实前端承担全部业务动作；API/DB不写业务；共享Maven/服务由Root排期，其他线程只读离线。待直接证据后决定是否需代码修改与严格RED/GREEN。当前无新增PASS。


### Root执行决定：定向测试调度任务替换

正式UI前置确认唯一handler行5629（DCC241016Root定向生效）暂停，参数1:2054545668044084228；最新48060属于2026-10-20，不能证明当前RAM注册。E16交接与原任务归属已核对。源码JobServiceImpl.deleteJob只逻辑删除infra_job与RAM注册，不删除历史执行日志；BaseDO.@TableLogic保留旧行。

采用已有正式UI最小恢复路径：E15先将唯一新文件走至延迟确认完成并记录精确fileId及未来时间；归档5629的配置和旧日志后，仅对暂停且归还Root的5629点击删除（逻辑退役，不触碰其旧目标文件），用同一已有handler新建精确1:<新fileId>任务。创建即注册RAM，无全局同步/重启/新增handler或配置。保留旧执行日志并最终核对可查询；新任务自然cron到期后暂停，记录新ID、T前0/T后1及原U正文。新文件准备失败则不退役旧任务；新任务失败则暂停/保留现场，不重提。

这属于当前用户授权的新文件E2E必要任务自有运行配置恢复，不是历史文件修复，不授权删除任何文件、签名、BPM、旧日志、账号/角色/项目或公用任务5625。证据只能证明当前单新文件运行，不宣称重启后永久自动恢复或全局批处理通过。


### 并发/培训共用本轮新文件的最小验证安排
E15独占当前窗口，已新建4431（DCC241021C15NEW1792564691149），历史对象未动。其结束后E18复用既有stage-a单runner创建两份新文件（E18与E13各一份），只推进至两个L待指派；Root核对精确身份后启动90秒只读锁观察，同runner单GO并发各一次L确认。E13不另建第三份文件，E18关闭后交接新E13对象，由原E13线程继续C/A/D和一次U培训现场诊断。失败不重放、不自动多轮碰撞。
Root已只读保存MySQL8.0.40/REPEATABLE-READ、既有serverUUID及stage节点/账本索引，证据e18-new-file-schema-readonly-20261021.txt，未执行写SQL。现存假设gap→ledger→stage符合路径但未证实原死锁；直接改普通读存在RR旧快照风险，因此暂不改生产。

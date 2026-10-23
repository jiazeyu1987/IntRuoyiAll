# A/B/C/D开发方向复核

日期2026-10-01，主目录实际int_qms，各worker实际codex/20260930-dcc-a/b/c/d，共同HEAD a801dc8。依据用户最新确认、v1.3确认补充、SC/IC与CC-2。范围为当前源码和任务/日志静态复核，不是本轮运行测试、完整逐行代码验收或E2E；活跃Owner源码没有被整合/覆盖。

## 结论

四模块业务分工和核心技术方向正确，没有发现需要推倒重写或重新拆分模块的依据。主要问题是确定业务缺口尚未闭环、源码/报告更新存在时差、主管理接线滞后。应按已交付真实合同整合前端操作链，不持续扩展无关功能或用不断增长的单测数代替完整操作。

| 模块 | 当前方向 | 已核对的正式代码依据 | 当前交付边界与优先事项 |
|---|---|---|---|
| A 流程/生命周期 | 正确，新审计修复已取得后续回归 | service/file/DccControlledFileLifecycleService.completeControl/activateLocked分离latest/currentActive，未来受控不废旧版，到期原子切换；WorkflowSignoffAssignmentService.assign签名安排/义务/save后改派；D唯一同步consumer；DccWorkflowFileStateAudit复用统一Gxp内核 | 本轮复核初始看到旧cc2-a-ledger-final-green.log 28项2FAIL；A随后用同事务JDBC当前读修复真实before缓存，directed-final.log于23:39:29完成DCC745＋BPM37＝782项PASS，23:47:56主应用compile PASS。须冻结交付、Root Review/策略登记后整合；接B已交草稿/跨File及C首次候选/本次日期。无需新审批节点或第二通知入口 |
| B 项目/属性/目录 | 正确，CC-2已形成可接入交付 | DccApplicationRoundService.reserveDraft/bindReservedDraft持久NULL BPM草稿；DccProjectApplicationSnapshotService.prepareDraft/forkToNewApplication；ProjectAttributesService.forkSavedToApplication记录source provenance；ProjectFolderMaintenanceService.delete与D共享project→folder锁、实际位置/引用保护、逻辑历史 | 最新报告160执行去重133场景，独立结果可交Root Review，非整体运行已过。下一步冻结最新103源码/19增量及策略/迁移合同供Root接收、A调用；不再越界写A Workflow，不复制D授权/引用计数；产品审核人员仍不猜 |
| C 版本/检出/查询 | 核心方向正确；CC-2完成项应优先收口 | VersionPolicy.formalTarget/nextWorking统一算法；RevisionService.createRevision存实际PARTIAL/REPLACEMENT并冻结所选正文；NameClaimService.claimIdentity用原完整源名binary身份；Query.getRevisionOptions/getControlledFileSelectorPage取正式latest、逻辑checkout、独立正文权限 | 本次effectiveDate已登记Req且进入Revision候选/hash，旧字段权限阻塞已解除。复核期间新增createInitialCandidate合同，23:40:27初始候选RED4项，目前实现仍明确not implemented；不是可交付结果。SelectorQuery.validated仍拒projectFolderId，placement查询/审批VIEW属性投影尚未闭环。先完成这些CC-2，再做非关键外围自审；不要把“正确拒绝不支持操作”当功能完成 |
| D 关联/引用/选择器 | 正确，处于组合与接入阶段 | ProjectReferenceService.create/cancel只委托B唯一leader并锁project→folder→Master→selected；count按项目去重，取消exact ref保源；RelationRemediationService.recordControlled只通知所选人、冻结期限与载荷；current/latest和historic/pinned版本分离 | 最新报告27类236后端/53前端是D实际范围，未夸大Root1022为自己重跑。下一步接B已交delete做create/cancel/delete组合，固定组件/API合同供Root接；无新独立Query/签名/权限系统，不恢复A直接recordControlled、不让引用擅自自动跟版 |

以上BE路径前缀为IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc；测试与报告在各原任务目录。查看A/C尚在更新的源码时，必须冻结交付指纹后再判定测试是否覆盖最终代码，不能凭文件名含green或报告头旧blocked下结论。

## 必须纠正的共同方向

1. **已确认需求不能再次变为待定或加门槛。** 共同HTML仍有旧培训逐人核验/保留长度未知，已由Root同步为“文控上传本次线下培训文件即可、作废保存20年”。当前实施起点是实际作废日期。提醒提前量/产品审核人员未定，不能借此新增全员线上培训、培训批准、任意admin审核或固定天数。
2. **先闭合实际前端操作，再继续扩展自审。** Root负责公共upload/detail/browser和文件夹/属性/部门/关联/签名/下发接线；线程独立组件PASS不证明这些前端路径已可用。主管理要接收B最新交付、C最终字段与A审计政策，及时同步正式依赖，不让worker反复等待“Review没到”。
3. **历史与本次、执行与最新、关联与引用必须继续分开。** 新申请取项目默认、已存草稿/返工保原来源及实际值、作废不改原属性；未生效最新版用于当前关联但不用于执行；历史只读不补今天默认；引用保持所选版，自动跟版尚未确认。
4. **测试必须围绕可失败的真实动作。** RED/GREEN、并发、权限、失败回滚是必要质量检查；数值/日期/序列化边界解决后按受影响清单回归即可，不反复跑已通过且未变的范围凑进展。B继承测试重复要继续去重；A后续782PASS应以冻结源码Review，不凭旧失败文件名或未更新报告否定新证据。当前没有授权真实E2E，不伪称已运行。
5. **不扩增审批或产品操作复杂度。** 统一审计是仓库正式要求，复用既有GxpAuditService属于范围；原因字段和快照用于实际记录。不得以满足内部技术登记为由增加不属于业务的培训批准、模板审批、第二套版本/引用/轮次系统或把“文件转移属性”自动执行为跨项目发送。

## 当前应执行的顺序

1. A冻结新审计及782PASS的最终交付；B交Root当前CC-2完整指纹/迁移/候选策略，Root接收后同步A/D。
2. C完成INITIAL候选、本次日期的最终回归、projectFolder正式latest分页和任务/VIEW冻结证据投影；A接实际初次送审/返工调用，禁止假BPM/猜轮次。
3. D在B正式删除及C候选服务到位后执行组合验收，保留现有组件合同；Root同步接三公共页面、二次确认、权限/版本/属性/签名历史、受控下发提醒。
4. 最后按原I-01..I-08和AC-01..26逐项核对可操作行为、覆盖范围和真实未定项。单测通过、blocked/in_progress或“独立实现交付”都不是系统已满足业务的证明。

## 本轮证据边界

只读检查最新task/report/integration-notes、B cc2-review-handoff与源码/当前日志，未重新执行模块测试、数据库、服务、E2E、Git写操作。Root仅更新方向Review及过期需求/任务指导，不修改四worker活跃生产源码。A旧2失败和后来782PASS分开核对；本轮未重新运行，PASS来源为A最新命令日志与其计数清单，整体Root Review仍未关闭。C日期已写代码且INITIAL正处RED阶段，当前最终报告尚未同步，不能把旧447/36当新日期/初次候选批次验收。

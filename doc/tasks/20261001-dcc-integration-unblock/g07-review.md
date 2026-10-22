# G07 当前交付 Review

2026-10-03。主目录实际分支 int_qms，整合分支 codex/20261001-dcc-integration，共同 HEAD a801dc8b91579241e221d129ab34343997673f40。本记录只证明当前源码审查和隔离验证；完整目标、真实页面和本地合入仍未完成。

## 生命周期交付已核验

- Root 逐项核对 backend_closure 的四项 G07 资产字节 SHA256，全部吻合。读取实际 DccControlledFileLifecycleService、十个三版本场景与调度注册 SQL，而非仅采用完成声明。
- activateLocked 在 Master 锁下锁整条链；新版真正生效时，同事务作废所有版本号更低的 ACTIVE / CONTROLLED_PENDING_EFFECTIVE。更高未来版本、未受控及失败候选不变。已过更高执行版的遗留低版待生效行按真实后继作废，不反向执行、不生成生效事件。
- 自动作废保留原受控/预设生效/批准日期、正文及签核；分别记录 SYSTEM_ACTOR=-1 审计和 supersededByFileId，并延长名称/编号占用到实际作废时间加20年。晚审计失败回滚全部旧版、新版、Master、占用和事件。
- 当前 g07-complete-regression.log 实际为 11 类368次，失败/错误/跳过均0，BUILD SUCCESS；g07-main-compile.log BUILD SUCCESS。368不是与历史474/731相加的独立覆盖数。隔离H2/正式GxP账本，外部事件发布端口在测试中明确替身，不宣称实库或页面结果。

## Review 发现的后续问题

1. **默认列表包含作废定位。** DccProjectBrowserMapper latestVersionOnly 只限制 f.id=m.latest_controlled_file_id。批准作废只清理当前执行定位，保留最新受控历史定位，因此默认列表可能返回已作废行，新的前端 wrapper 会准确拒绝整页。应在SQL查询/count/page同口径限制真实当前受控事实；不清历史、不退到旧执行版。已在主任务登记 backend_closure 后续边界，正式 Agent 回执才表示已开始修复。
2. **引用行没有正文入口。** DccProjectReferences 当前只有固定文件名称/版本/计数/取消。project-browser.mapReferences 还依赖来源 getProjectDiscovery，权限可能强于文件名称可见。已有P15精确 selectedControlledFileId 的 relation-permissions 可以提供正式名称/项目名/文件夹/正文权限；应复用并校验身份，保持引用固定版本、名称可见与正文权限分开。由 upload_closure 后续唯一负责，不能猜 canPreview 或新增平行接口。
3. **宽状态类型。** browserVersionOptions 返回 status?:string，而共享 wrapper 已采用 DccProjectBrowserStatus 联合类型。应使用正式 ProjectBrowserOptions，保留所有现有筛选行为，不能放宽共享类型通过检查。

G08-B1 后续实际交付：默认列表问题已经真实RED两项后修复，同一共享SQL谓词要求latest精确ID、ACTIVE/CONTROLLED_PENDING_EFFECTIVE且controlledTime非空；两项资产指纹吻合，274/6类实际回归和当前compile通过。该结论不证明后续新增引用明细API。引用固定版本入口正在所属Owner修复，完成前不先声称已通过类型/页面。

详情 Owner 正在专项只读检查完整12项公共入口；未知发现须转为具体范围分派，不将“源码组件存在”判为业务闭环。

详情审查已正式交付整合树 doc/tasks/20261002-dcc-detail-integration/public-ui-closure-audit.md。Root 复核引用正文入口、项目列表直接关联入口缺失、选择器单项目树、作废审批默认属性轮次、引用使用明细和产品创建核对阶段六项源码依据。优先分派实际作废BPM与文件native BPM不同的历史面板错轮次问题，不能只改标签。产品核对阶段按需求 flow-03 已有确认→核对→提交步骤处理，不另外虚构批准人员规则。其他入口随后按Owner明确分派，不把审查报告当完成。

## 迁移与运行边界

Root 重新生成 g07-migration-package.json：17个根迁移、43项依赖闭包，正式 policy gate passed。新增生效 job 的 infra_job 行以自动ID和唯一 handler_name 注册，STOP=2；显式参数未批准则拒绝。未连接或执行实际数据库。

调度注册文件是准备方案，不是已运行证明。实际 local profile 有 JobStartupSyncRunner 全量同步和 LocalQuartzAutoPauseRunner 后续收口；默认白名单只有ERP两项，DCC job 不在其中。任务启动必须先避免全库同步和无关任务运行，获准后只对确认的调度归属、白名单与Quartz触发状态核验；不能调用全局sync冒充只同步DCC。

目前 MySQL23306、Redis26379、整合8067/48067及48081均无监听；共享依赖启停许可和提醒配置问题仍待用户答复。未启动Docker/容器、未执行迁移、未修改共享配置、未运行E2E、未进行Git提交/合入。

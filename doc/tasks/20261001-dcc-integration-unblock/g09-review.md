# G09 公共引用与审批轮次组合 Review

2026-10-03。沿用主任务和既有整合树，主目录实际 int_qms，整合分支 codex/20261001-dcc-integration，共同 HEAD a801dc8b91579241e221d129ab34343997673f40。前一轮有实际源码、隔离回归和迁移准备进展，本轮继续Review，不累计全目标阻塞轮次。

## 已Review交付

UI-01：已保存引用精确绑定 selectedControlledFileId，以现有 relation-permissions 验证 tenant/Master/project/file/version 和独立 canPreview；来源项目 discovery 不再参与名称读取。浏览重查固定正文权限后使用实际ID；只读追溯另外执行强详情授权和身份校验，不借正文权限越权。原项目leader限制仍仅控制引用/取消。关闭或卸载后晚响应无导航，确认引用已保存而后读失败明确区分事实，阻止重复提交。

UI-04：实际文件、route和用户组成读取context。显式审批BPM/任务从既有真实TaskAPI核验processInstanceId和成员后，申请证据只选正式映射的实际办理轮次；无route审批上下文的普通详情仍默认native BPM。未核验/外来/缺失或重复映射明确报错，不能展示另一轮属性；切上下文同步清证据和晚响应。后端已有HistoryGuard仍验证租户、真实BPM定义/对象版本，前端context不授予办理或签名资格。Root核对BpmTaskRespVO及真实转换已有processInstanceId，不新增后端猜测字段。

Root执行9文件组合：g09-root-review-combined.log 115 PASS，0 fail/skip/cancel，涵盖详情轮次、培训、公共浏览/引用及关系。正式项目类型检查 tsconfig.relaxed.json、Node8GB，session58843 exit0。未改配置或安装依赖，沿用已安装工具的项目标准等效命令。Vite env.local构建session2556已经正式确认exit0、Build successful；既有Browserslist数据旧警告保留，无自动更新依赖。这批类型/构建只证明当前交付，后续生产修复仍需新的受影响验证；不是E2E。

## 当前新分派与边界

- backend_closure已正式续派并在实际任务标记in_progress，实施Root批准的usage-page最小只读合同；不新增schema或源编辑资格、不写实库。
- upload_closure下一批接UI-02，公共文件行直接关联按钮与轻量弹框复用DetailRelationsPanel；只能使用精确名称投影，不能强详情补位或放宽组件权限。
- detail_closure下一批接UI-03，共用选择器左侧项目分页/搜索与真实逻辑目录；来源、已选、申请默认/实际值和目标引用目录不能因浏览别项目变化。
- UI-06产品新建确认核对阶段仍待独立分派；目前未修，不将直接表单提交解释为原需求的完整步骤。

## 本轮追加实际实现

Root接UI-06独立basic-data范围，先执行实际 submitProjectProductRequest handler 的4项有效RED（旧实现直接写入、没有确认）。新增独立安全Vue文本核对summary，完整显示项目/产品/负责人正式账号/模板/3组默认属性/备注/原因/后台审核人；按钮分为确认申请信息→核对弹框提交申请。取消零写保表单，变更申请/表单/所选人员或模板/关闭上下文时拒绝；确认后重读正式审核配置，变化要求重确认；批准人员规则不变。新扩展缓存目录变化场景实际RED1后GREEN。确认是只读客户端步骤，后台仍负责本次配置和申请快照，不声称双读替代后端事务冻结。

g09-product-confirmation-regression.log当前16 PASS：8个新父handler/实际summary场景和8个既有审核配置行为，局部两生产文件lint exit0。旧catch补丁误命中retry handler后已立即恢复该行，仅正式submit catch处理cancel/close；最初取消错误提示FAIL保留，不写成首次GREEN。最终lint→正式项目types→env.local build的session66586已经核实exit0/Build successful。新summary文字使用Vue VNode，测试验证不使用innerHTML；全字段核对框限制65vh滚动，真实页面仍未验。

最终11文件组合 g09-final-combined-ui.log 131 PASS/0 fail/skip，g09-final-ui-receipt.json 固定11项生产/测试当前SHA256；不把此前115与当前131相加作为独立覆盖数。主目录与整合树六项非任务资产指纹保持，八项G08已Review源码未被本轮产品修复覆盖。

UI-05 后端已正式交付：Root核对6项生产/测试指纹全部吻合，真实117/9类回归和compile日志通过；源名称权限/目的项目hard scope/固定版本/并发REPEATABLE_READ已有真实隔离H2证据。该接口不是前端入口完成，仍需交浏览Owner接线。无新schema或实库操作。

以上后两项任务在主任务已经登记Owner和BDD，实际运行以formal followup回执为准；仅记录分工不等于执行。

当前仍未实施的生产路径：UI-02文件列表直接关联弹框、UI-03选择器左侧跨项目目录、UI-05引用使用明细的前端点击/列表。UI-06已由Root后续实际实现覆盖上面原待分派记录；UI-01/UI-04离线已Review。后两Owner没有本轮新followup回执时不能记成正在执行，下一轮需正式续派这些具体范围，不再重复完成项。

## 仍未达成的完整目标门禁

真实测试环境MySQL23306/Redis26379及任务服务8067/48067仍无监听；共享依赖启停许可、提醒业务值与生效调度配置未答，未静默猜值。迁移17根/43闭包仅准备，Flowable自动DDL必须在任务运行前关闭，Quartz全局同步/本地白名单需要核验。没有实库迁移、真实页面、Git实现提交或最终int_qms合入；整个目标保持active。

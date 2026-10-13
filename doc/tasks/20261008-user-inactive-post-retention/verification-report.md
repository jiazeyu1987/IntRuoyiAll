# UM-04 Verification Report

## Current Status

completed — UM-04目标修复及验证PASS，标准快进融合int_main，cleanup/managed实体归档/slot7释放通过；全量类型与非目标MES工作台失败保持。

## Scope

只修用户编辑保留已有停用岗位及完整集合显式移除；不实施 UM-02、不改变通用新增岗位必须启用、密码/会话、导入或 schema。初始测试基线 a3b8bedfeece394f286e23a9d5144361b951e8ac，runtime已显式对齐提交基线a1640b169f69453701c9425d66a974033cf44120且任务源码指纹未变；独立 codex/user-inactive-post-retention，slot7 8088/48088。

## Primary Whole-chain Static Review

以下是逐节点源码核对记录，不代表运行验证 PASS。最终源码稳定后复核锚点和指纹。

| 业务节点 | 实际代码依据 | 核对结果与验证边界 |
| --- | --- | --- |
| 管理员选择用户修改 | IntRuoyiFronted/src/views/system/user/index.vue 的 UserForm、openForm、success/getList | 用户行进入独立编辑器，保存完成通知父列表更新；页面角色按钮权限沿既有规则。真实任务用户修改、保存及列表更新通过。 |
| 编辑详情及候选加载 | UserForm.vue open、validateCandidates、validateAssignedPosts；src/api/system/user/index.ts getUserForUpdate | 同时加载编辑正式事实、部门和启用候选；所有来源校验完成才可提交。启用/停用来源冲突明确失败，未过滤既有停用绑定。 |
| 正式停用岗位展示与移除 | UserForm.vue visiblePostList、retainedDisabledPostIds、updatePostSelection | 停用事实有正式名称及“已停用”标记；选择值为精确十进制 Long 字符串，移除后排除同次编辑可重选项。真实 Element Plus tag关闭、候选消失及保存重开通过。 |
| 用户查询/更新接口权限 | UserController getUserForUpdate、updateUser；YudaoWebSecurityConfigurerAdapter；DataPermissionConfiguration | 新详情沿原 system:user:query，更新仍 system:user:update。正式安全链启用方法权限，默认认证；用户 Mapper 仍受部门/用户数据权限。Standalone MockMvc 不证明正式认证链。 |
| 锁定当前用户 | AdminUserServiceImpl getUserForUpdate/updateUser → lockUserForPostEdit → 既有 lockUserForSessionMutation；AdminUserMapper selectPermissionSubjectForUpdate | 编辑链需要可写 Spring 事务、显式匹配 tenant、正 userId；同租户未删用户 current FOR UPDATE。既有公共锁不改。 |
| 既有岗位事实完整性 | AdminUserMapper selectPostIdsForUpdate；service parseStoredPostIds、lockAndValidateUserPosts；UserPostMapper selectListByUserIdForUpdate | 锁内读取原始 JSON，拒非法 token/重复/越界；与同租户 active join 集合精确相等。合法 null+空关联可编辑。有效 join 重复在转 Set 前拒绝，历史逻辑删除不当作有效重复。 |
| 岗位当前有效性 | PostMapper selectListByIdsForUpdate；service validatePostMetadata | B∪T 按岗位 ID 排序取当前锁定读，空集合不查全表；租户/逻辑删除由正式 Mapper 拦截。所有既有项（含将被移除项）必须有有效正式名称/状态；新增项必须启用。 |
| 完整目标集合协议 | UserSaveReqVO postIds 局部反序列化；UserPostIdsDeserializer；service validateEditUserId/validatePostIds | UPDATE 缺失/null 失败，空数组表示移除全部；请求重复按 Set 身份语义处理。非数组/小数/非规范字符串等拒绝。CREATE 岗位可选约束保留，无全局 JSON 修改。 |
| 两份持久化及下游绑定 | service updateUser、updateUserPost；AdminUserMapper/UserPostMapper；getUserListByPostIds | 用户 JSON 写入完整 T；关联只插 T−B、删 B−T，保留交集的原关联行。用户更新行数、新增结果、删除行数必须成功；同一事务异常回滚。下游继续从正式 user_post 读取，非推测集合。 |
| 失败/取消与旧请求 | UserForm.vue formError、openGeneration、submitForm、dialogVisible watcher；正式 request wrapper | 未就绪不能保存，当前加载/保存错误保留正式文本并终止handler（API包装仍reject），无伪成功或未消费异常。关闭/新打开使旧结果不能污染当前编辑器；未保存取消零写入须真实页面核验。 |

## Current Source Anchors

已对齐a1640b169已提交主线，UM04生产/测试与参考依赖指纹未变。主Agent复查：用户页面index.vue:277修改入口；UserForm.vue:274打开、:286正式来源加载、:258移除选择、:322提交、:340成功后通知；UserController.java:182编辑GET、:185查询权限、:65更新权限；AdminUserServiceImpl.java:197更新事务、:262双集合锁内一致性、:306局部异常分类、:327原JSON校验、:358元资料、:589既有正式用户锁、:701编辑读取、:722下游正式关联；AdminUserMapper.java:25用户锁、:37原JSON；PostMapper.java:17排序锁定、UserPostMapper.java:19有效关联锁/排序、:26删除行数；YudaoWebSecurityConfigurerAdapter.java:48方法安全启用，DataPermissionConfiguration.java:21/24用户部门/本人数据范围。以上是静态结论与锚点，不替代真实权限拒绝或MySQL业务验收。

## Actual Verification

- 后端 combined-02：Java17.0.20/Maven3.9.16实际exit0；132 tests/0 failures/0 errors/0 skipped，四类含30个真实Spring事务例和9个HTTP合同例。保存报告XML、源码和class指纹；H2/standalone MockMvc不冒充真实MySQL或安全过滤器。
- 前端标准布局最终native50/50、legacy50/50（同一carrier，不是另外50个场景）；真实完整SFC+Vue renderer+生产API包装，子组件DOM与transport替代，不能记Element Plus/E2E。六文件ESLint exit0、0warnings。原worker报告已通过结构validator并清理；核心verification-evidence.json和本报告保留命令、时间、限制与完整freeze，安全本地副本在output/playwright/um04-20261008-accepted。
- 全量8GB vue-tsc实际exit2、6条白名单外诊断，没有UserForm/user/post/dept API诊断。仅做与当前main五个源码的只读指纹比对：2同、3不同，不宣称main全量重跑或全量PASS，不扩修范围外文件。详见frontend-types-standard-evidence.json。
- 最终隔离依赖：三份逐字节一致前端manifest在任务自有SSD物理目录安装标准node_modules/.pnpm；session51928实际exit0，冻结锁SHA前后相等，无NODE_PATH/alias/配置替换或主node_modules借用。此前外置virtual-store模块查找失败及两次布局安装失败均保留，不作为PASS。
- 第一版源码31模块runtime package（session53698）21:56:58实际exit0，Jar SHA36C1301D…857B5D7A3；11个打包后的生产class与132例测试时class SHA精确一致。package跳过执行测试，132例来自单独实际成功测试，见runtime-class-evidence.json。
- 原测试服务被外部中断后，用户明确授权恢复；正式脚本原包+schema readonly恢复8081/48081，healthUP、frontendHTTP200。该服务不承载UM04新代码验收。自有8088 HTTP200；初始包自有48088已UP；按归属守卫仅停止自有后端；当前基线runtime包22:40:57 exit0，Jar SHA A4FF5308…6334CFBF，11项UM04 class与受测哈希一致；当前自有PID53748/48088 healthUP，8088 HTTP200。
- 真实E2E：任务用户9908090349和岗位26/27/28均由页面创建；真实停用/保留/取消/失败拒绝/单项移除+启用新增/清空/创建候选/页面清理8项通过。16任务写=15业务成功+1精确岗位停用拒绝；无API/SQL写入、force-click或DOM注入。完整安全回执、实际存储阶段及截图哈希见 e2e-summary.json。
- 独立初审和中期未放行记录保留；最终 reviewer 已独立核10张截图、原生trace、自然26回执/16写及7份只读原存储，三层PASS；详见review-report.md/review-decision.json。
- 当前a1640b169基线全量类型重验：实际exit2，仅余BatchReverseTracePanel:299 TS6133和FrontlineFixedTemplatePanel:6555 TS2339；仍是范围外失败，不记全量PASS。UM04源码与4项carrier实际生产依赖指纹未变。已提交基线ff-only同步与package刷新分别留证。

## Remaining Gates

无UM-04剩余门禁。两项全量类型诊断和非目标MES工作台异常保留为范围外限制；不推送或发布。

## Real Acceptance And Error Boundaries

同租户两份正式存储逐阶段只读核验：仅改昵称备注后JSON仍[26,27]且原join163/164所有字段不变；C停用导致business1002005001时用户和joins完整不变；成功BC替换为[27,28]，A163软删、B164原身份/时间保持、C165新建；明确[]后JSON与active joins均空。页面清理后一个用户、三个岗位均正常逻辑删除，shared admin和芋道源码租户保留。

全局错误未忽略：首tab3条、第二tab2条个人工作台角标console errors对应自然page/count HTTP200/business500；后端MES工作台regexp_replace排序规则冲突，目标用户/岗位接口与控件独立通过，按docs/e2e-rules:307-314单列，不宣称全站健康。初始快照85条warnings、补充观察器180条warnings（既有ElDropdownItem directive）留档；目标真实行为独立通过。全程pageerror0，补充观察器requestfailed0、response解析错误0；这些零值不能冒充全量console0。

通知GET business401在15:36:59.041Z触发既有axios正式刷新：refresh POST business0=.182Z、原通知GET business0=.221Z，180ms闭环；12任务业务/准备写在其前，4个清理DELETE在其后且business0。原收集器未预先识别该标准认证分支而FAIL的记录保留；补证通过正式成功回执和源码关联，不滤掉401、不重放已成功业务。刷新URL令牌已脱敏。

脚本运输/控件定位失败全部保留，核实际页面后续验：点击隐藏combobox input被placeholder遮挡，改点原生可见wrapper；一次status父节点错误；一次Post详情可见而异步值尚未到达；都发生在对应保存前，未伪造生产失败或重复已成功写入。后续实际目标场景通过与原脚本FAIL分别记录。






## Integration And Closeout

Implementation commit `4d1a5041ea810ba7c6117b3a7b0b42d0e3e60ac2` fast-forwarded into local int_main with standard Git. All21 incoming normalized hashes match; two unrelated MES files preserved byte-for-byte and remain uncommitted, incoming overlap0. Pre/post merge runtime guards and post-merge main frontend50 tests PASS. No push or deployment.

Cleanup skill preview/apply PASS (keep9/delete9/block0/warn0); owned browser and runtime processes stopped with identity checks, ports8088/48088 free. Safe reviewed screenshot/response/storage evidence subset remains locally in `output/playwright/um04-20261008-accepted`; raw trace excluded. Automatic approval rejected task dependency deletion before execution. User confirmed manual cache deletion and exact absence verified; managed snapshot saved with physical cleanup running. Slot7 release and final records commit remain pending. Status remains ready_for_closeout.

## Final Closeout

PASS: managed归档恢复快照已保存，原checkout及Git登记实际不存在；仅slot7标记inactive，其他73条登记语义保持，8088/48088无监听。用户手动删除确切依赖缓存后独立核验不存在；自动审批拒绝和原harness失败事实保留。九项核心/声明记录结构与凭据扫描通过，精确本地收尾提交不含两个并行MES文件。真实实现提交为4d1a5041ea810ba7c6117b3a7b0b42d0e3e60ac2；记录提交以Git log实际值为准。

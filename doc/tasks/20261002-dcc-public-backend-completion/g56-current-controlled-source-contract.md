# G56 R02 当前受控关联来源正式合同

2026-10-05；状态ready_for_closeout_for_Root_review。Root已移交唯一Java/Maven归属，legacy R01源/测试与67/6XML冻结。本owner仅改DccFileRelationPermissions、Query.getRelationPermissions与必要元数据测试；不SQL/历史改写/currentset创建/FE/其它读写权限。BDD有效RED→currentreactor39/3全部PASS，源/test冻结、Maven释放；Root统一package/runtime/UI/Git/push。

## 精确响应

`DccFileRelationPermissions`新增primitive boolean `hasCurrentControlledSource`；其它现字段不改。它说明**已授权所选文件的同tenant/project/Master目前是否有正式最新受控来源**，不是selected.controlled，也不是canEdit/canPreview/executable或自动正文权限。ID继续原Long字符串。

- Master.latestControlledFileId为NULL → false，不fallback currentActive或扫链，不建currentset。
- 非NULL准确读取该File：实际ID等于指针、同tenant/同Master/同项目、未删，controlledTime存在，versionPolicy.parseStored成功且非横杠WORKING。ACTIVE/CONTROLLED_PENDING_EFFECTIVE → true；futurepending仍是最新受控来源。
- 合法OBSOLETE最新行在上述身份/正式control/version完整且obsoletedTime存在时 → false，保留正常终态历史；不回填或降级旧执行指针。SUPERSEDED不扩为false兼容：当前lifecycle该状态仅收口低WORKING，不能冒正式current来源。
- dangling/外tenant/错Master或project/删行/无真实control/非法version/WORKING或其它noncurrent/OBSOLETE缺作废时间 → 明确数据异常，不能catch后当false。

所选版本可以NEW工作/审批中/旧history且selected.controlled=false，而Master有合法latest受控时flag=true；也可以selected历史曾controlled=true但正规NULL/latest合法OBSOLETE时flag=false。caller只传selectedfileId，所有来源事实server读正式记录，不接受clientmode或project/product/name推断。

## 读取边界

在现 `getRelationPermissions` 已完成所选File名称授权、mutation/Binary权限独立判定、实际Master/project metadata身份之后读取latest事实；保持REPEATABLE_READ。selected.canEdit/canPreview不因此放宽。true只允许FE继续尝试原currentRelations API，原current/history/name/body守卫仍再次核资格；源存在不等于相关集/正文已授权。

FE严格validator必须收到boolean，缺字段/错误类型是DTO或包版本错误，不默认为false。false显示准确无当前受控来源说明，仍可读本版本真实历史；true用原current入口和历史合同。异常不能隐藏、GET不得创建空currentset或回填指针。

## 必要 BDD / 验证范围

- Given未受控selected WORKING且同Master最新正式ACTIVE或未来待生效，When正式元数据读，Thenflag=true，selectedcontrolled=false，canEdit/canPreview保持原值，返回exactLong身份。
- GivenNULL最新/合法最新OBSOLETE，When读，Thenfalse及原selectedhistory可读，不替换来源、不建集合；正常已受控selected/current true正向保持。
- Given损坏指针或foreign/id/Master/project/control/version/state事实，Then明确错误，不能false成功。
- Given合法name-onlyactor，Then可以读名称级正式元数据但false正文权限不变；未授权所选file仍拒绝。
- Before/after原selected/Master/latest/placement记录精确比较，证明只读；不以testrows数量冒真实DB/E2E。

优先现DccRelationNameMetadataDatabaseTest真实H2 Mapper→Query→HTTP JSON，旧source用JSON字段业务断言形成有效RED（无需新增accessor编译失败）；相关Selector元数据/RelationQuery现守卫有限回归。真实库/页面均本ownerNOT_RUN。初步只读发现与计划不作为已修复或全HTML PASS。

## 实际有限验证与交付

初新latestfixture遗漏原表required original_file_id，未到业务，不计RED。补精确原件ID后旧source实际JSONflag为null、selectedWORKING且Master真实ACTIVE要求true，有效RED1fail/0error，原XML封g56-current-source-effective-red.xml。实现primitiveboolean和准确pointer验证后首组合仅OBSOLETE测试错误地把name-onlyselected本身作废而遭原正确visibility拒绝；调整保持原授权selected与另一真实terminalhead，未mock放开历史/正文权限。

2026-10-05 14:37:45 currentreactor三类39执行，全部0fail/error/skip、exit0。Metadata21含primitive HTTPboolean/Long、NULL与selectedcontrolled分开、WORKING选定但真实head存在、futurepending与完整OBSOLETE、10种损坏事实不降级、原identity/place/name-only守卫；Selector15及FormalQueryGuard3相邻合同保留。读前后selectedrow/指针未变，无currentset写或源码SQL变更。

三份XML原bytes保存在g56-current-source-junit；receipt和production2/test1/compiledinventory在g56-current-source-fingerprints.json（sourceRecord44ae0928…f1c37/Querye187d642…c9b70）。本owner停止Maven/源写并释放Root，无实际业务DB/浏览器/Git/package或整体HTML满足声明。

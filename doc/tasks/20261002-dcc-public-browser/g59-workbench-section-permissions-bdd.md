# G59 — 旧工作台按正式菜单分区加载

Status: ready_for_closeout_for_Root_review。共享任务保持in_progress。RootR9/016独立文控明确没有旧training:mine；原三组Promise.all任一拒绝把所有旧指标置0。native线下记录队列独立有正式权限，保留不变。

Given账号只有native task:query资格，没有process-instance:query、旧training:mine或文件query，When实际loadWorkbench及模板运行，Then旧三分区不GET且无0指标/空成功table；native原读取及资格不变，不为页面消红扩权限。

Given现正式菜单允许某旧区，When该区HTTP/业务读取失败而另一允许区成功，Then失败区显示正式错误/未知count、无空成功table，成功区真实row/count保留；不因一错清全部，真实成功0允许显示。

Givenactor/tenant/route/菜单变化，When旧异步读取返回，Then原generation/context过期，响应不能污染新上下文，未授权区不能GET；刷新仍用原各正式API，无新provider或600秒流程规则变化。

唯一生产workbench/index.vue及presentation.ts的nullablecount投影，新增专属actualhandler/门禁/renderer测试，必要旧静态error断言同步；Parent/repairr2/G58category和browser保持。审批门禁是bpm:task:query与bpm:process-instance:query（实际DCC详情认证+文件硬scope非querymenu），确认training:mine，失败browser文件query，都是正式菜单不是角色猜测。先旧源有效RED→GREEN/必要native与下发回归/lint，Root全typesbuild/真实Doc复验。无DB/API/UI/Git/服务/Maven/fulltypesbuild。

有效RED4项全部FAIL/exit1（red.log）：actual原loadWorkbench无menu分区仍全请求、training失败把成功另外两区清空、live tenant late结果误载、原训练模板无gate/error隔离。Vue renderer初缺el-tag宿主注册警告保留，非阻断失败原因；末真实宿主注册后无warning，不修改生产迎合宿主。

实施2生产：旧三分区用正式menucomputed各自gate，独立loading/error/total；原loaderAPI及正文身份装配保。各区错误本区显示并全局聚合，指标number|null保持unknown显示“—”，成功0保持0，未授权section及指标隐藏且0GET。每区成功可独立保留，不全局catch重置0。当前工作台route+正式actor/tenant/三menu上下文与generation作读门禁，live租户漂移未触watch时也清理过期可见数据并明确刷新，不显示旧用户事实。

旧native加载、singleprovider/资格/路由、现独立pendingDistribution panel事实/流程均未改。API错误业务scope即使menus满足仍明确显示，不用类别或对象缺失替代menu判断，不给账号广权、不改变600秒逻辑。原metric route不变，仅count可null。

末3文件19项PASS、0fail/skip、exit0（final-frozen.log），包含新5 actualhandler/menu/late/模板renderer/metrichelper及原native6/下发8项回归，数量不累加历史。2生产ESLint --max-warnings 0实际exit0、无输出；旧workbench-static原合同单跑PASS。没有修改旧approval-load-error-context-static的all0断言，该脚本还含旧G57已不存在pendingDistributionRows要求，未作为本批门禁或声称通过；实际行为由专属测试验证。

Source/testfreeze，g59-workbench-section-permissions-fingerprints.json只替代workbench/index及presentation，新增专属test。Parent/repairr2、G58和Root workingnav保持原字节。Root新完整types/build、独立Doc真实页面待验，离线PASS不代E2E。结构/Keep/指纹校验完成后交Root。

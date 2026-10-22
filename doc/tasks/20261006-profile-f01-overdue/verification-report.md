# F01验证报告

## 放行结论
PASS / completed：F01原实现、定向静态/行为/后端查询回归和用户授权模拟数据的真实页面E2E通过，已融合int_main。本次只补核历史收尾：托管归档、实体目录和所属Git元数据消失、partial-node_modules不存在、slot7释放及核心记录清理全部PASS。原验证证据保持，没有重跑或新增数据写入。F02/F03/F04属于独立后续任务，结果见20261006-profile-remaining-fixes。

## 实现与审核
个人中心列表和角标显式请求includeOverdue=true；服务端用同一分页查询的TODO/OVERDUE集合返回列表和total，保留旧默认TODO及单OVERDUE调用。状态参数冲突显式拒绝。复用原本人/正式候选、租户及终态批次/ARCHIVE例外，不改正式任务身份、状态文案、导航、隐藏语义、权限或50条上限。
审核9个源码/测试差异：6个生产文件及3个测试文件；无生产配置、POM、锁文件、迁移、F02/F03/F04改动。可见逾期行进入原批次详情且保留workTaskId。隐藏只是个人可见性，按原设计不减少业务待办角标，不改变任务状态。

## F01实际业务链逐节点审查
本次结论限定为个人工作台的开放任务查询、计数、个人隐藏恢复及正式页面入口；不表示已验收下游归档、签名或放行业务。以下源码依据均为仓库相对路径，方法锚点可直接定位。

| 业务节点 | 代码依据 | 审查结果及验证边界 |
| --- | --- | --- |
| 个人中心入口与加载权限 | IntRuoyiFronted/src/router/modules/remaining.ts 的 personalWorkbenchTodoBadge；IntRuoyiFronted/src/views/Profile/components/ProfileWorkbench.vue 的 canViewEdhrWorkTasks、loadWorkbench、loadEdhrRows | 实际入口调用本人任务加载器，仅具备任一原查询权限时启用；列表显式 includeOverdue=true，仍为原50条首批，F02不在本次范围。 |
| 请求与接口鉴权 | IntRuoyiFronted/src/api/mes/pro/edhr/workTask.ts 的 getEdhrWorkTaskMyPage；IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrWorkTaskController.java 的 getMyPage | 原 GET /mes/pro/edhr-work-task/my-page，@Valid 与两项原查询权限检查保留；参数新增未引入新接口、数据源或鉴权绕过。 |
| 登录身份、状态参数及异常分支 | 同后端模块 service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java 的 getMyPage、requireLoginUserId | 联合分支要求登录用户，includeOverdue=true 与非空 status 冲突显式报错；旧默认TODO与单OVERDUE路径保持。错误不会转换为成功的默认数据；有定向回归证据。 |
| 正式任务数据与可办理集合 | 同后端模块 dal/mysql/pro/batchrecord/MesProEdhrWorkTaskMapper.java 的 selectMyOpenPage、baseMyWrapper、applyMyTaskVisibility、applyOpenWorkTaskBatchVisibility | 读取 mes_pro_edhr_work_task，以同一分页查询取 TODO/OVERDUE；复用本人/正式候选精确token与终态/ARCHIVE条件，不拼接两个分页结果、不改任务状态或落库。候选与终态分支有查询测试；租户拦截配置未改，本轮没有新增跨租户E2E，不宣称完成全租户隔离审计。 |
| 响应、行身份及角标 | 同Service的 buildWorkTaskRespPage；ProfileWorkbench.vue 的 mapEdhrWorkTaskRow；IntRuoyiFronted/src/store/modules/profileWorkbenchTodoBadge.ts 的 loadEdhrWorkTaskTodoTotal、refreshTodoTotal | 响应保留 page.total，行保留实际工作任务ID、原状态和对象；角标用相同联合查询的服务端total，加载失败继续抛错。生产loader单测与页面4983→4985→4983验证通过。 |
| 隐藏恢复与实际写入 | ProfileWorkbench.vue 的 handleHideTodo、handleRestoreTodo；IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/profileworkbench/ProfileWorkbenchTaskVisibilityServiceImpl.java 的 hideTask、restoreTask；对应 ProfileWorkbenchTaskVisibilityMapper.deletePhysicalByUserAndTaskKey | 隐藏只写 system_profile_workbench_task_visibility，使用当前用户/租户；恢复按tenant_id、user_id、task_key删除。页面确认、隐藏、恢复均通过；只读核验任务状态未变、个人隐藏记录为空。原业务角标不因个人隐藏减少。 |
| 下游正式页面入口 | ProfileWorkbench.vue 的 openTodo；IntRuoyiFronted/src/utils/edhrWorkTaskNavigation.ts 的 navigateToEdhrWorkTask、normalizeEdhrWorkTaskRouteParts | 保留原任务类型分支与正式身份，没有以TODO状态阻断OVERDUE。实际ARCHIVE逾期任务从页面到原批次详情，workTaskId保留且无加载失败。FILL/REWORK导航有相邻静态回归，本轮未执行其下游填写/签名/状态推进。 |

## 定向验证
- F01静态合同：node tests/e2e/profile-edhr-overdue-static.spec.cjs；修复前退出1缺少联合选项，修复后退出0。
- 相邻角标、统一列表、隐藏恢复、eDHR通知导航静态合同PASS；新增两个前端文件node --check PASS。
- node --test tests/e2e/profile-edhr-overdue-behavior.spec.cjs：3/3 PASS，退出0。执行生产loader AST，验证联合请求、服务端total和失败传播；网络边界为单测桩，独立于真实E2E。
- 目标3个前端生产文件ESLint PASS，退出0。
- 后端：mvn.cmd -B -ntp -pl yudao-module-mes -am '-Dmaven.repo.local=D:/IntRuoyiTaskRuntime/profile-f01-overdue/maven-repository' '-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage*' '-Dsurefire.failIfNoSpecifiedTests=false' test：退出0，BUILD SUCCESS；5/5 PASS，failures/errors/skipped均0。涵盖联合与默认/单状态、分页COUNT、非开放状态排除、状态冲突、候选99与199精确匹配、终态30/40/50/60及ARCHIVE例外，另3项原回归。
- Surefire XML：D:/IntRuoyiTaskRuntime/profile-f01-overdue/backend-targets/yudao-module-mes/surefire-reports/TEST-cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskServiceImplTest.xml；SHA256=11257AC8E88F59F7324100B58FBAB80A57D55A84A4DCE558C51E67C13FA64148。
- git diff --check、任务记录结构/状态JSON、临时POM原字节恢复及端口guard PASS。端口guard为codex/profile-f01-overdue/int_main slot7=8088/48088。
- 独立实际运行包构建31/31 SUCCESS，退出0，2026-10-06T19:32:07+08:00；运行jar SHA256=66ECC38AB2A54C08F0FCD4F5FB686A615554AF8095D89413A72133CB06B8A592。构建用于E2E启动，skip测试参数不计作额外测试PASS。

## Playwright实际页面E2E
认证：用户指定芋道源码/admin，仅运行时使用密码，文档与提交不记录明文。页面8088/user/profile，真实任务后端48088；没有直接fetch/API调用、路由拦截或mock。用户批准“没有数据，你来模拟”，允许准备/清理任务标记夹具，不将模拟任务宣称为自然生产样本。

1. 旧主前端8081页面复现：TODO2754可见、OVERDUE2755缺失；baseline-overdue-missing.png。
2. 第一轮夹具：模拟批次900000001235，任务2754—2758对应TODO/OVERDUE/DOING/DONE/CANCELED，类型ARCHIVE，批次CLOSED。实际新页面仅前两条可见；OVERDUE显示“已逾期”，其余三条不加入。f01-open-rows.png。
3. 页面隐藏2755并真实确认，进入“已隐藏1”；状态仍“已逾期”，角标4984保持原业务总数。页面恢复并切回待办，原2755行返回、状态/角标不变。f01-hidden-row.png、f01-restored-row.png。只读DB核验五个状态未变、隐藏记录已恢复为空。
4. 点击2755“进入/处理”：到/mes/pro/feedback/edhr-batch-execution/detail?id=900000001235&workTaskId=2755，实际详情加载完成、无加载失败alert；f01-formal-navigation.png。F01仅验证正式导航入口，不执行归档/签名/任务完成等扩大范围动作。
5. 第一轮夹具清理5条任务、1条批次，残留0。较长测试时间窗内并行任务改变总数，4984→4982的首次清理差量断言超时；不得作为产品失败或计数PASS。第二轮在短窗口重做明确增删验证。
6. 第二轮夹具：批次900000001237，任务2761—2765同五种状态；真实页面基线4983，插入五条后4985，只有TODO/OVERDUE两条显示。清理五条/批次后页面reload回4983，DOM无模拟行、无加载失败alert。4983→4985→4983精确PASS；f01-count-plus-two.png、f01-after-cleanup.png。两轮任务均有固定mark=SIMULATED:F01:20261006-profile-f01-overdue，只删除对应ID/租户/所有者/标记；既有工单1009212217仅引用，不写入。

所有截图及临时Playwright步骤文件位于D:/IntRuoyiTaskRuntime/profile-f01-overdue。CLI run-code使用UI定位和DOM断言；页面状态可见及只读状态核验作为结果依据。隐藏首次误用会被ElementPlus span遮挡的radio输入定位超时，随后点击真实标签成功；错误记录不抹除。首次错误假设隐藏应减少角标，复核已批准原设计后纠正，未改产品语义或放宽业务断言。

## 已知基线风险与环境边界
- pnpm ts:check退出2：src/utils/notifyMessageNavigation.ts(197,25) TS2677。该文件未改且与HEAD一致；临时恢复本任务3个前端文件至HEAD执行同一命令仍同诊断，finally恢复并核对hash一致。全项目类型检查FAIL保留，不声称全量PASS，不扩大第一项修复范围。
- 任务后端独立运行遇到原Quartz datasource配置及AI自动配置冲突，两次启动FAIL后只修正临时参数；最终实际服务启动PASS、页面功能验收通过。禁用Quartz保留原两个AI排除项，防止共享作业；过程内随机测试签名值获用户批准，不修改生产配置或增加fallback。
- 现有ReleaseWorkflowRecoveryScheduler后台仍输出RELEASE_WORKFLOW_OPERATION_TERMINATION_UNCONFIRMED，属于相邻放行恢复流程的既有运行态异常；F01页面无加载失败，未吞错或扩大修复。任务运行进程在收尾预检已不存活，8088/48088无监听；主服务没有被本任务停止/重启。
- E盘读取缓慢时使用任务专有D盘依赖/target。3154个MES源码镜像逐一字节hash相等，父lombok.config同字节；临时sourceDirectory POM已恢复SHA256=279EED96435600176853265D0338FE5E6DB73F55B09CE4660B9386BC64EE37DA且git diff为空。源码镜像manifest SHA256=9c886b201956c98d55b9c4ca090fde31b16996b179eb67a60e6ac8960e287982。
- 未执行F02全局分页/容量、F03资料编辑、F04改密或全量回归；当前用户门禁是F01定向静态与E2E。

## 经验核对
按project-experience-consolidation检索已有docs/e2e-rules.md、docs/worktree-memory.md：真实页面、空样本阻塞、独立依赖、基线错误区分、补丁绝对路径等通用规则已覆盖。临时D盘目录、模拟身份和启动参数仅保留任务证据；不新建长期经验文档，不编辑并行任务的经验文档。

## 收尾证据
实现/验证记录提交、核心cleanup preview/apply、主线并行文件指纹保护及ff-only融合均通过；剩余托管worktree归档、目录清理、slot7释放及completed记录待执行。按照最近根AGENTS，不基线提交并行改动、不自动推送、不重启主后端。

- 实现提交：f836485d04cf69f5389d28458719ae66abea6578（9文件，178 insertions/6 deletions）。
- 核心清理preview/apply PASS，7个keep/0个delete、blocked/warnings为空；核心任务目录仅4份记录，9文件源码指纹未变。首次apply FAIL(WinError3)及扩展路径apply中断保留历史；两个目录已删，partial-node_modules待托管归档并核验不存在。并行删除被自动审批拒绝(blocked by policy)，未执行；JSON回执见D盘cleanup-core-preview.json、cleanup-core-apply.json。

## 最终源码融合与收尾状态
- F01代码融合PASS：int_main已快进到e7be02862a6abf10248009af5094570e716f7ce2，包含实现f836485d04cf69f5389d28458719ae66abea6578。9个源码/测试与验证版本一致；五个并行文件SHA256未变。根端口guard通过，没有推送、发布或主后端重启。
- 核心记录清理PASS，授权模拟数据两轮残留0。托管archive_worktree queued后快照ls-files超时60秒，归档队列持续无法确认任务归档状态；附件archived_worktree不能等同目录清理成功。物理worktree、元数据、partial-node_modules及slot7(active=true)保留，尚不标completed。
- 实际待收尾：应用归档成功并核对目录/元数据消失、依赖残留不存在，再在互斥锁下释放slot7并提交completed记录。F01验证和已融合结论不回退，无需重复构建/业务测试。

## 历史收尾核验 / 2026-10-08T13:51:00.679290+08:00
托管artifact为archived_worktree，原精确checkout路径和所属Git元数据均不存在，partial-node_modules不存在；原互斥锁下slot7已active=false，其他登记项保持。主目录cleanup preview/apply keep6/delete0、blocked0/warnings0。旧归档超时记录为历史状态，IntRuoyi5属于其他任务并未操作。本次仅纠正记录，未重跑、推送或重启主服务。

## 历史收尾纠正 / 2026-10-08T14:41:02.477747+08:00
归档超时与目录保留描述是历史状态，当前已实际核验archived_worktree、精确checkout及所属元数据不存在、partial-node_modules不存在，slot7在原互斥锁下active=false。IntRuoyi5归属其他任务，未操作。主目录cleanup preview/apply keep6/delete0、blocked0/warnings0；同步机器状态completed及M3完成。原F01源码/E2E证据不重跑，未推送或重启主服务。

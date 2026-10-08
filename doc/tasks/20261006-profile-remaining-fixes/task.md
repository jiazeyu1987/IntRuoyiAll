# 个人中心剩余问题修复与主干融合

## Task Goal
在独立 worktree 用子 Agent 修复原方案剩余 F02、F04、F03，由主 Agent 做实际业务链 review 和静态代码审查；每项审查通过后才融合 int_main，未通过则退回子 Agent 修改。

## Milestones
- [x] M0：核对原修复文档与当前代码，建立独立 worktree、预约槽位及隔离评审状态。
- [x] M1：按优先级实现 F02 首批截断修复，通过静态业务链评审、编译/类型/lint和审查闭环。
- [x] M2：实现 F04 改密防重复提交及成功清除敏感字段，并通过定向验证和审查闭环。
- [x] M3：实现 F03 无联系方式账号可只修改昵称，并通过定向验证和审查闭环。
- [ ] M4：精确提交、融合主干，清理本任务资产并核验最终状态。

## Expected Verification
- 对原验收逐项提供实际代码链证据；F02 按用户批准仅静态评审+编译/类型/lint，F03/F04 保留定向行为和临时内存 H2 验证，不能只靠关键词合同判通过。
- 主 Agent 从个人中心入口追踪接口、权限、服务、数据写入和下游结果；独立 reviewer 提供结构化意见，主 Agent 保留最终放行责任。
- F02 编译/前端类型/目标 lint、F03/F04 定向行为和回归、差异与范围检查通过后才能融合；全项目既有错误与本次新增错误实际对照并区分。
- 本轮要求 review 和静态代码审查，没有新增真实 E2E 要求；不以 mock 测试宣称真实 E2E，不进行账号改密等环境写入。
- 按原优先级 F02 → F04 → F03，全部原范围必须完成，不能用容易通过的局部修补替代 F02 完整范围。

## 设计约束检查
- 已完成并融合的 F01 不重新开发；保留其 TODO/OVERDUE 集合、本人/正式候选、租户、批次终态及 ARCHIVE 例外。
- F01托管artifact已为archived_worktree、原物理目录不存在；不重新开发或使用其归档checkout，新隔离worktree保护所有并行资产。旧收尾记录将在实际slot7释放后按归属纠正；不得操作无关IntRuoyi5元数据。
- 子 Agent 默认 gpt-5.6-luna，只在指定隔离目录和分工路径修改；不自行放行、提交、融合、推送。
- 最近根 AGENTS 覆盖旧技能和关联文档默认 BDD/TDD、全脏基线提交及推送条款。用户授权本任务实现所需的精确提交和主干融合，没有授权推送、发布或重启主服务。
- 不引入 fallback、吞异常或默认成功；缺少必要前提准确记录。验证只使用任务自有资产，不修改既有数据库/账号。

## Current Status
ready_for_closeout：纯格式整改已核对21文件仅空白变化，实际增量依赖编译成功；F02完整业务链静态审查和476补充审查通过；最新476源码31模块依赖闭包编译成功、默认后端98项通过，前端55项、目标lint/5个SFC编译通过。类型检查全量FAIL，但与476不可变基线诊断完全一致、新增0。主Agent按批准门禁放行，F02运行/真实数据/E2E明确未验证；待精确提交、清理、FF融合、托管归档及槽位释放。

## Cleanup Keep
- doc/tasks/20261006-profile-remaining-fixes/task-state.json
- doc/tasks/20261006-profile-remaining-fixes/acceptance-amendment.md
- doc/tasks/20261006-profile-remaining-fixes/f02-backend-contract.md
- doc/tasks/20261006-profile-remaining-fixes/integration-manifest.json
- doc/tasks/20261006-profile-remaining-fixes/verification-evidence.json


## 实施归属和隔离环境
- F02 backend：profile_f02_worker；F02 frontend：profile_f02_frontend；F03/F04及共享profile.ts：profile_f03_worker。主Agent集中运行验证并负责state/最终放行。
- 槽位11，int_main profile，前端8092/后端48092；当前未启动服务，不使用主干48081。
- F02仅静态+编译/types/lint，F03/F04原定向行为/Validator/HTTP/H2回读；禁止任何真实MySQL连接、数据或账号写入。


## Closeout Policy
cleanup使用--worktree-closeout off，只清理本任务目录。最近AGENTS的任务归属规则覆盖旧文档/技能全脏基线提交和主目录全洁净要求；逐路径核对并保留所有并行修改，按批准范围精确提交后FF融合int_main。托管worktree用app archive，不用shell或Git remove；归档完成和目录不存在后再释放本任务slot11。没有推送/发布/主服务重启授权。

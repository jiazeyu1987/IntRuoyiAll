# 产品绑定迁移准入与旧版恢复依据

## 结论

应用修复提交：`b6cc9f536690f1849531f2c470cedbb74fc1b7fd`。在不改原迁移、不处理业务冲突的前提下，新增正式准入迁移及同名只读预检；使用现有 `migrationDependencies` 按原SQL的CRLF SHA256锁定前置依赖。真实发布工具已识别并绑定该预检，原产品迁移必须排在准入之后。

**新版继续阻断；本次不是数据修复或发布许可。** 338行冲突仍需业务决定，不能自动推断绑定、覆盖快照、删除证据、跳过原迁移或把失败记为APPLIED。可行的无业务写入方案是恢复原版本服务、保留冲突证据、暂停该产品能力的新版发布；如需先交付其他功能，应另行明确裁剪范围并构建完整独立版本，不能只从当前包剔除产品SQL，因为新代码直接依赖该表。

**停服前执行还存在维护侧缺口。** 本应用修复补齐强制绑定和停写后的重复断言，但当前维护 `intruoyi_runtime_agent.preflight` 没有调用目标数据预检。维护线程应补上停服前执行，才能避免同类冲突再次导致停机。不能将本应用修复描述为该端到端缺口已经全部消除。

## 已有证据与根因

- 固定旧候选：`469b95273494aa59b84644a1ad0e495596a51e6d`，构建 `JOB-e7c8f64f25` / `2026-11-26-23-54-25` 成功；`JOB-9fb8ac2ed9` 预检成功；`JOB-a2775541ef` / `OP-0cedda93cd80e6cab84e` 在产品迁移报ERROR1644/45000。这里只读本机已有证据与维护状态，不连接服务器。
- 本机证据 `C:/SpugHandoff/release/private/test-prod-20261126/product-conflicts-query.json` SHA256：`9e933c37cef6135613b56944c50b55bb36d79366e24874e3c1205bb127d1ba87`。独立解析得到338行：UNRESOLVED_PRODUCT 102、SNAPSHOT_PRODUCT_MISMATCH 140、MULTIPLE_CERTIFICATES 2、RELATION_CERTIFICATE_MISMATCH 94。是结果行数，不是去重证书数；私密明细不提交。
- 原 `20261125_dcc_registration_certificate_products.sql:1` 为schema且未声明requiresTargetPreflight；注释要求先预检，但注释不构成工具合同。原配套预检包含SET、无正式元数据/PASS标记，且未覆盖既有绑定表冲突，不可直接声称已经正式接入。
- 维护 `intruoyi_build.migration_inputs:160` 只对非schema或requiresTargetPreflight=true绑定目标SQL；`target_input_plan:223` 同样只检查该集合。因此此前输入计划通过不能证明产品身份业务条件满足。上一轮记录保留为当时实际检查结果，本记录明确补充其业务准入缺口。
- 维护 `intruoyi_runtime_agent.preflight:705` 最后调用migration_plan和snapshot_precheck后返回，没有执行普通target SQL；`deploy:1168` 先preflight，再stop_writers、捕获恢复点，随后 `apply_migration_plan:1280` 才在各迁移前执行 `run_target_preflight:1371`。本次既漏绑定，也缺停服前执行位置。

## 应用修复设计与检查边界

1. 新增 `20261127_dcc_registration_certificate_products_admission.sql`：schema/high/requiresTargetPreflight=true/applyOrder=1；只校验，不建绑定表、不改业务行。依赖原产品迁移已有的两个结构前置，避免循环。当前两环境计划的第一个APPLY即准入，避免在发现产品冲突之前先修改菜单/通知/材料；这不替代停服前执行。校验过程重复同名只读预检的数据谓词，停写后的数据变化仍会阻断。
2. 原产品SQL按实际发布CRLF摘要 `053edc551e2a9ebc87c47fda9e1c0e095245b6c4b09b754d90cc066be13b0123` 增加准入前置；原SQL、原诊断SQL字节不变，没有改迁移类型、supersedes、范围或历史台账。
3. 新单条WITH/SELECT严格复用原迁移的数据来源：CONFIRMED关系及已COMPLETED且生成ID一致的APPROVED关系；覆盖已发布及待上传预约证书、当前自产快照、跨租户产品唯一性和关系引用证书冲突。仍按租户连接，不填充缺失身份，不放宽原断言。
4. 既有绑定表使用明确阻断 `PRIOR_BINDING_TABLE_REQUIRES_REVIEW`：单条静态SELECT不能在表不存在时又安全引用其内容。当前候选的原迁移尚未APPLIED，发现该表即要求对部分失败/恢复状态单独审查，不能假定为空、自动删表或推断迁移完成。这是有真实部分执行证据的更严格准入；不影响已APPLIED原迁移的历史字节保护，但若后续另部署本准入迁移且表已存在，也会保守阻断，必须另行审查，不宣称普遍重试兼容。

## 维护侧必须补齐的停服前执行位置

维护线程应在已完成stage校验后、调用stop_writers前，对实际APPLY计划中全部已要求的targetPreflight执行同一只读、单PASS合同，并继续保留 `apply_migration_plan` 的停写后重检。直接调用现有 `run_target_preflight` 可复用包内摘要、stage回执、环境和输出校验，但必须确认root已stage且引用的是同一manifest/plan。

独立check-prerequisites路径同样应执行该集合；不得只修构建命令而让publish路径仍漏检。没有当前表/列的前置结构条件应明确阻断或给出经审查的分阶段检查，不能吞错或模拟通过。就本准入而言，全部业务表已由既有前置提供，且查询不引用新绑定表内容，可以在服务仍运行时检查；停写后仍需重检以消除并发变化。

本任务不修改、安装或提交维护仓库代码，不操作服务器。应用合同已可被现有工具识别；维护端前移执行和真实58数据阻断验证仍由维护线程完成。

## 验证结果

- 新增可复现测试 `scripts/tests/test_registration_product_admission_mysql.py`，仅接受带本任务label、network=none、无映射端口的隔离MySQL8.0.39。24组正常/异常场景分别执行只读事务内预检和准入迁移断言，两者结果一致；覆盖来源状态/生成身份、重复来源、多产品、空码名、跨租户、快照ID、预约上传、多个证书、关系证书冲突及残留表；无业务修复。
- 另运行真实原产品迁移：合成缺关系数据准确触发原首个45000；留下0行绑定表，快照SHA256不变，无新表外键，旧证书→快照查询仍通过。共2项MySQL测试通过，其中1项含24组场景。
- 来源登记及准入依赖回归3/3通过；普通/暂存菜单检查、提交hook、git diff --check通过；新只读SQL通过真实目标合同校验。
- 固定修复提交的干净稀疏checkout执行真实 `migration_inputs`、`historical_integrity`、`historical_package_integrity` 和 `target_input_plan`：937份SQL全量归类、925条迁移、1254项资产、8项来源引用；原封包全部迁移和历史台账摘要保留。
- 本机台账快照：测试855条，正式851条，各6条APPLY、4项必需预检，缺失0；准入在原产品迁移之前。逐环境仅在内存去掉新绑定，真实工具准确拒绝missing(1)。详见 `release-input-verification.json`。快照观测时间为2026-11-26早间，早于此次失败，不能冒充失败后58实时台账；运行计划应由维护重新采集。
- 验证使用实际CRLF checkout字节；未直接用主工作区可能残留的LF文件替代包摘要。私密业务证据只做只读计数，没有导入本机测试库或提交仓库。

## 旧版恢复：可保留DDL的条件依据

本机原包manifest指向 `2026-11-26-01-52-12` / `eb866bbaf4ea1b1c6db93daa63e77576766c3684`。针对该固定Git的Java/XML/YAML运行源检索无 `dcc_registration_certificate_product` 引用；本地重现原首个断言失败只新增孤立空表/索引/CHECK及校验过程，没有触发器、外键或对旧表的DDL变更。原SQL在所有业务断言成功后才INSERT绑定、DROP旧快照触发器和更新facts_json。因此，**若维护只读确认实际也停在这些断言、空表未写入、旧触发器和旧快照未改，可保留该孤立DDL而使用原镜像恢复**；没有必要为此删表或手工修改失败台账。

这不是完整恢复许可：

- 338行诊断本身不能精确定位实际失败断言；流水线公开日志仅给45000。维护必须对恢复点、当前表行数、旧快照触发器、已执行迁移及既有容器身份做只读核对，不能单凭本地重现推断服务器未写。
- 失败任务的原计划中，文件夹模板菜单位于产品迁移之前，执行器逐项成功即提交并记APPLIED。原旧Git没有 `IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/folder-template/index.vue`，因此此前已提交的菜单/角色/套餐授权可能留有旧前端不可用入口。DDL兼容不能证明菜单兼容；恢复点在停写后、SQL前捕获，可用于恢复原菜单/权限/台账的准确状态，具体恢复由维护负责，不能随意删菜单/改台账。
- 后续CE材料、类型通知和FDA日期在原计划中位于产品迁移之后；仍需以实际台账确认未执行。新类型通知会要求旧应用不提供的certificateTypeName，不能猜测已执行后也兼容旧镜像。
- 新候选代码直接调用ProductBindingService并查询绑定表；恢复旧版不等于新版可带空表上线。338行冲突保持阻断，正式57不得据此晋级。

恢复SQL7,196,341,458字节、两份卷归档及recovery-record存在与旧前后端停止，是维护保存的本机观察证据；本任务未检查归档可恢复性、未访问58，也未执行恢复。维护应优先使用既有受控恢复流程完成原镜像/配置/菜单/数据库状态的成对核验，再报告健康与重新开放结果。

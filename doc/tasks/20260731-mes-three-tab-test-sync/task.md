# 芋道源码三页签测试服同步

## Task Goal

将本机 `ruoyi-vue-pro` 数据库中 `tenant_id=1`（芋道源码）的三个页面数据，受控同步到测试服务器 `172.30.30.58` 同库同租户：

- 工序设置列表
- 工艺流程
- 排产工单

本任务只允许同步上述三页签正式数据；用户后续已追加授权同步三页签运行所缺失的物料、用户、生产工单依赖，并授权确定性 ID 重映射、剩余 schema/表单版本/权限范围/日历/工作站/外部引用处理，以及页面验证发现的最小批记录报表元数据依赖。仍不得跨租户覆盖、删除、复用冲突主键或同步无关业务数据；所有扩展范围必须有备份、显式证据和后置复核。

## Milestones

- [x] 建立任务证据、读取经验门禁并确认当前 Git/远端状态。
- [x] 生成并运行只读 preflight，复核源端范围、目标依赖、schema 差异和白名单外引用。
- [x] 若 preflight 阻塞仍存在，记录阻塞并保持测试服零写入。
- [x] 记录用户追加授权的缺失物料、用户、生产工单依赖同步范围，并验证目标库全局主键冲突。
- [x] 按用户授权生成确定性 ID 重映射并同步授权依赖，预检按映射校验用户/生产工单引用。
- [x] 按用户“继续授权”完成 schema 对齐、表单版本/权限范围、日历/工作站、外部引用清理和测试服备份证据。
- [x] 在单事务中按显式列清单替换白名单数据，完成行数、主键、业务键、hash 和白名单外零变更复核。
- [x] 使用 Playwright 登录测试服真实验证三个页面列表和关键接口。
- [x] 完成 cleanup preview/apply、经验沉淀、提交和推送前检查。

## Expected Verification

- BDD/TDD 证据写入 `execution-log.md`，生产变更先 RED 后 GREEN。
- 只读 preflight 必须证明：依赖完整、schema 对齐、外部引用安全、源/目标范围可替换。
- 数据写入前必须存在测试服逐表备份和恢复命令证据。
- 数据写入后必须逐表比对行数、主键集合、业务键、显式列 hash，并验证白名单外数据未变。
- 真实 E2E 必须通过测试服前端进入工序设置、工艺流程、排产工单三个页面。
- 若任一前置或验证失败，本任务状态保持 `blocked`，不得标记完成。

## Current Status

completed

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。缺失依赖、schema 差异、外部引用或校验失败均阻塞。
- `是否从根因和长期维护角度解决`：是。先对齐正式 schema 和依赖/引用门禁，再同步正式白名单数据。
- `是否存在临时补丁或绕过`：否。不使用默认值、临时映射、API-only 伪验证或全库重置。

## Preflight Notes

- 本任务开始时工作区已由并发任务生成基线提交 `6a1390ff`，当前分支 `int_main` 领先 `origin/int_main` 1 个提交；本任务不修改该并发提交内容。
- 当前默认策略为安全阻塞：若缺失依赖或白名单外活跃引用仍存在，本轮只输出阻塞证据，不写测试服数据。
- 2026-07-31 只读 preflight 已确认源白名单范围为 `2,989` 行、测试服当前白名单范围为 `1,096` 行，且存在 `13` 项硬阻塞；本轮未执行任何测试服写入、备份恢复或服务重启。
- 阻塞包含：测试服 schema 未对齐、源路线快照超过目标 `TEXT` 容量、动态表单版本/权限范围/物料/生产工单/日历/工作站/用户依赖缺失或不一致，以及白名单外 `19` 组活动引用仍指向将被替换的数据。
- 用户已追加授权缺失 `mes_md_item.id=924005`、`system_users.id=910269` 和 `33` 个缺失 `mes_pro_work_order` 依赖同步；重新预检后授权范围被识别为 `3` 类依赖，剩余阻塞为 `11` 项。
- 授权依赖同步当前仍阻塞：测试服 `system_users.id=910269` 已被 `tenant_id=122` 占用，且 `13` 个缺失生产工单 ID 已被 `tenant_id=122/162` 占用；在未获得“主键重映射并同步更新三页签引用”或“清理/覆盖其它租户冲突行”的明确授权前，不能插入这些依赖到 `tenant_id=1`。
- 已只读确认测试服 `tenant_id=1` 中授权依赖仍未插入：`mes_md_item=0`、`system_users=0`、`mes_pro_work_order=0`；前次失败尝试留下的三个备份表均为空行，仅作为失败证据保留。
- 2026-07-31 已按用户授权执行确定性 ID 重映射：`system_users.910269 -> 910293`，`mes_pro_work_order` 共 `18` 个冲突源 ID 映射到 `925781..925798`；另有 `20` 个生产工单保留源 ID 插入，`2` 个生产工单在目标已与源身份一致。
- 授权依赖同步已完成并通过后置校验：`mes_md_item.id=924005`、重映射用户、`38` 个插入生产工单均在测试服 `tenant_id=1` 可按源业务身份解析；本轮创建备份表 `mes_three_tab_dep_remap_backup_20260731012048_mes_md_item`、`mes_three_tab_dep_remap_backup_20260731012048_system_users`、`mes_three_tab_dep_remap_backup_20260731012048_mes_pro_work_order`。
- 重跑只读 preflight 已加载 `dependency-remap-plan.json`，物料、用户、生产工单依赖阻塞解除；随后用户“继续授权”覆盖剩余 schema、表单版本、权限范围、日历、工作站和外部引用处理，主三页签白名单同步已执行并复核通过。
- 2026-07-31 主同步替换 20 张白名单表，源端与测试服目标均为 `2,989` 行；逐表 `source_hash == target_hash`，无 missing/mismatched/active_extra。
- 首次测试服真实页面验证登录和滑块验证码通过，但 `/admin-api/mes/pro/process/page` 返回 `系统异常`；后端日志定位为 `Missing batch record report: routeProcessId=928609, reportId=1d05410f1d3140c5b8aa6786887ae69c`。
- 已按最小页面运行依赖同步 `mes_pro_batch_record_definition.id=47`、`mes_pro_batch_record_version.id=130` 和 14 条 `mes_pro_batch_record_report` 元数据，并创建 `m3brepbk_20260731115458_*` 备份表。
- 最终只读 preflight 结果 `blocker_count=0`；Playwright 真实登录测试服后，工序设置、工艺流程、排产工单三个页面列表接口和页面可见断言均 PASS。

## Applicable Experience Gates

- 测试服操作门禁：远端目标固定为 `172.30.30.58`，任何写入前必须证明目标环境、运行目录、备份、恢复路径和真实运行态验证；不得触碰正式服或备用服。
- 远端 MySQL 门禁：通过 SSH/容器标准输入执行 SQL，避免本地 PowerShell 或远端 shell 提前展开数据库密钥；证据不得包含密码、token、私钥或连接串密钥。
- release migration 门禁：若新增 schema SQL，首行必须有结构化 `release-migration` 元数据，配套 `script/tests` 静态契约，并通过全量 migration policy gate。
- 工艺路线导入完整性门禁：路线、路线工序、BOM、关键工序、批记录表单绑定和快照必须逐链路校验；不得用 `formBindings`、默认 `MAIN` 或其它表单链路代替批记录表单。
- 排产数据包门禁：排产工单迁移必须保留 `scheduleOrderId + routeProcessId` 快照身份，不能用当前路线、默认日历、默认产能或空快照重建。
- 外部引用门禁：白名单外任何活动业务表仍引用待替换工序、路线、路线工序或排产记录时必须阻塞，不得删除、改写或静默重映射。
- Git 并发基线门禁：最近非本任务基线提交 `6a1390ff` 不得伪装成本任务实现；本任务后续只选择性暂存自身文件。

## Cleanup Keep

- doc/tasks/20260731-mes-three-tab-test-sync/artifacts/
- doc/tasks/20260731-mes-three-tab-test-sync/tools/

## Final Verification Result

- `three_tab_sync_preflight.py` PASS：`blocker_count=0`，source/target whitelist rows 均为 `2,989`。
- `verify_test_server_three_tabs.mjs` PASS：测试服真实登录后，工序设置 `65`、工艺流程 `3`、排产工单 `10`。
- `task-closeout-cleanup` preview/apply PASS：保留任务记录、tools 与 artifacts；无删除、无阻塞、无 warnings。

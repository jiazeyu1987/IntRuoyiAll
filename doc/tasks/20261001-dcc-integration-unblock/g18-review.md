# G18 主管理 Review 与运行准备

2026-10-03。主目录实际 int_qms，整合树实际 codex/20261001-dcc-integration。当前实现不合入中间状态；真实页面和最终合入尚未关闭。

## 已核验交付

- 后端 G13 两生产/五测试文件原始 SHA256 全匹配。公开 HTTP→实际签名 v4 HMAC→统一签名→实际 BpmTaskServiceImpl/Flowable 测试，560/11 类无失败、错误、跳过。Root 阅读 canonical owner suffix 与同签名绑定、不同批准 task 更新当前 projection/保留旧证据、native MATRIX_APPROVAL 只给 PROCESS_IN_MODULE；隔离外部用户/存储/审计 fixture 的边界按子报告保留，不称真实页面。
- 前端 G14 detail/index.vue 和 G16 browser/index.vue LF SHA256 与子交付完全匹配。当前公开批准/owner/detail/training/application round 和检入/上传/清理/版本 selector 9 文件组合 68 PASS（g18-frontend-regression.log）。G16 MINOR-only、精确 Master/源版本/当前检出人/票据会话、晚响应/已保存刷新失败已读源码；正式升版仍独立所选工作正文申请。
- Root 项目 vue-tsc -p tsconfig.relaxed.json、8GB heap，exit0；正式 env.local Vite build exit0/Build successful。保留现有 Browserslist 数据过期提示，未改依赖。任务 yudao-server -am -DskipTests package exit0，10:55:08，当前 exec Jar 507250746 bytes；未启动服务。

## 运行库与迁移

- Docker restore 授权不等于升级授权。实际数据库 ruoyi-vue-pro / MySQL8.0.40，已确认未进行本任务 DDL/DML。G18 读取 defaults/collations/生成表达式、BPM V1–V3、policy、template、job；目标 V4 为空。
- 两租户各三动作 V3 procdef/model/info 已存在，V1/V2暂停、V3活动。旧 BPM XML hash已冻结，V4正式脚本只新增定义/模型/配置，不改 V3历史。
- UPLOAD DRAFT published 已在；旧 publish BPM_REQUIRED disabled，DIRECT published 已在；ACTIVE OBSOLETE published 已在且正式key/executor正确；待生效作废 policy、remediation template、activation job 都缺。旧 seed/目录/菜单/权限不重放。
- Master normalized_file_number utf8mb4_bin、现有新 logical identity 两索引及 claim active generated事实已核验；旧 master chain unique 仍在，正式 P1负责清理。容量 reason255→2000 纳入既有正式前向迁移。
- 扩展正式 metadata/manifest/policy gate 为19根/45完整闭包 PASS。真正候选19 SQL按拓扑固定SHA，包含14新结构root+旧P1约束清理+reason容量+V4/future policy/remediation三个新增配置，未包含paused activation SQL执行。旧base/catalog 精确历史hash保留，不采用当前不同语义文件覆盖账本。
- 源库只读备份：全表结构 gzip172127 bytes；210相关表数据 gzip26304379 bytes，展开310582007 bytes；16个实际受影响原表另有独立schema+data gzip2852433 bytes，展开47276368 bytes。三项gzip完整性/退出码/SHA256通过。原库回滚只能使用16表载荷，不能恢复全表schema覆盖其它表。保护载荷位于仓库之外 C:/IntRuoyiBackups/20261003-dcc-integration，不提交业务数据或凭据，不宣称对象附件灾备。尚未恢复演练。
- 数据库写入授权 question 已按具体方案发送：先同容器任务隔离真实副本演练，通过后才升级本机测试库；未答不能依赖时间执行。备份与候选清单可Review，SQL不直接代办E2E业务。

## 仍须推进

G15 Review 提供真实UI脚本但未运行完整闭环；项目负责人配置精确Long、审核配置真实doc_control角色、项目目录选择后检出入口可达性仍须有效行为核验并在缺陷成立时修复。任务专属Quartz不能启动共享任务；单独生效job每分钟只在安全范围/正式控件配置后验收。原库升级授权、真实页面、任务清理、本地提交/int_qms合入及合入后检查都未完成。

进一步源码核对：正式后端ReviewerConfig使用PermissionApi.hasAnyRoles，它仅取真实role code，不给super_admin默认doc_control；前端checkRole包含super_admin bypass。UI门禁差异已确认，必须补实际SFC RED→GREEN，不改变后台权限或共用permission helper。项目属性界面仍leaderId:number/直接API载荷，需验证大字符串及unsafe number，不能单凭类型注解声称已经发生精度损失。此两项新增Review尚未修复，不把68PASS当覆盖新发现。

备份完成后带标签精确COUNT复核仍41615/36685/339/269；当前Java运行进程为空，MySQL只有event_scheduler和本次只读query连接、活动事务0。event daemon本身不证明event为空，升级前须另读当前events/确认无writer，不把此快照当永久写入屏障。

## G19 收口

- 项目属性配置完成精确 Long 字符串、项目/账号租户与启用状态、属性快照、关闭/切换/卸载/ABA 及失败回执守卫；定向配置回归 22/22，属性既有 unit/race、SFC、局部 lint 通过。
- 审核人配置使用真实 `doc_control` 角色、项目更新权限、租户/操作者上下文，不使用前端共享 `super_admin` 角色提升；reviewer 回归、SFC、局部 lint 通过。后端配置权限未放宽。
- 启动任务同步增加 `yudao.local-job-control.startup-sync-enabled` 条件，false 时 Bean 不注册且不访问 JobService/SchedulerManager，缺省和 true 保持原行为；7 项 Maven 测试和 infra compile 通过。
- DCC 用户界面“有效版本”统一改为“受控版本”相关表述；browser/detail/handling summary/basic info、后端日志标签和术语合同测试均已更新。工作小版本仍保留为独立状态概念。
- Root 返工入口精确定位 helper 携带原文件、Master、存储目录和文件编号，浏览器只接受真实授权列表中同 Master 的所选版本，安全 ID/目录/编号失败即停止；相关定向导航测试通过。

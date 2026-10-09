# 主干提交推送与运行态验证报告

## Result
ready_for_closeout：主干代码已推送，前后端在线且运行负载与本次构建一致；等待 cleanup 和最终记录推送。

## Git Verification
- 冻结基线：6a012519b，39 个待提交文件；起始主干领先 origin/int_main 12 个提交。
- 基线提交：29df5ede23e231e397f6fd2daccbcbc9d33cb177，39 files changed，3067 insertions，207 deletions。
- 提交前冻结路径及 SHA256、暂存路径与内容归一核对、git diff --check、git diff --cached --check、端口守卫均 PASS。
- git push origin int_main -> PASS；推送后 HEAD 与 origin/int_main 一致，ahead=0 / behind=0。

## Targeted Verification
- 前端：node --test tests/e2e/active-order-copy-detail-long-id-static.spec.cjs tests/e2e/active-order-detail-erp-nonblocking-behavior.spec.cjs tests/e2e/team-leader-active-order-production-material-list-tab-static.spec.cjs tests/e2e/batch-record-version-review-dialog-behavior.spec.cjs tests/e2e/frontline-production-qualified-output-loss-behavior.spec.cjs -> 64/64 PASS。
- 后端：完整 yudao-server reactor package，8 个指定测试类共 96/96 PASS，31 模块 BUILD SUCCESS；准确命令记录在 execution-log.md。本轮源码编译目标 Java 17，实际测试 JDK 21.0.10。
- MySQL 查询回归：MES 6/6、Showroom 4/4 PASS，仅 SELECT。
- 正式启动前置：32/32 schema/seed 探针、展厅存储配置、对象存在性、MySQL/Redis Docker 路由检查 PASS；未执行数据库迁移。
- 新任务文档 UTF-8、标题/状态结构及既有新增 Markdown 标题检查 PASS；正式启动脚本 PowerShell parser PASS。
- 未执行真实 E2E、全量业务回归或前端生产构建，不把上述结果扩展为这些验收结论。

## Runtime Verification
- 构建期间并行任务已启动同一主干服务，本任务启动预检发现端口占用后核对归属与运行负载，未停止共享进程。
- 前端：8081，PID 20488，当前 E:/IntRuoyi/IntRuoyiFronted Vite env.local，HTTP 200 且入口有效。
- 后端：48081，PID 31316，当前主干稳定运行目录 Jar，HTTP 200 / UP，实际运行 JDK 17.0.20。
- 本次构建 Jar SHA256：1C8F95FBAED9DECD89F9DC99F1CB057B633135005371F75BC1FFD7C20EBABE72。
- 运行 Jar SHA256：C5C20F38BE9B9647288860B1C7DAE0A1D58BF500F6BA2668909BCCF7A494E817。
- 两份 Jar 全部 851 个归档条目名称与解压内容逐字节相同；归档整体 SHA256 不同不代表运行代码不同。
- 运行 Jar 修改时间 <= Java 进程启动时间，运行包没有被本次构建覆盖。

## Closeout
- project-experience-consolidation 核对现有经验，已覆盖本次 Git 冻结、端口归属、运行包核验和迁移授权边界，无新长期经验文档。
- cleanup preview/apply：待执行，仅 7 个本任务临时清单属于清理范围。
- 任务三份核心记录与共享运行态保留。

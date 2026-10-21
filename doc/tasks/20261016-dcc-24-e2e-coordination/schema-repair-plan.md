# 本机 QMS 缺失迁移实施范围

状态：实际库迁移及重复执行 PASS（2026-10-16 19:30:05），待新运行包启动后真实页面复验。

目标仅为本机 int-ruoyi-mysql / 127.0.0.1:23306 / ruoyi-vue-pro。使用原启动配置和数据卷，未切换数据源。当前运行库实际盘点见 schema-preflight.json；完整 20 项依赖及全库 release migration policy 均 PASS，见 schema-migration-policy.json。

执行白名单与文件 SHA 固定在 schema-repair-plan.json，依次为：

1. 20261015_dcc_category_approval_source_mode：类别新增来源模式，现有类别新列明确 PUBLIC_MATRIX，原列不变。
2. 20261015_dcc_application_custom_matrix：文件新增三项 nullable 申请事实，route_version_no 允许 NULL；不回填历史申请。
3. 20261014_dcc_delayed_effective_confirmation：新增六项 nullable 确认事实，三项已有时间精度提升至微秒，既有时间值不变。
4. 20261015_dcc_checkin_payload_hash：检出表新增 nullable 请求摘要，不回填历史。
5. 20261015_dcc_preview_reading_session：新增阅读会话表及正式唯一索引。

不重放完整依赖闭包中的初始化、种子、BPM 定义或历史迁移，不改旧账本 SHA/状态。执行前再次核实依赖所要求的实际结构、SQL SHA 和没有并发写入；未知漂移立即停止。

已只读备份六张受影响原表和迁移账本，共 45,211 行；压缩包 2,382,632 字节，解压验证 PASS。备份位置 C:/IntRuoyiBackups/20261016-dcc-e2e-unblock/20261016-184004/affected-tables.sql.gz；SHA 和各表行数见 JSON。原列定义、主键逐行摘要及原始建表语句单独保存在同目录 original-columns-and-row-hashes.json。日期按固定微秒格式对比，精度扩展不能掩盖时间值变化。

授权后先在新建任务专用库 dcc_e2e_20261016_schema_rehearsal 恢复上述副本，执行这五份原始 SQL 首次及重复验证；原列逐行摘要必须完全一致，新增类别列以外历史新列保持 NULL。然后冻结真实库写入窗口，重新备份/哈希，通过同一 schema 契约 RED→GREEN，最后真实页面复验。不得把副本验证算作业务 E2E PASS。

恢复边界：DDL 隐式提交，不能用事务回滚。发生差异先停止、保存证据并保持业务暂停；仅在没有新增业务写入的冻结窗口内，按原始建表语句和精确表备份恢复实际受影响表。账本不得全表覆盖，任务新建阅读会话表不得在已产生业务记录后直接删除。旧原列/行不应发生修改，恢复前必须复核窗口与表归属。

用户已明确答复“允许按方案迁移并继续验证”，记录在 execution-authorization.md。副本两轮执行 PASS；实际库在停止已确认的 QMS 后端、确认没有其它数据库会话后，重新备份至 C:/IntRuoyiBackups/20261016-dcc-e2e-unblock/20261016-192711/，再执行同一五份SQL两轮。schema-source-execution.json、schema-contract-source-green-1.json、schema-contract-source-green-2.json 证明结构转绿、原行原列摘要完全一致、旧账本未变。没有重放历史种子或写业务fixture。

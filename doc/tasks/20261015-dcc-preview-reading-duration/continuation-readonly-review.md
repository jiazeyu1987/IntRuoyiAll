# 本次继续：只读后端/迁移复核与证据边界

本次真实修改与新 RED/GREEN 仅为两个客户端生命周期缺陷。R01 完整 Duration、R02 方法安全保持上一轮关闭状态；125 后端 / 181 前端 / full types 属上一轮证据，本轮没有运行 Maven、共享 target、完整 types 或构建。

## 服务器重复/乱序请求的源码依据

- DccOnlineFilePreviewReadingSessionService 的 start/heartbeat/stop 六个公有写入口均为同一事务；startInternal/advance 都在写 session 前 authorize → lock(resource) → authorize。授权、token、租户或上下文异常直接传播，未走成功累计分支。
- DccControlledFilePreviewViewSessionMapper.lockControlledFile 按 tenant/file 锁正式资源；lockInfraFile 锁统一存储资源。selectOwnedByIdForUpdate 按 tenant/id 锁 session；selectActiveForUpdate 按 tenant/resource/file/user 找活跃记录。
- advance 在锁内重读 session：endedAt 非空时 stop 返回原记录，不再加时；heartbeat 则明确拒绝。heartbeat 先获得锁时只从当前 lastHeartbeatAt 计下一段，随后 stop 再从已前移的 lastHeartbeatAt 计尾部；stop 先提交时迟到 heartbeat 不再累计。这里证明源码的串行化与幂等意图，不冒充真实多连接实验。
- startInternal 同 event/user/tenant/client 命中 existing 时返回原记录，已结束记录不会复活。本次 READ-CONT-02 就是客户端遗漏 endedAt，误把这种合法“已结束重放”当作活跃会话；修复客户端，无需改后端幂等合同。
- summaryInternal 只读精确 tenant/resource/file/version/user 的服务器累计，客户端没有提交时长或跨版本汇总。viewer summarySequence 和 owner/generation 防止旧统计覆盖新版本；这次相关专属行为回归包含在 28 项内。
- 普通重复 heartbeat 没有独立请求序号；它按串行服务器接收时间的非重叠区间累计，不是按请求次数加固定秒数。本轮没有把“重复请求多次到达”误报为秒数重复相加，也不宣称能证明人的持续注意力。
- 同一事务内锁等待期间撤权是否立即被第二次普通 SELECT 观察，仍有集成证据边界：authorize 在锁前已读事件/权限；Query assertPreviewReadingAccess 使用普通 Mapper SELECT，项目配置未显式设置 local-cache-scope。实际 SqlSession 缓存/数据库隔离及外部授权端口行为未在本轮执行证明。此处仅交 Root 复核可见性，不登记为已复现产品缺陷，不改只读 Query，不编造新后端 RED/PASS。

## 迁移静态核对（本次实际执行静态断言，无数据库）

正式文件：IntRuoyiBackend/sql/mysql/20261015_dcc_preview_reading_session.sql。

8 项静态断言通过：单个 CREATE TABLE IF NOT EXISTS；没有 INSERT/UPDATE/DELETE/ALTER/REPLACE/TRUNCATE 历史 DML；active_marker 为数据库生成列；tenant/resource/file/user 单活跃唯一键；tenant/event/user/client 幂等唯一键；DO 不写生成列；Mapper 有资源/session FOR UPDATE；summary 按精确 tenant/user/version 统计。

声明 dependsOn 的 20260513_dcc_base_schema 与 20260528_dcc_controlled_file_protection 文件均存在，二者 metadata dependsOn 为空，因此该“声明的 DDL 依赖图”闭合；此前三项 policy gate 是历史证据，本次未重复运行。

这不是完整运行库部署前置证明：已有 DccControlledFileAccessEventDO 映射 processInstanceId，该列由既有 20261012_dcc_preview_issuing_round.sql 新增（该迁移自身依赖 protection）。它已是原有正文预览基线所需事实，不能因为本次新表 DDL 的三节点声明图闭合，就声称缺少它的库可运行当前整套 Java。Root 部署仍需整体 schema preflight；本轮不把原基线依赖重新包装为已修缺陷，也不擅改迁移或执行实库。

未验证：真实 MySQL 首次/重复执行、生成列/唯一键运行行为、事务回滚与多连接并发、完整运行 schema、真实 BPM/权限撤销时序、浏览器关闭网络送达。

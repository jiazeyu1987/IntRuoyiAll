> 2026-10-16 状态更新：本标准测试程序已通过两轮真实全量、增量、新目标恢复及页面验收，见 [验证成功记录](../../VERIFIED-20261016.md)。下文早期开发记录中的待验收状态以本次记录为准；适用范围仍为小程序。

# SmallApp 三组件版 r2

版本 `smallapp-20261014-r2-redis`。真实 MySQL 保存身份、会话和业务记录，MinIO 保存不可变附件，Redis 是每次登录及认证请求实际消费的必需会话缓存。Redis 失败即认证失败。旧 r1 的 22 文件封存包及历史报告保持封存；新包、runtime、配置、镜像及批准版本都必须独立。

## 运行与配置

需要任务私有 Linux CPython 3.11/3.12 和 requirements.txt 的十一项精确依赖，新增 redis==5.2.1。verify_runtime.py 导入真实 SDK、检查版本与接口，不建立服务连接。本机 Windows 检查不能证明 .59 Linux runtime 可用。W05 提供实际可重定位 runtime、架构、闭包及不可变 base/app image pins。Dockerfile 不联网安装、不选择默认镜像，前后端 UID/GID 10001。

config-contract.json 是字段描述符，不是可启动配置。真实 config/application-instance.yaml 为 UTF-8 strict JSON：SmallAppPublicConfiguration/v2、version、backend、frontend；backend 使用 SmallAppRuntimeConfig/v2，frontend 保持 SmallAppFrontendConfig/v1，version 必须 r2，origin 一致。没有旧 backend schema 兼容分支。materialize.py 消费已 pin 的真实公开文件及继承 FD，生成实际启动私有配置。

后端 FD 内容 {config,secrets}，首次安装 {config,secrets,bootstrap}，前端 {config}。SMALLAPP_CONFIG_FD 仅为已打开 FD >=3 编号。secrets 精确包含 mysql(username,password)、s3(accessKey,secretKey)、redis(username,password)、control(authKeyBase64,controlToken)，引用标签分别 smallapp.mysql/smallapp.s3/smallapp.redis/smallapp.control。私有文件 owner UID、0600、无 symlink，父目录0700；launch.py 打开固定路径。bootstrap 密码不留在运行配置，日志仅固定错误码及异常类。

MySQL 必须真实 ROW/FULL binlog、独立 brlab_20261012_smallapp_* 数据库；MinIO 独立 brlab-20261012-smallapp-* bucket，键 smallapp/122/；测试租户122/aoteman。**r2 六表结构与 r1 完全一致**。install.py 只用于首次空库，不能对已装 r1 的库再次安装、对 TARGET 安装或导入种子。W05 后续获授权升级窗口再装配新 runtime/config/image，不能干扰当前 r1 安装。

Redis 需要实际独立 SOURCE/TARGET standalone master 实例和各自 /data 卷，database 显式为0。instanceId 为 brlab-20261012-smallapp-<source|target>-<实际标识>，前缀 brlab-20261012:smallapp:122:<source|target>:。字段本身不证明服务隔离，W05/W08 仍需绑定实际 endpoint/container/image/volume/config，禁止共享或复用源服务。serverVersion 必须填实际 INFO 精确数字版本并与启动/health 一致。SET PXAT 需要 >=6.2.0（[官方 SET 文档](https://redis.io/docs/latest/commands/set/)）；目前 .59 Redis 版本与镜像未观察，不提供默认值。TLS 显式实际 CA 校验，RESP2/Python parser 固定，连接和命令零重试。

## 会话与恢复职责

SQL 保存 token SHA-256、tenant/user/CSRF 及 UTC 绝对 expires_at。Redis key 用该 hash，value 为对应身份/CSRF/绝对过期毫秒。登录实际 SQL 提交后 SET PXAT、GET/PTTL 读回，成功才给 cookie。Redis 失败关闭写入门并报错；已提交 SQL 会话没有返回成功 cookie，不自动重放。

每个认证请求在同一 Gate.lock 内读取有效 SQL 会话，再实际 GET/PTTL 校验缓存，使用已核对的 Redis identity/CSRF。缺 SQL、用户 inactive 或已过期，即使缓存仍存在也拒绝。仅成功 GET 返回 nil 且门 OPEN 才从有效 SQL 显式重建；连接失败、损坏、不匹配不能走此分支。QUIESCED/PROTECTED cache miss 报 WRITES_PAUSED，不 SET。PXAT 向下取整毫秒，不能延长 SQL 过期时间。注销在 Gate 内 SQL 删除、Redis DEL/读回再 SQL 提交，Redis 失败回滚 SQL并暂停。

源和目标使用不同 cookie 名，目标用独立浏览器上下文重新登录。恢复点后的源注销不属于更早 SQL 点，不能宣称该撤销被更早备份保存。保留/失效/重建期望、writer 清单与 W06 可观察范围见 recovery-semantics.json。

Redis 分类 REBUILD/DATABASE，不是 NOT_PRESENT 或原始缓存 RESTORE。redis-policy.json 是 recipe 配置来源，必须由原 source catalog/策略批准、pin 并实际读回。queue 无消费者，维持 NOT_PRESENT。

既有引擎 DERIVED_DATA 在 SQL 恢复后、应用启动前运行。业务函数 Store.rebuild_redis_from_database(existingNativeGuard) 供该目标执行器调用：TARGET/PROTECTED、独立 Redis database0 空，guard 为真实现有 bound/lease/target-lock/deadline 检查，调用前、每次写入、读回前后都执行。由恢复后有效 SQL 会话重建，过滤已自然过期项、保留原 PXAT，逐项真实 GET/PTTL 验证。没有 FLUSHDB、源缓存读取、种子、独立恢复入口或自动清空重试。

返回 SmallAppRedisRebuildObservation/v1 仅是业务读回观察，不是 RecoveryDerivedExecution/v1 证明。W04 需在原 derivedAdapters 注册 smallapp.redis-session-cache.mysql.v1，批准实际 scriptPin/trust SHA，消费现有 RecoveryDerivedRequest、DATABASE result pin、request/plan/recoveryTarget/targetIdentity pins 和 inherited lock，生成原规范结果及独立 verificationPins。本包不伪造批准或 proof wrapper；缺绑定阻塞其真实运行。

## 暂停、启动与同一引擎

启动实际 SHOW TABLES/DESCRIBE、核六表及唯一身份/租户范围，检查 bucket/对象意图及 Redis PING/INFO/TIME。TARGET 必须 PROTECTED，完整恢复链和原生保护核验后由负责人显式 OPEN。GET /health 检查真实三组件；/api/session 只有真实消费 Redis 成功才给 sessionCache 字段。

Gate 序列化记录、对象、登录/失败计数、注销及认证 cache miss。对象仍为 STAGED durable intent -> PUT -> 实际 GET/hash -> SQL ACTIVE/ref，退役 DELETE_PENDING -> delete/readback；失败保留意图并暂停。RECONCILE_OBJECTS 只暂停时处理无引用技术意图。Redis 自然 TTL 过期仅派生状态消失，无需捕获原始缓存。

control_client.py 仍用 SmallAppControlRequest/v1、私有 FD 和明确任务 socket，操作 STATUS/QUIESCE/STOP/OPEN/RECONCILE_OBJECTS。r2 回执 SmallAppControlResult/v2 追加真实 Redis health；QUIESCE/STOP 等待全部 writer 排空、核无待处理对象。原生 PID/container/tree 退出、ownership/fences、claim/grant/lease 仍由原执行器核验。

capture_binding.py 只生产原 reference/derived/object-source 配置并调用原 formal_capture_semantics 验证器，没有 dump/replay/copy/archive/restore/SSH。FULL、原生 I1、对象链、archive、SMB readback 和 restore 沿用既有同一入口及引擎版本。实际 consumed application-instance.yaml/.env/config/data.env/docker-compose.yml/data-compose.json、源镜像及 r2 runtime/source 随共同材料；Redis 镜像/配置/卷由现有环境和 registry 绑定。r1 grant 不能代替 r2 当前批准。

## 受影响验证及 W06

redis_checks.py 检查新增范围、原绝对过期、拒绝陈旧缓存、role cookie、真实 redis-py RESP2/零重试/TLS 接口与 rebuild 前置条件；不模拟数据库或 Redis 成功。critical_checks.py 只调整受影响 fixture，本次仅复跑受影响项，不改写历史 r1 二十项记录。语法、离线探针及归档读回都不是远端验收。

ui-contract.json 原登录/记录/单附件控件保持稳定。W06 实际页面写入；目标独立上下文登录后看记录/附件、下载真实内容和引用，最后可 GET /api/session、/health、/api/records。SQL/附件按 FULL+I1 保留，已过原 expiry/该点不存在/inactive 会话拒绝，目标缓存经原派生步骤重建，TTL 不延长。真实 Redis 请求、恢复 readback 和浏览器验收仍待运行，容器 UP 不等于三组件通过。

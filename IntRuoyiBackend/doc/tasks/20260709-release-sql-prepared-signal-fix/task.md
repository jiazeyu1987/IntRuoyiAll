# 20260709-release-sql-prepared-signal-fix

## 任务目标
修复测试服发布 required SQL 20260613_mes_smart_scheduling_t1_schema.sql 中 SIGNAL SQLSTATE 通过 PREPARE/EXECUTE 执行导致 MySQL 报 ERROR 1295 的问题，保持数据冲突 fail-fast 语义，并保证后续发布包只来自已提交 HEAD。

## 里程碑
- [x] 复现发布 SQL prepared SIGNAL 风险并记录 RED 测试。
- [x] 最小修复 SQL 冲突检查实现。
- [x] 运行 SQL 契约测试与发布迁移策略检查。
- [x] 提交后端修复并交回测试服发布流程重新构建。

## 预期验证
- python -X utf8 -m pytest script/tests/test_release_sql_no_prepared_signal.py -q 通过。
- python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql 通过。
- git status --short 仅包含本任务相关文件，提交后工作区干净。

## 当前状态
已完成：后端 SQL 修复与验证已完成，并已提交到临时发布 worktree；待合入 int_main 后重新构建测试服发布包。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，移除 prepared SIGNAL 协议不兼容路径，保留发布前数据冲突阻断。
- 是否存在临时补丁或绕过：否。

## 经验门禁
- 来源：D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md
  - # 构建发布耗时问题预防清单
  - 当任务涉及 IntRuoyi 运行控制台真实 E2E 构建发布包、发布到测试服、发布包候选验收或构建发布失败重试时，先读本文。
  - 本文只沉淀可复用经验；正式发布规则仍以 `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`、`server-access.md` 和 `login-access.md` 为准。
  - 如任务目标是让 Codex 直接照清单执行，请同时读取 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`。
  - ## 通用发布验证基线
  - 适用场景：任何功能修改、配置修改、脚本修改、菜单/权限调整、构建发布包和部署服务器。
  - 1. 唯一发布源头：所有发布链路只认一个维护仓发布入口和产物来源，本机源码通过不等于发布已通过。
  - 2. 配置必须代码化：发布相关的环境变量、路径、参数、菜单权限、脚本和构建开关优先写入仓库和发布脚本，不把服务器手工改动当最终方案。
  - 3. 验证发布产物：每次构建都要检查发布包、manifest、打包内容和关键文件是否与本次变更一致，不能只看编译成功。
  - 4. 环境差异显式检查：本机、构建机、测试服、备份服、正式服的镜像 tag、目录、挂载、数据库和账号基线都要明确核对，不能默认一致。
  - 5. 目标环境真实运行态：到服务器必须看真实运行中的镜像、进程、健康、页面和权限响应，不能只验健康检查或接口单点。
  - 6. 故障补成门禁：凡是本机正常、服务器失败、发布后暴露、或构建后才发现的问题，都要补成 preflight、测试或脚本门禁，不能只修一次就结束。
  - - 功能修改阶段：先确认修改会进入正确的发布源头和打包范围。
  - - 构建发布包阶段：确认产物、manifest、依赖和配置在包内完整且一致。
  - - 线上或测试服暴露的问题：必须回写到门禁或经验，不要停留在口头提醒。
  - ## 发布前必做预检
  -    - 如果刚改过维护控制台后端或发布脚本，必须重新构建并重启控制台，再用实际接口或页面确认候选列表、预览命令和门禁行为已生效。
  -    - 本次已证明 `nginx:1.27-alpine` 拉取超时或 EOF 会让前端镜像构建在后半程失败，必须在真实 E2E 提交前提前发现。
  - 3. 先跑迁移策略门禁。
  -      `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`
  -    - `release-migration dependsOn` 必须写 migrationId，即 SQL 文件名 stem，不带 `.sql` 后缀。
  -    - `code-only` 和 `-SkipDatabaseSync` 只表示不做数据同步，不表示跳过 schema、required SQL、迁移元数据或发布 manifest 门禁。
  -    - 构建预览必须包含 `-Mode build-release`、`-Component intruoyi`、`-SkipDatabaseSync`、`-SkipMinioSync`。
  -    - 测试服部署预览必须包含 `-Mode deploy-release`、`-Environment test`、`-ServerHost 172.30.30.58`、`-RemoteAppDir /opt/intruoyi/runtime`。
  -    - 预览命令不得包含 `172.30.30.57`、`promote-prod`、`backup-now`、`rollback`、`restore-data`。
  -    - 如果修改 `sql/mysql` 或 `script`，提交前跑：
  -    - `sql/mysql` 变更必须配 `script/tests` 下的测试，否则后端仓库 pre-commit 会阻止提交。
  - 7. 每次发布统一走临时发布 worktree。
  -    - “构建并发布到测试服务器”等所有发布命令，一律先创建本次专用的临时发布 `worktree`，禁止直接在主工作区执行构建、验证或发布。
  -    - 临时发布 `worktree` 必须检出本次发布目标提交；构建发布输入固定为该目标提交对应的 Git 已提交内容。
- 来源：D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md
  - # 构建发布备份恢复基线
  - - 构建发布包、发布到测试服/备份服/正式服。
  - - 涉及 worktree 发布隔离、合并或清理时先读取 `docs/worktree-memory.md`。
  - - 开始前必须确认目标环境、目标主机、目标库、对象存储 endpoint、发布包或备份点。
  - - 发布、备份、恢复、回滚不得用 mock、默认成功、静默跳过或自动降级掩盖失败。
  - - 缺少 manifest、schema 版本、备份链、恢复证据、环境凭据或授权时必须 fail fast。
  - - 发布代码入口必须先确认：发布、构建、部署脚本和运行控制台发布链路的修改统一在 `D:\ProjectPackage\Int\IntRuoyiMaintance`；`D:\ProjectPackage\Int\IntRuoyi` 只作为业务源码、SQL 和前后端发布输入。
  - - 真实写入、备份、恢复必须同一环境闭环，记录前端 URL、后端 action origin、目标主机和目标库证明。
  - - 备份或恢复前必须检查根分区、NAS、数据盘、临时目录、日志目录和挂载写入权限。
  - - 发布和恢复前必须证明前端、后端、数据库 schema、迁移脚本、required SQL 与 `releaseTag` 或兼容矩阵一致。
  - - 备份可用性必须以最终目录、manifest、checksum、对象清单和恢复演练证据共同判定。
  - - 新旧发布包、数据库 schema、对象 inventory 和 manifest 容易跨环境混跑。
  - - 候选发布包不能只按目录名或时间排序，必须读取 manifest 校验 releaseTag 和 sha256。
  - - 发布：远端 `.env`、运行镜像和 manifest `IMAGE_TAG` 一致，健康检查通过。
  - - 备份：最终备份根目录存在非空 manifest、数据库备份、对象 inventory 和 checksum。
  - - 恢复：schema 与代码兼容，文件记录与对象一致，后端健康，管理前端真实登录路径和本次核心业务路径可用。
  - - 自动化入口：`node scripts/preflight/publish-preflight.mjs --manifest <manifest.json> --environment <test|backup|prod> --sql-root <dir> --remote-host <host> --remote-app-dir <dir>`
  - 本文是 IntRuoyi 构建、发布、备份、恢复、回滚任务的强制前置文档。凡任务涉及以下任一事项，开始前必须先阅读并按本文检查：
  - - 构建发布包。
  - - 发布到测试服、备份服或正式服。
  - - 回滚代码、回滚数据、恢复到历史发布包。
  - - 默认只允许本机修改和验证；正式服务器默认禁止访问、发布、重启、写数据或读写正式数据，除非用户明确授权。
  - - 发布、备份、恢复任务不得用 mock、默认成功、静默跳过、自动降级掩盖失败。
  - - 缺少 manifest、schema 版本、备份链、恢复证据或环境凭据时必须 fail fast。
  - ## 构建发布备份恢复复盘加速清单
  - 后续构建、发布、备份、恢复任务开始后，先按以下顺序做快速确认，避免重复排查上次连续备份和发布验证暴露的问题。
  - - 本机进程来源：`48081` 后端和 `8081` 前端必须来自当前主仓库和目标分支，不能漂移到旧 worktree 或历史构建产物。
  - - 环境闭环：凡是“真实写入 + 备份 + 恢复”链路，写入目标、备份目标、恢复目标必须是同一环境；不得出现“本地写入、测试服备份”或“测试服写入、备份服恢复后再当成测试服结果验收”的跨环境混跑。开始前必须记录前端 URL、后端 action origin、目标主机和目标库证明，任一不一致直接 fail fast。
  - - 真实前端 action origin：运行控制台 E2E 等待提交响应时，`actionOrigin` 必须与浏览器实际发起请求的 origin 一致。若前端通过 `/admin-api` 代理提交，等待响应应匹配前端 origin；后端直连 origin 只用于明确的 API 直连验证，不能混用导致真实 operation 已提交但测试脚本误判超时。
  - - 正式服边界：未获用户明确授权时，运行控制台、状态脚本、发布脚本、备份脚本、恢复脚本和 E2E 都不得访问 `172.30.30.57` 的 SSH、HTTP、对象存储或数据库。
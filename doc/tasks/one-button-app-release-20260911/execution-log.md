# Execution Log

- Task ID: `one-button-app-release-20260911`
- Phase: P1 only
- App worktree: `D:\IntRuoyiWorktree\r260911-release-button\a`
- App baseline: `51d02916fcc8c5d188af4e0e72f777835b1fa20f`
- Verified app reference: `713c64112d7b626a2ba71788fd36cc32d686addc`

## BDD

BDD: 标准程序包固定无数据 -> Given 操作者创建标准发布 workflow / When 服务端解析业务意图 / Then `publishScope` 固定为 `app-release`，workflow 请求不接受 `with-data` 或任何基础设施参数。

BDD: 冻结批准源码 -> Given 维护仓与 IntRuoyi 根仓各有批准提交 / When 进入构建前或打包前门禁 / Then 两个 Git root 的 HEAD 和 dirty 均匹配冻结证据，maintenance/backend/frontend 三个 source role 均可追溯。

BDD: 测试失败阻断昂贵构建 -> Given 后端、前端或脚本测试任一失败 / When 执行 P1 构建合同 / Then Maven package、前端 build 和 Docker build 均不启动。

BDD: 三语言摘要一致且非法路径拒绝 -> Given 固定 artifact/manifest bytes、反斜杠路径或大小写冲突路径 / When Java、Python、PowerShell 计算双摘要 / Then合法向量四个摘要完全一致，非法路径以 `PACKAGE_PATH_INVALID` 阻断。

## Baseline And Verified-Line Review

- `713c641` 不是当前 `51d02916f` 的祖先。
- 三个验证线文件与当前主线发生真实差异：当前主线把用户名唯一约束恢复为全部历史行，验证线仅约束 `deleted=0` 的活跃用户。
- P1 将只移植 `active_canonical_username` 生成列、活跃行重复检查和对应测试断言，保留当前主线其它后续 schema/test 内容。

## TDD Evidence

- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-infra -am -Dtest=ReleaseWorkflowContractTest,ReleaseSourceFreezeTest,ReleaseWorkflowGateOrderTest,ReleaseDigestVectorTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，8 个合同测试均在运行期明确报告 P1 contract class 缺失；测试与全部依赖模块编译成功，不是编译前置失败。
- RED: `node IntRuoyiFronted/tests/e2e/runtime-control-one-button-static.spec.cjs` -> FAIL，客户端缺少仅含 `reason/sourceSelectionId` 的 workflow intent 合同。
- RED: `python -X utf8 -m pytest -q IntRuoyiBackend/script/tests/test_system_signature_password_t1_contract.py` -> FAIL，其中 2 个 P1 验证线断言证明当前主线缺 `active_canonical_username`；另 1 个 DCC T4 失败属于当前主线既有相邻问题，不计作 P1 RED。
- GREEN: Maven P1 合同命令 -> PASS，8 tests，0 failures/errors/skips。
- GREEN: 前端静态合同 -> PASS；客户端 workflow intent 仅含 reason/sourceSelectionId。
- GREEN: active username 验证线聚焦合同 -> PASS，2 passed、9 deselected；迁移 SQL 与 `713c641` 目标实现逐文件等价，当前主线其它测试/schema 内容保留。
- GREEN: `powershell.exe -File scripts/preflight/branch-runtime-port-guard.ps1` -> PASS，slot 41，frontend 8256，backend 48256；未启动端口。
- REGRESSION: C07 全量 infra reactor -> FAIL，首个非 P1 失败为既有 `CodegenEngineVue2Test` / `CodegenEngineVue3Test` 模板快照差异，共 12 failures；P1 定向类全部通过，未扩大修复。
- BLOCKER: `corepack pnpm --dir IntRuoyiFronted ts:check` -> app worktree 缺 `node_modules/cross-env`；C05 属 P4 门禁，不作为 P1 静态合同假绿，未临时安装依赖兜底。
- COMMIT: app P1 implementation -> `e9fed5ea86dfd8b8b63c779c37d523bc5d92df55`，仅包含 P1 Java/TypeScript/SQL/测试文件。

## Authorization Boundary

- 允许：本机 P1 代码、测试、任务分支提交。
- 禁止：P2-P5、真实 E2E、服务启动、NAS、数据库、MinIO、测试服、正式服和审查服访问或写入。

# 执行日志

## 用户意图

- 用户在批记录表单列表选择已发布表单后，右侧详情出现 `Request failed with status code 500`，要求排查并修复。

## BDD

- `BDD: 已发布批记录表单详情可正常加载 -> Given 用户已进入批记录表单列表且列表中存在已发布表单；When 用户选择该表单查看右侧详情；Then 详情请求成功并展示表单信息，不出现 HTTP 500。`

## 执行记录

- 已读取 `bug-regression-fix-loop`、后端、前端、E2E、登录和本地运行规则。
- `GREEN: experience-preflight -> PASS`：命中详情辅助请求错误归属、真实页面验证和后端 fail-fast 门禁。
- `RED: Playwright 访问 http://127.0.0.1:8081/mes/pro/batch-record-form-list -> FAIL`：真实登录后自动选择首行，`GET /admin-api/mes/pro/batch-record-report/cell-rules?reportId=a5c282e25c7b4e7baaa08570f65e5607` 返回 HTTP 500，右侧详情显示 `Request failed with status code 500`。
- `RED: runtime Jar integrity probe -> FAIL`：监听 PID `46388` 于 `2026-07-27 20:33:12` 启动，直接引用 `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`；该 Jar 于 `2026-07-27 20:54:09` 被重新打包覆盖，`JarModifiedAfterStart=True`。
- 后端异常栈同时出现 Jimu MiniDAO 模板资源缺失，以及 `ExceptionUtil`、`ChainedPersistenceExceptionTranslator`、`RequestUtil` 等跨 Hutool、Spring、Tomcat 依赖类无法加载，证明运行中可执行 Jar 被覆盖后发生延迟类加载损坏，而不是单一业务数据或单一类缺失。

## 根因判断

- 当前后端不是从稳定运行目录的不可变 Jar 副本启动，而是直接运行 Maven `target` 产物。
- 进程启动后并行 Maven 构建替换了同一路径 Jar；JVM 后续加载尚未读取的嵌套依赖和 Jimu SQL 模板时读取到不一致的归档内容，导致详情接口 500。
- 正式修复是停止已确认归属 `int_main` 的旧进程，并通过项目启动脚本构建、复制到 `output\runtime\int_main` 的独立 Jar 后启动；不修改业务数据，不添加 fallback。

## GREEN 与回归

- `GREEN: restart-int-ruoyi-local.ps1 -Component backend -> PASS`：旧 PID `46388` 已停止，新 PID `4000` 于 `2026-07-27 21:44:45` 启动，运行 Jar 为 `output\runtime\int_main\backend-runtime-control-20260727-214426.jar`。
- `GREEN: runtime Jar integrity probe -> PASS`：运行 Jar 修改时间为 `2026-07-27 21:44:23`，早于进程启动时间，`RuntimeJarUnchangedAfterStart=True`。
- `GREEN: Playwright 首次复验 -> PASS`：`signature-cell-markers` 与 `cell-rules` 均返回 HTTP 200；右侧选中标题为“产品信息”，无可见错误，预览 frame 数量为 1。
- `GREEN: mvn -pl yudao-server -am "-DskipTests" package -> PASS`：30 个 reactor 模块全部 `SUCCESS`，`BUILD SUCCESS`。
- `GREEN: target 重建后运行态隔离 -> PASS`：`target\yudao-server-exec.jar` 于 `2026-07-27 21:51:08` 再次生成，晚于运行进程启动时间；运行 PID、稳定运行 Jar 路径和运行 Jar 修改时间保持不变，health 仍为 `UP`。
- `GREEN: Playwright 构建后复验 -> PASS`：同一 `cell-rules` 和 `signature-cell-markers` 请求继续返回 HTTP 200，页面无失败响应、无右侧错误提示，预览正常显示。
- `GREEN: project-experience-consolidation -> PASS`：已将运行 Jar 不可变门禁合并到现有 `docs\local-runtime.md`，并在 `docs\experience-index.md` 增加关键词路由。
- `GREEN: bug-regression evidence validation -> PASS`：`validate_bug_regression.py` 返回 `Bug regression evidence is valid.`。
- `GREEN: task-closeout-cleanup preview/apply -> PASS`：保留四个任务核心证据文件，无删除项、阻塞项或警告。
- `BLOCKER: Git closeout -> concurrent dirty shared branch`：共享分支存在非本任务改动，未执行 commit/push，任务保持 `ready_for_closeout`。

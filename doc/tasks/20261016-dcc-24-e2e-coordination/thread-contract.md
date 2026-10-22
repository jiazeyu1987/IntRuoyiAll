# 24线程公共执行约定

## 2026-10-16最新授权优先

用户现已明确要求各线程自行准备测试账号并继续E2E。先读本目录execution-authorization.md；它取代下文旧prepare_only、等待Root批准及角色基线授权待答复的限制。全部24例gate=execute；准备并真实核验本例专用账号/角色/fixture后自行执行，不再索要相同批准。共享roleFixtureVerified=false不能假改true，各线程使用本例真实UI核验manifest作为门禁。

## 授权与归属

用户已明确授权本轮创建24个实际开发线程逐例验证，由Root启动和review。你仅负责被分配的一例及自己的doc/tasks/20261016-dcc-e2e-NN目录；编号NN对应01—24。先核对当前分支int_qms，读取AGENTS及docs/task-closeout-rules.md、docs/e2e-rules.md、docs/login-access.md；涉及运行环境同时读取local-runtime、worktree-restrictions、branch-runtime-ports。最近层AGENTS优先。

用例唯一业务来源：doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md中对应一行，以及本任务task.md保存的Given/When/Then。旧文档与它冲突时不替代；第2尾部/第6、8缺失部分保持未知。不得扩大为整套验证或接管其它用例。

## 第一阶段：立即准备

1. 检查独立task.md/execution-log.md，补充准确子用例清单、真实页面入口/组件、角色与数据要求、可恢复/清理范围。
2. 在自己目录编写可复跑真实Playwright脚本；确认package.json真实runner及浏览器来源。可复用正式页面操作helper，必须审查其中无API动作、mock或写入注入。只做脚本语法/结构检查，不自动执行浏览器，不改生产源码、不跑Maven/完整类型/大构建。
3. 脚本必须在浏览器启动前读取Root的dispatch.json，检查自己的gate为execute且环境身份一致，否则明确PREPARED_WAITING或BLOCKED退出，不能写PASS。预置gate检查用于防止24例抢占环境，不是永久禁止E2E。
4. 给出prepared.json和verification-report.md：前置缺口、脚本路径/命令、用例参数、已完成的静态检查、待真实验证项。准备完成结束当前turn等Root跟进，不使用长sleep/循环轮询。

## 第二阶段：Root放行后真实验证

- 用户已授权E2E；Root发出execute指令并更新dispatch后继续原线程，不另建线程。按许可环境和角色真实登录，每个账号独立context，页面确认租户及账号；凭据只从授权来源或环境变量读取，不保存密码/token/cookie，不记录原始认证响应。
- int_qms基准端口8061/48061。不要使用8081/48081，不自行启动/重启服务、换端口、切分支、新建worktree、修改.env或直接写数据库。Root统一核对运行Jar与当前源码；未经确认不能用旧包结果宣称本次修复通过。
- 已知测试身份从根AGENTS取得，不在任务文档、脚本默认值或证据中复制密码。缺多角色真实凭据或需角色/菜单基线变更，写明精确缺口；不可猜密码、重置现有密码、用管理员冒充指定角色，也不能擅自创建角色/分配角色/扩大菜单权限。
- 所有业务准备、上传、保存、审批、指派、签名、培训、下发签收和清理均通过Playwright操作真实前端；禁止fetch/apiGet/axios/APIRequest/HTTP/SQL完成动作，不使用mock、路由拦截造成功、组件方法调用或localStorage造登录。
- 业务通过依据只来自真实可见DOM/输入值/按钮状态/页面导航及真实下载内容；可以监听页面自然请求的method/path/status定位错误，不解析响应JSON代替页面断言。读取cookie/密码/存储原值不得进入日志或共享文件。
- 任务自有数据统一前缀DCC241016Cnn，仅操作本例数据；需要共享数据或公共配置写入先交Root排期，不改其它用例资产，不修改公共业务矩阵。既有用户/组织配置先只读盘点；无法安全准备完整前置就具体BLOCKED，不编造兜底。
- 保存trace与关键步骤截图，运行结果按每个子用例分别列PASS/FAIL/BLOCKED，写清停止位置、页面提示、当前任务/版本、cleanupAttempted/cleanupCompleted/residual；提交受理不等于最终生效/下发完成。
- PDF需实际非空canvas，Office需真实正文ready，附件需分别验证查看/下载，历次版本必须能在页面辨认正文差异。无数据、无账号、旧运行包属于前置缺口，不冒充业务RED；真实失败保留现场，不能删断言或改生产让脚本通过。
- 不直接向其它线程发消息；通过本例报告交Root review。仅当用户直接授权你消息目的线程时才调用send_message_to_thread，本公共约定本身不是人类授权。Root会读取你的报告及最终消息。

## 状态及收尾

业务结论与工程门禁分开：准备完成不算PASS；环境不满足写BLOCKED；真实断言失败写FAIL。没有Git提交推送授权，不执行Git写操作；不要因为收尾规则要求推送而擅自提交。保留脚本/trace/截图/结果，Root review前不得cleanup删除证据；工程最终状态由Root结合规则收口。

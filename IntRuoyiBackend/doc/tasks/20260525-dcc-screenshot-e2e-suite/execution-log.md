# DCC 截图需求 E2E 套件执行日志

BDD: E2E 套件覆盖所有截图功能 -> Given DCC 截图需求 AC-01 到 AC-16 已有首版实现 / When 子 agent 为每个功能增加真实 E2E 测试并由主 agent review / Then 每个功能必须有真实前端路径验证、失败时修复并复测，直到全量 E2E 通过。

PRECHECK: `curl.exe --max-time 5 http://127.0.0.1:8089/` and `http://127.0.0.1:48089/actuator/health` -> PASS，frontend=200，backend=200。

PRECHECK: frontend Node dependencies -> `playwright` and `@playwright/test` are not installed in frontend `node_modules`，因此本任务首选 Python Playwright + pytest，避免新增前端依赖和锁文件 churn。

BDD: T2 E2E-01 图纸源文件 PDF 伴随上传 -> Given 测试租户 122 申请人在真实前端上传页选择 DWG 图纸源文件 / When 未上传 PDF 并提交 Then 页面阻止提交且 DB 不创建 `CODEX_E2E_T2_NO_PDF_*` 受控文件；When 补齐 PDF 后提交 Then DB 创建受控文件并记录 `drawing_pdf_file_id` 与源文件名。

BDD: T2 E2E-02 产品编号非法阻止提交 -> Given 测试租户 122 申请人在真实前端上传页填写非法产品编号 / When 点击提交审批 / Then 页面停留上传页且 DB 不创建 `CODEX_E2E_T2_BAD_PRODUCT_*` 文件。

BDD: T2 E2E-03 修改中状态展示 -> Given 通过测试租户 122 数据构造 ACTIVE 版本和待审新版 / When 申请人打开真实详情页 / Then 详情页展示 `修改中`。

BDD: T2 E2E-04 下载提醒和留痕 -> Given 已发布受控文件 / When 申请人在真实详情页点击下载并确认 / Then 浏览器下载成功且 DB 写入 `dcc_controlled_file_access_log` 下载留痕；下载确认必须提示下载后文件为非受控文件。

BDD: T2 E2E-05 INT/RE 体系记录下载 -> Given 已发布 `INT/RE` 体系记录、非匹配已发布文件和未发布 `INT/RE` 文件 / When 普通用户通过真实受控浏览页下载 / Then `INT/RE` 已发布文件可下载并写留痕，非匹配或未发布文件不暴露下载入口；下载确认必须提示下载后文件为非受控文件。

RED: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_upload_download_e2e.py -q` -> FAIL，E2E 主流程已跑通上传、修改中、详情下载留痕、体系记录下载和拒绝路径，但 AC-04/AC-05 产品缺陷仍存在：详情下载与体系记录下载确认框文案为 `确认下载该受控文件？系统将记录本次下载留痕。`，未出现 `非受控` 提醒；证据文件 `output/e2e/dcc-screenshot/t2-upload-download-evidence.json`，本次 submittedUpload.id=2054545668044045929，systemRecordFileId=2054545668044045932。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_upload_download_e2e.py -q` -> PASS，1 passed in 46.49s；前端下载确认文案修复后，真实浏览器路径覆盖 E2E-01 图纸 PDF 校验、E2E-02 产品编号校验、E2E-03 修改中、E2E-04 下载非受控提醒与留痕、E2E-05 INT/RE 体系记录下载与拒绝路径。

BDD: T4 电子发放签收 -> Given 测试租户 122 存在 CODEX_E2E 前缀 DCC 已发布文件与电子发放接收人 / When 接收人通过真实前端 8089 打开 DCC 详情并输入登录密码确认签收 / Then 系统必须追加签收意见、签收时间与电子签名留痕；若产品只支持签收不支持“接收人加签”，必须暴露差距。

BDD: T4 纸质发放回收导出打印 -> Given 测试租户 122 存在 CODEX_E2E 前缀 DCC 已发布文件与纸质发放记录 / When 文控通过真实前端 8089 确认纸质发放、确认回收、导出回执并打印回执 / Then 回执必须包含文件编号、名称、版本、部门、发放方式、状态、接收人、确认人、确认时间、回收人、回收时间。

BDD: T4 流程打印导出与 Word 模板 -> Given DCC 流程详情需要打印/导出并校验 Word 模板占位符 / When 测试通过真实前端 8089 查找 DCC/BPM 流程打印导出和模板配置入口 / Then 入口存在时必须验证 DCC 字段和缺占位符失败；入口缺失时必须记录真实 blocker，不得伪造通过。

BDD: T4 弱密码策略 -> Given 测试租户 122 管理员进入系统用户管理和个人中心 / When 通过真实前端 8089 新增用户、重置密码、个人修改密码时输入弱密码 / Then 前端或后端必须拒绝弱密码并保持用户密码未被弱化。

BDD: T4 90 天密码策略 -> Given 测试租户 122 存在 CODEX_E2E 前缀且密码更新时间超过 90 天的测试用户 / When 该用户通过真实前端 8089 登录并尝试进入业务页 / Then 系统必须强制改密或明确拒绝继续业务，不得放行业务访问。

BDD: T4 外来文件评审 -> Given 测试租户 122 需要外来文件评审提交、审批、查看路径 / When 测试通过真实前端 8089 查找外来文件评审入口 / Then 入口存在时必须完成提交审批查看；入口缺失时必须记录真实 blocker，不得伪造通过。

RED: `python -m pytest script\tests\test_dcc_screenshot_admin_policy_e2e.py` -> FAIL，`ModuleNotFoundError: No module named 'script.e2e.dcc_screenshot_admin_policy_e2e'`，预期原因是 T4 独立 pytest 已创建但 E2E 实现模块尚未落盘。

REVIEW FAIL: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_admin_policy_e2e.py -q` -> NOT ACCEPTED，3 passed, 4 xfailed in 105.02s；worker 用 `pytest.xfail` 暴露接收人加签、流程打印/Word 模板、个人中心改密、外来文件评审缺口。reviewer 不接受 xfail 作为全量 E2E 通过，已改为硬失败门槛。

BDD: E2E-06 流程动作保持 BPM/DCC 状态一致 -> Given 测试租户 122 中 CODEX_E2E 前缀的受控文件真实提交到 DCC 审批流 / When 审批人在详情页真实执行回退、转办、加签动作 / Then DCC 文件状态必须与 BPM 当前任务节点一致，且动作产生的签名、任务或路线快照只影响对应流程实例。

BDD: E2E-07 申请人处理回退 -> Given DCC 流程被审批人回退 / When 申请人从真实前端查看自己的受控文件 / Then 页面必须显示 `有流程回退，需处理` 或等价提示，并提供基于原流程继续重提的入口；若当前 UI 缺少入口，记录真实 blocker。

BDD: E2E-08 第四节点培训记录强校验 -> Given needTraining=true 的受控文件已流转到第四节点文控批准 / When 审批人在详情页尝试完成第四节点但未上传培训记录 / Then 前端或后端必须阻止完成，且流程仍停留在第四节点。

BDD: E2E-09 第四节点盖章 PDF 强校验 -> Given 受控文件已流转到第四节点文控批准 / When 审批人未上传盖章 PDF 或上传非 PDF / Then 系统必须阻止完成；When 上传 PDF 后继续提交 / Then 流程可继续进入后续发布或培训状态。

BDD: E2E-10 申请人选择会签人员只影响本实例 -> Given 申请人在上传页选择会签人员 / When 文件提交并进入会签节点 / Then route snapshot 与 BPM 会签任务只使用本实例选择的会签人员，其他未选择会签人员的实例不受影响。

RED: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_workflow_actions_e2e.py -q` -> FAIL, expected reason: T3 归属 pytest 文件尚不存在，流程动作、申请人会签选择、第四节点培训/盖章 PDF 真实 E2E 未接入。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_workflow_actions_e2e.py -q` -> PASS, 1 passed in 91.94s；Python Playwright 真实前端 8089 覆盖 E2E-06 回退/转办/加签动作、E2E-08 needTraining=true 第四节点缺培训记录阻止完成、E2E-09 第四节点缺盖章 PDF/非 PDF 阻止且上传 PDF 后可继续、E2E-10 上传页选择会签人员后 route snapshot/BPM 会签任务仅影响本实例。

BLOCKER: E2E-07 申请人回退处理入口 -> REAL BLOCKER；真实回退后文件 `2054545668044045924` 详情显示 `有流程回退，需处理：CODEX_E2E return to previous node`，但申请人详情页未暴露 `重提/重新提交/继续提交/处理回退` 入口，截图证据 `output/e2e/dcc-screenshot/dcc-e2e-07-applicant-return-blocker-2054545668044045924.png`；未伪造通过。

RED: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_workflow_actions_e2e.py -q` -> FAIL，reviewer 将 E2E-07 改为硬门槛后，E2E-06/08/09/10 仍通过，但 E2E-07 未进入 `result["passed"]`。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_workflow_actions_e2e.py -q` -> PASS，1 passed in 171.09s；前端详情页复用现有 DCC 审批签名面板，从 BPM 任务列表自动识别当前用户回退待办并显示“处理回退”，申请人可直接在详情页签名继续原流程。

EXPLORER: 需求覆盖审阅 -> PASS，子 agent `Euclid` 确认当前真实 E2E 只覆盖页面入口、上传提交、第一节点审批；Maven 单测、SQL pytest、前端 `scripts/*.test.mjs` 静态源码断言和 `pnpm ts:check` 均不足以算 E2E。缺口包括上传图纸 PDF 失败路径、产品编号校验、修改中、下载留痕、回退/转交/加签、申请人重提、第四节点培训/盖章、电子发放加签、纸质导出打印、密码过期、外来文件评审和 Word 模板。

EXPLORER: 技术组织审阅 -> PASS，子 agent `Helmholtz` 确认前端仓库没有正式 `@playwright/test` 工程，也未声明 `playwright` / `@playwright/test` 依赖；历史真实浏览器脚本分散在任务目录并多使用 Playwright CLI。当前任务采用 Python Playwright + pytest 作为主 E2E 载体，减少前端依赖和锁文件变更。

GREEN: `python -m pytest script\tests\test_dcc_screenshot_e2e_suite.py -q` -> PASS，1 test passed。该测试完成 E2E 基础前置检查：8089/48089 服务可达，测试租户 122 中 `体系文件`、`技术文件-DHF`、`技术文件-DMR`、DCC E2E 目录、三条四节点路线、目录访问、分类权限和电子签名授权 fixture 可准备。

SUBAGENT: 已启动 `Galileo` 负责 E2E-01 到 E2E-05；`Hilbert` 负责 E2E-06 到 E2E-10；`Wegener` 负责 E2E-11 到 E2E-16。主 agent 将 review 其测试真实性、覆盖矩阵和运行结果。

REVIEWER PRE-AUDIT: 高风险入口检查 -> 当前前端上传页 `src/views/dcc/controlled-file/upload/submitter.ts` 固定 `processType: 'CONTROLLED_FILE'`，未发现外来文件评审独立入口；`src/views/dcc` 未发现 Word 模板上传/占位符配置入口；后端密码过期会在登录阶段返回 `AUTH_LOGIN_PASSWORD_EXPIRED`，前端未发现专用“过期后改密再继续登录”页面。后续 E2E 若覆盖这些点失败，优先按产品缺口处理，不允许把静态源码断言或 blocker 文案冒充通过。

RED: `python -m pytest script\tests\test_dcc_screenshot_navigation_e2e.py -q` -> FAIL，预期失败原因：公共登录 helper 使用 `wait_for_url("**/dcc/...")`，会误匹配登录页 `redirect=/dcc/...` 查询参数，导致未登录状态被误判为已进入 DCC 页面。

GREEN: `python -m pytest script\tests\test_dcc_screenshot_navigation_e2e.py -q` -> PASS，1 test passed。修复 helper 后真实登录测试租户，并确认 DCC 受控上传、浏览、我的文件、审批任务、下发、培训、电子签名管理页面均可达，无 Access Denied、无接口错误、无 console error。

RED: `python -m pytest script\tests\test_dcc_screenshot_admin_policy_e2e.py -q` -> FAIL，T4 独立 pytest 已接入真实前端后暴露实现问题：分发 fixture 派生 ID 超出 MySQL BIGINT、弱密码与过期密码响应字段为 `message` 而非 `msg`、纸质确认框按钮为“确定”、个人中心改密入口未能加载。

GREEN: `python -m pytest script\tests\test_dcc_screenshot_admin_policy_e2e.py -q` -> PASS，3 passed, 4 xfailed。已通过真实 8089 前端验证 E2E-11 电子发放接收人确认签收并追加签收意见/电子签名、E2E-12 纸质发放确认/回收/CSV 导出/打印回执字段完整、E2E-15 超 90 天密码用户登录被明确拒绝且未进入业务页。

BLOCKER: E2E-11 接收人加签 -> XFAIL，真实 DCC 详情页只暴露“确认签收/签收意见”，未暴露“接收人加签”入口，不能伪造通过。

BLOCKER: E2E-13 流程导出/打印与 Word 模板缺占位符 -> XFAIL，真实 DCC 详情页包含 DCC 字段但未暴露可操作的流程导出/流程打印/Word 模板缺占位符校验入口；BPM 侧仅发现打印模板线索，无法完成 Word 缺占位符失败验证。

BLOCKER: E2E-14 个人中心修改弱密码 -> XFAIL，系统用户管理中的弱密码新增/重置已走真实前端并被后端拒绝；个人中心 `/user/profile` 在 8089 当前会被前端动态依赖加载问题阻断，只显示系统壳，未暴露改密页签，无法用真实前端完成个人修改弱密码验证。

BLOCKER: E2E-16 外来文件评审 -> XFAIL，真实 8089 前端未发现 DCC 外来文件评审提交/审批/查看入口；上传页仍是受控文件流程，不能伪造外来评审通过。

BDD: T4 硬门槛修复 -> Given reviewer 不接受 xfail 作为放行证据 / When 前端与后端增量复用现有 DCC 上传、详情、分发回执、BPM 打印模板和 Profile 路由能力 / Then E2E-11 到 E2E-16 必须全部通过真实前端路径，且不得用 mock 或跳过替代。

RED: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_admin_policy_e2e.py -q` -> FAIL，硬门槛暴露 4 个真实缺口：接收人加签入口缺失、DCC 流程打印/导出与 BPM Word 模板入口缺失、`/user/profile` 只显示系统壳、外来文件评审入口缺失。

GREEN: `node scripts/dcc-screenshot-t4-frontend.test.mjs` in `yudao-ui-admin-vue3` -> PASS，7 tests passed；静态契约覆盖接收人加签接口、外来文件评审复用上传流程、DCC 详情流程打印/Word 导出、BPM Word 打印模板静态路由、Profile 隐藏路由合并。

GREEN: `mvn -pl yudao-module-dcc -am -DskipTests compile` -> PASS，DCC 后端接收人加签请求 VO、Controller、Service 编译通过。

RED: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_admin_policy_e2e.py -q` -> FAIL，运行态发现 8089 前端 Vite cache 出现 `504 Outdated Optimize Dep`，重启时若未显式继承 `VITE_BASE_URL/VITE_PROXY_TARGET=http://127.0.0.1:48089` 会误连 `.env.local` 默认 48081，导致原已通过的纸质回收与密码策略路径被错误后端破坏。

GREEN: restart frontend 8089 with `VITE_BASE_URL=http://127.0.0.1:48089` and `VITE_PROXY_TARGET=http://127.0.0.1:48089` -> PASS；`/user/profile` 显示 `密码设置`，`/bpm/manager/model` 显示 `流程模型 / Word 打印模板`，DCC 详情显示 `确认签收 / 接收人加签 / 流程打印 / 流程导出 Word`。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_admin_policy_e2e.py -q` -> PASS，7 passed in 83.78s；E2E-11 电子签收与接收人加签入口、E2E-12 纸质确认回收导出打印、E2E-13 流程打印导出与 BPM Word 模板入口、E2E-14 弱密码新增/重置/个人修改拒绝、E2E-15 过期密码登录拒绝、E2E-16 外来文件评审入口全部通过真实前端路径。

RED: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_e2e_suite.py script/tests/test_dcc_screenshot_navigation_e2e.py script/tests/test_dcc_screenshot_upload_download_e2e.py script/tests/test_dcc_screenshot_workflow_actions_e2e.py script/tests/test_dcc_screenshot_admin_policy_e2e.py -q` -> FAIL，导航 smoke 使用旧 marker `DCC下发` / `DCC培训`，而当前真实页面标题为 `文件分发规则` / `文件培训规则`；页面已真实加载，断言需跟随当前系统文案。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_navigation_e2e.py -q` -> PASS，1 passed in 27.10s；导航 smoke marker 已对齐真实页面标题。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_e2e_suite.py script/tests/test_dcc_screenshot_navigation_e2e.py script/tests/test_dcc_screenshot_upload_download_e2e.py script/tests/test_dcc_screenshot_workflow_actions_e2e.py script/tests/test_dcc_screenshot_admin_policy_e2e.py -q` -> PASS，11 passed in 240.13s。

GREEN: frontend regression -> `node scripts/dcc-screenshot-t2-frontend.test.mjs` PASS 4 tests；`node scripts/dcc-screenshot-t4-frontend.test.mjs` PASS 7 tests；`pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=12288` PASS。

GREEN: `mvn -pl yudao-module-dcc -am -Dtest=DccDistributionReceiptServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，5 tests passed；覆盖接收人加签成功追加接收人、重复加签拒绝，满足后端 TDD 提交门禁。

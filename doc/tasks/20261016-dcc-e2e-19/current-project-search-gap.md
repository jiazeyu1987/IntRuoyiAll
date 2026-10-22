# E2E-19 当前项目名称/编号搜索入口缺口

状态：RESOLVED_REAL_UI_PASS。Root唯一写者授权后完成有效行为RED→最小前端GREEN→原23项真实UI全PASS，最新结果见`runs/2026-10-18T12-05-58-909Z/result.json`及project-search-handoff.json。以下缺口、源码限制和待分配描述均为修复前历史。


状态：BLOCKED，待Root按生产冻结规则指定唯一文件写者。21项真实业务PASS，0 FAIL，剩余2项不可用“项目文件夹”或“全局”搜索替代。

## 真实触发与证据

- 原始要求：`doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md:13,50`明确区分当前项目搜索和授权范围搜索。
- 数据已完成正式流程并由两账号真实回读：P1R2项目306的目录一/二分别含4229/4237两份ACTIVE A/1；P2R2项目307目录一含4238 ACTIVE A/1。完整ID见fixture-manifest.json。
- 新context由DCC241016C19Both本人登录，选P1R2但不选具体文件夹。范围实际只有“全局文件”“项目文件夹”。名称DCC241016C19-NAME与编号DCC241016C19-NUM两次搜索均显示“请先选择项目逻辑文件夹”，对应自然browser-page响应数均0。没有调用接口或注入查询参数替代缺失入口。
- 实际运行：`runs/current-project-entry-2026-10-18T10-54-12-434Z/result.json`、两张截图及trace.zip；原参数21 PASS结果：`runs/2026-10-18T10-42-03-692Z/result.json`，DCC HTTP错误、page/console error均0。

## 直接源码依据与最小修复边界

1. `ProjectBrowserPanel.vue`的main只提供global/directory两个页签。
2. `project-browser.ts:97,115,129`选项目后切directory；该范围必须同时有project和folder，查询只在directory附带两者。
3. `applicationRead.ts:887`的loadDccProjectBrowserPage明确拒绝仅projectId、没有folderId的正式项目浏览查询，并且只有GLOBAL/PROJECT_FOLDER两种编码；同函数还应保留响应所属项目/文件夹一致性校验。
4. 后端现有`DccControlledFileQueryServiceImpl.getProjectBrowserPage:440`接受GLOBAL且可带正数dccProjectCodeId；`DccProjectBrowserMapper:22`已有项目等值过滤，folderId另行可选，权限仍在分页前验证。此为静态依据，没有自行调用API试探。

建议Root优先评估仅前端正式项目浏览链：明确增加“当前项目”范围，必须已选项目、无需选文件夹；通过现有正式GLOBAL+精确dccProjectCodeId过滤查询项目全部文件夹，响应逐行校验同一项目；保持原全局/文件夹范围及选择器链路原语义。预期生产文件仅ProjectBrowserPanel.vue、project-browser.ts、applicationRead.ts；不新增菜单/角色/错误码，不改OWNER/VIEW、SQL、数据库或其它生产文件。若Root确认后端现有合同不能承载，则另行明确范围，不能猜测或兜底。

## BDD / 待分配RED

Given P1两个不同文件夹各有一份ACTIVE文件，P2有一份，两个普通账号分别只读P1/P2或只读P1。

When 通过正式页面选择P1和“当前项目”，不选择单一文件夹，分别按名称及编号搜索。

Then 同时返回P1两个文件夹的两行、正确来源和固定版本正文，P2不混入；原全局搜索仍按授权范围、文件夹搜索仍只查指定文件夹。

有效RED应执行现有API适配器/ProjectBrowserState行为：仅项目查询须可用、应保留项目ID并允许同项目不同文件夹、异项目响应仍拒绝；禁止只断言源码字符串。Root分配后才补生产测试/修改，Root仍独占Maven/运行服务。

原e2e-19.cjs中的两个scope-gap分支须在真实入口可用后执行对应动作/打开两个正文，并据所有分支状态计算整体PASS及exit0；其余21项已有真实证据保留，相关改动后做必要回归。当前没有修改上述生产文件。

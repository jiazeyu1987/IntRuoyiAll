# Execution Log

- 用户要求：让“报工历史”默认展示所有历史；“报工管理”显示所有报工，不是只看当天。
- 技能：已使用 `bug-regression-fix-loop`，并读取其 bug evidence contract。
- 触发规则：已读取任务收尾、前端开发、E2E 和 PowerShell 编码规则。
- 经验门禁：已读取 `docs/experience-index.md`，命中“无筛选即不限制结果范围，请求省略 submitDate”的统一列表复合工具栏门禁。
- BDD: 生产组长报工管理默认全量 -> Given 生产组长进入报工管理且没有提交日期筛选；When 页面加载或重置筛选；Then 请求不携带 submitDate，仅带 leaderType=PRODUCTION 与 allocationView=WORKBENCH，列表显示当前权限内所有待处理报工。
- BDD: 生产组长报工历史默认全量 -> Given 生产组长进入报工历史且没有提交日期筛选；When 页面加载或切换页签；Then 请求不携带 submitDate，仅带 leaderType=PRODUCTION 与 allocationView=HISTORY，列表显示当前权限内全部报工历史。
- BDD: 用户显式提交日期筛选仍生效 -> Given 用户主动新增提交日期条件；When 点击查询；Then 请求携带用户选择的 submitDate，并按日期过滤。

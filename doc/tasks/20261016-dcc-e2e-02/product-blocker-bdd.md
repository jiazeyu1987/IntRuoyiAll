# C02-B04 — 缺部门负责人时提示不具体

- Given 本例正常基线已PASS，改选本例无负责人的DCC241016C02NoLeaderDept，其余输入有效。
- When U通过真实上传页创建受控文件；预检使创建按钮禁用。
- Then 应明确指出会签部门负责人缺失并阻止提交。
- RED：页面仅显示“Route preview failed because a stage has no resolved approver”，没有“部门负责人”具体提示；submit请求0、源件清理成功。
- GREEN：Root生产修复后在PID16776以原fixture复验PASS，显示真实会签部门DCC241016C02NoLeaderDept（ID：910355）及“未配置部门负责人”，按钮禁用、submit请求0、源件清理成功；证据见root-workflow-resume-result.json.originalFailureRecheck。
- 证据：real-ui\run-1792171312942\missing-department-owner-1792171600913-result.png；real-ui\run-1792171312942\DCC241016C02U-8.trace.zip。
- 原C02-B03正常基线阻塞现已解除，编制人/主管等正式确认字段均可见。以下保留旧历史，不作为当前停工理由。

# C02-B03 — 审批路线预检显示系统异常

## Latest V6 Recheck

- Root恢复后的新身份：PID34096，Jar e537370a8e6e4cf425ccd292d5856feb791e99f6124d9e19cad55697ac42f7ce。
- 原项目292/类型125/类别908746/8账号全部复用；作者U及负例作者新增的本人签名资格已通过UI补齐，业务签名授权/图片共6个身份就绪。
- 23:42:18起的独立新context预检，批准人/会签部门/文控完整选择可见，4次自然route-preview HTTP200，页面仍系统异常。未上传源件、未进入二次确认或受控申请提交。
- 原定义错误是否已经解除或是否同因，单凭通用UI提示无法确定；本例明确为APPLICATION_CUSTOM，不把公共原矩阵默认带出当作已验。
- 当前优先证据：root-workflow-resume-result.json；截图 real-ui/run-1792164268060/workflow-resume-preview-1792165338888-result.png；trace real-ui/run-1792164268060/DCC241016C02U-3.trace.zip。人员选择另有同前缀selected-people截图。
- 编制人/主管正式确认字段未能观察，仍BLOCKED。以下保留维护前的原始BDD/RED及清理证据；原因未知，本线程没有扩大生产修复范围。

## Scope / Runtime

- 用例 E2E-02「提交前完整性拦截」的正常申请基线；非参数必填拦截的业务 RED。
- int_qms，前端 8061，后端 48061 / PID43284；运行 Jar SHA256：caafeb2e008f0980d5c499448628bace9f87a2a0c69344000135e2ba650427ff。
- 运行身份由 Root 的 `doc/tasks/20261016-dcc-24-e2e-coordination/unblock-final-runtime-health.json` 提供；没有启停服务或修改数据库/生产代码。

## BDD

- Given 本例项目申请已真实审核至 COMPLETED；编制账号具有本例项目 EDIT 和本例类别 UPLOAD 权限；本例主管、会签部门负责人、批准人、文控均为独立已启用账号，签名授权/个人图片均已页面启用。
- When 编制人从“文控中心 → 文件上传”选本例项目/文件夹/类型，填写编号、A/1、延迟生效及实际属性，选择本例会签部门/批准人/文控，上传真实 Office 源件。
- Then 审批路线给出明确就绪或具体缺失项；正常基线能够打开二次确认，取消后不创建审批待办。
- Actual 源件上传成功并显示预览文件名称，正式审批预检区域持续显示“系统异常”，无法进入二次确认。

## Exact UI Fixture

| 对象 | 标识 |
|---|---|
| 编制人 | DCC241016C02U / 910419 |
| 项目 | DCC241016C02P1 / 292 |
| 项目文件夹 | DCC241016C02文件夹，来自模板11 |
| 类型 | DCC241016C02T1 / 125；技术文档 / 设计和开发策划阶段 / DCC241016C02T1 |
| 类别 | DCC241016C02CAT / 908746；审批来源 APPLICATION_CUSTOM |
| 正式项目权限 | Config 保留 OWNER；U/NoSupervisor 为 EDIT；S/L/A/D 为 VIEW |
| 类别上传权限 | 仅本例类别，UPLOAD → DCC241016C02Author |
| 直属主管 | DCC241016C02S / 910422 |
| 会签部门及负责人 | DCC241016C02Dept；DCC241016C02L / 910423 |
| 批准人 | DCC241016C02A / 910424 |
| 文控 | DCC241016C02D / 910425 |
| 源件 | assets/DCC241016C02-source.docx，hash见 asset-manifest.json |
| 基线编号 | DCC241016C02-normal-baseline-1792161119569 |
| 初始版本/生效方式 | A/1 / DELAYED，上传阶段未填具体日期 |

主体标识来自真实页面及自然请求 pathname；没有解析响应 JSON 或调用接口来配置/完成业务。

## RED / GREEN / REGRESSION

- RED（真实前置断言）：`ui-session.cjs` 登录本例 U 后执行 `{"action":"step","name":"verifyBaseline"}`；`case02-real-ui.cjs` 的“审批路线已就绪”可见 DOM 断言失败，实际为“系统异常”。本例正常基线为 BLOCKED。
- GREEN：未执行；产品修复超出本线程只维护测试资产的范围，交 Root 修复后按同一 fixture 复跑。
- REGRESSION（辅助真实核验）：8 个账号独立登录/菜单/任务数据已核对；4 个签名账号待办页均“暂无审批任务”；最后源件上传及页面临时件清理通过。
- 18 个参数分支均 BLOCKED / 未执行，见 parameter-results.json；未把预检失败当成字段拦截通过。

## Evidence

- 原始运行追加记录：real-case-results.json，最后一次 normal-baseline。
- 截图：real-ui/run-1792156209781/normal-baseline-1792161119570-result.png。
- trace：real-ui/run-1792156209781/DCC241016C02U-8.trace.zip。
- 自然请求：4 次 POST `/admin-api/dcc/controlled-files/route-preview` 为 HTTP200；页面业务提示“系统异常”。POST `/upload-preview` 为 HTTP200；最终 POST `/upload-temporary/session-cleanup` 为 HTTP200。
- 受控申请 `/submit` 请求为0，未进入受控申请审批/生效阶段。准备项目申请的 COMPLETED 单独记录，不能替代受控申请终态。
- 最后上传的临时件由真实源件行删除，页面提示“已清理本次上传临时文件”，源件行及预览消失。旧权限不足上传的服务器残留未独立核验；原失败记录保留。

## Re-run

- 先读取当前 Root dispatch，核对新运行身份并更新本例 manifest 的环境记录。
- `node doc/tasks/20261016-dcc-e2e-02/rerun-case.cjs --plan` 仅展示真实步骤，不启动浏览器。
- `node doc/tasks/20261016-dcc-e2e-02/rerun-case.cjs --execute` 使用本例已核验 fixture 和授权凭据，先跑正常基线；基线通过才继续逐参数验证。基线阻塞或清理/证据失败时停止。
- 凭据仅从本机 Windows 当前用户 DPAPI 私有配置或显式授权的运行时环境读取，不写入日志/报告/trace，不重置既有账号密码。

## Boundaries

故障原因尚未确定。本线程没有用更多权限、切换审批来源、API/SQL/mock 或改生产源码来绕过此预检失败。公共矩阵、admin 原有项目指派及其它线程资产均未修改。

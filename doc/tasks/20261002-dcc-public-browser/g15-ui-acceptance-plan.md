# G15 真实 UI 验收准备计划

准备完成不等于E2E运行。Root负责注册slot6的8067前端/48067后端、当前Jar/schema/配置部署、Docker及最终验收；本Agent未启动服务、未连接业务库、未运行实际Playwright。最新计划口径：Asia/Shanghai，提前7天工作台提醒，每分钟检查生效。不改机器时钟、不自行模拟成功数据。

## 可运行脚本和边界

文件 `doc/tasks/20261002-dcc-public-browser/dcc-public-ui-acceptance.e2e.cjs`，从任何cwd执行node绝对或仓库相对路径；通过其所在整合树的真实前端package定位Playwright。现存package.json里没有这个任务脚本别名，使用明确Node文件命令，不虚构pnpm script名。

| 模式 | 实际UI动作和验收范围 | 明确不覆盖 |
|---|---|---|
| preflight | 用输入的真实动态菜单链逐页进用户/部门/角色/签名/岗位/路线/类型/项目/产品/上传/浏览/工作台，打开正式项目创建窗口和审核配置，核对当前read error，截图 | 账号/岗位/签名/模板/矩阵/项目的真实保存、非admin审核、完全生命周期 |
| browse-cancel | 查任务自有项目→点击实际folder，默认latest及ALL/WORKING/审批/历史筛选；任务file row点关联→跨项目chooser→取消，引用usage明细→关闭 | 正式关联保存/并发乐观锁、关联随新受控/历史snapshot、无数据场景 |
| upload-cancel | 选择正式project option/folder/stage/template type/file，真实文件input上传→日期/编号→完整二次确认→取消，输入保留/零申请写；通过菜单离开上传触发正式临时文件清理 | 确认真正送审、部门执行/签名、盖章、生效/审批；预览本身是真临时写，绝不称整个模式零写 |
| references-cancel | 当前项目实际leader登录→选folder→引用真实chooser→取消，零引用保存 | 正式create/cancel、distinct-project与多目录计数及非leader负向 |
| reminders-read | doc_control+distribute登录→工作台正式pending list→刷新→日期升序→remindersOnly checkbox→截图 | 真实7天边界/调度部署/下发完成、无数据不算业务PASS |

脚本所有业务动作只用Locator click/fill/check/setInputFiles，业务入口通过实际菜单/行点击；`page.goto`仅用于真实登录页。无APIRequestContext/fetch/axios/SQL/page.evaluate/router注入/mock。自然页面request仅保留method/pathname用于取消零申请写辅助证明，不读body/header/token、不开URL自己代办业务，不解析网络JSON冒充DOM结果。

## 输入合同

运行环境必须提供：

- `DCC_E2E_FRONTEND_URL`：真实slot6入口，仅http://localhost:8067或http://127.0.0.1:8067。
- `DCC_E2E_TENANT`、`DCC_E2E_USERNAME`、`DCC_E2E_PASSWORD`：Root使用AGENTS真实测试身份或已由正式页面准备并获准的职责账号；不硬编码、不写文档/结果/trace/storageState。不能把admin自动认为doc_control/签名管理员/负责人。
- `DCC_E2E_MENU_PATHS_JSON`：从fresh登录实际可见菜单取得各key的标签数组；keys为users/departments/roles/signature/positions/routes/fileTypes/projectCodes/productCatalog/upload/browser/workbench。不要把菜单component当URL，不使用手拼detail/business ID。
- `DCC_E2E_TASK_MARKER`：以20261001-dcc-integration开头的本次自有资产标记。
- 浏览/引用：`DCC_E2E_PROJECT_NAME`、`DCC_E2E_FOLDER_NAME`、`DCC_E2E_FILE_NUMBER`，均来自已在正式页面创建/可见资产；脚本不发明ID。browse目标number可以来自自有受控文件。
- 上传另需 `DCC_E2E_PROJECT_OPTION_LABEL`（完整真实“项目名 · 项目代码”显示值）、`DCC_E2E_TEMPLATE_STAGE`、`DCC_E2E_TEMPLATE_TYPE`、`DCC_E2E_TEMPLATE_FILE_NAME`、`DCC_E2E_EFFECTIVE_DATE`、`DCC_E2E_UPLOAD_FILE`，真实文件及完整source名称与本任务资产可追踪；上传number也含task标记。无模板/目录绑定/实际属性/部门批准预检立即BLOCKED，不能用默认矩阵/日期/ID补齐。

如真实账号强制改密码/验证码/菜单缺失/启用签名缺失，记录BLOCKED。脚本不会绕开验证码或改账号策略。页面截图和trace在登录成功后开启，不采集登录密码输入；同一脚本未准备password签名/负责人审批动作，所以后续完整审批脚本需同样在录制前避免凭据泄漏或采用明确脱敏策略。

## 运行命令（仅准备，尚未执行）

```powershell
node doc/tasks/20261002-dcc-public-browser/dcc-public-ui-acceptance.e2e.cjs preflight
node doc/tasks/20261002-dcc-public-browser/dcc-public-ui-acceptance.e2e.cjs browse-cancel
node doc/tasks/20261002-dcc-public-browser/dcc-public-ui-acceptance.e2e.cjs upload-cancel
node doc/tasks/20261002-dcc-public-browser/dcc-public-ui-acceptance.e2e.cjs references-cancel
node doc/tasks/20261002-dcc-public-browser/dcc-public-ui-acceptance.e2e.cjs reminders-read
```

产物在任务 `e2e-artifacts/<mode>-<timestamp>/` 的result.json、每个限定步骤截图、trace.zip。只有status=PASS_SCOPED_UI_ONLY时表明该模式的实际限定UI断言通过；observedIssues/无task数据仍需处理，不升级为完整Goal PASS。失败保留结果/已登录trace，未登录失败不录密码截图。所观察网络路径是辅助，业务通过依据为DOM。

## 完整十二流程写验收顺序

1. 页面准备职责：真实用户启用/部门leader/岗位及业务permissions。每个签名者本人准备实际签名图片并启用；电子签名管理员页面授权。不要生成假签名图，不按用户名猜身份。
2. 基础管理员通过正式页面创建任务文件夹模板及三级以上类型/对应类别、NAS提交目录绑定、NEW/REVISION/OBSOLETE矩阵。若共用配置需Root先确认具体影响/恢复计划；不能改默认生产矩阵为测试捷径。
3. 文控页面配置非admin项目审核人。项目申请人页面新建两个任务项目/产品/不同leader、CE等三组属性和同模板；取消核对零写，再真正提交。指定审核账号页面审核，正式批准账号页面批准，刷新项目列表确认已创建。错误/停用配置及旧申请不被新配置接管另测。
4. 进入DCC基础条目，分别为项目维护项目文件模板及独立folder；改模板不改既有project tree。空目录新增/改/删；含child/file/reference阻断删除。当前没有数据就此停止，不API补齐。
5. 上传两项目任务文件：完整原名精确同名拒绝，大小写/后缀差异许可按实际支持格式分次UI操作。默认CE→本次FDA，二次确认取消保留，再真正提交；正式编号与temp/fullname事实一致。后改项目NMPA不覆盖历史CE/FDA。
6. 两部门即使同leader也分别指派签名；被指派人实际会签签名。错误密码和外人负向零推进。升版时选关联整改账号与期限，在当前本轮签名payload确认安排，不先造任务。
7. MATRIX_APPROVAL由真实批准人选择同租户启用文件负责人并同次签名；缺/停用/外租户拒绝，错误密码不推进。旧批准返回不关闭新弹框。冻结历史owner/signature/BPM正确。只有该节点适用，不给作废增加owner节点。
8. 不培训直接文控；勾培训由文控上传本次线下真实文件完成，旧ticket/file/BPM拒绝。文控审核签名，真实PDF盖章/存储失败不能受控/通知。预设未来生效保留、controlledTime系统生成。
9. 真实日期/任务检查生效：today/due/future及7天/8天边界由合法UI日期输入准备；Root确认每分钟注册job、正确Asia/Shanghai配置，用实际job页面/日志核验，禁止改全机时间或SQL改状态。未来受控旧版继续执行，生效时同事务旧版作废20年，三个版本不同生效次序需单独场景。
10. 工作台按日期下发：真实doc_control+distribute只读提醒，选实际部门/方式/收件人，二次确认后UI保存，future禁执行标识保留；刷新已下发移出，接收人本人登录核对通知/收件，不以管理员消息代替。
11. 浏览具体task文件在正式可达storage检出，另一账号争抢；本人新文件或合法remark-only检入，唯一工作小版A/1-1/-2，正式权限/源hash/position继承。具体旧工作正文PARTIAL或REPLACEMENT独立送审；失败连续同目标A/2重提，每次file/BPM/body/signature保留。没有从project可达storage路径则记录入口缺口而非API代做。
12. 列表关联→跨项目tree/全局→confirm：自Master禁选、current latest待生效、history原snapshot不变；受控成功才相关整改通知，失败不通知。负责人引用至两个folder/另project，distinct计1；非leader禁止，saved pin浏览/强trace权限分开，usage全局/可见/受限准确。取消精确引用，失败保留，最后count/color恢复。最后从受控selector操作panel发起独立作废，会签批准后结束、无后续节点，20年正文/名称编号占用。

以上全生命周期尚未自动执行或声明通过。本批脚本覆盖列表限定范围；Root继续逐步扩写/执行其它写入UI验收，不能用离线测试或自然HTTP200填空。

## 准备验证及已知限制

`node --check`仅校验语法；`verify-ui-acceptance-preparation.cjs`用AST确认禁止直接网络/SQL/mock/执行桥、必须真实UI动作、slot/时区/记录登录后开始、12flow报告与Cleanup Keep。校验脚本不会导入或执行E2E文件，无浏览器启动。Locator语义仍需真实DOM首跑，失败应修实际selector并记录，不靠接口捷径。

当前准备不连接服务检查角色/数据/schema；七天提醒与每分钟检查用户已确认，应写入正式运行配置/调度并验证，不能继续列为“用户未确认”。迁移是否执行由Root另行授权/验证；Docker恢复不等于迁移已完成。

# 第2步：取消项目预设文件名单的上传门禁

状态：IMPLEMENTED_FRONTEND_AWAITING_ROOT_INTEGRATION。仅主仓int_qms。原方案历史准备状态由Root本轮LD02授权推进；有限FE已按g47-free-file-upload-bdd执行有效RED/GREEN，具体交付在g47-free-file-upload-fingerprints.json。后端和Root运行验证单列；不处理第3步目录简化。下文是本批原最小方案及保留边界，不冒称整体完成。

目标操作：选择项目→选择项目存储文件夹→直接选择正式文件类型→选择真实文件→填写编号、版本、生效日期和申请属性→核实际审批矩阵→二次确认送审。**无需先维护项目预设文件名称或按项目模板阶段逐层选择。** 项目文件模板可保留可选名称建议，不能决定是否有上传资格。

## 最小接线

| 当前门禁/入口 | 第2步处理 |
|---|---|
| upload“阶段”与“文件类型”只来自projectFileTemplateItems | 替换为一个正式类型选择，数据取既有GET `/dcc/file-type-taxonomies/upload-options`。显示完整类型路径帮助区分同名项，最终只提交实际选中的taxonomy ID；不能默认第一条类型或从folder推类型 |
| `loadProjectFileTemplate` 空清单报“项目文件模板未配置”并禁用选择 | 清单读取改为可选建议，空清单不阻上传；如仍提供该辅助功能，读取失败在辅助区明确显示，不把它改成假成功，也不作为必需类型/文件资格 |
| `canSelectProjectTemplateFileName`、selectedProjectTemplateItemId、fileName validator精确匹配预设item | 移除“必须选模板名称/同模板类型”的资格条件；按真实选择文件和必需显示字段校验，不再用模板ID作为送审前置 |
| 阶段/类型/template-name切换handler同时清空类型与名称 | 改为实际单一类型变更的上下文失效：重取唯一分类、审批路线和既有存储目录，旧ticket/preview按正式清理与重传规则处理，不能把旧类型票据沿用 |
| 后端 `validateUploadLocation/validateUploadSelection` 的项目文件名单限制 | 后端Owner在公共新上传预览/提交调用边界去掉该名单资格条件，保留启用taxonomy路径及唯一active-category解析，不能改底层正式票据/名称/身份guard弱化所有业务 |

类型合同沿既有唯一正式API：启用上传候选→用户明确选实际taxonomy ID→GET `/{id}/active-category`解析唯一启用category ID→加载该category真实审批矩阵。停用类型/失效路径、分类缺失或歧义明确阻止，不借第一分类或其他动作路线。`upload-options`当前只筛active节点，前端仍须区分可选择的正式文件类型与分类父节点；其合法路径/映射由正式service最终复核，不新增另一套模板白名单。

保留提交 `fileTypeTaxonomyId/categoryId` 与后台映射一致性；类型ID按正式Long字符串处理，避免旧number声明隐式丢精度。无需新写API、无需把普通上传账号授category-manage权限。

## 文件名称与真实票据

最终HTML明确：判重是**真实源文件完整名称（含后缀、大小写精确）**，模板显示名和生成的受控PDF名都不能替代。现有submitter把 `sourceFileName=preview.fileName` 与显示 `fileName` 分开，后端最终 `sourceOriginalFileName` 来自真实SOURCE票据；跨项目全名/保留期名称占用应继续以此为准。

最短正向方式：用户选择本地真实File时，以该File.name初始化本次显示名称（随后正式preview核相同原件）；没有模板名称也可上传。不能自动拼后缀、改大小写或以预设标题替换真实原件名。若保留“显示名称”手填，明确它只是显示标题，不改变源文件完整名或名称claim，确认框继续展示实际preview完整名。

当前正式 `DccSourceUploadSession.newUploadPrefix(projectId,typeId,fileName)` 把显示fileName加入session，预览和提交一致是现有票据边界。因此在上传前从localFile.name建立本次名称上下文；上传后若用户改变显示名或类型，必须先按已有清理重传合同创建新session/ticket，不能直接换名字继续使用旧ticket。实际multipart originalFilename、infra stored.name、ticket.fileName、源bytes/SHA、actor/category/session/purpose均继续一致；不能为了取消预设名限制接受客户端伪造原名。

## 继续保留的门禁

项目及**项目存储文件夹仍必选**，真实folder归属于本项目/tenant且启用、正式项目EDIT/OWNER与分类UPLOAD权限保留；逻辑folder位置登记、同事务回滚保留。现有NAS提交目录加载/选择规则属于第3步，此步不同时改。

保留真实源文件支持类型/二进制验证、图纸配套PDF、preview/ticket绑定和精确SOURCE正文、正式Long、全球源全名/原编号claim及作废20年占用、单文件链/版本守卫。保留LD01正式产品投影及server身份解析，不能恢复projectCode替代产品；保留属性defaultSource/actual、真实部门/批准/签名readiness、培训选择、二次确认与上下文变化拒绝。没有新增14位编码或制造默认属性限制。

## 下一批最小BDD与组合验证

- Given合法启用项目/folder/type/category/审批路线且项目无文件名单，When用真实File输入预览并提交，Then送审成功，位置/实际完整源名/编号/版本/属性准确保存，不要求预设item或阶段白名单。
- Given模板可选建议与真实文件名不同，When上传/确认，Then真实SOURCE票据和sourceOriginalFileName保持实际全名；模板建议不改变claim或强迫改原件。
- Given类型变更/停用、category缺失或多重映射，When准备送审，Then旧preview/ticket失效或准确报错，不借第一类型/分类、旧矩阵或吞异常。
- Given同名原件在另一项目或保留期作废链已占用，When自由上传，Then正式nameclaim仍拒绝，换模板/显示标题/编号不绕过；同逻辑文件自己的合法版本沿用仍按原合同。

实施时FE实际SFC行为RED→GREEN与公开preview/submit真实服务组合各自覆盖无模板正向，再由Root真实页面选择任务原件验明。仅helper/static测试不能代替公共入口或真实票据。方案归属：FE uploader/type API/专属测试；后端Owner公共Workflow名单调用与必要正式type校验/组合测试；Root统一Review、类型/构建、迁移与页面执行。本文件不宣布第2步已经完成。

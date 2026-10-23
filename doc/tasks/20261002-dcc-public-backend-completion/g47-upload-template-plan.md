# LD02 / G47 正常上传无需文件模板名单

Status: ready_for_closeout — LD02 backend software frozen for Root review, runtime not executed by this agent.

HTML flow01:78：选择项目→存储文件夹→文件类型→实际文件→日期/编号/初始版本/属性/关联/培训→确认提交。它不要求文件名已在项目文件模板。项目存储文件夹模板属于创建/目录，不等于当前项目文件名单；两者不能混用。

有限调用事实：Workflow.prepareSubmitContext:2258-2264 对所有 controlledUploadSubmit 且非显式revision，调用 projectFileTemplateService.validateUploadSelection(project,type,fileName)。因此NEW正式提交与createWorking都被同一文件模板名单门禁挡住。正式模板服务自身管理/建议能力可保留，移除仅正常上传这条不符合业务的调用；不删除模板表/service/管理API。

BDD:

- Given 新文件完整原名不在项目文件模板甚至项目无该名单，但所选项目/逻辑文件夹/启用叶子类型与category映射/权限/真实票据/产品/名字占用/审批与日期均有效，When正式submit或working-create，Then正常创建/提交，真实sourceOriginalFileName取票据而非模板名，文件displayName不要求模板匹配。
- Given权限缺失、类型不是启用叶子或category绑定不匹配、真实票据失效，When同一入口，Then仍零创建/BPM推进；取消名单不放宽任何正式资格或伪产品来源。
- Given显式项目文件模板管理调用，When保存/维护模板，Then既有合法type/模板约束维持，不默认兼容/填名成功。

计划唯一生产Workflow删除正常上传validateUploadSelection调用及其不再使用注入（如其它路径没有使用才去注入）；统一normalupload和working-create仍prepareSubmitContext同正式边界。既有测试中要求模板失败/成功验证需按新业务重写为“名单配置无关，正式票据/权限仍严格”；不让mock总成功冒实际门禁。

有效RED必须在旧生产代码下，以template port会拒本新名的fixture验证实际正常submit/workingcreate误拒；证明正确type非名单名不是通过默认mockno-op。GREEN定向Workflow既有组与必要公共HTTP/建议模板service回归；保持sourceName exact、NameClaim二进制、editor/OWNER、typed产品身份、currenttenant/departmentreadiness/date/Placement。无新schema/DB或FE字段，先不并行做LD03目录。

补充同一完整入口：Workflow.validateSourceUploadContext:695-697 在上传预览/生成票据前另调用validateUploadLocation；该调用仍要求项目已配置文件模板type名单，故必须同步去除正常NEW_UPLOAD位置模板门禁。用正式resolveFileTypeTaxonomy/validateNewFileTaxonomyLeaf验证启用叶子（含真实分类路径和category exact绑定），不丢掉曾由模板间接执行的类型资格。CHECKIN/EXTERNAL_REVIEW不改变。原Template Service validateUploadLocation/Selection仍保留独立管理/建议合同和测试。

Root当前package活跃，任何测试编译可能更改target影响打包，因此此阶段不运行Maven，也不写生产/测试。Root确认package terminal后执行TDD，不重新请求业务许可。

## 最终接线和验证

Root先完成LD01package终态后才执行本批源码/测试。移除仅Workflow正常previewLocation/submitSelection两个项目文件模板名单门禁；TemplateService/Controller/维护能力未修改。NEW预览用正式activepath/depth>=3/leaf，preview及normalcontrolledsubmit均调用正式resolveActiveCategoryId且必须等reqcategory，保留唯一启用category映射；现有权限/产品/票据/真实原名占用/日期/逻辑Placement未放宽。

有效RED两个正向分别在旧preview/workingprepare被模板门禁拒；首GREEN178/2通过。关联尝试226含2Mockito unusedstub fixture错误保留；只修局部fixtures，不松production。Reviewer发现旧template间接uniquecategory资格需替代，新增actualTaxonomyAdminservice wiredtwoactiveCategories→Workflowpreview有效RED1assertfailure0error；正式unique映射调用后最终240/7全0，23:05:29。旧178/226与final240重复，不累加独立场景。

生产仅Workflow一文件，测试Workflow和SourceContext两文件；LD01原17中除Workflow/WorkflowTest后15资产rawSHA不变。7最终JUnitXML另存g47-*不可依赖target后续覆盖，源码/日志/XML精确SHA见fingerprints/verification。无Schema/DTO/FE/NAS改动，不运行实际DB/Redis/Token/服务/打包/Git/浏览器/E2E；后续Rootreview与真实UI验收另行处理。

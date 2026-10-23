# 线程B：项目属性与基础配置

任务id：20260930-dcc-b-project。Owner：B。优先级P0。依据需求v1.3与SC-1.0。G0/G1通过前只设计及编写BDD。

## 目标与边界

1. 项目创建及编辑增加目标市场、注册人/生产方两个独立选择、文件转移与目标；保存项目默认值。
2. 给上传、升版、作废提供共用属性组件及值对象，初始化来自项目默认；允许本申请修改、草稿回显、提交快照。公共文件字段由主管理接入，A负责流程提交与冻结。
3. 项目负责人使用正式账号事实，与当前project OWNER权限约定清晰；不得用projectLeader文本、申请人或任意菜单权限冒充唯一负责人。
4. 可复用文件夹模板库，项目创建时选择并生成项目独立目录；模板编辑仅权限检查、不审批；既有模板与项目文件名清单的转换须明确。
5. 文件类型增改删及被使用规则；向A输出类型到矩阵的正式映射，不直接改A路线。
6. 产品项目申请字段链路与批准创建事务保留。产品创建审核人仍待讨论，不擅自固定新角色、admin或跳过审核。只准备配置方案和已确定字段，不以待定角色阻断其他独立功能。

允许：项目、模板、类别/类型归属文件、项目对象、项目API wrapper、独立ProjectAttributes组件、对应测试。不得改共享ControlledFileDO/SubmitVO、WorkflowService、公共上传/详情/浏览页；交接入差异给主管理/A。

## 当前依据

- ProductCatalogTabPanel.vue有创建弹框，projectLeader为文本；DccProjectProductCreateServiceImpl.requireAdmin固定admin。
- DccProjectFileTemplateServiceImpl为逐项目“分类＋文件名”清单；ProjectFileTemplateController保存只校验update权限，不启动审批。
- DccProjectAccessServiceImpl与dcc_project_access_rule已有OWNER/EDIT/VIEW能力，必须复用正式授权事实并审查其与唯一负责人规则的差异。
- 三组属性尚未发现完整DCC默认/快照链，不能只增加表单控件。

## 里程碑

- B1：属性值对象、默认来源、申请快照、负责人合同和模板数据方案，G1Review。
- B2：项目默认值与共用组件RED/GREEN，输出冻结接口给A和主管理。
- B3：模板库、项目独立文件夹与类型映射，权限/重复/删除保护回归。
- B4：前端接入说明、迁移/历史缺值策略、B模块Review证据。

## 必须记录的BDD

| 编号 | Given | When | Then |
|---|---|---|---|
| B-01 | 创建项目并填写三组值 | 审核批准创建 | 默认值完整保存回显，两个身份选择独立 |
| B-02 | 新上传/升版/作废申请 | 选择/读取所属项目 | 三路径分别带出项目当前默认，允许修改 |
| B-03 | 本申请实际值已修改 | 提交、刷新或重开已保存草稿 | 保留本申请值，项目和其他文件不变 |
| B-04 | 历史申请已提交 | 修改项目默认 | 历史/在途快照不变，新申请读取新默认 |
| B-05 | 作废申请修改属性 | 提交作废 | 原受控版本历史属性不被回写 |
| B-06 | 草稿存在手动值 | 切换项目或恢复默认 | 先明确确认再替换，不能跨项目串值 |
| B-07 | 项目缺默认或用户无权限 | 发起/修改申请 | 明确报错，不偷偷设否/不适用或借其他项目 |
| B-08 | 有/无模板编辑权限 | 编辑保存模板 | 有权限直接保存不创建审批，无权限拒绝 |
| B-09 | 项目从模板生成目录 | 后续编辑模板 | 已有项目保持独立，新项目取得新结构 |
| B-10 | 类型/文件夹已被正式使用 | 删除/停用 | 按批准规则保护文件、引用与历史，不级联误删 |
| B-11 | 非负责人或其他项目负责人 | 请求项目负责人专属事实/操作 | 正式身份不可伪造，不从负责人姓名推断 |
| B-12 | 分类绑定不唯一/失效 | 上传类型/模板候选解析 | 明确阻止错误选择，不fallback第一类别 |

市场多选及N/A互斥等按G1确认的交互合同再写测试；未确认前标设计建议，不宣称业务已确定。

## 验证计划

现有测试：DccProjectFileTemplateServiceImplTest、DccProjectFileTemplateControllerTest、DccProjectAccessServiceImplTest、DccFileTypeTaxonomyAdminServiceImplTest、DccFileTypeTaxonomyControllerTest。新增项目属性服务/组件、创建事务和三申请取值的业务测试为必须项；现有项目创建静态检查不能替代新字段保存回读测试。

IntRuoyiBackend：

```powershell
mvn -pl yudao-module-dcc -am "-Dtest=DccProjectFileTemplateServiceImplTest,DccProjectFileTemplateControllerTest,DccProjectAccessServiceImplTest,DccFileTypeTaxonomyAdminServiceImplTest,DccFileTypeTaxonomyControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

实际新增测试追加列表并核对目标测试数量。IntRuoyiFronted现有静态回归候选：

```powershell
node tests/e2e/dcc-project-file-template-static.spec.js
node tests/e2e/dcc-project-product-approval-static.spec.js
```

这些是静态测试，不是业务E2E；开工先核对脚本确实不访问真实页面。属性组件应新增选择互斥、条件字段、切项目确认、请求失败可见及快照映射测试。主管理整合后统一类型与构建检查；真实保存回读仅另获当轮授权后进行。

## 交付与Review

输出项目属性合同、组件props/emit、创建/修改API、负责人解析、模板创建项目事务与删除影响、迁移方案、历史缺值明确展示方案、真实测试证据及公共文件接入差异。

主管理重点Review默认值≠申请快照、作废快照隔离、模板权限≠审批、负责人文本≠账号授权、历史不可回填伪造。审核人未定不得用假账号或默认通过解决。

## 可复制给线程B的指令

你负责线程B。先读AGENTS.md、对应docs规则、docs/dcc-parallel-delivery的README、shared-contract、ownership及本任务书。等待G0/G1通过再在独立worktree实施，先BDD和RED再GREEN。只改B归属文件并新增独立组件，公共文件接入交主管理。产品审核人待讨论，不猜测；禁止未经授权提交推送、数据库写入、服务重启、发布或真实E2E。交接口/组件、迁移、RED/GREEN及回归证据供主管理Review。

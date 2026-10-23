# DCC G03 其他类型本次自定义矩阵

## Goal

只处理 G03：其他类型由编制人配置本次会签部门、批准人、文控职责；正式主管从组织关系取得，沿用既定流程。本次配置随精确申请/版本/轮次保存，草稿恢复和驳回重提沿用，公共矩阵零变化。

## Authorization

- 最新最终接收：Root M14 真实 exit 0，23 reactor SUCCESS；M12 无生产差异的 963 PASS 与 M14 G48 声明 18 / 原父类 27 独立 45 PASS 合并，最终有效 47 份 class 报告 1008/1008 PASS，11865 输入零漂移。F02 38/38、T04 类型和 B01 构建均通过，2429 生产输入零漂移。全软件域正式接收，生产/tests 继续冻结，不重复套件或扩大范围；Root 统一清理/报告，本线程只同步记录，不进行代码、提交或跨线程消息。
- 当前分支须为 int_qms；本轮已核对。
- 初始阶段 SPEC_ONLY；C01后为 WRITE_GRANTED_LIMITED。可写自有记录、IntRuoyiFronted/src/views/dcc/controlled-file/upload/CustomApplicationMatrixEditor.vue 和 IntRuoyiFronted/tests/unit/dcc-upload-g03-custom-matrix-editor.test.cjs。
- C01覆盖初版文控岗位建议：正式类别唯一approvalSourceMode；本次批准人/文控审核人各一个明确账号；applicationCustomMatrix沿正式同资源保存/冻结，主管来源不变。
- A08于2026-10-15释放BE冻结并授新增service/file/DccApplicationCustomMatrix.java、DccApplicationCustomMatrixService.java及专属DccApplicationCustomMatrixResolverTest.java。私有javac/JUnit只写本任务运行目录，不写target；Maven仍无票。
- A10追加仅approvalSourceMode：Category DO/SaveReq/Resp/AdminServiceImpl/接口、TaxonomyAdminServiceImpl/RespVO必要upload投影锚点，FE fileCategories.ts/fileTypeTaxonomies.ts与categories/components/CategoryForm.vue，专属测试与唯一20261015_dcc_category_approval_source_mode.sql。TaxonomyController/接口未明确授，接入缺口先列精确Root转交请求；不写公共申请API。
- A14已转交TaxonomyAdminService.java接口与TaxonomyController.java，仅upload-options调用正式projection；非leaf路径保留，所有leaf必须唯一active category及合法mode，缺映射报错。维护CRUD和原权限不改。
- R03撤回A14整页leaf阻塞：已授projection/Resp/API逐项uploadBlocker，追加upload/file-type-options.ts仅保留source/blocker给选项。坏leaf保留明确原因，父路径/合法leaf不被挡；不改G01独占upload/index.vue/提交/Workflow。来源字段仍CategoryDO唯一，不新增schema字段。
- Root续派纯合同扩展：同一个DccApplicationCustomMatrixService新增PreviewSourceContext（无fileId/attributeRound）与明确preview canonical/read/bind reserved函数和专属测试。共享token/DO/Workflow仍G01待交，纯摘要不是签名证明。
- A20追加CategoryReviewMatrixTable.vue仅真实类别来源读取/编辑投影与专属测试；从正式getFileCategoryList读取唯一saved row，不给review row补默认来源，不改BE矩阵响应/原public策略，全类型仍等Root票。
- A22接DccApprovalMatrixTokenService.java整文件仅追加custom-purpose/phase与tagged supervisorHMAC方法，旧PUBLIC方法原文不改；可写独立20261015_dcc_application_custom_matrix.sql DDLguard，FileDO/G01和公共schema/G05仍只交exact hunk不写。
- A23/A24已实际接管a23-main-before.json清单当前27源：main Workflow/接口/FileDO/Resolver/Execution/Bound/VO与FE upload/submitter/matrix/API/read/DetailApplicationPanel、ReadinessResp/Service。不是等待G01；Query/Revision仍G06独占、schemaG05唯一。DatePolicy/Rule/Catalog/签名确认独立Root/G01保护。
- G01/G04 完成且 Root 明确转交共享文件后才能实施生产接线；本阶段不造平行 API。
- 禁止子 Agent、Git 提交/推送、分支切换、worktree、E2E、服务重启、远端、真实业务库写入。
- Maven、全类型检查、大构建须 Root 唯一验证票，本阶段不执行。

## Milestones

1. 核对规则、当前源码和相邻测试，记录本轮证据与源码指纹。
2. 完成 BDD、端到端最小合同、真实 RED 方案、精确文件/锚点和依赖。
3. 运行已获准独立定向测试与文档结构验证，交付 review-ready.md。
4. 待 Root 转交后执行真实 RED/GREEN/REGRESSION；本阶段不声明修复完成。

## Expected Verification

- 最终正式门禁已通过：M14 exit 0，23 reactor SUCCESS，G48 声明18 + 原父类27 独立合计45/45、0 fail/error/skip；M12 其余45类963 PASS 无生产差异，最终有效47份class报告共1008/1008 PASS，11865输入零漂移。G03 自有53项、A34 6及原父类已正式通过。
- 最后新增的直接 NEW 同载荷 replay 与异两账号载荷拒绝断言已在 M12 正式实际通过，原待验限制已解除；不再重复私有验证。
- M12 历史45 ERROR的类别fixture问题已在M14独立复验通过，不再是待验或软件失败。F02四suite38/38、T04类型检查exit0、B01构建exit0均实际通过，2429生产输入零漂移；各重叠前端scope不相加冒充唯一用例数。
- 软件验收已接受；工程仅 Git 提交/推送未授权。Root 统一清理/报告，本线程不自行收尾。未要求的 E2E、真实库迁移、服务或部署不属于本次软件失败项。
- 只读复核现有类型选择、矩阵预检、草稿保存/恢复、提交、驳回及流程派生链路。
- 读取并实际执行最接近的独立 Node 行为测试；区分行为与静态合同结果。
- 设计后续执行现有生产方法的 RED/GREEN 与隔离事务/重放回归；无 Maven 票不运行 Java。
- 文档结构、BDD、文件存在性和指纹校验。
- C01组件TDD：编译并挂载真实SFC模板及Vue状态，交互验证props/emits/回显/精确校验；Element Plus控件为显式隔离端口，不冒称真实页面。
- A08独立后端TDD：Java17编译当前两份生产源码与唯一专属测试；实际JUnitLauncher执行，私有输出按RED/GREEN/REGRESSION分目录，无项目旧jar/target。

## BDD

- Given 选择其他并配置本次会签部门/批准人/文控职责，When 保存后恢复草稿并提交，Then 实际保存配置完整回显并参与既定流程，公共矩阵零变化。
- Given 本次其他配置已经冻结且申请被驳回，When 原编制人检入并重提，Then 精确沿用本申请保存配置并复核当前人员，不因公共矩阵变化覆盖。
- Given 本次会签为空，When 提交，Then 提示“请至少选择一个会签部门”，且零流程推进。
- Given 无正式主管、无批准人或无文控职责，When 提交，Then 分别给出具体缺项并阻止。
- Given 自定义人员被停用/删除/转部门/缺少岗位、权限或签名授权，When 提交或驳回重提，Then 指明不可用角色/账号并拒绝，零签名和推进。
- Given 其他配置为独立申请/版本/轮次，When 读取历史或重放，Then 不串当前版，重放同载荷回读、异载荷拒绝；晚失败全事务回滚。

## 设计约束检查

- 无 fallback、降级、吞异常、默认原因、模拟成功。
- 主管身份不可由客户端任选；不自动选择具体会签人。
- 会签部门与下发部门独立；全部最终会签部门均需完成通过。
- 保持租户、组织、权限、精确版本、真实签名、事务与重放边界。
- 复用现有正式申请/API/执行合同/快照，不新增或修改公共矩阵。
- 不处理 G01/G04/G06 等他项，不写未授予共享文件。

## Current Status

blocked — software_verification_passed，软件验收已由 Root 正式接受：最终有效47份class报告1008/1008 PASS、M14 exit0/23 reactor SUCCESS，11865输入零漂移；F02 38/38、T04类型、B01构建均通过，2429生产输入零漂移。工程仅 Git 提交/推送未授权，不标 completed；Root 统一清理/报告。生产/tests 冻结，不重复套件、不扩大scope。未要求的E2E/部署不记软件失败。

## C01 Component BDD

- Given 正式类别APPLICATION_CUSTOM且保存了会签与两个明确账号，When 组件挂载/接收恢复值，Then 原值回显且零自动emit，不因目录变化覆盖。
- Given 本次自定义来源，When 实际选择/清除部门和批准/文控账号，Then 只emit对应本次受控值，全部部门可删，其他值保留且不修改props/目录。
- Given 公共来源或未授权编辑/目录加载失败，When 调用控件事件或validate，Then 不emit错误自定义配置并明确展示错误。
- Given 空会签/缺批准人/缺文控账号/不可用已保存身份，When validate(SUBMIT)，Then 精确缺项或角色账号报错；validate(SAVE)仅保留合法未填值，不伪造就绪。
- Given 大整数或重复/非法目录身份，When 编辑/校验，Then 字符串Long不丢精度，非法或重复明确拒绝；数据来源变化后无自动换人。

## A08 Backend BDD

- Given 正式APPLICATION_CUSTOM来源与两个明确账号/会签部门，When 编译，Then 仅生成既有会签ALL/100%、单批准及单文控节点，没有公共route ID/主管替代/其他流程节点。
- Given 已保留精确申请和轮次，When 生成并读取canonical/hash，Then Long为精确字符串、部门排序归一、不改输入，配置与租户/项目/类别/类型/原编制人/版本申请/轮次/准备定义/context绑定。
- Given 合法但未填完整的草稿，When canonical往返，Then null/空集与两个未填账号准确保留，不能通过正式编译；公共SOURCE或缺正式上下文明确拒绝。
- Given 缺会签/批准/文控、重复或非法身份、错digest/context或不规范JSON，When 正式编译/恢复，Then 具体错误且不自动恢复公共矩阵/默认账号/轮次。
- Given 已编译运行节点，When 上层接入，Then 必须继续复核真实组织主管、部门负责人及当前账号/岗位/阶段权限/签名授权/图片，本纯服务不声称人员资格或事务验收通过。

## A10 Category BDD

- Given 管理员新建类别，When 明确选择PUBLIC_MATRIX或APPLICATION_CUSTOM并保存，Then 正式类别字段原值持久化/回显，现有目录/类型/权限边界不变，不改公共矩阵。
- Given 现有类别来源缺失/未知，When 读取或编辑保存，Then 明确报错且零写，不能按名称或缺矩阵补PUBLIC；新建默认PUBLIC是明确创建配置，不是运行回填。
- Given 文件类型对应唯一启用类别，When 生成上传候选，Then 返回该正式类别来源；目录来源尚未接入Controller时准确报告依赖，不前端猜来源。
- Given 首次/重复迁移，When 校验正式SQL，Then 只增加类别配置列、既有类别显式PUBLIC一次迁移，未知已存在值拒绝，已由管理指定CUSTOM不被重放覆盖，无版本/申请/公共矩阵写入。
- Given 非leaf路径/合法leaf/缺映射leaf，When 正式upload-options投影，Then 非leaf明确null路径、合法leaf严格取唯一active类别mode、缺映射leaf错误；不按名字或空配置猜来源。

## R03 Row Blocker BDD

- Given 未映射A与PUBLIC的B及CUSTOM的C共存，When 正式服务/Controller返回候选、decoder解码并生成路径，Then A仍在且mode=null/具体blocker，B/C来源正确且blocker=null，父路径mode=null/blocker=null。
- Given 叶节点不足三级/映射歧义/来源未知，When 投影，Then 仅该项阻断并具体解释，不能推断PUBLIC、过滤该项或拒整个候选请求。
- Given 部门/类型/类别查询发生技术错误，When 读取候选，Then 原错误传播，不能catch成空目录、全部blocked或模拟成功。
- Given 响应为正式三形态，When decoder/helper处理，Then 明确blocked行可保留，但缺字段/空blocker/合法source与blocker混合/父节点有来源等非法合同仍准确报错；真实选中提交继续后端严格校验。

## Preview Context BDD

- Given 尚未分配file/保留轮次的正式custom预检，When 同服务生成preview canonical，Then 只用明确PreviewSourceContext、独立PREVIEW格式与真实上下文身份，不造0/defaultfile/defaultRound/publicroute。
- Given 正式已有签名票据校验通过的preview，When 分配实际文件/保留轮次并bind，Then 相同tenant/project/category/taxonomy/requester/type/context/exact定义生成RESERVED canonical；任一身份变化拒绝，两个格式不能互相冒用。
- Given 缺上下文/未知source/非法Long或被篡改preview，When canonical/read/bind，Then 具体错误；当前纯合同只检结构/digest，上层仍必须在现票据机制验HMAC/主管证据与当前人员就绪。

## A20 Review Category Source BDD

- Given 旧review矩阵row不含来源而正式类别保存CUSTOM/PUBLIC，When 点击编辑/预览，Then 读取精确categoryId对应saved row并传真实source及原属性，不猜PUBLIC或sort0。
- Given 正式读取失败/唯一身份缺失/未知来源，When 点击编辑，Then 当前可见区域显示具体错误且不打开/保存矩阵。
- Given 连续点击或页签/类别revision改变、卸载，When 旧类别读取晚回，Then 不打开旧申请弹框、不覆盖新选择反馈。

## A22 Custom HMAC BDD

- Given 完整custom preview/reserved实际身份和两账号部门，When 用现配置secret签发并验签，Then proof明确purpose/phase，绑canonicalhash/exactdefinition/时效；错tenant/file/round/context或跨purpose/phase拒绝。
- Given 真实组织主管Evidence，When 签发/核对custom tagged主管proof，Then 两账号部门/canonical上下文与真实主管source全部绑定，改主管/source/digest/时间/未知字段拒绝；不假publicrouteID或SHA冒HMAC。
- Given 合法同上下文proof重复校验/过期或未生效proof重放，When verify，Then 有效同载荷可回读，过期/将来/不同上下文拒绝；事务幂等由后续Workflow负责，不冒称token一次性消费。
- Given 既有PUBLICbaseline/native签名向量与新增三nullable草稿字段，When 追加方法/静态DDL验证，Then 旧方法原文和签名向量不变，DDL仅新增列无历史回填/默认值/真实库执行；savedcustom类别改mode按后续显式阻断hunk处理。

## A23/A24 Actual Workflow BDD

- Given 正式CUSTOM类型、组织主管及当前启用/岗位/权限/签名资格完备，When actualpreview→saveWorking→GET恢复→submit，Then 使用实际两账号/全部部门，正式主管节点先开始，公共route/node全表精确不变，custom snapshot绑实际file/round。
- Given 空部门/缺主管/缺账号/当前资格失效或savedcustom来源后来改变，When 直接预检/正式提交，Then 具体错误且零签名/BPM推进，不自动转PUBLIC或更新matrix。
- Given 已保存custom或已拒绝精确前驱，When 公共矩阵变化/重提，Then 仍读原custom、真实新round重绑HMAC，不复制旧签名；wrongtenant/file/round/phase拒绝。
- Given 当前真实交易晚失败或同键重放/同键异payload，When 保存/提交/冻结发生，Then 全部当次DB+BPM/附件事实回滚，可重试；同载荷只一实例，异载荷冲突。不要用独立service PASS代替。

## Cleanup Keep

- doc/tasks/20261014-dcc-gap-other-custom-matrix/official-verification-receipt.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/final-checkpoint-manifest.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/freeze-source-check.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/declared-test-methods.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/frozen-checkpoint-verification.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/upload-three-handback.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/upload-three-handback.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/g06-custom-inheritance-hunk.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/a23-main-before.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/a22-public-token-vectors.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/token-public-before.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/run-workflow-java.cjs
- doc/tasks/20261014-dcc-gap-other-custom-matrix/WorkflowJUnit.java
- doc/tasks/20261014-dcc-gap-other-custom-matrix/run-token-junit.ps1
- doc/tasks/20261014-dcc-gap-other-custom-matrix/token-test-support.py
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/regression-custom-13-public-v2/compile.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/regression-custom-13-public-v2/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/regression-custom-13-public-v2/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/regression-custom-13-public-v2/sources.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rework-10-stable/compile.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rework-10-stable/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rework-10-stable/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rework-10-stable/sources.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rf11-red/compile.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rf11-red/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rf11-red/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rf11-red/sources.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rf11-green/compile.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rf11-green/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rf11-green/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/workflow-junit/rf11-green/sources.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/changed-files.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/integration-notes.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/review-ready.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/source-fingerprints.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/validate-documents.cjs
- doc/tasks/20261014-dcc-gap-other-custom-matrix/backend-minimal-signatures.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/run-private-junit.ps1
- doc/tasks/20261014-dcc-gap-other-custom-matrix/PrivateJUnitRunner.java
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/red-result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/red-exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/red-sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/green-result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/green-exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/green-sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/regression-1-result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/regression-1-exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/regression-1-sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/a10-projection-dependency.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/a10-migration-policy.json
- doc/tasks/20261014-dcc-gap-other-custom-matrix/run-category-junit.ps1
- doc/tasks/20261014-dcc-gap-other-custom-matrix/CategoryJUnitRunner.java
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-test-support.py
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-1/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-1/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-1/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-behavior/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-behavior/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-behavior/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-1/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-1/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-1/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-projection/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-projection/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-projection/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-projection/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-projection/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-projection/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-leaf/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-leaf/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-leaf/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-leaf/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-leaf/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-leaf/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/regression-controller-2/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/regression-controller-2/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/regression-controller-2/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/application-integration-hunks.md
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-r03-row/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-r03-row/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/red-r03-row/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-r03-row/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-r03-row/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/green-r03-row/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/regression-r03-row/result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/regression-r03-row/exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/category-junit/regression-r03-row/sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/red-preview-result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/red-preview-exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/red-preview-sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/green-preview-result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/green-preview-exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/green-preview-sources.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/regression-preview-result.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/regression-preview-exit.txt
- doc/tasks/20261014-dcc-gap-other-custom-matrix/private-junit/regression-preview-sources.txt

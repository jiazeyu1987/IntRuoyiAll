# G48 / LD03 — 上传只选择项目文件夹的有限设计

状态：`prepared_for_root_design_review`。本记录只读当前 `int_qms` 源码并提出实现方向；未改生产代码、生成或执行 SQL、运行测试、访问 DB/API/浏览器、启动服务或执行 Git 写操作。LD02 后端唯一类型映射问题已在 G47 独立报告的最终闭合段关闭；本方案不重做该修复。

## 推荐结果

前端操作仍是：进入上传页 → 选择项目 → 选择项目存储文件夹 → 选择文件类型 → 上传真实文件 → 填写属性、版本、生效日期等 → 确认送审。上传人不再选择第二套分类/NAS 提交目录。

后端保存一条正式的“当前租户＋项目文件夹＋文件类别 → 存储目录”映射。首次提交时，使用该类别**已经配置的唯一根目录 ID**，在其下面创建一个数据库叶子目录，并与映射、文件版本及项目位置一起提交。以后同一键直接核对和使用这条映射。目录名称仅用于展示，不能用来寻找或接管已有目录。

这里的“创建目录”是 `dcc_file_directory` 数据库记录。现有 `DccDirectoryAdminServiceImpl.createDirectory:152–159` 只写该记录，不证明远程 NAS 已执行 mkdir、权限或 ACL 已配置。本批不新增远程 NAS 创建或冒充其成功，也不改变已配置的原文件对象存储、预览票据或源文件正文。

## 当前源码事实

Java 路径以下均相对 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`。

| 位置 | 已存在的事实 | 对本批的约束 |
| --- | --- | --- |
| `dal/dataobject/projectcode/DccProjectFolderDO` | 项目逻辑目录含项目、父目录、名称、模板来源；没有存储目录字段 | 不能把逻辑 ID 冒充 `directoryId` |
| `service/file/DccPublicUploadPlacementService.create` | 已在同一物理事务先锁项目、再锁准确项目文件夹，然后创建文件并登记位置 | 是内部映射解析的接入点；在 Master/File 锁前解析 |
| `service/projectcode/folder/DccProjectFilePlacementService.bind` | 校验实际 File.directoryId、项目、租户；位置表保存每个 File 的 `storageDirectoryId`，精确重放，真实 Gxp | 继续固化每个版本的位置，不将当前映射覆盖历史事实 |
| `service/file/DccPublicUploadPlacementService.lockSourceLocation / inherit` | 升版/检入仅继承源版本实际已登记的位置；历史未登记明确保留缺失 | 不根据新映射回填老版本、不迁移旧目录 |
| `service/file/DccControlledFileWorkflowServiceImpl.prepareSubmitContext:2292–2295 / validateSelectedDirectory:2135` | 当前另取类别绑定目录和客户端 directoryId；要求选中配置子树里的叶子 | 新建受控文件将改为验证服务端解析的叶子 ID |
| `dal/mysql/category/DccCategoryDirectoryBindingMapper.selectActiveByCategoryId:15–17` | 当前采用 `selectFirstOne`；表唯一键仅是 `(category_id,directory_id)` | 新映射初始化必须读取准确有效集合并要求唯一，不能取第一条 |
| `service/file/DccSourceUploadSession.newUploadPrefix:17–19` | 票据绑定项目、类型、实际文件名及会话；不包含项目文件夹/存储目录 | 票据不证明存储位置，位置由提交事务重新核验 |
| `service/projectcode/folder/DccProjectFolderMaintenanceService.delete:33–50` | 删除拒绝子目录、已有位置与引用 | 增加正式映射占用保护 |
| `service/directory/DccDirectoryAdminServiceImpl.deleteDirectorySubtree:269` | 管理删除会收集并删除目录中的业务图及 infra 文件 | 必须先保护被映射引用的目录/祖先子树，避免生成新位置后仍可被级联删除 |

`DccFileDirectoryDO` 和类别目录绑定 DO 当前继承 `BaseDO`，没有显式租户字段；原 schema 的 `tenant_id` 默认值是 0。新派生目录写入必须显式使用真实当前租户，不能依赖默认 0、扩大查询成 `tenant_id=0 OR 当前租户` 或顺便修复旧数据。原 schema 目录唯一键 `(parent_id,code)` 不含租户，生成 code 须使用稳定身份且满足 64 字符上限，不用用户目录名称匹配。

## 三种选择

| 选择 | 最小结构/接口 | 评价 |
| --- | --- | --- |
| 项目文件夹加一个 storageDirectoryId | 一列、上传后端取该列 | 同一项目文件夹可存放不同文件类型，其配置根目录可能不同；一个 ID 无法表达类别差异，不推荐 |
| 从位置记录/现有根目录推断 | 无新表；读既有文件的位置或直接取根目录 | 空文件夹无事实，旧位置可能多个，配置根可能非叶子；需要猜测或取第一条，不能采用 |
| **独立正式映射表** | **一张表＋内部服务；沿用上传 API** | **按租户/文件夹/类别表达准确来源，可创建真实 DB 叶子，可保护历史并发；推荐** |

## 最小正式映射合同

建议名称 `dcc_project_folder_storage_mapping`。最小字段：id、tenant_id、project_code_id、project_folder_id、category_id、base_directory_id、storage_directory_id，以及标准真实创建/更新时间和操作者字段。有效映射采用现有软删除规范；映射不提供上传客户端任意 CRUD。

- 唯一键 `(tenant_id,project_folder_id,category_id)`；projectCodeId 必须与文件夹正式归属一致。`storage_directory_id` 由服务端创建且只属于一条映射，使用明确唯一约束防跨项目接管。
- `base_directory_id` 保存首次解析的准确已配置根 ID，`storage_directory_id` 保存新叶子 ID；不能只保存字符串路径。新叶子直接位于该根之下，不依次建立项目/逻辑目录层级导致当前可提交节点变成非叶子。
- 叶子 code 使用租户、项目文件夹、类别稳定 ID 的受限格式；展示 name 可表达项目/文件夹。查找只使用映射 ID，不能把碰巧同名/同 code 的旧目录当作赢家。
- 已存在映射须核对租户、项目归属、类别、根、叶子 enabled/deleted/parent 与来源事实。配置根后来改变时，拒绝冲突并由配置维护人员明确处置，不自动换目录、选择新第一叶子或改历史位置；上传人不新增配置操作。
- 逻辑目录改名或移动不隐式更换存储目录 ID。复制模板只创建逻辑目录，不复制其他项目的映射。引用只写现有引用关系，既不建立新源存储映射，也不移动被引用文件。

该表的正式迁移是前向结构变更；不包含历史目录、文件、位置、名称、版本或签名数据改写。实际 schema 执行由 Root 单独准备具体清单与授权，不包含在此设计任务中。

## 提交事务和权限

1. 现有上传入口仍使用真实当前用户，校验当前租户、启用项目、项目 Editor/Owner、准确逻辑文件夹、唯一有效文件类型类别、类别 UPLOAD 权限以及真实源票据。新建不要求上传人取得目录管理角色。
2. `DccPublicUploadPlacementService.create` 在现有 `project → folder` 锁后调用内部映射服务。新增服务必须是同一数据源、`MANDATORY` 物理事务；既有映射锁定回读，新映射校验准确类别根配置。缺配置、多条有效配置、foreign tenant、停用根明确阻止提交。
3. 在准确根目录下面插入显式当前租户的 DB 叶子，再插入映射。目录 INSERT 与映射 INSERT 是本事务实际写入；不调用公共目录创建 API，不借此授予任意目录管理权限。并发相同键要锁定真实父身份并受唯一约束保护，回读仅接受已提交且所有事实相同的赢家；不得吞掉后续 File/placement/Gxp 的失败。
4. 把映射服务返回的目录 ID作为内部已验证结果交给 Workflow；新建受控文件不能用 caller 的 directoryId 覆盖它。Workflow 仍检查实际根子树和叶子，File.directoryId 与新 placement.storageDirectoryId 必须相同。
5. 文件创建、映射、派生目录、placement 和正式 Gxp 同一事务完成。文件/票据/Gxp 后续校验失败，应回滚本次数据库新增目录与映射；临时上传对象本来就已存在，不能将其误称为数据库事务自动回滚的正文。现有票据清理机制保持。
6. 在已有 `dcc.project-file-placement.bind` 的正式审计证据中表达实际 mappingId/baseDirectoryId/storageDirectoryId 和本次是否新建，复用当前真实身份与原因；必要的审计载荷变更由实现者核对正式策略合同，不新增批准节点或虚构签名。

锁顺序目标是 `project → folder → category/configured base → mapping → Master/File`。这是本方案要求，**不是已经实现的并发证明**。实现前须核对类别绑定修改、目录停用/改父/删除的实际写入口，使其对同一类别或根 ID 使用共用锁并在锁后重新检查映射占用。单在上传端锁一条根目录不能声称已保护现有管理删除；根删除/子树删除须先拒绝包含映射叶子或其配置根的目标集合。不要扩成全 NAS 管理平台。

`DccDirectoryAccessPermissionServiceImpl` 的查询/预览/下载规则是数据库权限，不是远程 ACL。派生叶子不能通过自动赋予所有用户权限来使页面通过。沿用现有项目/类别/文件的真实访问授权，实际详情读取路径若消费目录规则，必须与既有正式授权合同核对后闭环；不能静默放宽或声称权限已由 mkdir 证明。

## 前端与 API 的有限改动

- 正常 NEW 上传和创建 WORKING 的前端移除第二个存储目录选择、必填提示和首叶子自动选择；保留项目文件夹、真实类型/文件、LD01 产品投影及全部现有提交字段。
- 现有 `DccControlledFileSubmitReqVO.directoryId` 是全局必填，需要改成按真实分支校验：正常受控 NEW 由内部映射提供，客户端不允许提供任意替代目录；已有 EXTERNAL_REVIEW/历史显式目录分支继续原合同。不能做“未传自动生成、传了继续信任”的双轨 fallback。
- 优先内部 context 参数或正式派生结果承载目录 ID，不能把客户端 request field 当已授权证明。不得为了隐藏选择器而任意塞 rootId/逻辑 folderId。
- 预览只读校验配置是否可用，不建立映射或叶子。真实上传 preview 继续产生票据；不改变真实 File.name、actor/session/purpose/body 校验，也不从前端制造 sourceFileId。
- 继续沿用现有 submit/create-working API；不新增公开“传任意文件 ID 绑定映射”写接口。提交成功详情加载仍依赖真实文件授权，导航 from/returnTo 不是权限凭据。

## 后续实现的有限 BDD 与验证

以下是计划，不是本任务已运行的测试或实际成功。

| Given / When | Then |
| --- | --- |
| 启用项目和真实逻辑文件夹，唯一类别配置根为非叶子；首次 NEW 提交 | 无客户端目录选择；准确根下建立一个 DB 叶子和映射；File/placement 同 ID，真实审计同事务 |
| 同一租户/文件夹/类别已映射；另一个文件提交或同键并发 | 精确复用唯一映射，不新增第二个叶子；冲突载荷不接管已有目录 |
| 同一逻辑文件夹选择另一个类别 | 使用该类别的正式根，独立映射，不借用第一类目录 |
| 配置缺失/重复/foreign tenant，或最终创建/审计失败 | 明确错误；本次无 File/映射/目录数据库残留，不默认成功 |
| 旧版本原目录与现映射不同；检入/正式升版 | 沿用源版本明确位置，不回填历史，不改正文/签名 |
| 删除映射文件夹或配置根/包含叶子的子树 | 同锁后拒绝；引用取消不影响源映射和文件位置 |
| 真实页面选择项目文件夹并完成首次上传 | 全流程不出现额外 NAS 选择；详情及项目文件列表能读取实际文件。DB/API只能由 Root 只读核对身份、映射和历史，不替代 UI 动作 |

## 当前读取指纹和边界

读取日期 2026-10-04。下列为当前源码原始 SHA-256，不是新实现的交付清单：

| 文件 | SHA-256 |
| --- | --- |
| DccPublicUploadPlacementService.java | 63649419bc7beffed8dad08c7235418cc0e8e577acbc8d0aaa350ec5a3bdfbb1 |
| DccProjectFilePlacementService.java | 6bdc47dbc359561344da2bd846fbff5e0cfcae699b83ebb552e76bf955ced3b2 |
| DccProjectFolderMaintenanceService.java | 1f1a17bf4ed2513ef417615785bf369b316175401157ed85ec2c90187bcc2ce5 |
| DccDirectoryAdminServiceImpl.java | 665b5847f8d9bc51b28c265b44ad80104f4140398d9ec55318e2bd381e307009 |
| DccCategoryDirectoryBindingMapper.java | 8a6afb503083ed31aecb1f2972f21dd78495dc3420ffdc3fe8778d31ba196439 |
| DccDirectoryAccessPermissionServiceImpl.java | 98f834bd465d37377814b35a38c41d5fcb747fa506de95a3bf6f5b3669c32bdf |

Root 先确认这一本地正式映射方向，再分配有限 schema/后端/前端实现及真实验收；当前报告不证明实际 NAS 配置、生产权限、数据库迁移或 E2E 已通过。

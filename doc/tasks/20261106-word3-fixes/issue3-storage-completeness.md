# 新类型保存后无法受控上传：目录完整性修复

## 根因核验

实际失败链：受控上传页面 → `DccControlledFileHistoricalImportService.create` → `DccPublicUploadPlacementService.create` → `DccProjectFolderStorageService.resolve`。

任务环境日志18:42:23的参数为 project347/folder93/category908776/type205。只读JDBC核验：folder93确属启用的project347、本租户1、未删除且启用；category908776本租户1且启用，但 `dcc_category_directory_binding` 中该类别为0行。`storage.resolve` 要求类别恰好一条有效存储基础目录绑定，因此拒绝并报通用FOLDER_INVALID，参数本身没有错。

`DccFileTypeConfigurationService.create` 原先只构造类别与矩阵，没有传递目录绑定；`CategoryAdminService.createCategory` 只有收到明确directoryBinding才绑定。新增类型表单也没有目录输入，导致保存成功后无法用于受控上传。

## 最小修复

复用既有CategoryForm的目录树及directoryBinding结构，在新增可用类型时明确选择“存储目录”。PUBLIC_MATRIX和APPLICATION_CUSTOM均要求选择；仅分类路径节点不要求。后端在创建类型前锁定并校验所选目录属于本租户、未删除且启用，再将绑定传给现有类别创建事务，类型、类别、绑定、矩阵统一提交或回滚。没有默认推断目录，也没有修改底层storage拒绝规则。

生产文件仅4个：

- 后端 `DccFileTypeTaxonomySaveReqVO.java`
- 后端 `DccFileTypeConfigurationService.java`
- 前端 `api/dcc/controlledFile/fileTypeTaxonomies.ts`
- 前端 `views/dcc/controlled-file/basic-data/file-type-taxonomy/index.vue`

## 验证

`DccFileTypeConfigurationServiceTest` 11项和 `DccFileTypeConfigurationTransactionTest` 8项，共19/19 PASS，日志 `C:/Users/D01020/AppData/Local/Temp/word1106-review/agent1-storage-maven.log`。

覆盖缺目录、停用/跨租户目录在创建类型前拒绝；公用矩阵和“其他”自定义类型保存明确目录；pathNodeOnly不生成类别或绑定；绑定写入失败和矩阵部分写入失败时无type/category/binding/route/node残留。

前端全量 `vue-tsc --noEmit -p tsconfig.relaxed.json` 使用仓库标准8GB堆检查通过，最终SFC script/template编译通过，修改范围diff检查通过。首次使用Node默认4GB堆的检查因内存上限中断，调整堆后成功。未添加新依赖、权限或数据库迁移。

真实UI验收已在8063→48063、release20-58-31完成，全部动作由Playwright驱动真实页面，无API/DB业务写入：

- 新增类型未选择存储目录时，保存被阻止并明确提示“请选择启用的存储目录”。
- 在任务自有`WORD1106矩阵验收/验证路径`下新增`WORD1106-DIR-COMPLETE`（type209），明确选择“质量管理”目录和`WORD1106新建矩阵类型 · V3`（源category908776）。一次保存成功，之后没有编辑类别或补绑目录。
- 在类别权限页面仅对该新类别授予admin EDIT，然后在受控文件上传页面选择任务项目SOURCE347、F1文件夹93、新类型、A/1，导入`WORD1106-DIR-COMPLETE-ACTIVE.pdf`成功。
- 文件ID2054545668044084583，详情显示`ACTIVE / A/1`、任务新类型完整路径、最终目录`质量管理/项目#347 文件夹#93`。真实正文页面成功打开，PDF正文搜索marker `WORD1106-REFERENCE-BODY`命中1/1，截图已人工视觉核验。

证据目录`output/word1106-e2e/issue3-storage`：01必选提示、05明确目录和矩阵、06一次保存成功、07类别授权、08首次导入表单、09导入成功、10生效详情、11真实正文；`new-type-directory-first-import-trace.zip`为完整Playwright轨迹。可复用脚本`e2e/issue3-storage-completeness.cjs`已语法检查，实际本轮由`issue1-session.cjs`交互驱动同样页面步骤并保存轨迹；没有为重复脚本再造一遍数据。

验收中首次尝试沿既有`通用文件/其他类型`创建时，被原生命周期阶段规则拒绝（1080000147，其他类型非法），未报告该尝试为通过；随后改用本任务已明确可用的分类路径完成上述验收。该旧路径约束没有放宽。本轮UI覆盖PUBLIC_MATRIX；APPLICATION_CUSTOM的显式目录和事务原子性由上述后端测试覆盖。原category908776事先通过正式类别编辑页面绑定目录仅解锁旧数据，不计入此次新增类型完整性验收。

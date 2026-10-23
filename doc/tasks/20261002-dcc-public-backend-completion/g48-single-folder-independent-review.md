# G48 / LD03 单项目文件夹独立源码复核

状态：`in_progress_snapshot_only`。本复核只读生产源码；仅本报告可写。不修改源/测试，不运行 Maven/构建/测试/DB/API/浏览器/服务/Git 写操作。未冻结实现不按最终交付判定，不把已有测试或源码存在当真实运行证明。

## 首次有限读取

2026-10-04，当前前端开始接线，后端正式 mapping/context 源码尚未出现；该状态是实施中的先后顺序，**不是新增 P1 或宣称后端已完成**。现 `DccPublicUploadPlacementService` 仍为原 source SHA `63649419bc7beffed8dad08c7235418cc0e8e577acbc8d0aaa350ec5a3bdfbb1`，SubmitReqVO 仍有全局 directoryId `@NotNull`，SHA `7aef9c2b7afee6b191bffd5ce9de006a4c59e56125fec3d7d86505b742ef30bd`。Root/BE owner 必须在组合运行前交付服务端派生上下文和分支校验；不在此刻把 NEW 省略目录的 FE 接到旧后端并冒称通过。

当前 FE 快照方向符合有限设计：

- `upload/index.vue` 的目录控件、必填、目录读取和 submit preflight仅在 `!isNormalNewUpload` 分支保留；正常 NEW 明确只展示真实 logical folder，并说明实际存储位置在服务端提交时确定。
- `submitter.ts.buildSubmitPayload` 仅 `CONTROLLED_FILE + NEW` 省略 directoryId；EXTERNAL_REVIEW/显式旧分支保留实际字段，没有把 folderId/rootId/null 当替代目录。
- `workflow.ts.assertControlledFileSubmitRequest` 对正常 NEW 明确拒绝 caller拥有 directoryId 属性；没有用可选字段建立客户端覆盖目录的双轨 fallback。
- 正式类型唯一类别、raw File.name/SOURCE ticket、LD01 产品、属性/部门/二次确认边界保持既有入口。本次快照读取未发现可明确报告的新增主线 P1。

当次 FE raw SHA：index.vue `f95660aa266c4c603576981e7027124842777555eb1576aaa34dfa601f616532`；submitter.ts `0182a9f42bfb813a5da9b96169c23d195f8430983ebd505c9a241eb6380c267f`；workflow.ts `ae364b0b7780d6a2b575587ddc4d5881df91a901f454b1dbef8d3ee05719555e`。这些并发读取指纹只标识快照，不是最终交付 seal。

## 后端出现后的有限复核范围

1. 公共 NEW/WORKING 创建入口先真实 project/folder 锁，再解析正式 tenant/folder/category mapping，把不可由 caller覆盖的内部结果交给 Workflow；EXTERNAL 与旧历史继承不进入 NEW mapping。
2. 类别已配置根准确唯一、真实当前租户显式写入，不取 first binding/first leaf、不根据名称接管旧目录、不改 tenant=0 历史。
3. 派生 DB 叶子/mapping/File/placement/Gxp 同物理事务；按项目/文件夹/类别/根的共用锁保证重复/并发，无失败残留成功。票据正文清理和数据库事务边界分别准确描述。
4. 删除 logical folder、被映射叶子或包含它的根/子树时，管理路径在共用锁后检查正式占用；现有通用级联删除不能绕过 mapping。停用/移动/配置切换按实际共享锁合同核对，不推断已完成远程 NAS/ACL。

不对底层文件逐项无限轮询。等 Owner/Root 通知源码或最终指纹出现后有限复核，并将具体主线发现和闭合依据追加；目前没有最终源码审查结论，也没有本 reviewer 的测试或运行 PASS。

## 2026-10-05 初版后端出现后的四范围快照

Root 通知新 mapping/context 源已出现，Owner 正独占必要 GREEN/Maven；本次一次有限读取，未等待也未替代最终交付。**当前已实现的新建上传接线未见新的直接主线 P1**。管理占用与权限投影仍由 Owner 实施中；其尚未出现在本次读取源，不标成“最终修复遗漏”，也不提前关闭其验收范围。

### 1. 内部派生结果与动作隔离

`DccDerivedUploadStorage` 是 final 类、字段 final、构造器 package-private；无 REST 请求字段或公开 client factory。本次生产调用只有 `DccProjectFolderStorageService.resolve` 创建该对象。

`DccControlledFileController.submitControlledFile / createWorkingControlledFile` 在现真实权限和事务中改为 Function callback，把 PublicPlacement 获得的内部 storage传入 Workflow专用 `submitNewWithDerivedStorage / createWorkingWithDerivedStorage`。PublicPlacement明确拒绝 directoryId（包括显式 null 经 setter记录provided）、非 NEW、非法 processType；先取得真实project/folder并锁定后才解析。Workflow重新核 tenant/actor/project/folder/category，并保持 ordinaryNEW、真实type/category/product/ticket/body/name/version守卫。`prepareSubmitContext` 将内部 base/leaf作为实际校验值交给原 subtree/leaf校验及 Master/File创建，没有把 logicalfolder ID 或 client值当 storage。

EXTERNAL_REVIEW 仍经 `DccExternalFileReviewController.submitExternalReview` 和原 external service，不进入派生映射；原普通Controlled入口不会因此变成external混用接口。PublicPlacement的external早期分支仍不登记位置。`lockSourceLocation / inherit` 原历史事实读法保持，旧位置缺失不从 mapping推导。原内部非derivedWorkflow分支仍须真实原directory，因此不是全局取消目录资格。

### 2. 准确租户与唯一配置

`DccProjectFolderStorageService.resolve` 是 MANDATORY，拒 tenantIgnore、无效actor/启用状态/租户/项目folder归属/类型分类不一致与缺categoryUPLOAD。Mapper按真实tenant/category锁active category，再按准确tenant读全部active未删配置根，必须 size==1；不是原binding selectFirstOne。根与leaf使用显式tenant FORUPDATE读取。新leaf明确 `.tenantId(tenant)`，code只使用稳定tenant/folder/categoryID且不以展示name接管旧目录；mapping保存实际base/leafID。旧mapping严格核tenant/project/folder/category/base、未删，leaf启用、actualparent和无activechildren。无 firstleaf、tenant=0 OR、旧数据改写或远程NAS mkdir假证明。

### 3. 同事务与共享顺序

Controller实际事务 → PublicPlacement MANDATORY → project/folder锁 → storage MANDATORY/category/config-root/mapping/leaf锁 → Workflow事务/Master/File → placement MANDATORY/Gxp。新directory与mapping在该物理事务内真实insert，后续失败传播。`bindDerived`在原位置事实审计增加mapping/base/leaf/created字段，actualFile.directoryId与context必须一致。票据/原件对象在上传阶段已存在，不能宣称这些外部正文会被数据库事务自动撤销。

source锁顺序已可确认；与category/目录管理写入口的**共同锁和删除保护还需最终源复核**。本读不运行实际并发/rollback测试，不把注解存在当其数据库验证 PASS。

### 4. 占用与正式访问权限未提前闭合

本次 `DccDirectoryAdminServiceImpl` / `DccProjectFolderMaintenanceService` 尚无 mapping占用守卫，DirectoryAccessPermission仍只有旧直接目录规则投影。Root此前已通知Owner这两项正在补，故不重报旧源码P1或要求平台扩张。

最终需读准确mapping派生leaf动态投影其base正式权益、当前撤权同步，而非任意ancestor继承/复制全员权限；Query目录资格是精确leafID集合，不能只因storage创建成功就声称download可达。管理删除/停用/移动路径须共同锁后核mapping占用，避免Generic subtree删除File图越过派生事实。缺这两部分最终证据前，本报告只关闭上述新建context/uniqueconfiguration的当前快照读取，不宣布全LD03通过。

### 当次原始指纹

| 文件 | SHA-256 |
| --- | --- |
| DccProjectFolderStorageService.java | ecb14b90fe6512c6d85f9fbada5bf21f04a97524152fa463d755082f03a961d1 |
| DccDerivedUploadStorage.java | 5fd161de02ee5124d5bcd067e1d54453f743d8fc9f55fcfa7c1597a10141e351 |
| DccProjectFolderStorageMappingMapper.java | 58abec35f3f4c87cd80742544dfe47ec3cfa99707ade937eba488bf43a919b05 |
| DccPublicUploadPlacementService.java | 44d0eb527994c36506af6c1e30ae9ef24f121d4292f374c480c55f2555e4a947 |
| DccControlledFileWorkflowServiceImpl.java | 2aac80ea24c676af475924a97f364d2c004732e242f98093d4f3169c046471bf |
| DccProjectFilePlacementService.java | 27e139a8cb6195103dd693eca5ba661fa390de49ce5e7fa1314e3e77cff94c00 |

以上原始指纹是并发开发快照；Owner formatter/必要后续guard将产生新hash，最终以Root通知的正式manifest为准。未运行测试、实际API/DB/runtime，未对当前Owner测试结果自行作PASS声明。

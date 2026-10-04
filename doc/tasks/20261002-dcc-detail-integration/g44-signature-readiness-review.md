# G44 签名图片就绪性有限只读审查

Status: ready_for_closeout — 只读代码审查交 Root；无生产修改、Maven/测试、浏览器、数据库/Redis/Token/服务/Git操作。

本轮 Root 已真实 UI 核新项目270、负责人OWNER、文件夹/模板、技术调研报告类型与正式路线；剩余两条提示“审批人未配置有效签名图片”。本Agent没有读取实际业务库，不把两条提示推断为两个不同用户，也不替人员生成签名或取消检查。

## 结论

主线已有正式签名图片配置入口与服务。当前两条阻塞对应具体候选人/节点，可由真实 readonly readiness 结果里的 userId/stageCode 确定；不同节点可能重复同一人。图片阻塞可来自没有ACTIVE记录、infra_file缺失、正文对象缺失或哈希不符，不能仅凭通用提示判定“从未上传”。Root按实际personID核实后由本人在真实前端配置其真实图片。

一个确定P2显示缺口：`IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue:412-413` 仅渲染 blocker.message。后端 `DccControlledFileRouteReadinessService#blocker:214-225` 已提供 stageNo/stageCode/stageName/userId/userName，VO完整。相同图片错误在列表出现两次，无法告知哪个人/哪个环节需要处理。最小修复为同一列表显示服务器返回stageName+userName+message（userId仅用于稳定定位，不用猜候选）。本轮没有授权生产修改；不是电子签名门禁缺陷。

## 人员来源与岗位区别

- `DccControlledFileApprovalRouteAssigneeResolver#resolveSelectedDepartmentNode:153-158`：当前选择的会签部门只替换 MATRIX_REVIEW(DEPT)候选，`resolveDepartmentLeaders:260-282` 从正式 `system_dept.leader_user_id` 逐部门取负责人，验证部门/用户有效。不是拿该部门所有岗位用户替代负责人。
- 后续节点保留正式该action路线配置。`resolveApprovers:225-255` USER直接正式用户；DEPT负责人；POSITION→active position assignment的userId或systemPostId→正式系统岗位用户。不能将任意后台岗位/项目OWNER/admin默认当作会签候选。
- `DccControlledFileRouteReadinessService#evaluateResolvedRoute:96-130` 汇总节点用户，`AdminUserRespDTO.postIds` 为空是独立APPROVER_POST_MISSING阻塞；岗位不为空仅就绪条件，实际候选来自前述路线。
- MATRIX_REVIEW/DOC_CONTROL_REVIEW需dcc:controlled-file:review；MATRIX_APPROVAL/DOC_CONTROL_APPROVAL需approve（:203-212）；授权和有效图片各自独立检查。Root当前只报IMAGE_INVALID，不能据此对岗位/授权做新配置。

## 图片权威表、字段和正文校验

正式表 `dcc_electronic_signature_image`（DO:16；`20260706_dcc_signature_image_evidence_chain.sql:4-36`）。关键identity/metadata：id、tenant_id、user_id、version_no、file_id、storage_path、content_type、file_size、sha256、image_status、active、deleted、uploaded_by/at、enabled_at/disabled_at。

`DccElectronicSignatureImageMapper#selectActiveByUserId:15-19` 取当前tenant用户 active=true且image_status=ACTIVE的未删除行（tenant/逻辑删除由正式MyBatis拦截）。上传仅生成UPLOADED/activefalse；启用把旧ACTIVE改SUPERSEDED/false并将当前用户所属图片改ACTIVE/true。

`DccElectronicSignatureImageServiceImpl#requireActiveSnapshot:147-153` 读取ACTIVE行后调用 `verifyStoredImage:282-301`：

1. 从真实infra_file按file_id取config_id和path；二者必须存在。
2. `FileService.getFileContent(configId,path)`读取真实存储正文，必须非空。
3. 计算实际SHA256与图片表sha256 case-insensitive完全一致，返回snapshot VALID。

没有独立持久化verified_status字段，VALID是本次真实读取校验结果。正文对象存在不能仅靠DB字段证明，GET需要Root按既有只读端口核验，不直接输出对象路径/凭据。

`DccControlledFileRouteReadinessService#resolveImageValidity:160-174` 对每个唯一候选调用requireActiveSnapshot，需imageId/fileId非空+VALID；签名业务ServiceException或S3 NoSuchKey归并IMAGE_INVALID。其他不可识别错误不被默认成功。

上传校验：`validateUploadFile:224-229`/`validateImageContent:231-246`限制0<size<=2MiB，PNG/JPEG内容类型或扩展名、ImageIO实际可解码且长宽>0。图片保存目录dcc/signature-images；本轮不生成模拟签名。启用有非空reason且图片user_id等于当前用户，启用再次读正文+SHA。

## 已接通正式前端

`/signature-governance/my-signature`（remaining.ts:525-535，固定路由permission元数据signature-governance:policy:query；Root需实际确认该用户菜单/路由能打开，不在本轮假定权限）。index.vue:13只在my-signature页展示本人组件，组件无额外签名管理员要求。

本人操作：进入我的签名→填写变更原因→上传真实PNG/JPEG→前端自动启用→返回显示已启用/版本/时间。

- `SignatureGovernanceMySignaturePane.vue#handleSignatureImageFileChange:190-214` 调用uploadDccElectronicSignatureImage，再enableDccElectronicSignatureImage；两个正式请求均成功才显示“已上传并启用”。失败显示真实错误；不模拟完成。
- `signatures.ts:233-261` GET `/dcc/electronic-signature-authorizations/my-image`；POST `/my-image/upload` (Multipartfile+reason)；POST `/my-image/{imageId}/enable`。
- `DccElectronicSignatureAuthorizationController:69-102` 所有my-image端点只用getLoginUserId，不接目标userId，管理员不能凭同一路径帮另一个账号配置。普通认证仍由全局security处理。电子签名授权管理另一端点另要求signature:manage+electronic_signature_admin，与本人图片不是一回事。
- `DccElectronicSignatureManagementServiceImpl:285-303` 调正式ImageService；ImageService enable再核所属用户及真实正文。

可行当前方案：Root从readiness原始userId去重找出缺图人员；确认真实用户/本人真实签名图片；按已授权真实UI账号完成本人图片配置再刷新原上传预检。不允许用任意图片、借admin图片、换人、删守卫或DB填ACTIVE来代替。缺少真实资料或账号时，需明确该实际依赖；本轮不新增审批/保留平台。

## Root 可用只读查询模板（本Agent未执行）

参数必须从真实路由结果取，不能用猜的两个用户ID。全在Root已核sourceDB/tenant1执行。只返回人员和状态，不查密码/token/配置密钥。

```sql
-- ? = current selected department IDs from actual UI readiness input
SELECT d.id, d.name, d.status, d.leader_user_id,
       u.id AS user_id, u.username, u.nickname, u.status AS user_status, u.post_ids
FROM system_dept d
LEFT JOIN system_users u ON u.id=d.leader_user_id AND u.tenant_id=d.tenant_id AND u.deleted=b'0'
WHERE d.tenant_id=1 AND d.deleted=b'0' AND d.id IN (?, ?);

-- ? = exact blocker userId set, do not assume 2 distinct people
SELECT i.id, i.user_id, i.version_no, i.file_id, i.image_status, i.active, i.deleted,
       i.content_type, i.file_size, i.sha256, i.uploaded_by, i.uploaded_at, i.enabled_at, i.disabled_at
FROM dcc_electronic_signature_image i
WHERE i.tenant_id=1 AND i.user_id IN (?, ?) ORDER BY i.user_id,i.version_no,i.id;

-- Select metadata only; config secrets/body path remain protected separately.
SELECT i.id AS image_id, i.user_id, i.file_id, f.config_id, f.size, f.type,
       CASE WHEN f.id IS NULL THEN 0 ELSE 1 END AS metadata_exists,
       CASE WHEN f.path IS NULL OR f.path='' THEN 0 ELSE 1 END AS path_present
FROM dcc_electronic_signature_image i LEFT JOIN infra_file f ON f.id=i.file_id
WHERE i.tenant_id=1 AND i.deleted=b'0' AND i.active=1 AND i.image_status='ACTIVE' AND i.user_id IN (?, ?);
```

ACTIVE记录存在且metadata有值仍不能判VALID；Root需安全只读真实GET并SHA比较，或由真实页面预检后端正式requireActiveSnapshot确认。上述query无任何write，无自动调用。

## 既有验证与证据边界

已有 `DccControlledFileRouteReadinessServiceTest#evaluate_aggregatesPostPermissionAuthorizationAndImageBlockers:45`、`evaluate_treatsMissingSignatureObjectAsImageBlockerInsteadOfServerError:83`；`DccElectronicSignatureImageServiceImplTest#uploadMySignatureImage_rejectsUndecodableImageContent:72`等现有测试。这里只读这些源码，没有新跑测试、没有复现图片/文件存储PASS，更不把mock正文当实际就绪。

以下raw SHA记录本次读时源码，可由Root对变化重新核结论。本轮唯一写入此报告与任务收尾Keep条目，不更改任何生产或测试。

## 读时指纹

- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRouteReadinessService.java · 14914 bytes · SHA256 `59a50cbc9cecabc206847d5dfd9997f1e6604dbdf5c6075b3f986d119b6b8ef1`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileApprovalRouteAssigneeResolver.java · 18372 bytes · SHA256 `d2d4a8d0dc2126cd135d1131cf97dac17f7a219e7c6db09980ae9548cc27bc5c`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccElectronicSignatureImageServiceImpl.java · 17351 bytes · SHA256 `a615eb2ec60269d0b860816c830a10e49ee94e2079828b814eadff561c4cbb7f`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccElectronicSignatureImageMapper.java · 2613 bytes · SHA256 `99e78a3a7cd5e97dfeadc5660092a6f4dc45c296cc2295b592be3e4b32ecab34`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccElectronicSignatureImageDO.java · 1375 bytes · SHA256 `05198ee348593aec8ca12fa58eb7ba904feb577382ed3183d07007bc61513629`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRouteReadinessBlockerRespVO.java · 404 bytes · SHA256 `d5d4a2cf4c416ebaa18a391c9587f99635b01f4a4d162068e2ef65beae2340a8`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/signature/DccElectronicSignatureAuthorizationController.java · 6376 bytes · SHA256 `d50f8b3d7ed447f4d49f2f25f2ace2143afb90a467624f99dd552acfd095b74e`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccElectronicSignatureManagementServiceImpl.java · 100118 bytes · SHA256 `1d7f048c1c20651bce7a5e7fb8e1636e862ba5eb292ab0c49fcd4fed99bdf73e`
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue · 114244 bytes · SHA256 `ac6d4acceced7cbfd070302a89a6cf7cf6cc9d5242e64b6125963a0dc60c7169`
- IntRuoyiFronted/src/views/signature-governance/components/SignatureGovernanceMySignaturePane.vue · 11712 bytes · SHA256 `c2e97b1f10eee0244e61fd1eba9121587182d820aaa74745786568b7e578f4e8`
- IntRuoyiFronted/src/views/signature-governance/index.vue · 5004 bytes · SHA256 `a72a41cdcfc3484acea2113b1209e5977f5c38897abcd713290ec3661e56b06d`
- IntRuoyiFronted/src/router/modules/remaining.ts · 67878 bytes · SHA256 `c6a031abf0836a5aa56471ebfe8f3ffd749da6adb2c3bef9614a6747d944bab4`
- IntRuoyiFronted/src/api/dcc/controlledFile/signatures.ts · 12590 bytes · SHA256 `c4f7d456156f36e0a6344632125ed375ada4848c3ae3022262ac126ecfe61fca`
- IntRuoyiBackend/sql/mysql/20260706_dcc_signature_image_evidence_chain.sql · 4264 bytes · SHA256 `3a9176b8eaf13491739bd445fd1997ba166466484e7d3f8adca6e322ad47ac4b`
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccElectronicSignatureImageServiceImplTest.java · 3940 bytes · SHA256 `a41b7c63ecfc2dc9cfbcb08e733d636e10c367444566e64c00765c6fe8c23f0a`
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRouteReadinessServiceTest.java · 8093 bytes · SHA256 `6d7a38d2368e5f000ace4dfd8203280e3fb3f99099fa680213690bba0619a242`

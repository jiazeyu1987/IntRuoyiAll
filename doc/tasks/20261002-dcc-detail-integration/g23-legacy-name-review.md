# G23 历史 claim 名称前置只读审查

2026-10-03；状态 `read_only_review_ready_business_identity_unconfirmed`。Root完成真实库只读采集，本Agent只读源码/schema/冻结dump和facts，准备SELECT/validator并离线验证；没有连接DB、改生产/正式SQL/历史行、运行服务/Maven/types/build/Git/E2E。本文不授权回填，不把19候选迁移扩成数据修复。

## 真实结论

**25条active claim仍不能整体建立可配置身份。** 完整版本集合比对后，6条为`PROPOSED_VERIFIED_METADATA_MAPPING`（claim **19、20、21、22、23、24**）；19条为`UNCONFIRMED`。19条均涉及不同Master之间的**完整UTF-8原源文件名相同**；其中claim **7、18、28**同时存在同Master不同版本不同原源文件名。不能选最新版本替换其他历史版本或从模板名猜一个统一源名。

最早本工具只交叉单一候选名称，曾给7 proposed/18 unconfirmed；进一步真实facts审查发现claim17与未确认的claim7/28历史观察名相同。已用有效RED→GREEN修复：**所有Master全部版本的观察源名均纳入跨claim冲突，包括已UNCONFIRMED的多名Master**。最终只有6/19，旧7/18不再作为验收结果。

四组重复原名按UTF-8 bytes SHA256脱敏表示：

| 原名bytes SHA256 | 关联claim ID |
| --- | --- |
| e525457264d56a2de1db1355c5989c30456ab41a45afd164a1f0840b2e7b4ce7 | 5, 26, 27, 28, 29, 30, 32 |
| 4c45962cec54099602db78f51836a07c8f6e9c447894913db49a75e3536dc534 | 6, 8, 9, 11, 12, 15 |
| a0ca793d9c01b2a0fbdd19133b9f846be72f72848d73409323e36924b3adeadb | 7, 17, 28 |
| 9256a0e5831dccb6c498c3c3bc9919f96604624761b567377919533393356122 | 7, 10, 13, 14, 18 |

6、8、9、11、12、15另外有`MASTER_FORMAL_NUMBER_IDENTITY_INCOMPLETE`及版本项目/leaf/number不一致提示。即使能确定某个源名，也不等于`claimIdentity`完整项目/leaf/规范编号合同已成立；本工具把这项作为独立`formalIdentityReview`，不以名称提案伪造完整身份。

本次283事实包含25 claim、25 Master、39版本、39源metadata、39 ownership、76 ticket、39 source引用和1 runtime envelope。依据所采事实，未出现缺Master/源ID/metadata/源SHA、tenant错配、ownership/ticket hash冲突；这只是已采集关系一致性，**没有重新读取对象正文**。结果始终`source_bytes_verified=false`、`write_authorized=false`。

脱敏逐claim→Master→版本ID/sourceFileId/原名hash/sourceSHA和理由见同目录`g23-legacy-name-runtime-review-summary.json`；完整名字与源元数据只存在Root保护目录的facts及由Root生成的mapping result，不复制storage路由、URL、ticket/session/token、对象内容或凭据到任务记录。

## 为什么完整新上传E2E会被阻断

- 主任务 `g21-legacy-name-readiness.log`实际tenant1 active claim=25，旧schema没有`source_original_file_name`。正式 `sql/mysql/20260930_dcc_c_revision_identity.sql`只新增nullable事实/精确binary keys，不更新旧行；因此这些旧claim在C迁移后仍是NULL。不能把schema postflight PASS当业务名称前置已就绪。
- 后端 `DccControlledFileNameClaimMapper.java:30`的`countUnresolvedNames`按**tenant+deleted=0+source_original_file_name IS NULL**计数，没有项目或新文件名过滤。`DccControlledFileNameClaimService.java:47`的`preflightNewSourceName`对非零抛出`historical name claims require verified original source names`；`:93`的`claimIdentity`再次拒绝，不能靠换一个不存在的名字避开。
- 公开前端 `src/views/dcc/controlled-file/upload/index.vue:1390`设置`NEW_UPLOAD`，`:2225`上传`SOURCE`；`src/api/dcc/controlledFile/workflow.ts:1756`调用`/dcc/controlled-files/upload-preview`。Controller `DccControlledFileController.java:205`进入`DccControlledFileUploadServiceImpl.java:135`，正式SOURCE preflight在存储新源文件/ticket前发生。
- 所以tenant1从公开页面创建新文件的E2E在**选择实际源文件后上传预览/获取SOURCE ticket**处失败，尚不能进入有效工作版本新建、提交上传申请、会签/批准/培训/文控受控这条全新链路。失败审计仍可能正常记录，本报告不宣称接口零审计写入。现有文件只读浏览/其他已具备数据的独立链路是否可运行需各自验证，不能因本门禁推断全部页面都不可用。

Java源码根为 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`（service/file或dal/mysql/file/controller/admin/file）；前端根为`IntRuoyiFronted`。

## 只读采集与validator合同

`g23-legacy-name-mapping.sql`是一条纯SELECT，derived UNION ALL返回各类逐行JSON及同statement scope counts；所有版本包含历史/deleted标记，不做latest聚合，不`LIMIT 25`。Root运行`--batch --raw --skip-column-names --default-character-set=utf8mb4`采集，一条SELECT保证同一个InnoDB语句读取快照。只引用protected schema里已经存在的字段，不需要本次C新增列先部署。

查询路径：tenant1 active claim→实际`master_id`→该Master全部`dcc_controlled_file`→**该版真实`source_file_id`**→`infra_file.id/name`；ownership实际current/source/origin与tenant/hash、SOURCE ticket的正式bound-file/storage/hash/name/size、跨链source引用用于交叉核验。`original_file_id`仅诊断显示，**从不补齐source_file_id**；`normalized_name`和Master file_name只作为旧事实，不作为原名候选。没有取受控PDF、title、模板或删除扩展名/trim/casefold。

`infra_file`实际schema没有tenant_id/正文SHA，DO `@TenantIgnore`；不能伪造这些字段。租户依据DCC claim/Master/version/ownership/ticket链精确一致及cross references；hash取正式版本`source_sha256`并与已有ownership/ticket比对。`DccControlledFileSourceOwnershipService.createVerifiedCopy/readSource`会读取源并SHA256、复制保留`infra_file.name`，这是哈希字段的正式来源依据，但不证明现在对象内容仍然相同。metadata只输出file/config ID、basename的string+HEX、mime、size、deleted；不采path/url/凭据。

CLI从整合树运行（FACTS/RESULT均由Root指定保护目录）：

```powershell
python -B doc/tasks/20261002-dcc-detail-integration/g23-legacy-name-mapping.py validate --facts FACTS.jsonl --result RESULT.json
```

- exit0：全部`PROPOSED_VERIFIED_METADATA_MAPPING`，仍未授权写入或确认正文bytes。
- exit2：有`UNCONFIRMED`，输出每claim/每版本精确原因、观察源事实，不选择一个名字默认通过。
- exit1：`INVALID_CAPTURE`（错误DB/UUID/version、fresh claim数不等25、缺字段/重复/截断/unsafe ID/查询指纹变等），不得当零冲突或空列表。

所有Long以字符串收发，最多正signed Long；BPM字符串不套Long解析。UTF-8完整basename保持扩展名、大小写、尾空格，JSON值必须与HEX一致。缺Master/任何版本source/hash/metadata、删除状态、跨tenant/sourceownership/ticket/共享源矛盾、多源名、多个claim拥有相同观察源名均明确UNCONFIRMED。没有ownership/ticket时不制造记录，版本自己的sourceSHA必须存在；当前39版已有39ownership可实际交叉验证。

## 已运行证据及原失败保留

BDD先登记；工具缺失RED9→GREEN14。Root首轮CLI准确返回`INVALID_CAPTURE: Prepared SELECT checksum drift`：prepare曾按内存LF算合同SHA，Windows落盘CRLF不同。这不是业务data失败，不改facts不假PASS。新增真实raw bytes校验test有效RED1→GREEN15，prepare改为**写完SELECT后读取实际bytes算SHA**，查询内容/原字节未变、不重采。随后上述未确认多名Master跨claim冲突新增有效RED1→GREEN16。

最终`python -B .../test_g23_legacy_name_mapping.py` **16/16 PASS（0.103s）**，包括unsafe Long、count drift/truncated/重复、真实metadata而非模板/PDFfallback、缺Master/版本/source/hash、二进制名称、ownership/tenant/hash/ticket错、源跨链引用、所有历史版本跨claim冲突、单纯SELECT无敏感路由与raw checksum。只是离线工具验证；真实事实由Root采集，本Agent读receipt并逐raw SHA核验后调用真实validator。

实际Root采集：UTC `2026-10-03 07:29:28.463816`，MySQL8.0.40，database `ruoyi-vue-pro`，UUID `92ca05d0-aec8-11f1-a944-02b4e226a5ef`，1 SELECT/283 facts。查询raw SHA `8cecc9b7313117287ed6f67c2824a65bd9d9bf242cd4ae67e66117f44e2a6474`；facts SHA `8c17e87327b31160e86be9d4c9424670c9a5e8097c1d909c8382f021819a416b`。protected schema SHA `cf6a94375b95720637d7b75a471e508420ffa9cc6b399c4438851237d4fc66fa`。完整tool/contract/test/report SHA见最终`g23-legacy-name-preparation-receipt.json`。

## 后续业务决定与历史保护边界

1. Root保存最新validator的完整protected结果及raw SELECT receipt，逐项审查19冲突、3多名、6编号身份不足。6候选也只是可评审映射，不解除全部25的NULL门禁。
2. 如需进一步确认，按sourceFileId/configId正式存储读取实际对象并比对已冻结sourceSHA；别从URL猜对象位置或使用受控派生PDF。此动作未由本工具执行。
3. 同名跨Master与同Master多名需要业务负责人明确旧逻辑身份/历史适用范围，不能自动删claim、改deleted、给名字加后缀、按latest覆盖或从旧模板名套回源名。C新的tenant级binary unique会拒绝把重复全名直接写进多个active claim；这不只是缺一个字段值。
4. 只有可review业务决定、完整对象/租户/版本证据和单独历史身份配置授权具备后，才另拟精确范围/原值/并发守卫/审计/回退计划；不在本19条additive迁移里夹带UPDATE，不把旧ledger重写或重放旧seed来掩盖。
5. 若未来明确授权历史数据变更，必须先新鲜backup并保持版本/正文/签名/BPM历史原投影不变，限定claim身份配置可变字段、精确主键与preimage hash；失败不能恢复整表覆盖其他并行记录。现有G21演练的历史零写合同不能直接复用为这种额外写入授权，需独立验收。

原C failClosed守卫保留；本审查只补齐真实运行数据前置事实。整体新建上传E2E需要这些前置另行解决，本报告不称完整业务验收完成。

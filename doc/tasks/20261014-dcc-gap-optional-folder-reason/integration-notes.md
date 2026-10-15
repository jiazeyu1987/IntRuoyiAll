# G12 精确接入与审计合同（A01）

## A02接入最新状态
Root已核准本文独立合同，A02精确wrapper/test/policy及M02两服务已实施，按A07正式review接收（前端57/57，后端13/13 GREEN+5/5专属回归）。A04前端已归G01；两服务/G48测试正式归还Root并冻结，policy正式归还后由G01仅新增confirmation且保留strict/upload-bind，M02已收回。下文“Root保留/待票”等仅为历史设计依据，无当前软件待接入事项；最终以m02-final-receipt.json、m02-receipt.md和verification-report.md为准。运行策略发布与工程收尾仍未授权。

## 前端交还请求
upload/index.vue 与 upload/submitter.ts 最小改动已完成，专属行为 11/11 GREEN；可由 Root review 后交还 G01。G12 后续不再写这两个文件，除非 Root 指派复修。

## Root 保留：公共 API wrapper
文件：IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts。
锚点：assertControlledFileSubmitRequest 中 assertRequiredString(payload, 'projectFolderChangeReason', context)。
最小差异：移除必填，若字段非 null/undefined，仍要求 typeof string，trim 后长度 <= 500；空串/空白接受，不注入默认文本。保留 dccProjectCodeId/projectFolderId 精确身份校验及其它字段校验。
验收影响：createWorkingControlledFile / submitControlledFile 共享该校验，当前实际 wrapper 仍拒绝缺省原因。G12 的注入传输单测不是该正式 wrapper 已通过的证据。
专属 Root 接入测试：node --test tests/unit/dcc-upload-g12-api-optional-folder-reason.test.cjs，真实 wrapper 的 6 项中 4 FAIL / 2 PASS（已取得 RED；同时发现 wrapper 对 501 字无上限）。前述最小长度+类型校验应一起接入。

## Root 保留：旧相邻测试
文件：IntRuoyiFronted/tests/unit/dcc-upload-actual-attributes.test.cjs，36 行 test “ordinary upload rejects missing logical folder and empty placement reason”。
最小差异：保留缺文件夹拒绝；将空白说明拒绝改为返回 undefined/JSON 不携带说明；增加超 500 字拒绝（G12 新测试已有真实覆盖）。
改前 SHA256：F1C1E95C3F905E956E3AF997FF7BD62286A4AEB727309C6A3127DAEF0F2FCB9C。
实际回归 51 项：50 PASS / 1 FAIL，唯一失败为这个明确过时断言。未擅自改现有公共测试。

## 独立审计合同提案
1. 保留 dcc.project-file-placement.bind：人工/现有严格绑定与版本继承路径仍要求真实非空原因，保留 REQUIRED_CATEGORY_AND_TEXT；不改全局 GxpAuditService 或 DccProjectConfigurationAuditService.validateReason。
2. 新增 dcc.project-file-placement.upload-bind：仅正常 NEW 上传经服务器派生 storage 的 bindDerived 入口。登记说明 OPTIONAL_TEXT；前端缺省，后端审计按现有 NOT NULL reason 列将缺省/空白记为真实空串（无文字），不编造说明；非空 trim，超过 500 拒绝。初稿的 null 已撤回：当前迁移 20260908_gxp_audit_trail_core.sql 与 H2 fixture 均为 varchar(500) NOT NULL，使用空串即可表达未填写，无需迁移或改内核。
3. 两动作都保持同一物理事务、ABSENT_TO_PRESENT、真实租户和安全上下文操作者、精确 placement 事实和 storage mapping before/after。已有 placement 同归属重放零新事件；冲突归属拒绝。新动作仍使用真实 placement ID 幂等键，不新造事件身份来源。
4. bindDerived 必须具有非空服务器 DccDerivedUploadStorage 且 tenant/actor/project/folder/storage/category 全部匹配；不能因选择可选原因路径跳过任何权限或归属检查。普通 bind 不接收客户端可控的“原因可选”开关。

## Root 保留：最小 policy 新条目
文件：IntRuoyiBackend/config/gxp-audit-policy.yaml；在现有 dcc.project-file-placement.bind 后新增以下独立条目，旧条目和人工目录 create/update/delete 全部保持必填。

```yaml
  - operationId: dcc.project-file-placement.upload-bind
    sourceType: SERVICE_METHOD
    sourceLocator: cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFilePlacementService#bindDerived
    domain: DCC
    subjectType: DCC_PROJECT_FILE_PLACEMENT
    actionType: CREATE
    reasonPolicy: OPTIONAL_TEXT
    signaturePolicy: NOT_REQUIRED
    statePolicy: ABSENT_TO_PRESENT
    retentionClass: GXP_CONTROLLED_DOCUMENT
    testIds: [G12-OPTIONAL-UPLOAD-PLACEMENT]
    owner: dcc-integration
    applicability: GXP
```

全局 GxpAuditServiceImpl 的 validatePolicyRequirements 已支持 OPTIONAL_TEXT（只有 REQUIRED* 才拒绝空原因），无需改内核。运行环境策略发布/入库属于 Root 后续受控任务，本线程不写真实库。

## 验证票请求
需要 Root 唯一票运行 G48StorageProjectionTest 的新增 g12* 场景（H2真实业务服务/mapper/ledger）及其现有派生 storage 场景。先运行新测试取得真实 RED，再改两个服务，GREEN/相邻回归。票需确认当前 reactor/共享 target 所有权；无票不跑 Maven 或大构建。该类继承正式组合测试；运行自身定向方法可避免无关继承测试与该类 storage fixture 的历史冲突，须明确测试选择。
已有 DccPublicUploadPlacementTransactionTest 仍向正常 NEW 显式传 directoryId=101，已不符合当前服务拒绝 caller storage 的合同，不能把它的历史 PASS 当本轮证据；G48StorageProjectionTest 才是现行派生 storage 链路。

RED 建议命令（未执行，需 Root 发票）：
```powershell
mvn -pl yudao-module-dcc -am "-Dtest=G48StorageProjectionTest#g12*" "-Dsurefire.failIfNoSpecifiedTests=false" test
```
GREEN 后相邻建议范围：G48StorageProjectionTest 的 g12*、firstNormalUpload*、sameMapping*、callerDirectory*、mappedBase*、lockedCurrentCategory*，以及 DccProjectFormalCombinationTest 的 placement*。前者的现有正常上传专属 policy fixture 要同步改为新动作；不改后者人工/继承严格 bind fixture。

两个服务最小实现设计：create 在创建 storage/file 前只校验可选说明 trim 后 500 上限；bindDerived 明确非空服务器 context 并选 upload-bind；普通 bind 仍 audit.validateReason。私有绑定对 derived context 的 tenant/actor/project/folder/storage/category 在已有 placement 重放分支之前复核，缺省 reason 记空串，其它均真实 trim，原事务、project→folder→file 锁序及幂等零写入分支保留。

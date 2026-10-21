# E2E-17 既有证据对账（2026-10-21）

按原 E2E-17 六项合同，本例业务验收 PASS 保留。已通过的运行是 4369/A1（文件 2054545668044084369）；不是“R2 运行”。其 25 PASS/0 FAIL/0 BLOCKED 覆盖原合同16项及额外验证。4418/A1（文件 2054545668044084418）是随后一次独立新 fixture，9 PASS/0 FAIL/16 BLOCKED，不能覆盖已通过原例。正式用户模型是单一 `deptId`；同一人跨多个部门的正例按用户定案为不适用/需求范围外，未执行也不计 PASS。音频、视频仅作附件。

| 原始断言 | 既有真实UI证据及结论 | R3本次状态 | 最小下一步 |
|---|---|---|---|
| 未受控时不下发 | 4369/A1：UNCONTROLLED_R1,UNCONTROLLED_R2,UNCONTROLLED_R3，PASS；artifacts/rerun-20261020-r2-round3/notification-UNCONTROLLED-R1.png；artifacts/rerun-20261020-r2-round3/notification-UNCONTROLLED-R2.png；artifacts/rerun-20261020-r2-round3/notification-UNCONTROLLED-R3.png | UNCONTROLLED_R1:PASS,UNCONTROLLED_R2:PASS,UNCONTROLLED_R3:PASS | 无需重跑已接受断言；保留证据。 |
| 受控未生效时不下发 | 4369/A1：PENDING_EFFECTIVE_R1,PENDING_EFFECTIVE_R2,PENDING_EFFECTIVE_R3，PASS；artifacts/rerun-20261020-r2-round3/notification-PENDING_EFFECTIVE-R1.png；artifacts/rerun-20261020-r2-round3/notification-PENDING_EFFECTIVE-R2.png；artifacts/rerun-20261020-r2-round3/notification-PENDING_EFFECTIVE-R3.png | PENDING_EFFECTIVE_R1:PASS,PENDING_EFFECTIVE_R2:PASS,PENDING_EFFECTIVE_R3:PASS | 无需重跑已接受断言；保留证据。 |
| 仅受控且生效后向所选部门全部人员下发 | 4369/A1：ACTIVE_R1,ACTIVE_R2,ACTIVE_R3，PASS；artifacts/rerun-20261020-r2-round3/notification-ACTIVE-R1.png；artifacts/rerun-20261020-r2-round3/notification-ACTIVE-R2.png；artifacts/rerun-20261020-r2-round3/notification-ACTIVE-R3.png；artifacts/rerun-20261020-r2-round3/active-frozen-recipient-scope.png | 4418/A1 的相应分支因文件仍待生效而 BLOCKED；不据此否认4369原例 PASS | 原合同已由4369证据覆盖，不需重跑。4418是独立fixture；只有在单独要求验收该文件时，才先解决共享生效问题。 |
| 非所选范围不因默认会签混入 | 4369/A1：ACTIVE_OUT,ACTIVE_L,ACTIVE_C，PASS；artifacts/rerun-20261020-r2-round3/notification-ACTIVE-OUT.png；artifacts/rerun-20261020-r2-round3/notification-ACTIVE-L.png；artifacts/rerun-20261020-r2-round3/notification-ACTIVE-C.png | 4418/A1 的相应分支因文件仍待生效而 BLOCKED；不据此否认4369原例 PASS | 原合同已由4369证据覆盖，不需重跑。4418是独立fixture；只有在单独要求验收该文件时，才先解决共享生效问题。 |
| 每个接收人分别电子签名，一人签收不能代替其他人 | 4369/A1：RECEIPT_R1,RECEIPT_R2,RECEIPT_R3，PASS；artifacts/rerun-20261020-r2-round3/R1-receipt-result.png；artifacts/rerun-20261020-r2-round3/R2-receipt-result.png；artifacts/rerun-20261020-r2-round3/R3-receipt-result.png；artifacts/rerun-20261020-r2-round3/U-receipts-initial.png；artifacts/rerun-20261020-r2-round3/U-receipts-after-R1.png；artifacts/rerun-20261020-r2-round3/U-receipts-after-R2.png；artifacts/rerun-20261020-r2-round3/U-receipts-after-R3.png | 4418/A1相应分支因没有下发而 BLOCKED；既有4369签收 PASS 不受影响 | 原合同逐人签收已由4369证据覆盖；不重复签收。 |
| 页面可追踪未签收人员 | 4369/A1：INDEPENDENT_TRACKING，PASS；artifacts/rerun-20261020-r2-round3/U-receipts-initial.png；artifacts/rerun-20261020-r2-round3/U-receipts-after-R1.png；artifacts/rerun-20261020-r2-round3/U-receipts-after-R2.png；artifacts/rerun-20261020-r2-round3/U-receipts-after-R3.png | 4418/A1相应分支因没有下发而 BLOCKED；既有4369追踪 PASS 不受影响 | 原合同未签收追踪已由4369证据覆盖；不重复签收。 |

4369/A1名册：ReceiveA的R1/R3、ReceiveB的R2；首次本人签收分别2026-10-20 14:43:50、14:47:40、14:48:29，U正式回读待签3→2→1→0。此证据不推断其他实际组织归属。

4418/A1在T+163秒仍待生效、无分发，是独立的共享 scheduler 延迟生效问题，不是原例新业务断言。该次观察与“49分40秒”历史差值分别记录；历史时间线详见 `activation-delay-assessment-20261019-r2.md`，其分析对象为原4234/A1，不能嫁接到4369或4418。Root转述5625启用且每分钟运行；5625是否覆盖4418、为何未激活仍未定。5629只是定向handler筛选结果。最小后续由Root对账5625既有记录与4418正式候选范围；本次不重跑、不启浏览器。

4369/A1原例已覆盖原合同六项断言，不存在因4418重复运行未完成而产生的原合同缺口。4418仅有的额外分支是原合同之外的T前正文/水印、重复提交/权限（9 PASS/6 BLOCKED）；它们是本轮额外参数结果，不新增原例验收门禁。音频、视频仅作附件。历史记录及证据资产不改；Git/cleanup工程门禁不列业务失败。对账JSON记录文件号、签收证据路径及保护SHA。

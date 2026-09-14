# M0 Derived QA Regulation Fixture

## Scope

本文件记录 2026-08-02 按用户授权从球囊扩张压力泵路线 V21、逐工序 MAIN 批记录绑定、`PROCESS_INSPECTION` 表单槽位和 `过程检验记录 V3.0` 逆推出的本地临时 QA/PQC 规程夹具。

该夹具只用于 M0 真实前置数据准备，不代表正式 QA 规程版本模型已经实现。

## Source

| Source | Value |
|---|---|
| Tenant | `1 / 芋道源码` |
| Route | `922119 / RT000028 / 球囊扩张压力泵` |
| Published route version | `448 / V21` |
| Process inspection form slot | `PROCESS_INSPECTION` |
| Form template | `28 / 过程检验记录` |
| Form template version | `32 / V3.0` |
| Source file | `过程检验记录.docx` |
| Source inspection rows | `49` |

## Created Fixture

| Fixture | Value |
|---|---|
| QC template ID | `6` |
| QC template code | `RRM-20260801-QA-REG-PP-V21` |
| QC template name | `RRM-20260801 球囊扩张压力泵V21临时QA检验规程` |
| Product item | `902149 / 球囊扩张压力泵` |
| Derived method rows | `49` |
| Result rule | `符合 / 不符合` |
| Temporary first-inspection quantity | Source row quantity when present; otherwise `5` |
| Temporary patrol coefficient | `0.05` |

## Derived Method Coverage

| Source process in inspection record | Derived method rows | V21 route process mapping |
|---|---:|---|
| 清洗 | 1 | 粗洗工序、清洗工序 |
| 精洗 | 1 | 精洗工序 |
| 清洁 | 1 | 清洁工序 |
| 组装I | 2 | 组装Ⅰ工序 |
| 硅化I | 2 | 硅化Ⅰ工序 |
| 组装II | 4 | 组装Ⅱ工序 |
| 光固 | 14 | 光固Ⅰ工序、光固Ⅱ工序 |
| 硅化Ⅱ、Ⅲ | 2 | 硅化Ⅱ工序 |
| 检测 | 12 | 检测工序 |
| 组装Ⅲ | 10 | 未匹配 V21 同名路线工序，保留来源项，不静默改名 |

## Route Coverage Limits

- V21 route has MAIN batch-record bindings for 14 process records.
- V21 snapshot explicitly carries `PROCESS_INSPECTION / 过程检验记录 V3.0` form-slot binding on the first two batch-use configs; the inspection form itself contains the 49 method rows above.
- The source inspection record does not contain exact entries for `单包装工序`、`中包装工序`、`大包装工序`.
- The source inspection record contains `组装Ⅲ`, but the V21 route process list does not contain a same-name process; the fixture records it as unmatched instead of mapping it to packaging or another process.

## Temporary Data Contract

- Temporary first-inspection quantity and patrol coefficient are stored in `mes_qc_template_indicator.check_method` and `remark`.
- Temporary values are explicitly tagged as `RRM_TEMP_QA_REG`.
- The fixture does not resolve formal blockers for QA regulation ownership, immutable published version, PQC task identity, regulation snapshot, or per-piece detail model.

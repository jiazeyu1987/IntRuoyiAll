# Verification Report

## Summary

- Data update completed for `tenant_id=1` only.
- Updated `dcc_project_code.product_master_id` for 51 high-confidence project-code rows.
- Left 68 rows unbound because they require business confirmation or additional formal master-data naming rules.

## Matching Rules

- `exact_name`: normalized `dcc_project_code.project_name` equals normalized `mdm_product.name_cn`.
- `registration_suffix_removed`: removing registration/region suffixes such as `CE`、`FDA`、`TUV`、`国内`、`三类` still equals a unique enabled MDM product name.
- `generic_prefix_removed`: removing the generic prefix `一次性使用` still equals a unique enabled MDM product name.
- Excluded: fuzzy match, substring-only match, short generic names, duplicate candidates, disabled/deleted product rows, cross-tenant references.

## Verification Evidence

- RED check failed as expected before update: `IDI` had unique MDM product `INT-15` but empty `product_master_id`.
- Transaction write passed: expected candidates `51`; actual updated rows `51`.
- Final DB check passed: tenant `1` now has `51` bound and `68` unbound rows; tenant `122` remains `7` bound and `126` unbound.
- Reference integrity passed: invalid bound reference count `0`; tenant `1` bound-to-non-enabled product count `0`.
- Pressure pump verification passed: `IDI` is bound to `INT-15 / 按压式球囊扩充压力泵`; `IDPR` to `INT-14`; `ID` to `INT-12`; `IDE/IDE(CE)/IDE(FDA)` to `INT-13`.

## Remaining Unbound Scope

- Remaining rows include cases such as `AB`、`GW（BGGW）`、`NTPTCA`、`PBF`、`SDC`、`VIGW`、`MC`、`VAC`、`IRPTCA` and Codex test rows.
- These were intentionally not bound because the current data does not prove a unique formal MDM product through equal-name rules.

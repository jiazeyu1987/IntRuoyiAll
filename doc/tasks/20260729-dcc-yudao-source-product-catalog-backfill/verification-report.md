# Verification Report

## Result

PASS for the local `芋道源码` runtime target: Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro`.

## Target Resolution

- `芋道源码` exists in `system_tenant`.
- Local runtime rules identify Docker MySQL `23306/ruoyi-vue-pro` as the local application dependency.
- Separate Windows MySQL `3306` was not modified because the configured root credential returned `1045 Access denied`.

## Data Verification

- `dcc_product_catalog` columns `project_name` and `project_code`: present, `utf8mb4_unicode_ci`.
- Active `瑛泰产品` rows: 181.
- Rows with both project fields filled: 115.
- Selected high/low/无法对应 row set with project fields: 0.
- Sample row 2: `一次性使用血管鞘 / VS`.
- Sample row 61: `按压式球囊扩充压力泵 / IDI`.
- Sample rows 8, 25, 29: project fields remain NULL.

## Commands

- Formal migration re-run through Docker MySQL -> PASS.
- HEX-based schema/data verification -> PASS.
- Database schema evidence validator -> PASS.


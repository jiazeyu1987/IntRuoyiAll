BDD: three process pages post-commit comparison -> Given the generic layout rules were updated, When the latest Route B reports for 精洗 / 清洗 / 清洁 are regenerated and compared with their source images, Then we can verify whether the latest changes further improved visual fidelity.

RED: `GET http://127.0.0.1:8081/mes/pro/batchrecordtemplate?mode=designer&reportId=85468f144bf54c198df8ae6cf8027b41` -> FAIL, legacy no-hyphen route returned frontend `404`, so the live capture path had to use the registered route `/mes/pro/batch-record-template`.

GREEN: `GET http://127.0.0.1:48081/v3/api-docs` -> PASS
GREEN: `POST http://127.0.0.1:48081/admin-api/system/oauth2/token` -> PASS
GREEN: `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B` -> PASS, `updatedCount=15`
GREEN: live screenshot capture for `精洗 / 清洗 / 清洁` -> PASS

Conclusion:
- `精洗`: yes, improved. The header rhythm, gray/white label-value split, and overall width usage are more stable than before, and it remains the closest to the source image. Remaining differences are mainly designer chrome, placeholder text, and some dense cells on the right side.
- `清洗`: yes, improved. This page benefits the most from the generic repeated-subheader handling. Compared with the earlier flattened result, the segmented process blocks now stay on the process-page path more consistently. Remaining differences are still the densest right-side columns and designer-only visual chrome.
- `清洁`: yes, improved. Width usage and border hierarchy are more controlled than the older left-clustered version. Remaining differences are still in row spacing, designer chrome, and the fact that placeholder-heavy empty cells make the grid look busier than the paper source.

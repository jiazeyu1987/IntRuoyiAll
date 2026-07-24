BDD: post-change three-page visual review -> Given the generic visual-rule changes are applied, When the current live Route B screenshots for 精洗 / 清洗 / 清洁 are compared to their source images, Then we can determine whether each page improved and identify the remaining generic gaps.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS
GREEN: `GET http://127.0.0.1:48081/v3/api-docs` -> PASS
GREEN: `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B` -> PASS
GREEN: live screenshot capture for `精洗 / 清洗 / 清洁` -> PASS

Conclusion:
- `精洗`: improved; now close to the source image, with only minor spacing, grey-level, and line-weight differences remaining.
- `清洗`: improved the most compared with the earlier version, but still has the largest structural gap because repeated subheaders and multi-stage blocks remain too flat/dense.
- `清洁`: improved; width usage and line balance are better than before, but proportion, spacing, and line hierarchy still need generic tuning.

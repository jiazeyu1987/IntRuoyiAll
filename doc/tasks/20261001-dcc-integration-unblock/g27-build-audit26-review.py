"""Produce the superseding unapproved 26-operation quality review without enabling policy."""
from pathlib import Path
import hashlib
import html
import json
from html.parser import HTMLParser
from importlib.util import spec_from_file_location, module_from_spec

ROOT=Path(__file__).resolve().parent
SOURCE=Path('C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-public-backend-completion/g27-audit-26-operation-impact.json')
OLD_BUILDER=ROOT/'g26-build-audit-review.py'

def main():
    spec=spec_from_file_location('g27_review_labels',OLD_BUILDER);old=module_from_spec(spec);spec.loader.exec_module(old)
    labels={**old.NAMES,'dcc.controlled-file.legacy-name-occupancy.activate':'核验原件后登记历史原文件名占用'}
    data=json.loads(SOURCE.read_text(encoding='utf-8'));ops=data['missingOperations']
    assert len(ops)==26 and {o['operationId'] for o in ops}==set(labels)
    assert data['allOriginal33FieldsUnchanged'] and data['missingDccConfigurationCount']==26
    esc=html.escape
    rows=''.join(f'<tr><td>{i}</td><td>{esc(labels[o["operationId"]])}</td><td><code>{esc(o["operationId"])}</code></td></tr>' for i,o in enumerate(ops,1))
    doc=f'''<!doctype html><html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>DCC 26项审计规则审查候选</title><style>body{{font-family:system-ui,"Microsoft YaHei",sans-serif;color:#233248;max-width:1100px;margin:36px auto;padding:0 24px;line-height:1.65}}h1{{font-size:28px}}.state{{padding:16px;background:#fff2d5;border-left:5px solid #c6900d}}table{{border-collapse:collapse;width:100%;font-size:14px}}td,th{{border:1px solid #dce4ee;padding:10px;text-align:left}}th{{background:#f1f5fa}}code{{overflow-wrap:anywhere}}.note{{color:#52667c}}@media print{{body{{margin:0}}tr{{break-inside:avoid}}}}</style></head><body><h1>DCC 26项审计规则审查候选</h1><p class="state"><strong>尚未质量批准，尚未启用。</strong>用户已明确答复“尚未批准”。本页是审查资料，不记录批准或签名。</p><p>原25项候选保留为历史材料。这次补上“核验原件后登记历史原文件名占用”这一独立技术动作，形成26项待配置审计；其余原33项策略定义的字段保持原样。文件发布的现有运行登记继续保持原配置。</p><p>正常操作必须记录真实操作人、系统时间、原因、对象身份和变更前后事实。名称登记与审计同事务提交，任何审计失败都整体回滚；没有完整原件核验证据时不能登记。该技术登记不增加业务审批节点。</p><table><thead><tr><th>序号</th><th>业务动作</th><th>固定审计动作标识</th></tr></thead><tbody>{rows}</tbody></table><h2>固定批准范围</h2><p>版本：<code>{esc(data['candidatePolicyVersion'])}</code><br>策略SHA-256：<code>{esc(data['candidatePolicySha256'])}</code><br>源码覆盖报告SHA-256：<code>{esc(data['prospectiveSourceCoverageReportSha256'])}</code></p><p>质量负责人须审阅这一固定候选，形成实际批准意见、职责与账号、时间以及可核查的电子签名／记录依据。admin仅是此前提供的账号，不代表已经签名。业务节点要求的电子签名继续按实际流程执行。</p><p class="note">覆盖检查通过34项策略定义与12处实际注解定位。此检查不是质量批准、数据库配置执行或真实前端验收。此前25项的条件写入授权不扩大为26项；新增表、历史占用登记和3个缺失对象恢复均按各自授权边界执行。目前仅19项数据库升级已通过，实际39原件35匹配、4缺失，恢复尚未批准。</p><p><a href="g26-audit-rules-quality-review.html">此前25项未批准审查材料</a> · <a href="g27-review.md">本轮主管理Review</a></p></body></html>'''
    HTMLParser().feed(doc);assert len(ops)==26 and doc.count('<tr><td>')==26 and '尚未质量批准' in doc
    target=ROOT/'g27-audit26-quality-review.html';target.write_text(doc,encoding='utf-8')
    receipt={'status':'UNAPPROVED_26_OPERATION_REVIEW_ARTIFACT','operationCount':26,'sourceImpactSha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'policyHash':data['candidatePolicySha256'],'coverageHash':data['prospectiveSourceCoverageReportSha256'],'artifactSha256':hashlib.sha256(target.read_bytes()).hexdigest(),'qualityApproved':False,'configurationExecuted':False}
    (ROOT/'g27-audit26-quality-review.receipt.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8');print(json.dumps({'status':receipt['status'],'operationCount':26,'qualityApproved':False}))

if __name__=='__main__':main()

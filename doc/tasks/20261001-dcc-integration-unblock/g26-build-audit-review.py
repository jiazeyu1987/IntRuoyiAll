"""Build a human-readable, explicitly unapproved audit-rule review artifact."""
from pathlib import Path
import hashlib
import html
import json
from html.parser import HTMLParser

ROOT=Path(__file__).resolve().parent
SOURCE=Path('C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-public-browser/g22-gxp-25-operation-impact.json')
NAMES={
'dcc.controlled-file.activate':'新受控版本到达生效日期',
'dcc.controlled-file.auto-obsolete':'新版本生效后旧版自动作废',
'dcc.controlled-file.control':'文控审核后盖章受控',
'dcc.controlled-file.obsolete':'作废申请批准后文件作废',
'dcc.folder-template.delete':'删除文件夹模板',
'dcc.folder-template.save':'新增或修改文件夹模板',
'dcc.project-attributes.configure':'修改项目三组默认属性',
'dcc.project-file-placement.bind':'把文件正式存入项目文件夹',
'dcc.project-folder.create':'新增项目存储文件夹',
'dcc.project-folder.delete':'删除项目存储文件夹',
'dcc.project-folder.update':'修改项目存储文件夹',
'dcc.project-product.approve':'批准项目及产品创建申请',
'dcc.project-product.complete':'批准后完成项目及产品创建',
'dcc.project-product.create':'提交项目及产品创建申请',
'dcc.project-product.resubmit':'驳回后重新提交项目及产品申请',
'dcc.project-product.retry':'重试批准后的创建写入',
'dcc.project-product.review':'审核项目及产品创建申请',
'dcc.project-product.reviewer-config':'配置项目及产品审核人员',
'dcc.project-product.write-failed':'记录批准后创建写入失败',
'dcc.project-reference.cancel':'项目负责人取消文件引用',
'dcc.project-reference.create':'项目负责人引用文件',
'dcc.relation.arrange':'升版时安排关联文件整改',
'dcc.relation.controlled':'受控后登记关联文件整改通知',
'dcc.relation.notification.retry':'重试关联文件整改通知',
'dcc.relation.replace':'修改当前文件关联',
}

def main():
    r=json.loads(SOURCE.read_text(encoding='utf-8'));ops=r['operations']
    assert len(ops)==25 and {o['operationId'] for o in ops}==set(NAMES)
    assert all(o['reasonPolicy']=='REQUIRED_CATEGORY_AND_TEXT' and o['signaturePolicy']=='NOT_REQUIRED' for o in ops)
    esc=html.escape
    rows=''.join(f'<tr data-operation="{esc(o["operationId"])}"><td>{i}</td><td>{esc(NAMES[o["operationId"]])}</td><td>记录真实操作人、系统时间、变更原因、变更前后状态及对象身份</td></tr>' for i,o in enumerate(ops,1))
    policy=r['sources']['policy']['sha256'];coverage=r['formalCoverage']['reportSha256']
    doc=f'''<!doctype html><html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>DCC审计规则质量审查材料</title><style>body{{font-family:system-ui,"Microsoft YaHei",sans-serif;color:#223047;max-width:1100px;margin:36px auto;padding:0 24px;line-height:1.65}}h1{{font-size:28px}}.state{{background:#fff4d6;border-left:5px solid #ce970d;padding:16px}}table{{border-collapse:collapse;width:100%;font-size:14px}}th,td{{border:1px solid #dce3ec;padding:10px;text-align:left;vertical-align:top}}th{{background:#f1f5fa}}code{{overflow-wrap:anywhere}}.note{{color:#57677d}}@media print{{body{{margin:0;max-width:none}}tr{{break-inside:avoid}}}}</style></head><body><h1>DCC审计规则质量审查材料</h1><p class="state"><strong>状态：尚未批准，尚未启用。</strong>用户已于2026年10月3日明确确认“尚未批准”。admin为此前提供的账号，未形成实际质量电子签名。</p><p>本次准备为25项DCC操作追加审计登记。原有“文件发布”的审计登记保持原配置。成功业务操作与相应审计在同一事务内完成，审计失败则业务操作回滚。</p><p>下列审计事件自身不另要求电子签名。会签、批准、文控审核等业务环节已要求的电子签名继续按实际业务规则执行；这份审计策略生效仍需要实际质量批准。</p><table><thead><tr><th>序号</th><th>业务动作</th><th>记录内容</th></tr></thead><tbody>{rows}</tbody></table><h2>批准资料的范围</h2><p>请质量负责人核对这25项动作、固定策略和覆盖报告，记录批准意见、实际时间、质量职责与账号，以及可核查的电子签名或批准记录引用。此次材料不能替代实际签名。</p><p>策略版本：<code>{esc(r['policyVersion'])}</code><br>策略SHA-256：<code>{policy}</code><br>覆盖报告SHA-256：<code>{coverage}</code></p><p class="note">覆盖报告登记33项操作及11处注解定位，它证明源码映射，不能表示真实业务测试或质量批准已完成。本次19项数据库升级已通过；对象恢复、历史名称登记、25项审计启用及完整前端验收按各自实际状态记录。</p></body></html>'''
    parser=HTMLParser();parser.feed(doc);parser.close();assert doc.count('<tr data-operation=')==25 and '尚未批准' in doc
    target=ROOT/'g26-audit-rules-quality-review.html';target.write_text(doc,encoding='utf-8')
    receipt={'status':'UNAPPROVED_REVIEW_ARTIFACT','operationCount':25,'sourceImpactSha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'artifactSha256':hashlib.sha256(target.read_bytes()).hexdigest(),'qualityApproved':False,'configurationExecuted':False}
    (ROOT/'g26-audit-rules-quality-review.receipt.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(receipt))

if __name__=='__main__':main()

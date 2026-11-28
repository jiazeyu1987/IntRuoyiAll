from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import fitz

output = Path(__file__).parent
pdfmetrics.registerFont(TTFont('EmbeddedHei', 'C:/Windows/Fonts/simhei.ttf'))
common = [
    '模拟测试资料 / SIMULATED TEST ONLY',
    '非真实注册证、非监管批准、不可用于临床或经营',
    '公司及持证主体：上海七木医疗器械有限公司',
    '项目：IDI；DCC：0313170202；产品：按压式压力泵',
    '分类：二类 / II；英文名称：Simulated Pressure Pump',
]
documents = {}
for purpose in ['certificate', 'description']:
    documents[f'SIM-REG-20261126-1401-FDA-{purpose}-R2'] = common + [
        f'测试标识：SIM-REG-20261126-1401-FDA；材料：{purpose}',
        'K/P号：K261401；提交类型：Traditional 510(k)；产品代码：SIM',
        '批准日期：2026-11-25；型号：SIM-A',
        '是否客户持证：否；批准信函状态：有效；工厂登记状态：Active',
        '预期用途：仅验证系统材料流转，不用于临床。',
        'R2补正706：不使用国内模板的自产、住所及首次获证字段。',
        '不声明上述通用摘要为真实FDA制造信息。',
        '无独立生效日期、无有效期；不申请到期续证。',
    ]
for kind in ['CN', 'CE']:
    documents[f'SIM-REG-20261126-1401-{kind}-renewal-R2'] = common + [
        f'证号：SIM-REG-20261126-1401-{kind}；材料：续证',
        '型号：SIM-B' if kind == 'CE' else '型号：SIM-A',
        '首次获证日期：2026-11-25；本次续证批准日期：2026-11-26',
        '本次续证有效期至：2029-01-20',
        '生效日期：2026-11-27' if kind == 'CN' else 'CE批准日期即生效含义，无独立生效日期。',
        '自产：是；委托生产：否。',
        '预期用途：仅验证系统材料流转，不用于临床。',
        '英文名：Simulated Pressure Pump；公告机构：0123',
        '产品代码：SIM-IDI；商品名：SIM-PUMP；Rev.0',
        '本次不改变持证主体、公司、项目、DCC及现有型号。',
    ]
for name, lines in documents.items():
    target = output / f'{name}.pdf'
    document = canvas.Canvas(str(target))
    document.setFont('EmbeddedHei', 10)
    for index, line in enumerate(lines):
        document.drawString(30, 800 - index * 30, line)
    document.save()
    preview = fitz.open(target)
    assert 'SIMULATED TEST ONLY' in preview[0].get_text()
    preview[0].get_pixmap().save(str(target.with_suffix('.png')))
    print(target.name)

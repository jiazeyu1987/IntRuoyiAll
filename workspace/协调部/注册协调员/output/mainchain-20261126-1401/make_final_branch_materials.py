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
    '分类：二类；预期用途仅验证系统材料流转，不用于临床。',
]
documents = {}
for purpose in ['change', 'description']:
    documents[f'SIM-REG-20261126-1401-FDA-{purpose}-R3'] = common + [
        f'测试标识：SIM-REG-20261126-1401-FDA；材料：{purpose}',
        '产品英文名称：Simulated Pressure Pump',
        'K/P号：K261401；提交类型：Traditional 510(k)；产品代码：SIM',
        '原批准日期：2026-11-25；本次变更批件批准日期：2026-11-27',
        '是否客户持证：否；批准信函状态：有效；工厂登记状态：Active',
        '型号变更：原产品描述SIM-A，本次改为SIM-B。',
        '系统原型号字段为空；本次填入SIM-B，其他填报字段不变。',
        '不使用国内制造方式、住所、首次获证或有效期字段。',
    ]
documents['SIM-REG-20261126-1401-CN-change-R3'] = common + [
    '证号：SIM-REG-20261126-1401-CN；材料：型号变更批件',
    '原批准日期：2026-11-26；首次获证日期：2026-11-25',
    '原生效日期：2026-11-27；原有效期至：2029-01-20',
    '变更批件批准日期：2026-11-27；型号SIM-A改为SIM-B。',
    '原系统型号字段为空，本次填入SIM-B；其他现有字段不变。',
    '自产：是；委托生产：否。',
]
documents['SIM-REG-20261126-1401-CN-future-renewal-R3'] = common + [
    '证号：SIM-REG-20261126-1401-CN；材料：未来生效续证',
    '首次获证日期：2026-11-25；型号：SIM-B',
    '续证批准日期：2026-11-27；生效日期：2026-11-28',
    '续证有效期至：2030-01-20；类别不变。',
    '自产：是；委托生产：否；其他现有字段不变。',
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

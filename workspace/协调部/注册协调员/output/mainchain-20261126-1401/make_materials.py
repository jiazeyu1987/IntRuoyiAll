from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont

output = Path(__file__).parent
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
for kind in ['CN', 'CE', 'FDA']:
    for purpose in ['certificate', 'attachment', 'renewal', 'change']:
        document = canvas.Canvas(str(output / f'SIM-REG-20261126-1401-{kind}-{purpose}.pdf'))
        document.setFont('STSong-Light', 14)
        lines = ['模拟测试资料 / SIMULATED TEST ONLY', '非真实注册证、非监管批准、不可用于临床或经营', f'测试标识：SIM-REG-20261126-1401-{kind}', f'材料用途：{purpose}', '公司及持证主体：上海七木医疗器械有限公司', '项目：IDI；DCC：0313170202；产品：按压式压力泵', f'注册证号：SIM-REG-20261126-1401-{kind}', '分类：二类 / II；型号：SIM-A；住所：模拟测试路1号', '预期用途：仅验证系统材料流转，不用于临床。', '批准日期：2026-11-25']
        if kind != 'FDA':
            lines += ['有效期至2027-01-20；续证批准2026-11-26，有效至2029-01-20']
        if kind == 'CN':
            lines += ['生效2026-11-26；续证未来生效2026-11-27', '是否委托生产：否；是否自行生产：是']
        if kind == 'CE':
            lines += ['制造商：上海七木医疗器械有限公司；公告机构：0123', '产品代码：SIM-IDI；商品名：SIM-PUMP；Rev.0；签发2026-11-25']
        if kind == 'FDA':
            lines += ['批准信函和产品描述仅为格式样本；无独立生效日和有效期。']
        if purpose == 'change':
            lines += ['变更：型号由SIM-A变为SIM-B；其他信息不变。']
        for index, line in enumerate(lines):
            document.drawString(36, 800 - index * 34, line)
        document.save()
print('Created 12 simulated PDFs.')

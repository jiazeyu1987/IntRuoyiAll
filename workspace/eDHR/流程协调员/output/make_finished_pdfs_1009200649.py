from pathlib import Path
import fitz
from pypdf import PdfReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Paragraph, SimpleDocTemplate

output = Path(__file__).resolve().parent
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
body = ParagraphStyle('body', fontName='STSong-Light', fontSize=12, leading=21, spaceAfter=16, wordWrap='CJK')
title = ParagraphStyle('title', parent=body, fontSize=19, leading=28, alignment=1)
warning = ParagraphStyle('warning', parent=body, textColor='#A51D26', alignment=1)
for category, task in [('成品检验报告', '3101'), ('成品检验记录', '3102')]:
    path = output / f'SIM-1009200649-{category}-软件模拟.pdf'
    if path.exists():
        raise FileExistsError(path)
    paragraphs = [Paragraph(category + '（软件模拟）', title), Paragraph('软件模拟、非真实实测／质量批准', warning), Paragraph('模拟测试附件、非真实检验报告／记录、不可作为真实质量依据', warning)]
    texts = [
        '环境：本地8081／测试1；生产订单881MO084059；本轮活跃周期1009200649；批号34125090001；批次执行900000001352。',
        f'类别：{category}；本人正式资料任务{task}，工作台显示待处理，生成时间2026-11-21 11:13:33；责任岗钱凤琴（qianfengqin）。',
        '本附件仅用于本轮软件资料流程测试，不是实际检验报告或原始检验记录。不编造检验结果、实测读数、真实机构证明、印章或签字，不替代系统原始记录。',
        '质量信息保留：协调交接说明本轮损耗1、整体不合格已保留并在PQC放行意见披露。本资料不改为合格、不撤销不合格、不补造批准；此信息为交接说明，非本岗重新作出的质量判定。',
        '生产放行信息：协调交接报告申请343已放行，测试签名65501，时间2026-11-21北京时间11:13:32；本岗本次仅核对本人正式资料任务，不将交接信息冒充独立放行核验或真人亲签。',
        '当前仅完成附件准备，未上传、未保存为系统关联附件。上传故障修复并获得接续任务后，先查已有文件，再仅补未保存项；不操作其他订单、不审批、不上市放行、不归档。',
    ]
    paragraphs.extend(Paragraph(text, body) for text in texts)
    SimpleDocTemplate(str(path)).build(paragraphs)
    reader = PdfReader(path)
    text = ''.join(page.extract_text() for page in reader.pages)
    for required in ['1009200649', '881MO084059', '34125090001', task, '整体不合格', '不可作为真实质量依据']:
        if required not in text:
            raise ValueError((path, required))
    if len(reader.pages) != 1 or not path.read_bytes().startswith(b'%PDF-'):
        raise ValueError(path)
    document = fitz.open(path)
    document[0].get_pixmap().save(str(path.with_suffix('.png')))
    print(f'PASS | {path} | 1 page | PDF header and text verified | rendered')

from pathlib import Path
from xml.sax.saxutils import escape
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
for order, cycle in [('881MO083075', '1009200645'), ('881MO097262', '1009200646'), ('881MO088489', '1009200647')]:
    batch = 'SIM20261121' + cycle[-4:]
    for category in ['成品检验报告', '成品检验记录']:
        path = output / f'SIM-{cycle}-{category}-软件模拟.pdf'
        if path.exists():
            raise FileExistsError(path)
        texts = [
            f'本地8081／测试1租户；生产订单：{order}；活跃周期：{cycle}；生产批号：{batch}。',
            f'附件类别：{category}；责任岗位：成品文档（qianfengqin／钱凤琴）。仅适用本轮上述对象，不复用旧周期记录。',
            '用途：本轮软件流程模拟的资料上传验证附件，不是实际检验活动产生的报告或原始记录。',
            '本文件不出具合格／不合格质量结论，不编造检验项目结果、测量读数、真实机构证明、实际器具使用、印章或签字。',
            '真实生产检验原始证据、责任决定与系统审批记录以正式业务记录为准；本附件不能替代或证明任何真实实测、质量批准、上市放行或归档。',
            '提交边界：仅按本人正式获派类别，在核对PQC生产放行成立后上传；资料保存不等于质量审批或上市放行。',
        ]
        contents = [Paragraph(category + '（软件模拟）', title), Paragraph('软件模拟、非真实实测／质量批准', warning), Paragraph('模拟测试附件、非真实检验报告／记录、不可作为真实质量依据', warning)]
        contents += [Paragraph(escape(text), body) for text in texts]
        SimpleDocTemplate(str(path)).build(contents)
        reader = PdfReader(path)
        extracted = ''.join(page.extract_text() for page in reader.pages)
        for required in [order, cycle, batch, category, '软件模拟', '不可作为真实质量依据']:
            if required not in extracted:
                raise ValueError((path, required))
        if len(reader.pages) != 1 or not path.read_bytes().startswith(b'%PDF-'):
            raise ValueError(path)
        document = fitz.open(path)
        document[0].get_pixmap().save(str(path.with_suffix('.png')))
        print(f'PASS | {path} | 1 page | header/text verified | rendered')

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
for category, task in [('成品检验报告', '3095'), ('成品检验记录', '3096')]:
    path = output / f'SIM-1009200648-{category}-软件模拟.pdf'
    if path.exists():
        raise FileExistsError(path)
    paragraphs = [Paragraph(category + '（软件模拟）', title), Paragraph('软件模拟、非真实实测／质量批准', warning), Paragraph('模拟测试附件、非真实检验报告／记录、不可作为真实质量依据', warning)]
    texts = [
        '环境：本地8081／测试1；生产订单881MO100160；本轮活跃周期1009200648；批号SIM202611210648；批次执行900000001351。',
        f'类别：{category}；本人正式资料任务{task}，工作台显示待处理，生成时间2026-11-21 10:01:32；责任岗钱凤琴（qianfengqin）。',
        '本附件仅用于本轮软件资料流程测试，不是实际检验报告或原始检验记录。不编造检验结果、实测读数、真实机构证明、印章或签字，不替代系统原始记录。',
        '差异披露：详情顶部红字显示“单包装工序：冻结工艺输入物料 A006.011.2003 未匹配正式领料记录，请核对用料。”当前为非阻断提示，页面可以正常打开。',
        '冻结INPUT A006.011.2003与实际领料A006.011.2008分别保留；总表列示A006.011.2008，批号S20260801。本附件不更改任何冻结或领料记录，不证明物料等效或真实替代批准。',
        '本岗已核对总表PQC生产放行状态为已生产放行，页面朱利江测试签名时间2026-11-21 10:01:31。资料上传并不构成质量决定、上市放行或归档。',
    ]
    paragraphs.extend(Paragraph(text, body) for text in texts)
    SimpleDocTemplate(str(path)).build(paragraphs)
    reader = PdfReader(path)
    text = ''.join(page.extract_text() for page in reader.pages)
    for required in ['1009200648', '881MO100160', 'SIM202611210648', task, 'A006.011.2003', 'A006.011.2008', '不可作为真实质量依据']:
        if required not in text:
            raise ValueError((path, required))
    if len(reader.pages) != 1 or not path.read_bytes().startswith(b'%PDF-'):
        raise ValueError(path)
    document = fitz.open(path)
    document[0].get_pixmap().save(str(path.with_suffix('.png')))
    print(f'PASS | {path} | 1 page | PDF header and text verified | rendered')

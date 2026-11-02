import pptx
I=pptx.util.Inches;P=pptx.util.Pt;C=pptx.dml.color.RGBColor
A=pptx.enum.text.PP_ALIGN;M=pptx.enum.shapes.MSO_SHAPE
PRI=C(108,92,231);PD=C(74,63,181);PL=C(162,155,254);AC=C(0,206,201)
DK=C(45,34,60);MD=C(142,130,165);WH=C(255,255,255)
C1=C(240,238,255);C2=C(232,245,233);C3=C(227,242,253);C4=C(255,243,224)
FN=chr(77)+chr(105)+chr(99)+chr(114)+chr(111)+chr(115)+chr(111)+chr(102)+chr(116)+chr(32)+chr(89)+chr(97)+chr(72)+chr(101)+chr(105)
prs=pptx.Presentation();prs.slide_width=I(13.333);prs.slide_height=I(7.5)
W=prs.slide_width;H=prs.slide_height;BN=prs.slide_layouts[6]
def NB():return prs.slides.add_slide(BN)
def R(s,l,t,w,h,f=None,st=M.RECTANGLE):
 sh=s.shapes.add_shape(st,l,t,w,h);sh.line.fill.background()
 if f:sh.fill.solid();sh.fill.fore_color.rgb=f
 return sh
def TB(s,l,t,w,h,tx,sz=18,b=False,c=None,a=A.LEFT):
 tb=s.shapes.add_textbox(l,t,w,h);tf=tb.text_frame;tf.word_wrap=True
 p=tf.paragraphs[0];p.text=tx;p.font.size=P(sz);p.font.bold=b;p.font.color.rgb=c if c else DK;p.font.name=FN;p.alignment=a
 return tb
def AD(tf,tx,sz=16,b=False,c=None,sb=None):
 p=tf.add_paragraph();p.text=tx;p.font.size=P(sz);p.font.bold=b;p.font.color.rgb=c if c else DK;p.font.name=FN
 if sb:p.space_before=sb
 return p
def HA(s,t):
 R(s,0,0,W,P(4),PRI);R(s,0,0,I(5),I(1),PRI);TB(s,I(0.6),I(0.15),I(10),I(0.7),t,26,True,WH)
s=NB();R(s,0,0,W,H,PRI)
R(s,I(-1.5),I(-1.5),I(5),I(5),PD,M.OVAL);R(s,I(9),I(4.5),I(6),I(6),PL,M.OVAL)

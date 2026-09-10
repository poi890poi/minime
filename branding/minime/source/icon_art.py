"""Approved MinIME artwork. Original paths shared by Android, print and web exports."""
import math
from PIL import Image, ImageDraw
TEAL='#006765'
CREAM='#FFF7E5'
LEAF='#D9C28E'

def rounded(l,t,r,b,k):
    return [('M',l+k,t),('L',r-k,t),('Q',r,t,r,t+k),('L',r,b-k),('Q',r,b,r-k,b),('L',l+k,b),('Q',l,b,l,b-k),('L',l,t+k),('Q',l,t,l+k,t),('Z',)]

def transform(path,scale=1,dx=0,dy=0):
    return [(c[0],*(v*scale+(dx if i%2==0 else dy) for i,v in enumerate(c[1:]))) for c in path]

def soft_face():
    points=[]
    for i in range(513):
        t=2*math.pi*i/512;x=math.cos(t);y=math.sin(t)
        points.append((24+11*math.copysign(abs(x)**(2/2.6),x),31.5+9.5*math.copysign(abs(y)**(2/2.6),y)))
    path=[('M',*points[0])]
    for i in range(0,512,2):
        a,m,b=points[i:i+3]
        path.append(('Q',2*m[0]-(a[0]+b[0])/2,2*m[1]-(a[1]+b[1])/2,*b))
    return path+[('Z',)]

def geometry():
    # Face, hat, eyes, mouth. These preserve the approved proposal exactly.
    shapes=[
        (soft_face(),CREAM,None,0),
        ([('M',24,10.3),('Q',24.8,10.3,25.8,10.8),('L',40.5,17.8),('Q',42.5,19.1,39.8,19.1),('L',8.2,19.1),('Q',5.5,19.1,7.5,17.8),('L',22.2,10.8),('Q',23.2,10.3,24,10.3),('Z',)],LEAF,None,0),
        (rounded(18.2,28.5,20.7,32.5,1.25),TEAL,None,0),
        (rounded(27.3,28.5,29.8,32.5,1.25),TEAL,None,0),
        ([('M',21,34.8),('L',24,37),('L',27,34.8)],None,TEAL,1.7)]
    result=[]
    for i,(path,fill,stroke,width) in enumerate(shapes):
        if i!=1:path=transform(path,1.08,24*(1-1.08),31.5*(1-1.08));width*=1.08
        path=transform(path,1.12,24*(1-1.12),26*(1-1.12));width*=1.12
        result.append((path,fill,stroke,width))
    return result

def points(path):
    result=[];at=None
    for c in path:
        if c[0] in ('M','L'):at=c[1:];result.append(at)
        elif c[0]=='Q':
            a=at;q=c[1:3];b=c[3:5]
            for i in range(1,33):
                u=i/32;result.append(tuple((1-u)**2*a[j]+2*(1-u)*u*q[j]+u*u*b[j] for j in (0,1)))
            at=b
        elif c[0]=='Z':result.append(result[0])
    return result

def smile_outline():
    # Closed outline of the round-capped V, for a real even-odd alpha cutout.
    path,_,_,width=geometry()[4];a,b,c=[v[1:] for v in path];r=width/2
    theta=math.atan2(b[1]-a[1],b[0]-a[0]);normal=theta+math.pi/2
    p=[(a[0]+r*math.cos(normal),a[1]+r*math.sin(normal))]
    def arc(center,start,end):
        for i in range(25):
            t=start+(end-start)*i/24;p.append((center[0]+r*math.cos(t),center[1]+r*math.sin(t)))
    arc(b,normal,math.pi-normal)
    arc(c,math.pi-normal,-normal)
    p.append((b[0],b[1]-r/math.cos(theta)))
    arc(a,normal-math.pi,normal-2*math.pi)
    return [('M',*p[0])]+[('L',*v) for v in p[1:]]+[('Z',)]

def data(path):
    def num(v):return f'{v:.5f}'.rstrip('0').rstrip('.') or '0'
    return ' '.join(c[0]+' '.join(num(v) for v in c[1:]) for c in path)

def svg_paths(mono=None):
    g=geometry()
    if mono:
        holes=g[0][0]+g[2][0]+g[3][0]+smile_outline()
        return f'<path fill="{mono}" fill-rule="evenodd" d="{data(holes)}"/><path fill="{mono}" d="{data(g[1][0])}"/>'
    return ''.join(f'<path fill="{fill or "none"}" stroke="{stroke or "none"}" stroke-width="{width:.5f}" stroke-linecap="round" stroke-linejoin="round" d="{data(path)}"/>' for path,fill,stroke,width in g)

def svg(background='rounded',mono=None,adaptive=False):
    viewport=108 if adaptive else 48
    content=''
    if background:content=f'<path fill="{TEAL}" d="{data(rounded(0,0,48,48,13) if background=="rounded" else rounded(0,0,48,48,0))}"/>'
    content+=svg_paths(mono)
    if adaptive:content=f'<g transform="translate(18 18) scale(1.5)">{content}</g>'
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{viewport}" height="{viewport}" viewBox="0 0 {viewport} {viewport}">{content}</svg>\n'

def draw_path(draw,path,fill,stroke,width,scale,offset=0):
    p=[(x*scale+offset,y*scale+offset) for x,y in points(path)]
    if fill is not None:draw.polygon(p,fill=fill)
    if stroke is not None:
        draw.line(p,fill=stroke,width=round(width*scale),joint='curve')
        for x,y in p:draw.ellipse((x-width*scale/2,y-width*scale/2,x+width*scale/2,y+width*scale/2),fill=stroke)

def render(size=512,background='rounded',mono=None,adaptive=False):
    factor=4;im=Image.new('RGBA',(size*factor,size*factor));d=ImageDraw.Draw(im)
    s=size*factor/48
    if background=='circle':d.ellipse((0,0,size*factor-1,size*factor-1),fill=TEAL)
    elif background:draw_path(d,rounded(0,0,48,48,13 if background=='rounded' else 0),TEAL,None,0,s)
    if adaptive:s=size*factor/72;offset=size*factor/6
    else:offset=0
    if mono:
        mask=Image.new('L',im.size);md=ImageDraw.Draw(mask)
        for i,(path,fill,stroke,width) in enumerate(geometry()):draw_path(md,path,255 if i<2 else (0 if fill else None),0 if stroke else None,width,s,offset)
        ink=Image.new('RGBA',im.size,mono);im.alpha_composite(Image.composite(ink,Image.new('RGBA',im.size),mask))
    else:
        for path,fill,stroke,width in geometry():draw_path(d,path,fill,stroke,width,s,offset)
    return im.resize((size,size),Image.Resampling.LANCZOS)

def vector(mono=False,adaptive=True):
    viewport=108 if adaptive else 48
    rows=[f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{viewport}dp" android:height="{viewport}dp" android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">']
    if adaptive:rows.append('  <group android:translateX="18" android:translateY="18" android:scaleX="1.5" android:scaleY="1.5">')
    if mono:
        g=geometry();holes=g[0][0]+g[2][0]+g[3][0]+smile_outline()
        rows.append(f'    <path android:fillColor="#FFFFFF" android:fillType="evenOdd" android:pathData="{data(holes)}"/>')
        rows.append(f'    <path android:fillColor="#FFFFFF" android:pathData="{data(g[1][0])}"/>')
    else:
        if not adaptive:rows.append(f'    <path android:fillColor="{TEAL}" android:pathData="{data(rounded(0,0,48,48,13))}"/>')
        for path,fill,stroke,width in geometry():
            rows.append(f'    <path android:fillColor="{fill or "#00000000"}" android:strokeColor="{stroke or "#00000000"}" android:strokeWidth="{width:.5f}" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{data(path)}"/>')
    if adaptive:rows.append('  </group>')
    return '\n'.join(rows+['</vector>'])+'\n'

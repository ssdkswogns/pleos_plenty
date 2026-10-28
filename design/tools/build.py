from pathlib import Path
import base64, json, html, re, math

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'screens'; OUT.mkdir(exist_ok=True)
C={'navy':'#002C5F','blue':'#377EAC','sky':'#93BEDA','ice':'#DCECF5','white':'#FFFFFF','pearl':'#F5F8FB','gray':'#D6E0E8','muted':'#566E83','ink':'#19364D','night':'#0E2134','nightSurface':'#183349','nightBorder':'#365064'}
NAMES=['달빛종','구름고사리','물결연꽃']
REGIONS=['강릉 지역','춘천 지역','양양 지역']
ICONS={
 'garden':'<path d="M4 12h16l-2 8H6zM12 12V5M12 8C5 8 5 3 5 3s7-1 7 5ZM12 8c0-5 7-5 7-5s0 5-7 5"/>',
 'book':'<path d="M12 5v15M12 5C9 2 3 3 3 3v15s6-1 9 2c3-3 9-2 9-2V3s-6-1-9 2Z"/>',
 'bag':'<path d="M5 8h14l1 13H4L5 8ZM8 8V6a4 4 0 0 1 8 0v2M8 13h8"/>',
 'route':'<path d="M5 18c0-6 14 2 14-5S5 11 5 5"/><circle cx="5" cy="4" r="2"/><circle cx="5" cy="20" r="2"/>',
 'settings':'<path d="m9 3 6 0 1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1Z"/><circle cx="12" cy="12" r="3"/>',
 'friend':'<circle cx="9" cy="7" r="3"/><path d="M3 20v-3a6 6 0 0 1 12 0v3M17 4a3 3 0 0 1 0 6M18 14c3 0 4 2 4 5"/>',
 'arrow':'<path d="M4 12h15m-6-6 6 6-6 6"/>',
 'back':'<path d="M20 12H5m6-6-6 6 6 6"/>',
 'check':'<path d="m5 12 4 4L20 5"/>',
 'plus':'<path d="M12 5v14M5 12h14"/>',
 'close':'<path d="m6 6 12 12M18 6 6 18"/>',
 'drop':'<path d="M12 2s-7 8-7 13a7 7 0 0 0 14 0c0-5-7-13-7-13Z"/><path d="M8 15c0 2 1 3 3 3"/>',
 'spark':'<path d="m12 2 3 7 7 3-7 3-3 7-3-7-7-3 7-3Z"/>',
 'stone':'<path d="m4 8 7-5 8 4 3 8-7 6-10-3-3-5ZM4 8l9 4 6-5M13 12l2 9"/>',
 'pin':'<path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/>',
 'lock':'<rect x="5" y="10" width="14" height="11" rx="3"/><path d="M8 10V6a4 4 0 0 1 8 0v4M12 14v3"/>',
 'moon':'<path d="M20 14A9 9 0 0 1 10 3a9 9 0 1 0 10 11Z"/>',
 'sound':'<path d="M3 9h4l5-5v16l-5-5H3ZM16 8a6 6 0 0 1 0 8M19 5a10 10 0 0 1 0 14"/>',
 'pause':'<path d="M8 5v14M16 5v14"/>',
 'bolt':'<path d="m13 2-8 12h6l-1 8 9-13h-6Z"/>',
 'info':'<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7v.2"/>',
 'cloud':'<path d="M6 18a5 5 0 1 1 0-10 6 6 0 0 1 11-2 6 6 0 1 1 2 12ZM4 3l16 18"/>',
 'seed':'<path d="M12 3C5 6 3 11 6 17c3 5 10 4 12-1 3-7-3-11-6-13ZM9 17c0-5 1-8 4-11"/>',
 'refresh':'<path d="M20 8a9 9 0 0 0-15-3L2 8m0-5v5h5M4 16a9 9 0 0 0 15 3l3-3m0 5v-5h-5"/>',
 'heart':'<path d="M12 21 3 12C-2 5 7 0 12 6c5-6 14-1 9 6Z"/>',
}

MANIFEST=[]
class SVG:
 def __init__(self,w=1440,h=900):
  self.w=w;self.h=h;self.a=[];self.uid=0
  self.a.append('<defs><linearGradient id="veil"><stop offset="0" stop-color="#F5F8FB" stop-opacity=".98"/><stop offset=".37" stop-color="#F5F8FB" stop-opacity=".82"/><stop offset=".72" stop-color="#F5F8FB" stop-opacity=".08"/></linearGradient><radialGradient id="halo"><stop stop-color="#FFFFFF" stop-opacity="1"/><stop offset="1" stop-color="#DCECF5" stop-opacity="0"/></radialGradient><filter id="soft" x="-30%" y="-30%" width="160%" height="180%"><feDropShadow dx="0" dy="10" stdDeviation="16" flood-color="#173D5A" flood-opacity=".09"/></filter></defs>')
 def raw(self,s):self.a.append(s)
 def rect(self,x,y,w,h,fill='white',r=0,stroke=None,sw=1,opacity=1,shadow=False):
  fill=C.get(fill,fill);stroke=C.get(stroke,stroke)
  self.a.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" opacity="{opacity}"'+(f' stroke="{stroke}" stroke-width="{sw}"' if stroke else '')+(' filter="url(#soft)"' if shadow else '')+'/>')
 def text(self,s,x,y,size=24,color='ink',weight=400,anchor='start',line=None,spacing=None):
  color=C.get(color,color);line=line or size*1.5
  self.a.append(f'<text x="{x}" y="{y+size}" font-family="Noto Sans KR, sans-serif" font-size="{size}" font-weight="{weight}" fill="{color}" text-anchor="{anchor}"'+(f' letter-spacing="{spacing}"' if spacing else '')+'>')
  for j,t in enumerate(str(s).split('\n')):self.a.append(f'<tspan x="{x}" dy="{0 if j==0 else line}">{html.escape(t)}</tspan>')
  self.a.append('</text>')
 def icon(self,k,x,y,size=28,color='ink',stroke=1.7):
  self.a.append(f'<svg x="{x}" y="{y}" width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" stroke="{C.get(color,color)}" stroke-width="{stroke}" stroke-linecap="round" stroke-linejoin="round">{ICONS[k]}</svg>')
 def image(self,name,x,y,w,h,opacity=1,fit='xMidYMid meet'):
  self.a.append(f'<image href="../assets/{name}.png" x="{x}" y="{y}" width="{w}" height="{h}" preserveAspectRatio="{fit}" opacity="{opacity}"/>')
 def plant(self,r,c,x,y,w,h=None,opacity=1,alt=False):
  h=h or w
  if alt:self.image('moonbell-starlight',x,y,w,h,opacity);return
  bounds=[(0,0,418,440),(418,0,418,440),(836,0,418,440),(0,440,418,410),(418,440,418,410),(836,440,418,410),(0,850,418,404),(418,850,418,404),(836,850,418,404)]
  sx,sy,sw,sh=bounds[r*3+c]
  self.a.append(f'<svg x="{x}" y="{y}" width="{w}" height="{h}" viewBox="{sx} {sy} {sw} {sh}" preserveAspectRatio="xMidYMid meet" overflow="hidden" opacity="{opacity}"><image href="../assets/plant-atlas.png" width="1254" height="1254"/></svg>')
 def pill(self,s,x,y,w=None,fill='white',color='navy',icon=None):
  w=w or (len(s)*18+40+(30 if icon else 0));self.rect(x,y,w,44,fill,22,opacity=.96)
  if icon:self.icon(icon,x+14,y+10,24,color)
  self.text(s,x+(46 if icon else 20),y+8,18,color,500)
 def button(self,s,x,y,w=260,kind='primary',icon='arrow',h=64,disabled=False):
  fill={'primary':'navy','secondary':'white','tonal':'ice','ghost':'none'}[kind]
  color='white' if kind=='primary' else 'navy'
  if disabled:fill='gray';color='muted'
  self.rect(x,y,w,h,fill,20,'gray' if kind=='secondary' else None)
  self.text(s,x+w/2-(14 if icon else 0),y+(h-28)/2-3,22,color,600,'middle')
  if icon:self.icon(icon,x+w-45,y+(h-26)/2,26,color)
 def progress(self,x,y,w,p=.6,night=False):
  self.rect(x,y,w,10,'nightBorder' if night else 'gray',5)
  self.rect(x,y,w*p,10,'sky' if night else 'blue',5)
 def panel(self,x,y,w,h,night=False):self.rect(x,y,w,h,'nightSurface' if night else 'white',28,'nightBorder' if night else 'gray',opacity=.96,shadow=True)
 def save(self,id,title,phase='MVP',notes=''):
  data=f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{self.w}" height="{self.h}" viewBox="0 0 {self.w} {self.h}"><title>{html.escape(title)}</title>'+''.join(self.a)+'</svg>'
  (OUT/f'{id}.svg').write_text(data)
  MANIFEST.append({'id':id,'title':title,'phase':phase,'width':self.w,'height':self.h,'svg':f'screens/{id}.svg','png':f'screens/{id}.png','notes':notes})

# All geometry is editable; plant art is the original shared atlas.
ICONS.update({
 'trait-waterside':'<path d="M3 14c2-2 4-2 6 0s4 2 6 0 4-2 6 0M3 19c2-2 4-2 6 0s4 2 6 0 4-2 6 0M12 3c-2 3-3 4-3 6a3 3 0 0 0 6 0c0-2-1-3-3-6Z"/>',
 'trait-highland':'<path d="m2 20 8-15 6 11 3-6 3 10H2ZM7 11l3 2 3-2"/>',
 'trait-city':'<path d="M3 21V8h7v13M10 21V3h10v18M2 21h20M6 11v1m0 4v1M14 7h2m-2 4h2m-2 4h2M14 21v-3h2v3"/>',
 'minus':'<path d="M5 12h14"/>',
})
for name in ['trait-waterside','trait-highland','trait-city','back','close','minus','plus']:
 (ROOT/'assets/icons'/f'{name}.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">{ICONS[name]}</svg>')
TRAITS=[('trait-waterside','물가','물가의','달빛이슬','drop'),('trait-highland','산자락','산자락의','바람돌','stone'),('trait-city','도시','도시의','구름빛','spark')]

def shell(title,sub='',tab=0,back=True):
 s=SVG();s.rect(0,0,1440,900,'pearl');s.icon('garden',164,34,32,'navy');s.text('Plenty',209,32,26,'navy',700)
 s.pill('주차 중',1216,30,176,'white','navy','check')
 s.rect(24,112,96,760,'white',32,shadow=True)
 for i,(ic,tx) in enumerate([('garden','정원'),('book','도감'),('bag','가방'),('route','여정')]):
  y=136+i*140
  if i==tab:s.rect(36,y,72,94,'navy',22)
  col='white' if i==tab else 'muted';s.icon(ic,58,y+17,28,col);s.text(tx,72,y+55,16,col,500,'middle')
 s.icon('settings',58,804,28,'muted')
 if back:
  s.rect(164,114,64,64,'white',20,'gray');s.icon('back',184,134,24,'navy')
 s.text(title,248 if back else 164,118,34,'navy',700)
 if sub:s.text(sub,164,195,21,'muted')
 return s

def badge(s,i,x,y,state='default',prefix=False):
 ic,n,p,_,_=TRAITS[i];w=150
 fill,col,stroke=('navy','white',None) if state=='selected' else ('pearl','muted','gray') if state=='disabled' else ('ice','navy',None)
 s.rect(x,y,w,44,fill,22,stroke);s.icon(ic,x+14,y+10,24,col);s.text(p if prefix else n,x+49,y+8,18,col,500)
 if state=='selected':s.icon('check',x+w-28,y+14,16,col)

def score(s,x,y,label,ic,v,after=None,w=450,maxv=30):
 s.icon(ic,x,y+1,24,'blue');s.text(label,x+38,y,20,'ink',500)
 s.text(f'{v}점' if after is None else f'{v} → {after}점',x+w,y,20,'navy',600,'end')
 s.rect(x,y+39,w,8,'gray',4)
 if v:s.rect(x,y+39,w*min(v/maxv,1),8,'sky',4)
 if after is not None and after>v:s.rect(x+w*min(v/maxv,1),y+39,w*(min(after/maxv,1)-min(v/maxv,1)),8,'blue',4)

def footer(s,text,button='정원으로',x=164):
 s.text(text,x,813,19,'muted');s.button(button,1092,793,300,icon=None)

# Component board
s=shell('지역의 분위기를 담는 세 가지 특성','아이콘 · 이름 앞말 · 재료 연결',back=False)
for i,(ic,n,p,m,mi) in enumerate(TRAITS):
 x=164+i*414;s.panel(x,250,394,494);s.rect(x+32,282,88,88,'ice',24);s.icon(ic,x+54,304,44,'navy')
 s.text(n,x+32,398,32,'navy',700);s.text(p+' 달빛종',x+32,449,23,'muted');badge(s,i,x+32,505);badge(s,i,x+210,505,'selected');s.icon(mi,x+32,590,28,'blue');s.text(m+' 1개 → +5점',x+76,589,21,'ink');s.text('색상과 함께 모양·텍스트로 구분',x+32,665,18,'muted')
s.text('24 × 24  /  1.7 stroke  /  currentColor',164,800,22,'blue',600);s.save('00-trait-system','지역 특성 아이콘과 배지')
# 10 details: active and mature
for mature in [False,True]:
 s=shell('물가의 달빛종' if mature else '물가의 달빛종 새싹','화분 02 · 성체 이름이 확정됐어요.' if mature else '화분 02 · 현재 이름과 다음 이름은 다를 수 있어요.')
 s.panel(164,250,470,596);s.rect(185,282,430,430,'url(#halo)',215);s.plant(0,2 if mature else 1,224,304,350,350)
 badge(s,0,324,720,prefix=True);s.text('성체 · 누적 128 XP' if mature else '새싹 · 누적 37 XP',399,779,21,'muted',500,'middle')
 s.panel(666,250,726,596)
 if mature:
  s.pill('성장이 완료됐어요',706,284,230,'ice',icon='check');s.text('여정이 이름이 되었어요',706,354,30,'navy',700)
  s.text('성체의 이름 앞말은 이제 바뀌지 않아요.\n함께 떠나면 경험치는 계속 쌓여요.',706,420,23,'muted',line=38)
  s.rect(706,536,646,116,'pearl',22);s.text('확정된 이름',734,554,18,'muted');s.text('물가의 달빛종',734,590,28,'navy',600)
  s.button('동반자 선택으로',706,742,646)
 else:
  s.text('성체까지 63 XP',706,284,26,'navy',600);s.text('37 / 100 XP',1352,288,20,'muted',anchor='end');s.progress(706,337,646,.37)
  s.text('이번 단계의 특성 점수',706,382,22,'ink',600)
  for i,v in enumerate([12,9,1]):score(s,706,427+i*72,TRAITS[i][1],TRAITS[i][0],v,w=646,maxv=20)
  s.text('현재 점수로 예상하는 이름',706,660,18,'muted');s.text('물가의 달빛종',706,692,27,'navy',600)
  s.button('특성 가꾸기',706,755,390);s.button('동반자 선택',1112,755,240,'secondary',icon=None)
 s.save('10b-plant-mature' if mature else '10-plant-detail','식물 상세 · 성체' if mature else '식물 상세 · 새싹')
# 11 planting confirmation
for full in [False,True]:
 s=shell('말랑을 심을까요?','말랑 1개를 빈 화분으로 옮겨요. 다른 재료는 필요하지 않아요.',tab=2)
 s.panel(164,250,470,596);s.plant(2,0,224,304,350,350);s.text('물결연꽃 말랑',399,704,30,'navy',700,'middle');s.text('가방에 2개',399,756,22,'muted',anchor='middle')
 s.panel(666,250,726,596);s.text('심을 화분',706,286,27,'navy',700);s.text('첫 빈 화분을 미리 골랐어요. 다른 빈 칸도 선택할 수 있어요.',706,334,19,'muted')
 for i in range(6):
  x=706+(i%3)*218;y=391+(i//3)*124;occupied=full or i<2;selected=i==2 and not full
  s.rect(x,y,202,108,'ice' if selected else 'pearl',20,'navy' if selected else 'gray',2 if selected else 1)
  s.text(f'{i+1:02}',x+16,y+12,18,'muted');s.icon('garden' if occupied else 'check' if selected else 'plus',x+83,y+25,32,'navy' if selected else 'muted');s.text('사용 중' if occupied else '선택됨' if selected else '빈 화분',x+101,y+71,17,'muted',anchor='middle')
 s.text('빈 화분이 없어요. 말랑은 가방에 그대로 있어요.' if full else '물결연꽃 말랑  2 → 1개  ·  재료 사용 없음',706,657,21,'navy',500)
 s.text('새 화분을 사용할 수 있을 때 심어 주세요.' if full else '심은 뒤 동반자로 선택하면 여정에서 자라요.',706,701,19,'muted')
 s.button('가방으로',706,759,250,'secondary',icon=None);s.button('심을 화분 없음' if full else '03 화분에 심기',976,759,376,disabled=full)
 s.save('11b-plant-no-slot' if full else '11-plant-confirm','말랑 심기 · 빈 화분 없음' if full else '말랑 심기 확인')
# 12 materials
s=shell('특성 가꾸기','물가의 달빛종 새싹 · 재료는 이름 앞말에 영향을 주고, 경험치는 올리지 않아요.')
s.panel(164,250,552,596);s.text('어떤 재료를 줄까요?',204,282,27,'navy',700)
for i,(_,n,_,m,ic) in enumerate(TRAITS):
 y=343+i*113;sel=i==1;s.rect(204,y,472,94,'ice' if sel else 'pearl',20,'navy' if sel else 'gray',2 if sel else 1);s.icon(ic,226,y+29,32,'blue');s.text(m,280,y+14,23,'navy',600);s.text(n+' +5점 / 1개',280,y+51,18,'muted');s.text(['12개','4개','8개'][i],650,y+31,22,'navy',600,'end')
s.text('사용할 수량',204,711,21,'ink',500);s.rect(446,693,64,64,'pearl',20,'gray');s.icon('minus',466,713,24,'#9BAEBE');s.text('1',548,705,28,'navy',600,'middle');s.rect(582,693,64,64,'white',20,'gray');s.icon('plus',602,713,24)
s.text('바람돌 4개 중 1개 사용',204,793,19,'muted')
s.panel(748,250,644,596);s.text('사용 후 달라지는 점',788,282,27,'navy',700)
for i,v in enumerate([12,9,1]):score(s,788,350+i*78,TRAITS[i][1],TRAITS[i][0],v,v+5 if i==1 else v,w=564,maxv=20)
s.rect(788,599,564,119,'ice',22);s.text('다음 단계 예상 이름',812,617,18,'muted');s.text('산자락의 달빛종',812,652,29,'navy',700)
s.text('이후 여정에 따라 달라져요. 성체가 될 때 확정돼요.',788,731,18,'muted');s.button('바람돌 1개 주기',788,771,564)
s.save('12-trait-tuning','특성 가꾸기 · 점수와 예상 이름')
# 14 material sheet
s=shell('여행 가방','재료를 누르면 용도와 연결 특성을 볼 수 있어요.',tab=2,back=False)
for i,(_,n,_,m,ic) in enumerate(TRAITS):
 y=270+i*164;s.panel(164,y,550,140);s.icon(ic,200,y+42,48,'blue');s.text(m,278,y+30,28,'navy',600);s.text(n+' 특성을 가꿔요',278,y+78,21,'muted')
s.rect(0,0,1440,900,'navy',opacity=.22);s.rect(768,24,648,852,'white',28,'gray',shadow=True);s.rect(1328,48,64,64,'pearl',20);s.icon('close',1348,68,24,'navy');s.rect(808,142,100,100,'ice',28);s.icon('stone',836,170,44,'blue');s.text('바람돌',808,278,38,'navy',700);s.pill('보유 4개',1230,284,138,'ice');badge(s,1,808,353)
s.text('산의 바람을 품은 작은 돌',808,435,27,'navy',600);s.text('씨앗이나 새싹에게 1개를 주면\n산자락 특성이 5점 높아져요.',808,492,24,'muted',line=40)
s.rect(808,610,568,116,'pearl',22);s.text('이름은 다음 성장 단계에서 정해져요.\n경험치와 모습은 바뀌지 않아요.',832,635,21,'muted',line=35)
s.button('확인',808,774,568,icon=None);s.save('14a-material-sheet','재료 설명 · 오른쪽 시트')
# collection and read-only species
s=shell('우리의 작은 식물도감','만난 식물 2 / 3 · 화분에 심으면 도감에 기록돼요.',tab=1,back=False)
for r in range(3):
 x=164+r*414;s.panel(x,258,394,460);s.plant(r,2 if r!=1 else 1,x+72,289,250,250,.18 if r==2 else 1)
 if r==2:s.icon('lock',x+177,394,40,'navy')
 s.text('아직 만나지 못했어요' if r==2 else NAMES[r],x+30,572,27,'navy',700)
 s.text(['성체 · 누적 128 XP','새싹 · 37 / 100 XP','말랑을 심어 첫 만남을 기록해요'][r],x+30,618,19,'muted')
 if r<2:s.progress(x+30,680,334,1 if r==0 else .37)
s.text('가장 많이 자란 식물을 기준으로 보여요. 이름 앞말은 종 상세에서 확인해요.',164,785,21,'muted');s.save('15-collection','도감 · 단계와 XP')
s=shell('달빛종','식물도감 / 종 상세 · 읽기 전용',tab=1)
s.panel(164,250,410,596);s.plant(0,2,194,299,350,350);s.text('달빛처럼 조용히 피는 작은 종',369,708,24,'navy',600,'middle');s.text('만난 종 · 정원에 1개',369,757,20,'muted',anchor='middle')
s.panel(606,250,786,596);s.text('함께 알아가는 세 가지 모습',646,284,28,'navy',700)
for i,n in enumerate(['말랑','새싹','성체']):
 x=646+i*242;s.rect(x,346,220,225,'pearl',20);s.plant(0,i,x+30,356,160,160);s.text(n,x+110,526,20,'navy',600,'middle')
s.text('정원에서 가장 많이 자란 식물',646,612,20,'muted');badge(s,0,646,655,prefix=True);s.text('물가의 달빛종',818,656,28,'navy',600);s.text('성체 · 누적 128 XP',646,722,23,'ink');s.text('이곳에서는 재료 사용이나 동반자 변경을 할 수 없어요.',646,786,19,'muted');s.save('15a-species-detail','도감 종 상세 · 읽기 전용')
# Journey aligned companion and ready screens
s=shell('이번 여정의 동반자를 골라요','1~3개를 선택해요. 함께 떠난 식물만 거리 경험치를 받아요.')
for r in range(3):
 x=164+r*414;s.panel(x,258,394,420)
 if r<2:s.rect(x,258,394,420,'none',28,'blue',2);s.rect(x+326,280,40,40,'navy',20);s.icon('check',x+335,289,22,'white')
 s.plant(r,1 if r==0 else 2 if r==1 else 0,x+72,294,250,240);s.text(['물가의 달빛종 새싹','산자락의 구름고사리','물결연꽃 말랑'][r],x+28,565,25,'navy',600);s.text(['새싹 · 37 / 100 XP','성체 · 누적 128 XP','말랑 · 0 / 20 XP'][r],x+28,617,20,'muted')
s.panel(164,720,1228,126);s.text('선택한 동반자  2 / 3',204,762,27,'navy',600);s.button('이 식물들과 함께',1012,750,340);s.save('05-companions','동반자 선택 · 단계와 XP')
s=shell('여정 준비를 마쳤어요','출발하면 선택한 동반자들과 함께 거리를 기록해요.')
s.panel(164,250,688,596)
for i in range(2):
 y=292+i*182;s.plant(i,1 if i==0 else 2,200,y,160,160);s.text(['물가의 달빛종 새싹','산자락의 구름고사리'][i],385,y+35,28,'navy',600);s.text('이번 여정의 동반자',385,y+89,21,'muted')
s.text('출발 전까지 동반자를 바꿀 수 있어요.',204,744,22,'muted');s.panel(884,250,508,596);s.icon('route',924,290,40,'blue');s.text('주행이 시작되면',924,370,31,'navy',700);s.text('게임 화면은 자동으로 숨겨져요.\n주차 후 여정 결과를 확인해요.',924,432,23,'muted',line=39);s.pill('선택된 동반자 2개',924,583,260,'ice',icon='check');s.button('동반자 바꾸기',924,730,428,'secondary',icon='back');s.save('06-journey-ready','출발 대기 · 자동 출발')
# result sample internally consistent: sprout37+10=47, mature128+10=138
s=shell('함께한 만큼 자랐어요','도착 지역 · 강릉',tab=3);badge(s,0,1174,190)
s.panel(164,250,374,412);s.icon('route',204,290,40,'blue');s.text('함께한 거리',204,365,23,'muted');s.text('10.0',200,412,76,'navy',700);s.text('km',435,465,26,'muted');s.text('동반자마다 +10 XP',204,584,25,'blue',600)
for i in range(2):
 x=566+i*426;s.panel(x,250,400,412);s.plant(i,1 if i==0 else 2,x+98,272,205,205);s.text(['물가의 달빛종 새싹','산자락의 구름고사리'][i],x+26,494,24,'navy',600);s.text(['37 → 47 XP','128 → 138 XP'][i],x+26,541,27,'blue',600);s.text('물가 특성 +10점' if i==0 else '성체 · 이름은 그대로 유지돼요',x+26,602,19,'muted')
s.panel(164,694,1228,152);s.icon('seed',200,731,36,'blue');s.text('물결연꽃 말랑 1개 · 달빛이슬 1개',256,723,26,'navy',600);s.text('보상은 이미 가방에 담겼어요.',256,772,21,'muted');s.button('말랑 만나기',1052,737,300);s.save('08-journey-result','여정 결과 · 저장된 보상 확인')
s=shell('오늘의 새로운 만남','여정 결과 / 발견한 말랑',tab=3);s.rect(430,230,700,500,'url(#halo)',250);s.plant(2,0,625,251,310,310);s.text('물결연꽃 말랑',778,579,42,'navy',700,'middle');s.text('작은 화분에서 함께 자랄 준비가 됐어요.',778,650,24,'muted',anchor='middle');s.pill('가방에 보관 중 · 1개',632,705,294,'ice',icon='bag');s.button('심을 화분 고르기',450,789,370);s.button('나중에 심기',844,789,270,'secondary',icon=None);s.save('09-seed-discovery','말랑 발견 · 이미 지급된 아이템')
s=shell('새로운 모습으로 자랐어요','여정에서 얻은 경험치로 자동 성장했어요.',tab=3);s.plant(0,2,610,245,350,350);s.pill('새싹 → 성체',665,611,240,'ice',icon='spark');s.text('물가의 달빛종',790,682,39,'navy',700,'middle');s.text('이번 단계의 여정이 이름에 담겼어요.',790,745,23,'muted',anchor='middle');s.button('여정 결과로',610,805,360);s.save('13-growth-complete','자동 성장 결과 · 원화는 종류와 단계로만 변경')
# Edge cases board: concrete messages and button states
s=shell('흐름이 끊기지 않는 상태 처리','구현용 상태별 문구 · 모두 주차 중에만 표시',back=False)
states=[('점수 동점','물가 14 · 산자락 14 · 도시 0','예상: 물가의 달빛종','같은 점수면 물가 → 산자락 → 도시 순서'),('아직 지역 점수 없음','물가 0 · 산자락 0 · 도시 0','예상: 달빛종','0점이면 이름 앞말을 붙이지 않아요.'),('재료가 없어요','바람돌 0개 · 수량 선택과 사용 비활성','여정에서 재료를 모아 주세요.','확인만으로 재료를 사용하지 않아요.'),('거리 확인 중','차량의 주행거리를 기다리고 있어요.','결과를 확인하면 알려 드릴게요.','0 km나 보상 없음으로 확정하지 않아요.'),('도착 지역 미확인','거리 경험치는 정상 반영됐어요.','지역 점수와 지역 보상은 없어요.','정확한 위치나 획득 지역을 추측하지 않아요.'),('주행 제한 · 신호 미확인','게임 표시와 입력을 중단해요.','작성 중 선택은 보관 · 사용은 하지 않음','주차 복귀 후 재고와 단계부터 다시 확인')]
for i,(tt,a,b,c) in enumerate(states):
 x=164+(i%2)*628;y=250+(i//2)*200;s.panel(x,y,600,178);s.text(tt,x+28,y+20,25,'navy',700);s.text(a,x+28,y+65,20,'ink');s.text(b,x+28,y+101,20,'blue');s.text(c,x+28,y+138,16,'muted')
s.save('S01-states','특성·보상·제한 예외 상태')
# Navigation rule board with exact positions
s=shell('화면 안에서 돌아가는 방법','상세와 확인 화면은 제목 왼쪽에 뒤로가기를 고정해요.')
s.panel(164,260,1228,558);s.text('뒤로가기: 64 × 64  ·  아이콘: 24 × 24',204,301,29,'navy',700)
for i,(a,b) in enumerate([('10 식물 상세 → 정원','12 특성 가꾸기 → 10 식물 상세'),('11 심기 확인 → 호출한 화면','15a 종 상세 → 도감 목록'),('05 동반자 선택 → 정원','06 출발 대기 → 05 (주행 시작 전)'),('08 여정 결과 → 여정 목록','09 말랑 발견 → 08 (지급은 취소되지 않음)')]):
 y=386+i*77;s.icon('back',204,y+3,25,'blue');s.text(a,248,y,23,'ink');s.text(b,788,y,23,'ink')
s.text('시트는 오른쪽 위 닫기 · 뒤로가기는 저장/심기/재료 사용을 실행하지 않아요.',204,741,22,'muted');s.save('S02-navigation','내비게이션과 뒤로가기')
(ROOT/'screen-manifest.json').write_text(json.dumps(MANIFEST,ensure_ascii=False,indent=2))
# Self-contained index references files rather than embedding heavyweight art.
cards=''.join(f'<article><a href="{m["svg"]}"><img loading="lazy" src="{m["png"]}" alt="{m["title"]}"></a><h2>{m["id"]} · {m["title"]}</h2><a href="{m["svg"]}">SVG</a> · <a href="{m["png"]}">PNG</a></article>' for m in MANIFEST)
(ROOT/'index.html').write_text('<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Plenty · 성장 규칙 v2 디자인</title><style>body{margin:40px;background:#f5f8fb;color:#002c5f;font:16px sans-serif}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(540px,1fr));gap:28px}article{padding:16px;background:white;border:1px solid #d6e0e8;border-radius:24px}img{width:100%;border-radius:12px}h2{font-size:18px}a{color:#377eac}@media(max-width:650px){main{display:block}article{margin-bottom:24px}body{margin:16px}}</style><h1>Plenty · 작은 여정, 자라는 이름</h1><p>2026.10.28 · 새 성장 규칙 · 화면의 값은 각 상태를 설명하는 예시 데이터입니다.</p><p><a href="README.md">개발 인계 문서</a> · <a href="spec/DECISIONS.md">기획 결정</a></p><main>'+cards+'</main></html>')
print(f'Generated {len(MANIFEST)} SVG screens')
(ROOT/'assets/badges').mkdir(exist_ok=True)
for i,(ic,n,p,_,_) in enumerate(TRAITS):
 for state in ['default','selected','disabled','prefix']:
  b=SVG(150,44);badge(b,i,0,0,'default' if state=='prefix' else state,prefix=state=='prefix')
  data=f'<svg xmlns="http://www.w3.org/2000/svg" width="150" height="44" viewBox="0 0 150 44"><title>{n} 배지 {state}</title>'+''.join(b.a)+'</svg>'
  (ROOT/'assets/badges'/f'{ic[6:]}-{state}.svg').write_text(data)

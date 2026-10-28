# 디자인 재생성

`python3 design/tools/build.py`는 편집 가능한 SVG와 manifest를 만듭니다.
`npm install --no-save sharp` 후 `node design/tools/render.cjs`로 PNG를 만듭니다.
Noto Sans KR가 설치된 환경에서 렌더링하세요. 앱의 `app/src/main/res/font/noto_sans_kr.ttf`와 같은 글꼴입니다.
PNG는 검토용이며 앱 화면을 한 장의 이미지로 구현하지 않습니다.

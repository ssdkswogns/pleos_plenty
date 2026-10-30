# PROJECT_STATUS

마지막 갱신: 2026-10-30 · 전체 계획: 기획 폴더의 `plenty-dev-plan.md`

## 현재 상태

- **T01 완료**: `:app`(Compose) + `:core`(순수 Kotlin) 골격, 버전 카탈로그, 디자인 토큰, Noto Sans KR, 아이콘, 내비게이션 레일과 빈 화면.
- **T02 완료**: 디버그 전용 `VehicleProbeActivity`로 실측. 결과는 `API_FACTS.md`.
  - 누적 주행거리(km)는 읽기·구독·재시작 후 재조회 모두 동작. 트립 거리는 에뮬레이터 미지원.
  - 기어 D(정차 포함)부터 플랫폼이 비 DO 앱을 차단 화면으로 덮음. Activity는 멈추지 않으므로 앱이 UX 신호로 직접 일시정지해야 함.
  - FusedLocation 지원, `adb emu geo fix`로 위치 변경 가능.
  - Virtual Console HTTP API로 주행 시나리오 자동화 (`tools/vehicle-sim.sh`).
- **경로 결정: B(도착지 수집 + 누적 거리 차이) 기본선**, A(이동 중 지역 수집)는 "주행 중 화면 숨김 상태의 기록 허용" 답변 후 확장.
- **03 정원 홈 구현 (T06에서 앞당김, 목업 데이터)**: 배경+베일, 헤더 상태·재료 칩, 문구, 카트 원화에 슬롯 앵커 공식으로 식물 합성, 6칸 화분 패널, 여정 준비 버튼. 공통 컴포넌트 `GroveButton`(Primary/Secondary/Tonal·눌림·비활성), `StatusChip`, `PotSlot`, `PlantImage`(아틀라스 sourceRect), `TravelerCart`, `WorldBackdrop`. 화면 읽기 라벨 확인. 탭: 채운 화분→식물 상세(자리표시자), 빈 화분→가방, 여정 준비→동반자 선택(자리표시자).
  - 데이터는 `MockGardenState`(시안 값). "충전 중 · 64%"도 목업이며 실제 차량 값 아님. T05 저장소가 생기면 `GardenUiState`를 저장소에서 채운다.
  - 아직 없음: 세로/좁은 창 배치(P01), 밤 테마(24), 카트 상하 흔들림 모션, 화분 선택 상태 사용처.
- **디자인 v2 반영 (식물 관리 묶음)**: `design/` 결정판에 따라 콘텐츠 확정(특성 표시명 추가), 아이콘(빼기·특성 3종) 추가, 화면 구현:
  - 10/10b 식물 상세(현재 단계 특성 점수 막대·예상 이름·성체 확정), 12 특성 가꾸기(메모리 미리보기, 확정 시에만 재료 사용), 11/11b 심기 확인(빈 화분 직접 선택, 정원 빈 화분에서 오면 그 칸 우선), 14 가방 문구·연결(말랑→11, 재료→14a 시트), 15 도감(종 이름 제목, 대표 개체 규칙), 15a 종 상세(읽기 전용), 상세 화면 뒤로가기, 알림(3초, 화면 읽기 도구 전달).
  - 공통 컴포넌트: `DetailHeader`, `TraitBadge`, `TraitScoreRows`, `QuantityStepper`, `ToastHost`, `MaterialSheet`.
  - 에뮬레이터 확인: 재료 사용(30→35점, 알림, 이름·XP 불변), 심기(04 우선 선택 → 카트 앞줄 4번, 알림), 시트 시스템 뒤로가기, 레일 복귀.
  - 임시 문구: 구름고사리·물결연꽃 종 소개, 달빛이슬·구름빛 재료 소개(디자인에 예시 없음) — 기획 확인 필요.
- **T05 완료 (저장과 정산)**: Room DB(`plants`·`inventory`·`journeys`·`journey_growth`·`reward_ledger`·`daily_xp`·`region_claims`, 스키마는 `app/schemas`). `GardenRepository`의 모든 지급은 한 트랜잭션에서 "원장 확인 → 계산(`:core` `SettlementPlanner`) → 상태·원장 저장". 원장 키 유일 제약으로 재정산·재시작·추첨 직후 종료에도 1회 지급. 시작 선물 말랑 1개(1회). Robolectric 저장소 테스트 10개(같은 여정 2회, DB 재오픈, 추첨 고정, 일일 상한·지역 1일 1회, 미확인/무효 거리, 심기 제약, 재료 보정).
  - 홈·가방·도감이 저장소 상태를 표시 (`PlentyViewModel` → 화면별 UI 상태). 목업 데이터 제거.
- **14 가방 / 15 도감 구현**: 가방은 전체·말랑·재료 탭, 말랑 카드 "심기"(첫 빈 화분에 심고 정원으로), 화분이 차면 비활성. 재료 카드는 연결 특성 설명. 도감은 종별 가장 많이 자란 개체(이름 앞말 포함)와 단계 진행 막대, 미발견은 흐린 성체 그림+자물쇠. "만남" 기준은 화분에 심은 적 있음.
- **디버그 데모 명령** (debug 빌드만): `adb shell am broadcast --user 10 -n app.plenty/.debug.DebugCommandReceiver -a app.plenty.debug.SEED_DEMO` (초기화 후 말랑 3종 심기 + 지난 3일 MOCK 여정 3회 → 성체 2·새싹 1, 재료 1씩), `-a app.plenty.debug.RESET` (초기화).
- **정원 홈 모션 (plenty-motion-kit)**: 망토를 분리한 `traveler_base` + `traveler_cape`(기존 `traveler_cart.png` 대체), `drawBitmapMesh`로 망토 펄럭임(1.5초)·고개(6초)·어깨 숨결(3초), 카트 상하·미세 회전, 식물 흔들림. 배경 드리프트(4% 여유, 24초)·구름·안개·반짝임·전경 식물, 베일을 옅게 조정. 인터랙션: 카트 위 식물 탭 → 통통 튐, 그 밖 탭 → 잠깐 강한 바람.
  - 모션 게이트: `DrivingRestrictionSource`(CarUxRestrictions) ALLOWED + 화면 STARTED 이상 + 시스템 애니메이션 켜짐. 아니면 정지 합성. **실측: 주행 중 5초 동안 Plenty 렌더 프레임 0, 주차 복귀 후 5초 312프레임.**
  - 성능(에뮬레이터): release 빌드 cold start 0.7–1.2초, 애니메이션 약 55fps·지연 7.8%. debug 빌드는 시작 5초대·지연 20%대(디버그 특성). 남은 지연은 대부분 GPU 명령 제출(에뮬레이터 그래픽 번역).
  - 원화는 백그라운드 디코딩 후 0.22초 페이드. 투명 레이어는 draw 호출 alpha로 그림(그래픽 레이어 alpha는 매 프레임 오프스크린 렌더 → 프레임 반토막).
  - 시안과 다른 점: 전경 식물을 카트 **뒤**에 그림(scene.js는 앞). 화면 레이아웃상 전경이 배경 레이어에 있기 때문. 앱 내 "움직임 줄이기" 설정은 17 설정 화면 구현 시 게이트에 추가.
- **T03 완료**: `GAME_RULES.md` v1. 기획 결정(10/28): 말랑은 종류가 정해진 채 수집 → 화분에 심기(재료 없음) → 동반자(최대 3)로 주행 XP → 20 XP 새싹, 100 XP 성체(자동). 각 단계 동안 쌓인 **지역 특성 점수**가 새싹·성체 이름 앞말을 정함(그림은 동일). 재료는 특성 점수 보정. 특성 종류·앞말·지역은 기획 확정 전까지 임시 3종.
- **T04 완료**: `:core`에 `GameContent`(콘텐츠·수치), `JourneyDistance`(누적 거리 차이 → 유효/미확인/무효), `GrowthEngine`(XP·일일 상한·소수 잔여·단계 경계 점수 분배·이름 앞말·재료 보정), `RegionRewardRules`(1일 1회·첫 방문 말랑 보장·30%). 단위 테스트 23개 통과.
- **여정 묶음 완료 (05·06·08·09·13 + T08/T09, 10/30)**: 규칙은 `GAME_RULES.md` §8.
  - 차량 신호: `GearSource`(표준 `GEAR_SELECTION`, `CAR_POWERTRAIN`) + `OdometerSource`(Pleos Vehicle SDK, `CAR_DRIVE`) → `JourneyCoordinator`(앱 프로세스 단위, 출발 1.5초·도착 3초 debounce, 거리 확인 중 10초 재시도, 재시작 시 정산). Vehicle SDK는 이제 release에도 포함.
  - 저장: DB v2(`ready_selection` 테이블, 여정 `arrivedAt`·`resultSeenAt`, 성장 기록에 XP 전후·이름 앞말·특성 점수 스냅샷). v1의 끝난 여정은 이전 시 "본 것"으로 표시.
  - 화면: 05 동반자 선택(최대 3, 6개면 2줄), 06 출발 대기, 08 결과(저장값만, 개체별 XP·상한·성체 안내, 미확인/당일 재방문 문구, 확인 중·무효 상태), 09 발견, 13 성장 완료(여러 개면 순서대로·건너뛰기), 여정 탭(준비 상태 + 기록 목록 → 08). 홈 "여정 준비"는 준비가 있으면 06으로.
  - 테스트: 저장소 여정 5개, 이전 1개, 조정기 5개(P→D→P 정산, 주행 중 시작은 출발 아님, 짧은 이탈 무시, 재시작 정산, 주행거리 실패 재시도). 전체 앱 테스트 21개 + core 27개.
  - 에뮬레이터 확인: 60 km(강릉 대체) → 자동 08 → 13(새싹→성체) → 09 → 11 → 정원 강조, 10 km(지역 미확인·일일 상한 +0), 주행 중 앱 강제 종료 → 주차 → 재실행 정산.
  - 데모 기록(MOCK)은 결과 화면에 "데모 기록" 표시, 자동으로 열리지 않음.
- 내비게이션 규칙: 하위 화면(식물 상세·동반자 선택)은 소속 탭(정원)을 레일에서 선택 상태로 유지하고, 선택된 탭을 다시 누르면 그 탭의 첫 화면으로 돌아간다. Pleos 화면에는 시스템 뒤로가기 버튼이 보이지 않으므로 레일이 유일한 복귀 수단이다(화면 내 뒤로가기 버튼은 디자인 검토 대기).

## 막힌 점 / 확인 대기

- 팀명(APK 파일명), 콘솔 카테고리 — 기획 담당 결정 대기
- 종 소개(구름고사리·물결연꽃)·재료 소개(달빛이슬·구름빛) 문구 확정 — 지금은 개발 임시 문구
- 주행 중 게임 화면을 숨긴 상태의 위치·거리 기록 허용 여부 (경로 A 조건)
- 에뮬레이터 내비게이션 지도 미표시(지도 서버 연결 실패) — Plenty와 무관, 운영진 문의 목록에 추가
- applicationId `app.plenty`는 임시값
- 저장소 공개 범위: 현재 공개로 보임. 대회 전 비공개 전환 여부는 팀 결정 (비공개 시 심사용 계정 Collaborator 초대)

## 다음 티켓

1. **T10** 지역 판정: FusedLocation 도착 위치 → 행정 영역 매핑, 미확인/지원 지역 밖 구분. `ArrivalRegionSource`를 교체한다. 08의 "지원 지역 밖" 문구도 이때 추가
2. 17 설정(움직임 줄이기 포함), 01/02 첫 시작, 세로·좁은 창 배치
3. 앱이 주행 시작 전에 종료돼 있으면 출발을 놓친다(현재는 앱 프로세스가 살아 있어야 함). 필요하면 부팅 수신/서비스 검토

## 작업 메모

- 빌드: `.\gradlew.bat :core:test :app:testDebugUnitTest :app:assembleDebug` (이 셸은 `NoDefaultCurrentDirectoryInExePath=1`이라 `.\` 필요)
- 설치·실행: `adb install -r app\build\outputs\apk\debug\app-debug.apk` → `adb shell am start --user 10 -n app.plenty/.MainActivity`
- 프로브: `adb shell am start --user 10 -n app.plenty/.debug.VehicleProbeActivity`, 로그 `adb logcat -s PlentyProbe`
- 차량 상태: `bash tools/vehicle-sim.sh park|drive|speed 60|odometer 12345|soc 80|location 37.5665 126.9780|state` (콘솔 세션 heartbeat를 스크립트가 보냄. 주행거리는 늘리기만 가능)
- 여정 시연: 05에서 동반자 선택 → `drive` → `odometer <현재+60>` → `park` → 3초 뒤 08 자동 표시. 도착 지역(T10 전 임시): `adb shell am broadcast --user 10 -n app.plenty/.debug.DebugCommandReceiver -a app.plenty.debug.ARRIVAL_REGION --es region gangneung` (프로세스 종료 시 해제, `--es` 없이 보내면 미확인)
- 여정 로그: `adb logcat -s JourneyCoordinator`
- Virtual Console 열기: `tools\open-virtual-console.cmd` 더블클릭 (에뮬레이터 재시작 시 끊기는 8088 포트 연결을 다시 만들고 브라우저를 엶)
- 캡처: `adb exec-out screencap -p > screen.png` (cmd/bash). cold start 직후 캡처는 첫 프레임 전일 수 있음
- 화면 텍스트 확인: Git Bash에서 `MSYS_NO_PATHCONV=1` 후 `adb shell uiautomator dump --display 0 /sdcard/ui.xml && adb shell cat /sdcard/ui.xml` (Compose 접근성 라벨이 보임). 정원 홈은 애니메이션 때문에 idle이 오지 않아 dump가 실패하므로 캡처로 확인
- 저장소 테스트: `.\gradlew.bat :app:testDebugUnitTest` (Robolectric, 첫 실행 시 android-all jar 다운로드)
- 원화는 아직 PNG(약 4.3MB). WebP 변환은 마무리 단계에서
- 아이콘은 디자인 키트 SVG에서 생성한 파일이다. 손으로 고치지 말고 다시 생성한다

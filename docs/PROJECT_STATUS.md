# PROJECT_STATUS

마지막 갱신: 2026-10-28 · 전체 계획: 기획 폴더의 `plenty-dev-plan.md`

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
- 내비게이션 규칙: 하위 화면(식물 상세·동반자 선택)은 소속 탭(정원)을 레일에서 선택 상태로 유지하고, 선택된 탭을 다시 누르면 그 탭의 첫 화면으로 돌아간다. Pleos 화면에는 시스템 뒤로가기 버튼이 보이지 않으므로 레일이 유일한 복귀 수단이다(화면 내 뒤로가기 버튼은 디자인 검토 대기).

## 막힌 점 / 확인 대기

- 팀명(APK 파일명), 콘솔 카테고리, 성장 단계 정의(★) — 기획 담당 결정 대기
- 주행 중 게임 화면을 숨긴 상태의 위치·거리 기록 허용 여부 (경로 A 조건)
- 에뮬레이터 내비게이션 지도 미표시(지도 서버 연결 실패) — Plenty와 무관, 운영진 문의 목록에 추가
- applicationId `app.plenty`는 임시값
- git 첫 커밋 대기 (커밋에 쓸 이름·이메일 미정)

## 다음 티켓

1. **T03/T04** `GAME_RULES.md` 확정, `:core` GameEngine과 경계값 테스트
2. **T05** Room 저장과 정산 트랜잭션

## 작업 메모

- 빌드: `.\gradlew.bat :core:test :app:testDebugUnitTest :app:assembleDebug` (이 셸은 `NoDefaultCurrentDirectoryInExePath=1`이라 `.\` 필요)
- 설치·실행: `adb install -r app\build\outputs\apk\debug\app-debug.apk` → `adb shell am start --user 10 -n app.plenty/.MainActivity`
- 프로브: `adb shell am start --user 10 -n app.plenty/.debug.VehicleProbeActivity`, 로그 `adb logcat -s PlentyProbe`
- 차량 상태: `bash tools/vehicle-sim.sh park|drive|speed 60|odometer 12345|soc 80|location 37.5665 126.9780|state`
- 캡처: `adb exec-out screencap -p > screen.png` (cmd/bash). cold start 직후 캡처는 첫 프레임 전일 수 있음
- 화면 텍스트 확인: Git Bash에서 `MSYS_NO_PATHCONV=1` 후 `adb shell uiautomator dump --display 0 /sdcard/ui.xml && adb shell cat /sdcard/ui.xml` (Compose 접근성 라벨이 보임)
- 원화는 아직 PNG(약 4.3MB). WebP 변환은 마무리 단계에서
- 아이콘은 디자인 키트 SVG에서 생성한 파일이다. 손으로 고치지 말고 다시 생성한다

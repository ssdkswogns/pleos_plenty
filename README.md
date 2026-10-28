# Plenty

**작은 말랑, 커다란 여정.** 실제 여정에서 모은 씨앗 친구 "말랑"을 주차·충전 중에 깨우고 키워, 푸른 망토 아이의 카트 정원을 가꾸는 Pleos Connect용 수집·성장 게임입니다.

> 개발 중입니다(2026-10-28 기준 앱 골격 단계). 아래 항목은 제출 전에 갱신합니다.

## 실행 방법

1. Pleos Connect Emulator v2.1.0(API 34) AVD를 실행합니다.
2. `apk/` 폴더의 APK를 설치합니다: `adb install -r apk/<파일명>.apk`
3. 차량 런처에서 **Plenty**를 실행합니다. adb로 실행할 때는 `adb shell am start --user 10 -n app.plenty/.MainActivity`

## 테스트 환경과 필요한 설정

| 항목 | 값 |
| --- | --- |
| 에뮬레이터 | Pleos Connect v2.1.0, Android 14 (API 34), 2560×1440 |
| 빌드 | JDK 17, Gradle 8.10.2(wrapper), AGP 8.8.2, Kotlin 2.0.0 |
| SDK | minSdk 28, targetSdk 35 |

소스에서 빌드: `./gradlew :app:assembleDebug` (Windows: `.\gradlew.bat :app:assembleDebug`)

## APK 위치

`apk/` 폴더 (제출 시 `팀명_Plenty_release.apk`)

## 구조

- `app/` — Android 앱 (Jetpack Compose, Room, Pleos SDK 어댑터)
- `core/` — Android에 의존하지 않는 게임 규칙
- `docs/` — 검증된 API 사실, 진행 상황

## 사용한 자산과 라이선스

- 폰트: Noto Sans KR, SIL Open Font License 1.1 (`app/src/main/assets/licenses/NotoSansKR-OFL.txt`)
- 캐릭터·카트·배경·식물 원화, 아이콘: 이 프로젝트를 위해 제작한 디자인 키트 v1.0 (원화는 이미지 생성 도구로 제작)
- 망토 분리 캐릭터·구름·전경 원화와 모션 규격: 이 프로젝트를 위해 제작한 모션 키트 (원화는 이미지 생성 도구로 제작)

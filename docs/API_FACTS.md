# API_FACTS — 검증된 API·도구·버전

Pleos API는 이 문서에 "검증됨"으로 적힌 것만 코드에 사용한다. 문서에만 있는 사실은 "문서"로 표시하고, 실행 확인 후 "검증됨"으로 바꾼다.

## 빌드 도구 (검증됨, 2026-10-28)

| 항목 | 값 | 비고 |
| --- | --- | --- |
| Gradle | 8.10.2 | wrapper jar SHA-256 `2db75c40…8046` = 공식 값 |
| AGP | 8.8.2 | 공식 Pleos 샘플과 동일 |
| Kotlin | 2.0.0 (+ Compose 컴파일러 플러그인) | |
| JDK | Temurin 17.0.20 (`JAVA_HOME`) | Android Studio 2026.2.1 내장 JBR 25는 Gradle 8.10.2 미지원 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 28 | minSdk 28은 Pleos 요구사항 |
| Compose BOM | 2024.12.01 (Compose 1.7.x) | 샘플의 2024.04.01은 실제로 1.6.6/1.7.1이 섞여 해석됨 |
| KSP / Room | 2.0.0-1.0.24 / 2.6.1 | DB v2 (`AutoMigration` 1→2, 스키마는 `app/schemas`) |
| Navigation Compose | 2.8.5 | |
| Lifecycle / Activity Compose | 2.9.0 / 1.10.1 | 샘플과 동일 |

## Pleos SDK

| 항목 | 상태 | 내용 |
| --- | --- | --- |
| Maven 저장소 | 검증됨 | `https://nexus-playground.pleos.ai/repository/maven-releases/`, 인증 불필요 |
| Vehicle | 검증됨 | `ai.pleos.playground:Vehicle:2.3.2`. 저장소에는 2.0.3과 2.3.2만 있음(2.3.1 없음). `Vehicle(applicationContext)` → `initialize()` → `getOdometer()`/`getGear()` → `release()` |
| 누적 주행거리 | **검증됨 (T02, 앱 연결 10/30)** | `Odometer.getCurrentDistanceDriven(onComplete: (Float?) -> Unit, onFailed)`, `registerDistanceDriven(DistanceDrivenListener)` (`onDistanceDrivenUpdated(Float?)`, `onFailed(Exception)`). **단위 km** (Virtual Console 12345 km → 12345.0). 권한 `pleos.car.permission.CAR_DRIVE` (SDK `@RequiresPermission`). capability `accessType=READ`. 리스너는 값이 같아도 약 3–4초마다 호출되며 **값 변경보다 몇 초 늦게 올 수 있음** → 출발·도착 시점에는 `getCurrentDistanceDriven`으로 새로 읽는다. **앱 프로세스 종료 중 증가한 값도 재실행 후 그대로 읽힘** (주행 중 앱 강제 종료 → 주차 → 재실행으로 정산 확인, 10/30) |
| 트립 거리 | **에뮬레이터 미지원** | `getTripCurrent/Cumulative/SinceRefillDrivingDistance`, `registerTripCurrentDrivingDistance` → `ERROR_NOT_SUPPORTED_PROPERTY`. 사용하지 않음 |
| 기어 (Pleos SDK) | 검증됨, **deprecated** | `Gear.getCurrentGearSelectionState`/`registerGearSelection`(`CAR_DRIVE`)는 동작하지만 SDK가 deprecated("use the Google GEAR_SELECTION property"). 사용하지 않음 |
| 기어 (`GEAR_SELECTION`) | **검증됨 (10/30)** | 출발·도착 신호. `Car.createCar` → `getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager` → `registerCallback(cb, VehiclePropertyIds.GEAR_SELECTION, SENSOR_RATE_ONCHANGE)`. 구독 즉시 현재 값 1회, 이후 변경마다 이벤트(`status` 0). 값: P=4, D=8 (`android.car.VehicleGear`). 권한 `android.car.permission.CAR_POWERTRAIN`(설치 시 부여). `registerCallback`/`status`는 API 34에서 deprecated지만 에뮬레이터에서 검증된 호출이라 유지 |
| EvBattery | 문서(SDK 시그니처) | `getEvBatteryStateOfCharge((Int?) -> Unit, …)` 등, 권한 `CAR_ENERGY`. 미검증 |
| FusedLocation | 검증됨(샘플) | `ai.pleos.playground:FusedLocation:1.1.3`, 권한 `pleos.car.permission.FUSED_LOCATION`(런타임 요청). 에뮬레이터에서 `isSupported()` = true. **`adb emu geo fix <경도> <위도>`로 바꾼 위치가 그대로 수신됨**. 공모전 사용 가능(10/28 확인). 권한 허용/거부 흐름은 T10에서 확인 |
| 주행 제한 | **검증됨 (T02)** | Gradle `useLibrary("android.car")` + 매니페스트 `<uses-library android:name="android.car" android:required="false"/>`. `Car.createCar(context)` → `getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager` → `currentCarUxRestrictions`, `registerListener {}`. 별도 권한 불필요 |

### 주행 제한 실측 (T02, 2026-10-28)

| 차량 상태 | 플랫폼 주행 상태 | CarUxRestrictions | Plenty(비 DO 앱) 화면 |
| --- | --- | --- | --- |
| 기어 P | 0 parked | DO=false, 0x0 | 정상 |
| 기어 D, 0 km/h | 1 idling | **DO=true, 0x50** | **플랫폼 차단 화면**: 앱 아이콘 + "주행 중에는 사용할 수 없습니다." (배경 흐림) |
| 기어 D, 60 km/h | 2 moving | DO=true, 0x50 | 차단 화면 유지 |
| 다시 P | 0 parked | DO=false, 0x0 | 차단 해제, 직전 화면 그대로 |
| 상태 미확인 | unknown | DO=true, 0x1ff (기준 설정) | — |

- 차단 중에도 Activity는 `onStop`되지 않는다. **애니메이션·입력·타이머는 앱이 UX 제한 신호를 받아 직접 멈춰야 한다.**
- 차단 중에도 앱 프로세스의 거리·기어·UX 리스너 콜백은 계속 들어온다(기술적 사실, 정책상 허용 여부는 별도).
- 플랫폼 차단 화면이 있으므로 디자인 07(자체 제한 화면)은 필요 없다(HANDOFF §8-3).

## 에뮬레이터 (검증됨, 2026-10-28)

| 항목 | 값 |
| --- | --- |
| 이미지 | Pleos Connect v2.1.0, API 34, x86_64, android-automotive (42dot) |
| 화면 | 2560×1440, 160dpi (1dp = 1px) |
| 앱 창 | 1682×1210 dp, `multi-window` (x 853–2535, y 90–1300). 창 크기가 바뀔 수 있음 |
| 화면 사용자 | user 10 → `adb shell am start --user 10 -n app.plenty/.MainActivity` |
| Virtual Console | `adb forward tcp:8088 tcp:8088` → `http://127.0.0.1:8088/`. **HTTP API로 자동화 가능**: `PUT /vehicle/gear?gear=Park|Drive`, `PUT /vehicle/controls?speed=<km/h>&accelerator=0&brake=0&steering=0` (화면 UI에는 없는 차속 포함), `PUT /vehicle/odometer?traveled_distance_km=<km>`, `PUT /vehicle/ev-battery?field=soc&value=<%>`, `GET /vehicle/state`. 래퍼: `tools/vehicle-sim.sh`. 브라우저로 콘솔을 열면 일부 값(주행가능거리 등)이 초기화됨. **쓰기는 브라우저 세션 heartbeat(`POST /browser/session?client=<id>`, 약 3초 유효)가 살아 있을 때만 받음**, 없으면 HTTP 503. 세션이 끊겨도 이미 쓴 값은 유지. **누적 주행거리는 줄일 수 없음**(현재보다 작은 값은 HTTP 400) |
| 지도 | 내비게이션 앱 지도가 비어 있음. 지도 엔진이 자체 서버 gRPC 연결 시간 초과(네트워크는 정상). Plenty와 무관 |
| Plenty 첫 실행 | debug 빌드 cold start 4.2초 (10/28). release 빌드에서 재측정 필요 |

## 출처

- 공식 샘플: `github.com/Pleos-Playground/pleos-sample-vehicle-app`, `pleos-sample-fusedlocation-app` (main, 2026-10-16~17). 라이선스 파일 없음 → 코드 복사 금지, API 사용법만 참고
- 문서: document.pleos.ai (getting-started, Vehicle/FusedLocation project-setup, release-note 2026-10-19, use-vehicle-emulator, virtual-console-guide, driving-restriction-development-guide, review-driver-distraction-elements)

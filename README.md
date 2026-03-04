# DayLog

DayLog는 Android 기반의 일상 기록 관리 앱입니다.
사용자가 하루 동안의 활동을 카테고리로 기록하고, 캘린더를 통해 날짜별 기록을 확인할 수 있도록 설계되었습니다.
---

## 프로젝트 소개

DayLog는 일상 속 활동을 간단하게 기록하고 관리할 수 있도록 만든 Android 애플리케이션입니다.
사용자는 카테고리를 선택하여 기록을 남길 수 있으며, 캘린더에서 기록이 있는 날짜를 한눈에 확인할 수 있습니다.
Firebase를 활용하여 사용자별 데이터를 저장하도록 설계하여 여러 사용자가 앱을 사용하더라도 각자의 기록이 분리되어 관리됩니다.
---

## 개발 활경
-Language : Java
-IDE : Android Studio
-Database : Firebase Firestore
-Authentication : Firebase Authentication
-UI : XML Layout
---

## 주요 기능
### 1. 카테고리 기반 기록 작성
사용자는 다양한 카테고리를 선택하여 하루의 활동을 기록할 수 있습니다.

### 2. 캘린더 기반 기록 확인
기록이 있는 날짜를 캘린더에서 표시하여 특정 날짜의 기록을 쉽게 확인할 수 있습니다.

### 3. 사용자별 데이터 관리
Firebase Authentication을 통해 로그인한 사용자별로 기록을 분리하여 저장합니다.

### 4. 기록 자동 불러오기
이전에 작성한 기록은 다시 앱을 실행했을 때 자동으로 불러와 확인할 수 있습니다.

### 5. 사용자 이름 표시
사용자의 이름을 저장하고 캘린더 상단에서 인사 메세지 형태로 표시합니다.
---

## 프로젝트 구조
DayLog
├── activities
│ ├── MainActivity
│ ├── CalendarActivity
│ └── SettingsActivity
├── firebase
│ └── FirestoreDatabase
├── model
│ └── Record
└── ui
└── category_chip_layout
---

## 프로젝트를 통해 배운 점
-Firebase Authentication을 활용한 사용자 구분 처리
-Firestore를 이용한 데이터 저장 구조 설계
-Android 캘린더 UI와 기록 데이터 연동
-사용자 경험을 고려한 간단한 기록 인터페이스 설계
---

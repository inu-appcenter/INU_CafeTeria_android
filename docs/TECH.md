# TECH

개발 문서입니다.

## 배경

학식에서 번호를 기다리는 것이 영 불편하였습니다.   

이를 타파하기 위해 번호 알림을 골자로 하는 서비스가 개발되었습니다. (2019/09/06 기준 다운로드 5000+)

후에 모종의 이유로 번호 알림 서비스가 중단되었으며 클라이언트는 업데이트를 멈춘 채로 방치상태였습니다. (업데이트 2019년 2월에 끝)

가장 중요한 기능이 빠졌지만, 식단 확인과 생협 바코드 할인 기능은 아직 쓸만합니다.

위 두 기능을 주로 하여 새로 개발을 진행하였습니다.

## 개발 요구사항

위 두 개가 끝입니다. 식단 + 바코드.

## 서비스 개요

### 이 앱이 하는 일

이 앱은 서버와 통신합니다. 두 가지 이유에서입니다.

하나는 로그인 후 바코드를 가져오기 위함이며 다른 하나는 식단 정보를 가져오기 위함입니다.

### 서버가 하는 일

서버는 클라이언트의 부름에 답합니다.

로그인을 처리하며 바코드의 발급과 상태 제어를 담당합니다.


## 자세히

여기서부터는 안드로이드 클라이언트 개발에 관한 내용입니다.

### 프로젝트 환경

운영체제는 mac OS 10.14.6, 통합 개발 환경은 Android Studio 3.5를 사용하였습니다.
버전 관리는 Git과 GitHub을 사용하였습니다.

### 프로젝트 구성

이 프로젝트는 멀티모듈로 구성되어 있습니다.

단일 모듈과 비교한 멀티모듈의 장점은 다음과 같습니다:
- 모듈별로 테스트가 가능하다.
- 모든 모듈을 매번 컴파일할 필요가 없기 때문에 빌드 시간이 단축된다.
- 보기 좋다.

모듈은 4개로 이루어져 있으며, 각각은 하나의 계층을 담당합니다.

- app: 안드로이드 실행 모듈입니다. 표현을 포함한 외적인 것들을 담당합니다. Activity, Fragment 등의 Android 요소들이 위치합니다.
- domain: 앱의 핵심 비즈니스 로직과 데이터 모델을 정의합니다. 앱에 등장하는 거의 모든 동작은 이 domain 계층의 usecase에 의해 이루어집니다.
- data: 신뢰할 수 있는 하나의 저장소를 담당합니다. domain 계층에서 정의, 사용할 repository를 구현합니다.
- common: 계층과 관련 없는 플랫폼 독립적 API나 utility를 정의-구현 합니다.

각 모듈은 Kotlin 또는 Java(이 프로젝트에는 없음) 소스코드와 리소스를 포함합니다.

이 프로젝트에서는 app 모듈만이 리소스를 가지고 있으며, 나머지 모듈은 string을 제외한 리소스는 갖지 않습니다.

### 사용 언어

Kotlin을 사용합니다. Kotlin은 Java에 비해 상당한 이점을 가집니다.

- null-safety
- 풍부한 scope-funcion
- 함수의 1급객체화
- 함수지향 패러다임 

코드를 극단적으로 줄이는 것도 가능해집니다.
~~~kotlin
        return BaseViewHolder(view).apply {
            containerView.setOnClickListener { view ->
                getItem(adapterPosition)?.let { cafeteria ->
                    clickListener(view, cafeteria)
                }
            }
        }
~~~

### 신경쓴 것들 - 예외처리

예외 상황을 처리하기 위한 방법이 많이 있습니다. 

여러 좋은 방법 중 가장 잘 맞는 방법을 선택해야 합니다.

하나의 프로젝트에서 같은 상황에서 각기 다른 접근을 사용한다면 큰 혼란이 오며 안정성은 수직 하락합니다.

그래서 다음과 같이 정했습니다.

- 데이터와 예외는 같은 우선순위를 가진다. 즉 예외를 무시하지 말고 피드백을 내보내며 이를 처리할 것.
- 처리되지 않은 예외를 막기 위해 Repository와 UseCase의 모든 메소드는 wrapper로 감쌀 것.

이를 위해 다음 도구를 만들어 사용했습니다.

예외를 처리하고 감싸기 위해:
`Result`: Success와 Error 두 하위 타입으로 나뉘어지며, 전자는 결과로 가져야 할 데이터를, 후자는 그 과정에서 발생한 예외 객체를 담습니다.

실패 상황 발생 시 이를 사용자에게 알리기 위해:
`Failure`: 실패에 대한 정보를 담습니다. 메시지와 사용자에게 출력할지 여부를 속성으로 가집니다.
`Failable`: 어떤 종류든 실패가 발생할 가능성이 있는 클래스는 이 인터페이스를 상속받아 구현해야 합니다.
`FailableContainer`: `Failable` 객체를 속성으로 지니고 있는 클래스는 이 인터페이스를 상속받아야 합니다.
`FailableHandler`: 모든 하위 `Failable` 객체가 만든 `Failure` 객체를 처리하여 그 내용을 사용자에게 전달할 의무를 정의합니다.

위의 예외 처리 시스템을 사용하여 서버에서 데이터를 가져오는 과정은 다음과 같습니다.

1. 사용자의 의도가 감지됩니다.
2. 뷰모델은 서버로부터 데이터를 가져오는 UseCase를 실행합니다.
3. UseCase는 별도의 thread에서 repository에 접근하여 데이터를 가져옵니다.
4. 가져온 데이터의 성공 여부에 따라 UseCase의 onResult 콜백을 호출합니다. onSuccess 또는 onError입니다.
5. 뷰모델이 넘긴 콜백이 실행되고 이에 따라 뷰가 업데이트되며 사용자에게 피드백이 전달됩니다.

UseCase 안에서 예외가 발생한다면 자동으로 onError 콜백이 호출됩니다.

어떤 repository는 네트워크 요청을 보내고 답을 받습니다.

네트워크를 다룰 때에도 고려해야 할 예외 상황이 많습니다.

네트워크 요청의 결과를 크게 두가지로 나누었습니다: 성공 또는 실패.

실패는 예외의 타입에 따라 기본 예외 처리자 또는 지정된 예외 처리자에 의해 처리됩니다. 

이때 기본 동작은 예외의 종류에 따른 메시지를 해당 `Failable`을 구독하는 `FailableHandler`에게 알리는 것입니다.

`Failable`의 구독은 `FailableHandler`의 생명 주기가 막 시작되었을 때에 그가 가진 failables의 각각 failure(LiveData의 형태)들을 구독하는 방식으로 구현됩니다.

이렇게 함으로써 `Failable`을 상속받는 클래스는 언제 어디서든 문제가 생겼을 때에 `fail()` 메소드로 사용자에게 이를 전달할 수 있습니다.



### 신경쓴 것들 - 기능의 확장


// 작성중 20190906 06:12














## 이 문서의 목적

혹시나 누군가가 미래에 이 프로젝트를 맡게 되었을 때에 덜 고통받도록 하기 위함입니다.
## CineLink

![image](https://github.com/user-attachments/assets/34634a62-b3a3-49b4-ae19-b2b93039518b)

#### 당신과 영화의 특별한 만남, ClineLink가 이어드립니다.
- **최신 영화 및 상영관 정보 제공**: 개봉 예정작과 현재 상영 중인 영화 정보를 확인할 수 있습니다.
- **간편한 예매 및 결제**: 원하는 영화, 극장, 좌석을 선택하고 간편하게 결제할 수 있습니다.
- **실시간 예매 현황 반영**: 예매 가능한 좌석이 자동으로 업데이트되어 최신 정보를 제공합니다.
- **알림 기능 제공**: 예매 및 결제 완료 시 이메일 및 푸시 알림을 통해 정보를 제공합니다.

## Movie15 팀원
|팀원|태그| 담당기능               | Github 주소|
|-|--|--------------------|--------|
|김휘웅|팀원| 회원가입 및 로그인, 문의, 알림 | https://github.com/hwiung|

## 기술 스택
#### Back-end
[![My Skills](https://skillicons.dev/icons?i=idea,java,spring,mysql,redis,docker,rabbitmq,aws,github,githubactions,docker)](https://skillicons.dev)
- **IDE** : Intellij  
- **JDK** : openjdk Java 17  
- **Framework** :
    - Spring Framework  
    - Spring Security  
- **데이터 베이스**  
    - Mysql  
    - Redis  
- **외부 API & 라이브러리**  
    - TMDB API  
    - 토스페이먼츠 API  
    - RabbitMQ  
- **Infra**  
    - AWS EC2  
    - AWS S3  
    - AWS ElasticCache  
- **CI / CD**  
    - Git Actions  
    - Docker  

---

## 프로젝트 기능

<details>
<summary>회원가입 / 로그인</summary>
<div markdown="1">

- **시스템 초기 실행 시** `AdminInitializer`를 활용하여 기본 관리자 계정을 자동 생성하고, 관리자 계정의 존재 여부를 검증함  
- **권한 관리**  
    - `USER`, `ADMIN`  
    - 인증 / 인가  
    - Spring Security와 JWT를 기반으로 한 접근 제어 및 권한 관리  
        - `@PreAuthorize` 및 `@AuthenticationPrincipal`을 활용한 권한 검증 및 접근 제어  
        - 관리자 전용 API 보호 및 유저별 접근 제한  
        - 로그인 시 **Access Token**과 **Refresh Token** 발급 후, **Redis**에 저장 및 검증  
        - TTL(Time-To-Live) 설정을 통해 불필요한 데이터 저장 방지 및 **블랙리스트 토큰 관리**로 로그아웃 또는 만료된 토큰 재사용 방지  

</div>
</details>

<details>
<summary>영화 관리</summary>
<div markdown="1">

1. **영화 정보 등록 및 관리 (관리자)**  
   - 매일 새벽 3시에 인기 영화 40개를 영화관에게 제공  
   - 영화 상영시간, 제목, 줄거리, 장르, 포스터 이미지, 트레일러 등을 제공  
   - 인기 영화에서 벗어난 영화는 **1주일 뒤 논리적 삭제**  
   - 인기 영화에서 벗어났다가 다시 복귀한 영화는 **논리적 삭제 복구**  

2. **영화 조회**  
   - 영화 장르, 상영 시간, 제목, 지점별, 상영 중인 영화별 검색 가능  

3. **상영 시간 관리**  
   - 특정 극장에서 해당 영화가 언제 상영되는지 확인  
   - 상영 시간표에 따른 예약 가능 여부 반영  

</div>
</details>

<details>
<summary>영화관</summary>
<div markdown="1">

- **영화관 생성 (관리자)**  
    - 지역, 이름을 입력하여 생성 가능  
- **영화관 내부 관람관 관리**  
    - 영화관은 여러 관람관을 추가할 수 있음  
- **영화관 조회**  
    - 영화관의 지역과 이름으로 조회 가능  

</div>
</details>

<details>
<summary>관람관</summary>
<div markdown="1">

- **좌석 종류 및 등급**  
    - **노란색**: VIP 좌석  
    - **하늘색**: 일반 좌석
    - <img src="https://github.com/user-attachments/assets/d732cfb6-7d63-45b8-bbeb-8f4b80fe9175" width="300" height="300"/>
    - <img src="https://github.com/user-attachments/assets/607d226d-b286-4e67-988e-776f13bac84c" width="300" height="300"/>
    - <img src="https://github.com/user-attachments/assets/ec6ee751-e5d6-4f33-849f-b841e8a2fb5f" width="300" height="300"/>
    - <img src="https://github.com/user-attachments/assets/1828f8d7-3ea6-4d2e-a26a-7d3085ff4f66" width="300" height="300"/>
    - <img src="https://github.com/user-attachments/assets/b7931823-8887-480d-84cb-346f31726d6e" width="300" height="300"/>

</div>
</details>

<details>
<summary>영화 상영 스케줄</summary>
<div markdown="1">

- **영화 상영 스케줄 생성 (관리자)**  
    - **상영 날짜, 상영 시작 시간, 영화관 ID, 영화 ID**를 포함  
    - 영화의 **상영 시간 + 청소 시간(10분)** 으로 상영 종료 시간이 자동 계산  
    - 같은 관람관에서는 **상영 시간이 겹칠 수 없음**  

</div>
</details>

<details>
<summary>영화 예매</summary>
<div markdown="1">

- **영화 예매 생성**  
    - Redis를 통한 동시성 제어  
- **예매 조회, 수정, 취소 가능**  

</div>
</details>

<details>
<summary>결제 시스템 연동</summary>
<div markdown="1">

- **결제 API**  
    - Toss 결제 흐름도  
    - <img src="https://github.com/user-attachments/assets/3a3c98fe-389e-4b3a-bc7a-b41cbe5cfb6c" width="300" height="300"/>
- **결제 요청, 내역 확인, 취소 가능**  

</div>
</details>

<details>
<summary>알림 시스템</summary>
<div markdown="1">

- `JavaMailSender` → 회원가입 시 **이메일 인증 발송**  
- `RabbitMQ(x-delay-message)` → 회원가입 이메일 발송 **10분 후 유저 데이터 조회**, 미인증 시 해당 유저 데이터 삭제  
- `RabbitMQ(x-delay-message)` → **결제 성공 시 영화 시작 30분 전 이메일 전송 예약**  
- `RabbitMQ` → **결제 성공 및 결제 취소 시 이메일 발송**  

</div>
</details>

<details>
<summary>리뷰</summary>
<div markdown="1">

- **리뷰 생성, 조회, 수정, 삭제 가능**  
    - 조회 시 **페이지 처리** 적용  
    - 삭제 시 **본인 또는 관리자만 가능**  
- **영화 당 1인 1건만 작성 가능**  

</div>
</details>

<details>
<summary>문의 사항</summary>
<div markdown="1">

- **문의 사항 작성 시 파일 첨부 가능**  
- **관리자는 문의 상태를 `PENDING`, `ANSWERED`로 변경 가능**  
- **문의 상태가 `ANSWERED`일 경우, 사용자는 문의 수정 불가**  

</div>
</details>

---

## 인프라 설계도
![인프라_설계도531_ drawio](https://github.com/user-attachments/assets/10d77627-5652-40a0-9197-b934232c58b6)

## API 명세서
[API 명세서 링크](https://www.notion.so/teamsparta/cca7fb9345e549a9b278e8510e8e6288?v=ab8ed301c39040bda8d283fc700b7b6e&pvs=4)

## ERD
![롯데십오네마_ERD](https://github.com/user-attachments/assets/9b504ffb-2367-4f52-81e6-f6ec23e393ce)

## 스프링 데이터 JPA와 Proxy 동작 원리 정리
### MemberRepository에 구현체가 없어도 동작하는 이유
- Spring Data JPA는 우리가 직접 구현체를 작성하지 않아도,
  런타임에 해당 인터페이스의 구현체(프록시)를 자동 생성하여 주입한다.
- 프록시 객체는 실제로 SimpleJpaRepository를 상속한 구현체이며,
  JPA의 기본 CRUD 기능을 제공한다.

### 프록시 생성 방식 (JDK Dynamic Proxy vs CGLIB)
- 인터페이스 기반 Repository → JDK 동적 프록시
- 클래스 기반 Repository → CGLIB 프록시
---
## @EnableJpaRepositories 동작 설명
### @EnableJpaRepositories(basePackages = "study.datajpa.repository")

- 지정된 패키지 하위의 Repository 인터페이스들을 스캔한다.
- JpaRepository를 상속한 인터페이스에 대해,
  Spring Data JPA가 자동으로 구현체(프록시)를 생성하고 스프링 빈으로 등록한다.

### 스프링부트 사용 시

- @EnableJpaRepositories 생략 가능
- @SpringBootApplication이 선언된 패키지를 기준으로 하위 패키지를 자동 스캔한다.

### 정리

- MemberRepository에는 직접 구현 클래스가 없어도 동작한다.
- 이유는 Spring Data JPA가 프록시 구현체를 런타임에 생성해서 주입하기 때문.
- @EnableJpaRepositories는 이러한 프록시 생성을 위한 설정이며, 스프링부트에서는 기본값으로 자동 설정된다.

---


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

## 스프링 데이터 JPA의 쿼리 메서드 기능
### 메서드 이름으로 쿼리를 자동 생성
- 스프링 데이터 JPA는 인터페이스에 메서드 시그니처만 정의해도,
  메서드 이름을 분석하여 자동으로 JPQL 쿼리를 생성한다.
- Ex) findByUsername → SELECT m FROM Member m WHERE m.username = ?

### 동작 원리
- 메서드 명 분석 → 엔티티의 필드 이름 기반으로 쿼리 파싱
- 필드명이 정확히 일치해야 하며, 오타 또는 존재하지 않는 필드를 사용하면 오류 발생

### 필드명이 변경될 경우 주의사항
- 엔티티 클래스에서 필드명이 변경되면,
  관련된 쿼리 메서드 이름도 반드시 함께 변경해야 한다.
- 그렇지 않으면 스프링이 애플리케이션 로딩 시점에 오류를 발생시킨다.
- 이는 런타임 오류가 아닌 **애플리케이션 초기 구동 시점에 오류를 감지**할 수 있기 때문에
  오히려 버그를 빠르게 인지할 수 있다는 장점이 있다.

---
## NamedQuery가 실무에서 잘 사용되지 않는 이유
### NamedQuery란?
- `@NamedQuery`는 JPQL 쿼리를 엔티티 클래스 상단에 미리 정의해두고,
  이름으로 불러와 사용하는 방식
```java
@NamedQuery(
  name = "Member.findByUsername",
  query = "SELECT m FROM Member m WHERE m.username = :username"
)
```

### 실무에서 잘 사용하지 않는 이유
- 쿼리가 엔티티 클래스에 하드코딩되므로 유지보수성이 떨어짐
  → 엔티티가 복잡해지고 관심사가 섞임
- 파라미터가 많아질수록 쿼리 가독성이 급격히 저하됨
- 동적 쿼리 작성이 불가능하고, 컴파일 시점 오류 탐지도 어려움

---

### @Query 생략 가능하지만, 우선순위는 다음과 같다

- 스프링 데이터 JPA는 리포지토리 메서드에 대해 아래 순서로 쿼리를 결정한다

1. **먼저, 동일한 이름의 NamedQuery가 존재하는지 확인**  
   → `Member.findByUsername`이 정의되어 있다면 그것을 우선 사용

2. **NamedQuery가 없으면 메서드 이름 기반 쿼리 생성 시도**  
   → 메서드 명 분석 → `findByUsernameAndAgeGreaterThan` → JPQL 자동 생성

3. **필요 시 @Query로 명시적으로 작성 가능**  
   → 복잡한 쿼리, 서브쿼리, join fetch 등이 필요한 경우

> 실무에서는 @Query 또는 동적 쿼리 도구(QueryDSL 등)가 많이 사용됨

---

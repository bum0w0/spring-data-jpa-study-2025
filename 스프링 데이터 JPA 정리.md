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

## Spring Data JPA의 유연한 반환 타입 지원

### 단일 객체 반환
- 타입: Member
- 결과 없음: null
- 결과 2개 이상: 예외 발생

### 컬렉션 반환
- 타입: List<Member>, Set<Member>
- 결과 없음: 빈 컬렉션
- 예외 발생하지 않음

### Optional 반환
- 타입: Optional<Member>
- 결과 없음: Optional.empty()
- 결과 2개 이상: 예외 발생

### DTO 반환
- JPQL에서 new 키워드로 생성자 기반 DTO 매핑
- @Query("select new ...")

---

## 순수 JPA 페이징과 정렬

### 1. 페이징

```java
TypedQuery<Member> query = em.createQuery("select m from Member m", Member.class)
        .setFirstResult(offset)       // offset: 조회 시작 위치
        .setMaxResults(limit);        // limit: 조회할 데이터 수

List<Member> result = query.getResultList();

```

### 2. 정렬

String jpql = "select m from Member m order by m.username desc";

→ JPQL에서 order by 키워드로 정렬 수행 가능



### 실무 팁
- count 쿼리와 데이터 조회 쿼리를 분리하여 성능 최적화 가능
- 정렬 컬럼에는 인덱스를 걸어주는 것이 좋음
- Spring Data JPA에서는 Pageable, Sort를 활용한 페이징/정렬 자동화도 지원됨

---

## Spring Data JPA에서의 페이징

**Spring Data JPA는 Pageable 인터페이스를 통해 페이징 기능을 간편하게 지원한다.**

### 1. Repository 메서드 정의

- PagingAndSortingRepository 또는 JpaRepository 사용 시 다음과 같이 정의 가능:
```java
Page<Member> findByUsername(String username, Pageable pageable);
```

### 2. Pageable 객체 생성
- PageRequest 객체로 Pageable 생성
```java
PageRequest pageable = PageRequest.of(page, size);  
// Ex. PageRequest.of(0, 10) → 첫 페이지, 10개씩
```            

- 정렬 포함 시
```java
PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));
```

### 3. 페이지를 유지하면서 엔티티를 DTO로 변환하기

Page 객체 자체를 클라이언트에 직접 반환하기보다, 필요한 데이터만 추출하여 DTO로 감싸서 반환하는 방식 권장
```java
Page<MemberDto> toMap = page.map(
                member -> new MemberDto(member.getId(), member.getUsername(), member.getTeam().getName()));
```

### 4. count 쿼리 분리

- Spring Data JPA는 페이징 시 기본적으로 count 쿼리를 함께 실행하여 전체 데이터 개수를 가져온다.
- 복잡한 조인이나 조건이 있는 경우 count 쿼리가 성능 저하의 원인이 될 수 있다.
- 이때 @Query 어노테이션을 사용하여 countQuery를 분리할 수 있다.

```java
@Query(
    value = "select m from Member m left join m.team t",
    countQuery = "select count(m) from Member m"
)
Page<Member> findAllWithTeam(Pageable pageable);

```
**value는 실제 데이터 조회용, countQuery는 전체 개수 조회용 쿼리 → 성능 최적화에 유리**

---

## 벌크성 수정 쿼리 (Bulk Update / Delete)

- JPA에서는 일반적인 엔티티 수정은 영속성 컨텍스트를 통해 수행하지만,
  대량 데이터를 한 번에 수정/삭제할 때는 JPQL 벌크 쿼리를 사용함
- Ex. 전체 직원의 연봉을 10% 인상하는 경우

### 1. 기본 문법 (Spring Data JPA 기준)

```java
@Modifying  
@Query("update Member m set m.age = m.age + 1 where m.age >= :age")  
int bulkUpdate(@Param("age") int age);
```
→ 조건에 맞는 레코드를 한 번에 수정, 반환값은 수정된 행 수

### 2. 주의사항

- 벌크 쿼리는 **영속성 컨텍스트를 무시하고 바로 DB에 반영됨**
- 따라서 벌크 쿼리 실행 후에는 **영속성 컨텍스트 초기화 필요**
    - 순수 JPA: `em.clear()` 호출
    - Spring Data JPA: `@Modifying(clearAutomatically = true)` 사용 가능

### 3. 삭제 쿼리 예시

```java
@Modifying  
@Query("delete from Member m where m.age < :age")  
int bulkDelete(@Param("age") int age);

```


### 4. 실무 팁

- 벌크 연산 후에 같은 엔티티를 다시 조회/사용하려면 반드시 영속성 초기화할 것
- 트랜잭션 내부에서 실행되어야 하므로 `@Transactional`이 필요
- 복잡한 연산은 벌크 쿼리보다 비즈니스 로직 분리 후 개별 처리도 고려

### 5. 순수 JPA vs Spring Data JPA 차이점

| 항목 | 순수 JPA | Spring Data JPA |
|------|----------|------------------|
| 실행 방식 | `em.createQuery().executeUpdate()` 직접 작성 | `@Modifying + @Query` 선언적 방식 |
| 트랜잭션 처리 | `@Transactional` 직접 선언 | 클래스 또는 메서드 수준에서 선언 |
| clear 처리 | `em.clear()` 수동 호출 | `@Modifying(clearAutomatically = true)` 자동 가능 |
| 코드 위치 | Service 또는 Repository 구현체 내부 | Repository 인터페이스에 정의 가능 |
| 코드 양 | 비교적 길고 명시적 | 간결하고 선언적 |

→ 기능은 동일하지만, **Spring Data JPA가 선언형으로 더 간편하게 작성 가능**

---

## JPA Hint & Lock

### Hint (쿼리 힌트)
- JPA 성능 최적화를 위해 사용
- Hibernate에 특정 동작을 지시 (Ex. 읽기 전용, 타임아웃 등)

```java
@QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
List<Member> findReadOnlyMembers();
```

- 자주 사용하는 힌트
    - `org.hibernate.readOnly` : 읽기 전용 설정 (1차 캐시 저장 생략)
    - `javax.persistence.query.timeout` : 쿼리 타임아웃 설정

### Lock (락)

#### 비관적 락 (Pessimistic Lock)
- DB에서 **실제로 락을 걸어** 다른 트랜잭션의 접근을 차단 ()
- 강한 동시성 제어 필요할 때 사용

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Member findByIdForUpdate(Long id);
```

#### 낙관적 락 (Optimistic Lock)
- 엔티티에 `@Version` 필드를 추가하여 **수정 충돌 감지**
- DB에 락을 걸지 않음. 충돌 시 예외 발생

```java
@Version
private Integer version;
```
### 비교 정리

| 구분        | 설명                             | 특징                   |
|-------------|----------------------------------|------------------------|
| Hint        | 성능 최적화 지시                 | 주로 조회 성능 개선    |
| Pessimistic | DB에 락을 걸어 동시 수정 차단     | 강한 제어, 대기 발생 가능 |
| Optimistic  | 버전으로 수정 충돌 감지          | 락 없음, 예외 발생 가능 |

---

## 사용자 정의 리포지토리

- 기본 기능 외 커스텀 로직이 필요할 때 직접 구현하는 저장소
- Spring Data JPA에서 `~RepositoryCustom` 인터페이스 + `~RepositoryImpl` 클래스 구조로 사용
- 복잡한 쿼리, 동적 쿼리 등을 EntityManager로 직접 작성 가능
- 사용자 정의 리포지토리는 기존 JpaRepository와 함께 확장하여 사용
> 실무에서는 주로 QueryDSL이나 SpringJdbcTemplate을 함께 사용할 때 자주 사용됨

---

## 스프링 데이터 JPA 구현체 분석
### 구현체가 어떻게 생성되는가?
- Spring Data JPA는 런타임에 JpaRepository를 상속한 인터페이스에 대해 프록시 객체를 자동 생성한다.
- 이 프록시 객체는 SimpleJpaRepository를 기반으로 동작하며, 내부적으로 JPA의 EntityManager를 사용하여 CRUD 로직을 처리한다.
- 프록시 생성은 내부적으로 RepositoryFactoryBeanSupport → JpaRepositoryFactory → getTargetRepository() 과정을 거쳐 이루어진다.

### 실제 생성되는 클래스 - SimpleJpaRepository
- JpaRepository의 기본 구현체
- 모든 CRUD 동작의 실질적인 실행 주체
- 트랜잭션, 쿼리 메서드 호출, 엔티티 관리 등의 기능을 EntityManager를 통해 처리

### 스프링 데이터 JPA에서 트랜잭션 없이도 등록/변경이 가능한 이유
- 서비스 코드에서 @Transactional을 명시하지 않았는데도,
save(), delete() 등의 리포지토리 메서드를 호출하면 데이터가 정상적으로 등록/수정/삭제된다.

  ### 이유
  - 실제로 트랜잭션이 없는 것이 아니라, Spring Data JPA 내부에서 자동으로 트랜잭션을 적용하고 있기 때문이다.
  - Spring Data JPA는 리포지토리 구현체(SimpleJpaRepository)의 각 메서드에
  @Transactional 어노테이션이 기본적으로 설정되어 있다.

  ### 주의사항
  - 여러 작업(등록 + 수정 등)을 묶어서 처리하거나, 영속성 컨텍스트와의 정합성을 유지해야 할 경우는
  반드시 서비스 계층에 @Transactional을 명시하는 것이 좋다.

---
## 새로운 엔티티를 구별하는 법

### 1. 기본적인 JPA 식별 방식

JPA는 엔티티가 새로운 객체인지(INSERT), 이미 영속화된 객체인지(UPDATE)를 판단할 때 다음 기준을 사용함

- `@Id` 필드 값이 `null`이면 → 새로운 엔티티로 간주 (persist)
- `@Id` 필드 값이 존재하면 → 기존 엔티티로 간주 (merge)

```java
@Entity
public class User {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
}
```

- `new User("홍길동")` → id가 null → persist 호출됨
- `new User(1L, "홍길동")` → id가 존재 → merge 또는 update 대상

### 2. Assigned ID 전략 사용 시 문제점

`@GeneratedValue`를 사용하지 않고, **직접 id를 할당하는 전략**을 사용할 경우 문제가 발생함

```java
User user = new User();
user.setId("manual-id"); // 수동 설정
```

이 경우에도 id는 존재하므로 JPA는 기존 엔티티로 인식 → `merge` 시도함  
하지만 DB에 존재하지 않는다면 오류 발생 가능

### 3. 해결 방법 - `Persistable` 인터페이스 구현

JPA에서 새로운 엔티티임을 **명시적으로 판단**할 수 있도록 `Persistable` 인터페이스 제공

```java
public class EntityName implements Persistable<String> {

    private String id;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isNew() {
        // 신규 엔티티 여부를 명확히 판단
        return true; // 혹은 생성 시점 flag 사용
    }
}
```

- `isNew()` 메서드에서 새로운 엔티티 여부를 개발자가 직접 판단 가능
- UUID, 복합키 등 식별자 전략을 유연하게 관리 가능

#### 등록 시간 기반으로 판단할 수도 있음
엔티티의 createdDate가 아직 세팅되지 않았으면 새로 생성된 것으로 간주
```java
@CreatedDate
private LocalDateTime createdAt;

@Override
public boolean isNew() {
    return createdAt == null;
}
```

---
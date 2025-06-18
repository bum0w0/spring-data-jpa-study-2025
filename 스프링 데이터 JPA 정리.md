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
## 나머지 기능들
1. Specifications(명세)
2. Query by Example
3. Projections
4. 네이티브 쿼리

> Specifications, Query by Example, Projections, 네이티브 쿼리는 스프링 데이터 JPA에서 제공하는 기능이지만,
실무에서는 잘 안 쓰이는 이유는 표현력의 한계, 복잡한 쿼리 작성의 어려움, 유지보수 불편 등이 있다.
> - 대신 QueryDSL이나 @Query를 사용하는 경우가 훨씬 많음

## Specifications(명세)

- Spring Data JPA에서 JPA Criteria API를 기반으로 동적으로 WHERE 조건을 만들 수 있게 도와주는 기능
- Specification<T> 인터페이스를 사용해 조건 객체를 조립하고, 이를 조합해 복잡한 쿼리 구성 가능
#### 기본 구조
```java
public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder);
    // Root<T>: 엔티티 루트 (예: Member)
    // CriteriaQuery: 쿼리 전체 구조
    // CriteriaBuilder: 조건을 생성하는 도구 (equal, and, or 등)
}

```
#### 사용 예시 (Member 엔티티를 조건에 따라 조회하기 위함)
```java
public class MemberSpec {

    public static Specification<Member> username(String username) {
        return (root, query, builder) -> builder.equal(root.get("username"), username);
    }

    public static Specification<Member> teamName(String teamName) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(teamName)) return null;
            Join<Member, Team> t = root.join("team", JoinType.INNER);
            return builder.equal(t.get("name"), teamName);
        };
    }
}

```

## Query by Example

Query by Example은 **엔티티 객체 자체를 검색 조건으로 활용**할 수 있는 기능으로, 동적 쿼리를 간편하게 처리할 수 있다.  
Spring Data JPA는 `Example<T>`, `ExampleMatcher` API를 통해 이 기능을 제공하며, 이미 JpaRepository 인터페이스에 기본으로 포함되어 있다.

### 기본 사용법

```java
Member member = new Member("member1"); // 검색 조건 설정
Example<Member> example = Example.of(member);

List<Member> result = memberRepository.findAll(example);
```

→ member.username = 'member1' 조건으로 쿼리가 생성됨

### ExampleMatcher 활용

`ExampleMatcher`를 사용하면, 보다 유연한 검색 조건 지정이 가능하다.

```java
Member member = new Member();
member.setUsername("member");

ExampleMatcher matcher = ExampleMatcher.matching()
        .withIgnorePaths("age") // age는 무시
        .withMatcher("username", ExampleMatcher.GenericPropertyMatchers.startsWith());

Example<Member> example = Example.of(member, matcher);

List<Member> result = memberRepository.findAll(example);
```

→ `username like 'member%'` 형태의 JPQL로 변환되어 실행됨

### 장점

| 항목 | 설명 |
|------|------|
| 도메인 객체 사용 | 검색 조건을 엔티티 객체로 정의하므로 직관적 |
| 동적 쿼리 대응 | if문 없이 필드값 유무로 조건을 자동 처리 |
| 코드 간결성 | 복잡한 Criteria API나 QueryDSL 없이 작성 가능 |
| 저장소 추상화 | JPA뿐 아니라 MongoDB 등 NoSQL 저장소에도 동일 코드 사용 가능 |
| Repository 내장 | JpaRepository의 기본 기능으로 제공됨 (`findAll(Example)` 등) |

### 주의사항

- 복잡한 조건(OR, 범위 조건 등)은 표현 한계가 있어 QueryDSL이 더 적합
- inner join 만 가능하며 외부 조인이 안 됨 (LEFT 조인 안 됨)
- 단순 필터 기반의 조건 검색에 적합

---

## Projections
- Entity 전체가 아니라 일부 데이터만 조회하고 싶을 때 사용하는 기능

#### 종류 (중첩 가능)
  - 인터페이스 기반 Projections
  - 클래스 기반 Projections (DTO 생성자 방식)
  - 동적 Projections (파라미터로 타입 지정)
  - 중첩(Nested) Projections (연관 엔티티 포함)

#### 사용 목적
  - 성능 최적화 (필요한 필드만 조회 → select 절 최적화)
  - 화면에 필요한 DTO 구조를 그대로 반환

#### 주의사항
  - 엔티티와의 필드명 정확히 일치해야 함
  - 중첩 Projection 사용 시 fetch join 아님 → N+1 문제 주의
  - 클래스 기반 Projection은 new 키워드를 통해 생성자 매핑을 사용함

#### 리포지토리 작성 예시
  - 인터페이스 기반
  ```java
interface UsernameOnly {
    String getUsername();
}

List<UsernameOnly> findByUsername(String username);
```

  - DTO 기반 (생성자 방식)
```java
class MemberDto {
    private final String username;
    
    public MemberDto(String username) {
        this.username = username;
    }
    public String getUsername() {
        return username;
    }
}

@Query("select new com.example.MemberDto(m.username) from Member m where m.username = :username")
List<MemberDto> findMemberDtoByUsername(@Param("username") String username);
```

#### 동적 Projection
```java
<T> List<T> findByUsername(String username, Class<T> type);
// 사용하는 쪽에서 DTO, 인터페이스 타입 전달 가능
```
> - 단순 조회: 인터페이스 기반
> - 복잡거나 조건 있는 조회: DTO 기반
> - 여러형식 지원: 동적 Projection

---

## Native Query
JPQL이 아닌 순수 SQL 쿼리를 직접 작성하여 실행하는 방식

#### 사용 목적
- 복잡한 SQL 사용 필요 시 (서브쿼리, DB 특화 함수 등)
- 성능 최적화된 튜닝 쿼리 사용 시
- JPQL로 불가능한 DB 뷰, 프로시저 사용 등

#### 장점
- SQL을 그대로 사용할 수 있어 유연성 높음
- 복잡한 조건, 계산식 등을 DB에 맞춰 정밀하게 작성 가능

#### 단점
- 이식성 낮음 (DB 종속)
- 결과 매핑 시 타입 안정성 없음
- DTO 자동 매핑 불가 → 수동 매핑 필요

#### 매핑 방식
```java
@Query(value = "SELECT * FROM member WHERE username = :username", nativeQuery = true)  
Member findByNativeQuery(@Param("username") String username);
 ```

#### 사용 시 주의사항
- 반환 타입이 엔티티가 아닌 경우 수동 매핑 필요
- SQL 문법 및 컬럼명이 정확해야 함
- 결과 컬럼이 매핑 대상과 순서 및 타입 일치 필요
- 페이징 사용 시 count 쿼리도 명시해야 함

## Native Query + Projection
- 네이티브 쿼리에서도 Projection(인터페이스 기반)을 사용할 수 있음
- Spring Data JPA가 결과 컬럼명과 Projection 인터페이스의 getter를 기반으로 자동 매핑

#### 인터페이스 기반 Projection과 Native Query 사용 예시

```java
public interface MemberProjection {  
    String getUsername();  
    int getAge();  
}
```

```java
@Query(value = "select m.member_id as id, m.username, t.name as teamName "
        + "from member m left join team t on m.team_id = t.team_id",
        countQuery = "select count(*) from member",
        nativeQuery = true)
Page<MemberProjection> findByNativeProjection(Pageable pageable);
```
- 컬럼명은 인터페이스 메서드 이름과 일치해야 함

#### 장점
- 엔티티 불필요 → 빠르고 가볍게 조회
- 코드 간결 (인터페이스만 정의하면 됨)
- 네이티브 쿼리의 자유도 + Projection의 편의성

#### 주의사항
- 결과 컬럼명과 인터페이스 메서드 이름 정확히 일치해야 함
- 복잡한 join, group by 등에서는 동작이 불안정할 수 있음
- 인터페이스 기반만 지원됨 (DTO 생성자 방식은 동작하지 않음)
- 데이터베이스 컬럼명이 카멜케이스가 아닐 경우 alias 지정 필요

#### 한계와 대안
- DTO 생성자 방식은 네이티브에서 직접 지원되지 않음  
  → Object[]로 조회 후 서비스 레이어에서 DTO로 수동 변환하거나  
  → @SqlResultSetMapping 사용 (복잡함, 실무에서 권장되지 않음)

---

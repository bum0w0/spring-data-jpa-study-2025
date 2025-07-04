package study.data_jpa.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import study.data_jpa.dto.MemberDto;
import study.data_jpa.entity.Member;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, JpaSpecificationExecutor<Member> {
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);
    List<Member> findTop3ByOrderByAgeDesc();
    @Query(name = "Member.findByUsername")
    List<Member> findByUsername(@Param("username") String username);
    @Query("select m from Member m where m.username = :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);
    @Query("select m.username from Member m")
    List<String> findUsernameList();
    @Query("select new study.data_jpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
    List<MemberDto> findMemberDto();
    @Query("select m from Member m where m.username in :names")
    List<Member> findByNames(@Param("names") Collection<String> names);
    List<Member> findListByUsername(String username);
    Member findMemberByUsername(String username);
    Optional<Member> findOptionalByUsername(String username);
    Page<Member> findByAge(int age, Pageable pageable);
    @Query(value = "select m from Member m left join m.team t",
            countQuery = "select count(m) from Member m")
    Page<Member> findByAge_join(int age, Pageable pageable);
    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);
    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();
    @Override
    @EntityGraph(attributePaths = {"team"}) // @EntityGraph = fetch join
    List<Member> findAll();
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Member findReadOnlyByUsername(@Param("username") String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Member> findLockByUsername(String username);
    <T> List<T> findProjectionsByUsername(@Param("username") String username, Class<T> type);
    @Query(value = "select * from member where username = ?", nativeQuery = true)
    Member findByNativeQuery(String username);
    @Query(value = "select m.member_id as id, m.username, t.name as teamName "
            + "from member m left join team t on m.team_id = t.team_id",
            countQuery = "select count(*) from member",
            nativeQuery = true)
    Page<MemberProjection> findByNativeProjection(Pageable pageable);
}

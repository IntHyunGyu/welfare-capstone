package com.welfare.today.repository;

import com.welfare.today.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    boolean existsByMemberIdAndServId(Long memberId, String servId);

    @Modifying
    @Query("delete from Favorite f where f.memberId = :memberId and f.servId = :servId")
    int deleteByMemberIdAndServId(Long memberId, String servId);

    @Modifying
    @Query("delete from Favorite f where f.memberId = :memberId")
    int deleteByMemberId(Long memberId);
}

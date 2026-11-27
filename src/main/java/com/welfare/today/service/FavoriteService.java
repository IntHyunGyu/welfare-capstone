package com.welfare.today.service;

import com.welfare.today.entity.Favorite;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.FavoriteRepository;
import com.welfare.today.repository.MemberRepository;
import com.welfare.today.repository.WelfareBenefitRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 로그인 회원의 즐겨찾기 (DB). 저장/해제는 여러 번 눌러도 결과가 같다. */
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final WelfareBenefitRepository benefitRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, MemberRepository memberRepository,
                           WelfareBenefitRepository benefitRepository) {
        this.favoriteRepository = favoriteRepository;
        this.memberRepository = memberRepository;
        this.benefitRepository = benefitRepository;
    }

    /** 저장한 servId 목록, 최근에 저장한 것부터. */
    @Transactional(readOnly = true)
    public List<String> servIds(String username) {
        return favoriteRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId(username)).stream()
                .map(Favorite::getServId)
                .toList();
    }

    /** 저장한 혜택, 최근에 저장한 것부터. 그사이 데이터에서 빠진 혜택은 건너뛴다. */
    @Transactional(readOnly = true)
    public List<WelfareBenefit> benefits(String username) {
        List<String> ids = servIds(username);
        Map<String, WelfareBenefit> byId = benefitRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(WelfareBenefit::getServId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    /** @return 없는 혜택이면 false */
    @Transactional
    public boolean add(String username, String servId) {
        if (!benefitRepository.existsById(servId)) {
            return false;
        }
        Long memberId = memberId(username);
        if (!favoriteRepository.existsByMemberIdAndServId(memberId, servId)) {
            favoriteRepository.save(new Favorite(memberId, servId));
        }
        return true;
    }

    @Transactional
    public void remove(String username, String servId) {
        favoriteRepository.deleteByMemberIdAndServId(memberId(username), servId);
    }

    private Long memberId(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username))
                .getId();
    }
}

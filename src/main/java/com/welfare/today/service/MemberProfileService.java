package com.welfare.today.service;

import com.welfare.today.dto.WelfareProfile;
import com.welfare.today.entity.Member;
import com.welfare.today.entity.MemberProfile;
import com.welfare.today.repository.MemberProfileRepository;
import com.welfare.today.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** 회원 계정에 저장된 조건 읽기/저장. 비회원은 이 서비스를 쓰지 않고 세션에만 둔다. */
@Service
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository profileRepository;

    public MemberProfileService(MemberRepository memberRepository, MemberProfileRepository profileRepository) {
        this.memberRepository = memberRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public Optional<WelfareProfile> find(String username) {
        return memberRepository.findByUsername(username)
                .flatMap(member -> profileRepository.findById(member.getId()))
                .map(MemberProfile::toProfile);
    }

    @Transactional
    public void save(String username, WelfareProfile profile) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("로그인한 회원이 DB에 없음: " + username));
        profileRepository.findById(member.getId()).ifPresentOrElse(
                saved -> saved.update(profile),
                () -> profileRepository.save(new MemberProfile(member.getId(), profile)));
    }
}

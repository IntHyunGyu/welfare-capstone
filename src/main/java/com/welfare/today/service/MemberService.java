package com.welfare.today.service;

import com.welfare.today.entity.Member;
import com.welfare.today.repository.FavoriteRepository;
import com.welfare.today.repository.MemberProfileRepository;
import com.welfare.today.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MemberService implements UserDetailsService {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{4,20}$");
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 64;

    private final MemberRepository memberRepository;
    private final MemberProfileRepository profileRepository;
    private final FavoriteRepository favoriteRepository;
    private final PersistentTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Set<String> adminUsernames;

    /**
     * @param adminUsernames 관리자 아이디 목록(콤마 구분, 환경변수 ADMIN_USERNAMES). 관리자 비밀번호를 환경변수에 두지 않도록
     *                       본인이 먼저 가입한 아이디를 적으면 로그인할 때 ADMIN 권한이 붙는다. 목록에서 빼면 다음 로그인부터 해제.
     */
    public MemberService(MemberRepository memberRepository, MemberProfileRepository profileRepository,
                         FavoriteRepository favoriteRepository, PersistentTokenRepository tokenRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${welfare.admin.usernames:}") String adminUsernames) {
        this.memberRepository = memberRepository;
        this.profileRepository = profileRepository;
        this.favoriteRepository = favoriteRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsernames = Arrays.stream(adminUsernames.split(","))
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        String[] roles = adminUsernames.contains(member.getUsername())
                ? new String[]{Member.Role.USER.name(), Member.Role.ADMIN.name()}
                : new String[]{Member.Role.USER.name()};
        return User.withUsername(member.getUsername())
                .password(member.getPasswordHash())
                .roles(roles)
                .build();
    }

    /** 입력 검증 실패 사유를 화면에 보여줄 문장으로 돌려준다. 비어 있으면 가입 가능. */
    public List<String> validateSignUp(String username, String password, String passwordConfirm) {
        List<String> errors = new ArrayList<>();
        if (username == null || !USERNAME.matcher(username).matches()) {
            errors.add("아이디는 영문 소문자, 숫자, 밑줄(_)로 4~20자여야 해요.");
        } else if (memberRepository.existsByUsername(username)) {
            errors.add("이미 사용 중인 아이디예요.");
        }
        if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            errors.add("비밀번호는 8~64자여야 해요.");
        } else if (!password.equals(passwordConfirm)) {
            errors.add("비밀번호 확인이 일치하지 않아요.");
        }
        return errors;
    }

    @Transactional
    public Member signUp(String username, String password) {
        return memberRepository.save(new Member(username, passwordEncoder.encode(password), Member.Role.USER));
    }

    @Transactional(readOnly = true)
    public Member get(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    /** 비밀번호 변경 입력 검증. 비어 있으면 변경 가능. */
    @Transactional(readOnly = true)
    public List<String> validatePasswordChange(String username, String currentPassword,
                                               String newPassword, String newPasswordConfirm) {
        List<String> errors = new ArrayList<>();
        if (!passwordMatches(username, currentPassword)) {
            errors.add("현재 비밀번호가 맞지 않아요.");
            return errors;
        }
        if (newPassword == null || newPassword.length() < PASSWORD_MIN || newPassword.length() > PASSWORD_MAX) {
            errors.add("새 비밀번호는 8~64자여야 해요.");
        } else if (!newPassword.equals(newPasswordConfirm)) {
            errors.add("새 비밀번호 확인이 일치하지 않아요.");
        } else if (newPassword.equals(currentPassword)) {
            errors.add("지금 쓰는 비밀번호와 다른 비밀번호를 입력해 주세요.");
        }
        return errors;
    }

    /**
     * 비밀번호를 바꾸고, 이 회원의 "로그인 상태 유지" 토큰을 모두 지운다 —
     * 비밀번호가 새어 나갔을 수 있는 상황에서 바꾸는 경우가 많아, 다른 기기의 자동 로그인도 끊는다.
     */
    @Transactional
    public void changePassword(String username, String newPassword) {
        get(username).changePasswordHash(passwordEncoder.encode(newPassword));
        tokenRepository.removeUserTokens(username);
    }

    @Transactional(readOnly = true)
    public boolean passwordMatches(String username, String rawPassword) {
        return rawPassword != null && memberRepository.findByUsername(username)
                .map(member -> passwordEncoder.matches(rawPassword, member.getPasswordHash()))
                .orElse(false);
    }

    /** 회원 탈퇴: 즐겨찾기, 저장된 조건, 로그인 유지 토큰, 계정을 지운다. */
    @Transactional
    public void withdraw(String username) {
        Member member = get(username);
        favoriteRepository.deleteByMemberId(member.getId());
        profileRepository.deleteById(member.getId());
        tokenRepository.removeUserTokens(username);
        memberRepository.delete(member);
    }
}

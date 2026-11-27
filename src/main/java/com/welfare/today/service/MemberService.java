package com.welfare.today.service;

import com.welfare.today.entity.Member;
import com.welfare.today.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
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
    private final PasswordEncoder passwordEncoder;
    private final Set<String> adminUsernames;

    /**
     * @param adminUsernames 관리자 아이디 목록(콤마 구분, 환경변수 ADMIN_USERNAMES). 관리자 비밀번호를 환경변수에 두지 않도록
     *                       본인이 먼저 가입한 아이디를 적으면 로그인할 때 ADMIN 권한이 붙는다. 목록에서 빼면 다음 로그인부터 해제.
     */
    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                         @Value("${welfare.admin.usernames:}") String adminUsernames) {
        this.memberRepository = memberRepository;
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
}

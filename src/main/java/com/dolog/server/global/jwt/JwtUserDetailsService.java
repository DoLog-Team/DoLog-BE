//TODO: =======  user domian 만들고 연결 =======


//package com.dolog.server.global.jwt;
//
//import com.dolog.server.domain.user.entity.User;
//import com.dolog.server.domain.user.repository.UserRepository;
//import com.dolog.server.global.security.CustomUserDetails;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.*;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
///**
// * JWT 인증용 사용자 정보 로드 서비스
// */
//@Service
//@RequiredArgsConstructor
//public class JwtUserDetailsService implements UserDetailsService {
//
//    private final UserRepository userRepository;
//
//    @Override
//    public UserDetails loadUserByUsername(String nickname) throws UsernameNotFoundException {
//        User user = userRepository.findByNickname(nickname)
//                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + nickname));
//
//        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
//
//        // 기본 UserDetails 대신 CustomUserDetails 반환 (id 포함)
//        return new CustomUserDetails(user.getId(), user.getNickname(), user.getPassword(), authorities);
//    }
//}

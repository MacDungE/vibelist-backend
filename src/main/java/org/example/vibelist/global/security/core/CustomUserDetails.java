package org.example.vibelist.global.security.core;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    // 사용자 정보를 답는 인터페이스
    // 로그인한 사용자의 정보는 담아두는 역할

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //유저의 권한을 반환
        // Collections.singleton 사용자는 한가지 권한을 갖는 다는 의미
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole().name()));
    }

    /**
     * user의 PK 값
     *
     * @return
     */
    public Long getId() {
        return user.getId();
    }

    @Override
    public String getUsername() {
        return user.getUsername(); // 유저 식별할수 있고 유일한 ID;
    }

    @Override
    public String getPassword() {
        return user.getPassword(); // 유저의 비밀번호
    }

    @Override //계정상태가 활성화 상태인확인하는 것
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    @Override // 이 계정이 만료되었는지
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override // 이계정이 잠겨있는지
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override  // 이 계정이 만료되지 않았는지
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }
}

package org.example.vibelist.global.security.core;


import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 유저입니다" + username));
        return new CustomUserDetails(user);
    }


    public CustomUserDetails loadUserById(Long userid) throws UsernameNotFoundException {
        User user = userRepository.findById(userid).orElseThrow(
                () -> new UsernameNotFoundException("해당 유저가 존재하지 않습니다 -> " + userid));
        return new CustomUserDetails(user);
    }
}

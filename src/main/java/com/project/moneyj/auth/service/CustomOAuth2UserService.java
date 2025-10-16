package com.project.moneyj.auth.service;

import com.project.moneyj.auth.dto.CustomOAuth2User;
import com.project.moneyj.auth.info.KakaoUserInfo;
import com.project.moneyj.auth.info.OAuth2UserInfo;
import com.project.moneyj.user.domain.Role;
import com.project.moneyj.user.domain.User;
import com.project.moneyj.user.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo userInfo;
        if ("kakao".equals(registrationId)) {
            userInfo = new KakaoUserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth 공급자입니다.");
        }

        // DB에 사용자 존재 여부 확인
        Optional<User> optionalUser = userRepository.findByEmail(userInfo.getEmail());
        boolean isFirstLogin = optionalUser.isEmpty();

        User user = optionalUser.orElseGet(() -> {
            User newUser = new User(
                    userInfo.getNickname(),
                    userInfo.getEmail(),
                    userInfo.getProfileImage(),
                    Role.ROLE_USER
            );
            return userRepository.save(newUser);
        });

        // Spring Security 세션 인증 객체 생성
        return new CustomOAuth2User(user, attributes, isFirstLogin);
    }
}
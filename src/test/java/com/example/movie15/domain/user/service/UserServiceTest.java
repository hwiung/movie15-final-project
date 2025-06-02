package com.example.movie15.domain.user.service;

import com.example.movie15.domain.email.service.SignupEmailSenderService;
import com.example.movie15.domain.rabbitmq.producer.RabbitUserSignupProducer;
import com.example.movie15.domain.user.dto.JwtAuthResponse;
import com.example.movie15.domain.user.dto.LoginRequestDto;
import com.example.movie15.domain.user.dto.UserRequestDto;
import com.example.movie15.domain.user.entity.User;
import com.example.movie15.domain.user.repository.UserRepository;
import com.example.movie15.global.exception.BadValueException;
import com.example.movie15.global.exception.ExceptionType;
import com.example.movie15.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SignupEmailSenderService emailSenderService;

    @Mock
    private RabbitUserSignupProducer rabbitUserSignupProducer;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("중복 이메일 회원가입 예외 발생")
    void signup_shouldThrowException_whenEmailAlreadyExists() {
        UserRequestDto dto = new UserRequestDto("email@test.com", "password", "user");
        User existingUser = new User();
        given(userRepository.findByEmail(dto.getEmail())).willReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.signup(dto))
                .isInstanceOf(BadValueException.class)
                .hasMessageContaining(ExceptionType.EXIST_USER.getMessage());
    }

    @Test
    @DisplayName("정상적인 회원가입 성공")
    void signup_shouldSucceed_whenEmailIsNew() throws Exception {
        UserRequestDto dto = new UserRequestDto("email@test.com", "password", "user");
        given(userRepository.findByEmail(dto.getEmail())).willReturn(Optional.empty());
        given(emailSenderService.sendVerificationEmail(any())).willReturn("token");
        given(passwordEncoder.encode(any())).willReturn("encodedPassword");

        userService.signup(dto);

        verify(userRepository).save(any(User.class));
        verify(rabbitUserSignupProducer).userSignupEvent(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_shouldFail_whenEmailNotFound() {
        LoginRequestDto dto = new LoginRequestDto("nonexistent@test.com", "password");
        given(userRepository.findByEmailAndIsDeletedFalse(dto.getEmail())).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(ExceptionType.WRONG_EMAIL.getMessage());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_shouldFail_whenPasswordIncorrect() {
        LoginRequestDto dto = new LoginRequestDto("email@test.com", "wrongpassword");
        User user = new User("email@test.com", "encoded-password", "user");
        user.setVerified(true);
        given(userRepository.findByEmailAndIsDeletedFalse(dto.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(dto.getPassword(), user.getPassword())).willReturn(false);

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(ExceptionType.WRONG_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("로그인 성공 시 JWT 토큰 반환")
    void login_shouldReturnJwtAuthResponse_whenCredentialsAreValid() {
        LoginRequestDto dto = new LoginRequestDto("email@test.com", "password");
        User user = new User("email@test.com", "encoded-password", "user");
        user.setVerified(true);
        given(userRepository.findByEmailAndIsDeletedFalse(dto.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(dto.getPassword(), user.getPassword())).willReturn(true);
        given(jwtProvider.generateAccessToken(anyLong(), anyString())).willReturn("access-token");
        given(jwtProvider.generateRefreshToken(anyLong(), anyString())).willReturn("refresh-token");

        JwtAuthResponse response = userService.login(dto);

        assertEquals("Bearer", response.getTokenAuthScheme());
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    @DisplayName("비밀번호 확인 성공 시 true 반환")
    void checkPassword_shouldReturnTrue_whenPasswordMatches() {
        User user = new User("email@test.com", "encoded-password", "user");
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", user.getPassword())).willReturn(true);

        boolean result = userService.checkPassword(1L, "password");

        assertTrue(result);
    }

    @Test
    @DisplayName("비밀번호 확인 실패 시 false 반환")
    void checkPassword_shouldReturnFalse_whenPasswordDoesNotMatch() {
        User user = new User("email@test.com", "encoded-password", "user");
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", user.getPassword())).willReturn(false);

        boolean result = userService.checkPassword(1L, "wrong");

        assertFalse(result);
    }

    @Test
    @DisplayName("로그아웃 시 토큰 블랙리스트 등록")
    void logout_shouldBlacklistToken_whenTokenIsValid() {
        String token = "valid-token";
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getHeader("Authorization")).willReturn("Bearer " + token);
        given(jwtProvider.extractToken("Bearer " + token)).willReturn(token);
        given(jwtProvider.validateToken(token)).willReturn(true);

        userService.logout(request);

        verify(jwtProvider).blacklistToken(token);
    }
}
package com.example.movie15.domain.user.service;

import com.example.movie15.domain.user.dto.UserResponseDto;
import com.example.movie15.domain.user.entity.Role;
import com.example.movie15.domain.user.entity.User;
import com.example.movie15.domain.user.repository.UserRepository;
import com.example.movie15.global.exception.BadValueException;
import com.example.movie15.global.exception.ExceptionType;
import com.example.movie15.global.exception.NotFoundException;
import com.example.movie15.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("모든 유저 조회 성공")
    void getAllUsers_shouldReturnList() {
        User user = new User("email@test.com", "encoded", "유저");
        given(userRepository.findAll()).willReturn(List.of(user));

        List<UserResponseDto> users = adminService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("email@test.com");
    }

    @Test
    @DisplayName("유저 상세 조회 성공")
    void getUserDetails_shouldReturnUserDto_whenUserExists() {
        User user = new User("admin@naver.com", "encoded", "관리자");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponseDto response = adminService.getUserDetails(1L);

        assertThat(response.getNickname()).isEqualTo("관리자");
    }

    @Test
    @DisplayName("유저 상세 조회 실패 - 유저 없음")
    void getUserDetails_shouldThrow_whenUserNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserDetails(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ExceptionType.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("유저 권한 변경 성공")
    void updateUserRole_shouldSucceed_whenValidRole() {
        User user = new User("admin@naver.com", "encoded", "관리자");
        user.changeRole(Role.USER); // 초기 권한은 USER
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        adminService.updateUserRole(1L, "ADMIN");

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("유저 권한 변경 실패 - 동일한 권한")
    void updateUserRole_shouldThrow_whenSameRole() {
        User user = new User("admin@naver.com", "encoded", "관리자");
        user.changeRole(Role.ADMIN);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.updateUserRole(1L, "ADMIN"))
                .isInstanceOf(BadValueException.class)
                .hasMessageContaining(ExceptionType.ALREADY_SAME_ROLE.getMessage());
    }

    @Test
    @DisplayName("유저 권한 변경 실패 - 잘못된 권한 입력")
    void updateUserRole_shouldThrow_whenInvalidRole() {
        User user = new User("admin@naver.com", "encoded", "관리자");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.updateUserRole(1L, "NOTAROLE"))
                .isInstanceOf(BadValueException.class)
                .hasMessageContaining(ExceptionType.INVALID_USER_ROLE.getMessage());
    }

    @Test
    @DisplayName("유저 삭제 성공")
    void deleteUser_shouldSucceed_whenUserExistsAndNotDeleted() {
        User user = new User("admin@naver.com", "encoded", "관리자");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        adminService.deleteUser(1L);

        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("유저 삭제 실패 - 이미 삭제된 유저")
    void deleteUser_shouldThrow_whenUserAlreadyDeleted() {
        User user = new User("admin@naver.com", "encoded", "관리자");
        user.updateIsDeleted();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.deleteUser(1L))
                .isInstanceOf(BadValueException.class)
                .hasMessageContaining(ExceptionType.ALREADY_DELETED_USER.getMessage());
    }
}

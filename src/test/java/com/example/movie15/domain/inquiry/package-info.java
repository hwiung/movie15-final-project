package com.example.movie15.domain.inquiry;

import com.example.movie15.domain.inquiry.dto.InquiryRequestDto;
import com.example.movie15.domain.inquiry.entity.Inquiry;
import com.example.movie15.domain.inquiry.entity.InquiryFile;
import com.example.movie15.domain.inquiry.repository.InquiryFileRepository;
import com.example.movie15.domain.inquiry.repository.InquiryRepository;
import com.example.movie15.domain.inquiry.service.InquiryService;
import com.example.movie15.domain.user.entity.User;
import com.example.movie15.domain.user.repository.UserRepository;
import com.example.movie15.global.entity.File;
import com.example.movie15.global.exception.BadValueException;
import com.example.movie15.global.exception.ForbiddenException;
import com.example.movie15.global.model.FileExtension;
import com.example.movie15.global.model.FileType;
import com.example.movie15.global.repository.FileRepository;
import com.example.movie15.global.service.FileUploaderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest_Update {

    @Mock InquiryRepository inquiryRepository;
    @Mock InquiryFileRepository inquiryFileRepository;
    @Mock UserRepository userRepository;
    @Mock FileUploaderService fileUploaderService;
    @Mock FileRepository fileRepository;

    @InjectMocks InquiryService inquiryService;

    @Test
    @DisplayName("문의 수정 성공 - 본인 + 변경사항 있음 + 파일 교체")
    void updateInquiry_success_withFile() {
        // given
        Long userId = 1L;
        Long inquiryId = 10L;
        User user = new User("test@example.com", "pw", "홍길동");
        ReflectionTestUtils.setField(user, "id", userId);

        Inquiry inquiry = new Inquiry("제목1", "내용1", user);
        ReflectionTestUtils.setField(inquiry, "id", inquiryId);

        InquiryRequestDto dto = new InquiryRequestDto("제목2", "내용2");

        MultipartFile mockFile = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(mockFile);

        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry));
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(123L);
        when(fileUploaderService.uploadFile(mockFile)).thenReturn("https://url.com/test.jpg");

        // when
        inquiryService.updateInquiry(inquiryId, dto, files, userId);

        // then
        verify(inquiryRepository).save(any());
        verify(fileUploaderService).uploadFile(mockFile);
        verify(fileRepository).save(any());
        verify(inquiryFileRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("문의 수정 실패 - 본인이 아닌 경우")
    void updateInquiry_forbidden() {
        // given
        Long writerId = 1L;
        Long requesterId = 2L;
        Long inquiryId = 99L;

        User writer = new User("a@a.com", "pw", "작성자");
        ReflectionTestUtils.setField(writer, "id", writerId);
        Inquiry inquiry = new Inquiry("t", "c", writer);
        ReflectionTestUtils.setField(inquiry, "id", inquiryId);

        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry));

        InquiryRequestDto dto = new InquiryRequestDto("t", "c");

        // then
        assertThrows(ForbiddenException.class, () -> {
            inquiryService.updateInquiry(inquiryId, dto, null, requesterId);
        });

        verify(inquiryRepository, never()).save(any());
    }

    @Test
    @DisplayName("문의 수정 실패 - 변경사항 없음")
    void updateInquiry_noChanges() {
        // given
        Long userId = 1L;
        Long inquiryId = 5L;
        User user = new User("a@a.com", "pw", "작성자");
        ReflectionTestUtils.setField(user, "id", userId);

        Inquiry inquiry = new Inquiry("same", "same", user);
        ReflectionTestUtils.setField(inquiry, "id", inquiryId);

        InquiryRequestDto dto = new InquiryRequestDto("same", "same");

        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry));

        // then
        assertThrows(BadValueException.class, () -> {
            inquiryService.updateInquiry(inquiryId, dto, null, userId);
        });

        verify(inquiryRepository, never()).save(any());
    }
}

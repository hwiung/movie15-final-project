package com.example.movie15.domain.inquiry.service;

import com.example.movie15.domain.inquiry.dto.InquiryRequestDto;
import com.example.movie15.domain.inquiry.enums.InquiryStatus;
import com.example.movie15.domain.inquiry.repository.InquiryRepository;
import com.example.movie15.domain.user.entity.User;
import com.example.movie15.domain.user.repository.UserRepository;
import com.example.movie15.global.repository.FileRepository;
import com.example.movie15.global.service.FileUploaderService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InquiryServiceTest {

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileUploaderService fileUploaderService;

    @Test
    @DisplayName("문의 생성 - 파일 없이")
    void createInquiry_withoutFiles_success() throws Exception {
        // given
        User user = userRepository.save(new User("aaa1@email.com", "bbb123!@", "bbb1"));
        InquiryRequestDto dto = new InquiryRequestDto("문의 제목", "문의 내용", null);

        // when
        inquiryService.createInquiry(dto, null, user.getId());

        // then
        assertThat(inquiryRepository.findAll()).hasSize(1);
        assertThat(inquiryRepository.findAll().get(0).getSubject()).isEqualTo("문의 제목");
    }

    @Test
    @DisplayName("문의 생성 - 파일 포함")
    void createInquiry_withFiles_success() throws Exception {
        // given
        User user = userRepository.save(new User("cccc1@email.com", "cccc123!@", "ccc1"));

        MockMultipartFile mockfile = new MockMultipartFile(
                "file", "hello.png", "image/png", "fake-image".getBytes()
        );

        List<MultipartFile> files = List.of(mockfile);

        InquiryRequestDto dto = new InquiryRequestDto("파일 있는 문의", "내용", files);

        // when
        inquiryService.createInquiry(dto, files, user.getId());

        // then
        assertThat(inquiryRepository.findAll()).hasSize(1);
        assertThat(fileRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("문의 상태 변경 - 관리자가 답변 완료로 변경")
    void updateInquiryStatus_success() throws Exception {
        // given
        User user = userRepository.save(new User("admin@email.com", "admin123!@#", "admin1"));
        InquiryRequestDto dto = new InquiryRequestDto("제목", "내용", null);
        inquiryService.createInquiry(dto, null, user.getId());
        Long inquiryId = inquiryRepository.findAll().get(0).getId();

        // when
        inquiryService.updateInquiryStatus(inquiryId, InquiryStatus.ANSWERED);

        // then
        assertThat(inquiryRepository.findById(inquiryId).get().getStatus()).isEqualTo(InquiryStatus.ANSWERED);
    }
}
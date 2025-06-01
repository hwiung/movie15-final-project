package com.example.movie15.domain.inquiry.service;

import com.example.movie15.domain.inquiry.dto.InquiryRequestDto;
import com.example.movie15.domain.inquiry.dto.InquiryResponseDto;
import com.example.movie15.domain.inquiry.entity.Inquiry;
import com.example.movie15.domain.inquiry.entity.InquiryFile;
import com.example.movie15.domain.inquiry.enums.InquiryStatus;
import com.example.movie15.domain.inquiry.repository.InquiryFileRepository;
import com.example.movie15.domain.inquiry.repository.InquiryQueryRepository;
import com.example.movie15.domain.inquiry.repository.InquiryRepository;
import com.example.movie15.domain.user.entity.User;
import com.example.movie15.domain.user.repository.UserRepository;
import com.example.movie15.global.entity.File;
import com.example.movie15.global.exception.*;
import com.example.movie15.global.model.FileExtension;
import com.example.movie15.global.model.FileType;
import com.example.movie15.global.repository.FileRepository;
import com.example.movie15.global.security.JwtProvider;
import com.example.movie15.global.security.service.UserDetailsImpl;
import com.example.movie15.global.service.FileUploaderService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final FileUploaderService fileUploaderService;
    private final FileRepository fileRepository;
    private final InquiryFileRepository inquiryFileRepository;


    /**
     * 문의 사항 생성 (첨부파일 포함)
     */
    @Transactional
    public void createInquiry(InquiryRequestDto dto, List<MultipartFile> files, Long userId) throws IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ExceptionType.USER_NOT_FOUND));

        Inquiry inquiry = new Inquiry(dto.getSubject(), dto.getContent(), user);
        inquiryRepository.save(inquiry);

        if (files != null && !files.isEmpty()) {
            List<InquiryFile> inquiryFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                InquiryFile inquiryFile = uploadFile(file, inquiry);
                inquiryFiles.add(inquiryFile);
            }
            inquiryFileRepository.saveAll(inquiryFiles);
        }

        // 반환값 없음 (void)
    }

    /**
     * 파일 업로드 후 InquiryFile 객체 생성
     */
    private InquiryFile uploadFile(MultipartFile file, Inquiry inquiry) {
        try {
            //  파일 업로드
            String fileUrl = fileUploaderService.uploadFile(file);
            if (fileUrl == null || fileUrl.isEmpty()) {
                throw new IOException("파일 업로드 실패");
            }

            //  파일명 검증
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                throw new BadValueException(ExceptionType.INVALID_FILE_EXTENSION);
            }

            //  파일 확장자 검증
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toUpperCase();
            if (!FileExtension.isValidExtension(extension)) {
                throw new BadValueException(ExceptionType.INVALID_FILE_EXTENSION);
            }

            //  파일 엔티티 생성 후 저장
            File fileEntity = new File(
                    fileUrl,
                    originalFilename,
                    (int) file.getSize(),
                    FileExtension.valueOf(extension), //  문자열을 Enum으로 변환
                    FileType.INQUIRY
            );

            fileRepository.save(fileEntity);
            return new InquiryFile(inquiry, fileEntity);

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류 발생: " + e.getMessage());
        }
    }

    // 문의 사항 조회(사용자 본인)
    public Page<InquiryResponseDto> getUserInquiries(Long userId, Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findAllByUserId(userId, pageable);
        return inquiries.map(this::mapToResponseDto);
    }

    // 문의 사항 전체 조회(관리자)
    public Page<InquiryResponseDto> getAllInquiries(Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findAll(pageable);
        return inquiries.map(this::mapToResponseDto);
    }

    // 문의 사항 조회(관리자 -> 특정 사용자)
    public Page<InquiryResponseDto> getInquiriesByUserIdForAdmin(Long userId, Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findAllByUserId(userId, pageable);
        return inquiries.map(this::mapToResponseDto);
    }

    @Transactional
    public InquiryResponseDto updateInquiry(Long id, InquiryRequestDto dto, List<MultipartFile> files, Long userId) {
        //  문의 사항 조회 (존재하지 않으면 예외 발생)
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionType.INQUIRY_NOT_FOUND));

        //  사용자가 작성한 문의 사항인지 확인
        if (!inquiry.getUser().getId().equals(userId)) {
            throw new ForbiddenException(ExceptionType.INQUIRY_FORBIDDEN);
        }

        //  변경 사항 확인
        boolean hasContentChanges = !inquiry.getSubject().equals(dto.getSubject()) || !inquiry.getContent().equals(dto.getContent());
        boolean hasFileChanges = files != null && !files.isEmpty();

        if (!hasContentChanges && !hasFileChanges) {
            throw new BadValueException(ExceptionType.INQUIRY_NO_CHANGES);
        }

        // 문의 내용 업데이트
        inquiry.update(dto.getSubject(), dto.getContent());

        // 기존 파일 삭제 후 새로운 파일 업로드
        if (hasFileChanges) {
            List<InquiryFile> existingFiles = inquiryFileRepository.findByInquiry(inquiry);

            //  `inquiry_file` 테이블의 데이터 먼저 삭제
            inquiryFileRepository.deleteAll(existingFiles);

            //  `file` 테이블의 데이터 삭제
            for (InquiryFile inquiryFile : existingFiles) {
                File file = inquiryFile.getFile();
                fileUploaderService.deleteFile(file.getUrl()); // S3 삭제 (선택)
                fileRepository.delete(file);
            }

            // 새로운 파일 업로드 및 저장
            List<InquiryFile> inquiryFiles = files.stream()
                    .map(file -> uploadFile(file, inquiry))
                    .collect(Collectors.toList());

            inquiryFileRepository.saveAll(inquiryFiles);
        }

        Inquiry updatedInquiry = inquiryRepository.save(inquiry);
        return mapToResponseDto(updatedInquiry);
    }

    // 문의 사항 삭제(사용자)
    @Transactional
    public void deleteInquiry(Long id, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionType.INQUIRY_NOT_FOUND));

        // 사용자가 작성한 문의 사항인지 확인
        if (!inquiry.getUser().getId().equals(userId)) {
            throw new ForbiddenException(ExceptionType.INQUIRY_FORBIDDEN);
        }

        if (inquiry.isAnswered()) {
            throw new ConflictException(ExceptionType.INQUIRY_ALREADY_ANSWERED);
        }

        //  inquiry_file 데이터를 먼저 삭제
        List<InquiryFile> inquiryFiles = inquiryFileRepository.findByInquiry(inquiry);
        inquiryFileRepository.deleteAll(inquiryFiles); // 🔥 inquiry_file 테이블의 데이터를 먼저 삭제

        // file 데이터 삭제
        for (InquiryFile inquiryFile : inquiryFiles) {
            File file = inquiryFile.getFile();

            // 🔥 S3에서도 파일 삭제 (선택적)
            fileUploaderService.deleteFile(file.getUrl());

            // 🔥 file 테이블에서 삭제
            fileRepository.delete(file);
        }

        // inquiry 삭제
        inquiryRepository.delete(inquiry);
    }

    // 문의 사항 삭제(관리자)
    public void deleteInquiryByAdmin(Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionType.INQUIRY_NOT_FOUND));
        inquiryRepository.delete(inquiry);
    }

    @Transactional
    public void updateInquiryStatus(Long id, InquiryStatus status) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionType.INQUIRY_NOT_FOUND));

        inquiry.changeStatus(status);
        inquiryRepository.save(inquiry);
    }

    private InquiryResponseDto mapToResponseDto(Inquiry inquiry) {
        return new InquiryResponseDto(
                inquiry.getId(),
                inquiry.getSubject(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getInquiryFiles().stream()
                        .map(inquiryFile -> inquiryFile.getFile().getUrl()) // 파일 URL만 추출
                        .collect(Collectors.toList()),
                inquiry.getCreatedAt(),
                inquiry.getModifiedAt()
        );
    }

    //동적 쿼리 메소드
    private final InquiryQueryRepository inquiryQueryRepository;

    public Page<InquiryResponseDto> searchInquiries(
            Long userId,
            InquiryStatus status,
            String keyword,
            Pageable pageable
    ) {
        return inquiryQueryRepository.searchInquiries(userId, status, keyword, pageable);
    }
}
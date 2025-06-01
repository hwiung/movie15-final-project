package com.example.movie15.domain.inquiry.controller;

import com.example.movie15.domain.inquiry.dto.InquiryRequestDto;
import com.example.movie15.domain.inquiry.dto.InquiryResponseDto;
import com.example.movie15.domain.inquiry.dto.InquiryStatusRequestDto;
import com.example.movie15.domain.inquiry.enums.InquiryStatus;
import com.example.movie15.domain.inquiry.service.InquiryService;
import com.example.movie15.global.security.service.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
@RestController
public class InquiryController {

    private final InquiryService inquiryService;

    // 문의 사항 작성
    @PostMapping
    public ResponseEntity<String> createInquiry(
            @RequestPart(name = "inquiry") String inquiryJson,
            @RequestPart(name = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetailsImpl userDetails) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        InquiryRequestDto dto = objectMapper.readValue(inquiryJson, InquiryRequestDto.class);
        Long userId = userDetails.getId();

        inquiryService.createInquiry(dto, files, userId);
        return ResponseEntity.status(201).body("문의 생성완료 ");
    }

    // 문의 사항 조회(사용자 본인)
    @GetMapping("/user")
    public ResponseEntity<Page<InquiryResponseDto>> getUserInquiries(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long userId = userDetails.getId();
        Page<InquiryResponseDto> inquiries = inquiryService.getUserInquiries(userId, pageable);
        return ResponseEntity.ok(inquiries);
    }

    // 문의 사항 전체 조회(관리자)
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<InquiryResponseDto>> getAllInquiries(
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<InquiryResponseDto> response = inquiryService.getAllInquiries(pageable);
        return ResponseEntity.ok(response);
    }

    // 문의 사항 조회(관리자 -> 특정 사용자)
    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<InquiryResponseDto>> getInquiriesByUserIdForAdmin(
            @PathVariable Long userId,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<InquiryResponseDto> response = inquiryService.getInquiriesByUserIdForAdmin(userId, pageable);
        return ResponseEntity.ok(response);
    }

    // 문의 사항 수정
    @PatchMapping("/{id}")
    public ResponseEntity<InquiryResponseDto> updateInquiry(
            @PathVariable Long id,
            @RequestPart(name = "inquiry") String inquiryJson,
            @RequestPart(name = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetailsImpl userDetails) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        InquiryRequestDto dto = objectMapper.readValue(inquiryJson, InquiryRequestDto.class);

        Long userId = userDetails.getId();

        InquiryResponseDto response = inquiryService.updateInquiry(id, dto, files, userId);
        return ResponseEntity.ok(response);
    }

    // 문의 사항 삭제(사용자) -> 답변 상태: ANSWERED일 경우, 삭제 불가.
    @DeleteMapping("/{id}")
    @PreAuthorize("!hasRole('ADMIN')")
    public ResponseEntity<String> deleteInquiry(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long userId = userDetails.getId();
        inquiryService.deleteInquiry(id, userId);
        return ResponseEntity.ok("삭제되었습니다");
    }

    // 문의 사항 삭제(관리자)
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteInquiryByAdmin(@PathVariable Long id) {
        inquiryService.deleteInquiryByAdmin(id);
        return ResponseEntity.ok("해당 문의는 관리자가 삭제했습니다.");
    }

    // 문의 사항 상태 변경(관리자)
    @PatchMapping("/admin/status/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateInquiryStatus(@PathVariable Long id, @RequestBody InquiryStatusRequestDto requestDto) {

        inquiryService.updateInquiryStatus(id, requestDto.getStatus());
        return ResponseEntity.ok("문의 사항의 상태가 변경됐습니다.");
    }

    //검색 조건으로 문의 조회(관리자)
    @GetMapping("/admin/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<InquiryResponseDto>> searchInquiries(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<InquiryResponseDto> result = inquiryService.searchInquiries(userId, status, keyword, pageable);
        return ResponseEntity.ok(result);
    }
}

package com.example.movie15.domain.inquiry.repository;

import com.example.movie15.domain.inquiry.dto.InquiryResponseDto;
import com.example.movie15.domain.inquiry.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InquiryQueryRepository {
    Page<InquiryResponseDto> searchInquiries(Long userId, InquiryStatus status, String keyword, Pageable pageable);
}

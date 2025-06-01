package com.example.movie15.domain.inquiry.repository;

import com.example.movie15.domain.inquiry.dto.InquiryResponseDto;
import com.example.movie15.domain.inquiry.entity.Inquiry;
import com.example.movie15.domain.inquiry.entity.QInquiry;
import com.example.movie15.domain.inquiry.entity.QInquiryFile;
import com.example.movie15.domain.inquiry.enums.InquiryStatus;
import com.example.movie15.global.entity.QFile;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class InquiryQueryRepositoryImpl implements InquiryQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<InquiryResponseDto> searchInquiries(Long userId, InquiryStatus status, String keyword, Pageable pageable) {
        QInquiry inquiry = QInquiry.inquiry;
        QInquiryFile inquiryFile = QInquiryFile.inquiryFile;
        QFile file = QFile.file;

        BooleanBuilder builder = new BooleanBuilder();

        if (userId != null) {
            builder.and(inquiry.user.id.eq(userId));
        }

        if (status != null) {
            builder.and(inquiry.status.eq(status));
        }

        if (keyword != null && !keyword.isBlank()) {
            builder.and(
                    inquiry.subject.containsIgnoreCase(keyword)
                            .or(inquiry.content.containsIgnoreCase(keyword))
            );
        }

        List<Inquiry> content = queryFactory
                .selectFrom(inquiry)
                .leftJoin(inquiry.inquiryFiles, inquiryFile).fetchJoin()
                .leftJoin(inquiryFile.file, file).fetchJoin()
                .where(builder)
                .orderBy(inquiry.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(inquiry.count())
                .from(inquiry)
                .where(builder)
                .fetchOne();

        List<InquiryResponseDto> response = content.stream()
                .map(InquiryResponseDto::new)
                .collect(Collectors.toList());

        return new PageImpl<>(response, pageable, total);
    }
}

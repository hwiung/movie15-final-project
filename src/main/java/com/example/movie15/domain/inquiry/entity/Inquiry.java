package com.example.movie15.domain.inquiry.entity;

import com.example.movie15.domain.inquiry.enums.InquiryStatus;
import com.example.movie15.domain.user.entity.User;
import com.example.movie15.global.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "제목은 필수 입력값입니다.")
    private String subject;

    @NotBlank(message = "내용은 필수 입력값입니다.")
    private String content;

    @Enumerated(EnumType.STRING)
    private InquiryStatus status = InquiryStatus.PENDING; // 기본값: PENDING(답변 대기 중)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<InquiryFile> inquiryFiles = new ArrayList<>();

    public Inquiry(String subject, String content, User user) {
        this.subject = subject;
        this.content = content;
        this.user = user;
    }

    public void update(String subject, String content) {
        this.subject = subject;
        this.content = content;
    }

    public void changeStatus(InquiryStatus newStatus) {
        this.status = newStatus;
    }

    public boolean isAnswered() {
        return this.status == InquiryStatus.ANSWERED;
    }

    public List<InquiryFile> getInquiryFiles() {
        return inquiryFiles;
    }
}
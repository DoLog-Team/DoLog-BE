package com.dolog.server.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    private static final DateTimeFormatter formatterYmd = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter formatterMd = DateTimeFormatter.ofPattern("MM.dd");

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // LocalDate -> yyyy.mm.dd
    public String formatYmd(LocalDate date) {
        return (date == null) ? null : date.format(formatterYmd);
    }

    // LocalDate -> mm.dd
    public String formatMd(LocalDate date){
        return (date == null) ? null : date.format(formatterMd);
    }
}

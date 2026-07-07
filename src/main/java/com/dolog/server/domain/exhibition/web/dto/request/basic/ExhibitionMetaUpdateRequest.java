package com.dolog.server.domain.exhibition.web.dto.request.basic;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ExhibitionMetaUpdateRequest {
    private String ogTitle;
    private String ogDescription;
    private MultipartFile ogImage;
    private MultipartFile faviconImg;
}

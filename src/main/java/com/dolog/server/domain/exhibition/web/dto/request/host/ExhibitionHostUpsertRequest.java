package com.dolog.server.domain.exhibition.web.dto.request.host;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExhibitionHostUpsertRequest {
    private String name;
    private MultipartFile img;
    private String description;
    private String email;
}

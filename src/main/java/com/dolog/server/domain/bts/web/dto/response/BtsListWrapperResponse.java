package com.dolog.server.domain.bts.web.dto.response;

import java.util.List;

public record BtsListWrapperResponse(List<BtsListResponse> content, int totalElements) {}

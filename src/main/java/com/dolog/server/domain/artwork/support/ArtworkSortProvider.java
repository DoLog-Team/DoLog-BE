package com.dolog.server.domain.artwork.support;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ArtworkSortProvider {

    public Sort getSort(String sort) {

        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "orderIndex");
        }

        return switch (sort) {

            case "latest" ->
                    Sort.by(Sort.Direction.DESC, "createdAt");

            case "oldest" ->
                    Sort.by(Sort.Direction.ASC, "createdAt");

            case "title" ->
                    Sort.by(Sort.Direction.ASC, "title");

            default ->
                    Sort.by(Sort.Direction.ASC, "orderIndex");
        };
    }
}
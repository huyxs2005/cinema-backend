package com.cinema.hub.backend.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PromotionView {
    private final Integer id;
    private final String title;
    private final String description;
    private final String validThrough;
}

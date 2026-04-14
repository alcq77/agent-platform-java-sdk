package io.github.alcq77.cqgent.product.sdk.tool;

import io.github.alcq77.cqgent.product.spi.tool.ProductTool;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CurrentTimeProductTool implements ProductTool {

    @Override
    public String name() {
        return "current_time";
    }

    @Override
    public boolean supports(String userInput) {
        if (userInput == null) {
            return false;
        }
        String text = userInput.toLowerCase();
        return text.contains("时间") || text.contains("date") || text.contains("time");
    }

    @Override
    public String execute(String userInput) {
        return OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}

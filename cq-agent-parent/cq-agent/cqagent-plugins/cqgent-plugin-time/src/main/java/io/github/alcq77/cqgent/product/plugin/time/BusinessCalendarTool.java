package io.github.alcq77.cqgent.product.plugin.time;

import io.github.alcq77.cqgent.product.spi.tool.ProductTool;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 示例插件：返回业务日期。
 */
public class BusinessCalendarTool implements ProductTool {

    @Override
    public String name() {
        return "business_calendar";
    }

    @Override
    public boolean supports(String userInput) {
        if (userInput == null) {
            return false;
        }
        String text = userInput.toLowerCase();
        return text.contains("业务日期") || text.contains("business date");
    }

    @Override
    public String execute(String userInput) {
        return "今日业务日期: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}

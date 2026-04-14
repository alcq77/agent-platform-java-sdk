package io.github.alcq77.cqgent.product.spi.tool;

public interface ProductTool {

    String name();

    boolean supports(String userInput);

    String execute(String userInput);
}

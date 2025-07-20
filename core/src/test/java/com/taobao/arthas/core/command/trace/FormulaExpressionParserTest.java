package com.taobao.arthas.core.command.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormulaExpressionParserTest {

    @Test
    void parse() {
        FormulaExpressionParser parser = new FormulaExpressionParser();

        ExecutionContext context = new ExecutionContext();
        context.addMetric("content-length","81");
        context.addMetric("header-length","219");
        Object parse = parser.parse("metrics.content-length + metrics.header-length", context);
        System.out.println(parse);
    }
}
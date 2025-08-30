package kaitai

import kotlin.test.Test
import decoders.Kaitai
import decoders.MutableKaitaiTree
import decoders.Kaitai.TokenType

class KaitaiExpressionTests {
    @Test
    fun tokenizerTest() {
        val kaitai = Kaitai("tokenizer", KTStruct())
        val expressionParser = kaitai.ExpressionParser(MutableKaitaiTree(ioStream = booleanArrayOf()), KTStruct(), KTStruct(), booleanArrayOf(), 0, null)

        var expression: String = "13_37  "
        var result:  MutableList<Pair<Kaitai.TokenType, dynamic>> = expressionParser.tokenizeExpression(expression)
        var expected: Pair<TokenType, dynamic> = Pair(TokenType.INTEGER, 1337L)
        check(result.size == 1) {"The expected size is 0, actual size is ${result.size}"}
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "0B101010"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.INTEGER, 42L)
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "0o644"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.INTEGER, 420L)
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "0x3039"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.INTEGER, 12345L)
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "-+-123.0"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 4) {"The expected size is 4, actual size is ${result.size}"}
        expected = Pair(TokenType.MINUS, "-")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}
        expected = Pair(TokenType.PLUS, "+")
        check(result[1] == expected) {"The expected token is $expected, actual token is ${result[1]}"}
        expected = Pair(TokenType.MINUS, "-")
        check(result[2] == expected) {"The expected token is $expected, actual token is ${result[2]}"}
        expected = Pair(TokenType.FLOAT, 123.0F)
        check(result[3] == expected) {"The expected token is $expected, actual token is ${result[3]}"}

        expression = "345.6789E-4"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.FLOAT, 0.03456789)
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "true"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.BOOLEAN, true)
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "false"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.BOOLEAN, false)
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "\"Hello World, HI\""
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.STRING, "Hello World, HI")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "'   '"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.STRING, "   ")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "some_type::some_enum::some_value"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.ENUMCALL, "some_type::some_enum::some_value")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "some_field.as<some_type>.another_field.to_i(10)"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 7) {"The expected size is 7, actual size is ${result.size}"}
        expected = Pair(TokenType.IDENTIFIER, "some_field")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}
        expected = Pair(TokenType.DOT, ".")
        check(result[1] == expected) {"The expected token is $expected, actual token is ${result[1]}"}
        expected = Pair(TokenType.CAST, "some_type")
        check(result[2] == expected) {"The expected token is $expected, actual token is ${result[2]}"}
        expected = Pair(TokenType.DOT, ".")
        check(result[3] == expected) {"The expected token is $expected, actual token is ${result[3]}"}
        expected = Pair(TokenType.IDENTIFIER, "another_field")
        check(result[4] == expected) {"The expected token is $expected, actual token is ${result[4]}"}
        expected = Pair(TokenType.DOT, ".")
        check(result[5] == expected) {"The expected token is $expected, actual token is ${result[5]}"}
        expected = Pair(TokenType.IDENTIFIER, "to_i")
        check((result[6].second as Pair<Pair<TokenType, dynamic>, Pair<TokenType, dynamic>>).first == expected) {"The expected token is $expected, actual token is ${(result[6].second as Pair<Pair<TokenType, dynamic>, Pair<TokenType, dynamic>>).first}"}
        expected = Pair(TokenType.PARENTHESES, "10")
        check((result[6].second as Pair<Pair<TokenType, dynamic>, Pair<TokenType, dynamic>>).second == expected) {"The expected token is $expected, actual token is ${(result[6].second as Pair<Pair<TokenType, dynamic>, Pair<TokenType, dynamic>>).second}"}

        expression = "(some_expression * 5 - (another_expression))"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.PARENTHESES, "some_expression * 5 - (another_expression)")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "[3, 4, 5, 6][3-1]"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 2) {"The expected size is 2, actual size is ${result.size}"}
        expected = Pair(TokenType.ARRAY, "3, 4, 5, 6")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}
        expected = Pair(TokenType.INDEX, "3-1")
        check(result[1] == expected) {"The expected token is $expected, actual token is ${result[1]}"}

        expression = "(some_expression * 5 - (another_expression))"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 1) {"The expected size is 1, actual size is ${result.size}"}
        expected = Pair(TokenType.PARENTHESES, "some_expression * 5 - (another_expression)")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}

        expression = "+ - * / %"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 5) {"The expected size is 5, actual size is ${result.size}"}
        expected = Pair(TokenType.PLUS, "+")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}
        expected = Pair(TokenType.MINUS, "-")
        check(result[1] == expected) {"The expected token is $expected, actual token is ${result[1]}"}
        expected = Pair(TokenType.MUL, "*")
        check(result[2] == expected) {"The expected token is $expected, actual token is ${result[2]}"}
        expected = Pair(TokenType.DIV, "/")
        check(result[3] == expected) {"The expected token is $expected, actual token is ${result[3]}"}
        expected = Pair(TokenType.MODULO, "%")
        check(result[4] == expected) {"The expected token is $expected, actual token is ${result[4]}"}

        expression = "< <= > >= == !="
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 6) {"The expected size is 5, actual size is ${result.size}"}
        expected = Pair(TokenType.LESS, "<")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}
        expected = Pair(TokenType.LESSEQUAL, "<=")
        check(result[1] == expected) {"The expected token is $expected, actual token is ${result[1]}"}
        expected = Pair(TokenType.GREATER, ">")
        check(result[2] == expected) {"The expected token is $expected, actual token is ${result[2]}"}
        expected = Pair(TokenType.GREATEREQUAL, ">=")
        check(result[3] == expected) {"The expected token is $expected, actual token is ${result[3]}"}
        expected = Pair(TokenType.EQUAL, "==")
        check(result[4] == expected) {"The expected token is $expected, actual token is ${result[4]}"}
        expected = Pair(TokenType.NOTEQUAL, "!=")
        check(result[5] == expected) {"The expected token is $expected, actual token is ${result[5]}"}

        expression = "<< >> & | ^"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 5) {"The expected size is 5, actual size is ${result.size}"}
        expected = Pair(TokenType.LSHIFT, "<<")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}
        expected = Pair(TokenType.RSHIFT, ">>")
        check(result[1] == expected) {"The expected token is $expected, actual token is ${result[1]}"}
        expected = Pair(TokenType.BITWISEAND, "&")
        check(result[2] == expected) {"The expected token is $expected, actual token is ${result[2]}"}
        expected = Pair(TokenType.BITWISEOR, "|")
        check(result[3] == expected) {"The expected token is $expected, actual token is ${result[3]}"}
        expected = Pair(TokenType.BITWISEXOR, "^")
        check(result[4] == expected) {"The expected token is $expected, actual token is ${result[4]}"}

        expression = "not and ? or : andor"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 6) {"The expected size is 5, actual size is ${result.size}"}
        expected = Pair(TokenType.BOOLEANNOT, "not")
        check(result[0] == expected) {"The expected token is $expected, actual token is ${result[0]}"}
        expected = Pair(TokenType.BOOLEANAND, "and")
        check(result[1] == expected) {"The expected token is $expected, actual token is ${result[1]}"}
        expected = Pair(TokenType.QUESTIONMARK, "?")
        check(result[2] == expected) {"The expected token is $expected, actual token is ${result[2]}"}
        expected = Pair(TokenType.BOOLEANOR, "or")
        check(result[3] == expected) {"The expected token is $expected, actual token is ${result[3]}"}
        expected = Pair(TokenType.COLON, ":")
        check(result[4] == expected) {"The expected token is $expected, actual token is ${result[4]}"}
        expected = Pair(TokenType.IDENTIFIER, "andor")
        check(result[5] == expected) {"The expected token is $expected, actual token is ${result[5]}"}
    }

    @Test
    fun operationsTest() {
        val kaitai = Kaitai("operations", KTStruct())
        val expressionParser = kaitai.ExpressionParser(MutableKaitaiTree(ioStream = booleanArrayOf()), KTStruct(), KTStruct(), booleanArrayOf(), 0, null)

        var expression: String = "-5 + 6 + 3.3 + -2.0"
        var result: dynamic = expressionParser.parseExpression(expression)
        var expected: dynamic = 2.3
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "'Hello' + ' World!'"
        result = expressionParser.parseExpression(expression)
        expected = "Hello World!"
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "10 - 4 - 1.0 - 6.5"
        result = expressionParser.parseExpression(expression)
        expected = -1.5
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "+0b111 * -5 * -0.1 * 2.0"
        result = expressionParser.parseExpression(expression)
        expected = 7
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "7 / 2"
        result = expressionParser.parseExpression(expression)
        expected = 3
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "9 / 2 / 0.5 / -4"
        result = expressionParser.parseExpression(expression)
        expected = -2
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "10 % 3"
        result = expressionParser.parseExpression(expression)
        expected = 1
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "-5 % 3.0"
        result = expressionParser.parseExpression(expression)
        expected = 1
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "10.9 % 4.2"
        result = expressionParser.parseExpression(expression)
        expected = 2.5
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "5 < 4"
        result = expressionParser.parseExpression(expression)
        expected = false
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "3 <= 3.0"
        result = expressionParser.parseExpression(expression)
        expected = true
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "'C' > 'A'"
        result = expressionParser.parseExpression(expression)
        expected = true
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "-10.0 >= 10.0"
        result = expressionParser.parseExpression(expression)
        expected = false
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "(3 == -3) != (3.0 == 4.5)"
        result = expressionParser.parseExpression(expression)
        expected = false
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "3 == 3.0"
        result = expressionParser.parseExpression(expression)
        expected = true
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "'ABC' == 'abc'"
        result = expressionParser.parseExpression(expression)
        expected = false
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        // TODO: check equal with enums

        expression = "5 << 2"
        result = expressionParser.parseExpression(expression)
        expected = 20
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "13194139533312 >> 42"
        result = expressionParser.parseExpression(expression)
        expected = 3
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "0b101 & 0B11"
        result = expressionParser.parseExpression(expression)
        expected = 1
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "0b10101 | 0B11"
        result = expressionParser.parseExpression(expression)
        expected = 23
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "0b10101 ^ 0B11"
        result = expressionParser.parseExpression(expression)
        expected = 22
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "true and false ? not false : not true"
        result = expressionParser.parseExpression(expression)
        expected = false
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "true or false ? not false : not true"
        result = expressionParser.parseExpression(expression)
        expected = true
        check(result == expected) {"The expected result is $expected, actual result is $result"}

    }

    @Test
    fun arrayTest() {
        val kaitai = Kaitai("array", KTStruct())
        val expressionParser = kaitai.ExpressionParser(MutableKaitaiTree(ioStream = booleanArrayOf()), KTStruct(), KTStruct(), booleanArrayOf(), 0, null)

        var expression: String = "[3, 3+1, 3 << 2, 7 / 2, ]"
        var result: dynamic = expressionParser.parseExpression(expression)
        var expected: dynamic = mutableListOf(3, 4, 12, 3)
        check(result.size == expected.size) { "The expected size is ${expected.size}, actual size is ${result.size}" }
        for (i in 0..expected.size - 1) {
            check(result[i] == expected[i]) { "The expected result is ${expected[i]}, actual result is ${result[i]}" }
        }

        expression = "['Hel,lo', \"World!\", '']"
        result = expressionParser.parseExpression(expression) as MutableList<dynamic>
        expected = mutableListOf("Hel,lo", "World!", "")
        check(result.size == expected.size) { "The expected size is ${expected.size}, actual size is ${result.size}" }
        for (i in 0..<expected.size) {
            check(result[i] == expected[i]) { "The expected result is ${expected[i]}, actual result is ${result[i]}" }
        }
    }

    /*@Test
    fun identifierTest() {

    }*/

}
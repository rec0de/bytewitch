package kaitai

import bitmage.booleanArrayOfInts
import bitmage.byteArrayOfInts
import bitmage.toBooleanArray
import kotlin.test.Test
import decoders.Kaitai
import decoders.MutableKaitaiTree
import decoders.Kaitai.TokenType
import decoders.KaitaiBytes
import decoders.KaitaiEnum
import decoders.KaitaiList
import decoders.KaitaiResult
import decoders.KaitaiSignedInteger
import decoders.KaitaiUnsignedInteger
import kaitai.KaitaiTestUtils.checkElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToDynamic

class KaitaiExpressionTests {
    @Test
    fun tokenizerTest() {
        val kaitai = Kaitai("tokenizer", KTStruct())
        val expressionParser = kaitai.ExpressionParser(MutableKaitaiTree(ioStream = booleanArrayOf()), KTStruct(), KTStruct(), booleanArrayOf(), 0, null, null)

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

        expression = "some_type::some_enum::some_value & something_else"
        result = expressionParser.tokenizeExpression(expression)
        check(result.size == 3) {"The expected size is 3, actual size is ${result.size}"}
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
        val expressionParser = kaitai.ExpressionParser(MutableKaitaiTree(ioStream = booleanArrayOf()), KTStruct(), KTStruct(), booleanArrayOf(), 0, null, null)

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

        expression = "-1 <= 1"
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

        // Comparing int(long) with float is no longer supported
        /*expression = "3 == 3.0"
        result = expressionParser.parseExpression(expression)
        expected = false
        check(result == expected) {"The expected result is $expected, actual result is $result"}*/


        expression = "'ABC' == 'abc'"
        result = expressionParser.parseExpression(expression)
        expected = false
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "[0x12, 0x34, 0x56, 0x78] == [0x12, 0x34, 0x56]"
        result = expressionParser.parseExpression(expression)
        expected = false
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "[0x12, 0x34, 0x56, 0x78] == [0x12, 0x34, 0x56, 0x78]"
        result = expressionParser.parseExpression(expression)
        expected = true
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "[0x12, 0x34, 0x56] == [0x12, 0x34, 0x56, 0x78]"
        result = expressionParser.parseExpression(expression)
        expected = false
        check(result == expected) {"The expected result is $expected, actual result is $result"}

        expression = "[3+4] == [7]"
        result = expressionParser.parseExpression(expression)
        expected = true
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
        val expressionParser = kaitai.ExpressionParser(MutableKaitaiTree(ioStream = booleanArrayOf()), KTStruct(), KTStruct(), booleanArrayOf(), 0, null, null)

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

    @Test
    fun simpleIdentifierTest() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "a", type = KTType.Primitive("u1")),
                KTSeq(id = "b", type = KTType.Primitive("u1")),
                KTSeq(id = "c", size = StringOrInt.StringValue("a")),
                KTSeq(id = "d", size = StringOrInt.StringValue("b"), repeat = KTRepeat.EXPR, repeatExpr = "2"),
                KTSeq(id = "e", size = StringOrInt.StringValue("_index"), repeat = KTRepeat.EXPR, repeatExpr = "3"),
                KTSeq(id = "f0", size = StringOrInt.StringValue("1"), ifCondition = StringOrBoolean.StringValue("e[0] == [0x08]")),  // false
                KTSeq(id = "f1", size = StringOrInt.StringValue("1"), ifCondition = StringOrBoolean.StringValue("e[1] == [0x08]")),  // true
                KTSeq(id = "f2", size = StringOrInt.StringValue("1"), ifCondition = StringOrBoolean.StringValue("e[2][1] == 0x10")),  // true
                KTSeq(id = "g", type = KTType.Primitive("s1"), repeat = KTRepeat.UNTIL, repeatUntil = "_ == -1"),
            ),
        )

        val data = byteArrayOfInts(
            0x01,  // a
            0x02,  // b
            0x03,  // c
            0x04, 0x05, 0x06, 0x07,  // d
            0x08, 0x09, 0x10,  // e
            0x11, 0x12,  // f
            0x13, 0x14, 0xff,  // g
        )

        val decoder = Kaitai("simpleIdentifier", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        val c = result.bytesListTree["c"]
        checkElement(c, "c", KaitaiBytes::class, Pair(2, 3), Pair(0,0), booleanArrayOfInts(0x03))

        val d = result.bytesListTree["d"]
        checkElement(d, "d", KaitaiList::class, Pair(3, 7), Pair(0,0), booleanArrayOfInts(0x04, 0x05, 0x06, 0x07))
        val d0 = d.bytesListTree!![0]
        checkElement(d0, "d", KaitaiBytes::class, Pair(3, 5), Pair(0,0), booleanArrayOfInts(0x04, 0x05))
        val d1 = d.bytesListTree!![1]
        checkElement(d1, "d", KaitaiBytes::class, Pair(5, 7), Pair(0,0), booleanArrayOfInts(0x06, 0x07))

        val e = result.bytesListTree["e"]
        checkElement(e, "e", KaitaiList::class, Pair(7, 10), Pair(0,0), booleanArrayOfInts(0x08, 0x09, 0x10))
        val e0 = e.bytesListTree!![0]
        checkElement(e0, "e", KaitaiBytes::class, Pair(7, 7), Pair(0,0), booleanArrayOfInts())
        val e1 = e.bytesListTree!![1]
        checkElement(e1, "e", KaitaiBytes::class, Pair(7, 8), Pair(0,0), booleanArrayOfInts(0x08))
        val e2 = e.bytesListTree!![2]
        checkElement(e2, "e", KaitaiBytes::class, Pair(8, 10), Pair(0,0), booleanArrayOfInts(0x09, 0x10))

        try {
            result.bytesListTree["f0"]
            check(false)
        } catch (e: Exception) {
            check(e.message == "Could not find element with id f0") {"Exception \"Could not find element with id f0\" was expected but $e was thrown."}
        }
        val f1 = result.bytesListTree["f1"]
        checkElement(f1, "f1", KaitaiBytes::class, Pair(10, 11), Pair(0,0), booleanArrayOfInts(0x11))
        val f2 = result.bytesListTree["f2"]
        checkElement(f2, "f2", KaitaiBytes::class, Pair(11, 12), Pair(0,0), booleanArrayOfInts(0x12))

        val g = result.bytesListTree["g"]
        checkElement(g, "g", KaitaiList::class, Pair(12, 15), Pair(0,0), booleanArrayOfInts(0x13, 0x14, 0xff))
        val g0 = g.bytesListTree!![0]
        checkElement(g0, "g", KaitaiSignedInteger::class, Pair(12, 13), Pair(0,0), booleanArrayOfInts(0x13))
        val g1 = g.bytesListTree!![1]
        checkElement(g1, "g", KaitaiSignedInteger::class, Pair(13, 14), Pair(0,0), booleanArrayOfInts(0x14))
        val g2 = g.bytesListTree!![2]
        checkElement(g2, "g", KaitaiSignedInteger::class, Pair(14, 15), Pair(0,0), booleanArrayOfInts(0xff))
        try {
            g.bytesListTree!![3]
            check(false)
        } catch (e: Exception) {
            check(e is IndexOutOfBoundsException) {"IndexOutOfBoundsException was expected but $e was thrown."}
        }

    }

    @Test
    fun simpleIdentifierTypesTest() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "u1", type = KTType.Primitive("u1")),
                KTSeq(id = "u2", type = KTType.Primitive("u2")),
                KTSeq(id = "u4", type = KTType.Primitive("u4")),
                KTSeq(id = "u8", type = KTType.Primitive("u8")),

                KTSeq(id = "s1", type = KTType.Primitive("s1")),
                KTSeq(id = "s2", type = KTType.Primitive("s2")),
                KTSeq(id = "s4", type = KTType.Primitive("s4")),
                KTSeq(id = "s8", type = KTType.Primitive("s8")),

                KTSeq(id = "f4", type = KTType.Primitive("f4")),
                KTSeq(id = "f8", type = KTType.Primitive("f8")),

                KTSeq(id = "str", type = KTType.Primitive("str"), size = StringOrInt.StringValue("1")),
                KTSeq(id = "strz", type = KTType.Primitive("strz")),
            ),
        )

        val data = byteArrayOfInts(
            0x01,  // u1
            0x02, 0x02, // u2
            0x04, 0x04, 0x04, 0x04, // u4
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, // u8

            0x11,  // s1
            0x12, 0x12, // s2
            0x14, 0x14, 0x14, 0x14, // s4
            0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, // s8

            0x24, 0x24, 0x24, 0x24, // f4
            0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, // f8

            0x31, // str
            0x33, 0x33, 0x00 // strz
        )
        val kaitai = Kaitai("simpleIdentifierTypesTest", struct)
        val result = kaitai.decode(data, 0)
        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        val expressionParser = kaitai.ExpressionParser(result.bytesListTree, struct, null, data.toBooleanArray(), 0, null, null)

        var expressionResult = expressionParser.parseExpression("u1")
        check(expressionResult is Long) {"Expected type of u1 to be Long, got ${expressionResult::class.simpleName}" }
        check(expressionResult == 1) {"Expected result to be 1, got $expressionResult"}
        expressionResult = expressionParser.parseExpression("u2")
        check(expressionResult is Long) {"Expected type of u2 to be Long, got ${expressionResult::class.simpleName}" }
        check(expressionResult == 514L) {"Expected result to be 514, got $expressionResult"}
        expressionResult = expressionParser.parseExpression("u4")
        check(expressionResult is Long) {"Expected type of u4 to be Long, got ${expressionResult::class.simpleName}" }
        check(expressionResult == 67372036L) {"Expected result to be 67372036, got $expressionResult"}
        expressionResult = expressionParser.parseExpression("u8")
        check(expressionResult is Long) {"Expected type of u8 to be Long, got ${expressionResult::class.simpleName}" }
        check(expressionResult == 578721382704613384L) {"Expected result to be 578721382704613384, got $expressionResult"}

        expressionResult = expressionParser.parseExpression("s1")
        check(expressionResult is Long) {"Expected type of s1 to be Long, got ${expressionResult::class.simpleName}" }
        check(expressionResult == 17L) {"Expected result to be 17, got $expressionResult"}
        expressionResult = expressionParser.parseExpression("s2")
        check(expressionResult is Long) {"Expected type of s2 to be Long, got ${expressionResult::class.simpleName}" }
        check(expressionResult == 4626L) {"Expected result to be 4626, got $expressionResult"}
        expressionResult = expressionParser.parseExpression("s4")
        check(expressionResult is Long) {"Expected type of s4 to be Long, got ${expressionResult::class.simpleName}" }
        check(expressionResult == 336860180L) {"Expected result to be 336860180, got $expressionResult"}
        expressionResult = expressionParser.parseExpression("s8")
        check(expressionResult is Long) {"Expected type of s8 to be Long, got ${expressionResult::class.simpleName}" }
        check(expressionResult == 1736164148113840152L) {"Expected result to be 1736164148113840152, got $expressionResult"}

        expressionResult = expressionParser.parseExpression("f4")
        check(expressionResult is Double) {"Expected type of f4 to be Double, got ${expressionResult::class.simpleName}" }
        check(expressionResult == 3.559244355763391e-17) {"Expected result to be 3.559244355763391e-17, got $expressionResult"}
        expressionResult = expressionParser.parseExpression("f8")
        check(expressionResult is Double) {"Expected type of f8 to be Double, got ${expressionResult::class.simpleName}" }
        check(expressionResult == 3.0654356309538037e-115) {"Expected result to be 3.0654356309538037e-115, got $expressionResult"}

        expressionResult = expressionParser.parseExpression("str")
        check(expressionResult is String) {"Expected type of str to be String, got ${expressionResult::class.simpleName}" }
        check(expressionResult == "1") {"Expected result to be \"1\", got $expressionResult"}
        expressionResult = expressionParser.parseExpression("strz")
        check(expressionResult is String) {"Expected type of strz to be String, got ${expressionResult::class.simpleName}" }
        check(expressionResult == "33\u0000") {"Expected result to be \"33\u0000\", got $expressionResult"}
    }

    @Test
    fun EnumTest() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "protocol", type = KTType.Primitive("u1"), enum = "ip_protocols"),
                KTSeq(id = "test_true", size = StringOrInt.StringValue("protocol==ip_protocols::udp?4:2")),
                KTSeq(id = "test_false", size = StringOrInt.StringValue("protocol==ip_protocols::icmp?4:2")),
            ),
            enums = mapOf(
                Pair(
                    "ip_protocols",
                    KTEnum(
                        mapOf(
                            Pair(1, KTEnumValue(id = StringOrBoolean.StringValue("icmp"))),
                            Pair(6, KTEnumValue(id = StringOrBoolean.StringValue("tcp"))),
                            Pair(17, KTEnumValue(id = StringOrBoolean.StringValue("udp"))),
                        )
                    )
                ),
            )
        )

        val data = byteArrayOfInts(
            0x11, // ip_protcols: 17 -> udp
            0x11, 0x22, 0x33, 0x44,
            0x11, 0x22, 0x33, 0x44
        )

        val decoder = Kaitai("enums", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        var element = result.bytesListTree["test_true"]
        checkElement(element, id="test_true", elementClass=KaitaiBytes::class, sourceByteRange=Pair(1, 5), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x11, 0x22, 0x33, 0x44,))
        element = result.bytesListTree["test_false"]
        checkElement(element, id="test_false", elementClass=KaitaiBytes::class, sourceByteRange=Pair(5, 7), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x11, 0x22,))
    }

    @Test
    fun methodsTest() {
        val data = byteArrayOfInts(
            0x11,                   //intToString
            0x22,                   //floatToInt
            0x74, 0x65, 0x73, 0x74, //bytesBuffer
            0x33,                   //byteArrayLength
            0x44,                   //byteArrayToStr
            0x55,                   //stringLength
            0x66,                   //stringReverse
            0x77,                   //stringSubstring
            0x88,                   //stringToInt
            0x99,                   //stringToIntWithRadix
            0xaa,                   //enumToInt
            0xbb,                   //booleanToInt
            0x00, 0x00, 0x00, 0x2a, //rootBuffer
            0x00, 0x00, 0x00, 0x0d, //elementWithSubtype.parentBuffer
            0xcc,                   //elementWithSubtype.ubElementWithSubtype.parent
            0xdd,                   //elementWithSubtype.ubElementWithSubtype.root
            0xee,                   //elementWithSubtype.ubElementWithSubtype.ioEof
            0xff,                   //elementWithSubtype.ubElementWithSubtype.ioSize
            0x11,                   //elementWithSubtype.ubElementWithSubtype.ioPos
            0x22,                   //elementWithSubtype.subElementWithSubtypeAndSize.ioEof
            0x33,                   //elementWithSubtype.subElementWithSubtypeAndSize.ioSize
            0x44,                   //elementWithSubtype.subElementWithSubtypeAndSize.ioPos
            0x55,                   //elementWithSubtype.ioOfElementSize
            0x66,                   //arrayFirst
            0x77,                   //arrayLast
            0x88,                   //arraySize
            0x99,                   //arrayMin
            0xaa,                   //arrayMax
            0xbb,                   //arrayIndex
            0xcc,                   //arrayLength
            0xdd,                   //arrayToString
        )

        val struct = KTStruct(
            seq = listOf(
                KTSeq(id="intToString", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("0X11.to_s == '17'")),
                KTSeq(id="IntToStringFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("0X11.to_s != '17'")),
                KTSeq(id="floatToInt", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("3.0.to_i == 3")),
                KTSeq(id="floatToIntFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("3.0.to_i != 3")),
                KTSeq(id="bytesBuffer", size=StringOrInt.IntValue(4)),
                KTSeq(id="byteArrayLength", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("bytesBuffer.length == 4")),
                KTSeq(id="byteArrayLengthFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("bytesBuffer.length != 4")),
                KTSeq(id="byteArrayToString", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("bytesBuffer.to_s(UTF-8) == 'test'")),
                KTSeq(id="byteArrayToStringFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("bytesBuffer.to_s(UTF-8) != 'test'")),
                KTSeq(id="stringLength", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("'test'.length == 4")),
                KTSeq(id="stringLengthFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("'test'.length != 4")),
                KTSeq(id="stringReverse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("'test'.reverse == 'tset'")),
                KTSeq(id="stringReverseFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("'test'.reverse != 'tset'")),
                KTSeq(id="stringSubstring", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("'test'.substring(-2 * -1, 4-0) == 'st'")),
                KTSeq(id="stringSubstringFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("'test'.substring(-2 * -1, 4-0) != 'st'")),
                KTSeq(id="stringToInt", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("'5'.to_i == 5")),
                KTSeq(id="stringToIntFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("'5'.to_i != 5")),
                KTSeq(id="stringToIntWithRadix", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("'0x05'.to_i(16) == '0b101'.to_i(2)")),
                KTSeq(id="stringToIntWithRadixFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("'0x05'.to_i(16) != '0b101'.to_i(2)")),
                KTSeq(id="enumToInt", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("ip_protocols::tcp.to_i == 6")),
                KTSeq(id="enumToIntFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("ip_protocols::tcp.to_i != 6")),
                KTSeq(id="booleanToInt", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("true.to_i == 1")),
                KTSeq(id="booleanToIntFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("true.to_i != 1")),
                KTSeq(id="rootBuffer", type=KTType.Primitive("u4")),
                KTSeq(id="elementWithSubtype", type=KTType.Primitive("sub")),
                KTSeq(id="arrayFirst", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337].first == 21")),
                KTSeq(id="arrayFirstFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337].first != 21")),
                KTSeq(id="arrayLast", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337].last == 1337")),
                KTSeq(id="arrayLastFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337].last != 1337")),
                KTSeq(id="arraySize", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337].size == 3")),
                KTSeq(id="arraySizeFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337].size != 3")),
                KTSeq(id="arrayMin", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337].min == 21")),
                KTSeq(id="arrayMinFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337].min != 21")),
                KTSeq(id="arrayMax", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337].max == 1337")),
                KTSeq(id="arrayMaxFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337].max != 1337")),
                KTSeq(id="arrayIndex", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337][3-2] == 42")),
                KTSeq(id="arrayIndexFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[8 + 13, 42, 1337][3-2] != 42")),
                KTSeq(id="arrayLength", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[0x00, 0x80, 0xFF].length == 3")),
                KTSeq(id="arrayLengthFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[0x00, 0x80, 0xFF].length != 3")),
                KTSeq(id="arrayToString", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[0x74, 0x65, 0x73, 0x74,].to_s(ASCII) == 'test'")),
                KTSeq(id="arrayToStringFalse", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("[0x74, 0x65, 0x73, 0x74,].to_s(ASCII) != 'test'")),
            ),
            enums = mapOf(
                Pair(
                    "ip_protocols",
                    KTEnum(
                        mapOf(
                            Pair(1, KTEnumValue(id = StringOrBoolean.StringValue("icmp"))),
                            Pair(6, KTEnumValue(id = StringOrBoolean.StringValue("tcp"))),
                            Pair(17, KTEnumValue(id = StringOrBoolean.StringValue("udp"))),
                        )
                    )
                ),
            ),
            types = mapOf(
                Pair("sub", KTStruct(
                    seq = listOf(
                        KTSeq(id="parentBuffer", type=KTType.Primitive("u4")),
                        KTSeq(id="subElementWithSubtype", type=KTType.Primitive("subsub")),
                        KTSeq(id="subElementWithSubtypeAndSize", size=StringOrInt.IntValue(3), type=KTType.Primitive("subsubWithSize")),
                        KTSeq(id="ioOfElementSize", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("subElementWithSubtypeAndSize._io.size == 3")),
                    ),
                    types = mapOf(
                        Pair("subsub", KTStruct(
                            seq = listOf(
                                KTSeq(id="parent", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("_parent.parentBuffer == 13")),
                                KTSeq(id="root", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("_parent._parent.bytesBuffer == _root.bytesBuffer")),
                                KTSeq(id="ioEof", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("not _io.eof")),
                                KTSeq(id="ioSize", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("_io.size == ${data.size}")),
                                KTSeq(id="ioPos", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("_io.pos == 27")),
                            ),
                        )),
                        Pair("subsubWithSize", KTStruct(
                            seq = listOf(
                                KTSeq(id="ioEof", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("not _io.eof")),
                                KTSeq(id="ioSize", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("_io.size == 3")),
                                KTSeq(id="ioPos", type=KTType.Primitive("u1"), ifCondition=StringOrBoolean.StringValue("_io.pos == 2")),
                            ),
                        ))
                    )
                ))
            )
        )

        val decoder = Kaitai("methods", struct)

        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        var element = result.bytesListTree["intToString"]
        checkElement(element, id="intToString", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(0, 1), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x11))
        element = result.bytesListTree["floatToInt"]
        checkElement(element, id="floatToInt", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(1, 2), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x22))
        element = result.bytesListTree["byteArrayLength"]
        checkElement(element, id="byteArrayLength", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(6, 7), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x33))
        element = result.bytesListTree["byteArrayToString"]
        checkElement(element, id="byteArrayToString", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(7, 8), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x44))
        element = result.bytesListTree["stringLength"]
        checkElement(element, id="stringLength", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(8, 9), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x55))
        element = result.bytesListTree["stringReverse"]
        checkElement(element, id="stringReverse", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(9, 10), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x66))
        element = result.bytesListTree["stringSubstring"]
        checkElement(element, id="stringSubstring", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(10, 11), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x77))
        element = result.bytesListTree["stringToInt"]
        checkElement(element, id="stringToInt", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(11, 12), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x88))
        element = result.bytesListTree["stringToIntWithRadix"]
        checkElement(element, id="stringToIntWithRadix", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(12, 13), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x99))
        element = result.bytesListTree["enumToInt"]
        checkElement(element, id="enumToInt", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(13, 14), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xaa))
        element = result.bytesListTree["booleanToInt"]
        checkElement(element, id="booleanToInt", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(14, 15), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xbb))

        element = ((result.bytesListTree["elementWithSubtype"] as KaitaiResult).bytesListTree["subElementWithSubtype"] as KaitaiResult).bytesListTree["parent"]
        checkElement(element, id="parent", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(23, 24), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xcc))
        element = ((result.bytesListTree["elementWithSubtype"] as KaitaiResult).bytesListTree["subElementWithSubtype"] as KaitaiResult).bytesListTree["root"]
        checkElement(element, id="root", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(24, 25), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xdd))
        element = ((result.bytesListTree["elementWithSubtype"] as KaitaiResult).bytesListTree["subElementWithSubtype"] as KaitaiResult).bytesListTree["ioEof"]
        checkElement(element, id="ioEof", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(25, 26), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xee))
        element = ((result.bytesListTree["elementWithSubtype"] as KaitaiResult).bytesListTree["subElementWithSubtype"] as KaitaiResult).bytesListTree["ioSize"]
        checkElement(element, id="ioSize", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(26, 27), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xff))
        element = ((result.bytesListTree["elementWithSubtype"] as KaitaiResult).bytesListTree["subElementWithSubtype"] as KaitaiResult).bytesListTree["ioPos"]
        checkElement(element, id="ioPos", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(27, 28), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x11))

        element = ((result.bytesListTree["elementWithSubtype"] as KaitaiResult).bytesListTree["subElementWithSubtypeAndSize"] as KaitaiResult).bytesListTree["ioEof"]
        checkElement(element, id="ioEof", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(28, 29), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x22))
        element = ((result.bytesListTree["elementWithSubtype"] as KaitaiResult).bytesListTree["subElementWithSubtypeAndSize"] as KaitaiResult).bytesListTree["ioSize"]
        checkElement(element, id="ioSize", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(29, 30), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x33))
        element = ((result.bytesListTree["elementWithSubtype"] as KaitaiResult).bytesListTree["subElementWithSubtypeAndSize"] as KaitaiResult).bytesListTree["ioPos"]
        checkElement(element, id="ioPos", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(30, 31), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x44))

        element = (result.bytesListTree["elementWithSubtype"] as KaitaiResult).bytesListTree["ioOfElementSize"]
        checkElement(element, id="ioOfElementSize", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(31, 32), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x55))

        element = result.bytesListTree["arrayFirst"]
        checkElement(element, id="arrayFirst", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(32, 33), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x66))
        element = result.bytesListTree["arrayLast"]
        checkElement(element, id="arrayLast", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(33, 34), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x77))
        element = result.bytesListTree["arraySize"]
        checkElement(element, id="arraySize", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(34, 35), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x88))
        element = result.bytesListTree["arrayMin"]
        checkElement(element, id="arrayMin", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(35, 36), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x99))
        element = result.bytesListTree["arrayMax"]
        checkElement(element, id="arrayMax", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(36, 37), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xaa))
        element = result.bytesListTree["arrayIndex"]
        checkElement(element, id="arrayIndex", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(37, 38), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xbb))
        element = result.bytesListTree["arrayLength"]
        checkElement(element, id="arrayLength", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(38, 39), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xcc))
        element = result.bytesListTree["arrayToString"]
        checkElement(element, id="arrayToString", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(39, 40), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xdd))
    }
}
package ParserTests

import decoders.Nemesys.NemesysParsedMessage
import decoders.Nemesys.NemesysParser
import decoders.Nemesys.NemesysSegment
import kotlin.test.Test

class TestCustomParser {
    // choose the parser
    private fun parserForSegmentParsing(bytes: ByteArray, index: Int): NemesysParsedMessage {
        return NemesysParser().parse(bytes, index)
    }

    @Test
    fun testSegmentation() {
        TrainingMessageSamples.testMessages.forEachIndexed { index, testMessage ->
            val parsed = parserForSegmentParsing(testMessage.message, index)
            EvaluationHelper.printSegmentParsingResult(index, testMessage.segments, parsed.segments)
        }

        EvaluationHelper.printFinalScore()
    }

    @Test
    fun testMessageGroupSegmentation() {
        TrainingMessageSamples.messageGroups.forEach { group ->
            println("=== Testing Custom Parser Group ${group.typeId} ===")
            group.messages.forEach { testMessage ->
                val parsed = parserForSegmentParsing(testMessage.message, testMessage.index)
                EvaluationHelper.printSegmentParsingResult(testMessage.index, testMessage.segments, parsed.segments)
            }
        }

        EvaluationHelper.printFinalScore()
    }

    @Test
    fun testSequenceAlignment() {
        for ((index, test) in TrainingMessageSamples.alignmentTests.withIndex()) {
            val msgA = TrainingMessageSamples.testMessages[test.messageAIndex]
            val msgB = TrainingMessageSamples.testMessages[test.messageBIndex]
            val messages = mapOf(
                test.messageAIndex to NemesysParsedMessage(msgA.segments, msgA.message, test.messageAIndex),
                test.messageBIndex to NemesysParsedMessage(msgB.segments, msgB.message, test.messageBIndex)
            )
            EvaluationHelper.printSequenceAlignmentResult(index, messages, test.expectedAlignments)
        }

        EvaluationHelper.printFinalScore()
    }

    @Test
    fun testSegmentationWithSequenceAlignment() {
         for ((index, test) in TrainingMessageSamples.alignmentTests.withIndex()) {
            val msgA = TrainingMessageSamples.testMessages[test.messageAIndex]
            val msgB = TrainingMessageSamples.testMessages[test.messageBIndex]

            // do segmentation
            val parsedA = parserForSegmentParsing(msgA.message, test.messageAIndex)
            val parsedB = parserForSegmentParsing(msgB.message, test.messageBIndex)

            val actualParsed = mapOf(
                test.messageAIndex to parsedA,
                test.messageBIndex to parsedB
            )

            val expectedParsed = mapOf(
                test.messageAIndex to NemesysParsedMessage(msgA.segments, msgA.message, test.messageAIndex),
                test.messageBIndex to NemesysParsedMessage(msgB.segments, msgB.message, test.messageBIndex)
            )

            // do segmentation
            EvaluationHelper.printSegmentationWithSequenceAlignmentResult(
                testNumber = index,
                actualMessages = actualParsed,
                expectedSegments = expectedParsed,
                expectedAlignments = test.expectedAlignments
            )
        }

        EvaluationHelper.printFinalScore()
    }

}
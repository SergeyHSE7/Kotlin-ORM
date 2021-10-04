import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe

suspend inline fun FreeSpecContainerContext.sequenceTests() {
    lateinit var mySequence: SqlSequence

    "CREATE SEQUENCE" {
        shouldNotThrowAny {
            mySequence =
                SqlSequence("my_sequence", start = 10, increment = 5, maxValue = 20, minValue = 0, isCycle = true)
        }
    }

    "NEXTVAL" {
        mySequence.nextValue() shouldBe 10
        mySequence.nextValue() shouldBe 15
        mySequence.nextValue() shouldBe 20
    }

    "LASTVAL" {
        mySequence.lastValue() shouldBe 20
    }

    "CYCLE" {
        mySequence.nextValue() shouldBe 0
    }

    "RESTART" {
        mySequence.restart(10)
        mySequence.nextValue() shouldBe 10
        mySequence.restart(0)
        mySequence.nextValue() shouldBe 0

        shouldThrowExactly<IllegalArgumentException> { mySequence.restart(-10) }
    }

    "DROP SEQUENCE" {
        shouldNotThrowAny { mySequence.drop() }
    }
}

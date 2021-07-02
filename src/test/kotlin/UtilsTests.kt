import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.atteo.evo.inflector.English
import utils.Case
import utils.transformCase

internal class UtilsTests : FreeSpec({
    "String transform utils" - {
        "plural library check" {
                English.plural("word") shouldBe "words"
                English.plural("box") shouldBe "boxes"
                English.plural("entity") shouldBe "entities"
                English.plural("mouse") shouldBe "mice"
                English.plural("elf") shouldBe "elves"
        }
        "splitByCase check" {
                "PascalCase".transformCase(Case.Pascal, Case.Normal) shouldBe "Pascal Case"
                "camelCase".transformCase(Case.Camel, Case.Normal) shouldBe "Camel Case"
                "snake_case".transformCase(Case.Snake, Case.Normal) shouldBe "Snake Case"
                "kebab-case".transformCase(Case.Kebab, Case.Normal) shouldBe "Kebab Case"
        }
        "joinWithCase check" {
            "Pascal Case".transformCase(Case.Normal, Case.Pascal) shouldBe "PascalCase"
            "Camel Case".transformCase(Case.Normal, Case.Camel) shouldBe "camelCase"
            "Snake Case".transformCase(Case.Normal, Case.Snake) shouldBe "snake_case"
            "Kebab Case".transformCase(Case.Normal, Case.Kebab) shouldBe "kebab-case"
        }
        "transform to plural forms" {
            "Long Word".transformCase(Case.Normal, Case.Snake, true) shouldBe "long_words"
        }
    }
})

package sample

/**
 * KDoc with a [link] and a tag.
 * @param name the name
 */
@Suppress("UNUSED")
class Greeter(val name: String) {
    var count = 0x1F + 2.5f

    fun greet(): String {
        // line comment
        val c = 'x'
        val s = "Hello, $name! ${count + 1}\n"
        return if (c == 'x' && count >= 1) s else name as? String ?: ""
    }
}
/* block comment */

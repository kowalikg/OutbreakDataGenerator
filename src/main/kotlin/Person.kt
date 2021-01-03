open class Person(val id: Int, val firstName: String, val lastName: String, val group: Int, var isInfected: Boolean = false) {
    val contacts = mutableListOf<Contact>()
}
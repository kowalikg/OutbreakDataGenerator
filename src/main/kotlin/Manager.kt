class Manager(id: Int, firstName: String, lastName: String, group: Int, val level: Int,
              var hasManager : Boolean = false) : Person(id, firstName, lastName, group) {
    private val subWorkers = mutableListOf<Person>()

    fun addSubWorkers(p : List<Person>) {
        subWorkers.addAll(p)
    }

    fun getSubWorkers(): MutableList<Person> {
        return subWorkers
    }

}
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File
import java.nio.file.Paths
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random


class Generator(args: Array<String>) {
    private val baseEmployees: Int = args[0].toInt()
    private val peoplePerGroup: Int = args[1].toInt()
    private val peopleInfected: Int = args[2].toInt()
    private val dayOfInvestigation: Int = args[3].toInt()
    private val superInfections: Boolean = args[4] == "y"
    private val employees: MutableList<Person> = mutableListOf()
    private val managers: MutableList<Manager> = mutableListOf()
    private val folderToWrite: File
    private val employeesFile: File
    private val contactsFile: File

    private val names: List<List<String>> = csvReader().readAll(File("names.csv"))
    private val lastNames: List<List<String>> = csvReader().readAll(File("last_names.csv"))

    private var groups = 0
    private var employeeId = 1
    private var maxLevel = 0;

    init {
        val folder = args[5]
        val pathToWrite = Paths.get("").toAbsolutePath().toString().replace("\\", "/")

        folderToWrite = File("$pathToWrite/$folder")

        employeesFile = File("$pathToWrite/$folder/" + "/contacts" + "_" + baseEmployees + "_" + peoplePerGroup + "_" + peopleInfected + "_" + dayOfInvestigation + ".csv")
        contactsFile = File("$pathToWrite/$folder/" + "/employees" + "_" + baseEmployees + "_" + peoplePerGroup + "_" + peopleInfected + "_" + dayOfInvestigation + ".csv")
    }

    fun launch() {
        createEmployees()
        createTopLevelManagers()

        maxLevel = findTopManagerLevel()

        launchManagerTree()

        createResultFolder()

        createContactsFile()
        createEmployeesFile()
        connectWorkers()
        setMeetings()

        generateFriends()
        generateInfections()

        dumpEmployees()
        dumpContacts()
    }

    private fun createResultFolder() {
        if (!folderToWrite.exists()) {
            folderToWrite.mkdirs()
        }
    }

    private fun launchManagerTree() {
        val head = managers.find { e -> e.level == maxLevel }
        head?.let { managerTree(it) }
    }

    private fun findTopManagerLevel(): Int {
        return managers
                .map { e -> e.level }
                .max()!!
    }

    private fun createTopLevelManagers() {
        var level = 2
        while (groups > 1) {

            val newManagersAmount = ceil(groups.toDouble() / peoplePerGroup).toInt()

            groups = if (newManagersAmount == groups) groups - 1 else newManagersAmount
            for (group in 1..groups) {
                createManager(0, level)
            }
            level++
        }
    }

    private fun createEmployees() {
        groups = baseEmployees / peoplePerGroup
        val level = 1
        for (group in 1..groups) {
            for (worker in 1..peoplePerGroup) {
                createWorker(group)
            }
            createManager(group, level)
        }
    }

    private fun createEmployeesFile() {
        if (contactsFile.exists()) {
            contactsFile.delete()
        }
        val headers = listOf("id", "firstName", "lastName", "isInfected")
        csvWriter().writeAll(listOf(headers), contactsFile)
    }

    private fun createContactsFile() {
        if (employeesFile.exists()) {
            employeesFile.delete()
        }
        val headers = listOf("id1", "id2", "day")
        csvWriter().writeAll(listOf(headers), employeesFile)
    }

    private fun dumpEmployees() {
        val allEmployees = employees.toMutableList()
        allEmployees.addAll(managers.toList())
        for (person in allEmployees) {
            csvWriter().writeAll(listOf(listOf(person.id, person.firstName, person.lastName, person.isInfected)), contactsFile, append = true)
        }
    }

    private fun dumpContacts() {
        val allEmployees = employees.toMutableList()
        allEmployees.addAll(managers.toList())
        for (person in allEmployees) {
            person.contacts.forEach { contact ->
                csvWriter().writeAll(listOf(listOf(contact.id1, contact.id2, contact.day)), employeesFile, append = true)
            }
        }
    }

    private fun generateInfections() {
        val allEmployees = employees.toMutableList()
        allEmployees.addAll(managers.toList())
        val infected = allEmployees.shuffled().take(floor((peopleInfected * allEmployees.size) / 100.0).toInt())
        for (person in infected) {
            person.isInfected = true
        }
    }

    private fun generateFriends() {
        val allEmployees = mutableListOf<Person>()
        val groupSize = 5

        allEmployees.addAll(employees)
        allEmployees.addAll(managers)

        val divided = allEmployees.shuffled().chunked(groupSize)

        if (superInfections) divided[Random.nextInt(0, divided.size)].forEach { e -> e.isInfected = true }
        for (division in divided) {
            setMeeting(division, dayOfInvestigation - Random.nextInt(2, 14))
        }
    }

    private fun connectWorkers() {
        val firstLevelManagers = managers.filter { m -> m.level == 1 }
        for (manager in firstLevelManagers) {
            setMeeting(manager.getSubWorkers() + manager, dayOfInvestigation)

        }
    }

    private fun setMeetings() {
        for (level in 2..maxLevel) {
            val managers = managers.filter { m -> m.level == level }
            for (manager in managers) {
                setMeeting(manager.getSubWorkers() + manager, dayOfInvestigation - Random.nextInt(2, 14))
            }
        }
    }

    private fun setMeeting(contacts: List<Person>, day: Int) {
        for (employee in contacts) {
            for (collegue in contacts) {
                if (employee != collegue) {
                    val c = Contact(employee.id, collegue.id, day)
                    if (!employee.contacts.contains(c) && !collegue.contacts.contains(c)) {
                        employee.contacts.add(c)
                    }
                }
            }
        }
    }

    private fun createWorker(group: Int) {
        val name = randomName()
        employees.add(Person(employeeId++, name.first, name.second, group))
    }

    private fun createManager(group: Int, level: Int) {
        val name = randomName()
        managers.add(Manager(employeeId++, name.first, name.second, group, level))
    }

    private fun randomName(): Pair<String, String> {
        return Pair(names[Random.nextInt(0, names.size)][0], lastNames[Random.nextInt(0, lastNames.size)][0])
    }

    private fun managerTree(m: Manager) {
        if (m.level == 1) levelOneManager(m)
        val subEmployees = managers.filter { u -> !u.hasManager && u.level == m.level - 1 }
                .take(peoplePerGroup)
        m.addSubWorkers(subEmployees)
        for (under in subEmployees) {
            under.hasManager = true
            managerTree(under)
        }
    }

    private fun levelOneManager(m: Manager) {
        m.addSubWorkers(employees.filter { e -> e.group == m.group })
    }
}

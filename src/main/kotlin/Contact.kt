class Contact (val id1: Int, val id2: Int, val day: Int) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Contact) return false
        return (other.id1 == id1 && other.id2 == id2) || (other.id1 == id2 && other.id2 == id1)
    }

    override fun hashCode(): Int {
        var result = id1
        result = 31 * result + id2
        result = 31 * result + day
        return result
    }
}
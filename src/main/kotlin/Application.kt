object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        if(args.size != 6) {
            println("Invalid size of arguments! Expected 6 but was ${args.size}")
        }
        else {
            Generator(args).launch()
            println("Generation completed!")
        }
    }

}
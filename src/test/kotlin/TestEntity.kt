data class TestEntity(
    override var id: Int = 0,
    var string: String = "",
    var int: Int = 0,
    var human: Human? = null
): Entity() {
   // var human: Human? by Reference(HumanTable, Human())
}

val defaultTestEntities = listOf(
    TestEntity(string = "str1", int = 1),
    TestEntity(string = "str2", int = 2),
)

val TestTable = table<TestEntity>(true) {
    varchar(TestEntity::string).unique()
    integer(TestEntity::int)
    // id(TestEntity::human)

}.defaultEntities(defaultTestEntities)


data class Human(
    override var id: Int = 0,
    var name: String = "Josef",
    var age: Int = 32,
): Entity()

/*
val HumanTable = table<Human>(true) {
    varchar(Human::name)
    integer(Human::age)
}.defaultEntities(Human())*/

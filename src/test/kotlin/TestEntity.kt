data class TestEntity(
    override var id: Int = 0,
    var string: String = "",
    var int: Int = 0,
    var human: Human? = null
) : Entity()

val defaultTestEntities = listOf(
    TestEntity(string = "str1", int = 1),
    TestEntity(string = "str2", int = 2, human = Human(1)),
)

object TestTable : Table<TestEntity>(TestEntity::class, true, {
    varchar(TestEntity::string).unique()
    integer(TestEntity::int)
    reference(TestEntity::human, HumanTable)

}, defaultTestEntities)


data class Human(
    override var id: Int = 0,
    var name: String = "Josef",
    var age: Int = 32,
) : Entity() {
    val tests: List<TestEntity> by oneToMany(TestTable, TestEntity::human)
}

data class FollowerToFollowers(
    override var id: Int = 0,
    var follow: Human? = null,
    var follower: Human? = null
) : Entity()

object FollowerToFollowersTable : Table<FollowerToFollowers>(
    FollowerToFollowers::class, true,
    {
        reference(FollowerToFollowers::follow, HumanTable)
        reference(FollowerToFollowers::follower, HumanTable)
    },
    listOf(FollowerToFollowers())
)

object HumanTable : Table<Human>(
    Human::class, true, {
        varchar(Human::name)
        integer(Human::age)
    }, listOf(
        Human(),
        Human(name = "John", age = 28)
    )
)

/*val HumanTable = table<Human>(true) {
    varchar(Human::name)
    integer(Human::age)
}.defaultEntities(Human())*/

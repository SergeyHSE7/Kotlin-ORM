import utils.LazyProp

data class TestEntity(
    override var id: Int = 0,
    var string: String = "",
    var int: Int = 0,
    var int1: Int = 1,
    var human: Human? = null
) : Entity()

val defaultTestEntities = listOf(
    TestEntity(string = "str1", int = 1),
    TestEntity(string = "str2", int = 2, human = Human(1, name = "Josef", age = 32)),
)

object TestTable : Table<TestEntity>(TestEntity::class, true, {
    varchar(TestEntity::string).unique()
    integer(TestEntity::int)
    integer(TestEntity::int1)
    reference(TestEntity::human, HumanTable)

    uniqueColumns(TestEntity::int, TestEntity::int1)
}, defaultTestEntities)


data class Human(
    override var id: Int = 0,
    var name: String = "---",
    var age: Int = 0,
) : Entity() {
    @LazyProp
    val tests: List<TestEntity> by oneToMany(TestTable, TestEntity::human)
    val followers: List<Human?> by manyToMany(
        FollowerToFollowersTable,
        FollowerToFollower::follow,
        FollowerToFollower::follower
    )
}

object HumanTable : Table<Human>(
    Human::class, true, {
        varchar(Human::name)
        integer(Human::age)
    }, listOf(
        Human(name = "Josef", age = 32),
        Human(name = "John", age = 28)
    )
)


data class FollowerToFollower(
    override var id: Int = 0,
    var follow: Human? = null,
    var follower: Human? = null
) : Entity()

object FollowerToFollowersTable : Table<FollowerToFollower>(
    FollowerToFollower::class, true,
    {
        reference(FollowerToFollower::follow, HumanTable, Action.Cascade)
        reference(FollowerToFollower::follower, HumanTable, Action.Cascade)
    },
    listOf(FollowerToFollower(follow = Human(1), follower = Human(2)))
)

/*val HumanTable = table<Human>(true) {
    varchar(Human::name)
    integer(Human::age)
}.defaultEntities(Human(),
        Human(name = "John", age = 28))*/

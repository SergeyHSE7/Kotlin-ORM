
data class User(
    override var id: Int = 0,
    var nickName: String = "",
    var email: String? = null,
    var wallet: Float = 0f
) : Entity()

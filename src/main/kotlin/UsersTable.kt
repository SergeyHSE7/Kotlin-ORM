val UsersTable = table<User> {
    varchar(User::nickName, 40).unique().default("user")
    varchar(User::email)
    real(User::wallet).notNull().default(1_000_000f)

}.defaultEntities(
    User(nickName = "user", email = "user@mail.ru", wallet = 10_000f)
)


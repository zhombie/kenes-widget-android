package q19.kenes_widget.data.model

internal fun q19.kenes_widget.api.model.User?.toDomain(): User? {
    if (this == null) return null
    return User(
        firstName = firstName,
        lastName = lastName,
        middleName = middleName,
        iin = iin,
        phoneNumber = phoneNumber,
        email = email,
        birthDate = birthDate
    )
}
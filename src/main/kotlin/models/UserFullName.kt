package models

class UserFullName(var firstName: String, var secondName: String, var fatherName: String) {

    override fun toString(): String {
        return if (fatherName != "") {
            "$firstName $secondName $fatherName"
        } else {
            "$firstName $secondName"
        }
    }
}
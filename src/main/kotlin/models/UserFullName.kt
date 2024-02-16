package models

class UserFullName(var firstName: String, var secondName: String, var fatherName: String?) {

    override fun toString(): String {
        return if (fatherName != null) {
            "$secondName $firstName  $fatherName"
        } else {
            "$secondName $firstName "
        }
    }
}
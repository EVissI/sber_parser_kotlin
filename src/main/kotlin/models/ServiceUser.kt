package models

import models.UserFullName

class ServiceUser(
    var userFullName: UserFullName,
    var serviceName: String,
    var phone: String,
    var plotNumber: String,
    var payment: String
) {

    override fun toString(): String {
        return "models.ServiceUser{" +
                "userFullName='" + userFullName + '\'' +
                ", phone='" + phone + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", payment='" + payment + '\'' +
                ", plotNumber='" + plotNumber + '\'' +
                '}'
    }
}
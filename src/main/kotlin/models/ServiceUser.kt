package models

class ServiceUser(
    var userFullName: UserFullName,
    var serviceName: String,
    var meterReadings: MeterReadings?,
    var referenceRetrievalNumber: String,
    var plotNumber: String,
    var payment: String
) {

    override fun toString(): String {
        return "models.ServiceUser{" +
                "userFullName='" + userFullName + '\'' +
                ", RRN ='" + referenceRetrievalNumber + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", meterReadings='" + meterReadings + '\'' +
                ", payment='" + payment + '\'' +
                ", plotNumber='" + plotNumber + '\'' +
                '}'
    }
}
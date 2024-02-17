package models

class ServiceUser(
    var userFullName: UserFullName,
    var services: MutableList<Service>,
    var referenceRetrievalNumber: String,
    var plotNumber: String,
    var payment:String
) {

    override fun toString(): String {
        var result: String =
                "models.ServiceUser{" +
                "userFullName='" + userFullName + '\'' +
                ", RRN ='" + referenceRetrievalNumber + '\'' +
                ", plotNumber='" + plotNumber + '\''
        for (service in services){
            result += if (service.servicePayment != null&& service.meterReadings != null){
                ", serviceName='" + service.serviceName + ": " + service.servicePayment + '\''+", meterReadings='" + service.meterReadings + '\''
            }else if(service.servicePayment != null){
                ", serviceName='" + service.serviceName + ": " + service.servicePayment + '\''
            }else if(service.meterReadings != null){
                ", serviceName='" + service.serviceName + '\''+", meterReadings='" + service.meterReadings + '\''
            }
            else{
                ", serviceName='" + service.serviceName  + '\''
            }
        }
        result += ", allPayment = '" + payment + '\''+
            "}\n"
        return result
    }
}
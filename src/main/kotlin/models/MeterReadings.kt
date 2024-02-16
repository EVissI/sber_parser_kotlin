package models

 class MeterReadings(var currentValue:String?,var pervValue:String? ) {
     override fun toString(): String {
         return if (pervValue != null) {
             "Текущее значение счетчика: $currentValue, прошлое значение:$pervValue"
         } else {
             "Значение счетчика: $currentValue"
         }
     }

 }

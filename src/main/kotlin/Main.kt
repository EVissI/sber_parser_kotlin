import com.sun.org.slf4j.internal.LoggerFactory
import me.xdrop.fuzzywuzzy.FuzzySearch
import models.MeterReadings
import models.ServiceUser
import models.UserFullName
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Math.abs
import java.lang.Math.max

class Main {
    companion object {
        private val log = LoggerFactory.getLogger(Main::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
           println(sberTxtPaymentInfoParser())
        }


        private fun sberTxtPaymentInfoParser():List<ServiceUser>{
            val userData = mutableListOf<ServiceUser>()
            try {
                FileInputStream("D:\\sber_parser\\sber_parser_kotlin\\src\\main\\resources\\testFiles\\5433117921_40703810602000000022_062.txt").use { fileInputStream ->
                    InputStreamReader(fileInputStream, "UTF-8").use { inputStreamReader ->
                        BufferedReader(inputStreamReader).use { reader ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null && !line!!.contains("=")) {
                                val values = line!!.split(";")
                                val userFullNameStr = values[6].split(" ")
                                val userFullName = createUserFullNameWithOptionalFatherName(userFullNameStr)
                                val serviceString = values[7]
                                val payment = values[8]
                                val plotNumber = values[5]
                                val referenceRetrievalNumber = values[4]


                                val serviceWords = serviceString.replace(Regex("(м3|квч|кв/ч)"), "")
                                    .replace(Regex("\\d+"), "").trim()
                                val serviceName = takeServiceNameFromStr(serviceWords)

                                var meterReadings: MeterReadings? = null
                                if(hasUtilityMeter(serviceName)){
                                    val serviceNumbers = Regex("\\d{2,}").findAll(serviceString).map { it.value }.toMutableList()
                                    if (serviceNumbers.contains(plotNumber)){
                                        serviceNumbers.remove(plotNumber)
                                    }
                                    if(serviceNumbers.isNotEmpty()){
                                        meterReadings = takeUtlMeterReading(serviceName,serviceNumbers)
                                    }
                                }

                                val user = ServiceUser(userFullName, serviceName, meterReadings ,referenceRetrievalNumber, plotNumber, payment)
                                userData.add(user)
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                log.error("File reading error: ", e)
            }
            return userData
        }

        private fun takeUtlMeterReading(serviceName: String,numbers: MutableList<String>): MeterReadings? {
            val meterReadingNumbers = mutableListOf<Int>()
            val result = MeterReadings(null,null)
            val averageValueUtlMeter = mapOf(
                "Электричество" to 600,
                "Холодная вода" to 20,
                "Горячая вода" to 5,
                "Газоснабжение" to 50,
            )

            for (number in numbers){
                if(number.length > 2 && numbers[0].toInt() < 99999){
                    meterReadingNumbers.add(number.toInt())
                    }
            }

            when (meterReadingNumbers.count()) {
                2 -> {
                    val targetAverageValue = averageValueUtlMeter[serviceName]
                    val differance = kotlin.math.abs(meterReadingNumbers.max() - meterReadingNumbers.min())
                    log.debug("разница для $serviceName:$differance")
                    if (targetAverageValue != null) {
                        if(differance >= targetAverageValue/2 && differance<=targetAverageValue*2){
                            result.pervValue =meterReadingNumbers.min().toString()
                            result.currentValue =meterReadingNumbers.max().toString()
                            return result
                        }else
                            return null
                    }
                }
                1 -> {
                    result.currentValue = meterReadingNumbers[0].toString()
                    return result
                }
                else -> {
                    return null
                }
            }
            return null
        }
        private fun hasUtilityMeter(serviceName: String): Boolean {
            val serviceNameWhoHaveUtlMeter = listOf("Электричество", "Холодная вода", "Горячая вода", "Газоснабжение")
            for (phrase in serviceNameWhoHaveUtlMeter) {
                if (phrase in serviceName) {
                    return true
                }
            }
            return false
        }

        private fun createUserFullNameWithOptionalFatherName(userFullNameStr: List<String>): UserFullName {
            val firstName = userFullNameStr[1]
            val secondName = userFullNameStr[0]
            val fatherName = if (userFullNameStr.size > 2) userFullNameStr[2] else ""
            return UserFullName(firstName, secondName, fatherName)
        }

        private fun takeServiceNameFromStr(serviceString: String): String {
            val words = serviceString.split(Regex("\\s+"))
            val referenceList = mapOf(
                "Членский взнос" to "Членский взнос / чв / чл взнос",
                "Целевой взнос" to "Целевой взнос / цв / цел взнос",
                "Охрана" to "Охрана",
                "Вывоз мусора" to "Вывоз мусора",
                "Водоотведение" to "Водоотведение",
                "Отопление" to "Отопление",
                "Капитальный ремонт" to "Капитальный ремонт / кап рем",
                "Домофон" to "Домофон",
                "Содержание жилья" to "Содержание жилья / сод жилья",
                "Пеня" to "Пеня",
                "Электричество" to "Электричество / элво / эл-во / Электроэнергия / ээ",
                "Холодная вода" to "Холодная вода / хв / хол вода / хол вод",
                "Горячая вода" to "Горячая вода / гв / гор вода / гор вод",
                "Газоснабжение" to "Газоснабжение / газ / гс ",
            )
            var result = String()
            var beautifulResult   = String()
            var resultWeightRatio = 0;

            for (synonyms in referenceList.entries) {
                for (word in words) {
                    if (FuzzySearch.weightedRatio(word, synonyms.value) > resultWeightRatio){
                        result = word
                        beautifulResult = synonyms.key
                        resultWeightRatio = FuzzySearch.weightedRatio(result, synonyms.value)
                    }
                }
            }

            if (result.isEmpty()) {
                log.warn("ServiceName is Empty, reference don't work at that line")
            }
            return beautifulResult
        }
    }
}
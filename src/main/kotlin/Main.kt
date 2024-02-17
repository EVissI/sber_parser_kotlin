import com.sun.org.slf4j.internal.LoggerFactory
import me.xdrop.fuzzywuzzy.FuzzySearch
import models.MeterReadings
import models.Service
import models.ServiceUser
import models.UserFullName
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

class Main {
    companion object {
        private val log = LoggerFactory.getLogger(Main::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
           println(sberTxtPaymentsInfoParser())
        }


        private fun sberTxtPaymentsInfoParser():List<ServiceUser>{
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
                                val services = takeServiceNameFromStr(serviceWords)
                                val serviceNumbers = Regex(pattern = "\\d{2,}").findAll(serviceString).map { it.value }.toMutableList()
                                var servicePayments:MutableList<String>? = mutableListOf()
                                serviceNumbers.removeIf { it == plotNumber }
                                if(serviceNumbers.isNotEmpty()){ servicePayments = findSubsetSum(serviceNumbers,payment.replace(',','.').toDouble())}
                                servicePayments?.let { serviceNumbers.removeAll(it) }


                                for(service in services){
                                    if(!servicePayments.isNullOrEmpty()){
                                        service.servicePayment = servicePayments[0]
                                        servicePayments.removeAt(0)
                                    }

                                    if(hasUtilityMeter(service.serviceName)){
                                        if(serviceNumbers.isNotEmpty()){
                                            service.meterReadings = takeUtlMeterReading(service.serviceName,serviceNumbers)
                                        }
                                    }
                                }

                                val user = ServiceUser(userFullName, services,referenceRetrievalNumber, plotNumber, payment)
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
                    log.debug("$serviceName:$differance")
                    if (targetAverageValue != null) {
                        return if(differance >= targetAverageValue/2 && differance<=targetAverageValue*2){
                            result.pervValue =meterReadingNumbers.min().toString()
                            result.currentValue =meterReadingNumbers.max().toString()
                            result
                        }else
                            null
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

        private fun takeServiceNameFromStr(serviceString: String): MutableList<Service> {
            val words = serviceString.split(Regex("\\s+"))
            val referenceList = mapOf(
                "Членский взнос" to "Членский  / чв / чл",
                "Целевой взнос" to "Целевой / цв / цел",
                "Охрана" to "Охрана",
                "Вывоз мусора" to "Вывоз мусора",
                "Водоотведение" to "Водоотведение",
                "Отопление" to "Отопление",
                "Капитальный ремонт" to "Капитальный ремонт / кап рем",
                "Домофон" to "Домофон",
                "Содержание жилья" to "Содержание жилья / сод жилья",
                "Пеня" to "Пеня",
                "Электричество" to "Электричество / элво / эл-во / Электроэнергия / ээ",
                "Холодная вода" to "Холодная / хв / хол / хол",
                "Горячая вода" to "Горячая / гв / гор / гор",
                "Газоснабжение" to "Газоснабжение / газ / гс ",
            )
            val result : MutableList<Service> = mutableListOf()

            for (synonyms in referenceList.entries) {
                for (word in words) {
                    if (FuzzySearch.weightedRatio(word, synonyms.value) > 85){
                        result.add(Service(synonyms.key,null,null))
                        break
                    }
                }
            }

            if (result.isEmpty()) {
                log.warn("ServiceName is Empty, reference don't work at that line")
            }
            return result
        }

        private fun findSubsetSum(numbers: MutableList<String>, targetSum: Double): MutableList<String>? {
            if (numbers.isEmpty()) {
                return null
            }

            val dp = Array(numbers.size + 1) { IntArray((targetSum * 10).toInt() + 1) }
            dp[0][0] = 1

            for (i in 1..numbers.size) {
                val num = (numbers[i - 1].toDouble() * 10).toInt()
                for (j in 0..targetSum.toInt() * 10) {
                    dp[i][j] = dp[i - 1][j]
                    if (j >= num) {
                        dp[i][j] = dp[i][j] or dp[i - 1][j - num]
                    }
                }
            }

            if (dp[numbers.size][(targetSum * 10).toInt()] != 1) {
                return null
            }

            val subset = mutableListOf<String>()
            var i = numbers.size
            var j = (targetSum * 10).toInt()

            while (i > 0 && j > 0) {
                if (dp[i - 1][j] == 1) {
                    i -= 1
                } else {
                    subset.add(numbers[i - 1])
                    j -= (numbers[i - 1].toDouble() * 10).toInt()
                    i -= 1
                }
            }

            return subset
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

    }
}
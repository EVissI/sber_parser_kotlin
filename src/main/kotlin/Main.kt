import com.sun.org.slf4j.internal.LoggerFactory
import me.xdrop.fuzzywuzzy.FuzzySearch
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
           println(sberTxtPaymentInfoParser())
        }
        private fun sberTxtPaymentInfoParser():List<ServiceUser>{
            val userData = mutableListOf<ServiceUser>()
            try {
                FileInputStream("D:\\sber_parser\\sber_parser_kotlin\\src\\main\\resources\\testFiles\\5433117921_40703810602000000022_142.txt").use { fileInputStream ->
                    InputStreamReader(fileInputStream, "windows-1251").use { inputStreamReader ->
                        BufferedReader(inputStreamReader).use { reader ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null && !line!!.contains("=")) {
                                val values = line!!.split(";")
                                val userFullNameStr = values[6].split(" ")
                                val userFullName = createUserFullNameWithOptionalFatherName(userFullNameStr)
                                val user = ServiceUser(userFullName, takeServiceNameFromStr(values[7]), values[4], values[5], values[8])
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

        private fun createUserFullNameWithOptionalFatherName(userFullNameStr: List<String>): UserFullName {
            val firstName = userFullNameStr[1]
            val secondName = userFullNameStr[0]
            val fatherName = if (userFullNameStr.size > 2) userFullNameStr[2] else ""
            return UserFullName(firstName, secondName, fatherName)
        }

        private fun takeServiceNameFromStr(serviceStr: String): String {
            val words = serviceStr.split("\\s+".toRegex())
            val referenceList = listOf(
                "Эл-во,Электричество,Эл-энергия,Эл-эн,э/э,Эл энергия",
                "Целевой взнос,ЦВ,Цел. Взн.,Цел. взнос,Ц.В.,цв,Целевой Взн,Ц-В"
            )
            val result = StringBuilder()
            for (word in words) {
                for (synonyms in referenceList) {
                    val synonymList = synonyms.split(",")
                    for (synonym in synonymList) {
                        if (FuzzySearch.weightedRatio(word, synonym) > 85) {
                            result.append(word).append(" ")
                            break
                        }
                    }
                }
            }
            if (result.isEmpty()) {
                log.warn("ServiceName is Empty, reference don't work at that line")
            }
            return result.toString().trim()
        }
    }
}
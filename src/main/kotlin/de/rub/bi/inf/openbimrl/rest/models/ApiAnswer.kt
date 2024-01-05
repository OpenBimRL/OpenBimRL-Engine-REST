package de.rub.bi.inf.openbimrl.rest.models

class ApiAnswer<T>(answer: T, val status: String = "ok") {
    val content = answer
}
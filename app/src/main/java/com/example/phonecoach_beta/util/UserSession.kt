package com.example.phonecoach_beta.util

object UserSession {
    var uid : String = ""
    var loginId : String = ""
    var email : String = ""
    var role : String=""
    var familyId: String? = null

    fun clear() {
        uid = ""
        loginId = ""
        email = ""
        role = ""
        familyId = null
    }
    val isIndividual get() = role == "individual"
    val isParent get() = role == "parent"
    val isChild get() = role == "child"
}
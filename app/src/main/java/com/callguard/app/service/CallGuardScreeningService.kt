package com.callguard.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.callguard.app.data.AppDatabase
import com.callguard.app.data.CallHistoryEntity
import com.callguard.app.domain.BlockingDecision
import com.callguard.app.domain.BlockingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallGuardScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val engine = BlockingEngine()

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: return

        scope.launch {
            val db = AppDatabase.get(applicationContext)
            val rules = db.ruleDao().getEnabled()
            when (val decision = engine.evaluate(number, rules)) {
                BlockingDecision.Allow -> Unit
                is BlockingDecision.Block -> {
                    val response = CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipNotification(true)
                        .setSkipCallLog(false)
                        .build()

                    respondToCall(callDetails, response)

                    db.callHistoryDao().insert(
                        CallHistoryEntity(
                            phoneNumber = number,
                            ruleId = decision.ruleId
                        )
                    )
                }
            }
        }
    }
}

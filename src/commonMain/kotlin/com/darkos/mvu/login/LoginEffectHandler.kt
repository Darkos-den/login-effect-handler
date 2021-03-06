package com.darkos.mvu.login

import com.darkos.mvu.EffectHandler
import com.darkos.mvu.login.model.LoginResult
import com.darkos.mvu.login.model.mvu.LoginEffect
import com.darkos.mvu.login.model.mvu.LoginMessage
import com.darkos.mvu.models.Effect
import com.darkos.mvu.models.Message
import com.darkos.mvu.validation.IValidationHandler
import com.darkos.mvu.validation.model.mvu.ValidationEffect

abstract class LoginEffectHandler<T : Any, R : Any>(
    private val remote: LoginRemote<T, R>,
    private val validationHandler: IValidationHandler
) : EffectHandler {

    abstract suspend fun processLoginResult(result: R)
    abstract suspend fun processError(error: Exception): Message
    abstract suspend fun processSuccess(): Message

    override suspend fun call(effect: Effect): Message {
        return when (effect) {
            is ValidationEffect -> validationHandler.call(effect)
            is LoginEffect -> processLoginEffect(effect)
            else -> throw IllegalArgumentException("new supported effect $effect")
        }
    }

    private suspend fun processLoginEffect(effect: LoginEffect): Message {
        return when (effect) {
            is LoginEffect.Process<*> -> {
                login(effect.data as T)
            }
            is LoginEffect.Error -> {
                processError(effect.e)
            }
            is LoginEffect.Success -> {
                processSuccess()
            }
        }
    }

    private suspend fun login(data: T): LoginMessage {
        return remote.login(data).let {
            when (it) {
                is LoginResult.Error -> {
                    LoginMessage.LoginFailed(it.error)
                }
                is LoginResult.Success -> {
                    processLoginResult(it.data)
                    LoginMessage.LoginSuccess
                }
            }
        }
    }
}
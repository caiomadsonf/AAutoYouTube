package br.zx9.krpqw.aabbcc

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class CarService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // ALLOW_ALL_HOSTS_VALIDATOR é aceitável para desenvolvimento/DHU.
        // Para publicação na Play Store, substituir por validação de certificado.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return CarSession()
    }
}
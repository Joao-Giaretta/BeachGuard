package com.beachguard.projeto3_equipe26.components

// uma classe que representa uma conta de usuário
class Account (val nome: String, val email: String, val cpf: String, val dataNascimento: String, val celular: String, val role: String) {
    class Validate(private val account: Account) {
        private fun isNameValid(): Boolean {
            return account.nome.isNotEmpty()
        }
        private fun isEmailValid(): Boolean {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(account.email).matches()
        }
        private fun isCpfValid(): Boolean {
            return (account.cpf.length == 11 && account.cpf.matches(Regex("[0-9]+")))
        }
        private fun isDataNascimentoValid(): Boolean {
            return account.dataNascimento.matches(Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}"))
        }
        private fun isCelularValid(): Boolean {
            return account.celular.matches(Regex("\\([0-9]{2}\\) [0-9]{5}-[0-9]{4}"))
        }
        fun isValid(): Boolean {
            return isNameValid() && isEmailValid() && isCpfValid() && isDataNascimentoValid() && isCelularValid()
        }
    }
}
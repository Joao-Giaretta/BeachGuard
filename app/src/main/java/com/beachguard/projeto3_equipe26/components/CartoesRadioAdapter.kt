package com.beachguard.projeto3_equipe26.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.beachguard.projeto3_equipe26.R

class CartoesRadioAdapter(private var cartoes: List<Cartao>) : RecyclerView.Adapter<CartoesRadioAdapter.CartaoViewHolder>(){

    // Atributo que representa o cartão selecionado
    private var cartaoSelecionado: Cartao? = null

    inner class CartaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Atributo que representa o RadioButton
        val radioButton: RadioButton = itemView.findViewById(R.id.radioButtonCartao)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartaoViewHolder {
        // Infla o layout do cartão
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.cartao_radio, parent, false)
        return CartaoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CartaoViewHolder, position: Int) {
        // Configuração dos dados do cartão
        val cartao = cartoes[position]
        holder.radioButton.text = "${cartao.cardNumber} - ${cartao.cardName}"
        holder.radioButton.isChecked = cartao == cartaoSelecionado
        holder.radioButton.setOnClickListener {
            cartaoSelecionado = cartao
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        // Retorna a quantidade de cartões
        return cartoes.size
    }
}
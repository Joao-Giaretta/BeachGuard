package com.beachguard.projeto3_equipe26.components

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.beachguard.projeto3_equipe26.R
import com.beachguard.projeto3_equipe26.databinding.CartaoBinding

class CartoesListAdapter(private val cartoes: List<Cartao>, private val listener: OnDeleteCard) : RecyclerView.Adapter<CartoesListAdapter.CartaoViewHolder>() {
    inner class CartaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        // Atributo que representa o layout do cartão
        val binding = CartaoBinding.bind(itemView)
        fun bind(cartao: Cartao){
            // Configuração dos dados do cartão
            Log.d("CartaoAdapter", "Configurando dados do cartão: $cartao")
            binding.tvNumeroCartao.text = cartao.cardNumber
            binding.tvNomeCartao.text = cartao.cardName
            binding.tvDataCartao.text = cartao.dataValidade

        }

        init {
            // Configuração do clique do botão de exclusão
            binding.btnDelete.setOnClickListener {
                listener.delete(cartoes[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartaoViewHolder {
        // Infla o layout do cartão
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cartao, parent, false)
        Log.d("CartaoAdapter", "ViewHolder criada")
        return CartaoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartaoViewHolder, position: Int) {
        // Configuração dos dados do cartão
        Log.d("Adapter", "onBindViewHolder - Posição: $position")
        holder.bind(cartoes[position])

        // Configuração do clique do botão de exclusão
        holder.binding.btnDelete.setOnClickListener {
            Log.d("Adapter", "onDeleteClick acionado para o cartão na posição $position")
            listener.delete(cartoes[position])
        }
    }

    override fun getItemCount(): Int {
        // Retorna a quantidade de cartões
        return cartoes.size
    }

}